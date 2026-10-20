/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlanner;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionExplainer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageItem;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCoverageStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInClassicalBackendProfiles;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTask;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTaskType;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisAtomDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShell;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShellAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionMechanismClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionConfidenceLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicProblemId;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicStructureProblem;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmountUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ChemistryQuantumProgramPackageGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ElectronicHamiltonianProgramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.mitigation.ErrorMitigationPlanner;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.StateVectorPauliVqeEnergyBackend;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationTechnique;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModel;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModelKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramArtifact;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackageOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramSkip;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeConvergenceStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionEventKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionTimeline;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryCapabilityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryComputationParityPlanner;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryFollowUpCalculationPlanner;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryWorkflowStoragePlanner;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryWorkflowService;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ReactionPredictionFollowUpPlanner;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic.ChemistryWorkflowDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic.ChemistryWorkflowDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.followup.ChemistryFollowUpCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJobId;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowMode;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityItem;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity.ChemistryComputationParityStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationKind;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPriority;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowCandidateExecutionSummary;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowCandidateTrace;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowExecutionBundle;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.result.ChemistryWorkflowStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStage;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStageResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStageStatus;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.storage.ChemistryWorkflowStoragePlan;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

public final class ChemistryWorkflowServiceTest {

