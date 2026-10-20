/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;

/**
 * Полный документ хранения химического проекта: молекулы, реакции, metadata и расширения.
 */
public final class ChemistryStorageDocument {

  private final LaboratoryExperiment laboratoryExperiment;
  private final ChemistryStorageFormatVersion version;
  private final String projectId;
  private final String displayName;
  private final Map<String, String> metadata;
  private final List<Molecule> molecules;
  private final List<Reaction> reactions;
  private final List<ReactionMechanism> mechanisms;
  private final List<ChemistryStorageReactionRecord> reactionRecords;
  private final List<ChemistryStorageReactionOutcomeRecord> reactionOutcomeRecords;
  private final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords;
  private final List<ChemistryStorageReactionHistoryEvent> reactionHistoryEvents;
  private final List<ChemistryStoragePredictionRecord> predictionRecords;
  private final List<ChemistryStorageComputationPlanRecord> computationPlanRecords;
  private final List<ChemistryStorageClassicalResultRecord> classicalResultRecords;
  private final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords;
  private final List<ChemistryStorageRoadmapRecord> roadmapRecords;
  private final List<ChemistryStorageExtension> extensions;

  private ChemistryStorageDocument(
      final LaboratoryExperiment laboratoryExperiment,
      final ChemistryStorageFormatVersion version,
      final String projectId,
      final String displayName,
      final Map<String, String> metadata,
      final List<Molecule> molecules,
      final List<Reaction> reactions,
      final List<ReactionMechanism> mechanisms,
      final List<ChemistryStorageReactionRecord> reactionRecords,
      final List<ChemistryStorageReactionOutcomeRecord> reactionOutcomeRecords,
      final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords,
      final List<ChemistryStorageReactionHistoryEvent> reactionHistoryEvents,
      final List<ChemistryStoragePredictionRecord> predictionRecords,
      final List<ChemistryStorageComputationPlanRecord> computationPlanRecords,
      final List<ChemistryStorageClassicalResultRecord> classicalResultRecords,
      final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords,
      final List<ChemistryStorageRoadmapRecord> roadmapRecords,
      final List<ChemistryStorageExtension> extensions) {
    this.laboratoryExperiment = laboratoryExperiment;
    this.version = version;
    this.projectId = projectId;
    this.displayName = displayName;
    this.metadata = metadata;
    this.molecules = molecules;
    this.reactions = reactions;
    this.mechanisms = mechanisms;
    this.reactionRecords = reactionRecords;
    this.reactionOutcomeRecords = reactionOutcomeRecords;
    this.conditionSnapshotRecords = conditionSnapshotRecords;
    this.reactionHistoryEvents = reactionHistoryEvents;
    this.predictionRecords = predictionRecords;
    this.computationPlanRecords = computationPlanRecords;
    this.classicalResultRecords = classicalResultRecords;
    this.quantumProgramRecords = quantumProgramRecords;
    this.roadmapRecords = roadmapRecords;
    this.extensions = extensions;
  }

  public static Builder builder(final String projectId, final String displayName) {
    return new Builder(projectId, displayName);
  }

  public LaboratoryExperiment laboratoryExperiment() {
    return this.laboratoryExperiment;
  }

  /** Copies every native section, including computation records and notebook data. */
  public Builder toBuilder() {
    final Builder builder = builder(this.projectId, this.displayName).version(this.version);
    if (this.laboratoryExperiment != null) {
      builder.laboratoryExperiment(this.laboratoryExperiment);
    }
    this.metadata.forEach(builder::metadata);
    this.molecules.forEach(builder::molecule);
    this.reactions.forEach(builder::reaction);
    this.mechanisms.forEach(builder::mechanism);
    this.reactionRecords.forEach(builder::reactionRecord);
    this.reactionOutcomeRecords.forEach(builder::reactionOutcomeRecord);
    this.conditionSnapshotRecords.forEach(builder::conditionSnapshotRecord);
    this.reactionHistoryEvents.forEach(builder::reactionHistoryEvent);
    this.predictionRecords.forEach(builder::predictionRecord);
    this.computationPlanRecords.forEach(builder::computationPlanRecord);
    this.classicalResultRecords.forEach(builder::classicalResultRecord);
    this.quantumProgramRecords.forEach(builder::quantumProgramRecord);
    this.roadmapRecords.forEach(builder::roadmapRecord);
    this.extensions.forEach(builder::extension);
    return builder;
  }

  public ChemistryStorageFormatVersion version() {
    return this.version;
  }

  public String projectId() {
    return this.projectId;
  }

