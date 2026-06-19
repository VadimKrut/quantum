/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.unit;

/** Значение энергии с единицей измерения, используемое в химических расчётах и профилях. */
public final class EnergyValue {

  private final double value;
  private final EnergyUnit unit;

  private EnergyValue(
      final double value,
      final EnergyUnit unit
  ) {
    this.value = value;
    this.unit = unit;
  }

  public static EnergyValue of(
      final double value,
      final EnergyUnit unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Energy value must be finite.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Energy unit must not be null.");
    }
    return new EnergyValue(value, unit);
  }

  public double value() {
    return value;
  }

  public EnergyUnit unit() {
    return unit;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof EnergyValue)) {
      return false;
    }
    final EnergyValue energy = (EnergyValue) other;
    return Double.compare(value, energy.value) == 0 && unit == energy.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(value);
    result = 31 * result + unit.hashCode();
    return result;
  }
}