  @Test
  public void validateOnlyRunsCoreValidationAndSkipsOtherStages() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("validate_h2"),
                ChemistryWorkflowMode.VALIDATE_ONLY,
                ChemistryWorkflowServiceTest.hydrogen(),
                null,
                null,
                null));

    assertTrue(
        result.status() == ChemistryWorkflowStatus.SUCCESS
            || result.status() == ChemistryWorkflowStatus.PARTIAL,
        ChemistryWorkflowServiceTest.workflowStageSummary(result)
            + " "
            + result.diagnostics().diagnostics());
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.HAMILTONIAN_PREPARATION, ChemistryWorkflowStageStatus.SKIPPED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.FERMION_MAPPING, ChemistryWorkflowStageStatus.SKIPPED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
            ChemistryWorkflowStageStatus.SKIPPED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.SKIPPED));
    assertTrue(result.coreValidationResult().valid());
    assertFalse(result.hasCapabilityReport());
    assertNull(result.classicalPreflightResult());
    assertFalse(result.hasClassicalCalculationResult());
    assertNull(result.electronicProblem());
    assertEquals(ChemistryWorkflowStageStatus.SKIPPED, result.stageResults().get(1).status());
    assertTrue(result.hasStage(ChemistryWorkflowStage.CLASSICAL_EXECUTION));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.CLASSICAL_EXECUTION, ChemistryWorkflowStageStatus.SKIPPED));
    assertEquals(1, result.completedStageCount());
    assertEquals(0, result.failedStageCount());
    assertTrue(result.skippedStageCount() > 0);
  }

  @Test
  public void classicalOnlyProducesNativeClassicalPlan() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("classical_h2"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertTrue(
        result.status() == ChemistryWorkflowStatus.SUCCESS
            || result.status() == ChemistryWorkflowStatus.PARTIAL,
        ChemistryWorkflowServiceTest.workflowStageSummary(result)
            + " "
            + result.diagnostics().diagnostics());
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.HAMILTONIAN_PREPARATION, ChemistryWorkflowStageStatus.SKIPPED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.FERMION_MAPPING, ChemistryWorkflowStageStatus.SKIPPED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
            ChemistryWorkflowStageStatus.SKIPPED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.SKIPPED));
    assertNotNull(result.classicalPreflightResult());
    assertTrue(result.hasCapabilityReport());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        result.capabilityReport().requireItem("integrals.one-electron").status());
    assertNotNull(result.classicalCalculationPlan());
    assertTrue(result.hasClassicalCalculationResult());
    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.classicalCalculationResult().status());
    assertFalse(result.classicalCalculationResult().sections().isEmpty());
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.CLASSICAL_EXECUTION, ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(result.completedStageCount() > 1);
    assertEquals(0, result.failedStageCount());
    assertNull(result.electronicProblem());
    assertTrue(result.hasStoragePlan());
    assertFalse(result.storagePlan().hasReactionRecord());
    assertFalse(result.storagePlan().hasPredictionRecord());
    assertFalse(result.storagePlan().hasComputationPlanRecord());
    assertTrue(result.storagePlan().hasClassicalResultRecords());
    final ChemistryStorageDocument document =
        result.storagePlan().toDocument("classical_h2_project", "Classical H2 project");
    assertEquals(1, document.molecules().size());
    assertEquals(1, document.classicalResultRecords().size());
    assertTrue(document.reactions().isEmpty());
  }

  @Test
  public void classicalWorkflowPreservesCasscfOrbitalRotationOptions() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("classical_h2_casscf_orbital_rotation"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.casscfOrbitalRotationRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertTrue(
        result.status() == ChemistryWorkflowStatus.SUCCESS
            || result.status() == ChemistryWorkflowStatus.PARTIAL,
        result.stageResults().toString() + " " + result.diagnostics().diagnostics());
    assertTrue(result.hasClassicalCalculationResult());
    assertTrue(
        result.classicalCalculationResult().status() == ClassicalCalculationStatus.SUCCEEDED
            || result.classicalCalculationResult().status() == ClassicalCalculationStatus.PARTIAL,
        result.classicalCalculationResult().diagnostics().toString());
    assertEquals(
        "ALL_NON_REDUNDANT",
        ChemistryWorkflowServiceTest.classicalValue(result, "orbital_rotation_space"));
    assertTrue(
        Double.parseDouble(
                ChemistryWorkflowServiceTest.classicalValue(result, "evaluated_orbital_rotations"))
            > 0.0);
    assertTrue(
        Double.parseDouble(
                ChemistryWorkflowServiceTest.classicalValue(
                    result, "orbital_optimization_energy_improvement"))
            >= 0.0);
    assertTrue(
        Double.parseDouble(
                ChemistryWorkflowServiceTest.classicalValue(
                    result, "max_residual_orbital_energy_improvement"))
            < 1.0E-6);
  }

  @Test
  public void
      fullQuantumPreparationKeepsCasscfOrbitalOptimizationAndGeneratesNativeQuantumPackage() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_casscf_orbital_rotation"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.casscfOrbitalRotationHamiltonianRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertTrue(
        result.status() == ChemistryWorkflowStatus.SUCCESS
            || result.status() == ChemistryWorkflowStatus.PARTIAL,
        ChemistryWorkflowServiceTest.workflowStageSummary(result)
            + " "
            + result.diagnostics().diagnostics());
    assertTrue(result.hasClassicalCalculationResult());
    assertTrue(
        result.classicalCalculationResult().status() == ClassicalCalculationStatus.SUCCEEDED
            || result.classicalCalculationResult().status() == ClassicalCalculationStatus.PARTIAL,
        result.classicalCalculationResult().diagnostics().toString());
    assertEquals(
        "ALL_NON_REDUNDANT",
        ChemistryWorkflowServiceTest.classicalValue(result, "orbital_rotation_space"));
    assertTrue(
        Double.parseDouble(
                ChemistryWorkflowServiceTest.classicalValue(result, "evaluated_orbital_rotations"))
            > 0.0);
    assertNotNull(result.electronicProblem());
    assertTrue(result.hasQuantumProgramPackage());
    assertTrue(result.quantumProgramPackage().skippedArtifacts().isEmpty());
    assertTrue(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION));
    assertTrue(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(),
            ChemistryQuantumProgramKind.SPIN_BLOCK_PARITY_TAPERED_EVOLUTION));
    assertTrue(result.quantumProgramPackage().provenance().known());
    assertTrue(result.quantumProgramPackage().provenance().orbitalOptimized());
    assertEquals(
        "MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD:CASSCF",
        result.quantumProgramPackage().provenance().referenceMethod());
    assertEquals("2e/2o", result.quantumProgramPackage().provenance().activeSpaceDescription());
    assertEquals(
        FermionToQubitMapping.JORDAN_WIGNER,
        result.quantumProgramPackage().provenance().requestedMapping());
    assertTrue(result.quantumProgramPackage().totalOperationCount() > 0L);
    assertEquals(result.quantumProgram(), result.quantumProgramPackage().primaryProgram());
  }

  @Test
  public void hamiltonianModeMapsNativeHamiltonianWithoutExternalRuntime() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("hamiltonian_h2"),
                ChemistryWorkflowMode.HAMILTONIAN_ONLY,
                ChemistryWorkflowServiceTest.hydrogen(),
                null,
                null,
                ChemistryWorkflowServiceTest.problem()));

    assertEquals(ChemistryWorkflowStatus.SUCCESS, result.status());
    assertNotNull(result.electronicProblem());
    assertNull(result.quantumProgram());
  }

  @Test
  public void fullQuantumPreparationGeneratesQuantumIrProgram() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                ChemistryWorkflowServiceTest.problem()));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.electronicProblem());
    assertNotNull(result.quantumProgram());
    assertTrue(result.hasQuantumProgramPackage());
    assertEquals(result.quantumProgram(), result.quantumProgramPackage().primaryProgram());
    assertTrue(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertTrue(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.UCCSD_ANSATZ));
    assertTrue(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ));
    assertTrue(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertTrue(result.quantumProgramPackage().totalOperationCount() > 0L);
    assertTrue(result.quantumProgram().circuitCount() > 0);
    assertTrue(result.hasStoragePlan());
    assertTrue(result.storagePlan().hasClassicalResultRecords());
    assertTrue(result.storagePlan().hasExtensions());
    assertEquals("quantum_program_package", result.storagePlan().extensions().get(0).kind());
  }

  @Test
  public void fullQuantumPreparationUsesJobQuantumPackageOptions() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_selective_package"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                ChemistryWorkflowServiceTest.problem(),
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                null,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION),
                    1.0E-7)));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertTrue(result.hasQuantumProgramPackage());
    assertEquals(2, result.quantumProgramPackage().artifacts().size());
    assertTrue(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertTrue(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertFalse(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.UCCSD_ANSATZ));
    assertFalse(
        ChemistryWorkflowServiceTest.hasQuantumProgramKind(
            result.quantumProgramPackage(), ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION));
  }

  @Test
  public void fullQuantumPreparationGeneratesReducedActiveSpaceProblemFromClassicalRequest() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_reduced_active_space"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(
                    BasisSet.STO_3G, ActiveSpace.of(2, 1)),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.electronicProblem());
    assertTrue(result.hasClassicalCalculationResult());
    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.classicalCalculationResult().status());
    assertEquals(1, result.electronicProblem().hamiltonian().activeSpace().orbitalCount());
    assertEquals(2, result.electronicProblem().hamiltonian().activeSpace().electronCount());
    assertNotNull(result.quantumProgram());
    assertTrue(result.hasQuantumProgramPackage());
    assertFalse(result.quantumProgramPackage().artifactIds().isEmpty());
    assertTrue(result.quantumProgram().circuitCount() > 0);
  }

  @Test
  public void fullQuantumPreparationPlansErrorMitigationWhenRequested() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ErrorMitigationRequest mitigationRequest =
        ErrorMitigationRequest.of(
            ActiveSpace.of(2, 2),
            QuantumNoiseModel.of(QuantumNoiseModelKind.DEPOLARIZING, 0.001, 0.02, 0.03),
            List.of(
                ErrorMitigationTechnique.READOUT_CALIBRATION,
                ErrorMitigationTechnique.ZERO_NOISE_EXTRAPOLATION,
                ErrorMitigationTechnique.SYMMETRY_VERIFICATION,
                ErrorMitigationTechnique.PARTICLE_NUMBER_POST_SELECTION,
                ErrorMitigationTechnique.SPIN_PARITY_POST_SELECTION,
                ErrorMitigationTechnique.PAULI_TWIRLING),
            1024,
            true,
            true);

    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_mitigation"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                ChemistryWorkflowServiceTest.problem(),
                FermionToQubitMapping.JORDAN_WIGNER,
                mitigationRequest));
    final ErrorMitigationPlan plan = result.errorMitigationPlan();

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.quantumProgram());
    assertNotNull(plan);
    assertTrue(result.hasErrorMitigationPlan());
    assertEquals(ErrorMitigationStatus.PLANNED, plan.status());
    assertTrue(plan.recommendedShots() > mitigationRequest.baseShots());
    assertTrue(plan.techniques().contains(ErrorMitigationTechnique.READOUT_CALIBRATION));
    assertTrue(plan.techniques().contains(ErrorMitigationTechnique.ZERO_NOISE_EXTRAPOLATION));
    assertTrue(
        result.stageResults().stream()
            .anyMatch(
                stage ->
                    stage.stage() == ChemistryWorkflowStage.ERROR_MITIGATION_PLANNING
                        && stage.status() == ChemistryWorkflowStageStatus.COMPLETED));
  }

  @Test
  public void fullQuantumPreparationAutoBuildsNativeVqeBackendWhenExplicitBackendIsAbsent() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_vqe_auto_backend"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                ChemistryWorkflowServiceTest.problem(),
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                ChemistryWorkflowServiceTest.vqeRequest()));

    assertTrue(
        result.status() == ChemistryWorkflowStatus.SUCCESS
            || result.status() == ChemistryWorkflowStatus.PARTIAL,
        result.stageResults().toString() + " " + result.diagnostics().diagnostics());
    assertTrue(result.hasVqeOptimizationResult());
    assertFalse(result.vqeOptimizationResult().backendFailed());
    assertTrue(Double.isFinite(result.vqeOptimizationResult().bestEnergy()));
    assertFalse(
        ChemistryWorkflowServiceTest.hasDiagnostic(
            result, ChemistryWorkflowDiagnosticCode.VQE_BACKEND_UNAVAILABLE));
    assertTrue(
        result.stageHasStatus(
                ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.COMPLETED)
            || result.stageHasStatus(
                ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.WARNING));
  }

  @Test
  public void fullQuantumPreparationRunsVqeWhenBackendIsConfigured() {
    final ChemistryWorkflowService service =
        new ChemistryWorkflowService(
            new ChemistryCoreValidator(),
            new ClassicalChemistryService(),
            new ElectronicHamiltonianQubitMapper(),
            new ElectronicHamiltonianProgramGenerator(),
            new ChemistryQuantumProgramPackageGenerator(),
            new ErrorMitigationPlanner(),
            new ReactionPredictionExplainer(),
            new ElectronicStructurePreparationPipeline(),
            new ReactionPredictionFollowUpPlanner(),
            new ReactionConditionScenarioPlanner(),
            new ChemistryComputationParityPlanner(),
            new ChemistryCapabilityAnalyzer(),
            new ChemistryWorkflowStoragePlanner(),
            (program, parameters, shots, mitigationPlan) ->
                VqeEnergyEvaluation.success(parameters[0] * parameters[0], 0.0, shots));
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_vqe_with_backend"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                ChemistryWorkflowServiceTest.problem(),
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                ChemistryWorkflowServiceTest.vqeRequest()));

    assertEquals(ChemistryWorkflowStatus.SUCCESS, result.status(), result.diagnostics().toString());
    assertTrue(result.hasVqeOptimizationResult());
    assertEquals(VqeConvergenceStatus.CONVERGED, result.vqeOptimizationResult().status());
    assertEquals(0.0, result.vqeOptimizationResult().bestEnergy(), 1.0E-15);
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.COMPLETED));
  }

  @Test
  public void
      fullQuantumPreparationRunsLocalStateVectorVqeAgainstNativeMappedHydrogenHamiltonian() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(ChemistryWorkflowServiceTest.classicalRequest());
    final StateVectorPauliVqeEnergyBackend backend =
        new StateVectorPauliVqeEnergyBackend(
            new ElectronicHamiltonianQubitMapper()
                .map(preparation.hamiltonian(), FermionToQubitMapping.JORDAN_WIGNER));
    final ChemistryWorkflowService service =
        new ChemistryWorkflowService(
            new ChemistryCoreValidator(),
            new ClassicalChemistryService(),
            new ElectronicHamiltonianQubitMapper(),
            new ElectronicHamiltonianProgramGenerator(),
            new ChemistryQuantumProgramPackageGenerator(),
            new ErrorMitigationPlanner(),
            new ReactionPredictionExplainer(),
            new ElectronicStructurePreparationPipeline(),
            new ReactionPredictionFollowUpPlanner(),
            new ReactionConditionScenarioPlanner(),
            new ChemistryComputationParityPlanner(),
            new ChemistryCapabilityAnalyzer(),
            new ChemistryWorkflowStoragePlanner(),
            backend);
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_native_statevector_vqe"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                ChemistryWorkflowServiceTest.vqeRequest()));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertTrue(result.hasVqeOptimizationResult());
    assertFalse(result.vqeOptimizationResult().backendFailed());
    assertTrue(Double.isFinite(result.vqeOptimizationResult().bestEnergy()));
    assertTrue(result.vqeOptimizationResult().evaluationCount() > 0L);
    assertTrue(result.vqeOptimizationResult().totalShots() > 0L);
    assertTrue(
        result.stageHasStatus(
                ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.COMPLETED)
            || result.stageHasStatus(
                ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.WARNING));
  }

  @Test
  public void fullQuantumPreparationAutoBuildsLocalStateVectorVqeBackendFromMappedHamiltonian() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_auto_native_statevector_vqe"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                ChemistryWorkflowServiceTest.vqeRequest()));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.electronicProblem());
    assertTrue(result.hasQuantumProgramPackage());
    assertTrue(result.quantumProgramPackage().hasArtifacts());
    assertTrue(result.hasVqeOptimizationResult());
    assertFalse(result.vqeOptimizationResult().backendFailed());
    assertTrue(Double.isFinite(result.vqeOptimizationResult().bestEnergy()));
    assertTrue(result.vqeOptimizationResult().evaluationCount() > 0L);
    assertTrue(
        result.stageHasStatus(
                ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.COMPLETED)
            || result.stageHasStatus(
                ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.WARNING));
    assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ChemistryWorkflowDiagnosticCode.VQE_BACKEND_UNAVAILABLE),
        result.diagnostics().diagnostics().toString());
  }

  @Test
  public void fullQuantumPreparationRunsWithCasscfOptimizedHamiltonianPreparation() {
    final StateVectorPauliVqeEnergyBackend backend =
        new StateVectorPauliVqeEnergyBackend(
            new ElectronicHamiltonianQubitMapper()
                .map(
                    new ElectronicStructurePreparationPipeline()
                        .prepare(ChemistryWorkflowServiceTest.casscfOptimizedHamiltonianRequest())
                        .hamiltonian(),
                    FermionToQubitMapping.JORDAN_WIGNER));
    final ChemistryWorkflowService service =
        new ChemistryWorkflowService(
            new ChemistryCoreValidator(),
            new ClassicalChemistryService(),
            new ElectronicHamiltonianQubitMapper(),
            new ElectronicHamiltonianProgramGenerator(),
            new ChemistryQuantumProgramPackageGenerator(),
            new ErrorMitigationPlanner(),
            new ReactionPredictionExplainer(),
            new ElectronicStructurePreparationPipeline(),
            new ReactionPredictionFollowUpPlanner(),
            new ReactionConditionScenarioPlanner(),
            new ChemistryComputationParityPlanner(),
            new ChemistryCapabilityAnalyzer(),
            new ChemistryWorkflowStoragePlanner(),
            backend);
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_casscf_optimized_quantum"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.casscfOptimizedHamiltonianRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                ChemistryWorkflowServiceTest.vqeRequest()));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.electronicProblem());
    assertEquals(2, result.electronicProblem().hamiltonian().spatialOrbitalCount());
    assertEquals(4, result.electronicProblem().hamiltonian().spinOrbitalCount());
    assertTrue(result.electronicProblem().hamiltonian().integralCount() > 0);
    assertTrue(result.hasQuantumProgramPackage());
    assertTrue(result.quantumProgramPackage().hasArtifacts());
    assertTrue(result.quantumProgramPackage().provenance().orbitalOptimized());
    assertEquals(
        "CASSCF_ORBITAL_OPTIMIZED:Hartree-Fock",
        result.quantumProgramPackage().provenance().referenceMethod());
    assertEquals(4, result.quantumProgramPackage().provenance().spinOrbitalCount());
    assertTrue(result.hasVqeOptimizationResult());
    assertFalse(result.vqeOptimizationResult().backendFailed());
  }

  @Test
  public void fullQuantumPreparationAutoRunsVqeFromCasscfOptimizedHamiltonian() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_h2_casscf_auto_native_vqe"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.casscfOrbitalRotationHamiltonianRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                ChemistryWorkflowServiceTest.vqeRequest()));

    assertTrue(
        result.status() == ChemistryWorkflowStatus.SUCCESS
            || result.status() == ChemistryWorkflowStatus.PARTIAL,
        ChemistryWorkflowServiceTest.workflowStageSummary(result)
            + " "
            + result.diagnostics().diagnostics());
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
            ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.FERMION_MAPPING, ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
            ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(
        result.stageHasStatus(
                ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.COMPLETED)
            || result.stageHasStatus(
                ChemistryWorkflowStage.VQE_OPTIMIZATION, ChemistryWorkflowStageStatus.WARNING));
    assertNotNull(result.electronicProblem());
    assertEquals(2, result.electronicProblem().hamiltonian().spatialOrbitalCount());
    assertEquals(4, result.electronicProblem().hamiltonian().spinOrbitalCount());
    assertEquals(
        "ALL_NON_REDUNDANT",
        ChemistryWorkflowServiceTest.classicalValue(result, "orbital_rotation_space"));
    assertTrue(
        Double.parseDouble(
                ChemistryWorkflowServiceTest.classicalValue(result, "evaluated_orbital_rotations"))
            > 0.0);
    assertTrue(
        Double.parseDouble(
                ChemistryWorkflowServiceTest.classicalValue(
                    result, "max_residual_orbital_gradient"))
            >= 0.0);
    assertTrue(result.hasQuantumProgramPackage());
    assertTrue(result.quantumProgramPackage().hasArtifacts());
    assertTrue(result.hasVqeOptimizationResult());
    assertFalse(result.vqeOptimizationResult().backendFailed());
    assertTrue(Double.isFinite(result.vqeOptimizationResult().bestEnergy()));
    assertTrue(result.vqeOptimizationResult().evaluationCount() > 0L);
    assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ChemistryWorkflowDiagnosticCode.VQE_BACKEND_UNAVAILABLE),
        result.diagnostics().diagnostics().toString());
  }

  @Test
  public void fullQuantumPreparationAutoRunsVqeForHeterocycleActiveSpaceHamiltonian() {
    final ActiveSpace activeSpace = ActiveSpace.of(4, 4);
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_heterocycle_active_space_vqe"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.ringMolecule("vqe_pyridine_probe", "N"),
                ChemistryWorkflowServiceTest.classicalRequest(
                    ChemistryWorkflowServiceTest.ringMolecule("vqe_pyridine_probe", "N"),
                    ChemistryWorkflowServiceTest.compactHcnBasis(),
                    activeSpace,
                    List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 0))),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                ChemistryWorkflowServiceTest.vqeRequest(activeSpace, 8),
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ),
                    1.0E-6)));

    assertTrue(
        result.status() == ChemistryWorkflowStatus.SUCCESS
            || result.status() == ChemistryWorkflowStatus.PARTIAL,
        ChemistryWorkflowServiceTest.workflowStageSummary(result)
            + " "
            + result.diagnostics().diagnostics());
    assertNotNull(result.electronicProblem());
    assertEquals(4, result.electronicProblem().hamiltonian().activeSpace().electronCount());
    assertEquals(4, result.electronicProblem().hamiltonian().activeSpace().orbitalCount());
    assertEquals(8, result.electronicProblem().hamiltonian().spinOrbitalCount());
    assertTrue(result.electronicProblem().hamiltonian().integralCount() > 0);
    assertTrue(result.hasQuantumProgramPackage());
    assertTrue(
        result
            .quantumProgramPackage()
            .hasArtifact(
                ChemistryQuantumProgramKind.PAULI_EVOLUTION, FermionToQubitMapping.JORDAN_WIGNER));
    assertTrue(
        result
            .quantumProgramPackage()
            .hasArtifact(ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ));
    assertTrue(result.hasVqeOptimizationResult());
    assertFalse(result.vqeOptimizationResult().backendFailed());
    assertTrue(Double.isFinite(result.vqeOptimizationResult().bestEnergy()));
    assertTrue(result.vqeOptimizationResult().evaluationCount() > 0L);
    assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ChemistryWorkflowDiagnosticCode.VQE_BACKEND_UNAVAILABLE),
        result.diagnostics().diagnostics().toString());
  }

  @Test
  public void fullQuantumPreparationBuildsNativeHamiltonianWhenElectronicProblemIsAbsent() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_h2"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.electronicProblem());
    assertTrue(result.hasCapabilityReport());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        result.capabilityReport().requireItem("integrals.full-eri").status());
    assertEquals(2, result.electronicProblem().hamiltonian().spatialOrbitalCount());
    assertEquals(4, result.electronicProblem().hamiltonian().spinOrbitalCount());
    assertTrue(result.electronicProblem().hamiltonian().integralCount() > 0);
    assertTrue(
        ChemistryWorkflowServiceTest.workflowStageSummary(result)
            .contains("full two-electron, electrons=2, qubits=4"),
        ChemistryWorkflowServiceTest.workflowStageSummary(result));
    assertTrue(
        ChemistryWorkflowServiceTest.workflowStageSummary(result).contains("integrals=6"),
        ChemistryWorkflowServiceTest.workflowStageSummary(result));
    assertNotNull(result.quantumProgram());
    assertTrue(result.quantumProgram().circuitCount() > 0);
  }

  @Test
  public void fullQuantumPreparationSupportsParityFermionMapping() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_h2_parity"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.PARITY));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertEquals(FermionToQubitMapping.PARITY, result.job().fermionToQubitMapping());
    assertNotNull(result.electronicProblem());
    assertNotNull(result.quantumProgram());
    assertTrue(result.quantumProgram().circuitCount() > 0);
  }

  @Test
  public void fullQuantumPreparationSupportsBravyiKitaevFermionMapping() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_h2_bravyi_kitaev"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.BRAVYI_KITAEV));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertEquals(FermionToQubitMapping.BRAVYI_KITAEV, result.job().fermionToQubitMapping());
    assertNotNull(result.electronicProblem());
    assertNotNull(result.quantumProgram());
    assertTrue(result.quantumProgram().circuitCount() > 0);
  }

  @Test
  public void fullQuantumPreparationSupportsSpinBlockFermionMapping() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_h2_spin_block_bk"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.SPIN_BLOCK_BRAVYI_KITAEV));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertEquals(
        FermionToQubitMapping.SPIN_BLOCK_BRAVYI_KITAEV, result.job().fermionToQubitMapping());
    assertNotNull(result.electronicProblem());
    assertNotNull(result.quantumProgram());
    assertTrue(result.quantumProgram().circuitCount() > 0);
  }

  @Test
  public void fullQuantumPreparationMapsDirectOpenShellHamiltonianIntoQuantumIrPackage() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_li_direct_uhf_quantum_package"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.lithiumAtom(),
                ChemistryWorkflowServiceTest.directUnrestrictedLithiumHamiltonianRequest(),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                null,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(ChemistryQuantumProgramKind.PAULI_EVOLUTION),
                    1.0E-8)));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.classicalCalculationResult());
    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.classicalCalculationResult().status());
    assertEquals(
        "DIRECT_HARTREE_FOCK",
        ChemistryWorkflowServiceTest.classicalValue(result, "preparation_mode"));
    assertNotNull(result.electronicProblem());
    assertEquals(3, result.electronicProblem().hamiltonian().activeSpace().electronCount());
    assertEquals(5, result.electronicProblem().hamiltonian().activeSpace().orbitalCount());
    assertEquals(10, result.electronicProblem().hamiltonian().spinOrbitalCount());
    assertTrue(result.electronicProblem().hamiltonian().integralCount() > 0);
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.FERMION_MAPPING, ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(result.hasQuantumProgramPackage());
    assertEquals(1, result.quantumProgramPackage().artifactCount());
    assertTrue(
        result
            .quantumProgramPackage()
            .hasArtifact(
                ChemistryQuantumProgramKind.PAULI_EVOLUTION, FermionToQubitMapping.JORDAN_WIGNER));
    assertTrue(Quantum.validate(result.quantumProgramPackage().primaryProgram()).isValid());
  }

  @Test
  public void fullQuantumPreparationCanUseReactionJobWithMoleculeLevelCalculationTarget() {
    final Molecule reactant =
        ChemistryWorkflowServiceTest.ringMolecule("reaction_route_pyridine", "N");
    final Molecule candidate =
        ChemistryWorkflowServiceTest.acyclicMolecule("reaction_route_candidate", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("reaction_level_quantum_route"),
            "Reaction-level quantum route",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(candidate, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("reaction_level_candidate_hamiltonian"),
            candidate,
            ElectronicStructureMethod.HARTREE_FOCK,
            ChemistryWorkflowServiceTest.compactHcnBasis(),
            ActiveSpace.of(4, 4),
            List.of(
                ClassicalCalculationKind.SINGLE_POINT_ENERGY,
                ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 0)),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("reaction_level_candidate_quantum_package"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                reaction,
                request,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                null,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION),
                    1.0E-6)));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.classicalCalculationResult());
    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.classicalCalculationResult().status());
    assertNotNull(result.electronicProblem());
    assertEquals(candidate, result.electronicProblem().molecule());
    assertEquals(4, result.electronicProblem().hamiltonian().electronCount());
    assertEquals(4, result.electronicProblem().hamiltonian().spatialOrbitalCount());
    assertEquals(8, result.electronicProblem().hamiltonian().spinOrbitalCount());
    assertTrue(result.electronicProblem().hamiltonian().integralCount() > 0);
    assertTrue(result.hasQuantumProgramPackage());
    assertTrue(
        result
            .quantumProgramPackage()
            .hasArtifact(
                ChemistryQuantumProgramKind.PAULI_EVOLUTION, FermionToQubitMapping.JORDAN_WIGNER));
    assertTrue(
        result
            .quantumProgramPackage()
            .hasArtifact(
                ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION,
                FermionToQubitMapping.JORDAN_WIGNER));
    assertTrue(Quantum.validate(result.quantumProgramPackage().primaryProgram()).isValid());
  }

  @Test
  public void fullQuantumPreparationMapsHeteroarylRadicalWithoutExternalBackendOrLookup() {
    final Molecule radical = AbInitioWorkflowReferenceRegressionTest.pyridylRadical();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("full_native_pyridyl_radical_workflow"),
            radical,
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.HARTREE_FOCK,
                ElectronicStructureMethodName.of("UHF"),
                ElectronicStructureSpinTreatment.UNRESTRICTED),
            AbInitioWorkflowReferenceRegressionTest.compactHcnBasis(),
            ActiveSpace.of(5, 4),
            List.of(
                ClassicalCalculationKind.SINGLE_POINT_ENERGY,
                ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 0)),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_pyridyl_radical_quantum_package"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                radical,
                request,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null,
                null,
                FermionToQubitMapping.JORDAN_WIGNER,
                null,
                null,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION),
                    1.0E-6)));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.classicalCalculationResult());
    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.classicalCalculationResult().status());
    assertEquals("UHF", ChemistryWorkflowServiceTest.classicalValue(result, "reference_method"));
    assertEquals(
        "false", ChemistryWorkflowServiceTest.classicalValue(result, "external_reference_route"));
    assertNotNull(result.electronicProblem());
    assertEquals(5, result.electronicProblem().hamiltonian().electronCount());
    assertEquals(4, result.electronicProblem().hamiltonian().spatialOrbitalCount());
    assertEquals(8, result.electronicProblem().hamiltonian().spinOrbitalCount());
    assertTrue(result.electronicProblem().hamiltonian().integralCount() > 0);
    assertTrue(result.hasQuantumProgramPackage());
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.HAMILTONIAN_PREPARATION,
            ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.FERMION_MAPPING, ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(
        result.stageHasStatus(
            ChemistryWorkflowStage.QUANTUM_PROGRAM_PREPARATION,
            ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(result.hasStoragePlan());
    assertTrue(result.storagePlan().hasClassicalResultRecords());
    assertTrue(result.storagePlan().hasExtensions());
    assertTrue(result.electronicProblem().hamiltonian().activeSpace().electronCount() % 2 == 1);
    assertTrue(
        result.quantumProgramPackage().spinOrbitalCount()
            >= result.quantumProgramPackage().electronCount());
    assertTrue(
        result
            .quantumProgramPackage()
            .hasArtifact(
                ChemistryQuantumProgramKind.PAULI_EVOLUTION, FermionToQubitMapping.JORDAN_WIGNER));
    assertTrue(
        result
            .quantumProgramPackage()
            .hasArtifact(
                ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION,
                FermionToQubitMapping.JORDAN_WIGNER));
    assertTrue(Quantum.validate(result.quantumProgramPackage().primaryProgram()).isValid());
    assertFalse(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")),
        result.diagnostics().diagnostics().toString());
  }

  @Test
  public void nativeHamiltonianPreflightRejectsUnsupportedBasisWithoutRunningIntegralPipeline() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_h2_unsupported_basis"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(
                    BasisSet.of(BasisSetName.of("cc-pVDZ")), ActiveSpace.of(2, 2)),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertEquals(ChemistryWorkflowStatus.FAILED, result.status());
    assertTrue(result.hasCapabilityReport());
    assertTrue(result.capabilityReport().hasBlockingUnsupportedItems());
    assertNull(result.electronicProblem());
    assertTrue(
        ChemistryWorkflowServiceTest.hasDiagnostic(
            result, ChemistryWorkflowDiagnosticCode.HAMILTONIAN_CAPABILITY_UNSUPPORTED));
  }

  @Test
  public void nativeHamiltonianPreflightRejectsEcpBasisTextAsCapabilityBoundary() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final String ecpBasisText =
        "H 0\n" + "S 1 1.00\n" + "  3.42525091 1.0\n" + "****\n" + "ECP\n" + "H-ECP 0 1\n";
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_h2_ecp_basis_text"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(
                    BasisSet.defined(BasisSetName.of("h-ecp-text"), ecpBasisText),
                    ActiveSpace.of(2, 2)),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertEquals(ChemistryWorkflowStatus.FAILED, result.status());
    assertTrue(result.hasCapabilityReport());
    assertEquals(
        ChemistryCapabilityStatus.UNSUPPORTED,
        result.capabilityReport().requireItem("integrals.full-eri").status());
    assertTrue(
        result
            .capabilityReport()
            .requireItem("integrals.full-eri")
            .message()
            .contains("ECP or pseudopotential"));
    assertNull(result.electronicProblem());
    assertTrue(
        ChemistryWorkflowServiceTest.hasDiagnostic(
            result, ChemistryWorkflowDiagnosticCode.HAMILTONIAN_CAPABILITY_UNSUPPORTED));
  }

  @Test
  public void nativeHamiltonianPreflightReportsHighAngularFullTensorResourceBoundary() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final BasisSet highAngularBasis =
        BasisSet.structured(
            BasisSetName.of("fe-hik-structured"),
            BasisSetDefinition.of(
                List.of(
                    BasisAtomDefinition.of(
                        ElementSymbol.of("Fe"),
                        List.of(
                            ChemistryWorkflowServiceTest.shell(BasisShellAngularMomentum.H),
                            ChemistryWorkflowServiceTest.shell(BasisShellAngularMomentum.I),
                            ChemistryWorkflowServiceTest.shell(BasisShellAngularMomentum.K))))));
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_fe_high_angular_basis"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.ironAtom(),
                ChemistryWorkflowServiceTest.classicalRequest(
                    ChemistryWorkflowServiceTest.ironAtom(),
                    highAngularBasis,
                    ActiveSpace.of(2, 2)),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertEquals(ChemistryWorkflowStatus.FAILED, result.status());
    assertTrue(result.hasCapabilityReport());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        result.capabilityReport().requireItem("integrals.one-electron").status());
    assertEquals(
        ChemistryCapabilityStatus.UNSUPPORTED,
        result.capabilityReport().requireItem("integrals.full-eri").status());
    assertTrue(
        result
            .capabilityReport()
            .requireItem("integrals.full-eri")
            .message()
            .contains("packed double elements"));
    assertNull(result.electronicProblem());
  }

  @Test
  public void nativeHamiltonianPreflightSupportsReducedActiveSpaceProjection() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_h2_reduced_active_space"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.hydrogen(),
                ChemistryWorkflowServiceTest.classicalRequest(
                    BasisSet.STO_3G, ActiveSpace.of(2, 1)),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.electronicProblem());
    assertEquals(1, result.electronicProblem().hamiltonian().activeSpace().orbitalCount());
    assertEquals(2, result.electronicProblem().hamiltonian().activeSpace().electronCount());
    assertFalse(
        ChemistryWorkflowServiceTest.hasDiagnostic(
            result, ChemistryWorkflowDiagnosticCode.HAMILTONIAN_CAPABILITY_UNSUPPORTED));
  }

  @Test
  public void nativeHamiltonianPreflightBuildsFirstRowPShellHamiltonian() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("full_native_carbon_p_shell"),
                ChemistryWorkflowMode.FULL_QUANTUM_PREPARATION,
                ChemistryWorkflowServiceTest.carbonAtom(),
                ChemistryWorkflowServiceTest.classicalRequest(
                    ChemistryWorkflowServiceTest.carbonAtom(),
                    BasisSet.STO_3G,
                    ActiveSpace.of(6, 5)),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                null));

    assertEquals(
        ChemistryWorkflowStatus.SUCCESS,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.electronicProblem());
    assertEquals(5, result.electronicProblem().hamiltonian().spatialOrbitalCount());
    assertEquals(10, result.electronicProblem().hamiltonian().spinOrbitalCount());
    assertTrue(result.electronicProblem().hamiltonian().integralCount() > 0);
    assertNotNull(result.quantumProgram());
  }

  @Test
  public void workflowCarriesOrganicReactionPredictionResult() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final Molecule diazine = ChemistryWorkflowServiceTest.ringMolecule("diazine", "N");
    final Molecule acyclicDiazineIsomer =
        ChemistryWorkflowServiceTest.acyclicMolecule("acyclic_diazine_isomer", "N");
    final ReactionConditions conditions =
        ReactionConditions.of(
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
            "condition-rich heterocycle workflow");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("heterocycle_prediction_reaction"),
            "Heterocycle prediction reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            conditions,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest classicalRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("heterocycle_prediction_classical"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_apply_calibration_priors", true)),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "heterocycle_prediction_request",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                ChemistryWorkflowServiceTest.productCandidate(acyclicDiazineIsomer, 0.5),
                ChemistryWorkflowServiceTest.productCandidate(diazine, 0.5)),
            null);

    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("workflow_prediction"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                reaction,
                classicalRequest,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                predictionRequest,
                null));

    assertEquals(
        ChemistryWorkflowStatus.PARTIAL,
        result.status(),
        result.diagnostics().diagnostics().toString());
    assertNotNull(result.reactionPredictionResult());
    assertTrue(result.hasReactionPredictionResult());
    assertTrue(result.hasReactionPredictionDiagnostics());
    assertTrue(result.reactionPredictionDiagnosticCount() > 0);
    assertEquals(
        result.reactionPredictionResult().diagnosticCount(ReactionPredictionEvidenceKind.WARNING),
        result.reactionPredictionDiagnosticCount(ReactionPredictionEvidenceKind.WARNING));
    assertTrue(
        result.reactionPredictionEvidenceCount(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
            > 0);
    assertTrue(
        result.reactionPredictionEvidenceWeight(ReactionPredictionEvidenceKind.CALIBRATION_PRIOR)
            > 0.0);
    assertTrue(
        result.reactionPredictionHasWeightedEvidence(
            ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    final ClassicalCalculationRequest calibrationDisabledRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("heterocycle_prediction_without_calibration"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.bool(
                    "reaction_prediction_apply_calibration_priors", false)),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ChemistryWorkflowResult calibrationDisabledResult =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("workflow_prediction_without_calibration"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                reaction,
                calibrationDisabledRequest,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                predictionRequest,
                null));
    assertEquals(
        0,
        calibrationDisabledResult.reactionPredictionEvidenceCount(
            ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    assertFalse(
        calibrationDisabledResult.reactionPredictionHasWeightedEvidence(
            ReactionPredictionEvidenceKind.CALIBRATION_PRIOR));
    assertTrue(result.predictedMechanismCount() > 0);
    assertNotNull(result.reactionPredictionExplanation());
    assertTrue(result.hasReactionPredictionExplanation());
    assertTrue(result.reactionPredictionExplanation().conditionAware());
    assertTrue(result.reactionPredictionExplanation().conditionSuitability().explicitConditions());
    assertTrue(
        result.reactionPredictionExplanation().conditionSuitability().stronglyIncompatible(),
        result.reactionPredictionExplanation().conditionSuitability().limitingEvidence().stream()
            .map(
                evidence ->
                    evidence.kind() + ":" + evidence.weight() + ":" + evidence.description())
            .toList()
            .toString());
    assertFalse(
        result.reactionPredictionExplanation().conditionSuitability().limitingEvidence().isEmpty());
    assertFalse(
        result
            .reactionPredictionExplanation()
            .conditionSuitability()
            .supportingEvidence()
            .isEmpty());
    assertFalse(result.reactionPredictionExplanation().dominantEvidence().isEmpty());
    assertTrue(
        result.reactionPredictionExplanation().pathwayAssessment().mechanismClass()
                == ReactionMechanismClass.HETEROCYCLE_FUNCTIONALIZATION
            || result.reactionPredictionExplanation().pathwayAssessment().mechanismClass()
                == ReactionMechanismClass.CROSS_COUPLING,
        result.reactionPredictionExplanation().pathwayAssessment().mechanismClass().name());
    assertTrue(
        result.reactionPredictionExplanation().pathwayAssessment().hasPathwaySteps()
            || result.predictedMechanismCount() > 0);
    assertNotNull(
        result.reactionPredictionExplanation().pathwayAssessment().regioselectivityClass());
    assertTrue(
        result
            .reactionPredictionExplanation()
            .pathwayAssessment()
            .conditionWindow()
            .hasRequiredConditionSignals());
    assertTrue(result.hasRecommendations());
    assertTrue(result.hasFollowUpCalculationPlan());
    assertTrue(result.followUpCalculationPlan().requestCount() > 0);
    assertTrue(result.hasConditionEffectProfile());
    assertTrue(result.hasConditionScenarioPlan());
    assertTrue(result.hasConditionScenarioEvaluationPlan());
    assertTrue(result.hasComputationParityPlan());
    assertEquals(
        ChemistryComputationParityStatus.CLASSICAL_ONLY,
        result.computationParityPlan().firstItem().status());
    assertTrue(result.computationParityPlan().firstItem().hasClassicalPlan());
    assertTrue(result.computationParityPlan().firstItem().hasClassicalResult());
    assertFalse(result.computationParityPlan().firstItem().hasQuantumPrograms());
    assertTrue(result.computationParityPlan().firstItem().hasMechanisms());
    assertTrue(
        result.computationParityPlan().firstItem().hasPathwaySteps()
            || result.computationParityPlan().firstItem().hasMechanisms());
    assertTrue(result.computationParityPlan().firstItem().hasBlockingDiagnostics());
    assertTrue(
        result
            .computationParityPlan()
            .firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.REACTION_PREDICTION_LOW_CONFIDENCE));
    assertTrue(
        result
            .computationParityPlan()
            .firstItem()
            .hasDiagnostic(ChemistryComputationParityDiagnosticCode.MISSING_QUANTUM_PROGRAMS));
    assertFalse(
        result
            .computationParityPlan()
            .firstItem()
            .hasDiagnostic(ChemistryComputationParityDiagnosticCode.MISSING_CLASSICAL_RESULT));
    assertTrue(result.conditionEffectProfile().scoreAdjustment() > 0.0);
    assertEquals(0, result.conditionEffectProfile().riskCount());
    assertTrue(
        result
            .conditionEffectProfile()
            .hasEffect(ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC));
    assertTrue(
        result.conditionEffectProfile().hasEffect(ReactionConditionEffectKind.COPPER_CATALYSIS));
    assertTrue(
        result.conditionEffectProfile().hasEffect(ReactionConditionEffectKind.THERMAL_ACTIVATION));
    assertTrue(
        result.conditionEffectProfile().hasEffect(ReactionConditionEffectKind.INERT_DRY_CONTROL));
    assertTrue(
        result
            .conditionEffectProfile()
            .hasEffect(ReactionConditionEffectKind.HETEROCYCLE_COMPATIBILITY));
    assertTrue(result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.BASELINE));
    assertTrue(
        result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.ROOM_TEMPERATURE));
    assertTrue(result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.COOLED));
    assertTrue(
        result
            .conditionScenarioPlan()
            .hasKind(ReactionConditionScenarioKind.POLYETHER_HIGH_BOILING_SOLVENT));
    assertTrue(
        result
            .conditionScenarioPlan()
            .hasKind(ReactionConditionScenarioKind.SEALED_HEATED_CONDITION));
    assertTrue(
        result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.FLOW_CONDITION));
    assertTrue(
        result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.SHORT_DURATION));
    assertTrue(
        result.stageResults().stream()
            .anyMatch(
                stage ->
                    stage.stage() == ChemistryWorkflowStage.CONDITION_SCENARIO_PLANNING
                        && stage.status() == ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(
        result.stageResults().stream()
            .anyMatch(
                stage ->
                    stage.stage() == ChemistryWorkflowStage.CONDITION_EFFECT_ANALYSIS
                        && stage.status() == ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(
        result.stageResults().stream()
            .anyMatch(
                stage ->
                    stage.stage() == ChemistryWorkflowStage.CONDITION_SCENARIO_EVALUATION
                        && stage.status() == ChemistryWorkflowStageStatus.COMPLETED));
    assertFalse(result.conditionScenarioEvaluationPlan().successfulEvaluations().isEmpty());
    assertTrue(result.conditionScenarioEvaluationPlan().bestEvaluation().rankingScore() > 0.0);
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION));
    assertTrue(
        result.followUpCalculationPlan().requests().stream()
            .anyMatch(
                request ->
                    request
                            .calculationKinds()
                            .contains(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION)
                        && request.subject().subjectKind() == ChemistrySubjectKind.MOLECULE));
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind()
                            == ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.KINETIC_ANALYSIS));
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind()
                            == ChemistryWorkflowRecommendationKind.MECHANISM_DISAMBIGUATION
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS));
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind()
                            == ChemistryWorkflowRecommendationKind.MECHANISM_DISAMBIGUATION
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS));
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind()
                            == ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.MICROKINETIC_MODELING));
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION));
    assertTrue(
        result.followUpCalculationPlan().requests().stream()
            .anyMatch(
                request ->
                    request
                            .calculationKinds()
                            .contains(ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION)
                        && request.subject().subjectKind() == ChemistrySubjectKind.MOLECULE));
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.RELATIVISTIC_CORRECTION));
    assertTrue(
        result.stageResults().stream()
            .anyMatch(
                stage ->
                    stage.stage() == ChemistryWorkflowStage.FOLLOW_UP_PLANNING
                        && stage.status() == ChemistryWorkflowStageStatus.COMPLETED));
    assertTrue(result.hasStoragePlan());
    assertEquals(
        "workflow_prediction_reaction_record_0", result.storagePlan().reactionRecord().id());
    assertEquals(
        "workflow_prediction_prediction_record_0", result.storagePlan().predictionRecord().id());
    assertEquals(
        "workflow_prediction_computation_plan_0",
        result.storagePlan().computationPlanRecord().id());
    assertFalse(result.storagePlan().computationPlanRecord().classicalPlanIds().isEmpty());
    assertFalse(result.storagePlan().computationPlanRecord().classicalResultIds().isEmpty());
    assertFalse(result.storagePlan().computationPlanRecord().quantumProgramIds().isEmpty());
    assertTrue(result.storagePlan().computationPlanRecord().deferredExecution());
    assertTrue(result.storagePlan().hasConditionSnapshotRecords());
    assertTrue(result.storagePlan().hasExtensions());
    assertTrue(
        result.storagePlan().extensions().stream()
            .anyMatch(extension -> "reaction_condition_scenario_plan".equals(extension.kind())));
    assertTrue(
        result.storagePlan().extensions().stream()
            .anyMatch(
                extension -> "reaction_condition_scenario_evaluation".equals(extension.kind())));
    assertEquals(
        "workflow_prediction_condition_snapshot_0",
        result.storagePlan().conditionSnapshotRecords().get(0).id());
    assertTrue(result.storagePlan().hasHistoryEvent());
    assertEquals(
        ChemistryStorageReactionEventKind.PREDICTION_RECORDED,
        result.storagePlan().historyEvent().kind());
    final ChemistryStorageDocument document =
        result
            .storagePlan()
            .toDocument("workflow_prediction_project", "Workflow prediction project");
    final ChemistryStorageService storageService = new ChemistryStorageService();
    final ChemistryStorageResult<ChemistryStorageDocument> roundTrip =
        storageService.roundTrip(document);
    assertTrue(roundTrip.success(), roundTrip.diagnostics().toString());
    assertEquals(document.molecules().size(), roundTrip.value().molecules().size());
    assertEquals(document.reactions().size(), roundTrip.value().reactions().size());
    assertEquals(
        document.reactionRecords().get(0).id(), roundTrip.value().reactionRecords().get(0).id());
    assertEquals(
        document.predictionRecords().get(0).candidates().size(),
        roundTrip.value().predictionRecords().get(0).candidates().size());
    assertTrue(
        roundTrip.value().predictionRecords().get(0).candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.evidence().stream()
                        .anyMatch(evidence -> evidence.contains("CALIBRATION_PRIOR"))));
    assertFalse(
        roundTrip
            .value()
            .predictionRecords()
            .get(0)
            .candidates()
            .get(0)
            .scoreComponents()
            .isEmpty());
    assertEquals(
        document.computationPlanRecords().get(0).classicalPlanIds(),
        roundTrip.value().computationPlanRecords().get(0).classicalPlanIds());
    assertEquals(
        document.computationPlanRecords().get(0).classicalResultIds(),
        roundTrip.value().computationPlanRecords().get(0).classicalResultIds());
    assertEquals(
        document.computationPlanRecords().get(0).quantumProgramIds(),
        roundTrip.value().computationPlanRecords().get(0).quantumProgramIds());
    assertTrue(roundTrip.value().roadmapRecords().get(0).hasCandidates());
    assertTrue(
        roundTrip.value().extensions().stream()
            .anyMatch(
                extension ->
                    "reaction_condition_scenario_plan".equals(extension.kind())
                        && Integer.toString(result.conditionScenarioPlan().scenarios().size())
                            .equals(extension.properties().get("scenarioCount"))
                        && extension.bodyLines().stream()
                            .anyMatch(line -> line.contains("POLYETHER_HIGH_BOILING_SOLVENT"))));
    assertTrue(
        roundTrip.value().extensions().stream()
            .anyMatch(
                extension ->
                    "reaction_condition_scenario_evaluation".equals(extension.kind())
                        && Integer.toString(
                                result.conditionScenarioEvaluationPlan().evaluations().size())
                            .equals(extension.properties().get("evaluationCount"))
                        && extension.bodyLines().stream()
                            .anyMatch(line -> line.contains("bestProbability="))));
    assertTrue(
        roundTrip.value().roadmapRecords().get(0).candidates().get(0).explicitProductGraph());
    assertTrue(
        roundTrip.value().roadmapRecords().get(0).candidates().get(0).electronicRerankReady());
    assertTrue(
        roundTrip.value().roadmapRecords().get(0).candidates().get(0).quantumWorkflowReady());
    assertEquals(
        "ready", roundTrip.value().roadmapRecords().get(0).candidates().get(0).readinessReason());
    assertEquals(1, roundTrip.value().conditionSnapshotRecords().size());
    assertEquals(
        document.conditionSnapshotRecords().get(0),
        roundTrip.value().conditionSnapshotRecords().get(0));
    assertEquals(1, roundTrip.value().reactionHistoryEvents().size());
    assertTrue(roundTrip.value().reactionHistoryEvents().get(0).referencesWork());
    assertEquals(
        List.of("workflow_prediction_prediction_record_0"),
        roundTrip.value().reactionHistoryEvents().get(0).predictionRecordIds());
    assertEquals(
        List.of("workflow_prediction_computation_plan_0"),
        roundTrip.value().reactionHistoryEvents().get(0).computationPlanRecordIds());
    final ChemistryStorageReactionTimeline timeline =
        storageService.reactionTimeline(
            roundTrip.value(), roundTrip.value().reactions().get(0).id().value());
    assertEquals(1, timeline.eventCount());
    assertEquals(
        roundTrip.value().computationPlanRecords().get(0).quantumProgramIds(),
        timeline.events().get(0).quantumProgramIds());
    assertEquals(
        List.of("workflow_prediction_condition_snapshot_0"),
        timeline.events().get(0).conditionSnapshotRecordIds());
    final ru.pathcreator.vadim.quantum.chemistry.storage.domain.history
            .ChemistryStorageReactionSnapshot
        snapshot =
            storageService.reactionSnapshot(
                roundTrip.value(), roundTrip.value().reactions().get(0).id().value(), 0);
    assertTrue(snapshot.hasRoadmaps());
    assertTrue(snapshot.hasClassicalResults());
    assertTrue(snapshot.hasQuantumPrograms());
    assertEquals(roundTrip.value().roadmapRecords().get(0), snapshot.roadmapRecords().get(0));
    assertEquals(
        roundTrip.value().classicalResultRecords().get(0),
        snapshot.classicalResultRecords().get(0));
    assertTrue(
        result.reactionPredictionResult().candidates().stream()
            .anyMatch(
                candidate ->
                    candidate.products().participants().get(0).molecule().equals(diazine)));
    assertTrue(
        result.reactionPredictionResult().bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT));
    assertTrue(
        result.reactionPredictionResult().bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.CATALYST_EFFECT));
    assertTrue(
        result.reactionPredictionResult().bestCandidate().evidence().stream()
            .anyMatch(
                evidence -> evidence.kind() == ReactionPredictionEvidenceKind.SOLVENT_EFFECT));
  }

  @Test
  public void quantifiedOrganicRecipeTriggersScaleUpConditionAndFollowUpPlanning() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final Molecule heteroarylChloride =
        ChemistryWorkflowServiceTest.ringMolecule("chloro_dimethyl_nicotinonitrile_probe", "N");
    final Molecule benzylamine =
        ChemistryWorkflowServiceTest.acyclicMolecule("benzylamine_probe", "N");
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(120.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(3.0, TimeDurationUnit.HOUR),
            List.of(
                ReactionConditionComponent.of(
                    ReactionComponentRole.REACTANT,
                    "2-Chloro-4,6-dimethyl-3-pyridinecarbonitrile",
                    SubstanceAmount.of(10.0, SubstanceAmountUnit.GRAM),
                    null,
                    null,
                    null,
                    null,
                    ReactionPhase.SOLUTION,
                    "heteroaryl chloride limiting substrate"),
                ReactionConditionComponent.of(
                    ReactionComponentRole.REACTANT,
                    "benzylamine",
                    SubstanceAmount.of(6.5, SubstanceAmountUnit.GRAM),
                    null,
                    null,
                    null,
                    null,
                    ReactionPhase.LIQUID,
                    "nucleophilic amine partner"),
                ReactionConditionComponent.of(
                    ReactionComponentRole.SOLVENT,
                    "PEG4000",
                    SubstanceAmount.of(70.0, SubstanceAmountUnit.GRAM),
                    null,
                    null,
                    null,
                    null,
                    ReactionPhase.LIQUID,
                    "polyether high-boiling solvent")),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
            "quantified high-boiling heteroaryl amination recipe");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("quantified_heteroaryl_amination_reaction"),
            "Quantified heteroaryl amination reaction",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(heteroarylChloride, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(benzylamine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(heteroarylChloride, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(benzylamine, StoichiometricCoefficient.ONE))),
            conditions,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest classicalRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("quantified_heteroaryl_amination_classical"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "quantified_heteroaryl_amination_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(
                ReactionProductCandidate.of(
                    reaction.products(),
                    1.0,
                    100.0,
                    "balanced quantified recipe candidate",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.USER_RULE,
                            "candidate supplied by quantified workflow test",
                            0.1)))),
            "condition-rich quantified heteroaryl amination");

    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("workflow_quantified_amination"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                reaction,
                classicalRequest,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                predictionRequest,
                null));

    assertTrue(
        result.status() == ChemistryWorkflowStatus.SUCCESS
            || result.status() == ChemistryWorkflowStatus.PARTIAL,
        result.diagnostics().diagnostics().toString());
    assertTrue(result.hasConditionScenarioPlan());
    assertTrue(result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.BASELINE));
    assertTrue(
        result
            .conditionScenarioPlan()
            .hasKind(ReactionConditionScenarioKind.POLYETHER_HIGH_BOILING_SOLVENT));
    assertTrue(
        result
            .conditionScenarioPlan()
            .hasKind(ReactionConditionScenarioKind.LARGE_SCALE_MASS_TRANSFER));
    assertTrue(result.hasRecommendations());
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind()
                            == ChemistryWorkflowRecommendationKind.SCALE_UP_RISK_ANALYSIS
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS),
        result.recommendationPlan().recommendations().toString());
    assertTrue(result.hasFollowUpCalculationPlan());
    assertTrue(
        result.followUpCalculationPlan().requests().stream()
            .anyMatch(
                request ->
                    request
                        .calculationKinds()
                        .contains(ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS)));
    final List<ClassicalCalculationResult> followUpResults =
        service.executeFollowUpCalculations(
            result, List.of(ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS));
    assertTrue(
        followUpResults.stream()
            .anyMatch(
                calculationResult ->
                    calculationResult.hasSection(ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS)
                        && ChemistryWorkflowServiceTest.hasClassicalValue(
                            calculationResult, "total_quantified_mass")
                        && ChemistryWorkflowServiceTest.hasClassicalValue(
                            calculationResult, "viscosity_solvent_proxy")
                        && ChemistryWorkflowServiceTest.hasClassicalValue(
                            calculationResult, "heat_transfer_proxy")));
  }

  @Test
  public void followUpPlannerStopsAtRequestLimitBeforeBuildingFullPlan() {
    final ChemistryFollowUpCalculationPlanner planner = new ChemistryFollowUpCalculationPlanner();
    final Molecule heterocycle =
        ChemistryWorkflowServiceTest.ringMolecule("bundle_storage_diazine", "N");
    final ReactionConditions conditions =
        ReactionConditions.of(
            Temperature.of(120.0, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(3.0, TimeDurationUnit.HOUR),
            List.of(
                ReactionConditionComponent.of(
                    ReactionComponentRole.REACTANT,
                    "condition-rich heteroaryl substrate",
                    SubstanceAmount.of(10.0, SubstanceAmountUnit.GRAM),
                    null,
                    null,
                    null,
                    null,
                    ReactionPhase.SOLUTION,
                    "heterocycle follow-up substrate"),
                ReactionConditionComponent.of(
                    ReactionComponentRole.SOLVENT,
                    "PEG4000",
                    SubstanceAmount.of(70.0, SubstanceAmountUnit.GRAM),
                    null,
                    null,
                    null,
                    null,
                    ReactionPhase.LIQUID,
                    "polyether high-boiling solvent")),
            List.of(
                ReactionConditionFeature.named(ReactionConditionFeatureKind.HEATING),
                ReactionConditionFeature.named(ReactionConditionFeatureKind.BASIC_MEDIUM)),
            "bundle storage condition-rich heteroaryl workflow");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("bundle_storage_reaction"),
            "Bundle storage reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(heterocycle, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(heterocycle, StoichiometricCoefficient.ONE))),
            conditions,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest classicalRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("bundle_storage_classical"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "bundle_storage_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(
                OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION,
                OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(
                ReactionProductCandidate.of(
                    reaction.products(),
                    1.0,
                    100.0,
                    "bundle storage seed candidate",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.USER_RULE,
                            "candidate supplied by bundle storage workflow test",
                            0.1)))),
            "bundle storage workflow");
    final ChemistryWorkflowJob job =
        ChemistryWorkflowJob.of(
            ChemistryWorkflowJobId.of("workflow_bundle_storage"),
            ChemistryWorkflowMode.CLASSICAL_ONLY,
            reaction,
            classicalRequest,
            BuiltInClassicalBackendProfiles.universalPlanning(),
            predictionRequest,
            null);
    final ChemistryWorkflowRecommendationPlan recommendationPlan =
        ChemistryWorkflowRecommendationPlan.of(
            "workflow_bundle_storage",
            List.of(
                ChemistryWorkflowRecommendation.of(
                    ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                    ChemistryWorkflowRecommendationPriority.HIGH,
                    0,
                    ClassicalCalculationKind.REACTION_ENERGY,
                    "candidate_0_energy",
                    "Refine candidate energy."),
                ChemistryWorkflowRecommendation.of(
                    ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                    ChemistryWorkflowRecommendationPriority.HIGH,
                    0,
                    ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS,
                    "candidate_0_properties",
                    "Refine candidate properties."),
                ChemistryWorkflowRecommendation.of(
                    ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                    ChemistryWorkflowRecommendationPriority.MEDIUM,
                    0,
                    ClassicalCalculationKind.SPECTROSCOPY_PREDICTION,
                    "candidate_0_spectroscopy",
                    "Predict candidate spectra."),
                ChemistryWorkflowRecommendation.of(
                    ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
                    ChemistryWorkflowRecommendationPriority.MEDIUM,
                    -1,
                    ClassicalCalculationKind.SOLVENT_SCREENING,
                    "solvent_screening",
                    "Screen solvent sensitivity."),
                ChemistryWorkflowRecommendation.of(
                    ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
                    ChemistryWorkflowRecommendationPriority.LOW,
                    -1,
                    ClassicalCalculationKind.CATALYST_SCREENING,
                    "catalyst_screening",
                    "Screen catalyst sensitivity.")));
    final ChemistryFollowUpCalculationPlan limitedPlan =
        planner.planAll(job, recommendationPlan, null, 3);

    assertEquals(3, limitedPlan.requestCount());
    assertEquals(
        ClassicalCalculationKind.REACTION_ENERGY,
        limitedPlan.requests().get(0).calculationKinds().get(0));
    assertEquals(
        ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS,
        limitedPlan.requests().get(1).calculationKinds().get(0));
    assertEquals(
        ClassicalCalculationKind.SPECTROSCOPY_PREDICTION,
        limitedPlan.requests().get(2).calculationKinds().get(0));

    final ChemistryFollowUpCalculationPlan screenedPlan =
        planner
            .planAll(job, recommendationPlan, null)
            .filteredByKinds(
                List.of(
                    ClassicalCalculationKind.SOLVENT_SCREENING,
                    ClassicalCalculationKind.CATALYST_SCREENING));
    assertEquals(2, screenedPlan.requestCount());
    assertTrue(screenedPlan.hasKind(ClassicalCalculationKind.SOLVENT_SCREENING));
    assertTrue(screenedPlan.hasKind(ClassicalCalculationKind.CATALYST_SCREENING));
    assertFalse(screenedPlan.hasKind(ClassicalCalculationKind.REACTION_ENERGY));
  }

  @Test
  public void boundedFollowUpExecutionPreservesResultOrderAndRejectsInvalidParallelism() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final Molecule molecule = ChemistryWorkflowServiceTest.hydrogen();
    final ChemistryFollowUpCalculationPlan plan =
        ChemistryFollowUpCalculationPlan.of(
            "bounded_follow_up_execution",
            List.of(
                ChemistryWorkflowServiceTest.propertyRequest("bounded_property_0", molecule),
                ChemistryWorkflowServiceTest.propertyRequest("bounded_property_1", molecule),
                ChemistryWorkflowServiceTest.propertyRequest("bounded_property_2", molecule)));

    final List<ClassicalCalculationResult> sequential =
        service.executeFollowUpCalculationsSequentially(plan);
    final List<ClassicalCalculationResult> bounded = service.executeFollowUpCalculations(plan, 2);

    assertEquals(sequential.size(), bounded.size());
    for (int i = 0; i < sequential.size(); ++i) {
      assertEquals(sequential.get(i).calculationId(), bounded.get(i).calculationId());
      assertEquals(sequential.get(i).status(), bounded.get(i).status());
      assertTrue(bounded.get(i).hasSection(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS));
    }
    assertThrows(
        IllegalArgumentException.class, () -> service.executeFollowUpCalculations(plan, 0));
  }

  @Test
  public void boundedAllFollowUpExecutionAndParityUseControlledParallelism() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final Molecule molecule = ChemistryWorkflowServiceTest.hydrogen();
    final ChemistryWorkflowResult result =
        ChemistryWorkflowServiceTest.resultWithPropertyRecommendations(molecule);

    final List<ClassicalCalculationResult> allResults =
        service.executeAllFollowUpCalculations(
            result, List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS), 2);
    final ChemistryComputationParityPlan parityPlan =
        service.executeAllFollowUpCalculationsAndPlanParity(result, 2);
    final ChemistryWorkflowExecutionBundle bundle =
        ChemistryWorkflowExecutionBundle.of(result, allResults, parityPlan);
    final ChemistryWorkflowCandidateExecutionSummary summary = bundle.candidateExecutionSummary(0);

    assertEquals(3, allResults.size());
    assertTrue(
        allResults
            .get(0)
            .calculationId()
            .value()
            .startsWith("all_follow_up_job_followup_candidate_0_0"));
    assertTrue(
        allResults
            .get(1)
            .calculationId()
            .value()
            .startsWith("all_follow_up_job_followup_candidate_0_1"));
    assertTrue(
        allResults
            .get(2)
            .calculationId()
            .value()
            .startsWith("all_follow_up_job_followup_candidate_0_2"));
    assertTrue(
        allResults.stream()
            .allMatch(
                calculationResult ->
                    calculationResult.hasSection(
                        ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS)));
    assertFalse(parityPlan.items().isEmpty());
    assertTrue(bundle.hasCandidateExecutionSummaries());
    assertEquals(1, bundle.candidateExecutionSummaryCount());
    assertEquals(3, summary.executedClassicalResultCount());
    assertEquals(0, summary.successfulClassicalResultCount());
    assertEquals(3, summary.partialClassicalResultCount());
    assertEquals(0, summary.failedClassicalResultCount());
    assertEquals(0, summary.missingClassicalResultCount());
    assertFalse(summary.readyForDecision());
    assertThrows(
        IllegalArgumentException.class, () -> service.executeAllFollowUpCalculations(result, 0));
  }

  @Test
  public void executionBundleBuildsMachineReadableCandidateTraceFromPredictionAndParity() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final Molecule heterocycle =
        ChemistryWorkflowServiceTest.ringMolecule("candidate_trace_diazine", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("candidate_trace_reaction"),
            "Candidate trace reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(heterocycle, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(heterocycle, StoichiometricCoefficient.ONE))));
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "candidate_trace_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                ReactionProductCandidate.of(
                    reaction.products(),
                    0.7,
                    70.0,
                    "trace seed candidate",
                    List.of(
                        ReactionPredictionEvidence.of(
                            ReactionPredictionEvidenceKind.USER_RULE,
                            "trace candidate supplied by test",
                            0.2)))),
            "candidate trace workflow");
    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("workflow_candidate_trace"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                reaction,
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("candidate_trace_classical"),
                    reaction,
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION)),
                BuiltInClassicalBackendProfiles.universalPlanning(),
                predictionRequest,
                null));
    final ChemistryComputationParityPlan parityPlan =
        ChemistryComputationParityPlan.of(
            "workflow_candidate_trace",
            List.of(
                ChemistryComputationParityItem.of(
                    0,
                    List.of("heterocycle_substitution"),
                    4,
                    List.of("candidate_0_energy"),
                    List.of("candidate_0_energy_result"),
                    List.of("candidate_0_vqe_program"),
                    ChemistryComputationParityStatus.READY,
                    "candidate trace parity ready")));

    final ChemistryWorkflowExecutionBundle bundle =
        ChemistryWorkflowExecutionBundle.of(result, List.of(), parityPlan);
    final ChemistryWorkflowCandidateTrace trace = bundle.candidateTrace(0);

    assertTrue(bundle.hasCandidateTraces());
    assertEquals(1, bundle.candidateTraceCount());
    assertEquals(1.0, trace.probability(), 1.0E-12);
    assertEquals(70.0, trace.expectedYieldPercent(), 1.0E-12);
    assertTrue(trace.evidenceCount() >= 1);
    assertEquals(4, trace.pathwayStepCount());
    assertTrue(trace.hasClassicalPlan());
    assertTrue(trace.hasClassicalResults());
    assertTrue(trace.hasQuantumPrograms());
    assertTrue(trace.readyForClassicalQuantumParity());
    final ChemistryWorkflowStoragePlan storagePlan =
        new ChemistryWorkflowStoragePlanner().plan(bundle);
    assertTrue(
        storagePlan.extensions().stream()
            .anyMatch(extension -> "workflow_candidate_trace".equals(extension.kind())));
  }

  @Test
  public void workflowPromotesReactionPredictionWarningsToDiagnostics() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final Molecule diazine = ChemistryWorkflowServiceTest.ringMolecule("warning_diazine", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("warning_prediction_reaction"),
            "Warning prediction reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest classicalRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("warning_prediction_classical"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "warning_prediction_request",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(ChemistryWorkflowServiceTest.warningProductCandidate(diazine, 1.0)),
            "warning evidence must become workflow diagnostic");

    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("workflow_prediction_warning"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                reaction,
                classicalRequest,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                predictionRequest,
                null));

    assertEquals(ChemistryWorkflowStatus.PARTIAL, result.status());
    assertTrue(result.hasConditionScenarioPlan());
    assertTrue(result.conditionScenarioPlan().hasKind(ReactionConditionScenarioKind.BASELINE));
    assertTrue(
        ChemistryWorkflowServiceTest.hasDiagnostic(
            result, ChemistryWorkflowDiagnosticCode.REACTION_PREDICTION_WARNING));
    assertTrue(result.hasReactionPredictionResult());
    assertTrue(result.hasRecommendations());
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.WARNING_REVIEW));
  }

  @Test
  public void computationParityReportsFailedClassicalResultAsBlockingDiagnostic() {
    final ChemistryComputationParityPlanner planner = new ChemistryComputationParityPlanner();
    final Molecule diazine =
        ChemistryWorkflowServiceTest.ringMolecule("parity_failed_diazine", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("parity_failed_reaction"),
            "Parity failed reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "parity_failed_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(ChemistryWorkflowServiceTest.productCandidate(diazine, 1.0)),
            "parity failure test");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            predictionRequest,
            ReactionPredictionMethodType.USER_SUPPLIED,
            predictionRequest.seedCandidates(),
            List.of());

    final ChemistryComputationParityPlan plan =
        planner.plan(
            "parity_failed_job",
            predictionResult,
            null,
            ChemistryWorkflowRecommendationPlan.EMPTY,
            ClassicalCalculationResult.of(
                ClassicalCalculationId.of("parity_failed_classical"),
                ClassicalCalculationStatus.FAILED,
                List.of(),
                null,
                ClassicalCalculationCoverageReport.of(
                    List.of(
                        ClassicalCalculationCoverageItem.of(
                            ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION,
                            ClassicalCoverageStatus.UNSUPPORTED,
                            "parity_failed_test_result",
                            List.of())))),
            null);

    assertTrue(plan.hasBlockingDiagnostics());
    assertEquals(ChemistryComputationParityStatus.CLASSICAL_ONLY, plan.firstItem().status());
    assertTrue(
        plan.firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.CLASSICAL_RESULT_NOT_SUCCEEDED));
    assertTrue(
        plan.firstItem()
            .hasDiagnostic(ChemistryComputationParityDiagnosticCode.MISSING_QUANTUM_PROGRAMS));
  }

  @Test
  public void computationParityReportsApproximateClassicalCoverageAsWarning() {
    final ChemistryComputationParityPlan plan =
        ChemistryWorkflowServiceTest.planWithClassicalCoverage(
            ClassicalCalculationStatus.PARTIAL,
            ClassicalCoverageStatus.APPROXIMATE,
            "harmonic_frequency_local_model");

    assertFalse(plan.hasBlockingDiagnostics());
    assertTrue(
        plan.firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.CLASSICAL_COVERAGE_APPROXIMATE));
    assertFalse(
        plan.firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.CLASSICAL_COVERAGE_UNSUPPORTED));
  }

  @Test
  public void computationParityReportsUnsupportedClassicalCoverageAsBlockingDiagnostic() {
    final ChemistryComputationParityPlan plan =
        ChemistryWorkflowServiceTest.planWithClassicalCoverage(
            ClassicalCalculationStatus.PARTIAL,
            ClassicalCoverageStatus.UNSUPPORTED,
            "unsupported_custom_basis");

    assertTrue(plan.hasBlockingDiagnostics());
    assertTrue(
        plan.firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.CLASSICAL_COVERAGE_UNSUPPORTED));
  }

  @Test
  public void computationParityReportsLowConfidenceAndIncompletePredictionMass() {
    final ChemistryComputationParityPlanner planner = new ChemistryComputationParityPlanner();
    final Molecule diazine =
        ChemistryWorkflowServiceTest.ringMolecule("parity_low_confidence_diazine", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "parity_low_confidence_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(ChemistryWorkflowServiceTest.productCandidate(diazine, 0.70)),
            "low confidence parity prediction");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());
    final ReactionPredictionExplanation explanation =
        ReactionPredictionExplanation.of(
            ReactionPredictionConfidenceLevel.LOW,
            0.70,
            0.0,
            0.70,
            1,
            1,
            1,
            true,
            null,
            null,
            List.of(),
            List.of());

    final ChemistryComputationParityPlan plan =
        planner.plan(
            "parity_low_confidence_job",
            predictionResult,
            explanation,
            ChemistryWorkflowRecommendationPlan.EMPTY,
            null,
            null);

    assertTrue(plan.hasBlockingDiagnostics());
    assertTrue(
        plan.firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.REACTION_PREDICTION_LOW_CONFIDENCE));
    assertTrue(
        plan.firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode
                    .REACTION_PREDICTION_INCOMPLETE_PROBABILITY_MASS));
  }

  @Test
  public void computationParityLinksCandidateSpecificQuantumPrograms() {
    final ChemistryComputationParityPlanner planner = new ChemistryComputationParityPlanner();
    final Molecule first = ChemistryWorkflowServiceTest.ringMolecule("parity_candidate_first", "N");
    final Molecule second =
        ChemistryWorkflowServiceTest.ringMolecule("parity_candidate_second", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "parity_multi_candidate_prediction",
            ReactionSide.of(List.of(ReactionParticipant.of(first, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                ChemistryWorkflowServiceTest.productCandidate(first, 0.65),
                ChemistryWorkflowServiceTest.productCandidate(second, 0.35)),
            "multi candidate parity");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());
    final ChemistryQuantumProgramPackage quantumPackage =
        ChemistryQuantumProgramPackage.of(
            "parity_multi_candidate_package",
            FermionToQubitMapping.JORDAN_WIGNER,
            2,
            1,
            List.of(
                ChemistryQuantumProgramArtifact.of(
                    "candidate_0_vqe",
                    ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ,
                    FermionToQubitMapping.JORDAN_WIGNER,
                    "candidate_0 product energy program",
                    ChemistryWorkflowServiceTest.singleQubitProgram("candidate_zero")),
                ChemistryQuantumProgramArtifact.of(
                    "candidate_1_vqe",
                    ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ,
                    FermionToQubitMapping.JORDAN_WIGNER,
                    "candidate_1 product energy program",
                    ChemistryWorkflowServiceTest.singleQubitProgram("candidate_one"))),
            List.of());

    final ChemistryComputationParityPlan plan =
        planner.plan(
            "parity_multi_candidate_job",
            predictionResult,
            null,
            ChemistryWorkflowRecommendationPlan.EMPTY,
            null,
            quantumPackage);

    assertEquals(2, plan.items().size());
    assertEquals(ChemistryComputationParityStatus.QUANTUM_ONLY, plan.items().get(0).status());
    assertEquals(List.of("candidate_0_vqe"), plan.items().get(0).quantumProgramIds());
    assertEquals(ChemistryComputationParityStatus.QUANTUM_ONLY, plan.items().get(1).status());
    assertEquals(List.of("candidate_1_vqe"), plan.items().get(1).quantumProgramIds());
    assertFalse(
        plan.items()
            .get(1)
            .hasDiagnostic(ChemistryComputationParityDiagnosticCode.MISSING_QUANTUM_PROGRAMS));
  }

  @Test
  public void computationParityLinksCandidateSpecificClassicalResults() {
    final ChemistryComputationParityPlanner planner = new ChemistryComputationParityPlanner();
    final Molecule first =
        ChemistryWorkflowServiceTest.ringMolecule("parity_classical_candidate_first", "N");
    final Molecule second =
        ChemistryWorkflowServiceTest.ringMolecule("parity_classical_candidate_second", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "parity_classical_multi_candidate_prediction",
            ReactionSide.of(List.of(ReactionParticipant.of(first, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                ChemistryWorkflowServiceTest.productCandidate(first, 0.55),
                ChemistryWorkflowServiceTest.productCandidate(second, 0.45)),
            "multi candidate classical parity");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());
    final ClassicalCalculationResult candidateOneResult =
        ClassicalCalculationResult.of(
            ClassicalCalculationId.of("candidate_1_reaction_energy"),
            ClassicalCalculationStatus.SUCCEEDED,
            List.of(),
            null,
            ClassicalCalculationCoverageReport.of(
                List.of(
                    ClassicalCalculationCoverageItem.of(
                        ClassicalCalculationKind.REACTION_ENERGY,
                        ClassicalCoverageStatus.EXACT,
                        "candidate-specific reaction energy",
                        List.of()))));

    final ChemistryComputationParityPlan plan =
        planner.planWithClassicalResults(
            "parity_classical_multi_candidate_job",
            predictionResult,
            null,
            ChemistryWorkflowRecommendationPlan.EMPTY,
            List.of(candidateOneResult),
            null);

    assertEquals(2, plan.items().size());
    assertEquals(ChemistryComputationParityStatus.MISSING_BOTH, plan.items().get(0).status());
    assertTrue(plan.items().get(0).classicalResultIds().isEmpty());
    assertEquals(ChemistryComputationParityStatus.CLASSICAL_ONLY, plan.items().get(1).status());
    assertEquals(List.of("candidate_1_reaction_energy"), plan.items().get(1).classicalResultIds());
    assertFalse(
        plan.items()
            .get(0)
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.CLASSICAL_RESULT_NOT_SUCCEEDED));
  }

  @Test
  public void computationParityLinksQuantumProgramsFromCandidateFollowUpResults() {
    final ChemistryComputationParityPlanner planner = new ChemistryComputationParityPlanner();
    final Molecule first =
        ChemistryWorkflowServiceTest.ringMolecule("parity_followup_quantum_candidate_first", "N");
    final Molecule second =
        ChemistryWorkflowServiceTest.ringMolecule("parity_followup_quantum_candidate_second", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "parity_followup_quantum_multi_candidate_prediction",
            ReactionSide.of(List.of(ReactionParticipant.of(first, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                ChemistryWorkflowServiceTest.productCandidate(first, 0.56),
                ChemistryWorkflowServiceTest.productCandidate(second, 0.44)),
            "multi candidate quantum follow-up parity");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());
    final ClassicalCalculationResult candidateOneQuantumResult =
        ClassicalCalculationResult.of(
            ClassicalCalculationId.of("candidate_1_quantum_program_preparation"),
            ClassicalCalculationStatus.SUCCEEDED,
            List.of(
                ClassicalResultSection.of(
                    ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION,
                    "Candidate 1 quantum program preparation",
                    List.of(
                        ClassicalResultValue.of(
                            "artifacts",
                            ClassicalResultValueKind.TABLE,
                            "",
                            List.of(
                                "id\tkind\tmapping\toperations\tdescription",
                                "candidate_1_jordan_wigner_pauli_evolution\tPAULI_EVOLUTION\tJORDAN_WIGNER\t12\tcandidate program"))))),
            null,
            ClassicalCalculationCoverageReport.of(
                List.of(
                    ClassicalCalculationCoverageItem.of(
                        ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION,
                        ClassicalCoverageStatus.EXACT,
                        "candidate quantum program preparation",
                        List.of()))));

    final ChemistryComputationParityPlan plan =
        planner.planWithClassicalResults(
            "parity_followup_quantum_multi_candidate_job",
            predictionResult,
            null,
            ChemistryWorkflowRecommendationPlan.EMPTY,
            List.of(candidateOneQuantumResult),
            null);

    assertEquals(2, plan.items().size());
    assertEquals(ChemistryComputationParityStatus.MISSING_BOTH, plan.items().get(0).status());
    assertEquals(ChemistryComputationParityStatus.READY, plan.items().get(1).status());
    assertEquals(
        List.of("candidate_1_jordan_wigner_pauli_evolution"),
        plan.items().get(1).quantumProgramIds());
    assertFalse(
        plan.items()
            .get(1)
            .hasDiagnostic(ChemistryComputationParityDiagnosticCode.MISSING_QUANTUM_PROGRAMS));
  }

  @Test
  public void computationParityLinksCandidateSpecificQuantumSkips() {
    final ChemistryComputationParityPlanner planner = new ChemistryComputationParityPlanner();
    final Molecule first =
        ChemistryWorkflowServiceTest.ringMolecule("parity_skip_candidate_first", "N");
    final Molecule second =
        ChemistryWorkflowServiceTest.ringMolecule("parity_skip_candidate_second", "C");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "parity_skip_multi_candidate_prediction",
            ReactionSide.of(List.of(ReactionParticipant.of(first, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(
                ChemistryWorkflowServiceTest.productCandidate(first, 0.6),
                ChemistryWorkflowServiceTest.productCandidate(second, 0.4)),
            "multi candidate quantum skip parity");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());
    final ChemistryQuantumProgramPackage quantumPackage =
        ChemistryQuantumProgramPackage.of(
            "parity_skip_multi_candidate_package",
            FermionToQubitMapping.JORDAN_WIGNER,
            2,
            1,
            List.of(),
            List.of(
                ChemistryQuantumProgramSkip.of(
                    "candidate_1_qpe_skip",
                    ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION,
                    FermionToQubitMapping.JORDAN_WIGNER,
                    "candidate_1 active space exceeds requested QPE budget")));

    final ChemistryComputationParityPlan plan =
        planner.plan(
            "parity_skip_multi_candidate_job",
            predictionResult,
            null,
            ChemistryWorkflowRecommendationPlan.EMPTY,
            null,
            quantumPackage);

    assertEquals(2, plan.items().size());
    assertFalse(
        plan.items()
            .get(0)
            .hasDiagnostic(ChemistryComputationParityDiagnosticCode.QUANTUM_PROGRAM_SKIPPED));
    assertTrue(
        plan.items()
            .get(1)
            .hasDiagnostic(ChemistryComputationParityDiagnosticCode.QUANTUM_PROGRAM_SKIPPED));
  }

  @Test
  public void computationParityMarksAdaptVqePoolAsPendingSelectionRun() {
    final ChemistryComputationParityPlanner planner = new ChemistryComputationParityPlanner();
    final Molecule diazine =
        ChemistryWorkflowServiceTest.ringMolecule("parity_adapt_vqe_diazine", "N");
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "parity_adapt_vqe_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(ChemistryWorkflowServiceTest.productCandidate(diazine, 0.7)),
            "ADAPT-VQE parity candidate");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());
    final ChemistryQuantumProgramPackage quantumPackage =
        ChemistryQuantumProgramPackage.of(
            "parity_adapt_vqe_package",
            FermionToQubitMapping.JORDAN_WIGNER,
            2,
            1,
            List.of(
                ChemistryQuantumProgramArtifact.of(
                    "candidate_0_adapt_vqe_pool",
                    ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ,
                    null,
                    "candidate_0 ADAPT-VQE operator-pool program",
                    ChemistryWorkflowServiceTest.singleQubitProgram("candidate_adapt_vqe"))),
            List.of());

    final ChemistryComputationParityPlan plan =
        planner.plan(
            "parity_adapt_vqe_job",
            predictionResult,
            null,
            ChemistryWorkflowRecommendationPlan.EMPTY,
            null,
            quantumPackage);

    assertEquals(ChemistryComputationParityStatus.QUANTUM_ONLY, plan.firstItem().status());
    assertTrue(
        plan.firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.ADAPT_VQE_SELECTION_NOT_EXECUTED));
    assertFalse(
        plan.firstItem()
            .hasDiagnostic(
                ChemistryComputationParityDiagnosticCode.QUANTUM_PROGRAM_WITHOUT_MAPPING));
  }

  @Test
  public void followUpPlannerCreatesMechanismSpecificCalculationChecks() {
    final ReactionPredictionFollowUpPlanner planner = new ReactionPredictionFollowUpPlanner();
    final Molecule diazine =
        ChemistryWorkflowServiceTest.ringMolecule("mechanism_follow_up_diazine", "N");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            0.8,
            62.0,
            "mechanism follow-up candidate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Canonical C-H activation mechanism contributes 5 detailed staged events.",
                    0.2),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Canonical Sandmeyer substitution mechanism contributes electron transfer route.",
                    0.15),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Canonical polymerization mechanism indicates radical kinetic competition.",
                    0.1)),
            List.of());
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "mechanism_follow_up_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(candidate),
            "mechanism follow-up");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());

    final ChemistryWorkflowRecommendationPlan plan =
        planner.plan("mechanism_follow_up_job", result);

    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.TRANSITION_STATE_SEARCH));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.REACTION_PATH));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.KINETIC_ANALYSIS));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.DESCRIPTOR_ANALYSIS));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.BASIS_SET_CONVERGENCE));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.METHOD_BENCHMARK));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.MECHANISM_ENUMERATION));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.SOLVENT_SCREENING));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.CATALYST_SCREENING));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS));
  }

  @Test
  public void followUpPlannerKeepsMechanismChecksCandidateResolved() {
    final ReactionPredictionFollowUpPlanner planner = new ReactionPredictionFollowUpPlanner();
    final ChemistryFollowUpCalculationPlanner calculationPlanner =
        new ChemistryFollowUpCalculationPlanner();
    final Molecule first =
        ChemistryWorkflowServiceTest.ringMolecule("mechanism_candidate_first", "N");
    final Molecule second =
        ChemistryWorkflowServiceTest.ringMolecule("mechanism_candidate_second", "C");
    final ReactionProductCandidate firstCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(List.of(ReactionParticipant.of(first, StoichiometricCoefficient.ONE))),
            0.55,
            55.0,
            "first mechanism candidate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Canonical C-H activation mechanism contributes 5 detailed staged events.",
                    0.2)));
    final ReactionProductCandidate secondCandidate =
        ReactionProductCandidate.of(
            ReactionSide.of(List.of(ReactionParticipant.of(second, StoichiometricCoefficient.ONE))),
            0.45,
            45.0,
            "second mechanism candidate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Canonical C-H activation mechanism contributes 5 detailed staged events.",
                    0.2)));
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("candidate_resolved_mechanism_reaction"),
            "Candidate resolved mechanism reaction",
            ReactionSide.of(List.of(ReactionParticipant.of(first, StoichiometricCoefficient.ONE))),
            ReactionSide.of(List.of(ReactionParticipant.of(first, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "candidate_resolved_mechanism_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(firstCandidate, secondCandidate),
            "candidate resolved mechanism follow-up");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());
    final ChemistryWorkflowRecommendationPlan recommendationPlan =
        planner.plan("candidate_resolved_mechanism_job", result);
    final ChemistryWorkflowJob job =
        ChemistryWorkflowJob.of(
            ChemistryWorkflowJobId.of("candidate_resolved_mechanism_job"),
            ChemistryWorkflowMode.CLASSICAL_ONLY,
            reaction,
            ClassicalCalculationRequest.of(
                ClassicalCalculationId.of("candidate_resolved_mechanism_classical"),
                reaction,
                ElectronicStructureMethod.HARTREE_FOCK,
                BasisSet.STO_3G,
                List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION)),
            BuiltInClassicalBackendProfiles.universalPlanning(),
            request,
            null);

    final ChemistryFollowUpCalculationPlan calculationPlan =
        calculationPlanner.planAll(job, recommendationPlan, result);

    assertTrue(
        recommendationPlan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 0
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.REACTION_PATH));
    assertTrue(
        recommendationPlan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 1
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.REACTION_PATH));
    assertTrue(
        calculationPlan.requestsForCandidate(0).stream()
            .anyMatch(
                requestForCandidate -> requestForCandidate.id().value().contains("candidate_0")));
    assertTrue(
        calculationPlan.requestsForCandidate(1).stream()
            .anyMatch(
                requestForCandidate -> requestForCandidate.id().value().contains("candidate_1")));
    assertTrue(
        recommendationPlan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 0
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.MP2_CORRELATION));
    assertTrue(
        recommendationPlan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 0
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.CCSD_CORRELATION));
    assertTrue(
        recommendationPlan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 0
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.CISD_CORRELATION));
    assertTrue(
        recommendationPlan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 0
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.FCI_DIAGONALIZATION));
    assertTrue(
        calculationPlan.requestsForCandidate(0).stream()
            .filter(
                requestForCandidate ->
                    requestForCandidate
                        .calculationKinds()
                        .contains(ClassicalCalculationKind.MP2_CORRELATION))
            .anyMatch(requestForCandidate -> requestForCandidate.subject() == first));
    assertTrue(
        calculationPlan.requestsForCandidate(0).stream()
            .filter(
                requestForCandidate ->
                    requestForCandidate
                        .calculationKinds()
                        .contains(ClassicalCalculationKind.CCSD_CORRELATION))
            .anyMatch(requestForCandidate -> requestForCandidate.subject() == first));
  }

  @Test
  public void followUpPlannerKeepsMultiProductCandidateReactionContext() {
    final ReactionPredictionFollowUpPlanner planner = new ReactionPredictionFollowUpPlanner();
    final ChemistryFollowUpCalculationPlanner calculationPlanner =
        new ChemistryFollowUpCalculationPlanner();
    final Molecule reactant =
        ChemistryWorkflowServiceTest.acyclicMolecule("multi_product_context_reactant", "O");
    final Molecule firstProduct =
        ChemistryWorkflowServiceTest.acyclicMolecule("multi_product_context_first", "C");
    final Molecule secondProduct =
        ChemistryWorkflowServiceTest.acyclicMolecule("multi_product_context_second", "N");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(firstProduct, StoichiometricCoefficient.ONE),
                    ReactionParticipant.of(secondProduct, StoichiometricCoefficient.ONE))),
            0.61,
            58.0,
            "multi-product cleavage candidate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Canonical C-H activation mechanism contributes 5 detailed staged events.",
                    0.2)));
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("multi_product_context_reaction"),
            "Multi product context reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "multi_product_context_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            List.of(candidate),
            "multi product context follow-up");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            predictionRequest,
            ReactionPredictionMethodType.USER_SUPPLIED,
            predictionRequest.seedCandidates(),
            List.of());
    final ChemistryWorkflowRecommendationPlan recommendationPlan =
        planner.plan("multi_product_context_job", predictionResult);
    final ChemistryWorkflowJob job =
        ChemistryWorkflowJob.of(
            ChemistryWorkflowJobId.of("multi_product_context_job"),
            ChemistryWorkflowMode.CLASSICAL_ONLY,
            reaction,
            ClassicalCalculationRequest.of(
                ClassicalCalculationId.of("multi_product_context_classical"),
                reaction,
                ElectronicStructureMethod.HARTREE_FOCK,
                BasisSet.STO_3G,
                List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION)),
            BuiltInClassicalBackendProfiles.universalPlanning(),
            predictionRequest,
            null);

    final ChemistryFollowUpCalculationPlan calculationPlan =
        calculationPlanner.planAll(job, recommendationPlan, predictionResult);
    final List<ClassicalCalculationRequest> postHfRequests =
        calculationPlan.requestsForCandidate(0).stream()
            .filter(
                requestForCandidate ->
                    requestForCandidate
                        .calculationKinds()
                        .contains(ClassicalCalculationKind.MP2_CORRELATION))
            .toList();

    assertEquals(2, postHfRequests.size());
    assertEquals(ChemistrySubjectKind.MOLECULE, postHfRequests.get(0).subject().subjectKind());
    assertEquals(ChemistrySubjectKind.MOLECULE, postHfRequests.get(1).subject().subjectKind());
    assertTrue(
        postHfRequests.stream()
            .anyMatch(requestForCandidate -> requestForCandidate.subject() == firstProduct));
    assertTrue(
        postHfRequests.stream()
            .anyMatch(requestForCandidate -> requestForCandidate.subject() == secondProduct));
    assertTrue(
        postHfRequests.stream()
            .anyMatch(
                requestForCandidate ->
                    requestForCandidate.options().stream()
                        .anyMatch(
                            option ->
                                option.name().equals("workflow_product_index")
                                    && option.value().equals("0"))));
    assertTrue(
        postHfRequests.stream()
            .anyMatch(
                requestForCandidate ->
                    requestForCandidate.options().stream()
                        .anyMatch(
                            option ->
                                option.name().equals("workflow_product_index")
                                    && option.value().equals("1"))));
  }

  @Test
  public void followUpPlannerKeepsProbableSideProductsBeyondLegacyFiveCandidateLimit() {
    final ReactionPredictionFollowUpPlanner planner = new ReactionPredictionFollowUpPlanner();
    final Molecule reactant =
        ChemistryWorkflowServiceTest.ringMolecule("probable_side_product_followup_reactant", "N");
    final ArrayList<ReactionProductCandidate> candidates =
        new ArrayList<ReactionProductCandidate>();
    for (int i = 0; i < 7; ++i) {
      candidates.add(
          ChemistryWorkflowServiceTest.productCandidate(
              ChemistryWorkflowServiceTest.ringMolecule(
                  "probable_side_product_followup_candidate_" + i, i % 2 == 0 ? "N" : "C"),
              i == 6 ? 0.04 : 0.12));
    }
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "probable_side_product_followup_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            candidates,
            "probable side product follow-up");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());

    final ChemistryWorkflowRecommendationPlan plan =
        planner.plan("probable_side_product_followup_job", result);

    assertTrue(
        plan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 6
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION));
  }

  @Test
  public void followUpPlannerTurnsSurrogateProductChannelsIntoMaterializationActions() {
    final ReactionPredictionFollowUpPlanner planner = new ReactionPredictionFollowUpPlanner();
    final Molecule reactant =
        ChemistryWorkflowServiceTest.ringMolecule("surrogate_materialization_reactant", "N");
    final Molecule placeholderProduct =
        ChemistryWorkflowServiceTest.ringMolecule("surrogate_materialization_placeholder", "N");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(placeholderProduct, StoichiometricCoefficient.ONE))),
            0.31,
            22.0,
            "surrogate product channel",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.SURROGATE_PRODUCT,
                    "Fallback surrogate product channel; explicit molecule graph is not yet resolved.",
                    -0.35),
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Heterocycle substitution mechanism path remains chemically plausible.",
                    0.30)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "surrogate_materialization_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(candidate),
            "surrogate materialization follow-up");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());

    final ChemistryWorkflowRecommendationPlan plan =
        planner.plan("surrogate_materialization_follow_up_job", result);

    assertTrue(
        plan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 0
                        && recommendation.calculationKind() == null
                        && "candidate_0/explicit-product-graph-materialization"
                            .equals(recommendation.target())),
        plan.recommendations().toString());
    assertTrue(
        plan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 0
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.REACTION_PATH
                        && "candidate_0/surrogate-mechanism-to-structure-path"
                            .equals(recommendation.target())),
        plan.recommendations().toString());
    assertFalse(
        plan.recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.candidateIndex() == 0
                        && recommendation.calculationKind()
                            == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
        plan.recommendations().toString());
  }

  @Test
  public void storageRoadmapTreatsSurrogatePlaceholderAsMissingExplicitProductGraph() {
    final ReactionPredictionFollowUpPlanner planner = new ReactionPredictionFollowUpPlanner();
    final ChemistryWorkflowStoragePlanner storagePlanner = new ChemistryWorkflowStoragePlanner();
    final Molecule reactant =
        ChemistryWorkflowServiceTest.ringMolecule("surrogate_storage_reactant", "N");
    final Molecule placeholderProduct =
        ChemistryWorkflowServiceTest.ringMolecule("surrogate_storage_placeholder", "N");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(placeholderProduct, StoichiometricCoefficient.ONE))),
            0.31,
            22.0,
            "surrogate product channel",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.SURROGATE_PRODUCT,
                    "Fallback surrogate product channel; explicit molecule graph is not yet resolved.",
                    -0.35)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "surrogate_storage_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(candidate),
            "surrogate storage follow-up");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());
    final ChemistryWorkflowRecommendationPlan recommendationPlan =
        planner.plan("surrogate_storage_follow_up_job", result);
    final ChemistryWorkflowStoragePlan storagePlan =
        storagePlanner.plan(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("surrogate_storage_follow_up_job"),
                ChemistryWorkflowMode.VALIDATE_ONLY,
                reactant,
                null,
                null,
                null),
            result,
            recommendationPlan,
            null,
            List.of());

    assertTrue(storagePlan.hasRoadmapRecord());
    assertFalse(storagePlan.roadmapRecord().candidates().get(0).explicitProductGraph());
    assertFalse(storagePlan.roadmapRecord().candidates().get(0).electronicRerankReady());
    assertFalse(storagePlan.roadmapRecord().candidates().get(0).quantumWorkflowReady());
    assertEquals(
        "missing_explicit_product_graph",
        storagePlan.roadmapRecord().candidates().get(0).readinessReason());
    assertTrue(
        storagePlan
            .roadmapRecord()
            .candidates()
            .get(0)
            .followUpActionTargets()
            .contains("candidate_0/explicit-product-graph-materialization"));
    assertTrue(
        storagePlan
            .roadmapRecord()
            .candidates()
            .get(0)
            .followUpActionTargets()
            .contains("candidate_0/surrogate-mechanism-to-structure-path"));
    assertTrue(
        storagePlan.roadmapRecord().candidates().get(0).quantumPreparationTargets().isEmpty());
  }

  @Test
  public void followUpPlannerTreatsCarbonylAdditionAsElectronicStructureSensitive() {
    final ReactionPredictionFollowUpPlanner planner = new ReactionPredictionFollowUpPlanner();
    final Molecule reactant =
        ChemistryWorkflowServiceTest.acyclicMolecule("carbonyl_addition_follow_up_reactant", "O");
    final Molecule product =
        ChemistryWorkflowServiceTest.acyclicMolecule("carbonyl_addition_follow_up_product", "C");
    final ReactionProductCandidate candidate =
        ReactionProductCandidate.of(
            ReactionSide.of(
                List.of(ReactionParticipant.of(product, StoichiometricCoefficient.ONE))),
            0.74,
            71.0,
            "carbonyl addition follow-up candidate",
            List.of(
                ReactionPredictionEvidence.of(
                    ReactionPredictionEvidenceKind.USER_RULE,
                    "Organometallic carbonyl addition through enolate-like polarized transition state.",
                    0.35)));
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "carbonyl_addition_follow_up_prediction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(reactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            List.of(OrganicReactionDomain.ADDITION),
            List.of(candidate),
            "carbonyl addition follow-up");
    final ReactionPredictionResult result =
        ReactionPredictionResult.of(
            request,
            ReactionPredictionMethodType.USER_SUPPLIED,
            request.seedCandidates(),
            List.of());

    final ChemistryWorkflowRecommendationPlan plan =
        planner.plan("carbonyl_addition_follow_up_job", result);

    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION));
    assertTrue(
        ChemistryWorkflowServiceTest.hasRecommendationCalculation(
            plan, ClassicalCalculationKind.HAMILTONIAN_PREPARATION));
  }

  @Test
  public void workflowTurnsIncompleteCrossCouplingEvidenceIntoConditionRepairRecommendations() {
    final ChemistryWorkflowService service = new ChemistryWorkflowService();
    final Molecule heteroarylReactant =
        ChemistryWorkflowServiceTest.ringMolecule("repair_cross_coupling_reactant", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("repair_cross_coupling_reaction"),
            "Repair cross-coupling reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(heteroarylReactant, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(heteroarylReactant, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ClassicalCalculationRequest classicalRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("repair_cross_coupling_classical"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "repair_cross_coupling_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.CROSS_COUPLING),
            List.of(ChemistryWorkflowServiceTest.productCandidate(heteroarylReactant, 0.7)),
            "cross-coupling without explicit catalyst/base must become actionable");

    final ChemistryWorkflowResult result =
        service.run(
            ChemistryWorkflowJob.of(
                ChemistryWorkflowJobId.of("workflow_cross_coupling_repair"),
                ChemistryWorkflowMode.CLASSICAL_ONLY,
                reaction,
                classicalRequest,
                BuiltInClassicalBackendProfiles.universalPlanning(),
                predictionRequest,
                null));

    assertEquals(ChemistryWorkflowStatus.PARTIAL, result.status());
    assertTrue(result.hasRecommendations());
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.CONDITION_REPAIR
                        && "conditions/catalyst".equals(recommendation.target())));
    assertTrue(
        result.recommendationPlan().recommendations().stream()
            .anyMatch(
                recommendation ->
                    recommendation.kind() == ChemistryWorkflowRecommendationKind.CONDITION_REPAIR
                        && "conditions/base".equals(recommendation.target())));
    assertTrue(
        result.reactionPredictionResult().diagnostics().stream()
            .anyMatch(
                evidence -> evidence.description().contains("no explicit catalyst component")));
    assertTrue(
        result.reactionPredictionResult().diagnostics().stream()
            .anyMatch(evidence -> evidence.description().contains("no base component")));
  }

  private static ClassicalCalculationRequest classicalRequest() {
    return ChemistryWorkflowServiceTest.classicalRequest(
        ChemistryWorkflowServiceTest.hydrogen(), BasisSet.STO_3G, ActiveSpace.of(2, 2));
  }

  private static ClassicalCalculationRequest classicalRequest(
      final Molecule molecule, final BasisSet basisSet, final ActiveSpace activeSpace) {
    return ChemistryWorkflowServiceTest.classicalRequest(
        molecule, basisSet, activeSpace, List.of());
  }

  private static ClassicalCalculationRequest classicalRequest(
      final Molecule molecule,
      final BasisSet basisSet,
      final ActiveSpace activeSpace,
      final List<ClassicalCalculationOption> options) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(molecule.id().value() + "_classical"),
        molecule,
        ElectronicStructureMethod.HARTREE_FOCK,
        basisSet,
        activeSpace,
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        options,
        ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
  }

  private static ClassicalCalculationRequest propertyRequest(
      final String id, final Molecule molecule) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(id),
        molecule,
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS));
  }

  private static ChemistryWorkflowResult resultWithPropertyRecommendations(
      final Molecule molecule) {
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("all_follow_up_reaction"),
            "All follow-up reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE))));
    final ChemistryWorkflowJob job =
        ChemistryWorkflowJob.of(
            ChemistryWorkflowJobId.of("all_follow_up_job"),
            ChemistryWorkflowMode.CLASSICAL_ONLY,
            reaction,
            ChemistryWorkflowServiceTest.propertyRequest("all_property_seed", molecule),
            BuiltInClassicalBackendProfiles.universalPlanning(),
            null,
            null);
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "all_follow_up_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(ChemistryWorkflowServiceTest.productCandidate(molecule, 1.0)),
            "all follow-up bounded execution");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            predictionRequest,
            ReactionPredictionMethodType.USER_SUPPLIED,
            predictionRequest.seedCandidates(),
            List.of());
    final ChemistryWorkflowRecommendationPlan recommendationPlan =
        ChemistryWorkflowRecommendationPlan.of(
            "all_follow_up_job",
            List.of(
                ChemistryWorkflowRecommendation.of(
                    ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                    ChemistryWorkflowRecommendationPriority.HIGH,
                    0,
                    ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS,
                    "all_property_0",
                    "Refine candidate property zero."),
                ChemistryWorkflowRecommendation.of(
                    ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                    ChemistryWorkflowRecommendationPriority.HIGH,
                    0,
                    ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS,
                    "all_property_1",
                    "Refine candidate property one."),
                ChemistryWorkflowRecommendation.of(
                    ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                    ChemistryWorkflowRecommendationPriority.MEDIUM,
                    0,
                    ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS,
                    "all_property_2",
                    "Refine candidate property two.")));
    return ChemistryWorkflowResult.of(
        job,
        ChemistryWorkflowStatus.SUCCESS,
        null,
        null,
        null,
        predictionResult,
        recommendationPlan,
        null,
        null,
        List.of(
            ChemistryWorkflowStageResult.of(
                ChemistryWorkflowStage.FOLLOW_UP_PLANNING,
                ChemistryWorkflowStageStatus.COMPLETED,
                "recommendations prepared")),
        ChemistryWorkflowDiagnosticSet.EMPTY);
  }

  private static ClassicalCalculationRequest casscfOrbitalRotationRequest() {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("h2_casscf_orbital_rotation_workflow"),
        ChemistryWorkflowServiceTest.hydrogen(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
            ElectronicStructureMethodName.of("CASSCF")),
        BasisSet.SIX_31G,
        ActiveSpace.of(2, 2),
        List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
        List.of(
            ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
            ClassicalCalculationOption.integer("fci_beta_electrons", 1),
            ClassicalCalculationOption.text("casscf_orbital_rotation_space", "all_non_redundant"),
            ClassicalCalculationOption.integer("casscf_max_iterations", 12),
            ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 256),
            ClassicalCalculationOption.floating("casscf_initial_step", 0.08),
            ClassicalCalculationOption.floating("casscf_minimum_step", 1.0E-5),
            ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-8)),
        ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
  }

  private static ClassicalCalculationRequest casscfOrbitalRotationHamiltonianRequest() {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("h2_casscf_orbital_rotation_quantum_workflow"),
        ChemistryWorkflowServiceTest.hydrogen(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
            ElectronicStructureMethodName.of("CASSCF")),
        BasisSet.SIX_31G,
        ActiveSpace.of(2, 2),
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(
            ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
            ClassicalCalculationOption.integer("fci_beta_electrons", 1),
            ClassicalCalculationOption.text("casscf_orbital_rotation_space", "all_non_redundant"),
            ClassicalCalculationOption.integer("casscf_max_iterations", 12),
            ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 256),
            ClassicalCalculationOption.floating("casscf_initial_step", 0.08),
            ClassicalCalculationOption.floating("casscf_minimum_step", 1.0E-5),
            ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-8)),
        ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
  }

  private static ClassicalCalculationRequest casscfOptimizedHamiltonianRequest() {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("h2_casscf_optimized_hamiltonian_preparation"),
        ChemistryWorkflowServiceTest.hydrogen(),
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        ActiveSpace.of(2, 2),
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(
            ClassicalCalculationOption.text("hamiltonian_orbital_optimization", "casscf"),
            ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
            ClassicalCalculationOption.integer("fci_beta_electrons", 1),
            ClassicalCalculationOption.text("casscf_orbital_rotation_space", "all_non_redundant"),
            ClassicalCalculationOption.integer("casscf_max_iterations", 4),
            ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 2),
            ClassicalCalculationOption.floating("casscf_initial_step", 0.05),
            ClassicalCalculationOption.floating("casscf_minimum_step", 1.0E-5),
            ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-8)),
        ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
  }

  private static ClassicalCalculationRequest directUnrestrictedLithiumHamiltonianRequest() {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("li_direct_uhf_hamiltonian"),
        ChemistryWorkflowServiceTest.lithiumAtom(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.HARTREE_FOCK,
            ElectronicStructureMethodName.of("UHF"),
            ElectronicStructureSpinTreatment.UNRESTRICTED),
        BasisSet.STO_3G,
        ActiveSpace.of(3, 5),
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(ClassicalCalculationOption.text("scf_integral_mode", "direct")),
        ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
  }

  private static String classicalValue(final ChemistryWorkflowResult result, final String name) {
    final List<ClassicalResultSection> sections = result.classicalCalculationResult().sections();
    for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
      final List<ClassicalResultValue> values = sections.get(sectionIndex).values();
      for (int valueIndex = 0; valueIndex < values.size(); ++valueIndex) {
        final ClassicalResultValue value = values.get(valueIndex);
        if (value.name().equals(name)) {
          return value.firstValue();
        }
      }
    }
    throw new IllegalArgumentException("Missing classical workflow result value: " + name);
  }

  private static boolean hasClassicalValue(
      final ClassicalCalculationResult result, final String name) {
    final List<ClassicalResultSection> sections = result.sections();
    for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
      final List<ClassicalResultValue> values = sections.get(sectionIndex).values();
      for (int valueIndex = 0; valueIndex < values.size(); ++valueIndex) {
        if (values.get(valueIndex).name().equals(name)) {
          return true;
        }
      }
    }
    return false;
  }

  private static String workflowStageSummary(final ChemistryWorkflowResult result) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < result.stageResults().size(); ++i) {
      if (i > 0) {
        builder.append("; ");
      }
      builder
          .append(result.stageResults().get(i).stage())
          .append('=')
          .append(result.stageResults().get(i).status())
          .append('(')
          .append(result.stageResults().get(i).message())
          .append(')');
    }
    return builder.toString();
  }

  private static QuantumProgram singleQubitProgram(final String circuitName) {
    return Quantum.programBuilder()
        .circuit(circuitName)
        .qreg("q", 1)
        .creg("c", 1)
        .h("q[0]")
        .measure("q[0]", "c[0]")
        .build();
  }

  private static ChemistryComputationParityPlan planWithClassicalCoverage(
      final ClassicalCalculationStatus resultStatus,
      final ClassicalCoverageStatus coverageStatus,
      final String coverageModel) {
    final ChemistryComputationParityPlanner planner = new ChemistryComputationParityPlanner();
    final Molecule diazine =
        ChemistryWorkflowServiceTest.ringMolecule("coverage_parity_diazine", "N");
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("coverage_parity_reaction"),
            "Coverage parity reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(diazine, StoichiometricCoefficient.ONE))),
            ReactionConditions.EMPTY,
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
    final ReactionPredictionRequest predictionRequest =
        ReactionPredictionRequest.of(
            "coverage_parity_prediction",
            reaction.reactants(),
            reaction.conditions(),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(ChemistryWorkflowServiceTest.productCandidate(diazine, 1.0)),
            "coverage parity test");
    final ReactionPredictionResult predictionResult =
        ReactionPredictionResult.of(
            predictionRequest,
            ReactionPredictionMethodType.USER_SUPPLIED,
            predictionRequest.seedCandidates(),
            List.of());
    final ClassicalCalculationResult classicalResult =
        ClassicalCalculationResult.of(
            ClassicalCalculationId.of("coverage_parity_classical"),
            resultStatus,
            List.of(),
            null,
            ClassicalCalculationCoverageReport.of(
                List.of(
                    ClassicalCalculationCoverageItem.of(
                        ClassicalCalculationKind.FREQUENCY_ANALYSIS,
                        coverageStatus,
                        coverageModel,
                        List.of()))));
    return planner.plan(
        "coverage_parity_job",
        predictionResult,
        null,
        ChemistryWorkflowRecommendationPlan.EMPTY,
        classicalResult,
        null);
  }

  private static ClassicalCalculationRequest classicalRequest(
      final BasisSet basisSet, final ActiveSpace activeSpace) {
    return ChemistryWorkflowServiceTest.classicalRequest(
        ChemistryWorkflowServiceTest.hydrogen(), basisSet, activeSpace);
  }

  private static VqeOptimizationRequest vqeRequest() {
    return ChemistryWorkflowServiceTest.vqeRequest(ActiveSpace.of(2, 2), 4);
  }

  private static VqeOptimizationRequest vqeRequest(
      final ActiveSpace activeSpace, final int parameterCount) {
    final double[] parameters = new double[parameterCount];
    return VqeOptimizationRequest.of(
        activeSpace,
        VariationalChemistryOptions.of(
            ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE,
            "workflow_vqe",
            "q",
            "c",
            "theta",
            1,
            activeSpace.spinOrbitalCount(),
            true),
        parameters,
        VqeOptimizationOptions.of(16, 256, 0.002, 1.0E-5, 1.0E-12, 0.001, 1.0E-3, 1.0E-8),
        ErrorMitigationRequest.of(
            activeSpace,
            QuantumNoiseModel.ideal(),
            List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
            128,
            false,
            false));
  }

  private static boolean hasDiagnostic(
      final ChemistryWorkflowResult result, final ChemistryWorkflowDiagnosticCode code) {
    for (int i = 0; i < result.diagnostics().diagnostics().size(); ++i) {
      if (result.diagnostics().diagnostics().get(i).code() == code) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasRecommendationCalculation(
      final ChemistryWorkflowRecommendationPlan plan, final ClassicalCalculationKind kind) {
    return plan.recommendations().stream()
        .anyMatch(recommendation -> recommendation.calculationKind() == kind);
  }

  private static boolean hasQuantumProgramKind(
      final ChemistryQuantumProgramPackage pack, final ChemistryQuantumProgramKind kind) {
    return pack.artifacts().stream().anyMatch(artifact -> artifact.kind() == kind);
  }

  private static ElectronicStructureProblem problem() {
    final ActiveSpace activeSpace = ActiveSpace.of(2, 2);
    return ElectronicStructureProblem.of(
        ElectronicProblemId.of("h2_problem"),
        ChemistryWorkflowServiceTest.hydrogen(),
        ChemistryTask.of(ChemistryTaskType.HAMILTONIAN_PREPARATION, BasisSet.STO_3G, activeSpace),
        ElectronicHamiltonian.of(
            activeSpace,
            0.7137539936876182,
            List.of(
                OneElectronIntegral.of(0, 0, -1.252477495),
                OneElectronIntegral.of(1, 1, -0.475934275)),
            List.of(TwoElectronIntegral.of(0, 0, 1, 1, 0.674493166))));
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
                Coordinate3D.of(0.0, 0.0, 0.7414, LengthUnit.ANGSTROM))),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule carbonAtom() {
    return Molecule.of(
        MoleculeId.of("carbon_atom"),
        "Carbon atom",
        List.of(
            Atom.of(
                AtomId.of("c0"),
                ElementSymbol.of("C"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM))),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(3));
  }

  private static Molecule lithiumAtom() {
    return Molecule.of(
        MoleculeId.of("lithium_atom"),
        "Lithium atom",
        List.of(
            Atom.of(
                AtomId.of("li0"),
                ElementSymbol.of("Li"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM))),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Molecule ironAtom() {
    return Molecule.of(
        MoleculeId.of("iron_atom"),
        "Iron atom",
        List.of(
            Atom.of(
                AtomId.of("fe0"),
                ElementSymbol.of("Fe"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM))),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static ReactionProductCandidate productCandidate(
      final Molecule molecule, final double probability) {
    return ReactionProductCandidate.of(
        ReactionSide.of(List.of(ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE))),
        probability,
        probability * 100.0,
        "workflow candidate",
        List.of(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.USER_RULE,
                "candidate supplied by workflow test",
                0.1)));
  }

  private static ReactionProductCandidate warningProductCandidate(
      final Molecule molecule, final double probability) {
    return ReactionProductCandidate.of(
        ReactionSide.of(List.of(ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE))),
        probability,
        probability * 100.0,
        "warning workflow candidate",
        List.of(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.WARNING,
                "condition set is intentionally incomplete for route reliability",
                0.4)));
  }

  private static Molecule ringMolecule(final String id, final String firstAtomSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            ChemistryWorkflowServiceTest.ringAtom("a0", firstAtomSymbol),
            ChemistryWorkflowServiceTest.ringAtom("a1", "C"),
            ChemistryWorkflowServiceTest.ringAtom("a2", "C"),
            ChemistryWorkflowServiceTest.ringAtom("a3", "N".equals(firstAtomSymbol) ? "N" : "C"),
            ChemistryWorkflowServiceTest.ringAtom("a4", "C"),
            ChemistryWorkflowServiceTest.ringAtom("a5", "C")),
        List.of(
            ChemistryWorkflowServiceTest.ringBond("a0", "a1"),
            ChemistryWorkflowServiceTest.ringBond("a1", "a2"),
            ChemistryWorkflowServiceTest.ringBond("a2", "a3"),
            ChemistryWorkflowServiceTest.ringBond("a3", "a4"),
            ChemistryWorkflowServiceTest.ringBond("a4", "a5"),
            ChemistryWorkflowServiceTest.ringBond("a5", "a0")),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule acyclicMolecule(final String id, final String firstAtomSymbol) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            ChemistryWorkflowServiceTest.ringAtom("a0", firstAtomSymbol),
            ChemistryWorkflowServiceTest.ringAtom("a1", "C"),
            ChemistryWorkflowServiceTest.ringAtom("a2", "C"),
            ChemistryWorkflowServiceTest.ringAtom("a3", "N".equals(firstAtomSymbol) ? "N" : "C"),
            ChemistryWorkflowServiceTest.ringAtom("a4", "C"),
            ChemistryWorkflowServiceTest.ringAtom("a5", "C")),
        List.of(
            ChemistryWorkflowServiceTest.ringBond("a0", "a1"),
            ChemistryWorkflowServiceTest.ringBond("a1", "a2"),
            ChemistryWorkflowServiceTest.ringBond("a2", "a3"),
            ChemistryWorkflowServiceTest.ringBond("a3", "a4"),
            ChemistryWorkflowServiceTest.ringBond("a4", "a5")),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
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

  private static Bond ringBond(final String first, final String second) {
    return Bond.of(AtomId.of(first), AtomId.of(second), BondType.AROMATIC);
  }

  private static BasisShell shell(final BasisShellAngularMomentum angularMomentum) {
    return BasisShell.of(
        angularMomentum,
        List.of(BasisPrimitive.of(0.25 + (double) angularMomentum.angularMomentum(), 1.0)));
  }

  private static BasisSet compactHcnBasis() {
    return BasisSet.structured(
        BasisSetName.of("workflow-compact-hcn-s"),
        BasisSetDefinition.of(
            List.of(
                BasisAtomDefinition.of(
                    ElementSymbol.of("H"),
                    List.of(ChemistryWorkflowServiceTest.shell(BasisShellAngularMomentum.S))),
                BasisAtomDefinition.of(
                    ElementSymbol.of("C"),
                    List.of(
                        ChemistryWorkflowServiceTest.shell(BasisShellAngularMomentum.S),
                        ChemistryWorkflowServiceTest.shell(BasisShellAngularMomentum.P))),
                BasisAtomDefinition.of(
                    ElementSymbol.of("N"),
                    List.of(
                        ChemistryWorkflowServiceTest.shell(BasisShellAngularMomentum.S),
                        ChemistryWorkflowServiceTest.shell(BasisShellAngularMomentum.P))))));
  }
}
