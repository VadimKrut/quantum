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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisAtomDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShell;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisShellAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
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
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ChemistryQuantumProgramPackageGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackageOptions;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;

public final class AbInitioWorkflowReferenceRegressionTest {

  @Test
  public void hydrogenHamiltonianJordanWignerMatchesOpenFermionPyscfReference() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                AbInitioWorkflowReferenceRegressionTest.hamiltonianRequest(
                    "workflow_h2_openfermion_reference",
                    AbInitioWorkflowReferenceRegressionTest.hydrogen(),
                    ActiveSpace.of(2, 2)));
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();
    final VerifiedElectronicHamiltonianMapping mapping =
        mapper.mapVerified(preparation.hamiltonian(), FermionToQubitMapping.JORDAN_WIGNER, 1.0E-8);

    assertTrue(mapping.verified(), mapping.spectralResult().maxDifference() + "");
    assertEquals(29, mapping.fermionOperator().termCount());
    assertEquals(15, mapping.qubitOperator().termCount());
    assertEquals(4, mapping.qubitOperator().requiredQubitCount());
    assertTrue(mapping.qubitOperator().hermitianWithinTolerance(1.0E-12));
    AbInitioWorkflowReferenceRegressionTest.assertPauliCoefficient(
        mapping.qubitOperator(), QubitPauliTerm.IDENTITY, -0.09943763924469742, 1.0E-6);
    AbInitioWorkflowReferenceRegressionTest.assertPauliCoefficient(
        mapping.qubitOperator(),
        QubitPauliTerm.of(
            new int[] {0},
            new ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis[] {
              ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis.Z
            }),
        0.17112898644416824,
        1.0E-6);
    AbInitioWorkflowReferenceRegressionTest.assertPauliCoefficient(
        mapping.qubitOperator(),
        QubitPauliTerm.of(
            new int[] {0, 1, 2, 3},
            new ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis[] {
              ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis.X,
              ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis.X,
              ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis.Y,
              ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis.Y
            }),
        -0.04532847460209732,
        1.0E-6);
  }

  @Test
  public void preparationPipelineReusesIdenticalNativeHamiltonianRequest() {
    final ElectronicStructurePreparationPipeline pipeline =
        new ElectronicStructurePreparationPipeline();
    final ClassicalCalculationRequest request =
        AbInitioWorkflowReferenceRegressionTest.hamiltonianRequest(
            "workflow_h2_cached_hamiltonian",
            AbInitioWorkflowReferenceRegressionTest.hydrogen(),
            ActiveSpace.of(2, 1));

    final ElectronicStructurePreparationResult first = pipeline.prepare(request);
    final ElectronicStructurePreparationResult second = pipeline.prepare(request);

    assertSame(first, second);
    assertEquals(1, pipeline.cachedPreparationCount());
    assertTrue(first.hamiltonian().integralCount() > 0);
  }

  @Test
  public void hydrogenAbInitioHamiltonianGeneratesCompleteNativeQuantumProgramPackage() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                AbInitioWorkflowReferenceRegressionTest.hamiltonianRequest(
                    "workflow_h2_quantum_package",
                    AbInitioWorkflowReferenceRegressionTest.hydrogen(),
                    ActiveSpace.of(2, 2)));
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "workflow_h2_quantum_package",
                preparation.hamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER);

    assertEquals(4, pack.spinOrbitalCount());
    assertEquals(2, pack.electronCount());
    assertTrue(pack.skippedArtifacts().isEmpty(), pack.skippedArtifacts().toString());
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.PAULI_EVOLUTION);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.UCCSD_ANSATZ);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ);
    assertTrue(pack.totalOperationCount() > 0L);
  }

  @Test
  public void
      nativeDiazirineActiveSpaceGeneratesVerifiedQuantumProgramPackageWithoutExternalBackend() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_diazirine_native_quantum_package"),
                    AbInitioWorkflowReferenceRegressionTest.diazirine(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    AbInitioWorkflowReferenceRegressionTest.compactHcnBasis(),
                    ActiveSpace.of(4, 4),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 0)),
                    ChemistryMetadata.EMPTY));
    final ElectronicHamiltonian hamiltonian = preparation.hamiltonian();
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "workflow_diazirine_native_quantum_package",
                hamiltonian,
                FermionToQubitMapping.JORDAN_WIGNER);

    assertEquals("COMPACT-HCN-S", preparation.basisSetName());
    assertEquals(4, hamiltonian.electronCount());
    assertEquals(4, hamiltonian.spatialOrbitalCount());
    assertEquals(8, hamiltonian.spinOrbitalCount());
    assertTrue(hamiltonian.integralCount() > 0);
    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final VerifiedElectronicHamiltonianMapping verified =
          mapper.mapVerified(hamiltonian, mapping, 1.0E-7);
      assertTrue(
          verified.verified(),
          verified.mapping() + " " + verified.spectralResult().maxDifference());
      assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-12));
    }
    assertTrue(pack.skippedArtifacts().isEmpty(), pack.skippedArtifacts().toString());
    assertTrue(pack.artifactCount(ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT) > 0);
    assertTrue(pack.artifactCount(ChemistryQuantumProgramKind.PAULI_EVOLUTION) > 0);
    assertTrue(pack.artifactCount(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION) > 0);
    assertTrue(pack.artifactCount(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION) > 0);
    assertTrue(pack.totalOperationCount() > 0L);
  }

  @Test
  public void nativeHydrogenHamiltonianPreparationCanUseCasscfOptimizedOrbitals() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_h2_casscf_optimized_hamiltonian"),
                    AbInitioWorkflowReferenceRegressionTest.hydrogen(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    ActiveSpace.of(2, 2),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(
                        ClassicalCalculationOption.text(
                            "hamiltonian_orbital_optimization", "casscf"),
                        ClassicalCalculationOption.integer("casscf_max_iterations", 4),
                        ClassicalCalculationOption.integer("casscf_max_orbital_pairs", 2)),
                    ChemistryMetadata.EMPTY));
    final VerifiedElectronicHamiltonianMapping verified =
        new ElectronicHamiltonianQubitMapper()
            .mapVerified(preparation.hamiltonian(), FermionToQubitMapping.JORDAN_WIGNER, 1.0E-8);
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "workflow_h2_casscf_optimized_quantum_package",
                preparation.hamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION,
                        ChemistryQuantumProgramKind.UCCSD_ANSATZ,
                        ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ),
                    1.0E-8));

    assertTrue(preparation.hasCasscfResult());
    assertTrue(preparation.hasLimitationReason());
    assertEquals(2, preparation.casscfResult().activeElectronCount());
    assertEquals(2, preparation.casscfResult().activeOrbitalCount());
    assertTrue(preparation.casscfResult().evaluatedRotationCount() >= 0);
    assertTrue(preparation.casscfResult().maxOrthonormalityError() < 1.0E-8);
    assertTrue(preparation.hamiltonian().integralCount() > 0);
    assertTrue(verified.verified(), verified.spectralResult().maxDifference() + "");
    assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-12));
    assertEquals(4, pack.artifactCount());
    assertTrue(pack.skippedArtifacts().isEmpty(), pack.skippedArtifacts().toString());
    assertTrue(pack.totalOperationCount() > 0L);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.PAULI_EVOLUTION);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION);
  }

  @Test
  public void nativeHydrogenHamiltonianPreparationCanUseKohnShamDftOrbitals() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_h2_dft_orbital_hamiltonian"),
                    AbInitioWorkflowReferenceRegressionTest.hydrogen(),
                    ElectronicStructureMethod.densityFunctional("PBE"),
                    BasisSet.STO_3G,
                    ActiveSpace.of(2, 2),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(),
                    ChemistryMetadata.EMPTY));
    final VerifiedElectronicHamiltonianMapping verified =
        new ElectronicHamiltonianQubitMapper()
            .mapNormalOrderedVerified(
                preparation.hamiltonian(), FermionToQubitMapping.JORDAN_WIGNER, 1.0E-8);

    assertTrue(preparation.hasKohnShamDftReference());
    assertTrue(preparation.hasLimitationReason());
    assertTrue(preparation.limitationReason().contains("Kohn-Sham"));
    assertTrue(preparation.kohnShamDftResult().converged());
    assertEquals(2.0, preparation.kohnShamDftResult().densityTrace(), 1.0E-7);
    assertEquals(2, preparation.hamiltonian().electronCount());
    assertEquals(2, preparation.hamiltonian().spatialOrbitalCount());
    assertTrue(preparation.hamiltonian().integralCount() > 0);
    assertTrue(verified.verified(), verified.spectralResult().maxDifference() + "");
    assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-12));
  }

  @Test
  public void nativeOpenShellKohnShamHamiltonianMapsAndGeneratesQuantumArtifacts() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_h_atom_unrestricted_pbe_quantum_package"),
                    AbInitioWorkflowReferenceRegressionTest.hydrogenAtomRadical(),
                    ElectronicStructureMethod.densityFunctional("PBE"),
                    BasisSet.STO_3G,
                    ActiveSpace.of(1, 1),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(
                        ClassicalCalculationOption.integer("active_space_first_orbital", 0),
                        ClassicalCalculationOption.text(
                            "uhf_hamiltonian_orbital_channel", "alpha")),
                    ChemistryMetadata.EMPTY));
    final VerifiedElectronicHamiltonianMapping verified =
        new ElectronicHamiltonianQubitMapper()
            .mapNormalOrderedVerified(
                preparation.hamiltonian(), FermionToQubitMapping.JORDAN_WIGNER, 1.0E-8);
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "workflow_h_atom_unrestricted_pbe_quantum_package",
                preparation.hamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION),
                    1.0E-8));

    assertTrue(preparation.hasKohnShamDftReference());
    assertTrue(preparation.kohnShamDftResult().unrestricted());
    assertEquals(1, preparation.kohnShamDftResult().alphaElectronCount());
    assertEquals(0, preparation.kohnShamDftResult().betaElectronCount());
    assertEquals(1.0, preparation.kohnShamDftResult().densityTrace(), 1.0E-7);
    assertEquals(1, preparation.hamiltonian().electronCount());
    assertEquals(1, preparation.hamiltonian().spatialOrbitalCount());
    assertEquals(2, preparation.hamiltonian().spinOrbitalCount());
    assertTrue(preparation.hamiltonian().integralCount() > 0);
    assertTrue(verified.verified(), verified.spectralResult().maxDifference() + "");
    assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-12));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION));
    assertTrue(pack.totalOperationCount() > 0L);
  }

  @Test
  public void nativePyridineActiveSpaceMapsEveryQubitEncodingWithoutExternalBackend() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_pyridine_native_active_space_mappings"),
                    AbInitioWorkflowReferenceRegressionTest.pyridine(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    AbInitioWorkflowReferenceRegressionTest.compactHcnBasis(),
                    ActiveSpace.of(4, 4),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 0)),
                    ChemistryMetadata.EMPTY));
    final ElectronicHamiltonian hamiltonian = preparation.hamiltonian();
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();

    assertEquals("COMPACT-HCN-S", preparation.basisSetName());
    assertEquals(4, hamiltonian.electronCount());
    assertEquals(4, hamiltonian.spatialOrbitalCount());
    assertEquals(8, hamiltonian.spinOrbitalCount());
    assertTrue(hamiltonian.integralCount() > 0);
    final List<VerifiedElectronicHamiltonianMapping> verifiedMappings =
        mapper.mapNormalOrderedVerified(
            hamiltonian, List.of(FermionToQubitMapping.values()), 1.0E-6);
    for (final VerifiedElectronicHamiltonianMapping verified : verifiedMappings) {
      assertTrue(
          verified.verified(),
          verified.mapping() + " " + verified.spectralResult().maxDifference());
      assertTrue(verified.fermionOperator().termCount() > 0);
      assertTrue(verified.qubitOperator().termCount() > 0);
      assertTrue(verified.qubitOperator().requiredQubitCount() <= hamiltonian.spinOrbitalCount());
      assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-12));
    }
  }

  @Test
  public void nativePyridineActiveSpaceGeneratesQuantumArtifactsWithoutExternalBackend() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_pyridine_native_quantum_package"),
                    AbInitioWorkflowReferenceRegressionTest.pyridine(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    AbInitioWorkflowReferenceRegressionTest.compactHcnBasis(),
                    ActiveSpace.of(4, 4),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 0)),
                    ChemistryMetadata.EMPTY));
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "workflow_pyridine_native_quantum_package",
                preparation.hamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION,
                        ChemistryQuantumProgramKind.UCCSD_ANSATZ),
                    1.0E-6));

    assertEquals(8, pack.spinOrbitalCount());
    assertEquals(4, pack.electronCount());
    assertTrue(pack.skippedArtifacts().isEmpty(), pack.skippedArtifacts().toString());
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.PAULI_EVOLUTION);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION);
    AbInitioWorkflowReferenceRegressionTest.assertPackageContains(
        pack, ChemistryQuantumProgramKind.UCCSD_ANSATZ);
    assertEquals(1, pack.artifactCount(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertEquals(1, pack.artifactCount(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertEquals(1, pack.artifactCount(ChemistryQuantumProgramKind.UCCSD_ANSATZ));
    assertTrue(pack.totalOperationCount() > 0L);
  }

  @Test
  public void nativePyridylRadicalActiveSpaceGeneratesVerifiedMappingsWithoutExternalBackend() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_pyridyl_radical_native_quantum_package"),
                    AbInitioWorkflowReferenceRegressionTest.pyridylRadical(),
                    ElectronicStructureMethod.HARTREE_FOCK.withSpinTreatment(
                        ru.pathcreator.vadim.quantum.chemistry.domain.method
                            .ElectronicStructureSpinTreatment.UNRESTRICTED),
                    AbInitioWorkflowReferenceRegressionTest.compactHcnBasis(),
                    ActiveSpace.of(5, 4),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 0)),
                    ChemistryMetadata.EMPTY));
    final ElectronicHamiltonian hamiltonian = preparation.hamiltonian();
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "workflow_pyridyl_radical_native_quantum_package",
                hamiltonian,
                FermionToQubitMapping.JORDAN_WIGNER,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION),
                    1.0E-6));

    assertTrue(preparation.hasUhfReference());
    assertEquals(5, hamiltonian.electronCount());
    assertEquals(4, hamiltonian.spatialOrbitalCount());
    assertEquals(8, hamiltonian.spinOrbitalCount());
    assertTrue(hamiltonian.integralCount() > 0);
    final List<VerifiedElectronicHamiltonianMapping> verifiedMappings =
        mapper.mapNormalOrderedVerified(
            hamiltonian, List.of(FermionToQubitMapping.values()), 1.0E-6);
    for (final VerifiedElectronicHamiltonianMapping verified : verifiedMappings) {
      assertTrue(
          verified.verified(),
          verified.mapping() + " " + verified.spectralResult().maxDifference());
      assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-12));
    }
    assertTrue(pack.skippedArtifacts().isEmpty(), pack.skippedArtifacts().toString());
    assertTrue(pack.artifactCount(ChemistryQuantumProgramKind.PAULI_EVOLUTION) > 0);
    assertTrue(pack.artifactCount(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION) > 0);
    assertTrue(pack.totalOperationCount() > 0L);
  }

  private static ClassicalCalculationRequest hamiltonianRequest(
      final String id, final Molecule molecule, final ActiveSpace activeSpace) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(id),
        molecule,
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        activeSpace,
        List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
        List.of(),
        ChemistryMetadata.EMPTY);
  }

  private static void assertPauliCoefficient(
      final QubitPauliOperator operator,
      final QubitPauliTerm expectedTerm,
      final double expectedReal,
      final double tolerance) {
    for (int i = 0; i < operator.termCount(); ++i) {
      if (!operator.term(i).equals(expectedTerm)) {
        continue;
      }
      assertEquals(
          expectedReal, operator.coefficient(i).real(), tolerance, expectedTerm.toString());
      assertEquals(0.0, operator.coefficient(i).imaginary(), tolerance, expectedTerm.toString());
      return;
    }
    throw new AssertionError("Missing Pauli term " + expectedTerm);
  }

  private static void assertPackageContains(
      final ChemistryQuantumProgramPackage pack, final ChemistryQuantumProgramKind kind) {
    assertTrue(
        pack.artifacts().stream().anyMatch(artifact -> artifact.kind() == kind), kind.name());
  }

  private static Molecule hydrogen() {
    return Molecule.of(
        MoleculeId.of("workflow.reference.h2"),
        "Hydrogen workflow reference",
        List.of(
            AbInitioWorkflowReferenceRegressionTest.atom("h0", "H", 0.0, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h1", "H", 0.0, 0.0, 0.741848095192207)),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogenAtomRadical() {
    return Molecule.of(
        MoleculeId.of("workflow.reference.h_atom_radical"),
        "Hydrogen atom radical workflow reference",
        List.of(AbInitioWorkflowReferenceRegressionTest.radicalAtom("h0", "H", 0.0, 0.0, 0.0, 1)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Molecule pyridine() {
    return Molecule.of(
        MoleculeId.of("workflow.reference.pyridine"),
        "Pyridine workflow reference",
        List.of(
            AbInitioWorkflowReferenceRegressionTest.atom("n0", "N", 1.39, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c1", "C", 0.695, 1.204, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c2", "C", -0.695, 1.204, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c3", "C", -1.39, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c4", "C", -0.695, -1.204, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c5", "C", 0.695, -1.204, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h1", "H", 1.24, 2.148, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h2", "H", -1.24, 2.148, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h3", "H", -2.48, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h4", "H", -1.24, -2.148, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h5", "H", 1.24, -2.148, 0.0)),
        AbInitioWorkflowReferenceRegressionTest.pyridineBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule diazirine() {
    return Molecule.of(
        MoleculeId.of("workflow.reference.diazirine"),
        "Diazirine native workflow reference",
        List.of(
            AbInitioWorkflowReferenceRegressionTest.atom("c0", "C", 0.0, -0.78, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("n1", "N", -0.67, 0.38, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("n2", "N", 0.67, 0.38, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h3", "H", -0.86, -1.42, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h4", "H", 0.86, -1.42, 0.0)),
        List.of(
            Bond.of(AtomId.of("c0"), AtomId.of("n1"), BondType.SINGLE),
            Bond.of(AtomId.of("c0"), AtomId.of("n2"), BondType.SINGLE),
            Bond.of(AtomId.of("n1"), AtomId.of("n2"), BondType.DOUBLE),
            Bond.of(AtomId.of("c0"), AtomId.of("h3"), BondType.SINGLE),
            Bond.of(AtomId.of("c0"), AtomId.of("h4"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  static Molecule pyridylRadical() {
    return Molecule.of(
        MoleculeId.of("workflow.reference.pyridyl_radical"),
        "Pyridyl radical native workflow reference",
        List.of(
            AbInitioWorkflowReferenceRegressionTest.radicalAtom("n0", "N", 1.39, 0.0, 0.0, 1),
            AbInitioWorkflowReferenceRegressionTest.atom("c1", "C", 0.695, 1.204, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c2", "C", -0.695, 1.204, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c3", "C", -1.39, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c4", "C", -0.695, -1.204, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("c5", "C", 0.695, -1.204, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h1", "H", 1.24, 2.148, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h2", "H", -1.24, 2.148, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h3", "H", -2.48, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionTest.atom("h4", "H", -1.24, -2.148, 0.0)),
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
            Bond.of(AtomId.of("c4"), AtomId.of("h4"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  static BasisSet compactHcnBasis() {
    return BasisSet.structured(
        BasisSetName.of("COMPACT-HCN-S"),
        BasisSetDefinition.of(
            List.of(
                BasisAtomDefinition.of(
                    ElementSymbol.of("H"),
                    List.of(AbInitioWorkflowReferenceRegressionTest.sShell(1.24))),
                BasisAtomDefinition.of(
                    ElementSymbol.of("C"),
                    List.of(
                        AbInitioWorkflowReferenceRegressionTest.sShell(5.0),
                        AbInitioWorkflowReferenceRegressionTest.sShell(1.5),
                        AbInitioWorkflowReferenceRegressionTest.sShell(0.5),
                        AbInitioWorkflowReferenceRegressionTest.sShell(0.15))),
                BasisAtomDefinition.of(
                    ElementSymbol.of("N"),
                    List.of(
                        AbInitioWorkflowReferenceRegressionTest.sShell(6.0),
                        AbInitioWorkflowReferenceRegressionTest.sShell(1.8),
                        AbInitioWorkflowReferenceRegressionTest.sShell(0.6),
                        AbInitioWorkflowReferenceRegressionTest.sShell(0.2))))));
  }

  private static BasisShell sShell(final double exponent) {
    return BasisShell.of(BasisShellAngularMomentum.S, List.of(BasisPrimitive.of(exponent, 1.0)));
  }

  private static List<Bond> pyridineBonds() {
    final List<Bond> bonds =
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
                Bond.of(AtomId.of("c4"), AtomId.of("h4"), BondType.SINGLE),
                Bond.of(AtomId.of("c5"), AtomId.of("h5"), BondType.SINGLE)));
    return List.copyOf(bonds);
  }

  private static Atom atom(
      final String id, final String symbol, final double x, final double y, final double z) {
    return Atom.of(
        AtomId.of(id),
        ElementSymbol.of(symbol),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM),
        FormalCharge.NEUTRAL,
        null,
        RadicalState.CLOSED_SHELL,
        ChemistryMetadata.EMPTY);
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
}
