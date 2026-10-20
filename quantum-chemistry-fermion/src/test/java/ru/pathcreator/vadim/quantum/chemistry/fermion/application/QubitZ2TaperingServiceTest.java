/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.tapering.QubitZ2TaperingService;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.tapering.FixedZ2Symmetry;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.tapering.QubitTaperingResult;

public final class QubitZ2TaperingServiceTest {

  @Test
  public void fixedZSymmetryTaperingSubstitutesEigenvaluesAndCompactsQubitIndexes() {
    final QubitPauliOperator operator =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.IDENTITY, 1.0)
            .add(QubitPauliTerm.of(2, PauliAxis.Z), 2.0)
            .add(
                QubitPauliTerm.of(
                    new int[] {0, 2, 3}, new PauliAxis[] {PauliAxis.X, PauliAxis.Z, PauliAxis.Y}),
                3.0)
            .build();
    final QubitTaperingResult result =
        new QubitZ2TaperingService().taperByFixedZ(operator, 4, List.of(FixedZ2Symmetry.of(2, -1)));

    assertEquals(1, result.removedQubitCount());
    assertEquals(3, result.taperedQubitCount());
    assertEquals(
        -1.0, QubitZ2TaperingServiceTest.real(result.operator(), QubitPauliTerm.IDENTITY), 1.0E-12);
    assertEquals(
        -3.0,
        QubitZ2TaperingServiceTest.real(
            result.operator(),
            QubitPauliTerm.of(new int[] {0, 2}, new PauliAxis[] {PauliAxis.X, PauliAxis.Y})),
        1.0E-12);
  }

  @Test
  public void emptyFixedZSymmetryTaperingKeepsOperatorAndQubitCount() {
    final QubitPauliOperator operator =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.IDENTITY, 1.0)
            .add(QubitPauliTerm.of(1, PauliAxis.Z), -0.25)
            .build();

    final QubitTaperingResult result =
        new QubitZ2TaperingService().taperByFixedZ(operator, 2, List.of());

    assertEquals(0, result.removedQubitCount());
    assertEquals(2, result.originalQubitCount());
    assertEquals(2, result.taperedQubitCount());
    assertEquals(operator, result.operator());
  }

  @Test
  public void fixedZSymmetryTaperingRejectsSymmetryOutsideOriginalSpace() {
    final QubitPauliOperator operator =
        QubitPauliOperator.builder().add(QubitPauliTerm.of(0, PauliAxis.Z), 1.0).build();

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new QubitZ2TaperingService()
                    .taperByFixedZ(operator, 1, List.of(FixedZ2Symmetry.of(1, 1))));

    assertTrue(exception.getMessage().contains("outside original tapering space"));
  }

  @Test
  public void fixedZSymmetryTaperingRejectsNonCommutingPauliAxisOnRemovedQubit() {
    final QubitPauliOperator operator =
        QubitPauliOperator.builder().add(QubitPauliTerm.of(1, PauliAxis.X), 1.0).build();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new QubitZ2TaperingService()
                .taperByFixedZ(operator, 2, List.of(FixedZ2Symmetry.of(1, 1))));
  }

  @Test
  public void fixedZSymmetryTaperingRejectsNullSymmetryEntry() {
    final QubitPauliOperator operator =
        QubitPauliOperator.builder().add(QubitPauliTerm.of(0, PauliAxis.Z), 1.0).build();

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new QubitZ2TaperingService()
                    .taperByFixedZ(
                        operator, 1, java.util.Arrays.asList(FixedZ2Symmetry.of(0, 1), null)));

    assertTrue(exception.getMessage().contains("must not contain null"));
  }

  @Test
  public void fixedZSymmetryTaperingRejectsDuplicateSymmetryQubits() {
    final QubitPauliOperator operator =
        QubitPauliOperator.builder().add(QubitPauliTerm.of(0, PauliAxis.Z), 1.0).build();

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new QubitZ2TaperingService()
                    .taperByFixedZ(
                        operator, 1, List.of(FixedZ2Symmetry.of(0, 1), FixedZ2Symmetry.of(0, -1))));

    assertTrue(exception.getMessage().contains("must be unique"));
  }

  @Test
  public void spinBlockParityTaperingRemovesAlphaAndTotalParityQubitsForHydrogenActiveSpace() {
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
    final QubitPauliOperator parityOperator =
        new FermionToQubitMapper()
            .map(
                fermionOperator,
                FermionToQubitMapping.SPIN_BLOCK_PARITY,
                hamiltonian.spinOrbitalCount());

    final QubitTaperingResult result =
        new QubitZ2TaperingService()
            .taperSpinBlockParity(parityOperator, hamiltonian.spinOrbitalCount(), 1, 1);

    assertEquals(2, result.removedQubitCount());
    assertEquals(2, result.taperedQubitCount());
    assertTrue(result.operator().termCount() > 0);
    assertTrue(result.operator().requiredQubitCount() <= 2);
    assertTrue(result.operator().hermitianWithinTolerance(1.0E-10));
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
