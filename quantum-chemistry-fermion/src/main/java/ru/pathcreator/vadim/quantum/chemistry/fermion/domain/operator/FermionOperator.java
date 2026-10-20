/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator;

import java.util.Arrays;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;

/** Sparse fermionic operator with merging of equal ladder terms. */
public final class FermionOperator {

  private final FermionTerm[] terms;
  private final double[] realCoefficients;
  private final double[] imaginaryCoefficients;

  private FermionOperator(
      final FermionTerm[] terms,
      final double[] realCoefficients,
      final double[] imaginaryCoefficients) {
    this.terms = terms;
    this.realCoefficients = realCoefficients;
    this.imaginaryCoefficients = imaginaryCoefficients;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static FermionOperator zero() {
    return new FermionOperator(new FermionTerm[0], new double[0], new double[0]);
  }

  public int termCount() {
    return this.terms.length;
  }

  public boolean empty() {
    return this.terms.length == 0;
  }

  public FermionTerm term(final int index) {
    this.requireIndex(index);
    return this.terms[index];
  }

  public ComplexCoefficient coefficient(final int index) {
    this.requireIndex(index);
    return ComplexCoefficient.of(this.realCoefficients[index], this.imaginaryCoefficients[index]);
  }

  public FermionOperator add(final FermionOperator other) {
    if (other == null) {
      throw new IllegalArgumentException("Fermion operator addend must not be null.");
    }
    final Builder builder = FermionOperator.builder();
    builder.add(this);
    builder.add(other);
    return builder.build();
  }

  public FermionOperator multiply(final FermionOperator other) {
    if (other == null) {
      throw new IllegalArgumentException("Fermion operator multiplier must not be null.");
    }
    final Builder builder = FermionOperator.builder();
    for (int i = 0; i < this.termCount(); ++i) {
      final ComplexCoefficient leftCoefficient = this.coefficient(i);
      for (int j = 0; j < other.termCount(); ++j) {
        builder.add(
            this.terms[i].append(other.terms[j]), leftCoefficient.multiply(other.coefficient(j)));
      }
    }
    return builder.build();
  }

  public FermionOperator multiply(final ComplexCoefficient coefficient) {
    if (coefficient == null) {
      throw new IllegalArgumentException("Fermion coefficient multiplier must not be null.");
    }
    final Builder builder = FermionOperator.builder();
    for (int i = 0; i < this.termCount(); ++i) {
      builder.add(this.terms[i], this.coefficient(i).multiply(coefficient));
    }
    return builder.build();
  }

  public FermionOperator hermitianConjugate() {
    final Builder builder = FermionOperator.builder();
    for (int i = 0; i < this.termCount(); ++i) {
      builder.add(this.terms[i].hermitianConjugate(), this.coefficient(i).conjugate());
    }
    return builder.build();
  }

  private void requireIndex(final int index) {
    if (index < 0 || index >= this.terms.length) {
      throw new IndexOutOfBoundsException("Fermion operator term index is outside operator.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FermionOperator)) {
      return false;
    }
    final FermionOperator operator = (FermionOperator) other;
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

  /** Mutable accumulator that compresses equal terms before immutable operator creation. */
  public static final class Builder {

    private FermionTerm[] tableTerms;
    private double[] tableReal;
    private double[] tableImaginary;
    private FermionTerm[] order;
    private int size;
    private int occupied;
    private int orderSize;

    private Builder() {
      this.tableTerms = new FermionTerm[16];
      this.tableReal = new double[16];
      this.tableImaginary = new double[16];
      this.order = new FermionTerm[16];
    }

    public Builder add(final FermionTerm term, final double coefficient) {
      return this.add(term, ComplexCoefficient.real(coefficient));
    }

    public Builder add(final FermionTerm term, final ComplexCoefficient coefficient) {
      if (term == null) {
        throw new IllegalArgumentException("Fermion term must not be null.");
      }
      if (coefficient == null) {
        throw new IllegalArgumentException("Fermion coefficient must not be null.");
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

    public Builder add(final FermionOperator operator) {
      if (operator == null) {
        throw new IllegalArgumentException("Fermion operator must not be null.");
      }
      for (int i = 0; i < operator.termCount(); ++i) {
        this.add(operator.term(i), operator.coefficient(i));
      }
      return this;
    }

    public FermionOperator build() {
      if (this.size == 0) {
        return FermionOperator.zero();
      }
      final FermionTerm[] terms = new FermionTerm[this.size];
      final double[] real = new double[this.size];
      final double[] imaginary = new double[this.size];
      int index = 0;
      for (int i = 0; i < this.orderSize; ++i) {
        final FermionTerm term = this.order[i];
        final int slot = this.slot(term);
        if (Builder.zero(this.tableReal[slot], this.tableImaginary[slot])) {
          continue;
        }
        terms[index] = term;
        real[index] = this.tableReal[slot];
        imaginary[index] = this.tableImaginary[slot];
        ++index;
      }
      return new FermionOperator(terms, real, imaginary);
    }

    private void appendOrder(final FermionTerm term) {
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
      final FermionTerm[] previousTerms = this.tableTerms;
      final double[] previousReal = this.tableReal;
      final double[] previousImaginary = this.tableImaginary;
      this.tableTerms = new FermionTerm[previousTerms.length * 2];
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

    private int slot(final FermionTerm term) {
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
