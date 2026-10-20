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
 * Структурированная оценка пути реакции: механизм, селективность, окно условий и маршрут элементарных шагов.
 */
public final class ReactionPathwayAssessment {

  private final ReactionMechanismClass mechanismClass;
  private final ReactionRegioselectivityClass regioselectivityClass;
  private final ReactionConditionWindow conditionWindow;
  private final ReactionPredictionConfidenceLevel confidenceLevel;
  private final List<ReactionPathwayStep> pathwaySteps;
  private final List<ReactionPredictionEvidence> supportingEvidence;
  private final List<ReactionPredictionEvidence> limitingEvidence;

  private ReactionPathwayAssessment(
      final ReactionMechanismClass mechanismClass,
      final ReactionRegioselectivityClass regioselectivityClass,
      final ReactionConditionWindow conditionWindow,
      final ReactionPredictionConfidenceLevel confidenceLevel,
      final List<ReactionPathwayStep> pathwaySteps,
      final List<ReactionPredictionEvidence> supportingEvidence,
      final List<ReactionPredictionEvidence> limitingEvidence) {
    this.mechanismClass = mechanismClass;
    this.regioselectivityClass = regioselectivityClass;
    this.conditionWindow = conditionWindow;
    this.confidenceLevel = confidenceLevel;
    this.pathwaySteps = pathwaySteps;
    this.supportingEvidence = supportingEvidence;
    this.limitingEvidence = limitingEvidence;
  }

  public static ReactionPathwayAssessment of(
      final ReactionMechanismClass mechanismClass,
      final ReactionRegioselectivityClass regioselectivityClass,
      final ReactionConditionWindow conditionWindow,
      final ReactionPredictionConfidenceLevel confidenceLevel,
      final List<ReactionPredictionEvidence> supportingEvidence,
      final List<ReactionPredictionEvidence> limitingEvidence) {
    return ReactionPathwayAssessment.of(
        mechanismClass,
        regioselectivityClass,
        conditionWindow,
        confidenceLevel,
        List.of(),
        supportingEvidence,
        limitingEvidence);
  }

  public static ReactionPathwayAssessment of(
      final ReactionMechanismClass mechanismClass,
      final ReactionRegioselectivityClass regioselectivityClass,
      final ReactionConditionWindow conditionWindow,
      final ReactionPredictionConfidenceLevel confidenceLevel,
      final List<ReactionPathwayStep> pathwaySteps,
      final List<ReactionPredictionEvidence> supportingEvidence,
      final List<ReactionPredictionEvidence> limitingEvidence) {
    if (mechanismClass == null) {
      throw new IllegalArgumentException("Reaction pathway mechanism class must not be null.");
    }
    if (regioselectivityClass == null) {
      throw new IllegalArgumentException(
          "Reaction pathway regioselectivity class must not be null.");
    }
    if (conditionWindow == null) {
      throw new IllegalArgumentException("Reaction pathway condition window must not be null.");
    }
    if (confidenceLevel == null) {
      throw new IllegalArgumentException("Reaction pathway confidence level must not be null.");
    }
    return new ReactionPathwayAssessment(
        mechanismClass,
        regioselectivityClass,
        conditionWindow,
        confidenceLevel,
        ReactionPathwayAssessment.checkedPathwaySteps(pathwaySteps),
        ReactionPathwayAssessment.checkedEvidence(supportingEvidence),
        ReactionPathwayAssessment.checkedEvidence(limitingEvidence));
  }

  public ReactionMechanismClass mechanismClass() {
    return this.mechanismClass;
  }

  public ReactionRegioselectivityClass regioselectivityClass() {
    return this.regioselectivityClass;
  }

  public ReactionConditionWindow conditionWindow() {
    return this.conditionWindow;
  }

  public ReactionPredictionConfidenceLevel confidenceLevel() {
    return this.confidenceLevel;
  }

  public List<ReactionPathwayStep> pathwaySteps() {
    return this.pathwaySteps;
  }

  public boolean hasPathwaySteps() {
    return !this.pathwaySteps.isEmpty();
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

  private static List<ReactionPathwayStep> checkedPathwaySteps(
      final List<ReactionPathwayStep> steps) {
    if (steps == null || steps.isEmpty()) {
      return List.of();
    }
    final ReactionPathwayStep[] checked = new ReactionPathwayStep[steps.size()];
    for (int i = 0; i < steps.size(); ++i) {
      final ReactionPathwayStep step = steps.get(i);
      if (step == null) {
        throw new IllegalArgumentException("Reaction pathway assessment step must not be null.");
      }
      checked[i] = step;
    }
    return List.of(checked);
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
          "Reaction pathway assessment evidence must not contain null value.");
    }
    return List.copyOf(evidence);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPathwayAssessment)) {
      return false;
    }
    final ReactionPathwayAssessment assessment = (ReactionPathwayAssessment) other;
    return this.mechanismClass == assessment.mechanismClass
        && this.regioselectivityClass == assessment.regioselectivityClass
        && Objects.equals(this.conditionWindow, assessment.conditionWindow)
        && this.confidenceLevel == assessment.confidenceLevel
        && Objects.equals(this.pathwaySteps, assessment.pathwaySteps)
        && Objects.equals(this.supportingEvidence, assessment.supportingEvidence)
        && Objects.equals(this.limitingEvidence, assessment.limitingEvidence);
  }

  public int hashCode() {
    return Objects.hash(
        this.mechanismClass,
        this.regioselectivityClass,
        this.conditionWindow,
        this.confidenceLevel,
        this.pathwaySteps,
        this.supportingEvidence,
        this.limitingEvidence);
  }
}
