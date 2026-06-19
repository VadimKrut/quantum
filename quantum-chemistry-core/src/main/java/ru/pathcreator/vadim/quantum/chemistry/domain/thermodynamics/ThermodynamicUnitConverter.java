/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics;

import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;

public final class ThermodynamicUnitConverter {

  public static final double HARTREE_TO_KILOJOULE_PER_MOLE = 2625.4996394799;
  public static final double ELECTRON_VOLT_TO_KILOJOULE_PER_MOLE = 96.4853321233;
  public static final double KILOCALORIE_TO_KILOJOULE = 4.184;
  public static final double CALORIE_TO_JOULE = 4.184;

  private ThermodynamicUnitConverter() {}

  public static double energyKiloJoulePerMole(final EnergyValue energy) {
    if (energy == null) {
      throw new IllegalArgumentException("Energy value must not be null.");
    }
    switch (energy.unit()) {
      case HARTREE:
        {
          return energy.value() * 2625.4996394799;
        }
      case ELECTRON_VOLT:
        {
          return energy.value() * 96.4853321233;
        }
      case KILOJOULE_PER_MOLE:
        {
          return energy.value();
        }
      case KILOCALORIE_PER_MOLE:
        {
          return energy.value() * 4.184;
        }
    }
    throw new IllegalStateException("Unsupported energy unit.");
  }

  public static double entropyJoulePerMoleKelvin(final EntropyValue entropy) {
    if (entropy == null) {
      throw new IllegalArgumentException("Entropy value must not be null.");
    }
    switch (entropy.unit()) {
      case JOULE_PER_MOLE_KELVIN:
        {
          return entropy.value();
        }
      case KILOJOULE_PER_MOLE_KELVIN:
        {
          return entropy.value() * 1000.0;
        }
      case CALORIE_PER_MOLE_KELVIN:
        {
          return entropy.value() * 4.184;
        }
    }
    throw new IllegalStateException("Unsupported entropy unit.");
  }

  public static double temperatureKelvin(final Temperature temperature) {
    if (temperature == null) {
      throw new IllegalArgumentException("Temperature must not be null.");
    }
    if (temperature.unit() == TemperatureUnit.KELVIN) {
      return temperature.value();
    }
    if (temperature.unit() == TemperatureUnit.CELSIUS) {
      return temperature.value() + 273.15;
    }
    throw new IllegalStateException("Unsupported temperature unit.");
  }
}