/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryExperimentReport.*;

/** Read-only projection of stored records. Exact references never follow a later correction. */
final class LaboratoryExperimentReportDetails {
  private final LaboratoryExperiment experiment;
  private final Options options;
  private final List<Block> blocks;

  LaboratoryExperimentReportDetails(
      final LaboratoryExperiment experiment, final Options options, final List<Block> blocks) {
    this.experiment = experiment;
    this.options = options;
    this.blocks = blocks;
  }

  private static void interrupted() {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException("Подготовка отчёта отменена.");
    }
  }

  private void heading(final String text, final int level) {
    interrupted();
    this.blocks.add(new Heading(text, level));
  }

  private void text(final String text) {
    interrupted();
    if (!text.isBlank()) {
      this.blocks.add(new Paragraph(text, false));
    }
  }

  private void audit(final String text) {
    if (this.options.diagnostics()) {
      this.context(text);
    }
  }

  private void context(final String text) {
    interrupted();
    this.blocks.add(new Paragraph(text, true));
  }

  private void table(final List<String> columns, final List<List<String>> rows) {
    interrupted();
    if (!rows.isEmpty()) {
      this.blocks.add(new Table(columns, rows));
    }
  }

  private static String n(final BigDecimal value) {
    return LaboratoryExperimentReport.number(value);
  }

  private static String q(final LaboratoryQuantity value) {
    return value == null ? "не указано" : n(value.value()) + " " + value.unit();
  }

  private static String present(final String value) {
    return value == null || value.isBlank() ? "не указан" : value;
  }

  private String sample(final String id) {
    if (id == null) {
      return "без отдельного физического образца";
    }
    return this.experiment.samples().stream()
        .filter(s -> s.id().equals(id))
        .findFirst()
        .map(s -> s.code() + (this.options.diagnostics() ? " [" + id + "]" : ""))
        .orElse("недоступный образец");
  }

  private String stage(final String id) {
    if (id == null) {
      return "общая запись опыта";
    }
    return this.experiment.stages().stream()
        .filter(s -> s.id().equals(id))
        .findFirst()
        .map(s -> s.title() + " [" + id + "]")
        .orElse("стадия отсутствует в текущем плане [" + id + "]");
  }

  void advanced() {
    final var all = this.experiment.analyticalEvidence().entries();
    if (all.isEmpty()) {
      return;
    }
    this.heading("Дополнительная аналитика и подтверждение продукта", 1);
    this.audit(
        this.options.superseded()
            ? "Включены исходные и исправленные версии дополнительных анализов."
            : "Показаны последние версии дополнительных анализов. Точные исторические ссылки досье и мультиплетов сохранены отдельно, без замены последней версией.");
    this.audit(
        "Численные результаты показаны теми же методами, что в карточке анализа, по сохранённым входам. Новых измерений, интегрирования или автоматического заключения этот отчёт не создаёт. Полные трассы и матрицы остаются в QCHEM.");
    final Set<String> old = new HashSet<>();
    all.forEach(
        e -> {
          if (e.correctsId() != null) {
            old.add(e.correctsId());
          }
        });
    for (final var entry : all) {
      if (!this.options.superseded() && old.contains(entry.id())) {
        continue;
      }
      this.heading(entry.title() + " · " + entry.data().method(), 2);
      if (!this.options.diagnostics()) {
        this.context("Происхождение: " + entry.origin());
      }
      this.audit(
          "ID: EVIDENCE:"
              + entry.id()
              + " · записано: "
              + entry.recordedAt()
              + "\nПроисхождение: "
              + entry.origin()
              + "\nКомпонент: "
              + entry.componentId()
              + " · образец: "
              + this.sample(entry.sampleId())
              + "\nСнимок структуры: "
              + entry.molecule().displayName()
              + " · "
              + entry.molecule().formula().hillNotation()
              + " ["
              + entry.molecule().id().value()
              + "]"
              + (entry.correctsId() == null ? "" : "\nИсправляет: EVIDENCE:" + entry.correctsId())
              + (old.contains(entry.id())
                  ? "\nИсторическая версия: есть последующее исправление."
                  : "\nПоследняя версия этой записи."));
      this.context("Источник: " + present(entry.source()));
      try {
        this.text(
            entry.data() instanceof Dossier
                ? LaboratoryEvidenceService.dossierSummary(this.experiment, entry)
                : LaboratoryEvidenceService.description(entry));
      } catch (final IllegalArgumentException error) {
        this.text(
            "Расчётное описание недоступно: "
                + error.getMessage()
                + ". Сохранённые входы приведены ниже; результат не подменён нулём.");
        this.text(entry.note());
      }
      switch (entry.data()) {
        case Correlations c ->
            this.table(
                List.of("ID корреляции", "δ X / Y, ppm", "Назначения X / Y"),
                c.correlations().stream()
                    .map(
                        p ->
                            List.of(
                                p.id(),
                                n(p.xPpm()) + " / " + n(p.yPpm()),
                                LaboratoryEvidenceService.assignments(p.xAtoms())
                                    + " / "
                                    + LaboratoryEvidenceService.assignments(p.yAtoms())))
                    .toList());
        case Multiplets m -> {
          this.table(
              List.of("ID мультиплета", "Метка", "δ, ppm"),
              m.multiplets().stream()
                  .map(p -> List.of(p.id(), p.label(), n(p.centerPpm())))
                  .toList());
          if (m.analysisId() != null) {
            this.reference(new AnalysisRef(AnalysisKind.LEGACY, m.analysisId()));
          }
        }
        case QuantitativeNmr q -> {
          this.table(
              List.of("Сохранённый вход qNMR", "Образец", "Стандарт: " + q.standardName()),
              List.of(
                  List.of("Навеска", q(q.sampleMass()), q(q.standardMass())),
                  List.of(
                      "Молярная масса, г/моль", n(q.sampleMolarMass()), n(q.standardMolarMass())),
                  List.of("Интеграл", n(q.sampleIntegral()), n(q.standardIntegral())),
                  List.of(
                      "Число ядер",
                      Integer.toString(q.sampleNuclei()),
                      Integer.toString(q.standardNuclei())),
                  List.of("Чистота стандарта, %", "не применяется", n(q.standardPurityPercent()))));
          this.context("Подготовка порции: " + q.preparation());
        }
        case Chromatogram c -> {
          this.context(
              "Метод: "
                  + c.technique()
                  + " · детектор: "
                  + c.detector()
                  + "\nСохранённая трасса: "
                  + c.trace().size()
                  + " точек"
                  + (c.trace().isEmpty()
                      ? "; введён список пиков, кривая не создаётся."
                      : " · время "
                          + n(c.trace().getFirst().timeMin())
                          + "–"
                          + n(c.trace().getLast().timeMin())
                          + " мин.")
                  + "\nТочный количественный пик: "
                  + present(c.quantifiedPeakId())
                  + "\nМасса представленной порции: "
                  + q(c.representedSampleMass())
                  + "\nОтдельно сообщённая массовая доля: "
                  + (c.reportedPurityPercent() == null
                      ? "не указана"
                      : n(c.reportedPurityPercent())
                          + " % · источник: "
                          + present(c.reportedPuritySource()))
                  + "\nПодготовка порции: "
                  + present(c.preparation()));
          this.table(
              List.of("ID / пик", "Границы, мин", "tR, мин / площадь", "Участие в сумме"),
              c.peaks().stream()
                  .map(
                      p ->
                          List.of(
                              p.id() + "\n" + p.label(),
                              n(p.fromMin()) + "–" + n(p.toMin()),
                              n(p.retentionMin()) + " / " + n(p.area()),
                              p.included() ? "включён" : "исключён: " + p.exclusionReason()))
                  .toList());
          if (c.calibration() != null) {
            this.table(
                List.of("Масса стандарта", "Площадь"),
                c.calibration().points().stream()
                    .map(p -> List.of(q(p.mass()), n(p.area())))
                    .toList());
            this.context("Источник калибровки: " + c.calibration().source());
          }
        }
        case Dossier d -> {
          this.audit(
              "Ссылки заключения разрешены по сохранённым ID; исторические версии не заменены текущими. Заключение пользователя: "
                  + d.conclusion());
          this.text(
              "Сохранённое основание заключения: "
                  + present(d.conclusionText())
                  + "\nЗаданный допуск МС: "
                  + n(d.massTolerancePpm())
                  + " ppm");
          this.table(
              List.of("Замечание / расхождение", "Текст", "Состояние"),
              d.findings().stream()
                  .map(
                      f ->
                          List.of(
                              LaboratoryEvidenceService.severityLabel(f.severity())
                                  + " · "
                                  + f.category(),
                              f.text(),
                              f.resolved() ? "разрешено пользователем" : "открыто"))
                  .toList());
          d.analysisIds().forEach(id -> this.reference(new AnalysisRef(AnalysisKind.LEGACY, id)));
          d.evidenceIds().forEach(id -> this.reference(new AnalysisRef(AnalysisKind.EVIDENCE, id)));
        }
      }
      if (this.options.attachments()) {
        this.context(
            "Связанные вложения: "
                + (entry.attachmentIds().isEmpty()
                    ? "нет"
                    : String.join(", ", entry.attachmentIds())));
      }
    }
  }

  private void reference(final AnalysisRef reference) {
    try {
      final var a = LaboratoryReactionOperationsService.resolveAnalysis(this.experiment, reference);
      if (!this.options.diagnostics()) {
        this.context(
            a.title()
                + " · "
                + a.method()
                + " · "
                + a.origin()
                + (a.source().isBlank() ? "" : "\nИсточник: " + a.source())
                + (a.historical() ? " · прежняя версия анализа" : ""));
        return;
      }
      this.context(
          "Точная ссылка "
              + reference.kind()
              + ":"
              + reference.id()
              + " · "
              + a.title()
              + " · "
              + a.method()
              + "\nДата: "
              + a.recordedAt()
              + " · происхождение: "
              + a.origin()
              + "\nОбразец: "
              + this.sample(a.sampleId())
              + "\nИсточник: "
              + present(a.source())
              + (a.historical()
                  ? "\nИсторическая версия; последующее исправление не подставлено."
                  : ""));
    } catch (final IllegalArgumentException error) {
      this.context(
          "Точная ссылка "
              + reference.kind()
              + ":"
              + reference.id()
              + " недоступна: "
              + error.getMessage()
              + ". Другая версия не подставлена.");
    }
  }

  private void observation(final Observation observation) {
    this.context(
        "Происхождение записанных значений: "
            + (observation.origin() == Origin.UNSPECIFIED
                ? "не указано; измерение не подтверждено"
                : observation.origin())
            + (observation.origin() == Origin.LITERATURE || observation.origin() == Origin.SIMULATED
                ? "; не собственное измерение данного образца"
                : ""));
    observation.analyses().forEach(this::reference);
  }

  void procedure() {
    if (!this.options.diagnostics()) {
      this.conciseProcedure();
      return;
    }
    final var procedure = this.experiment.procedure();
    if (procedure.steps().isEmpty() && procedure.events().isEmpty()) {
      return;
    }
    this.heading("Пошаговая методика и записи выполнения", 1);
    this.context(
        "Инструкции и плановые количества не являются выполнением. Записи выполнения сохраняют собственные снимки; их количество не заменяется новым масштабом. Происхождение событий этой модели отдельно не типизировано: оно приводится в исходном тексте, а не выводится из статуса действия.");
    final Map<String, LaboratoryProcedure.Step> steps = new LinkedHashMap<>();
    procedure.steps().forEach(s -> steps.put(s.id(), s));
    procedure.events().forEach(e -> steps.putIfAbsent(e.stepId(), e.snapshot()));
    final Set<String> current = new HashSet<>();
    procedure.steps().forEach(s -> current.add(s.id()));
    final Set<String> corrected = new HashSet<>();
    procedure
        .events()
        .forEach(
            e -> {
              if (e.correctsId() != null) {
                corrected.add(e.correctsId());
              }
            });
    for (final var step : steps.values()) {
      this.heading(step.title() + " · " + step.kind() + " · " + procedure.status(step.id()), 2);
      this.context(
          "Действие: "
              + step.id()
              + " · "
              + this.stage(step.stageId())
              + (current.contains(step.id())
                  ? "\nТекущий план / сохранённая инструкция действия."
                  : "\nИсторический снимок; действие отсутствует в текущем плане."));
      this.text(step.instruction());
      this.context(
          "Компонент: "
              + present(step.componentId())
              + " · доля плановой загрузки: "
              + n(step.portion())
              + "\nЗаданная длительность: "
              + LaboratoryProcedureService.duration(step.durationSeconds())
              + " · температура: "
              + (step.temperatureC() == null ? "не указана" : n(step.temperatureC()) + " °C"));
      if (current.contains(step.id())) {
        try {
          final var resolved = LaboratoryProcedureService.resolve(this.experiment, step);
          this.context(
              "Плановое количество для действия: "
                  + q(resolved.plannedQuantity())
                  + " · "
                  + resolved.componentName());
          resolved.warnings().forEach(this::context);
        } catch (final IllegalArgumentException error) {
          this.context("Плановое количество недоступно: " + error.getMessage());
        }
      }
      for (final var event : procedure.eventsFor(step.id())) {
        if (!this.options.journal() && corrected.contains(event.id())) {
          continue;
        }
        this.text(
            "Запись выполнения: "
                + event.action()
                + " · "
                + event.recordedAt()
                + "\n"
                + event.text());
        this.context(
            "ID: "
                + event.id()
                + (event.correctsId() == null ? "" : " · исправляет: " + event.correctsId())
                + (corrected.contains(event.id()) ? " · есть последующее исправление" : "")
                + "\nСохранённый компонент: "
                + present(event.componentName())
                + "\nПлан при записи: "
                + q(event.plannedQuantity())
                + "\nЗаписанное количество (происхождение не типизировано): "
                + q(event.actualQuantity())
                + "\nИсточник сведений: сохранённое событие автора, текст приведён выше.");
      }
    }
  }

  private void conciseProcedure() {
    final var procedure = this.experiment.procedure();
    if (procedure.steps().isEmpty() && procedure.events().isEmpty()) {
      return;
    }
    this.heading("Методика", 1);
    final Map<String, LaboratoryProcedure.Step> steps = new LinkedHashMap<>();
    procedure.steps().forEach(s -> steps.put(s.id(), s));
    procedure.events().forEach(v -> steps.putIfAbsent(v.stepId(), v.snapshot()));
    final Set<String> corrected = new HashSet<>();
    procedure
        .events()
        .forEach(
            v -> {
              if (v.correctsId() != null) {
                corrected.add(v.correctsId());
              }
            });
    int index = 0;
    for (final var planned : steps.values()) {
      final var events =
          procedure.eventsFor(planned.id()).stream()
              .filter(v -> !corrected.contains(v.id()))
              .toList();
      final var recorded =
          events.reversed().stream()
              .filter(
                  v ->
                      v.action() == LaboratoryProcedure.Action.COMPLETE
                          || v.action() == LaboratoryProcedure.Action.RECORD_COMPLETE
                          || v.action() == LaboratoryProcedure.Action.START)
              .findFirst()
              .orElse(null);
      final var step = recorded == null ? planned : recorded.snapshot();
      final String state =
          switch (procedure.status(planned.id())) {
            case PLANNED -> " · план";
            case RUNNING -> " · выполняется";
            case COMPLETED -> "";
            case SKIPPED -> " · пропущено";
          };
      this.heading(++index + ". " + step.title() + state, 2);
      if (this.experiment.stages().size() > 1) {
        this.context(
            this.experiment.stages().stream()
                .filter(v -> v.id().equals(step.stageId()))
                .map(LaboratoryReactionStage::title)
                .findFirst()
                .orElse("Общая операция"));
      }
      this.text(step.instruction());
      if (step.instruction().isBlank() && recorded != null && recorded.actualQuantity() != null) {
        this.text("Количество: " + LaboratoryReportQuantities.quantity(recorded.actualQuantity()));
      }
      if (step.instruction().isBlank()
          && (step.temperatureC() != null || step.durationSeconds() > 0)) {
        this.text(
            (step.temperatureC() == null ? "" : n(step.temperatureC()) + " °C")
                + (step.temperatureC() == null || step.durationSeconds() == 0 ? "" : "; ")
                + (step.durationSeconds() == 0
                    ? ""
                    : LaboratoryProcedureService.duration(step.durationSeconds())));
      }
      for (final var event : events) {
        if (!event.text().isBlank()
            && !event
                .text()
                .equals("Выполнено ранее. Фактическое время начала и окончания не указано.")) {
          this.text("Наблюдение: " + event.text());
        }
      }
    }
  }

  private <T extends Revision> void revisions(final List<T> records, final Consumer<T> append) {
    final Set<String> old = new HashSet<>();
    records.forEach(
        r -> {
          if (r.correctsId() != null) {
            old.add(r.correctsId());
          }
        });
    for (final T record : records) {
      if (!this.options.journal() && old.contains(record.id())) {
        continue;
      }
      this.audit(
          "ID записи: "
              + record.id()
              + " · "
              + record.recordedAt()
              + "\nСтадия в записи: "
              + (record.stageId() == null
                  ? "общая запись опыта"
                  : record.stageTitle() + " [" + record.stageId() + "]")
              + (record.correctsId() == null ? "" : "\nИсправляет: " + record.correctsId())
              + (old.contains(record.id())
                  ? "\nИсторическая версия; есть последующее исправление."
                  : "\nПоследняя версия этой записи."));
      if (!this.options.diagnostics()
          && this.experiment.stages().size() > 1
          && !record.stageTitle().isBlank()) {
        this.context(record.stageTitle());
      }
      append.accept(record);
    }
  }

  void operations() {
    if (!this.options.diagnostics()) {
      this.conciseOperations();
      return;
    }
    final var operations = this.experiment.operations();
    if (operations.timePoints().isEmpty()
        && operations.workups().isEmpty()
        && operations.purifications().isEmpty()) {
      return;
    }
    this.heading("Контроль, обработка и очистка", 1);
    this.audit(
        "План и записанные значения показаны отдельно. Наличие карточки обработки или очистки не доказывает выполнения; неизвестное количество не равно нулю. Ссылки на анализы сами по себе не рассчитывают конверсию, чистоту или выход. "
            + (this.options.journal()
                ? "Включена история исправлений."
                : "Показаны последние версии; полный журнал включает прежние версии."));
    this.revisions(
        operations.timePoints(),
        p -> {
          this.heading("Контрольная проба · " + p.label(), 2);
          this.text(
              "Время от начала: "
                  + n(p.elapsed())
                  + " "
                  + p.elapsedUnit()
                  + " · метод: "
                  + p.method()
                  + "\nОбразец: "
                  + this.sample(p.sampleId())
                  + "\nКонверсия: "
                  + (p.conversion() == null
                      ? "не указана"
                      : n(p.conversion()) + " " + p.conversionUnit())
                  + "\nИсточник: "
                  + present(p.source())
                  + "\n"
                  + p.note());
          this.observation(p.observation());
        });
    this.revisions(
        operations.workups(),
        w -> {
          this.heading("Запись обработки · " + w.title(), 2);
          this.text(w.note());
          this.table(
              List.of("ID фазы", "Название", "Вид"),
              w.phases().stream()
                  .map(p -> List.of(p.id(), p.label(), p.kind().toString()))
                  .toList());
          for (final var s : w.steps()) {
            this.text(
                s.kind()
                    + " · "
                    + s.material()
                    + "\nID операции: "
                    + s.id()
                    + " · из фазы: "
                    + present(s.fromPhaseId())
                    + " → в фазу: "
                    + present(s.toPhaseId())
                    + "\nПовторов: "
                    + s.repetitions()
                    + " · суммарно план: "
                    + q(s.planned())
                    + " · записано: "
                    + q(s.actual())
                    + "\nИсточник: "
                    + present(s.source())
                    + "\n"
                    + s.note());
            this.observation(s.observation());
          }
          this.losses(w.losses());
        });
    this.revisions(
        operations.purifications(),
        p -> {
          this.heading("Запись очистки · " + p.title() + " · " + p.kind(), 2);
          this.text(
              "Исходный образец: "
                  + this.sample(p.inputSampleId())
                  + "\nЗагрузка, план: "
                  + q(p.inputPlanned())
                  + " · записано: "
                  + q(p.inputActual())
                  + "\nИсточник загрузки: "
                  + present(p.source())
                  + "\nНеподвижная фаза: "
                  + present(p.stationaryPhase())
                  + " · сорбент: "
                  + q(p.sorbentMass())
                  + "\nЭлюент A: "
                  + present(p.solventA())
                  + " · B: "
                  + present(p.solventB())
                  + "\n"
                  + p.note());
          this.observation(p.observation());
          this.table(
              List.of("Градиент, " + p.axis(), "B, %", "Поток, мл/мин"),
              p.gradient().stream()
                  .map(
                      g -> List.of(n(g.position()), n(g.solventBPercent()), n(g.flowMlPerMinute())))
                  .toList());
          for (final var f : p.fractions()) {
            this.text(
                "Фракция: "
                    + f.label()
                    + " ["
                    + f.id()
                    + "] · интервал: "
                    + (f.start() == null
                        ? "не указан"
                        : n(f.start()) + "–" + n(f.end()) + " " + p.axis())
                    + "\nПлан: "
                    + q(f.planned())
                    + " · записано: "
                    + q(f.actual())
                    + " · чистота: "
                    + (f.purityPercent() == null ? "не указана" : n(f.purityPercent()) + " %")
                    + "\nОбразец: "
                    + this.sample(f.sampleId())
                    + "\nИсточник: "
                    + present(f.source())
                    + "\n"
                    + f.note());
            this.observation(f.observation());
          }
          for (final var pool : p.pools()) {
            this.text(
                "Объединение: "
                    + pool.label()
                    + " ["
                    + pool.id()
                    + "]\nТочные фракции: "
                    + String.join(", ", pool.fractionIds())
                    + "\nЗаписанное количество после объединения: "
                    + q(pool.actual())
                    + "\nОбразец: "
                    + this.sample(pool.sampleId())
                    + "\nИсточник: "
                    + present(pool.source())
                    + "\n"
                    + pool.note());
            this.observation(pool.observation());
          }
          this.losses(p.losses());
        });
  }

  private void losses(final List<Loss> losses) {
    for (final var loss : losses) {
      this.text(
          "Потеря / поток: "
              + loss.label()
              + " ["
              + loss.id()
              + "]\nПлан: "
              + q(loss.planned())
              + " · записано: "
              + q(loss.actual())
              + "\nИсточник: "
              + present(loss.source()));
      this.observation(loss.observation());
    }
  }

  private void quantities(final LaboratoryQuantity plan, final LaboratoryQuantity actual) {
    if (plan != null) {
      this.text("План: " + LaboratoryReportQuantities.quantity(plan));
    }
    if (actual != null) {
      this.text("Записано: " + LaboratoryReportQuantities.quantity(actual));
    }
  }

  private void origin(final Observation value, final String source) {
    if (value.origin() != Origin.MEASURED) {
      this.context("Происхождение: " + value.origin());
    }
    if (source != null && !source.isBlank()) {
      this.context("Источник: " + source);
    }
    value.analyses().forEach(this::reference);
  }

  private void conciseLosses(final List<Loss> values) {
    for (final var v : values) {
      this.text("Потери: " + v.label());
      this.quantities(v.planned(), v.actual());
      this.origin(v.observation(), v.source());
    }
  }

  private void conciseOperations() {
    final var o = this.experiment.operations();
    if (o.timePoints().isEmpty() && o.workups().isEmpty() && o.purifications().isEmpty()) {
      return;
    }
    this.heading("Контроль, обработка и очистка", 1);
    this.revisions(
        o.timePoints(),
        p -> {
          this.heading(p.label(), 2);
          this.text(
              "Время от начала: "
                  + n(p.elapsed())
                  + " "
                  + p.elapsedUnit()
                  + "; метод: "
                  + p.method());
          if (p.conversion() != null) {
            this.text("Конверсия: " + n(p.conversion()) + " " + p.conversionUnit());
          }
          if (p.sampleId() != null) {
            this.text("Образец: " + this.sample(p.sampleId()));
          }
          this.text(p.note());
          this.origin(p.observation(), p.source());
        });
    this.revisions(
        o.workups(),
        w -> {
          this.heading(w.title(), 2);
          this.text(w.note());
          for (final var s : w.steps()) {
            this.text(
                s.kind()
                    + ": "
                    + s.material()
                    + (s.repetitions() > 1 ? "; повторов: " + s.repetitions() : ""));
            if (s.fromPhaseId() != null) {
              this.text(
                  "Из фазы: "
                      + w.phases().stream()
                          .filter(v -> v.id().equals(s.fromPhaseId()))
                          .findFirst()
                          .orElseThrow()
                          .label());
            }
            if (s.toPhaseId() != null) {
              this.text(
                  "В фазу: "
                      + w.phases().stream()
                          .filter(v -> v.id().equals(s.toPhaseId()))
                          .findFirst()
                          .orElseThrow()
                          .label());
            }
            this.quantities(s.planned(), s.actual());
            this.text(s.note());
            this.origin(s.observation(), s.source());
          }
          this.conciseLosses(w.losses());
        });
    this.revisions(
        o.purifications(),
        p -> {
          this.heading(p.title() + " · " + p.kind(), 2);
          if (p.inputSampleId() != null) {
            this.text("Образец: " + this.sample(p.inputSampleId()));
          }
          this.quantities(p.inputPlanned(), p.inputActual());
          this.text(p.note());
          if (!p.stationaryPhase().isBlank()) {
            this.text("Неподвижная фаза: " + p.stationaryPhase());
          }
          if (p.sorbentMass() != null) {
            this.text("Сорбент: " + LaboratoryReportQuantities.quantity(p.sorbentMass()));
          }
          if (!p.solventA().isBlank()) {
            this.text("Элюент A: " + p.solventA());
          }
          if (!p.solventB().isBlank()) {
            this.text("Элюент B: " + p.solventB());
          }
          this.origin(p.observation(), p.source());
          this.table(
              List.of("Градиент, " + p.axis(), "B, %", "Поток, мл/мин"),
              p.gradient().stream()
                  .map(
                      g -> List.of(n(g.position()), n(g.solventBPercent()), n(g.flowMlPerMinute())))
                  .toList());
          for (final var f : p.fractions()) {
            this.text(
                "Фракция: "
                    + f.label()
                    + (f.start() == null
                        ? ""
                        : "; " + n(f.start()) + "-" + n(f.end()) + " " + p.axis()));
            this.quantities(f.planned(), f.actual());
            if (f.purityPercent() != null) {
              this.text("Чистота: " + n(f.purityPercent()) + " %");
            }
            this.text(f.note());
            this.origin(f.observation(), f.source());
          }
          for (final var pool : p.pools()) {
            this.text("Объединение: " + pool.label());
            this.text(
                "Фракции: "
                    + String.join(
                        ", ",
                        pool.fractionIds().stream()
                            .map(
                                id ->
                                    p.fractions().stream()
                                        .filter(f -> f.id().equals(id))
                                        .findFirst()
                                        .orElseThrow()
                                        .label())
                            .toList()));
            this.quantities(null, pool.actual());
            this.text(pool.note());
            this.origin(pool.observation(), pool.source());
          }
          this.conciseLosses(p.losses());
        });
  }

  void outcomes() {
    if (this.experiment.operations().stageOutcomes().isEmpty()) {
      return;
    }
    if (!this.options.diagnostics()) {
      this.heading("Заключение", 1);
      final Set<String> old = new HashSet<>();
      this.experiment
          .operations()
          .stageOutcomes()
          .forEach(
              o -> {
                if (o.correctsId() != null) {
                  old.add(o.correctsId());
                }
              });
      for (final var o : this.experiment.operations().stageOutcomes()) {
        if (!old.contains(o.id())) {
          this.text(o.stageTitle() + ": " + o.status() + ".");
          this.text(o.source());
          this.text(o.reason());
          if (o.observation().origin() != Origin.MEASURED) {
            this.context("Основание: " + o.observation().origin());
          }
        }
      }
      return;
    }
    this.heading("Указанные исходы стадий", 1);
    this.context(
        "Ручные заключения о результате. Из статуса не выводятся количество, чистота или конверсия; происхождение и основание приведены у каждой записи.");
    this.revisions(
        this.experiment.operations().stageOutcomes(),
        o -> {
          this.heading(o.stageTitle() + " · " + o.status(), 2);
          this.observation(o.observation());
          this.text("Источник: " + o.source() + "\nОснование / причина: " + present(o.reason()));
          this.products("Цель", o.targets());
          this.products("Побочный продукт", o.byproducts());
        });
  }

  private void products(final String role, final List<OutcomeProduct> products) {
    for (final var p : products) {
      this.text(
          role
              + ": "
              + p.snapshot().displayName()
              + " · "
              + p.snapshot().formula().hillNotation()
              + "\nКомпонент в записи: "
              + p.componentId()
              + " · сохранённый снимок структуры: "
              + p.snapshot().id().value());
    }
  }
}
