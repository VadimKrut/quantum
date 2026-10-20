/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calibration.ReactionOutcomeCalibrationLibrary;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreBreakdown;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/**
 * Мягко переносит наблюдаемые распределения продуктов на уже построенный механистический прогноз.
 */
public final class ReactionPredictionCalibrationApplier {

  private final ReactionOutcomeCalibrationLibrary calibrationLibrary;

  public ReactionPredictionCalibrationApplier(
      final ReactionOutcomeCalibrationLibrary calibrationLibrary) {
    this.calibrationLibrary =
        calibrationLibrary == null ? ReactionOutcomeCalibrationLibrary.EMPTY : calibrationLibrary;
  }

  public ReactionPredictionResult apply(
      final ReactionPredictionResult result, final boolean enabled) {
    if (result == null) {
      throw new IllegalArgumentException("Reaction prediction result must not be null.");
    }
    if (!enabled || this.calibrationLibrary.empty() || !result.hasCandidates()) {
      return result;
    }
    final ReactionOutcomeCalibrationScorer scorer =
        new ReactionOutcomeCalibrationScorer(this.calibrationLibrary);
    final List<ReactionProductCandidate> candidates = result.candidates();
    final double[] weighted = new double[candidates.size()];
    @SuppressWarnings("unchecked")
    final List<ReactionPredictionEvidence>[] evidence = new List[candidates.size()];
    double total = 0.0;
    boolean changed = false;
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      final ArrayList<ReactionPredictionEvidence> candidateEvidence =
          new ArrayList<ReactionPredictionEvidence>(candidate.evidence());
      final double logWeight =
          scorer.calibrationPriorWeight(result.request(), candidate, candidateEvidence);
      evidence[i] = List.copyOf(candidateEvidence);
      weighted[i] = candidate.probability() * Math.exp(logWeight);
      total += weighted[i];
      changed = changed || Math.abs(logWeight) > 0.0;
    }
    if (!changed || total <= 0.0 || !Double.isFinite(total)) {
      return result;
    }
    final ArrayList<ReactionProductCandidate> normalized =
        new ArrayList<ReactionProductCandidate>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      final double probability = weighted[i] / total;
      normalized.add(
          ReactionProductCandidate.of(
              candidate.products(),
              probability,
              ReactionPredictionCalibrationApplier.calibratedYield(candidate, probability),
              candidate.selectivityNote(),
              evidence[i],
              candidate.predictedMechanisms(),
              ReactionPredictionCalibrationApplier.calibratedScoreBreakdown(
                  candidate, probability)));
    }
    return ReactionPredictionResult.of(
        result.request(),
        result.methodType(),
        normalized,
        result.mechanismChannels(),
        result.diagnostics());
  }

  public ReactionOutcomeCalibrationLibrary calibrationLibrary() {
    return this.calibrationLibrary;
  }

  private static double calibratedYield(
      final ReactionProductCandidate candidate, final double probability) {
    if (candidate.probability() <= 0.0) {
      return Math.max(0.0, Math.min(100.0, candidate.expectedYieldPercent()));
    }
    return Math.max(
        0.0,
        Math.min(100.0, candidate.expectedYieldPercent() * probability / candidate.probability()));
  }

  private static ReactionPredictionScoreBreakdown calibratedScoreBreakdown(
      final ReactionProductCandidate candidate, final double probability) {
    final ReactionPredictionScoreBreakdown breakdown = candidate.scoreBreakdown();
    return ReactionPredictionScoreBreakdown.of(
        breakdown.rawScore(), probability, breakdown.components());
  }
}
