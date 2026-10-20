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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

public final class FermionOperatorAlgebraTest {

  @Test
  public void normalOrderAppliesCanonicalAnticommutationContractions() {
    final FermionOperator normalOrdered =
        new FermionOperatorAlgebra()
            .normalOrder(
                FermionOperator.builder()
                    .add(FermionTerm.of(FermionTerm.annihilation(2), FermionTerm.creation(2)), 1.0)
                    .build());

    assertEquals(2, normalOrdered.termCount());
    assertEquals(
        1.0, FermionOperatorAlgebraTest.real(normalOrdered, FermionTerm.IDENTITY), 1.0E-12);
    assertEquals(
        -1.0,
        FermionOperatorAlgebraTest.real(
            normalOrdered, FermionTerm.of(FermionTerm.creation(2), FermionTerm.annihilation(2))),
        1.0E-12);
  }

  @Test
  public void normalOrderDropsDuplicateSameActionLadderOperators() {
    final FermionOperator normalOrdered =
        new FermionOperatorAlgebra()
            .normalOrder(
                FermionOperator.builder()
                    .add(FermionTerm.of(FermionTerm.creation(0), FermionTerm.creation(0)), 1.0)
                    .add(
                        FermionTerm.of(FermionTerm.annihilation(1), FermionTerm.annihilation(1)),
                        1.0)
                    .build());

    assertTrue(normalOrdered.empty());
  }

  @Test
  public void commutatorTracksNumberOperatorLadderSign() {
    final FermionOperatorAlgebra algebra = new FermionOperatorAlgebra();
    final FermionOperator numberOperator =
        FermionOperator.builder()
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(1)), 1.0)
            .build();
    final FermionOperator creation =
        FermionOperator.builder().add(FermionTerm.of(FermionTerm.creation(1)), 1.0).build();
    final FermionOperator annihilation =
        FermionOperator.builder().add(FermionTerm.of(FermionTerm.annihilation(1)), 1.0).build();

    assertEquals(
        1.0,
        FermionOperatorAlgebraTest.real(
            algebra.commutator(numberOperator, creation), FermionTerm.of(FermionTerm.creation(1))),
        1.0E-12);
    assertEquals(
        -1.0,
        FermionOperatorAlgebraTest.real(
            algebra.commutator(numberOperator, annihilation),
            FermionTerm.of(FermionTerm.annihilation(1))),
        1.0E-12);
  }

  @Test
  public void electronicHamiltonianIsHermitianAfterNormalOrdering() {
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
    final FermionOperator fermionOperator =
        new ElectronicHamiltonianFermionMapper().map(hamiltonian);
    final FermionOperatorAlgebra algebra = new FermionOperatorAlgebra();

    assertTrue(algebra.hermitianWithinTolerance(algebra.normalOrder(fermionOperator), 1.0E-10));
  }

  private static double real(final FermionOperator operator, final FermionTerm term) {
    for (int i = 0; i < operator.termCount(); ++i) {
      if (!operator.term(i).equals(term)) {
        continue;
      }
      assertEquals(0.0, operator.coefficient(i).imaginary(), 1.0E-12);
      return operator.coefficient(i).real();
    }
    return 0.0;
  }
}
