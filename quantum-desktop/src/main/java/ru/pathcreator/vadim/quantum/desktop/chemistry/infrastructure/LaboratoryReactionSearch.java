/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeStructureSearch;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.search.CdkMoleculeStructureSearch;

/** Read-only transformation search over saved local experiments, with stage-local roles and explicit yield references. */
public final class LaboratoryReactionSearch {
  public enum Outcome {
    ALL("Любой результат"),
    OBSERVED("Есть фактическое количество продукта"),
    POSITIVE("Есть полученный продукт (> 0)"),
    ZERO("Записан нулевой продукт"),
    UNRECORDED("Нет собственного измерения продукта"),
    LITERATURE("Литературный результат"),
    SIMULATED("Учебный / модельный результат");
    private final String title;

    Outcome(final String title) {
      this.title = title;
    }

    public String toString() {
      return this.title;
    }
  }

  /** Explicit stage conclusions are independent of recorded quantities and their origin filters. */
  public record StageOutcomeFilter(OutcomeStatus status, Origin origin, boolean hasByproducts) {
    public static final StageOutcomeFilter ANY = new StageOutcomeFilter(null, null, false);

    public StageOutcomeFilter {
      if (origin == Origin.UNSPECIFIED) {
        throw new IllegalArgumentException(
            "Происхождение исхода стадии должно быть явным; для любого происхождения оставьте фильтр пустым.");
      }
    }

    public boolean active() {
      return status != null || origin != null || hasByproducts;
    }
  }

  public record Request(
      Molecule source,
      Molecule product,
      MoleculeStructureSearch.Options options,
      String text,
      Outcome outcome,
      BigDecimal minimumYield,
      BigDecimal maximumYield,
      MoleculeDrawing sourceDrawing,
      MoleculeDrawing productDrawing,
      StageOutcomeFilter stageOutcomeFilter) {
    public Request(
        final Molecule source,
        final Molecule product,
        final MoleculeStructureSearch.Options options,
        final String text,
        final Outcome outcome,
        final BigDecimal minimumYield,
        final BigDecimal maximumYield,
        final MoleculeDrawing sourceDrawing,
        final MoleculeDrawing productDrawing) {
      this(
          source,
          product,
          options,
          text,
          outcome,
          minimumYield,
          maximumYield,
          sourceDrawing,
          productDrawing,
          StageOutcomeFilter.ANY);
    }

    public Request(
        final Molecule source,
        final Molecule product,
        final MoleculeStructureSearch.Options options,
        final String text,
        final Outcome outcome,
        final BigDecimal minimumYield,
        final BigDecimal maximumYield) {
      this(
          source,
          product,
          options,
          text,
          outcome,
          minimumYield,
          maximumYield,
          null,
          null,
          StageOutcomeFilter.ANY);
    }

    public Request {
      Objects.requireNonNull(options);
      Objects.requireNonNull(text);
      Objects.requireNonNull(outcome);
      Objects.requireNonNull(stageOutcomeFilter);
      if (sourceDrawing != null && !sourceDrawing.molecule().equals(source)
          || productDrawing != null && !productDrawing.molecule().equals(product)) {
        throw new IllegalArgumentException("Рисунок запроса не соответствует выбранной структуре.");
      }
      text = text.strip();
      if (text.length() > 2000) {
        throw new IllegalArgumentException("Запрос содержит больше 2000 символов.");
      }
      for (final var value : Arrays.asList(minimumYield, maximumYield)) {
        if (value != null
            && (value.signum() < 0
                || value.precision() > 30
                || Math.abs((long) value.scale()) > 30)) {
          throw new IllegalArgumentException(
              "Граница выхода должна быть неотрицательным числом до 30 знаков.");
        }
      }
      if (minimumYield != null
          && maximumYield != null
          && minimumYield.compareTo(maximumYield) > 0) {
        throw new IllegalArgumentException("Нижняя граница выхода больше верхней.");
      }
      if ((minimumYield != null || maximumYield != null) && source == null) {
        throw new IllegalArgumentException(
            "Для поиска по выходу выберите исходник запроса: единственное совпадение станет опорным.");
      }
    }

    public boolean yieldFiltered() {
      return this.minimumYield != null || this.maximumYield != null;
    }
  }

