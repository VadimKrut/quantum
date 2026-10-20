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

/** Итоговая таблица решений для prediction workflow: продукты, механизмы, риски и follow-up расчеты. */
public final class OrganicReactionDecisionMatrix {

  private final List<OrganicReactionDecisionRow> rows;
  private final List<String> programLevelRecommendations;
  private final double probabilityMass;
  private final boolean probabilityMassComplete;
  private final boolean hasHighRiskChannels;
  private final boolean hasQuantumFollowUp;
  private final boolean hasClassicalFollowUp;

  private OrganicReactionDecisionMatrix(
      final List<OrganicReactionDecisionRow> rows,
      final List<String> programLevelRecommendations,
      final double probabilityMass,
      final boolean probabilityMassComplete,
      final boolean hasHighRiskChannels,
      final boolean hasQuantumFollowUp,
      final boolean hasClassicalFollowUp) {
    this.rows = rows;
    this.programLevelRecommendations = programLevelRecommendations;
    this.probabilityMass = probabilityMass;
    this.probabilityMassComplete = probabilityMassComplete;
    this.hasHighRiskChannels = hasHighRiskChannels;
    this.hasQuantumFollowUp = hasQuantumFollowUp;
    this.hasClassicalFollowUp = hasClassicalFollowUp;
  }

  public static OrganicReactionDecisionMatrix of(
      final List<OrganicReactionDecisionRow> rows,
      final List<String> programLevelRecommendations,
      final double probabilityMass,
      final boolean probabilityMassComplete) {
    if (rows == null) {
      throw new IllegalArgumentException("Organic reaction decision matrix rows must not be null.");
    }
    if (programLevelRecommendations == null) {
      throw new IllegalArgumentException(
          "Organic reaction decision matrix recommendations must not be null.");
    }
    if (!Double.isFinite(probabilityMass) || probabilityMass < 0.0) {
      throw new IllegalArgumentException(
          "Organic reaction decision matrix probability mass must be finite and non-negative.");
    }
    boolean highRisk = false;
    boolean quantum = false;
    boolean classical = false;
    for (int i = 0; i < rows.size(); ++i) {
      final OrganicReactionDecisionRow row = rows.get(i);
      if (row == null) {
        throw new IllegalArgumentException(
            "Organic reaction decision matrix row must not be null.");
      }
      highRisk = highRisk || row.highRisk();
      quantum = quantum || row.quantumFollowUpRecommended();
      classical = classical || row.classicalFollowUpRecommended();
    }
    return new OrganicReactionDecisionMatrix(
        List.copyOf(rows),
        OrganicReactionDecisionMatrix.checkedRecommendations(programLevelRecommendations),
        probabilityMass,
        probabilityMassComplete,
        highRisk,
        quantum,
        classical);
  }

  public List<OrganicReactionDecisionRow> rows() {
    return this.rows;
  }

  public List<String> programLevelRecommendations() {
    return this.programLevelRecommendations;
  }

  public double probabilityMass() {
    return this.probabilityMass;
  }

  public boolean probabilityMassComplete() {
    return this.probabilityMassComplete;
  }

  public boolean hasHighRiskChannels() {
    return this.hasHighRiskChannels;
  }

  public boolean hasQuantumFollowUp() {
    return this.hasQuantumFollowUp;
  }

  public boolean hasClassicalFollowUp() {
    return this.hasClassicalFollowUp;
  }

  public boolean readyForDecision() {
    return !this.rows.isEmpty() && this.probabilityMassComplete && !this.hasHighRiskChannels;
  }

  private static List<String> checkedRecommendations(final List<String> recommendations) {
    for (int i = 0; i < recommendations.size(); ++i) {
      final String recommendation = recommendations.get(i);
      if (recommendation == null || recommendation.isBlank()) {
        throw new IllegalArgumentException(
            "Organic reaction decision matrix recommendation must not be blank.");
      }
    }
    return List.copyOf(recommendations);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrganicReactionDecisionMatrix)) {
      return false;
    }
    final OrganicReactionDecisionMatrix matrix = (OrganicReactionDecisionMatrix) other;
    return Double.compare(this.probabilityMass, matrix.probabilityMass) == 0
        && this.probabilityMassComplete == matrix.probabilityMassComplete
        && this.hasHighRiskChannels == matrix.hasHighRiskChannels
        && this.hasQuantumFollowUp == matrix.hasQuantumFollowUp
        && this.hasClassicalFollowUp == matrix.hasClassicalFollowUp
        && this.rows.equals(matrix.rows)
        && this.programLevelRecommendations.equals(matrix.programLevelRecommendations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.rows,
        this.programLevelRecommendations,
        this.probabilityMass,
        this.probabilityMassComplete,
        this.hasHighRiskChannels,
        this.hasQuantumFollowUp,
        this.hasClassicalFollowUp);
  }
}
