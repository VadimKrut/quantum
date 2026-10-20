/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageConditionSnapshotRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageQuantumProgramRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;

/**
 * Итоговый storage-пакет workflow: реакция, прогнозы, исходы, roadmap и шаг истории.
 */
public final class ChemistryWorkflowStoragePlan {

  public static final ChemistryWorkflowStoragePlan EMPTY =
      new ChemistryWorkflowStoragePlan(
          null, null, null, null, List.of(), List.of(), List.of(), List.of(), null, List.of(),
          List.of());

  private final ChemistryStorageReactionRecord reactionRecord;
  private final ChemistryStoragePredictionRecord predictionRecord;
  private final ChemistryStorageComputationPlanRecord computationPlanRecord;
  private final ChemistryStorageRoadmapRecord roadmapRecord;
  private final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords;
  private final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords;
  private final List<ChemistryStorageClassicalResultRecord> classicalResultRecords;
  private final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords;
  private final ChemistryStorageReactionHistoryEvent historyEvent;
  private final List<ChemistryStorageExtension> extensions;
  private final List<Molecule> additionalMolecules;

  private ChemistryWorkflowStoragePlan(
      final ChemistryStorageReactionRecord reactionRecord,
      final ChemistryStoragePredictionRecord predictionRecord,
      final ChemistryStorageComputationPlanRecord computationPlanRecord,
      final ChemistryStorageRoadmapRecord roadmapRecord,
      final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords,
      final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords,
      final ChemistryStorageReactionHistoryEvent historyEvent,
      final List<ChemistryStorageExtension> extensions,
      final List<Molecule> additionalMolecules) {
    this.reactionRecord = reactionRecord;
    this.predictionRecord = predictionRecord;
    this.computationPlanRecord = computationPlanRecord;
    this.roadmapRecord = roadmapRecord;
    this.conditionSnapshotRecords = conditionSnapshotRecords;
    this.outcomeRecords = outcomeRecords;
    this.classicalResultRecords = classicalResultRecords;
    this.quantumProgramRecords = quantumProgramRecords;
    this.historyEvent = historyEvent;
    this.extensions = extensions;
    this.additionalMolecules = additionalMolecules;
  }

  public static ChemistryWorkflowStoragePlan of(
      final ChemistryStorageReactionRecord reactionRecord,
      final ChemistryStoragePredictionRecord predictionRecord,
      final ChemistryStorageComputationPlanRecord computationPlanRecord) {
    return ChemistryWorkflowStoragePlan.of(
        reactionRecord,
        predictionRecord,
        computationPlanRecord,
        null,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        null,
        List.of(),
        List.of());
  }

  public static ChemistryWorkflowStoragePlan of(
      final ChemistryStorageReactionRecord reactionRecord,
      final ChemistryStoragePredictionRecord predictionRecord,
      final ChemistryStorageComputationPlanRecord computationPlanRecord,
      final List<Molecule> additionalMolecules) {
    return ChemistryWorkflowStoragePlan.of(
        reactionRecord,
        predictionRecord,
        computationPlanRecord,
        null,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        null,
        List.of(),
        additionalMolecules);
  }

  public static ChemistryWorkflowStoragePlan of(
      final ChemistryStorageReactionRecord reactionRecord,
      final ChemistryStoragePredictionRecord predictionRecord,
      final ChemistryStorageComputationPlanRecord computationPlanRecord,
      final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords,
      final ChemistryStorageReactionHistoryEvent historyEvent,
      final List<Molecule> additionalMolecules) {
    return ChemistryWorkflowStoragePlan.of(
        reactionRecord,
        predictionRecord,
        computationPlanRecord,
        null,
        List.of(),
        outcomeRecords,
        List.of(),
        List.of(),
        historyEvent,
        List.of(),
        additionalMolecules);
  }

