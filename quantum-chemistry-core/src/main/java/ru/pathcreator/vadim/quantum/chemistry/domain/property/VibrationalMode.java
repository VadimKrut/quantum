/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.property;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

public final class VibrationalMode {

  private final String id;
  private final VibrationalFrequency frequency;
  private final double irIntensityKilometerPerMole;
  private final double ramanActivity;
  private final List<NormalModeDisplacement> displacements;

  private VibrationalMode(
      final String id,
      final VibrationalFrequency frequency,
      final double irIntensityKilometerPerMole,
      final double ramanActivity,
      final List<NormalModeDisplacement> displacements) {
    this.id = id;
    this.frequency = frequency;
    this.irIntensityKilometerPerMole = irIntensityKilometerPerMole;
    this.ramanActivity = ramanActivity;
    this.displacements = displacements;
  }

  public static VibrationalMode of(
      final String id,
      final VibrationalFrequency frequency,
      final double irIntensityKilometerPerMole,
      final double ramanActivity,
      final List<NormalModeDisplacement> displacements) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Vibrational mode id");
    if (frequency == null) {
      throw new IllegalArgumentException("Vibrational mode frequency must not be null.");
    }
    if (!Double.isFinite(irIntensityKilometerPerMole) || irIntensityKilometerPerMole < 0.0) {
      throw new IllegalArgumentException("IR intensity must be finite and non-negative.");
    }
    if (!Double.isFinite(ramanActivity) || ramanActivity < 0.0) {
      throw new IllegalArgumentException("Raman activity must be finite and non-negative.");
    }
    final List<NormalModeDisplacement> checkedDisplacements =
        List.copyOf(VibrationalMode.requireDisplacements(displacements));
    return new VibrationalMode(
        checkedId, frequency, irIntensityKilometerPerMole, ramanActivity, checkedDisplacements);
  }

  public String id() {
    return this.id;
  }

  public VibrationalFrequency frequency() {
    return this.frequency;
  }

  public double irIntensityKilometerPerMole() {
    return this.irIntensityKilometerPerMole;
  }

  public double ramanActivity() {
    return this.ramanActivity;
  }

  public List<NormalModeDisplacement> displacements() {
    return this.displacements;
  }

  public boolean imaginary() {
    return this.frequency.imaginary();
  }

  private static List<NormalModeDisplacement> requireDisplacements(
      final List<NormalModeDisplacement> displacements) {
    if (displacements == null || displacements.isEmpty()) {
      throw new IllegalArgumentException("Vibrational mode displacements must not be empty.");
    }
    for (int i = 0; i < displacements.size(); ++i) {
      NormalModeDisplacement displacement = displacements.get(i);
      if (displacement == null) {
        throw new IllegalArgumentException("Normal mode displacement must not be null.");
      }
      for (int j = i + 1; j < displacements.size(); ++j) {
        NormalModeDisplacement other = displacements.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Normal mode displacement must not be null.");
        }
        if (!displacement.atomId().equals(other.atomId())) continue;
        throw new IllegalArgumentException("Normal mode displacement atom ids must be unique.");
      }
    }
    return displacements;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof VibrationalMode)) {
      return false;
    }
    final VibrationalMode mode = (VibrationalMode) other;
    return Double.compare(this.irIntensityKilometerPerMole, mode.irIntensityKilometerPerMole) == 0
        && Double.compare(this.ramanActivity, mode.ramanActivity) == 0
        && Objects.equals(this.id, mode.id)
        && Objects.equals(this.frequency, mode.frequency)
        && Objects.equals(this.displacements, mode.displacements);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.frequency,
        this.irIntensityKilometerPerMole,
        this.ramanActivity,
        this.displacements);
  }
}