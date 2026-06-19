/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.solution;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

public final class SolutionEnvironment {

  public static final SolutionEnvironment VACUUM =
      new SolutionEnvironment(Solvent.VACUUM, null, null, null, null);
  private final Solvent solvent;
  private final PHValue ph;
  private final IonicStrength ionicStrength;
  private final Temperature temperature;
  private final Pressure pressure;

  private SolutionEnvironment(
      final Solvent solvent,
      final PHValue ph,
      final IonicStrength ionicStrength,
      final Temperature temperature,
      final Pressure pressure) {
    this.solvent = solvent;
    this.ph = ph;
    this.ionicStrength = ionicStrength;
    this.temperature = temperature;
    this.pressure = pressure;
  }

  public static SolutionEnvironment aqueous(final PHValue ph) {
    return SolutionEnvironment.of(Solvent.WATER, ph, IonicStrength.ZERO, null, null);
  }

  public static SolutionEnvironment of(
      final Solvent solvent,
      final PHValue ph,
      final IonicStrength ionicStrength,
      final Temperature temperature,
      final Pressure pressure) {
    Solvent checkedSolvent;
    final Solvent solvent2 = checkedSolvent = solvent == null ? Solvent.VACUUM : solvent;
    if (ph != null && !checkedSolvent.aqueous()) {
      throw new IllegalArgumentException("pH requires aqueous solvent environment.");
    }
    if (checkedSolvent.equals(Solvent.VACUUM)
        && (ph != null || ionicStrength != null || pressure != null)) {
      throw new IllegalArgumentException(
          "Vacuum environment must not contain solution-specific values.");
    }
    if (checkedSolvent.equals(Solvent.VACUUM) && temperature == null) {
      return VACUUM;
    }
    return new SolutionEnvironment(checkedSolvent, ph, ionicStrength, temperature, pressure);
  }

  public Solvent solvent() {
    return this.solvent;
  }

  public PHValue ph() {
    return this.ph;
  }

  public boolean hasPH() {
    return this.ph != null;
  }

  public IonicStrength ionicStrength() {
    return this.ionicStrength;
  }

  public boolean hasIonicStrength() {
    return this.ionicStrength != null;
  }

  public Temperature temperature() {
    return this.temperature;
  }

  public boolean hasTemperature() {
    return this.temperature != null;
  }

  public Pressure pressure() {
    return this.pressure;
  }

  public boolean hasPressure() {
    return this.pressure != null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SolutionEnvironment)) {
      return false;
    }
    final SolutionEnvironment environment = (SolutionEnvironment) other;
    return Objects.equals(this.solvent, environment.solvent)
        && Objects.equals(this.ph, environment.ph)
        && Objects.equals(this.ionicStrength, environment.ionicStrength)
        && Objects.equals(this.temperature, environment.temperature)
        && Objects.equals(this.pressure, environment.pressure);
  }

  public int hashCode() {
    return Objects.hash(this.solvent, this.ph, this.ionicStrength, this.temperature, this.pressure);
  }
}