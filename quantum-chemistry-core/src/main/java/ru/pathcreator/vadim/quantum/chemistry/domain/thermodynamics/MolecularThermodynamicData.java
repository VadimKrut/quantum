/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

public final class MolecularThermodynamicData {

  private static final double TEMPERATURE_TOLERANCE_KELVIN = 1.0E-9;
  private final MoleculeId moleculeId;
  private final Temperature temperature;
  private final EnergyValue enthalpy;
  private final EnergyValue gibbsFreeEnergy;
  private final EntropyValue entropy;
  private final EnergyValue zeroPointEnergy;

  private MolecularThermodynamicData(
      final MoleculeId moleculeId,
      final Temperature temperature,
      final EnergyValue enthalpy,
      final EnergyValue gibbsFreeEnergy,
      final EntropyValue entropy,
      final EnergyValue zeroPointEnergy) {
    this.moleculeId = moleculeId;
    this.temperature = temperature;
    this.enthalpy = enthalpy;
    this.gibbsFreeEnergy = gibbsFreeEnergy;
    this.entropy = entropy;
    this.zeroPointEnergy = zeroPointEnergy;
  }

  public static MolecularThermodynamicData of(
      final MoleculeId moleculeId,
      final Temperature temperature,
      final EnergyValue enthalpy,
      final EnergyValue gibbsFreeEnergy,
      final EntropyValue entropy,
      final EnergyValue zeroPointEnergy) {
    if (moleculeId == null) {
      throw new IllegalArgumentException("Thermodynamic molecule id must not be null.");
    }
    if (temperature == null) {
      throw new IllegalArgumentException("Thermodynamic data temperature must not be null.");
    }
    if (enthalpy == null && gibbsFreeEnergy == null && entropy == null && zeroPointEnergy == null) {
      throw new IllegalArgumentException("Thermodynamic data must contain at least one quantity.");
    }
    return new MolecularThermodynamicData(
        moleculeId, temperature, enthalpy, gibbsFreeEnergy, entropy, zeroPointEnergy);
  }

  public MoleculeId moleculeId() {
    return this.moleculeId;
  }

  public Temperature temperature() {
    return this.temperature;
  }

  public EnergyValue enthalpy() {
    return this.enthalpy;
  }

  public boolean hasEnthalpy() {
    return this.enthalpy != null;
  }

  public EnergyValue gibbsFreeEnergy() {
    return this.gibbsFreeEnergy;
  }

  public boolean hasGibbsFreeEnergy() {
    return this.gibbsFreeEnergy != null;
  }

  public EntropyValue entropy() {
    return this.entropy;
  }

  public boolean hasEntropy() {
    return this.entropy != null;
  }

  public EnergyValue zeroPointEnergy() {
    return this.zeroPointEnergy;
  }

  public boolean hasZeroPointEnergy() {
    return this.zeroPointEnergy != null;
  }

  public boolean sameTemperature(final Temperature other) {
    double otherKelvin;
    final double ownKelvin = ThermodynamicUnitConverter.temperatureKelvin(this.temperature);
    return Math.abs(ownKelvin - (otherKelvin = ThermodynamicUnitConverter.temperatureKelvin(other)))
        <= TEMPERATURE_TOLERANCE_KELVIN;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularThermodynamicData)) {
      return false;
    }
    final MolecularThermodynamicData data = (MolecularThermodynamicData) other;
    return Objects.equals(this.moleculeId, data.moleculeId)
        && Objects.equals(this.temperature, data.temperature)
        && Objects.equals(this.enthalpy, data.enthalpy)
        && Objects.equals(this.gibbsFreeEnergy, data.gibbsFreeEnergy)
        && Objects.equals(this.entropy, data.entropy)
        && Objects.equals(this.zeroPointEnergy, data.zeroPointEnergy);
  }

  public int hashCode() {
    return Objects.hash(
        this.moleculeId,
        this.temperature,
        this.enthalpy,
        this.gibbsFreeEnergy,
        this.entropy,
        this.zeroPointEnergy);
  }
}