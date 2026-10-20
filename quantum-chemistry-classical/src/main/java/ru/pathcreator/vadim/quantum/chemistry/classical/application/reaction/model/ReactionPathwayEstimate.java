/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.reaction.model;

import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;

/**
 * Итоговая оценка пути реакции с механизмом, барьерами и кинетическими показателями.
 */
public final class ReactionPathwayEstimate {

  private final ReactionBondChangeSet bondChanges;
  private final ReactionMechanism mechanism;
  private final double temperatureKelvin;
  private final double conditionActivationShift;
  private final double forwardBarrier;
  private final double reverseBarrier;
  private final double reactionEnergy;
  private final double thermalEnergy;
  private final double forwardRate;
  private final double reverseRate;
  private final String pathwayBand;

  private ReactionPathwayEstimate(
      final ReactionBondChangeSet bondChanges,
      final ReactionMechanism mechanism,
      final double temperatureKelvin,
      final double conditionActivationShift,
      final double forwardBarrier,
      final double reverseBarrier,
      final double reactionEnergy,
      final double thermalEnergy,
      final double forwardRate,
      final double reverseRate,
      final String pathwayBand) {
    this.bondChanges = bondChanges;
    this.mechanism = mechanism;
    this.temperatureKelvin = temperatureKelvin;
    this.conditionActivationShift = conditionActivationShift;
    this.forwardBarrier = forwardBarrier;
    this.reverseBarrier = reverseBarrier;
    this.reactionEnergy = reactionEnergy;
    this.thermalEnergy = thermalEnergy;
    this.forwardRate = forwardRate;
    this.reverseRate = reverseRate;
    this.pathwayBand = pathwayBand;
  }

  public static ReactionPathwayEstimate of(
      final ReactionBondChangeSet bondChanges,
      final ReactionMechanism mechanism,
      final double temperatureKelvin,
      final double conditionActivationShift,
      final double forwardBarrier,
      final double reverseBarrier,
      final double reactionEnergy,
      final double thermalEnergy,
      final double forwardRate,
      final double reverseRate,
      final String pathwayBand) {
    if (bondChanges == null) {
      throw new IllegalArgumentException("Reaction pathway bond changes must not be null.");
    }
    ReactionPathwayEstimate.requireFinitePositive(temperatureKelvin, "Temperature");
    ReactionPathwayEstimate.requireFinite(conditionActivationShift, "Condition activation shift");
    ReactionPathwayEstimate.requireFinitePositive(forwardBarrier, "Forward barrier");
    ReactionPathwayEstimate.requireFinitePositive(reverseBarrier, "Reverse barrier");
    ReactionPathwayEstimate.requireFinite(reactionEnergy, "Reaction energy");
    ReactionPathwayEstimate.requireFinitePositive(thermalEnergy, "Thermal energy");
    ReactionPathwayEstimate.requireFiniteNonNegative(forwardRate, "Forward rate");
    ReactionPathwayEstimate.requireFiniteNonNegative(reverseRate, "Reverse rate");
    if (pathwayBand == null || pathwayBand.isBlank()) {
      throw new IllegalArgumentException("Reaction pathway band must not be blank.");
    }
    return new ReactionPathwayEstimate(
        bondChanges,
        mechanism,
        temperatureKelvin,
        conditionActivationShift,
        forwardBarrier,
        reverseBarrier,
        reactionEnergy,
        thermalEnergy,
        forwardRate,
        reverseRate,
        pathwayBand);
  }

  public ReactionBondChangeSet bondChanges() {
    return this.bondChanges;
  }

  public ReactionMechanism mechanism() {
    return this.mechanism;
  }

  public boolean hasMechanism() {
    return this.mechanism != null;
  }

  public double temperatureKelvin() {
    return this.temperatureKelvin;
  }

  public double conditionActivationShift() {
    return this.conditionActivationShift;
  }

  public double forwardBarrier() {
    return this.forwardBarrier;
  }

  public double reverseBarrier() {
    return this.reverseBarrier;
  }

  public double reactionEnergy() {
    return this.reactionEnergy;
  }

  public double thermalEnergy() {
    return this.thermalEnergy;
  }

  public double forwardRate() {
    return this.forwardRate;
  }

  public double reverseRate() {
    return this.reverseRate;
  }

  public double forwardReverseRateRatio() {
    if (this.reverseRate <= Double.MIN_NORMAL) {
      return Double.MAX_VALUE;
    }
    return this.forwardRate / this.reverseRate;
  }

  public String pathwayBand() {
    return this.pathwayBand;
  }

  private static void requireFinite(final double value, final String subjectName) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subjectName + " must be finite.");
    }
  }

  private static void requireFinitePositive(final double value, final String subjectName) {
    ReactionPathwayEstimate.requireFinite(value, subjectName);
    if (value <= 0.0) {
      throw new IllegalArgumentException(subjectName + " must be positive.");
    }
  }

  private static void requireFiniteNonNegative(final double value, final String subjectName) {
    ReactionPathwayEstimate.requireFinite(value, subjectName);
    if (value < 0.0) {
      throw new IllegalArgumentException(subjectName + " must not be negative.");
    }
  }
}
