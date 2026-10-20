/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicStructureProblem;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic.ChemistryWorkflowDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.followup.ChemistryFollowUpCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStage;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStageResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStageStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.storage.ChemistryWorkflowStoragePlan;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Неизменяемый результат chemistry workflow со всеми промежуточными артефактами.
 */
public final class ChemistryWorkflowResult {

  private final ChemistryWorkflowJob job;
  private final ChemistryWorkflowStatus status;
  private final ChemistryValidationResult coreValidationResult;
  private final ChemistryCapabilityReport capabilityReport;
  private final ClassicalPreflightResult classicalPreflightResult;
  private final ClassicalCalculationPlan classicalCalculationPlan;
  private final ClassicalCalculationResult classicalCalculationResult;
  private final ReactionPredictionResult reactionPredictionResult;
  private final ReactionPredictionExplanation reactionPredictionExplanation;
  private final ReactionConditionEffectProfile conditionEffectProfile;
  private final ReactionConditionScenarioPlan conditionScenarioPlan;
  private final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan;
  private final ChemistryWorkflowRecommendationPlan recommendationPlan;
  private final ChemistryFollowUpCalculationPlan followUpCalculationPlan;
  private final ElectronicStructureProblem electronicProblem;
  private final QuantumProgram quantumProgram;
  private final ChemistryQuantumProgramPackage quantumProgramPackage;
  private final ChemistryComputationParityPlan computationParityPlan;
  private final ErrorMitigationPlan errorMitigationPlan;
  private final VqeOptimizationResult vqeOptimizationResult;
  private final ChemistryWorkflowStoragePlan storagePlan;
  private final List<ChemistryWorkflowStageResult> stageResults;
  private final ChemistryWorkflowDiagnosticSet diagnostics;

  private ChemistryWorkflowResult(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowStatus status,
      final ChemistryValidationResult coreValidationResult,
      final ChemistryCapabilityReport capabilityReport,
      final ClassicalPreflightResult classicalPreflightResult,
      final ClassicalCalculationPlan classicalCalculationPlan,
      final ClassicalCalculationResult classicalCalculationResult,
      final ReactionPredictionResult reactionPredictionResult,
      final ReactionPredictionExplanation reactionPredictionExplanation,
      final ReactionConditionEffectProfile conditionEffectProfile,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ChemistryFollowUpCalculationPlan followUpCalculationPlan,
      final ElectronicStructureProblem electronicProblem,
      final QuantumProgram quantumProgram,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final ChemistryComputationParityPlan computationParityPlan,
      final ErrorMitigationPlan errorMitigationPlan,
      final VqeOptimizationResult vqeOptimizationResult,
      final ChemistryWorkflowStoragePlan storagePlan,
      final List<ChemistryWorkflowStageResult> stageResults,
      final ChemistryWorkflowDiagnosticSet diagnostics) {
    this.job = job;
    this.status = status;
    this.coreValidationResult = coreValidationResult;
    this.capabilityReport = capabilityReport;
    this.classicalPreflightResult = classicalPreflightResult;
    this.classicalCalculationPlan = classicalCalculationPlan;
    this.classicalCalculationResult = classicalCalculationResult;
    this.reactionPredictionResult = reactionPredictionResult;
    this.reactionPredictionExplanation = reactionPredictionExplanation;
    this.conditionEffectProfile = conditionEffectProfile;
    this.conditionScenarioPlan = conditionScenarioPlan;
    this.conditionScenarioEvaluationPlan = conditionScenarioEvaluationPlan;
    this.recommendationPlan = recommendationPlan;
    this.followUpCalculationPlan = followUpCalculationPlan;
    this.electronicProblem = electronicProblem;
    this.quantumProgram = quantumProgram;
    this.quantumProgramPackage = quantumProgramPackage;
    this.computationParityPlan = computationParityPlan;
    this.errorMitigationPlan = errorMitigationPlan;
    this.vqeOptimizationResult = vqeOptimizationResult;
    this.storagePlan = storagePlan;
    this.stageResults = stageResults;
    this.diagnostics = diagnostics;
  }

  public static ChemistryWorkflowResult of(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowStatus status,
      final ChemistryValidationResult coreValidationResult,
      final ClassicalPreflightResult classicalPreflightResult,
      final ClassicalCalculationPlan classicalCalculationPlan,
      final ElectronicStructureProblem electronicProblem,
      final QuantumProgram quantumProgram,
      final List<ChemistryWorkflowStageResult> stageResults,
      final ChemistryWorkflowDiagnosticSet diagnostics) {
    return ChemistryWorkflowResult.of(
        job,
        status,
        coreValidationResult,
        null,
        classicalPreflightResult,
        classicalCalculationPlan,
        null,
        null,
        null,
        null,
        null,
        null,
        ChemistryWorkflowRecommendationPlan.EMPTY,
        ChemistryFollowUpCalculationPlan.EMPTY,
        electronicProblem,
        quantumProgram,
        null,
        ChemistryComputationParityPlan.EMPTY,
        null,
        null,
        ChemistryWorkflowStoragePlan.EMPTY,
        stageResults,
        diagnostics);
  }

