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

/**
 * Упорядоченная строка fermionic ladder operators.
 */
public final class FermionTerm {

  public static final FermionTerm IDENTITY = new FermionTerm(new long[0], 1);

  private final long[] encodedOperators;
  private final int hashCode;

  private FermionTerm(final long[] encodedOperators, final int hashCode) {
    this.encodedOperators = encodedOperators;
    this.hashCode = hashCode;
  }

  public static FermionTerm of(final long... encodedOperators) {
    if (encodedOperators == null || encodedOperators.length == 0) {
      return IDENTITY;
    }
    final long[] copy = Arrays.copyOf(encodedOperators, encodedOperators.length);
    for (int i = 0; i < copy.length; ++i) {
      if (copy[i] < 0L) {
        throw new IllegalArgumentException("Encoded fermion operator must not be negative.");
      }
    }
    return new FermionTerm(copy, Arrays.hashCode(copy));
  }

  public static long creation(final int spinOrbital) {
    return FermionTerm.encode(spinOrbital, FermionLadderAction.CREATION);
  }

  public static long annihilation(final int spinOrbital) {
    return FermionTerm.encode(spinOrbital, FermionLadderAction.ANNIHILATION);
  }

  public static long encode(final int spinOrbital, final FermionLadderAction action) {
    if (spinOrbital < 0) {
      throw new IllegalArgumentException("Spin orbital index must not be negative.");
    }
    if (action == null) {
      throw new IllegalArgumentException("Fermion ladder action must not be null.");
    }
    return ((long) spinOrbital << 1) | (action == FermionLadderAction.CREATION ? 1L : 0L);
  }

  public static int spinOrbital(final long encodedOperator) {
    return Math.toIntExact(encodedOperator >>> 1);
  }

  public static FermionLadderAction action(final long encodedOperator) {
    return (encodedOperator & 1L) == 1L
        ? FermionLadderAction.CREATION
        : FermionLadderAction.ANNIHILATION;
  }

  public int length() {
    return this.encodedOperators.length;
  }

  public boolean identity() {
    return this.encodedOperators.length == 0;
  }

  public long operator(final int index) {
    if (index < 0 || index >= this.encodedOperators.length) {
      throw new IndexOutOfBoundsException("Fermion operator index is outside term.");
    }
    return this.encodedOperators[index];
  }

  public long[] encodedOperators() {
    return Arrays.copyOf(this.encodedOperators, this.encodedOperators.length);
  }

  public FermionTerm append(final FermionTerm other) {
    if (other == null) {
      throw new IllegalArgumentException("Fermion term to append must not be null.");
    }
    if (this.identity()) {
      return other;
    }
    if (other.identity()) {
      return this;
    }
    final long[] result =
        Arrays.copyOf(this.encodedOperators, this.encodedOperators.length + other.length());
    for (int i = 0; i < other.length(); ++i) {
      result[this.encodedOperators.length + i] = other.operator(i);
    }
    return FermionTerm.of(result);
  }

  public FermionTerm hermitianConjugate() {
    if (this.identity()) {
      return this;
    }
    final long[] result = new long[this.encodedOperators.length];
    for (int i = 0; i < this.encodedOperators.length; ++i) {
      final long encodedOperator = this.encodedOperators[this.encodedOperators.length - 1 - i];
      final FermionLadderAction action = FermionTerm.action(encodedOperator);
      result[i] =
          FermionTerm.encode(
              FermionTerm.spinOrbital(encodedOperator),
              action == FermionLadderAction.CREATION
                  ? FermionLadderAction.ANNIHILATION
                  : FermionLadderAction.CREATION);
    }
    return FermionTerm.of(result);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FermionTerm)) {
      return false;
    }
    final FermionTerm term = (FermionTerm) other;
    return Arrays.equals(this.encodedOperators, term.encodedOperators);
  }

  public int hashCode() {
    return this.hashCode;
  }

  public String toString() {
    if (this.identity()) {
      return "I";
    }
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < this.encodedOperators.length; ++i) {
      if (i > 0) {
        builder.append(' ');
      }
      final long encodedOperator = this.encodedOperators[i];
      builder.append(FermionTerm.spinOrbital(encodedOperator));
      builder.append(
          FermionTerm.action(encodedOperator) == FermionLadderAction.CREATION ? '^' : ' ');
    }
    return builder.toString();
  }
}
