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
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;

/** Один возможный набор продуктов с вероятностью, выходом и объяснениями. */
public final class ReactionProductCandidate {

  private final ReactionSide products;
  private final double probability;
  private final double expectedYieldPercent;
  private final String selectivityNote;
  private final List<ReactionPredictionEvidence> evidence;
  private final List<PredictedReactionMechanism> predictedMechanisms;
  private final ReactionPredictionScoreBreakdown scoreBreakdown;

  private ReactionProductCandidate(
      final ReactionSide products,
      final double probability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final List<ReactionPredictionEvidence> evidence,
      final List<PredictedReactionMechanism> predictedMechanisms,
      final ReactionPredictionScoreBreakdown scoreBreakdown) {
    this.products = products;
    this.probability = probability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.selectivityNote = selectivityNote;
    this.evidence = evidence;
    this.predictedMechanisms = predictedMechanisms;
    this.scoreBreakdown = scoreBreakdown;
  }

  public static ReactionProductCandidate of(
      final ReactionSide products,
      final double probability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final List<ReactionPredictionEvidence> evidence) {
    return ReactionProductCandidate.of(
        products,
        probability,
        expectedYieldPercent,
        selectivityNote,
        evidence,
        List.of(),
        ReactionPredictionScoreBreakdown.empty(probability));
  }

  public static ReactionProductCandidate of(
      final ReactionSide products,
      final double probability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final List<ReactionPredictionEvidence> evidence,
      final List<PredictedReactionMechanism> predictedMechanisms) {
    return ReactionProductCandidate.of(
        products,
        probability,
        expectedYieldPercent,
        selectivityNote,
        evidence,
        predictedMechanisms,
        ReactionPredictionScoreBreakdown.empty(probability));
  }

  public static ReactionProductCandidate of(
      final ReactionSide products,
      final double probability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final List<ReactionPredictionEvidence> evidence,
      final List<PredictedReactionMechanism> predictedMechanisms,
      final ReactionPredictionScoreBreakdown scoreBreakdown) {
    if (products == null) {
      throw new IllegalArgumentException("Reaction product candidate products must not be null.");
    }
    if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException(
          "Reaction product candidate probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Reaction product candidate expected yield percent must be in [0, 100].");
    }
    return new ReactionProductCandidate(
        products,
        probability,
        expectedYieldPercent,
        TextValue.optionalText(selectivityNote),
        ReactionProductCandidate.checkedEvidence(evidence),
        ReactionProductCandidate.checkedMechanisms(predictedMechanisms),
        ReactionProductCandidate.checkedScoreBreakdown(scoreBreakdown, probability));
  }

  public ReactionSide products() {
    return this.products;
  }

  public double probability() {
    return this.probability;
  }

  public double expectedYieldPercent() {
    return this.expectedYieldPercent;
  }

  public String selectivityNote() {
    return this.selectivityNote;
  }

  public boolean hasSelectivityNote() {
    return this.selectivityNote != null;
  }

  public List<ReactionPredictionEvidence> evidence() {
    return this.evidence;
  }

  public boolean hasEvidence() {
    return !this.evidence.isEmpty();
  }

  public boolean hasEvidence(final ReactionPredictionEvidenceKind kind) {
    return this.evidenceCount(kind) > 0;
  }

  public int evidenceCount(final ReactionPredictionEvidenceKind kind) {
    ReactionProductCandidate.requireEvidenceKind(kind);
    int count = 0;
    for (int i = 0; i < this.evidence.size(); ++i) {
      if (this.evidence.get(i).kind() == kind) {
        ++count;
      }
    }
    return count;
  }

  public double evidenceWeight(final ReactionPredictionEvidenceKind kind) {
    ReactionProductCandidate.requireEvidenceKind(kind);
    double weight = 0.0;
    for (int i = 0; i < this.evidence.size(); ++i) {
      final ReactionPredictionEvidence item = this.evidence.get(i);
      if (item.kind() == kind) {
        weight += item.weight();
      }
    }
    return weight;
  }

  public boolean hasWeightedEvidence(final ReactionPredictionEvidenceKind kind) {
    return Math.abs(this.evidenceWeight(kind)) > 0.0;
  }

  public List<PredictedReactionMechanism> predictedMechanisms() {
    return this.predictedMechanisms;
  }

  public int predictedMechanismCount() {
    return this.predictedMechanisms.size();
  }

  public boolean hasPredictedMechanisms() {
    return !this.predictedMechanisms.isEmpty();
  }

  public ReactionPredictionScoreBreakdown scoreBreakdown() {
    return this.scoreBreakdown;
  }

  public boolean hasScoreBreakdown() {
    return this.scoreBreakdown.hasComponents();
  }

  public ReactionProductCandidate withPredictedMechanisms(
      final List<PredictedReactionMechanism> mechanisms) {
    return ReactionProductCandidate.of(
        this.products,
        this.probability,
        this.expectedYieldPercent,
        this.selectivityNote,
        this.evidence,
        mechanisms,
        this.scoreBreakdown);
  }

  public ReactionProductCandidate withScoreBreakdown(
      final ReactionPredictionScoreBreakdown breakdown) {
    return ReactionProductCandidate.of(
        this.products,
        this.probability,
        this.expectedYieldPercent,
        this.selectivityNote,
        this.evidence,
        this.predictedMechanisms,
        breakdown);
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
      throw new IllegalArgumentException("Reaction product candidate evidence must not be null.");
    }
    return List.copyOf(evidence);
  }

  private static List<PredictedReactionMechanism> checkedMechanisms(
      final List<PredictedReactionMechanism> mechanisms) {
    if (mechanisms == null || mechanisms.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < mechanisms.size(); ++i) {
      if (mechanisms.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Reaction product candidate mechanism must not be null.");
    }
    return List.copyOf(mechanisms);
  }

  private static ReactionPredictionScoreBreakdown checkedScoreBreakdown(
      final ReactionPredictionScoreBreakdown breakdown, final double probability) {
    if (breakdown == null) {
      return ReactionPredictionScoreBreakdown.empty(probability);
    }
    return breakdown;
  }

  private static void requireEvidenceKind(final ReactionPredictionEvidenceKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Reaction prediction evidence kind must not be null.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionProductCandidate)) {
      return false;
    }
    final ReactionProductCandidate candidate = (ReactionProductCandidate) other;
    return Objects.equals(this.products, candidate.products)
        && Double.compare(this.probability, candidate.probability) == 0
        && Double.compare(this.expectedYieldPercent, candidate.expectedYieldPercent) == 0
        && Objects.equals(this.selectivityNote, candidate.selectivityNote)
        && Objects.equals(this.evidence, candidate.evidence)
        && Objects.equals(this.predictedMechanisms, candidate.predictedMechanisms)
        && Objects.equals(this.scoreBreakdown, candidate.scoreBreakdown);
  }

  public int hashCode() {
    return Objects.hash(
        this.products,
        this.probability,
        this.expectedYieldPercent,
        this.selectivityNote,
        this.evidence,
        this.predictedMechanisms,
        this.scoreBreakdown);
  }
}
