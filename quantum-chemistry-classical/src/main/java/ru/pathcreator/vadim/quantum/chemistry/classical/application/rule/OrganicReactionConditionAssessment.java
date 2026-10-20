/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.rule;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;

/**
 * Оценка совместимости условий реакции с конкретным органическим правилом.
 */
public final class OrganicReactionConditionAssessment {

  private final double scoreAdjustment;
  private final boolean stronglyIncompatible;
  private final List<ReactionPredictionEvidence> evidence;

  private OrganicReactionConditionAssessment(
      final double scoreAdjustment,
      final boolean stronglyIncompatible,
      final List<ReactionPredictionEvidence> evidence) {
    this.scoreAdjustment = scoreAdjustment;
    this.stronglyIncompatible = stronglyIncompatible;
    this.evidence = evidence;
  }

  public static OrganicReactionConditionAssessment of(
      final double scoreAdjustment,
      final boolean stronglyIncompatible,
      final List<ReactionPredictionEvidence> evidence) {
    if (!Double.isFinite(scoreAdjustment)) {
      throw new IllegalArgumentException("Organic condition score adjustment must be finite.");
    }
    return new OrganicReactionConditionAssessment(
        scoreAdjustment,
        stronglyIncompatible,
        OrganicReactionConditionAssessment.checkedEvidence(evidence));
  }

  public double scoreAdjustment() {
    return this.scoreAdjustment;
  }

  public boolean stronglyIncompatible() {
    return this.stronglyIncompatible;
  }

  public List<ReactionPredictionEvidence> evidence() {
    return this.evidence;
  }

  private static List<ReactionPredictionEvidence> checkedEvidence(
      final List<ReactionPredictionEvidence> evidence) {
    if (evidence == null || evidence.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Organic condition evidence must not contain null value.");
    }
    return List.copyOf(evidence);
  }
}
