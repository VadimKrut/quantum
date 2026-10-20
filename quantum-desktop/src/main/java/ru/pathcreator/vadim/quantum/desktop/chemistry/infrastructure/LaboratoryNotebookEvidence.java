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
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Read-only evidence overview. Organizational progress is deliberately not inferred here. */
public final class LaboratoryNotebookEvidence {
  public static final int ACTIVITY_LIMIT = 12, SAMPLE_LIMIT = 12, ANALYSIS_LIMIT = 8;

  public enum ActivityKind {
    PROCEDURE,
    OBSERVATION,
    WORKUP,
    PURIFICATION,
    OUTCOME,
    ANALYSIS
  }

  public enum Action {
    EXPERIMENT,
    PROCEDURE,
    OPERATIONS,
    SAMPLES,
    ANALYSIS,
    EVIDENCE
  }

  public record ProcedureSummary(
      int planned,
      int running,
      int completed,
      int skipped,
      String nextStepId,
      String nextStepTitle) {}

  public record Activity(
      ActivityKind kind,
      String id,
      String stageId,
      String title,
      String detail,
      Instant at,
      String origin) {}

  public record SampleSummary(
      String id,
      String componentId,
      String code,
      String label,
      LaboratoryQuantity remaining,
      String location,
      LaboratorySample.Status status,
      Instant updatedAt,
      String note) {}

  public record AnalysisRecord(
      AnalysisKind kind,
      String id,
      String componentId,
      String sampleId,
      String title,
      String method,
      Instant at,
      LaboratoryAnalysis.Origin origin,
      String source,
      boolean unresolved) {}

  public record AnalysisSummary(
      int total,
      int latestCount,
      List<AnalysisRecord> recent,
      int unresolved,
      int pendingConclusions) {
    public AnalysisSummary {
      recent = List.copyOf(recent);
    }
  }

  public record OutcomeSummary(
      String id,
      String stageId,
      String stageTitle,
      OutcomeStatus status,
      Origin origin,
      String reason,
      Instant at) {}

  public record NextAction(
      Action action, String caption, String stageId, String sampleId, String recordId) {}

  public record Summary(
      String experimentId,
      String title,
      ProcedureSummary procedure,
      List<Activity> recentActivity,
      int sampleCount,
      List<SampleSummary> samples,
      AnalysisSummary analyses,
      List<OutcomeSummary> outcomes,
      NextAction nextAction,
      List<String> warnings,
      MoleculeDrawing preview) {
    public Summary {
      recentActivity = List.copyOf(recentActivity);
      samples = List.copyOf(samples);
      outcomes = List.copyOf(outcomes);
      warnings = List.copyOf(warnings);
    }
  }

  private static final Comparator<Activity> ACTIVITY_ORDER =
      Comparator.comparing(Activity::at).thenComparing(Activity::id);

  private static final class Recent {
    final PriorityQueue<Activity> items = new PriorityQueue<>(ACTIVITY_ORDER);
    final Map<ActivityKind, Activity> latestKind = new EnumMap<>(ActivityKind.class);

    List<Activity> values() {
      final var selected = new LinkedHashMap<String, Activity>();
      // Keep the latest workup and purification visible even after many later analytical records.
      this.latestKind.values().forEach(a -> selected.put(a.kind() + ":" + a.id(), a));
      this.items.stream()
          .sorted(ACTIVITY_ORDER.reversed())
          .forEach(
              a -> {
                if (selected.size() < ACTIVITY_LIMIT)
                  selected.putIfAbsent(a.kind() + ":" + a.id(), a);
              });
      return selected.values().stream().sorted(ACTIVITY_ORDER.reversed()).toList();
    }
  }

  private LaboratoryNotebookEvidence() {}

  public static Summary read(final Path path) throws IOException {
    return read(path, () -> false);
  }

