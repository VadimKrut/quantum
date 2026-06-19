/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.filesystem.ChemistryStorageFileRepository;

public final class ChemistryStorageRoundTripTest {

  @Test
  public void roundTripKeepsMoleculeReactionAndExtensions() {
    final Molecule reagent = ChemistryStorageRoundTripTest.chiralMolecule();
    final Molecule product = ChemistryStorageRoundTripTest.productMolecule();
    final Reaction reaction = ChemistryStorageRoundTripTest.reaction(reagent, product);
    final ChemistryStorageDocument document =
        ChemistryStorageDocument.builder("project_alpha", "Alpha chemistry project")
            .metadata("owner", "lab")
            .metadata("purpose", "round-trip")
            .molecule(reagent)
            .molecule(product)
            .reaction(reaction)
            .extension(
                ChemistryStorageExtension.of(
                    "classical_task",
                    "task_1",
                    Map.of("engine", "future_java"),
                    List.of("line one", "line two with spaces")))
            .build();

    final ChemistryStorageService service = new ChemistryStorageService();
    final ChemistryStorageResult<String> written = service.write(document);
    Assertions.assertTrue(written.success(), written.diagnostics().toString());
    Assertions.assertFalse(written.value().contains("{"));
    Assertions.assertFalse(written.value().contains("}"));

    final ChemistryStorageResult<ChemistryStorageDocument> read = service.read(written.value());
    Assertions.assertTrue(read.success(), read.diagnostics().toString());
    Assertions.assertEquals(document, read.value());

    final ChemistryStorageResult<ChemistryStorageDocument> secondRead =
        service.read(service.write(read.value()).value());
    Assertions.assertTrue(secondRead.success(), secondRead.diagnostics().toString());
    Assertions.assertEquals(read.value(), secondRead.value());
  }

  @Test
  public void parserReturnsDiagnosticsForInvalidInput() {
    final ChemistryStorageService service = new ChemistryStorageService();
    final ChemistryStorageResult<ChemistryStorageDocument> empty = service.read("   ");
    Assertions.assertFalse(empty.success());
    Assertions.assertTrue(empty.hasErrors());
    Assertions.assertEquals(
        ChemistryStorageDiagnosticCode.EMPTY_INPUT,
        empty.diagnostics().get(0).code());

    final ChemistryStorageResult<ChemistryStorageDocument> invalid =
        service.read("QCHEM version=1\r\nmolecule id=x name=\"bad\"\r\nend");
    Assertions.assertFalse(invalid.success());
    Assertions.assertTrue(invalid.hasErrors());

    final ChemistryStorageResult<ChemistryStorageDocument> unclosedQuote =
        service.read("QCHEM version=1\r\nproject id=broken name=\"Broken\r\n");
    Assertions.assertFalse(unclosedQuote.success());
    Assertions.assertEquals(
        ChemistryStorageDiagnosticCode.INVALID_LINE,
        unclosedQuote.diagnostics().get(0).code());
  }

  @Test
  public void parserReturnsSpecificDiagnosticsForStorageContractErrors() {
    final ChemistryStorageService service = new ChemistryStorageService();
    final String missingEnd =
        "QCHEM version=1\r\n"
            + "project id=project_missing_end name=\"Missing end\"\r\n"
            + "molecule id=water name=\"Water\" charge=0 spin=1 symmetry=C1 symmetryNumber=1 "
            + "rotation=NONE rotationDegrees=null rotationWavelengthNm=null\r\n";
    final ChemistryStorageResult<ChemistryStorageDocument> missingEndResult =
        service.read(missingEnd);
    Assertions.assertFalse(missingEndResult.success());
    Assertions.assertEquals(
        ChemistryStorageDiagnosticCode.MISSING_END,
        missingEndResult.diagnostics().get(0).code());

    final String unknownReference =
        "QCHEM version=1\r\n"
            + "project id=project_unknown_ref name=\"Unknown reference\"\r\n"
            + "reaction id=r1 name=\"Broken\"\r\n"
            + "  reactant molecule=missing coefficient=1\r\n"
            + "  product molecule=missing coefficient=1\r\n"
            + "end\r\n";
    final ChemistryStorageResult<ChemistryStorageDocument> unknownReferenceResult =
        service.read(unknownReference);
    Assertions.assertFalse(unknownReferenceResult.success());
    Assertions.assertEquals(
        ChemistryStorageDiagnosticCode.UNKNOWN_MOLECULE_REFERENCE,
        unknownReferenceResult.diagnostics().get(0).code());
  }

  @Test
  public void documentRejectsDuplicateMoleculeIdsAndDetachedReactionMolecules() {
    final Molecule water = ChemistryStorageRoundTripTest.productMolecule();
    final Executable duplicateMoleculeDocumentCreation =
        new Executable() {
          public void execute() {
            ChemistryStorageDocument.builder("duplicates", "Duplicates")
                .molecule(water)
                .molecule(water)
                .build();
          }
        };
    Assertions.assertThrows(
        IllegalArgumentException.class,
        duplicateMoleculeDocumentCreation);

    final Molecule reagent = ChemistryStorageRoundTripTest.chiralMolecule();
    final Reaction detachedReaction = ChemistryStorageRoundTripTest.reaction(reagent, water);
    final Executable detachedReactionDocumentCreation =
        new Executable() {
          public void execute() {
            ChemistryStorageDocument.builder("detached", "Detached")
                .molecule(water)
                .reaction(detachedReaction)
                .build();
          }
        };
    Assertions.assertThrows(
        IllegalArgumentException.class,
        detachedReactionDocumentCreation);
  }

