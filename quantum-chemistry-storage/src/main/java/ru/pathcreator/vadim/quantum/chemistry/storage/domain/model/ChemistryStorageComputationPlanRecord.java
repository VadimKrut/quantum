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

/** План расчетов, привязанный к записи истории реакции и подготовленным classical/quantum задачам. */
public final class ChemistryStorageComputationPlanRecord {

  private final String id;
  private final String reactionRecordId;
  private final int historyIndex;
  private final ChemistryStorageReactionRecordStatus status;
  private final List<String> classicalPlanIds;
  private final List<String> classicalResultIds;
  private final List<String> quantumProgramIds;
  private final boolean deferredExecution;
  private final String note;

  private ChemistryStorageComputationPlanRecord(
      final String id,
      final String reactionRecordId,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final boolean deferredExecution,
      final String note) {
    this.id = id;
    this.reactionRecordId = reactionRecordId;
    this.historyIndex = historyIndex;
    this.status = status;
    this.classicalPlanIds = classicalPlanIds;
    this.classicalResultIds = classicalResultIds;
    this.quantumProgramIds = quantumProgramIds;
    this.deferredExecution = deferredExecution;
    this.note = note;
  }

  public static ChemistryStorageComputationPlanRecord of(
      final String id,
      final String reactionRecordId,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final List<String> classicalPlanIds,
      final List<String> quantumProgramIds,
      final boolean deferredExecution,
      final String note) {
    return ChemistryStorageComputationPlanRecord.of(
        id,
        reactionRecordId,
        historyIndex,
        status,
        classicalPlanIds,
        List.of(),
        quantumProgramIds,
        deferredExecution,
        note);
  }

  public static ChemistryStorageComputationPlanRecord of(
      final String id,
      final String reactionRecordId,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final boolean deferredExecution,
      final String note) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException("Computation plan history index must be non-negative.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Computation plan status must not be null.");
    }
    return new ChemistryStorageComputationPlanRecord(
        IdentifierValue.requireIdentifier(id, "Computation plan id"),
        IdentifierValue.requireIdentifier(reactionRecordId, "Computation plan reaction record id"),
        historyIndex,
        status,
        ChemistryStorageComputationPlanRecord.checkedIds(classicalPlanIds, "classical plan id"),
        ChemistryStorageComputationPlanRecord.checkedIds(classicalResultIds, "classical result id"),
        ChemistryStorageComputationPlanRecord.checkedIds(quantumProgramIds, "quantum program id"),
        deferredExecution,
        TextValue.optionalText(note));
  }

  public String id() {
    return this.id;
  }

  public String reactionRecordId() {
    return this.reactionRecordId;
  }

  public int historyIndex() {
    return this.historyIndex;
  }

  public ChemistryStorageReactionRecordStatus status() {
    return this.status;
  }

  public List<String> classicalPlanIds() {
    return this.classicalPlanIds;
  }

  public List<String> classicalResultIds() {
    return this.classicalResultIds;
  }

  public List<String> quantumProgramIds() {
    return this.quantumProgramIds;
  }

  public boolean deferredExecution() {
    return this.deferredExecution;
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
  }

  public boolean hasPreparedWork() {
    return !this.classicalPlanIds.isEmpty()
        || !this.classicalResultIds.isEmpty()
        || !this.quantumProgramIds.isEmpty();
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

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageComputationPlanRecord)) {
      return false;
    }
    final ChemistryStorageComputationPlanRecord record =
        (ChemistryStorageComputationPlanRecord) other;
    return this.historyIndex == record.historyIndex
        && this.deferredExecution == record.deferredExecution
        && Objects.equals(this.id, record.id)
        && Objects.equals(this.reactionRecordId, record.reactionRecordId)
        && this.status == record.status
        && Objects.equals(this.classicalPlanIds, record.classicalPlanIds)
        && Objects.equals(this.classicalResultIds, record.classicalResultIds)
        && Objects.equals(this.quantumProgramIds, record.quantumProgramIds)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.reactionRecordId,
        this.historyIndex,
        this.status,
        this.classicalPlanIds,
        this.classicalResultIds,
        this.quantumProgramIds,
        this.deferredExecution,
        this.note);
  }
}
