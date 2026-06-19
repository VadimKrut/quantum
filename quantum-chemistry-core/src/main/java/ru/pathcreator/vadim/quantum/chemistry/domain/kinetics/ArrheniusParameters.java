/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.kinetics;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.ThermodynamicUnitConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

/** Параметры Аррениуса для вычисления rate constant при заданной температуре. */
public final class ArrheniusParameters {

  private static final double GAS_CONSTANT_KILOJOULE_PER_MOLE_KELVIN = 0.00831446261815324;
  private final double preExponentialFactor;
  private final RateConstantUnit unit;
  private final EnergyValue activationEnergy;

  private ArrheniusParameters(
      final double preExponentialFactor,
      final RateConstantUnit unit,
      final EnergyValue activationEnergy) {
    this.preExponentialFactor = preExponentialFactor;
    this.unit = unit;
    this.activationEnergy = activationEnergy;
  }

  public static ArrheniusParameters of(
      final double preExponentialFactor,
      final RateConstantUnit unit,
      final EnergyValue activationEnergy) {
    if (!Double.isFinite(preExponentialFactor)) {
      throw new IllegalArgumentException("Arrhenius pre-exponential factor must be finite.");
    }
    if (preExponentialFactor <= 0.0) {
      throw new IllegalArgumentException("Arrhenius pre-exponential factor must be positive.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Arrhenius rate constant unit must not be null.");
    }
    if (activationEnergy == null) {
      throw new IllegalArgumentException("Arrhenius activation energy must not be null.");
    }
    return new ArrheniusParameters(preExponentialFactor, unit, activationEnergy);
  }

  public double preExponentialFactor() {
    return this.preExponentialFactor;
  }

  public RateConstantUnit unit() {
    return this.unit;
  }

  public EnergyValue activationEnergy() {
    return this.activationEnergy;
  }

  public RateConstant rateConstantAt(final Temperature temperature) {
    final double temperatureKelvin = ThermodynamicUnitConverter.temperatureKelvin(temperature);
    if (temperatureKelvin <= 0.0) {
      throw new IllegalArgumentException("Arrhenius temperature must be above zero kelvin.");
    }
    final double activationEnergyKiloJoulePerMole =
        ThermodynamicUnitConverter.energyKiloJoulePerMole(this.activationEnergy);
    final double exponent =
        -activationEnergyKiloJoulePerMole
            / (GAS_CONSTANT_KILOJOULE_PER_MOLE_KELVIN * temperatureKelvin);
    return RateConstant.of(this.preExponentialFactor * Math.exp(exponent), this.unit);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ArrheniusParameters)) {
      return false;
    }
    final ArrheniusParameters parameters = (ArrheniusParameters) other;
    return Double.compare(this.preExponentialFactor, parameters.preExponentialFactor) == 0
        && this.unit == parameters.unit
        && Objects.equals(this.activationEnergy, parameters.activationEnergy);
  }

  public int hashCode() {
    int result = Double.hashCode(this.preExponentialFactor);
    result = 31 * result + this.unit.hashCode();
    result = 31 * result + this.activationEnergy.hashCode();
    return result;
  }
}