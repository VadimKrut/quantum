/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.unit;

/** Молярная концентрация компонента реакционной среды. */
public final class MolarConcentration {

  private final double value;
  private final MolarConcentrationUnit unit;

  private MolarConcentration(
      final double value,
      final MolarConcentrationUnit unit
  ) {
    this.value = value;
    this.unit = unit;
  }

  public static MolarConcentration of(
      final double value,
      final MolarConcentrationUnit unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Molar concentration must be finite.");
    }
    if (value <= 0.0) {
      throw new IllegalArgumentException("Molar concentration must be positive.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Molar concentration unit must not be null.");
    }
    return new MolarConcentration(value, unit);
  }

  public double value() {
    return value;
  }

  public MolarConcentrationUnit unit() {
    return unit;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolarConcentration)) {
      return false;
    }
    final MolarConcentration concentration = (MolarConcentration) other;
    return Double.compare(value, concentration.value) == 0 && unit == concentration.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(value);
    result = 31 * result + unit.hashCode();
    return result;
  }
}