/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Сохраненная расчетная дорожная карта одного кандидата продукта.
 */
public final class ChemistryStorageRoadmapCandidateRecord {

  private final int candidateIndex;
  private final double probability;
  private final double expectedYieldPercent;
  private final List<String> productMoleculeIds;
  private final List<String> productFormulaSignatures;
  private final List<String> productStoichiometrySignatures;
  private final List<String> mechanismIds;
  private final List<String> mechanismDescriptors;
  private final List<String> classicalCalculationKinds;
  private final List<String> classicalPlanIds;
  private final List<String> followUpActionTargets;
  private final List<String> quantumPreparationTargets;
  private final boolean explicitProductGraph;
  private final boolean electronicRerankReady;
  private final boolean quantumWorkflowReady;
  private final String readinessReason;

  private ChemistryStorageRoadmapCandidateRecord(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final List<String> productMoleculeIds,
      final List<String> productFormulaSignatures,
      final List<String> productStoichiometrySignatures,
      final List<String> mechanismIds,
      final List<String> mechanismDescriptors,
      final List<String> classicalCalculationKinds,
      final List<String> classicalPlanIds,
      final List<String> followUpActionTargets,
      final List<String> quantumPreparationTargets,
      final boolean explicitProductGraph,
      final boolean electronicRerankReady,
      final boolean quantumWorkflowReady,
      final String readinessReason) {
    this.candidateIndex = candidateIndex;
    this.probability = probability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.productMoleculeIds = productMoleculeIds;
    this.productFormulaSignatures = productFormulaSignatures;
    this.productStoichiometrySignatures = productStoichiometrySignatures;
    this.mechanismIds = mechanismIds;
    this.mechanismDescriptors = mechanismDescriptors;
    this.classicalCalculationKinds = classicalCalculationKinds;
    this.classicalPlanIds = classicalPlanIds;
    this.followUpActionTargets = followUpActionTargets;
    this.quantumPreparationTargets = quantumPreparationTargets;
    this.explicitProductGraph = explicitProductGraph;
    this.electronicRerankReady = electronicRerankReady;
    this.quantumWorkflowReady = quantumWorkflowReady;
    this.readinessReason = readinessReason;
  }

