/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.unit;

/** Температура реакционной среды или расчетной модели с проверкой абсолютного нуля. */
public final class Temperature {

  private static final double ABSOLUTE_ZERO_CELSIUS = -273.15;
  private static final double CELSIUS_TO_KELVIN_SHIFT = 273.15;

  private final double value;
  private final TemperatureUnit unit;

  private Temperature(final double value, final TemperatureUnit unit) {
    this.value = value;
    this.unit = unit;
  }

  public static Temperature of(final double value, final TemperatureUnit unit) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Temperature must be finite.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Temperature unit must not be null.");
    }
    if (unit == TemperatureUnit.KELVIN && value < 0.0) {
      throw new IllegalArgumentException("Kelvin temperature must not be below absolute zero.");
    }
    if (unit == TemperatureUnit.CELSIUS && value < ABSOLUTE_ZERO_CELSIUS) {
      throw new IllegalArgumentException("Celsius temperature must not be below absolute zero.");
    }
    return new Temperature(value, unit);
  }

  public double value() {
    return this.value;
  }

  public TemperatureUnit unit() {
    return this.unit;
  }

  public double kelvin() {
    if (this.unit == TemperatureUnit.CELSIUS) {
      return this.value + CELSIUS_TO_KELVIN_SHIFT;
    }
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Temperature)) {
      return false;
    }
    final Temperature temperature = (Temperature) other;
    return Double.compare(this.value, temperature.value) == 0 && this.unit == temperature.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(this.value);
    result = 31 * result + this.unit.hashCode();
    return result;
  }
}
