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
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.EntropyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.ThermodynamicUnitConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

public final class EyringParameters {

  private static final double BOLTZMANN_CONSTANT_JOULE_PER_KELVIN = 1.380649E-23;
  private static final double PLANCK_CONSTANT_JOULE_SECOND = 6.62607015E-34;
  private static final double GAS_CONSTANT_JOULE_PER_MOLE_KELVIN = 8.31446261815324;
  private final EnergyValue activationEnthalpy;
  private final EntropyValue activationEntropy;

  private EyringParameters(
      final EnergyValue activationEnthalpy,
      final EntropyValue activationEntropy
  ) {
    this.activationEnthalpy = activationEnthalpy;
    this.activationEntropy = activationEntropy;
  }

  public static EyringParameters of(
      final EnergyValue activationEnthalpy, final EntropyValue activationEntropy) {
    if (activationEnthalpy == null) {
      throw new IllegalArgumentException("Eyring activation enthalpy must not be null.");
    }
    if (activationEntropy == null) {
      throw new IllegalArgumentException("Eyring activation entropy must not be null.");
    }
    return new EyringParameters(activationEnthalpy, activationEntropy);
  }

  public EnergyValue activationEnthalpy() {
    return this.activationEnthalpy;
  }

  public EntropyValue activationEntropy() {
    return this.activationEntropy;
  }

  public RateConstant rateConstantAt(final Temperature temperature) {
    final double temperatureKelvin = ThermodynamicUnitConverter.temperatureKelvin(temperature);
    if (temperatureKelvin <= 0.0) {
      throw new IllegalArgumentException("Eyring temperature must be above zero kelvin.");
    }
    final double activationEnthalpyJoulePerMole =
        ThermodynamicUnitConverter.energyKiloJoulePerMole(this.activationEnthalpy) * 1000.0;
    final double activationEntropyJoulePerMoleKelvin =
        ThermodynamicUnitConverter.entropyJoulePerMoleKelvin(this.activationEntropy);
    final double thermalFactor =
        BOLTZMANN_CONSTANT_JOULE_PER_KELVIN * temperatureKelvin / PLANCK_CONSTANT_JOULE_SECOND;
    final double entropyFactor =
        Math.exp(activationEntropyJoulePerMoleKelvin / GAS_CONSTANT_JOULE_PER_MOLE_KELVIN);
    final double enthalpyFactor =
        Math.exp(
            -activationEnthalpyJoulePerMole
                / (GAS_CONSTANT_JOULE_PER_MOLE_KELVIN * temperatureKelvin));
    return RateConstant.of(
        thermalFactor * entropyFactor * enthalpyFactor, RateConstantUnit.PER_SECOND);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof EyringParameters)) {
      return false;
    }
    final EyringParameters parameters = (EyringParameters) other;
    return Objects.equals(this.activationEnthalpy, parameters.activationEnthalpy)
        && Objects.equals(this.activationEntropy, parameters.activationEntropy);
  }

  public int hashCode() {
    return Objects.hash(this.activationEnthalpy, this.activationEntropy);
  }
}