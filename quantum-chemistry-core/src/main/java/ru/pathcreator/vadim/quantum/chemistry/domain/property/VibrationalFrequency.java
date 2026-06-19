/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.property;

/** Частота нормальной моды с признаком real/imaginary. */
public final class VibrationalFrequency {

  private final double magnitude;
  private final VibrationalFrequencyUnit unit;
  private final VibrationalFrequencyKind kind;

  private VibrationalFrequency(
      final double magnitude,
      final VibrationalFrequencyUnit unit,
      final VibrationalFrequencyKind kind) {
    this.magnitude = magnitude;
    this.unit = unit;
    this.kind = kind;
  }

  public static VibrationalFrequency of(
      final double magnitude,
      final VibrationalFrequencyUnit unit,
      final VibrationalFrequencyKind kind) {
    if (!Double.isFinite(magnitude)) {
      throw new IllegalArgumentException("Vibrational frequency must be finite.");
    }
    if (magnitude <= 0.0) {
      throw new IllegalArgumentException("Vibrational frequency magnitude must be positive.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Vibrational frequency unit must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Vibrational frequency kind must not be null.");
    }
    return new VibrationalFrequency(magnitude, unit, kind);
  }

  public double magnitude() {
    return this.magnitude;
  }

  public VibrationalFrequencyUnit unit() {
    return this.unit;
  }

  public VibrationalFrequencyKind kind() {
    return this.kind;
  }

  public boolean imaginary() {
    return this.kind == VibrationalFrequencyKind.IMAGINARY;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof VibrationalFrequency)) {
      return false;
    }
    final VibrationalFrequency frequency = (VibrationalFrequency) other;
    return Double.compare(this.magnitude, frequency.magnitude) == 0
        && this.unit == frequency.unit
        && this.kind == frequency.kind;
  }

  public int hashCode() {
    int result = Double.hashCode(this.magnitude);
    result = 31 * result + this.unit.hashCode();
    result = 31 * result + this.kind.hashCode();
    return result;
  }
}