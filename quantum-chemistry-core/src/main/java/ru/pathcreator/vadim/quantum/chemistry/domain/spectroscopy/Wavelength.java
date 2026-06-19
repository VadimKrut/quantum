/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy;

public final class Wavelength {

  private static final double MIN_NANOMETERS = 0.001;
  private static final double MAX_NANOMETERS = 1.0E9;
  private final double nanometers;

  private Wavelength(final double nanometers) {
    this.nanometers = nanometers;
  }

  public static Wavelength nanometers(final double nanometers) {
    if (!Double.isFinite(nanometers)) {
      throw new IllegalArgumentException("Wavelength must be finite.");
    }
    if (nanometers < MIN_NANOMETERS || nanometers > MAX_NANOMETERS) {
      throw new IllegalArgumentException("Wavelength is outside supported bounds.");
    }
    return new Wavelength(nanometers);
  }

  public double nanometers() {
    return this.nanometers;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Wavelength)) {
      return false;
    }
    final Wavelength wavelength = (Wavelength) other;
    return Double.compare(this.nanometers, wavelength.nanometers) == 0;
  }

  public int hashCode() {
    return Double.hashCode(this.nanometers);
  }
}