  public static ChemistryWorkflowResult of(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowStatus status,
      final ChemistryValidationResult coreValidationResult,
      final ClassicalPreflightResult classicalPreflightResult,
      final ClassicalCalculationPlan classicalCalculationPlan,
      final ReactionPredictionResult reactionPredictionResult,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ElectronicStructureProblem electronicProblem,
      final QuantumProgram quantumProgram,
      final List<ChemistryWorkflowStageResult> stageResults,
      final ChemistryWorkflowDiagnosticSet diagnostics) {
    return ChemistryWorkflowResult.of(
        job,
        status,
        coreValidationResult,
        null,
        classicalPreflightResult,
        classicalCalculationPlan,
        null,
        reactionPredictionResult,
        null,
        null,
        null,
        null,
        recommendationPlan,
        ChemistryFollowUpCalculationPlan.EMPTY,
        electronicProblem,
        quantumProgram,
        null,
        ChemistryComputationParityPlan.EMPTY,
        null,
        null,
        ChemistryWorkflowStoragePlan.EMPTY,
        stageResults,
        diagnostics);
  }

  public static ChemistryWorkflowResult of(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowStatus status,
      final ChemistryValidationResult coreValidationResult,
      final ChemistryCapabilityReport capabilityReport,
      final ClassicalPreflightResult classicalPreflightResult,
      final ClassicalCalculationPlan classicalCalculationPlan,
      final ClassicalCalculationResult classicalCalculationResult,
      final ReactionPredictionResult reactionPredictionResult,
      final ReactionPredictionExplanation reactionPredictionExplanation,
      final ReactionConditionEffectProfile conditionEffectProfile,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ChemistryFollowUpCalculationPlan followUpCalculationPlan,
      final ElectronicStructureProblem electronicProblem,
      final QuantumProgram quantumProgram,
      final ErrorMitigationPlan errorMitigationPlan,
      final List<ChemistryWorkflowStageResult> stageResults,
      final ChemistryWorkflowDiagnosticSet diagnostics) {
    return ChemistryWorkflowResult.of(
        job,
        status,
        coreValidationResult,
        capabilityReport,
        classicalPreflightResult,
        classicalCalculationPlan,
        classicalCalculationResult,
        reactionPredictionResult,
        reactionPredictionExplanation,
        conditionEffectProfile,
        conditionScenarioPlan,
        conditionScenarioEvaluationPlan,
        recommendationPlan,
        followUpCalculationPlan,
        electronicProblem,
        quantumProgram,
        null,
        ChemistryComputationParityPlan.EMPTY,
        errorMitigationPlan,
        null,
        ChemistryWorkflowStoragePlan.EMPTY,
        stageResults,
        diagnostics);
  }

  public static ChemistryWorkflowResult of(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowStatus status,
      final ChemistryValidationResult coreValidationResult,
      final ChemistryCapabilityReport capabilityReport,
      final ClassicalPreflightResult classicalPreflightResult,
      final ClassicalCalculationPlan classicalCalculationPlan,
      final ClassicalCalculationResult classicalCalculationResult,
      final ReactionPredictionResult reactionPredictionResult,
      final ReactionPredictionExplanation reactionPredictionExplanation,
      final ReactionConditionEffectProfile conditionEffectProfile,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ChemistryFollowUpCalculationPlan followUpCalculationPlan,
      final ElectronicStructureProblem electronicProblem,
      final QuantumProgram quantumProgram,
      final ErrorMitigationPlan errorMitigationPlan,
      final ChemistryWorkflowStoragePlan storagePlan,
      final List<ChemistryWorkflowStageResult> stageResults,
      final ChemistryWorkflowDiagnosticSet diagnostics) {
    return ChemistryWorkflowResult.of(
        job,
        status,
        coreValidationResult,
        capabilityReport,
        classicalPreflightResult,
        classicalCalculationPlan,
        classicalCalculationResult,
        reactionPredictionResult,
        reactionPredictionExplanation,
        conditionEffectProfile,
        conditionScenarioPlan,
        conditionScenarioEvaluationPlan,
        recommendationPlan,
        followUpCalculationPlan,
        electronicProblem,
        quantumProgram,
        null,
        ChemistryComputationParityPlan.EMPTY,
        errorMitigationPlan,
        null,
        storagePlan,
        stageResults,
        diagnostics);
  }

