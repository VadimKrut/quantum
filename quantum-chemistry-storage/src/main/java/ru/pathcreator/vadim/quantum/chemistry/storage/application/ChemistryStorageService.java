/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionScoreComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionEventKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionSnapshot;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionTimeline;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageConditionSnapshotRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageQuantumProgramRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecordStatus;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text.QchemTextReader;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text.QchemTextWriter;

/**
 * Application facade для чтения, записи и построения chemistry storage document.
 */
public final class ChemistryStorageService {

  private final QchemTextReader reader;
  private final QchemTextWriter writer;

  public ChemistryStorageService() {
    this(new QchemTextReader(), new QchemTextWriter());
  }

  public ChemistryStorageService(final QchemTextReader reader, final QchemTextWriter writer) {
    if (reader == null) {
      throw new IllegalArgumentException("Chemistry storage reader must not be null.");
    }
    if (writer == null) {
      throw new IllegalArgumentException("Chemistry storage writer must not be null.");
    }
    this.reader = reader;
    this.writer = writer;
  }

  public ChemistryStorageResult<ChemistryStorageDocument> read(final String content) {
    return this.reader.read(content);
  }

  public ChemistryStoragePredictionRecord predictionRecord(
      final String id,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final ReactionPredictionResult result,
      final String note) {
    if (result == null) {
      throw new IllegalArgumentException("Reaction prediction result must not be null.");
    }
    return ChemistryStoragePredictionRecord.of(
        id,
        result.request().id(),
        historyIndex,
        result.methodType(),
        status,
        ChemistryStorageService.predictionCandidates(result.candidates()),
        ChemistryStorageService.evidenceItems(result.diagnostics()),
        note);
  }

