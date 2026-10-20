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
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/**
 * Оценка одного кандидата продукта до нормализации вероятностей внутри результата прогноза.
 */
public final class ReactionCandidateScore {

  private final ReactionProductCandidate candidate;
  private final List<ReactionPredictionEvidence> evidence;
  private final double score;
  private final int sourceIndex;

  private ReactionCandidateScore(
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence,
      final double score,
      final int sourceIndex) {
    this.candidate = candidate;
    this.evidence = evidence;
    this.score = score;
    this.sourceIndex = sourceIndex;
  }

  public static ReactionCandidateScore of(
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence,
      final double score,
      final int sourceIndex) {
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction candidate score candidate must not be null.");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction candidate score evidence must not be null.");
    }
    for (int i = 0; i < evidence.size(); ++i) {
      if (evidence.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Reaction candidate score evidence must not contain null.");
    }
    if (!Double.isFinite(score) || score <= 0.0) {
      throw new IllegalArgumentException("Reaction candidate score must be positive and finite.");
    }
    if (sourceIndex < 0) {
      throw new IllegalArgumentException("Reaction candidate source index must be non-negative.");
    }
    return new ReactionCandidateScore(candidate, List.copyOf(evidence), score, sourceIndex);
  }

  public ReactionProductCandidate candidate() {
    return this.candidate;
  }

  public List<ReactionPredictionEvidence> evidence() {
    return this.evidence;
  }

  public double score() {
    return this.score;
  }

  public int sourceIndex() {
    return this.sourceIndex;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionCandidateScore)) {
      return false;
    }
    final ReactionCandidateScore candidateScore = (ReactionCandidateScore) other;
    return Double.compare(this.score, candidateScore.score) == 0
        && this.sourceIndex == candidateScore.sourceIndex
        && Objects.equals(this.candidate, candidateScore.candidate)
        && Objects.equals(this.evidence, candidateScore.evidence);
  }

  public int hashCode() {
    return Objects.hash(this.candidate, this.evidence, this.score, this.sourceIndex);
  }
}
