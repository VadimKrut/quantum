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

/** Sparse qubit Pauli operator. */
public final class QubitPauliOperator {

  private final QubitPauliTerm[] terms;
  private final double[] realCoefficients;
  private final double[] imaginaryCoefficients;

  private QubitPauliOperator(
      final QubitPauliTerm[] terms,
      final double[] realCoefficients,
      final double[] imaginaryCoefficients) {
    this.terms = terms;
    this.realCoefficients = realCoefficients;
    this.imaginaryCoefficients = imaginaryCoefficients;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static QubitPauliOperator zero() {
    return new QubitPauliOperator(new QubitPauliTerm[0], new double[0], new double[0]);
  }

  public static QubitPauliOperator identity(final ComplexCoefficient coefficient) {
    return QubitPauliOperator.builder().add(QubitPauliTerm.IDENTITY, coefficient).build();
  }

  public int termCount() {
    return this.terms.length;
  }

  public QubitPauliTerm term(final int index) {
    this.requireIndex(index);
    return this.terms[index];
  }

  public ComplexCoefficient coefficient(final int index) {
    this.requireIndex(index);
    return ComplexCoefficient.of(this.realCoefficients[index], this.imaginaryCoefficients[index]);
  }

  public QubitPauliOperator add(final QubitPauliOperator other) {
    if (other == null) {
      throw new IllegalArgumentException("Qubit Pauli operator addend must not be null.");
    }
    final Builder builder = QubitPauliOperator.builder();
    builder.add(this);
    builder.add(other);
    return builder.build();
  }

  public QubitPauliOperator multiply(final QubitPauliOperator other) {
    if (other == null) {
      throw new IllegalArgumentException("Qubit Pauli operator multiplier must not be null.");
    }
    final Builder builder = QubitPauliOperator.builder();
    for (int i = 0; i < this.termCount(); ++i) {
      final ComplexCoefficient leftCoefficient = this.coefficient(i);
      for (int j = 0; j < other.termCount(); ++j) {
        final QubitPauliTerm.Product product = this.terms[i].multiply(other.terms[j]);
        builder.add(
            product.term(),
            leftCoefficient.multiply(other.coefficient(j)).multiply(product.phase()));
      }
    }
    return builder.build();
  }

  public QubitPauliOperator multiply(final ComplexCoefficient coefficient) {
    if (coefficient == null) {
      throw new IllegalArgumentException("Qubit Pauli coefficient multiplier must not be null.");
    }
    final Builder builder = QubitPauliOperator.builder();
    for (int i = 0; i < this.termCount(); ++i) {
      builder.add(this.terms[i], this.coefficient(i).multiply(coefficient));
    }
    return builder.build();
  }

  public int requiredQubitCount() {
    int max = -1;
    for (int i = 0; i < this.terms.length; ++i) {
      final QubitPauliTerm term = this.terms[i];
      for (int j = 0; j < term.length(); ++j) {
        max = Math.max(max, term.qubit(j));
      }
    }
    return max + 1;
  }

  public boolean hermitianWithinTolerance(final double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException("Hermitian tolerance must be finite and non-negative.");
    }
    for (int i = 0; i < this.termCount(); ++i) {
      if (Math.abs(this.imaginaryCoefficients[i]) > tolerance) {
        return false;
      }
    }
    return true;
  }

