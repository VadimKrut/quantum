/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryReactionOperations.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Manual stage conclusions; neither quantities nor analysis results infer an outcome. */
public final class LaboratoryStageOutcomeService {
  private LaboratoryStageOutcomeService() {}

  public static StageOutcome create(
      final LaboratoryDrawing expected,
      final String stageId,
      final OutcomeStatus status,
      final List<String> targetIds,
      final List<String> byproductIds,
      final Observation observation,
      final String source,
      final String reason,
      final String correctsId,
      final Instant at) {
    final var experiment = Objects.requireNonNull(expected).experiment();
    final var stage = stage(experiment, stageId);
    final var outcome =
        new StageOutcome(
            LaboratoryReactionOperationsService.newId("stage_outcome"),
            stage.id(),
            stage.title(),
            at,
            status,
            snapshots(experiment, stage, targetIds),
            snapshots(experiment, stage, byproductIds),
            observation,
            source,
            reason,
            correctsId);
    append(experiment.operations(), outcome);
    validateNew(experiment, outcome);
    return outcome;
  }

  public static LaboratoryReactionOperations append(
      final LaboratoryReactionOperations previous, final StageOutcome outcome) {
    final var values = new ArrayList<>(previous.stageOutcomes());
    values.add(outcome);
    return new LaboratoryReactionOperations(
        previous.timePoints(), previous.workups(), previous.purifications(), values);
  }

  /** Only new records need live participants; old snapshots remain valid after editing/removing their stage. */
  public static void validateNew(
      final LaboratoryExperiment experiment, final StageOutcome outcome) {
    final var stage = stage(experiment, outcome.stageId());
    if (!stage.title().equals(outcome.stageTitle())) {
      throw new IllegalArgumentException(
          "Название стадии изменилось. Откройте оценку исхода заново.");
    }
    final var members = new HashSet<String>();
    stage.participants().forEach(p -> members.add(p.componentId()));
    final List<OutcomeProduct> products = new ArrayList<>(outcome.targets());
    products.addAll(outcome.byproducts());
    for (final var product : products) {
      final var component = product(experiment, stage, product.componentId());
      final var m = component.molecule();
      final var expected =
          Molecule.of(
              product.snapshot().id(),
              m.displayName(),
              m.atoms(),
              m.bonds(),
              m.charge(),
              m.spinMultiplicity(),
              m.stereochemistry(),
              m.conformation(),
              m.opticalRotation(),
              m.symmetry(),
              m.metadata());
      if (!expected.equals(product.snapshot()) || m.id().equals(product.snapshot().id())) {
        throw new IllegalArgumentException(
            "Сохранённая структура продукта не соответствует выбранному участнику стадии. Откройте оценку заново.");
      }
    }
    for (final var reference : outcome.observation().analyses()) {
      LaboratoryReactionOperationsService.resolveAnalysis(experiment, reference);
      if (!members.contains(analysisComponent(experiment, reference))) {
        throw new IllegalArgumentException(
            "Анализ относится к участнику другой стадии. Выберите анализ участника оцениваемой стадии.");
      }
    }
  }

  public static List<LaboratoryReactionOperationsService.AnalysisChoice> stageAnalysisCandidates(
      final LaboratoryExperiment experiment, final String stageId, final boolean includeHistory) {
    final var members = new HashSet<String>();
    stage(experiment, stageId).participants().forEach(p -> members.add(p.componentId()));
    final var legacyOld = new HashSet<String>();
    final var evidenceOld = new HashSet<String>();
    experiment
        .analyses()
        .forEach(
            a -> {
              if (a.correctsId() != null) legacyOld.add(a.correctsId());
            });
    experiment
        .analyticalEvidence()
        .entries()
        .forEach(
            a -> {
              if (a.correctsId() != null) evidenceOld.add(a.correctsId());
            });
    final List<LaboratoryReactionOperationsService.AnalysisChoice> result = new ArrayList<>();
    for (final var a : experiment.analyses())
      if (members.contains(a.componentId()) && (includeHistory || !legacyOld.contains(a.id()))) {
        result.add(
            new LaboratoryReactionOperationsService.AnalysisChoice(
                new AnalysisRef(AnalysisKind.LEGACY, a.id()),
                a.title(),
                a.data().method(),
                a.sampleId(),
                a.recordedAt(),
                a.origin(),
                a.source(),
                legacyOld.contains(a.id())));
      }
    for (final var a : experiment.analyticalEvidence().entries())
      if (members.contains(a.componentId())
          && !(a.data() instanceof LaboratoryAnalyticalEvidence.Dossier)
          && (includeHistory || !evidenceOld.contains(a.id()))) {
        result.add(
            new LaboratoryReactionOperationsService.AnalysisChoice(
                new AnalysisRef(AnalysisKind.EVIDENCE, a.id()),
                a.title(),
                a.data().method(),
                a.sampleId(),
                a.recordedAt(),
                a.origin(),
                a.source(),
                evidenceOld.contains(a.id())));
      }
    return result.stream()
        .sorted(
            Comparator.comparing(LaboratoryReactionOperationsService.AnalysisChoice::recordedAt)
                .reversed()
                .thenComparing(LaboratoryReactionOperationsService.AnalysisChoice::title))
        .toList();
  }

  private static List<OutcomeProduct> snapshots(
      final LaboratoryExperiment experiment,
      final LaboratoryReactionStage stage,
      final List<String> ids) {
    if (ids.size() > 200) {
      throw new IllegalArgumentException("Укажите не более 200 продуктов.");
    }
    return ids.stream()
        .map(
            id ->
                new OutcomeProduct(
                    id,
                    LaboratoryAnalysisSummary.snapshot(
                        product(experiment, stage, id).molecule(),
                        LaboratoryReactionOperationsService.newId("outcome_product"))))
        .toList();
  }

  private static LaboratoryReactionStage stage(
      final LaboratoryExperiment experiment, final String id) {
    return experiment.stages().stream()
        .filter(s -> s.id().equals(id))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Выберите существующую стадию опыта для оценки исхода."));
  }

  private static LaboratoryComponent product(
      final LaboratoryExperiment experiment, final LaboratoryReactionStage stage, final String id) {
    if (stage.participants().stream()
        .noneMatch(p -> p.componentId().equals(id) && p.role() == ReactionComponentRole.PRODUCT)) {
      throw new IllegalArgumentException(
          "Целевой или побочный продукт должен быть участником выбранной стадии с ролью «Продукт».");
    }
    return experiment.components().stream()
        .filter(c -> c.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Выбранный продукт отсутствует в опыте."));
  }

  private static String analysisComponent(
      final LaboratoryExperiment experiment, final AnalysisRef ref) {
    return ref.kind() == AnalysisKind.LEGACY
        ? experiment.analyses().stream()
            .filter(a -> a.id().equals(ref.id()))
            .findFirst()
            .orElseThrow()
            .componentId()
        : experiment.analyticalEvidence().entries().stream()
            .filter(a -> a.id().equals(ref.id()))
            .findFirst()
            .orElseThrow()
            .componentId();
  }
}
