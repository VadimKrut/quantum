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

/** Сохраненный кандидат продукта внутри снимка прогноза реакции. */
public final class ChemistryStoragePredictionCandidateRecord {

  private final int rank;
  private final List<String> productMoleculeIds;
  private final List<String> productFormulaSignatures;
  private final List<String> productStoichiometrySignatures;
  private final double probability;
  private final double expectedYieldPercent;
  private final List<String> mechanismIds;
  private final List<String> mechanismDescriptors;
  private final List<String> scoreComponents;
  private final List<String> evidence;
  private final String note;

  private ChemistryStoragePredictionCandidateRecord(
      final int rank,
      final List<String> productMoleculeIds,
      final List<String> productFormulaSignatures,
      final List<String> productStoichiometrySignatures,
      final double probability,
      final double expectedYieldPercent,
      final List<String> mechanismIds,
      final List<String> mechanismDescriptors,
      final List<String> scoreComponents,
      final List<String> evidence,
      final String note) {
    this.rank = rank;
    this.productMoleculeIds = productMoleculeIds;
    this.productFormulaSignatures = productFormulaSignatures;
    this.productStoichiometrySignatures = productStoichiometrySignatures;
    this.probability = probability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.mechanismIds = mechanismIds;
    this.mechanismDescriptors = mechanismDescriptors;
    this.scoreComponents = scoreComponents;
    this.evidence = evidence;
    this.note = note;
  }

  public static ChemistryStoragePredictionCandidateRecord of(
      final int rank,
      final List<String> productMoleculeIds,
      final List<String> productFormulaSignatures,
      final List<String> productStoichiometrySignatures,
      final double probability,
      final double expectedYieldPercent,
      final List<String> mechanismIds,
      final List<String> mechanismDescriptors,
      final List<String> scoreComponents,
      final List<String> evidence,
      final String note) {
    if (rank < 0) {
      throw new IllegalArgumentException("Prediction candidate rank must be non-negative.");
    }
    if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("Prediction candidate probability must be in [0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Prediction candidate expected yield percent must be in [0, 100].");
    }
    return new ChemistryStoragePredictionCandidateRecord(
        rank,
        ChemistryStoragePredictionCandidateRecord.checkedIds(
            productMoleculeIds, "Prediction candidate product molecule id"),
        ChemistryStoragePredictionCandidateRecord.checkedTexts(
            productFormulaSignatures, "Prediction candidate product formula signature"),
        ChemistryStoragePredictionCandidateRecord.checkedTexts(
            productStoichiometrySignatures, "Prediction candidate product stoichiometry signature"),
        probability,
        expectedYieldPercent,
        ChemistryStoragePredictionCandidateRecord.checkedIds(
            mechanismIds, "Prediction candidate mechanism id"),
        ChemistryStoragePredictionCandidateRecord.checkedTexts(
            mechanismDescriptors, "Prediction candidate mechanism descriptor"),
        ChemistryStoragePredictionCandidateRecord.checkedTexts(
            scoreComponents, "Prediction candidate score component"),
        ChemistryStoragePredictionCandidateRecord.checkedTexts(
            evidence, "Prediction candidate evidence"),
        TextValue.optionalText(note));
  }

  public int rank() {
    return this.rank;
  }

  public List<String> productMoleculeIds() {
    return this.productMoleculeIds;
  }

  public List<String> productFormulaSignatures() {
    return this.productFormulaSignatures;
  }

  public boolean hasProductFormulaSignatures() {
    return !this.productFormulaSignatures.isEmpty();
  }

  public List<String> productStoichiometrySignatures() {
    return this.productStoichiometrySignatures;
  }

  public boolean hasProductStoichiometrySignatures() {
    return !this.productStoichiometrySignatures.isEmpty();
  }

  public double probability() {
    return this.probability;
  }

  public double expectedYieldPercent() {
    return this.expectedYieldPercent;
  }

  public List<String> mechanismIds() {
    return this.mechanismIds;
  }

  public List<String> mechanismDescriptors() {
    return this.mechanismDescriptors;
  }

  public boolean hasMechanismDescriptors() {
    return !this.mechanismDescriptors.isEmpty();
  }

  public List<String> scoreComponents() {
    return this.scoreComponents;
  }

  public boolean hasScoreComponents() {
    return !this.scoreComponents.isEmpty();
  }

  public List<String> evidence() {
    return this.evidence;
  }

  public boolean hasEvidence() {
    return !this.evidence.isEmpty();
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
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
    if (!(other instanceof ChemistryStoragePredictionCandidateRecord)) {
      return false;
    }
    final ChemistryStoragePredictionCandidateRecord record =
        (ChemistryStoragePredictionCandidateRecord) other;
    return this.rank == record.rank
        && Double.compare(this.probability, record.probability) == 0
        && Double.compare(this.expectedYieldPercent, record.expectedYieldPercent) == 0
        && Objects.equals(this.productMoleculeIds, record.productMoleculeIds)
        && Objects.equals(this.productFormulaSignatures, record.productFormulaSignatures)
        && Objects.equals(
            this.productStoichiometrySignatures, record.productStoichiometrySignatures)
        && Objects.equals(this.mechanismIds, record.mechanismIds)
        && Objects.equals(this.mechanismDescriptors, record.mechanismDescriptors)
        && Objects.equals(this.scoreComponents, record.scoreComponents)
        && Objects.equals(this.evidence, record.evidence)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.rank,
        this.productMoleculeIds,
        this.productFormulaSignatures,
        this.productStoichiometrySignatures,
        this.probability,
        this.expectedYieldPercent,
        this.mechanismIds,
        this.mechanismDescriptors,
        this.scoreComponents,
        this.evidence,
        this.note);
  }
}