  private void requireIndex(final int index) {
    if (index < 0 || index >= this.terms.length) {
      throw new IndexOutOfBoundsException("Qubit Pauli operator term index is outside operator.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof QubitPauliOperator)) {
      return false;
    }
    final QubitPauliOperator operator = (QubitPauliOperator) other;
    return Arrays.equals(this.terms, operator.terms)
        && Arrays.equals(this.realCoefficients, operator.realCoefficients)
        && Arrays.equals(this.imaginaryCoefficients, operator.imaginaryCoefficients);
  }

  public int hashCode() {
    int result = Arrays.hashCode(this.terms);
    result = 31 * result + Arrays.hashCode(this.realCoefficients);
    result = 31 * result + Arrays.hashCode(this.imaginaryCoefficients);
    return result;
  }

  /** Mutable accumulator for merging equal Pauli strings. */
  public static final class Builder {

    private QubitPauliTerm[] tableTerms;
    private double[] tableReal;
    private double[] tableImaginary;
    private QubitPauliTerm[] order;
    private int size;
    private int occupied;
    private int orderSize;

    private Builder() {
      this.tableTerms = new QubitPauliTerm[16];
      this.tableReal = new double[16];
      this.tableImaginary = new double[16];
      this.order = new QubitPauliTerm[16];
    }

    public Builder add(final QubitPauliTerm term, final double coefficient) {
      return this.add(term, ComplexCoefficient.real(coefficient));
    }

    public Builder add(final QubitPauliTerm term, final ComplexCoefficient coefficient) {
      if (term == null) {
        throw new IllegalArgumentException("Qubit Pauli term must not be null.");
      }
      if (coefficient == null) {
        throw new IllegalArgumentException("Qubit Pauli coefficient must not be null.");
      }
      if (coefficient.zero()) {
        return this;
      }
      this.ensureCapacity();
      final int slot = this.slot(term);
      if (this.tableTerms[slot] == null) {
        this.tableTerms[slot] = term;
        this.tableReal[slot] = coefficient.real();
        this.tableImaginary[slot] = coefficient.imaginary();
        this.appendOrder(term);
        ++this.size;
        ++this.occupied;
      } else {
        final boolean wasZero = Builder.zero(this.tableReal[slot], this.tableImaginary[slot]);
        this.tableReal[slot] += coefficient.real();
        this.tableImaginary[slot] += coefficient.imaginary();
        final boolean nowZero = Builder.zero(this.tableReal[slot], this.tableImaginary[slot]);
        if (!wasZero && nowZero) {
          --this.size;
        } else if (wasZero && !nowZero) {
          ++this.size;
        }
      }
      return this;
    }

    public Builder add(final QubitPauliOperator operator) {
      if (operator == null) {
        throw new IllegalArgumentException("Qubit Pauli operator must not be null.");
      }
      for (int i = 0; i < operator.termCount(); ++i) {
        this.add(operator.term(i), operator.coefficient(i));
      }
      return this;
    }

    public QubitPauliOperator build() {
      if (this.size == 0) {
        return QubitPauliOperator.zero();
      }
      final QubitPauliTerm[] terms = new QubitPauliTerm[this.size];
      final double[] real = new double[this.size];
      final double[] imaginary = new double[this.size];
      int index = 0;
      for (int i = 0; i < this.orderSize; ++i) {
        final QubitPauliTerm term = this.order[i];
        final int slot = this.slot(term);
        if (Builder.zero(this.tableReal[slot], this.tableImaginary[slot])) {
          continue;
        }
        terms[index] = term;
        real[index] = this.tableReal[slot];
        imaginary[index] = this.tableImaginary[slot];
        ++index;
      }
      return new QubitPauliOperator(terms, real, imaginary);
    }

    private void appendOrder(final QubitPauliTerm term) {
      if (this.orderSize == this.order.length) {
        this.order = Arrays.copyOf(this.order, this.order.length * 2);
      }
      this.order[this.orderSize] = term;
      ++this.orderSize;
    }

    private void ensureCapacity() {
      if ((this.occupied + 1) * 4 < this.tableTerms.length * 3) {
        return;
      }
      final QubitPauliTerm[] previousTerms = this.tableTerms;
      final double[] previousReal = this.tableReal;
      final double[] previousImaginary = this.tableImaginary;
      this.tableTerms = new QubitPauliTerm[previousTerms.length * 2];
      this.tableReal = new double[this.tableTerms.length];
      this.tableImaginary = new double[this.tableTerms.length];
      this.occupied = 0;
      for (int i = 0; i < previousTerms.length; ++i) {
        if (previousTerms[i] == null) {
          continue;
        }
        final int slot = this.slot(previousTerms[i]);
        this.tableTerms[slot] = previousTerms[i];
        this.tableReal[slot] = previousReal[i];
        this.tableImaginary[slot] = previousImaginary[i];
        ++this.occupied;
      }
    }

    private int slot(final QubitPauliTerm term) {
      int slot = term.hashCode() & (this.tableTerms.length - 1);
      while (this.tableTerms[slot] != null && !this.tableTerms[slot].equals(term)) {
        slot = (slot + 1) & (this.tableTerms.length - 1);
      }
      return slot;
    }

    private static boolean zero(final double real, final double imaginary) {
      return Math.abs(real) <= 1.0E-14 && Math.abs(imaginary) <= 1.0E-14;
    }
  }
}
