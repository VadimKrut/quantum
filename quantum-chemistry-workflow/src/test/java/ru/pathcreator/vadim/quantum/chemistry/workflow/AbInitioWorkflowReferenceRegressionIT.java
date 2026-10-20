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

import java.util.ArrayList;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
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
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;

public final class AbInitioWorkflowReferenceRegressionIT {

  @Test
  public void hydrogenHamiltonianJordanWignerMatchesOpenFermionPyscfReference() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                AbInitioWorkflowReferenceRegressionIT.hamiltonianRequest(
                    "workflow_h2_openfermion_reference",
                    AbInitioWorkflowReferenceRegressionIT.hydrogen(),
                    ActiveSpace.of(2, 2)));
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();
    final VerifiedElectronicHamiltonianMapping mapping =
        mapper.mapVerified(preparation.hamiltonian(), FermionToQubitMapping.JORDAN_WIGNER, 1.0E-8);

    assertTrue(mapping.verified(), mapping.spectralResult().maxDifference() + "");
    assertEquals(29, mapping.fermionOperator().termCount());
    assertEquals(15, mapping.qubitOperator().termCount());
    assertEquals(4, mapping.qubitOperator().requiredQubitCount());
    assertTrue(mapping.qubitOperator().hermitianWithinTolerance(1.0E-12));
    AbInitioWorkflowReferenceRegressionIT.assertPauliCoefficient(
        mapping.qubitOperator(), QubitPauliTerm.IDENTITY, -0.09943763924469742, 1.0E-6);
    AbInitioWorkflowReferenceRegressionIT.assertPauliCoefficient(
        mapping.qubitOperator(),
        QubitPauliTerm.of(
            new int[] {0},
            new ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis[] {
              ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis.Z
            }),
        0.17112898644416824,
        1.0E-6);
    AbInitioWorkflowReferenceRegressionIT.assertPauliCoefficient(
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
  public void pyridineActiveSpaceProducesVerifiedQubitHamiltoniansForAllNativeMappings() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_pyridine_active_space"),
                    AbInitioWorkflowReferenceRegressionIT.pyridine(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    ActiveSpace.of(4, 4),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(ClassicalCalculationOption.integer("active_space_first_orbital", 19)),
                    ChemistryMetadata.EMPTY));
    final ElectronicHamiltonian hamiltonian = preparation.hamiltonian();
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();

    assertEquals(4, hamiltonian.electronCount());
    assertEquals(4, hamiltonian.spatialOrbitalCount());
    assertEquals(8, hamiltonian.spinOrbitalCount());
    assertTrue(hamiltonian.integralCount() > 0);
    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final VerifiedElectronicHamiltonianMapping verified =
          mapper.mapVerified(hamiltonian, mapping, 1.0E-5);
      assertTrue(verified.verified(), mapping + " " + verified.spectralResult().maxDifference());
      assertTrue(verified.fermionOperator().termCount() > 0);
      assertTrue(verified.qubitOperator().termCount() > 0);
      assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-12));
    }
  }

  @Test
  public void hydrogenPyScfNamed631gHamiltonianMapsThroughAllNativeMappings() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("workflow_h2_631g_backend_mapping"),
                    AbInitioWorkflowReferenceRegressionIT.hydrogen(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.of(BasisSetName.of("6-31G")),
                    ActiveSpace.of(2, 4),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(ClassicalCalculationOption.text("hamiltonian_backend", "pyscf")),
                    ChemistryMetadata.EMPTY));
    final ElectronicHamiltonian hamiltonian = preparation.hamiltonian();
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();

    assertEquals("6-31G", preparation.basisSetName());
    assertEquals(4, preparation.basisOrbitalCount());
    assertEquals(2, hamiltonian.electronCount());
    assertEquals(4, hamiltonian.spatialOrbitalCount());
    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final VerifiedElectronicHamiltonianMapping verified =
          mapper.mapVerified(hamiltonian, mapping, 1.0E-8);
      assertTrue(verified.verified(), mapping + " " + verified.spectralResult().maxDifference());
      assertTrue(verified.fermionOperator().termCount() > 0);
      assertTrue(verified.qubitOperator().termCount() > 0);
    }
  }

  @Test
  public void hydrogenAbInitioHamiltonianGeneratesCompleteNativeQuantumProgramPackage() {
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                AbInitioWorkflowReferenceRegressionIT.hamiltonianRequest(
                    "workflow_h2_quantum_package",
                    AbInitioWorkflowReferenceRegressionIT.hydrogen(),
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
    AbInitioWorkflowReferenceRegressionIT.assertPackageContains(
        pack, ChemistryQuantumProgramKind.PAULI_EVOLUTION);
    AbInitioWorkflowReferenceRegressionIT.assertPackageContains(
        pack, ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION);
    AbInitioWorkflowReferenceRegressionIT.assertPackageContains(
        pack, ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION);
    AbInitioWorkflowReferenceRegressionIT.assertPackageContains(
        pack, ChemistryQuantumProgramKind.UCCSD_ANSATZ);
    AbInitioWorkflowReferenceRegressionIT.assertPackageContains(
        pack, ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ);
    AbInitioWorkflowReferenceRegressionIT.assertPackageContains(
        pack, ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ);
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
            AbInitioWorkflowReferenceRegressionIT.atom("h0", "H", 0.0, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("h1", "H", 0.0, 0.0, 0.741848095192207)),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule pyridine() {
    return Molecule.of(
        MoleculeId.of("workflow.reference.pyridine"),
        "Pyridine workflow reference",
        List.of(
            AbInitioWorkflowReferenceRegressionIT.atom("n0", "N", 1.39, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("c1", "C", 0.695, 1.204, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("c2", "C", -0.695, 1.204, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("c3", "C", -1.39, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("c4", "C", -0.695, -1.204, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("c5", "C", 0.695, -1.204, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("h1", "H", 1.24, 2.148, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("h2", "H", -1.24, 2.148, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("h3", "H", -2.48, 0.0, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("h4", "H", -1.24, -2.148, 0.0),
            AbInitioWorkflowReferenceRegressionIT.atom("h5", "H", 1.24, -2.148, 0.0)),
        AbInitioWorkflowReferenceRegressionIT.pyridineBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
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
}
