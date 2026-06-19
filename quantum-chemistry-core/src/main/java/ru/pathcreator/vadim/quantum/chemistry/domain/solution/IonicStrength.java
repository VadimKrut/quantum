/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.solution;

import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;

/** Ионная сила раствора в единице молярной концентрации. */
public final class IonicStrength {

  public static final IonicStrength ZERO =
      new IonicStrength(0.0, MolarConcentrationUnit.MOLE_PER_LITER);
  private final double value;
  private final MolarConcentrationUnit unit;

  private IonicStrength(
      final double value,
      final MolarConcentrationUnit unit
  ) {
    this.value = value;
    this.unit = unit;
  }

  public static IonicStrength of(
      final double value,
      final MolarConcentrationUnit unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Ionic strength must be finite.");
    }
    if (value < 0.0) {
      throw new IllegalArgumentException("Ionic strength must not be negative.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Ionic strength unit must not be null.");
    }
    if (Double.compare(value, 0.0) == 0 && unit == MolarConcentrationUnit.MOLE_PER_LITER) {
      return ZERO;
    }
    return new IonicStrength(value, unit);
  }

  public double value() {
    return this.value;
  }

  public MolarConcentrationUnit unit() {
    return this.unit;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof IonicStrength)) {
      return false;
    }
    final IonicStrength strength = (IonicStrength) other;
    return Double.compare(this.value, strength.value) == 0 && this.unit == strength.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(this.value);
    result = 31 * result + this.unit.hashCode();
    return result;
  }
}