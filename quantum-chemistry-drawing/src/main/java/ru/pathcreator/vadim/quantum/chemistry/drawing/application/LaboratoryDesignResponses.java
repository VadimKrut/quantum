/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperimentalDesign.Run;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.AnalysisRef;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.Origin;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryReactionOperationsService.AnalysisChoice;

/** Response provenance guards. Linking an exact analysis does not derive a numeric response from it. */
public final class LaboratoryDesignResponses {
  private LaboratoryDesignResponses() {}

  public static void validateEdit(final Run previous, final Run next) {
    validateEdit(previous, next, null);
  }

  /** A retained historical value may remain unspecified; authoring a new value requires explicit provenance. */
  public static void validateEdit(
      final Run previous, final Run next, final LaboratoryExperiment exactExperiment) {
    Objects.requireNonNull(next);
    if (previous != null && !previous.id().equals(next.id())) {
      throw new IllegalArgumentException("Отклик относится к другому запуску матрицы.");
    }
    final boolean sameValue =
        previous != null
            && (previous.response() == null
                ? next.response() == null
                : next.response() != null && previous.response().compareTo(next.response()) == 0);
    if (next.response() != null
        && next.responseOrigin() == Origin.UNSPECIFIED
        && !(sameValue
            && previous.responseOrigin() == Origin.UNSPECIFIED
            && previous.responseSource().equals(next.responseSource()))) {
      throw new IllegalArgumentException(
          "Выберите происхождение отклика: измерение, литература или расчёт / модель; укажите источник.");
    }
    if (next.responseAnalysis() != null) {
      final boolean retained =
          sameValue
              && previous.responseOrigin() == next.responseOrigin()
              && Objects.equals(previous.responseAnalysis(), next.responseAnalysis())
              && previous.experimentId().equals(next.experimentId())
              && previous.experimentFile().equals(next.experimentFile());
      if (exactExperiment != null) {
        resolve(next, exactExperiment);
      } else if (!retained) {
        throw new IllegalArgumentException(
            "Для новой связи или изменения отклика прочитайте связанный опыт и проверьте точную версию анализа; либо явно уберите ссылку.");
      }
    }
  }

  public static List<AnalysisChoice> analysisCandidates(
      final Run run, final LaboratoryExperiment experiment, final boolean includeHistory) {
    requireExperiment(run, experiment);
    final Set<String> samples = new LinkedHashSet<>();
    samples.add(null);
    experiment.samples().forEach(s -> samples.add(s.id()));
    final List<AnalysisChoice> result = new ArrayList<>();
    for (final String sample : samples) {
      result.addAll(
          LaboratoryReactionOperationsService.analysisCandidates(
              experiment, sample, includeHistory));
    }
    return result.stream()
        .sorted(
            Comparator.comparing(AnalysisChoice::recordedAt)
                .reversed()
                .thenComparing(AnalysisChoice::title))
        .toList();
  }

  public static AnalysisChoice resolve(final Run run, final LaboratoryExperiment experiment) {
    requireExperiment(run, experiment);
    if (run.responseAnalysis() == null) {
      throw new IllegalArgumentException("У отклика нет выбранной аналитической ссылки.");
    }
    final var analysis =
        LaboratoryReactionOperationsService.resolveAnalysis(experiment, run.responseAnalysis());
    if (run.responseOrigin() == Origin.UNSPECIFIED
        || !analysis.origin().name().equals(run.responseOrigin().name())) {
      throw new IllegalArgumentException(
          "Происхождение отклика не совпадает с выбранным анализом. Литературный или модельный анализ не становится измерением.");
    }
    return analysis;
  }

  /** Attach only the exact reference; the manually entered value, source and note are preserved. */
  public static Run withAnalysis(
      final Run run, final LaboratoryExperiment experiment, final AnalysisRef reference) {
    final var linked =
        new Run(
            run.id(),
            run.standardOrder(),
            run.replicate(),
            run.center(),
            run.levels(),
            run.position(),
            run.experimentFile(),
            run.experimentId(),
            run.response(),
            run.responseNote(),
            run.responseOrigin(),
            run.responseSource(),
            Objects.requireNonNull(reference));
    resolve(linked, experiment);
    return linked;
  }

  private static void requireExperiment(final Run run, final LaboratoryExperiment experiment) {
    Objects.requireNonNull(run);
    Objects.requireNonNull(experiment);
    if (run.experimentFile().isEmpty() || !run.experimentId().equals(experiment.id())) {
      throw new IllegalArgumentException(
          "Анализы должны принадлежать точному опубликованному опыту этого запуска.");
    }
  }

  public static List<String> diagnostics(final LaboratoryExperimentalDesign design) {
    final var counts = new EnumMap<Origin, Integer>(Origin.class);
    for (final var run : design.runs()) {
      if (run.response() != null) {
        counts.merge(run.responseOrigin(), 1, Integer::sum);
      }
    }
    if (counts.isEmpty()) {
      return List.of("Отклики не внесены; плановые количества не используются как результаты.");
    }
    final List<String> messages = new ArrayList<>();
    messages.add(
        "Происхождение введённых откликов: "
            + counts.entrySet().stream()
                .map(e -> e.getKey() + " — " + e.getValue())
                .collect(java.util.stream.Collectors.joining("; "))
            + ".");
    if (counts.containsKey(Origin.UNSPECIFIED)) {
      messages.add(
          "У части прежних откликов происхождение не указано. Они сохранены без потери, но не подтверждены как измерения.");
    }
    if (counts.containsKey(Origin.LITERATURE) || counts.containsKey(Origin.SIMULATED)) {
      messages.add(
          "Литературные и модельные отклики участвуют в расчёте как явно введённые числа; коэффициенты и разброс этой матрицы не являются подтверждением измерений текущего исследования.");
    }
    if (counts.size() > 1) {
      messages.add(
          "Матрица объединяет отклики разного происхождения. Проверьте сопоставимость данных перед интерпретацией эффектов.");
    }
    if (design.runs().stream()
        .anyMatch(r -> r.response() != null && r.responseAnalysis() != null)) {
      messages.add(
          "Аналитические ссылки фиксируют выбранные версии по ID; расчёт эффектов не перечитывает внешние файлы и не заменяет ссылки последними исправлениями.");
    }
    return List.copyOf(messages);
  }
}
