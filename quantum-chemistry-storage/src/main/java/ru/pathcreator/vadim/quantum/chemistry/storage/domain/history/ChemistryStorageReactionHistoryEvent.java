/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.history;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Одно событие в истории реакции, связывающее ручные правки, прогнозы и расчеты.
 */
public final class ChemistryStorageReactionHistoryEvent {

  private final String id;
  private final String reactionId;
  private final int historyIndex;
  private final ChemistryStorageReactionEventKind kind;
  private final String title;
  private final String description;
  private final List<String> reactionRecordIds;
  private final List<String> conditionSnapshotRecordIds;
  private final List<String> predictionRecordIds;
  private final List<String> computationPlanRecordIds;
  private final List<String> classicalResultIds;
  private final List<String> quantumProgramIds;

  private ChemistryStorageReactionHistoryEvent(
      final String id,
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionEventKind kind,
      final String title,
      final String description,
      final List<String> reactionRecordIds,
      final List<String> conditionSnapshotRecordIds,
      final List<String> predictionRecordIds,
      final List<String> computationPlanRecordIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds) {
    this.id = id;
    this.reactionId = reactionId;
    this.historyIndex = historyIndex;
    this.kind = kind;
    this.title = title;
    this.description = description;
    this.reactionRecordIds = reactionRecordIds;
    this.conditionSnapshotRecordIds = conditionSnapshotRecordIds;
    this.predictionRecordIds = predictionRecordIds;
    this.computationPlanRecordIds = computationPlanRecordIds;
    this.classicalResultIds = classicalResultIds;
    this.quantumProgramIds = quantumProgramIds;
  }

  public static ChemistryStorageReactionHistoryEvent of(
      final String id,
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionEventKind kind,
      final String title,
      final String description,
      final List<String> reactionRecordIds,
      final List<String> conditionSnapshotRecordIds,
      final List<String> predictionRecordIds,
      final List<String> computationPlanRecordIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException("Reaction history event index must be non-negative.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Reaction history event kind must not be null.");
    }
    return new ChemistryStorageReactionHistoryEvent(
        IdentifierValue.requireIdentifier(id, "Reaction history event id"),
        IdentifierValue.requireIdentifier(reactionId, "Reaction history event reaction id"),
        historyIndex,
        kind,
        TextValue.requireText(title, "Reaction history event title"),
        TextValue.optionalText(description),
        ChemistryStorageReactionHistoryEvent.checkedIds(
            reactionRecordIds, "Reaction history event reaction record id"),
        ChemistryStorageReactionHistoryEvent.checkedIds(
            conditionSnapshotRecordIds, "Reaction history event condition snapshot record id"),
        ChemistryStorageReactionHistoryEvent.checkedIds(
            predictionRecordIds, "Reaction history event prediction record id"),
        ChemistryStorageReactionHistoryEvent.checkedIds(
            computationPlanRecordIds, "Reaction history event computation plan record id"),
        ChemistryStorageReactionHistoryEvent.checkedIds(
            classicalResultIds, "Reaction history event classical result id"),
        ChemistryStorageReactionHistoryEvent.checkedIds(
            quantumProgramIds, "Reaction history event quantum program id"));
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

  public ChemistryStorageReactionEventKind kind() {
    return this.kind;
  }

  public String title() {
    return this.title;
  }

  public String description() {
    return this.description;
  }

  public boolean hasDescription() {
    return this.description != null;
  }

  public List<String> reactionRecordIds() {
    return this.reactionRecordIds;
  }

  public List<String> conditionSnapshotRecordIds() {
    return this.conditionSnapshotRecordIds;
  }

  public List<String> predictionRecordIds() {
    return this.predictionRecordIds;
  }

  public List<String> computationPlanRecordIds() {
    return this.computationPlanRecordIds;
  }

  public List<String> classicalResultIds() {
    return this.classicalResultIds;
  }

  public List<String> quantumProgramIds() {
    return this.quantumProgramIds;
  }

  public boolean referencesWork() {
    return !this.reactionRecordIds.isEmpty()
        || !this.conditionSnapshotRecordIds.isEmpty()
        || !this.predictionRecordIds.isEmpty()
        || !this.computationPlanRecordIds.isEmpty()
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
    if (!(other instanceof ChemistryStorageReactionHistoryEvent)) {
      return false;
    }
    final ChemistryStorageReactionHistoryEvent event = (ChemistryStorageReactionHistoryEvent) other;
    return this.historyIndex == event.historyIndex
        && Objects.equals(this.id, event.id)
        && Objects.equals(this.reactionId, event.reactionId)
        && this.kind == event.kind
        && Objects.equals(this.title, event.title)
        && Objects.equals(this.description, event.description)
        && Objects.equals(this.reactionRecordIds, event.reactionRecordIds)
        && Objects.equals(this.conditionSnapshotRecordIds, event.conditionSnapshotRecordIds)
        && Objects.equals(this.predictionRecordIds, event.predictionRecordIds)
        && Objects.equals(this.computationPlanRecordIds, event.computationPlanRecordIds)
        && Objects.equals(this.classicalResultIds, event.classicalResultIds)
        && Objects.equals(this.quantumProgramIds, event.quantumProgramIds);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.reactionId,
        this.historyIndex,
        this.kind,
        this.title,
        this.description,
        this.reactionRecordIds,
        this.conditionSnapshotRecordIds,
        this.predictionRecordIds,
        this.computationPlanRecordIds,
        this.classicalResultIds,
        this.quantumProgramIds);
  }
}
