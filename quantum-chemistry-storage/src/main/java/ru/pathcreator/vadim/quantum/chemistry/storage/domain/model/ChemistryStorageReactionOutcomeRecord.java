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
 * Ветка исхода реакции: продукты, вероятность, выход, механизм и расчетные артефакты.
 */
public final class ChemistryStorageReactionOutcomeRecord {

  private final String id;
  private final String reactionId;
  private final int historyIndex;
  private final ChemistryStorageReactionOutcomeKind kind;
  private final List<String> productMoleculeIds;
  private final double probability;
  private final Double yieldPercent;
  private final List<String> mechanismIds;
  private final List<String> classicalResultIds;
  private final List<String> quantumProgramIds;
  private final String conditionSummary;
  private final String note;

  private ChemistryStorageReactionOutcomeRecord(
      final String id,
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionOutcomeKind kind,
      final List<String> productMoleculeIds,
      final double probability,
      final Double yieldPercent,
      final List<String> mechanismIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final String conditionSummary,
      final String note) {
    this.id = id;
    this.reactionId = reactionId;
    this.historyIndex = historyIndex;
    this.kind = kind;
    this.productMoleculeIds = productMoleculeIds;
    this.probability = probability;
    this.yieldPercent = yieldPercent;
    this.mechanismIds = mechanismIds;
    this.classicalResultIds = classicalResultIds;
    this.quantumProgramIds = quantumProgramIds;
    this.conditionSummary = conditionSummary;
    this.note = note;
  }

  public static ChemistryStorageReactionOutcomeRecord of(
      final String id,
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionOutcomeKind kind,
      final List<String> productMoleculeIds,
      final double probability,
      final Double yieldPercent,
      final List<String> mechanismIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final String conditionSummary,
      final String note) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException("Reaction outcome history index must be non-negative.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Reaction outcome kind must not be null.");
    }
    if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("Reaction outcome probability must be in [0, 1].");
    }
    if (yieldPercent != null
        && (!Double.isFinite(yieldPercent) || yieldPercent < 0.0 || yieldPercent > 100.0)) {
      throw new IllegalArgumentException("Reaction outcome yield percent must be in [0, 100].");
    }
    return new ChemistryStorageReactionOutcomeRecord(
        IdentifierValue.requireIdentifier(id, "Reaction outcome id"),
        IdentifierValue.requireIdentifier(reactionId, "Reaction outcome reaction id"),
        historyIndex,
        kind,
        ChemistryStorageReactionOutcomeRecord.checkedIds(
            productMoleculeIds, "Reaction outcome product molecule id", true),
        probability,
        yieldPercent,
        ChemistryStorageReactionOutcomeRecord.checkedIds(
            mechanismIds, "Reaction outcome mechanism id", false),
        ChemistryStorageReactionOutcomeRecord.checkedIds(
            classicalResultIds, "Reaction outcome classical result id", false),
        ChemistryStorageReactionOutcomeRecord.checkedIds(
            quantumProgramIds, "Reaction outcome quantum program id", false),
        TextValue.optionalText(conditionSummary),
        TextValue.optionalText(note));
  }

  public String id() {
    return this.id;
  }

  public String reactionId() {
    return this.reactionId;
  }

  public int historyIndex() {
    return this.historyIndex;
  }

  public ChemistryStorageReactionOutcomeKind kind() {
    return this.kind;
  }

  public List<String> productMoleculeIds() {
    return this.productMoleculeIds;
  }

  public double probability() {
    return this.probability;
  }

  public Double yieldPercent() {
    return this.yieldPercent;
  }

  public boolean hasYieldPercent() {
    return this.yieldPercent != null;
  }

  public List<String> mechanismIds() {
    return this.mechanismIds;
  }

  public List<String> classicalResultIds() {
    return this.classicalResultIds;
  }

  public List<String> quantumProgramIds() {
    return this.quantumProgramIds;
  }

  public String conditionSummary() {
    return this.conditionSummary;
  }

  public boolean hasConditionSummary() {
    return this.conditionSummary != null;
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
  }

  private static List<String> checkedIds(
      final List<String> ids, final String subjectName, final boolean required) {
    if (ids == null || ids.isEmpty()) {
      if (required) {
        throw new IllegalArgumentException(subjectName + " list must not be empty.");
      }
      return List.of();
    }
    final String[] checked = new String[ids.size()];
    for (int i = 0; i < ids.size(); ++i) {
      checked[i] = IdentifierValue.requireIdentifier(ids.get(i), subjectName);
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageReactionOutcomeRecord)) {
      return false;
    }
    final ChemistryStorageReactionOutcomeRecord record =
        (ChemistryStorageReactionOutcomeRecord) other;
    return this.historyIndex == record.historyIndex
        && Double.compare(this.probability, record.probability) == 0
        && Objects.equals(this.id, record.id)
        && Objects.equals(this.reactionId, record.reactionId)
        && this.kind == record.kind
        && Objects.equals(this.productMoleculeIds, record.productMoleculeIds)
        && Objects.equals(this.yieldPercent, record.yieldPercent)
        && Objects.equals(this.mechanismIds, record.mechanismIds)
        && Objects.equals(this.classicalResultIds, record.classicalResultIds)
        && Objects.equals(this.quantumProgramIds, record.quantumProgramIds)
        && Objects.equals(this.conditionSummary, record.conditionSummary)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.reactionId,
        this.historyIndex,
        this.kind,
        this.productMoleculeIds,
        this.probability,
        this.yieldPercent,
        this.mechanismIds,
        this.classicalResultIds,
        this.quantumProgramIds,
        this.conditionSummary,
        this.note);
  }
}
