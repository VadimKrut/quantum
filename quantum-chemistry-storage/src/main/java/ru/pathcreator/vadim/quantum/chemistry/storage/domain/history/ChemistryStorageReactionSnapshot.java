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
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageConditionSnapshotRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageQuantumProgramRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;

/**
 * Снимок состояния реакции на конкретном шаге истории.
 */
public final class ChemistryStorageReactionSnapshot {

  private final String reactionId;
  private final int historyIndex;
  private final ChemistryStorageReactionRecord reactionRecord;
  private final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords;
  private final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords;
  private final List<ChemistryStoragePredictionRecord> predictionRecords;
  private final List<ChemistryStorageRoadmapRecord> roadmapRecords;
  private final List<ChemistryStorageComputationPlanRecord> computationPlanRecords;
  private final List<ChemistryStorageClassicalResultRecord> classicalResultRecords;
  private final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords;
  private final List<ChemistryStorageReactionHistoryEvent> historyEvents;

  private ChemistryStorageReactionSnapshot(
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionRecord reactionRecord,
      final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords,
      final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords,
      final List<ChemistryStoragePredictionRecord> predictionRecords,
      final List<ChemistryStorageRoadmapRecord> roadmapRecords,
      final List<ChemistryStorageComputationPlanRecord> computationPlanRecords,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords,
      final List<ChemistryStorageReactionHistoryEvent> historyEvents) {
    this.reactionId = reactionId;
    this.historyIndex = historyIndex;
    this.reactionRecord = reactionRecord;
    this.conditionSnapshotRecords = conditionSnapshotRecords;
    this.outcomeRecords = outcomeRecords;
    this.predictionRecords = predictionRecords;
    this.roadmapRecords = roadmapRecords;
    this.computationPlanRecords = computationPlanRecords;
    this.classicalResultRecords = classicalResultRecords;
    this.quantumProgramRecords = quantumProgramRecords;
    this.historyEvents = historyEvents;
  }