  public static ChemistryWorkflowStoragePlan ofMolecularCalculation(
      final List<Molecule> molecules,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageExtension> extensions) {
    final List<Molecule> checkedMolecules =
        ChemistryWorkflowStoragePlan.checkedMolecules(molecules);
    final List<ChemistryStorageClassicalResultRecord> checkedClassicalResults =
        ChemistryWorkflowStoragePlan.checkedClassicalResults(classicalResultRecords);
    final List<ChemistryStorageExtension> checkedExtensions =
        ChemistryWorkflowStoragePlan.checkedExtensions(extensions);
    if (checkedMolecules.isEmpty()
        && checkedClassicalResults.isEmpty()
        && checkedExtensions.isEmpty()) {
      return EMPTY;
    }
    return new ChemistryWorkflowStoragePlan(
        null,
        null,
        null,
        null,
        List.of(),
        List.of(),
        checkedClassicalResults,
        List.of(),
        null,
        checkedExtensions,
        checkedMolecules);
  }

  public static ChemistryWorkflowStoragePlan of(
      final ChemistryStorageReactionRecord reactionRecord,
      final ChemistryStoragePredictionRecord predictionRecord,
      final ChemistryStorageComputationPlanRecord computationPlanRecord,
      final ChemistryStorageRoadmapRecord roadmapRecord,
      final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords,
      final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords,
      final ChemistryStorageReactionHistoryEvent historyEvent,
      final List<ChemistryStorageExtension> extensions,
      final List<Molecule> additionalMolecules) {
    if (reactionRecord == null || predictionRecord == null || computationPlanRecord == null) {
      throw new IllegalArgumentException(
          "Reaction workflow storage plan requires reaction, prediction and computation records.");
    }
    return new ChemistryWorkflowStoragePlan(
        reactionRecord,
        predictionRecord,
        computationPlanRecord,
        roadmapRecord,
        ChemistryWorkflowStoragePlan.checkedConditionSnapshots(conditionSnapshotRecords),
        ChemistryWorkflowStoragePlan.checkedOutcomes(outcomeRecords),
        ChemistryWorkflowStoragePlan.checkedClassicalResults(classicalResultRecords),
        ChemistryWorkflowStoragePlan.checkedQuantumPrograms(quantumProgramRecords),
        historyEvent,
        ChemistryWorkflowStoragePlan.checkedExtensions(extensions),
        ChemistryWorkflowStoragePlan.checkedMolecules(additionalMolecules));
  }

  public static ChemistryWorkflowStoragePlan of(
      final ChemistryStorageReactionRecord reactionRecord,
      final ChemistryStoragePredictionRecord predictionRecord,
      final ChemistryStorageComputationPlanRecord computationPlanRecord,
      final ChemistryStorageReactionHistoryEvent historyEvent,
      final List<Molecule> additionalMolecules) {
    return ChemistryWorkflowStoragePlan.of(
        reactionRecord,
        predictionRecord,
        computationPlanRecord,
        null,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        historyEvent,
        List.of(),
        additionalMolecules);
  }

  public boolean empty() {
    return this.reactionRecord == null
        && this.predictionRecord == null
        && this.computationPlanRecord == null
        && this.roadmapRecord == null
        && this.conditionSnapshotRecords.isEmpty()
        && this.outcomeRecords.isEmpty()
        && this.classicalResultRecords.isEmpty()
        && this.quantumProgramRecords.isEmpty()
        && this.historyEvent == null
        && this.extensions.isEmpty()
        && this.additionalMolecules.isEmpty();
  }

  public boolean hasReactionRecord() {
    return !this.empty() && this.reactionRecord != null;
  }

  public ChemistryStorageReactionRecord reactionRecord() {
    if (!this.hasReactionRecord()) {
      throw new IllegalStateException(
          "Chemistry workflow storage plan does not contain reaction record.");
    }
    return this.reactionRecord;
  }

  public boolean hasPredictionRecord() {
    return !this.empty() && this.predictionRecord != null;
  }

  public ChemistryStoragePredictionRecord predictionRecord() {
    if (!this.hasPredictionRecord()) {
      throw new IllegalStateException(
          "Chemistry workflow storage plan does not contain prediction record.");
    }
    return this.predictionRecord;
  }

  public boolean hasComputationPlanRecord() {
    return !this.empty() && this.computationPlanRecord != null;
  }

