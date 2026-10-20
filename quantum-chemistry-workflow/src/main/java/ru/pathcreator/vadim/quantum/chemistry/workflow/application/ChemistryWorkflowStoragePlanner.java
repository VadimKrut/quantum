/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.api.QuantumIrFiles;
import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnostic;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenario;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageItem;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPathwayStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionEventKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalCoverageItemRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultSectionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultValueRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageConditionSnapshotRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageQuantumProgramRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecordStatus;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeIteration;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramArtifact;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityItem;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationKind;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowCandidateTrace;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowExecutionBundle;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.storage.ChemistryWorkflowStoragePlan;

/**
 * Собирает storage-ready записи реакции из результата workflow для UI/API.
 */
public final class ChemistryWorkflowStoragePlanner {

  private final ChemistryStorageService storageService;

  public ChemistryWorkflowStoragePlanner() {
    this(new ChemistryStorageService());
  }

  public ChemistryWorkflowStoragePlanner(final ChemistryStorageService storageService) {
    if (storageService == null) {
      throw new IllegalArgumentException("Chemistry workflow storage service must not be null.");
    }
    this.storageService = storageService;
  }

  public ChemistryWorkflowStoragePlan plan(final ChemistryWorkflowResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return this.plan(
        result.job(),
        result.reactionPredictionResult(),
        result.hasReactionPredictionExplanation() ? result.reactionPredictionExplanation() : null,
        result.recommendationPlan(),
        result.hasClassicalCalculationResult()
            ? result.classicalCalculationResult().calculationId().value()
            : null,
        result.hasQuantumProgramPackage()
            ? result.quantumProgramPackage().artifactIds()
            : List.of(),
        result.hasConditionScenarioPlan() ? result.conditionScenarioPlan().scenarios().size() : 0,
        result.hasConditionScenarioEvaluationPlan()
            ? result.conditionScenarioEvaluationPlan().evaluations().size()
            : 0,
        result.hasConditionScenarioPlan() ? result.conditionScenarioPlan() : null,
        result.hasConditionScenarioEvaluationPlan()
            ? result.conditionScenarioEvaluationPlan()
            : null,
        result.hasClassicalCalculationResult() ? result.classicalCalculationResult() : null,
        result.hasQuantumProgramPackage()
            ? result.quantumProgramPackage()
            : ChemistryQuantumProgramPackage.EMPTY,
        result.hasVqeOptimizationResult() ? result.vqeOptimizationResult() : null);
  }

  public ChemistryWorkflowStoragePlan plan(final ChemistryWorkflowExecutionBundle bundle) {
    if (bundle == null) {
      throw new IllegalArgumentException("Chemistry workflow execution bundle must not be null.");
    }
    final ChemistryWorkflowResult result = bundle.workflowResult();
    return this.plan(
        result.job(),
        result.reactionPredictionResult(),
        result.hasReactionPredictionExplanation() ? result.reactionPredictionExplanation() : null,
        result.recommendationPlan(),
        result.hasClassicalCalculationResult()
            ? result.classicalCalculationResult().calculationId().value()
            : null,
        result.hasQuantumProgramPackage()
            ? result.quantumProgramPackage().artifactIds()
            : List.of(),
        result.hasConditionScenarioPlan() ? result.conditionScenarioPlan().scenarios().size() : 0,
        result.hasConditionScenarioEvaluationPlan()
            ? result.conditionScenarioEvaluationPlan().evaluations().size()
            : 0,
        result.hasConditionScenarioPlan() ? result.conditionScenarioPlan() : null,
        result.hasConditionScenarioEvaluationPlan()
            ? result.conditionScenarioEvaluationPlan()
            : null,
        ChemistryWorkflowStoragePlanner.allClassicalResults(
            result.hasClassicalCalculationResult() ? result.classicalCalculationResult() : null,
            bundle.followUpResults()),
        result.hasQuantumProgramPackage()
            ? result.quantumProgramPackage()
            : ChemistryQuantumProgramPackage.EMPTY,
        result.hasVqeOptimizationResult() ? result.vqeOptimizationResult() : null,
        bundle.computationParityPlan(),
        bundle.candidateTraces());
  }

  public ChemistryWorkflowStoragePlan plan(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult predictionResult,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final String classicalResultId,
      final List<String> quantumProgramIds) {
    return this.plan(
        job, predictionResult, null, recommendationPlan, classicalResultId, quantumProgramIds);
  }

  public ChemistryWorkflowStoragePlan plan(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final String classicalResultId,
      final List<String> quantumProgramIds) {
    return this.plan(
        job,
        predictionResult,
        explanation,
        recommendationPlan,
        classicalResultId,
        quantumProgramIds,
        null);
  }

  public ChemistryWorkflowStoragePlan plan(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final String classicalResultId,
      final List<String> quantumProgramIds,
      final VqeOptimizationResult vqeOptimizationResult) {
    return this.plan(
        job,
        predictionResult,
        explanation,
        recommendationPlan,
        classicalResultId,
        quantumProgramIds,
        0,
        0,
        null,
        null,
        null,
        ChemistryQuantumProgramPackage.EMPTY,
        vqeOptimizationResult,
        ChemistryComputationParityPlan.EMPTY,
        List.of());
  }

  public ChemistryWorkflowStoragePlan plan(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final String classicalResultId,
      final List<String> quantumProgramIds,
      final int conditionScenarioCount,
      final int evaluatedConditionScenarioCount,
      final ClassicalCalculationResult classicalCalculationResult,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final VqeOptimizationResult vqeOptimizationResult) {
    return this.plan(
        job,
        predictionResult,
        explanation,
        recommendationPlan,
        classicalResultId,
        quantumProgramIds,
        conditionScenarioCount,
        evaluatedConditionScenarioCount,
        null,
        null,
        ChemistryWorkflowStoragePlanner.allClassicalResults(classicalCalculationResult, List.of()),
        quantumProgramPackage,
        vqeOptimizationResult,
        ChemistryComputationParityPlan.EMPTY,
        List.of());
  }

  public ChemistryWorkflowStoragePlan plan(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final String classicalResultId,
      final List<String> quantumProgramIds,
      final int conditionScenarioCount,
      final int evaluatedConditionScenarioCount,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final ClassicalCalculationResult classicalCalculationResult,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final VqeOptimizationResult vqeOptimizationResult) {
    return this.plan(
        job,
        predictionResult,
        explanation,
        recommendationPlan,
        classicalResultId,
        quantumProgramIds,
        conditionScenarioCount,
        evaluatedConditionScenarioCount,
        conditionScenarioPlan,
        conditionScenarioEvaluationPlan,
        ChemistryWorkflowStoragePlanner.allClassicalResults(classicalCalculationResult, List.of()),
        quantumProgramPackage,
        vqeOptimizationResult,
        ChemistryComputationParityPlan.EMPTY,
        List.of());
  }

