/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.geometry;

/** Значение длины с единицей измерения и конвертацией через ангстремы. */
public final class LengthValue {

  private final double value;
  private final LengthUnit unit;

  private LengthValue(
      final double value,
      final LengthUnit unit
  ) {
    this.value = value;
    this.unit = unit;
  }

  public static LengthValue of(
      final double value,
      final LengthUnit unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Length value must be finite.");
    }
    if (value < 0.0) {
      throw new IllegalArgumentException("Length value must not be negative.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Length unit must not be null.");
    }
    return new LengthValue(value, unit);
  }

  public double value() {
    return this.value;
  }

  public LengthUnit unit() {
    return this.unit;
  }

  public double in(final LengthUnit targetUnit) {
    if (targetUnit == null) {
      throw new IllegalArgumentException("Target length unit must not be null.");
    }
    return targetUnit.fromAngstrom(this.unit.toAngstrom(this.value));
  }

  public double angstroms() {
    return this.in(LengthUnit.ANGSTROM);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof LengthValue)) {
      return false;
    }
    final LengthValue lengthValue = (LengthValue) other;
    return Double.compare(this.value, lengthValue.value) == 0 && this.unit == lengthValue.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(this.value);
    result = 31 * result + this.unit.hashCode();
    return result;
  }
}