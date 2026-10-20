/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.*;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;

/** Arithmetic and append-only commands for manually recorded monitoring, workup and purification. */
public final class LaboratoryReactionOperationsService {
  private LaboratoryReactionOperationsService() {}

  public record PoolTotal(
      String id,
      String label,
      LaboratoryQuantity quantity,
      boolean measured,
      boolean recorded,
      Origin origin) {}

  public record Totals(
      LaboratoryQuantityUnit unit,
      BigDecimal input,
      BigDecimal knownOutputs,
      BigDecimal knownLosses,
      BigDecimal unaccounted,
      boolean complete,
      List<PoolTotal> pools,
      List<String> warnings) {
    public Totals {
      pools = List.copyOf(pools);
      warnings = List.copyOf(warnings);
    }
  }

  /** Stable references resolve the selected version, never its later correction. */
  public record AnalysisChoice(
      AnalysisRef reference,
      String title,
      String method,
      String sampleId,
      Instant recordedAt,
      LaboratoryAnalysis.Origin origin,
      String source,
      boolean historical) {}

  public static List<AnalysisChoice> analysisCandidates(
      final LaboratoryExperiment experiment, final String sampleId, final boolean includeHistory) {
    final Set<String> legacyOld = new HashSet<>(), evidenceOld = new HashSet<>();
    experiment
        .analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) {
                legacyOld.add(a.correctsId());
              }
            });
    experiment
        .analyticalEvidence()
        .entries()
        .forEach(
            a -> {
              if (a.correctsId() != null) {
                evidenceOld.add(a.correctsId());
              }
            });
    final List<AnalysisChoice> result = new ArrayList<>();
    for (final var a : experiment.analyses()) {
      if (Objects.equals(sampleId, a.sampleId())
          && (includeHistory || !legacyOld.contains(a.id()))) {
        result.add(
            new AnalysisChoice(
                new AnalysisRef(AnalysisKind.LEGACY, a.id()),
                a.title(),
                a.data().method(),
                a.sampleId(),
                a.recordedAt(),
                a.origin(),
                a.source(),
                legacyOld.contains(a.id())));
      }
    }
    for (final var a : experiment.analyticalEvidence().entries()) {
      if (Objects.equals(sampleId, a.sampleId())
          && !(a.data() instanceof LaboratoryAnalyticalEvidence.Dossier)
          && (includeHistory || !evidenceOld.contains(a.id()))) {
        result.add(
            new AnalysisChoice(
                new AnalysisRef(AnalysisKind.EVIDENCE, a.id()),
                a.title(),
                a.data().method(),
                a.sampleId(),
                a.recordedAt(),
                a.origin(),
                a.source(),
                evidenceOld.contains(a.id())));
      }
    }
    return result.stream()
        .sorted(
            Comparator.comparing(AnalysisChoice::recordedAt)
                .reversed()
                .thenComparing(AnalysisChoice::title))
        .toList();
  }

  public static AnalysisChoice resolveAnalysis(
      final LaboratoryExperiment experiment, final AnalysisRef reference) {
    final String sampleId;
    if (reference.kind() == AnalysisKind.LEGACY) {
      sampleId =
          experiment.analyses().stream()
              .filter(a -> a.id().equals(reference.id()))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Связанный анализ отсутствует."))
              .sampleId();
    } else {
      sampleId =
          experiment.analyticalEvidence().entries().stream()
              .filter(a -> a.id().equals(reference.id()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalArgumentException("Связанная аналитическая запись отсутствует."))
              .sampleId();
    }
    return analysisCandidates(experiment, sampleId, true).stream()
        .filter(a -> a.reference().equals(reference))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Этот тип записи не является подходящим анализом контроля или очистки."));
  }

  public static void validateObservation(
      final LaboratoryExperiment experiment, final String sampleId, final Observation observation) {
    for (final var reference : observation.analyses()) {
      if (!Objects.equals(sampleId, resolveAnalysis(experiment, reference).sampleId())) {
        throw new IllegalArgumentException(
            "Анализ относится к другому физическому образцу. Уберите несовместимую связь или выберите исходный образец.");
      }
    }
  }

  public static String originLabel(final Origin value) {
    return switch (value) {
      case UNSPECIFIED -> "Не указано";
      case MEASURED -> "Измерено";
      case LITERATURE -> "Литература";
      case SIMULATED -> "Расчёт / модель";
    };
  }

  private static boolean sameNumber(final BigDecimal a, final BigDecimal b) {
    return a == null ? b == null : b != null && a.compareTo(b) == 0;
  }

  private static boolean sameQuantity(final LaboratoryQuantity a, final LaboratoryQuantity b) {
    return a == null
        ? b == null
        : b != null && a.unit() == b.unit() && sameNumber(a.value(), b.value());
  }

  private static void origin(
      final Observation next,
      final Observation before,
      final boolean hasFact,
      final boolean unchanged,
      final String label) {
    if (hasFact
        && next.origin() == Origin.UNSPECIFIED
        && (before == null || before.origin() != Origin.UNSPECIFIED || !unchanged)) {
      throw new IllegalArgumentException(
          label + ": выберите происхождение значения — измерение, литература или расчёт / модель.");
    }
  }

  private static void losses(final List<Loss> next, final List<Loss> before) {
    for (final var value : next) {
      final var old =
          before.stream().filter(v -> v.id().equals(value.id())).findFirst().orElse(null);
      origin(
          value.observation(),
          old == null ? null : old.observation(),
          value.actual() != null,
          old != null
              && sameQuantity(value.actual(), old.actual())
              && value.source().equals(old.source()),
          value.label());
    }
  }

  private static void origins(final Revision record, final Revision original) {
    if (record instanceof TimePoint p) {
      final var old = original instanceof TimePoint v ? v : null;
      origin(
          p.observation(),
          old == null ? null : old.observation(),
          true,
          old != null
              && sameNumber(p.elapsed(), old.elapsed())
              && p.elapsedUnit() == old.elapsedUnit()
              && sameNumber(p.conversion(), old.conversion())
              && p.conversionUnit() == old.conversionUnit()
              && p.method() == old.method()
              && Objects.equals(p.sampleId(), old.sampleId())
              && p.source().equals(old.source()),
          "Контрольная проба");
    } else if (record instanceof Workup w) {
      final var old = original instanceof Workup v ? v : null;
      for (final var step : w.steps()) {
        final var before =
            old == null
                ? null
                : old.steps().stream()
                    .filter(v -> v.id().equals(step.id()))
                    .findFirst()
                    .orElse(null);
        origin(
            step.observation(),
            before == null ? null : before.observation(),
            step.actual() != null,
            before != null
                && sameQuantity(step.actual(), before.actual())
                && step.source().equals(before.source())
                && step.kind() == before.kind()
                && step.material().equals(before.material())
                && step.repetitions() == before.repetitions()
                && Objects.equals(step.fromPhaseId(), before.fromPhaseId())
                && Objects.equals(step.toPhaseId(), before.toPhaseId()),
            "Операция обработки");
      }
      losses(w.losses(), old == null ? List.of() : old.losses());
    } else if (record instanceof Purification r) {
      final var old = original instanceof Purification v ? v : null;
      origin(
          r.observation(),
          old == null ? null : old.observation(),
          r.inputActual() != null,
          old != null
              && sameQuantity(r.inputActual(), old.inputActual())
              && Objects.equals(r.inputSampleId(), old.inputSampleId())
              && r.source().equals(old.source()),
          "Загрузка очистки");
      for (final var value : r.fractions()) {
        final var before =
            old == null
                ? null
                : old.fractions().stream()
                    .filter(v -> v.id().equals(value.id()))
                    .findFirst()
                    .orElse(null);
        origin(
            value.observation(),
            before == null ? null : before.observation(),
            value.actual() != null || value.purityPercent() != null,
            before != null
                && sameQuantity(value.actual(), before.actual())
                && sameNumber(value.purityPercent(), before.purityPercent())
                && Objects.equals(value.sampleId(), before.sampleId())
                && value.source().equals(before.source()),
            value.label());
      }
      for (final var value : r.pools()) {
        final var before =
            old == null
                ? null
                : old.pools().stream()
                    .filter(v -> v.id().equals(value.id()))
                    .findFirst()
                    .orElse(null);
        origin(
            value.observation(),
            before == null ? null : before.observation(),
            value.actual() != null,
            before != null
                && sameQuantity(value.actual(), before.actual())
                && value.fractionIds().equals(before.fractionIds())
                && Objects.equals(value.sampleId(), before.sampleId())
                && value.source().equals(before.source()),
            value.label());
      }
      losses(r.losses(), old == null ? List.of() : old.losses());
    }
  }

  public static BigDecimal minutes(final TimePoint point) {
    return switch (point.elapsedUnit()) {
      case SECOND -> point.elapsed().divide(BigDecimal.valueOf(60), MathContext.DECIMAL128);
      case MINUTE -> point.elapsed();
      case HOUR -> point.elapsed().multiply(BigDecimal.valueOf(60));
    };
  }

  public static BigDecimal conversionPercent(final TimePoint point) {
    return point.conversion() == null
        ? null
        : point.conversionUnit() == ConversionUnit.PERCENT
            ? point.conversion()
            : point.conversion().multiply(BigDecimal.valueOf(100));
  }

  public static LaboratoryReactionOperations append(
      final LaboratoryReactionOperations previous, final TimePoint point) {
    final var values = new ArrayList<>(previous.timePoints());
    values.add(point);
    return new LaboratoryReactionOperations(
        values, previous.workups(), previous.purifications(), previous.stageOutcomes());
  }

  public static LaboratoryReactionOperations append(
      final LaboratoryReactionOperations previous, final Workup workup) {
    final var values = new ArrayList<>(previous.workups());
    values.add(workup);
    return new LaboratoryReactionOperations(
        previous.timePoints(), values, previous.purifications(), previous.stageOutcomes());
  }

  public static LaboratoryReactionOperations append(
      final LaboratoryReactionOperations previous, final Purification run) {
    final var values = new ArrayList<>(previous.purifications());
    values.add(run);
    return new LaboratoryReactionOperations(
        previous.timePoints(), previous.workups(), values, previous.stageOutcomes());
  }

  public static void validateNewLinks(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionOperations previous,
      final LaboratoryReactionOperations next) {
    LaboratoryReactionOperations.requireAppend(previous, next);
    final Set<String> stages = new HashSet<>();
    experiment.stages().forEach(s -> stages.add(s.id()));
    final Set<String> samples = new HashSet<>();
    experiment.samples().forEach(s -> samples.add(s.id()));
    final Map<String, Revision> records = new HashMap<>();
    previous.timePoints().forEach(r -> records.put(r.id(), r));
    previous.workups().forEach(r -> records.put(r.id(), r));
    previous.purifications().forEach(r -> records.put(r.id(), r));
    previous.stageOutcomes().forEach(r -> records.put(r.id(), r));
    final List<Revision> additions = new ArrayList<>();
    additions.addAll(next.timePoints());
    additions.addAll(next.workups());
    additions.addAll(next.purifications());
    additions.addAll(next.stageOutcomes());
    for (final var record : additions) {
      if (records.containsKey(record.id())) {
        continue;
      }
      final var original = record.correctsId() == null ? null : records.get(record.correctsId());
      if (record.stageId() != null
          && !stages.contains(record.stageId())
          && (original == null || !Objects.equals(original.stageId(), record.stageId()))) {
        throw new IllegalArgumentException("Выбранная стадия отсутствует в опыте.");
      }
      origins(record, original);
      if (record instanceof StageOutcome outcome) {
        LaboratoryStageOutcomeService.validateNew(experiment, outcome);
      }
      if (record instanceof TimePoint point) {
        sample(point.sampleId(), samples);
        validateObservation(experiment, point.sampleId(), point.observation());
      }
      if (record instanceof Purification run) {
        sample(run.inputSampleId(), samples);
        run.fractions().forEach(f -> sample(f.sampleId(), samples));
        run.pools().forEach(p -> sample(p.sampleId(), samples));
        validateObservation(experiment, run.inputSampleId(), run.observation());
        run.fractions()
            .forEach(f -> validateObservation(experiment, f.sampleId(), f.observation()));
        run.pools().forEach(p -> validateObservation(experiment, p.sampleId(), p.observation()));
      }
    }
  }

  private static void sample(final String id, final Set<String> known) {
    if (id != null && !known.contains(id)) {
      throw new IllegalArgumentException(
          "Связанный физический образец отсутствует в опыте. Выберите его в разделе образцов.");
    }
  }

  /** Existing workup/purification methods remain plans; every measurement and physical-sample link is cleared. */
  public static LaboratoryReactionOperations repeatPlan(
      final LaboratoryReactionOperations source, final LaboratoryExperiment target) {
    final Set<String> stages = new HashSet<>();
    target.stages().forEach(s -> stages.add(s.id()));
    final List<Workup> workups = new ArrayList<>();
    final List<Purification> runs = new ArrayList<>();
    for (final var workup : source.activeWorkups()) {
      if (workup.stageId() != null && !stages.contains(workup.stageId())) {
        continue;
      }
      final var steps =
          workup.steps().stream()
              .map(
                  s ->
                      new WorkupStep(
                          s.id(),
                          s.kind(),
                          s.fromPhaseId(),
                          s.toPhaseId(),
                          s.material(),
                          s.repetitions(),
                          s.planned(),
                          null,
                          "",
                          s.note()))
              .toList();
      final var losses =
          workup.losses().stream()
              .map(l -> new Loss(l.id(), l.label(), l.planned(), null, ""))
              .toList();
      workups.add(
          new Workup(
              newId("workup"),
              workup.stageId(),
              workup.stageTitle(),
              Instant.now(),
              workup.title(),
              workup.phases(),
              steps,
              losses,
              workup.note(),
              null));
    }
    for (final var run : source.activePurifications()) {
      if (run.stageId() != null && !stages.contains(run.stageId())) {
        continue;
      }
      final var fractions =
          run.fractions().stream()
              .map(
                  f ->
                      new Fraction(
                          f.id(),
                          f.label(),
                          f.start(),
                          f.end(),
                          f.planned(),
                          null,
                          null,
                          null,
                          "",
                          ""))
              .toList();
      final var losses =
          run.losses().stream()
              .map(l -> new Loss(l.id(), l.label(), l.planned(), null, ""))
              .toList();
      runs.add(
          new Purification(
              newId("purification"),
              run.stageId(),
              run.stageTitle(),
              Instant.now(),
              run.title(),
              run.kind(),
              null,
              run.inputPlanned(),
              null,
              run.stationaryPhase(),
              run.sorbentMass(),
              run.solventA(),
              run.solventB(),
              run.axis(),
              run.gradient(),
              fractions,
              List.of(),
              losses,
              "",
              run.note(),
              null));
    }
    return new LaboratoryReactionOperations(List.of(), workups, runs, List.of());
  }

  public static String newId(final String prefix) {
    return prefix + "_" + UUID.randomUUID();
  }

  /** Pools replace their source fractions in totals. Missing/incompatible values remain explicitly unknown. */
  public static Totals totals(final Purification run) {
    final List<String> warnings = new ArrayList<>();
    final List<PoolTotal> poolTotals = new ArrayList<>();
    final Set<String> pooled = new HashSet<>();
    final Map<String, Fraction> fractions = new LinkedHashMap<>();
    run.fractions().forEach(f -> fractions.put(f.id(), f));
    final List<LaboratoryQuantity> outputs = new ArrayList<>();
    boolean complete = true;
    for (final var pool : run.pools()) {
      pooled.addAll(pool.fractionIds());
      LaboratoryQuantity amount = pool.actual();
      if (amount == null) {
        amount = sum(pool.fractionIds().stream().map(id -> fractions.get(id).actual()).toList());
        complete = false;
        warnings.add(
            pool.label()
                + ": количество после объединения не указано."
                + (amount == null
                    ? " Сумма фракций тоже неизвестна или несопоставима."
                    : " Показана только арифметическая сумма исходных фракций."));
      }
      poolTotals.add(
          new PoolTotal(
              pool.id(),
              pool.label(),
              amount,
              pool.actual() != null && pool.observation().origin() == Origin.MEASURED,
              pool.actual() != null,
              pool.observation().origin()));
      outputs.add(amount);
    }
    for (final var fraction : run.fractions()) {
      if (!pooled.contains(fraction.id())) {
        outputs.add(fraction.actual());
        if (fraction.actual() == null) {
          warnings.add(fraction.label() + ": полученное количество не указано.");
          complete = false;
        }
      }
    }
    if (outputs.isEmpty()) {
      complete = false;
      warnings.add("Не записаны выходящие фракции или объединения.");
    }
    LaboratoryQuantityUnit unit = run.inputActual() == null ? null : run.inputActual().unit();
    if (unit == null) {
      unit =
          outputs.stream()
              .filter(Objects::nonNull)
              .map(LaboratoryQuantity::unit)
              .findFirst()
              .orElse(null);
    }
    final BigDecimal input = run.inputActual() == null ? null : run.inputActual().value();
    if (input == null) {
      complete = false;
      warnings.add("Загрузка очистки не указана; материальный баланс неизвестен.");
    }
    BigDecimal output = BigDecimal.ZERO, losses = BigDecimal.ZERO;
    for (final var value : outputs) {
      if (value == null) {
        complete = false;
        continue;
      }
      if (unit == null || !unit.dimension().equals(value.unit().dimension())) {
        complete = false;
        warnings.add(
            "Часть выходящих количеств задана в другой размерности. Масса, объём и моли без дополнительных данных не складываются.");
      } else {
        output = output.add(value.in(unit).value());
      }
    }
    for (final var loss : run.losses()) {
      if (loss.actual() == null) {
        complete = false;
        warnings.add(loss.label() + ": количество потери не указано.");
        continue;
      }
      if (unit == null || !unit.dimension().equals(loss.actual().unit().dimension())) {
        complete = false;
        warnings.add(loss.label() + ": размерность потери не совпадает с материальным учётом.");
      } else {
        losses = losses.add(loss.actual().in(unit).value());
      }
    }
    final BigDecimal difference =
        complete && input != null ? input.subtract(output).subtract(losses) : null;
    if (difference != null && difference.signum() < 0) {
      warnings.add(
          "Учтённый выход с потерями больше загрузки. Проверьте состав, единицы и происхождение масс; это не выход реакции.");
    }
    final Set<Origin> origins = new LinkedHashSet<>();
    if (run.inputActual() != null) {
      origins.add(run.observation().origin());
    }
    run.fractions().stream()
        .filter(f -> f.actual() != null && !pooled.contains(f.id()))
        .forEach(f -> origins.add(f.observation().origin()));
    run.pools().stream()
        .filter(p -> p.actual() != null)
        .forEach(p -> origins.add(p.observation().origin()));
    run.pools().stream()
        .filter(p -> p.actual() == null)
        .flatMap(p -> p.fractionIds().stream())
        .map(fractions::get)
        .filter(f -> f.actual() != null)
        .forEach(f -> origins.add(f.observation().origin()));
    run.losses().stream()
        .filter(l -> l.actual() != null)
        .forEach(l -> origins.add(l.observation().origin()));
    if (!origins.isEmpty()) {
      warnings.add(
          "Происхождение количеств: "
              + String.join(
                  " / ",
                  origins.stream().map(LaboratoryReactionOperationsService::originLabel).toList())
              + (origins.size() > 1 ? " · смешанные источники." : "."));
    }
    warnings.add(
        "Разница загрузки и учтённых потоков не является измеренной потерей и не рассчитывает химический выход или чистоту.");
    return new Totals(
        unit,
        input,
        output,
        losses,
        difference,
        complete,
        poolTotals,
        List.copyOf(new LinkedHashSet<>(warnings)));
  }

  private static LaboratoryQuantity sum(final List<LaboratoryQuantity> values) {
    if (values.isEmpty() || values.stream().anyMatch(Objects::isNull)) {
      return null;
    }
    final var unit = values.getFirst().unit();
    if (values.stream().anyMatch(v -> !v.unit().dimension().equals(unit.dimension()))) {
      return null;
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (final var value : values) {
      sum = sum.add(value.in(unit).value());
    }
    return new LaboratoryQuantity(sum, unit);
  }
}