  public ChemistryWorkflowStoragePlan plan(
      final ChemistryWorkflowJob job,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final String classicalResultId,
      final List<String> quantumProgramIds,
      final int conditionScenarioCount,
      final int evaluatedConditionScenarioCount,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final List<ClassicalCalculationResult> classicalCalculationResults,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final VqeOptimizationResult vqeOptimizationResult,
      final ChemistryComputationParityPlan computationParityPlan,
      final List<ChemistryWorkflowCandidateTrace> candidateTraces) {
    if (job == null) {
      throw new IllegalArgumentException("Chemistry workflow job must not be null.");
    }
    final String jobId = job.id().value();
    final List<String> storageClassicalResultIds =
        ChemistryWorkflowStoragePlanner.classicalResultIds(
            classicalResultId, classicalCalculationResults, computationParityPlan);
    final List<String> storageQuantumProgramIds =
        ChemistryWorkflowStoragePlanner.quantumProgramIds(
            job, recommendationPlan, quantumProgramIds, computationParityPlan);
    final List<ChemistryStorageClassicalResultRecord> classicalResultRecords =
        ChemistryWorkflowStoragePlanner.classicalResultRecords(jobId, classicalCalculationResults);
    final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords =
        ChemistryWorkflowStoragePlanner.quantumProgramRecords(jobId, quantumProgramPackage);
    final List<ChemistryStorageExtension> extensions =
        ChemistryWorkflowStoragePlanner.extensions(
            jobId,
            storageQuantumProgramIds,
            vqeOptimizationResult,
            conditionScenarioPlan,
            conditionScenarioEvaluationPlan,
            candidateTraces);
    if (predictionResult == null || !predictionResult.hasCandidates()) {
      return ChemistryWorkflowStoragePlan.ofMolecularCalculation(
          ChemistryWorkflowStoragePlanner.subjectMolecules(job),
          classicalResultRecords,
          extensions);
    }
    final ChemistryStorageReactionRecord reactionRecord =
        ChemistryStorageReactionRecord.of(
            jobId + "_reaction_record_0",
            ChemistryWorkflowStoragePlanner.predictedReaction(jobId, predictionResult),
            ChemistryStorageReactionRecordStatus.PLANNED,
            0,
            null,
            "Predicted reaction route created by chemistry workflow.");
    final ChemistryStoragePredictionRecord predictionRecord =
        this.storageService.predictionRecord(
            jobId + "_prediction_record_0",
            0,
            ChemistryStorageReactionRecordStatus.PLANNED,
            predictionResult,
            "Ranked product candidates with mechanism hypotheses.");
    final ChemistryStorageComputationPlanRecord computationPlanRecord =
        this.storageService.computationPlanRecord(
            jobId + "_computation_plan_0",
            reactionRecord.id(),
            0,
            ChemistryStorageReactionRecordStatus.PLANNED,
            ChemistryWorkflowStoragePlanner.classicalPlanIds(recommendationPlan),
            storageClassicalResultIds,
            storageQuantumProgramIds,
            true,
            "Deferred classical and quantum follow-up calculations prepared by workflow.");
    final List<ChemistryStorageQuantumProgramRecord> completeQuantumProgramRecords =
        ChemistryWorkflowStoragePlanner.completeQuantumProgramRecords(
            computationPlanRecord.id(),
            computationPlanRecord.quantumProgramIds(),
            quantumProgramRecords);
    final ChemistryStorageConditionSnapshotRecord conditionSnapshotRecord =
        ChemistryStorageConditionSnapshotRecord.of(
            jobId + "_condition_snapshot_0",
            reactionRecord.reaction().id().value(),
            0,
            "Workflow input conditions",
            predictionResult.request().conditions(),
            "Reaction conditions used by prediction, classical planning and Quantum IR preparation.");
    final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords =
        this.outcomeRecords(
            jobId,
            reactionRecord.reaction().id().value(),
            predictionResult,
            recommendationPlan,
            storageQuantumProgramIds,
            computationPlanRecord);
    final ChemistryStorageRoadmapRecord roadmapRecord =
        ChemistryWorkflowStoragePlanner.roadmapRecord(
            jobId,
            predictionResult,
            recommendationPlan,
            storageQuantumProgramIds,
            conditionScenarioCount,
            evaluatedConditionScenarioCount);
    final ChemistryStorageReactionHistoryEvent historyEvent =
        this.storageService.reactionHistoryEvent(
            jobId + "_history_event_0",
            reactionRecord.reaction().id().value(),
            0,
            ChemistryStorageReactionEventKind.PREDICTION_RECORDED,
            "Prediction, classical result and quantum work prepared",
            ChemistryWorkflowStoragePlanner.historyDescription(explanation),
            List.of(reactionRecord.id()),
            List.of(conditionSnapshotRecord.id()),
            List.of(predictionRecord.id()),
            List.of(computationPlanRecord.id()),
            computationPlanRecord.classicalResultIds(),
            computationPlanRecord.quantumProgramIds());
    return ChemistryWorkflowStoragePlan.of(
        reactionRecord,
        predictionRecord,
        computationPlanRecord,
        roadmapRecord,
        List.of(conditionSnapshotRecord),
        outcomeRecords,
        classicalResultRecords,
        completeQuantumProgramRecords,
        historyEvent,
        extensions,
        ChemistryWorkflowStoragePlanner.candidateProductMolecules(predictionResult));
  }

  private static List<ChemistryStorageQuantumProgramRecord> completeQuantumProgramRecords(
      final String computationPlanRecordId,
      final List<String> quantumProgramIds,
      final List<ChemistryStorageQuantumProgramRecord> existingRecords) {
    final ArrayList<ChemistryStorageQuantumProgramRecord> records =
        new ArrayList<ChemistryStorageQuantumProgramRecord>(existingRecords);
    for (int i = 0; i < quantumProgramIds.size(); ++i) {
      final String quantumProgramId = quantumProgramIds.get(i);
      if (ChemistryWorkflowStoragePlanner.containsQuantumProgramRecord(records, quantumProgramId)) {
        continue;
      }
      records.add(
          ChemistryStorageQuantumProgramRecord.of(
              quantumProgramId,
              computationPlanRecordId,
              0,
              "PLANNED_QUANTUM_WORK",
              "QUANTUM_IR",
              ChemistryWorkflowStoragePlanner.plannedQuantumProgramPayload(quantumProgramId),
              null,
              List.of("PLANNED_DEFERRED"),
              "Deferred Quantum IR preparation target reserved by computation workflow."));
    }
    return List.copyOf(records);
  }

