/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics;

/** Значение энтропии с единицей измерения для термодинамических расчётов. */
public final class EntropyValue {

  private final double value;
  private final EntropyUnit unit;

  private EntropyValue(
      final double value,
      final EntropyUnit unit
  ) {
    this.value = value;
    this.unit = unit;
  }

  public static EntropyValue of(
      final double value,
      final EntropyUnit unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Entropy value must be finite.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Entropy unit must not be null.");
    }
    return new EntropyValue(value, unit);
  }

  public double value() {
    return value;
  }

  public EntropyUnit unit() {
    return unit;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof EntropyValue)) {
      return false;
    }
    final EntropyValue entropy = (EntropyValue) other;
    return Double.compare(value, entropy.value) == 0 && unit == entropy.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(value);
    result = 31 * result + unit.hashCode();
    return result;
  }
}