  public record Participant(LaboratoryComponent component, MoleculeStructureSearch.Match match) {}

  public record Yield(String productId, String referenceId, BigDecimal percent) {}

  public record LiteratureSource(
      LaboratoryLiterature.Method method, LaboratoryLiterature.Reference reference) {
    public LiteratureSource {
      Objects.requireNonNull(method);
      Objects.requireNonNull(reference);
    }

    public boolean common() {
      return this.method.stageId() == null;
    }
  }

  public record Hit(
      Path path,
      LaboratoryDrawing drawing,
      LaboratoryReactionStage stage,
      List<Participant> sources,
      List<Participant> products,
      List<Yield> yields,
      List<String> notes,
      double score) {
    public Hit {
      sources = List.copyOf(sources);
      products = List.copyOf(products);
      yields = List.copyOf(yields);
      notes = List.copyOf(notes);
    }

    public String key() {
      return this.path + "#" + this.stage.id();
    }

    public boolean hasFact() {
      return this.products.stream().anyMatch(p -> observed(p.component()));
    }
  }

  public record Result(
      List<Hit> hits,
      List<String> diagnostics,
      int documents,
      int stages,
      int comparisons,
      int errors,
      boolean complete) {
    public Result {
      hits = List.copyOf(hits);
      diagnostics = List.copyOf(diagnostics);
    }
  }

  private record Document(Path path, LaboratoryDrawing drawing) {}

  private static final MoleculeStructureSearch.Match ANY =
      new MoleculeStructureSearch.Match(true, 1, Set.of());

