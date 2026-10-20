/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient;

import java.util.Objects;

/** Комплексный коэффициент operator term без зависимости от внешних numeric библиотек. */
public final class ComplexCoefficient {

  public static final ComplexCoefficient ZERO = new ComplexCoefficient(0.0, 0.0);
  public static final ComplexCoefficient ONE = new ComplexCoefficient(1.0, 0.0);

  private static final double ZERO_TOLERANCE = 1.0E-14;

  private final double real;
  private final double imaginary;

  private ComplexCoefficient(final double real, final double imaginary) {
    this.real = real;
    this.imaginary = imaginary;
  }

  public static ComplexCoefficient of(final double real, final double imaginary) {
    if (!Double.isFinite(real) || !Double.isFinite(imaginary)) {
      throw new IllegalArgumentException("Complex coefficient parts must be finite.");
    }
    if (Math.abs(real) <= ZERO_TOLERANCE && Math.abs(imaginary) <= ZERO_TOLERANCE) {
      return ZERO;
    }
    if (imaginary == 0.0 && real == 1.0) {
      return ONE;
    }
    return new ComplexCoefficient(real, imaginary);
  }

  public static ComplexCoefficient real(final double value) {
    return ComplexCoefficient.of(value, 0.0);
  }

  public double real() {
    return this.real;
  }

  public double imaginary() {
    return this.imaginary;
  }

  public boolean zero() {
    return Math.abs(this.real) <= ZERO_TOLERANCE && Math.abs(this.imaginary) <= ZERO_TOLERANCE;
  }

  public ComplexCoefficient add(final ComplexCoefficient other) {
    if (other == null) {
      throw new IllegalArgumentException("Complex coefficient addend must not be null.");
    }
    return ComplexCoefficient.of(this.real + other.real, this.imaginary + other.imaginary);
  }

  public ComplexCoefficient negate() {
    return ComplexCoefficient.of(-this.real, -this.imaginary);
  }

  public ComplexCoefficient multiply(final ComplexCoefficient other) {
    if (other == null) {
      throw new IllegalArgumentException("Complex coefficient multiplier must not be null.");
    }
    return ComplexCoefficient.of(
        this.real * other.real - this.imaginary * other.imaginary,
        this.real * other.imaginary + this.imaginary * other.real);
  }

  public ComplexCoefficient multiply(final double multiplier) {
    if (!Double.isFinite(multiplier)) {
      throw new IllegalArgumentException("Complex coefficient multiplier must be finite.");
    }
    return ComplexCoefficient.of(this.real * multiplier, this.imaginary * multiplier);
  }

  public ComplexCoefficient conjugate() {
    return ComplexCoefficient.of(this.real, -this.imaginary);
  }

  public boolean effectivelyReal() {
    return Math.abs(this.imaginary) <= ZERO_TOLERANCE;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ComplexCoefficient)) {
      return false;
    }
    final ComplexCoefficient coefficient = (ComplexCoefficient) other;
    return Double.compare(this.real, coefficient.real) == 0
        && Double.compare(this.imaginary, coefficient.imaginary) == 0;
  }

  public int hashCode() {
    return Objects.hash(this.real, this.imaginary);
  }

  public String toString() {
    return this.real + (this.imaginary < 0.0 ? " - " : " + ") + Math.abs(this.imaginary) + "i";
  }
}
