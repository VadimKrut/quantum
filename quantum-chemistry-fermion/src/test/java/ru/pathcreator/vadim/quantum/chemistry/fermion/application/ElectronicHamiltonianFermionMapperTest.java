/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping;

public final class ElectronicHamiltonianFermionMapperTest {

  @Test
  public void expandsCanonicalHydrogenHamiltonianToHermitianFermionAndQubitOperators() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
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
    final FermionOperator fermionOperator =
        new ElectronicHamiltonianFermionMapper().map(hamiltonian);
    final QubitPauliOperator qubitOperator =
        new FermionToQubitMapper()
            .map(
                fermionOperator,
                FermionToQubitMapping.JORDAN_WIGNER,
                hamiltonian.spinOrbitalCount());

    assertTrue(fermionOperator.termCount() > hamiltonian.integralCount());
    assertTrue(qubitOperator.termCount() > 0);
    assertTrue(qubitOperator.hermitianWithinTolerance(1.0E-10));
  }

  @Test
  public void normalOrderedHamiltonianExpansionRemainsHermitianAndMappable() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(2, 2),
            0.713753993687618,
            List.of(
                OneElectronIntegral.of(0, 0, -1.252463573565),
                OneElectronIntegral.of(1, 1, -0.475948715221),
                OneElectronIntegral.of(0, 1, 0.12)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.674488766357),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.663468096424),
                TwoElectronIntegral.of(0, 1, 1, 0, 0.181288808211),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.697393767423)));
    final ElectronicHamiltonianFermionMapper fermionMapper =
        new ElectronicHamiltonianFermionMapper();
    final FermionOperator normalOrdered = fermionMapper.mapNormalOrdered(hamiltonian);
    final QubitPauliOperator qubitOperator =
        new FermionToQubitMapper()
            .map(
                normalOrdered, FermionToQubitMapping.BRAVYI_KITAEV, hamiltonian.spinOrbitalCount());

    assertTrue(new FermionOperatorAlgebra().hermitianWithinTolerance(normalOrdered, 1.0E-10));
    assertTrue(qubitOperator.hermitianWithinTolerance(1.0E-10));
    assertTrue(normalOrdered.termCount() > 0);
  }

  @Test
  public void normalOrderedVerifiedFacadeWorksForAllBuiltInMappings() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(2, 2),
            0.713753993687618,
            List.of(
                OneElectronIntegral.of(0, 0, -1.252463573565),
                OneElectronIntegral.of(1, 1, -0.475948715221),
                OneElectronIntegral.of(0, 1, 0.12)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.674488766357),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.663468096424),
                TwoElectronIntegral.of(0, 1, 1, 0, 0.181288808211),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.697393767423)));
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final VerifiedElectronicHamiltonianMapping verified =
          mapper.mapNormalOrderedVerified(hamiltonian, mapping, 1.0E-8);
      assertTrue(verified.verified(), mapping + " " + verified.spectralResult().maxDifference());
      assertTrue(verified.qubitOperator().hermitianWithinTolerance(1.0E-10));
    }
  }

  @Test
  public void mapsNumberOperatorWithAllBuiltInMappings() {
    final FermionOperator numberOperator =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(1)), 1.0)
            .build();
    final FermionToQubitMapper mapper = new FermionToQubitMapper();

    final QubitPauliOperator jordanWigner =
        mapper.map(numberOperator, FermionToQubitMapping.JORDAN_WIGNER, 2);
    final QubitPauliOperator reversedJordanWigner =
        mapper.map(numberOperator, FermionToQubitMapping.REVERSED_JORDAN_WIGNER, 2);
    final QubitPauliOperator parity = mapper.map(numberOperator, FermionToQubitMapping.PARITY, 2);
    final QubitPauliOperator bravyiKitaev =
        mapper.map(numberOperator, FermionToQubitMapping.BRAVYI_KITAEV, 2);

    assertEquals(
        0.5,
        ElectronicHamiltonianFermionMapperTest.real(jordanWigner, QubitPauliTerm.IDENTITY),
        1.0E-12);
    assertEquals(
        -0.5,
        ElectronicHamiltonianFermionMapperTest.real(
            jordanWigner, QubitPauliTerm.of(1, PauliAxis.Z)),
        1.0E-12);
    assertEquals(
        -0.5,
        ElectronicHamiltonianFermionMapperTest.real(
            reversedJordanWigner, QubitPauliTerm.of(0, PauliAxis.Z)),
        1.0E-12);
    assertEquals(
        -0.5,
        ElectronicHamiltonianFermionMapperTest.real(
            parity,
            QubitPauliTerm.of(new int[] {0, 1}, new PauliAxis[] {PauliAxis.Z, PauliAxis.Z})),
        1.0E-12);
    assertEquals(
        0.5,
        ElectronicHamiltonianFermionMapperTest.real(bravyiKitaev, QubitPauliTerm.IDENTITY),
        1.0E-12);
    assertEquals(
        -0.5,
        ElectronicHamiltonianFermionMapperTest.real(
            bravyiKitaev,
            QubitPauliTerm.of(new int[] {0, 1}, new PauliAxis[] {PauliAxis.Z, PauliAxis.Z})),
        1.0E-12);
    assertTrue(jordanWigner.hermitianWithinTolerance(1.0E-12));
    assertTrue(reversedJordanWigner.hermitianWithinTolerance(1.0E-12));
    assertTrue(parity.hermitianWithinTolerance(1.0E-12));
    assertTrue(bravyiKitaev.hermitianWithinTolerance(1.0E-12));
  }

  @Test
  public void bravyiKitaevMapsFirstModeNumberOperatorToLocalOccupationParity() {
    final FermionOperator numberOperator =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(0), FermionTerm.annihilation(0)), 1.0)
            .build();
    final QubitPauliOperator bravyiKitaev =
        new FermionToQubitMapper().map(numberOperator, FermionToQubitMapping.BRAVYI_KITAEV, 2);

    assertEquals(
        0.5,
        ElectronicHamiltonianFermionMapperTest.real(bravyiKitaev, QubitPauliTerm.IDENTITY),
        1.0E-12);
    assertEquals(
        -0.5,
        ElectronicHamiltonianFermionMapperTest.real(
            bravyiKitaev, QubitPauliTerm.of(0, PauliAxis.Z)),
        1.0E-12);
    assertTrue(bravyiKitaev.hermitianWithinTolerance(1.0E-12));
  }

  @Test
  public void mapsHydrogenHamiltonianWithEveryBuiltInMapping() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
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
    final FermionOperator fermionOperator =
        new ElectronicHamiltonianFermionMapper().map(hamiltonian);
    final FermionToQubitMapper mapper = new FermionToQubitMapper();

    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final QubitPauliOperator qubitOperator =
          mapper.map(fermionOperator, mapping, hamiltonian.spinOrbitalCount());

      assertTrue(qubitOperator.termCount() > 0, mapping.name());
      assertTrue(
          qubitOperator.requiredQubitCount() <= hamiltonian.spinOrbitalCount(), mapping.name());
      assertTrue(qubitOperator.hermitianWithinTolerance(1.0E-10), mapping.name());
    }
  }

  @Test
  public void spinBlockMappingsMatchExplicitSpinOrbitalPermutation() {
    final FermionOperator interleavedNumberOperator =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(1)), 1.0)
            .add(FermionTerm.of(FermionTerm.creation(2), FermionTerm.annihilation(2)), 0.75)
            .build();
    final FermionOperator blockNumberOperator =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(2), FermionTerm.annihilation(2)), 1.0)
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(1)), 0.75)
            .build();
    final FermionToQubitMapper mapper = new FermionToQubitMapper();

    assertEquals(
        mapper.map(blockNumberOperator, FermionToQubitMapping.JORDAN_WIGNER, 4),
        mapper.map(interleavedNumberOperator, FermionToQubitMapping.SPIN_BLOCK_JORDAN_WIGNER, 4));
    assertEquals(
        mapper.map(blockNumberOperator, FermionToQubitMapping.PARITY, 4),
        mapper.map(interleavedNumberOperator, FermionToQubitMapping.SPIN_BLOCK_PARITY, 4));
    assertEquals(
        mapper.map(blockNumberOperator, FermionToQubitMapping.BRAVYI_KITAEV, 4),
        mapper.map(interleavedNumberOperator, FermionToQubitMapping.SPIN_BLOCK_BRAVYI_KITAEV, 4));
  }

  @Test
  public void spinBlockMappingsRejectOddSpinOrbitalCount() {
    final FermionOperator numberOperator =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(1)), 1.0)
            .build();

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            new FermionToQubitMapper()
                .map(numberOperator, FermionToQubitMapping.SPIN_BLOCK_JORDAN_WIGNER, 3));
  }

  private static double real(final QubitPauliOperator operator, final QubitPauliTerm term) {
    for (int i = 0; i < operator.termCount(); ++i) {
      if (operator.term(i).equals(term)) {
        assertEquals(0.0, operator.coefficient(i).imaginary(), 1.0E-12);
        return operator.coefficient(i).real();
      }
    }
    throw new AssertionError("Pauli term not found: " + term);
  }
}
