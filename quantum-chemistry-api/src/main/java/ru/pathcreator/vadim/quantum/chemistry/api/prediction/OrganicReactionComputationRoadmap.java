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
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;

/**
 * Единая дорожная карта расчетов после прогноза органической реакции.
 */
public final class OrganicReactionComputationRoadmap {

  private final String requestId;
  private final OrganicReactionPredictionWorkflowStatus status;
  private final int conditionScenarioCount;
  private final int evaluatedConditionScenarioCount;
  private final List<OrganicReactionCandidateRoadmap> candidates;
  private final List<ChemistryWorkflowRecommendation> programLevelRecommendations;

  private OrganicReactionComputationRoadmap(
      final String requestId,
      final OrganicReactionPredictionWorkflowStatus status,
      final int conditionScenarioCount,
      final int evaluatedConditionScenarioCount,
      final List<OrganicReactionCandidateRoadmap> candidates,
      final List<ChemistryWorkflowRecommendation> programLevelRecommendations) {
    this.requestId = requestId;
    this.status = status;
    this.conditionScenarioCount = conditionScenarioCount;
    this.evaluatedConditionScenarioCount = evaluatedConditionScenarioCount;
    this.candidates = candidates;
    this.programLevelRecommendations = programLevelRecommendations;
  }

  public static OrganicReactionComputationRoadmap of(
      final String requestId,
      final OrganicReactionPredictionWorkflowStatus status,
      final int conditionScenarioCount,
      final int evaluatedConditionScenarioCount,
      final List<OrganicReactionCandidateRoadmap> candidates,
      final List<ChemistryWorkflowRecommendation> programLevelRecommendations) {
    if (status == null) {
      throw new IllegalArgumentException(
          "Organic reaction computation roadmap status must not be null.");
    }
    if (conditionScenarioCount < 0 || evaluatedConditionScenarioCount < 0) {
      throw new IllegalArgumentException(
          "Organic reaction computation roadmap scenario counts must be non-negative.");
    }
    if (evaluatedConditionScenarioCount > conditionScenarioCount) {
      throw new IllegalArgumentException(
          "Evaluated condition scenario count must not exceed planned count.");
    }
    return new OrganicReactionComputationRoadmap(
        IdentifierValue.requireIdentifier(
            requestId, "Organic reaction computation roadmap request id"),
        status,
        conditionScenarioCount,
        evaluatedConditionScenarioCount,
        OrganicReactionComputationRoadmap.checkedCandidates(candidates),
        OrganicReactionComputationRoadmap.checkedRecommendations(programLevelRecommendations));
  }

  public String requestId() {
    return this.requestId;
  }

  public OrganicReactionPredictionWorkflowStatus status() {
    return this.status;
  }

  public int conditionScenarioCount() {
    return this.conditionScenarioCount;
  }

  public int evaluatedConditionScenarioCount() {
    return this.evaluatedConditionScenarioCount;
  }

  public List<OrganicReactionCandidateRoadmap> candidates() {
    return this.candidates;
  }

  public List<ChemistryWorkflowRecommendation> programLevelRecommendations() {
    return this.programLevelRecommendations;
  }

  public boolean hasCandidates() {
    return !this.candidates.isEmpty();
  }

  public boolean hasProgramLevelRecommendations() {
    return !this.programLevelRecommendations.isEmpty();
  }

  public boolean hasClassicalCalculations() {
    for (int i = 0; i < this.candidates.size(); ++i) {
      if (this.candidates.get(i).hasClassicalCalculations()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasProgramLevelClassicalCalculations() {
    for (int i = 0; i < this.programLevelRecommendations.size(); ++i) {
      if (this.programLevelRecommendations.get(i).hasCalculationKind()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasQuantumPreparation() {
    for (int i = 0; i < this.candidates.size(); ++i) {
      if (this.candidates.get(i).hasQuantumPreparation()) {
        return true;
      }
    }
    return false;
  }

  public boolean readyForFollowUpWork() {
    return this.hasClassicalCalculations()
        || this.hasProgramLevelClassicalCalculations()
        || this.hasQuantumPreparation();
  }

  private static List<OrganicReactionCandidateRoadmap> checkedCandidates(
      final List<OrganicReactionCandidateRoadmap> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < candidates.size(); ++i) {
      final OrganicReactionCandidateRoadmap candidate = candidates.get(i);
      if (candidate == null) {
        throw new IllegalArgumentException("Organic reaction roadmap candidate must not be null.");
      }
      if (candidate.candidateIndex() == i) {
        continue;
      }
      throw new IllegalArgumentException(
          "Organic reaction roadmap candidate index must match order.");
    }
    return List.copyOf(candidates);
  }

  private static List<ChemistryWorkflowRecommendation> checkedRecommendations(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (recommendations == null || recommendations.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < recommendations.size(); ++i) {
      if (recommendations.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Organic reaction roadmap recommendation must not be null.");
    }
    return List.copyOf(recommendations);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrganicReactionComputationRoadmap)) {
      return false;
    }
    final OrganicReactionComputationRoadmap roadmap = (OrganicReactionComputationRoadmap) other;
    return this.conditionScenarioCount == roadmap.conditionScenarioCount
        && this.evaluatedConditionScenarioCount == roadmap.evaluatedConditionScenarioCount
        && Objects.equals(this.requestId, roadmap.requestId)
        && this.status == roadmap.status
        && Objects.equals(this.candidates, roadmap.candidates)
        && Objects.equals(this.programLevelRecommendations, roadmap.programLevelRecommendations);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.requestId);
    result = ChemistryHash.include(result, this.status);
    result = ChemistryHash.include(result, this.conditionScenarioCount);
    result = ChemistryHash.include(result, this.evaluatedConditionScenarioCount);
    result = ChemistryHash.include(result, this.candidates);
    result = ChemistryHash.include(result, this.programLevelRecommendations);
    return result;
  }
}
