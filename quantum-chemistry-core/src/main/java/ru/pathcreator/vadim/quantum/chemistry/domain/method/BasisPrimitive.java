/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

/** Один primitive Gaussian внутри contracted basis shell. */
public final class BasisPrimitive {

  private final double exponent;
  private final double coefficient;

  private BasisPrimitive(final double exponent, final double coefficient) {
    this.exponent = exponent;
    this.coefficient = coefficient;
  }

  public static BasisPrimitive of(final double exponent, final double coefficient) {
    if (!Double.isFinite(exponent)) {
      throw new IllegalArgumentException("Basis primitive exponent must be finite.");
    }
    if (exponent <= 0.0) {
      throw new IllegalArgumentException("Basis primitive exponent must be positive.");
    }
    if (!Double.isFinite(coefficient)) {
      throw new IllegalArgumentException("Basis primitive coefficient must be finite.");
    }
    if (coefficient == 0.0) {
      throw new IllegalArgumentException("Basis primitive coefficient must not be zero.");
    }
    return new BasisPrimitive(exponent, coefficient);
  }

  public double exponent() {
    return this.exponent;
  }

  public double coefficient() {
    return this.coefficient;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BasisPrimitive)) {
      return false;
    }
    final BasisPrimitive primitive = (BasisPrimitive) other;
    return Double.compare(this.exponent, primitive.exponent) == 0
        && Double.compare(this.coefficient, primitive.coefficient) == 0;
  }

  public int hashCode() {
    int result = Double.hashCode(this.exponent);
    result = 31 * result + Double.hashCode(this.coefficient);
    return result;
  }
}