  private static boolean containsQuantumProgramRecord(
      final List<ChemistryStorageQuantumProgramRecord> records, final String quantumProgramId) {
    for (int i = 0; i < records.size(); ++i) {
      if (!records.get(i).id().equals(quantumProgramId)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static String plannedQuantumProgramPayload(final String quantumProgramId) {
    return "artifact "
        + quantumProgramId
        + "\nkind PLANNED_QUANTUM_WORK"
        + "\nmapping none"
        + "\ncircuits 0"
        + "\noperations 0";
  }

  private static List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords(
      final String jobId, final ChemistryQuantumProgramPackage pack) {
    if (pack == null || !pack.hasArtifacts()) {
      return List.of();
    }
    final List<ChemistryQuantumProgramArtifact> artifacts = pack.artifacts();
    final ArrayList<ChemistryStorageQuantumProgramRecord> records =
        new ArrayList<ChemistryStorageQuantumProgramRecord>(artifacts.size());
    for (int i = 0; i < artifacts.size(); ++i) {
      final ChemistryQuantumProgramArtifact artifact = artifacts.get(i);
      final QuantumIrWriteResult writeResult = QuantumIrFiles.writeToString(artifact.program());
      records.add(
          ChemistryStorageQuantumProgramRecord.of(
              ChemistryWorkflowStoragePlanner.safeId(artifact.id()),
              jobId + "_computation_plan_0",
              0,
              artifact.kind().name(),
              artifact.hasMapping() ? artifact.mapping().name() : "QUANTUM_IR",
              ChemistryWorkflowStoragePlanner.quantumProgramNativePayload(artifact, writeResult),
              null,
              ChemistryWorkflowStoragePlanner.quantumProgramDiagnostics(artifact, writeResult),
              artifact.description()));
    }
    return List.copyOf(records);
  }

  private static String quantumProgramNativePayload(
      final ChemistryQuantumProgramArtifact artifact, final QuantumIrWriteResult result) {
    if (result.isSuccess()) {
      return "format quantum-ir-json"
          + "\nartifact "
          + ChemistryWorkflowStoragePlanner.safeId(artifact.id())
          + "\nkind "
          + artifact.kind().name()
          + "\nmapping "
          + (artifact.hasMapping() ? artifact.mapping().name() : "none")
          + "\ncircuits "
          + artifact.circuitCount()
          + "\noperations "
          + artifact.operationCount()
          + "\npayload"
          + "\n"
          + result.content();
    }
    return "format quantum-ir-unavailable"
        + "\nartifact "
        + ChemistryWorkflowStoragePlanner.safeId(artifact.id())
        + "\nkind "
        + artifact.kind().name()
        + "\nmapping "
        + (artifact.hasMapping() ? artifact.mapping().name() : "none")
        + "\ncircuits "
        + artifact.circuitCount()
        + "\noperations "
        + artifact.operationCount();
  }

  private static List<String> quantumProgramDiagnostics(
      final ChemistryQuantumProgramArtifact artifact, final QuantumIrWriteResult result) {
    final List<PersistenceDiagnostic> diagnostics = result.diagnostics();
    final ArrayList<String> rows = new ArrayList<String>(diagnostics.size() + 2);
    rows.add("CIRCUITS:" + artifact.circuitCount());
    rows.add("OPERATIONS:" + artifact.operationCount());
    for (int i = 0; i < diagnostics.size(); ++i) {
      rows.add(
          diagnostics.get(i).severity().name()
              + ":"
              + diagnostics.get(i).code().name()
              + ":"
              + diagnostics.get(i).message());
    }
    if (!result.isSuccess()) {
      rows.add("NATIVE_PAYLOAD_WRITE_FAILED");
    }
    return List.copyOf(rows);
  }

  private static List<ChemistryStorageExtension> extensions(
      final String jobId,
      final List<String> quantumProgramIds,
      final VqeOptimizationResult vqeOptimizationResult,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final List<ChemistryWorkflowCandidateTrace> candidateTraces) {
    final ArrayList<ChemistryStorageExtension> extensions =
        new ArrayList<ChemistryStorageExtension>();
    if (vqeOptimizationResult != null) {
      extensions.add(ChemistryWorkflowStoragePlanner.vqeExtension(jobId, vqeOptimizationResult));
    }
    final ChemistryStorageExtension scenarioPlanExtension =
        ChemistryWorkflowStoragePlanner.conditionScenarioPlanExtension(
            jobId, conditionScenarioPlan);
    if (scenarioPlanExtension != null) {
      extensions.add(scenarioPlanExtension);
    }
    final ChemistryStorageExtension scenarioEvaluationExtension =
        ChemistryWorkflowStoragePlanner.conditionScenarioEvaluationExtension(
            jobId, conditionScenarioEvaluationPlan);
    if (scenarioEvaluationExtension != null) {
      extensions.add(scenarioEvaluationExtension);
    }
    final ChemistryStorageExtension quantumProgramExtension =
        ChemistryWorkflowStoragePlanner.quantumProgramExtension(jobId, quantumProgramIds);
    if (quantumProgramExtension != null) {
      extensions.add(quantumProgramExtension);
    }
    final ChemistryStorageExtension candidateTraceExtension =
        ChemistryWorkflowStoragePlanner.candidateTraceExtension(jobId, candidateTraces);
    if (candidateTraceExtension != null) {
      extensions.add(candidateTraceExtension);
    }
    return List.copyOf(extensions);
  }

  private static ChemistryStorageExtension candidateTraceExtension(
      final String jobId, final List<ChemistryWorkflowCandidateTrace> traces) {
    if (traces == null || traces.isEmpty()) {
      return null;
    }
    final ArrayList<String> body = new ArrayList<String>(traces.size());
    for (int i = 0; i < traces.size(); ++i) {
      final ChemistryWorkflowCandidateTrace trace = traces.get(i);
      body.add(
          "candidate["
              + trace.candidateIndex()
              + "] probability="
              + trace.probability()
              + " yieldPercent="
              + trace.expectedYieldPercent()
              + " parity="
              + trace.parityStatus().name()
              + " pathwaySteps="
              + trace.pathwayStepCount()
              + " evidence="
              + trace.evidenceCount()
              + " mechanisms="
              + trace.predictedMechanismCount()
              + " classicalPlans="
              + ChemistryWorkflowStoragePlanner.safeInline(
                  String.join(",", trace.classicalPlanIds()))
              + " classicalResults="
              + ChemistryWorkflowStoragePlanner.safeInline(
                  String.join(",", trace.classicalResultIds()))
              + " quantumPrograms="
              + ChemistryWorkflowStoragePlanner.safeInline(
                  String.join(",", trace.quantumProgramIds()))
              + " diagnostics="
              + trace.diagnosticCount()
              + " blockingDiagnostics="
              + trace.blockingDiagnosticCount());
    }
    final LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
    properties.put("traceCount", Integer.toString(traces.size()));
    return ChemistryStorageExtension.of(
        "workflow_candidate_trace", jobId + "_candidate_trace_0", properties, body);
  }

  private static ChemistryStorageExtension conditionScenarioPlanExtension(
      final String jobId, final ReactionConditionScenarioPlan plan) {
    if (plan == null || plan.empty()) {
      return null;
    }
    final List<ReactionConditionScenario> scenarios = plan.scenarios();
    final ArrayList<String> body = new ArrayList<String>(scenarios.size());
    for (int i = 0; i < scenarios.size(); ++i) {
      final ReactionConditionScenario scenario = scenarios.get(i);
      body.add(
          "scenario["
              + i
              + "] id="
              + ChemistryWorkflowStoragePlanner.safeId(scenario.id())
              + " kind="
              + scenario.kind().name()
              + " rationale="
              + ChemistryWorkflowStoragePlanner.safeInline(scenario.rationale())
              + " conditions="
              + ChemistryWorkflowStoragePlanner.safeInline(
                  ChemistryWorkflowStoragePlanner.conditionSummary(scenario.conditions())));
    }
    final LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
    properties.put("requestId", plan.requestId());
    properties.put("scenarioCount", Integer.toString(scenarios.size()));
    return ChemistryStorageExtension.of(
        "reaction_condition_scenario_plan", jobId + "_condition_scenario_plan_0", properties, body);
  }

  private static ChemistryStorageExtension conditionScenarioEvaluationExtension(
      final String jobId, final ReactionConditionScenarioEvaluationPlan plan) {
    if (plan == null || plan.empty()) {
      return null;
    }
    final List<ReactionConditionScenarioEvaluation> evaluations = plan.rankedEvaluations();
    final ArrayList<String> body = new ArrayList<String>(evaluations.size());
    for (int i = 0; i < evaluations.size(); ++i) {
      final ReactionConditionScenarioEvaluation evaluation = evaluations.get(i);
      body.add(
          "evaluation["
              + i
              + "] id="
              + ChemistryWorkflowStoragePlanner.safeId(evaluation.scenario().id())
              + " kind="
              + evaluation.scenario().kind().name()
              + " score="
              + evaluation.rankingScore()
              + " bestProbability="
              + evaluation.bestProbability()
              + " bestYield="
              + evaluation.bestExpectedYieldPercent()
              + " probabilityDelta="
              + evaluation.baselineProbabilityDelta()
              + " yieldDelta="
              + evaluation.baselineExpectedYieldDelta()
              + " probabilityMass="
              + evaluation.probabilityMass()
              + " diagnostics="
              + evaluation.diagnosticCount()
              + " improvesBaseline="
              + evaluation.improvesBaseline()
              + " worsensBaseline="
              + evaluation.worsensBaseline());
    }
    final ReactionConditionScenarioEvaluation bestEvaluation = plan.bestEvaluation();
    final LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
    properties.put("requestId", plan.requestId());
    properties.put("evaluationCount", Integer.toString(plan.evaluations().size()));
    properties.put("improvingCount", Integer.toString(plan.improvingEvaluations().size()));
    properties.put("worseningCount", Integer.toString(plan.worseningEvaluations().size()));
    properties.put("bestScenarioId", bestEvaluation.scenario().id());
    properties.put("bestScenarioKind", bestEvaluation.scenario().kind().name());
    properties.put("bestRankingScore", Double.toString(bestEvaluation.rankingScore()));
    return ChemistryStorageExtension.of(
        "reaction_condition_scenario_evaluation",
        jobId + "_condition_scenario_evaluation_0",
        properties,
        body);
  }

  private static String conditionSummary(final ReactionConditions conditions) {
    if (conditions == null || conditions.empty()) {
      return "empty";
    }
    final ArrayList<String> parts = new ArrayList<String>();
    if (conditions.temperature() != null) {
      parts.add(
          "temperature="
              + conditions.temperature().value()
              + conditions.temperature().unit().name());
      parts.add("kelvin=" + conditions.temperature().kelvin());
    }
    if (conditions.pressure() != null) {
      parts.add("pressure=" + conditions.pressure().value() + conditions.pressure().unit().name());
    }
    if (conditions.duration() != null) {
      parts.add("duration=" + conditions.duration().value() + conditions.duration().unit().name());
      parts.add("hours=" + conditions.duration().hours());
    }
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      parts.add("component" + i + "=" + component.role().name() + ":" + component.name());
    }
    final List<ReactionConditionFeature> features = conditions.features();
    for (int i = 0; i < features.size(); ++i) {
      parts.add("feature" + i + "=" + features.get(i).kind().name());
    }
    if (conditions.note() != null) {
      parts.add("note=" + conditions.note());
    }
    return String.join(";", parts);
  }

  private static String safeInline(final String value) {
    if (value == null || value.isBlank()) {
      return "none";
    }
    return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
  }

  private static ChemistryStorageExtension quantumProgramExtension(
      final String jobId, final List<String> quantumProgramIds) {
    if (quantumProgramIds == null || quantumProgramIds.isEmpty()) {
      return null;
    }
    final ArrayList<String> body = new ArrayList<String>();
    for (int i = 0; i < quantumProgramIds.size(); ++i) {
      final String id = quantumProgramIds.get(i);
      if (id == null || id.isBlank()) {
        continue;
      }
      body.add("program[" + i + "]=" + ChemistryWorkflowStoragePlanner.safeId(id));
    }
    if (body.isEmpty()) {
      return null;
    }
    final LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
    properties.put("programCount", Integer.toString(body.size()));
    return ChemistryStorageExtension.of(
        "quantum_program_package", jobId + "_quantum_program_package_0", properties, body);
  }

  private static ChemistryStorageExtension vqeExtension(
      final String jobId, final VqeOptimizationResult result) {
    final LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
    properties.put("status", result.status().name());
    properties.put("bestEnergy", Double.toString(result.bestEnergy()));
    properties.put("evaluationCount", Long.toString(result.evaluationCount()));
    properties.put("failedEvaluationCount", Long.toString(result.failedEvaluationCount()));
    properties.put("totalShots", Long.toString(result.totalShots()));
    properties.put("diagnostic", result.diagnostic());
    final ArrayList<String> body = new ArrayList<String>();
    final double[] bestParameters = result.bestParametersCopy();
    for (int i = 0; i < bestParameters.length; ++i) {
      body.add("parameter[" + i + "]=" + bestParameters[i]);
    }
    final List<VqeIteration> iterations = result.iterations();
    for (int i = 0; i < iterations.size(); ++i) {
      final VqeIteration iteration = iterations.get(i);
      body.add(
          "iteration["
              + iteration.index()
              + "] energy="
              + iteration.energy()
              + " uncertainty="
              + iteration.uncertainty()
              + " step="
              + iteration.stepSize()
              + " gradient="
              + iteration.gradientNorm()
              + " accepted="
              + iteration.acceptedImprovement());
    }
    return ChemistryStorageExtension.of(
        "quantum_vqe_result", jobId + "_vqe_result_0", properties, body);
  }

  private List<ChemistryStorageReactionOutcomeRecord> outcomeRecords(
      final String jobId,
      final String reactionId,
      final ReactionPredictionResult predictionResult,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final List<String> quantumProgramIds,
      final ChemistryStorageComputationPlanRecord computationPlanRecord) {
    final List<ReactionProductCandidate> candidates = predictionResult.candidates();
    final ArrayList<ChemistryStorageReactionOutcomeRecord> records =
        new ArrayList<ChemistryStorageReactionOutcomeRecord>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      records.add(
          this.storageService.reactionOutcomeRecord(
              jobId + "_outcome_" + i,
              reactionId,
              0,
              ChemistryStorageReactionOutcomeKind.PREDICTED,
              candidates.get(i),
              ChemistryWorkflowStoragePlanner.classicalResultIdsForCandidate(
                  i, computationPlanRecord.classicalResultIds()),
              ChemistryWorkflowStoragePlanner.quantumProgramIdsForCandidate(
                  jobId,
                  recommendationPlan,
                  quantumProgramIds,
                  i,
                  computationPlanRecord.quantumProgramIds()),
              "Predicted under workflow reaction conditions.",
              "Rank " + i + " product outcome preserved for history rewind."));
    }
    return List.copyOf(records);
  }

  private static List<String> classicalResultIdsForCandidate(
      final int candidateIndex, final List<String> classicalResultIds) {
    if (classicalResultIds == null || classicalResultIds.isEmpty()) {
      return List.of();
    }
    final List<String> candidateMarkedIds =
        ChemistryWorkflowStoragePlanner.idsForCandidateMarker(candidateIndex, classicalResultIds);
    if (!candidateMarkedIds.isEmpty()
        || ChemistryWorkflowStoragePlanner.hasCandidateMarkers(classicalResultIds)) {
      return candidateMarkedIds;
    }
    return candidateIndex == 0 ? List.copyOf(classicalResultIds) : List.of();
  }

  private static Reaction predictedReaction(
      final String jobId, final ReactionPredictionResult result) {
    return Reaction.of(
        ReactionId.of(jobId + "_predicted_reaction_0"),
        "Predicted reaction for " + result.request().id(),
        result.request().reactants(),
        result.bestCandidate().products(),
        result.request().conditions(),
        ChemistryMetadata.EMPTY);
  }

  private static List<String> classicalPlanIds(final ChemistryWorkflowRecommendationPlan plan) {
    if (plan == null || plan.empty()) {
      return List.of();
    }
    final List<ChemistryWorkflowRecommendation> recommendations = plan.recommendations();
    final ArrayList<String> ids = new ArrayList<String>(recommendations.size());
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (!recommendation.hasCalculationKind()) {
        continue;
      }
      ids.add(
          ChemistryWorkflowStoragePlanner.recommendationPlanId(
              plan.workflowJobId(), recommendation, i));
    }
    return List.copyOf(ids);
  }

