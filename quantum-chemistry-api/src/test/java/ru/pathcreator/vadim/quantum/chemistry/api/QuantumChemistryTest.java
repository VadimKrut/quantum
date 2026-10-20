/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionCandidateRoadmap;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionComputationRoadmap;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionDecisionMatrix;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionDecisionRow;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionPredictionWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.api.prediction.OrganicReactionPredictionWorkflowStatus;
import ru.pathcreator.vadim.quantum.chemistry.api.workflow.ChemistryWorkflowRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInClassicalBackendProfiles;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.verification.MappingSpectralVerification;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntryKind;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryQuery;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionResult;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryExcitationRank;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.measurement.PauliExpectationMeasurementResult;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationTechnique;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModel;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModelKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeConvergenceStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecordStatus;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionEventKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionSnapshot;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionTimeline;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJobId;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowMode;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationKind;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStage;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStageStatus;

public final class QuantumChemistryTest {

  @Test
  public void publicFacadeRunsWorkflowFromRequestObject() {
    final Molecule hydrogen = QuantumChemistryTest.hydrogen();
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder("public_request_workflow", hydrogen)
            .activeSpace(ActiveSpace.of(2, 2))
            .reactionConditions(QuantumChemistryTest.heterocycleConditions())
            .domains(List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION))
            .seedCandidates(List.of(QuantumChemistryTest.candidate(hydrogen, 1.0)))
            .boolOption("advanced_prediction_apply_calibration_priors", false)
            .boolOption("reaction_prediction_electronic_rerank", false)
            .mapping(FermionToQubitMapping.JORDAN_WIGNER)
            .note("public request workflow")
            .build();

    final ChemistryWorkflowResult result = QuantumChemistry.runWorkflow(request);

