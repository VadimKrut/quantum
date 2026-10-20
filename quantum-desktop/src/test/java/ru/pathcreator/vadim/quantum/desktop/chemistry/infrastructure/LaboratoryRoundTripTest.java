/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSketchService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;

final class LaboratoryRoundTripTest {

  @TempDir Path directory;
  private final MoleculeSketchService sketch = new MoleculeSketchService();

  @Test
  void createsEditsSavesClosesAndReopensWithoutLosingCoreOrLayout() throws Exception {
    final LaboratoryEditor editor = new LaboratoryEditor();
    final MoleculeDrawing ring = this.sketch.create(true, 190, 240);
    final String component = editor.add(ring, ReactionComponentRole.REACTANT);
    final var atom = ring.molecule().atoms().getFirst().id();
    final MoleculeDrawing nitrogen = this.sketch.changeAtom(ring, atom, "N", 1, 15);
    final MoleculeDrawing moved = this.sketch.move(nitrogen, atom, 260, 175);
    final MoleculeDrawing extended =
        this.sketch.extend(moved, atom, "O", BondType.DOUBLE, 330, 175);
    editor.replace(component, extended, ReactionComponentRole.REACTANT);
    editor.details(
        "Опыт № 1: \"проверка\"",
        ReactionConditions.of(
            Temperature.of(80, TemperatureUnit.CELSIUS),
            null,
            TimeDuration.of(2.5, TimeDurationUnit.HOUR),
            List.of(),
            List.of(),
            "Нагревание\nПри перемешивании"),
        "Наблюдение\n\nРусский текст, путь C:\\опыты\\1 и символы α β");
    final var expected = editor.document();
    final Path file = this.directory.resolve("опыт.qchem");
    new LaboratoryFileRepository().write(file, expected, null);
    final LaboratoryEditor restarted = new LaboratoryEditor();
    restarted.open(new LaboratoryFileRepository().read(file).drawing());
    assertEquals(expected, restarted.document());
    assertFalse(restarted.dirty());
    assertFalse(restarted.canUndo());
    final var coreDocument = new ChemistryStorageService().read(Files.readString(file));
    assertTrue(coreDocument.success());
    assertEquals(extended.molecule(), coreDocument.value().molecules().getFirst());
    assertEquals(expected.experiment(), coreDocument.value().laboratoryExperiment());
    assertTrue(extended.molecule().atoms().stream().noneMatch(value -> value.hasCoordinate()));
  }

  @Test
  void savesEmptyDraftAndKeepsSameMoleculeInDifferentRoles() {
    final LaboratoryEditor editor = new LaboratoryEditor();
    final LaboratoryFileCodec codec = new LaboratoryFileCodec();
    assertEquals(editor.document(), codec.read(codec.write(editor.document())));
    final MoleculeDrawing molecule = this.sketch.create(false, 100, 100);
    editor.add(molecule, ReactionComponentRole.REACTANT);
    editor.add(
        this.sketch.move(molecule, molecule.molecule().atoms().getFirst().id(), 500, 100),
        ReactionComponentRole.PRODUCT);
    final var restored = codec.read(codec.write(editor.document()));
    assertEquals(2, restored.experiment().components().size());
    assertEquals(editor.document(), restored);
  }

  @Test
  void detectsExternalEditAndKeepsPreviousVersionAndDirtyState() throws Exception {
    final LaboratoryEditor editor = new LaboratoryEditor();
    final LaboratoryFileRepository repository = new LaboratoryFileRepository();
    final Path file = this.directory.resolve("experiment.qchem");
    final String initial = repository.write(file, editor.document(), null);
    final String original = Files.readString(file);
    editor.add(this.sketch.create(true, 100, 100), ReactionComponentRole.REACTANT);
    final String updated = repository.write(file, editor.document(), initial);
    assertEquals(original, Files.readString(this.directory.resolve("experiment.qchem.bak")));
    Files.writeString(file, "external change");
    assertThrows(IOException.class, () -> repository.write(file, editor.document(), updated));
    assertEquals("external change", Files.readString(file));
    assertTrue(editor.dirty());
    try (final var paths = Files.list(this.directory)) {
      assertTrue(paths.noneMatch(path -> path.toString().endsWith(".tmp")));
    }
  }

