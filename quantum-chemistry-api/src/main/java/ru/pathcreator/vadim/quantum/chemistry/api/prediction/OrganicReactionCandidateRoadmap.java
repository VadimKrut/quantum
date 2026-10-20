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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPriority;

/**
 * Расчетная дорожная карта одного предсказанного продукта реакции.
 */
public final class OrganicReactionCandidateRoadmap {

  private final int candidateIndex;
  private final double probability;
  private final double expectedYieldPercent;
  private final List<String> productMoleculeIds;
  private final List<String> productFormulaSignatures;
  private final List<String> productStoichiometrySignatures;
  private final List<String> mechanismIds;
  private final List<String> mechanismDescriptors;
  private final List<ClassicalCalculationKind> classicalCalculationKinds;
  private final List<String> quantumPreparationTargets;
  private final boolean explicitProductGraph;
  private final boolean electronicRerankReady;
  private final boolean quantumWorkflowReady;
  private final String readinessReason;
  private final List<ChemistryWorkflowRecommendation> recommendations;

  private OrganicReactionCandidateRoadmap(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final List<String> productMoleculeIds,
      final List<String> productFormulaSignatures,
      final List<String> productStoichiometrySignatures,
      final List<String> mechanismIds,
      final List<String> mechanismDescriptors,
      final List<ClassicalCalculationKind> classicalCalculationKinds,
      final List<String> quantumPreparationTargets,
      final boolean explicitProductGraph,
      final boolean electronicRerankReady,
      final boolean quantumWorkflowReady,
      final String readinessReason,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    this.candidateIndex = candidateIndex;
    this.probability = probability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.productMoleculeIds = productMoleculeIds;
    this.productFormulaSignatures = productFormulaSignatures;
    this.productStoichiometrySignatures = productStoichiometrySignatures;
    this.mechanismIds = mechanismIds;
    this.mechanismDescriptors = mechanismDescriptors;
    this.classicalCalculationKinds = classicalCalculationKinds;
    this.quantumPreparationTargets = quantumPreparationTargets;
    this.explicitProductGraph = explicitProductGraph;
    this.electronicRerankReady = electronicRerankReady;
    this.quantumWorkflowReady = quantumWorkflowReady;
    this.readinessReason = readinessReason;
    this.recommendations = recommendations;
  }

