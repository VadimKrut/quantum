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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.MolecularConformation;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismEnergyPoint;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismPointKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionCoordinateValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistrySource;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistrySourceLocation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentPurity;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricEquivalent;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotationDirection;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereocenterKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.MolecularSymmetry;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.PointGroupName;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmountUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionEventKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultSectionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultValueRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalCoverageItemRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageConditionSnapshotRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageFormatVersion;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageQuantumProgramRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecordStatus;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;

/**
 * Parser собственного QCHEM storage format с диагностикой вместо штатных workflow-исключений.
 */
public final class QchemTextReader {

  public ChemistryStorageResult<ChemistryStorageDocument> read(final String content) {
    final ArrayList<ChemistryStorageDiagnostic> diagnostics =
        new ArrayList<ChemistryStorageDiagnostic>();
    try {
      if (content == null || content.trim().isEmpty()) {
        diagnostics.add(
            QchemTextReader.error(
                ChemistryStorageDiagnosticCode.EMPTY_INPUT, "Storage input is empty.", 0));
        return ChemistryStorageResult.failure(diagnostics);
      }
      final ArrayList<QchemLine> lines = QchemTextReader.parseLines(content, diagnostics);
      if (!diagnostics.isEmpty()) {
        return ChemistryStorageResult.failure(diagnostics);
      }
      return QchemTextReader.readLines(lines, diagnostics);
    } catch (final StorageTextException exception) {
      diagnostics.add(
          QchemTextReader.error(exception.code(), exception.getMessage(), exception.line()));
      return ChemistryStorageResult.failure(diagnostics);
    } catch (final RuntimeException exception) {
      diagnostics.add(
          QchemTextReader.error(
              ChemistryStorageDiagnosticCode.DOMAIN_REJECTED_VALUE, exception.getMessage(), 0));
      return ChemistryStorageResult.failure(diagnostics);
    }
  }

  private static ChemistryStorageResult<ChemistryStorageDocument> readLines(
      final List<QchemLine> lines, final List<ChemistryStorageDiagnostic> diagnostics) {
    if (lines.size() < 2) {
      diagnostics.add(
          QchemTextReader.error(
              ChemistryStorageDiagnosticCode.INVALID_HEADER,
              "QCHEM header and project line are required.",
              0));
      return ChemistryStorageResult.failure(diagnostics);
    }
    final QchemLine header = lines.get(0);
    if (!"QCHEM".equals(header.command())) {
      diagnostics.add(
          QchemTextReader.error(
              ChemistryStorageDiagnosticCode.INVALID_HEADER,
              "First storage line must be QCHEM.",
              header.lineNumber()));
      return ChemistryStorageResult.failure(diagnostics);
    }
    final ChemistryStorageFormatVersion version = QchemTextReader.readVersion(header);
    final QchemLine project = lines.get(1);
    if (!"project".equals(project.command())) {
      diagnostics.add(
          QchemTextReader.error(
              ChemistryStorageDiagnosticCode.MISSING_PROJECT,
              "Second storage line must define project.",
              project.lineNumber()));
      return ChemistryStorageResult.failure(diagnostics);
    }
    final ChemistryStorageDocument.Builder builder =
        ChemistryStorageDocument.builder(
            project.required("id"), QchemEscaper.unquote(project.required("name")));
    builder.version(version);
    final LinkedHashMap<String, Molecule> moleculesById = new LinkedHashMap<String, Molecule>();
    final LinkedHashMap<String, Reaction> reactionsById = new LinkedHashMap<String, Reaction>();
    int index = 2;
    while (index < lines.size()) {
      final QchemLine line = lines.get(index);
      if ("project-meta".equals(line.command())) {
        builder.metadata(line.required("key"), QchemEscaper.unquote(line.required("value")));
        ++index;
      } else if ("molecule".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        final Molecule molecule = QchemTextReader.readMolecule(section);
        moleculesById.put(molecule.id().value(), molecule);
        builder.molecule(molecule);
        index = section.endIndex() + 1;
      } else if ("laboratory-experiment".equals(line.command())) {
        if (version.number() < 2) {
          throw new IllegalArgumentException("Laboratory documents require QCHEM version 2.");
        }
        final Section section = Section.collect(lines, index);
        if (version.number() < 17
            && section.body().stream()
                .anyMatch(
                    item ->
                        java.util.Set.of(
                                "evidence-2d-spectrum-v1",
                                "evidence-2d-spectrum-ref-v1",
                                "evidence-multiplets-source-v1")
                            .contains(item.command()))) {
          throw new IllegalArgumentException("Связанные 1D/2D спектры требуют QCHEM version=17.");
        }
        if (version.number() < 15
            && section.body().stream()
                .anyMatch(
                    item ->
                        java.util.Set.of("operations-observation-v1", "operations-analysis-ref-v1")
                            .contains(item.command()))) {
          throw new IllegalArgumentException(
              "Происхождение и аналитические ссылки операций требуют QCHEM version=15.");
        }
        if (version.number() < 14
            && section.body().stream()
                .anyMatch(
                    item ->
                        java.util.Set.of(
                                "stage-classification-v1",
                                "stage-reaction-name-v1",
                                "stage-mechanism-v1")
                            .contains(item.command()))) {
          throw new IllegalArgumentException(
              "Stage reaction names and mechanisms require QCHEM version14.");
        }
        builder
            .laboratoryExperiment(
                QchemLaboratoryCodec.read(section.header(), section.body(), moleculesById))
            .version(version);
        index = section.endIndex() + 1;
      } else if ("reaction".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        final Reaction reaction = QchemTextReader.readReaction(section, moleculesById);
        reactionsById.put(reaction.id().value(), reaction);
        builder.reaction(reaction);
        index = section.endIndex() + 1;
      } else if ("mechanism".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.mechanism(QchemTextReader.readMechanism(section, reactionsById, moleculesById));
        index = section.endIndex() + 1;
      } else if ("reaction-record".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.reactionRecord(QchemTextReader.readReactionRecord(section, reactionsById));
        index = section.endIndex() + 1;
      } else if ("reaction-outcome-record".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.reactionOutcomeRecord(QchemTextReader.readReactionOutcomeRecord(section));
        index = section.endIndex() + 1;
      } else if ("condition-snapshot-record".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.conditionSnapshotRecord(QchemTextReader.readConditionSnapshotRecord(section));
        index = section.endIndex() + 1;
      } else if ("reaction-history-event".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.reactionHistoryEvent(QchemTextReader.readReactionHistoryEvent(section));
        index = section.endIndex() + 1;
      } else if ("prediction-record".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.predictionRecord(QchemTextReader.readPredictionRecord(section));
        index = section.endIndex() + 1;
      } else if ("computation-plan-record".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.computationPlanRecord(QchemTextReader.readComputationPlanRecord(section));
        index = section.endIndex() + 1;
      } else if ("classical-result-record".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.classicalResultRecord(QchemTextReader.readClassicalResultRecord(section));
        index = section.endIndex() + 1;
      } else if ("quantum-program-record".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.quantumProgramRecord(QchemTextReader.readQuantumProgramRecord(section));
        index = section.endIndex() + 1;
      } else if ("roadmap-record".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.roadmapRecord(QchemTextReader.readRoadmapRecord(section));
        index = section.endIndex() + 1;
      } else if ("extension".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.extension(QchemTextReader.readExtension(section));
        index = section.endIndex() + 1;
      } else {
        diagnostics.add(
            QchemTextReader.error(
                ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
                "Unknown top-level storage section: " + line.command(),
                line.lineNumber()));
        return ChemistryStorageResult.failure(diagnostics);
      }
    }
    return ChemistryStorageResult.success(builder.build(), List.copyOf(diagnostics));
  }

