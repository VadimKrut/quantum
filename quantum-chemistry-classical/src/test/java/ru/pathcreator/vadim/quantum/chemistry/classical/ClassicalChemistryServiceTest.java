/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalSubjectSize;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.backend.ClassicalBackendExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCoverageStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalCapability;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalPreflightStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalResourceEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInClassicalBackendProfiles;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.PlanningOnlyClassicalBackend;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
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

final class ClassicalChemistryServiceTest {

  @Test
  void supportsWaterEnergyOptimizationAndFrequencyPlan() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalChemistryServiceTest.waterRequest(
            List.of(
                ClassicalCalculationKind.SINGLE_POINT_ENERGY,
                ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
                ClassicalCalculationKind.FREQUENCY_ANALYSIS));

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationPlan plan =
        service.plan(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalResourceEstimate estimate = plan.totalResourceEstimate();

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
    Assertions.assertFalse(preflight.diagnostics().hasErrors());
    Assertions.assertFalse(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
    Assertions.assertEquals(3, plan.steps().size());
    Assertions.assertEquals(
        ClassicalCalculationKind.GEOMETRY_OPTIMIZATION, plan.steps().get(1).kind());
    Assertions.assertTrue(
        plan.steps().get(1).requiredCapabilities().contains(ClassicalCapability.GEOMETRY_GRADIENT));
    Assertions.assertEquals(3L, estimate.atomCount());
    Assertions.assertEquals(2L, estimate.bondCount());
    Assertions.assertEquals(10L, estimate.electronCount());
    Assertions.assertTrue(estimate.estimatedMemoryBytes() > 0L);
  }

  @Test
  void coverageReportsJavaNativePostHfGeometryGradientWithoutExternalBackend() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalBackendProfile profile =
        ClassicalBackendProfile.of(
            "classical.local.posthf_derivatives",
            "Java Native Post-HF Derivative Profile",
            "1.0",
            ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
            List.of(ChemistrySubjectKind.MOLECULE),
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(ElectronicStructureMethodType.COUPLED_CLUSTER),
            List.of("STO-3G"),
            List.of(
                ClassicalCapability.MOLECULE_INPUT,
                ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY,
                ClassicalCapability.GEOMETRY_GRADIENT,
                ClassicalCapability.LOCAL_EXECUTION),
            8L,
            16L,
            64L,
            4L,
            16L);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("ccsd_gradient_preflight"),
            ClassicalChemistryServiceTest.hydrogen(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCSD")),
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 1.0E-3)),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ClassicalPreflightResult preflight = service.preflight(request, profile);
    final ClassicalCalculationCoverageReport coverage = service.analyzeCoverage(request, profile);

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
    Assertions.assertFalse(preflight.diagnostics().hasErrors());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, coverage.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_geometry", coverage.items().get(0).model());
    Assertions.assertTrue(
        preflight.diagnostics().diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  @Test
  void rejectsUnsupportedBasisAndMethodWithStableDiagnostics() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("bad_method"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.CUSTOM,
                ElectronicStructureMethodName.of("unsupported-method-probe")),
            BasisSet.of(BasisSetName.of("cc-pVQZ")),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertTrue(preflight.diagnostics().diagnostics().size() >= 2);
    Assertions.assertEquals(
        ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
        preflight.diagnostics().diagnostics().get(0).code());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED,
        preflight.diagnostics().diagnostics().get(1).code());
  }

  @Test
  void classifiesEcpBasisTextWithStableDiagnosticCode() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final String ecpBasisText =
        "H 0\n"
            + "S 1 1.00\n"
            + "  3.42525091 1.0\n"
            + "****\n"
            + "O 0\n"
            + "S 1 1.00\n"
            + "  130.70932 1.0\n"
            + "****\n"
            + "ECP\n"
            + "O-ECP 0 1\n";
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_ecp_basis_text"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.defined(BasisSetName.of("water-ecp-text"), ecpBasisText),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.BASIS_ECP_UNSUPPORTED));
  }

  @Test
  void classifiesInvalidActiveSpaceWithStableDiagnosticCode() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_active_space_too_large"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 64),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(),
            null);

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED));
  }

  @Test
  void requiresCompleteGeometryForGeometryTasks() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule molecule =
        Molecule.of(
            MoleculeId.of("bare_h2"),
            "Bare H2",
            List.of(
                Atom.of(AtomId.of("h1"), ElementSymbol.of("H"), null),
                Atom.of(AtomId.of("h2"), ElementSymbol.of("H"), null)),
            List.of(Bond.of(AtomId.of("h1"), AtomId.of("h2"), BondType.SINGLE)),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("geometry_without_coordinates"),
            molecule,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION));

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
        preflight.diagnostics().diagnostics().get(0).code());
  }

  @Test
  void allowsFullSpaceHamiltonianPreparationWithoutActiveSpaceWindow() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("full_space_hamiltonian_without_active_space"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION));

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertNotEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertFalse(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.ACTIVE_SPACE_REQUIRED));
  }

  @Test
  void supportsActiveSpaceHamiltonianPreparationWhenProfileAllowsIt() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_active_space"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.bool("freeze_core", true)),
            null);

    final ClassicalCalculationPlan plan =
        service.plan(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, plan.preflightResult().status());
    Assertions.assertFalse(plan.preflightResult().diagnostics().hasErrors());
    Assertions.assertFalse(
        ClassicalChemistryServiceTest.hasDiagnostic(
            plan.preflightResult(), ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
    Assertions.assertEquals(2L, plan.totalResourceEstimate().activeElectronCount());
    Assertions.assertEquals(2L, plan.totalResourceEstimate().activeOrbitalCount());
    Assertions.assertTrue(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.ELECTRONIC_HAMILTONIAN));
  }

  @Test
  void supportsQuantumProgramPreparationWhenProfileAllowsIt() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_quantum_program_preparation"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
            List.of(ClassicalCalculationOption.bool("freeze_core", true)),
            null);

    final ClassicalCalculationPlan plan =
        service.plan(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, plan.preflightResult().status());
    Assertions.assertFalse(plan.preflightResult().diagnostics().hasErrors());
    Assertions.assertEquals(
        ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION, plan.steps().get(0).kind());
    Assertions.assertTrue(
        plan.steps().get(0).requiredCapabilities().contains(ClassicalCapability.ACTIVE_SPACE));
    Assertions.assertTrue(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.ELECTRONIC_HAMILTONIAN));
    Assertions.assertTrue(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.QUANTUM_PROGRAM_PREPARATION));
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, coverage.status());
    Assertions.assertEquals(
        ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION, coverage.items().get(0).kind());
  }

  @Test
  void supportsAutomaticActiveSpaceSelectionWithoutPreexistingActiveSpace() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_active_space_selection"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.ACTIVE_SPACE_SELECTION),
            List.of(
                ClassicalCalculationOption.integer("active_space_min_orbitals", 2),
                ClassicalCalculationOption.integer("active_space_max_orbitals", 4)),
            null);

    final ClassicalCalculationPlan plan =
        service.plan(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(
        ClassicalPreflightStatus.NEEDS_APPROXIMATION, plan.preflightResult().status());
    Assertions.assertFalse(plan.preflightResult().diagnostics().hasErrors());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            plan.preflightResult(), ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
    Assertions.assertEquals(
        ClassicalCalculationKind.ACTIVE_SPACE_SELECTION, plan.steps().get(0).kind());
    Assertions.assertTrue(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.ACTIVE_SPACE_SELECTION));
    Assertions.assertTrue(
        plan.steps().get(0).requiredCapabilities().contains(ClassicalCapability.ACTIVE_SPACE));
    Assertions.assertFalse(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.ELECTRONIC_HAMILTONIAN));
  }

  @Test
  void dftPreflightReportsSelfConsistentHybridGridKernelAsApproximateInSmallMoleculeBackend() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_dft_self_consistent_preflight"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.densityFunctional("B3LYP"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.NEEDS_APPROXIMATION, preflight.status());
    Assertions.assertFalse(preflight.diagnostics().hasErrors());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
  }

  @Test
  void coverageSeparatesExactRhfDftCcdAndCcsd() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest rhfRequest =
        ClassicalChemistryServiceTest.waterRequest(
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    final ClassicalCalculationRequest dftRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_dft_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.densityFunctional("B3LYP"),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    final ClassicalCalculationRequest ccsdRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_ccsd_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCSD")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    final ClassicalCalculationRequest ccdRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_ccd_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.of(
                ElectronicStructureMethodType.COUPLED_CLUSTER,
                ElectronicStructureMethodName.of("CCD")),
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationCoverageReport rhfCoverage =
        service.analyzeCoverage(
            rhfRequest, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationCoverageReport dftCoverage =
        service.analyzeCoverage(
            dftRequest, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationCoverageReport ccsdCoverage =
        service.analyzeCoverage(
            ccsdRequest, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationCoverageReport ccdCoverage =
        service.analyzeCoverage(
            ccdRequest, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, rhfCoverage.status());
    Assertions.assertTrue(rhfCoverage.diagnostics().empty());
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, dftCoverage.status());
    Assertions.assertFalse(dftCoverage.diagnostics().empty());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, ccdCoverage.status());
    Assertions.assertTrue(ccdCoverage.diagnostics().empty());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, ccsdCoverage.status());
    Assertions.assertTrue(ccsdCoverage.diagnostics().empty());
  }

  @Test
  void coverageReportsJavaNativeScfMolecularPropertyModel() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_native_property_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS));

    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, coverage.status());
    Assertions.assertEquals(
        "java_native_scf_density_moment_mulliken_frontier_property_analysis",
        coverage.items().get(0).model());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
        coverage.diagnostics().diagnostics().get(0).code());
  }

  @Test
  void coverageReportsElectronicRerankedReactionPredictionModel() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("coverage_h2_prediction_rerank"),
            "Coverage H2 prediction rerank",
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalChemistryServiceTest.hydrogen(), StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        ClassicalChemistryServiceTest.hydrogen(), StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("coverage_h2_prediction_rerank"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION),
            List.of(
                ClassicalCalculationOption.text("reaction_prediction_electronic_rerank", "true")),
            null);

    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.NEEDS_APPROXIMATION, preflight.status());
    Assertions.assertFalse(preflight.diagnostics().hasErrors());
    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, coverage.status());
    Assertions.assertEquals(
        "topology_condition_and_java_native_electronic_rerank", coverage.items().get(0).model());
    Assertions.assertTrue(
        coverage
            .diagnostics()
            .diagnostics()
            .get(0)
            .message()
            .contains("not an outcome lookup table"));
  }

  @Test
  void coverageAcceptsThirdPeriodMolecularPropertiesWithBuiltInSix31g() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_chloride_native_property_coverage"),
            ClassicalChemistryServiceTest.hydrogenChloride(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS));

    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, coverage.status());
    Assertions.assertEquals(
        "java_native_scf_density_moment_mulliken_frontier_property_analysis",
        coverage.items().get(0).model());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
        coverage.diagnostics().diagnostics().get(0).code());
  }

  @Test
  void coverageReportsJavaNativeExcitedStateModel() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_native_excited_state_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.EXCITED_STATE_ANALYSIS));

    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, coverage.status());
    Assertions.assertEquals("java_native_cis_tda_excited_state", coverage.items().get(0).model());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
        coverage.diagnostics().diagnostics().get(0).code());
  }

  @Test
  void coverageSeparatesBondedAndElectronicGeometryDerivativeModels() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest bondedGradientRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_bonded_gradient_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(ClassicalCalculationOption.text("geometry_energy_model", "bonded_force_field")),
            null);
    final ClassicalCalculationRequest electronicGradientRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_electronic_gradient_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference")),
            null);
    final ClassicalCalculationRequest electronicOptimizationRequest =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_electronic_optimization_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference")),
            null);

    final ClassicalCalculationCoverageReport bondedCoverage =
        service.analyzeCoverage(
            bondedGradientRequest, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationCoverageReport electronicGradientCoverage =
        service.analyzeCoverage(
            electronicGradientRequest, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationCoverageReport electronicOptimizationCoverage =
        service.analyzeCoverage(
            electronicOptimizationRequest, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalCoverageStatus.APPROXIMATE, bondedCoverage.status());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
        bondedCoverage.diagnostics().diagnostics().get(0).code());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, electronicGradientCoverage.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_geometry",
        electronicGradientCoverage.items().get(0).model());
    Assertions.assertTrue(electronicGradientCoverage.diagnostics().empty());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, electronicOptimizationCoverage.status());
    Assertions.assertEquals(
        "electronic_structure_finite_difference_geometry",
        electronicOptimizationCoverage.items().get(0).model());
    Assertions.assertTrue(electronicOptimizationCoverage.diagnostics().empty());
  }

  @Test
  void coverageReportsElectronicStructureHessianFrequencyAndThermodynamicsModels() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_electronic_hessian_frequency_coverage"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(
                ClassicalCalculationKind.HESSIAN_ANALYSIS,
                ClassicalCalculationKind.FREQUENCY_ANALYSIS,
                ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference")),
            null);

    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, coverage.status());
    Assertions.assertEquals(3, coverage.items().size());
    for (int i = 0; i < coverage.items().size(); ++i) {
      Assertions.assertEquals(
          "electronic_structure_finite_difference_hessian", coverage.items().get(i).model());
    }
    Assertions.assertTrue(coverage.diagnostics().empty());
  }

  @Test
  void coverageRoutesPostHartreeFockKindsToNativeExecutorModels() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_posthf_coverage"),
            ClassicalChemistryServiceTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(
                ClassicalCalculationKind.MP2_CORRELATION,
                ClassicalCalculationKind.CISD_CORRELATION,
                ClassicalCalculationKind.CCSD_CORRELATION,
                ClassicalCalculationKind.FCI_DIAGONALIZATION,
                ClassicalCalculationKind.POST_HF_CORRELATION),
            List.of(),
            null);

    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, coverage.status());
    Assertions.assertEquals(5, coverage.items().size());
    Assertions.assertEquals(
        "closed_shell_mp2_over_dense_mo_integrals", coverage.items().get(0).model());
    Assertions.assertEquals(
        "determinant_space_cisd_over_two_electron_hamiltonian", coverage.items().get(1).model());
    Assertions.assertEquals(
        "closed_shell_ccsd_over_dense_mo_integrals", coverage.items().get(2).model());
    Assertions.assertEquals(
        "determinant_space_fci_over_two_electron_hamiltonian", coverage.items().get(3).model());
    Assertions.assertEquals("java_native_post_hartree_fock_suite", coverage.items().get(4).model());
    Assertions.assertTrue(preflight.diagnostics().empty());
    Assertions.assertTrue(coverage.diagnostics().empty());
  }

  @Test
  void serviceExecutesJavaNativeQuantumVqeOptimizationWorkflow() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_native_vqe_optimization"),
            ClassicalChemistryServiceTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION),
            List.of(
                ClassicalCalculationOption.text("vqe_max_iterations", "3"),
                ClassicalCalculationOption.text("vqe_base_shots", "128"),
                ClassicalCalculationOption.text("vqe_max_shots", "256"),
                ClassicalCalculationOption.text("vqe_parameter_count", "2")),
            null);

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationPlan plan =
        service.plan(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, coverage.status());
    Assertions.assertEquals("Quantum VQE optimization", plan.steps().get(0).name());
    Assertions.assertEquals(2L, plan.totalResourceEstimate().activeElectronCount());
    Assertions.assertEquals(2L, plan.totalResourceEstimate().activeOrbitalCount());
    Assertions.assertEquals(
        ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalScalingClass
            .EXPONENTIAL,
        plan.totalResourceEstimate().scalingClass());
    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION));
    Assertions.assertEquals("true", result.requireFirstValue("mapping_verified"));
    Assertions.assertTrue(Double.isFinite(result.requireScalar("vqe_best_energy")));
    Assertions.assertTrue(result.requireScalar("vqe_iteration_count") >= 1.0);
    Assertions.assertTrue(
        result.requireScalar("vqe_evaluation_count")
            >= result.requireScalar("vqe_iteration_count"));
    Assertions.assertTrue(result.requireScalar("vqe_program_operation_count") > 0.0);
    Assertions.assertFalse(result.requireValues("vqe_iterations").isEmpty());
    Assertions.assertEquals(
        1, new ClassicalResultQualityAnalyzer().analyze(result).approximateCount());
    Assertions.assertEquals(0, new ClassicalResultQualityAnalyzer().analyze(result).unknownCount());
  }

  @Test
  void coverageReportsReactionThermodynamicsWithoutMoleculeCastFailure() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule hydrogen = ClassicalChemistryServiceTest.hydrogen();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("hydrogen_reaction_thermodynamics"),
            "Hydrogen reaction thermodynamics",
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("hydrogen_reaction_thermodynamics_coverage"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS),
            List.of(),
            null);

    final ClassicalCalculationCoverageReport coverage =
        service.analyzeCoverage(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalCoverageStatus.EXACT, coverage.status());
    Assertions.assertEquals(
        ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS, coverage.items().get(0).kind());
    Assertions.assertEquals(
        "stoichiometric_sum_of_java_native_single_point_energies", coverage.items().get(0).model());
  }

  @Test
  void preflightRejectsUnsupportedSolventAndPeriodicOptions() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_unsupported_environment_options"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.text("solvent_model", "water_pcm"),
                ClassicalCalculationOption.bool("periodic_boundary_conditions", true),
                ClassicalCalculationOption.text("relativistic_model", "scalar_zora")),
            null);

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    final var execution = service.execute(request);
    Assertions.assertEquals(
        ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus
            .FAILED,
        execution.status());
    Assertions.assertFalse(execution.hasSection(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    Assertions.assertTrue(
        execution.diagnostics().diagnostics().stream()
            .anyMatch(item -> item.code() == ClassicalDiagnosticCode.SOLVENT_MODEL_UNSUPPORTED));
    Assertions.assertTrue(
        execution.diagnostics().diagnostics().stream()
            .anyMatch(item -> item.code() == ClassicalDiagnosticCode.PERIODIC_MODEL_UNSUPPORTED));
    Assertions.assertTrue(
        execution.diagnostics().diagnostics().stream()
            .anyMatch(
                item -> item.code() == ClassicalDiagnosticCode.RELATIVISTIC_MODEL_UNSUPPORTED));
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.SOLVENT_MODEL_UNSUPPORTED));
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.PERIODIC_MODEL_UNSUPPORTED));
  }

  @Test
  void preflightPlansRelativisticCorrectionAsCapabilityBoundCalculation() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_relativistic_preflight"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.RELATIVISTIC_CORRECTION),
            List.of(ClassicalCalculationOption.text("relativistic_model", "mass_velocity_darwin")),
            null);

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final ClassicalCalculationPlan plan =
        service.plan(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.NEEDS_APPROXIMATION, preflight.status());
    Assertions.assertFalse(preflight.diagnostics().hasErrors());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.APPROXIMATION_REQUIRED));
    Assertions.assertEquals(
        ClassicalCalculationKind.RELATIVISTIC_CORRECTION, plan.steps().get(0).kind());
    Assertions.assertTrue(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.RELATIVISTIC_EFFECTS));
  }

  @Test
  void preflightRejectsRelativisticCorrectionWhenCapabilityIsMissing() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalBackendProfile profile =
        ClassicalBackendProfile.of(
            "relativistic.no_capability",
            "Relativistic Without Capability",
            "1.0",
            ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
            List.of(ChemistrySubjectKind.MOLECULE),
            List.of(ClassicalCalculationKind.RELATIVISTIC_CORRECTION),
            List.of(ElectronicStructureMethodType.HARTREE_FOCK),
            List.of("STO-3G"),
            List.of(ClassicalCapability.MOLECULE_INPUT),
            100L,
            200L,
            1000L,
            4L,
            1L);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_relativistic_missing_capability"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.RELATIVISTIC_CORRECTION),
            List.of(ClassicalCalculationOption.text("relativistic_model", "scalar_zora")),
            null);

    final ClassicalPreflightResult preflight = service.preflight(request, profile);

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.RELATIVISTIC_MODEL_UNSUPPORTED));
  }

  @Test
  void preflightPlansPeriodicBoundaryAnalysisWhenCapabilityIsPresent() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_periodic_preflight"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS),
            List.of(
                ClassicalCalculationOption.bool("periodic_boundary_conditions", true),
                ClassicalCalculationOption.text("periodic_model", "minimum_image")),
            null);

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.universalPlanning());
    final ClassicalCalculationPlan plan =
        service.plan(request, BuiltInClassicalBackendProfiles.universalPlanning());

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
    Assertions.assertFalse(preflight.diagnostics().hasErrors());
    Assertions.assertEquals(
        ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS, plan.steps().get(0).kind());
    Assertions.assertTrue(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.PERIODIC_BOUNDARY_CONDITIONS));
    Assertions.assertTrue(
        plan.steps()
            .get(0)
            .requiredCapabilities()
            .contains(ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY));
  }

  @Test
  void preflightRejectsPeriodicBoundaryAnalysisWhenCapabilityIsMissing() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("water_periodic_missing_capability"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS),
            List.of(ClassicalCalculationOption.bool("periodic_boundary_conditions", true)),
            null);

    final ClassicalPreflightResult preflight =
        service.preflight(request, profileWithoutPeriodicSupport());

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.CALCULATION_KIND_UNSUPPORTED));
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.PERIODIC_MODEL_UNSUPPORTED));
  }

  @Test
  void preflightRejectsGeometryOptimizationWhenGradientCapabilityIsMissing() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalBackendProfile profile =
        ClassicalBackendProfile.of(
            "geometry.no_gradient",
            "Geometry Without Gradient",
            "1.0",
            ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
            List.of(ChemistrySubjectKind.MOLECULE),
            List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION),
            List.of(ElectronicStructureMethodType.HARTREE_FOCK),
            List.of("STO-3G"),
            List.of(
                ClassicalCapability.MOLECULE_INPUT, ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY),
            100L,
            200L,
            1000L,
            4L,
            1L);
    final ClassicalCalculationRequest request =
        ClassicalChemistryServiceTest.waterRequest(
            List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION));

    final ClassicalPreflightResult preflight = service.preflight(request, profile);

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.GRADIENT_REQUIRED));
  }

  @Test
  void preflightRejectsStandaloneGeometryGradientWhenCapabilityIsMissing() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalBackendProfile profile =
        ClassicalBackendProfile.of(
            "gradient.no_gradient",
            "Gradient Without Gradient Capability",
            "1.0",
            ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
            List.of(ChemistrySubjectKind.MOLECULE),
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(ElectronicStructureMethodType.HARTREE_FOCK),
            List.of("STO-3G"),
            List.of(
                ClassicalCapability.MOLECULE_INPUT, ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY),
            100L,
            200L,
            1000L,
            4L,
            1L);
    final ClassicalCalculationRequest request =
        ClassicalChemistryServiceTest.waterRequest(
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT));

    final ClassicalPreflightResult preflight = service.preflight(request, profile);

    Assertions.assertEquals(ClassicalPreflightStatus.UNSUPPORTED, preflight.status());
    Assertions.assertTrue(
        ClassicalChemistryServiceTest.hasDiagnostic(
            preflight, ClassicalDiagnosticCode.GRADIENT_REQUIRED));
  }

  @Test
  void preflightAcceptsJavaNativeAdvancedReactionPlanningMatrix() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule hydrogen = ClassicalChemistryServiceTest.hydrogen();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("advanced_reaction_preflight"),
            "Advanced reaction preflight",
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_reaction_preflight"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(
                ClassicalCalculationKind.SELECTIVITY_ANALYSIS,
                ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS,
                ClassicalCalculationKind.REACTION_CONDITION_SCREENING,
                ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION,
                ClassicalCalculationKind.SOLVENT_SCREENING,
                ClassicalCalculationKind.CATALYST_SCREENING,
                ClassicalCalculationKind.CONCENTRATION_SCREENING,
                ClassicalCalculationKind.UNCERTAINTY_ANALYSIS,
                ClassicalCalculationKind.MECHANISM_ENUMERATION,
                ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS,
                ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION,
                ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS,
                ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS,
                ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS,
                ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS,
                ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS,
                ClassicalCalculationKind.MICROKINETIC_MODELING,
                ClassicalCalculationKind.RETROSYNTHESIS_PLANNING,
                ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS));

    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());

    Assertions.assertEquals(ClassicalPreflightStatus.NEEDS_APPROXIMATION, preflight.status());
    Assertions.assertFalse(preflight.diagnostics().hasErrors());
  }

  @Test
  void reactionPredictionResultCarriesConditionScenarioEvaluationMatrix() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule hydrogen = ClassicalChemistryServiceTest.hydrogen();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("condition_matrix_prediction_reaction"),
            "Condition matrix prediction reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("condition_matrix_prediction"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));

    final ClassicalCalculationResult result = service.execute(request);

    Assertions.assertTrue(result.successful() || result.partial());
    Assertions.assertTrue(result.hasSection(ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION));
    Assertions.assertTrue(result.hasValue("condition_scenario_evaluation_count"));
    Assertions.assertTrue(result.hasValue("best_condition_scenario_kind"));
    Assertions.assertTrue(result.hasValue("condition_scenario_evaluation"));
    Assertions.assertTrue(
        result
            .requireValue("condition_scenario_evaluation")
            .values()
            .get(0)
            .contains("baseline_direction"));
  }

  @Test
  void analyzesReactionSizeAcrossReactantsAndProducts() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final Molecule hydrogen = ClassicalChemistryServiceTest.hydrogen();
    final Molecule water = ClassicalChemistryServiceTest.water();
    final Reaction reaction =
        Reaction.of(
            ReactionId.of("probe_reaction"),
            "Probe reaction",
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.of(2)))),
            ReactionSide.of(List.of(ReactionParticipant.of(water, StoichiometricCoefficient.ONE))));
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("reaction_energy"),
            reaction,
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.REACTION_ENERGY));

    final ClassicalSubjectSize size = service.analyzeSubject(request);
    final ClassicalPreflightResult preflight =
        service.preflight(request, BuiltInClassicalBackendProfiles.universalPlanning());

    Assertions.assertEquals(7L, size.atomCount());
    Assertions.assertEquals(4L, size.bondCount());
    Assertions.assertEquals(14L, size.electronCount());
    Assertions.assertEquals(2L, size.participantCount());
    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
  }

  @Test
  void planningOnlyBackendReturnsPlannedResultWithoutPretendingToCompute() {
    final PlanningOnlyClassicalBackend backend = new PlanningOnlyClassicalBackend();
    final ClassicalCalculationRequest request =
        ClassicalChemistryServiceTest.waterRequest(
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationPlan plan = backend.plan(request);
    final ClassicalCalculationResult result = backend.execute(plan);

    Assertions.assertEquals(ClassicalCalculationStatus.PLANNED, result.status());
    Assertions.assertEquals(request.id(), result.calculationId());
    Assertions.assertEquals(
        ClassicalDiagnosticCode.PLAN_READY, result.diagnostics().diagnostics().get(0).code());
  }

  @Test
  void requestCollectionsAreImmutableAndRejectDuplicates() {
    final ClassicalCalculationRequest request =
        ClassicalChemistryServiceTest.waterRequest(
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));
    final Executable mutateRequestKinds =
        new Executable() {

          public final void execute() {
            request.calculationKinds().add(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS);
          }
        };
    final Executable createDuplicateKinds =
        new Executable() {

          public final void execute() {
            ClassicalChemistryServiceTest.waterRequest(
                List.of(
                    ClassicalCalculationKind.SINGLE_POINT_ENERGY,
                    ClassicalCalculationKind.SINGLE_POINT_ENERGY));
          }
        };

    Assertions.assertThrows(UnsupportedOperationException.class, mutateRequestKinds);
    Assertions.assertThrows(IllegalArgumentException.class, createDuplicateKinds);
  }

  @Test
  void profileCanRepresentNarrowDescriptorOnlyBackends() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalBackendProfile profile =
        ClassicalBackendProfile.of(
            "descriptor.only",
            "Descriptor Only",
            "1.0",
            ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
            List.of(ChemistrySubjectKind.MOLECULE),
            List.of(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS),
            List.of(ElectronicStructureMethodType.HARTREE_FOCK),
            List.of(),
            List.of(ClassicalCapability.MOLECULE_INPUT, ClassicalCapability.DESCRIPTOR_ANALYSIS),
            100L,
            200L,
            1000L,
            4L,
            1L);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("descriptor_probe"),
            ClassicalChemistryServiceTest.water(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            List.of(ClassicalCalculationKind.DESCRIPTOR_ANALYSIS));

    final ClassicalPreflightResult preflight = service.preflight(request, profile);

    Assertions.assertEquals(ClassicalPreflightStatus.SUPPORTED, preflight.status());
  }

  @Test
  void coverageRoutesLargeOpenShellHartreeFockThroughDirectUhfWhenPackedTensorIsTooLarge() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalBackendProfile profile =
        ClassicalBackendProfile.of(
            "classical.local.large_direct_uhf",
            "Large Direct UHF",
            "1.0",
            ClassicalBackendExecutionMode.LOCAL_IN_PROCESS,
            List.of(ChemistrySubjectKind.MOLECULE),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(ElectronicStructureMethodType.HARTREE_FOCK),
            List.of("6-31G"),
            List.of(
                ClassicalCapability.MOLECULE_INPUT,
                ClassicalCapability.THREE_DIMENSIONAL_GEOMETRY,
                ClassicalCapability.LOCAL_EXECUTION),
            64L,
            128L,
            256L,
            4L,
            128L);
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("large_open_shell_direct_uhf_coverage"),
            ClassicalChemistryServiceTest.largeOpenShellCarbonNitrogenRadical(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.SIX_31G,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY));

    final ClassicalCalculationCoverageReport coverage = service.analyzeCoverage(request, profile);

    Assertions.assertEquals(
        ClassicalCoverageStatus.EXACT, coverage.status(), coverage.items().toString());
    Assertions.assertEquals(
        "direct_gaussian_unrestricted_hartree_fock", coverage.items().get(0).model());
    Assertions.assertTrue(
        coverage.items().get(0).diagnostics().stream()
            .noneMatch(
                diagnostic ->
                    diagnostic.message().toLowerCase(java.util.Locale.ROOT).contains("pyscf")));
  }

  private static ClassicalCalculationRequest waterRequest(
      final List<ClassicalCalculationKind> kinds) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("water_calculation"),
        ClassicalChemistryServiceTest.water(),
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        kinds);
  }

  private static boolean hasDiagnostic(
      final ClassicalPreflightResult preflight, final ClassicalDiagnosticCode code) {
    return preflight.diagnostics().diagnostics().stream()
        .anyMatch(diagnostic -> diagnostic.code() == code);
  }

  private static Molecule water() {
    return Molecule.of(
        MoleculeId.of("water"),
        "Water",
        List.of(
            Atom.of(
                AtomId.of("o"),
                ElementSymbol.of("O"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("h1"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.95, 0.0, 0.0, LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("h2"),
                ElementSymbol.of("H"),
                Coordinate3D.of(-0.24, 0.92, 0.0, LengthUnit.ANGSTROM))),
        List.of(
            Bond.of(AtomId.of("o"), AtomId.of("h1"), BondType.SINGLE),
            Bond.of(AtomId.of("o"), AtomId.of("h2"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogen() {
    return Molecule.of(
        MoleculeId.of("hydrogen"),
        "Hydrogen",
        List.of(
            Atom.of(
                AtomId.of("h1"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("h2"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.74, 0.0, 0.0, LengthUnit.ANGSTROM))),
        List.of(Bond.of(AtomId.of("h1"), AtomId.of("h2"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogenChloride() {
    return Molecule.of(
        MoleculeId.of("hydrogen_chloride"),
        "Hydrogen chloride",
        List.of(
            Atom.of(
                AtomId.of("h0"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("cl0"),
                ElementSymbol.of("Cl"),
                Coordinate3D.of(0.0, 0.0, 1.27455, LengthUnit.ANGSTROM))),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("cl0"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule largeOpenShellCarbonNitrogenRadical() {
    return Molecule.of(
        MoleculeId.of("large_open_shell_carbon_nitrogen_radical"),
        "Large open-shell carbon-nitrogen radical",
        List.of(
            ClassicalChemistryServiceTest.ringAtom("a0", "N", 0, 1),
            ClassicalChemistryServiceTest.ringAtom("a1", "C", 1, 1),
            ClassicalChemistryServiceTest.ringAtom("a2", "C", 2, 1),
            ClassicalChemistryServiceTest.ringAtom("a3", "N", 3, 1),
            ClassicalChemistryServiceTest.ringAtom("a4", "C", 4, 1),
            ClassicalChemistryServiceTest.ringAtom("a5", "C", 5, 1),
            ClassicalChemistryServiceTest.ringAtom("a6", "C", 6, 1),
            ClassicalChemistryServiceTest.ringAtom("a7", "N", 7, 1),
            ClassicalChemistryServiceTest.ringAtom("a8", "C", 8, 1),
            ClassicalChemistryServiceTest.ringAtom("a9", "C", 9, 1),
            ClassicalChemistryServiceTest.ringAtom("a10", "C", 10, 1),
            ClassicalChemistryServiceTest.ringAtom("a11", "N", 11, 1),
            ClassicalChemistryServiceTest.ringAtom("h0", "H", 0, 2),
            ClassicalChemistryServiceTest.ringAtom("h1", "H", 1, 2),
            ClassicalChemistryServiceTest.ringAtom("h2", "H", 2, 2),
            ClassicalChemistryServiceTest.ringAtom("h3", "H", 3, 2),
            ClassicalChemistryServiceTest.ringAtom("h4", "H", 4, 2),
            ClassicalChemistryServiceTest.ringAtom("h5", "H", 5, 2),
            ClassicalChemistryServiceTest.ringAtom("h6", "H", 6, 2),
            ClassicalChemistryServiceTest.ringAtom("h7", "H", 7, 2)),
        List.of(
            Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.AROMATIC),
            Bond.of(AtomId.of("a1"), AtomId.of("a2"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a3"), BondType.AROMATIC),
            Bond.of(AtomId.of("a3"), AtomId.of("a4"), BondType.AROMATIC),
            Bond.of(AtomId.of("a4"), AtomId.of("a5"), BondType.AROMATIC),
            Bond.of(AtomId.of("a5"), AtomId.of("a0"), BondType.AROMATIC),
            Bond.of(AtomId.of("a6"), AtomId.of("a7"), BondType.AROMATIC),
            Bond.of(AtomId.of("a7"), AtomId.of("a8"), BondType.AROMATIC),
            Bond.of(AtomId.of("a8"), AtomId.of("a9"), BondType.AROMATIC),
            Bond.of(AtomId.of("a9"), AtomId.of("a10"), BondType.AROMATIC),
            Bond.of(AtomId.of("a10"), AtomId.of("a11"), BondType.AROMATIC),
            Bond.of(AtomId.of("a11"), AtomId.of("a6"), BondType.AROMATIC),
            Bond.of(AtomId.of("a2"), AtomId.of("a6"), BondType.SINGLE),
            Bond.of(AtomId.of("a5"), AtomId.of("a9"), BondType.SINGLE),
            Bond.of(AtomId.of("a0"), AtomId.of("h0"), BondType.SINGLE),
            Bond.of(AtomId.of("a1"), AtomId.of("h1"), BondType.SINGLE),
            Bond.of(AtomId.of("a3"), AtomId.of("h2"), BondType.SINGLE),
            Bond.of(AtomId.of("a4"), AtomId.of("h3"), BondType.SINGLE),
            Bond.of(AtomId.of("a7"), AtomId.of("h4"), BondType.SINGLE),
            Bond.of(AtomId.of("a8"), AtomId.of("h5"), BondType.SINGLE),
            Bond.of(AtomId.of("a10"), AtomId.of("h6"), BondType.SINGLE),
            Bond.of(AtomId.of("a11"), AtomId.of("h7"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Atom ringAtom(
      final String id, final String element, final int position, final int radius) {
    final double angle = (Math.PI / 6.0) * (double) position;
    final double radialScale = 1.35 * (double) radius;
    return Atom.of(
        AtomId.of(id),
        ElementSymbol.of(element),
        Coordinate3D.of(
            radialScale * Math.cos(angle),
            radialScale * Math.sin(angle),
            0.0,
            LengthUnit.ANGSTROM));
  }

  private static ClassicalBackendProfile profileWithoutPeriodicSupport() {
    final var source = BuiltInClassicalBackendProfiles.smallMoleculeAbInitio();
    return ClassicalBackendProfile.of(
        "test.without.periodic",
        source.displayName(),
        source.version(),
        source.executionMode(),
        source.supportedSubjectKinds(),
        source.supportedCalculationKinds().stream()
            .filter(kind -> kind != ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS)
            .toList(),
        source.supportedMethodTypes(),
        source.supportedBasisSets(),
        source.capabilities().stream()
            .filter(capability -> capability != ClassicalCapability.PERIODIC_BOUNDARY_CONDITIONS)
            .toList(),
        source.maxAtoms(),
        source.maxBonds(),
        source.maxElectrons(),
        source.maxParticipants(),
        source.maxActiveOrbitals());
  }
}
