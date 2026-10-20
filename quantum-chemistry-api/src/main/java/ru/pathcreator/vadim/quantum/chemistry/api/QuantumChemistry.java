/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionCandidateRoadmap;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionComputationRoadmap;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionComputationRoadmapBuilder;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionDecisionMatrix;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionDecisionMatrixBuilder;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionPredictionWorkflow;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionPredictionWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.api.workflow.ChemistryWorkflowRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlanner;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionMechanismCatalogCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionExplainer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionOutcomeBenchmarkEvaluator;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionOutcomeCalibrationCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeBenchmarkReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismCatalogCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInClassicalBackendProfiles;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.verification.MappingSpectralVerification;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.library.application.ChemistryLibraryService;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryQuery;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryRegistry;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.adapt.AdaptVqeSelector;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ChemistryQuantumProgramPackageGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ElectronicHamiltonianProgramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.PauliExpectationMeasurementAggregator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.mitigation.ErrorMitigationPlanner;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionResult;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.measurement.PauliExpectationMeasurementResult;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.VqeEnergyBackend;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.VqeOptimizer;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionEventKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionSnapshot;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionTimeline;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecordStatus;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryCapabilityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryComputationParityPlanner;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryWorkflowStoragePlanner;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryWorkflowService;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ReactionPredictionFollowUpPlanner;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.followup.ChemistryFollowUpCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationKind;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowExecutionBundle;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.storage.ChemistryWorkflowStoragePlan;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

/**
 * Публичная точка входа в chemistry stack для приложений и внешнего Quantum IR API.
 */
public final class QuantumChemistry {

  private static final double DEFAULT_MAPPING_VERIFICATION_TOLERANCE = 1.0E-9;

  private QuantumChemistry() {}

  public static OrganicReactionPredictionWorkflowResult predictOrganicReaction(
      final ReactionPredictionRequest request) {
    return new OrganicReactionPredictionWorkflow().predict(request);
  }

  public static OrganicReactionPredictionWorkflowResult predictOrganicReaction(
      final ClassicalCalculationRequest calculationRequest,
      final ReactionPredictionRequest request) {
    return new OrganicReactionPredictionWorkflow().predict(request, calculationRequest);
  }

  public static OrganicReactionPredictionWorkflowResult predictOrganicReactionFromLibraryMolecules(
      final String requestId,
      final List<String> reactantEntryIds,
      final ReactionConditions conditions,
      final List<OrganicReactionDomain> domains,
      final List<ClassicalCalculationOption> calculationOptions) {
    return QuantumChemistry.predictOrganicReactionFromLibraryMolecules(
        requestId,
        reactantEntryIds,
        conditions,
        domains,
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        calculationOptions);
  }

  public static OrganicReactionPredictionWorkflowResult predictOrganicReactionFromLibraryMolecules(
      final String requestId,
      final List<String> reactantEntryIds,
      final ReactionConditions conditions,
      final List<OrganicReactionDomain> domains,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final List<ClassicalCalculationOption> calculationOptions) {
    if (method == null) {
      throw new IllegalArgumentException("Library molecule prediction method must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("Library molecule prediction basis set must not be null.");
    }
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            requestId,
            QuantumChemistry.reactionSideFromLibraryMolecules(reactantEntryIds),
            conditions,
            domains,
            List.of(),
            "Prediction request built from Quantum chemistry library molecule entries.");
    return QuantumChemistry.predictOrganicReactionWithGeneratedCandidateCalculations(
        request, request.id() + "_calculation", method, basisSet, calculationOptions);
  }