  public static ChemistryWorkflowResult of(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowStatus status,
      final ChemistryValidationResult coreValidationResult,
      final ChemistryCapabilityReport capabilityReport,
      final ClassicalPreflightResult classicalPreflightResult,
      final ClassicalCalculationPlan classicalCalculationPlan,
      final ClassicalCalculationResult classicalCalculationResult,
      final ReactionPredictionResult reactionPredictionResult,
      final ReactionPredictionExplanation reactionPredictionExplanation,
      final ReactionConditionEffectProfile conditionEffectProfile,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ChemistryFollowUpCalculationPlan followUpCalculationPlan,
      final ElectronicStructureProblem electronicProblem,
      final QuantumProgram quantumProgram,
      final ChemistryQuantumProgramPackage quantumProgramPackage,
      final ChemistryComputationParityPlan computationParityPlan,
      final ErrorMitigationPlan errorMitigationPlan,
      final VqeOptimizationResult vqeOptimizationResult,
      final ChemistryWorkflowStoragePlan storagePlan,
      final List<ChemistryWorkflowStageResult> stageResults,
      final ChemistryWorkflowDiagnosticSet diagnostics) {
    if (job == null) {
      throw new IllegalArgumentException("Chemistry workflow job must not be null.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Chemistry workflow status must not be null.");
    }
    if (stageResults == null || stageResults.isEmpty()) {
      throw new IllegalArgumentException("Chemistry workflow stage results must not be empty.");
    }
    return new ChemistryWorkflowResult(
        job,
        status,
        coreValidationResult,
        capabilityReport,
        classicalPreflightResult,
        classicalCalculationPlan,
        classicalCalculationResult,
        reactionPredictionResult,
        reactionPredictionExplanation,
        conditionEffectProfile,
        conditionScenarioPlan,
        conditionScenarioEvaluationPlan,
        recommendationPlan == null ? ChemistryWorkflowRecommendationPlan.EMPTY : recommendationPlan,
        followUpCalculationPlan == null
            ? ChemistryFollowUpCalculationPlan.EMPTY
            : followUpCalculationPlan,
        electronicProblem,
        quantumProgram,
        quantumProgramPackage == null
            ? ChemistryQuantumProgramPackage.EMPTY
            : quantumProgramPackage,
        computationParityPlan == null
            ? ChemistryComputationParityPlan.EMPTY
            : computationParityPlan,
        errorMitigationPlan,
        vqeOptimizationResult,
        storagePlan == null ? ChemistryWorkflowStoragePlan.EMPTY : storagePlan,
        List.copyOf(stageResults),
        diagnostics == null ? ChemistryWorkflowDiagnosticSet.EMPTY : diagnostics);
  }

  public ChemistryWorkflowResult withComputationParityAndStoragePlan(
      final ChemistryComputationParityPlan computationParityPlan,
      final ChemistryWorkflowStoragePlan storagePlan) {
    return ChemistryWorkflowResult.of(
        this.job,
        this.status,
        this.coreValidationResult,
        this.capabilityReport,
        this.classicalPreflightResult,
        this.classicalCalculationPlan,
        this.classicalCalculationResult,
        this.reactionPredictionResult,
        this.reactionPredictionExplanation,
        this.conditionEffectProfile,
        this.conditionScenarioPlan,
        this.conditionScenarioEvaluationPlan,
        this.recommendationPlan,
        this.followUpCalculationPlan,
        this.electronicProblem,
        this.quantumProgram,
        this.quantumProgramPackage,
        computationParityPlan,
        this.errorMitigationPlan,
        this.vqeOptimizationResult,
        storagePlan,
        this.stageResults,
        this.diagnostics);
  }

  public ChemistryWorkflowJob job() {
    return this.job;
  }

  public ChemistryWorkflowStatus status() {
    return this.status;
  }

  public ChemistryValidationResult coreValidationResult() {
    return this.coreValidationResult;
  }

  public ChemistryCapabilityReport capabilityReport() {
    return this.capabilityReport;
  }

  public boolean hasCapabilityReport() {
    return this.capabilityReport != null;
  }

  public ClassicalPreflightResult classicalPreflightResult() {
    return this.classicalPreflightResult;
  }

  public ClassicalCalculationPlan classicalCalculationPlan() {
    return this.classicalCalculationPlan;
  }

  public ClassicalCalculationResult classicalCalculationResult() {
    return this.classicalCalculationResult;
  }