  public Result search(
      final Path folder,
      final Request request,
      final BooleanSupplier cancelled,
      final Consumer<String> progress)
      throws IOException {
    Objects.requireNonNull(request);
    Objects.requireNonNull(cancelled);
    Objects.requireNonNull(progress);
    check(cancelled);
    final var engine = new CdkMoleculeStructureSearch();
    final var sourceQuery =
        request.source == null
            ? null
            : request.sourceDrawing == null
                ? engine.compile(request.source, request.options, cancelled)
                : engine.compile(request.sourceDrawing, request.options, cancelled);
    final var productQuery =
        request.product == null
            ? null
            : request.productDrawing == null
                ? engine.compile(request.product, request.options, cancelled)
                : engine.compile(request.productDrawing, request.options, cancelled);
    final List<Document> documents = new ArrayList<>();
    final int[] omittedDocuments = {0};
    progress.accept("Читаю сохранённые опыты проекта…");
    final var index =
        new LaboratoryExperimentIndex()
            .scan(
                folder,
                new LaboratoryExperimentIndex.Limits(
                    4000, 24, LaboratoryExperimentIndex.PROJECT_SCAN_BYTES),
                (path, drawing) -> {
                  check(cancelled);
                  if (documents.size() >= 300) {
                    omittedDocuments[0]++;
                    return;
                  }
                  documents.add(new Document(path, drawing));
                  if (documents.size() % 10 == 1) {
                    progress.accept(
                        "Прочитано опытов: " + documents.size() + " · " + path.getFileName());
                  }
                });
    final List<String> diagnostics = new ArrayList<>(index.diagnostics());
    index.entries().stream()
        .filter(e -> e.problem() != null)
        .forEach(e -> diagnostics.add(e.path().getFileName() + ": " + e.problem()));
    if (omittedDocuments[0] > 0) {
      diagnostics.add(
          "Поиск ограничен 300 опытами. Ещё "
              + omittedDocuments[0]
              + " прочитанных опытов не сопоставлены; сузьте папку.");
    }
    final int[] comparisons = {0},
        errors = {(int) index.entries().stream().filter(e -> e.problem() != null).count()};
    final List<Hit> hits = new ArrayList<>();
    int examined = 0, withoutStage = 0, unknownYield = 0;
    boolean limited = false;
    outer:
    for (final var document : documents) {
      check(cancelled);
      final var experiment = document.drawing.experiment();
      if (experiment.stages().isEmpty()) {
        withoutStage++;
        continue;
      }
      final Map<String, LaboratoryComponent> components = new LinkedHashMap<>();
      experiment.components().forEach(c -> components.put(c.id(), c));
      final Map<String, MoleculeStructureSearch.Match> sourceCache = new HashMap<>(),
          productCache = new HashMap<>();
      for (final var stage : experiment.stages()) {
        check(cancelled);
        if (examined >= 3000 || comparisons[0] >= 8000) {
          limited = true;
          break outer;
        }
        examined++;
        if (examined % 10 == 1) {
          progress.accept("Проверяю превращение " + examined + " · " + experiment.title());
        }
        if (!stageOutcomeMatches(experiment, stage, request.stageOutcomeFilter)
            || !textMatches(experiment, stage, request.text, cancelled)) {
          continue;
        }
        try {
          final var sources =
              matches(
                  stage,
                  components,
                  document.drawing.layouts(),
                  ReactionComponentRole.REACTANT,
                  sourceQuery,
                  sourceCache,
                  comparisons,
                  cancelled);
          final var products =
              matches(
                  stage,
                  components,
                  document.drawing.layouts(),
                  ReactionComponentRole.PRODUCT,
                  productQuery,
                  productCache,
                  comparisons,
                  cancelled);
          if (request.source != null && sources.isEmpty()
              || request.product != null && products.isEmpty()) {
            continue;
          }
          if (!outcome(request.outcome, products)) {
            continue;
          }
          final List<String> notes = new ArrayList<>();
          List<Yield> yields;
          if (commonNotesMatch(experiment, request.text)) {
            notes.add(
                "Текст запроса встречается в общих заметках или журнале опыта. Это не устанавливает исход выбранной стадии.");
          }
          stageOutcome(experiment, stage)
              .ifPresent(
                  value -> {
                    final Set<String> present =
                        stage.participants().stream()
                            .filter(p -> p.role() == ReactionComponentRole.PRODUCT)
                            .map(LaboratoryStageParticipant::componentId)
                            .collect(java.util.stream.Collectors.toSet());
                    final boolean changed =
                        java.util.stream.Stream.concat(
                                value.targets().stream(), value.byproducts().stream())
                            .anyMatch(
                                product -> {
                                  if (!present.contains(product.componentId())) {
                                    return true;
                                  }
                                  final String snapshot =
                                      ru.pathcreator.vadim.quantum.chemistry.domain.identity
                                          .MolecularIdentity.signature(product.snapshot());
                                  return experiment.components().stream()
                                      .filter(c -> c.id().equals(product.componentId()))
                                      .noneMatch(
                                          c ->
                                              ru.pathcreator.vadim.quantum.chemistry.domain.identity
                                                  .MolecularIdentity.signature(c.molecule())
                                                  .equals(snapshot));
                                });
                    if (changed) {
                      notes.add(
                          "Участники стадии изменены после записи исхода. Заключение относится к сохранённым снимкам продуктов; текущая схема не переопределяет старый результат.");
                    }
                  });
          if (request.source == null) {
            yields = List.of();
            notes.add(
                "Для выхода выберите опорный исходник в карточке; отсутствие выбранной основы не заменяется средним выходом.");
          } else if (sources.size() != 1) {
            yields = List.of();
            notes.add(
                "С исходником запроса совпало "
                    + sources.size()
                    + " участников стадии. Уточните опорный исходник в карточке.");
          } else {
            try {
              yields =
                  yields(
                      document.drawing,
                      stage,
                      sources.getFirst().component.id(),
                      products.stream()
                          .map(p -> p.component.id())
                          .collect(java.util.stream.Collectors.toSet()),
                      notes);
            } catch (final RuntimeException error) {
              yields = List.of();
              notes.add("Выход не рассчитан: " + message(error));
            }
          }
          if (request.yieldFiltered()) {
            if (yields.isEmpty()) {
              unknownYield++;
              continue;
            }
            if (yields.stream()
                .noneMatch(
                    y ->
                        (request.minimumYield == null
                                || y.percent.compareTo(request.minimumYield) >= 0)
                            && (request.maximumYield == null
                                || y.percent.compareTo(request.maximumYield) <= 0))) {
              continue;
            }
          }
          final double sourceScore =
              request.source == null
                  ? 1
                  : sources.stream().mapToDouble(p -> p.match.score()).max().orElse(0);
          final double productScore =
              request.product == null
                  ? 1
                  : products.stream().mapToDouble(p -> p.match.score()).max().orElse(0);
          hits.add(
              new Hit(
                  document.path,
                  document.drawing,
                  stage,
                  sources,
                  products,
                  yields,
                  notes,
                  Math.min(sourceScore, productScore)));
        } catch (final CancellationException exception) {
          throw exception;
        } catch (final LimitReached exception) {
          limited = true;
          break outer;
        } catch (final RuntimeException exception) {
          errors[0]++;
          diagnostics.add(
              document.path.getFileName() + " · " + stage.title() + ": " + message(exception));
        }
      }
    }
    if (limited) {
      diagnostics.add(
          "Достигнут предел 3000 стадий или 8000 структурных сопоставлений. Остальная часть проекта не проверена.");
    }
    if (withoutStage > 0) {
      diagnostics.add(
          "В "
              + withoutStage
              + " опытах нет явных стадий: они не участвуют в поиске превращений. Состав отдельных соединений не принят за реакцию.");
    }
    if (unknownYield > 0) {
      diagnostics.add(
          "Для "
              + unknownYield
              + " подходящих стадий нельзя проверить диапазон выхода: не определена единственная основа или недостаточно фактических данных.");
    }
    hits.sort(
        Comparator.comparingDouble(Hit::score)
            .reversed()
            .thenComparing(h -> h.drawing.experiment().title(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Hit::key));
    check(cancelled);
    return new Result(
        hits,
        diagnostics,
        documents.size(),
        examined,
        comparisons[0],
        errors[0],
        diagnostics.isEmpty());
  }

  private static List<Participant> matches(
      final LaboratoryReactionStage stage,
      final Map<String, LaboratoryComponent> components,
      final Map<String, MoleculeDrawing> layouts,
      final ReactionComponentRole role,
      final MoleculeStructureSearch.Query query,
      final Map<String, MoleculeStructureSearch.Match> cache,
      final int[] comparisons,
      final BooleanSupplier cancelled) {
    final List<Participant> result = new ArrayList<>();
    for (final var participant : stage.participants()) {
      if (participant.role() != role) {
        continue;
      }
      check(cancelled);
      final var component = components.get(participant.componentId());
      var match = cache.get(component.id());
      if (match == null) {
        if (query != null && comparisons[0] >= 8000) {
          throw new LimitReached();
        }
        if (query != null) {
          comparisons[0]++;
        }
        match =
            query == null
                ? ANY
                : layouts.containsKey(component.id())
                    ? query.match(layouts.get(component.id()))
                    : query.match(component.molecule());
        cache.put(component.id(), match);
      }
      if (match.matched()) {
        result.add(new Participant(component, match));
      }
    }
    result.sort(
        Comparator.comparingDouble((Participant p) -> p.match.score())
            .reversed()
            .thenComparing(
                p -> p.component.molecule().displayName(), String.CASE_INSENSITIVE_ORDER));
    return List.copyOf(result);
  }

  /** Exact per-product yield from the existing quantity core, relative to the explicitly chosen stage reactant. */
  public static List<Yield> yields(
      final LaboratoryDrawing drawing,
      final LaboratoryReactionStage stage,
      final String referenceId,
      final Set<String> productIds,
      final List<String> notes) {
    if (stage.participants().stream()
        .noneMatch(
            p ->
                p.componentId().equals(referenceId)
                    && p.role() == ReactionComponentRole.REACTANT)) {
      throw new IllegalArgumentException("Опорным может быть только исходник выбранной стадии.");
    }
    final var experiment = drawing.experiment();
    final var reference =
        experiment.components().stream()
            .filter(c -> c.id().equals(referenceId))
            .findFirst()
            .orElseThrow();
    if (!observed(reference)) {
      notes.add(
          "У опорного исходника нет подтверждённой фактической загрузки. Выход по плановой загрузке не подставляется.");
      return List.of();
    }
    for (final var c : experiment.components()) {
      if (stage.participants().stream().anyMatch(p -> p.componentId().equals(c.id()))) {
        bounded(c);
      }
    }
    final var report =
        LaboratoryStoichiometry.preparative(
            experiment, stage, LaboratoryStoichiometry.Basis.ACTUAL, referenceId);
    final List<Yield> result = new ArrayList<>();
    for (final var row : report.rows()) {
      if (row.participant().role() != ReactionComponentRole.PRODUCT
          || !productIds.contains(row.component().id())) {
        continue;
      }
      if (!observed(row.component())) {
        notes.add(
            row.component().molecule().displayName()
                + ": "
                + row.component().evidence()
                + "; собственный фактический выход не подставляется.");
        continue;
      }
      if (row.yieldPercent() != null) {
        result.add(new Yield(row.component().id(), referenceId, row.yieldPercent()));
      } else {
        notes.add(row.component().molecule().displayName() + ": " + row.diagnostic());
      }
    }
    notes.addAll(report.diagnostics());
    return List.copyOf(result);
  }

  public static List<LaboratoryProcedure.Step> method(
      final LaboratoryDrawing drawing, final LaboratoryReactionStage stage) {
    return drawing.experiment().procedure().steps().stream()
        .filter(s -> s.stageId() == null || s.stageId().equals(stage.id()))
        .toList();
  }

  public static List<LaboratoryProcedure.Event> observations(
      final LaboratoryDrawing drawing, final LaboratoryReactionStage stage) {
    return drawing.experiment().procedure().events().stream()
        .filter(e -> e.snapshot().stageId() == null || e.snapshot().stageId().equals(stage.id()))
        .toList();
  }

  /** Only a linked method gives a reference stage scope. A common method is explicitly scoped to the whole experiment. */
  public static List<LiteratureSource> literature(
      final LaboratoryExperiment experiment, final LaboratoryReactionStage stage) {
    return literature(experiment, stage, () -> false);
  }

  private static List<LiteratureSource> literature(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final BooleanSupplier cancelled) {
    final Map<String, LaboratoryLiterature.Reference> references = new HashMap<>();
    for (final var reference : experiment.literature().references()) {
      check(cancelled);
      references.put(reference.id(), reference);
    }
    final List<LiteratureSource> result = new ArrayList<>();
    for (final var method : experiment.literature().methods()) {
      check(cancelled);
      if (method.stageId() == null || method.stageId().equals(stage.id())) {
        result.add(new LiteratureSource(method, references.get(method.referenceId())));
      }
    }
    return List.copyOf(result);
  }

  /** Complete read-only provenance; reported yield stays a literature value and never enters yields(). */
  public static String literatureText(final LiteratureSource source) {
    final var method = source.method();
    final var reference = source.reference();
    final StringBuilder text =
        new StringBuilder(
                source.common()
                    ? "Общая литературная методика опыта: "
                    : "Литературная методика стадии: ")
            .append(method.title())
            .append('\n')
            .append("Источник: ")
            .append(reference.citation())
            .append('\n');
    detail(text, "Авторы", reference.authors());
    detail(text, "Статья", reference.url());
    detail(text, "Supporting information", reference.supplementUrl());
    if (!reference.attachmentIds().isEmpty()) {
      detail(
          text,
          "Локальные вложения · идентификаторы",
          String.join(", ", reference.attachmentIds()));
    }
    detail(text, "Замечания к источнику", reference.notes());
    detail(text, "Перенос источника", reference.copyNote());
    if (method.literatureYieldPercent() != null) {
      text.append("Литературный выход: ")
          .append(method.literatureYieldPercent().stripTrailingZeros().toPlainString())
          .append(" % · ")
          .append(method.yieldBasis())
          .append(". Не измерение этого опыта.\n");
    } else {
      detail(text, "Основание литературного выхода", method.yieldBasis());
    }
    text.append("\nПроцедура из источника:\n").append(method.procedureText()).append('\n');
    detail(text, "Замечания к методике", method.notes());
    detail(text, "Перенос методики", method.copyNote());
    return text.toString();
  }

  private static void detail(final StringBuilder text, final String label, final String value) {
    if (!value.isBlank()) {
      text.append(label).append(": ").append(value).append('\n');
    }
  }

  public static String conditionsText(final ReactionConditions conditions) {
    final List<String> parts = new ArrayList<>();
    final String basic = LaboratoryProjectOverview.conditionsText(conditions);
    if (!basic.isBlank()) {
      parts.add(basic);
    }
    for (final var material : conditions.components()) {
      if (material.note() != null && !material.note().isBlank()) {
        parts.add(material.name() + ": " + material.note());
      }
    }
    for (final var feature : conditions.features()) {
      final String kind =
          switch (feature.kind()) {
            case INERT_ATMOSPHERE -> "Инертная атмосфера";
            case AIR_SENSITIVE -> "Чувствительность к воздуху";
            case ANHYDROUS -> "Безводные условия";
            case CATALYST_PRESENT -> "Катализатор";
            case DEHYDRATING_AGENT -> "Осушитель";
            case ACIDIC_MEDIUM -> "Кислая среда";
            case BASIC_MEDIUM -> "Основная среда";
            case OXIDIZING_MEDIUM -> "Окислительная среда";
            case REDUCING_MEDIUM -> "Восстановительная среда";
            case HYDROGENATION -> "Гидрирование";
            case PHOTOCHEMICAL -> "Фотохимия";
            case ELECTROCHEMICAL -> "Электрохимия";
            case MICROWAVE -> "Микроволновое нагревание";
            case SEALED_VESSEL -> "Закрытый сосуд";
            case FLOW_REACTOR -> "Проточный режим";
            case REFLUX -> "Кипячение с обратным холодильником";
            case COOLING -> "Охлаждение";
            case HEATING -> "Нагревание";
            case ADDITION_RATE -> "Скорость добавления";
            case WORKUP -> "Обработка";
            case PURIFICATION -> "Очистка";
            case CUSTOM -> "Дополнительное условие";
          };
      parts.add(
          kind
              + (feature.hasValue() ? ": " + feature.value() : "")
              + (feature.hasNote() ? " · " + feature.note() : ""));
    }
    return String.join(" · ", parts);
  }

  public static boolean observed(final LaboratoryComponent component) {
    return component.evidence() == LaboratoryEvidence.OBSERVED && component.actual() != null;
  }

  private static boolean outcome(final Outcome outcome, final List<Participant> products) {
    return switch (outcome) {
      case ALL -> true;
      case OBSERVED -> products.stream().anyMatch(p -> observed(p.component));
      case POSITIVE ->
          products.stream()
              .anyMatch(p -> observed(p.component) && p.component.actual().value().signum() > 0);
      case ZERO ->
          products.stream()
              .anyMatch(p -> observed(p.component) && p.component.actual().value().signum() == 0);
      case UNRECORDED -> products.stream().noneMatch(p -> observed(p.component));
      case LITERATURE ->
          products.stream()
              .anyMatch(
                  p ->
                      p.component.evidence() == LaboratoryEvidence.LITERATURE
                          && p.component.actual() != null);
      case SIMULATED ->
          products.stream()
              .anyMatch(
                  p ->
                      p.component.evidence() == LaboratoryEvidence.SIMULATED
                          && p.component.actual() != null);
    };
  }

  public static Optional<StageOutcome> stageOutcome(
      final LaboratoryExperiment experiment, final LaboratoryReactionStage stage) {
    return experiment.operations().outcomeFor(stage.id());
  }

  public static String stageOutcomeText(
      final LaboratoryExperiment experiment, final LaboratoryReactionStage stage) {
    final var value = stageOutcome(experiment, stage).orElse(null);
    if (value == null) {
      return "Исход стадии: не оценено (запись отсутствует)";
    }
    final String targets =
        String.join("; ", value.targets().stream().map(p -> p.snapshot().displayName()).toList());
    final String byproducts =
        String.join(
            "; ", value.byproducts().stream().map(p -> p.snapshot().displayName()).toList());
    return "Исход стадии: "
        + value.status()
        + " · "
        + value.observation().origin()
        + (targets.isEmpty() ? "" : "\nЦель: " + targets)
        + (byproducts.isEmpty() ? "" : "\nПобочные продукты: " + byproducts)
        + "\nОснование / причина: "
        + value.reason()
        + "\nИсточник: "
        + value.source();
  }

  public static boolean stageOutcomeMatches(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final StageOutcomeFilter filter) {
    Objects.requireNonNull(filter);
    final var value = stageOutcome(experiment, stage).orElse(null);
    if (value == null) {
      return (filter.status() == null || filter.status() == OutcomeStatus.NOT_EVALUATED)
          && filter.origin() == null
          && !filter.hasByproducts();
    }
    return (filter.status() == null || filter.status() == value.status())
        && (filter.origin() == null || filter.origin() == value.observation().origin())
        && (!filter.hasByproducts() || !value.byproducts().isEmpty());
  }

  private static boolean commonNotesMatch(
      final LaboratoryExperiment experiment, final String query) {
    if (query.isBlank()) {
      return false;
    }
    final var text = new StringBuilder(experiment.notes());
    experiment.journal().forEach(entry -> text.append(' ').append(entry.text()));
    final String normalized = LaboratoryExperimentIndex.normalize(text.toString());
    final var words =
        Arrays.stream(LaboratoryExperimentIndex.normalize(query).split(" "))
            .filter(word -> !word.isBlank())
            .toList();
    return !words.isEmpty() && words.stream().allMatch(normalized::contains);
  }

  private static boolean textMatches(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final String query,
      final BooleanSupplier cancelled) {
    if (query.isBlank()) {
      return true;
    }
    final var text =
        new StringBuilder(experiment.title())
            .append(' ')
            .append(experiment.notes())
            .append(' ')
            .append(stage.title())
            .append(" ")
            .append(LaboratoryReactionClassificationText.searchable(stage.classification()))
            .append(' ')
            .append(conditionsText(experiment.conditions()))
            .append(' ')
            .append(conditionsText(stage.conditions()));
    for (final var c : experiment.components()) {
      if (stage.participants().stream().anyMatch(p -> p.componentId().equals(c.id()))) {
        text.append(' ')
            .append(c.molecule().displayName())
            .append(' ')
            .append(c.molecule().formula().hillNotation());
      }
    }
    for (final var entry : experiment.journal()) {
      check(cancelled);
      text.append(' ').append(entry.text());
    }
    for (final var step : experiment.procedure().steps()) {
      check(cancelled);
      if (step.stageId() == null || step.stageId().equals(stage.id())) {
        text.append(' ').append(step.title()).append(' ').append(step.instruction());
      }
    }
    for (final var event : experiment.procedure().events()) {
      check(cancelled);
      if (event.snapshot().stageId() == null || event.snapshot().stageId().equals(stage.id())) {
        text.append(' ').append(event.text());
      }
    }
    text.append(" ")
        .append(LaboratoryReactionMethodText.method(experiment, stage, cancelled))
        .append(" ")
        .append(LaboratoryReactionMethodText.observations(experiment, stage, cancelled));
    final String normalized = LaboratoryExperimentIndex.normalize(text.toString());
    final List<String> remaining =
        new ArrayList<>(Arrays.asList(LaboratoryExperimentIndex.normalize(query).split(" ")));
    remaining.removeIf(normalized::contains);
    if (remaining.isEmpty()) {
      return true;
    }
    for (final var source : literature(experiment, stage, cancelled)) {
      check(cancelled);
      final String content = LaboratoryExperimentIndex.normalize(literatureText(source));
      remaining.removeIf(content::contains);
      if (remaining.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static void bounded(final LaboratoryComponent component) {
    final List<BigDecimal> numbers = new ArrayList<>();
    if (component.actual() != null) {
      numbers.add(component.actual().value());
    }
    final var p = component.properties();
    numbers.addAll(Arrays.asList(p.molarMass(), p.purityPercent(), p.molarity(), p.density()));
    for (final var n : numbers) {
      if (n != null && (n.precision() > 100 || Math.abs((long) n.scale()) > 100)) {
        throw new IllegalArgumentException(
            "Слишком большое число в количестве или свойствах «"
                + component.molecule().displayName()
                + "».");
      }
    }
  }

  private static String message(final Throwable error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
  }

  private static void check(final BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      throw new CancellationException("Поиск превращений остановлен.");
    }
  }

  private static final class LimitReached extends RuntimeException {}
}
