/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant;

import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionLadderAction;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

/**
 * Проецирует real Hermitian fermion Hamiltonian на Slater determinant basis.
 */
public final class DeterminantSpaceHamiltonianProjector {

  public double[] projectSymmetric(final FermionOperator operator, final long[] determinants) {
    final double[] matrix = this.project(operator, determinants);
    DeterminantSpaceHamiltonianProjector.symmetrize(matrix, determinants.length);
    return matrix;
  }

  public double[] project(final FermionOperator operator, final long[] determinants) {
    if (operator == null) {
      throw new IllegalArgumentException("Fermion operator must not be null.");
    }
    if (determinants == null || determinants.length == 0) {
      throw new IllegalArgumentException("Determinant basis must not be empty.");
    }
    final LongDeterminantIndex determinantIndex = LongDeterminantIndex.of(determinants);
    final int size = determinants.length;
    final double[] matrix = new double[Math.multiplyExact(size, size)];
    for (int column = 0; column < size; ++column) {
      final long ket = determinants[column];
      for (int termIndex = 0; termIndex < operator.termCount(); ++termIndex) {
        final FermionTerm term = operator.term(termIndex);
        final ComplexCoefficient coefficient = operator.coefficient(termIndex);
        if (!coefficient.effectivelyReal()) {
          throw new IllegalArgumentException(
              "Determinant-space projection requires a real Hermitian Hamiltonian.");
        }
        final AppliedDeterminantTerm applied =
            DeterminantSpaceHamiltonianProjector.apply(term, ket);
        if (!applied.nonZero()) {
          continue;
        }
        final int row = determinantIndex.indexOf(applied.determinant());
        if (row >= 0) {
          matrix[row * size + column] += applied.phase() * coefficient.real();
        }
      }
    }
    return matrix;
  }

  public static AppliedDeterminantTerm apply(final FermionTerm term, final long determinant) {
    if (term == null) {
      throw new IllegalArgumentException("Fermion term must not be null.");
    }
    long state = determinant;
    double phase = 1.0;
    for (int i = term.length() - 1; i >= 0; --i) {
      final long encodedOperator = term.operator(i);
      final int orbital = FermionTerm.spinOrbital(encodedOperator);
      final long mask = 1L << orbital;
      final boolean occupied = (state & mask) != 0L;
      if (FermionTerm.action(encodedOperator) == FermionLadderAction.CREATION) {
        if (occupied) {
          return AppliedDeterminantTerm.zero();
        }
        phase *= DeterminantSpaceHamiltonianProjector.fermionPhase(state, orbital);
        state |= mask;
      } else {
        if (!occupied) {
          return AppliedDeterminantTerm.zero();
        }
        phase *= DeterminantSpaceHamiltonianProjector.fermionPhase(state, orbital);
        state &= ~mask;
      }
    }
    return AppliedDeterminantTerm.of(state, phase);
  }

  public static double fermionPhase(final long determinant, final int orbital) {
    final long lowerMask = (1L << orbital) - 1L;
    return (Long.bitCount(determinant & lowerMask) & 1) == 0 ? 1.0 : -1.0;
  }

  public static void symmetrize(final double[] matrix, final int size) {
    for (int row = 0; row < size; ++row) {
      for (int column = row + 1; column < size; ++column) {
        final int upperIndex = row * size + column;
        final int lowerIndex = column * size + row;
        final double value = 0.5 * (matrix[upperIndex] + matrix[lowerIndex]);
        matrix[upperIndex] = value;
        matrix[lowerIndex] = value;
      }
    }
  }
}
