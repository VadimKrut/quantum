/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text;

import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;

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
    final List<Reaction> reactions = document.reactions();
    for (int i = 0; i < reactions.size(); ++i) {
      QchemTextWriter.writeReaction(builder, reactions.get(i));
    }
    final List<ChemistryStorageExtension> extensions = document.extensions();
    for (int i = 0; i < extensions.size(); ++i) {
      QchemTextWriter.writeExtension(builder, extensions.get(i));
    }
    return builder.toString().trim();
  }

  private static void writeMolecule(
      final StringBuilder builder,
      final Molecule molecule) {
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

  private static void writeAtom(
      final StringBuilder builder,
      final Atom atom) {
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

  private static void writeAtomMetadata(
      final StringBuilder builder,
      final Atom atom) {
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
      final StringBuilder builder,
      final Atom atom,
      final Map<String, String> values) {
    for (final Map.Entry<String, String> entry : values.entrySet()) {
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

  private static void writeBond(
      final StringBuilder builder,
      final Bond bond) {
    builder
        .append("  bond first=")
        .append(bond.firstAtomId().value())
        .append(" second=")
        .append(bond.secondAtomId().value())
        .append(" type=")
        .append(bond.type())
        .append("\r\n");
  }

  private static void writeStereocenter(
      final StringBuilder builder,
      final Stereocenter center) {
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

  private static void writeTorsion(
      final StringBuilder builder,
      final TorsionAngle torsion) {
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

  private static void writeReaction(
      final StringBuilder builder,
      final Reaction reaction) {
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
    }
    builder.append("\r\n");
    QchemTextWriter.writeReactionSide(builder, "reactant", reaction.reactants());
    QchemTextWriter.writeReactionSide(builder, "product", reaction.products());
    final List<ReactionConditionComponent> components = reaction.conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
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
          .append(
              component.hasConcentration() ? component.concentration().unit().name() : "none")
          .append(" equivalent=")
          .append(
              component.hasEquivalent()
                  ? Double.toString(component.equivalent().value())
                  : "none")
          .append(" loadingPercent=")
          .append(component.hasLoading() ? Double.toString(component.loading().percent()) : "none")
          .append(" purityPercent=")
          .append(component.hasPurity() ? Double.toString(component.purity().percent()) : "none")
          .append(" note=")
          .append(QchemEscaper.quote(component.note()))
          .append("\r\n");
    }
    QchemTextWriter.writeMetadata(builder, reaction.metadata());
    builder.append("end\r\n");
  }

  private static void writeReactionSide(
      final StringBuilder builder,
      final String command,
      final ReactionSide side) {
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

  private static void writeExtension(
      final StringBuilder builder,
      final ChemistryStorageExtension extension) {
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

  private static void writeMetadata(
      final StringBuilder builder,
      final ChemistryMetadata metadata) {
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
      final StringBuilder builder,
      final String command,
      final Map<String, String> values) {
    for (final Map.Entry<String, String> entry : values.entrySet()) {
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