  private static List<String> quantumProgramIdsForCandidate(
      final String jobId,
      final ChemistryWorkflowRecommendationPlan plan,
      final List<String> explicitQuantumProgramIds,
      final int candidateIndex,
      final List<String> fallbackQuantumIds) {
    final ArrayList<String> ids = new ArrayList<String>();
    if (plan != null && !plan.empty()) {
      final List<ChemistryWorkflowRecommendation> recommendations = plan.recommendations();
      for (int i = 0; i < recommendations.size(); ++i) {
        final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
        if (recommendation.kind() != ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION
            || !ChemistryWorkflowStoragePlanner.recommendationAppliesToCandidate(
                recommendation, candidateIndex)) {
          continue;
        }
        ids.add(ChemistryWorkflowStoragePlanner.safeId(jobId + "_quantum_preparation_" + i));
      }
    }
    if (ids.isEmpty() && explicitQuantumProgramIds != null && candidateIndex == 0) {
      for (int i = 0; i < explicitQuantumProgramIds.size(); ++i) {
        final String id = explicitQuantumProgramIds.get(i);
        if (id == null || id.isBlank()) {
          continue;
        }
        ids.add(ChemistryWorkflowStoragePlanner.safeId(id));
      }
    }
    if (!ids.isEmpty()) {
      return List.copyOf(ids);
    }
    if (fallbackQuantumIds == null || fallbackQuantumIds.isEmpty()) {
      return List.of();
    }
    final List<String> candidateMarkedIds =
        ChemistryWorkflowStoragePlanner.idsForCandidateMarker(candidateIndex, fallbackQuantumIds);
    if (!candidateMarkedIds.isEmpty()
        || ChemistryWorkflowStoragePlanner.hasCandidateMarkers(fallbackQuantumIds)) {
      return candidateMarkedIds;
    }
    return candidateIndex == 0 ? List.copyOf(fallbackQuantumIds) : List.of();
  }

