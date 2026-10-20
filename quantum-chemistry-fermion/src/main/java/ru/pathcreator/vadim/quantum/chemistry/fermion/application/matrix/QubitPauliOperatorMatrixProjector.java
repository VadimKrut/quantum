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
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;

/**
 * Строит computational-basis matrix для qubit Pauli operator.
 */
public final class QubitPauliOperatorMatrixProjector {

  private static final int MAX_QUBITS = 12;

  public DenseComplexMatrix project(final QubitPauliOperator operator, final int qubitCount) {
    if (operator == null) {
      throw new IllegalArgumentException(
          "Qubit Pauli operator for matrix projection must not be null.");
    }
    if (qubitCount <= 0 || qubitCount > MAX_QUBITS) {
      throw new IllegalArgumentException(
          "Qubit Pauli matrix projection qubit count is outside supported range.");
    }
    final int matrixSize = 1 << qubitCount;
    final DenseComplexMatrix matrix = DenseComplexMatrix.zero(matrixSize);
    for (int termIndex = 0; termIndex < operator.termCount(); ++termIndex) {
      this.addTerm(matrix, operator.term(termIndex), operator.coefficient(termIndex), qubitCount);
    }
    return matrix;
  }

  private void addTerm(
      final DenseComplexMatrix matrix,
      final QubitPauliTerm term,
      final ComplexCoefficient coefficient,
      final int qubitCount) {
    final int matrixSize = 1 << qubitCount;
    for (int column = 0; column < matrixSize; ++column) {
      QubitKet ket = QubitKet.of(column, ComplexCoefficient.ONE);
      for (int axisIndex = 0; axisIndex < term.length(); ++axisIndex) {
        ket = this.applyAxis(ket, term.qubit(axisIndex), term.axis(axisIndex));
      }
      matrix.add(ket.state(), column, coefficient.multiply(ket.phase()));
    }
  }

  private QubitKet applyAxis(final QubitKet input, final int qubit, final PauliAxis axis) {
    final int mask = 1 << qubit;
    if (axis == PauliAxis.X) {
      return QubitKet.of(input.state() ^ mask, input.phase());
    }
    if (axis == PauliAxis.Z) {
      return QubitKet.of(
          input.state(), (input.state() & mask) == 0 ? input.phase() : input.phase().negate());
    }
    if ((input.state() & mask) == 0) {
      return QubitKet.of(
          input.state() | mask, input.phase().multiply(ComplexCoefficient.of(0.0, 1.0)));
    }
    return QubitKet.of(
        input.state() & ~mask, input.phase().multiply(ComplexCoefficient.of(0.0, -1.0)));
  }

  private static final class QubitKet {

    private final int state;
    private final ComplexCoefficient phase;

    private QubitKet(final int state, final ComplexCoefficient phase) {
      this.state = state;
      this.phase = phase;
    }

    private static QubitKet of(final int state, final ComplexCoefficient phase) {
      return new QubitKet(state, phase);
    }

    private int state() {
      return this.state;
    }

    private ComplexCoefficient phase() {
      return this.phase;
    }
  }
}
