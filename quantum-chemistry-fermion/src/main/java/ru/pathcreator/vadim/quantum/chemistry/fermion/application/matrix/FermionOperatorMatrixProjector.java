/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application.matrix;

import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionLadderAction;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

/**
 * Строит occupation-basis matrix для fermionic operator на малом active space.
 */
public final class FermionOperatorMatrixProjector {

  private static final int MAX_SPIN_ORBITALS = 12;

  public DenseComplexMatrix project(final FermionOperator operator, final int spinOrbitalCount) {
    if (operator == null) {
      throw new IllegalArgumentException(
          "Fermion operator for matrix projection must not be null.");
    }
    if (spinOrbitalCount <= 0 || spinOrbitalCount > MAX_SPIN_ORBITALS) {
      throw new IllegalArgumentException(
          "Fermion matrix projection spin orbital count is outside supported range.");
    }
    final int matrixSize = 1 << spinOrbitalCount;
    final DenseComplexMatrix matrix = DenseComplexMatrix.zero(matrixSize);
    for (int termIndex = 0; termIndex < operator.termCount(); ++termIndex) {
      this.addTerm(
          matrix, operator.term(termIndex), operator.coefficient(termIndex), spinOrbitalCount);
    }
    return matrix;
  }

  private void addTerm(
      final DenseComplexMatrix matrix,
      final FermionTerm term,
      final ComplexCoefficient coefficient,
      final int spinOrbitalCount) {
    final int matrixSize = 1 << spinOrbitalCount;
    for (int column = 0; column < matrixSize; ++column) {
      final FermionKet ket = this.apply(term, column);
      if (!ket.valid()) {
        continue;
      }
      matrix.add(ket.state(), column, coefficient.multiply(ket.sign()));
    }
  }

  private FermionKet apply(final FermionTerm term, final int inputState) {
    int state = inputState;
    int sign = 1;
    for (int i = term.length() - 1; i >= 0; --i) {
      final long encodedOperator = term.operator(i);
      final int spinOrbital = FermionTerm.spinOrbital(encodedOperator);
      final int mask = 1 << spinOrbital;
      if ((Integer.bitCount(state & (mask - 1)) & 1) != 0) {
        sign = -sign;
      }
      if (FermionTerm.action(encodedOperator) == FermionLadderAction.ANNIHILATION) {
        if ((state & mask) == 0) {
          return FermionKet.invalid();
        }
        state &= ~mask;
      } else {
        if ((state & mask) != 0) {
          return FermionKet.invalid();
        }
        state |= mask;
      }
    }
    return FermionKet.of(state, sign);
  }

  private static final class FermionKet {

    private final int state;
    private final int sign;

    private FermionKet(final int state, final int sign) {
      this.state = state;
      this.sign = sign;
    }

    private static FermionKet of(final int state, final int sign) {
      return new FermionKet(state, sign);
    }

    private static FermionKet invalid() {
      return new FermionKet(-1, 0);
    }

    private boolean valid() {
      return this.sign != 0;
    }

    private int state() {
      return this.state;
    }

    private double sign() {
      return (double) this.sign;
    }
  }
}