  public boolean hasClassicalCalculationResult() {
    return this.classicalCalculationResult != null;
  }

  public ReactionPredictionResult reactionPredictionResult() {
    return this.reactionPredictionResult;
  }

  public boolean hasReactionPredictionResult() {
    return this.reactionPredictionResult != null;
  }

  public boolean hasReactionPredictionDiagnostics() {
    return this.reactionPredictionResult != null && this.reactionPredictionResult.hasDiagnostics();
  }

  public int reactionPredictionDiagnosticCount() {
    return this.reactionPredictionResult == null
        ? 0
        : this.reactionPredictionResult.diagnosticCount();
  }

  public int reactionPredictionDiagnosticCount(final ReactionPredictionEvidenceKind kind) {
    if (this.reactionPredictionResult == null) {
      return 0;
    }
    return this.reactionPredictionResult.diagnosticCount(kind);
  }

  public int reactionPredictionEvidenceCount(final ReactionPredictionEvidenceKind kind) {
    if (this.reactionPredictionResult == null) {
      return 0;
    }
    return this.reactionPredictionResult.evidenceCount(kind);
  }

  public double reactionPredictionEvidenceWeight(final ReactionPredictionEvidenceKind kind) {
    if (this.reactionPredictionResult == null) {
      return 0.0;
    }
    return this.reactionPredictionResult.evidenceWeight(kind);
  }

  public boolean reactionPredictionHasWeightedEvidence(final ReactionPredictionEvidenceKind kind) {
    return Math.abs(this.reactionPredictionEvidenceWeight(kind)) > 0.0;
  }

  public int predictedMechanismCount() {
    return this.reactionPredictionResult == null
        ? 0
        : this.reactionPredictionResult.predictedMechanismCount();
  }

  public ReactionPredictionExplanation reactionPredictionExplanation() {
    return this.reactionPredictionExplanation;
  }

  public boolean hasReactionPredictionExplanation() {
    return this.reactionPredictionExplanation != null;
  }

  public ReactionConditionEffectProfile conditionEffectProfile() {
    return this.conditionEffectProfile;
  }

  public boolean hasConditionEffectProfile() {
    return this.conditionEffectProfile != null && !this.conditionEffectProfile.empty();
  }

  public ReactionConditionScenarioPlan conditionScenarioPlan() {
    return this.conditionScenarioPlan;
  }

  public boolean hasConditionScenarioPlan() {
    return this.conditionScenarioPlan != null && !this.conditionScenarioPlan.empty();
  }

  public ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan() {
    return this.conditionScenarioEvaluationPlan;
  }

  public boolean hasConditionScenarioEvaluationPlan() {
    return this.conditionScenarioEvaluationPlan != null
        && !this.conditionScenarioEvaluationPlan.empty();
  }

  public ChemistryWorkflowRecommendationPlan recommendationPlan() {
    return this.recommendationPlan;
  }

  public boolean hasRecommendations() {
    return !this.recommendationPlan.empty();
  }

  public ChemistryFollowUpCalculationPlan followUpCalculationPlan() {
    return this.followUpCalculationPlan;
  }

  public boolean hasFollowUpCalculationPlan() {
    return !this.followUpCalculationPlan.empty();
  }

  public ElectronicStructureProblem electronicProblem() {
    return this.electronicProblem;
  }

  public QuantumProgram quantumProgram() {
    return this.quantumProgram;
  }

  public ChemistryQuantumProgramPackage quantumProgramPackage() {
    return this.quantumProgramPackage;
  }

  public boolean hasQuantumProgramPackage() {
    return this.quantumProgramPackage != null && this.quantumProgramPackage.hasArtifacts();
  }

  public ChemistryComputationParityPlan computationParityPlan() {
    return this.computationParityPlan;
  }

  public boolean hasComputationParityPlan() {
    return this.computationParityPlan != null && !this.computationParityPlan.empty();
  }

  public ErrorMitigationPlan errorMitigationPlan() {
    return this.errorMitigationPlan;
  }

  public boolean hasErrorMitigationPlan() {
    return this.errorMitigationPlan != null;
  }

  public VqeOptimizationResult vqeOptimizationResult() {
    return this.vqeOptimizationResult;
  }

  public boolean hasVqeOptimizationResult() {
    return this.vqeOptimizationResult != null;
  }

  public ChemistryWorkflowStoragePlan storagePlan() {
    return this.storagePlan;
  }

  public boolean hasStoragePlan() {
    return !this.storagePlan.empty();
  }

  public List<ChemistryWorkflowStageResult> stageResults() {
    return this.stageResults;
  }

