/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy;

public final class OscillatorStrength {

  private final double value;

  private OscillatorStrength(final double value) {
    this.value = value;
  }

  public static OscillatorStrength of(final double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Oscillator strength must be finite.");
    }
    if (value < 0.0) {
      throw new IllegalArgumentException("Oscillator strength must be non-negative.");
    }
    return new OscillatorStrength(value);
  }

  public double value() {
    return this.value;
  }

  public boolean opticallyAllowed() {
    return this.value > 0.0;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OscillatorStrength)) {
      return false;
    }
    final OscillatorStrength strength = (OscillatorStrength) other;
    return Double.compare(this.value, strength.value) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.value);
  }
}