  public static ChemistryStorageReactionSnapshot of(
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionRecord reactionRecord,
      final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords,
      final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords,
      final List<ChemistryStoragePredictionRecord> predictionRecords,
      final List<ChemistryStorageRoadmapRecord> roadmapRecords,
      final List<ChemistryStorageComputationPlanRecord> computationPlanRecords,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords,
      final List<ChemistryStorageReactionHistoryEvent> historyEvents) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException(
          "Storage reaction snapshot history index must be non-negative.");
    }
    if (reactionRecord == null) {
      throw new IllegalArgumentException(
          "Storage reaction snapshot reaction record must not be null.");
    }
    final String checkedReactionId =
        IdentifierValue.requireIdentifier(reactionId, "Storage reaction snapshot reaction id");
    if (!checkedReactionId.equals(reactionRecord.reaction().id().value())) {
      throw new IllegalArgumentException(
          "Storage reaction snapshot record belongs to another reaction.");
    }
    return new ChemistryStorageReactionSnapshot(
        checkedReactionId,
        historyIndex,
        reactionRecord,
        ChemistryStorageReactionSnapshot.checkedConditionSnapshots(
            checkedReactionId, conditionSnapshotRecords),
        ChemistryStorageReactionSnapshot.checkedOutcomes(checkedReactionId, outcomeRecords),
        ChemistryStorageReactionSnapshot.checkedPredictions(predictionRecords),
        ChemistryStorageReactionSnapshot.checkedRoadmaps(roadmapRecords),
        ChemistryStorageReactionSnapshot.checkedComputationPlans(computationPlanRecords),
        ChemistryStorageReactionSnapshot.checkedClassicalResults(classicalResultRecords),
        ChemistryStorageReactionSnapshot.checkedQuantumPrograms(quantumProgramRecords),
        ChemistryStorageReactionSnapshot.checkedEvents(checkedReactionId, historyEvents));
  }

  public String reactionId() {
    return this.reactionId;
  }

  public int historyIndex() {
    return this.historyIndex;
  }

  public ChemistryStorageReactionRecord reactionRecord() {
    return this.reactionRecord;
  }

  public List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords() {
    return this.conditionSnapshotRecords;
  }

  public boolean hasConditionSnapshots() {
    return !this.conditionSnapshotRecords.isEmpty();
  }

  public List<ChemistryStorageReactionOutcomeRecord> outcomeRecords() {
    return this.outcomeRecords;
  }

  public boolean hasOutcomes() {
    return !this.outcomeRecords.isEmpty();
  }

  public List<ChemistryStoragePredictionRecord> predictionRecords() {
    return this.predictionRecords;
  }

  public boolean hasPredictions() {
    return !this.predictionRecords.isEmpty();
  }

  public List<ChemistryStorageRoadmapRecord> roadmapRecords() {
    return this.roadmapRecords;
  }

  public boolean hasRoadmaps() {
    return !this.roadmapRecords.isEmpty();
  }

  public List<ChemistryStorageComputationPlanRecord> computationPlanRecords() {
    return this.computationPlanRecords;
  }

  public boolean hasComputationPlans() {
    return !this.computationPlanRecords.isEmpty();
  }

  public List<ChemistryStorageClassicalResultRecord> classicalResultRecords() {
    return this.classicalResultRecords;
  }

  public boolean hasClassicalResults() {
    return !this.classicalResultRecords.isEmpty();
  }

  public List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords() {
    return this.quantumProgramRecords;
  }

  public boolean hasQuantumPrograms() {
    return !this.quantumProgramRecords.isEmpty();
  }

  public List<ChemistryStorageReactionHistoryEvent> historyEvents() {
    return this.historyEvents;
  }

  public boolean hasHistoryEvents() {
    return !this.historyEvents.isEmpty();
  }

  private static List<ChemistryStoragePredictionRecord> checkedPredictions(
      final List<ChemistryStoragePredictionRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) == null) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot prediction record must not be null.");
      }
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageRoadmapRecord> checkedRoadmaps(
      final List<ChemistryStorageRoadmapRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) == null) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot roadmap record must not be null.");
      }
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageConditionSnapshotRecord> checkedConditionSnapshots(
      final String reactionId, final List<ChemistryStorageConditionSnapshotRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageConditionSnapshotRecord record = records.get(i);
      if (record == null) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot condition record must not be null.");
      }
      if (!reactionId.equals(record.reactionId())) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot condition belongs to another reaction.");
      }
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageReactionOutcomeRecord> checkedOutcomes(
      final String reactionId, final List<ChemistryStorageReactionOutcomeRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageReactionOutcomeRecord record = records.get(i);
      if (record == null) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot outcome record must not be null.");
      }
      if (!reactionId.equals(record.reactionId())) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot outcome belongs to another reaction.");
      }
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageComputationPlanRecord> checkedComputationPlans(
      final List<ChemistryStorageComputationPlanRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) == null) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot computation plan record must not be null.");
      }
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageClassicalResultRecord> checkedClassicalResults(
      final List<ChemistryStorageClassicalResultRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) == null) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot classical result record must not be null.");
      }
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageQuantumProgramRecord> checkedQuantumPrograms(
      final List<ChemistryStorageQuantumProgramRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) == null) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot quantum program record must not be null.");
      }
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageReactionHistoryEvent> checkedEvents(
      final String reactionId, final List<ChemistryStorageReactionHistoryEvent> events) {
    if (events == null || events.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < events.size(); ++i) {
      final ChemistryStorageReactionHistoryEvent event = events.get(i);
      if (event == null) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot history event must not be null.");
      }
      if (!reactionId.equals(event.reactionId())) {
        throw new IllegalArgumentException(
            "Storage reaction snapshot event belongs to another reaction.");
      }
    }
    return List.copyOf(events);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageReactionSnapshot)) {
      return false;
    }
    final ChemistryStorageReactionSnapshot snapshot = (ChemistryStorageReactionSnapshot) other;
    return this.historyIndex == snapshot.historyIndex
        && Objects.equals(this.reactionId, snapshot.reactionId)
        && Objects.equals(this.reactionRecord, snapshot.reactionRecord)
        && Objects.equals(this.conditionSnapshotRecords, snapshot.conditionSnapshotRecords)
        && Objects.equals(this.outcomeRecords, snapshot.outcomeRecords)
        && Objects.equals(this.predictionRecords, snapshot.predictionRecords)
        && Objects.equals(this.roadmapRecords, snapshot.roadmapRecords)
        && Objects.equals(this.computationPlanRecords, snapshot.computationPlanRecords)
        && Objects.equals(this.classicalResultRecords, snapshot.classicalResultRecords)
        && Objects.equals(this.quantumProgramRecords, snapshot.quantumProgramRecords)
        && Objects.equals(this.historyEvents, snapshot.historyEvents);
  }

  public int hashCode() {
    return Objects.hash(
        this.reactionId,
        this.historyIndex,
        this.reactionRecord,
        this.conditionSnapshotRecords,
        this.outcomeRecords,
        this.predictionRecords,
        this.roadmapRecords,
        this.computationPlanRecords,
        this.classicalResultRecords,
        this.quantumProgramRecords,
        this.historyEvents);
  }
}
