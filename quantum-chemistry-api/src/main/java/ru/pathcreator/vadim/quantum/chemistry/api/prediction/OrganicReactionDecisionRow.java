/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.api.prediction;

import java.util.List;
import java.util.Objects;

/** Строка прикладной матрицы решений по одному возможному каналу реакции. */
public final class OrganicReactionDecisionRow {

  private final int candidateIndex;
  private final List<String> productNames;
  private final double probability;
  private final double expectedYieldPercent;
  private final List<String> mechanismFamilies;
  private final List<String> mechanismTrace;
  private final List<String> evidence;
  private final List<String> recommendedCalculations;
  private final List<String> quantumTargets;
  private final double confidenceScore;
  private final boolean highRisk;
  private final boolean quantumFollowUpRecommended;
  private final boolean classicalFollowUpRecommended;

  private OrganicReactionDecisionRow(
      final int candidateIndex,
      final List<String> productNames,
      final double probability,
      final double expectedYieldPercent,
      final List<String> mechanismFamilies,
      final List<String> mechanismTrace,
      final List<String> evidence,
      final List<String> recommendedCalculations,
      final List<String> quantumTargets,
      final double confidenceScore,
      final boolean highRisk,
      final boolean quantumFollowUpRecommended,
      final boolean classicalFollowUpRecommended) {
    this.candidateIndex = candidateIndex;
    this.productNames = productNames;
    this.probability = probability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.mechanismFamilies = mechanismFamilies;
    this.mechanismTrace = mechanismTrace;
    this.evidence = evidence;
    this.recommendedCalculations = recommendedCalculations;
    this.quantumTargets = quantumTargets;
    this.confidenceScore = confidenceScore;
    this.highRisk = highRisk;
    this.quantumFollowUpRecommended = quantumFollowUpRecommended;
    this.classicalFollowUpRecommended = classicalFollowUpRecommended;
  }

  public static OrganicReactionDecisionRow of(
      final int candidateIndex,
      final List<String> productNames,
      final double probability,
      final double expectedYieldPercent,
      final List<String> mechanismFamilies,
      final List<String> mechanismTrace,
      final List<String> evidence,
      final List<String> recommendedCalculations,
      final List<String> quantumTargets,
      final double confidenceScore,
      final boolean highRisk,
      final boolean quantumFollowUpRecommended,
      final boolean classicalFollowUpRecommended) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Organic reaction decision row candidate index must not be negative.");
    }
    OrganicReactionDecisionRow.requireProbability(probability, "probability");
    OrganicReactionDecisionRow.requirePercent(expectedYieldPercent, "expected yield percent");
    OrganicReactionDecisionRow.requireProbability(confidenceScore, "confidence score");
    return new OrganicReactionDecisionRow(
        candidateIndex,
        OrganicReactionDecisionRow.checkedStrings(productNames, "product name"),
        probability,
        expectedYieldPercent,
        OrganicReactionDecisionRow.checkedStrings(mechanismFamilies, "mechanism family"),
        OrganicReactionDecisionRow.checkedStrings(mechanismTrace, "mechanism trace"),
        OrganicReactionDecisionRow.checkedStrings(evidence, "evidence"),
        OrganicReactionDecisionRow.checkedStrings(
            recommendedCalculations, "recommended calculation"),
        OrganicReactionDecisionRow.checkedStrings(quantumTargets, "quantum target"),
        confidenceScore,
        highRisk,
        quantumFollowUpRecommended,
        classicalFollowUpRecommended);
  }

  public int candidateIndex() {
    return this.candidateIndex;
  }

  public List<String> productNames() {
    return this.productNames;
  }

  public double probability() {
    return this.probability;
  }

  public double expectedYieldPercent() {
    return this.expectedYieldPercent;
  }

  public List<String> mechanismFamilies() {
    return this.mechanismFamilies;
  }

  public List<String> mechanismTrace() {
    return this.mechanismTrace;
  }

  public List<String> evidence() {
    return this.evidence;
  }

  public List<String> recommendedCalculations() {
    return this.recommendedCalculations;
  }

  public List<String> quantumTargets() {
    return this.quantumTargets;
  }

  public double confidenceScore() {
    return this.confidenceScore;
  }

  public boolean highRisk() {
    return this.highRisk;
  }

  public boolean quantumFollowUpRecommended() {
    return this.quantumFollowUpRecommended;
  }

  public boolean classicalFollowUpRecommended() {
    return this.classicalFollowUpRecommended;
  }

  public boolean needsFollowUp() {
    return this.quantumFollowUpRecommended || this.classicalFollowUpRecommended || this.highRisk;
  }

  private static void requireProbability(final double value, final String subject) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(
          "Organic reaction decision row " + subject + " must be in [0, 1].");
    }
  }

  private static void requirePercent(final double value, final String subject) {
    if (!Double.isFinite(value) || value < 0.0 || value > 100.0) {
      throw new IllegalArgumentException(
          "Organic reaction decision row " + subject + " must be in [0, 100].");
    }
  }

  private static List<String> checkedStrings(final List<String> values, final String subject) {
    if (values == null) {
      throw new IllegalArgumentException(
          "Organic reaction decision row " + subject + " list must not be null.");
    }
    for (int i = 0; i < values.size(); ++i) {
      final String value = values.get(i);
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(
            "Organic reaction decision row " + subject + " must not be blank.");
      }
    }
    return List.copyOf(values);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrganicReactionDecisionRow)) {
      return false;
    }
    final OrganicReactionDecisionRow row = (OrganicReactionDecisionRow) other;
    return this.candidateIndex == row.candidateIndex
        && Double.compare(this.probability, row.probability) == 0
        && Double.compare(this.expectedYieldPercent, row.expectedYieldPercent) == 0
        && Double.compare(this.confidenceScore, row.confidenceScore) == 0
        && this.highRisk == row.highRisk
        && this.quantumFollowUpRecommended == row.quantumFollowUpRecommended
        && this.classicalFollowUpRecommended == row.classicalFollowUpRecommended
        && this.productNames.equals(row.productNames)
        && this.mechanismFamilies.equals(row.mechanismFamilies)
        && this.mechanismTrace.equals(row.mechanismTrace)
        && this.evidence.equals(row.evidence)
        && this.recommendedCalculations.equals(row.recommendedCalculations)
        && this.quantumTargets.equals(row.quantumTargets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.candidateIndex,
        this.productNames,
        this.probability,
        this.expectedYieldPercent,
        this.mechanismFamilies,
        this.mechanismTrace,
        this.evidence,
        this.recommendedCalculations,
        this.quantumTargets,
        this.confidenceScore,
        this.highRisk,
        this.quantumFollowUpRecommended,
        this.classicalFollowUpRecommended);
  }
}
