/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismEnergyPoint;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultSectionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultValueRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalCoverageItemRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageConditionSnapshotRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageQuantumProgramRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;

/**
 * Canonical writer собственного QCHEM storage format.
 */
public final class QchemTextWriter {

  public String write(final ChemistryStorageDocument document) {
    if (document == null) {
      throw new IllegalArgumentException("Storage document must not be null.");
    }
    final StringBuilder builder = new StringBuilder(4096);
    builder.append("QCHEM version=").append(document.version().number()).append("\r\n");
    builder
        .append("project id=")
        .append(document.projectId())
        .append(" name=")
        .append(QchemEscaper.quote(document.displayName()))
        .append("\r\n");
    QchemTextWriter.writeMap(builder, "project-meta", document.metadata());
    final List<Molecule> molecules = document.molecules();
    for (int i = 0; i < molecules.size(); ++i) {
      QchemTextWriter.writeMolecule(builder, molecules.get(i));
    }
    if (document.laboratoryExperiment() != null) {
      QchemLaboratoryCodec.write(builder, document.laboratoryExperiment());
    }
    final List<Reaction> reactions = document.reactions();
    for (int i = 0; i < reactions.size(); ++i) {
      QchemTextWriter.writeReaction(builder, reactions.get(i));
    }
    final List<ReactionMechanism> mechanisms = document.mechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      QchemTextWriter.writeMechanism(builder, mechanisms.get(i));
    }
    final List<ChemistryStorageReactionRecord> records = document.reactionRecords();
    for (int i = 0; i < records.size(); ++i) {
      QchemTextWriter.writeReactionRecord(builder, records.get(i));
    }
    final List<ChemistryStorageReactionOutcomeRecord> outcomeRecords =
        document.reactionOutcomeRecords();
    for (int i = 0; i < outcomeRecords.size(); ++i) {
      QchemTextWriter.writeReactionOutcomeRecord(builder, outcomeRecords.get(i));
    }
    final List<ChemistryStorageConditionSnapshotRecord> conditionSnapshotRecords =
        document.conditionSnapshotRecords();
    for (int i = 0; i < conditionSnapshotRecords.size(); ++i) {
      QchemTextWriter.writeConditionSnapshotRecord(builder, conditionSnapshotRecords.get(i));
    }
    final List<ChemistryStorageReactionHistoryEvent> historyEvents =
        document.reactionHistoryEvents();
    for (int i = 0; i < historyEvents.size(); ++i) {
      QchemTextWriter.writeReactionHistoryEvent(builder, historyEvents.get(i));
    }
    final List<ChemistryStoragePredictionRecord> predictionRecords = document.predictionRecords();
    for (int i = 0; i < predictionRecords.size(); ++i) {
      QchemTextWriter.writePredictionRecord(builder, predictionRecords.get(i));
    }
    final List<ChemistryStorageComputationPlanRecord> computationPlanRecords =
        document.computationPlanRecords();
    for (int i = 0; i < computationPlanRecords.size(); ++i) {
      QchemTextWriter.writeComputationPlanRecord(builder, computationPlanRecords.get(i));
    }
    final List<ChemistryStorageClassicalResultRecord> classicalResultRecords =
        document.classicalResultRecords();
    for (int i = 0; i < classicalResultRecords.size(); ++i) {
      QchemTextWriter.writeClassicalResultRecord(builder, classicalResultRecords.get(i));
    }
    final List<ChemistryStorageQuantumProgramRecord> quantumProgramRecords =
        document.quantumProgramRecords();
    for (int i = 0; i < quantumProgramRecords.size(); ++i) {
      QchemTextWriter.writeQuantumProgramRecord(builder, quantumProgramRecords.get(i));
    }
    final List<ChemistryStorageRoadmapRecord> roadmapRecords = document.roadmapRecords();
    for (int i = 0; i < roadmapRecords.size(); ++i) {
      QchemTextWriter.writeRoadmapRecord(builder, roadmapRecords.get(i));
    }
    final List<ChemistryStorageExtension> extensions = document.extensions();
    for (int i = 0; i < extensions.size(); ++i) {
      QchemTextWriter.writeExtension(builder, extensions.get(i));
    }
    return builder.toString().trim();
  }

  private static void writeMolecule(final StringBuilder builder, final Molecule molecule) {
    builder
        .append("molecule id=")
        .append(molecule.id().value())
        .append(" name=")
        .append(QchemEscaper.quote(molecule.displayName()))
        .append(" charge=")
        .append(molecule.charge().value())
        .append(" spin=")
        .append(molecule.spinMultiplicity().value())
        .append(" symmetry=")
        .append(molecule.symmetry().pointGroupName().value())
        .append(" symmetryNumber=")
        .append(molecule.symmetry().symmetryNumber())
        .append(" rotation=")
        .append(molecule.opticalRotation().direction())
        .append(" rotationDegrees=")
        .append(molecule.opticalRotation().degrees())
        .append(" rotationWavelengthNm=")
        .append(molecule.opticalRotation().wavelengthNanometers())
        .append("\r\n");
    QchemTextWriter.writeMetadata(builder, molecule.metadata());
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      QchemTextWriter.writeAtom(builder, atom);
      QchemTextWriter.writeAtomMetadata(builder, atom);
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      QchemTextWriter.writeBond(builder, bonds.get(i));
    }
    final List<Stereocenter> centers = molecule.stereochemistry().centers();
    for (int i = 0; i < centers.size(); ++i) {
      QchemTextWriter.writeStereocenter(builder, centers.get(i));
    }
    final List<TorsionAngle> torsions = molecule.conformation().torsionAngles();
    for (int i = 0; i < torsions.size(); ++i) {
      QchemTextWriter.writeTorsion(builder, torsions.get(i));
    }
    builder.append("end\r\n");
  }

  private static void writeAtom(final StringBuilder builder, final Atom atom) {
    builder
        .append("  atom id=")
        .append(atom.id().value())
        .append(" element=")
        .append(atom.element().symbol().value())
        .append(" formal=")
        .append(atom.formalCharge().value())
        .append(" isotope=")
        .append(atom.hasIsotope() ? Integer.toString(atom.isotope().massNumber()) : "none")
        .append(" radical=")
        .append(atom.radicalState().unpairedElectrons());
    if (atom.hasCoordinate()) {
      builder
          .append(" x=")
          .append(atom.coordinate().x())
          .append(" y=")
          .append(atom.coordinate().y())
          .append(" z=")
          .append(atom.coordinate().z())
          .append(" unit=")
          .append(atom.coordinate().unit());
    } else {
      builder.append(" x=none y=none z=none unit=none");
    }
    builder.append("\r\n");
  }

  private static void writeAtomMetadata(final StringBuilder builder, final Atom atom) {
    final ChemistryMetadata metadata = atom.metadata();
    if (metadata == null || metadata.equals(ChemistryMetadata.EMPTY)) {
      return;
    }
    if (metadata.hasSource()) {
      builder
          .append("  atom-source atom=")
          .append(atom.id().value())
          .append(" format=")
          .append(QchemEscaper.quote(metadata.source().format()))
          .append(" description=")
          .append(QchemEscaper.quote(metadata.source().description()))
          .append("\r\n");
    }
    if (metadata.hasLocation()) {
      builder
          .append("  atom-location atom=")
          .append(atom.id().value())
          .append(" line=")
          .append(metadata.location().line())
          .append(" column=")
          .append(metadata.location().column())
          .append("\r\n");
    }
    QchemTextWriter.writeAtomMetadataAttributes(builder, atom, metadata.attributes());
  }

  private static void writeAtomMetadataAttributes(
      final StringBuilder builder, final Atom atom, final Map<String, String> values) {
    for (final Map.Entry<String, String> entry : new java.util.TreeMap<>(values).entrySet()) {
      builder
          .append("  atom-metadata atom=")
          .append(atom.id().value())
          .append(" key=")
          .append(entry.getKey())
          .append(" value=")
          .append(QchemEscaper.quote(entry.getValue()))
          .append("\r\n");
    }
  }

  private static void writeBond(final StringBuilder builder, final Bond bond) {
    builder
        .append("  bond first=")
        .append(bond.firstAtomId().value())
        .append(" second=")
        .append(bond.secondAtomId().value())
        .append(" type=")
        .append(bond.type())
        .append("\r\n");
  }

  private static void writeStereocenter(final StringBuilder builder, final Stereocenter center) {
    builder
        .append("  stereo kind=")
        .append(center.kind())
        .append(" descriptor=")
        .append(center.descriptor())
        .append(" primary=")
        .append(center.primaryAtomId().value())
        .append(" secondary=")
        .append(center.hasSecondaryAtom() ? center.secondaryAtomId().value() : "none")
        .append(" refs=");
    for (int i = 0; i < center.referenceAtomIds().size(); ++i) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(center.referenceAtomIds().get(i).value());
    }
    builder.append("\r\n");
  }

  private static void writeTorsion(final StringBuilder builder, final TorsionAngle torsion) {
    builder
        .append("  torsion first=")
        .append(torsion.firstAtomId().value())
        .append(" second=")
        .append(torsion.secondAtomId().value())
        .append(" third=")
        .append(torsion.thirdAtomId().value())
        .append(" fourth=")
        .append(torsion.fourthAtomId().value())
        .append(" degrees=")
        .append(torsion.degrees())
        .append("\r\n");
  }

  private static void writeReaction(final StringBuilder builder, final Reaction reaction) {
    builder
        .append("reaction id=")
        .append(reaction.id().value())
        .append(" name=")
        .append(QchemEscaper.quote(reaction.displayName()));
    if (!reaction.conditions().empty()) {
      builder.append(" note=").append(QchemEscaper.quote(reaction.conditions().note()));
      if (reaction.conditions().temperature() != null) {
        builder
            .append(" temperature=")
            .append(reaction.conditions().temperature().value())
            .append(" temperatureUnit=")
            .append(reaction.conditions().temperature().unit());
      }
      if (reaction.conditions().pressure() != null) {
        builder
            .append(" pressure=")
            .append(reaction.conditions().pressure().value())
            .append(" pressureUnit=")
            .append(reaction.conditions().pressure().unit());
      }
      if (reaction.conditions().duration() != null) {
        builder
            .append(" duration=")
            .append(reaction.conditions().duration().value())
            .append(" durationUnit=")
            .append(reaction.conditions().duration().unit());
      }
    }
    builder.append("\r\n");
    QchemTextWriter.writeReactionSide(builder, "reactant", reaction.reactants());
    QchemTextWriter.writeReactionSide(builder, "product", reaction.products());
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      QchemTextWriter.writeConditionComponent(builder, components.get(i));
    }
    final List<ReactionConditionFeature> features = reaction.conditions().features();
    for (int i = 0; i < features.size(); ++i) {
      QchemTextWriter.writeConditionFeature(builder, features.get(i));
    }
    QchemTextWriter.writeMetadata(builder, reaction.metadata());
    builder.append("end\r\n");
  }

  static void writeConditionComponent(
      final StringBuilder builder, final ReactionConditionComponent component) {
    builder
        .append("  condition role=")
        .append(component.role())
        .append(" name=")
        .append(QchemEscaper.quote(component.name()))
        .append(" phase=")
        .append(component.phase())
        .append(" amount=")
        .append(component.hasAmount() ? Double.toString(component.amount().value()) : "none")
        .append(" amountUnit=")
        .append(component.hasAmount() ? component.amount().unit().name() : "none")
        .append(" concentration=")
        .append(
            component.hasConcentration()
                ? Double.toString(component.concentration().value())
                : "none")
        .append(" concentrationUnit=")
        .append(component.hasConcentration() ? component.concentration().unit().name() : "none")
        .append(" equivalent=")
        .append(
            component.hasEquivalent() ? Double.toString(component.equivalent().value()) : "none")
        .append(" loadingPercent=")
        .append(component.hasLoading() ? Double.toString(component.loading().percent()) : "none")
        .append(" purityPercent=")
        .append(component.hasPurity() ? Double.toString(component.purity().percent()) : "none")
        .append(" note=")
        .append(QchemEscaper.quote(component.note()))
        .append("\r\n");
  }

  static void writeConditionFeature(
      final StringBuilder builder, final ReactionConditionFeature feature) {
    builder
        .append("  feature kind=")
        .append(feature.kind())
        .append(" value=")
        .append(QchemEscaper.quote(feature.value()))
        .append(" note=")
        .append(QchemEscaper.quote(feature.note()))
        .append("\r\n");
  }

  private static void writeReactionSide(
      final StringBuilder builder, final String command, final ReactionSide side) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      builder
          .append("  ")
          .append(command)
          .append(" molecule=")
          .append(participant.molecule().id().value())
          .append(" coefficient=")
          .append(participant.coefficient().value())
          .append("\r\n");
    }
  }

  private static void writeMechanism(
      final StringBuilder builder, final ReactionMechanism mechanism) {
    builder
        .append("mechanism id=")
        .append(mechanism.id())
        .append(" family=")
        .append(mechanism.family())
        .append(" reaction=")
        .append(mechanism.overallReaction().id().value())
        .append("\r\n");
    final List<ElementaryReactionStep> steps = mechanism.steps();
    for (int i = 0; i < steps.size(); ++i) {
      QchemTextWriter.writeMechanismStep(builder, steps.get(i));
    }
    final List<MechanismEnergyPoint> energyProfile = mechanism.energyProfile();
    for (int i = 0; i < energyProfile.size(); ++i) {
      QchemTextWriter.writeEnergyPoint(builder, energyProfile.get(i));
    }
    builder.append("end\r\n");
  }

  private static void writeMechanismStep(
      final StringBuilder builder, final ElementaryReactionStep step) {
    builder
        .append("  step id=")
        .append(step.id())
        .append(" reaction=")
        .append(step.reaction().id().value())
        .append(" transitionState=")
        .append(step.hasTransitionState() ? step.transitionState().id().value() : "none")
        .append(" forwardBarrier=")
        .append(step.hasForwardBarrier() ? Double.toString(step.forwardBarrier().value()) : "none")
        .append(" forwardBarrierUnit=")
        .append(step.hasForwardBarrier() ? step.forwardBarrier().unit().name() : "none")
        .append(" reverseBarrier=")
        .append(step.hasReverseBarrier() ? Double.toString(step.reverseBarrier().value()) : "none")
        .append(" reverseBarrierUnit=")
        .append(step.hasReverseBarrier() ? step.reverseBarrier().unit().name() : "none")
        .append(" reactionEnergy=")
        .append(step.hasReactionEnergy() ? Double.toString(step.reactionEnergy().value()) : "none")
        .append(" reactionEnergyUnit=")
        .append(step.hasReactionEnergy() ? step.reactionEnergy().unit().name() : "none")
        .append("\r\n");
    final List<MechanisticEvent> events = step.events();
    for (int i = 0; i < events.size(); ++i) {
      QchemTextWriter.writeMechanisticEvent(builder, step.id(), events.get(i));
    }
  }

  private static void writeMechanisticEvent(
      final StringBuilder builder, final String stepId, final MechanisticEvent event) {
    builder
        .append("  event step=")
        .append(stepId)
        .append(" id=")
        .append(event.id())
        .append(" order=")
        .append(event.orderIndex())
        .append(" kind=")
        .append(event.kind())
        .append(" weight=")
        .append(event.probabilityWeight())
        .append(" refs=")
        .append(QchemEscaper.quote(String.join(",", event.participantReferences())))
        .append(" description=")
        .append(QchemEscaper.quote(event.description()))
        .append("\r\n");
  }

  private static void writeEnergyPoint(
      final StringBuilder builder, final MechanismEnergyPoint point) {
    builder
        .append("  energy id=")
        .append(point.id())
        .append(" kind=")
        .append(point.kind())
        .append(" coordinate=")
        .append(point.coordinate().value())
        .append(" value=")
        .append(point.relativeEnergy().value())
        .append(" unit=")
        .append(point.relativeEnergy().unit())
        .append(" molecule=")
        .append(
            point.hasRepresentativeMolecule() ? point.representativeMoleculeId().value() : "none")
        .append(" molecules=")
        .append(QchemEscaper.quote(QchemTextWriter.representativeMoleculeIds(point)))
        .append("\r\n");
  }

  private static String representativeMoleculeIds(final MechanismEnergyPoint point) {
    if (!point.hasRepresentativeMolecule()) {
      return "";
    }
    final ArrayList<String> ids = new ArrayList<String>(point.representativeMoleculeIds().size());
    for (int i = 0; i < point.representativeMoleculeIds().size(); ++i) {
      ids.add(point.representativeMoleculeIds().get(i).value());
    }
    return String.join(",", ids);
  }

  private static void writeReactionRecord(
      final StringBuilder builder, final ChemistryStorageReactionRecord record) {
    builder
        .append("reaction-record id=")
        .append(record.id())
        .append(" reaction=")
        .append(record.reaction().id().value())
        .append(" status=")
        .append(record.status())
        .append(" history=")
        .append(record.historyIndex())
        .append(" yield=")
        .append(
            record.hasObservedYieldPercent()
                ? Double.toString(record.observedYieldPercent())
                : "none")
        .append(" note=")
        .append(QchemEscaper.quote(record.note()))
        .append("\r\n")
        .append("end\r\n");
  }

  private static void writeReactionOutcomeRecord(
      final StringBuilder builder, final ChemistryStorageReactionOutcomeRecord record) {
    builder
        .append("reaction-outcome-record id=")
        .append(record.id())
        .append(" reaction=")
        .append(record.reactionId())
        .append(" history=")
        .append(record.historyIndex())
        .append(" kind=")
        .append(record.kind())
        .append(" products=")
        .append(QchemEscaper.quote(String.join(",", record.productMoleculeIds())))
        .append(" probability=")
        .append(record.probability())
        .append(" yield=")
        .append(record.hasYieldPercent() ? Double.toString(record.yieldPercent()) : "none")
        .append(" mechanisms=")
        .append(QchemEscaper.quote(String.join(",", record.mechanismIds())))
        .append(" classicalResults=")
        .append(QchemEscaper.quote(String.join(",", record.classicalResultIds())))
        .append(" quantumPrograms=")
        .append(QchemEscaper.quote(String.join(",", record.quantumProgramIds())))
        .append(" condition=")
        .append(QchemEscaper.quote(record.conditionSummary()))
        .append(" note=")
        .append(QchemEscaper.quote(record.note()))
        .append("\r\n")
        .append("end\r\n");
  }

  private static void writeConditionSnapshotRecord(
      final StringBuilder builder, final ChemistryStorageConditionSnapshotRecord record) {
    builder
        .append("condition-snapshot-record id=")
        .append(record.id())
        .append(" reaction=")
        .append(record.reactionId())
        .append(" history=")
        .append(record.historyIndex())
        .append(" label=")
        .append(QchemEscaper.quote(record.label()));
    if (!record.conditions().empty()) {
      builder.append(" note=").append(QchemEscaper.quote(record.conditions().note()));
      if (record.conditions().temperature() != null) {
        builder
            .append(" temperature=")
            .append(record.conditions().temperature().value())
            .append(" temperatureUnit=")
            .append(record.conditions().temperature().unit());
      }
      if (record.conditions().pressure() != null) {
        builder
            .append(" pressure=")
            .append(record.conditions().pressure().value())
            .append(" pressureUnit=")
            .append(record.conditions().pressure().unit());
      }
      if (record.conditions().duration() != null) {
        builder
            .append(" duration=")
            .append(record.conditions().duration().value())
            .append(" durationUnit=")
            .append(record.conditions().duration().unit());
      }
    }
    builder.append(" snapshotNote=").append(QchemEscaper.quote(record.note())).append("\r\n");
    final List<ReactionConditionComponent> components = record.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      QchemTextWriter.writeConditionComponent(builder, components.get(i));
    }
    final List<ReactionConditionFeature> features = record.conditions().features();
    for (int i = 0; i < features.size(); ++i) {
      QchemTextWriter.writeConditionFeature(builder, features.get(i));
    }
    builder.append("end\r\n");
  }

  private static void writeReactionHistoryEvent(
      final StringBuilder builder, final ChemistryStorageReactionHistoryEvent event) {
    builder
        .append("reaction-history-event id=")
        .append(event.id())
        .append(" reaction=")
        .append(event.reactionId())
        .append(" history=")
        .append(event.historyIndex())
        .append(" kind=")
        .append(event.kind())
        .append(" title=")
        .append(QchemEscaper.quote(event.title()))
        .append(" description=")
        .append(QchemEscaper.quote(event.description()))
        .append(" reactionRecords=")
        .append(QchemEscaper.quote(String.join(",", event.reactionRecordIds())))
        .append(" conditionSnapshots=")
        .append(QchemEscaper.quote(String.join(",", event.conditionSnapshotRecordIds())))
        .append(" predictions=")
        .append(QchemEscaper.quote(String.join(",", event.predictionRecordIds())))
        .append(" computationPlans=")
        .append(QchemEscaper.quote(String.join(",", event.computationPlanRecordIds())))
        .append(" classicalResults=")
        .append(QchemEscaper.quote(String.join(",", event.classicalResultIds())))
        .append(" quantumPrograms=")
        .append(QchemEscaper.quote(String.join(",", event.quantumProgramIds())))
        .append("\r\n")
        .append("end\r\n");
  }

  private static void writePredictionRecord(
      final StringBuilder builder, final ChemistryStoragePredictionRecord record) {
    builder
        .append("prediction-record id=")
        .append(record.id())
        .append(" request=")
        .append(record.requestId())
        .append(" method=")
        .append(record.methodType())
        .append(" status=")
        .append(record.status())
        .append(" history=")
        .append(record.historyIndex())
        .append(" diagnostics=")
        .append(QchemEscaper.quote(String.join("||", record.diagnostics())))
        .append(" note=")
        .append(QchemEscaper.quote(record.note()))
        .append("\r\n");
    final List<ChemistryStoragePredictionCandidateRecord> candidates = record.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      QchemTextWriter.writePredictionCandidate(builder, candidates.get(i));
    }
    builder.append("end\r\n");
  }

  private static void writePredictionCandidate(
      final StringBuilder builder, final ChemistryStoragePredictionCandidateRecord candidate) {
    builder
        .append("  candidate rank=")
        .append(candidate.rank())
        .append(" products=")
        .append(QchemEscaper.quote(String.join(",", candidate.productMoleculeIds())))
        .append(" formulas=")
        .append(QchemEscaper.quote(String.join(",", candidate.productFormulaSignatures())))
        .append(" stoichiometry=")
        .append(QchemEscaper.quote(String.join("||", candidate.productStoichiometrySignatures())))
        .append(" probability=")
        .append(candidate.probability())
        .append(" yield=")
        .append(candidate.expectedYieldPercent())
        .append(" mechanisms=")
        .append(QchemEscaper.quote(String.join(",", candidate.mechanismIds())))
        .append(" mechanismDetails=")
        .append(QchemEscaper.quote(String.join("||", candidate.mechanismDescriptors())))
        .append(" score=")
        .append(QchemEscaper.quote(String.join("||", candidate.scoreComponents())))
        .append(" evidence=")
        .append(QchemEscaper.quote(String.join("||", candidate.evidence())))
        .append(" note=")
        .append(QchemEscaper.quote(candidate.note()))
        .append("\r\n");
  }

  private static void writeComputationPlanRecord(
      final StringBuilder builder, final ChemistryStorageComputationPlanRecord record) {
    builder
        .append("computation-plan-record id=")
        .append(record.id())
        .append(" reactionRecord=")
        .append(record.reactionRecordId())
        .append(" status=")
        .append(record.status())
        .append(" history=")
        .append(record.historyIndex())
        .append(" deferred=")
        .append(record.deferredExecution())
        .append(" classical=")
        .append(QchemEscaper.quote(String.join(",", record.classicalPlanIds())))
        .append(" classicalResults=")
        .append(QchemEscaper.quote(String.join(",", record.classicalResultIds())))
        .append(" quantum=")
        .append(QchemEscaper.quote(String.join(",", record.quantumProgramIds())))
        .append(" note=")
        .append(QchemEscaper.quote(record.note()))
        .append("\r\n")
        .append("end\r\n");
  }

  private static void writeClassicalResultRecord(
      final StringBuilder builder, final ChemistryStorageClassicalResultRecord record) {
    builder
        .append("classical-result-record id=")
        .append(record.id())
        .append(" calculation=")
        .append(record.calculationId())
        .append(" status=")
        .append(record.status())
        .append(" history=")
        .append(record.historyIndex())
        .append(" diagnostics=")
        .append(record.diagnosticCount())
        .append(" errors=")
        .append(record.errorCount())
        .append(" warnings=")
        .append(record.warningCount())
        .append(" coverage=")
        .append(record.coverageStatus())
        .append(" note=")
        .append(QchemEscaper.quote(record.note()))
        .append("\r\n");
    final List<ChemistryStorageClassicalCoverageItemRecord> coverageItems = record.coverageItems();
    for (int i = 0; i < coverageItems.size(); ++i) {
      QchemTextWriter.writeClassicalCoverageItem(builder, i, coverageItems.get(i));
    }
    final List<ChemistryStorageClassicalResultSectionRecord> sections = record.sections();
    for (int i = 0; i < sections.size(); ++i) {
      QchemTextWriter.writeClassicalResultSection(builder, i, sections.get(i));
    }
    builder.append("end\r\n");
  }

  private static void writeQuantumProgramRecord(
      final StringBuilder builder, final ChemistryStorageQuantumProgramRecord record) {
    builder
        .append("quantum-program-record id=")
        .append(record.id())
        .append(" source=")
        .append(record.sourceRecordId())
        .append(" history=")
        .append(record.historyIndex())
        .append(" kind=")
        .append(record.kind())
        .append(" target=")
        .append(record.target())
        .append(" diagnostics=")
        .append(QchemEscaper.quote(String.join("||", record.diagnostics())))
        .append(" note=")
        .append(QchemEscaper.quote(record.note()))
        .append("\r\n");
    if (record.hasNativePayload()) {
      QchemTextWriter.writePayloadLines(builder, "native", record.nativePayload());
    }
    if (record.hasExportedPayload()) {
      QchemTextWriter.writePayloadLines(builder, "export", record.exportedPayload());
    }
    builder.append("end\r\n");
  }

  private static void writePayloadLines(
      final StringBuilder builder, final String kind, final String payload) {
    final String[] lines = payload.split("\n", -1);
    for (int i = 0; i < lines.length; ++i) {
      builder
          .append("  payload kind=")
          .append(kind)
          .append(" text=")
          .append(QchemEscaper.quote(lines[i]))
          .append("\r\n");
    }
  }

  private static void writeClassicalCoverageItem(
      final StringBuilder builder,
      final int index,
      final ChemistryStorageClassicalCoverageItemRecord item) {
    builder
        .append("  coverage index=")
        .append(index)
        .append(" kind=")
        .append(item.kind())
        .append(" status=")
        .append(item.status())
        .append(" model=")
        .append(QchemEscaper.quote(item.model()))
        .append("\r\n");
  }

  private static void writeClassicalResultSection(
      final StringBuilder builder,
      final int index,
      final ChemistryStorageClassicalResultSectionRecord section) {
    builder
        .append("  section index=")
        .append(index)
        .append(" kind=")
        .append(section.kind())
        .append(" title=")
        .append(QchemEscaper.quote(section.title()))
        .append("\r\n");
    final List<ChemistryStorageClassicalResultValueRecord> values = section.values();
    for (int i = 0; i < values.size(); ++i) {
      final ChemistryStorageClassicalResultValueRecord value = values.get(i);
      builder
          .append("  value section=")
          .append(index)
          .append(" name=")
          .append(value.name())
          .append(" kind=")
          .append(value.kind())
          .append(" unit=")
          .append(QchemEscaper.quote(value.unit()))
          .append(" values=")
          .append(QchemEscaper.quote(String.join("||", value.values())))
          .append("\r\n");
    }
  }

  private static void writeRoadmapRecord(
      final StringBuilder builder, final ChemistryStorageRoadmapRecord record) {
    builder
        .append("roadmap-record id=")
        .append(record.id())
        .append(" request=")
        .append(record.requestId())
        .append(" status=")
        .append(record.status())
        .append(" history=")
        .append(record.historyIndex())
        .append(" scenarios=")
        .append(record.conditionScenarioCount())
        .append(" evaluatedScenarios=")
        .append(record.evaluatedConditionScenarioCount())
        .append(" programTargets=")
        .append(QchemEscaper.quote(String.join(",", record.programLevelRecommendationTargets())))
        .append(" note=")
        .append(QchemEscaper.quote(record.note()))
        .append("\r\n");
    final List<ChemistryStorageRoadmapCandidateRecord> candidates = record.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      QchemTextWriter.writeRoadmapCandidate(builder, candidates.get(i));
    }
    builder.append("end\r\n");
  }

  private static void writeRoadmapCandidate(
      final StringBuilder builder, final ChemistryStorageRoadmapCandidateRecord candidate) {
    builder
        .append("  roadmap-candidate index=")
        .append(candidate.candidateIndex())
        .append(" products=")
        .append(QchemEscaper.quote(String.join(",", candidate.productMoleculeIds())))
        .append(" formulas=")
        .append(QchemEscaper.quote(String.join(",", candidate.productFormulaSignatures())))
        .append(" stoichiometry=")
        .append(QchemEscaper.quote(String.join("||", candidate.productStoichiometrySignatures())))
        .append(" probability=")
        .append(candidate.probability())
        .append(" yield=")
        .append(candidate.expectedYieldPercent())
        .append(" mechanisms=")
        .append(QchemEscaper.quote(String.join(",", candidate.mechanismIds())))
        .append(" mechanismDetails=")
        .append(QchemEscaper.quote(String.join("||", candidate.mechanismDescriptors())))
        .append(" classical=")
        .append(QchemEscaper.quote(String.join(",", candidate.classicalCalculationKinds())))
        .append(" classicalPlans=")
        .append(QchemEscaper.quote(String.join(",", candidate.classicalPlanIds())))
        .append(" followUpActions=")
        .append(QchemEscaper.quote(String.join("||", candidate.followUpActionTargets())))
        .append(" quantumTargets=")
        .append(QchemEscaper.quote(String.join(",", candidate.quantumPreparationTargets())))
        .append(" explicitProductGraph=")
        .append(candidate.explicitProductGraph())
        .append(" electronicRerankReady=")
        .append(candidate.electronicRerankReady())
        .append(" quantumWorkflowReady=")
        .append(candidate.quantumWorkflowReady())
        .append(" readinessReason=")
        .append(QchemEscaper.quote(candidate.readinessReason()))
        .append("\r\n");
  }

  private static void writeExtension(
      final StringBuilder builder, final ChemistryStorageExtension extension) {
    builder
        .append("extension kind=")
        .append(extension.kind())
        .append(" id=")
        .append(extension.id())
        .append("\r\n");
    QchemTextWriter.writeMap(builder, "  property", extension.properties());
    final List<String> lines = extension.bodyLines();
    for (int i = 0; i < lines.size(); ++i) {
      builder.append("  body text=").append(QchemEscaper.quote(lines.get(i))).append("\r\n");
    }
    builder.append("end\r\n");
  }

  private static void writeMetadata(final StringBuilder builder, final ChemistryMetadata metadata) {
    if (metadata == null || metadata.equals(ChemistryMetadata.EMPTY)) {
      return;
    }
    if (metadata.hasSource()) {
      builder
          .append("  source format=")
          .append(QchemEscaper.quote(metadata.source().format()))
          .append(" description=")
          .append(QchemEscaper.quote(metadata.source().description()))
          .append("\r\n");
    }
    if (metadata.hasLocation()) {
      builder
          .append("  location line=")
          .append(metadata.location().line())
          .append(" column=")
          .append(metadata.location().column())
          .append("\r\n");
    }
    QchemTextWriter.writeMap(builder, "  metadata", metadata.attributes());
  }

  private static void writeMap(
      final StringBuilder builder, final String command, final Map<String, String> values) {
    for (final Map.Entry<String, String> entry : new java.util.TreeMap<>(values).entrySet()) {
      builder
          .append(command)
          .append(" key=")
          .append(entry.getKey())
          .append(" value=")
          .append(QchemEscaper.quote(entry.getValue()))
          .append("\r\n");
    }
  }
}
