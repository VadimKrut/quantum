/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.kinetics;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;

public final class KineticMeasurement {

  private final ReactionConditions conditions;
  private final List<ConcentrationPoint> concentrations;
  private final ReactionRateValue observedRate;

  private KineticMeasurement(
      final ReactionConditions conditions,
      final List<ConcentrationPoint> concentrations,
      final ReactionRateValue observedRate) {
    this.conditions = conditions;
    this.concentrations = concentrations;
    this.observedRate = observedRate;
  }

  public static KineticMeasurement of(
      final ReactionConditions conditions,
      final List<ConcentrationPoint> concentrations,
      final ReactionRateValue observedRate) {
    if (conditions == null || conditions.empty() || conditions.temperature() == null) {
      throw new IllegalArgumentException("Kinetic measurement must define temperature.");
    }
    if (observedRate == null) {
      throw new IllegalArgumentException("Kinetic measurement observed rate must not be null.");
    }
    final List<ConcentrationPoint> checkedConcentrations =
        List.copyOf(KineticMeasurement.requireConcentrations(concentrations));
    return new KineticMeasurement(conditions, checkedConcentrations, observedRate);
  }

  public ReactionConditions conditions() {
    return this.conditions;
  }

  public List<ConcentrationPoint> concentrations() {
    return this.concentrations;
  }

  public ReactionRateValue observedRate() {
    return this.observedRate;
  }

  public double observedRateMolePerLiterSecond() {
    return KineticUnitConverter.rateMolePerLiterSecond(this.observedRate);
  }

  private static List<ConcentrationPoint> requireConcentrations(
      final List<ConcentrationPoint> concentrations) {
    if (concentrations == null || concentrations.isEmpty()) {
      throw new IllegalArgumentException("Kinetic measurement concentrations must not be empty.");
    }
    for (int i = 0; i < concentrations.size(); ++i) {
      ConcentrationPoint point = concentrations.get(i);
      if (point == null) {
        throw new IllegalArgumentException("Kinetic concentration point must not be null.");
      }
      for (int j = i + 1; j < concentrations.size(); ++j) {
        ConcentrationPoint other = concentrations.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Kinetic concentration point must not be null.");
        }
        if (!point.moleculeId().equals(other.moleculeId())) continue;
        throw new IllegalArgumentException(
            "Kinetic concentration points must use unique molecule ids.");
      }
    }
    return concentrations;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof KineticMeasurement)) {
      return false;
    }
    final KineticMeasurement measurement = (KineticMeasurement) other;
    return Objects.equals(this.conditions, measurement.conditions)
        && Objects.equals(this.concentrations, measurement.concentrations)
        && Objects.equals(this.observedRate, measurement.observedRate);
  }

  public int hashCode() {
    return Objects.hash(this.conditions, this.concentrations, this.observedRate);
  }
}