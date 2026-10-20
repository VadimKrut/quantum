/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli;

import java.util.Arrays;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;

/** Canonical Pauli string: qubit indexes sorted, identity axes removed. */
public final class QubitPauliTerm {

  public static final QubitPauliTerm IDENTITY = new QubitPauliTerm(new int[0], new PauliAxis[0], 1);

  private final int[] qubits;
  private final PauliAxis[] axes;
  private final int hashCode;

  private QubitPauliTerm(final int[] qubits, final PauliAxis[] axes, final int hashCode) {
    this.qubits = qubits;
    this.axes = axes;
    this.hashCode = hashCode;
  }

  public static QubitPauliTerm of(final int qubit, final PauliAxis axis) {
    return QubitPauliTerm.of(new int[] {qubit}, new PauliAxis[] {axis});
  }

  public static QubitPauliTerm of(final int[] qubits, final PauliAxis[] axes) {
    if (qubits == null || axes == null) {
      throw new IllegalArgumentException("Pauli term arrays must not be null.");
    }
    if (qubits.length != axes.length) {
      throw new IllegalArgumentException("Pauli qubit and axis arrays must have equal length.");
    }
    if (qubits.length == 0) {
      return IDENTITY;
    }
    final int[] qubitCopy = Arrays.copyOf(qubits, qubits.length);
    final PauliAxis[] axisCopy = Arrays.copyOf(axes, axes.length);
    QubitPauliTerm.sort(qubitCopy, axisCopy);
    int compactLength = 0;
    int previousQubit = -1;
    for (int i = 0; i < qubitCopy.length; ++i) {
      if (qubitCopy[i] < 0) {
        throw new IllegalArgumentException("Pauli qubit index must not be negative.");
      }
      if (axisCopy[i] == null) {
        throw new IllegalArgumentException("Pauli axis must not be null.");
      }
      if (qubitCopy[i] == previousQubit) {
        throw new IllegalArgumentException("Pauli term must not contain duplicate qubit indexes.");
      }
      previousQubit = qubitCopy[i];
      qubitCopy[compactLength] = qubitCopy[i];
      axisCopy[compactLength] = axisCopy[i];
      ++compactLength;
    }
    final int[] compactQubits = Arrays.copyOf(qubitCopy, compactLength);
    final PauliAxis[] compactAxes = Arrays.copyOf(axisCopy, compactLength);
    return new QubitPauliTerm(
        compactQubits,
        compactAxes,
        31 * Arrays.hashCode(compactQubits) + Arrays.hashCode(compactAxes));
  }

  public int length() {
    return this.qubits.length;
  }

  public boolean identity() {
    return this.qubits.length == 0;
  }

  public int qubit(final int index) {
    this.requireIndex(index);
    return this.qubits[index];
  }

  public PauliAxis axis(final int index) {
    this.requireIndex(index);
    return this.axes[index];
  }

  public Product multiply(final QubitPauliTerm other) {
    if (other == null) {
      throw new IllegalArgumentException("Pauli term multiplier must not be null.");
    }
    final int[] resultQubits = new int[this.length() + other.length()];
    final PauliAxis[] resultAxes = new PauliAxis[resultQubits.length];
    ComplexCoefficient phase = ComplexCoefficient.ONE;
    int left = 0;
    int right = 0;
    int result = 0;
    while (left < this.length() || right < other.length()) {
      if (right >= other.length()
          || left < this.length() && this.qubits[left] < other.qubits[right]) {
        resultQubits[result] = this.qubits[left];
        resultAxes[result] = this.axes[left];
        ++left;
        ++result;
      } else if (left >= this.length() || other.qubits[right] < this.qubits[left]) {
        resultQubits[result] = other.qubits[right];
        resultAxes[result] = other.axes[right];
        ++right;
        ++result;
      } else {
        final AxisProduct axisProduct =
            QubitPauliTerm.multiplyAxis(this.axes[left], other.axes[right]);
        phase = phase.multiply(axisProduct.phase);
        if (axisProduct.axis != null) {
          resultQubits[result] = this.qubits[left];
          resultAxes[result] = axisProduct.axis;
          ++result;
        }
        ++left;
        ++right;
      }
    }
    return new Product(
        QubitPauliTerm.of(Arrays.copyOf(resultQubits, result), Arrays.copyOf(resultAxes, result)),
        phase);
  }

  private static AxisProduct multiplyAxis(final PauliAxis left, final PauliAxis right) {
    if (left == right) {
      return new AxisProduct(null, ComplexCoefficient.ONE);
    }
    if (left == PauliAxis.X && right == PauliAxis.Y) {
      return new AxisProduct(PauliAxis.Z, ComplexCoefficient.of(0.0, 1.0));
    }
    if (left == PauliAxis.Y && right == PauliAxis.X) {
      return new AxisProduct(PauliAxis.Z, ComplexCoefficient.of(0.0, -1.0));
    }
    if (left == PauliAxis.Y && right == PauliAxis.Z) {
      return new AxisProduct(PauliAxis.X, ComplexCoefficient.of(0.0, 1.0));
    }
    if (left == PauliAxis.Z && right == PauliAxis.Y) {
      return new AxisProduct(PauliAxis.X, ComplexCoefficient.of(0.0, -1.0));
    }
    if (left == PauliAxis.Z && right == PauliAxis.X) {
      return new AxisProduct(PauliAxis.Y, ComplexCoefficient.of(0.0, 1.0));
    }
    return new AxisProduct(PauliAxis.Y, ComplexCoefficient.of(0.0, -1.0));
  }

  private static void sort(final int[] qubits, final PauliAxis[] axes) {
    for (int i = 1; i < qubits.length; ++i) {
      final int qubit = qubits[i];
      final PauliAxis axis = axes[i];
      int j = i - 1;
      while (j >= 0 && qubits[j] > qubit) {
        qubits[j + 1] = qubits[j];
        axes[j + 1] = axes[j];
        --j;
      }
      qubits[j + 1] = qubit;
      axes[j + 1] = axis;
    }
  }

  private void requireIndex(final int index) {
    if (index < 0 || index >= this.qubits.length) {
      throw new IndexOutOfBoundsException("Pauli term index is outside term.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof QubitPauliTerm)) {
      return false;
    }
    final QubitPauliTerm term = (QubitPauliTerm) other;
    return Arrays.equals(this.qubits, term.qubits) && Arrays.equals(this.axes, term.axes);
  }

  public int hashCode() {
    return this.hashCode;
  }

  public String toString() {
    if (this.identity()) {
      return "I";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < this.qubits.length; ++i) {
      if (i > 0) {
        builder.append(' ');
      }
      builder.append(this.axes[i]).append(this.qubits[i]);
    }
    return builder.toString();
  }

  private static final class AxisProduct {

    private final PauliAxis axis;
    private final ComplexCoefficient phase;

    private AxisProduct(final PauliAxis axis, final ComplexCoefficient phase) {
      this.axis = axis;
      this.phase = phase;
    }
  }

  /**
   * Результат умножения Pauli strings с фазой от некоммутирующих осей.
   */
  public static final class Product {

    private final QubitPauliTerm term;
    private final ComplexCoefficient phase;

    private Product(final QubitPauliTerm term, final ComplexCoefficient phase) {
      this.term = term;
      this.phase = phase;
    }

    public QubitPauliTerm term() {
      return this.term;
    }

    public ComplexCoefficient phase() {
      return this.phase;
    }
  }
}
