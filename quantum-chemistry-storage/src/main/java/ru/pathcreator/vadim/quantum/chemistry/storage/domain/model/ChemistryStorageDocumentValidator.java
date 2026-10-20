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
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;

/**
 * Инварианты storage document, которые не относятся к химической валидности самих агрегатов.
 */
final class ChemistryStorageDocumentValidator {

  private ChemistryStorageDocumentValidator() {}

  static void requireUniqueMolecules(final List<Molecule> molecules) {
    for (int i = 0; i < molecules.size(); ++i) {
      final String id = molecules.get(i).id().value();
      for (int j = i + 1; j < molecules.size(); ++j) {
        if (!id.equals(molecules.get(j).id().value())) {
          continue;
        }
        throw new IllegalArgumentException("Storage document contains duplicate molecule id.");
      }
    }
  }

  static void requireUniqueReactions(final List<Reaction> reactions) {
    for (int i = 0; i < reactions.size(); ++i) {
      final String id = reactions.get(i).id().value();
      for (int j = i + 1; j < reactions.size(); ++j) {
        if (!id.equals(reactions.get(j).id().value())) {
          continue;
        }
        throw new IllegalArgumentException("Storage document contains duplicate reaction id.");
      }
    }
  }

  static void requireUniqueMechanisms(final List<ReactionMechanism> mechanisms) {
    for (int i = 0; i < mechanisms.size(); ++i) {
      final String id = mechanisms.get(i).id();
      for (int j = i + 1; j < mechanisms.size(); ++j) {
        if (!id.equals(mechanisms.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException("Storage document contains duplicate mechanism id.");
      }
    }
  }

  static void requireUniqueReactionRecords(final List<ChemistryStorageReactionRecord> records) {
    for (int i = 0; i < records.size(); ++i) {
      final String id = records.get(i).id();
      for (int j = i + 1; j < records.size(); ++j) {
        if (!id.equals(records.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate reaction record id.");
      }
    }
  }

  static void requireUniqueReactionHistoryEvents(
      final List<ChemistryStorageReactionHistoryEvent> events) {
    for (int i = 0; i < events.size(); ++i) {
      final String id = events.get(i).id();
      for (int j = i + 1; j < events.size(); ++j) {
        if (!id.equals(events.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate reaction history event id.");
      }
    }
  }

  static void requireUniqueReactionOutcomeRecords(
      final List<ChemistryStorageReactionOutcomeRecord> records) {
    for (int i = 0; i < records.size(); ++i) {
      final String id = records.get(i).id();
      for (int j = i + 1; j < records.size(); ++j) {
        if (!id.equals(records.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate reaction outcome record id.");
      }
    }
  }

  static void requireUniqueConditionSnapshotRecords(
      final List<ChemistryStorageConditionSnapshotRecord> records) {
    for (int i = 0; i < records.size(); ++i) {
      final String id = records.get(i).id();
      for (int j = i + 1; j < records.size(); ++j) {
        if (!id.equals(records.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate condition snapshot record id.");
      }
    }
  }

  static void requireUniquePredictionRecords(final List<ChemistryStoragePredictionRecord> records) {
    for (int i = 0; i < records.size(); ++i) {
      final String id = records.get(i).id();
      for (int j = i + 1; j < records.size(); ++j) {
        if (!id.equals(records.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate prediction record id.");
      }
    }
  }

  static void requireUniqueComputationPlanRecords(
      final List<ChemistryStorageComputationPlanRecord> records) {
    for (int i = 0; i < records.size(); ++i) {
      final String id = records.get(i).id();
      for (int j = i + 1; j < records.size(); ++j) {
        if (!id.equals(records.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate computation plan record id.");
      }
    }
  }

  static void requireUniqueClassicalResultRecords(
      final List<ChemistryStorageClassicalResultRecord> records) {
    for (int i = 0; i < records.size(); ++i) {
      final String id = records.get(i).id();
      for (int j = i + 1; j < records.size(); ++j) {
        if (!id.equals(records.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate classical result record id.");
      }
    }
  }

  static void requireUniqueQuantumProgramRecords(
      final List<ChemistryStorageQuantumProgramRecord> records) {
    for (int i = 0; i < records.size(); ++i) {
      final String id = records.get(i).id();
      for (int j = i + 1; j < records.size(); ++j) {
        if (!id.equals(records.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate quantum program record id.");
      }
    }
  }

  static void requireReactionMoleculesPresent(
      final List<Reaction> reactions, final List<Molecule> molecules) {
    for (int i = 0; i < reactions.size(); ++i) {
      ChemistryStorageDocumentValidator.requireSideMoleculesPresent(
          reactions.get(i).reactants(), molecules);
      ChemistryStorageDocumentValidator.requireSideMoleculesPresent(
          reactions.get(i).products(), molecules);
    }
  }

  static void requireMechanismReactionsPresent(
      final List<ReactionMechanism> mechanisms, final List<Reaction> reactions) {
    for (int i = 0; i < mechanisms.size(); ++i) {
      final ReactionMechanism mechanism = mechanisms.get(i);
      ChemistryStorageDocumentValidator.requireReactionPresent(
          mechanism.overallReaction(),
          reactions,
          "Storage mechanism references overall reaction missing from document.");
      final List<ElementaryReactionStep> steps = mechanism.steps();
      for (int j = 0; j < steps.size(); ++j) {
        ChemistryStorageDocumentValidator.requireReactionPresent(
            steps.get(j).reaction(),
            reactions,
            "Storage mechanism references elementary reaction missing from document.");
      }
    }
  }

  static void requireReactionRecordsPresent(
      final List<ChemistryStorageReactionRecord> records, final List<Reaction> reactions) {
    for (int i = 0; i < records.size(); ++i) {
      ChemistryStorageDocumentValidator.requireReactionPresent(
          records.get(i).reaction(),
          reactions,
          "Storage reaction record references reaction missing from document.");
    }
  }

  static void requireReactionHistoryEventsPresent(
      final List<ChemistryStorageReactionHistoryEvent> events,
      final List<Reaction> reactions,
      final List<ChemistryStorageReactionRecord> reactionRecords,
      final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords,
      final List<ChemistryStoragePredictionRecord> predictionRecords,
      final List<ChemistryStorageComputationPlanRecord> computationPlanRecords,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords) {
    for (int i = 0; i < events.size(); ++i) {
      final ChemistryStorageReactionHistoryEvent event = events.get(i);
      if (!ChemistryStorageDocumentValidator.containsReaction(reactions, event.reactionId())) {
        throw new IllegalArgumentException(
            "Storage reaction history event references reaction missing from document.");
      }
      ChemistryStorageDocumentValidator.requireKnownReactionRecordIds(
          event.reactionRecordIds(), reactionRecords);
      ChemistryStorageDocumentValidator.requireKnownConditionSnapshotRecordIds(
          event.conditionSnapshotRecordIds(), conditionSnapshotRecords);
      ChemistryStorageDocumentValidator.requireKnownPredictionRecordIds(
          event.predictionRecordIds(), predictionRecords);
      ChemistryStorageDocumentValidator.requireKnownComputationPlanIds(
          event.computationPlanRecordIds(), computationPlanRecords);
      ChemistryStorageDocumentValidator.requireKnownClassicalResultIds(
          event.classicalResultIds(), classicalResultRecords);
      ChemistryStorageDocumentValidator.requireKnownQuantumProgramIds(
          event.quantumProgramIds(), quantumProgramRecords);
    }
  }

  static void requireUniqueRoadmapRecords(final List<ChemistryStorageRoadmapRecord> records) {
    for (int i = 0; i < records.size(); ++i) {
      final String id = records.get(i).id();
      for (int j = i + 1; j < records.size(); ++j) {
        if (!id.equals(records.get(j).id())) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage document contains duplicate roadmap record id.");
      }
    }
  }

  static void requireReactionOutcomeRecordsPresent(
      final List<ChemistryStorageReactionOutcomeRecord> records,
      final List<Reaction> reactions,
      final List<Molecule> molecules) {
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageReactionOutcomeRecord record = records.get(i);
      if (!ChemistryStorageDocumentValidator.containsReaction(reactions, record.reactionId())) {
        throw new IllegalArgumentException(
            "Storage reaction outcome record references reaction missing from document.");
      }
      final List<String> productIds = record.productMoleculeIds();
      for (int productIndex = 0; productIndex < productIds.size(); ++productIndex) {
        if (ChemistryStorageDocumentValidator.containsMolecule(
            molecules, productIds.get(productIndex))) {
          continue;
        }
        throw new IllegalArgumentException(
            "Storage reaction outcome record references product molecule missing from document.");
      }
    }
  }

  static void requireReactionOutcomeRecordsLinkKnownWork(
      final List<ChemistryStorageReactionOutcomeRecord> records,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords) {
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageReactionOutcomeRecord record = records.get(i);
      ChemistryStorageDocumentValidator.requireKnownClassicalResultIds(
          record.classicalResultIds(), classicalResultRecords);
      ChemistryStorageDocumentValidator.requireKnownQuantumProgramIds(
          record.quantumProgramIds(), quantumProgramRecords);
    }
  }

  static void requireConditionSnapshotRecordsPresent(
      final List<ChemistryStorageConditionSnapshotRecord> records, final List<Reaction> reactions) {
    for (int i = 0; i < records.size(); ++i) {
      if (ChemistryStorageDocumentValidator.containsReaction(
          reactions, records.get(i).reactionId())) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage condition snapshot record references reaction missing from document.");
    }
  }

  static void requirePredictionRecordsPresent(
      final List<ChemistryStoragePredictionRecord> records, final List<Molecule> molecules) {
    for (int recordIndex = 0; recordIndex < records.size(); ++recordIndex) {
      final List<ChemistryStoragePredictionCandidateRecord> candidates =
          records.get(recordIndex).candidates();
      for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
        ChemistryStorageDocumentValidator.requireCandidateMoleculesPresent(
            candidates.get(candidateIndex), molecules);
      }
    }
  }

  static void requireComputationPlanRecordsPresent(
      final List<ChemistryStorageComputationPlanRecord> records,
      final List<ChemistryStorageReactionRecord> reactionRecords,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords) {
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageComputationPlanRecord record = records.get(i);
      if (!ChemistryStorageDocumentValidator.containsReactionRecord(
          reactionRecords, record.reactionRecordId())) {
        throw new IllegalArgumentException(
            "Storage computation plan references reaction record missing from document.");
      }
      ChemistryStorageDocumentValidator.requireKnownClassicalResultIds(
          record.classicalResultIds(), classicalResultRecords);
      ChemistryStorageDocumentValidator.requireKnownQuantumProgramIds(
          record.quantumProgramIds(), quantumProgramRecords);
    }
  }

  static void requireQuantumProgramRecordsPresent(
      final List<ChemistryStorageQuantumProgramRecord> records,
      final List<ChemistryStorageReactionRecord> reactionRecords,
      final List<ChemistryStorageComputationPlanRecord> computationPlanRecords) {
    for (int i = 0; i < records.size(); ++i) {
      final ChemistryStorageQuantumProgramRecord record = records.get(i);
      if (ChemistryStorageDocumentValidator.containsReactionRecord(
              reactionRecords, record.sourceRecordId())
          || ChemistryStorageDocumentValidator.containsComputationPlanRecord(
              computationPlanRecords, record.sourceRecordId())) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage quantum program references source record missing from document.");
    }
  }

  static void requireRoadmapRecordsPresent(
      final List<ChemistryStorageRoadmapRecord> records,
      final List<Molecule> molecules,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords) {
    for (int recordIndex = 0; recordIndex < records.size(); ++recordIndex) {
      final List<ChemistryStorageRoadmapCandidateRecord> candidates =
          records.get(recordIndex).candidates();
      for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
        final List<String> productIds = candidates.get(candidateIndex).productMoleculeIds();
        for (int productIndex = 0; productIndex < productIds.size(); ++productIndex) {
          if (ChemistryStorageDocumentValidator.containsMolecule(
              molecules, productIds.get(productIndex))) {
            continue;
          }
          throw new IllegalArgumentException(
              "Storage roadmap record references product molecule missing from document.");
        }
        final List<String> targets = candidates.get(candidateIndex).quantumPreparationTargets();
        for (int targetIndex = 0; targetIndex < targets.size(); ++targetIndex) {
          if (targets.get(targetIndex).isBlank()) {
            throw new IllegalArgumentException(
                "Storage roadmap record contains blank quantum preparation target.");
          }
        }
      }
    }
  }

  private static void requireCandidateMoleculesPresent(
      final ChemistryStoragePredictionCandidateRecord candidate, final List<Molecule> molecules) {
    final List<String> ids = candidate.productMoleculeIds();
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryStorageDocumentValidator.containsMolecule(molecules, ids.get(i))) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage prediction record references product molecule missing from document.");
    }
  }

  private static void requireReactionPresent(
      final Reaction reaction, final List<Reaction> reactions, final String message) {
    if (ChemistryStorageDocumentValidator.containsReaction(reactions, reaction.id().value())) {
      return;
    }
    throw new IllegalArgumentException(message);
  }

  private static void requireSideMoleculesPresent(
      final ReactionSide side, final List<Molecule> molecules) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final String moleculeId = participants.get(i).molecule().id().value();
      if (ChemistryStorageDocumentValidator.containsMolecule(molecules, moleculeId)) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage reaction references molecule missing from document.");
    }
  }

  private static boolean containsMolecule(final List<Molecule> molecules, final String moleculeId) {
    for (int i = 0; i < molecules.size(); ++i) {
      if (!molecules.get(i).id().value().equals(moleculeId)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static boolean containsReaction(final List<Reaction> reactions, final String reactionId) {
    for (int i = 0; i < reactions.size(); ++i) {
      if (!reactions.get(i).id().value().equals(reactionId)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static boolean containsReactionRecord(
      final List<ChemistryStorageReactionRecord> records, final String recordId) {
    for (int i = 0; i < records.size(); ++i) {
      if (!records.get(i).id().equals(recordId)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static void requireKnownReactionRecordIds(
      final List<String> ids, final List<ChemistryStorageReactionRecord> records) {
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryStorageDocumentValidator.containsReactionRecord(records, ids.get(i))) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage reaction history event references reaction record missing from document.");
    }
  }

  private static void requireKnownPredictionRecordIds(
      final List<String> ids, final List<ChemistryStoragePredictionRecord> records) {
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryStorageDocumentValidator.containsPredictionRecord(records, ids.get(i))) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage reaction history event references prediction record missing from document.");
    }
  }

  private static void requireKnownConditionSnapshotRecordIds(
      final List<String> ids, final List<ChemistryStorageConditionSnapshotRecord> records) {
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryStorageDocumentValidator.containsConditionSnapshotRecord(records, ids.get(i))) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage reaction history event references condition snapshot record missing from document.");
    }
  }

  private static boolean containsConditionSnapshotRecord(
      final List<ChemistryStorageConditionSnapshotRecord> records, final String recordId) {
    for (int i = 0; i < records.size(); ++i) {
      if (!records.get(i).id().equals(recordId)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static void requireKnownComputationPlanIds(
      final List<String> ids, final List<ChemistryStorageComputationPlanRecord> records) {
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryStorageDocumentValidator.containsComputationPlanRecord(records, ids.get(i))) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage reaction history event references computation plan record missing from document.");
    }
  }

  private static boolean containsPredictionRecord(
      final List<ChemistryStoragePredictionRecord> records, final String recordId) {
    for (int i = 0; i < records.size(); ++i) {
      if (!records.get(i).id().equals(recordId)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static boolean containsComputationPlanRecord(
      final List<ChemistryStorageComputationPlanRecord> records, final String recordId) {
    for (int i = 0; i < records.size(); ++i) {
      if (!records.get(i).id().equals(recordId)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static void requireKnownClassicalResultIds(
      final List<String> ids, final List<ChemistryStorageClassicalResultRecord> records) {
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryStorageDocumentValidator.containsClassicalResultRecord(records, ids.get(i))) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage record references classical result missing from document.");
    }
  }

  private static boolean containsClassicalResultRecord(
      final List<ChemistryStorageClassicalResultRecord> records, final String recordId) {
    for (int i = 0; i < records.size(); ++i) {
      if (!records.get(i).id().equals(recordId)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static void requireKnownQuantumProgramIds(
      final List<String> ids, final List<ChemistryStorageQuantumProgramRecord> records) {
    for (int i = 0; i < ids.size(); ++i) {
      if (ChemistryStorageDocumentValidator.containsQuantumProgramRecord(records, ids.get(i))) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage record references quantum program missing from document.");
    }
  }

  private static boolean containsQuantumProgramRecord(
      final List<ChemistryStorageQuantumProgramRecord> records, final String recordId) {
    for (int i = 0; i < records.size(); ++i) {
      if (!records.get(i).id().equals(recordId)) {
        continue;
      }
      return true;
    }
    return false;
  }
}