  public ChemistryStorageComputationPlanRecord computationPlanRecord() {
    if (!this.hasComputationPlanRecord()) {
      throw new IllegalStateException(
          "Chemistry workflow storage plan does not contain computation plan record.");
    }
    return this.computationPlanRecord;
  }

  public ChemistryStorageRoadmapRecord roadmapRecord() {
    if (this.empty()) {
      throw new IllegalStateException("Chemistry workflow storage plan is empty.");
    }
    return this.roadmapRecord;
  }

  public boolean hasRoadmapRecord() {
    return !this.empty() && this.roadmapRecord != null;
  }

  public List<ChemistryStorageReactionOutcomeRecord> outcomeRecords() {
    if (this.empty()) {
      throw new IllegalStateException("Chemistry workflow storage plan is empty.");
    }
    return this.outcomeRecords;
  }

  public List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords() {
    if (this.empty()) {
      throw new IllegalStateException("Chemistry workflow storage plan is empty.");
    }
    return this.conditionSnapshotRecords;
  }

  public boolean hasConditionSnapshotRecords() {
    return !this.empty() && !this.conditionSnapshotRecords.isEmpty();
  }

  public boolean hasOutcomeRecords() {
    return !this.empty() && !this.outcomeRecords.isEmpty();
  }

  public List<ChemistryStorageClassicalResultRecord> classicalResultRecords() {
    if (this.empty()) {
      throw new IllegalStateException("Chemistry workflow storage plan is empty.");
    }
    return this.classicalResultRecords;
  }

  public boolean hasClassicalResultRecords() {
    return !this.empty() && !this.classicalResultRecords.isEmpty();
  }

  public List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords() {
    if (this.empty()) {
      throw new IllegalStateException("Chemistry workflow storage plan is empty.");
    }
    return this.quantumProgramRecords;
  }

  public boolean hasQuantumProgramRecords() {
    return !this.empty() && !this.quantumProgramRecords.isEmpty();
  }

  public ChemistryStorageReactionHistoryEvent historyEvent() {
    if (this.empty()) {
      throw new IllegalStateException("Chemistry workflow storage plan is empty.");
    }
    return this.historyEvent;
  }

  public boolean hasHistoryEvent() {
    return !this.empty() && this.historyEvent != null;
  }

  public List<ChemistryStorageExtension> extensions() {
    if (this.empty()) {
      throw new IllegalStateException("Chemistry workflow storage plan is empty.");
    }
    return this.extensions;
  }

  public boolean hasExtensions() {
    return !this.empty() && !this.extensions.isEmpty();
  }

  public ChemistryStorageDocument toDocument(final String projectId, final String displayName) {
    if (this.empty()) {
      throw new IllegalStateException(
          "Empty chemistry workflow storage plan cannot build document.");
    }
    final ChemistryStorageDocument.Builder builder =
        ChemistryStorageDocument.builder(
            IdentifierValue.requireIdentifier(projectId, "Workflow storage project id"),
            TextValue.requireText(displayName, "Workflow storage display name"));
    if (this.reactionRecord != null) {
      builder.reaction(this.reactionRecord.reaction());
      builder.reactionRecord(this.reactionRecord);
    }
    if (this.predictionRecord != null) {
      builder.predictionRecord(this.predictionRecord);
    }
    if (this.computationPlanRecord != null) {
      builder.computationPlanRecord(this.computationPlanRecord);
    }
    if (this.roadmapRecord != null) {
      builder.roadmapRecord(this.roadmapRecord);
    }
    for (int i = 0; i < this.conditionSnapshotRecords.size(); ++i) {
      builder.conditionSnapshotRecord(this.conditionSnapshotRecords.get(i));
    }
    for (int i = 0; i < this.outcomeRecords.size(); ++i) {
      builder.reactionOutcomeRecord(this.outcomeRecords.get(i));
    }
    for (int i = 0; i < this.classicalResultRecords.size(); ++i) {
      builder.classicalResultRecord(this.classicalResultRecords.get(i));
    }
    for (int i = 0; i < this.quantumProgramRecords.size(); ++i) {
      builder.quantumProgramRecord(this.quantumProgramRecords.get(i));
    }
    if (this.historyEvent != null) {
      builder.reactionHistoryEvent(this.historyEvent);
    }
    for (int i = 0; i < this.extensions.size(); ++i) {
      builder.extension(this.extensions.get(i));
    }
    final List<Molecule> molecules = this.molecules();
    for (int i = 0; i < molecules.size(); ++i) {
      builder.molecule(molecules.get(i));
    }
    return builder.build();
  }

