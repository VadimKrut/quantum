/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application;

import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionLadderAction;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionTerm;

/** Выполняет базовую алгебру фермионных операторов без зависимости от внешних symbolic CAS. */
public final class FermionOperatorAlgebra {

  private static final double DEFAULT_TOLERANCE = 1.0E-10;

  public FermionOperator normalOrder(final FermionOperator operator) {
    if (operator == null) {
      throw new IllegalArgumentException("Fermion operator for normal ordering must not be null.");
    }
    final FermionOperator.Builder builder = FermionOperator.builder();
    for (int i = 0; i < operator.termCount(); ++i) {
      this.normalOrderTerm(operator.term(i).encodedOperators(), operator.coefficient(i), builder);
    }
    return builder.build();
  }

  public FermionOperator commutator(final FermionOperator left, final FermionOperator right) {
    return this.normalOrder(
        left.multiply(right).add(right.multiply(left).multiply(ComplexCoefficient.real(-1.0))));
  }

  public FermionOperator anticommutator(final FermionOperator left, final FermionOperator right) {
    return this.normalOrder(left.multiply(right).add(right.multiply(left)));
  }

  public boolean hermitianWithinTolerance(final FermionOperator operator) {
    return this.hermitianWithinTolerance(operator, DEFAULT_TOLERANCE);
  }

  public boolean hermitianWithinTolerance(final FermionOperator operator, final double tolerance) {
    if (operator == null) {
      throw new IllegalArgumentException(
          "Fermion operator for Hermiticity check must not be null.");
    }
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Fermion Hermiticity tolerance must be finite and non-negative.");
    }
    final FermionOperator delta =
        this.normalOrder(
            operator.add(operator.hermitianConjugate().multiply(ComplexCoefficient.real(-1.0))));
    for (int i = 0; i < delta.termCount(); ++i) {
      final ComplexCoefficient coefficient = delta.coefficient(i);
      if (Math.abs(coefficient.real()) > tolerance
          || Math.abs(coefficient.imaginary()) > tolerance) {
        return false;
      }
    }
    return true;
  }

  private void normalOrderTerm(
      final long[] encodedOperators,
      final ComplexCoefficient coefficient,
      final FermionOperator.Builder builder) {
    if (coefficient.zero()) {
      return;
    }
    for (int i = 0; i + 1 < encodedOperators.length; ++i) {
      final long left = encodedOperators[i];
      final long right = encodedOperators[i + 1];
      if (!this.outOfCanonicalOrder(left, right)) {
        continue;
      }
      if (this.sameLadderOperator(left, right)) {
        return;
      }
      if (this.contractionPossible(left, right)) {
        this.normalOrderTerm(
            FermionOperatorAlgebra.removePair(encodedOperators, i), coefficient, builder);
      }
      this.normalOrderTerm(
          FermionOperatorAlgebra.swapAdjacent(encodedOperators, i),
          coefficient.multiply(-1.0),
          builder);
      return;
    }
    builder.add(FermionTerm.of(encodedOperators), coefficient);
  }

  private boolean outOfCanonicalOrder(final long left, final long right) {
    final FermionLadderAction leftAction = FermionTerm.action(left);
    final FermionLadderAction rightAction = FermionTerm.action(right);
    final int leftSpinOrbital = FermionTerm.spinOrbital(left);
    final int rightSpinOrbital = FermionTerm.spinOrbital(right);
    if (leftAction == rightAction && leftSpinOrbital == rightSpinOrbital) {
      return true;
    }
    if (leftAction == FermionLadderAction.ANNIHILATION
        && rightAction == FermionLadderAction.CREATION) {
      return true;
    }
    if (leftAction == FermionLadderAction.CREATION && rightAction == FermionLadderAction.CREATION) {
      return leftSpinOrbital < rightSpinOrbital;
    }
    if (leftAction == FermionLadderAction.ANNIHILATION
        && rightAction == FermionLadderAction.ANNIHILATION) {
      return leftSpinOrbital > rightSpinOrbital;
    }
    return false;
  }

  private boolean sameLadderOperator(final long left, final long right) {
    return FermionTerm.action(left) == FermionTerm.action(right)
        && FermionTerm.spinOrbital(left) == FermionTerm.spinOrbital(right);
  }

  private boolean contractionPossible(final long left, final long right) {
    return FermionTerm.action(left) == FermionLadderAction.ANNIHILATION
        && FermionTerm.action(right) == FermionLadderAction.CREATION
        && FermionTerm.spinOrbital(left) == FermionTerm.spinOrbital(right);
  }

  private static long[] removePair(final long[] encodedOperators, final int firstIndex) {
    final long[] result = new long[encodedOperators.length - 2];
    int target = 0;
    for (int i = 0; i < encodedOperators.length; ++i) {
      if (i == firstIndex || i == firstIndex + 1) {
        continue;
      }
      result[target] = encodedOperators[i];
      ++target;
    }
    return result;
  }

  private static long[] swapAdjacent(final long[] encodedOperators, final int firstIndex) {
    final long[] result = encodedOperators.clone();
    final long value = result[firstIndex];
    result[firstIndex] = result[firstIndex + 1];
    result[firstIndex + 1] = value;
    return result;
  }
}
