/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Набор конкурирующих продуктовых каналов из одной статьи, серии опытов или лабораторной калибровки. */
public final class ReactionOutcomeDistributionDataset {

  private static final double MAX_ROUNDED_PROBABILITY_MASS = 1.05;

  private final String id;
  private final String title;
  private final String source;
  private final ReactionOutcomeSourceQuality sourceQuality;
  private final List<ReactionOutcomeCalibrationObservation> observations;

  private ReactionOutcomeDistributionDataset(
      final String id,
      final String title,
      final String source,
      final ReactionOutcomeSourceQuality sourceQuality,
      final List<ReactionOutcomeCalibrationObservation> observations) {
    this.id = id;
    this.title = title;
    this.source = source;
    this.sourceQuality = sourceQuality;
    this.observations = observations;
  }

  public static ReactionOutcomeDistributionDataset of(
      final String id,
      final String title,
      final String source,
      final ReactionOutcomeSourceQuality sourceQuality,
      final List<ReactionOutcomeCalibrationObservation> observations) {
    if (observations == null || observations.isEmpty()) {
      throw new IllegalArgumentException(
          "Reaction outcome distribution observations must not be empty.");
    }
    for (int i = 0; i < observations.size(); ++i) {
      if (observations.get(i) == null) {
        throw new IllegalArgumentException(
            "Reaction outcome distribution observation must not be null.");
      }
    }
    final ReactionOutcomeDistributionDataset dataset =
        new ReactionOutcomeDistributionDataset(
            TextValue.requireText(id, "Reaction outcome distribution dataset id"),
            TextValue.requireText(title, "Reaction outcome distribution dataset title"),
            TextValue.requireText(source, "Reaction outcome distribution dataset source"),
            sourceQuality == null ? ReactionOutcomeSourceQuality.UNKNOWN : sourceQuality,
            List.copyOf(observations));
    if (dataset.observedProbabilityMass() > MAX_ROUNDED_PROBABILITY_MASS) {
      throw new IllegalArgumentException(
          "Reaction outcome distribution probability mass must not exceed 1.05.");
    }
    return dataset;
  }

  public String id() {
    return this.id;
  }

  public String title() {
    return this.title;
  }

  public String source() {
    return this.source;
  }

  public ReactionOutcomeSourceQuality sourceQuality() {
    return this.sourceQuality;
  }

  public List<ReactionOutcomeCalibrationObservation> observations() {
    return this.observations;
  }

  public double observedProbabilityMass() {
    double mass = 0.0;
    for (int i = 0; i < this.observations.size(); ++i) {
      mass += this.observations.get(i).observedProbability();
    }
    return mass;
  }

  public double unassignedProbabilityMass() {
    return Math.max(0.0, 1.0 - this.observedProbabilityMass());
  }

  public boolean hasUnassignedProbabilityMass(final double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Reaction outcome distribution tolerance must be non-negative.");
    }
    return this.unassignedProbabilityMass() > tolerance;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionOutcomeDistributionDataset)) {
      return false;
    }
    final ReactionOutcomeDistributionDataset dataset = (ReactionOutcomeDistributionDataset) other;
    return Objects.equals(this.id, dataset.id)
        && Objects.equals(this.title, dataset.title)
        && Objects.equals(this.source, dataset.source)
        && this.sourceQuality == dataset.sourceQuality
        && Objects.equals(this.observations, dataset.observations);
  }

  public int hashCode() {
    return Objects.hash(this.id, this.title, this.source, this.sourceQuality, this.observations);
  }
}