  private static Molecule readMolecule(final Section section) {
    final QchemLine header = section.header();
    final ArrayList<QchemLine> atomLines = new ArrayList<QchemLine>();
    final LinkedHashMap<String, MetadataBuilder> atomMetadata =
        new LinkedHashMap<String, MetadataBuilder>();
    final ArrayList<Bond> bonds = new ArrayList<Bond>();
    final ArrayList<Stereocenter> centers = new ArrayList<Stereocenter>();
    final ArrayList<TorsionAngle> torsions = new ArrayList<TorsionAngle>();
    final MetadataBuilder metadata = new MetadataBuilder();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      switch (line.command()) {
        case "atom":
          atomLines.add(line);
          break;
        case "atom-source":
        case "atom-location":
        case "atom-metadata":
          QchemTextReader.atomMetadataBuilder(atomMetadata, line).acceptAtom(line);
          break;
        case "bond":
          bonds.add(QchemTextReader.readBond(line));
          break;
        case "stereo":
          centers.add(QchemTextReader.readStereocenter(line));
          break;
        case "torsion":
          torsions.add(QchemTextReader.readTorsion(line));
          break;
        case "source":
        case "location":
        case "metadata":
          metadata.accept(line);
          break;
        default:
          throw new StorageTextException(
              ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
              "Unknown molecule storage line: " + line.command(),
              line.lineNumber());
      }
    }
    final ArrayList<Atom> atoms = new ArrayList<Atom>();
    for (int i = 0; i < atomLines.size(); ++i) {
      final QchemLine atomLine = atomLines.get(i);
      final MetadataBuilder atomMetadataBuilder = atomMetadata.get(atomLine.required("id"));
      atoms.add(QchemTextReader.readAtom(atomLine, atomMetadataBuilder));
    }
    return Molecule.of(
        MoleculeId.of(header.required("id")),
        QchemEscaper.unquote(header.required("name")),
        atoms,
        bonds,
        MolecularCharge.of(QchemTextReader.requiredInt(header, "charge")),
        SpinMultiplicity.of(QchemTextReader.requiredInt(header, "spin")),
        Stereochemistry.of(centers),
        MolecularConformation.of(torsions),
        QchemTextReader.readOpticalRotation(header),
        MolecularSymmetry.of(
            PointGroupName.of(header.required("symmetry")),
            QchemTextReader.requiredInt(header, "symmetryNumber")),
        metadata.build());
  }

  private static Atom readAtom(final QchemLine line, final MetadataBuilder metadata) {
    final String xValue = line.required("x");
    final Coordinate3D coordinate;
    if ("none".equals(xValue)) {
      coordinate = null;
    } else {
      coordinate =
          Coordinate3D.of(
              Double.parseDouble(xValue),
              QchemTextReader.requiredDouble(line, "y"),
              QchemTextReader.requiredDouble(line, "z"),
              LengthUnit.valueOf(line.required("unit")));
    }
    final String isotopeValue = line.required("isotope");
    final Isotope isotope =
        "none".equals(isotopeValue) ? null : Isotope.of(Integer.parseInt(isotopeValue));
    return Atom.of(
        AtomId.of(line.required("id")),
        ElementSymbol.of(line.required("element")),
        coordinate,
        FormalCharge.of(QchemTextReader.requiredInt(line, "formal")),
        isotope,
        RadicalState.of(QchemTextReader.requiredInt(line, "radical")),
        metadata == null ? ChemistryMetadata.EMPTY : metadata.build());
  }

  private static Bond readBond(final QchemLine line) {
    return Bond.of(
        AtomId.of(line.required("first")),
        AtomId.of(line.required("second")),
        BondType.valueOf(line.required("type")));
  }

  private static Stereocenter readStereocenter(final QchemLine line) {
    final String secondary = line.required("secondary");
    return Stereocenter.of(
        StereocenterKind.valueOf(line.required("kind")),
        StereochemicalDescriptor.valueOf(line.required("descriptor")),
        AtomId.of(line.required("primary")),
        "none".equals(secondary) ? null : AtomId.of(secondary),
        QchemTextReader.atomIds(line.required("refs")));
  }

  private static TorsionAngle readTorsion(final QchemLine line) {
    return TorsionAngle.of(
        AtomId.of(line.required("first")),
        AtomId.of(line.required("second")),
        AtomId.of(line.required("third")),
        AtomId.of(line.required("fourth")),
        QchemTextReader.requiredDouble(line, "degrees"));
  }

  private static Reaction readReaction(
      final Section section, final Map<String, Molecule> moleculesById) {
    final ArrayList<ReactionParticipant> reactants = new ArrayList<ReactionParticipant>();
    final ArrayList<ReactionParticipant> products = new ArrayList<ReactionParticipant>();
    final ArrayList<ReactionConditionComponent> components =
        new ArrayList<ReactionConditionComponent>();
    final ArrayList<ReactionConditionFeature> features = new ArrayList<ReactionConditionFeature>();
    final MetadataBuilder metadata = new MetadataBuilder();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      switch (line.command()) {
        case "reactant":
          reactants.add(QchemTextReader.readParticipant(line, moleculesById));
          break;
        case "product":
          products.add(QchemTextReader.readParticipant(line, moleculesById));
          break;
        case "condition":
          components.add(QchemTextReader.readCondition(line));
          break;
        case "feature":
          features.add(QchemTextReader.readConditionFeature(line));
          break;
        case "source":
        case "location":
        case "metadata":
          metadata.accept(line);
          break;
        default:
          throw new StorageTextException(
              ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
              "Unknown reaction storage line: " + line.command(),
              line.lineNumber());
      }
    }
    return Reaction.of(
        ReactionId.of(section.header().required("id")),
        QchemEscaper.unquote(section.header().required("name")),
        ReactionSide.of(reactants),
        ReactionSide.of(products),
        QchemTextReader.readConditions(section.header(), components, features),
        metadata.build());
  }

  private static ReactionMechanism readMechanism(
      final Section section,
      final Map<String, Reaction> reactionsById,
      final Map<String, Molecule> moleculesById) {
    final ArrayList<QchemLine> stepLines = new ArrayList<QchemLine>();
    final ArrayList<QchemLine> eventLines = new ArrayList<QchemLine>();
    final ArrayList<MechanismEnergyPoint> energyPoints = new ArrayList<MechanismEnergyPoint>();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      switch (line.command()) {
        case "step":
          stepLines.add(line);
          break;
        case "event":
          eventLines.add(line);
          break;
        case "energy":
          energyPoints.add(QchemTextReader.readEnergyPoint(line));
          break;
        default:
          throw new StorageTextException(
              ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
              "Unknown mechanism storage line: " + line.command(),
              line.lineNumber());
      }
    }
    final ArrayList<ElementaryReactionStep> steps = new ArrayList<ElementaryReactionStep>();
    for (int i = 0; i < stepLines.size(); ++i) {
      steps.add(
          QchemTextReader.readMechanismStep(
              stepLines.get(i), eventLines, reactionsById, moleculesById));
    }
    return ReactionMechanism.of(
        section.header().required("id"),
        ReactionMechanismFamily.valueOf(section.header().required("family")),
        QchemTextReader.requiredReaction(
            reactionsById, section.header().required("reaction"), section.header().lineNumber()),
        steps,
        energyPoints);
  }

  private static ElementaryReactionStep readMechanismStep(
      final QchemLine line,
      final List<QchemLine> eventLines,
      final Map<String, Reaction> reactionsById,
      final Map<String, Molecule> moleculesById) {
    final String stepId = line.required("id");
    final String transitionStateId = line.required("transitionState");
    return ElementaryReactionStep.of(
        stepId,
        QchemTextReader.requiredReaction(
            reactionsById, line.required("reaction"), line.lineNumber()),
        "none".equals(transitionStateId) ? null : moleculesById.get(transitionStateId),
        QchemTextReader.readEnergyValue(line, "forwardBarrier", "forwardBarrierUnit"),
        QchemTextReader.readEnergyValue(line, "reverseBarrier", "reverseBarrierUnit"),
        QchemTextReader.readEnergyValue(line, "reactionEnergy", "reactionEnergyUnit"),
        QchemTextReader.readMechanisticEvents(stepId, eventLines));
  }

  private static List<MechanisticEvent> readMechanisticEvents(
      final String stepId, final List<QchemLine> eventLines) {
    final ArrayList<MechanisticEvent> events = new ArrayList<MechanisticEvent>();
    for (int i = 0; i < eventLines.size(); ++i) {
      final QchemLine line = eventLines.get(i);
      if (!stepId.equals(line.required("step"))) {
        continue;
      }
      events.add(
          MechanisticEvent.of(
              line.required("id"),
              QchemTextReader.requiredInt(line, "order"),
              MechanisticEventKind.valueOf(line.required("kind")),
              QchemEscaper.unquote(line.required("description")),
              QchemTextReader.stringList(QchemEscaper.unquote(line.required("refs"))),
              QchemTextReader.requiredDouble(line, "weight")));
    }
    return List.copyOf(events);
  }

  private static MechanismEnergyPoint readEnergyPoint(final QchemLine line) {
    final String moleculeId = line.required("molecule");
    final String moleculeIds = line.optional("molecules");
    if (moleculeIds != null) {
      return MechanismEnergyPoint.ofMany(
          line.required("id"),
          MechanismPointKind.valueOf(line.required("kind")),
          ReactionCoordinateValue.of(QchemTextReader.requiredDouble(line, "coordinate")),
          EnergyValue.of(
              QchemTextReader.requiredDouble(line, "value"),
              EnergyUnit.valueOf(line.required("unit"))),
          QchemTextReader.moleculeIds(QchemEscaper.unquote(moleculeIds)));
    }
    return MechanismEnergyPoint.of(
        line.required("id"),
        MechanismPointKind.valueOf(line.required("kind")),
        ReactionCoordinateValue.of(QchemTextReader.requiredDouble(line, "coordinate")),
        EnergyValue.of(
            QchemTextReader.requiredDouble(line, "value"),
            EnergyUnit.valueOf(line.required("unit"))),
        "none".equals(moleculeId) ? null : MoleculeId.of(moleculeId));
  }

  private static List<MoleculeId> moleculeIds(final String value) {
    final List<String> ids = QchemTextReader.stringList(value);
    final ArrayList<MoleculeId> result = new ArrayList<MoleculeId>(ids.size());
    for (int i = 0; i < ids.size(); ++i) {
      result.add(MoleculeId.of(ids.get(i)));
    }
    return List.copyOf(result);
  }

  private static ChemistryStorageReactionRecord readReactionRecord(
      final Section section, final Map<String, Reaction> reactionsById) {
    final QchemLine header = section.header();
    return ChemistryStorageReactionRecord.of(
        header.required("id"),
        QchemTextReader.requiredReaction(
            reactionsById, header.required("reaction"), header.lineNumber()),
        ChemistryStorageReactionRecordStatus.valueOf(header.required("status")),
        QchemTextReader.requiredInt(header, "history"),
        QchemTextReader.optionalDouble(header.required("yield")),
        QchemEscaper.unquote(header.required("note")));
  }

  private static ChemistryStoragePredictionRecord readPredictionRecord(final Section section) {
    final QchemLine header = section.header();
    final ArrayList<ChemistryStoragePredictionCandidateRecord> candidates =
        new ArrayList<ChemistryStoragePredictionCandidateRecord>();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      if ("candidate".equals(line.command())) {
        candidates.add(QchemTextReader.readPredictionCandidate(line));
        continue;
      }
      throw new StorageTextException(
          ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
          "Unknown prediction record storage line: " + line.command(),
          line.lineNumber());
    }
    return ChemistryStoragePredictionRecord.of(
        header.required("id"),
        header.required("request"),
        QchemTextReader.requiredInt(header, "history"),
        ReactionPredictionMethodType.valueOf(header.required("method")),
        ChemistryStorageReactionRecordStatus.valueOf(header.required("status")),
        candidates,
        QchemTextReader.diagnosticList(header),
        QchemEscaper.unquote(header.required("note")));
  }

  private static ChemistryStorageReactionOutcomeRecord readReactionOutcomeRecord(
      final Section section) {
    final QchemLine header = section.header();
    return ChemistryStorageReactionOutcomeRecord.of(
        header.required("id"),
        header.required("reaction"),
        QchemTextReader.requiredInt(header, "history"),
        ChemistryStorageReactionOutcomeKind.valueOf(header.required("kind")),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("products"))),
        QchemTextReader.requiredDouble(header, "probability"),
        QchemTextReader.optionalDouble(header.required("yield")),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("mechanisms"))),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("classicalResults"))),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("quantumPrograms"))),
        QchemEscaper.unquote(header.required("condition")),
        QchemEscaper.unquote(header.required("note")));
  }

  private static ChemistryStorageConditionSnapshotRecord readConditionSnapshotRecord(
      final Section section) {
    final QchemLine header = section.header();
    final ArrayList<ReactionConditionComponent> components =
        new ArrayList<ReactionConditionComponent>();
    final ArrayList<ReactionConditionFeature> features = new ArrayList<ReactionConditionFeature>();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      if ("condition".equals(line.command())) {
        components.add(QchemTextReader.readCondition(line));
        continue;
      }
      if ("feature".equals(line.command())) {
        features.add(QchemTextReader.readConditionFeature(line));
        continue;
      }
      throw new StorageTextException(
          ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
          "Unknown condition snapshot storage line: " + line.command(),
          line.lineNumber());
    }
    return ChemistryStorageConditionSnapshotRecord.of(
        header.required("id"),
        header.required("reaction"),
        QchemTextReader.requiredInt(header, "history"),
        QchemEscaper.unquote(header.required("label")),
        QchemTextReader.readConditions(header, components, features),
        QchemEscaper.unquote(QchemTextReader.optionalValue(header, "snapshotNote")));
  }

  private static ChemistryStorageReactionHistoryEvent readReactionHistoryEvent(
      final Section section) {
    final QchemLine header = section.header();
    return ChemistryStorageReactionHistoryEvent.of(
        header.required("id"),
        header.required("reaction"),
        QchemTextReader.requiredInt(header, "history"),
        ChemistryStorageReactionEventKind.valueOf(header.required("kind")),
        QchemEscaper.unquote(header.required("title")),
        QchemEscaper.unquote(header.required("description")),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("reactionRecords"))),
        QchemTextReader.stringList(
            QchemEscaper.unquote(QchemTextReader.optionalValue(header, "conditionSnapshots"))),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("predictions"))),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("computationPlans"))),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("classicalResults"))),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("quantumPrograms"))));
  }

  private static ChemistryStoragePredictionCandidateRecord readPredictionCandidate(
      final QchemLine line) {
    return ChemistryStoragePredictionCandidateRecord.of(
        QchemTextReader.requiredInt(line, "rank"),
        QchemTextReader.stringList(QchemEscaper.unquote(line.required("products"))),
        QchemTextReader.stringList(
            QchemEscaper.unquote(QchemTextReader.optionalValue(line, "formulas"))),
        QchemTextReader.stringListByDelimiter(
            QchemEscaper.unquote(QchemTextReader.optionalValue(line, "stoichiometry")), "\\|\\|"),
        QchemTextReader.requiredDouble(line, "probability"),
        QchemTextReader.requiredDouble(line, "yield"),
        QchemTextReader.stringList(QchemEscaper.unquote(line.required("mechanisms"))),
        QchemTextReader.mechanismDescriptorList(line),
        QchemTextReader.scoreComponentList(line),
        QchemTextReader.evidenceList(line),
        QchemEscaper.unquote(line.required("note")));
  }

  private static List<String> diagnosticList(final QchemLine line) {
    return QchemTextReader.stringListByDelimiter(
        QchemEscaper.unquote(line.required("diagnostics")), "\\|\\|");
  }

  private static List<String> evidenceList(final QchemLine line) {
    return QchemTextReader.stringListByDelimiter(
        QchemEscaper.unquote(line.required("evidence")), "\\|\\|");
  }

  private static List<String> mechanismDescriptorList(final QchemLine line) {
    final String raw = line.optional("mechanismDetails");
    if (raw == null) {
      return List.of();
    }
    return QchemTextReader.stringListByDelimiter(QchemEscaper.unquote(raw), "\\|\\|");
  }

  private static List<String> scoreComponentList(final QchemLine line) {
    final String raw = line.optional("score");
    if (raw == null) {
      return List.of();
    }
    return QchemTextReader.stringListByDelimiter(QchemEscaper.unquote(raw), "\\|\\|");
  }

  private static ChemistryStorageComputationPlanRecord readComputationPlanRecord(
      final Section section) {
    final QchemLine header = section.header();
    return ChemistryStorageComputationPlanRecord.of(
        header.required("id"),
        header.required("reactionRecord"),
        QchemTextReader.requiredInt(header, "history"),
        ChemistryStorageReactionRecordStatus.valueOf(header.required("status")),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("classical"))),
        QchemTextReader.stringList(
            QchemEscaper.unquote(QchemTextReader.optionalValue(header, "classicalResults"))),
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("quantum"))),
        Boolean.parseBoolean(header.required("deferred")),
        QchemEscaper.unquote(header.required("note")));
  }

  private static ChemistryStorageClassicalResultRecord readClassicalResultRecord(
      final Section section) {
    final QchemLine header = section.header();
    final LinkedHashMap<Integer, ClassicalResultSectionBuilder> builders =
        new LinkedHashMap<Integer, ClassicalResultSectionBuilder>();
    final LinkedHashMap<Integer, ChemistryStorageClassicalCoverageItemRecord> coverageItems =
        new LinkedHashMap<Integer, ChemistryStorageClassicalCoverageItemRecord>();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      if ("coverage".equals(line.command())) {
        final int index = QchemTextReader.requiredInt(line, "index");
        if (coverageItems.containsKey(index)) {
          throw new StorageTextException(
              ChemistryStorageDiagnosticCode.DOMAIN_REJECTED_VALUE,
              "Duplicate classical result coverage index: " + index,
              line.lineNumber());
        }
        coverageItems.put(
            index,
            ChemistryStorageClassicalCoverageItemRecord.of(
                line.required("kind"),
                line.required("status"),
                QchemEscaper.unquote(line.required("model"))));
      } else if ("section".equals(line.command())) {
        final int index = QchemTextReader.requiredInt(line, "index");
        if (builders.containsKey(index)) {
          throw new StorageTextException(
              ChemistryStorageDiagnosticCode.DOMAIN_REJECTED_VALUE,
              "Duplicate classical result section index: " + index,
              line.lineNumber());
        }
        builders.put(
            index,
            new ClassicalResultSectionBuilder(
                line.required("kind"), QchemEscaper.unquote(line.required("title"))));
      } else if ("value".equals(line.command())) {
        final int sectionIndex = QchemTextReader.requiredInt(line, "section");
        final ClassicalResultSectionBuilder builder = builders.get(sectionIndex);
        if (builder == null) {
          throw new StorageTextException(
              ChemistryStorageDiagnosticCode.DOMAIN_REJECTED_VALUE,
              "Classical result value references unknown section index: " + sectionIndex,
              line.lineNumber());
        }
        builder.add(
            ChemistryStorageClassicalResultValueRecord.of(
                line.required("name"),
                line.required("kind"),
                QchemEscaper.unquote(line.required("unit")),
                QchemTextReader.stringListByDelimiter(
                    QchemEscaper.unquote(line.required("values")), "\\|\\|")));
      } else {
        throw new StorageTextException(
            ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
            "Unknown classical result record storage line: " + line.command(),
            line.lineNumber());
      }
    }
    final ArrayList<ChemistryStorageClassicalResultSectionRecord> sections =
        new ArrayList<ChemistryStorageClassicalResultSectionRecord>(builders.size());
    for (final Map.Entry<Integer, ClassicalResultSectionBuilder> entry : builders.entrySet()) {
      sections.add(entry.getValue().build());
    }
    return ChemistryStorageClassicalResultRecord.of(
        header.required("id"),
        header.required("calculation"),
        QchemTextReader.requiredInt(header, "history"),
        header.required("status"),
        QchemTextReader.requiredInt(header, "diagnostics"),
        QchemTextReader.requiredInt(header, "errors"),
        QchemTextReader.requiredInt(header, "warnings"),
        header.required("coverage"),
        List.copyOf(coverageItems.values()),
        sections,
        QchemEscaper.unquote(header.required("note")));
  }

  private static ChemistryStorageQuantumProgramRecord readQuantumProgramRecord(
      final Section section) {
    final QchemLine header = section.header();
    final ArrayList<String> nativeLines = new ArrayList<String>();
    final ArrayList<String> exportLines = new ArrayList<String>();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      if (!"payload".equals(line.command())) {
        throw new StorageTextException(
            ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
            "Unknown quantum program record storage line: " + line.command(),
            line.lineNumber());
      }
      final String kind = line.required("kind");
      if ("native".equals(kind)) {
        nativeLines.add(QchemEscaper.unquote(line.required("text")));
      } else if ("export".equals(kind)) {
        exportLines.add(QchemEscaper.unquote(line.required("text")));
      } else {
        throw new StorageTextException(
            ChemistryStorageDiagnosticCode.DOMAIN_REJECTED_VALUE,
            "Unsupported quantum program payload kind: " + kind,
            line.lineNumber());
      }
    }
    return ChemistryStorageQuantumProgramRecord.of(
        header.required("id"),
        header.required("source"),
        QchemTextReader.requiredInt(header, "history"),
        header.required("kind"),
        header.required("target"),
        QchemTextReader.joinPayload(nativeLines),
        QchemTextReader.joinPayload(exportLines),
        QchemTextReader.stringListByDelimiter(
            QchemEscaper.unquote(header.required("diagnostics")), "\\|\\|"),
        QchemEscaper.unquote(header.required("note")));
  }

  private static ChemistryStorageRoadmapRecord readRoadmapRecord(final Section section) {
    final QchemLine header = section.header();
    final ArrayList<ChemistryStorageRoadmapCandidateRecord> candidates =
        new ArrayList<ChemistryStorageRoadmapCandidateRecord>();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      if ("roadmap-candidate".equals(line.command())) {
        candidates.add(QchemTextReader.readRoadmapCandidate(line));
        continue;
      }
      throw new StorageTextException(
          ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
          "Unknown roadmap record storage line: " + line.command(),
          line.lineNumber());
    }
    return ChemistryStorageRoadmapRecord.of(
        header.required("id"),
        header.required("request"),
        QchemTextReader.requiredInt(header, "history"),
        header.required("status"),
        QchemTextReader.requiredInt(header, "scenarios"),
        QchemTextReader.requiredInt(header, "evaluatedScenarios"),
        candidates,
        QchemTextReader.stringList(QchemEscaper.unquote(header.required("programTargets"))),
        QchemEscaper.unquote(header.required("note")));
  }

  private static ChemistryStorageRoadmapCandidateRecord readRoadmapCandidate(final QchemLine line) {
    return ChemistryStorageRoadmapCandidateRecord.of(
        QchemTextReader.requiredInt(line, "index"),
        QchemTextReader.requiredDouble(line, "probability"),
        QchemTextReader.requiredDouble(line, "yield"),
        QchemTextReader.stringList(QchemEscaper.unquote(line.required("products"))),
        QchemTextReader.stringList(
            QchemEscaper.unquote(QchemTextReader.optionalValue(line, "formulas"))),
        QchemTextReader.stringListByDelimiter(
            QchemEscaper.unquote(QchemTextReader.optionalValue(line, "stoichiometry")), "\\|\\|"),
        QchemTextReader.stringList(QchemEscaper.unquote(line.required("mechanisms"))),
        QchemTextReader.textList(
            QchemEscaper.unquote(QchemTextReader.optionalValue(line, "mechanismDetails"))),
        QchemTextReader.stringList(QchemEscaper.unquote(line.required("classical"))),
        QchemTextReader.stringList(
            QchemEscaper.unquote(QchemTextReader.optionalValue(line, "classicalPlans"))),
        QchemTextReader.textList(
            QchemEscaper.unquote(QchemTextReader.optionalValue(line, "followUpActions"))),
        QchemTextReader.stringList(QchemEscaper.unquote(line.required("quantumTargets"))),
        QchemTextReader.optionalBoolean(line, "explicitProductGraph", false),
        QchemTextReader.optionalBoolean(line, "electronicRerankReady", false),
        QchemTextReader.optionalBoolean(line, "quantumWorkflowReady", false),
        QchemEscaper.unquote(
            QchemTextReader.optionalValue(line, "readinessReason", "not_checked")));
  }

  private static boolean optionalBoolean(
      final QchemLine line, final String key, final boolean defaultValue) {
    final String value = line.optional(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }

  private static String optionalValue(final QchemLine line, final String key) {
    final String value = line.optional(key);
    return value == null ? "" : value;
  }

  private static String optionalValue(
      final QchemLine line, final String key, final String defaultValue) {
    final String value = line.optional(key);
    return value == null ? defaultValue : value;
  }

  private static ReactionParticipant readParticipant(
      final QchemLine line, final Map<String, Molecule> moleculesById) {
    final String moleculeId = line.required("molecule");
    final Molecule molecule = moleculesById.get(moleculeId);
    if (molecule == null) {
      throw new StorageTextException(
          ChemistryStorageDiagnosticCode.UNKNOWN_MOLECULE_REFERENCE,
          "Reaction references unknown molecule: " + moleculeId,
          line.lineNumber());
    }
    return ReactionParticipant.of(
        molecule, StoichiometricCoefficient.of(QchemTextReader.requiredInt(line, "coefficient")));
  }

  static ReactionConditionComponent readCondition(final QchemLine line) {
    return ReactionConditionComponent.of(
        ReactionComponentRole.valueOf(line.required("role")),
        QchemEscaper.unquote(line.required("name")),
        QchemTextReader.readAmount(line),
        QchemTextReader.readConcentration(line),
        QchemTextReader.readEquivalent(line),
        QchemTextReader.readLoading(line),
        QchemTextReader.readPurity(line),
        ReactionPhase.valueOf(line.required("phase")),
        QchemEscaper.unquote(line.required("note")));
  }

  static ReactionConditionFeature readConditionFeature(final QchemLine line) {
    return ReactionConditionFeature.of(
        ReactionConditionFeatureKind.valueOf(line.required("kind")),
        QchemEscaper.unquote(line.required("value")),
        QchemEscaper.unquote(line.required("note")));
  }

  private static SubstanceAmount readAmount(final QchemLine line) {
    final String value = line.required("amount");
    if ("none".equals(value)) {
      return null;
    }
    return SubstanceAmount.of(
        Double.parseDouble(value), SubstanceAmountUnit.valueOf(line.required("amountUnit")));
  }

  private static MolarConcentration readConcentration(final QchemLine line) {
    final String value = line.required("concentration");
    if ("none".equals(value)) {
      return null;
    }
    return MolarConcentration.of(
        Double.parseDouble(value),
        MolarConcentrationUnit.valueOf(line.required("concentrationUnit")));
  }

  private static StoichiometricEquivalent readEquivalent(final QchemLine line) {
    final String value = line.required("equivalent");
    return "none".equals(value) ? null : StoichiometricEquivalent.of(Double.parseDouble(value));
  }

  private static ReactionComponentLoading readLoading(final QchemLine line) {
    final String value = line.required("loadingPercent");
    return "none".equals(value)
        ? null
        : ReactionComponentLoading.percent(Double.parseDouble(value));
  }

  private static ReactionComponentPurity readPurity(final QchemLine line) {
    final String value = line.required("purityPercent");
    return "none".equals(value) ? null : ReactionComponentPurity.percent(Double.parseDouble(value));
  }

  static ReactionConditions readConditions(
      final QchemLine header,
      final List<ReactionConditionComponent> components,
      final List<ReactionConditionFeature> features) {
    final String temperatureValue = header.optional("temperature");
    final Temperature temperature =
        temperatureValue == null
            ? null
            : Temperature.of(
                Double.parseDouble(temperatureValue),
                TemperatureUnit.valueOf(header.required("temperatureUnit")));
    final String pressureValue = header.optional("pressure");
    final Pressure pressure =
        pressureValue == null
            ? null
            : Pressure.of(
                Double.parseDouble(pressureValue),
                PressureUnit.valueOf(header.required("pressureUnit")));
    final String durationValue = header.optional("duration");
    final TimeDuration duration =
        durationValue == null
            ? null
            : TimeDuration.of(
                Double.parseDouble(durationValue),
                TimeDurationUnit.valueOf(header.required("durationUnit")));
    final String noteValue = header.optional("note");
    return ReactionConditions.of(
        temperature,
        pressure,
        duration,
        components,
        features,
        noteValue == null ? null : QchemEscaper.unquote(noteValue));
  }

  private static ChemistryStorageExtension readExtension(final Section section) {
    final LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
    final ArrayList<String> body = new ArrayList<String>();
    for (int i = 0; i < section.body().size(); ++i) {
      final QchemLine line = section.body().get(i);
      if ("property".equals(line.command())) {
        properties.put(line.required("key"), QchemEscaper.unquote(line.required("value")));
      } else if ("body".equals(line.command())) {
        body.add(QchemEscaper.unquote(line.required("text")));
      } else {
        throw new StorageTextException(
            ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
            "Unknown extension storage line: " + line.command(),
            line.lineNumber());
      }
    }
    return ChemistryStorageExtension.of(
        section.header().required("kind"), section.header().required("id"), properties, body);
  }

  private static Reaction requiredReaction(
      final Map<String, Reaction> reactionsById, final String reactionId, final int lineNumber) {
    final Reaction reaction = reactionsById.get(reactionId);
    if (reaction != null) {
      return reaction;
    }
    throw new StorageTextException(
        ChemistryStorageDiagnosticCode.UNKNOWN_SECTION,
        "Storage section references unknown reaction: " + reactionId,
        lineNumber);
  }

  private static EnergyValue readEnergyValue(
      final QchemLine line, final String valueKey, final String unitKey) {
    final String value = line.required(valueKey);
    if ("none".equals(value)) {
      return null;
    }
    return EnergyValue.of(Double.parseDouble(value), EnergyUnit.valueOf(line.required(unitKey)));
  }

  private static Double optionalDouble(final String value) {
    return "none".equals(value) ? null : Double.valueOf(value);
  }

  private static List<String> stringList(final String raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    final String[] parts = raw.split(",");
    final ArrayList<String> result = new ArrayList<String>();
    for (int i = 0; i < parts.length; ++i) {
      if (parts[i].isEmpty()) {
        continue;
      }
      result.add(parts[i]);
    }
    return List.copyOf(result);
  }

  private static List<String> textList(final String raw) {
    return QchemTextReader.stringListByDelimiter(raw, "\\|\\|");
  }

  private static List<String> stringListByDelimiter(final String raw, final String delimiterRegex) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    final String[] parts = raw.split(delimiterRegex);
    final ArrayList<String> result = new ArrayList<String>();
    for (int i = 0; i < parts.length; ++i) {
      if (parts[i].isEmpty()) {
        continue;
      }
      result.add(parts[i]);
    }
    return List.copyOf(result);
  }

  private static String joinPayload(final List<String> lines) {
    if (lines.isEmpty()) {
      return null;
    }
    return String.join("\n", lines);
  }

  private static OpticalRotation readOpticalRotation(final QchemLine header) {
    final OpticalRotationDirection direction =
        OpticalRotationDirection.valueOf(header.required("rotation"));
    final String degreesValue = header.required("rotationDegrees");
    final String wavelengthValue = header.required("rotationWavelengthNm");
    return OpticalRotation.of(
        direction,
        "null".equals(degreesValue) ? null : Double.valueOf(degreesValue),
        null,
        "null".equals(wavelengthValue) ? null : Double.valueOf(wavelengthValue));
  }

  private static ArrayList<AtomId> atomIds(final String raw) {
    final ArrayList<AtomId> result = new ArrayList<AtomId>();
    final String[] parts = raw.split(",");
    for (int i = 0; i < parts.length; ++i) {
      result.add(AtomId.of(parts[i]));
    }
    return result;
  }

  private static ArrayList<QchemLine> parseLines(
      final String content, final List<ChemistryStorageDiagnostic> diagnostics) {
    final ArrayList<QchemLine> result = new ArrayList<QchemLine>();
    final String[] rawLines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");
    for (int i = 0; i < rawLines.length; ++i) {
      try {
        final QchemLine line = QchemLine.parse(i + 1, rawLines[i]);
        if (line != null) {
          result.add(line);
        }
      } catch (final RuntimeException exception) {
        diagnostics.add(
            QchemTextReader.error(
                ChemistryStorageDiagnosticCode.INVALID_LINE, exception.getMessage(), i + 1));
      }
    }
    return result;
  }

  private static ChemistryStorageFormatVersion readVersion(final QchemLine header) {
    try {
      return ChemistryStorageFormatVersion.of(QchemTextReader.requiredInt(header, "version"));
    } catch (final RuntimeException exception) {
      throw new StorageTextException(
          ChemistryStorageDiagnosticCode.UNSUPPORTED_VERSION,
          exception.getMessage(),
          header.lineNumber());
    }
  }

  private static MetadataBuilder atomMetadataBuilder(
      final Map<String, MetadataBuilder> builders, final QchemLine line) {
    final String atomId = line.required("atom");
    MetadataBuilder builder = builders.get(atomId);
    if (builder == null) {
      builder = new MetadataBuilder();
      builders.put(atomId, builder);
    }
    return builder;
  }

  private static int requiredInt(final QchemLine line, final String key) {
    return Integer.parseInt(line.required(key));
  }

  private static double requiredDouble(final QchemLine line, final String key) {
    return Double.parseDouble(line.required(key));
  }

  private static ChemistryStorageDiagnostic error(
      final ChemistryStorageDiagnosticCode code, final String message, final int line) {
    return ChemistryStorageDiagnostic.of(
        ChemistryStorageDiagnosticSeverity.ERROR,
        code,
        message == null ? "Storage operation failed." : message,
        line);
  }

  /**
   * Блок top-level section между header и end.
   */
  private static final class Section {

    private final QchemLine header;
    private final List<QchemLine> body;
    private final int endIndex;

    private Section(final QchemLine header, final List<QchemLine> body, final int endIndex) {
      this.header = header;
      this.body = body;
      this.endIndex = endIndex;
    }

    static Section collect(final List<QchemLine> lines, final int startIndex) {
      final ArrayList<QchemLine> body = new ArrayList<QchemLine>();
      for (int i = startIndex + 1; i < lines.size(); ++i) {
        final QchemLine line = lines.get(i);
        if ("end".equals(line.command())) {
          return new Section(lines.get(startIndex), body, i);
        }
        body.add(line);
      }
      throw new StorageTextException(
          ChemistryStorageDiagnosticCode.MISSING_END,
          "Storage section has no end line.",
          lines.get(startIndex).lineNumber());
    }

    QchemLine header() {
      return this.header;
    }

    List<QchemLine> body() {
      return this.body;
    }

    int endIndex() {
      return this.endIndex;
    }
  }

  /**
   * Mutable builder для metadata внутри molecule/reaction block.
   */
  private static final class MetadataBuilder {

    private ChemistrySource source;
    private ChemistrySourceLocation location;
    private final LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>();

    void accept(final QchemLine line) {
      if ("source".equals(line.command())) {
        this.source =
            ChemistrySource.of(
                QchemEscaper.unquote(line.required("format")),
                QchemEscaper.unquote(line.required("description")));
      } else if ("location".equals(line.command())) {
        this.location =
            ChemistrySourceLocation.of(
                QchemTextReader.requiredInt(line, "line"),
                QchemTextReader.requiredInt(line, "column"));
      } else if ("metadata".equals(line.command())) {
        this.attributes.put(line.required("key"), QchemEscaper.unquote(line.required("value")));
      } else {
        throw new IllegalArgumentException("Unsupported metadata line.");
      }
    }

    void acceptAtom(final QchemLine line) {
      if ("atom-source".equals(line.command())) {
        this.source =
            ChemistrySource.of(
                QchemEscaper.unquote(line.required("format")),
                QchemEscaper.unquote(line.required("description")));
      } else if ("atom-location".equals(line.command())) {
        this.location =
            ChemistrySourceLocation.of(
                QchemTextReader.requiredInt(line, "line"),
                QchemTextReader.requiredInt(line, "column"));
      } else if ("atom-metadata".equals(line.command())) {
        this.attributes.put(line.required("key"), QchemEscaper.unquote(line.required("value")));
      } else {
        throw new IllegalArgumentException("Unsupported atom metadata line.");
      }
    }

    ChemistryMetadata build() {
      return ChemistryMetadata.of(this.source, this.location, this.attributes);
    }
  }

  private static final class StorageTextException extends RuntimeException {

    private final ChemistryStorageDiagnosticCode code;
    private final int line;

    private StorageTextException(
        final ChemistryStorageDiagnosticCode code, final String message, final int line) {
      super(message);
      this.code = code;
      this.line = line;
    }

    ChemistryStorageDiagnosticCode code() {
      return this.code;
    }

    int line() {
      return this.line;
    }
  }

  private static final class ClassicalResultSectionBuilder {

    private final String kind;
    private final String title;
    private final ArrayList<ChemistryStorageClassicalResultValueRecord> values =
        new ArrayList<ChemistryStorageClassicalResultValueRecord>();

    private ClassicalResultSectionBuilder(final String kind, final String title) {
      this.kind = kind;
      this.title = title;
    }

    private void add(final ChemistryStorageClassicalResultValueRecord value) {
      this.values.add(value);
    }

    private ChemistryStorageClassicalResultSectionRecord build() {
      return ChemistryStorageClassicalResultSectionRecord.of(this.kind, this.title, this.values);
    }
  }
}
