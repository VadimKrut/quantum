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
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistrySource;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistrySourceLocation;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentPurity;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
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
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageFormatVersion;

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
                ChemistryStorageDiagnosticCode.EMPTY_INPUT,
                "Storage input is empty.",
                0));
        return ChemistryStorageResult.failure(diagnostics);
      }
      final ArrayList<QchemLine> lines = QchemTextReader.parseLines(content, diagnostics);
      if (!diagnostics.isEmpty()) {
        return ChemistryStorageResult.failure(diagnostics);
      }
      return QchemTextReader.readLines(lines, diagnostics);
    } catch (final StorageTextException exception) {
      diagnostics.add(
          QchemTextReader.error(
              exception.code(),
              exception.getMessage(),
              exception.line()));
      return ChemistryStorageResult.failure(diagnostics);
    } catch (final RuntimeException exception) {
      diagnostics.add(
          QchemTextReader.error(
              ChemistryStorageDiagnosticCode.DOMAIN_REJECTED_VALUE,
              exception.getMessage(),
              0));
      return ChemistryStorageResult.failure(diagnostics);
    }
  }

  private static ChemistryStorageResult<ChemistryStorageDocument> readLines(
      final List<QchemLine> lines,
      final List<ChemistryStorageDiagnostic> diagnostics) {
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
            project.required("id"),
            QchemEscaper.unquote(project.required("name")));
    builder.version(version);
    final LinkedHashMap<String, Molecule> moleculesById = new LinkedHashMap<String, Molecule>();
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
      } else if ("reaction".equals(line.command())) {
        final Section section = Section.collect(lines, index);
        builder.reaction(QchemTextReader.readReaction(section, moleculesById));
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

  private static Atom readAtom(
      final QchemLine line,
      final MetadataBuilder metadata) {
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
      final Section section,
      final Map<String, Molecule> moleculesById) {
    final ArrayList<ReactionParticipant> reactants = new ArrayList<ReactionParticipant>();
    final ArrayList<ReactionParticipant> products = new ArrayList<ReactionParticipant>();
    final ArrayList<ReactionConditionComponent> components =
        new ArrayList<ReactionConditionComponent>();
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
        QchemTextReader.readConditions(section.header(), components),
        metadata.build());
  }

  private static ReactionParticipant readParticipant(
      final QchemLine line,
      final Map<String, Molecule> moleculesById) {
    final String moleculeId = line.required("molecule");
    final Molecule molecule = moleculesById.get(moleculeId);
    if (molecule == null) {
      throw new StorageTextException(
          ChemistryStorageDiagnosticCode.UNKNOWN_MOLECULE_REFERENCE,
          "Reaction references unknown molecule: " + moleculeId,
          line.lineNumber());
    }
    return ReactionParticipant.of(
        molecule,
        StoichiometricCoefficient.of(QchemTextReader.requiredInt(line, "coefficient")));
  }

  private static ReactionConditionComponent readCondition(final QchemLine line) {
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

  private static SubstanceAmount readAmount(final QchemLine line) {
    final String value = line.required("amount");
    if ("none".equals(value)) {
      return null;
    }
    return SubstanceAmount.of(
        Double.parseDouble(value),
        SubstanceAmountUnit.valueOf(line.required("amountUnit")));
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
    return "none".equals(value) ? null : ReactionComponentLoading.percent(Double.parseDouble(value));
  }

  private static ReactionComponentPurity readPurity(final QchemLine line) {
    final String value = line.required("purityPercent");
    return "none".equals(value) ? null : ReactionComponentPurity.percent(Double.parseDouble(value));
  }

  private static ReactionConditions readConditions(
      final QchemLine header,
      final List<ReactionConditionComponent> components) {
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
    final String noteValue = header.optional("note");
    return ReactionConditions.of(
        temperature,
        pressure,
        components,
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
        section.header().required("kind"),
        section.header().required("id"),
        properties,
        body);
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
      final String content,
      final List<ChemistryStorageDiagnostic> diagnostics) {
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
                ChemistryStorageDiagnosticCode.INVALID_LINE,
                exception.getMessage(),
                i + 1));
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
      final Map<String, MetadataBuilder> builders,
      final QchemLine line) {
    final String atomId = line.required("atom");
    MetadataBuilder builder = builders.get(atomId);
    if (builder == null) {
      builder = new MetadataBuilder();
      builders.put(atomId, builder);
    }
    return builder;
  }

  private static int requiredInt(
      final QchemLine line,
      final String key) {
    return Integer.parseInt(line.required(key));
  }

  private static double requiredDouble(
      final QchemLine line,
      final String key) {
    return Double.parseDouble(line.required(key));
  }

  private static ChemistryStorageDiagnostic error(
      final ChemistryStorageDiagnosticCode code,
      final String message,
      final int line) {
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

    private Section(
        final QchemLine header,
        final List<QchemLine> body,
        final int endIndex) {
      this.header = header;
      this.body = body;
      this.endIndex = endIndex;
    }

    static Section collect(
        final List<QchemLine> lines,
        final int startIndex) {
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
        final ChemistryStorageDiagnosticCode code,
        final String message,
        final int line) {
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
}