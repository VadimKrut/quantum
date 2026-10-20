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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionApplicabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionUncertaintyReport;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/**
 * Оценивает ширину и причины неопределенности прогноза продуктового распределения.
 */
public final class ReactionPredictionUncertaintyAnalyzer {

  public ReactionPredictionUncertaintyReport analyze(
      final ReactionPredictionResult result,
      final ReactionPredictionApplicabilityReport applicability) {
    if (result == null) {
      throw new IllegalArgumentException("Reaction uncertainty result must not be null.");
    }
    if (applicability == null) {
      throw new IllegalArgumentException(
          "Reaction uncertainty applicability report must not be null.");
    }
    final double entropy = ReactionPredictionUncertaintyAnalyzer.entropy(result.candidates());
    final double best = result.hasCandidates() ? result.bestCandidate().probability() : 0.0;
    final double second =
        ReactionPredictionUncertaintyAnalyzer.secondProbability(result.candidates());
    final double gap = Math.max(0.0, best - second);
    final int riskEvidenceCount =
        ReactionPredictionUncertaintyAnalyzer.riskEvidenceCount(result.candidates());
    final int warningCount = result.evidenceCount(ReactionPredictionEvidenceKind.WARNING);
    final double uncertainty =
        ReactionPredictionUncertaintyAnalyzer.clamp(
            0.18
                + Math.min(0.30, entropy / 4.0)
                + Math.min(0.20, result.missingProbabilityMass())
                + 0.05 * applicability.closeCompetitorCount()
                + 0.04 * applicability.missingCriticalConditionCount()
                + 0.035 * applicability.genericFallbackMechanismCount()
                + 0.030 * applicability.unknownMechanismCount()
                + 0.03 * riskEvidenceCount
                + 0.02 * warningCount
                - Math.min(0.20, gap * 0.40)
                - Math.min(0.10, applicability.productionDetailedMechanismCount() * 0.025)
                - Math.min(0.05, applicability.chemicallyUsableMechanismCount() * 0.010)
                - Math.min(0.16, applicability.applicabilityScore() * 0.16));
    final double halfWidth = ReactionPredictionUncertaintyAnalyzer.clamp(0.05 + uncertainty * 0.45);
    final ArrayList<String> rows = new ArrayList<String>();
    rows.add("metric\tvalue\tmeaning");
    rows.add(
        "distribution_entropy\t" + entropy + "\tHigher values mean broader product distribution.");
    rows.add(
        "best_probability\t" + best + "\tCurrent normalized probability of the leading product.");
    rows.add("second_probability\t" + second + "\tClosest competing product probability.");
    rows.add("probability_gap\t" + gap + "\tSmall gap means selectivity should be reranked.");
    rows.add(
        "missing_probability_mass\t"
            + result.missingProbabilityMass()
            + "\tProbability not represented by explicit candidates.");
    rows.add(
        "risk_evidence_count\t"
            + riskEvidenceCount
            + "\tNegative or competing-pathway evidence attached to candidates.");
    rows.add(
        "warning_count\t"
            + warningCount
            + "\tWarnings from prediction diagnostics and candidate evidence.");
    rows.add(
        "production_detailed_mechanism_count\t"
            + applicability.productionDetailedMechanismCount()
            + "\tDetailed staged mechanisms reduce prediction uncertainty.");
    rows.add(
        "chemically_usable_mechanism_count\t"
            + applicability.chemicallyUsableMechanismCount()
            + "\tUsable staged mechanisms reduce prediction uncertainty moderately.");
    rows.add(
        "generic_fallback_mechanism_count\t"
            + applicability.genericFallbackMechanismCount()
            + "\tTaxonomy fallback mechanisms increase prediction uncertainty.");
    rows.add(
        "unknown_mechanism_count\t"
            + applicability.unknownMechanismCount()
            + "\tUnregistered mechanism links increase prediction uncertainty.");
    rows.add(
        "applicability_score\t"
            + applicability.applicabilityScore()
            + "\tGeneral fit between reaction record and prediction model.");
    return ReactionPredictionUncertaintyReport.of(
        uncertainty,
        ReactionPredictionUncertaintyAnalyzer.clamp(best - halfWidth),
        ReactionPredictionUncertaintyAnalyzer.clamp(best + halfWidth),
        entropy,
        gap,
        riskEvidenceCount,
        warningCount,
        rows);
  }

  private static double entropy(final List<ReactionProductCandidate> candidates) {
    double entropy = 0.0;
    for (int i = 0; i < candidates.size(); ++i) {
      final double probability = candidates.get(i).probability();
      if (probability > 0.0 && Double.isFinite(probability)) {
        entropy -= probability * Math.log(probability);
      }
    }
    return entropy;
  }

  private static double secondProbability(final List<ReactionProductCandidate> candidates) {
    double best = 0.0;
    double second = 0.0;
    for (int i = 0; i < candidates.size(); ++i) {
      final double probability = candidates.get(i).probability();
      if (probability > best) {
        second = best;
        best = probability;
      } else if (probability > second) {
        second = probability;
      }
    }
    return second;
  }

  private static int riskEvidenceCount(final List<ReactionProductCandidate> candidates) {
    int count = 0;
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      if (candidate.evidenceWeight(ReactionPredictionEvidenceKind.COMPETING_PATHWAY) < 0.0
          || candidate.evidenceWeight(ReactionPredictionEvidenceKind.WARNING) < 0.0) {
        ++count;
      }
    }
    return count;
  }

  private static double clamp(final double value) {
    if (!Double.isFinite(value) || value <= 0.0) {
      return 0.0;
    }
    if (value >= 1.0) {
      return 1.0;
    }
    return value;
  }
}
