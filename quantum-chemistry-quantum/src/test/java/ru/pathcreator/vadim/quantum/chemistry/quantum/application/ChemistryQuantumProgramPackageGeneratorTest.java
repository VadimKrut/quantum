/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramArtifact;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackageOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumPreparationProvenance;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramSkip;

final class ChemistryQuantumProgramPackageGeneratorTest {

  @Test
  void packageContainsEvolutionVariationalAndPhaseEstimationPrograms() {
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "h2",
                ChemistryQuantumProgramPackageGeneratorTest.hydrogenActiveSpaceHamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER);

    assertTrue(pack.hasArtifacts());
    assertFalse(pack.artifactIds().isEmpty());
    assertTrue(pack.totalOperationCount() > 0L);
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.HARTREE_FOCK_REFERENCE));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.UCCSD_ANSATZ));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertEquals(pack.artifacts().size(), pack.artifactCount());
    assertEquals(pack.skippedArtifacts().size(), pack.skippedArtifactCount());
    assertNotNull(pack.requireArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertNotNull(pack.requireArtifact(ChemistryQuantumProgramKind.HARTREE_FOCK_REFERENCE));
    assertFalse(
        pack.requireArtifact(ChemistryQuantumProgramKind.HARTREE_FOCK_REFERENCE).hasMapping());
    assertNotNull(pack.artifactOrNull(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertNull(pack.skipOrNull(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertEquals("", pack.skipReasonOrEmpty(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertEquals(
        FermionToQubitMapping.values().length,
        pack.artifactCount(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertEquals(
        FermionToQubitMapping.values().length,
        pack.artifacts(ChemistryQuantumProgramKind.PAULI_EVOLUTION).size());
    assertEquals(
        FermionToQubitMapping.JORDAN_WIGNER,
        pack.primaryProgram()
                == pack.requireArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION).program()
            ? pack.requireArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION).mapping()
            : null);
    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      assertTrue(
          pack.hasArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION, mapping), mapping.name());
      assertTrue(
          pack.hasArtifact(ChemistryQuantumProgramKind.PAULI_EXPECTATION_MEASUREMENT, mapping),
          mapping.name());
      assertTrue(
          pack.hasArtifact(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION, mapping),
          mapping.name());
      assertTrue(
          pack.hasArtifact(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION, mapping),
          mapping.name());
    }
    for (final ChemistryQuantumProgramArtifact artifact : pack.artifacts()) {
      assertTrue(artifact.operationCount() > 0L);
      assertTrue(Quantum.validate(artifact.program()).isValid(), artifact.id());
    }
  }

  @Test
  void hartreeFockReferenceArtifactPreparesClosedPrefixOccupation() {
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "h2_reference",
                ChemistryQuantumProgramPackageGeneratorTest.hydrogenActiveSpaceHamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(ChemistryQuantumProgramKind.HARTREE_FOCK_REFERENCE),
                    1.0E-8));

    assertEquals(1, pack.artifactCount());
    final ChemistryQuantumProgramArtifact artifact =
        pack.requireArtifact(ChemistryQuantumProgramKind.HARTREE_FOCK_REFERENCE);
    assertFalse(artifact.hasMapping());
    assertEquals(2L, artifact.operationCount());
    assertTrue(Quantum.validate(artifact.program()).isValid(), artifact.id());
  }

  @Test
  void packageOptionsLimitMappingsAndProgramKindsForLargeActiveSpaces() {
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "h2_selective",
                ChemistryQuantumProgramPackageGeneratorTest.hydrogenActiveSpaceHamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION),
                    1.0E-6));

    assertTrue(pack.hasArtifacts());
    assertEquals(2, pack.artifactCount());
    assertTrue(
        pack.hasArtifact(
            ChemistryQuantumProgramKind.PAULI_EVOLUTION, FermionToQubitMapping.JORDAN_WIGNER));
    assertTrue(
        pack.hasArtifact(
            ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION,
            FermionToQubitMapping.JORDAN_WIGNER));
    assertFalse(
        pack.hasArtifact(
            ChemistryQuantumProgramKind.PAULI_EVOLUTION, FermionToQubitMapping.PARITY));
    assertFalse(pack.hasArtifact(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION));
    assertFalse(pack.hasArtifact(ChemistryQuantumProgramKind.UCCSD_ANSATZ));
    assertTrue(pack.skippedArtifacts().isEmpty(), pack.skippedArtifacts().toString());
    for (final ChemistryQuantumProgramArtifact artifact : pack.artifacts()) {
      assertTrue(Quantum.validate(artifact.program()).isValid(), artifact.id());
    }
  }

  @Test
  void packageBuildsValidatedProgramsForHeterocycleLikeOpenShellActiveSpace() {
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "heterocycle_radical_probe",
                ChemistryQuantumProgramPackageGeneratorTest.heterocycleLikeOpenShellHamiltonian(),
                FermionToQubitMapping.BRAVYI_KITAEV,
                ChemistryQuantumProgramPackageOptions.of(
                    List.of(
                        FermionToQubitMapping.BRAVYI_KITAEV,
                        FermionToQubitMapping.JORDAN_WIGNER,
                        FermionToQubitMapping.PARITY),
                    EnumSet.of(
                        ChemistryQuantumProgramKind.PAULI_EVOLUTION,
                        ChemistryQuantumProgramKind.UCCSD_ANSATZ,
                        ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ,
                        ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ,
                        ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION),
                    1.0E-7));

    assertTrue(pack.hasArtifacts());
    assertTrue(pack.generationComplete(), pack.skippedArtifacts().toString());
    assertEquals(3, pack.artifactCount(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertEquals(3, pack.artifactCount(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.UCCSD_ANSATZ));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ));
    assertTrue(pack.hasArtifact(ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ));
    assertTrue(pack.totalOperationCount() > 100L);
    for (final ChemistryQuantumProgramArtifact artifact : pack.artifacts()) {
      assertTrue(artifact.operationCount() > 0L, artifact.id());
      assertTrue(Quantum.validate(artifact.program()).isValid(), artifact.id());
    }
  }

  @Test
  void packageBuildsVerifiedEvolutionForLargeActiveSpaceBeyondExhaustiveSpectralLimit() {
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "large_active_space_probe",
                ChemistryQuantumProgramPackageGeneratorTest.largeActiveSpaceHamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER,
                ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
                    FermionToQubitMapping.JORDAN_WIGNER,
                    EnumSet.of(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION),
                    1.0E-8));

    assertTrue(pack.hasArtifacts());
    assertTrue(pack.generationComplete(), pack.skippedArtifacts().toString());
    assertEquals(1, pack.artifactCount());
    assertEquals(1, pack.artifactCount(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION));
    assertEquals(10, pack.spinOrbitalCount());
    assertEquals(4, pack.electronCount());
    final ChemistryQuantumProgramArtifact artifact =
        pack.requireArtifact(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION);
    assertEquals(FermionToQubitMapping.JORDAN_WIGNER, artifact.mapping());
    assertTrue(artifact.operationCount() > 0L);
    assertTrue(Quantum.validate(artifact.program()).isValid(), artifact.id());
  }

  @Test
  void packageRejectsDuplicateArtifactIds() {
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "h2",
                ChemistryQuantumProgramPackageGeneratorTest.hydrogenActiveSpaceHamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER);
    final ChemistryQuantumProgramArtifact artifact =
        pack.requireArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryQuantumProgramPackage.of(
                "duplicated",
                FermionToQubitMapping.JORDAN_WIGNER,
                2,
                2,
                List.of(artifact, artifact),
                List.of()));
  }

  @Test
  void packageAllowsSeveralArtifactsOfSameKindForCandidateSpecificPrograms() {
    final ChemistryQuantumProgramPackage pack =
        new ChemistryQuantumProgramPackageGenerator()
            .generate(
                "h2",
                ChemistryQuantumProgramPackageGeneratorTest.hydrogenActiveSpaceHamiltonian(),
                FermionToQubitMapping.JORDAN_WIGNER);
    final ChemistryQuantumProgramArtifact artifact =
        pack.requireArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION);
    final ChemistryQuantumProgramArtifact repeatedKind =
        ChemistryQuantumProgramArtifact.of(
            artifact.id() + "_candidate_2",
            artifact.kind(),
            artifact.mapping(),
            artifact.description(),
            artifact.program());

    final ChemistryQuantumProgramPackage repeatedKindPack =
        ChemistryQuantumProgramPackage.of(
            "conflicting",
            FermionToQubitMapping.JORDAN_WIGNER,
            2,
            2,
            List.of(artifact, repeatedKind),
            List.of());

    assertEquals(2, repeatedKindPack.artifactCount(ChemistryQuantumProgramKind.PAULI_EVOLUTION));
    assertEquals(2, repeatedKindPack.artifacts(ChemistryQuantumProgramKind.PAULI_EVOLUTION).size());
  }

  @Test
  void packageRejectsKnownProvenanceThatDoesNotMatchHamiltonianDimensions() {
    final ChemistryQuantumPreparationProvenance provenance =
        ChemistryQuantumPreparationProvenance.of(
            "RHF",
            "DIRECT_GAUSSIAN",
            "CLOSED_SHELL",
            "STO-3G",
            "2e/2so",
            "source_calculation",
            2,
            2,
            1,
            FermionToQubitMapping.JORDAN_WIGNER,
            false);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryQuantumProgramPackage.of(
                "bad_electron_count",
                FermionToQubitMapping.JORDAN_WIGNER,
                2,
                1,
                provenance,
                List.of(),
                List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryQuantumProgramPackage.of(
                "bad_spin_orbital_count",
                FermionToQubitMapping.JORDAN_WIGNER,
                4,
                2,
                provenance,
                List.of(),
                List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryQuantumProgramPackage.of(
                "bad_mapping",
                FermionToQubitMapping.PARITY,
                2,
                2,
                provenance,
                List.of(),
                List.of()));
  }

  @Test
  void packageAcceptsConsistentKnownProvenance() {
    final ChemistryQuantumPreparationProvenance provenance =
        ChemistryQuantumPreparationProvenance.of(
            "RHF",
            "DIRECT_GAUSSIAN",
            "CLOSED_SHELL",
            "STO-3G",
            "2e/2so",
            "source_calculation",
            2,
            2,
            1,
            FermionToQubitMapping.JORDAN_WIGNER,
            false);

    final ChemistryQuantumProgramPackage pack =
        ChemistryQuantumProgramPackage.of(
            "consistent_provenance",
            FermionToQubitMapping.JORDAN_WIGNER,
            2,
            2,
            provenance,
            List.of(),
            List.of());

    assertEquals(provenance, pack.provenance());
    assertEquals(2, pack.spinOrbitalCount());
    assertEquals(2, pack.electronCount());
  }

  @Test
  void packageExposesSkippedProgramReasons() {
    final ChemistryQuantumProgramPackage pack =
        ChemistryQuantumProgramPackage.of(
            "skip_case",
            FermionToQubitMapping.JORDAN_WIGNER,
            2,
            2,
            List.of(),
            List.of(
                ChemistryQuantumProgramSkip.of(
                    "skip_case_qpe",
                    ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION,
                    FermionToQubitMapping.JORDAN_WIGNER,
                    "phase register unavailable")));

    assertFalse(pack.generationComplete());
    assertFalse(pack.hasArtifact(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertTrue(pack.hasSkip(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertEquals(
        "phase register unavailable",
        pack.skipReasonOrEmpty(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
    assertThrows(
        IllegalStateException.class,
        () -> pack.requireArtifact(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION));
  }

  private static ElectronicHamiltonian hydrogenActiveSpaceHamiltonian() {
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

  private static ElectronicHamiltonian heterocycleLikeOpenShellHamiltonian() {
    return ElectronicHamiltonian.of(
        ActiveSpace.of(5, 4),
        4.218391,
        List.of(
            OneElectronIntegral.of(0, 0, -1.137),
            OneElectronIntegral.of(1, 1, -0.872),
            OneElectronIntegral.of(2, 2, -0.641),
            OneElectronIntegral.of(3, 3, -0.512),
            OneElectronIntegral.of(0, 1, -0.084),
            OneElectronIntegral.of(1, 2, -0.071),
            OneElectronIntegral.of(2, 3, -0.053),
            OneElectronIntegral.of(0, 3, 0.019),
            OneElectronIntegral.of(0, 2, 0.013),
            OneElectronIntegral.of(1, 3, -0.011)),
        List.of(
            TwoElectronIntegral.of(0, 0, 0, 0, 0.724),
            TwoElectronIntegral.of(1, 1, 1, 1, 0.681),
            TwoElectronIntegral.of(2, 2, 2, 2, 0.642),
            TwoElectronIntegral.of(3, 3, 3, 3, 0.617),
            TwoElectronIntegral.of(0, 0, 1, 1, 0.311),
            TwoElectronIntegral.of(0, 0, 2, 2, 0.284),
            TwoElectronIntegral.of(0, 0, 3, 3, 0.237),
            TwoElectronIntegral.of(1, 1, 2, 2, 0.299),
            TwoElectronIntegral.of(1, 1, 3, 3, 0.221),
            TwoElectronIntegral.of(2, 2, 3, 3, 0.263),
            TwoElectronIntegral.of(0, 1, 0, 1, 0.094),
            TwoElectronIntegral.of(1, 2, 1, 2, 0.088),
            TwoElectronIntegral.of(2, 3, 2, 3, 0.076),
            TwoElectronIntegral.of(0, 3, 0, 3, 0.041),
            TwoElectronIntegral.of(0, 1, 2, 3, 0.019),
            TwoElectronIntegral.of(0, 2, 1, 3, -0.014)));
  }

  private static ElectronicHamiltonian largeActiveSpaceHamiltonian() {
    return ElectronicHamiltonian.of(
        ActiveSpace.of(4, 5),
        3.25,
        List.of(
            OneElectronIntegral.of(0, 0, -1.11),
            OneElectronIntegral.of(1, 1, -0.92),
            OneElectronIntegral.of(2, 2, -0.74),
            OneElectronIntegral.of(3, 3, -0.51),
            OneElectronIntegral.of(4, 4, -0.33),
            OneElectronIntegral.of(0, 1, -0.08),
            OneElectronIntegral.of(1, 2, -0.06),
            OneElectronIntegral.of(2, 3, -0.04),
            OneElectronIntegral.of(3, 4, -0.03)),
        List.of(
            TwoElectronIntegral.of(0, 0, 0, 0, 0.72),
            TwoElectronIntegral.of(1, 1, 1, 1, 0.68),
            TwoElectronIntegral.of(2, 2, 2, 2, 0.61),
            TwoElectronIntegral.of(3, 3, 3, 3, 0.55),
            TwoElectronIntegral.of(4, 4, 4, 4, 0.49),
            TwoElectronIntegral.of(0, 0, 1, 1, 0.31),
            TwoElectronIntegral.of(1, 1, 2, 2, 0.28),
            TwoElectronIntegral.of(2, 2, 3, 3, 0.24),
            TwoElectronIntegral.of(3, 3, 4, 4, 0.21),
            TwoElectronIntegral.of(0, 1, 0, 1, 0.07),
            TwoElectronIntegral.of(1, 2, 1, 2, 0.06),
            TwoElectronIntegral.of(2, 3, 2, 3, 0.05),
            TwoElectronIntegral.of(3, 4, 3, 4, 0.04)));
  }
}
