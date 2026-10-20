/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

import java.util.List;
import java.util.Objects;

/**
 * Оценка того, насколько заданные условия реакции согласуются с прогнозом.
 */
public final class ReactionConditionSuitability {

  private final double score;
  private final boolean explicitConditions;
  private final boolean stronglyIncompatible;
  private final List<ReactionPredictionEvidence> supportingEvidence;
  private final List<ReactionPredictionEvidence> limitingEvidence;

  private ReactionConditionSuitability(
      final double score,
      final boolean explicitConditions,
      final boolean stronglyIncompatible,
      final List<ReactionPredictionEvidence> supportingEvidence,
      final List<ReactionPredictionEvidence> limitingEvidence) {
    this.score = score;
    this.explicitConditions = explicitConditions;
    this.stronglyIncompatible = stronglyIncompatible;
    this.supportingEvidence = supportingEvidence;
    this.limitingEvidence = limitingEvidence;
  }

  public static ReactionConditionSuitability of(
      final double score,
      final boolean explicitConditions,
      final boolean stronglyIncompatible,
      final List<ReactionPredictionEvidence> supportingEvidence,
      final List<ReactionPredictionEvidence> limitingEvidence) {
    if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
      throw new IllegalArgumentException("Reaction condition suitability score must be in [0, 1].");
    }
    return new ReactionConditionSuitability(
        score,
        explicitConditions,
        stronglyIncompatible,
        ReactionConditionSuitability.checkedEvidence(supportingEvidence),
        ReactionConditionSuitability.checkedEvidence(limitingEvidence));
  }

  public static ReactionConditionSuitability unknown() {
    return ReactionConditionSuitability.of(0.0, false, false, List.of(), List.of());
  }

  public double score() {
    return this.score;
  }

  public boolean explicitConditions() {
    return this.explicitConditions;
  }

  public boolean stronglyIncompatible() {
    return this.stronglyIncompatible;
  }

  public List<ReactionPredictionEvidence> supportingEvidence() {
    return this.supportingEvidence;
  }

  public List<ReactionPredictionEvidence> limitingEvidence() {
    return this.limitingEvidence;
  }

  public boolean hasLimitations() {
    return !this.limitingEvidence.isEmpty();
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
      throw new IllegalArgumentException(
          "Reaction condition suitability evidence must not contain null value.");
    }
    return List.copyOf(evidence);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionSuitability)) {
      return false;
    }
    final ReactionConditionSuitability suitability = (ReactionConditionSuitability) other;
    return Double.compare(this.score, suitability.score) == 0
        && this.explicitConditions == suitability.explicitConditions
        && this.stronglyIncompatible == suitability.stronglyIncompatible
        && Objects.equals(this.supportingEvidence, suitability.supportingEvidence)
        && Objects.equals(this.limitingEvidence, suitability.limitingEvidence);
  }

  public int hashCode() {
    return Objects.hash(
        this.score,
        this.explicitConditions,
        this.stronglyIncompatible,
        this.supportingEvidence,
        this.limitingEvidence);
  }
}
