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
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;

public final class ReactionKineticProfile {

  private final String id;
  private final Reaction reaction;
  private final ReactionRateLaw rateLaw;
  private final List<KineticMeasurement> measurements;

  private ReactionKineticProfile(
      final String id,
      final Reaction reaction,
      final ReactionRateLaw rateLaw,
      final List<KineticMeasurement> measurements) {
    this.id = id;
    this.reaction = reaction;
    this.rateLaw = rateLaw;
    this.measurements = measurements;
  }

  public static ReactionKineticProfile of(
      final String id,
      final Reaction reaction,
      final ReactionRateLaw rateLaw,
      final List<KineticMeasurement> measurements) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Reaction kinetic profile id");
    if (reaction == null) {
      throw new IllegalArgumentException("Kinetic profile reaction must not be null.");
    }
    if (!reaction.balance().balanced()) {
      throw new IllegalArgumentException("Kinetic profile requires balanced reaction.");
    }
    if (rateLaw == null) {
      throw new IllegalArgumentException("Kinetic profile rate law must not be null.");
    }
    rateLaw.validateAgainstReaction(reaction);
    final List<KineticMeasurement> checkedMeasurements =
        List.copyOf(ReactionKineticProfile.requireMeasurements(rateLaw, measurements));
    return new ReactionKineticProfile(checkedId, reaction, rateLaw, checkedMeasurements);
  }

  public String id() {
    return this.id;
  }

  public Reaction reaction() {
    return this.reaction;
  }

  public ReactionRateLaw rateLaw() {
    return this.rateLaw;
  }

  public List<KineticMeasurement> measurements() {
    return this.measurements;
  }

  public double predictedRateMolePerLiterSecond(final List<ConcentrationPoint> concentrations) {
    return this.rateLaw.rateMolePerLiterSecond(concentrations);
  }

  public double residualMolePerLiterSecond(final int measurementIndex) {
    if (measurementIndex < 0 || measurementIndex >= this.measurements.size()) {
      throw new IllegalArgumentException("Kinetic measurement index is outside profile bounds.");
    }
    KineticMeasurement measurement = this.measurements.get(measurementIndex);
    return measurement.observedRateMolePerLiterSecond()
        - this.predictedRateMolePerLiterSecond(measurement.concentrations());
  }

  private static List<KineticMeasurement> requireMeasurements(
      final ReactionRateLaw rateLaw, final List<KineticMeasurement> measurements) {
    if (measurements == null) {
      return List.of();
    }
    for (int i = 0; i < measurements.size(); ++i) {
      KineticMeasurement measurement = measurements.get(i);
      if (measurement == null) {
        throw new IllegalArgumentException("Kinetic measurement must not be null.");
      }
      rateLaw.rateMolePerLiterSecond(measurement.concentrations());
    }
    return measurements;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionKineticProfile)) {
      return false;
    }
    final ReactionKineticProfile profile = (ReactionKineticProfile) other;
    return Objects.equals(this.id, profile.id)
        && Objects.equals(this.reaction, profile.reaction)
        && Objects.equals(this.rateLaw, profile.rateLaw)
        && Objects.equals(this.measurements, profile.measurements);
  }

  public int hashCode() {
    return Objects.hash(this.id, this.reaction, this.rateLaw, this.measurements);
  }
}