  @Test
  void editingAnOccurrenceDoesNotCorruptTheSameMoleculeInAnotherRole() {
    final LaboratoryEditor editor = new LaboratoryEditor();
    final MoleculeDrawing molecule = this.sketch.create(true, 100, 100);
    editor.add(molecule, ReactionComponentRole.REACTANT);
    final String product = editor.add(molecule, ReactionComponentRole.PRODUCT);
    editor.replace(
        product,
        this.sketch.changeAtom(molecule, molecule.molecule().atoms().getFirst().id(), "N", 0, null),
        ReactionComponentRole.PRODUCT);
    assertEquals(
        molecule.molecule(), editor.document().experiment().components().getFirst().molecule());
    assertNotEquals(
        molecule.moleculeId(),
        editor.document().experiment().components().getLast().molecule().id());
    final LaboratoryFileCodec codec = new LaboratoryFileCodec();
    assertEquals(editor.document(), codec.read(codec.write(editor.document())));
  }

  @Test
  void undoRedoIncludesDocumentFieldsAndDirtyMarker() {
    final LaboratoryEditor editor = new LaboratoryEditor();
    editor.add(this.sketch.create(true, 100, 100), ReactionComponentRole.REACTANT);
    editor.markSaved();
    final var saved = editor.document();
    editor.details("Изменённый опыт", ReactionConditions.EMPTY, "Новая заметка");
    assertTrue(editor.dirty());
    editor.undo();
    assertEquals(saved, editor.document());
    assertFalse(editor.dirty());
    editor.redo();
    assertEquals("Новая заметка", editor.document().experiment().notes());
    assertTrue(editor.dirty());
  }

  @Test
  void preservesStereochemistryAndRichCoreConditionsAndRejectsInvalidStereoEdit() {
    MoleculeDrawing drawing = this.sketch.create(false, 150, 150);
    final var center = drawing.molecule().atoms().getFirst().id();
    for (final String symbol : List.of("F", "Cl", "Br", "H")) {
      drawing =
          this.sketch.extend(
              drawing, center, symbol, BondType.SINGLE, 100 + drawing.atoms().size() * 40, 250);
    }
    final Molecule base = drawing.molecule();
    final var atoms = base.atoms();
    final Stereochemistry stereo =
        Stereochemistry.of(
            List.of(
                Stereocenter.ofTetrahedralAtom(
                    center,
                    StereochemicalDescriptor.R,
                    atoms.get(1).id(),
                    atoms.get(2).id(),
                    atoms.get(3).id(),
                    atoms.get(4).id())));
    final Molecule molecule =
        Molecule.of(
            base.id(),
            base.displayName(),
            atoms,
            base.bonds(),
            base.charge(),
            base.spinMultiplicity(),
            stereo,
            base.metadata());
    final var styles =
        drawing.bonds().stream()
            .map(
                bond ->
                    BondDrawing.of(
                        bond.firstAtomId(), bond.secondAtomId(), BondDrawingStyle.WEDGE_UP))
            .toList();
    final MoleculeDrawing chiral = MoleculeDrawing.of(molecule, drawing.atoms(), styles, List.of());
    final LaboratoryEditor editor = new LaboratoryEditor();
    editor.add(chiral, ReactionComponentRole.REACTANT);
    editor.details(
        "Хиральный образец",
        ReactionConditions.of(
            null,
            Pressure.of(1, PressureUnit.ATMOSPHERE),
            null,
            List.of(ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "Вода")),
            List.of(ReactionConditionFeature.named(ReactionConditionFeatureKind.INERT_ATMOSPHERE)),
            null),
        "");
    final LaboratoryFileCodec codec = new LaboratoryFileCodec();
    assertEquals(editor.document(), codec.read(codec.write(editor.document())));
    assertEquals(molecule, this.sketch.move(chiral, center, 350, 360).molecule());
    assertThrows(
        IllegalArgumentException.class, () -> this.sketch.removeAtom(chiral, atoms.get(1).id()));
  }

  @Test
  void rejectsDamagedLayoutAndUnsupportedSectionsInsteadOfDroppingThem() {
    final LaboratoryEditor editor = new LaboratoryEditor();
    editor.add(this.sketch.create(true, 100, 100), ReactionComponentRole.REACTANT);
    final LaboratoryFileCodec codec = new LaboratoryFileCodec();
    final String text = codec.write(editor.document());
    assertThrows(
        IllegalArgumentException.class,
        () -> codec.read(text.replace("laboratory-layout-v1", "laboratory-layout-v99")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            codec.read(
                text
                    + "extension kind=future-data id=unknown\r\n  body text=\"do not discard\"\r\nend\r\n"));
  }
}
