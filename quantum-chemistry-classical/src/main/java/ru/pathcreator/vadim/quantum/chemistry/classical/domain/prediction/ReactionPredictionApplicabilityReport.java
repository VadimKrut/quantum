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

/** Сводка применимости прогноза реакции: готовность, риски и трассировка причин. */
public final class ReactionPredictionApplicabilityReport {

  private final ReactionPredictionReadinessLevel readinessLevel;
  private final double applicabilityScore;
  private final boolean quantumFollowupRecommended;
  private final boolean electronicRerankRecommended;
  private final int closeCompetitorCount;
  private final int missingCriticalConditionCount;
  private final int productionDetailedMechanismCount;
  private final int chemicallyUsableMechanismCount;
  private final int genericFallbackMechanismCount;
  private final int unknownMechanismCount;
  private final List<String> traceRows;

  private ReactionPredictionApplicabilityReport(
      final ReactionPredictionReadinessLevel readinessLevel,
      final double applicabilityScore,
      final boolean quantumFollowupRecommended,
      final boolean electronicRerankRecommended,
      final int closeCompetitorCount,
      final int missingCriticalConditionCount,
      final int productionDetailedMechanismCount,
      final int chemicallyUsableMechanismCount,
      final int genericFallbackMechanismCount,
      final int unknownMechanismCount,
      final List<String> traceRows) {
    this.readinessLevel = readinessLevel;
    this.applicabilityScore = applicabilityScore;
    this.quantumFollowupRecommended = quantumFollowupRecommended;
    this.electronicRerankRecommended = electronicRerankRecommended;
    this.closeCompetitorCount = closeCompetitorCount;
    this.missingCriticalConditionCount = missingCriticalConditionCount;
    this.productionDetailedMechanismCount = productionDetailedMechanismCount;
    this.chemicallyUsableMechanismCount = chemicallyUsableMechanismCount;
    this.genericFallbackMechanismCount = genericFallbackMechanismCount;
    this.unknownMechanismCount = unknownMechanismCount;
    this.traceRows = traceRows;
  }

  public static ReactionPredictionApplicabilityReport of(
      final ReactionPredictionReadinessLevel readinessLevel,
      final double applicabilityScore,
      final boolean quantumFollowupRecommended,
      final boolean electronicRerankRecommended,
      final int closeCompetitorCount,
      final int missingCriticalConditionCount,
      final int productionDetailedMechanismCount,
      final int chemicallyUsableMechanismCount,
      final int genericFallbackMechanismCount,
      final int unknownMechanismCount,
      final List<String> traceRows) {
    if (readinessLevel == null) {
      throw new IllegalArgumentException("Reaction prediction readiness level must not be null.");
    }
    if (!Double.isFinite(applicabilityScore)
        || applicabilityScore < 0.0
        || applicabilityScore > 1.0) {
      throw new IllegalArgumentException(
          "Reaction prediction applicability score must be in [0, 1].");
    }
    if (closeCompetitorCount < 0) {
      throw new IllegalArgumentException("Reaction close competitor count must be non-negative.");
    }
    if (missingCriticalConditionCount < 0) {
      throw new IllegalArgumentException(
          "Reaction missing critical condition count must be non-negative.");
    }
    if (productionDetailedMechanismCount < 0
        || chemicallyUsableMechanismCount < 0
        || genericFallbackMechanismCount < 0
        || unknownMechanismCount < 0) {
      throw new IllegalArgumentException("Reaction mechanism quality counts must be non-negative.");
    }
    return new ReactionPredictionApplicabilityReport(
        readinessLevel,
        applicabilityScore,
        quantumFollowupRecommended,
        electronicRerankRecommended,
        closeCompetitorCount,
        missingCriticalConditionCount,
        productionDetailedMechanismCount,
        chemicallyUsableMechanismCount,
        genericFallbackMechanismCount,
        unknownMechanismCount,
        ReactionPredictionApplicabilityReport.checkedTraceRows(traceRows));
  }

  public ReactionPredictionReadinessLevel readinessLevel() {
    return this.readinessLevel;
  }

  public double applicabilityScore() {
    return this.applicabilityScore;
  }

  public boolean quantumFollowupRecommended() {
    return this.quantumFollowupRecommended;
  }

  public boolean electronicRerankRecommended() {
    return this.electronicRerankRecommended;
  }

  public int closeCompetitorCount() {
    return this.closeCompetitorCount;
  }

  public int missingCriticalConditionCount() {
    return this.missingCriticalConditionCount;
  }

  public int productionDetailedMechanismCount() {
    return this.productionDetailedMechanismCount;
  }

  public int chemicallyUsableMechanismCount() {
    return this.chemicallyUsableMechanismCount;
  }

  public int genericFallbackMechanismCount() {
    return this.genericFallbackMechanismCount;
  }

  public int unknownMechanismCount() {
    return this.unknownMechanismCount;
  }

  public List<String> traceRows() {
    return this.traceRows;
  }

  private static List<String> checkedTraceRows(final List<String> traceRows) {
    if (traceRows == null || traceRows.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < traceRows.size(); ++i) {
      if (traceRows.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Reaction prediction applicability trace row must not be null.");
    }
    return List.copyOf(traceRows);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionApplicabilityReport)) {
      return false;
    }
    final ReactionPredictionApplicabilityReport report =
        (ReactionPredictionApplicabilityReport) other;
    return Double.compare(this.applicabilityScore, report.applicabilityScore) == 0
        && this.quantumFollowupRecommended == report.quantumFollowupRecommended
        && this.electronicRerankRecommended == report.electronicRerankRecommended
        && this.closeCompetitorCount == report.closeCompetitorCount
        && this.missingCriticalConditionCount == report.missingCriticalConditionCount
        && this.productionDetailedMechanismCount == report.productionDetailedMechanismCount
        && this.chemicallyUsableMechanismCount == report.chemicallyUsableMechanismCount
        && this.genericFallbackMechanismCount == report.genericFallbackMechanismCount
        && this.unknownMechanismCount == report.unknownMechanismCount
        && this.readinessLevel == report.readinessLevel
        && Objects.equals(this.traceRows, report.traceRows);
  }

  public int hashCode() {
    return Objects.hash(
        this.readinessLevel,
        this.applicabilityScore,
        this.quantumFollowupRecommended,
        this.electronicRerankRecommended,
        this.closeCompetitorCount,
        this.missingCriticalConditionCount,
        this.productionDetailedMechanismCount,
        this.chemicallyUsableMechanismCount,
        this.genericFallbackMechanismCount,
        this.unknownMechanismCount,
        this.traceRows);
  }
}
