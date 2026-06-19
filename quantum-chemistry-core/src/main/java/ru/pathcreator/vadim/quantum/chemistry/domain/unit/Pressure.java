/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.unit;

/** Давление реакционной среды с физически допустимой неотрицательной величиной. */
public final class Pressure {

  private final double value;
  private final PressureUnit unit;

  private Pressure(
      final double value,
      final PressureUnit unit
  ) {
    this.value = value;
    this.unit = unit;
  }

  public static Pressure of(
      final double value,
      final PressureUnit unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Pressure must be finite.");
    }
    if (value < 0.0) {
      throw new IllegalArgumentException("Pressure must not be negative.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Pressure unit must not be null.");
    }
    return new Pressure(value, unit);
  }

  public double value() {
    return value;
  }

  public PressureUnit unit() {
    return unit;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Pressure)) {
      return false;
    }
    final Pressure pressure = (Pressure) other;
    return Double.compare(value, pressure.value) == 0 && unit == pressure.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(value);
    result = 31 * result + unit.hashCode();
    return result;
  }
}