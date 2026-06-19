/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.kinetics;

/** Константа скорости реакции в выбранной кинетической размерности. */
public final class RateConstant {

  private final double value;
  private final RateConstantUnit unit;

  private RateConstant(
      final double value,
      final RateConstantUnit unit
  ) {
    this.value = value;
    this.unit = unit;
  }

  public static RateConstant of(
      final double value,
      final RateConstantUnit unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Rate constant must be finite.");
    }
    if (value <= 0.0) {
      throw new IllegalArgumentException("Rate constant must be positive.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Rate constant unit must not be null.");
    }
    return new RateConstant(value, unit);
  }

  public double value() {
    return this.value;
  }

  public RateConstantUnit unit() {
    return this.unit;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RateConstant)) {
      return false;
    }
    final RateConstant constant = (RateConstant) other;
    return Double.compare(this.value, constant.value) == 0 && this.unit == constant.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(this.value);
    result = 31 * result + this.unit.hashCode();
    return result;
  }
}