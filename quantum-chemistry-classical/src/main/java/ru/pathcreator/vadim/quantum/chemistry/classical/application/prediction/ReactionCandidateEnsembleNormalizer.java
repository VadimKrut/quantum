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
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;

/**
 * Превращает независимые оценки продуктовых каналов в относительное распределение ансамбля.
 */
public final class ReactionCandidateEnsembleNormalizer {

  private static final double MIN_MULTIPLIER_LOG = -2.0;
  private static final double MAX_MULTIPLIER_LOG = 2.0;
  private static final double SURROGATE_PRODUCT_LOG_PENALTY = 1.25;
  private static final double CLOSE_COMPETITION_LOGIT_GAP = 0.42;
  private static final double BROAD_SELECTIVITY_TEMPERATURE = 1.35;
  private static final double CLOSE_COMPETITION_TEMPERATURE = 1.2;

  public List<ReactionCandidateScore> adjust(final List<ReactionCandidateScore> scores) {
    if (scores == null) {
      throw new IllegalArgumentException("Reaction candidate score list must not be null.");
    }
    if (scores.isEmpty()) {
      return List.of();
    }
    final double[] logits = new double[scores.size()];
    double maxLogit = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < scores.size(); ++i) {
      final ReactionCandidateScore score = scores.get(i);
      if (score == null) {
        throw new IllegalArgumentException("Reaction candidate score list must not contain null.");
      }
      logits[i] = ReactionCandidateEnsembleNormalizer.ensembleLogit(score);
      if (!Double.isFinite(logits[i])) {
        logits[i] = 0.0;
      }
      maxLogit = Math.max(maxLogit, logits[i]);
    }
    double expTotal = 0.0;
    final double[] expValues = new double[scores.size()];
    for (int i = 0; i < scores.size(); ++i) {
      expValues[i] = Math.exp(logits[i] - maxLogit);
      expTotal += expValues[i];
    }
    if (!Double.isFinite(expTotal) || expTotal <= 0.0) {
      expTotal = scores.size();
      for (int i = 0; i < expValues.length; ++i) {
        expValues[i] = 1.0;
      }
    }
    final EnsembleShape ensembleShape =
        ReactionCandidateEnsembleNormalizer.ensembleShape(scores, logits);
    final ArrayList<ReactionCandidateScore> adjusted =
        new ArrayList<ReactionCandidateScore>(scores.size());
    for (int i = 0; i < scores.size(); ++i) {
      adjusted.add(
          ReactionCandidateEnsembleNormalizer.adjustedScore(
              scores.get(i), logits[i], expValues[i] / expTotal, ensembleShape));
    }
    return List.copyOf(adjusted);
  }

  private static ReactionCandidateScore adjustedScore(
      final ReactionCandidateScore score,
      final double logit,
      final double relativeShare,
      final EnsembleShape ensembleShape) {
    final double multiplier =
        Math.exp(
                Math.max(
                    MIN_MULTIPLIER_LOG,
                    Math.min(MAX_MULTIPLIER_LOG, logit / ensembleShape.temperature)))
            * ReactionCandidateEnsembleNormalizer.userHypothesisMultiplier(score.evidence());
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(score.evidence().size() + 2);
    evidence.addAll(score.evidence());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.QUANTUM_ENERGY_MODEL,
            "Candidate ensemble adjustment: relative kinetic/thermodynamic share="
                + relativeShare
                + ", multiplier="
                + multiplier
                + ", mechanism-aware logit="
                + logit
                + ", ensemble temperature="
                + ensembleShape.temperature
                + ".",
            ReactionCandidateEnsembleNormalizer.clampedEvidenceWeight(relativeShare - 0.5)));
    if (ensembleShape.competitive) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Competitive product ensemble: top mechanism channels have close logits or explicit selectivity warnings; probability distribution was softened instead of forcing a single dominant product.",
              -0.08));
    }
    return ReactionCandidateScore.of(
        score.candidate(), evidence, score.score() * multiplier, score.sourceIndex());
  }

  private static EnsembleShape ensembleShape(
      final List<ReactionCandidateScore> scores, final double[] logits) {
    if (scores.size() <= 1) {
      return EnsembleShape.DEFAULT;
    }
    double first = Double.NEGATIVE_INFINITY;
    double second = Double.NEGATIVE_INFINITY;
    boolean selectivityCompetition = false;
    for (int i = 0; i < logits.length; ++i) {
      final double logit = logits[i];
      if (logit >= first) {
        second = first;
        first = logit;
      } else if (logit > second) {
        second = logit;
      }
      if (ReactionCandidateEnsembleNormalizer.hasSelectivityOrCompetitionEvidence(
          scores.get(i).evidence())) {
        selectivityCompetition = true;
      }
    }
    final boolean closeCompetition =
        Double.isFinite(first)
            && Double.isFinite(second)
            && first - second <= CLOSE_COMPETITION_LOGIT_GAP;
    if (selectivityCompetition) {
      return new EnsembleShape(BROAD_SELECTIVITY_TEMPERATURE, true);
    }
    if (closeCompetition) {
      return new EnsembleShape(CLOSE_COMPETITION_TEMPERATURE, true);
    }
    return EnsembleShape.DEFAULT;
  }

  private static double ensembleLogit(final ReactionCandidateScore score) {
    double thermodynamicWeight = 0.0;
    double kineticWeight = 0.0;
    double electronicWeight = 0.0;
    double userRuleWeight = 0.0;
    double userHypothesisWeight = 0.0;
    double warningWeight = 0.0;
    double quantitativeRecipeWeight = 0.0;
    double selectivityWeight = 0.0;
    double surrogateProductPenalty = 0.0;
    int warningCount = 0;
    final List<ReactionPredictionEvidence> evidence = score.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (item.kind() == ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR) {
        thermodynamicWeight += item.weight();
      } else if (item.kind() == ReactionPredictionEvidenceKind.KINETIC_FACTOR) {
        kineticWeight += item.weight();
        if (ReactionCandidateEnsembleNormalizer.quantitativeRecipeEvidence(item)) {
          quantitativeRecipeWeight += item.weight();
        }
      } else if (item.kind() == ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR) {
        electronicWeight += item.weight();
      } else if (item.kind() == ReactionPredictionEvidenceKind.USER_RULE) {
        userRuleWeight += item.weight();
        if (ReactionCandidateEnsembleNormalizer.userHypothesisEvidence(item)) {
          userHypothesisWeight += Math.max(0.0, item.weight());
        }
      } else if (item.kind() == ReactionPredictionEvidenceKind.USER_SEED) {
        userHypothesisWeight += 0.12;
      } else if (item.kind() == ReactionPredictionEvidenceKind.COMPETING_PATHWAY
          || item.kind() == ReactionPredictionEvidenceKind.WARNING) {
        warningWeight += item.weight();
        ++warningCount;
        if (ReactionCandidateEnsembleNormalizer.quantitativeRecipeEvidence(item)) {
          quantitativeRecipeWeight += item.weight();
        }
        if (ReactionCandidateEnsembleNormalizer.selectivityEvidence(item)) {
          selectivityWeight += item.weight();
        }
      } else if (item.kind() == ReactionPredictionEvidenceKind.SURROGATE_PRODUCT) {
        surrogateProductPenalty += SURROGATE_PRODUCT_LOG_PENALTY;
      }
      if (item.kind() == ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR
          || item.kind() == ReactionPredictionEvidenceKind.KINETIC_FACTOR
          || item.kind() == ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH) {
        if (ReactionCandidateEnsembleNormalizer.selectivityEvidence(item)) {
          selectivityWeight += item.weight();
        }
      }
    }
    return 2.2 * thermodynamicWeight
        + 2.6 * kineticWeight
        + 1.1 * electronicWeight
        + 1.7 * userRuleWeight
        + 0.55 * userHypothesisWeight
        + 1.25 * quantitativeRecipeWeight
        + 1.15 * selectivityWeight
        + 1.4 * warningWeight
        + ReactionCandidateEnsembleNormalizer.mechanismRouteLogit(score)
        - ReactionCandidateEnsembleNormalizer.warningSaturationPenalty(warningCount)
        - Math.min(2.5, surrogateProductPenalty)
        + Math.log(Math.max(1.0E-12, score.candidate().probability() + 1.0E-6));
  }

  private static double mechanismRouteLogit(final ReactionCandidateScore score) {
    final List<PredictedReactionMechanism> mechanisms = score.candidate().predictedMechanisms();
    if (mechanisms.isEmpty()) {
      return 0.0;
    }
    double probabilityMass = 0.0;
    double expectedYieldMass = 0.0;
    int detailedStepCount = 0;
    for (int i = 0; i < mechanisms.size(); ++i) {
      final PredictedReactionMechanism mechanism = mechanisms.get(i);
      probabilityMass += mechanism.pathwayProbability();
      expectedYieldMass += mechanism.expectedYieldPercent() / 100.0;
      if (mechanism.hasDetailedMechanism()) {
        detailedStepCount += mechanism.detailedMechanism().eventCount();
      }
    }
    final double routeConfidence = Math.min(1.0, 0.65 * probabilityMass + 0.35 * expectedYieldMass);
    final double detailSupport = Math.min(0.18, 0.015 * detailedStepCount);
    final double diverseMechanismSupport =
        mechanisms.size() > 1 ? Math.min(0.12, 0.03 * (mechanisms.size() - 1)) : 0.0;
    return 0.9 * routeConfidence + detailSupport + diverseMechanismSupport;
  }

  private static double warningSaturationPenalty(final int warningCount) {
    if (warningCount <= 1) {
      return 0.0;
    }
    return Math.min(0.35, 0.045 * (warningCount - 1));
  }

  private static boolean quantitativeRecipeEvidence(final ReactionPredictionEvidence evidence) {
    final String description = evidence.description().toLowerCase(java.util.Locale.ROOT);
    return description.contains("quantified")
        || description.contains("stoichiometric")
        || description.contains("equivalent")
        || description.contains("gram-scale")
        || description.contains("limiting-reagent")
        || description.contains("mass-transfer");
  }

  private static boolean selectivityEvidence(final ReactionPredictionEvidence evidence) {
    final String description = evidence.description().toLowerCase(java.util.Locale.ROOT);
    return description.contains("selectivity")
        || description.contains("regioisomer")
        || description.contains("over-substitution")
        || description.contains("side-product")
        || description.contains("side reaction")
        || description.contains("product distribution")
        || description.contains("single looked-up product")
        || description.contains("local site")
        || description.contains("site accessibility")
        || description.contains("leaving-group strength");
  }

  private static boolean hasSelectivityOrCompetitionEvidence(
      final List<ReactionPredictionEvidence> evidence) {
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      if (item.kind() == ReactionPredictionEvidenceKind.COMPETING_PATHWAY
          || ReactionCandidateEnsembleNormalizer.selectivityEvidence(item)) {
        return true;
      }
    }
    return false;
  }

  private static boolean userHypothesisEvidence(final ReactionPredictionEvidence evidence) {
    final String description = evidence.description().toLowerCase(java.util.Locale.ROOT);
    return evidence.kind() == ReactionPredictionEvidenceKind.USER_SEED
        || description.contains("product seed")
        || description.contains("user-declared product")
        || description.contains("user hypothesis");
  }

  private static double userHypothesisMultiplier(final List<ReactionPredictionEvidence> evidence) {
    for (int i = 0; i < evidence.size(); ++i) {
      if (ReactionCandidateEnsembleNormalizer.userHypothesisEvidence(evidence.get(i))) {
        return 1.15;
      }
    }
    return 1.0;
  }

  private static double clampedEvidenceWeight(final double value) {
    if (value > 1.0) {
      return 1.0;
    }
    if (value < -1.0) {
      return -1.0;
    }
    return value;
  }

  private static final class EnsembleShape {

    private static final EnsembleShape DEFAULT = new EnsembleShape(1.0, false);

    private final double temperature;
    private final boolean competitive;

    private EnsembleShape(final double temperature, final boolean competitive) {
      this.temperature = temperature;
      this.competitive = competitive;
    }
  }
}