  public static OrganicReactionCandidateRoadmap of(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final List<String> productMoleculeIds,
      final List<String> productFormulaSignatures,
      final List<String> productStoichiometrySignatures,
      final List<String> mechanismIds,
      final List<String> mechanismDescriptors,
      final List<ClassicalCalculationKind> classicalCalculationKinds,
      final List<String> quantumPreparationTargets,
      final boolean explicitProductGraph,
      final boolean electronicRerankReady,
      final boolean quantumWorkflowReady,
      final String readinessReason,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Organic reaction roadmap candidate index must be non-negative.");
    }
    if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("Organic reaction roadmap probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Organic reaction roadmap yield percent must be in [0, 100].");
    }
    return new OrganicReactionCandidateRoadmap(
        candidateIndex,
        probability,
        expectedYieldPercent,
        OrganicReactionCandidateRoadmap.checkedTexts(productMoleculeIds, "product molecule id"),
        OrganicReactionCandidateRoadmap.checkedTexts(
            productFormulaSignatures, "product formula signature"),
        OrganicReactionCandidateRoadmap.checkedTexts(
            productStoichiometrySignatures, "product stoichiometry signature"),
        OrganicReactionCandidateRoadmap.checkedTexts(mechanismIds, "mechanism id"),
        OrganicReactionCandidateRoadmap.checkedTexts(mechanismDescriptors, "mechanism descriptor"),
        OrganicReactionCandidateRoadmap.checkedKinds(classicalCalculationKinds),
        OrganicReactionCandidateRoadmap.checkedTexts(
            quantumPreparationTargets, "quantum preparation target"),
        explicitProductGraph,
        electronicRerankReady,
        quantumWorkflowReady,
        OrganicReactionCandidateRoadmap.checkedReadinessReason(readinessReason),
        OrganicReactionCandidateRoadmap.checkedRecommendations(recommendations));
  }

  public int candidateIndex() {
    return this.candidateIndex;
  }

  public double probability() {
    return this.probability;
  }

  public double expectedYieldPercent() {
    return this.expectedYieldPercent;
  }

  public List<String> productMoleculeIds() {
    return this.productMoleculeIds;
  }

  public List<String> productFormulaSignatures() {
    return this.productFormulaSignatures;
  }

  public List<String> productStoichiometrySignatures() {
    return this.productStoichiometrySignatures;
  }

  public List<String> mechanismIds() {
    return this.mechanismIds;
  }

  public List<String> mechanismDescriptors() {
    return this.mechanismDescriptors;
  }

  public List<ClassicalCalculationKind> classicalCalculationKinds() {
    return this.classicalCalculationKinds;
  }

  public List<String> quantumPreparationTargets() {
    return this.quantumPreparationTargets;
  }

  public boolean explicitProductGraph() {
    return this.explicitProductGraph;
  }

  public boolean electronicRerankReady() {
    return this.electronicRerankReady;
  }

  public boolean quantumWorkflowReady() {
    return this.quantumWorkflowReady;
  }

  public String readinessReason() {
    return this.readinessReason;
  }

  public List<ChemistryWorkflowRecommendation> recommendations() {
    return this.recommendations;
  }

  public boolean hasClassicalCalculations() {
    return !this.classicalCalculationKinds.isEmpty();
  }

  public boolean hasQuantumPreparation() {
    return !this.quantumPreparationTargets.isEmpty();
  }

  public boolean hasMechanisticRoute() {
    return !this.mechanismIds.isEmpty();
  }

  public boolean hasMechanismDescriptors() {
    return !this.mechanismDescriptors.isEmpty();
  }

  public boolean hasProductFormulaSignatures() {
    return !this.productFormulaSignatures.isEmpty();
  }

  public boolean hasProductStoichiometrySignatures() {
    return !this.productStoichiometrySignatures.isEmpty();
  }

  public boolean hasHighPriorityWork() {
    for (int i = 0; i < this.recommendations.size(); ++i) {
      if (this.recommendations.get(i).priority() == ChemistryWorkflowRecommendationPriority.HIGH) {
        return true;
      }
    }
    return false;
  }

  private static List<String> checkedTexts(final List<String> values, final String subject) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < values.size(); ++i) {
      final String value = values.get(i);
      if (value != null && !value.isBlank()) {
        continue;
      }
      throw new IllegalArgumentException(
          "Organic reaction roadmap " + subject + " must not be blank.");
    }
    return List.copyOf(values);
  }

  private static List<ClassicalCalculationKind> checkedKinds(
      final List<ClassicalCalculationKind> kinds) {
    if (kinds == null || kinds.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < kinds.size(); ++i) {
      if (kinds.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Organic reaction roadmap calculation kind must not be null.");
    }
    return List.copyOf(kinds);
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

  private static String checkedReadinessReason(final String readinessReason) {
    if (readinessReason == null || readinessReason.isBlank()) {
      throw new IllegalArgumentException(
          "Organic reaction roadmap readiness reason must not be blank.");
    }
    return readinessReason;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrganicReactionCandidateRoadmap)) {
      return false;
    }
    final OrganicReactionCandidateRoadmap roadmap = (OrganicReactionCandidateRoadmap) other;
    return this.candidateIndex == roadmap.candidateIndex
        && Double.compare(this.probability, roadmap.probability) == 0
        && Double.compare(this.expectedYieldPercent, roadmap.expectedYieldPercent) == 0
        && Objects.equals(this.productMoleculeIds, roadmap.productMoleculeIds)
        && Objects.equals(this.productFormulaSignatures, roadmap.productFormulaSignatures)
        && Objects.equals(
            this.productStoichiometrySignatures, roadmap.productStoichiometrySignatures)
        && Objects.equals(this.mechanismIds, roadmap.mechanismIds)
        && Objects.equals(this.mechanismDescriptors, roadmap.mechanismDescriptors)
        && Objects.equals(this.classicalCalculationKinds, roadmap.classicalCalculationKinds)
        && Objects.equals(this.quantumPreparationTargets, roadmap.quantumPreparationTargets)
        && this.explicitProductGraph == roadmap.explicitProductGraph
        && this.electronicRerankReady == roadmap.electronicRerankReady
        && this.quantumWorkflowReady == roadmap.quantumWorkflowReady
        && Objects.equals(this.readinessReason, roadmap.readinessReason)
        && Objects.equals(this.recommendations, roadmap.recommendations);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.candidateIndex);
    result = ChemistryHash.include(result, this.probability);
    result = ChemistryHash.include(result, this.expectedYieldPercent);
    result = ChemistryHash.include(result, this.productMoleculeIds);
    result = ChemistryHash.include(result, this.productFormulaSignatures);
    result = ChemistryHash.include(result, this.productStoichiometrySignatures);
    result = ChemistryHash.include(result, this.mechanismIds);
    result = ChemistryHash.include(result, this.mechanismDescriptors);
    result = ChemistryHash.include(result, this.classicalCalculationKinds);
    result = ChemistryHash.include(result, this.quantumPreparationTargets);
    result = ChemistryHash.include(result, this.explicitProductGraph);
    result = ChemistryHash.include(result, this.electronicRerankReady);
    result = ChemistryHash.include(result, this.quantumWorkflowReady);
    result = ChemistryHash.include(result, this.readinessReason);
    result = ChemistryHash.include(result, this.recommendations);
    return result;
  }
}
