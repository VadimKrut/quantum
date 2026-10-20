/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/**
 * Иммутабельный план следующих расчетов и проверок после workflow.
 */
public final class ChemistryWorkflowRecommendationPlan {

  public static final ChemistryWorkflowRecommendationPlan EMPTY =
      new ChemistryWorkflowRecommendationPlan("", List.of());

  private final String workflowJobId;
  private final List<ChemistryWorkflowRecommendation> recommendations;

  private ChemistryWorkflowRecommendationPlan(
      final String workflowJobId, final List<ChemistryWorkflowRecommendation> recommendations) {
    this.workflowJobId = workflowJobId;
    this.recommendations = recommendations;
  }

  public static ChemistryWorkflowRecommendationPlan of(
      final String workflowJobId, final List<ChemistryWorkflowRecommendation> recommendations) {
    return new ChemistryWorkflowRecommendationPlan(
        IdentifierValue.requireIdentifier(
            workflowJobId, "Chemistry workflow recommendation plan job id"),
        List.copyOf(ChemistryWorkflowRecommendationPlan.requireRecommendations(recommendations)));
  }

  public String workflowJobId() {
    return this.workflowJobId;
  }

  public List<ChemistryWorkflowRecommendation> recommendations() {
    return this.recommendations;
  }

  public boolean empty() {
    return this.recommendations.isEmpty();
  }

  public int size() {
    return this.recommendations.size();
  }

  public List<ChemistryWorkflowRecommendation> recommendationsForCandidate(
      final int candidateIndex) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Chemistry workflow candidate index must be non-negative.");
    }
    final ArrayList<ChemistryWorkflowRecommendation> result =
        new ArrayList<ChemistryWorkflowRecommendation>();
    for (int i = 0; i < this.recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = this.recommendations.get(i);
      if (recommendation.candidateIndex() == candidateIndex) {
        result.add(recommendation);
      }
    }
    return List.copyOf(result);
  }

  public List<ChemistryWorkflowRecommendation> programLevelRecommendations() {
    final ArrayList<ChemistryWorkflowRecommendation> result =
        new ArrayList<ChemistryWorkflowRecommendation>();
    for (int i = 0; i < this.recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = this.recommendations.get(i);
      if (!recommendation.appliesToCandidate()) {
        result.add(recommendation);
      }
    }
    return List.copyOf(result);
  }

  public List<ChemistryWorkflowRecommendation> recommendationsByPriority(
      final ChemistryWorkflowRecommendationPriority priority) {
    if (priority == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow recommendation priority must not be null.");
    }
    final ArrayList<ChemistryWorkflowRecommendation> result =
        new ArrayList<ChemistryWorkflowRecommendation>();
    for (int i = 0; i < this.recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = this.recommendations.get(i);
      if (recommendation.priority() == priority) {
        result.add(recommendation);
      }
    }
    return List.copyOf(result);
  }

  public List<ClassicalCalculationKind> calculationKindsByPriority(
      final ChemistryWorkflowRecommendationPriority priority) {
    final List<ChemistryWorkflowRecommendation> filtered = this.recommendationsByPriority(priority);
    final ArrayList<ClassicalCalculationKind> result = new ArrayList<ClassicalCalculationKind>();
    for (int i = 0; i < filtered.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = filtered.get(i);
      if (!recommendation.hasCalculationKind()
          || result.contains(recommendation.calculationKind())) {
        continue;
      }
      result.add(recommendation.calculationKind());
    }
    return List.copyOf(result);
  }

  public boolean hasHighPriorityRecommendations() {
    for (int i = 0; i < this.recommendations.size(); ++i) {
      if (this.recommendations.get(i).priority() == ChemistryWorkflowRecommendationPriority.HIGH) {
        return true;
      }
    }
    return false;
  }

  private static List<ChemistryWorkflowRecommendation> requireRecommendations(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (recommendations == null || recommendations.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < recommendations.size(); ++i) {
      if (recommendations.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Chemistry workflow recommendation must not be null.");
    }
    return recommendations;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowRecommendationPlan)) {
      return false;
    }
    final ChemistryWorkflowRecommendationPlan plan = (ChemistryWorkflowRecommendationPlan) other;
    return Objects.equals(this.workflowJobId, plan.workflowJobId)
        && Objects.equals(this.recommendations, plan.recommendations);
  }

  public int hashCode() {
    return Objects.hash(this.workflowJobId, this.recommendations);
  }
}