  private static List<String> idsForCandidateMarker(
      final int candidateIndex, final List<String> ids) {
    final ArrayList<String> matches = new ArrayList<String>();
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryWorkflowStoragePlanner.candidateMarkerIndex(ids.get(i)) != candidateIndex) {
        continue;
      }
      matches.add(ids.get(i));
    }
    return List.copyOf(matches);
  }

  private static boolean hasCandidateMarkers(final List<String> ids) {
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryWorkflowStoragePlanner.candidateMarkerIndex(ids.get(i)) >= 0) {
        return true;
      }
    }
    return false;
  }

  private static int candidateMarkerIndex(final String value) {
    if (value == null) {
      return -1;
    }
    final String normalized = value.toLowerCase(Locale.ROOT);
    final int marker = normalized.indexOf("candidate_");
    if (marker < 0) {
      return -1;
    }
    int position = marker + "candidate_".length();
    int parsed = 0;
    boolean hasDigit = false;
    while (position < normalized.length()) {
      final char digit = normalized.charAt(position);
      if (digit < '0' || digit > '9') {
        break;
      }
      hasDigit = true;
      parsed = parsed * 10 + digit - '0';
      ++position;
    }
    return hasDigit ? parsed : -1;
  }

  private static boolean recommendationAppliesToCandidate(
      final ChemistryWorkflowRecommendation recommendation, final int candidateIndex) {
    return recommendation.appliesToCandidate()
        ? recommendation.candidateIndex() == candidateIndex
        : candidateIndex == 0;
  }

  private static String recommendationPlanId(
      final String workflowJobId,
      final ChemistryWorkflowRecommendation recommendation,
      final int recommendationIndex) {
    final String candidateMarker =
        recommendation.appliesToCandidate()
            ? "candidate_" + recommendation.candidateIndex()
            : "program";
    return ChemistryWorkflowStoragePlanner.safeId(
        workflowJobId
            + "_classical_"
            + candidateMarker
            + "_"
            + recommendationIndex
            + "_"
            + recommendation.calculationKind().name().toLowerCase(Locale.ROOT));
  }

  private static List<String> classicalResultIds(final String classicalResultId) {
    if (classicalResultId == null || classicalResultId.isBlank()) {
      return List.of();
    }
    return List.of(ChemistryWorkflowStoragePlanner.safeId(classicalResultId));
  }

  private static List<String> classicalResultIds(
      final String primaryClassicalResultId,
      final List<ClassicalCalculationResult> classicalCalculationResults,
      final ChemistryComputationParityPlan computationParityPlan) {
    final ArrayList<String> ids = new ArrayList<String>();
    ChemistryWorkflowStoragePlanner.addId(ids, primaryClassicalResultId);
    if (classicalCalculationResults != null) {
      for (int i = 0; i < classicalCalculationResults.size(); ++i) {
        ChemistryWorkflowStoragePlanner.addId(
            ids, classicalCalculationResults.get(i).calculationId().value());
      }
    }
    if (computationParityPlan != null && !computationParityPlan.empty()) {
      final List<ChemistryComputationParityItem> items = computationParityPlan.items();
      for (int itemIndex = 0; itemIndex < items.size(); ++itemIndex) {
        final List<String> parityIds = items.get(itemIndex).classicalResultIds();
        for (int idIndex = 0; idIndex < parityIds.size(); ++idIndex) {
          ChemistryWorkflowStoragePlanner.addId(ids, parityIds.get(idIndex));
        }
      }
    }
    return List.copyOf(ids);
  }

  private static List<ChemistryStorageClassicalResultRecord> classicalResultRecords(
      final String jobId, final ClassicalCalculationResult result) {
    if (result == null) {
      return List.of();
    }
    final List<ClassicalDiagnostic> diagnostics = result.diagnostics().diagnostics();
    return List.of(
        ChemistryStorageClassicalResultRecord.of(
            ChemistryWorkflowStoragePlanner.safeId(result.calculationId().value()),
            result.calculationId().value(),
            0,
            result.status().name(),
            diagnostics.size(),
            ChemistryWorkflowStoragePlanner.diagnosticCount(
                diagnostics, ClassicalDiagnosticSeverity.ERROR),
            ChemistryWorkflowStoragePlanner.diagnosticCount(
                diagnostics, ClassicalDiagnosticSeverity.WARNING),
            result.coverageReport().status().name(),
            ChemistryWorkflowStoragePlanner.classicalCoverageItems(result.coverageReport().items()),
            ChemistryWorkflowStoragePlanner.classicalResultSections(result.sections()),
            "Portable classical calculation result stored for reaction history rewind."));
  }

  private static List<ChemistryStorageClassicalResultRecord> classicalResultRecords(
      final String jobId, final List<ClassicalCalculationResult> results) {
    if (results == null || results.isEmpty()) {
      return List.of();
    }
    final ArrayList<ChemistryStorageClassicalResultRecord> records =
        new ArrayList<ChemistryStorageClassicalResultRecord>(results.size());
    for (int i = 0; i < results.size(); ++i) {
      records.addAll(ChemistryWorkflowStoragePlanner.classicalResultRecords(jobId, results.get(i)));
    }
    return List.copyOf(records);
  }

  private static List<ClassicalCalculationResult> allClassicalResults(
      final ClassicalCalculationResult primaryResult,
      final List<ClassicalCalculationResult> followUpResults) {
    final ArrayList<ClassicalCalculationResult> results =
        new ArrayList<ClassicalCalculationResult>();
    final ArrayList<String> ids = new ArrayList<String>();
    ChemistryWorkflowStoragePlanner.addClassicalResult(results, ids, primaryResult);
    if (followUpResults != null) {
      for (int i = 0; i < followUpResults.size(); ++i) {
        ChemistryWorkflowStoragePlanner.addClassicalResult(results, ids, followUpResults.get(i));
      }
    }
    return List.copyOf(results);
  }

  private static void addClassicalResult(
      final List<ClassicalCalculationResult> results,
      final List<String> ids,
      final ClassicalCalculationResult result) {
    if (result == null) {
      return;
    }
    final String id = ChemistryWorkflowStoragePlanner.safeId(result.calculationId().value());
    if (ids.contains(id)) {
      return;
    }
    ids.add(id);
    results.add(result);
  }

  private static List<ChemistryStorageClassicalCoverageItemRecord> classicalCoverageItems(
      final List<ClassicalCalculationCoverageItem> items) {
    final ArrayList<ChemistryStorageClassicalCoverageItemRecord> records =
        new ArrayList<ChemistryStorageClassicalCoverageItemRecord>(items.size());
    for (int i = 0; i < items.size(); ++i) {
      final ClassicalCalculationCoverageItem item = items.get(i);
      records.add(
          ChemistryStorageClassicalCoverageItemRecord.of(
              item.kind().name(), item.status().name(), item.model()));
    }
    return List.copyOf(records);
  }

  private static int diagnosticCount(
      final List<ClassicalDiagnostic> diagnostics, final ClassicalDiagnosticSeverity severity) {
    int count = 0;
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i).severity() == severity) {
        ++count;
      }
    }
    return count;
  }

  private static List<ChemistryStorageClassicalResultSectionRecord> classicalResultSections(
      final List<ClassicalResultSection> sections) {
    if (sections.isEmpty()) {
      return List.of();
    }
    final ArrayList<ChemistryStorageClassicalResultSectionRecord> records =
        new ArrayList<ChemistryStorageClassicalResultSectionRecord>(sections.size());
    for (int i = 0; i < sections.size(); ++i) {
      final ClassicalResultSection section = sections.get(i);
      records.add(
          ChemistryStorageClassicalResultSectionRecord.of(
              section.kind().name(),
              section.title(),
              ChemistryWorkflowStoragePlanner.classicalResultValues(section.values())));
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageClassicalResultValueRecord> classicalResultValues(
      final List<ClassicalResultValue> values) {
    if (values.isEmpty()) {
      return List.of();
    }
    final ArrayList<ChemistryStorageClassicalResultValueRecord> records =
        new ArrayList<ChemistryStorageClassicalResultValueRecord>(values.size());
    for (int i = 0; i < values.size(); ++i) {
      final ClassicalResultValue value = values.get(i);
      records.add(
          ChemistryStorageClassicalResultValueRecord.of(
              value.name(), value.kind().name(), value.unit(), value.values()));
    }
    return List.copyOf(records);
  }

  private static List<String> quantumProgramIds(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan plan,
      final List<String> quantumProgramIds) {
    return ChemistryWorkflowStoragePlanner.quantumProgramIds(
        job, plan, quantumProgramIds, ChemistryComputationParityPlan.EMPTY);
  }

  private static List<String> quantumProgramIds(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan plan,
      final List<String> quantumProgramIds,
      final ChemistryComputationParityPlan computationParityPlan) {
    final ArrayList<String> ids = new ArrayList<String>();
    if (quantumProgramIds != null) {
      for (int i = 0; i < quantumProgramIds.size(); ++i) {
        ChemistryWorkflowStoragePlanner.addId(ids, quantumProgramIds.get(i));
      }
    }
    if (plan != null && !plan.empty()) {
      final List<ChemistryWorkflowRecommendation> recommendations = plan.recommendations();
      for (int i = 0; i < recommendations.size(); ++i) {
        if (recommendations.get(i).kind()
            != ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION) {
          continue;
        }
        ids.add(
            ChemistryWorkflowStoragePlanner.safeId(
                plan.workflowJobId() + "_quantum_preparation_" + i));
      }
    }
    if (computationParityPlan != null && !computationParityPlan.empty()) {
      final List<ChemistryComputationParityItem> items = computationParityPlan.items();
      for (int itemIndex = 0; itemIndex < items.size(); ++itemIndex) {
        final List<String> parityIds = items.get(itemIndex).quantumProgramIds();
        for (int idIndex = 0; idIndex < parityIds.size(); ++idIndex) {
          ChemistryWorkflowStoragePlanner.addId(ids, parityIds.get(idIndex));
        }
      }
    }
    return List.copyOf(ids);
  }

  private static void addId(final List<String> ids, final String id) {
    if (id == null || id.isBlank()) {
      return;
    }
    final String safeId = ChemistryWorkflowStoragePlanner.safeId(id);
    if (ids.contains(safeId)) {
      return;
    }
    ids.add(safeId);
  }

  private static ChemistryStorageRoadmapRecord roadmapRecord(
      final String jobId,
      final ReactionPredictionResult predictionResult,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final List<String> quantumProgramIds,
      final int conditionScenarioCount,
      final int evaluatedConditionScenarioCount) {
    return ChemistryStorageRoadmapRecord.of(
        jobId + "_roadmap_0",
        predictionResult.request().id(),
        0,
        ChemistryStorageReactionRecordStatus.PLANNED.name(),
        conditionScenarioCount,
        evaluatedConditionScenarioCount,
        ChemistryWorkflowStoragePlanner.roadmapCandidateRecords(
            jobId, predictionResult, recommendationPlan, quantumProgramIds),
        ChemistryWorkflowStoragePlanner.programLevelRecommendationTargets(recommendationPlan),
        "Workflow roadmap preserving product candidates, follow-up calculations and Quantum IR targets.");
  }

  private static List<ChemistryStorageRoadmapCandidateRecord> roadmapCandidateRecords(
      final String jobId,
      final ReactionPredictionResult predictionResult,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final List<String> quantumProgramIds) {
    final List<ReactionProductCandidate> candidates = predictionResult.candidates();
    final ArrayList<ChemistryStorageRoadmapCandidateRecord> records =
        new ArrayList<ChemistryStorageRoadmapCandidateRecord>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      records.add(
          ChemistryStorageRoadmapCandidateRecord.of(
              i,
              candidate.probability(),
              candidate.expectedYieldPercent(),
              ChemistryWorkflowStoragePlanner.productMoleculeIds(candidate),
              ChemistryWorkflowStoragePlanner.productFormulaSignatures(candidate),
              ChemistryWorkflowStoragePlanner.productStoichiometrySignatures(candidate),
              ChemistryWorkflowStoragePlanner.mechanismIds(candidate),
              ChemistryWorkflowStoragePlanner.mechanismDescriptors(candidate),
              ChemistryWorkflowStoragePlanner.classicalCalculationKindNames(recommendationPlan, i),
              ChemistryWorkflowStoragePlanner.classicalPlanIdsForRoadmapCandidate(
                  jobId, recommendationPlan, i),
              ChemistryWorkflowStoragePlanner.followUpActionTargets(recommendationPlan, i),
              ChemistryWorkflowStoragePlanner.quantumPreparationTargets(
                  jobId, recommendationPlan, quantumProgramIds, candidate, i),
              ChemistryWorkflowStoragePlanner.explicitProductGraph(candidate),
              ChemistryWorkflowStoragePlanner.electronicRerankReady(candidate),
              ChemistryWorkflowStoragePlanner.quantumWorkflowReady(candidate),
              ChemistryWorkflowStoragePlanner.readinessReason(candidate)));
    }
    return List.copyOf(records);
  }

  private static boolean explicitProductGraph(final ReactionProductCandidate candidate) {
    return !candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)
        && !candidate.products().participants().isEmpty();
  }

  private static boolean electronicRerankReady(final ReactionProductCandidate candidate) {
    return ChemistryWorkflowStoragePlanner.explicitProductGraph(candidate)
        && candidate.products().participantCount() > 0;
  }

  private static boolean quantumWorkflowReady(final ReactionProductCandidate candidate) {
    return ChemistryWorkflowStoragePlanner.electronicRerankReady(candidate)
        && candidate.hasPredictedMechanisms()
        && candidate.hasScoreBreakdown();
  }

  private static String readinessReason(final ReactionProductCandidate candidate) {
    if (!ChemistryWorkflowStoragePlanner.explicitProductGraph(candidate)) {
      return "missing_explicit_product_graph";
    }
    if (!candidate.hasPredictedMechanisms()) {
      return "missing_mechanism_trace";
    }
    if (!candidate.hasScoreBreakdown()) {
      return "missing_score_breakdown";
    }
    return "ready";
  }

  private static List<String> productMoleculeIds(final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    final ArrayList<String> ids = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      ids.add(participants.get(i).molecule().id().value());
    }
    return List.copyOf(ids);
  }

  private static List<String> productFormulaSignatures(final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    final ArrayList<String> formulas = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      formulas.add(participants.get(i).molecule().formula().hillNotation());
    }
    return List.copyOf(formulas);
  }

  private static List<String> productStoichiometrySignatures(
      final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    final ArrayList<String> signatures = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      signatures.add(
          participant.coefficient().value()
              + "*"
              + participant.molecule().id().value()
              + ":"
              + participant.molecule().formula().hillNotation());
    }
    return List.copyOf(signatures);
  }

  private static List<String> mechanismIds(final ReactionProductCandidate candidate) {
    if (!candidate.hasPredictedMechanisms()) {
      return List.of();
    }
    final ArrayList<String> ids = new ArrayList<String>(candidate.predictedMechanisms().size());
    for (int i = 0; i < candidate.predictedMechanisms().size(); ++i) {
      ids.add(candidate.predictedMechanisms().get(i).mechanismId());
    }
    return List.copyOf(ids);
  }

  private static List<String> mechanismDescriptors(final ReactionProductCandidate candidate) {
    if (!candidate.hasPredictedMechanisms()) {
      return List.of();
    }
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    final ArrayList<String> descriptors = new ArrayList<String>(mechanisms.size());
    for (int i = 0; i < mechanisms.size(); ++i) {
      descriptors.add(ChemistryWorkflowStoragePlanner.mechanismDescriptor(mechanisms.get(i)));
    }
    return List.copyOf(descriptors);
  }

  private static String mechanismDescriptor(final PredictedReactionMechanism mechanism) {
    final StringBuilder builder =
        new StringBuilder(mechanism.mechanismId())
            .append(":")
            .append(mechanism.family().name())
            .append(":p=")
            .append(mechanism.pathwayProbability())
            .append(":yield=")
            .append(mechanism.expectedYieldPercent());
    if (!mechanism.hasDetailedMechanism()) {
      return builder.toString();
    }
    final ReactionMechanism detailed = mechanism.detailedMechanism();
    builder
        .append(":steps=")
        .append(detailed.steps().size())
        .append(":events=")
        .append(detailed.eventCount())
        .append(":energyPoints=")
        .append(detailed.energyProfile().size())
        .append(":trace=")
        .append(ChemistryWorkflowStoragePlanner.mechanismTrace(detailed));
    return builder.toString();
  }

  private static String mechanismTrace(final ReactionMechanism mechanism) {
    final StringBuilder builder = new StringBuilder();
    final List<ElementaryReactionStep> steps = mechanism.steps();
    for (int stepIndex = 0; stepIndex < steps.size(); ++stepIndex) {
      if (stepIndex > 0) {
        builder.append(">");
      }
      final ElementaryReactionStep step = steps.get(stepIndex);
      builder.append(step.id());
      final List<MechanisticEvent> events = step.events();
      if (events.isEmpty()) {
        continue;
      }
      builder.append("[");
      for (int eventIndex = 0; eventIndex < events.size(); ++eventIndex) {
        if (eventIndex > 0) {
          builder.append(",");
        }
        builder.append(events.get(eventIndex).kind().name());
      }
      builder.append("]");
    }
    return builder.toString();
  }

  private static List<String> classicalCalculationKindNames(
      final ChemistryWorkflowRecommendationPlan plan, final int candidateIndex) {
    if (plan == null || plan.empty()) {
      return List.of();
    }
    final List<ChemistryWorkflowRecommendation> recommendations =
        plan.recommendationsForCandidate(candidateIndex);
    final ArrayList<String> names = new ArrayList<String>(recommendations.size());
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (!recommendation.hasCalculationKind()) {
        continue;
      }
      names.add(recommendation.calculationKind().name());
    }
    return List.copyOf(names);
  }

  private static List<String> classicalPlanIdsForRoadmapCandidate(
      final String jobId,
      final ChemistryWorkflowRecommendationPlan plan,
      final int candidateIndex) {
    if (plan == null || plan.empty()) {
      return List.of();
    }
    final List<ChemistryWorkflowRecommendation> recommendations = plan.recommendations();
    final ArrayList<String> ids = new ArrayList<String>(recommendations.size());
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (!recommendation.hasCalculationKind()
          || !ChemistryWorkflowStoragePlanner.recommendationAppliesToCandidate(
              recommendation, candidateIndex)) {
        continue;
      }
      ids.add(ChemistryWorkflowStoragePlanner.recommendationPlanId(jobId, recommendation, i));
    }
    return List.copyOf(ids);
  }

  private static List<String> quantumPreparationTargets(
      final String jobId,
      final ChemistryWorkflowRecommendationPlan plan,
      final List<String> quantumProgramIds,
      final ReactionProductCandidate candidate,
      final int candidateIndex) {
    final ArrayList<String> targets = new ArrayList<String>();
    if (!ChemistryWorkflowStoragePlanner.quantumWorkflowReady(candidate)) {
      return List.of();
    }
    if (plan != null && !plan.empty()) {
      final List<ChemistryWorkflowRecommendation> recommendations =
          plan.recommendationsForCandidate(candidateIndex);
      for (int i = 0; i < recommendations.size(); ++i) {
        final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
        if (recommendation.kind() != ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION) {
          continue;
        }
        targets.add(ChemistryWorkflowStoragePlanner.safeId(recommendation.target()));
      }
    }
    if (targets.isEmpty() && quantumProgramIds != null) {
      for (int i = 0; i < quantumProgramIds.size(); ++i) {
        final String id = quantumProgramIds.get(i);
        if (id != null && !id.isBlank()) {
          targets.add(ChemistryWorkflowStoragePlanner.safeId(id));
        }
      }
    }
    if (targets.isEmpty()) {
      targets.add(jobId + "_candidate_" + candidateIndex + "_quantum_preparation");
    }
    return List.copyOf(targets);
  }

  private static List<String> followUpActionTargets(
      final ChemistryWorkflowRecommendationPlan plan, final int candidateIndex) {
    if (plan == null || plan.empty()) {
      return List.of();
    }
    final List<ChemistryWorkflowRecommendation> recommendations =
        plan.recommendationsForCandidate(candidateIndex);
    final ArrayList<String> targets = new ArrayList<String>(recommendations.size());
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (recommendation.kind() == ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION) {
        continue;
      }
      targets.add(recommendation.target());
    }
    return List.copyOf(targets);
  }

  private static List<String> programLevelRecommendationTargets(
      final ChemistryWorkflowRecommendationPlan plan) {
    if (plan == null || plan.empty()) {
      return List.of();
    }
    final ArrayList<String> targets = new ArrayList<String>();
    final List<ChemistryWorkflowRecommendation> recommendations = plan.recommendations();
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (recommendation.appliesToCandidate()) {
        continue;
      }
      targets.add(recommendation.target());
    }
    return List.copyOf(targets);
  }

  private static String historyDescription(final ReactionPredictionExplanation explanation) {
    if (explanation == null || !explanation.pathwayAssessment().hasPathwaySteps()) {
      return "Workflow recorded ranked products, classical calculation output and deferred Quantum IR work.";
    }
    final StringBuilder builder =
        new StringBuilder(
            "Workflow recorded ranked products, classical calculation output, deferred Quantum IR work and mechanism route: ");
    final List<ReactionPathwayStep> steps = explanation.pathwayAssessment().pathwaySteps();
    final int limit = Math.min(steps.size(), 12);
    for (int i = 0; i < limit; ++i) {
      if (i > 0) {
        builder.append(" -> ");
      }
      final ReactionPathwayStep step = steps.get(i);
      builder.append(step.eventKind().name());
    }
    if (steps.size() > limit) {
      builder.append(" -> ...");
    }
    return builder.toString();
  }

  private static List<Molecule> candidateProductMolecules(final ReactionPredictionResult result) {
    final ArrayList<Molecule> molecules = new ArrayList<Molecule>();
    final List<ReactionProductCandidate> candidates = result.candidates();
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final List<ReactionParticipant> participants =
          candidates.get(candidateIndex).products().participants();
      for (int participantIndex = 0; participantIndex < participants.size(); ++participantIndex) {
        molecules.add(participants.get(participantIndex).molecule());
      }
    }
    return List.copyOf(molecules);
  }

  private static List<Molecule> subjectMolecules(final ChemistryWorkflowJob job) {
    if (job.subject().subjectKind() == ChemistrySubjectKind.MOLECULE) {
      return List.of((Molecule) job.subject());
    }
    if (job.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
      final Reaction reaction = (Reaction) job.subject();
      final LinkedHashMap<String, Molecule> molecules = new LinkedHashMap<String, Molecule>();
      ChemistryWorkflowStoragePlanner.collectParticipantMolecules(
          reaction.reactants().participants(), molecules);
      ChemistryWorkflowStoragePlanner.collectParticipantMolecules(
          reaction.products().participants(), molecules);
      return List.copyOf(molecules.values());
    }
    return List.of();
  }

  private static void collectParticipantMolecules(
      final List<ReactionParticipant> participants,
      final LinkedHashMap<String, Molecule> molecules) {
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      molecules.putIfAbsent(molecule.id().value(), molecule);
    }
  }

  private static String safeId(final String value) {
    final StringBuilder builder = new StringBuilder(value.length());
    boolean previousUnderscore = false;
    for (int i = 0; i < value.length(); ++i) {
      final char symbol = Character.toLowerCase(value.charAt(i));
      final boolean identifierSymbol =
          (symbol >= 'a' && symbol <= 'z') || (symbol >= '0' && symbol <= '9') || symbol == '_';
      if (identifierSymbol) {
        builder.append(symbol);
        previousUnderscore = false;
      } else if (!previousUnderscore) {
        builder.append('_');
        previousUnderscore = true;
      }
    }
    if (builder.length() == 0 || !Character.isLetter(builder.charAt(0))) {
      builder.insert(0, "id_");
    }
    final int lastIndex = builder.length() - 1;
    if (builder.charAt(lastIndex) == '_') {
      builder.deleteCharAt(lastIndex);
    }
    return builder.toString();
  }
}
