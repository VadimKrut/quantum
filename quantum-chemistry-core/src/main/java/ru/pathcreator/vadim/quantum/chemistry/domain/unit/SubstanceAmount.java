/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.unit;

/** Количество вещества для условий реакции, загрузок и расчётов выхода. */
public final class SubstanceAmount {

  private final double value;
  private final SubstanceAmountUnit unit;

  private SubstanceAmount(final double value, final SubstanceAmountUnit unit) {
    this.value = value;
    this.unit = unit;
  }

  public static SubstanceAmount of(final double value, final SubstanceAmountUnit unit) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Substance amount must be finite.");
    }
    if (value <= 0.0) {
      throw new IllegalArgumentException("Substance amount must be positive.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Substance amount unit must not be null.");
    }
    return new SubstanceAmount(value, unit);
  }

  public double value() {
    return value;
  }

  public SubstanceAmountUnit unit() {
    return unit;
  }

  public boolean molarQuantity() {
    return this.unit == SubstanceAmountUnit.MOLE
        || this.unit == SubstanceAmountUnit.MILLIMOLE
        || this.unit == SubstanceAmountUnit.MICROMOLE;
  }

  public boolean massQuantity() {
    return this.unit == SubstanceAmountUnit.KILOGRAM
        || this.unit == SubstanceAmountUnit.GRAM
        || this.unit == SubstanceAmountUnit.MILLIGRAM
        || this.unit == SubstanceAmountUnit.MICROGRAM;
  }

  public double moleValue() {
    if (!this.molarQuantity()) {
      throw new IllegalStateException("Substance amount is not a molar quantity.");
    }
    if (this.unit == SubstanceAmountUnit.MILLIMOLE) {
      return this.value / 1000.0;
    }
    if (this.unit == SubstanceAmountUnit.MICROMOLE) {
      return this.value / 1000000.0;
    }
    return this.value;
  }

  public double gramValue() {
    if (!this.massQuantity()) {
      throw new IllegalStateException("Substance amount is not a mass quantity.");
    }
    if (this.unit == SubstanceAmountUnit.KILOGRAM) {
      return this.value * 1000.0;
    }
    if (this.unit == SubstanceAmountUnit.MILLIGRAM) {
      return this.value / 1000.0;
    }
    if (this.unit == SubstanceAmountUnit.MICROGRAM) {
      return this.value / 1000000.0;
    }
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SubstanceAmount)) {
      return false;
    }
    final SubstanceAmount amount = (SubstanceAmount) other;
    return Double.compare(value, amount.value) == 0 && unit == amount.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(value);
    result = 31 * result + unit.hashCode();
    return result;
  }
}
