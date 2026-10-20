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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ChemistryCapabilityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityStatus;

public final class ChemistryCapabilityAnalyzerTest {

  @Test
  public void analyzerReportsSupportedNativeH2Pipeline() {
    final ChemistryCapabilityReport report =
        new ChemistryCapabilityAnalyzer()
            .analyze(ChemistryCapabilityAnalyzerTest.h2Request(BasisSet.STO_3G));

    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("subject.molecule").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("integrals.one-electron").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("integrals.full-eri").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("scf.hartree-fock").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("fermion.mapping").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("quantum.vqe-loop").status());
    assertTrue(report.requireItem("posthf.active-space").message().contains("VQE"));
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        report.requireItem("coverage.single_point_energy").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        report.requireItem("coverage.hamiltonian_preparation").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        report.requireItem("coverage.quantum_vqe_optimization").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.active_space_selection").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL, report.requireItem("geometry.gradient").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("workflow.periodic").status());
    assertTrue(report.partialCount() > 0);
  }

  @Test
  public void analyzerSupportsGOneElectronAndFullSparseGeri() {
    final ChemistryCapabilityReport report =
        new ChemistryCapabilityAnalyzer()
            .analyze(
                ChemistryCapabilityAnalyzerTest.carbonRequest(
                    ChemistryCapabilityAnalyzerTest.gShellBasis()));

    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("integrals.one-electron").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("integrals.full-eri").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        report.requireItem("coverage.hamiltonian_preparation").status());
    assertTrue(
        report
            .requireItem("integrals.full-eri")
            .message()
            .contains("Dense two-electron ERI tensor"));
    assertTrue(!report.hasBlockingUnsupportedItems());
  }

  @Test
  public void analyzerReportsDirectActiveSpaceHamiltonianWithoutDenseTensorClaim() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("direct_active_space_capability"),
            ChemistryCapabilityAnalyzerTest.carbon(),
            ElectronicStructureMethod.HARTREE_FOCK.withSpinTreatment(
                ElectronicStructureSpinTreatment.UNRESTRICTED),
            ChemistryCapabilityAnalyzerTest.gShellBasis(),
            ActiveSpace.of(6, 6),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.text("scf_integral_mode", "direct")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("integrals.full-eri").status());
    assertTrue(
        report.requireItem("integrals.full-eri").message().contains("active-space Hamiltonian"));
    assertTrue(report.requireItem("integrals.full-eri").message().contains("without"));
    assertTrue(
        !report
            .requireItem("integrals.full-eri")
            .message()
            .contains("Dense two-electron ERI tensor"));
  }

  @Test
  public void analyzerReportsHybridDftAsPartialInsteadOfPretendingFullBackendParity() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_b3lyp_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("B3LYP"),
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(ChemistryCapabilityStatus.SUPPORTED, report.requireItem("scf.dft").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.single_point_energy").status());
    assertTrue(report.requireItem("scf.dft").message().contains("coverage profile"));
  }

  @Test
  public void analyzerReportsPyscfDftSinglePointBackendAsExternalReferenceCoverage() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_b3lyp_pyscf_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.densityFunctional("B3LYP"),
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(ClassicalCalculationOption.text("single_point_backend", "pyscf")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.single_point_energy").status());
    assertTrue(report.requireItem("coverage.single_point_energy").message().contains("pyscf"));
    assertTrue(
        report
            .requireItem("coverage.single_point_energy")
            .message()
            .contains("external reference"));
  }

  @Test
  public void analyzerReportsElectronicFiniteDifferenceGeometryAsRequestedSupportedPath() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_geometry_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(
                ClassicalCalculationKind.GEOMETRY_GRADIENT,
                ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
                ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("geometry.gradient").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED, report.requireItem("geometry.optimization").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        report.requireItem("coverage.geometry_gradient").status());
    assertEquals(
        ChemistryCapabilityStatus.SUPPORTED,
        report.requireItem("coverage.geometry_optimization").status());
    assertTrue(report.requireItem("geometry.gradient").message().contains("single-point"));
  }

  @Test
  public void analyzerReportsPyScfAnalyticGradientBackendAsExternalReferenceCoverage() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_pyscf_gradient_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(ClassicalCalculationOption.text("geometry_gradient_backend", "pyscf")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.geometry_gradient").status());
    assertTrue(report.requireItem("coverage.geometry_gradient").message().contains("pyscf"));
    assertTrue(
        report.requireItem("coverage.geometry_gradient").message().contains("external reference"));
  }

  @Test
  public void analyzerReportsPyScfHessianFrequencyBackendAsExternalReferenceCoverage() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_pyscf_hessian_frequency_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(
                ClassicalCalculationKind.HESSIAN_ANALYSIS,
                ClassicalCalculationKind.FREQUENCY_ANALYSIS),
            List.of(ClassicalCalculationOption.text("vibrational_backend", "pyscf")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.hessian_analysis").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.frequency_analysis").status());
    assertTrue(report.requireItem("coverage.hessian_analysis").message().contains("pyscf"));
    assertTrue(report.requireItem("coverage.frequency_analysis").message().contains("pyscf"));
    assertTrue(
        report.requireItem("coverage.hessian_analysis").message().contains("external reference"));
    assertTrue(
        report.requireItem("coverage.frequency_analysis").message().contains("external reference"));
  }

  @Test
  public void analyzerReportsPyScfGeometryOptimizationBackendAsExternalReferenceCoverage() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_pyscf_geometry_optimization_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION),
            List.of(ClassicalCalculationOption.text("geometry_optimization_backend", "pyscf")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.geometry_optimization").status());
    assertTrue(report.requireItem("coverage.geometry_optimization").message().contains("pyscf"));
    assertTrue(
        report
            .requireItem("coverage.geometry_optimization")
            .message()
            .contains("external reference"));
  }

  @Test
  public void analyzerReportsPyScfExcitedStateBackendAsExternalReferenceCoverage() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_pyscf_excited_state_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.EXCITED_STATE_ANALYSIS),
            List.of(ClassicalCalculationOption.text("excited_state_backend", "pyscf")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.excited_state_analysis").status());
    assertTrue(report.requireItem("coverage.excited_state_analysis").message().contains("pyscf"));
    assertTrue(
        report
            .requireItem("coverage.excited_state_analysis")
            .message()
            .contains("external reference"));
  }

  @Test
  public void analyzerMapsAdvancedReactionMechanismCalculationsToPartialCoverage() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("advanced_mechanism_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(
                ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS,
                ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION,
                ClassicalCalculationKind.MICROKINETIC_MODELING,
                ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS),
            List.of(),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.competing_pathway_analysis").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.reaction_condition_optimization").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.microkinetic_modeling").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.transition_state_ensemble_analysis").status());
  }

  @Test
  public void analyzerReportsPyScfHamiltonianBackendAsExternalReferencePreparationRoute() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("pyscf_hamiltonian_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            ActiveSpace.of(2, 2),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.text("hamiltonian_backend", "pyscf")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.PARTIAL, report.requireItem("integrals.one-electron").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL, report.requireItem("integrals.full-eri").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.hamiltonian_preparation").status());
    assertTrue(
        report.requireItem("integrals.full-eri").message().contains("external reference route"));
    assertTrue(
        report
            .requireItem("integrals.full-eri")
            .message()
            .contains("not Java-native integral parity"));
  }

  @Test
  public void
      analyzerReportsPyScfNamedBasisHamiltonianBackendAsExternalReferencePreparationRoute() {
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("pyscf_named_basis_hamiltonian_capability"),
            ChemistryCapabilityAnalyzerTest.hydrogen(),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.of(BasisSetName.of("6-31G")),
            ActiveSpace.of(2, 4),
            List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
            List.of(ClassicalCalculationOption.text("hamiltonian_backend", "pyscf")),
            ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);

    final ChemistryCapabilityReport report = new ChemistryCapabilityAnalyzer().analyze(request);

    assertEquals(
        ChemistryCapabilityStatus.PARTIAL, report.requireItem("integrals.one-electron").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL, report.requireItem("integrals.full-eri").status());
    assertEquals(
        ChemistryCapabilityStatus.PARTIAL,
        report.requireItem("coverage.hamiltonian_preparation").status());
    assertTrue(report.requireItem("integrals.full-eri").message().contains("6-31G"));
    assertTrue(
        report.requireItem("integrals.full-eri").message().contains("external reference route"));
  }

  private static ClassicalCalculationRequest h2Request(final BasisSet basisSet) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("h2_capability"),
        ChemistryCapabilityAnalyzerTest.hydrogen(),
        ElectronicStructureMethod.HARTREE_FOCK,
        basisSet,
        ActiveSpace.of(2, 2),
        List.of(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION,
            ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION,
            ClassicalCalculationKind.ACTIVE_SPACE_SELECTION),
        List.of(),
        ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
  }

  private static ClassicalCalculationRequest carbonRequest(final BasisSet basisSet) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of("carbon_g_capability"),
        ChemistryCapabilityAnalyzerTest.carbon(),
        ElectronicStructureMethod.HARTREE_FOCK.withSpinTreatment(
            ElectronicStructureSpinTreatment.UNRESTRICTED),
        basisSet,
        ActiveSpace.of(6, 15),
        List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(),
        ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.EMPTY);
  }

  private static BasisSet gShellBasis() {
    return BasisSet.structured(
        BasisSetName.of("single-carbon-g"),
        BasisSetDefinition.of(
            List.of(
                BasisAtomDefinition.of(
                    ElementSymbol.of("C"),
                    List.of(
                        BasisShell.of(
                            BasisShellAngularMomentum.G, List.of(BasisPrimitive.of(0.7, 1.0))))))));
  }

  private static Molecule hydrogen() {
    return Molecule.of(
        MoleculeId.of("h2_capability_molecule"),
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

  private static Molecule carbon() {
    return Molecule.of(
        MoleculeId.of("carbon_g_capability_molecule"),
        "Carbon",
        List.of(
            Atom.of(
                AtomId.of("c0"),
                ElementSymbol.of("C"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM))),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(3));
  }
}
