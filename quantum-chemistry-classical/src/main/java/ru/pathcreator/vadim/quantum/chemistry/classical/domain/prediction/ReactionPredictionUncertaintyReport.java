/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Описывает неопределенность распределения продуктов реакции.
 */
public final class ReactionPredictionUncertaintyReport {

  private final double uncertaintyScore;
  private final double topCandidateLowerBound;
  private final double topCandidateUpperBound;
  private final double distributionEntropy;
  private final double probabilityGap;
  private final int riskEvidenceCount;
  private final int warningCount;
  private final List<String> rows;

  private ReactionPredictionUncertaintyReport(
      final double uncertaintyScore,
      final double topCandidateLowerBound,
      final double topCandidateUpperBound,
      final double distributionEntropy,
      final double probabilityGap,
      final int riskEvidenceCount,
      final int warningCount,
      final List<String> rows) {
    this.uncertaintyScore = uncertaintyScore;
    this.topCandidateLowerBound = topCandidateLowerBound;
    this.topCandidateUpperBound = topCandidateUpperBound;
    this.distributionEntropy = distributionEntropy;
    this.probabilityGap = probabilityGap;
    this.riskEvidenceCount = riskEvidenceCount;
    this.warningCount = warningCount;
    this.rows = rows;
  }

  public static ReactionPredictionUncertaintyReport of(
      final double uncertaintyScore,
      final double topCandidateLowerBound,
      final double topCandidateUpperBound,
      final double distributionEntropy,
      final double probabilityGap,
      final int riskEvidenceCount,
      final int warningCount,
      final List<String> rows) {
    ReactionPredictionUncertaintyReport.requireFraction(
        uncertaintyScore, "Prediction uncertainty score");
    ReactionPredictionUncertaintyReport.requireFraction(
        topCandidateLowerBound, "Prediction lower bound");
    ReactionPredictionUncertaintyReport.requireFraction(
        topCandidateUpperBound, "Prediction upper bound");
    if (topCandidateLowerBound > topCandidateUpperBound) {
      throw new IllegalArgumentException("Prediction uncertainty interval is inverted.");
    }
    if (!Double.isFinite(distributionEntropy) || distributionEntropy < 0.0) {
      throw new IllegalArgumentException(
          "Prediction distribution entropy must be finite and non-negative.");
    }
    ReactionPredictionUncertaintyReport.requireFraction(
        probabilityGap, "Prediction probability gap");
    if (riskEvidenceCount < 0 || warningCount < 0) {
      throw new IllegalArgumentException(
          "Prediction uncertainty evidence counts must not be negative.");
    }
    return new ReactionPredictionUncertaintyReport(
        uncertaintyScore,
        topCandidateLowerBound,
        topCandidateUpperBound,
        distributionEntropy,
        probabilityGap,
        riskEvidenceCount,
        warningCount,
        List.copyOf(ReactionPredictionUncertaintyReport.requireRows(rows)));
  }

  public double uncertaintyScore() {
    return this.uncertaintyScore;
  }

  public double topCandidateLowerBound() {
    return this.topCandidateLowerBound;
  }

  public double topCandidateUpperBound() {
    return this.topCandidateUpperBound;
  }

  public double distributionEntropy() {
    return this.distributionEntropy;
  }

  public double probabilityGap() {
    return this.probabilityGap;
  }

  public int riskEvidenceCount() {
    return this.riskEvidenceCount;
  }

  public int warningCount() {
    return this.warningCount;
  }

  public List<String> rows() {
    return this.rows;
  }

  private static void requireFraction(final double value, final String subject) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(subject + " must be in [0, 1].");
    }
  }

  private static List<String> requireRows(final List<String> rows) {
    if (rows == null || rows.isEmpty()) {
      return List.of("metric\tvalue\tmeaning");
    }
    for (int i = 0; i < rows.size(); ++i) {
      if (rows.get(i) != null && !rows.get(i).isBlank()) {
        continue;
      }
      throw new IllegalArgumentException("Prediction uncertainty row must not be blank.");
    }
    return rows;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionUncertaintyReport)) {
      return false;
    }
    final ReactionPredictionUncertaintyReport report = (ReactionPredictionUncertaintyReport) other;
    return Double.compare(this.uncertaintyScore, report.uncertaintyScore) == 0
        && Double.compare(this.topCandidateLowerBound, report.topCandidateLowerBound) == 0
        && Double.compare(this.topCandidateUpperBound, report.topCandidateUpperBound) == 0
        && Double.compare(this.distributionEntropy, report.distributionEntropy) == 0
        && Double.compare(this.probabilityGap, report.probabilityGap) == 0
        && this.riskEvidenceCount == report.riskEvidenceCount
        && this.warningCount == report.warningCount
        && Objects.equals(this.rows, report.rows);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.uncertaintyScore);
    result = ChemistryHash.include(result, this.topCandidateLowerBound);
    result = ChemistryHash.include(result, this.topCandidateUpperBound);
    result = ChemistryHash.include(result, this.distributionEntropy);
    result = ChemistryHash.include(result, this.probabilityGap);
    result = ChemistryHash.include(result, this.riskEvidenceCount);
    result = ChemistryHash.include(result, this.warningCount);
    result = ChemistryHash.include(result, this.rows);
    return result;
  }
}