  @Test
  public void fileRepositoryWritesAndReadsUtf8StorageFile() throws Exception {
    final ChemistryStorageDocument document =
        ChemistryStorageDocument.builder("project_file", "File project")
            .molecule(ChemistryStorageRoundTripTest.productMolecule())
            .build();
    final Path directory = Files.createTempDirectory("qchem-storage-test");
    final Path file = directory.resolve("sample.qchem");
    final ChemistryStorageFileRepository repository = new ChemistryStorageFileRepository();

    final ChemistryStorageResult<Path> written = repository.write(file, document);
    Assertions.assertTrue(written.success(), written.diagnostics().toString());
    Assertions.assertTrue(Files.exists(file));

    final ChemistryStorageResult<ChemistryStorageDocument> read = repository.read(file);
    Assertions.assertTrue(read.success(), read.diagnostics().toString());
    Assertions.assertEquals(document, read.value());
  }

  private static Molecule chiralMolecule() {
    final AtomId c = AtomId.of("c");
    final AtomId h = AtomId.of("h");
    final AtomId cl = AtomId.of("cl");
    final AtomId br = AtomId.of("br");
    final AtomId f = AtomId.of("f");
    return Molecule.of(
        MoleculeId.of("chiral_halide"),
        "Chiral halide",
        List.of(
            ChemistryStorageRoundTripTest.atomWithMetadata(c, "C", 0.0, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(h, "H", 1.0, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(cl, "Cl", 0.0, 1.0, 0.0),
            ChemistryStorageRoundTripTest.atom(br, "Br", 0.0, 0.0, 1.0),
            Atom.of(
                f,
                ElementSymbol.of("F"),
                Coordinate3D.of(-1.0, -1.0, -1.0, LengthUnit.ANGSTROM),
                FormalCharge.NEUTRAL,
                Isotope.of(19),
                RadicalState.CLOSED_SHELL,
                ChemistryMetadata.EMPTY)),
        List.of(
            Bond.of(c, h, BondType.SINGLE),
            Bond.of(c, cl, BondType.SINGLE),
            Bond.of(c, br, BondType.SINGLE),
            Bond.of(c, f, BondType.SINGLE),
            Bond.of(h, cl, BondType.SINGLE),
            Bond.of(cl, br, BondType.SINGLE),
            Bond.of(br, f, BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        Stereochemistry.of(
            List.of(Stereocenter.ofTetrahedralAtom(c, StereochemicalDescriptor.R, h, cl, br, f))),
        MolecularConformation.of(List.of(TorsionAngle.of(h, cl, br, f, 60.0))),
        OpticalRotation.of(
            OpticalRotationDirection.DEXTROROTATORY,
            12.5,
            null,
            589.0),
        MolecularSymmetry.of(PointGroupName.C1, 1),
        ChemistryMetadata.of(
            ChemistrySource.of("manual", "unit test"),
            ChemistrySourceLocation.of(3, 7),
            Map.of("note", "metadata survives")));
  }

  private static Molecule productMolecule() {
    final AtomId o = AtomId.of("o");
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    return Molecule.of(
        MoleculeId.of("water"),
        "Water",
        List.of(
            ChemistryStorageRoundTripTest.atom(o, "O", 0.0, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(h1, "H", 0.96, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(h2, "H", -0.24, 0.93, 0.0)),
        List.of(Bond.of(o, h1, BondType.SINGLE), Bond.of(o, h2, BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Reaction reaction(
      final Molecule reagent,
      final Molecule product) {
    return Reaction.of(
        ReactionId.of("hydrolysis"),
        "Hydrolysis storage check",
        ReactionSide.of(List.of(ReactionParticipant.of(reagent, StoichiometricCoefficient.ONE))),
        ReactionSide.of(List.of(ReactionParticipant.of(product, StoichiometricCoefficient.of(2)))),
        ReactionConditions.of(
            Temperature.of(298.15, TemperatureUnit.KELVIN),
            Pressure.of(1.0, PressureUnit.ATMOSPHERE),
            List.of(
                ReactionConditionComponent.of(
                    ReactionComponentRole.SOLVENT,
                    "water",
                    SubstanceAmount.of(1.5, SubstanceAmountUnit.MOLE),
                    MolarConcentration.of(0.1, MolarConcentrationUnit.MOLE_PER_LITER),
                    StoichiometricEquivalent.of(2.0),
                    ReactionComponentLoading.percent(5.0),
                    ReactionComponentPurity.percent(99.0),
                    ReactionPhase.AQUEOUS,
                    "condition note")),
            "reaction note"),
        ChemistryMetadata.of(null, null, Map.of("route", "test")));
  }

  private static Atom atom(
      final AtomId id,
      final String symbol,
      final double x,
      final double y,
      final double z) {
    return Atom.of(
        id,
        ElementSymbol.of(symbol),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM));
  }

  private static Atom atomWithMetadata(
      final AtomId id,
      final String symbol,
      final double x,
      final double y,
      final double z) {
    return Atom.of(
        id,
        ElementSymbol.of(symbol),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM),
        FormalCharge.NEUTRAL,
        null,
        RadicalState.CLOSED_SHELL,
        ChemistryMetadata.of(
            ChemistrySource.of("lab_book", "atom provenance"),
            ChemistrySourceLocation.of(5, 11),
            Map.of("role", "stereocenter")));
  }
}