    Assertions.assertNotEquals(
        ChemistryWorkflowStatus.FAILED, result.status(), result.diagnostics().toString());
    Assertions.assertFalse(result.diagnostics().hasErrors());
    Assertions.assertTrue(result.hasCapabilityReport());
    Assertions.assertTrue(result.hasClassicalCalculationResult());
    Assertions.assertTrue(result.hasReactionPredictionResult());
    Assertions.assertTrue(result.hasQuantumProgramPackage());
    Assertions.assertTrue(result.hasComputationParityPlan());
    Assertions.assertTrue(result.hasStoragePlan());
    Assertions.assertEquals(
        result.reactionPredictionResult().hasDiagnostics(),
        result.hasReactionPredictionDiagnostics());
    Assertions.assertEquals(
        result.reactionPredictionResult().diagnosticCount(),
        result.reactionPredictionDiagnosticCount());
    Assertions.assertEquals(
        result.reactionPredictionResult().diagnosticCount(ReactionPredictionEvidenceKind.WARNING),
        result.reactionPredictionDiagnosticCount(ReactionPredictionEvidenceKind.WARNING));
    Assertions.assertEquals(
        result.reactionPredictionResult().predictedMechanismCount(),
        result.predictedMechanismCount());
    Assertions.assertEquals(2, request.calculationOptions().size());
    Assertions.assertEquals(2, request.toClassicalCalculationRequest().options().size());
    Assertions.assertEquals(
        2, QuantumChemistry.workflowJob(request).classicalRequest().options().size());
    Assertions.assertEquals(request.toJob(), QuantumChemistry.workflowJob(request));
  }

  @Test
  public void publicFacadeBuildsCandidateQuantumWorkflowVqeOptions() {
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder(
                "public_candidate_quantum_workflow_vqe_options", QuantumChemistryTest.hydrogen())
            .prepareCandidateQuantumWorkflows(3, 2)
            .executeCandidateQuantumWorkflowVqe()
            .prepareCandidateQuantumWorkflows(1, 1)
            .build();

    Assertions.assertEquals(
        "true",
        QuantumChemistryTest.optionValue(request, "reaction_prediction_prepare_quantum_workflows"));
    Assertions.assertEquals(
        "1",
        QuantumChemistryTest.optionValue(
            request, "reaction_prediction_prepare_quantum_workflows_max_candidates"));
    Assertions.assertEquals(
        "1",
        QuantumChemistryTest.optionValue(
            request, "reaction_prediction_prepare_quantum_workflows_max_participants"));
    Assertions.assertEquals(
        "true",
        QuantumChemistryTest.optionValue(
            request, "reaction_prediction_execute_quantum_workflow_vqe"));
    Assertions.assertEquals(
        "true",
        QuantumChemistryTest.optionValue(
            request, "advanced_prediction_execute_quantum_workflow_vqe"));
    Assertions.assertEquals(
        request.calculationOptions().size(),
        request.calculationOptions().stream().map(option -> option.name()).distinct().count());
  }

  @Test
  public void publicFacadeRunsQuantumWorkflowRequestWithAutomaticActiveSpace() {
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder(
                "public_request_without_active_space", QuantumChemistryTest.hydrogen())
            .boolOption("advanced_prediction_apply_calibration_priors", false)
            .boolOption("reaction_prediction_electronic_rerank", false)
            .build();

    final ChemistryWorkflowResult result = QuantumChemistry.runWorkflow(request);

    Assertions.assertFalse(request.hasActiveSpace());
    Assertions.assertNotEquals(
        ChemistryWorkflowStatus.FAILED, result.status(), result.diagnostics().toString());
    Assertions.assertFalse(result.diagnostics().hasErrors());
    Assertions.assertTrue(result.hasQuantumProgramPackage());
  }

  @Test
  public void publicFacadeBuildsQuantumProgramPreparationWithAutomaticActiveSpace() {
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder(
                "public_quantum_program_without_active_space", QuantumChemistryTest.hydrogen())
            .mode(ChemistryWorkflowMode.CLASSICAL_ONLY)
            .calculationKinds(List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION))
            .build();

    Assertions.assertFalse(request.hasActiveSpace());
    Assertions.assertEquals(
        List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION), request.calculationKinds());
  }

  @Test
  public void publicFacadeBuildsReactionWorkflowRequestFromReactionSubject() {
    final Molecule diazine = QuantumChemistryTest.ringMolecule("request_reaction_diazine", "N");
    final Molecule acyclicDiazine =
        QuantumChemistryTest.acyclicMolecule("request_reaction_product", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("request_reaction"),
            "Request reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(acyclicDiazine, StoichiometricCoefficient.ONE))),
            QuantumChemistryTest.heterocycleConditions(),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder("public_reaction_request_workflow", reaction)
            .mode(ChemistryWorkflowMode.CLASSICAL_ONLY)
            .calculationKinds(
                List.of(
                    ClassicalCalculationKind.DESCRIPTOR_ANALYSIS,
                    ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION,
                    ClassicalCalculationKind.REACTION_ENERGY,
                    ClassicalCalculationKind.KINETIC_ANALYSIS))
            .domains(List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION))
            .seedCandidates(List.of(QuantumChemistryTest.candidate(acyclicDiazine, 1.0)))
            .textOption("reaction_energy_model", "bond_energy")
            .note("reaction subject workflow with explicit bond-energy model")
            .build();

    final ChemistryWorkflowResult result = QuantumChemistry.runWorkflow(request);

    Assertions.assertNotEquals(
        ChemistryWorkflowStatus.FAILED, result.status(), result.diagnostics().toString());
    Assertions.assertSame(reaction, request.subject());
    Assertions.assertEquals(
        reaction.reactants(), request.toReactionPredictionRequest().reactants());
    Assertions.assertEquals(
        reaction.conditions(), request.toReactionPredictionRequest().conditions());
    Assertions.assertTrue(result.hasClassicalCalculationResult());
    Assertions.assertTrue(result.hasReactionPredictionResult());
    Assertions.assertTrue(result.hasConditionScenarioEvaluationPlan());
    Assertions.assertTrue(result.hasStoragePlan());
    Assertions.assertFalse(result.hasQuantumProgramPackage());
    Assertions.assertEquals(
        result.reactionPredictionResult().predictedMechanismCount(),
        result.predictedMechanismCount());
  }

  @Test
  public void publicFacadePredictsHeterocycleProductsAndFollowUpPlan() {
    final Molecule diazine = QuantumChemistryTest.ringMolecule("diazine", "N");
    final Molecule acyclicDiazine = QuantumChemistryTest.acyclicMolecule("acyclic_diazine", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "public_heterocycle_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            QuantumChemistryTest.heterocycleConditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                QuantumChemistryTest.candidate(acyclicDiazine, 0.5),
                QuantumChemistryTest.candidate(diazine, 0.5)),
            "public API heterocycle workflow");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("public_workflow_reaction"),
            "Public workflow reaction",
            request.reactants(),
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            request.conditions(),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final OrganicReactionPredictionWorkflowResult result =
        QuantumChemistry.predictOrganicReaction(request);

    Assertions.assertEquals(
        OrganicReactionPredictionWorkflowStatus.SUCCESS,
        result.status(),
        result.validationResult().diagnostics().toString());
    Assertions.assertTrue(result.success());
    Assertions.assertTrue(result.hasPredictionResult());
    Assertions.assertTrue(result.hasExplanation());
    Assertions.assertTrue(result.hasRecommendations());
    Assertions.assertTrue(result.hasConditionScenarioPlan());
    Assertions.assertTrue(result.hasConditionScenarioEvaluationPlan());
    Assertions.assertTrue(
        result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.BASELINE));
    Assertions.assertTrue(
        result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.COOLED));
    Assertions.assertFalse(
        result.conditionScenarioEvaluationPlan().successfulEvaluations().isEmpty());
    Assertions.assertTrue(
        result.predictionResult().candidates().stream()
            .anyMatch(candidate -> !candidate.evidence().isEmpty()));
    Assertions.assertEquals(1.0, result.predictionResult().probabilityMass(), 1.0E-12);
    Assertions.assertTrue(result.predictionResult().probabilityMassComplete(1.0E-12));
    Assertions.assertTrue(
        result.predictionResult().candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.products().participants().get(0).molecule().equals(diazine)));
    final ClassicalCalculationRequest predictionCalculationRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("public_prediction_with_priors"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_apply_calibration_priors", true)),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final OrganicReactionPredictionWorkflowResult withPriorsResult =
        QuantumChemistry.predictOrganicReaction(predictionCalculationRequest, request);
    Assertions.assertEquals(
        OrganicReactionPredictionWorkflowStatus.SUCCESS,
        withPriorsResult.status(),
        withPriorsResult.validationResult().diagnostics().toString());
    Assertions.assertTrue(
        withPriorsResult.predictionResult().candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    final ClassicalCalculationRequest noPriorsCalculationRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("public_prediction_without_priors"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_apply_calibration_priors", false)),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final OrganicReactionPredictionWorkflowResult noPriorsResult =
        QuantumChemistry.predictOrganicReaction(noPriorsCalculationRequest, request);
    Assertions.assertEquals(
        OrganicReactionPredictionWorkflowStatus.SUCCESS,
        noPriorsResult.status(),
        noPriorsResult.validationResult().diagnostics().toString());
    Assertions.assertTrue(
        noPriorsResult.predictionResult().candidates().stream()
            .flatMap(candidate -> candidate.evidence().stream())
            .noneMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    Assertions.assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION));
    Assertions.assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind()
                            == ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.KINETIC_ANALYSIS));
    Assertions.assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind()
                            == ChemistryWorkflowRecommendationKind.MECHANISM_DISAMBIGUATION
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS));
    Assertions.assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
    final OrganicReactionComputationRoadmap roadmap =
        QuantumChemistry.organicReactionComputationRoadmap(result);
    Assertions.assertEquals(result.request().id(), roadmap.requestId());
    Assertions.assertEquals(result.status(), roadmap.status());
    Assertions.assertEquals(
        result.conditionScenarioPlan().scenarios().size(), roadmap.conditionScenarioCount());
    Assertions.assertEquals(
        result.conditionScenarioEvaluationPlan().evaluations().size(),
        roadmap.evaluatedConditionScenarioCount());
    Assertions.assertEquals(
        result.predictionResult().candidates().size(), roadmap.candidates().size());
    Assertions.assertTrue(roadmap.hasProgramLevelClassicalCalculations());
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.UNCERTAINTY_ANALYSIS));
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.MECHANISM_ENUMERATION));
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.SOLVENT_SCREENING));
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.CATALYST_SCREENING));
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS));
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION));
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS));
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS));
    Assertions.assertTrue(
        roadmap.programLevelRecommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.calculationKind()
                        == ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS));
    Assertions.assertTrue(roadmap.readyForFollowUpWork());
    Assertions.assertTrue(roadmap.hasClassicalCalculations());
    Assertions.assertTrue(roadmap.hasQuantumPreparation());
    final OrganicReactionCandidateRoadmap bestRoadmap = roadmap.candidates().get(0);
    Assertions.assertEquals(0, bestRoadmap.candidateIndex());
    Assertions.assertTrue(bestRoadmap.hasMechanisticRoute());
    Assertions.assertTrue(bestRoadmap.hasHighPriorityWork());
    Assertions.assertTrue(bestRoadmap.hasClassicalCalculations());
    Assertions.assertTrue(bestRoadmap.hasQuantumPreparation());
    Assertions.assertTrue(bestRoadmap.explicitProductGraph());
    Assertions.assertTrue(bestRoadmap.electronicRerankReady());
    Assertions.assertTrue(bestRoadmap.quantumWorkflowReady());
    Assertions.assertEquals("ready", bestRoadmap.readinessReason());
    Assertions.assertTrue(
        bestRoadmap
            .productMoleculeIds()
            .contains(
                result
                    .predictionResult()
                    .bestCandidate()
                    .products()
                    .participants()
                    .get(0)
                    .molecule()
                    .id()
                    .value()));
    Assertions.assertEquals(
        result
            .predictionResult()
            .bestCandidate()
            .products()
            .participants()
            .get(0)
            .molecule()
            .formula()
            .hillNotation(),
        bestRoadmap.productFormulaSignatures().get(0));
    Assertions.assertTrue(
        bestRoadmap
            .productStoichiometrySignatures()
            .get(0)
            .contains(bestRoadmap.productFormulaSignatures().get(0)));
    Assertions.assertTrue(
        bestRoadmap
            .classicalCalculationKinds()
            .contains(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION));
    Assertions.assertTrue(
        bestRoadmap
            .classicalCalculationKinds()
            .contains(ClassicalCalculationKind.BASIS_SET_CONVERGENCE));
    Assertions.assertTrue(
        bestRoadmap
            .classicalCalculationKinds()
            .contains(ClassicalCalculationKind.METHOD_BENCHMARK));
    Assertions.assertTrue(
        bestRoadmap
            .classicalCalculationKinds()
            .contains(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
    Assertions.assertEquals(roadmap, result.computationRoadmap());
    final OrganicReactionDecisionMatrix decisionMatrix =
        QuantumChemistry.organicReactionDecisionMatrix(result);
    Assertions.assertEquals(
        result.predictionResult().candidates().size(), decisionMatrix.rows().size());
    Assertions.assertEquals(
        result.predictionResult().probabilityMass(), decisionMatrix.probabilityMass(), 1.0E-12);
    Assertions.assertTrue(decisionMatrix.probabilityMassComplete());
    Assertions.assertTrue(decisionMatrix.hasClassicalFollowUp());
    Assertions.assertTrue(decisionMatrix.hasQuantumFollowUp());
    Assertions.assertTrue(
        decisionMatrix.programLevelRecommendations().stream()
            .anyMatch(recommendation -> recommendation.startsWith("CONDITION_SCREENING best=")));
    final ChemistryWorkflowResult publicWorkflowResult =
        QuantumChemistry.runWorkflow(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("public_filtered_followup_workflow"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                reaction,
                predictionCalculationRequest,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                request,
                null));
    final List<ClassicalCalculationResult> selectedFollowUpResults =
        QuantumChemistry.runWorkflowFollowUpCalculations(
            publicWorkflowResult, List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS));
    Assertions.assertFalse(selectedFollowUpResults.isEmpty());
    Assertions.assertTrue(
        selectedFollowUpResults.stream()
            .allMatch(
                calculationResult ->
                    calculationResult.hasSection(
                        ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS)));
    Assertions.assertTrue(selectedFollowUpResults.get(0).hasValue("product_distribution_rows"));
    Assertions.assertFalse(
        QuantumChemistry.runWorkflowFollowUpCalculationsAndPlanParity(
                publicWorkflowResult,
                List.of(ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS))
            .empty());
    final OrganicReactionDecisionRow bestDecision = decisionMatrix.rows().get(0);
    Assertions.assertEquals(0, bestDecision.candidateIndex());
    Assertions.assertFalse(bestDecision.productNames().isEmpty());
    Assertions.assertFalse(bestDecision.mechanismFamilies().isEmpty());
    Assertions.assertFalse(bestDecision.mechanismTrace().isEmpty());
    Assertions.assertFalse(bestDecision.evidence().isEmpty());
    Assertions.assertTrue(bestDecision.classicalFollowUpRecommended());
    Assertions.assertTrue(bestDecision.quantumFollowUpRecommended());
    Assertions.assertEquals(
        decisionMatrix, QuantumChemistry.predictOrganicReactionDecisionMatrix(request));
    Assertions.assertEquals(decisionMatrix, result.decisionMatrix());
    final ChemistryStorageRoadmapRecord roadmapRecord =
        QuantumChemistry.roadmapStorageRecord(
            "public_roadmap_record", 1, roadmap, "public API roadmap storage record");
    Assertions.assertEquals(roadmap.requestId(), roadmapRecord.requestId());
    Assertions.assertEquals(roadmap.status().name(), roadmapRecord.status());
    Assertions.assertEquals(roadmap.candidates().size(), roadmapRecord.candidates().size());
    Assertions.assertTrue(roadmapRecord.hasCandidates());
    Assertions.assertTrue(
        roadmapRecord
            .candidates()
            .get(0)
            .classicalCalculationKinds()
            .contains(ClassicalCalculationKind.HAMILTONIAN_PREPARATION.name()));
    Assertions.assertEquals(
        bestRoadmap.quantumPreparationTargets(),
        roadmapRecord.candidates().get(0).quantumPreparationTargets());
    Assertions.assertFalse(roadmapRecord.candidates().get(0).followUpActionTargets().isEmpty());
    Assertions.assertTrue(
        roadmapRecord.candidates().get(0).followUpActionTargets().stream()
            .anyMatch(target -> target.contains("geometry")));
    Assertions.assertEquals(
        bestRoadmap.mechanismDescriptors(),
        roadmapRecord.candidates().get(0).mechanismDescriptors());
    Assertions.assertEquals(
        bestRoadmap.explicitProductGraph(),
        roadmapRecord.candidates().get(0).explicitProductGraph());
    Assertions.assertEquals(
        bestRoadmap.electronicRerankReady(),
        roadmapRecord.candidates().get(0).electronicRerankReady());
    Assertions.assertEquals(
        bestRoadmap.quantumWorkflowReady(),
        roadmapRecord.candidates().get(0).quantumWorkflowReady());
    Assertions.assertEquals(
        bestRoadmap.readinessReason(), roadmapRecord.candidates().get(0).readinessReason());
    final ReactionPredictionExplanation explanation = result.explanation();
    Assertions.assertNotNull(explanation.confidenceLevel());
    Assertions.assertTrue(explanation.conditionAware());
    Assertions.assertEquals(
        ReactionMechanismClass.CROSS_COUPLING, explanation.pathwayAssessment().mechanismClass());
    Assertions.assertNotNull(explanation.pathwayAssessment().regioselectivityClass());
    Assertions.assertTrue(
        explanation.pathwayAssessment().conditionWindow().hasRequiredConditionSignals());
    Assertions.assertTrue(explanation.positiveEvidenceCount() > 0);
    Assertions.assertFalse(explanation.dominantEvidence().isEmpty());

    final ChemistryStoragePredictionRecord record =
        QuantumChemistry.predictionStorageRecord(
            "public_prediction_record",
            0,
            ChemistryStorageReactionRecordStatus.SIMULATED_CLASSICAL,
            result.predictionResult(),
            "public API storage snapshot");
    Assertions.assertEquals("public_heterocycle_prediction", record.requestId());
    Assertions.assertEquals(
        result.predictionResult().candidates().size(), record.candidates().size());
    Assertions.assertEquals(
        ChemistryStorageReactionRecordStatus.SIMULATED_CLASSICAL, record.status());
    final ChemistryStorageComputationPlanRecord computationPlanRecord =
        QuantumChemistry.computationPlanStorageRecord(
            "public_computation_plan",
            "public_reaction_record",
            1,
            ChemistryStorageReactionRecordStatus.PLANNED,
            List.of("public_classical_screen", "public_condition_scan"),
            List.of("public_classical_screen_result"),
            List.of("public_vqe_program", "public_qpe_program"),
            true,
            "deferred user calculation");
    Assertions.assertEquals("public_reaction_record", computationPlanRecord.reactionRecordId());
    Assertions.assertTrue(computationPlanRecord.deferredExecution());
    Assertions.assertTrue(computationPlanRecord.hasPreparedWork());
    Assertions.assertEquals(2, computationPlanRecord.classicalPlanIds().size());
    Assertions.assertEquals(1, computationPlanRecord.classicalResultIds().size());
    Assertions.assertEquals(2, computationPlanRecord.quantumProgramIds().size());
    final ChemistryStorageReactionOutcomeRecord publicOutcome =
        QuantumChemistry.reactionOutcomeStorageRecord(
            "public_outcome",
            "public_reaction",
            1,
            ChemistryStorageReactionOutcomeKind.PREDICTED,
            result.predictionResult().bestCandidate(),
            List.of("public_classical_result"),
            List.of("public_quantum_program"),
            "Public API condition summary.",
            "Public API outcome record.");
    Assertions.assertEquals(ChemistryStorageReactionOutcomeKind.PREDICTED, publicOutcome.kind());
    Assertions.assertFalse(publicOutcome.productMoleculeIds().isEmpty());
    Assertions.assertEquals(1, publicOutcome.quantumProgramIds().size());

    final ClassicalCalculationRequest workflowClassicalRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("public_workflow_classical"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));
    final ChemistryWorkflowResult workflowResult =
        QuantumChemistry.runWorkflow(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("public_workflow_storage"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                reaction,
                workflowClassicalRequest,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                request,
                null));
    Assertions.assertTrue(workflowResult.hasStoragePlan());
    Assertions.assertTrue(workflowResult.hasClassicalCalculationResult());
    Assertions.assertEquals(
        workflowResult.computationParityPlan(),
        QuantumChemistry.workflowComputationParityPlan(workflowResult));
    Assertions.assertFalse(QuantumChemistry.workflowComputationParityPlan(workflowResult).empty());
    final ChemistryStorageDocument workflowDocument =
        QuantumChemistry.workflowStorageDocument(
            workflowResult, "public_workflow_storage_project", "Public workflow storage project");
    Assertions.assertEquals(1, workflowDocument.reactionRecords().size());
    Assertions.assertFalse(workflowDocument.reactionOutcomeRecords().isEmpty());
    Assertions.assertEquals(1, workflowDocument.predictionRecords().size());
    Assertions.assertEquals(1, workflowDocument.computationPlanRecords().size());
    Assertions.assertEquals(1, workflowDocument.roadmapRecords().size());
    Assertions.assertEquals(request.id(), workflowDocument.roadmapRecords().get(0).requestId());
    Assertions.assertFalse(workflowDocument.roadmapRecords().get(0).candidates().isEmpty());
    Assertions.assertFalse(
        workflowDocument.computationPlanRecords().get(0).classicalPlanIds().isEmpty());
    Assertions.assertFalse(
        workflowDocument.computationPlanRecords().get(0).classicalResultIds().isEmpty());
    Assertions.assertFalse(
        workflowDocument.computationPlanRecords().get(0).quantumProgramIds().isEmpty());
    Assertions.assertEquals(1, workflowDocument.classicalResultRecords().size());
    Assertions.assertFalse(workflowDocument.quantumProgramRecords().isEmpty());
    Assertions.assertEquals(
        workflowDocument.computationPlanRecords().get(0).quantumProgramIds(),
        workflowDocument.quantumProgramRecords().stream()
            .map(programRecord -> programRecord.id())
            .toList());
    final ChemistryStorageClassicalResultRecord workflowClassicalResultRecord =
        workflowDocument.classicalResultRecords().get(0);
    Assertions.assertEquals(
        workflowResult.classicalCalculationResult().calculationId().value(),
        workflowClassicalResultRecord.calculationId());
    Assertions.assertEquals(
        workflowResult.classicalCalculationResult().status().name(),
        workflowClassicalResultRecord.status());
    Assertions.assertTrue(workflowClassicalResultRecord.hasSections());
    Assertions.assertEquals(
        workflowResult.classicalCalculationResult().diagnostics().diagnostics().size(),
        workflowClassicalResultRecord.diagnosticCount());
    final ChemistryStorageReactionTimeline timeline =
        QuantumChemistry.reactionStorageTimeline(
            workflowDocument, workflowDocument.reactions().get(0).id().value());
    Assertions.assertEquals(1, timeline.size());
    Assertions.assertEquals(1, timeline.eventCount());
    Assertions.assertEquals(workflowDocument.reactionRecords().get(0), timeline.latest());
    Assertions.assertEquals(
        workflowDocument.computationPlanRecords().get(0).quantumProgramIds(),
        timeline.events().get(0).quantumProgramIds());
    final ChemistryStorageReactionSnapshot snapshot =
        QuantumChemistry.reactionStorageSnapshot(
            workflowDocument, workflowDocument.reactions().get(0).id().value(), 0);
    Assertions.assertEquals(workflowDocument.reactionRecords().get(0), snapshot.reactionRecord());
    Assertions.assertTrue(snapshot.hasPredictions());
    Assertions.assertTrue(snapshot.hasOutcomes());
    Assertions.assertTrue(snapshot.hasComputationPlans());
    Assertions.assertTrue(snapshot.hasQuantumPrograms());

    final ChemistryStorageReactionHistoryEvent publicEvent =
        QuantumChemistry.reactionHistoryStorageEvent(
            "public_history_event",
            "public_workflow_reaction",
            2,
            ChemistryStorageReactionEventKind.OBSERVATION_ADDED,
            "Observed product update",
            "User recorded an experimental product observation.",
            List.of(workflowDocument.reactionRecords().get(0).id()),
            List.of(),
            List.of(workflowDocument.predictionRecords().get(0).id()),
            List.of(workflowDocument.computationPlanRecords().get(0).id()),
            workflowDocument.computationPlanRecords().get(0).classicalResultIds(),
            workflowDocument.computationPlanRecords().get(0).quantumProgramIds());
    Assertions.assertEquals(
        ChemistryStorageReactionEventKind.OBSERVATION_ADDED, publicEvent.kind());
    Assertions.assertTrue(publicEvent.referencesWork());
  }

  @Test
  public void publicFacadeCanCalculateGeneratedPredictionCandidateAndPrepareQuantumWorkflow() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "public_generated_candidate_quantum_preparation",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        QuantumChemistryTest.hydrogenAtom(), StoichiometricCoefficient.of(2)))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.UNKNOWN),
            List.of(),
            "public API generated candidate calculation");

    final OrganicReactionPredictionWorkflowResult result =
        QuantumChemistry.predictOrganicReactionWithGeneratedCandidateCalculations(
            request,
            "public_generated_candidate_calculation",
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_prepare_quantum_workflows", true),
                ClassicalCalculationOption.integer(
                    "reaction_prediction_prepare_quantum_workflows_max_candidates", 1),
                ClassicalCalculationOption.integer(
                    "reaction_prediction_prepare_quantum_workflows_max_participants", 1)));

    Assertions.assertNotEquals(
        OrganicReactionPredictionWorkflowStatus.FAILED,
        result.status(),
        result.validationResult().diagnostics().toString());
    Assertions.assertTrue(result.hasPredictionResult());
    Assertions.assertTrue(result.predictionResult().hasCandidates());
    Assertions.assertTrue(result.hasClassicalCalculationResult());
    Assertions.assertEquals(
        "true",
        QuantumChemistryTest.value(
            result.classicalCalculationResult(), "candidate_quantum_workflow_preparation_enabled"));
    final String quantumPreparationTable =
        String.join(
            "\n",
            QuantumChemistryTest.values(
                result.classicalCalculationResult(), "candidate_quantum_workflow_preparation"));
    Assertions.assertTrue(quantumPreparationTable.contains("prepared"), quantumPreparationTable);
    Assertions.assertTrue(quantumPreparationTable.contains("true"), quantumPreparationTable);
  }

  @Test
  public void publicFacadeReportsPartialWhenNoCandidatesAreAvailable() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "public_empty_prediction",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        QuantumChemistryTest.ringMolecule("diazine", "N"),
                        StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS),
            List.of(),
            "no curated candidate or generated template product");

    final OrganicReactionPredictionWorkflowResult result =
        QuantumChemistry.predictOrganicReaction(request);

    Assertions.assertEquals(OrganicReactionPredictionWorkflowStatus.PARTIAL, result.status());
    Assertions.assertTrue(result.hasPredictionResult());
    Assertions.assertTrue(result.hasExplanation());
    Assertions.assertTrue(result.hasConditionScenarioPlan());
    Assertions.assertTrue(result.hasConditionScenarioEvaluationPlan());
    Assertions.assertFalse(result.predictionResult().hasCandidates());
    Assertions.assertEquals(0.0, result.predictionResult().probabilityMass(), 1.0E-12);
    Assertions.assertEquals(1.0, result.predictionResult().missingProbabilityMass(), 1.0E-12);
    Assertions.assertFalse(result.hasRecommendations());
    final OrganicReactionComputationRoadmap roadmap =
        QuantumChemistry.organicReactionComputationRoadmap(result);
    Assertions.assertEquals(request.id(), roadmap.requestId());
    Assertions.assertEquals(result.status(), roadmap.status());
    Assertions.assertFalse(roadmap.hasCandidates());
    Assertions.assertFalse(roadmap.readyForFollowUpWork());
    Assertions.assertEquals(
        result.conditionScenarioPlan().scenarios().size(), roadmap.conditionScenarioCount());
    Assertions.assertEquals(
        result.conditionScenarioEvaluationPlan().evaluations().size(),
        roadmap.evaluatedConditionScenarioCount());
  }

  @Test
  public void publicFacadeExposesConditionRepairForIncompleteCrossCoupling() {
    final Molecule heteroarylReactant =
        QuantumChemistryTest.ringMolecule("public_cross_coupling_reactant", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "public_cross_coupling_repair",
            ReactionSide.of(
                List.of(ReactionParticipant.of(heteroarylReactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(QuantumChemistryTest.candidate(heteroarylReactant, 0.7)),
            "public API cross-coupling condition repair");

    final OrganicReactionPredictionWorkflowResult result =
        QuantumChemistry.predictOrganicReaction(request);

    Assertions.assertEquals(OrganicReactionPredictionWorkflowStatus.SUCCESS, result.status());
    Assertions.assertTrue(result.hasRecommendations());
    Assertions.assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.CONDITION_REPAIR
                        && "conditions/catalyst".equals(recommendation.target())));
    Assertions.assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.CONDITION_REPAIR
                        && "conditions/base".equals(recommendation.target())));
  }

  @Test
  public void publicFacadePlansImmutableReactionConditionScenarioMatrix() {
    final Molecule diazine = QuantumChemistryTest.ringMolecule("scenario_diazine", "N");
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(60.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(8.0, TimeDurationUnit.HOUR),
            List.of(ReactionConditionComponent.named(ReactionComponentRole.CATALYST, "Pd(PPh3)4")),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
            "contradictory acid/base with catalyst component");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "public_condition_scenario_plan",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            conditions,
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(QuantumChemistryTest.candidate(diazine, 0.5)),
            "public API condition scenario planning");

    final ReactionConditionScenarioPlan plan =
        QuantumChemistry.planReactionConditionScenarios(request);
    final ReactionConditionScenarioEvaluationPlan evaluationPlan =
        QuantumChemistry.evaluateReactionConditionScenarios(request);

    Assertions.assertFalse(plan.empty());
    Assertions.assertFalse(evaluationPlan.empty());
    Assertions.assertEquals(plan.scenarios().size(), evaluationPlan.evaluations().size());
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.BASELINE));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.ROOM_TEMPERATURE));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.HEATED));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.COOLED));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.INERT_ANHYDROUS));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.CATALYST_SIGNAL));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.ACID_ONLY));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.BASE_ONLY));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.SHORT_DURATION));
    Assertions.assertTrue(plan.hasKind(ReactionConditionScenarioKind.LONG_DURATION));
    Assertions.assertSame(conditions, plan.scenarios().get(0).conditions());
    Assertions.assertFalse(conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT));
    Assertions.assertNotNull(evaluationPlan.bestEvaluation());
    Assertions.assertTrue(evaluationPlan.bestEvaluation().rankingScore() > 0.0);
    Assertions.assertTrue(
        plan.scenarios().stream()
            .filter(scenario -> scenario.kind() == ReactionConditionScenarioKind.CATALYST_SIGNAL)
            .findFirst()
            .orElseThrow()
            .conditions()
            .hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT));
  }

  @Test
  public void publicFacadeAnalyzesOrganicConditionEffects() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "public_condition_effects",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        QuantumChemistryTest.ringMolecule("effect_diazine", "N"),
                        StoichiometricCoefficient.ONE))),
            QuantumChemistryTest.heterocycleConditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            "condition effect analysis");

    final ReactionConditionEffectProfile profile =
        QuantumChemistry.analyzeReactionConditionEffects(request);

    Assertions.assertFalse(profile.empty());
    Assertions.assertTrue(profile.scoreAdjustment() > 0.0);
    Assertions.assertTrue(profile.supportCount() > 0);
    Assertions.assertEquals(0, profile.riskCount());
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SOLVENT_HIGH_BOILING));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.THERMAL_ACTIVATION));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.INERT_DRY_CONTROL));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.HETEROCYCLE_COMPATIBILITY));
  }

  @Test
  public void publicFacadeMarksContradictoryConditionEffectsAsRisk() {
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(25.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(1.0, TimeDurationUnit.HOUR),
            List.of(),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.ACIDIC_MEDIUM),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.AIR_SENSITIVE)),
            "contradictory condition effect profile");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "public_condition_effect_risks",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        QuantumChemistryTest.ringMolecule("risk_diazine", "N"),
                        StoichiometricCoefficient.ONE))),
            conditions,
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(),
            "risk effect analysis");

    final ReactionConditionEffectProfile profile =
        QuantumChemistry.analyzeReactionConditionEffects(request);

    Assertions.assertFalse(profile.empty());
    Assertions.assertTrue(profile.riskCount() >= 2);
    Assertions.assertTrue(profile.scoreAdjustment() < 0.0);
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.CONTRADICTORY_ACID_BASE));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.AIR_MOISTURE_RISK));
    Assertions.assertFalse(profile.riskEffects().isEmpty());
  }

  @Test
  public void publicFacadeExposesChemistryLibrarySearch() {
    final List<ChemistryLibraryEntry> entries =
        QuantumChemistry.searchLibrary(
            ChemistryLibraryQuery.builder().text("acetone").limit(3).build());

    Assertions.assertFalse(entries.isEmpty());
    Assertions.assertEquals(ChemistryLibraryEntryKind.MOLECULE, entries.get(0).kind());
    Assertions.assertEquals(
        "molecule.acetone", QuantumChemistry.requireLibraryEntry("molecule.acetone").id());
  }

  @Test
  public void publicFacadePredictsOrganicReactionFromLibraryMoleculeIds() {
    final OrganicReactionPredictionWorkflowResult result =
        QuantumChemistry.predictOrganicReactionFromLibraryMolecules(
            "public_library_snar_prediction",
            List.of(
                "molecule.two_chloro_four_six_dimethyl_three_pyridinecarbonitrile",
                "molecule.benzylamine"),
            QuantumChemistryTest.heterocycleConditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of());

    Assertions.assertEquals(OrganicReactionPredictionWorkflowStatus.SUCCESS, result.status());
    Assertions.assertTrue(result.hasPredictionResult());
    Assertions.assertTrue(result.predictionResult().hasCandidates());
    Assertions.assertTrue(
        result.predictionResult().bestCandidate().predictedMechanisms().stream()
            .anyMatch(
                mechanism ->
                    mechanism.family().name().contains("NUCLEOPHILIC_AROMATIC_SUBSTITUTION")));
    Assertions.assertTrue(
        result.predictionResult().bestCandidate().products().participants().stream()
            .anyMatch(
                participant ->
                    participant.molecule().formula().hillNotation().contains("C15H15N3")));
    final OrganicReactionComputationRoadmap roadmap =
        QuantumChemistry.organicReactionComputationRoadmap(result);
    Assertions.assertTrue(roadmap.readyForFollowUpWork());
    Assertions.assertTrue(roadmap.hasClassicalCalculations());
    Assertions.assertTrue(roadmap.hasQuantumPreparation());
    Assertions.assertTrue(roadmap.candidates().get(0).hasProductFormulaSignatures());
    Assertions.assertTrue(
        roadmap.candidates().get(0).productFormulaSignatures().stream()
            .anyMatch(signature -> signature.contains("C15H15N3")));
    final ChemistryStorageRoadmapRecord storageRoadmap =
        QuantumChemistry.roadmapStorageRecord(
            "public_library_snar_roadmap", 0, roadmap, "library SNAr workflow roadmap");
    Assertions.assertTrue(
        storageRoadmap.candidates().get(0).productFormulaSignatures().stream()
            .anyMatch(signature -> signature.contains("C15H15N3")));
    Assertions.assertTrue(
        storageRoadmap.candidates().get(0).productStoichiometrySignatures().stream()
            .anyMatch(signature -> signature.contains("C15H15N3")));
  }

  @Test
  public void publicFacadePlansQuantumChemistryErrorMitigation() {
    final ErrorMitigationPlan plan =
        QuantumChemistry.planErrorMitigation(
            ErrorMitigationRequest.of(
                ActiveSpace.of(4, 4),
                QuantumNoiseModel.of(QuantumNoiseModelKind.DEPOLARIZING, 0.002, 0.02, 0.03),
                List.of(
                    ErrorMitigationTechnique.READOUT_CALIBRATION,
                    ErrorMitigationTechnique.ZERO_NOISE_EXTRAPOLATION,
                    ErrorMitigationTechnique.SYMMETRY_VERIFICATION,
                    ErrorMitigationTechnique.PARTICLE_NUMBER_POST_SELECTION,
                    ErrorMitigationTechnique.SPIN_PARITY_POST_SELECTION,
                    ErrorMitigationTechnique.PAULI_TWIRLING),
                1000,
                true,
                true));

    Assertions.assertEquals(ErrorMitigationStatus.PLANNED, plan.status());
    Assertions.assertTrue(plan.recommendedShots() > 1000);
    Assertions.assertTrue(plan.techniques().contains(ErrorMitigationTechnique.READOUT_CALIBRATION));
    Assertions.assertFalse(plan.hasErrors());
  }

  @Test
  public void publicFacadePreparesCompleteQuantumProgramPackage() {
    final ChemistryQuantumProgramPackage pack =
        QuantumChemistry.prepareQuantumPrograms(
            "public_h2_quantum",
            QuantumChemistryTest.hydrogenHamiltonian(),
            FermionToQubitMapping.JORDAN_WIGNER);

    Assertions.assertTrue(pack.hasArtifacts());
    Assertions.assertFalse(pack.artifactIds().isEmpty());
    Assertions.assertTrue(pack.totalOperationCount() > 0L);
    Assertions.assertTrue(Quantum.validate(pack.primaryProgram()).isValid());
    Assertions.assertTrue(
        pack.artifacts().stream()
            .anyMatch(
                artifact ->
                    artifact.kind() == ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT));
    Assertions.assertTrue(
        pack.artifacts().stream()
            .anyMatch(artifact -> artifact.kind() == ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    Assertions.assertTrue(
        pack.artifacts().stream()
            .anyMatch(artifact -> artifact.kind() == ChemistryQuantumProgramKind.UCCSD_ANSATZ));
    Assertions.assertTrue(
        pack.artifacts().stream()
            .anyMatch(
                artifact ->
                    artifact.kind() == ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
  }

  @Test
  public void publicFacadePreparesHamiltonianMappingAndQuantumPackageFromCalculationRequest() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("public_native_hamiltonian_to_quantum_programs"),
            QuantumChemistryTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(
                ClassicalCalculationKind.HAMILTONIAN_PREPARATION,
                ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
            List.of(),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ElectronicHamiltonian hamiltonian =
        QuantumChemistry.prepareElectronicHamiltonian(request);
    final var verified =
        QuantumChemistry.verifyHamiltonianMapping(hamiltonian, FermionToQubitMapping.JORDAN_WIGNER);
    final ChemistryQuantumProgramPackage pack =
        QuantumChemistry.prepareQuantumPrograms(request, FermionToQubitMapping.JORDAN_WIGNER);

    Assertions.assertEquals(2, hamiltonian.activeSpace().electronCount());
    Assertions.assertEquals(FermionToQubitMapping.JORDAN_WIGNER, verified.mapping());
    Assertions.assertTrue(verified.qubitOperator().termCount() > 0);
    Assertions.assertTrue(pack.hasArtifacts());
    Assertions.assertTrue(Quantum.validate(pack.primaryProgram()).isValid());
  }

  @Test
  public void publicFacadeRunsWorkflowVqeWhenBackendIsProvided() {
    final VqeOptimizationRequest vqeRequest =
        VqeOptimizationRequest.of(
            ActiveSpace.of(2, 2),
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE,
                "public_workflow_vqe",
                "q",
                "c",
                "theta",
                1,
                4,
                true),
            new double[] {0.0},
            VqeOptimizationOptions.of(16, 256, 0.002, 1.0E-5, 1.0E-12, 0.001, 1.0E-3, 1.0E-8),
            ErrorMitigationRequest.of(
                ActiveSpace.of(2, 2),
                QuantumNoiseModel.ideal(),
                List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                128,
                false,
                false));
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder("public_workflow_vqe", QuantumChemistryTest.hydrogen())
            .activeSpace(ActiveSpace.of(2, 2))
            .reactionConditions(QuantumChemistryTest.heterocycleConditions())
            .domains(List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION))
            .seedCandidates(
                List.of(QuantumChemistryTest.candidate(QuantumChemistryTest.hydrogen(), 1.0)))
            .mapping(FermionToQubitMapping.JORDAN_WIGNER)
            .vqeOptimizationRequest(vqeRequest)
            .note("public workflow VQE")
            .build();

    final ChemistryWorkflowResult result =
        QuantumChemistry.runWorkflow(
            request,
            (program, parameters, shots, mitigationPlan) ->
                VqeEnergyEvaluation.success(parameters[0] * parameters[0], 0.0, shots));

    Assertions.assertNotEquals(
        ChemistryWorkflowStatus.FAILED, result.status(), result.diagnostics().toString());
    Assertions.assertTrue(result.hasVqeOptimizationResult());
    Assertions.assertEquals(
        VqeConvergenceStatus.CONVERGED, result.vqeOptimizationResult().status());
    Assertions.assertEquals(0.0, result.vqeOptimizationResult().bestEnergy(), 1.0E-15);
    Assertions.assertTrue(result.hasQuantumProgramPackage());
    Assertions.assertTrue(result.hasStoragePlan());
    Assertions.assertTrue(result.storagePlan().hasExtensions());
    Assertions.assertEquals("quantum_vqe_result", result.storagePlan().extensions().get(0).kind());
    Assertions.assertEquals(
        "CONVERGED", result.storagePlan().extensions().get(0).properties().get("status"));
    final ChemistryStorageDocument document =
        QuantumChemistry.workflowStorageDocument(
            result, "public_workflow_vqe_project", "Public workflow VQE project");
    final ChemistryStorageResult<String> written = new ChemistryStorageService().write(document);
    Assertions.assertTrue(written.success(), written.diagnostics().toString());
    Assertions.assertTrue(written.value().contains("extension kind=quantum_vqe_result"));
    Assertions.assertTrue(written.value().contains("bestEnergy"));
    final ChemistryStorageResult<ChemistryStorageDocument> read =
        new ChemistryStorageService().read(written.value());
    Assertions.assertTrue(read.success(), read.diagnostics().toString());
    Assertions.assertEquals(document, read.value());
    Assertions.assertEquals(
        "CONVERGED", read.value().extensions().get(0).properties().get("status"));
    Assertions.assertEquals(request.toJob(), result.job());
  }

  @Test
  public void publicFacadePreparesOpenShellRadicalQuantumWorkflow() {
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder(
                "public_hydrogen_radical_quantum", QuantumChemistryTest.hydrogenAtom())
            .activeSpace(ActiveSpace.of(1, 1))
            .reactionConditions(QuantumChemistryTest.heterocycleConditions())
            .domains(List.of(OrganicReactionDomain.OXIDATION_REDUCTION))
            .seedCandidates(
                List.of(QuantumChemistryTest.candidate(QuantumChemistryTest.hydrogenAtom(), 1.0)))
            .mapping(FermionToQubitMapping.JORDAN_WIGNER)
            .note("public open-shell radical quantum workflow")
            .build();

    final ChemistryWorkflowResult result = QuantumChemistry.runWorkflow(request);

    Assertions.assertNotEquals(
        ChemistryWorkflowStatus.FAILED, result.status(), result.diagnostics().toString());
    Assertions.assertTrue(result.hasClassicalCalculationResult());
    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.classicalCalculationResult().status(),
        result.classicalCalculationResult().diagnostics().toString());
    Assertions.assertNotNull(result.electronicProblem());
    Assertions.assertEquals(
        1, result.electronicProblem().hamiltonian().activeSpace().electronCount());
    Assertions.assertEquals(
        1, result.electronicProblem().hamiltonian().activeSpace().orbitalCount());
    Assertions.assertTrue(result.hasQuantumProgramPackage());
    Assertions.assertTrue(result.quantumProgramPackage().hasArtifacts());
    Assertions.assertTrue(
        Quantum.validate(result.quantumProgramPackage().primaryProgram()).isValid());
    Assertions.assertTrue(result.hasStoragePlan());
  }

  @Test
  public void publicFacadeRunsVqeWithGradientConvergenceEvidence() {
    final VqeOptimizationRequest request =
        VqeOptimizationRequest.of(
            ActiveSpace.of(2, 2),
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE,
                "public_vqe",
                "q",
                "c",
                "theta",
                1,
                4,
                true),
            new double[] {0.0},
            VqeOptimizationOptions.of(16, 256, 0.002, 1.0E-5, 1.0E-12, 0.001, 1.0E-3, 1.0E-8),
            ErrorMitigationRequest.of(
                ActiveSpace.of(2, 2),
                QuantumNoiseModel.ideal(),
                List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                128,
                false,
                false));

    final VqeOptimizationResult result =
        QuantumChemistry.runVqe(
            request,
            (program, parameters, shots, mitigationPlan) ->
                VqeEnergyEvaluation.success(parameters[0] * parameters[0], 0.0, shots));

    Assertions.assertEquals(VqeConvergenceStatus.CONVERGED, result.status());
    Assertions.assertTrue(result.diagnostic().contains("gradient"));
    Assertions.assertEquals(0.0, result.bestEnergy(), 1.0E-15);
    Assertions.assertEquals(
        0.0, result.iterations().get(result.iterations().size() - 1).gradientNorm(), 1.0E-12);
    Assertions.assertTrue(result.totalShots() >= result.evaluationCount() * 256L);
  }

  @Test
  public void publicFacadeAggregatesPauliMeasurementEnergyFromCountsAndSimulationResults() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.IDENTITY, 0.25)
            .add(QubitPauliTerm.of(0, PauliAxis.Z), 0.5)
            .build();

    final PauliExpectationMeasurementResult countResult =
        QuantumChemistry.aggregatePauliMeasurementEnergy(
            hamiltonian, List.of(Map.of("0", 75L, "1", 25L)));
    final PauliExpectationMeasurementResult simulationResult =
        QuantumChemistry.aggregatePauliMeasurementEnergyFromSimulationResults(
            hamiltonian,
            List.of(
                SimulationResult.of(1, 1, 100, Map.of("0", 75L, "1", 25L), List.of(), List.of())));

    Assertions.assertEquals(0.5, countResult.termExpectation(0), 1.0E-12);
    Assertions.assertEquals(0.25, countResult.termContribution(0), 1.0E-12);
    Assertions.assertEquals(0.5, countResult.energy(), 1.0E-12);
    Assertions.assertEquals(countResult.energy(), simulationResult.energy(), 1.0E-12);

    final ChemistryStorageExtension extension =
        QuantumChemistry.pauliMeasurementEnergyStorageExtension(
            "public_pauli_measurement_energy", "public_hamiltonian", countResult);
    Assertions.assertEquals("quantum_pauli_measurement_energy", extension.kind());
    Assertions.assertEquals("public_hamiltonian", extension.properties().get("source"));
    Assertions.assertEquals("0.5", extension.properties().get("energy"));
    Assertions.assertEquals("1", extension.properties().get("measured_term_count"));
    Assertions.assertTrue(extension.bodyLines().get(0).contains("contribution=0.25"));
  }

  @Test
  public void publicFacadeSelectsAdaptVqeOperatorsAndReturnsIrProgram() {
    final ActiveSpace activeSpace = ActiveSpace.of(2, 2);
    final AdaptVqeSelectionResult result =
        QuantumChemistry.selectAdaptVqe(
            activeSpace,
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.ADAPT_VQE_OPERATOR_POOL,
                "public_adapt_vqe",
                "q",
                "c",
                "theta",
                1,
                8,
                true),
            AdaptVqeSelectionOptions.of(2, 3, 256, 0.125, 1.0E-5, 0),
            ErrorMitigationRequest.of(
                activeSpace,
                QuantumNoiseModel.ideal(),
                List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                256,
                true,
                true),
            (program, parameters, shots, mitigationPlan) -> {
              if (parameters.length == 0) {
                return VqeEnergyEvaluation.success(0.0, 0.0, shots);
              }
              if (QuantumChemistryTest.containsDoubleExcitationParameter(program)) {
                return VqeEnergyEvaluation.success(-0.5, 0.0, shots);
              }
              return VqeEnergyEvaluation.success(-0.01, 0.0, shots);
            });

    Assertions.assertEquals(AdaptVqeSelectionStatus.CONVERGED, result.status());
    Assertions.assertEquals(1, result.selectedExcitations().size());
    Assertions.assertEquals(
        ChemistryExcitationRank.DOUBLE, result.selectedExcitations().get(0).rank());
    Assertions.assertEquals(1, result.selectedParametersCopy().length);
    Assertions.assertTrue(result.bestEnergy() < result.referenceEnergy());
    Assertions.assertTrue(Quantum.validate(result.selectedProgram()).isValid());
    Assertions.assertTrue(
        QuantumChemistryTest.containsDoubleExcitationParameter(result.selectedProgram()));
  }

  @Test
  public void publicFacadeVerifiesFermionToQubitMappingSpectralEquivalence() {
    final FermionOperator operator =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(0), FermionTerm.annihilation(0)), 0.5)
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(1)), -0.25)
            .build();

    final MappingSpectralVerification.MappingSpectralVerificationResult result =
        QuantumChemistry.verifyFermionToQubitMapping(
            operator, FermionToQubitMapping.BRAVYI_KITAEV, 2, 3, 1.0E-10);

    Assertions.assertTrue(result.equivalentWithinTolerance());
    Assertions.assertEquals(FermionToQubitMapping.BRAVYI_KITAEV, result.mapping());
    Assertions.assertEquals(2, result.spinOrbitalCount());
    Assertions.assertEquals(3, result.momentCount());
    Assertions.assertEquals(0.0, result.maxDifference(), 1.0E-10);
  }

  @Test
  public void publicFacadePreflightsPlansAndRunsLocalClassicalDft() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("public_hydrogen_dft"),
            QuantumChemistryTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("B3LYP"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalPreflightResult preflight =
        QuantumChemistry.preflightClassicalCalculation(request);
    final ClassicalCalculationPlan plan = QuantumChemistry.planClassicalCalculation(request);
    final ClassicalCalculationResult result = QuantumChemistry.runClassicalCalculation(request);

    Assertions.assertEquals(ClassicalPreflightStatus.NEEDS_APPROXIMATION, preflight.status());
    Assertions.assertTrue(
        preflight.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
    Assertions.assertEquals(1, plan.steps().size());
    Assertions.assertEquals(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, plan.steps().get(0).kind());
    Assertions.assertEquals(ClassicalCalculationStatus.PARTIAL, result.status());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.code() == ClassicalDiagnosticCode.RESULT_PARTIAL));
    Assertions.assertEquals("DFT_KOHN_SHAM", QuantumChemistryTest.value(result, "method"));
    Assertions.assertEquals("B3LYP", QuantumChemistryTest.value(result, "functional"));
    Assertions.assertEquals("HYBRID_GGA", QuantumChemistryTest.value(result, "functional_family"));
    Assertions.assertEquals(
        "KOHN_SHAM_DFT", QuantumChemistryTest.value(result, "reference_method"));
    Assertions.assertTrue(
        Double.isFinite(Double.parseDouble(QuantumChemistryTest.value(result, "total_energy"))));
  }

  @Test
  public void publicFacadeRunsPostHartreeFockThroughClassicalWorkflow() {
    final ChemistryWorkflowRequest request =
        ChemistryWorkflowRequest.builder("public_workflow_h2_ccsd", QuantumChemistryTest.hydrogen())
            .mode(ChemistryWorkflowMode.CLASSICAL_ONLY)
            .method(
                ElectronicStructureMethod.of(
                    ElectronicStructureMethodType.COUPLED_CLUSTER,
                    ElectronicStructureMethodName.of("CCSD")))
            .basisSet(BasisSet.STO_3G)
            .calculationKinds(List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY))
            .note("public post-HF workflow")
            .build();

    final ChemistryWorkflowResult result = QuantumChemistry.runWorkflow(request);

    Assertions.assertNotEquals(
        ChemistryWorkflowStatus.FAILED,
        result.status(),
        result.diagnostics().diagnostics().toString());
    Assertions.assertTrue(result.hasClassicalCalculationResult());
    Assertions.assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.classicalCalculationResult().status(),
        result.classicalCalculationResult().diagnostics().toString());
    Assertions.assertEquals(
        "CCSD", QuantumChemistryTest.value(result.classicalCalculationResult(), "method"));
    Assertions.assertTrue(
        Double.parseDouble(
                QuantumChemistryTest.value(
                    result.classicalCalculationResult(), "correlation_energy"))
            < 0.0);
    Assertions.assertTrue(
        Double.isFinite(
            Double.parseDouble(
                QuantumChemistryTest.value(result.classicalCalculationResult(), "total_energy"))));
    Assertions.assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.CLASSICAL_EXECUTION, ChemistryWorkflowStageStatus.COMPLETED));
    Assertions.assertTrue(result.hasStoragePlan());
    Assertions.assertTrue(result.storagePlan().hasClassicalResultRecords());
  }

  @Test
  public void publicFacadeAnalyzesComputationalChemistryCapability() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("public_capability_h2"),
            QuantumChemistryTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(
                ClassicalCalculationKind.SINGLE_POINT_ENERGY,
                ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report =
        QuantumChemistry.analyzeComputationalCapabilities(request);

    Assertions.assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("integrals.full-eri").status());
    Assertions.assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("fermion.mapping").status());
    Assertions.assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("quantum.vqe-loop").status());
  }

  @Test
  public void publicFacadeReportsQuantumMethodAsQuantumPreparationWorkflow() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("public_capability_vqe"),
            QuantumChemistryTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.QUANTUM_VARIATIONAL,
                ElectronicStructureMethodName.of("VQE")),
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
            List.of(),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report =
        QuantumChemistry.analyzeComputationalCapabilities(request);

    Assertions.assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("scf.method").status());
    Assertions.assertTrue(
        report.requireItem("scf.method").message().contains("Quantum IR preparation"));
    Assertions.assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("quantum.vqe-loop").status());
  }

  private static ReactionConditions heterocycleConditions() {
    return ReactionConditions.of(
        Temperature.of(80.0, TemperatureUnit.CELSIUS),
        null,
        TimeDuration.of(8.0, TimeDurationUnit.HOUR),
        List.of(
            ReactionConditionComponent.of(
                ReactionComponentRole.SOLVENT,
                "DMSO",
                null,
                null,
                ReactionPhase.SOLUTION,
                "polar aprotic solvent"),
            ReactionConditionComponent.of(
                ReactionComponentRole.CATALYST,
                "CuI",
                null,
                null,
                null,
                ReactionComponentLoading.percent(10.0),
                null,
                ReactionPhase.SOLID,
                "heterocycle coupling catalyst")),
        List.of(
            ReactionConditionFeature.named(ReactionConditionFeatureKind.ANHYDROUS),
            ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE),
            ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
            ReactionConditionFeature.named(ReactionConditionFeatureKind.CATALYST_PRESENT)),
        "public API condition-rich heterocycle workflow");
  }

  private static ReactionProductCandidate candidate(
      final Molecule molecule, final double probability) {
    return ReactionProductCandidate.of(
        ReactionSide.of(List.of(ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE))),
        probability,
        probability * 100.0,
        "public API seed candidate",
        List.of(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.USER_RULE,
                "candidate supplied by public API test",
                0.1)));
  }

  private static Molecule ringMolecule(final String id, final String firstAtomSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            QuantumChemistryTest.ringAtom("a0", firstAtomSymbol),
            QuantumChemistryTest.ringAtom("a1", "C"),
            QuantumChemistryTest.ringAtom("a2", "C"),
            QuantumChemistryTest.ringAtom("a3", "N".equals(firstAtomSymbol) ? "N" : "C"),
            QuantumChemistryTest.ringAtom("a4", "C"),
            QuantumChemistryTest.ringAtom("a5", "C")),
        List.of(
            QuantumChemistryTest.ringBond("a0", "a1"),
            QuantumChemistryTest.ringBond("a1", "a2"),
            QuantumChemistryTest.ringBond("a2", "a3"),
            QuantumChemistryTest.ringBond("a3", "a4"),
            QuantumChemistryTest.ringBond("a4", "a5"),
            QuantumChemistryTest.ringBond("a5", "a0")),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule acyclicMolecule(final String id, final String firstAtomSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            QuantumChemistryTest.ringAtom("a0", firstAtomSymbol),
            QuantumChemistryTest.ringAtom("a1", "C"),
            QuantumChemistryTest.ringAtom("a2", "C"),
            QuantumChemistryTest.ringAtom("a3", "N".equals(firstAtomSymbol) ? "N" : "C"),
            QuantumChemistryTest.ringAtom("a4", "C"),
            QuantumChemistryTest.ringAtom("a5", "C")),
        List.of(
            QuantumChemistryTest.ringBond("a0", "a1"),
            QuantumChemistryTest.ringBond("a1", "a2"),
            QuantumChemistryTest.ringBond("a2", "a3"),
            QuantumChemistryTest.ringBond("a3", "a4"),
            QuantumChemistryTest.ringBond("a4", "a5")),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogen() {
    return Molecule.of(
        MoleculeId.of("h2"),
        "Hydrogen",
        List.of(
            Atom.of(
                AtomId.of("h0"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("h1"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.74, LengthUnit.ANGSTROM))),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogenAtom() {
    return Molecule.of(
        MoleculeId.of("h_radical"),
        "Hydrogen radical",
        List.of(
            Atom.of(
                AtomId.of("h0"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM))),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static ElectronicHamiltonian hydrogenHamiltonian() {
    return ElectronicHamiltonian.of(
        ActiveSpace.of(2, 2),
        0.713753993687618,
        List.of(
            OneElectronIntegral.of(0, 0, -1.252463573565),
            OneElectronIntegral.of(1, 1, -0.475948715221)),
        List.of(
            TwoElectronIntegral.of(0, 0, 0, 0, 0.674488766357),
            TwoElectronIntegral.of(0, 0, 1, 1, 0.663468096424),
            TwoElectronIntegral.of(0, 1, 0, 1, 0.181288808211),
            TwoElectronIntegral.of(1, 1, 1, 1, 0.697393767423)));
  }

  private static String optionValue(final ChemistryWorkflowRequest request, final String name) {
    final List<ClassicalCalculationOption> options = request.calculationOptions();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(name)) {
        return option.value();
      }
    }
    throw new IllegalArgumentException("Chemistry workflow option is absent: " + name);
  }

  private static String value(final ClassicalCalculationResult result, final String name) {
    return QuantumChemistryTest.values(result, name).get(0);
  }

  private static List<String> values(final ClassicalCalculationResult result, final String name) {
    final List<ClassicalResultSection> sections = result.sections();
    for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
      final List<ClassicalResultValue> values = sections.get(sectionIndex).values();
      for (int valueIndex = 0; valueIndex < values.size(); ++valueIndex) {
        final ClassicalResultValue value = values.get(valueIndex);
        if (value.name().equals(name)) {
          return value.values();
        }
      }
    }
    throw new IllegalArgumentException("Classical result value is absent: " + name);
  }

  private static Atom ringAtom(final String id, final String symbol) {
    final int index = id.charAt(1) - '0';
    return Atom.of(
        AtomId.of(id),
        ElementSymbol.of(symbol),
        Coordinate3D.of(
            Math.cos(index * Math.PI / 3.0),
            Math.sin(index * Math.PI / 3.0),
            0.0,
            LengthUnit.ANGSTROM));
  }

  private static boolean containsDoubleExcitationParameter(final QuantumProgram program) {
    for (int circuit = 0; circuit < program.circuitCount(); circuit++) {
      for (int operation = 0; operation < program.circuit(circuit).operationCount(); operation++) {
        if (program.circuit(circuit).operation(operation) instanceof GateOperation gate) {
          for (int parameter = 0; parameter < gate.parameterCount(); parameter++) {
            if (containsDoubleSymbol(gate.parameter(parameter))) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean containsDoubleSymbol(final ParameterExpression expression) {
    if (expression.isNamed()) {
      return expression.name().contains("_double_");
    }
    if (expression.isUnary()) {
      return containsDoubleSymbol(expression.left());
    }
    return expression.isBinary()
        && (containsDoubleSymbol(expression.left()) || containsDoubleSymbol(expression.right()));
  }

  private static Bond ringBond(final String first, final String second) {
    return Bond.of(AtomId.of(first), AtomId.of(second), BondType.AROMATIC);
  }
}