  public static ChemistryStorageRoadmapCandidateRecord of(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final List<String> productMoleculeIds,
      final List<String> productFormulaSignatures,
      final List<String> productStoichiometrySignatures,
      final List<String> mechanismIds,
      final List<String> mechanismDescriptors,
      final List<String> classicalCalculationKinds,
      final List<String> classicalPlanIds,
      final List<String> followUpActionTargets,
      final List<String> quantumPreparationTargets,
      final boolean explicitProductGraph,
      final boolean electronicRerankReady,
      final boolean quantumWorkflowReady,
      final String readinessReason) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException("Storage roadmap candidate index must be non-negative.");
    }
    if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException(
          "Storage roadmap candidate probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Storage roadmap candidate yield percent must be in [0, 100].");
    }
    return new ChemistryStorageRoadmapCandidateRecord(
        candidateIndex,
        probability,
        expectedYieldPercent,
        ChemistryStorageRoadmapCandidateRecord.checkedIds(
            productMoleculeIds, "product molecule id"),
        ChemistryStorageRoadmapCandidateRecord.checkedTexts(
            productFormulaSignatures, "product formula signature"),
        ChemistryStorageRoadmapCandidateRecord.checkedTexts(
            productStoichiometrySignatures, "product stoichiometry signature"),
        ChemistryStorageRoadmapCandidateRecord.checkedIds(mechanismIds, "mechanism id"),
        ChemistryStorageRoadmapCandidateRecord.checkedTexts(
            mechanismDescriptors, "mechanism descriptor"),
        ChemistryStorageRoadmapCandidateRecord.checkedIds(
            classicalCalculationKinds, "classical calculation kind"),
        ChemistryStorageRoadmapCandidateRecord.checkedIds(classicalPlanIds, "classical plan id"),
        ChemistryStorageRoadmapCandidateRecord.checkedTexts(
            followUpActionTargets, "follow-up action target"),
        ChemistryStorageRoadmapCandidateRecord.checkedTexts(
            quantumPreparationTargets, "quantum preparation target"),
        explicitProductGraph,
        electronicRerankReady,
        quantumWorkflowReady,
        TextValue.requireText(readinessReason, "Storage roadmap readiness reason"));
  }

  public static ChemistryStorageRoadmapCandidateRecord of(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final List<String> productMoleculeIds,
      final List<String> mechanismIds,
      final List<String> mechanismDescriptors,
      final List<String> classicalCalculationKinds,
      final List<String> classicalPlanIds,
      final List<String> quantumPreparationTargets) {
    return ChemistryStorageRoadmapCandidateRecord.of(
        candidateIndex,
        probability,
        expectedYieldPercent,
        productMoleculeIds,
        List.of(),
        List.of(),
        mechanismIds,
        mechanismDescriptors,
        classicalCalculationKinds,
        classicalPlanIds,
        List.of(),
        quantumPreparationTargets,
        false,
        false,
        false,
        "not_checked");
  }

  public static ChemistryStorageRoadmapCandidateRecord of(
      final int candidateIndex,
      final double probability,
      final double expectedYieldPercent,
      final List<String> productMoleculeIds,
      final List<String> mechanismIds,
      final List<String> classicalCalculationKinds,
      final List<String> quantumPreparationTargets) {
    return ChemistryStorageRoadmapCandidateRecord.of(
        candidateIndex,
        probability,
        expectedYieldPercent,
        productMoleculeIds,
        mechanismIds,
        List.of(),
        classicalCalculationKinds,
        List.of(),
        quantumPreparationTargets);
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

  public List<String> classicalCalculationKinds() {
    return this.classicalCalculationKinds;
  }

  public List<String> classicalPlanIds() {
    return this.classicalPlanIds;
  }

  public List<String> followUpActionTargets() {
    return this.followUpActionTargets;
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

  private static List<String> checkedIds(final List<String> values, final String subject) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      checked[i] = IdentifierValue.requireIdentifier(values.get(i), subject);
    }
    return List.of(checked);
  }

  private static List<String> checkedTexts(final List<String> values, final String subject) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      checked[i] = TextValue.requireText(values.get(i), subject);
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageRoadmapCandidateRecord)) {
      return false;
    }
    final ChemistryStorageRoadmapCandidateRecord record =
        (ChemistryStorageRoadmapCandidateRecord) other;
    return this.candidateIndex == record.candidateIndex
        && Double.compare(this.probability, record.probability) == 0
        && Double.compare(this.expectedYieldPercent, record.expectedYieldPercent) == 0
        && Objects.equals(this.productMoleculeIds, record.productMoleculeIds)
        && Objects.equals(this.productFormulaSignatures, record.productFormulaSignatures)
        && Objects.equals(
            this.productStoichiometrySignatures, record.productStoichiometrySignatures)
        && Objects.equals(this.mechanismIds, record.mechanismIds)
        && Objects.equals(this.mechanismDescriptors, record.mechanismDescriptors)
        && Objects.equals(this.classicalCalculationKinds, record.classicalCalculationKinds)
        && Objects.equals(this.classicalPlanIds, record.classicalPlanIds)
        && Objects.equals(this.followUpActionTargets, record.followUpActionTargets)
        && Objects.equals(this.quantumPreparationTargets, record.quantumPreparationTargets)
        && this.explicitProductGraph == record.explicitProductGraph
        && this.electronicRerankReady == record.electronicRerankReady
        && this.quantumWorkflowReady == record.quantumWorkflowReady
        && Objects.equals(this.readinessReason, record.readinessReason);
  }

  public int hashCode() {
    return Objects.hash(
        this.candidateIndex,
        this.probability,
        this.expectedYieldPercent,
        this.productMoleculeIds,
        this.productFormulaSignatures,
        this.productStoichiometrySignatures,
        this.mechanismIds,
        this.mechanismDescriptors,
        this.classicalCalculationKinds,
        this.classicalPlanIds,
        this.followUpActionTargets,
        this.quantumPreparationTargets,
        this.explicitProductGraph,
        this.electronicRerankReady,
        this.quantumWorkflowReady,
        this.readinessReason);
  }
}
