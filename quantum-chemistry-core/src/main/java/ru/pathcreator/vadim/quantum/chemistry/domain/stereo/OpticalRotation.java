/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.stereo;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

public final class OpticalRotation {

  public static final OpticalRotation UNKNOWN =
      new OpticalRotation(OpticalRotationDirection.UNKNOWN, null, null, null);
  private static final double MIN_WAVELENGTH_NANOMETERS = 1.0;
  private static final double MAX_WAVELENGTH_NANOMETERS = 1000000.0;
  private final OpticalRotationDirection direction;
  private final Double degrees;
  private final Temperature temperature;
  private final Double wavelengthNanometers;

  private OpticalRotation(
      final OpticalRotationDirection direction,
      final Double degrees,
      final Temperature temperature,
      final Double wavelengthNanometers) {
    this.direction = direction;
    this.degrees = degrees;
    this.temperature = temperature;
    this.wavelengthNanometers = wavelengthNanometers;
  }

  public static OpticalRotation of(
      final OpticalRotationDirection direction,
      final Double degrees,
      final Temperature temperature,
      final Double wavelengthNanometers) {
    if (direction == null) {
      throw new IllegalArgumentException("Optical rotation direction must not be null.");
    }
    if (direction == OpticalRotationDirection.UNKNOWN) {
      if (degrees != null || temperature != null || wavelengthNanometers != null) {
        throw new IllegalArgumentException(
            "Unknown optical rotation must not contain measured values.");
      }
      return UNKNOWN;
    }
    final Double checkedDegrees = OpticalRotation.requireDegrees(direction, degrees);
    final Double checkedWavelength = OpticalRotation.requireWavelength(wavelengthNanometers);
    return new OpticalRotation(direction, checkedDegrees, temperature, checkedWavelength);
  }

  public OpticalRotationDirection direction() {
    return this.direction;
  }

  public Double degrees() {
    return this.degrees;
  }

  public boolean hasDegrees() {
    return this.degrees != null;
  }

  public Temperature temperature() {
    return this.temperature;
  }

  public boolean hasTemperature() {
    return this.temperature != null;
  }

  public Double wavelengthNanometers() {
    return this.wavelengthNanometers;
  }

  public boolean hasWavelength() {
    return this.wavelengthNanometers != null;
  }

  private static Double requireDegrees(
      final OpticalRotationDirection direction,
      final Double degrees
  ) {
    if (degrees == null) {
      if (direction == OpticalRotationDirection.NONE) {
        return 0.0;
      }
      throw new IllegalArgumentException(
          "Non-zero optical rotation direction requires degree value.");
    }
    if (!Double.isFinite(degrees)) {
      throw new IllegalArgumentException("Optical rotation degrees must be finite.");
    }
    if (direction == OpticalRotationDirection.DEXTROROTATORY && degrees <= 0.0) {
      throw new IllegalArgumentException(
          "Dextrorotatory optical rotation must have positive degrees.");
    }
    if (direction == OpticalRotationDirection.LEVOROTATORY && degrees >= 0.0) {
      throw new IllegalArgumentException(
          "Levorotatory optical rotation must have negative degrees.");
    }
    if (direction == OpticalRotationDirection.NONE && Double.compare(degrees, 0.0) != 0) {
      throw new IllegalArgumentException("No optical rotation must have zero degrees.");
    }
    return degrees;
  }

  private static Double requireWavelength(final Double wavelengthNanometers) {
    if (wavelengthNanometers == null) {
      return null;
    }
    if (!Double.isFinite(wavelengthNanometers)) {
      throw new IllegalArgumentException("Optical rotation wavelength must be finite.");
    }
    if (wavelengthNanometers < MIN_WAVELENGTH_NANOMETERS
        || wavelengthNanometers > MAX_WAVELENGTH_NANOMETERS) {
      throw new IllegalArgumentException(
          "Optical rotation wavelength must be between 1 and 1000000 nm.");
    }
    return wavelengthNanometers;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OpticalRotation)) {
      return false;
    }
    final OpticalRotation rotation = (OpticalRotation) other;
    return this.direction == rotation.direction
        && Objects.equals(this.degrees, rotation.degrees)
        && Objects.equals(this.temperature, rotation.temperature)
        && Objects.equals(this.wavelengthNanometers, rotation.wavelengthNanometers);
  }

  public int hashCode() {
    return Objects.hash(
        new Object[] {this.direction, this.degrees, this.temperature, this.wavelengthNanometers});
  }
}