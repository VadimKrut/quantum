/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.kinetics;

/** Наблюдаемая скорость реакции в выбранной единице измерения. */
public final class ReactionRateValue {

  private final double value;
  private final ReactionRateUnit unit;

  private ReactionRateValue(
      final double value,
      final ReactionRateUnit unit
  ) {
    this.value = value;
    this.unit = unit;
  }

  public static ReactionRateValue of(
      final double value,
      final ReactionRateUnit unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Reaction rate must be finite.");
    }
    if (value <= 0.0) {
      throw new IllegalArgumentException("Reaction rate must be positive.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Reaction rate unit must not be null.");
    }
    return new ReactionRateValue(value, unit);
  }

  public double value() {
    return this.value;
  }

  public ReactionRateUnit unit() {
    return this.unit;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionRateValue)) {
      return false;
    }
    final ReactionRateValue rate = (ReactionRateValue) other;
    return Double.compare(this.value, rate.value) == 0 && this.unit == rate.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(this.value);
    result = 31 * result + this.unit.hashCode();
    return result;
  }
}