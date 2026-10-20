/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.FermionSlaterDeterminantExpectationEstimator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

public final class DeterminantSpaceHamiltonianProjectorTest {

  @Test
  public void fullAndCisdGeneratorsProduceExpectedPrimitiveDeterminants() {
    assertArrayEquals(
        new long[] {3L, 5L, 9L, 6L, 10L, 12L}, DeterminantSpaceGenerator.full(4, 2, 100));
    assertArrayEquals(new long[] {3L, 5L, 6L, 9L, 10L, 12L}, DeterminantSpaceGenerator.cisd(4, 2));
    assertArrayEquals(
        new long[] {3L, 6L, 9L, 12L}, DeterminantSpaceGenerator.spinSector(4, 2, 1, 1, 100));
  }

  @Test
  public void projectedDiagonalMatchesSlaterDeterminantExpectationEstimator() {
    final FermionOperator operator =
        FermionOperator.builder()
            .add(FermionTerm.IDENTITY, 1.5)
            .add(FermionTerm.of(FermionTerm.creation(0), FermionTerm.annihilation(0)), -0.25)
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(1)), 0.75)
            .add(FermionTerm.of(FermionTerm.creation(1), FermionTerm.annihilation(0)), 0.2)
            .add(FermionTerm.of(FermionTerm.creation(0), FermionTerm.annihilation(1)), 0.2)
            .build();
    final long[] determinants = new long[] {1L, 2L};
    final double[] matrix =
        new DeterminantSpaceHamiltonianProjector().projectSymmetric(operator, determinants);
    final FermionSlaterDeterminantExpectationEstimator estimator =
        new FermionSlaterDeterminantExpectationEstimator();

    assertEquals(estimator.expectation(operator, new boolean[] {true, false}), matrix[0], 1.0E-12);
    assertEquals(estimator.expectation(operator, new boolean[] {false, true}), matrix[3], 1.0E-12);
    assertEquals(matrix[1], matrix[2], 1.0E-12);
    assertEquals(0.2, matrix[1], 1.0E-12);
  }

  @Test
  public void fermionApplicationPreservesExchangeSign() {
    final AppliedDeterminantTerm applied =
        DeterminantSpaceHamiltonianProjector.apply(
            FermionTerm.of(
                FermionTerm.creation(0),
                FermionTerm.creation(1),
                FermionTerm.annihilation(0),
                FermionTerm.annihilation(1)),
            3L);

    assertEquals(3L, applied.determinant());
    assertEquals(-1.0, applied.phase(), 1.0E-12);
  }
}
