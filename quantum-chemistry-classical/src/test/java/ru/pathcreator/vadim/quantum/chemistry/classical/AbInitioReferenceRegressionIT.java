/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

public final class AbInitioReferenceRegressionIT {

  private static final double ENERGY_TOLERANCE = 1.0E-5;

  @Test
  public void rhfMp2CcsdAndFciHydrogenMatchPyscfSto3gReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_rhf_reference",
        AbInitioReferenceRegressionIT.hydrogen(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -1.1166596958005601,
        AbInitioReferenceRegressionIT.ENERGY_TOLERANCE);
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_mp2_reference",
        AbInitioReferenceRegressionIT.hydrogen(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.PERTURBATION_THEORY,
            ElectronicStructureMethodName.of("MP2")),
        -1.1298409409176147,
        2.0E-5);
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_ccsd_reference",
        AbInitioReferenceRegressionIT.hydrogen(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.COUPLED_CLUSTER,
            ElectronicStructureMethodName.of("CCSD")),
        -1.1372652912071937,
        2.0E-5);
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_fci_reference",
        AbInitioReferenceRegressionIT.hydrogen(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION,
            ElectronicStructureMethodName.of("FCI")),
        -1.1372651246284575,
        2.0E-5);
  }

  @Test
  public void hydrogenRhfMatchesPyscfReferenceWithSelfContained631gBasisText() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_rhf_631g_text_reference",
        AbInitioReferenceRegressionIT.hydrogen(),
        ElectronicStructureMethod.HARTREE_FOCK,
        AbInitioReferenceRegressionIT.hydrogen631g(),
        -1.126726567372355,
        2.0E-5);
  }

  @Test
  public void hydrogenAt08RhfMatchesPyscfReferenceWithSelfContained631gBasisText() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_rhf_631g_08_text_reference",
        AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
        ElectronicStructureMethod.HARTREE_FOCK,
        AbInitioReferenceRegressionIT.hydrogen631g(),
        -1.1237129360075349,
        2.0E-5);
  }

  @Test
  public void hydrogenAt08Mp2MatchesPyscfReferenceWithSelfContained631gBasisText() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_mp2_631g_08_text_reference",
        AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.PERTURBATION_THEORY,
            ElectronicStructureMethodName.of("MP2")),
        AbInitioReferenceRegressionIT.hydrogen631g(),
        -1.1417478077275993,
        2.0E-5);
  }

  @Test
  public void hydrogenDftSinglePointCanUsePyscfBackendReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_b3lyp_pyscf_reference",
        AbInitioReferenceRegressionIT.hydrogen(),
        ElectronicStructureMethod.densityFunctional("B3LYP"),
        -1.16537879693694,
        2.0E-5,
        AbInitioReferenceRegressionIT.pyScfBackend());
  }

  @Test
  public void hydrogenSinglePointCanUsePyscfDdcosmoSolventReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_rhf_pyscf_ddcosmo_reference",
        AbInitioReferenceRegressionIT.hydrogen(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -1.116753127450925,
        2.0E-8,
        AbInitioReferenceRegressionIT.pyScfBackend(),
        ClassicalCalculationOption.text("solvent_model", "ddcosmo"),
        ClassicalCalculationOption.floating("solvent_dielectric", 78.3553));
  }

  @Test
  public void hydrogenCasscfActiveSpaceCanUsePyscfBackendReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_casscf_2e2o_pyscf_reference",
        AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
            ElectronicStructureMethodName.of("CASSCF")),
        ActiveSpace.of(2, 2),
        -1.1341476666770947,
        2.0E-8,
        AbInitioReferenceRegressionIT.pyScfBackend());
  }

  @Test
  public void hydrogenCasscfJavaNativeOrbitalOptimizationMatchesPyscfReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_casscf_2e2o_java_native_reference",
        AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
            ElectronicStructureMethodName.of("CASSCF")),
        ActiveSpace.of(2, 2),
        -1.1341476666770947,
        2.0E-5);
  }

  @Test
  public void hydrogen631gCasscfJavaNativeOptimizesActiveOrbitalsAgainstPyscfReference() {
    AbInitioReferenceRegressionIT.assertCasscfOrbitalOptimizationSinglePoint(
        "h2_631g_casscf_2e2o_java_native_reference",
        AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
            ElectronicStructureMethodName.of("CASSCF")),
        AbInitioReferenceRegressionIT.hydrogen631g(),
        ActiveSpace.of(2, 2),
        -1.1450978403687986,
        5.0E-4,
        ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
        ClassicalCalculationOption.integer("fci_beta_electrons", 1),
        ClassicalCalculationOption.integer("casscf_max_iterations", 48),
        ClassicalCalculationOption.floating("casscf_initial_step", 0.08),
        ClassicalCalculationOption.floating("casscf_minimum_step", 2.5E-6),
        ClassicalCalculationOption.floating("casscf_energy_tolerance", 1.0E-9));
  }

  @Test
  public void hydrogen631gCasciJavaNativeMatchesPyscfReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_631g_casci_2e2o_java_native_reference",
        AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
            ElectronicStructureMethodName.of("CASCI")),
        AbInitioReferenceRegressionIT.hydrogen631g(),
        ActiveSpace.of(2, 2),
        -1.1307998516386222,
        5.0E-4,
        ClassicalCalculationOption.integer("fci_alpha_electrons", 1),
        ClassicalCalculationOption.integer("fci_beta_electrons", 1));
  }

  @Test
  public void hydrogenCisdCanUsePyscfBackendReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_cisd_pyscf_reference",
        AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.CONFIGURATION_INTERACTION,
            ElectronicStructureMethodName.of("CISD")),
        -1.1341476666770955,
        2.0E-8,
        AbInitioReferenceRegressionIT.pyScfBackend());
  }

  @Test
  public void hydrogenCisdJavaNativeMatchesPyscfReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "h2_cisd_java_native_reference",
        AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.CONFIGURATION_INTERACTION,
            ElectronicStructureMethodName.of("CISD")),
        -1.1341476666770955,
        2.0E-5);
  }

  @Test
  public void lithiumHydrideCcsdJavaNativeMatchesPyscfReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "lih_ccsd_java_native_reference",
        AbInitioReferenceRegressionIT.lithiumHydride(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.COUPLED_CLUSTER,
            ElectronicStructureMethodName.of("CCSD")),
        -7.882313821664072,
        5.0E-5,
        ClassicalCalculationOption.integer("ccd_max_iterations", 320),
        ClassicalCalculationOption.floating("ccd_damping", 0.10),
        ClassicalCalculationOption.floating("ccd_energy_tolerance", 1.0E-9));
  }

  @Test
  public void hydrogenHamiltonianBackendAcceptsPyScfNamed631gWithoutJavaBasisDefinition() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_hamiltonian_pyscf_631g_reference"),
                    AbInitioReferenceRegressionIT.hydrogen(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.of(BasisSetName.of("6-31G")),
                    null,
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(ClassicalCalculationOption.text("hamiltonian_backend", "pyscf")),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(4.0, result.requireScalar("basis_orbital_count"), 0.0);
    assertEquals(2.0, result.requireScalar("electron_count"), 0.0);
    assertTrue(result.requireScalar("two_electron_integral_count") > 0.0);
  }

  @Test
  public void hydrogenElectronicFiniteDifferenceGradientMatchesPyscfFiniteDifferenceReference() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_gradient_pyscf_reference"),
            AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 5.0E-4)),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertEquals(0.139429260815227, AbInitioReferenceRegressionIT.gradientZ(result, "h1"), 2.0E-5);
    assertEquals(-0.139429260815227, AbInitioReferenceRegressionIT.gradientZ(result, "h0"), 2.0E-5);
  }

  @Test
  public void hydrogenAnalyticGradientCanUsePyscfBackendReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_analytic_gradient_pyscf_reference"),
                    AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
                    List.of(ClassicalCalculationOption.text("geometry_gradient_backend", "pyscf")),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(-1.1108503974765949, result.requireScalar("energy"), 1.0E-7);
    assertEquals(0.13942956, AbInitioReferenceRegressionIT.gradientZ(result, "h1"), 2.0E-5);
    assertEquals(-0.13942956, AbInitioReferenceRegressionIT.gradientZ(result, "h0"), 2.0E-5);
  }

  @Test
  public void hydrogenMp2AnalyticGradientCanUsePyscfBackendReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_mp2_analytic_gradient_pyscf_reference"),
                    AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
                    ElectronicStructureMethod.of(
                        ElectronicStructureMethodType.PERTURBATION_THEORY,
                        ElectronicStructureMethodName.of("MP2")),
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
                    List.of(ClassicalCalculationOption.text("geometry_gradient_backend", "pyscf")),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(
        0.11389298094896158, AbInitioReferenceRegressionIT.gradientZ(result, "h1"), 2.0E-5);
    assertEquals(
        -0.11389298094896158, AbInitioReferenceRegressionIT.gradientZ(result, "h0"), 2.0E-5);
  }

  @Test
  public void hydrogenAnalyticHessianAndFrequencyCanUsePyscfBackendReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_hessian_frequency_pyscf_reference"),
                    AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(
                        ClassicalCalculationKind.HESSIAN_ANALYSIS,
                        ClassicalCalculationKind.FREQUENCY_ANALYSIS),
                    List.of(ClassicalCalculationOption.text("vibrational_backend", "pyscf")),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(-1.1108503974765949, result.requireScalar("energy"), 1.0E-7);
    assertEquals(6.0, result.requireScalar("hessian_dimension"), 0.0);
    assertEquals(
        0.33776877199734906,
        AbInitioReferenceRegressionIT.hessianValue(result, 0, 2, 0, 2),
        2.0E-7);
    assertEquals(
        -0.3377687719973489,
        AbInitioReferenceRegressionIT.hessianValue(result, 0, 2, 1, 2),
        2.0E-7);
    assertEquals(4208.22310128, AbInitioReferenceRegressionIT.firstFrequency(result), 2.0E-4);
  }

  @Test
  public void hydrogenFiniteDifferenceHessianJavaNativeMatchesPyscfAnalyticReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_hessian_java_native_reference"),
                    AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.HESSIAN_ANALYSIS),
                    List.of(
                        ClassicalCalculationOption.text(
                            "geometry_energy_model", "electronic_structure_finite_difference"),
                        ClassicalCalculationOption.floating(
                            "geometry_finite_difference_step_angstrom", 1.0E-3)),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(-1.1108503974765949, result.requireScalar("energy"), 1.0E-7);
    assertEquals(
        1.20619789978349, AbInitioReferenceRegressionIT.hessianValue(result, 0, 2, 0, 2), 2.0E-3);
    assertEquals(
        -1.2062090591347108,
        AbInitioReferenceRegressionIT.hessianValue(result, 0, 2, 1, 2),
        2.0E-3);
  }

  @Test
  public void hydrogenGeometryOptimizationCanUsePyscfGeometricBackendReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_geometry_optimization_pyscf_reference"),
                    AbInitioReferenceRegressionIT.hydrogenAtDistance(1.2),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.GEOMETRY_OPTIMIZATION),
                    List.of(
                        ClassicalCalculationOption.text("geometry_optimization_backend", "pyscf"),
                        ClassicalCalculationOption.integer("geometry_max_iterations", 20)),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(-1.005106706568489, result.requireScalar("initial_energy"), 2.0E-8);
    assertEquals(-1.1175058851143305, result.requireScalar("final_energy"), 2.0E-8);
    assertEquals(
        0.71223118, AbInitioReferenceRegressionIT.optimizedDistance(result, "h0", "h1"), 2.0E-5);
  }

  @Test
  public void hydrogenExcitedStateAnalysisCanUsePyscfTdhfBackendReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_excited_state_pyscf_reference"),
                    AbInitioReferenceRegressionIT.hydrogen(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.EXCITED_STATE_ANALYSIS),
                    List.of(
                        ClassicalCalculationOption.text("excited_state_backend", "pyscf"),
                        ClassicalCalculationOption.integer("excited_state_count", 3)),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(1.0, result.requireScalar("state_count"), 0.0);
    assertEquals(
        25.272058677132627,
        AbInitioReferenceRegressionIT.firstExcitedStateValue(result, 2),
        2.0E-5);
    assertEquals(
        0.8851139, AbInitioReferenceRegressionIT.firstExcitedStateValue(result, 4), 2.0E-5);
  }

  @Test
  public void waterMolecularPropertyAnalysisCanUsePyscfBackendReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("water_property_pyscf_reference"),
                    AbInitioReferenceRegressionIT.water(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS),
                    List.of(ClassicalCalculationOption.text("property_backend", "pyscf")),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(-74.96306312972757, result.requireScalar("total_energy"), 2.0E-8);
    assertEquals(1.7255153075107506, result.requireScalar("dipole_norm"), 2.0E-7);
    assertEquals(0.9964096243694294, result.requireScalar("homo_lumo_gap"), 2.0E-7);
    assertEquals(-74.65592438960283, result.requireScalar("vertical_cation_energy"), 2.0E-8);
    assertEquals(-74.37636089533399, result.requireScalar("vertical_anion_energy"), 2.0E-8);
    assertEquals(0.3071387401247705, result.requireScalar("vertical_ionization_potential"), 2.0E-8);
    assertEquals(
        8.357670888641263, result.requireScalar("vertical_ionization_potential_ev"), 2.0E-7);
    assertEquals(-0.5867022343936128, result.requireScalar("vertical_electron_affinity"), 2.0E-8);
    assertEquals(
        -15.964981111468783, result.requireScalar("vertical_electron_affinity_ev"), 2.0E-7);
    assertEquals(-0.36550976, AbInitioReferenceRegressionIT.mullikenCharge(result, "o0"), 2.0E-6);
    assertEquals(0.18275488, AbInitioReferenceRegressionIT.mullikenCharge(result, "h0"), 2.0E-6);
    assertEquals(0.18275488, AbInitioReferenceRegressionIT.mullikenCharge(result, "h1"), 2.0E-6);
  }

  @Test
  public void hydrogenBasisSetConvergenceCanUsePyscfBenchmarkBackendReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_basis_convergence_pyscf_reference"),
                    AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.BASIS_SET_CONVERGENCE),
                    List.of(
                        ClassicalCalculationOption.text("basis_convergence_backend", "pyscf"),
                        ClassicalCalculationOption.text("basis_set_list", "sto-3g,3-21g")),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    final List<String> rows =
        AbInitioReferenceRegressionIT.table(result, "pyscf_basis_convergence_rows");
    assertEquals(2, rows.size());
    assertEquals(
        -1.1108503974765949, AbInitioReferenceRegressionIT.benchmarkEnergy(rows, "sto-3g"), 2.0E-8);
    assertEquals(
        -1.120212815205734, AbInitioReferenceRegressionIT.benchmarkEnergy(rows, "3-21g"), 2.0E-8);
  }

  @Test
  public void hydrogenMethodBenchmarkCanUsePyscfBenchmarkBackendReference() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_method_benchmark_pyscf_reference"),
                    AbInitioReferenceRegressionIT.hydrogenAtDistance(0.8),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.METHOD_BENCHMARK),
                    List.of(
                        ClassicalCalculationOption.text("method_benchmark_backend", "pyscf"),
                        ClassicalCalculationOption.text("method_list", "hf,mp2")),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    final List<String> rows =
        AbInitioReferenceRegressionIT.table(result, "pyscf_method_benchmark_rows");
    assertEquals(2, rows.size());
    assertEquals(
        -1.1108503974765949,
        AbInitioReferenceRegressionIT.benchmarkMethodEnergy(rows, "hf"),
        2.0E-8);
    assertEquals(
        -1.1254535339931433,
        AbInitioReferenceRegressionIT.benchmarkMethodEnergy(rows, "mp2"),
        2.0E-8);
  }

  @Test
  public void waterRhfMp2CcsdAndFciMatchPyscfSto3gReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "water_rhf_reference",
        AbInitioReferenceRegressionIT.water(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -74.96306312972757,
        2.0E-5);
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "water_mp2_reference",
        AbInitioReferenceRegressionIT.water(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.PERTURBATION_THEORY,
            ElectronicStructureMethodName.of("MP2")),
        -74.99862994794735,
        5.0E-5);
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "water_ccsd_reference",
        AbInitioReferenceRegressionIT.water(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.COUPLED_CLUSTER,
            ElectronicStructureMethodName.of("CCSD")),
        -75.01253063681308,
        2.0E-4,
        ClassicalCalculationOption.integer("ccd_max_iterations", 360),
        ClassicalCalculationOption.floating("ccd_damping", 0.08),
        ClassicalCalculationOption.floating("ccd_energy_tolerance", 1.0E-9));
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "water_fci_reference",
        AbInitioReferenceRegressionIT.water(),
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION,
            ElectronicStructureMethodName.of("FCI")),
        -75.01264711899165,
        2.0E-4,
        AbInitioReferenceRegressionIT.pyScfBackend(),
        ClassicalCalculationOption.longValue("pyscf_timeout_seconds", 600L),
        ClassicalCalculationOption.integer("fci_alpha_electrons", 5),
        ClassicalCalculationOption.integer("fci_beta_electrons", 5),
        ClassicalCalculationOption.integer("fci_max_determinants", 20000));
  }

  @Test
  public void waterRhfMatchesPyscfReferenceWithSelfContained631gBasisText() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "water_rhf_631g_text_reference",
        AbInitioReferenceRegressionIT.water(),
        ElectronicStructureMethod.HARTREE_FOCK,
        AbInitioReferenceRegressionIT.water631g(),
        -75.98394849810526,
        2.0E-4);
  }

  @Test
  public void pyridineRhfMatchesPyscfSto3gReferenceForHeterocycleScalePipeline() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "pyridine_rhf_reference",
        AbInitioReferenceRegressionIT.pyridine(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -243.62999494994588,
        5.0E-4);
  }

  @Test
  public void pyridylRadicalUhfMatchesPyscfSto3gReferenceForOpenShellHeterocycle() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "pyridyl_radical_uhf_reference",
        AbInitioReferenceRegressionIT.pyridylRadical(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -242.9742766378945,
        5.0E-4);
  }

  @Test
  public void oxygenAndNitrogenHeterocyclesMatchPyscfSto3gRhfReferences() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "furan_rhf_reference",
        AbInitioReferenceRegressionIT.furan(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -225.66664775774493,
        5.0E-4);
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "imidazole_rhf_reference",
        AbInitioReferenceRegressionIT.imidazole(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -221.9114315066218,
        5.0E-4);
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "pyrrole_rhf_reference",
        AbInitioReferenceRegressionIT.pyrrole(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -206.1157760904769,
        5.0E-4);
  }

  @Test
  public void oxygenHeterocycleRadicalMatchesPyscfSto3gUhfReference() {
    AbInitioReferenceRegressionIT.assertSinglePoint(
        "furan_radical_uhf_reference",
        AbInitioReferenceRegressionIT.furanRadical(),
        ElectronicStructureMethod.HARTREE_FOCK,
        -224.99977462226505,
        5.0E-4);
  }

  private static void assertSinglePoint(
      final String id,
      final Molecule molecule,
      final ElectronicStructureMethod method,
      final double expectedTotalEnergy,
      final double tolerance,
      final ClassicalCalculationOption... options) {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(id),
            molecule,
            method,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(options),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(expectedTotalEnergy, result.requireScalar("total_energy"), tolerance, id);
  }

  private static void assertCasscfOrbitalOptimizationSinglePoint(
      final String id,
      final Molecule molecule,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final ActiveSpace activeSpace,
      final double expectedTotalEnergy,
      final double tolerance,
      final ClassicalCalculationOption... options) {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(id),
            molecule,
            method,
            basisSet,
            activeSpace,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(options),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertTrue(result.requireScalar("evaluated_orbital_rotations") > 0.0);
    assertTrue(result.requireScalar("accepted_orbital_rotations") > 0.0);
    assertTrue(result.requireScalar("orbital_optimization_energy_improvement") >= 0.0);
    assertTrue(
        AbInitioReferenceRegressionIT.table(result, "orbital_optimization_history").size() > 1);
    assertEquals(expectedTotalEnergy, result.requireScalar("total_energy"), tolerance, id);
  }

  private static void assertSinglePoint(
      final String id,
      final Molecule molecule,
      final ElectronicStructureMethod method,
      final ActiveSpace activeSpace,
      final double expectedTotalEnergy,
      final double tolerance,
      final ClassicalCalculationOption... options) {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(id),
            molecule,
            method,
            BasisSet.STO_3G,
            activeSpace,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(options),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(expectedTotalEnergy, result.requireScalar("total_energy"), tolerance, id);
  }

  private static void assertSinglePoint(
      final String id,
      final Molecule molecule,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final ActiveSpace activeSpace,
      final double expectedTotalEnergy,
      final double tolerance,
      final ClassicalCalculationOption... options) {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(id),
            molecule,
            method,
            basisSet,
            activeSpace,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(options),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(expectedTotalEnergy, result.requireScalar("total_energy"), tolerance, id);
  }

  private static void assertSinglePoint(
      final String id,
      final Molecule molecule,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final double expectedTotalEnergy,
      final double tolerance,
      final ClassicalCalculationOption... options) {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of(id),
            molecule,
            method,
            basisSet,
            null,
            List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
            List.of(options),
            ChemistryMetadata.EMPTY);

    final ClassicalCalculationResult result = service.execute(request);

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED, result.status(), result.diagnostics().toString());
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(expectedTotalEnergy, result.requireScalar("total_energy"), tolerance, id);
  }

  private static BasisSet hydrogen631g() {
    return BasisSet.defined(
        BasisSetName.of("6-31G"),
        """
        H 0
        S 3 1.00
          18.7311370  0.03349460
           2.8253937  0.23472695
           0.6401217  0.81375733
        S 1 1.00
           0.1612778  1.00000000
        ****
        """);
  }

  private static BasisSet water631g() {
    return BasisSet.defined(
        BasisSetName.of("6-31G"),
        """
        O 0
        S 6 1.00
          5484.6717 0.0018311
          825.23495 0.0139501
          188.04696 0.0684451
          52.9645 0.2327143
          16.89757 0.470193
          5.7996353 0.3585209
        S 3 1.00
          15.539616 -0.1107775
          3.5999336 -0.1480263
          1.0137618 1.130767
        S 1 1.00
          0.2700058 1.0000000
        P 3 1.00
          15.539616 0.0708743
          3.5999336 0.3397528
          1.0137618 0.7271586
        P 1 1.00
          0.2700058 1.0000000
        ****
        H 0
        S 3 1.00
          18.7311370 0.03349460
          2.8253937 0.23472695
          0.6401217 0.81375733
        S 1 1.00
          0.1612778 1.00000000
        ****
        """);
  }

  private static Molecule hydrogen() {
    return AbInitioReferenceRegressionIT.hydrogenAtDistance(0.741848095192207);
  }

  private static Molecule hydrogenAtDistance(final double distanceAngstrom) {
    return Molecule.of(
        MoleculeId.of("reference.h2"),
        "Hydrogen reference",
        List.of(
            AbInitioReferenceRegressionIT.atom("h0", "H", 0.0, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("h1", "H", 0.0, 0.0, distanceAngstrom)),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule lithiumHydride() {
    return Molecule.of(
        MoleculeId.of("reference.lih"),
        "Lithium hydride reference",
        List.of(
            AbInitioReferenceRegressionIT.atom("li0", "Li", 0.0, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("h0", "H", 0.0, 0.0, 1.6)),
        List.of(Bond.of(AtomId.of("li0"), AtomId.of("h0"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule water() {
    return Molecule.of(
        MoleculeId.of("reference.water"),
        "Water reference",
        List.of(
            AbInitioReferenceRegressionIT.atom("o0", "O", 0.0, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("h0", "H", 0.0, -0.757, 0.587),
            AbInitioReferenceRegressionIT.atom("h1", "H", 0.0, 0.757, 0.587)),
        List.of(
            Bond.of(AtomId.of("o0"), AtomId.of("h0"), BondType.SINGLE),
            Bond.of(AtomId.of("o0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule pyridine() {
    return Molecule.of(
        MoleculeId.of("reference.pyridine"),
        "Pyridine reference",
        List.of(
            AbInitioReferenceRegressionIT.atom("n0", "N", 1.39, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("c1", "C", 0.695, 1.204, 0.0),
            AbInitioReferenceRegressionIT.atom("c2", "C", -0.695, 1.204, 0.0),
            AbInitioReferenceRegressionIT.atom("c3", "C", -1.39, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("c4", "C", -0.695, -1.204, 0.0),
            AbInitioReferenceRegressionIT.atom("c5", "C", 0.695, -1.204, 0.0),
            AbInitioReferenceRegressionIT.atom("h1", "H", 1.24, 2.148, 0.0),
            AbInitioReferenceRegressionIT.atom("h2", "H", -1.24, 2.148, 0.0),
            AbInitioReferenceRegressionIT.atom("h3", "H", -2.48, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("h4", "H", -1.24, -2.148, 0.0),
            AbInitioReferenceRegressionIT.atom("h5", "H", 1.24, -2.148, 0.0)),
        AbInitioReferenceRegressionIT.pyridineBonds(true),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule pyridylRadical() {
    return Molecule.of(
        MoleculeId.of("reference.pyridyl_radical"),
        "Pyridyl radical reference",
        List.of(
            AbInitioReferenceRegressionIT.radicalAtom("n0", "N", 1.39, 0.0, 0.0, 1),
            AbInitioReferenceRegressionIT.atom("c1", "C", 0.695, 1.204, 0.0),
            AbInitioReferenceRegressionIT.atom("c2", "C", -0.695, 1.204, 0.0),
            AbInitioReferenceRegressionIT.atom("c3", "C", -1.39, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("c4", "C", -0.695, -1.204, 0.0),
            AbInitioReferenceRegressionIT.atom("c5", "C", 0.695, -1.204, 0.0),
            AbInitioReferenceRegressionIT.atom("h1", "H", 1.24, 2.148, 0.0),
            AbInitioReferenceRegressionIT.atom("h2", "H", -1.24, 2.148, 0.0),
            AbInitioReferenceRegressionIT.atom("h3", "H", -2.48, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("h4", "H", -1.24, -2.148, 0.0)),
        AbInitioReferenceRegressionIT.pyridineBonds(false),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Molecule furan() {
    return Molecule.of(
        MoleculeId.of("reference.furan"),
        "Furan reference",
        List.of(
            AbInitioReferenceRegressionIT.atom("o0", "O", 1.16, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("c1", "C", 0.358, 1.102, 0.0),
            AbInitioReferenceRegressionIT.atom("c2", "C", -0.938, 0.681, 0.0),
            AbInitioReferenceRegressionIT.atom("c3", "C", -0.938, -0.681, 0.0),
            AbInitioReferenceRegressionIT.atom("c4", "C", 0.358, -1.102, 0.0),
            AbInitioReferenceRegressionIT.atom("h1", "H", 1.260, 1.951, 0.0),
            AbInitioReferenceRegressionIT.atom("h2", "H", -1.889, 1.173, 0.0),
            AbInitioReferenceRegressionIT.atom("h3", "H", -1.889, -1.173, 0.0),
            AbInitioReferenceRegressionIT.atom("h4", "H", 1.260, -1.951, 0.0)),
        AbInitioReferenceRegressionIT.furanBonds(true),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule furanRadical() {
    return Molecule.of(
        MoleculeId.of("reference.furan_radical"),
        "Furan radical reference",
        List.of(
            AbInitioReferenceRegressionIT.radicalAtom("o0", "O", 1.16, 0.0, 0.0, 1),
            AbInitioReferenceRegressionIT.atom("c1", "C", 0.358, 1.102, 0.0),
            AbInitioReferenceRegressionIT.atom("c2", "C", -0.938, 0.681, 0.0),
            AbInitioReferenceRegressionIT.atom("c3", "C", -0.938, -0.681, 0.0),
            AbInitioReferenceRegressionIT.atom("c4", "C", 0.358, -1.102, 0.0),
            AbInitioReferenceRegressionIT.atom("h1", "H", 1.260, 1.951, 0.0),
            AbInitioReferenceRegressionIT.atom("h2", "H", -1.889, 1.173, 0.0),
            AbInitioReferenceRegressionIT.atom("h3", "H", -1.889, -1.173, 0.0)),
        AbInitioReferenceRegressionIT.furanBonds(false),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Molecule imidazole() {
    return Molecule.of(
        MoleculeId.of("reference.imidazole"),
        "Imidazole reference",
        List.of(
            AbInitioReferenceRegressionIT.atom("n0", "N", 1.083, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("c1", "C", 0.335, 1.031, 0.0),
            AbInitioReferenceRegressionIT.atom("n2", "N", -0.876, 0.637, 0.0),
            AbInitioReferenceRegressionIT.atom("c3", "C", -0.876, -0.637, 0.0),
            AbInitioReferenceRegressionIT.atom("c4", "C", 0.335, -1.031, 0.0),
            AbInitioReferenceRegressionIT.atom("h1", "H", 0.708, 2.034, 0.0),
            AbInitioReferenceRegressionIT.atom("h2", "H", -1.762, 1.153, 0.0),
            AbInitioReferenceRegressionIT.atom("h3", "H", -1.762, -1.153, 0.0),
            AbInitioReferenceRegressionIT.atom("h4", "H", 0.708, -2.034, 0.0)),
        AbInitioReferenceRegressionIT.imidazoleBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule pyrrole() {
    return Molecule.of(
        MoleculeId.of("reference.pyrrole"),
        "Pyrrole reference",
        List.of(
            AbInitioReferenceRegressionIT.atom("n0", "N", 1.12, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("c1", "C", 0.346, 1.065, 0.0),
            AbInitioReferenceRegressionIT.atom("c2", "C", -0.906, 0.658, 0.0),
            AbInitioReferenceRegressionIT.atom("c3", "C", -0.906, -0.658, 0.0),
            AbInitioReferenceRegressionIT.atom("c4", "C", 0.346, -1.065, 0.0),
            AbInitioReferenceRegressionIT.atom("h0", "H", 2.13, 0.0, 0.0),
            AbInitioReferenceRegressionIT.atom("h1", "H", 1.196, 1.884, 0.0),
            AbInitioReferenceRegressionIT.atom("h2", "H", -1.824, 1.126, 0.0),
            AbInitioReferenceRegressionIT.atom("h3", "H", -1.824, -1.126, 0.0),
            AbInitioReferenceRegressionIT.atom("h4", "H", 1.196, -1.884, 0.0)),
        AbInitioReferenceRegressionIT.pyrroleBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static List<Bond> pyridineBonds(final boolean withLastHydrogen) {
    final List<Bond> ring =
        new ArrayList<Bond>(
            List.of(
                Bond.of(AtomId.of("n0"), AtomId.of("c1"), BondType.AROMATIC),
                Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.AROMATIC),
                Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.AROMATIC),
                Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
                Bond.of(AtomId.of("c4"), AtomId.of("c5"), BondType.AROMATIC),
                Bond.of(AtomId.of("c5"), AtomId.of("n0"), BondType.AROMATIC),
                Bond.of(AtomId.of("c1"), AtomId.of("h1"), BondType.SINGLE),
                Bond.of(AtomId.of("c2"), AtomId.of("h2"), BondType.SINGLE),
                Bond.of(AtomId.of("c3"), AtomId.of("h3"), BondType.SINGLE),
                Bond.of(AtomId.of("c4"), AtomId.of("h4"), BondType.SINGLE)));
    if (withLastHydrogen) {
      ring.add(Bond.of(AtomId.of("c5"), AtomId.of("h5"), BondType.SINGLE));
    }
    return List.copyOf(ring);
  }

  private static List<Bond> furanBonds(final boolean withFourthHydrogen) {
    final List<Bond> bonds =
        new ArrayList<Bond>(
            List.of(
                Bond.of(AtomId.of("o0"), AtomId.of("c1"), BondType.AROMATIC),
                Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.AROMATIC),
                Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.AROMATIC),
                Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
                Bond.of(AtomId.of("c4"), AtomId.of("o0"), BondType.AROMATIC),
                Bond.of(AtomId.of("c1"), AtomId.of("h1"), BondType.SINGLE),
                Bond.of(AtomId.of("c2"), AtomId.of("h2"), BondType.SINGLE),
                Bond.of(AtomId.of("c3"), AtomId.of("h3"), BondType.SINGLE)));
    if (withFourthHydrogen) {
      bonds.add(Bond.of(AtomId.of("c4"), AtomId.of("h4"), BondType.SINGLE));
    }
    return List.copyOf(bonds);
  }

  private static List<Bond> imidazoleBonds() {
    return List.of(
        Bond.of(AtomId.of("n0"), AtomId.of("c1"), BondType.AROMATIC),
        Bond.of(AtomId.of("c1"), AtomId.of("n2"), BondType.AROMATIC),
        Bond.of(AtomId.of("n2"), AtomId.of("c3"), BondType.AROMATIC),
        Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
        Bond.of(AtomId.of("c4"), AtomId.of("n0"), BondType.AROMATIC),
        Bond.of(AtomId.of("c1"), AtomId.of("h1"), BondType.SINGLE),
        Bond.of(AtomId.of("n2"), AtomId.of("h2"), BondType.SINGLE),
        Bond.of(AtomId.of("c3"), AtomId.of("h3"), BondType.SINGLE),
        Bond.of(AtomId.of("c4"), AtomId.of("h4"), BondType.SINGLE));
  }

  private static List<Bond> pyrroleBonds() {
    return List.of(
        Bond.of(AtomId.of("n0"), AtomId.of("c1"), BondType.AROMATIC),
        Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.AROMATIC),
        Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.AROMATIC),
        Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
        Bond.of(AtomId.of("c4"), AtomId.of("n0"), BondType.AROMATIC),
        Bond.of(AtomId.of("n0"), AtomId.of("h0"), BondType.SINGLE),
        Bond.of(AtomId.of("c1"), AtomId.of("h1"), BondType.SINGLE),
        Bond.of(AtomId.of("c2"), AtomId.of("h2"), BondType.SINGLE),
        Bond.of(AtomId.of("c3"), AtomId.of("h3"), BondType.SINGLE),
        Bond.of(AtomId.of("c4"), AtomId.of("h4"), BondType.SINGLE));
  }

  private static Atom atom(
      final String id, final String symbol, final double x, final double y, final double z) {
    return AbInitioReferenceRegressionIT.radicalAtom(id, symbol, x, y, z, 0);
  }

  private static Atom radicalAtom(
      final String id,
      final String symbol,
      final double x,
      final double y,
      final double z,
      final int unpairedElectronCount) {
    return Atom.of(
        AtomId.of(id),
        ElementSymbol.of(symbol),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM),
        FormalCharge.NEUTRAL,
        null,
        RadicalState.of(unpairedElectronCount),
        ChemistryMetadata.EMPTY);
  }

  private static ClassicalCalculationOption pyScfBackend() {
    return ClassicalCalculationOption.text("single_point_backend", "pyscf");
  }

  private static double gradientZ(final ClassicalCalculationResult result, final String atomId) {
    final List<String> rows = AbInitioReferenceRegressionIT.table(result, "gradient_rows");
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length < 9 || !atomId.equals(parts[0])) {
        continue;
      }
      return Double.parseDouble(parts[7]);
    }
    throw new AssertionError("Missing gradient row for " + atomId);
  }

  private static List<String> table(final ClassicalCalculationResult result, final String name) {
    for (int sectionIndex = 0; sectionIndex < result.sections().size(); ++sectionIndex) {
      final List<
              ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue>
          values = result.sections().get(sectionIndex).values();
      for (int valueIndex = 0; valueIndex < values.size(); ++valueIndex) {
        if (name.equals(values.get(valueIndex).name())) {
          return values.get(valueIndex).values();
        }
      }
    }
    throw new AssertionError("Missing result value " + name);
  }

  private static double hessianValue(
      final ClassicalCalculationResult result,
      final int firstAtomIndex,
      final int firstAxis,
      final int secondAtomIndex,
      final int secondAxis) {
    final List<String> rows = AbInitioReferenceRegressionIT.table(result, "hessian_rows");
    final int firstCoordinate = firstAtomIndex * 3 + firstAxis;
    final int secondCoordinate = secondAtomIndex * 3 + secondAxis;
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length >= 3
          && Integer.parseInt(parts[0]) == firstCoordinate
          && Integer.parseInt(parts[1]) == secondCoordinate) {
        return Double.parseDouble(parts[2]);
      }
      if (parts.length >= 5
          && Integer.parseInt(parts[0]) == firstAtomIndex
          && Integer.parseInt(parts[1]) == firstAxis
          && Integer.parseInt(parts[2]) == secondAtomIndex
          && Integer.parseInt(parts[3]) == secondAxis) {
        return Double.parseDouble(parts[4]);
      }
    }
    throw new AssertionError("Missing Hessian row.");
  }

  private static double benchmarkEnergy(final List<String> rows, final String basis) {
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length >= 6 && basis.equals(parts[2])) {
        return Double.parseDouble(parts[3]);
      }
    }
    throw new AssertionError("Missing benchmark basis row " + basis);
  }

  private static double benchmarkMethodEnergy(final List<String> rows, final String method) {
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length >= 6 && method.equals(parts[1])) {
        return Double.parseDouble(parts[3]);
      }
    }
    throw new AssertionError("Missing benchmark method row " + method);
  }

  private static double firstFrequency(final ClassicalCalculationResult result) {
    final List<String> frequencies = result.requireValues("frequencies");
    if (frequencies.isEmpty()) {
      throw new AssertionError("Missing frequency values.");
    }
    return Double.parseDouble(frequencies.get(0));
  }

  private static double optimizedDistance(
      final ClassicalCalculationResult result,
      final String firstAtomId,
      final String secondAtomId) {
    final double[] first = AbInitioReferenceRegressionIT.optimizedCoordinate(result, firstAtomId);
    final double[] second = AbInitioReferenceRegressionIT.optimizedCoordinate(result, secondAtomId);
    final double dx = first[0] - second[0];
    final double dy = first[1] - second[1];
    final double dz = first[2] - second[2];
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static double[] optimizedCoordinate(
      final ClassicalCalculationResult result, final String atomId) {
    final List<String> rows = AbInitioReferenceRegressionIT.table(result, "optimized_coordinates");
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length >= 5 && atomId.equals(parts[0])) {
        return new double[] {
          Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4])
        };
      }
    }
    throw new AssertionError("Missing optimized coordinate for " + atomId);
  }

  private static double firstExcitedStateValue(
      final ClassicalCalculationResult result, final int column) {
    final List<String> rows = AbInitioReferenceRegressionIT.table(result, "excited_state_rows");
    if (rows.isEmpty()) {
      throw new AssertionError("Missing excited state rows.");
    }
    final String[] parts = rows.get(0).split(",");
    if (parts.length <= column) {
      throw new AssertionError("Excited state row has too few columns.");
    }
    return Double.parseDouble(parts[column]);
  }

  private static double mullikenCharge(
      final ClassicalCalculationResult result, final String atomId) {
    final List<String> rows = AbInitioReferenceRegressionIT.table(result, "mulliken_charge_rows");
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length >= 4 && atomId.equals(parts[0])) {
        return Double.parseDouble(parts[3]);
      }
    }
    throw new AssertionError("Missing Mulliken charge row for " + atomId);
  }
}