  public String displayName() {
    return this.displayName;
  }

  public Map<String, String> metadata() {
    return this.metadata;
  }

  public List<Molecule> molecules() {
    return this.molecules;
  }

  public List<Reaction> reactions() {
    return this.reactions;
  }

  public List<ReactionMechanism> mechanisms() {
    return this.mechanisms;
  }

  public List<ChemistryStorageReactionRecord> reactionRecords() {
    return this.reactionRecords;
  }

  public List<ChemistryStorageReactionOutcomeRecord> reactionOutcomeRecords() {
    return this.reactionOutcomeRecords;
  }

  public List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords() {
    return this.conditionSnapshotRecords;
  }

  public List<ChemistryStorageReactionHistoryEvent> reactionHistoryEvents() {
    return this.reactionHistoryEvents;
  }

  public List<ChemistryStoragePredictionRecord> predictionRecords() {
    return this.predictionRecords;
  }

  public List<ChemistryStorageComputationPlanRecord> computationPlanRecords() {
    return this.computationPlanRecords;
  }

  public List<ChemistryStorageClassicalResultRecord> classicalResultRecords() {
    return this.classicalResultRecords;
  }

  public List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords() {
    return this.quantumProgramRecords;
  }

  public List<ChemistryStorageRoadmapRecord> roadmapRecords() {
    return this.roadmapRecords;
  }