  public ChemistryWorkflowStageResult stageResult(final ChemistryWorkflowStage stage) {
    if (stage == null) {
      throw new IllegalArgumentException("Chemistry workflow stage must not be null.");
    }
    for (int i = 0; i < this.stageResults.size(); ++i) {
      final ChemistryWorkflowStageResult result = this.stageResults.get(i);
      if (result.stage() == stage) {
        return result;
      }
    }
    return null;
  }

  public boolean hasStage(final ChemistryWorkflowStage stage) {
    return this.stageResult(stage) != null;
  }

  public boolean stageHasStatus(
      final ChemistryWorkflowStage stage, final ChemistryWorkflowStageStatus status) {
    if (status == null) {
      throw new IllegalArgumentException("Chemistry workflow stage status must not be null.");
    }
    final ChemistryWorkflowStageResult result = this.stageResult(stage);
    return result != null && result.status() == status;
  }

  public int stageCount(final ChemistryWorkflowStageStatus status) {
    if (status == null) {
      throw new IllegalArgumentException("Chemistry workflow stage status must not be null.");
    }
    int count = 0;
    for (int i = 0; i < this.stageResults.size(); ++i) {
      if (this.stageResults.get(i).status() == status) {
        ++count;
      }
    }
    return count;
  }

  public int completedStageCount() {
    return this.stageCount(ChemistryWorkflowStageStatus.COMPLETED);
  }

  public int warningStageCount() {
    return this.stageCount(ChemistryWorkflowStageStatus.WARNING);
  }

  public int failedStageCount() {
    return this.stageCount(ChemistryWorkflowStageStatus.FAILED);
  }

  public int skippedStageCount() {
    return this.stageCount(ChemistryWorkflowStageStatus.SKIPPED);
  }

  public ChemistryWorkflowDiagnosticSet diagnostics() {
    return this.diagnostics;
  }

  public boolean success() {
    return this.status == ChemistryWorkflowStatus.SUCCESS;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowResult)) {
      return false;
    }
    final ChemistryWorkflowResult result = (ChemistryWorkflowResult) other;
    return Objects.equals(this.job, result.job)
        && this.status == result.status
        && Objects.equals(this.coreValidationResult, result.coreValidationResult)
        && Objects.equals(this.capabilityReport, result.capabilityReport)
        && Objects.equals(this.classicalPreflightResult, result.classicalPreflightResult)
        && Objects.equals(this.classicalCalculationPlan, result.classicalCalculationPlan)
        && Objects.equals(this.classicalCalculationResult, result.classicalCalculationResult)
        && Objects.equals(this.reactionPredictionResult, result.reactionPredictionResult)
        && Objects.equals(this.reactionPredictionExplanation, result.reactionPredictionExplanation)
        && Objects.equals(this.conditionEffectProfile, result.conditionEffectProfile)
        && Objects.equals(this.conditionScenarioPlan, result.conditionScenarioPlan)
        && Objects.equals(
            this.conditionScenarioEvaluationPlan, result.conditionScenarioEvaluationPlan)
        && Objects.equals(this.recommendationPlan, result.recommendationPlan)
        && Objects.equals(this.followUpCalculationPlan, result.followUpCalculationPlan)
        && Objects.equals(this.electronicProblem, result.electronicProblem)
        && Objects.equals(this.quantumProgram, result.quantumProgram)
        && Objects.equals(this.quantumProgramPackage, result.quantumProgramPackage)
        && Objects.equals(this.computationParityPlan, result.computationParityPlan)
        && Objects.equals(this.errorMitigationPlan, result.errorMitigationPlan)
        && Objects.equals(this.vqeOptimizationResult, result.vqeOptimizationResult)
        && Objects.equals(this.storagePlan, result.storagePlan)
        && Objects.equals(this.stageResults, result.stageResults)
        && Objects.equals(this.diagnostics, result.diagnostics);
  }

  public int hashCode() {
    return Objects.hash(
        this.job,
        this.status,
        this.coreValidationResult,
        this.capabilityReport,
        this.classicalPreflightResult,
        this.classicalCalculationPlan,
        this.classicalCalculationResult,
        this.reactionPredictionResult,
        this.reactionPredictionExplanation,
        this.conditionEffectProfile,
        this.conditionScenarioPlan,
        this.conditionScenarioEvaluationPlan,
        this.recommendationPlan,
        this.followUpCalculationPlan,
        this.electronicProblem,
        this.quantumProgram,
        this.quantumProgramPackage,
        this.computationParityPlan,
        this.errorMitigationPlan,
        this.vqeOptimizationResult,
        this.storagePlan,
        this.stageResults,
        this.diagnostics);
  }
}