  private List<Molecule> molecules() {
    final LinkedHashMap<String, Molecule> molecules = new LinkedHashMap<String, Molecule>();
    if (this.reactionRecord != null) {
      ChemistryWorkflowStoragePlan.collectMolecules(
          this.reactionRecord.reaction().reactants().participants(), molecules);
      ChemistryWorkflowStoragePlan.collectMolecules(
          this.reactionRecord.reaction().products().participants(), molecules);
    }
    for (int i = 0; i < this.additionalMolecules.size(); ++i) {
      final Molecule molecule = this.additionalMolecules.get(i);
      molecules.putIfAbsent(molecule.id().value(), molecule);
    }
    return List.copyOf(molecules.values());
  }

  private static List<Molecule> checkedMolecules(final List<Molecule> molecules) {
    if (molecules == null || molecules.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < molecules.size(); ++i) {
      if (molecules.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Chemistry workflow storage molecule must not be null.");
    }
    return List.copyOf(molecules);
  }

  private static List<ChemistryStorageReactionOutcomeRecord> checkedOutcomes(
      final List<ChemistryStorageReactionOutcomeRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Chemistry workflow storage outcome record must not be null.");
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageConditionSnapshotRecord> checkedConditionSnapshots(
      final List<ChemistryStorageConditionSnapshotRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Chemistry workflow storage condition snapshot must not be null.");
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageClassicalResultRecord> checkedClassicalResults(
      final List<ChemistryStorageClassicalResultRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Chemistry workflow storage classical result must not be null.");
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageQuantumProgramRecord> checkedQuantumPrograms(
      final List<ChemistryStorageQuantumProgramRecord> records) {
    if (records == null || records.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < records.size(); ++i) {
      if (records.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Chemistry workflow storage quantum program must not be null.");
    }
    return List.copyOf(records);
  }

  private static List<ChemistryStorageExtension> checkedExtensions(
      final List<ChemistryStorageExtension> extensions) {
    if (extensions == null || extensions.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < extensions.size(); ++i) {
      if (extensions.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Chemistry workflow storage extension must not be null.");
    }
    return List.copyOf(extensions);
  }

  private static void collectMolecules(
      final List<ReactionParticipant> participants, final Map<String, Molecule> molecules) {
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      molecules.putIfAbsent(molecule.id().value(), molecule);
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowStoragePlan)) {
      return false;
    }
    final ChemistryWorkflowStoragePlan plan = (ChemistryWorkflowStoragePlan) other;
    return Objects.equals(this.reactionRecord, plan.reactionRecord)
        && Objects.equals(this.predictionRecord, plan.predictionRecord)
        && Objects.equals(this.computationPlanRecord, plan.computationPlanRecord)
        && Objects.equals(this.roadmapRecord, plan.roadmapRecord)
        && Objects.equals(this.conditionSnapshotRecords, plan.conditionSnapshotRecords)
        && Objects.equals(this.outcomeRecords, plan.outcomeRecords)
        && Objects.equals(this.classicalResultRecords, plan.classicalResultRecords)
        && Objects.equals(this.quantumProgramRecords, plan.quantumProgramRecords)
        && Objects.equals(this.historyEvent, plan.historyEvent)
        && Objects.equals(this.extensions, plan.extensions)
        && Objects.equals(this.additionalMolecules, plan.additionalMolecules);
  }

  public int hashCode() {
    return Objects.hash(
        this.reactionRecord,
        this.predictionRecord,
        this.computationPlanRecord,
        this.roadmapRecord,
        this.conditionSnapshotRecords,
        this.outcomeRecords,
        this.classicalResultRecords,
        this.quantumProgramRecords,
        this.historyEvent,
        this.extensions,
        this.additionalMolecules);
  }
}