  public List<ChemistryStorageExtension> extensions() {
    return this.extensions;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageDocument)) {
      return false;
    }
    final ChemistryStorageDocument document = (ChemistryStorageDocument) other;
    return Objects.equals(this.laboratoryExperiment, document.laboratoryExperiment)
        && Objects.equals(this.version, document.version)
        && Objects.equals(this.projectId, document.projectId)
        && Objects.equals(this.displayName, document.displayName)
        && Objects.equals(this.metadata, document.metadata)
        && Objects.equals(this.molecules, document.molecules)
        && Objects.equals(this.reactions, document.reactions)
        && Objects.equals(this.mechanisms, document.mechanisms)
        && Objects.equals(this.reactionRecords, document.reactionRecords)
        && Objects.equals(this.reactionOutcomeRecords, document.reactionOutcomeRecords)
        && Objects.equals(this.conditionSnapshotRecords, document.conditionSnapshotRecords)
        && Objects.equals(this.reactionHistoryEvents, document.reactionHistoryEvents)
        && Objects.equals(this.predictionRecords, document.predictionRecords)
        && Objects.equals(this.computationPlanRecords, document.computationPlanRecords)
        && Objects.equals(this.classicalResultRecords, document.classicalResultRecords)
        && Objects.equals(this.quantumProgramRecords, document.quantumProgramRecords)
        && Objects.equals(this.roadmapRecords, document.roadmapRecords)
        && Objects.equals(this.extensions, document.extensions);
  }

  public int hashCode() {
    return Objects.hash(
        this.laboratoryExperiment,
        this.version,
        this.projectId,
        this.displayName,
        this.metadata,
        this.molecules,
        this.reactions,
        this.mechanisms,
        this.reactionRecords,
        this.reactionOutcomeRecords,
        this.conditionSnapshotRecords,
        this.reactionHistoryEvents,
        this.predictionRecords,
        this.computationPlanRecords,
        this.classicalResultRecords,
        this.quantumProgramRecords,
        this.roadmapRecords,
        this.extensions);
  }

  /**
   * Mutable builder для удобной сборки immutable storage document.
   */
  public static final class Builder {

    private LaboratoryExperiment laboratoryExperiment;

    private final String projectId;
    private final String displayName;
    private ChemistryStorageFormatVersion version = ChemistryStorageFormatVersion.CURRENT;
    private final LinkedHashMap<String, String> metadata = new LinkedHashMap<String, String>();
    private final ArrayList<Molecule> molecules = new ArrayList<Molecule>();
    private final ArrayList<Reaction> reactions = new ArrayList<Reaction>();
    private final ArrayList<ReactionMechanism> mechanisms = new ArrayList<ReactionMechanism>();
    private final ArrayList<ChemistryStorageReactionRecord> reactionRecords =
        new ArrayList<ChemistryStorageReactionRecord>();
    private final ArrayList<ChemistryStorageReactionOutcomeRecord> reactionOutcomeRecords =
        new ArrayList<ChemistryStorageReactionOutcomeRecord>();
    private final ArrayList<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords =
        new ArrayList<ChemistryStorageConditionSnapshotRecord>();
    private final ArrayList<ChemistryStorageReactionHistoryEvent> reactionHistoryEvents =
        new ArrayList<ChemistryStorageReactionHistoryEvent>();
    private final ArrayList<ChemistryStoragePredictionRecord> predictionRecords =
        new ArrayList<ChemistryStoragePredictionRecord>();
    private final ArrayList<ChemistryStorageComputationPlanRecord> computationPlanRecords =
        new ArrayList<ChemistryStorageComputationPlanRecord>();
    private final ArrayList<ChemistryStorageClassicalResultRecord> classicalResultRecords =
        new ArrayList<ChemistryStorageClassicalResultRecord>();
    private final ArrayList<ChemistryStorageQuantumProgramRecord> quantumProgramRecords =
        new ArrayList<ChemistryStorageQuantumProgramRecord>();
    private final ArrayList<ChemistryStorageRoadmapRecord> roadmapRecords =
        new ArrayList<ChemistryStorageRoadmapRecord>();
    private final ArrayList<ChemistryStorageExtension> extensions =
        new ArrayList<ChemistryStorageExtension>();

    private Builder(final String projectId, final String displayName) {
      this.projectId = IdentifierValue.requireIdentifier(projectId, "Storage project id");
      this.displayName = TextValue.requireText(displayName, "Storage project display name");
    }

    private static int laboratoryVersion(final LaboratoryExperiment experiment) {
      if (experiment.analyticalEvidence().entries().stream()
          .anyMatch(
              e ->
                  e.data()
                              instanceof
                              ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                                      .LaboratoryAnalyticalEvidence.Correlations
                                  c
                          && c.spectrum() != null
                      || e.data()
                              instanceof
                              ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                                      .LaboratoryAnalyticalEvidence.Multiplets
                                  m
                          && m.analysisId() != null)) {
        return 17;
      }
      if (!experiment.operations().stageOutcomes().isEmpty()) {
        return 16;
      }
      if (experiment.operations().hasObservationMetadata()) {
        return 15;
      }
      if (experiment.stages().stream().anyMatch(stage -> !stage.classification().empty())) {
        return 14;
      }
      if (experiment.components().stream()
          .anyMatch(
              c ->
                  c.evidence()
                          == ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                              .LaboratoryEvidence.LITERATURE
                      || c.evidence()
                          == ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                              .LaboratoryEvidence.SIMULATED)) {
        return 13;
      }
      if (!experiment.literature().empty()) {
        return 12;
      }
      if (!experiment
              .operations()
              .equals(
                  ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                      .LaboratoryReactionOperations.EMPTY)
          || !experiment.analyticalEvidence().entries().isEmpty()) {
        return 11;
      }
      if (!experiment.route().empty()
          || experiment.samples().stream().anyMatch(sample -> sample.origin() != null)) {
        return 10;
      }
      if (!experiment.procedure().steps().isEmpty() || !experiment.procedure().events().isEmpty()) {
        return 9;
      }
      if (experiment.stages().stream().anyMatch(s -> !s.atomMapping().empty())) {
        return 8;
      }
      if (experiment.analyses().stream()
          .anyMatch(
              a ->
                  a.data()
                          instanceof
                          ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalysis
                                  .Nmr
                              n
                      && n.trace() != null)) {
        return 7;
      }
      if (!experiment.samples().isEmpty()) {
        return 6;
      }
      if (!experiment.analyses().isEmpty()) {
        return 5;
      }
      if (!experiment.stages().isEmpty()) {
        return 4;
      }
      return !experiment.journal().isEmpty()
              || !experiment.attachments().isEmpty()
              || experiment.components().stream()
                  .anyMatch(
                      component ->
                          component.planned() != null
                              || component.actual() != null
                              || component.evidence()
                                  != ru.pathcreator.vadim.quantum.chemistry.domain.notebook
                                      .LaboratoryEvidence.PLANNED)
          ? 3
          : 2;
    }

    public Builder laboratoryExperiment(final LaboratoryExperiment experiment) {
      if (this.laboratoryExperiment != null) {
        throw new IllegalArgumentException(
            "A notebook file contains exactly one laboratory experiment.");
      }
      this.laboratoryExperiment = Objects.requireNonNull(experiment);
      this.version =
          ChemistryStorageFormatVersion.of(
              Math.max(this.version.number(), laboratoryVersion(experiment)));
      return this;
    }

    public Builder version(final ChemistryStorageFormatVersion version) {
      if (version == null) {
        throw new IllegalArgumentException("Storage format version must not be null.");
      }
      this.version = version;
      return this;
    }

    public Builder metadata(final String key, final String value) {
      this.metadata.put(
          IdentifierValue.requireIdentifier(key, "Storage metadata key"),
          TextValue.requireText(value, "Storage metadata value"));
      return this;
    }

    public Builder molecule(final Molecule molecule) {
      if (molecule == null) {
        throw new IllegalArgumentException("Storage molecule must not be null.");
      }
      this.molecules.add(molecule);
      return this;
    }

    public Builder removeMetadata(final String key) {
      this.metadata.remove(key);
      return this;
    }

    public Builder reaction(final Reaction reaction) {
      if (reaction == null) {
        throw new IllegalArgumentException("Storage reaction must not be null.");
      }
      this.reactions.add(reaction);
      return this;
    }

    public Builder mechanism(final ReactionMechanism mechanism) {
      if (mechanism == null) {
        throw new IllegalArgumentException("Storage mechanism must not be null.");
      }
      this.mechanisms.add(mechanism);
      return this;
    }

    public Builder reactionRecord(final ChemistryStorageReactionRecord record) {
      if (record == null) {
        throw new IllegalArgumentException("Storage reaction record must not be null.");
      }
      this.reactionRecords.add(record);
      return this;
    }

    public Builder reactionOutcomeRecord(final ChemistryStorageReactionOutcomeRecord record) {
      if (record == null) {
        throw new IllegalArgumentException("Storage reaction outcome record must not be null.");
      }
      this.reactionOutcomeRecords.add(record);
      return this;
    }

    public Builder conditionSnapshotRecord(final ChemistryStorageConditionSnapshotRecord record) {
      if (record == null) {
        throw new IllegalArgumentException("Storage condition snapshot record must not be null.");
      }
      this.conditionSnapshotRecords.add(record);
      return this;
    }

    public Builder reactionHistoryEvent(final ChemistryStorageReactionHistoryEvent event) {
      if (event == null) {
        throw new IllegalArgumentException("Storage reaction history event must not be null.");
      }
      this.reactionHistoryEvents.add(event);
      return this;
    }

    public Builder predictionRecord(final ChemistryStoragePredictionRecord record) {
      if (record == null) {
        throw new IllegalArgumentException("Storage prediction record must not be null.");
      }
      this.predictionRecords.add(record);
      return this;
    }

    public Builder computationPlanRecord(final ChemistryStorageComputationPlanRecord record) {
      if (record == null) {
        throw new IllegalArgumentException("Storage computation plan record must not be null.");
      }
      this.computationPlanRecords.add(record);
      return this;
    }

    public Builder classicalResultRecord(final ChemistryStorageClassicalResultRecord record) {
      if (record == null) {
        throw new IllegalArgumentException("Storage classical result record must not be null.");
      }
      this.classicalResultRecords.add(record);
      return this;
    }

    public Builder quantumProgramRecord(final ChemistryStorageQuantumProgramRecord record) {
      if (record == null) {
        throw new IllegalArgumentException("Storage quantum program record must not be null.");
      }
      this.quantumProgramRecords.add(record);
      return this;
    }

    public Builder roadmapRecord(final ChemistryStorageRoadmapRecord record) {
      if (record == null) {
        throw new IllegalArgumentException("Storage roadmap record must not be null.");
      }
      this.roadmapRecords.add(record);
      return this;
    }

    public Builder extension(final ChemistryStorageExtension extension) {
      if (extension == null) {
        throw new IllegalArgumentException("Storage extension must not be null.");
      }
      this.extensions.add(extension);
      return this;
    }

    public ChemistryStorageDocument build() {
      if (this.laboratoryExperiment != null) {
        if (this.version.number() < laboratoryVersion(this.laboratoryExperiment)) {
          throw new IllegalArgumentException(
              "Laboratory data requires QCHEM version "
                  + laboratoryVersion(this.laboratoryExperiment)
                  + ".");
        }
        for (final var outcome : this.laboratoryExperiment.operations().stageOutcomes()) {
          for (final var product :
              java.util.stream.Stream.concat(
                      outcome.targets().stream(), outcome.byproducts().stream())
                  .toList()) {
            if (!this.molecules.contains(product.snapshot())) {
              throw new IllegalArgumentException(
                  "Stage outcome product must reference a stored molecule snapshot.");
            }
          }
        }
        for (final var evidence : this.laboratoryExperiment.analyticalEvidence().entries()) {
          if (!this.molecules.contains(evidence.molecule())) {
            throw new IllegalArgumentException(
                "Analytical evidence must reference a stored molecule snapshot.");
          }
        }
        for (final var sample : this.laboratoryExperiment.samples()) {
          if (!this.molecules.contains(sample.molecule())) {
            throw new IllegalArgumentException("Sample snapshot must reference a stored molecule.");
          }
        }
        for (final var analysis : this.laboratoryExperiment.analyses()) {
          if (!this.molecules.contains(analysis.molecule())) {
            throw new IllegalArgumentException(
                "Analysis snapshot must reference a stored molecule.");
          }
        }
        for (final var component : this.laboratoryExperiment.components()) {
          if (!this.molecules.contains(component.molecule())) {
            throw new IllegalArgumentException(
                "Laboratory component must reference a stored molecule.");
          }
        }
      }
      ChemistryStorageDocumentValidator.requireUniqueMolecules(this.molecules);
      ChemistryStorageDocumentValidator.requireUniqueReactions(this.reactions);
      ChemistryStorageDocumentValidator.requireUniqueMechanisms(this.mechanisms);
      ChemistryStorageDocumentValidator.requireUniqueReactionRecords(this.reactionRecords);
      ChemistryStorageDocumentValidator.requireUniqueReactionOutcomeRecords(
          this.reactionOutcomeRecords);
      ChemistryStorageDocumentValidator.requireUniqueConditionSnapshotRecords(
          this.conditionSnapshotRecords);
      ChemistryStorageDocumentValidator.requireUniqueReactionHistoryEvents(
          this.reactionHistoryEvents);
      ChemistryStorageDocumentValidator.requireUniquePredictionRecords(this.predictionRecords);
      ChemistryStorageDocumentValidator.requireUniqueComputationPlanRecords(
          this.computationPlanRecords);
      ChemistryStorageDocumentValidator.requireUniqueClassicalResultRecords(
          this.classicalResultRecords);
      ChemistryStorageDocumentValidator.requireUniqueQuantumProgramRecords(
          this.quantumProgramRecords);
      ChemistryStorageDocumentValidator.requireUniqueRoadmapRecords(this.roadmapRecords);
      ChemistryStorageDocumentValidator.requireReactionMoleculesPresent(
          this.reactions, this.molecules);
      ChemistryStorageDocumentValidator.requireMechanismReactionsPresent(
          this.mechanisms, this.reactions);
      ChemistryStorageDocumentValidator.requireReactionRecordsPresent(
          this.reactionRecords, this.reactions);
      ChemistryStorageDocumentValidator.requireReactionOutcomeRecordsPresent(
          this.reactionOutcomeRecords, this.reactions, this.molecules);
      ChemistryStorageDocumentValidator.requireReactionOutcomeRecordsLinkKnownWork(
          this.reactionOutcomeRecords, this.classicalResultRecords, this.quantumProgramRecords);
      ChemistryStorageDocumentValidator.requireConditionSnapshotRecordsPresent(
          this.conditionSnapshotRecords, this.reactions);
      ChemistryStorageDocumentValidator.requireReactionHistoryEventsPresent(
          this.reactionHistoryEvents,
          this.reactions,
          this.reactionRecords,
          this.conditionSnapshotRecords,
          this.predictionRecords,
          this.computationPlanRecords,
          this.classicalResultRecords,
          this.quantumProgramRecords);
      ChemistryStorageDocumentValidator.requirePredictionRecordsPresent(
          this.predictionRecords, this.molecules);
      ChemistryStorageDocumentValidator.requireComputationPlanRecordsPresent(
          this.computationPlanRecords,
          this.reactionRecords,
          this.classicalResultRecords,
          this.quantumProgramRecords);
      ChemistryStorageDocumentValidator.requireQuantumProgramRecordsPresent(
          this.quantumProgramRecords, this.reactionRecords, this.computationPlanRecords);
      ChemistryStorageDocumentValidator.requireRoadmapRecordsPresent(
          this.roadmapRecords, this.molecules, this.quantumProgramRecords);
      return new ChemistryStorageDocument(
          this.laboratoryExperiment,
          this.version,
          this.projectId,
          this.displayName,
          Map.copyOf(this.metadata),
          List.copyOf(this.molecules),
          List.copyOf(this.reactions),
          List.copyOf(this.mechanisms),
          List.copyOf(this.reactionRecords),
          List.copyOf(this.reactionOutcomeRecords),
          List.copyOf(this.conditionSnapshotRecords),
          List.copyOf(this.reactionHistoryEvents),
          List.copyOf(this.predictionRecords),
          List.copyOf(this.computationPlanRecords),
          List.copyOf(this.classicalResultRecords),
          List.copyOf(this.quantumProgramRecords),
          List.copyOf(this.roadmapRecords),
          List.copyOf(this.extensions));
    }
  }
}