  public ChemistryStorageComputationPlanRecord computationPlanRecord(
      final String id,
      final String reactionRecordId,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final List<String> classicalPlanIds,
      final List<String> quantumProgramIds,
      final boolean deferredExecution,
      final String note) {
    return this.computationPlanRecord(
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

  public ChemistryStorageComputationPlanRecord computationPlanRecord(
      final String id,
      final String reactionRecordId,
      final int historyIndex,
      final ChemistryStorageReactionRecordStatus status,
      final List<String> classicalPlanIds,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final boolean deferredExecution,
      final String note) {
    return ChemistryStorageComputationPlanRecord.of(
        id,
        reactionRecordId,
        historyIndex,
        status,
        classicalPlanIds,
        classicalResultIds,
        quantumProgramIds,
        deferredExecution,
        note);
  }

  public ChemistryStorageReactionOutcomeRecord reactionOutcomeRecord(
      final String id,
      final String reactionId,
      final int historyIndex,
      final ChemistryStorageReactionOutcomeKind kind,
      final ReactionProductCandidate candidate,
      final List<String> classicalResultIds,
      final List<String> quantumProgramIds,
      final String conditionSummary,
      final String note) {
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction outcome candidate must not be null.");
    }
    return ChemistryStorageReactionOutcomeRecord.of(
        id,
        reactionId,
        historyIndex,
        kind,
        ChemistryStorageService.productMoleculeIds(candidate),
        candidate.probability(),
        candidate.expectedYieldPercent(),
        ChemistryStorageService.mechanismIds(candidate),
        classicalResultIds,
        quantumProgramIds,
        conditionSummary,
        note);
  }

  public ChemistryStorageQuantumProgramRecord quantumProgramRecord(
      final String id,
      final String sourceRecordId,
      final int historyIndex,
      final String kind,
      final String target,
      final String nativePayload,
      final String exportedPayload,
      final List<String> diagnostics,
      final String note) {
    return ChemistryStorageQuantumProgramRecord.of(
        id,
        sourceRecordId,
        historyIndex,
        kind,
        target,
        nativePayload,
        exportedPayload,
        diagnostics,
        note);
  }

  public ChemistryStorageReactionHistoryEvent reactionHistoryEvent(
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
    return ChemistryStorageReactionHistoryEvent.of(
        id,
        reactionId,
        historyIndex,
        kind,
        title,
        description,
        reactionRecordIds,
        conditionSnapshotRecordIds,
        predictionRecordIds,
        computationPlanRecordIds,
        classicalResultIds,
        quantumProgramIds);
  }

  public ChemistryStorageResult<String> write(final ChemistryStorageDocument document) {
    try {
      return ChemistryStorageResult.success(this.writer.write(document), List.of());
    } catch (final RuntimeException exception) {
      return ChemistryStorageResult.failure(
          List.of(
              ChemistryStorageDiagnostic.of(
                  ChemistryStorageDiagnosticSeverity.ERROR,
                  ChemistryStorageDiagnosticCode.DOMAIN_REJECTED_VALUE,
                  exception.getMessage() == null ? "Storage write failed." : exception.getMessage(),
                  0)));
    }
  }

  public ChemistryStorageResult<ChemistryStorageDocument> roundTrip(
      final ChemistryStorageDocument document) {
    final ChemistryStorageResult<String> writeResult = this.write(document);
    if (!writeResult.success()) {
      return ChemistryStorageResult.failure(writeResult.diagnostics());
    }
    return this.read(writeResult.value());
  }

  public ChemistryStorageReactionTimeline reactionTimeline(
      final ChemistryStorageDocument document, final String reactionId) {
    if (document == null) {
      throw new IllegalArgumentException("Chemistry storage document must not be null.");
    }
    final ArrayList<ChemistryStorageReactionRecord> records =
        new ArrayList<ChemistryStorageReactionRecord>();
    final List<ChemistryStorageReactionRecord> documentRecords = document.reactionRecords();
    for (int i = 0; i < documentRecords.size(); ++i) {
      final ChemistryStorageReactionRecord record = documentRecords.get(i);
      if (record.reaction().id().value().equals(reactionId)) {
        records.add(record);
      }
    }
    final ArrayList<ChemistryStorageReactionHistoryEvent> events =
        new ArrayList<ChemistryStorageReactionHistoryEvent>();
    final List<ChemistryStorageReactionHistoryEvent> documentEvents =
        document.reactionHistoryEvents();
    for (int i = 0; i < documentEvents.size(); ++i) {
      final ChemistryStorageReactionHistoryEvent event = documentEvents.get(i);
      if (event.reactionId().equals(reactionId)) {
        events.add(event);
      }
    }
    return ChemistryStorageReactionTimeline.of(reactionId, records, events);
  }

  public ChemistryStorageReactionSnapshot reactionSnapshot(
      final ChemistryStorageDocument document, final String reactionId, final int historyIndex) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException(
          "Storage reaction snapshot history index must be non-negative.");
    }
    final ChemistryStorageReactionTimeline timeline = this.reactionTimeline(document, reactionId);
    final ChemistryStorageReactionRecord reactionRecord =
        ChemistryStorageService.latestRecordAtOrBefore(timeline, historyIndex);
    return ChemistryStorageReactionSnapshot.of(
        reactionId,
        historyIndex,
        reactionRecord,
        ChemistryStorageService.conditionSnapshotRecordsAt(document, reactionId, historyIndex),
        ChemistryStorageService.outcomeRecordsAt(document, reactionId, historyIndex),
        ChemistryStorageService.predictionRecordsAt(document, timeline, historyIndex),
        ChemistryStorageService.roadmapRecordsAt(document, timeline, historyIndex),
        ChemistryStorageService.computationPlanRecordsAt(document, timeline, historyIndex),
        ChemistryStorageService.classicalResultRecordsAt(
            document, timeline, reactionId, historyIndex),
        ChemistryStorageService.quantumProgramRecordsAt(
            document, timeline, reactionId, historyIndex),
        ChemistryStorageService.historyEventsAt(timeline, historyIndex));
  }

  private static ChemistryStorageReactionRecord latestRecordAtOrBefore(
      final ChemistryStorageReactionTimeline timeline, final int historyIndex) {
    ChemistryStorageReactionRecord selected = null;
    final List<ChemistryStorageReactionRecord> records = timeline.records();
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageReactionRecord record = records.get(i);
      if (record.historyIndex() <= historyIndex) {
        selected = record;
      }
    }
    if (selected == null) {
      throw new IllegalArgumentException(
          "Storage reaction snapshot has no reaction record at requested history.");
    }
    return selected;
  }

  private static List<ChemistryStorageReactionHistoryEvent> historyEventsAt(
      final ChemistryStorageReactionTimeline timeline, final int historyIndex) {
    final List<ChemistryStorageReactionHistoryEvent> events = timeline.events();
    final ArrayList<ChemistryStorageReactionHistoryEvent> result =
        new ArrayList<ChemistryStorageReactionHistoryEvent>(events.size());
    for (int i = 0; i < events.size(); ++i) {
      final ChemistryStorageReactionHistoryEvent event = events.get(i);
      if (event.historyIndex() <= historyIndex) {
        result.add(event);
      }
    }
    return List.copyOf(result);
  }

  private static List<ChemistryStoragePredictionRecord> predictionRecordsAt(
      final ChemistryStorageDocument document,
      final ChemistryStorageReactionTimeline timeline,
      final int historyIndex) {
    final List<ChemistryStorageReactionHistoryEvent> events =
        ChemistryStorageService.historyEventsAt(timeline, historyIndex);
    final ArrayList<String> predictionIds = new ArrayList<String>();
    for (int eventIndex = 0; eventIndex < events.size(); ++eventIndex) {
      final List<String> ids = events.get(eventIndex).predictionRecordIds();
      for (int idIndex = 0; idIndex < ids.size(); ++idIndex) {
        if (!predictionIds.contains(ids.get(idIndex))) {
          predictionIds.add(ids.get(idIndex));
        }
      }
    }
    final List<ChemistryStoragePredictionRecord> records = document.predictionRecords();
    final boolean legacySingleReaction =
        document.reactionRecords().stream().map(record -> record.reaction().id()).distinct().count()
                == 1
            && document.reactionHistoryEvents().stream()
                .allMatch(event -> event.predictionRecordIds().isEmpty());
    final ArrayList<ChemistryStoragePredictionRecord> result =
        new ArrayList<ChemistryStoragePredictionRecord>(records.size());
    for (int recordIndex = 0; recordIndex < records.size(); ++recordIndex) {
      final ChemistryStoragePredictionRecord record = records.get(recordIndex);
      if (record.historyIndex() <= historyIndex
          && (predictionIds.contains(record.id()) || legacySingleReaction)) {
        result.add(record);
      }
    }
    return List.copyOf(result);
  }

  private static List<ChemistryStorageReactionOutcomeRecord> outcomeRecordsAt(
      final ChemistryStorageDocument document, final String reactionId, final int historyIndex) {
    final List<ChemistryStorageReactionOutcomeRecord> records = document.reactionOutcomeRecords();
    final ArrayList<ChemistryStorageReactionOutcomeRecord> result =
        new ArrayList<ChemistryStorageReactionOutcomeRecord>(records.size());
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageReactionOutcomeRecord record = records.get(i);
      if (record.historyIndex() <= historyIndex && record.reactionId().equals(reactionId)) {
        result.add(record);
      }
    }
    return List.copyOf(result);
  }

  private static List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecordsAt(
      final ChemistryStorageDocument document, final String reactionId, final int historyIndex) {
    final List<ChemistryStorageConditionSnapshotRecord> records =
        document.conditionSnapshotRecords();
    final ArrayList<ChemistryStorageConditionSnapshotRecord> result =
        new ArrayList<ChemistryStorageConditionSnapshotRecord>(records.size());
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageConditionSnapshotRecord record = records.get(i);
      if (record.historyIndex() <= historyIndex && record.reactionId().equals(reactionId)) {
        result.add(record);
      }
    }
    return List.copyOf(result);
  }

  private static List<ChemistryStorageComputationPlanRecord> computationPlanRecordsAt(
      final ChemistryStorageDocument document,
      final ChemistryStorageReactionTimeline timeline,
      final int historyIndex) {
    final ArrayList<String> reactionRecordIds = new ArrayList<String>();
    final List<ChemistryStorageReactionRecord> reactionRecords = timeline.records();
    for (int i = 0; i < reactionRecords.size(); ++i) {
      final ChemistryStorageReactionRecord record = reactionRecords.get(i);
      if (record.historyIndex() <= historyIndex && !reactionRecordIds.contains(record.id())) {
        reactionRecordIds.add(record.id());
      }
    }
    final List<ChemistryStorageComputationPlanRecord> records = document.computationPlanRecords();
    final ArrayList<ChemistryStorageComputationPlanRecord> result =
        new ArrayList<ChemistryStorageComputationPlanRecord>(records.size());
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageComputationPlanRecord record = records.get(i);
      if (record.historyIndex() <= historyIndex
          && reactionRecordIds.contains(record.reactionRecordId())) {
        result.add(record);
      }
    }
    return List.copyOf(result);
  }

  private static List<ChemistryStorageRoadmapRecord> roadmapRecordsAt(
      final ChemistryStorageDocument document,
      final ChemistryStorageReactionTimeline timeline,
      final int historyIndex) {
    final java.util.Set<String> requests =
        ChemistryStorageService.predictionRecordsAt(document, timeline, historyIndex).stream()
            .map(ChemistryStoragePredictionRecord::requestId)
            .collect(java.util.stream.Collectors.toSet());
    final boolean legacySingleReaction =
        document.predictionRecords().isEmpty()
            && document.reactionRecords().stream()
                    .map(record -> record.reaction().id())
                    .distinct()
                    .count()
                == 1;
    final List<ChemistryStorageRoadmapRecord> records = document.roadmapRecords();
    final ArrayList<ChemistryStorageRoadmapRecord> result =
        new ArrayList<ChemistryStorageRoadmapRecord>(records.size());
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageRoadmapRecord record = records.get(i);
      if (record.historyIndex() <= historyIndex
          && (requests.contains(record.requestId()) || legacySingleReaction)) {
        result.add(record);
      }
    }
    return List.copyOf(result);
  }

  private static List<ChemistryStorageClassicalResultRecord> classicalResultRecordsAt(
      final ChemistryStorageDocument document,
      final ChemistryStorageReactionTimeline timeline,
      final String reactionId,
      final int historyIndex) {
    final ArrayList<String> classicalResultIds = new ArrayList<String>();
    final List<ChemistryStorageReactionHistoryEvent> events =
        ChemistryStorageService.historyEventsAt(timeline, historyIndex);
    for (int eventIndex = 0; eventIndex < events.size(); ++eventIndex) {
      ChemistryStorageService.addMissingIds(
          classicalResultIds, events.get(eventIndex).classicalResultIds());
    }
    final List<ChemistryStorageComputationPlanRecord> computationPlans =
        ChemistryStorageService.computationPlanRecordsAt(document, timeline, historyIndex);
    for (int planIndex = 0; planIndex < computationPlans.size(); ++planIndex) {
      ChemistryStorageService.addMissingIds(
          classicalResultIds, computationPlans.get(planIndex).classicalResultIds());
    }
    final List<ChemistryStorageReactionOutcomeRecord> outcomes =
        ChemistryStorageService.outcomeRecordsAt(document, reactionId, historyIndex);
    for (int outcomeIndex = 0; outcomeIndex < outcomes.size(); ++outcomeIndex) {
      ChemistryStorageService.addMissingIds(
          classicalResultIds, outcomes.get(outcomeIndex).classicalResultIds());
    }
    final List<ChemistryStorageClassicalResultRecord> records = document.classicalResultRecords();
    final ArrayList<ChemistryStorageClassicalResultRecord> result =
        new ArrayList<ChemistryStorageClassicalResultRecord>(records.size());
    for (int recordIndex = 0; recordIndex < records.size(); ++recordIndex) {
      final ChemistryStorageClassicalResultRecord record = records.get(recordIndex);
      if (record.historyIndex() <= historyIndex && classicalResultIds.contains(record.id())) {
        result.add(record);
      }
    }
    return List.copyOf(result);
  }

  private static List<ChemistryStorageQuantumProgramRecord> quantumProgramRecordsAt(
      final ChemistryStorageDocument document,
      final ChemistryStorageReactionTimeline timeline,
      final String reactionId,
      final int historyIndex) {
    final ArrayList<String> quantumProgramIds = new ArrayList<String>();
    final List<ChemistryStorageReactionHistoryEvent> events =
        ChemistryStorageService.historyEventsAt(timeline, historyIndex);
    for (int eventIndex = 0; eventIndex < events.size(); ++eventIndex) {
      ChemistryStorageService.addMissingIds(
          quantumProgramIds, events.get(eventIndex).quantumProgramIds());
    }
    final List<ChemistryStorageComputationPlanRecord> computationPlans =
        ChemistryStorageService.computationPlanRecordsAt(document, timeline, historyIndex);
    for (int planIndex = 0; planIndex < computationPlans.size(); ++planIndex) {
      ChemistryStorageService.addMissingIds(
          quantumProgramIds, computationPlans.get(planIndex).quantumProgramIds());
    }
    final List<ChemistryStorageReactionOutcomeRecord> outcomes =
        ChemistryStorageService.outcomeRecordsAt(document, reactionId, historyIndex);
    for (int outcomeIndex = 0; outcomeIndex < outcomes.size(); ++outcomeIndex) {
      ChemistryStorageService.addMissingIds(
          quantumProgramIds, outcomes.get(outcomeIndex).quantumProgramIds());
    }
    final List<ChemistryStorageQuantumProgramRecord> records = document.quantumProgramRecords();
    final ArrayList<ChemistryStorageQuantumProgramRecord> result =
        new ArrayList<ChemistryStorageQuantumProgramRecord>(records.size());
    for (int recordIndex = 0; recordIndex < records.size(); ++recordIndex) {
      final ChemistryStorageQuantumProgramRecord record = records.get(recordIndex);
      if (record.historyIndex() <= historyIndex && quantumProgramIds.contains(record.id())) {
        result.add(record);
      }
    }
    return List.copyOf(result);
  }

  private static void addMissingIds(final List<String> target, final List<String> source) {
    for (int i = 0; i < source.size(); ++i) {
      if (target.contains(source.get(i))) {
        continue;
      }
      target.add(source.get(i));
    }
  }

  private static List<ChemistryStoragePredictionCandidateRecord> predictionCandidates(
      final List<ReactionProductCandidate> candidates) {
    final ArrayList<ChemistryStoragePredictionCandidateRecord> records =
        new ArrayList<ChemistryStoragePredictionCandidateRecord>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      records.add(
          ChemistryStoragePredictionCandidateRecord.of(
              i,
              ChemistryStorageService.productMoleculeIds(candidate),
              ChemistryStorageService.productFormulaSignatures(candidate),
              ChemistryStorageService.productStoichiometrySignatures(candidate),
              candidate.probability(),
              candidate.expectedYieldPercent(),
              ChemistryStorageService.mechanismIds(candidate),
              ChemistryStorageService.mechanismDescriptors(candidate),
              ChemistryStorageService.scoreComponents(candidate),
              ChemistryStorageService.evidenceItems(candidate.evidence()),
              candidate.selectivityNote()));
    }
    return List.copyOf(records);
  }

  private static List<String> productMoleculeIds(final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    final ArrayList<String> ids = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      ids.add(participants.get(i).molecule().id().value());
    }
    return List.copyOf(ids);
  }

  private static List<String> productFormulaSignatures(final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    final ArrayList<String> formulas = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      formulas.add(participants.get(i).molecule().formula().hillNotation());
    }
    return List.copyOf(formulas);
  }

  private static List<String> productStoichiometrySignatures(
      final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    final ArrayList<String> signatures = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      signatures.add(
          participant.coefficient().value()
              + "*"
              + participant.molecule().id().value()
              + ":"
              + participant.molecule().formula().hillNotation());
    }
    return List.copyOf(signatures);
  }

  private static List<String> mechanismIds(final ReactionProductCandidate candidate) {
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    final ArrayList<String> ids = new ArrayList<String>(mechanisms.size());
    for (int i = 0; i < mechanisms.size(); ++i) {
      ids.add(mechanisms.get(i).mechanismId());
    }
    return List.copyOf(ids);
  }

  private static List<String> mechanismDescriptors(final ReactionProductCandidate candidate) {
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    final ArrayList<String> descriptors = new ArrayList<String>(mechanisms.size());
    for (int i = 0; i < mechanisms.size(); ++i) {
      final PredictedReactionMechanism mechanism = mechanisms.get(i);
      descriptors.add(ChemistryStorageService.mechanismDescriptor(mechanism));
    }
    return List.copyOf(descriptors);
  }

  private static String mechanismDescriptor(final PredictedReactionMechanism mechanism) {
    final StringBuilder builder = new StringBuilder();
    builder
        .append(mechanism.mechanismId())
        .append(":")
        .append(mechanism.family().name())
        .append(":p=")
        .append(mechanism.pathwayProbability())
        .append(":yield=")
        .append(mechanism.expectedYieldPercent());
    if (!mechanism.hasDetailedMechanism()) {
      return builder.toString();
    }
    builder
        .append(":steps=")
        .append(mechanism.detailedMechanism().steps().size())
        .append(":events=")
        .append(mechanism.detailedMechanism().eventCount())
        .append(":energyPoints=")
        .append(mechanism.detailedMechanism().energyProfile().size())
        .append(":trace=");
    for (int stepIndex = 0; stepIndex < mechanism.detailedMechanism().steps().size(); ++stepIndex) {
      if (stepIndex > 0) {
        builder.append(">");
      }
      builder.append(mechanism.detailedMechanism().steps().get(stepIndex).id());
      builder.append("[");
      for (int eventIndex = 0;
          eventIndex < mechanism.detailedMechanism().steps().get(stepIndex).events().size();
          ++eventIndex) {
        if (eventIndex > 0) {
          builder.append(",");
        }
        builder.append(
            mechanism
                .detailedMechanism()
                .steps()
                .get(stepIndex)
                .events()
                .get(eventIndex)
                .kind()
                .name());
      }
      builder.append("]");
    }
    return builder.toString();
  }

  private static List<String> scoreComponents(final ReactionProductCandidate candidate) {
    if (!candidate.hasScoreBreakdown()) {
      return List.of();
    }
    final List<ReactionPredictionScoreComponent> components =
        candidate.scoreBreakdown().components();
    final ArrayList<String> values = new ArrayList<String>(components.size());
    for (int i = 0; i < components.size(); ++i) {
      final ReactionPredictionScoreComponent component = components.get(i);
      values.add(component.kind() + ":" + component.weight() + ":" + component.description());
    }
    return List.copyOf(values);
  }

  private static List<String> evidenceItems(final List<ReactionPredictionEvidence> evidence) {
    final ArrayList<String> values = new ArrayList<String>(evidence.size());
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      values.add(item.kind() + ":" + item.weight() + ":" + item.description());
    }
    return List.copyOf(values);
  }
}
