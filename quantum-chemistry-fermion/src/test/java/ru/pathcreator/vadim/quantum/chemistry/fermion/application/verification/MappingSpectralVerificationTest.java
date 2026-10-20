/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application.verification;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianFermionMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

public final class MappingSpectralVerificationTest {

  @Test
  public void verifiesNumberOperatorSpectralMomentsForEveryMapping() {
    final FermionOperator operator =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(0), FermionTerm.annihilation(0)), 0.75)
            .add(FermionTerm.of(FermionTerm.creation(2), FermionTerm.annihilation(2)), -0.25)
            .build();
    final MappingSpectralVerification verification = new MappingSpectralVerification();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final MappingSpectralVerification.MappingSpectralVerificationResult result =
          verification.verify(operator, mapping, 4, 4, 1.0E-9);

      assertTrue(
          result.equivalentWithinTolerance(),
          mapping.name() + " max difference " + result.maxDifference());
      assertArrayEquals(result.fermionMoments(), result.qubitMoments(), 1.0E-9, mapping.name());
    }
  }

  @Test
  public void verifiesExcitationOperatorSpectralMomentsForEveryMapping() {
    final FermionOperator excitation =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(0), FermionTerm.annihilation(1)), 0.35)
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(0)), 0.35)
            .add(FermionTerm.of(FermionTerm.creation(2), FermionTerm.annihilation(3)), -0.125)
            .add(FermionTerm.of(FermionTerm.creation(3), FermionTerm.annihilation(2)), -0.125)
            .build();
    final MappingSpectralVerification verification = new MappingSpectralVerification();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final MappingSpectralVerification.MappingSpectralVerificationResult result =
          verification.verify(excitation, mapping, 4, 4, 1.0E-9);

      assertTrue(
          result.equivalentWithinTolerance(),
          mapping.name() + " max difference " + result.maxDifference());
    }
  }

  @Test
  public void verifiesHydrogenLikeElectronicHamiltonianSpectralMomentsForEveryMapping() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(2, 2),
            0.713753993687618,
            List.of(
                OneElectronIntegral.of(0, 0, -1.252463573565),
                OneElectronIntegral.of(1, 1, -0.475948715221),
                OneElectronIntegral.of(0, 1, 0.03451219)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.674488766357),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.663468096424),
                TwoElectronIntegral.of(0, 1, 0, 1, 0.181288808211),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.697393767423)));
    final FermionOperator operator = new ElectronicHamiltonianFermionMapper().map(hamiltonian);
    final MappingSpectralVerification verification = new MappingSpectralVerification();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final MappingSpectralVerification.MappingSpectralVerificationResult result =
          verification.verify(operator, mapping, hamiltonian.spinOrbitalCount(), 4, 1.0E-8);

      assertTrue(
          result.equivalentWithinTolerance(),
          mapping.name() + " max difference " + result.maxDifference());
    }
  }

  @Test
  public void exhaustivelyVerifiesSmallElectronicHamiltonianForEveryMapping() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(2, 2),
            0.713753993687618,
            List.of(
                OneElectronIntegral.of(0, 0, -1.252463573565),
                OneElectronIntegral.of(1, 1, -0.475948715221),
                OneElectronIntegral.of(0, 1, 0.03451219)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.674488766357),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.663468096424),
                TwoElectronIntegral.of(0, 1, 0, 1, 0.181288808211),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.697393767423)));
    final FermionOperator operator = new ElectronicHamiltonianFermionMapper().map(hamiltonian);
    final MappingSpectralVerification verification = new MappingSpectralVerification();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final MappingSpectralVerification.MappingSpectralVerificationResult result =
          verification.verifyExhaustiveSmallSpace(
              operator, mapping, hamiltonian.spinOrbitalCount(), 1.0E-8);

      assertEquals(1 << hamiltonian.spinOrbitalCount(), result.momentCount(), mapping.name());
      assertTrue(
          result.equivalentWithinTolerance(),
          mapping.name() + " max difference " + result.maxDifference());
      assertTrue(result.fermionHermitianDeviation() <= 1.0E-8, mapping.name());
      assertTrue(result.qubitHermitianDeviation() <= 1.0E-8, mapping.name());
    }
  }

  @Test
  public void verifiedMappingFallsBackToStructuralLargeSpaceForLargerActiveSpaces() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(4, 5),
            1.125,
            List.of(
                OneElectronIntegral.of(0, 0, -1.00),
                OneElectronIntegral.of(1, 1, -0.72),
                OneElectronIntegral.of(2, 2, -0.31),
                OneElectronIntegral.of(3, 3, 0.18),
                OneElectronIntegral.of(4, 4, 0.44),
                OneElectronIntegral.of(0, 1, 0.025),
                OneElectronIntegral.of(2, 3, -0.018)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.62),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.58),
                TwoElectronIntegral.of(2, 2, 2, 2, 0.41),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.19),
                TwoElectronIntegral.of(1, 1, 2, 2, 0.14),
                TwoElectronIntegral.of(3, 3, 4, 4, 0.07)));
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final VerifiedElectronicHamiltonianMapping verified =
          mapper.mapVerified(hamiltonian, mapping, 1.0E-8);

      assertTrue(verified.verified(), mapping.name());
      assertEquals(
          "STRUCTURAL_LARGE_SPACE", verified.spectralResult().verificationMode(), mapping.name());
      assertEquals(0, verified.spectralResult().momentCount(), mapping.name());
      assertTrue(
          verified.qubitOperator().requiredQubitCount() <= hamiltonian.spinOrbitalCount(),
          mapping.name());
      assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-8), mapping.name());
    }
  }
}