  public static Summary read(final Path path, final BooleanSupplier cancelled) throws IOException {
    check(cancelled);
    // The repository bounds and strictly decodes the document; this path never calls write().
    final var document = new LaboratoryFileRepository().read(path).drawing();
    check(cancelled);
    return summarize(document, cancelled);
  }

  public static Summary summarize(final LaboratoryDrawing drawing) {
    return summarize(drawing, () -> false);
  }

  public static Summary summarize(
      final LaboratoryDrawing drawing, final BooleanSupplier cancelled) {
    Objects.requireNonNull(drawing);
    Objects.requireNonNull(cancelled);
    check(cancelled);
    final var e = drawing.experiment();
    final var warnings = new ArrayList<String>();
    final var recent = new Recent();
    final var states = new HashMap<String, LaboratoryProcedure.Status>();
    for (final var event : e.procedure().events()) {
      check(cancelled);
      switch (event.action()) {
        case START -> states.put(event.stepId(), LaboratoryProcedure.Status.RUNNING);
        case COMPLETE, RECORD_COMPLETE ->
            states.put(event.stepId(), LaboratoryProcedure.Status.COMPLETED);
        case SKIP -> states.put(event.stepId(), LaboratoryProcedure.Status.SKIPPED);
        default -> {}
      }
      offer(
          recent,
          event.recordedAt(),
          () ->
              new Activity(
                  ActivityKind.PROCEDURE,
                  event.id(),
                  event.snapshot().stageId(),
                  event.action() + " · " + event.snapshot().title(),
                  clip(event.text(), 1800),
                  event.recordedAt(),
                  "Запись выполнения; происхождение отдельно не задано"));
    }
    int planned = 0, running = 0, completed = 0, skipped = 0;
    LaboratoryProcedure.Step nextPlanned = null, nextRunning = null;
    final Set<String> stepIds = new HashSet<>();
    for (final var step : e.procedure().steps()) {
      check(cancelled);
      stepIds.add(step.id());
      switch (states.getOrDefault(step.id(), LaboratoryProcedure.Status.PLANNED)) {
        case PLANNED -> {
          planned++;
          if (nextPlanned == null) nextPlanned = step;
        }
        case RUNNING -> {
          running++;
          if (nextRunning == null) nextRunning = step;
        }
        case COMPLETED -> completed++;
        case SKIPPED -> skipped++;
      }
    }
    if (states.keySet().stream().anyMatch(id -> !stepIds.contains(id)))
      warnings.add(
          "История содержит шаги, отсутствующие в текущем плане; счётчики относятся к текущим шагам.");
    final var nextStep = nextRunning == null ? nextPlanned : nextRunning;
    final var procedure =
        new ProcedureSummary(
            planned,
            running,
            completed,
            skipped,
            nextStep == null ? null : nextStep.id(),
            nextStep == null ? "" : nextStep.title());
    final Set<String> correctedNotes = new HashSet<>();
    e.journal()
        .forEach(
            v -> {
              if (v.correctsId() != null) correctedNotes.add(v.correctsId());
            });
    for (final var note : e.journal()) {
      check(cancelled);
      if (correctedNotes.contains(note.id())) continue;
      offer(
          recent,
          note.recordedAt(),
          () ->
              new Activity(
                  ActivityKind.OBSERVATION,
                  note.id(),
                  null,
                  note.kind().toString(),
                  clip(note.text(), 1800),
                  note.recordedAt(),
                  "Запись журнала; происхождение отдельно не задано"));
    }
    for (final var point : e.operations().activeTimePoints()) {
      check(cancelled);
      offer(
          recent,
          point.recordedAt(),
          () ->
              new Activity(
                  ActivityKind.OBSERVATION,
                  point.id(),
                  point.stageId(),
                  point.label(),
                  clip(
                      point.elapsed()
                          + " "
                          + point.elapsedUnit()
                          + " · "
                          + point.method()
                          + "\nКонверсия: "
                          + (point.conversion() == null
                              ? "не указана"
                              : point.conversion() + " " + point.conversionUnit())
                          + "\n"
                          + point.note()
                          + "\nИсточник: "
                          + point.source(),
                      1800),
                  point.recordedAt(),
                  origin(point.observation().origin())));
    }
    for (final var workup : e.operations().activeWorkups()) {
      check(cancelled);
      offer(
          recent,
          workup.recordedAt(),
          () ->
              new Activity(
                  ActivityKind.WORKUP,
                  workup.id(),
                  workup.stageId(),
                  (workupRecorded(workup) ? "Записи обработки · " : "План обработки · ")
                      + workup.title(),
                  workupDetail(workup),
                  workup.recordedAt(),
                  workupOrigin(workup)));
    }
    for (final var purification : e.operations().activePurifications()) {
      check(cancelled);
      offer(
          recent,
          purification.recordedAt(),
          () ->
              new Activity(
                  ActivityKind.PURIFICATION,
                  purification.id(),
                  purification.stageId(),
                  (purificationRecorded(purification) ? "Записи очистки · " : "План очистки · ")
                      + purification.title(),
                  clip(
                      purification.kind()
                          + "\n"
                          + purification.note()
                          + "\nЗагрузка, план: "
                          + quantity(purification.inputPlanned())
                          + "; записано: "
                          + quantity(purification.inputActual())
                          + "\nФракций: "
                          + purification.fractions().size()
                          + "; объединений: "
                          + purification.pools().size()
                          + "\nИсточник: "
                          + purification.source(),
                      1800),
                  purification.recordedAt(),
                  origin(purification.observation().origin())));
    }
    final var outcomes = new ArrayList<OutcomeSummary>();
    for (final var outcome : e.operations().activeStageOutcomes()) {
      check(cancelled);
      outcomes.add(
          new OutcomeSummary(
              outcome.id(),
              outcome.stageId(),
              outcome.stageTitle(),
              outcome.status(),
              outcome.observation().origin(),
              clip(outcome.reason(), 1800),
              outcome.recordedAt()));
      offer(
          recent,
          outcome.recordedAt(),
          () ->
              new Activity(
                  ActivityKind.OUTCOME,
                  outcome.id(),
                  outcome.stageId(),
                  outcome.status() + " · " + outcome.stageTitle(),
                  clip(outcome.reason() + "\nИсточник: " + outcome.source(), 1800),
                  outcome.recordedAt(),
                  origin(outcome.observation().origin())));
    }
    final var samples = new ArrayList<SampleSummary>();
    for (final var sample : e.samples()) {
      check(cancelled);
      final var balance = sample.balance();
      final var last = sample.events().getLast();
      samples.add(
          new SampleSummary(
              sample.id(),
              sample.componentId(),
              sample.code(),
              balance.label(),
              balance.remaining(),
              balance.location(),
              balance.status(),
              last.time(),
              clip(last.note(), 1000)));
    }
    samples.sort(
        Comparator.comparing(SampleSummary::updatedAt).thenComparing(SampleSummary::id).reversed());
    final var analyses = new ArrayList<AnalysisRecord>();
    final Set<String> corrected = new HashSet<>();
    e.analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) corrected.add(a.correctsId());
            });
    for (final var a : e.analyses()) {
      check(cancelled);
      if (corrected.contains(a.id())) continue;
      analyses.add(
          new AnalysisRecord(
              AnalysisKind.LEGACY,
              a.id(),
              a.componentId(),
              a.sampleId(),
              a.title(),
              method(a),
              a.recordedAt(),
              a.origin(),
              clip(a.source(), 1000),
              false));
    }
    int unresolved = 0, pendingConclusions = 0;
    for (final var a : e.analyticalEvidence().latest()) {
      check(cancelled);
      boolean open = false;
      if (a.data() instanceof Dossier dossier) {
        final int count =
            (int)
                dossier.findings().stream()
                    .filter(f -> !f.resolved() && f.severity() != Severity.INFORMATION)
                    .count();
        unresolved += count;
        open = count > 0 || dossier.conclusion() == Conclusion.PENDING;
        if (dossier.conclusion() == Conclusion.PENDING) pendingConclusions++;
      }
      analyses.add(
          new AnalysisRecord(
              AnalysisKind.EVIDENCE,
              a.id(),
              a.componentId(),
              a.sampleId(),
              a.title(),
              a.data().method(),
              a.recordedAt(),
              a.origin(),
              clip(a.source(), 1000),
              open));
    }
    analyses.sort(
        Comparator.comparing(AnalysisRecord::at).thenComparing(AnalysisRecord::id).reversed());
    for (final var a : analyses)
      offer(
          recent,
          a.at(),
          () ->
              new Activity(
                  ActivityKind.ANALYSIS,
                  a.id(),
                  null,
                  a.method() + " · " + a.title(),
                  "Источник: "
                      + a.source()
                      + (a.unresolved()
                          ? "\nВ досье есть открытые вопросы или непринятое заключение."
                          : ""),
                  a.at(),
                  a.origin().toString()));
    final var analytical =
        new AnalysisSummary(
            e.analyses().size() + e.analyticalEvidence().entries().size(),
            analyses.size(),
            analyses.stream().limit(ANALYSIS_LIMIT).toList(),
            unresolved,
            pendingConclusions);
    if (unresolved > 0)
      warnings.add("Нерешённых замечаний в актуальных досье: " + unresolved + ".");
    if (pendingConclusions > 0)
      warnings.add("Досье без принятого пользователем заключения: " + pendingConclusions + ".");
    if (e.samples().size() > SAMPLE_LIMIT)
      warnings.add(
          "Показаны последние "
              + SAMPLE_LIMIT
              + " образцов из "
              + e.samples().size()
              + "; полный список — в опыте.");
    if (analyses.size() > ANALYSIS_LIMIT)
      warnings.add(
          "Показаны последние "
              + ANALYSIS_LIMIT
              + " актуальных анализов; предыдущие версии сохранены в опыте.");
    final var next = nextAction(nextRunning, nextPlanned, samples, analyses);
    final var component =
        e.components().stream()
            .filter(c -> c.role() == ReactionComponentRole.PRODUCT)
            .findFirst()
            .orElse(e.components().isEmpty() ? null : e.components().getFirst());
    return new Summary(
        e.id(),
        e.title(),
        procedure,
        recent.values(),
        e.samples().size(),
        samples.stream().limit(SAMPLE_LIMIT).toList(),
        analytical,
        outcomes,
        next,
        warnings,
        component == null ? null : drawing.layouts().get(component.id()));
  }

  private static NextAction nextAction(
      final LaboratoryProcedure.Step running,
      final LaboratoryProcedure.Step planned,
      final List<SampleSummary> samples,
      final List<AnalysisRecord> analyses) {
    if (running != null)
      return new NextAction(
          Action.PROCEDURE, "Открыть выполняемый шаг", running.stageId(), null, running.id());
    final var unresolved =
        analyses.stream().filter(AnalysisRecord::unresolved).findFirst().orElse(null);
    if (unresolved != null)
      return new NextAction(
          Action.EVIDENCE, "Открыть досье", null, unresolved.sampleId(), unresolved.id());
    if (!analyses.isEmpty()) {
      final var a = analyses.getFirst();
      return new NextAction(
          a.kind() == AnalysisKind.LEGACY ? Action.ANALYSIS : Action.EVIDENCE,
          "Открыть аналитическую запись",
          null,
          a.sampleId(),
          a.id());
    }
    if (!samples.isEmpty())
      return new NextAction(
          Action.ANALYSIS,
          "Добавить анализ",
          null,
          samples.size() == 1 ? samples.getFirst().id() : null,
          null);
    if (planned != null)
      return new NextAction(
          Action.PROCEDURE, "Открыть методику", planned.stageId(), null, planned.id());
    return new NextAction(Action.EXPERIMENT, "Открыть опыт", null, null, null);
  }

  private static String method(final LaboratoryAnalysis a) {
    if (a.data() instanceof LaboratoryAnalysis.Nmr nmr) return "ЯМР " + nmr.nucleus();
    if (a.data() instanceof LaboratoryAnalysis.Mass mass) return mass.technique();
    return "ТСХ";
  }

  private static String workupDetail(final Workup workup) {
    final var text = new StringBuilder();
    for (final var step : workup.steps()) {
      if (text.length() >= 1400) {
        text.append("\n… остальные операции — в опыте");
        break;
      }
      text.append(step.kind())
          .append(" · ")
          .append(clip(step.material(), 240))
          .append("\nПлан: ")
          .append(quantity(step.planned()))
          .append("; записано: ")
          .append(quantity(step.actual()))
          .append(" · ")
          .append(origin(step.observation().origin()))
          .append('\n')
          .append(clip(step.note(), 350))
          .append('\n');
    }
    text.append(workup.note());
    return clip(text.toString(), 1800);
  }

  private static String workupOrigin(final Workup workup) {
    final var origins = new LinkedHashSet<Origin>();
    workup.steps().forEach(s -> origins.add(s.observation().origin()));
    workup.losses().forEach(s -> origins.add(s.observation().origin()));
    return origins.isEmpty()
        ? "Не указано"
        : String.join(" / ", origins.stream().map(LaboratoryNotebookEvidence::origin).toList());
  }

  private static boolean observed(final Observation observation) {
    return observation.origin() != Origin.UNSPECIFIED || !observation.analyses().isEmpty();
  }

  private static boolean workupRecorded(final Workup workup) {
    return workup.steps().stream().anyMatch(s -> s.actual() != null || observed(s.observation()))
        || workup.losses().stream().anyMatch(s -> s.actual() != null || observed(s.observation()));
  }

  private static boolean purificationRecorded(final Purification purification) {
    return purification.inputActual() != null
        || observed(purification.observation())
        || purification.fractions().stream()
            .anyMatch(
                f ->
                    f.actual() != null
                        || f.purityPercent() != null
                        || f.sampleId() != null
                        || observed(f.observation()))
        || purification.pools().stream()
            .anyMatch(p -> p.actual() != null || p.sampleId() != null || observed(p.observation()))
        || purification.losses().stream()
            .anyMatch(l -> l.actual() != null || observed(l.observation()));
  }

  private static String origin(final Origin origin) {
    return switch (origin) {
      case MEASURED -> "Измерено";
      case LITERATURE -> "Литература";
      case SIMULATED -> "Расчёт / модель";
      case UNSPECIFIED -> "Не указано";
    };
  }

  private static String quantity(final LaboratoryQuantity quantity) {
    return quantity == null
        ? "не задано"
        : quantity.value().stripTrailingZeros().toString() + " " + quantity.unit();
  }

  private static String clip(final String value, final int max) {
    return value.length() <= max ? value : value.substring(0, max - 1) + "…";
  }

  private static void offer(
      final Recent recent, final Instant at, final Supplier<Activity> create) {
    final var activity = create.get();
    recent.latestKind.merge(
        activity.kind(), activity, (a, b) -> ACTIVITY_ORDER.compare(a, b) > 0 ? a : b);
    if (recent.items.size() < ACTIVITY_LIMIT || !at.isBefore(recent.items.peek().at())) {
      recent.items.add(activity);
      if (recent.items.size() > ACTIVITY_LIMIT) recent.items.poll();
    }
  }

  private static void check(final BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
      throw new CancellationException("Чтение опыта остановлено.");
  }
}
