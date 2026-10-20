/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;

/** Оценка site-selectivity для одного сгенерированного regioisomer/product channel. */
public final class ReactionSiteSelectivityAssessment {

  private final double probability;
  private final double expectedYieldPercent;
  private final List<ReactionPredictionEvidence> evidence;

  private ReactionSiteSelectivityAssessment(
      final double probability,
      final double expectedYieldPercent,
      final List<ReactionPredictionEvidence> evidence) {
    this.probability = probability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.evidence = evidence;
  }

  public static ReactionSiteSelectivityAssessment of(
      final double probability,
      final double expectedYieldPercent,
      final List<ReactionPredictionEvidence> evidence) {
    if (!Double.isFinite(probability) || probability <= 0.0 || probability > 1.0) {
      throw new IllegalArgumentException(
          "Reaction site-selectivity probability must be in (0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Reaction site-selectivity yield percent must be in [0, 100].");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction site-selectivity evidence must not be null.");
    }
    return new ReactionSiteSelectivityAssessment(
        probability, expectedYieldPercent, List.copyOf(evidence));
  }

  public double probability() {
    return this.probability;
  }

  public double expectedYieldPercent() {
    return this.expectedYieldPercent;
  }

  public List<ReactionPredictionEvidence> evidence() {
    return this.evidence;
  }
}
