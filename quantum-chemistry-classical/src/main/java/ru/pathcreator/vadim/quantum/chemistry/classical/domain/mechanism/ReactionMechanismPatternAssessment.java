/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism;

import java.util.List;
import java.util.Objects;

/**
 * Оценка применимости механистического шаблона к конкретной постановке реакции.
 */
public final class ReactionMechanismPatternAssessment {

  private final ReactionMechanismPattern pattern;
  private final List<String> matchedApplicabilitySignals;
  private final List<String> matchedRiskSignals;
  private final double scoreAdjustment;

  private ReactionMechanismPatternAssessment(
      final ReactionMechanismPattern pattern,
      final List<String> matchedApplicabilitySignals,
      final List<String> matchedRiskSignals,
      final double scoreAdjustment) {
    this.pattern = pattern;
    this.matchedApplicabilitySignals = matchedApplicabilitySignals;
    this.matchedRiskSignals = matchedRiskSignals;
    this.scoreAdjustment = scoreAdjustment;
  }

  public static ReactionMechanismPatternAssessment of(
      final ReactionMechanismPattern pattern,
      final List<String> matchedApplicabilitySignals,
      final List<String> matchedRiskSignals,
      final double scoreAdjustment) {
    if (pattern == null) {
      throw new IllegalArgumentException(
          "Reaction mechanism pattern assessment pattern must not be null.");
    }
    if (!Double.isFinite(scoreAdjustment)) {
      throw new IllegalArgumentException(
          "Reaction mechanism pattern assessment score must be finite.");
    }
    return new ReactionMechanismPatternAssessment(
        pattern,
        ReactionMechanismPatternAssessment.checkedSignals(matchedApplicabilitySignals),
        ReactionMechanismPatternAssessment.checkedSignals(matchedRiskSignals),
        scoreAdjustment);
  }

  public ReactionMechanismPattern pattern() {
    return this.pattern;
  }

  public List<String> matchedApplicabilitySignals() {
    return this.matchedApplicabilitySignals;
  }

  public boolean hasApplicabilityMatches() {
    return !this.matchedApplicabilitySignals.isEmpty();
  }

  public List<String> matchedRiskSignals() {
    return this.matchedRiskSignals;
  }

  public boolean hasRiskMatches() {
    return !this.matchedRiskSignals.isEmpty();
  }

  public double scoreAdjustment() {
    return this.scoreAdjustment;
  }

  private static List<String> checkedSignals(final List<String> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      final String value = values.get(i);
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(
            "Reaction mechanism pattern assessment signal must not be blank.");
      }
      checked[i] = value;
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionMechanismPatternAssessment)) {
      return false;
    }
    final ReactionMechanismPatternAssessment assessment =
        (ReactionMechanismPatternAssessment) other;
    return Double.compare(this.scoreAdjustment, assessment.scoreAdjustment) == 0
        && Objects.equals(this.pattern, assessment.pattern)
        && Objects.equals(this.matchedApplicabilitySignals, assessment.matchedApplicabilitySignals)
        && Objects.equals(this.matchedRiskSignals, assessment.matchedRiskSignals);
  }

  public int hashCode() {
    return Objects.hash(
        this.pattern,
        this.matchedApplicabilitySignals,
        this.matchedRiskSignals,
        this.scoreAdjustment);
  }
}
