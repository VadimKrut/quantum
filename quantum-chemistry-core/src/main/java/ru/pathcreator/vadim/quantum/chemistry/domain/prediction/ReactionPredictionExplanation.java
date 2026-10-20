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
 * Объяснение результата прогнозирования реакции для UI, API и audit-логики.
 */
public final class ReactionPredictionExplanation {

  private final ReactionPredictionConfidenceLevel confidenceLevel;
  private final double topProbability;
  private final double runnerUpProbability;
  private final double probabilityGap;
  private final int candidateCount;
  private final int positiveEvidenceCount;
  private final int negativeEvidenceCount;
  private final boolean conditionAware;
  private final ReactionConditionSuitability conditionSuitability;
  private final ReactionPathwayAssessment pathwayAssessment;
  private final List<ReactionPredictionEvidence> dominantEvidence;
  private final List<ReactionPredictionEvidence> riskEvidence;

  private ReactionPredictionExplanation(
      final ReactionPredictionConfidenceLevel confidenceLevel,
      final double topProbability,
      final double runnerUpProbability,
      final double probabilityGap,
      final int candidateCount,
      final int positiveEvidenceCount,
      final int negativeEvidenceCount,
      final boolean conditionAware,
      final ReactionConditionSuitability conditionSuitability,
      final ReactionPathwayAssessment pathwayAssessment,
      final List<ReactionPredictionEvidence> dominantEvidence,
      final List<ReactionPredictionEvidence> riskEvidence) {
    this.confidenceLevel = confidenceLevel;
    this.topProbability = topProbability;
    this.runnerUpProbability = runnerUpProbability;
    this.probabilityGap = probabilityGap;
    this.candidateCount = candidateCount;
    this.positiveEvidenceCount = positiveEvidenceCount;
    this.negativeEvidenceCount = negativeEvidenceCount;
    this.conditionAware = conditionAware;
    this.conditionSuitability = conditionSuitability;
    this.pathwayAssessment = pathwayAssessment;
    this.dominantEvidence = dominantEvidence;
    this.riskEvidence = riskEvidence;
  }

  public static ReactionPredictionExplanation of(
      final ReactionPredictionConfidenceLevel confidenceLevel,
      final double topProbability,
      final double runnerUpProbability,
      final double probabilityGap,
      final int candidateCount,
      final int positiveEvidenceCount,
      final int negativeEvidenceCount,
      final boolean conditionAware,
      final ReactionConditionSuitability conditionSuitability,
      final ReactionPathwayAssessment pathwayAssessment,
      final List<ReactionPredictionEvidence> dominantEvidence,
      final List<ReactionPredictionEvidence> riskEvidence) {
    if (confidenceLevel == null) {
      throw new IllegalArgumentException("Reaction prediction confidence level must not be null.");
    }
    ReactionPredictionExplanation.requireProbability(topProbability, "top probability");
    ReactionPredictionExplanation.requireProbability(runnerUpProbability, "runner-up probability");
    ReactionPredictionExplanation.requireProbability(probabilityGap, "probability gap");
    if (candidateCount < 0) {
      throw new IllegalArgumentException(
          "Reaction prediction candidate count must be non-negative.");
    }
    if (positiveEvidenceCount < 0 || negativeEvidenceCount < 0) {
      throw new IllegalArgumentException(
          "Reaction prediction evidence counts must be non-negative.");
    }
    return new ReactionPredictionExplanation(
        confidenceLevel,
        topProbability,
        runnerUpProbability,
        probabilityGap,
        candidateCount,
        positiveEvidenceCount,
        negativeEvidenceCount,
        conditionAware,
        conditionSuitability == null
            ? ReactionConditionSuitability.unknown()
            : conditionSuitability,
        pathwayAssessment == null
            ? ReactionPredictionExplanation.defaultPathwayAssessment(confidenceLevel)
            : pathwayAssessment,
        List.copyOf(ReactionPredictionExplanation.requireEvidence(dominantEvidence)),
        List.copyOf(ReactionPredictionExplanation.requireEvidence(riskEvidence)));
  }

  public ReactionPredictionConfidenceLevel confidenceLevel() {
    return this.confidenceLevel;
  }

  public double topProbability() {
    return this.topProbability;
  }

  public double runnerUpProbability() {
    return this.runnerUpProbability;
  }

  public double probabilityGap() {
    return this.probabilityGap;
  }

  public int candidateCount() {
    return this.candidateCount;
  }

  public int positiveEvidenceCount() {
    return this.positiveEvidenceCount;
  }

  public int negativeEvidenceCount() {
    return this.negativeEvidenceCount;
  }

  public boolean conditionAware() {
    return this.conditionAware;
  }

  public ReactionConditionSuitability conditionSuitability() {
    return this.conditionSuitability;
  }

  public ReactionPathwayAssessment pathwayAssessment() {
    return this.pathwayAssessment;
  }

  public List<ReactionPredictionEvidence> dominantEvidence() {
    return this.dominantEvidence;
  }

  public List<ReactionPredictionEvidence> riskEvidence() {
    return this.riskEvidence;
  }

  public boolean hasRisks() {
    return !this.riskEvidence.isEmpty();
  }

  private static void requireProbability(final double value, final String subject) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException("Reaction prediction " + subject + " must be in [0, 1].");
    }
  }

  private static List<ReactionPredictionEvidence> requireEvidence(
      final List<ReactionPredictionEvidence> evidence) {
    if (evidence == null || evidence.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Reaction prediction explanation evidence contains null value.");
    }
    return evidence;
  }

  private static ReactionPathwayAssessment defaultPathwayAssessment(
      final ReactionPredictionConfidenceLevel confidenceLevel) {
    return ReactionPathwayAssessment.of(
        ReactionMechanismClass.UNKNOWN,
        ReactionRegioselectivityClass.UNKNOWN,
        ReactionConditionWindow.broad("No structured pathway assessment was supplied."),
        confidenceLevel,
        List.of(),
        List.of());
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionExplanation)) {
      return false;
    }
    final ReactionPredictionExplanation explanation = (ReactionPredictionExplanation) other;
    return Double.compare(this.topProbability, explanation.topProbability) == 0
        && Double.compare(this.runnerUpProbability, explanation.runnerUpProbability) == 0
        && Double.compare(this.probabilityGap, explanation.probabilityGap) == 0
        && this.candidateCount == explanation.candidateCount
        && this.positiveEvidenceCount == explanation.positiveEvidenceCount
        && this.negativeEvidenceCount == explanation.negativeEvidenceCount
        && this.conditionAware == explanation.conditionAware
        && this.confidenceLevel == explanation.confidenceLevel
        && Objects.equals(this.conditionSuitability, explanation.conditionSuitability)
        && Objects.equals(this.pathwayAssessment, explanation.pathwayAssessment)
        && Objects.equals(this.dominantEvidence, explanation.dominantEvidence)
        && Objects.equals(this.riskEvidence, explanation.riskEvidence);
  }

  public int hashCode() {
    return Objects.hash(
        this.confidenceLevel,
        this.topProbability,
        this.runnerUpProbability,
        this.probabilityGap,
        this.candidateCount,
        this.positiveEvidenceCount,
        this.negativeEvidenceCount,
        this.conditionAware,
        this.conditionSuitability,
        this.pathwayAssessment,
        this.dominantEvidence,
        this.riskEvidence);
  }
}
