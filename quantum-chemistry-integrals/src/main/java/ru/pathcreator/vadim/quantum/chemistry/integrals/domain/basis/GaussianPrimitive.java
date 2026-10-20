/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis;

/**
 * Primitive Gaussian coefficient in a contracted atomic basis function.
 */
public final class GaussianPrimitive {

  private final double exponent;
  private final double coefficient;

  private GaussianPrimitive(final double exponent, final double coefficient) {
    this.exponent = exponent;
    this.coefficient = coefficient;
  }

  public static GaussianPrimitive of(final double exponent, final double coefficient) {
    if (!Double.isFinite(exponent) || exponent <= 0.0) {
      throw new IllegalArgumentException(
          "Gaussian primitive exponent must be finite and positive.");
    }
    if (!Double.isFinite(coefficient)) {
      throw new IllegalArgumentException("Gaussian primitive coefficient must be finite.");
    }
    return new GaussianPrimitive(exponent, coefficient);
  }

  public double exponent() {
    return this.exponent;
  }

  public double coefficient() {
    return this.coefficient;
  }
}