  public static OrganicReactionPredictionWorkflowResult
      predictOrganicReactionWithGeneratedCandidateCalculations(
          final ReactionPredictionRequest request,
          final String calculationId,
          final ElectronicStructureMethod method,
          final BasisSet basisSet,
          final List<ClassicalCalculationOption> calculationOptions) {
    if (request == null) {
      throw new IllegalArgumentException("Organic reaction prediction request must not be null.");
    }
    if (method == null) {
      throw new IllegalArgumentException(
          "Organic reaction prediction calculation method must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException(
          "Organic reaction prediction calculation basis set must not be null.");
    }
    final List<ClassicalCalculationOption> checkedOptions =
        calculationOptions == null ? List.of() : List.copyOf(calculationOptions);
    if (checkedOptions.isEmpty()) {
      return QuantumChemistry.predictOrganicReaction(request);
    }
    final OrganicReactionPredictionWorkflowResult baseline =
        QuantumChemistry.predictOrganicReaction(request);
    if (!baseline.hasPredictionResult() || !baseline.predictionResult().hasCandidates()) {
      return baseline;
    }
    final ReactionProductCandidate bestCandidate = baseline.predictionResult().bestCandidate();
    final ClassicalCalculationRequest calculationRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(
                calculationId == null || calculationId.trim().isEmpty()
                    ? request.id() + "_generated_candidate_calculation"
                    : calculationId.trim()),
            Reaction.of(
                ReactionId.of(request.id() + "_generated_candidate_reaction"),
                "Generated-candidate calculation for " + request.id(),
                request.reactants(),
                bestCandidate.products(),
                request.conditions(),
                ChemistryMetadata.EMPTY),
            method,
            basisSet,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            checkedOptions,
            ChemistryMetadata.EMPTY);
    return QuantumChemistry.predictOrganicReaction(calculationRequest, request);
  }

  public static OrganicReactionDecisionMatrix predictOrganicReactionDecisionMatrix(
      final ReactionPredictionRequest request) {
    return QuantumChemistry.organicReactionDecisionMatrix(
        QuantumChemistry.predictOrganicReaction(request));
  }

  public static OrganicReactionDecisionMatrix predictOrganicReactionDecisionMatrix(
      final ClassicalCalculationRequest calculationRequest,
      final ReactionPredictionRequest request) {
    return QuantumChemistry.organicReactionDecisionMatrix(
        QuantumChemistry.predictOrganicReaction(calculationRequest, request));
  }

  public static ReactionOutcomeCalibrationCoverageReport reactionCalibrationCoverage(
      final ReactionOutcomeCalibrationLibrary calibrationLibrary) {
    return new ReactionOutcomeCalibrationCoverageAnalyzer().analyze(calibrationLibrary);
  }

  public static ReactionOutcomeBenchmarkReport reactionPredictionBenchmark(
      final ReactionPredictionResult prediction,
      final ReactionOutcomeCalibrationLibrary benchmarks) {
    return new ReactionOutcomeBenchmarkEvaluator().evaluate(prediction, benchmarks);
  }

  public static ReactionMechanismCatalogCoverageReport builtInReactionMechanismCatalogCoverage() {
    return new ReactionMechanismCatalogCoverageAnalyzer()
        .analyze(BuiltInReactionMechanismPatternCatalog.organicAndPhysicalChemistry());
  }

  public static ClassicalResultQualityReport classicalResultQuality(
      final ClassicalCalculationResult result) {
    return new ClassicalResultQualityAnalyzer().analyze(result);
  }

  public static MolecularFeatureSet analyzeMolecularFeatures(final Molecule molecule) {
    return new MolecularFeatureAnalyzer().analyze(molecule);
  }

  public static OrganicReactionDecisionMatrix organicReactionDecisionMatrix(
      final OrganicReactionPredictionWorkflowResult result) {
    return new OrganicReactionDecisionMatrixBuilder().build(result);
  }

  public static OrganicReactionComputationRoadmap organicReactionComputationRoadmap(
      final OrganicReactionPredictionWorkflowResult result) {
    return new OrganicReactionComputationRoadmapBuilder().build(result);
  }

  public static ChemistryStorageRoadmapRecord roadmapStorageRecord(
      final String id,
      final int historyIndex,
      final OrganicReactionComputationRoadmap roadmap,
      final String note) {
    if (roadmap == null) {
      throw new IllegalArgumentException("Organic reaction computation roadmap must not be null.");
    }
    return ChemistryStorageRoadmapRecord.of(
        id,
        roadmap.requestId(),
        historyIndex,
        roadmap.status().name(),
        roadmap.conditionScenarioCount(),
        roadmap.evaluatedConditionScenarioCount(),
        QuantumChemistry.roadmapCandidateStorageRecords(roadmap.candidates()),
        QuantumChemistry.recommendationTargets(roadmap.programLevelRecommendations()),
        note);
  }

  public static ReactionPredictionExplanation explainReactionPrediction(
      final ReactionPredictionResult result) {
    return new ReactionPredictionExplainer().explain(result);
  }

  private static List<ChemistryStorageRoadmapCandidateRecord> roadmapCandidateStorageRecords(
      final List<OrganicReactionCandidateRoadmap> candidates) {
    final ArrayList<ChemistryStorageRoadmapCandidateRecord> records =
        new ArrayList<>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      final OrganicReactionCandidateRoadmap candidate = candidates.get(i);
      records.add(
          ChemistryStorageRoadmapCandidateRecord.of(
              candidate.candidateIndex(),
              candidate.probability(),
              candidate.expectedYieldPercent(),
              candidate.productMoleculeIds(),
              candidate.productFormulaSignatures(),
              candidate.productStoichiometrySignatures(),
              candidate.mechanismIds(),
              candidate.mechanismDescriptors(),
              QuantumChemistry.classicalCalculationKindNames(candidate),
              List.of(),
              QuantumChemistry.followUpActionTargets(candidate),
              candidate.quantumPreparationTargets(),
              candidate.explicitProductGraph(),
              candidate.electronicRerankReady(),
              candidate.quantumWorkflowReady(),
              candidate.readinessReason()));
    }
    return List.copyOf(records);
  }

  private static List<String> classicalCalculationKindNames(
      final OrganicReactionCandidateRoadmap candidate) {
    final ArrayList<String> names = new ArrayList<>(candidate.classicalCalculationKinds().size());
    for (int i = 0; i < candidate.classicalCalculationKinds().size(); ++i) {
      names.add(candidate.classicalCalculationKinds().get(i).name());
    }
    return List.copyOf(names);
  }

  private static List<String> followUpActionTargets(
      final OrganicReactionCandidateRoadmap candidate) {
    final List<ChemistryWorkflowRecommendation> recommendations = candidate.recommendations();
    if (recommendations.isEmpty()) {
      return List.of();
    }
    final ArrayList<String> targets = new ArrayList<>(recommendations.size());
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (recommendation.kind() == ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION) {
        continue;
      }
      targets.add(recommendation.target());
    }
    return List.copyOf(targets);
  }

  private static List<String> recommendationTargets(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final ArrayList<String> targets = new ArrayList<>(recommendations.size());
    for (int i = 0; i < recommendations.size(); ++i) {
      targets.add(recommendations.get(i).target());
    }
    return List.copyOf(targets);
  }

  public static ChemistryStoragePredictionRecord predictionStorageRecord(
      final String id,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final ReactionPredictionResult result,
      final String note) {
    return new ChemistryStorageService().predictionRecord(id, historyIndex, status, result, note);
  }

  public static ChemistryStorageComputationPlanRecord computationPlanStorageRecord(
      final String id,
      final String reactionRecordId,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final List<String> classicalPlanIds,
      final List<String> quantumProgramIds,
      final boolean deferredExecution,
      final String note) {
    return QuantumChemistry.computationPlanStorageRecord(
        id,
        reactionRecordId,
        historyIndex,
        status,
        classicalPlanIds,
        List.of(),
        quantumProgramIds,
        deferredExecution,
        note);
  }

  public static ChemistryStorageComputationPlanRecord computationPlanStorageRecord(
      final String id,
      final String reactionRecordId,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final boolean deferredExecution,
      final String note) {
    return new ChemistryStorageService()
        .computationPlanRecord(
            id,
            reactionRecordId,
            historyIndex,
            status,
            classicalPlanIds,
            classicalResultIds,
            quantumProgramIds,
            deferredExecution,
            note);
  }

  public static ChemistryStorageReactionOutcomeRecord reactionOutcomeStorageRecord(
      final String id,
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionOutcomeKind kind,
      final ReactionProductCandidate candidate,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final String conditionSummary,
      final String note) {
    return new ChemistryStorageService()
        .reactionOutcomeRecord(
            id,
            reactionId,
            historyIndex,
            kind,
            candidate,
            classicalResultIds,
            quantumProgramIds,
            conditionSummary,
            note);
  }

  public static ChemistryStorageReactionHistoryEvent reactionHistoryStorageEvent(
      final String id,
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionEventKind kind,
      final String title,
      final String description,
      final List<String> reactionRecordIds,
      final List<String> conditionSnapshotRecordIds,
      final List<String> predictionRecordIds,
      final List<String> computationPlanRecordIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds) {
    return new ChemistryStorageService()
        .reactionHistoryEvent(
            id,
            reactionId,
            historyIndex,
            kind,
            title,
            description,
            reactionRecordIds,
            conditionSnapshotRecordIds,
            predictionRecordIds,
            computationPlanRecordIds,
            classicalResultIds,
            quantumProgramIds);
  }

  public static ChemistryWorkflowStoragePlan workflowStoragePlan(
      final ChemistryWorkflowResult result) {
    return new ChemistryWorkflowStoragePlanner().plan(result);
  }

  public static ChemistryWorkflowStoragePlan workflowStoragePlan(
      final ChemistryWorkflowExecutionBundle bundle) {
    return new ChemistryWorkflowStoragePlanner().plan(bundle);
  }

  public static ChemistryComputationParityPlan workflowComputationParityPlan(
      final ChemistryWorkflowResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Chemistry workflow result must not be null.");
    }
    return result.computationParityPlan();
  }

  public static ChemistryComputationParityPlan workflowComputationParityPlan(
      final ChemistryWorkflowResult result,
      final List<ClassicalCalculationResult> executedFollowUpResults) {
    return new ChemistryWorkflowService().planComputationParity(result, executedFollowUpResults);
  }

  public static ChemistryFollowUpCalculationPlan workflowFollowUpCalculationPlan(
      final ChemistryWorkflowResult result) {
    return new ChemistryWorkflowService().planFollowUpCalculations(result);
  }

  public static List<ClassicalCalculationResult> runWorkflowFollowUpCalculations(
      final ChemistryWorkflowResult result) {
    return new ChemistryWorkflowService().executeFollowUpCalculations(result);
  }

  public static List<ClassicalCalculationResult> runWorkflowFollowUpCalculations(
      final ChemistryWorkflowResult result, final int maxFollowUpRequestCount) {
    return new ChemistryWorkflowService()
        .executeFollowUpCalculations(result, maxFollowUpRequestCount);
  }

  public static List<ClassicalCalculationResult> runWorkflowFollowUpCalculations(
      final ChemistryWorkflowResult result,
      final int maxFollowUpRequestCount,
      final int maxParallelFollowUpCount) {
    return new ChemistryWorkflowService()
        .executeFollowUpCalculations(result, maxFollowUpRequestCount, maxParallelFollowUpCount);
  }

  public static List<ClassicalCalculationResult> runWorkflowFollowUpCalculations(
      final ChemistryWorkflowResult result, final List<ClassicalCalculationKind> calculationKinds) {
    return new ChemistryWorkflowService().executeFollowUpCalculations(result, calculationKinds);
  }

  public static List<ClassicalCalculationResult> runAllWorkflowFollowUpCalculations(
      final ChemistryWorkflowResult result, final List<ClassicalCalculationKind> calculationKinds) {
    return new ChemistryWorkflowService().executeAllFollowUpCalculations(result, calculationKinds);
  }

  public static List<ClassicalCalculationResult> runAllWorkflowFollowUpCalculations(
      final ChemistryWorkflowResult result, final int maxParallelFollowUpCount) {
    return new ChemistryWorkflowService()
        .executeAllFollowUpCalculations(result, maxParallelFollowUpCount);
  }

  public static List<ClassicalCalculationResult> runAllWorkflowFollowUpCalculations(
      final ChemistryWorkflowResult result,
      final List<ClassicalCalculationKind> calculationKinds,
      final int maxParallelFollowUpCount) {
    return new ChemistryWorkflowService()
        .executeAllFollowUpCalculations(result, calculationKinds, maxParallelFollowUpCount);
  }

  public static ChemistryComputationParityPlan runWorkflowFollowUpCalculationsAndPlanParity(
      final ChemistryWorkflowResult result) {
    return new ChemistryWorkflowService().executeAllFollowUpCalculationsAndPlanParity(result);
  }

  public static ChemistryComputationParityPlan runWorkflowFollowUpCalculationsAndPlanParity(
      final ChemistryWorkflowResult result, final int maxParallelFollowUpCount) {
    return new ChemistryWorkflowService()
        .executeAllFollowUpCalculationsAndPlanParity(result, maxParallelFollowUpCount);
  }

  public static ChemistryComputationParityPlan runWorkflowFollowUpCalculationsAndPlanParity(
      final ChemistryWorkflowResult result, final List<ClassicalCalculationKind> calculationKinds) {
    return new ChemistryWorkflowService()
        .executeFollowUpCalculationsAndPlanParity(result, calculationKinds);
  }

  public static ChemistryStorageDocument workflowStorageDocument(
      final ChemistryWorkflowResult result, final String projectId, final String displayName) {
    return QuantumChemistry.workflowStoragePlan(result).toDocument(projectId, displayName);
  }

  public static ChemistryStorageDocument workflowStorageDocument(
      final ChemistryWorkflowExecutionBundle bundle,
      final String projectId,
      final String displayName) {
    return QuantumChemistry.workflowStoragePlan(bundle).toDocument(projectId, displayName);
  }

  public static ChemistryStorageReactionTimeline reactionStorageTimeline(
      final ChemistryStorageDocument document, final String reactionId) {
    return new ChemistryStorageService().reactionTimeline(document, reactionId);
  }

  public static ChemistryStorageReactionSnapshot reactionStorageSnapshot(
      final ChemistryStorageDocument document, final String reactionId, final int historyIndex) {
    return new ChemistryStorageService().reactionSnapshot(document, reactionId, historyIndex);
  }

  public static ReactionConditionScenarioPlan planReactionConditionScenarios(
      final ReactionPredictionRequest request) {
    return new ReactionConditionScenarioPlanner().plan(request);
  }

  public static ReactionConditionScenarioEvaluationPlan evaluateReactionConditionScenarios(
      final ReactionPredictionRequest request) {
    return new ClassicalChemistryService().evaluateReactionConditionScenarios(request);
  }

  public static ReactionConditionEffectProfile analyzeReactionConditionEffects(
      final ReactionPredictionRequest request) {
    return new ClassicalChemistryService().analyzeReactionConditionEffects(request);
  }

  public static ClassicalPreflightResult preflightClassicalCalculation(
      final ClassicalCalculationRequest request) {
    return QuantumChemistry.preflightClassicalCalculation(
        request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
  }

  public static ClassicalPreflightResult preflightClassicalCalculation(
      final ClassicalCalculationRequest request, final ClassicalBackendProfile profile) {
    return new ClassicalChemistryService().preflight(request, profile);
  }

  public static ClassicalCalculationPlan planClassicalCalculation(
      final ClassicalCalculationRequest request) {
    return QuantumChemistry.planClassicalCalculation(
        request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
  }

  public static ClassicalCalculationPlan planClassicalCalculation(
      final ClassicalCalculationRequest request, final ClassicalBackendProfile profile) {
    return new ClassicalChemistryService().plan(request, profile);
  }

  /** Graph-based initial coordinates only; no relaxation, implicit hydrogens or stereochemical inference. */
  public static Molecule prepareInitialGeometry(final Molecule molecule) {
    return ru.pathcreator.vadim.quantum.chemistry.api.geometry.ChemistryGeometrySupport
        .prepareInitial(molecule);
  }

  public static Molecule optimizedGeometry(
      final Molecule input, final ClassicalCalculationResult result) {
    return ru.pathcreator.vadim.quantum.chemistry.api.geometry.ChemistryGeometrySupport.optimized(
        input, result);
  }

  public static ClassicalCalculationResult runClassicalCalculation(
      final ClassicalCalculationRequest request) {
    return new ClassicalChemistryService().execute(request);
  }

  public static ChemistryCapabilityReport analyzeComputationalCapabilities(
      final ClassicalCalculationRequest request) {
    return new ChemistryCapabilityAnalyzer().analyze(request);
  }

  public static ChemistryWorkflowResult runWorkflow(final ChemistryWorkflowJob job) {
    return new ChemistryWorkflowService().run(job);
  }

  public static ChemistryWorkflowExecutionBundle runWorkflowWithAllFollowUpCalculations(
      final ChemistryWorkflowJob job) {
    return new ChemistryWorkflowService().runWithAllFollowUpCalculations(job);
  }

  public static ChemistryWorkflowExecutionBundle runWorkflowWithAllFollowUpCalculations(
      final ChemistryWorkflowJob job, final int maxParallelFollowUpCount) {
    return new ChemistryWorkflowService()
        .runWithAllFollowUpCalculations(job, maxParallelFollowUpCount);
  }

  public static ChemistryWorkflowExecutionBundle runWorkflowWithFollowUpCalculationLimit(
      final ChemistryWorkflowJob job, final int maxFollowUpRequestCount) {
    return new ChemistryWorkflowService()
        .runWithFollowUpCalculationLimit(job, maxFollowUpRequestCount);
  }

  public static ChemistryWorkflowExecutionBundle runWorkflowWithFollowUpCalculationLimit(
      final ChemistryWorkflowJob job,
      final int maxFollowUpRequestCount,
      final int maxParallelFollowUpCount) {
    return new ChemistryWorkflowService()
        .runWithFollowUpCalculationLimit(job, maxFollowUpRequestCount, maxParallelFollowUpCount);
  }

  public static ChemistryWorkflowJob workflowJob(final ChemistryWorkflowRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Chemistry workflow request must not be null.");
    }
    return request.toJob();
  }

  public static ChemistryWorkflowResult runWorkflow(final ChemistryWorkflowRequest request) {
    return QuantumChemistry.runWorkflow(QuantumChemistry.workflowJob(request));
  }

  public static ChemistryWorkflowExecutionBundle runWorkflowWithAllFollowUpCalculations(
      final ChemistryWorkflowRequest request) {
    return QuantumChemistry.runWorkflowWithAllFollowUpCalculations(
        QuantumChemistry.workflowJob(request));
  }

  public static ChemistryWorkflowExecutionBundle runWorkflowWithAllFollowUpCalculations(
      final ChemistryWorkflowRequest request, final int maxParallelFollowUpCount) {
    return QuantumChemistry.runWorkflowWithAllFollowUpCalculations(
        QuantumChemistry.workflowJob(request), maxParallelFollowUpCount);
  }

  public static ChemistryWorkflowExecutionBundle runWorkflowWithFollowUpCalculationLimit(
      final ChemistryWorkflowRequest request, final int maxFollowUpRequestCount) {
    return QuantumChemistry.runWorkflowWithFollowUpCalculationLimit(
        QuantumChemistry.workflowJob(request), maxFollowUpRequestCount);
  }

  public static ChemistryWorkflowExecutionBundle runWorkflowWithFollowUpCalculationLimit(
      final ChemistryWorkflowRequest request,
      final int maxFollowUpRequestCount,
      final int maxParallelFollowUpCount) {
    return QuantumChemistry.runWorkflowWithFollowUpCalculationLimit(
        QuantumChemistry.workflowJob(request), maxFollowUpRequestCount, maxParallelFollowUpCount);
  }

  public static ChemistryWorkflowResult runWorkflow(
      final ChemistryWorkflowRequest request, final VqeEnergyBackend backend) {
    if (request == null) {
      throw new IllegalArgumentException("Chemistry workflow request must not be null.");
    }
    final ElectronicStructurePreparationPipeline preparationPipeline =
        new ElectronicStructurePreparationPipeline();
    return new ChemistryWorkflowService(
            new ChemistryCoreValidator(),
            new ClassicalChemistryService(preparationPipeline),
            new ElectronicHamiltonianQubitMapper(),
            new ElectronicHamiltonianProgramGenerator(),
            new ChemistryQuantumProgramPackageGenerator(),
            new ErrorMitigationPlanner(),
            new ReactionPredictionExplainer(),
            preparationPipeline,
            new ReactionPredictionFollowUpPlanner(),
            new ReactionConditionScenarioPlanner(),
            new ChemistryComputationParityPlanner(),
            new ChemistryCapabilityAnalyzer(preparationPipeline),
            new ChemistryWorkflowStoragePlanner(),
            backend)
        .run(request.toJob());
  }

  public static ChemistryWorkflowResult preflightWorkflow(final ChemistryWorkflowJob job) {
    return new ChemistryWorkflowService().preflight(job);
  }

  public static ChemistryWorkflowResult preflightWorkflow(final ChemistryWorkflowRequest request) {
    return QuantumChemistry.preflightWorkflow(QuantumChemistry.workflowJob(request));
  }

  public static ErrorMitigationPlan planErrorMitigation(final ErrorMitigationRequest request) {
    return new ErrorMitigationPlanner().plan(request);
  }

  public static ChemistryQuantumProgramPackage prepareQuantumPrograms(
      final String sourceId,
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping mapping) {
    return new ChemistryQuantumProgramPackageGenerator().generate(sourceId, hamiltonian, mapping);
  }

  public static ElectronicStructurePreparationResult prepareElectronicStructure(
      final ClassicalCalculationRequest request) {
    return new ElectronicStructurePreparationPipeline().prepare(request);
  }

  public static ElectronicHamiltonian prepareElectronicHamiltonian(
      final ClassicalCalculationRequest request) {
    return QuantumChemistry.prepareElectronicStructure(request).hamiltonian();
  }

  public static VerifiedElectronicHamiltonianMapping verifyHamiltonianMapping(
      final ElectronicHamiltonian hamiltonian, final FermionToQubitMapping mapping) {
    return QuantumChemistry.verifyHamiltonianMapping(
        hamiltonian, mapping, DEFAULT_MAPPING_VERIFICATION_TOLERANCE);
  }

  public static VerifiedElectronicHamiltonianMapping verifyHamiltonianMapping(
      final ElectronicHamiltonian hamiltonian,
      final FermionToQubitMapping mapping,
      final double tolerance) {
    return new ElectronicHamiltonianQubitMapper().mapVerified(hamiltonian, mapping, tolerance);
  }

  public static List<VerifiedElectronicHamiltonianMapping> verifyHamiltonianMappings(
      final ElectronicHamiltonian hamiltonian, final List<FermionToQubitMapping> mappings) {
    return QuantumChemistry.verifyHamiltonianMappings(
        hamiltonian, mappings, DEFAULT_MAPPING_VERIFICATION_TOLERANCE);
  }

  public static List<VerifiedElectronicHamiltonianMapping> verifyHamiltonianMappings(
      final ElectronicHamiltonian hamiltonian,
      final List<FermionToQubitMapping> mappings,
      final double tolerance) {
    return new ElectronicHamiltonianQubitMapper()
        .mapNormalOrderedVerified(hamiltonian, mappings, tolerance);
  }

  public static ChemistryQuantumProgramPackage prepareQuantumPrograms(
      final ClassicalCalculationRequest request, final FermionToQubitMapping mapping) {
    if (request == null) {
      throw new IllegalArgumentException("Chemistry quantum program request must not be null.");
    }
    return QuantumChemistry.prepareQuantumPrograms(request.id().value(), request, mapping);
  }

  public static ChemistryQuantumProgramPackage prepareQuantumPrograms(
      final String sourceId,
      final ClassicalCalculationRequest request,
      final FermionToQubitMapping mapping) {
    return QuantumChemistry.prepareQuantumPrograms(
        sourceId, QuantumChemistry.prepareElectronicHamiltonian(request), mapping);
  }

  public static VqeOptimizationResult runVqe(
      final VqeOptimizationRequest request, final VqeEnergyBackend backend) {
    return new VqeOptimizer(backend).optimize(request);
  }

  public static PauliExpectationMeasurementResult aggregatePauliMeasurementEnergy(
      final QubitPauliOperator hamiltonian, final List<Map<String, Long>> termCounts) {
    return new PauliExpectationMeasurementAggregator().aggregate(hamiltonian, termCounts);
  }

  public static PauliExpectationMeasurementResult
      aggregatePauliMeasurementEnergyFromSimulationResults(
          final QubitPauliOperator hamiltonian, final List<SimulationResult> simulationResults) {
    return new PauliExpectationMeasurementAggregator()
        .aggregateSimulationResults(hamiltonian, simulationResults);
  }

  public static ChemistryStorageExtension pauliMeasurementEnergyStorageExtension(
      final String id, final String sourceId, final PauliExpectationMeasurementResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Pauli measurement energy result must not be null.");
    }
    final LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
    properties.put(
        "source", sourceId == null || sourceId.trim().isEmpty() ? "unknown" : sourceId.trim());
    properties.put("energy", Double.toString(result.energy()));
    properties.put("identity_offset", Double.toString(result.identityOffset()));
    properties.put("measured_term_count", Integer.toString(result.measuredTermCount()));
    final ArrayList<String> bodyLines = new ArrayList<String>(result.measuredTermCount());
    for (int i = 0; i < result.measuredTermCount(); ++i) {
      bodyLines.add(
          "term["
              + i
              + "] expectation="
              + result.termExpectation(i)
              + " contribution="
              + result.termContribution(i)
              + " shots="
              + result.termShots(i));
    }
    if (bodyLines.isEmpty()) {
      bodyLines.add("identity-only Hamiltonian measurement result");
    }
    return ChemistryStorageExtension.of(
        "quantum_pauli_measurement_energy", id, properties, bodyLines);
  }

  public static AdaptVqeSelectionResult selectAdaptVqe(
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions ansatzOptions,
      final AdaptVqeSelectionOptions selectionOptions,
      final ErrorMitigationRequest mitigationRequest,
      final VqeEnergyBackend backend) {
    return new AdaptVqeSelector(backend)
        .select(activeSpace, ansatzOptions, selectionOptions, mitigationRequest);
  }

  public static MappingSpectralVerification.MappingSpectralVerificationResult
      verifyFermionToQubitMapping(
          final FermionOperator operator,
          final FermionToQubitMapping mapping,
          final int spinOrbitalCount,
          final int momentCount,
          final double tolerance) {
    return new MappingSpectralVerification()
        .verify(operator, mapping, spinOrbitalCount, momentCount, tolerance);
  }

  public static ChemistryLibraryRegistry builtInLibrary() {
    return new ChemistryLibraryService().builtInRegistry();
  }

  public static List<ChemistryLibraryEntry> searchLibrary(final ChemistryLibraryQuery query) {
    return new ChemistryLibraryService().search(query);
  }

  public static ChemistryLibraryEntry requireLibraryEntry(final String id) {
    return new ChemistryLibraryService().requireBuiltIn(id);
  }

  private static ReactionSide reactionSideFromLibraryMolecules(final List<String> entryIds) {
    if (entryIds == null || entryIds.isEmpty()) {
      throw new IllegalArgumentException(
          "Chemistry library reactant entry list must not be empty.");
    }
    final ArrayList<ReactionParticipant> participants =
        new ArrayList<ReactionParticipant>(entryIds.size());
    for (int index = 0; index < entryIds.size(); index++) {
      final ChemistryLibraryEntry entry = QuantumChemistry.requireLibraryEntry(entryIds.get(index));
      final ChemistryStorageDocument document = entry.document();
      if (document.molecules().isEmpty()) {
        throw new IllegalArgumentException(
            "Chemistry library entry does not contain a molecule: " + entry.id() + ".");
      }
      participants.add(
          ReactionParticipant.of(document.molecules().get(0), StoichiometricCoefficient.ONE));
    }
    return ReactionSide.of(participants);
  }
}
