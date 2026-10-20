/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Local reaction monitoring, workup and purification records. Corrections append; unknown values remain absent. */
public record LaboratoryReactionOperations(
    List<TimePoint> timePoints,
    List<Workup> workups,
    List<Purification> purifications,
    List<StageOutcome> stageOutcomes) {
  public LaboratoryReactionOperations(
      final List<TimePoint> timePoints,
      final List<Workup> workups,
      final List<Purification> purifications) {
    this(timePoints, workups, purifications, List.of());
  }

  public static final LaboratoryReactionOperations EMPTY =
      new LaboratoryReactionOperations(List.of(), List.of(), List.of());

  public enum ElapsedUnit {
    SECOND("с"),
    MINUTE("мин"),
    HOUR("ч");
    private final String label;

    ElapsedUnit(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum ConversionUnit {
    PERCENT("%"),
    FRACTION("доля 0–1");
    private final String label;

    ConversionUnit(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum AnalysisMethod {
    TLC("ТСХ"),
    HPLC("ВЭЖХ"),
    GC("ГХ"),
    NMR("ЯМР"),
    OTHER("Другой метод");
    private final String label;

    AnalysisMethod(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum PhaseKind {
    MIXTURE("Реакционная смесь"),
    ORGANIC("Органическая фаза"),
    AQUEOUS("Водная фаза"),
    SOLID("Твёрдая фаза"),
    FILTRATE("Фильтрат"),
    OTHER("Другая фаза");
    private final String label;

    PhaseKind(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum WorkupKind {
    QUENCH("Обработка реагентом"),
    EXTRACT("Экстракция"),
    WASH("Промывка"),
    SEPARATE("Разделение фаз"),
    DRY("Сушка"),
    FILTER("Фильтрование"),
    CONCENTRATE("Концентрирование"),
    TRANSFER("Перенос"),
    OTHER("Другая операция");
    private final String label;

    WorkupKind(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum PurificationKind {
    COLUMN("Колоночная хроматография"),
    RECRYSTALLIZATION("Перекристаллизация"),
    PREPARATIVE_HPLC("Препаративная ВЭЖХ"),
    CUSTOM("Другой метод");
    private final String label;

    PurificationKind(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum GradientAxis {
    MINUTES("мин"),
    MILLILITRES("мл"),
    COLUMN_VOLUMES("объёмов колонки");
    private final String label;

    GradientAxis(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  /** Origin describes the stated observation, never a conversion inferred from linked analysis. */
  public enum Origin {
    UNSPECIFIED("Не указано"),
    MEASURED("Измерено"),
    LITERATURE("По литературе"),
    SIMULATED("Расчёт / модель");
    private final String label;

    Origin(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum AnalysisKind {
    LEGACY,
    EVIDENCE
  }

  public record AnalysisRef(AnalysisKind kind, String id) {
    public AnalysisRef {
      Objects.requireNonNull(kind, "Тип аналитической ссылки");
      id = identifier(id);
    }
  }

  public record Observation(Origin origin, List<AnalysisRef> analyses) {
    public static final Observation EMPTY = new Observation(Origin.UNSPECIFIED, List.of());

    public Observation {
      Objects.requireNonNull(origin, "Происхождение наблюдения");
      analyses = List.copyOf(analyses);
      if (analyses.size() > 64 || new HashSet<>(analyses).size() != analyses.size()) {
        throw new IllegalArgumentException("Укажите до 64 различных аналитических ссылок.");
      }
    }
  }

  public interface Revision {
    String id();

    String stageId();

    String stageTitle();

    Instant recordedAt();

    String correctsId();
  }

  public enum OutcomeStatus {
    TARGET_OBTAINED("Цель получена"),
    TARGET_NOT_OBTAINED("Цель не получена"),
    MIXTURE("Смесь"),
    NOT_EVALUATED("Не оценено");
    private final String label;

    OutcomeStatus(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public record OutcomeProduct(String componentId, Molecule snapshot) {
    public OutcomeProduct {
      componentId = identifier(componentId);
      Objects.requireNonNull(snapshot, "Сохранённая структура продукта");
    }
  }

  /** Explicit author conclusion, independent of quantities and analytical confirmation. */
  public record StageOutcome(
      String id,
      String stageId,
      String stageTitle,
      Instant recordedAt,
      OutcomeStatus status,
      List<OutcomeProduct> targets,
      List<OutcomeProduct> byproducts,
      Observation observation,
      String source,
      String reason,
      String correctsId)
      implements Revision {
    public StageOutcome {
      id = identifier(id);
      stageId = identifier(stageId);
      stageTitle = required(stageTitle, 500);
      Objects.requireNonNull(recordedAt);
      Objects.requireNonNull(status);
      targets = List.copyOf(targets);
      byproducts = List.copyOf(byproducts);
      observation = Objects.requireNonNull(observation, "Происхождение исхода стадии");
      source = text(source, 2000);
      reason = text(reason, 10000);
      correctsId = optional(correctsId);
      if (observation.origin() == Origin.UNSPECIFIED || source.isBlank()) {
        throw new IllegalArgumentException(
            "Укажите происхождение и источник заключения об исходе стадии.");
      }
      limit(targets.size() + byproducts.size(), 200, "продуктов в исходе стадии");
      final Set<String> products = unique(targets, OutcomeProduct::componentId);
      for (final var product : byproducts) {
        if (!products.add(product.componentId())) {
          throw new IllegalArgumentException(
              "Целевые и побочные продукты не должны повторяться или пересекаться.");
        }
      }
      final List<OutcomeProduct> all = new ArrayList<>(targets);
      all.addAll(byproducts);
      unique(all, p -> p.snapshot().id().value());
      if ((status == OutcomeStatus.TARGET_OBTAINED || status == OutcomeStatus.TARGET_NOT_OBTAINED)
          && targets.isEmpty()) {
        throw new IllegalArgumentException("Выберите хотя бы один целевой продукт стадии.");
      }
      if ((correctsId != null || status == OutcomeStatus.MIXTURE && all.isEmpty())
          && reason.isBlank()) {
        throw new IllegalArgumentException(
            "Укажите причину исправления или поясните смесь неизвестного состава.");
      }
    }
  }

  public record TimePoint(
      String id,
      String stageId,
      String stageTitle,
      Instant recordedAt,
      BigDecimal elapsed,
      ElapsedUnit elapsedUnit,
      String sampleId,
      String label,
      AnalysisMethod method,
      BigDecimal conversion,
      ConversionUnit conversionUnit,
      String source,
      String note,
      String correctsId,
      Observation observation)
      implements Revision {
    public TimePoint(
        String id,
        String stageId,
        String stageTitle,
        Instant recordedAt,
        BigDecimal elapsed,
        ElapsedUnit elapsedUnit,
        String sampleId,
        String label,
        AnalysisMethod method,
        BigDecimal conversion,
        ConversionUnit conversionUnit,
        String source,
        String note,
        String correctsId) {
      this(
          id,
          stageId,
          stageTitle,
          recordedAt,
          elapsed,
          elapsedUnit,
          sampleId,
          label,
          method,
          conversion,
          conversionUnit,
          source,
          note,
          correctsId,
          Observation.EMPTY);
    }

    public TimePoint {
      observation = Objects.requireNonNull(observation, "Происхождение наблюдения");
      id = identifier(id);
      stageId = optional(stageId);
      stageTitle = text(stageTitle, 500);
      Objects.requireNonNull(recordedAt);
      elapsed = number(elapsed, true);
      Objects.requireNonNull(elapsedUnit);
      sampleId = optional(sampleId);
      label = required(label, 500);
      Objects.requireNonNull(method);
      Objects.requireNonNull(conversionUnit);
      conversion = number(conversion, false);
      if (conversion != null
          && conversion.compareTo(
                  conversionUnit == ConversionUnit.PERCENT ? new BigDecimal("100") : BigDecimal.ONE)
              > 0) {
        throw new IllegalArgumentException("Конверсия выходит за диапазон выбранной единицы.");
      }
      source = text(source, 2000);
      note = text(note, 10000);
      correctsId = optional(correctsId);
      if (conversion != null && source.isBlank()) {
        throw new IllegalArgumentException(
            "Укажите источник или основание конверсии. ТСХ или заметка сами по себе не дают числовую конверсию.");
      }
    }
  }

  public record Phase(String id, String label, PhaseKind kind) {
    public Phase {
      id = identifier(id);
      label = required(label, 500);
      Objects.requireNonNull(kind);
    }
  }

  public record WorkupStep(
      String id,
      WorkupKind kind,
      String fromPhaseId,
      String toPhaseId,
      String material,
      int repetitions,
      LaboratoryQuantity planned,
      LaboratoryQuantity actual,
      String source,
      String note,
      Observation observation) {
    public WorkupStep(
        String id,
        WorkupKind kind,
        String fromPhaseId,
        String toPhaseId,
        String material,
        int repetitions,
        LaboratoryQuantity planned,
        LaboratoryQuantity actual,
        String source,
        String note) {
      this(
          id,
          kind,
          fromPhaseId,
          toPhaseId,
          material,
          repetitions,
          planned,
          actual,
          source,
          note,
          Observation.EMPTY);
    }

    public WorkupStep {
      observation = Objects.requireNonNull(observation, "Происхождение наблюдения");
      id = identifier(id);
      Objects.requireNonNull(kind);
      fromPhaseId = optional(fromPhaseId);
      toPhaseId = optional(toPhaseId);
      material = text(material, 1000);
      if (repetitions < 1 || repetitions > 1000) {
        throw new IllegalArgumentException(
            "Число повторов должно быть от 1 до 1000. Количество задаётся суммарно для всех повторов.");
      }
      quantity(planned);
      quantity(actual);
      source = text(source, 2000);
      note = text(note, 10000);
      fact(actual, source);
    }
  }

  public record Loss(
      String id,
      String label,
      LaboratoryQuantity planned,
      LaboratoryQuantity actual,
      String source,
      Observation observation) {
    public Loss(
        String id,
        String label,
        LaboratoryQuantity planned,
        LaboratoryQuantity actual,
        String source) {
      this(id, label, planned, actual, source, Observation.EMPTY);
    }

    public Loss {
      observation = Objects.requireNonNull(observation, "Происхождение наблюдения");
      id = identifier(id);
      label = required(label, 500);
      quantity(planned);
      quantity(actual);
      source = text(source, 2000);
      fact(actual, source);
    }
  }

  public record Workup(
      String id,
      String stageId,
      String stageTitle,
      Instant recordedAt,
      String title,
      List<Phase> phases,
      List<WorkupStep> steps,
      List<Loss> losses,
      String note,
      String correctsId)
      implements Revision {
    public Workup {
      id = identifier(id);
      stageId = optional(stageId);
      stageTitle = text(stageTitle, 500);
      Objects.requireNonNull(recordedAt);
      title = required(title, 500);
      note = text(note, 20000);
      correctsId = optional(correctsId);
      phases = List.copyOf(phases);
      steps = List.copyOf(steps);
      losses = List.copyOf(losses);
      limit(phases.size(), 200, "фаз");
      limit(steps.size(), 2000, "операций обработки");
      limit(losses.size(), 2000, "записей потерь");
      final var ids = unique(phases, Phase::id);
      unique(steps, WorkupStep::id);
      unique(losses, Loss::id);
      for (final var step : steps) {
        if (step.fromPhaseId != null && !ids.contains(step.fromPhaseId)
            || step.toPhaseId != null && !ids.contains(step.toPhaseId)) {
          throw new IllegalArgumentException("Операция ссылается на отсутствующую фазу.");
        }
      }
    }
  }

  public record GradientPoint(
      BigDecimal position, BigDecimal solventBPercent, BigDecimal flowMlPerMinute) {
    public GradientPoint {
      position = number(position, true);
      solventBPercent = number(solventBPercent, true);
      flowMlPerMinute = number(flowMlPerMinute, false);
      if (solventBPercent.compareTo(new BigDecimal("100")) > 0
          || flowMlPerMinute != null && flowMlPerMinute.signum() == 0) {
        throw new IllegalArgumentException(
            "Доля B должна быть от 0 до 100 %, скорость потока — положительной.");
      }
    }
  }

  public record Fraction(
      String id,
      String label,
      BigDecimal start,
      BigDecimal end,
      LaboratoryQuantity planned,
      LaboratoryQuantity actual,
      BigDecimal purityPercent,
      String sampleId,
      String source,
      String note,
      Observation observation) {
    public Fraction(
        String id,
        String label,
        BigDecimal start,
        BigDecimal end,
        LaboratoryQuantity planned,
        LaboratoryQuantity actual,
        BigDecimal purityPercent,
        String sampleId,
        String source,
        String note) {
      this(
          id,
          label,
          start,
          end,
          planned,
          actual,
          purityPercent,
          sampleId,
          source,
          note,
          Observation.EMPTY);
    }

    public Fraction {
      observation = Objects.requireNonNull(observation, "Происхождение наблюдения");
      id = identifier(id);
      label = required(label, 500);
      start = number(start, false);
      end = number(end, false);
      quantity(planned);
      quantity(actual);
      purityPercent = number(purityPercent, false);
      if ((start == null) != (end == null) || start != null && start.compareTo(end) > 0) {
        throw new IllegalArgumentException(
            "Границы фракции задаются вместе в возрастающем порядке.");
      }
      if (purityPercent != null && purityPercent.compareTo(new BigDecimal("100")) > 0) {
        throw new IllegalArgumentException("Чистота должна быть от 0 до 100 %.");
      }
      sampleId = optional(sampleId);
      source = text(source, 2000);
      note = text(note, 10000);
      fact(actual, source);
      if (purityPercent != null && source.isBlank()) {
        throw new IllegalArgumentException("Укажите источник или основание чистоты фракции.");
      }
    }
  }

  public record Pool(
      String id,
      String label,
      List<String> fractionIds,
      LaboratoryQuantity actual,
      String sampleId,
      String source,
      String note,
      Observation observation) {
    public Pool(
        String id,
        String label,
        List<String> fractionIds,
        LaboratoryQuantity actual,
        String sampleId,
        String source,
        String note) {
      this(id, label, fractionIds, actual, sampleId, source, note, Observation.EMPTY);
    }

    public Pool {
      observation = Objects.requireNonNull(observation, "Происхождение наблюдения");
      id = identifier(id);
      label = required(label, 500);
      fractionIds = List.copyOf(fractionIds);
      if (fractionIds.size() < 2 || fractionIds.size() > 2000) {
        throw new IllegalArgumentException("Для объединения выберите от 2 до 2000 фракций.");
      }
      final var seen = new HashSet<String>();
      for (final var fraction : fractionIds) {
        if (!seen.add(identifier(fraction))) {
          throw new IllegalArgumentException("Фракция указана в объединении дважды.");
        }
      }
      quantity(actual);
      sampleId = optional(sampleId);
      source = text(source, 2000);
      note = text(note, 10000);
      fact(actual, source);
    }
  }

  public record Purification(
      String id,
      String stageId,
      String stageTitle,
      Instant recordedAt,
      String title,
      PurificationKind kind,
      String inputSampleId,
      LaboratoryQuantity inputPlanned,
      LaboratoryQuantity inputActual,
      String stationaryPhase,
      LaboratoryQuantity sorbentMass,
      String solventA,
      String solventB,
      GradientAxis axis,
      List<GradientPoint> gradient,
      List<Fraction> fractions,
      List<Pool> pools,
      List<Loss> losses,
      String source,
      String note,
      String correctsId,
      Observation observation)
      implements Revision {
    public Purification(
        String id,
        String stageId,
        String stageTitle,
        Instant recordedAt,
        String title,
        PurificationKind kind,
        String inputSampleId,
        LaboratoryQuantity inputPlanned,
        LaboratoryQuantity inputActual,
        String stationaryPhase,
        LaboratoryQuantity sorbentMass,
        String solventA,
        String solventB,
        GradientAxis axis,
        List<GradientPoint> gradient,
        List<Fraction> fractions,
        List<Pool> pools,
        List<Loss> losses,
        String source,
        String note,
        String correctsId) {
      this(
          id,
          stageId,
          stageTitle,
          recordedAt,
          title,
          kind,
          inputSampleId,
          inputPlanned,
          inputActual,
          stationaryPhase,
          sorbentMass,
          solventA,
          solventB,
          axis,
          gradient,
          fractions,
          pools,
          losses,
          source,
          note,
          correctsId,
          Observation.EMPTY);
    }

    public Purification {
      observation = Objects.requireNonNull(observation, "Происхождение наблюдения");
      id = identifier(id);
      stageId = optional(stageId);
      stageTitle = text(stageTitle, 500);
      Objects.requireNonNull(recordedAt);
      title = required(title, 500);
      Objects.requireNonNull(kind);
      inputSampleId = optional(inputSampleId);
      quantity(inputPlanned);
      quantity(inputActual);
      stationaryPhase = text(stationaryPhase, 1000);
      quantity(sorbentMass);
      if (sorbentMass != null && !sorbentMass.unit().dimension().equals("mass")) {
        throw new IllegalArgumentException("Количество сорбента задаётся как масса.");
      }
      solventA = text(solventA, 1000);
      solventB = text(solventB, 1000);
      Objects.requireNonNull(axis);
      gradient = List.copyOf(gradient);
      fractions = List.copyOf(fractions);
      pools = List.copyOf(pools);
      losses = List.copyOf(losses);
      limit(gradient.size(), 1000, "точек градиента");
      limit(fractions.size(), 2000, "фракций");
      limit(pools.size(), 1000, "объединений");
      limit(losses.size(), 2000, "потерь");
      BigDecimal previous = null;
      for (final var point : gradient) {
        if (previous != null && point.position.compareTo(previous) <= 0) {
          throw new IllegalArgumentException("Положения точек градиента должны строго возрастать.");
        }
        previous = point.position;
      }
      if (!gradient.isEmpty() && (solventA.isBlank() || solventB.isBlank())) {
        throw new IllegalArgumentException("Для градиента назовите растворители A и B.");
      }
      final var known = unique(fractions, Fraction::id);
      unique(pools, Pool::id);
      unique(losses, Loss::id);
      final var used = new HashSet<String>();
      for (final var pool : pools) {
        for (final var fraction : pool.fractionIds) {
          if (!known.contains(fraction) || !used.add(fraction)) {
            throw new IllegalArgumentException(
                "Фракция отсутствует или уже включена в другое объединение. Двойной учёт запрещён.");
          }
        }
      }
      source = text(source, 2000);
      note = text(note, 20000);
      correctsId = optional(correctsId);
      fact(inputActual, source);
    }
  }

  public LaboratoryReactionOperations {
    timePoints = List.copyOf(timePoints);
    workups = List.copyOf(workups);
    purifications = List.copyOf(purifications);
    stageOutcomes = List.copyOf(stageOutcomes);
    limit(timePoints.size(), 10000, "проб с историей");
    limit(workups.size(), 1000, "версий обработки");
    limit(purifications.size(), 1000, "версий очистки");
    history(timePoints);
    history(workups);
    history(purifications);
    limit(stageOutcomes.size(), 10000, "версий исходов стадий");
    outcomeHistory(stageOutcomes);
    final List<Revision> revisions = new ArrayList<>();
    revisions.addAll(timePoints);
    revisions.addAll(workups);
    revisions.addAll(purifications);
    revisions.addAll(stageOutcomes);
    unique(revisions, Revision::id);
    long children = 0, characters = 0;
    for (final var point : timePoints) {
      characters +=
          point.stageTitle.length()
              + point.label.length()
              + point.source.length()
              + point.note.length();
    }
    for (final var workup : workups) {
      children += workup.phases.size() + workup.steps.size() + workup.losses.size();
      characters += workup.stageTitle.length() + workup.title.length() + workup.note.length();
      for (final var phase : workup.phases) {
        characters += phase.label.length();
      }
      for (final var step : workup.steps) {
        characters += step.material.length() + step.source.length() + step.note.length();
      }
      for (final var loss : workup.losses) {
        characters += loss.label.length() + loss.source.length();
      }
    }
    for (final var run : purifications) {
      children += run.gradient.size() + run.fractions.size() + run.pools.size() + run.losses.size();
      characters +=
          run.stageTitle.length()
              + run.title.length()
              + run.note.length()
              + run.source.length()
              + run.stationaryPhase.length()
              + run.solventA.length()
              + run.solventB.length();
      for (final var f : run.fractions) {
        characters += f.label.length() + f.note.length() + f.source.length();
      }
      for (final var p : run.pools) {
        characters += p.label.length() + p.note.length() + p.source.length();
      }
      for (final var loss : run.losses) {
        characters += loss.label.length() + loss.source.length();
      }
    }
    for (final var outcome : stageOutcomes) {
      children += outcome.targets().size() + outcome.byproducts().size();
      characters +=
          outcome.stageTitle().length() + outcome.source().length() + outcome.reason().length();
    }
    long references = 0;
    for (final var observation : observations(timePoints, workups, purifications, stageOutcomes)) {
      references += observation.analyses().size();
      if (references > 100000) {
        throw new IllegalArgumentException(
            "В одном опыте поддерживается до 100000 аналитических ссылок операций.");
      }
      for (final var reference : observation.analyses()) {
        characters += reference.id().length();
      }
    }
    if (children > 100000 || characters > 10000000) {
      throw new IllegalArgumentException(
          "Записи обработки и очистки превышают 100000 строк или 10 миллионов символов.");
    }
  }

  public boolean hasObservationMetadata() {
    return observations(this.timePoints, this.workups, this.purifications, this.stageOutcomes)
        .stream()
        .anyMatch(o -> !o.equals(Observation.EMPTY));
  }

  private static List<Observation> observations(
      final List<TimePoint> points,
      final List<Workup> workups,
      final List<Purification> runs,
      final List<StageOutcome> outcomes) {
    final List<Observation> values = new ArrayList<>();
    points.forEach(p -> values.add(p.observation()));
    workups.forEach(
        w -> {
          w.steps().forEach(s -> values.add(s.observation()));
          w.losses().forEach(l -> values.add(l.observation()));
        });
    runs.forEach(
        r -> {
          values.add(r.observation());
          r.fractions().forEach(f -> values.add(f.observation()));
          r.pools().forEach(p -> values.add(p.observation()));
          r.losses().forEach(l -> values.add(l.observation()));
        });
    outcomes.forEach(o -> values.add(o.observation()));
    return values;
  }

  /** Saved links resolve exact immutable versions; latest-only is a selection policy, not a history invariant. */
  public void validateLinks(
      final List<LaboratorySample> samples,
      final List<LaboratoryAnalysis> analyses,
      final LaboratoryAnalyticalEvidence evidence) {
    final Map<String, LaboratoryAnalysis> legacy = new HashMap<>();
    analyses.forEach(a -> legacy.put(a.id(), a));
    final Map<String, LaboratoryAnalyticalEvidence.Entry> advanced = new HashMap<>();
    evidence.entries().forEach(a -> advanced.put(a.id(), a));
    final Set<String> sampleIds = new HashSet<>();
    samples.forEach(s -> sampleIds.add(s.id()));
    for (final var p : this.timePoints) {
      links(p.observation(), p.sampleId(), true, sampleIds, legacy, advanced);
    }
    for (final var outcome : this.stageOutcomes) {
      links(outcome.observation(), null, false, sampleIds, legacy, advanced);
    }
    for (final var w : this.workups) {
      w.steps().forEach(s -> links(s.observation(), null, false, sampleIds, legacy, advanced));
      w.losses().forEach(l -> links(l.observation(), null, false, sampleIds, legacy, advanced));
    }
    for (final var r : this.purifications) {
      links(r.observation(), r.inputSampleId(), true, sampleIds, legacy, advanced);
      r.fractions()
          .forEach(f -> links(f.observation(), f.sampleId(), true, sampleIds, legacy, advanced));
      r.pools()
          .forEach(p -> links(p.observation(), p.sampleId(), true, sampleIds, legacy, advanced));
      r.losses().forEach(l -> links(l.observation(), null, false, sampleIds, legacy, advanced));
    }
  }

  private static void links(
      final Observation observation,
      final String sampleId,
      final boolean sampleBound,
      final Set<String> samples,
      final Map<String, LaboratoryAnalysis> legacy,
      final Map<String, LaboratoryAnalyticalEvidence.Entry> evidence) {
    for (final var ref : observation.analyses()) {
      final String targetSample;
      if (ref.kind() == AnalysisKind.LEGACY) {
        final var target = legacy.get(ref.id());
        if (target == null) {
          throw new IllegalArgumentException(
              "Аналитическая ссылка операции отсутствует: " + ref.id());
        }
        targetSample = target.sampleId();
      } else {
        final var target = evidence.get(ref.id());
        if (target == null || target.data() instanceof LaboratoryAnalyticalEvidence.Dossier) {
          throw new IllegalArgumentException(
              "Ссылка операции должна указывать на существующий анализ, не на досье: " + ref.id());
        }
        targetSample = target.sampleId();
      }
      if (targetSample != null && !samples.contains(targetSample)
          || sampleBound && !Objects.equals(sampleId, targetSample)) {
        throw new IllegalArgumentException(
            "Аналитическая ссылка операции относится к другому или отсутствующему физическому образцу.");
      }
    }
  }

  public List<TimePoint> activeTimePoints() {
    return active(this.timePoints);
  }

  public List<Workup> activeWorkups() {
    return active(this.workups);
  }

  public List<Purification> activePurifications() {
    return active(this.purifications);
  }

  public List<StageOutcome> activeStageOutcomes() {
    return active(this.stageOutcomes);
  }

  public Optional<StageOutcome> outcomeFor(final String stageId) {
    return this.activeStageOutcomes().stream().filter(o -> o.stageId().equals(stageId)).findFirst();
  }

  public static void requireAppend(
      final LaboratoryReactionOperations previous, final LaboratoryReactionOperations next) {
    contains(previous.timePoints, next.timePoints);
    contains(previous.workups, next.workups);
    contains(previous.purifications, next.purifications);
    contains(previous.stageOutcomes, next.stageOutcomes);
  }

  public static LaboratoryReactionOperations merge(
      final LaboratoryReactionOperations previous, final LaboratoryReactionOperations current) {
    return new LaboratoryReactionOperations(
        mergeRecords(previous.timePoints, current.timePoints),
        mergeRecords(previous.workups, current.workups),
        mergeRecords(previous.purifications, current.purifications),
        mergeRecords(previous.stageOutcomes, current.stageOutcomes));
  }

  private static <T extends Revision> List<T> mergeRecords(
      final List<T> before, final List<T> current) {
    final var merged = new LinkedHashMap<String, T>();
    for (final var value : before) {
      merged.put(value.id(), value);
    }
    for (final var value : current) {
      final var old = merged.putIfAbsent(value.id(), value);
      if (old != null && !old.equals(value)) {
        throw new IllegalArgumentException(
            "Историческая запись обработки изменена. Создайте исправление.");
      }
    }
    return merged.values().stream().sorted(Comparator.comparing(Revision::recordedAt)).toList();
  }

  private static <T extends Revision> void contains(final List<T> before, final List<T> after) {
    final var records = new HashMap<String, T>();
    after.forEach(v -> records.put(v.id(), v));
    for (final var value : before) {
      if (!value.equals(records.get(value.id()))) {
        throw new IllegalArgumentException(
            "Записанные наблюдения и версии нельзя удалить или переписать. Добавьте исправление.");
      }
    }
  }

  private static <T extends Revision> void history(final List<T> records) {
    final var known = new HashMap<String, T>();
    final var corrected = new HashSet<String>();
    for (final var value : records) {
      if (known.containsKey(value.id())) {
        throw new IllegalArgumentException("Повтор идентификатора записи обработки.");
      }
      if (value.correctsId() != null) {
        final var original = known.get(value.correctsId());
        if (original == null
            || !Objects.equals(original.stageId(), value.stageId())
            || value.recordedAt().isBefore(original.recordedAt())
            || !corrected.add(original.id())) {
          throw new IllegalArgumentException(
              "Исправление должно продолжать последнюю версию записи той же стадии.");
        }
      }
      known.put(value.id(), value);
    }
  }

  private static void outcomeHistory(final List<StageOutcome> records) {
    history(records);
    final Map<String, StageOutcome> latest = new HashMap<>();
    for (final var outcome : records) {
      final var previous = latest.put(outcome.stageId(), outcome);
      if (previous == null && outcome.correctsId() != null
          || previous != null && !previous.id().equals(outcome.correctsId())) {
        throw new IllegalArgumentException(
            "Для стадии существует одна цепочка исходов. Исправьте последнюю версию заключения.");
      }
    }
  }

  private static <T extends Revision> List<T> active(final List<T> records) {
    final var corrected = new HashSet<String>();
    records.forEach(
        r -> {
          if (r.correctsId() != null) {
            corrected.add(r.correctsId());
          }
        });
    return records.stream().filter(r -> !corrected.contains(r.id())).toList();
  }

  private static <T> Set<String> unique(final List<T> records, final Function<T, String> key) {
    final Set<String> ids = new HashSet<>();
    for (final var value : records) {
      if (!ids.add(key.apply(value))) {
        throw new IllegalArgumentException("Повтор идентификатора строки обработки.");
      }
    }
    return ids;
  }

  private static String identifier(final String value) {
    final var id = IdentifierValue.requireIdentifier(value, "Идентификатор записи обработки");
    if (id.length() > 200) {
      throw new IllegalArgumentException("Слишком длинный идентификатор.");
    }
    return id;
  }

  private static String optional(final String value) {
    return value == null ? null : identifier(value);
  }

  private static String text(final String value, final int max) {
    Objects.requireNonNull(value);
    if (value.length() > max) {
      throw new IllegalArgumentException("Слишком длинное поле обработки.");
    }
    return value.strip();
  }

  private static String required(final String value, final int max) {
    final var result = text(value, max);
    if (result.isBlank()) {
      throw new IllegalArgumentException("Заполните название записи.");
    }
    return result;
  }

  private static BigDecimal number(final BigDecimal value, final boolean required) {
    if (value == null) {
      if (required) {
        throw new IllegalArgumentException("Укажите числовое значение.");
      }
      return null;
    }
    if (value.signum() < 0 || value.precision() > 40 || Math.abs((long) value.scale()) > 24) {
      throw new IllegalArgumentException(
          "Число должно быть неотрицательным и содержать до 40 значащих цифр.");
    }
    final var normalized = value.stripTrailingZeros();
    if (Math.abs((long) normalized.scale()) > 24) {
      throw new IllegalArgumentException(
          "Число выходит за поддерживаемый диапазон показателя степени.");
    }
    return normalized;
  }

  private static void quantity(final LaboratoryQuantity value) {
    if (value != null) {
      number(value.value(), true);
    }
  }

  private static void fact(final LaboratoryQuantity actual, final String source) {
    if (actual != null && source.isBlank()) {
      throw new IllegalArgumentException(
          "Для указанного количества укажите источник или способ получения значения.");
    }
  }

  private static void limit(final int count, final int maximum, final String label) {
    if (count > maximum) {
      throw new IllegalArgumentException("Поддерживается до " + maximum + " " + label + ".");
    }
  }
}
