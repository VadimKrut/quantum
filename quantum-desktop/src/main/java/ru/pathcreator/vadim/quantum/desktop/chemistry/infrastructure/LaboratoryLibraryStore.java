/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryMoleculePlacement;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryCategory;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryDifficulty;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntry;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryEntryKind;
import ru.pathcreator.vadim.quantum.chemistry.library.domain.catalog.ChemistryLibraryAttachmentPoint;
import ru.pathcreator.vadim.quantum.chemistry.library.infrastructure.filesystem.ChemistryLibraryFileRepository;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;

/** Notebook adapter over the existing chemistry library; no separate catalog or format. */
public final class LaboratoryLibraryStore {
  private static final String PORTS = "library.entry.attachment.";

  public ChemistryLibraryEntry fragment(
      final MoleculeDrawing drawing,
      final String name,
      final String description,
      final List<String> tags,
      final List<String> references,
      final List<ChemistryLibraryAttachmentPoint> points) {
    if (points.isEmpty() || points.size() > 64) {
      throw new IllegalArgumentException("Укажите от 1 до 64 точек присоединения.");
    }
    final var structure = this.structure(drawing, name, description, tags, references);
    final var builder =
        structure.document().toBuilder()
            .metadata(PORTS + "version", "1")
            .metadata(PORTS + "count", Integer.toString(points.size()));
    for (int index = 0; index < points.size(); index++) {
      final var point = points.get(index);
      builder
          .metadata(PORTS + index + ".atom", point.atomId().value())
          .metadata(PORTS + index + ".label", point.label())
          .metadata(PORTS + index + ".bond", point.bondType().name());
    }
    final var result =
        ChemistryLibraryEntry.of(
            structure.id(),
            name,
            ChemistryLibraryEntryKind.FRAGMENT,
            structure.category(),
            structure.difficulty(),
            description,
            tags,
            references,
            builder.build());
    this.attachmentPoints(result);
    return result;
  }

  public List<ChemistryLibraryAttachmentPoint> attachmentPoints(final ChemistryLibraryEntry entry) {
    if (entry.kind() != ChemistryLibraryEntryKind.FRAGMENT) {
      return List.of();
    }
    final var metadata = entry.document().metadata();
    if (metadata.keySet().stream().noneMatch(key -> key.startsWith(PORTS))) {
      return List.of();
    }
    if (!"1".equals(metadata.get(PORTS + "version")) || entry.document().molecules().size() != 1) {
      throw new IllegalArgumentException("Неподдержанный формат точек присоединения.");
    }
    final int count;
    try {
      count = Integer.parseInt(metadata.get(PORTS + "count"));
    } catch (final RuntimeException exception) {
      throw new IllegalArgumentException("Повреждено количество точек присоединения.");
    }
    if (count < 1 || count > 64) {
      throw new IllegalArgumentException("Допустимо от 1 до 64 точек присоединения.");
    }
    final var allowed = new java.util.HashSet<String>(List.of(PORTS + "version", PORTS + "count"));
    final var atoms =
        new java.util.HashSet<ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId>();
    final var labels = new java.util.HashSet<String>();
    final var points = new java.util.ArrayList<ChemistryLibraryAttachmentPoint>();
    for (int index = 0; index < count; index++) {
      for (final String field : List.of("atom", "label", "bond")) {
        allowed.add(PORTS + index + "." + field);
      }
      final ChemistryLibraryAttachmentPoint point;
      try {
        point =
            new ChemistryLibraryAttachmentPoint(
                ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId.of(
                    metadata.get(PORTS + index + ".atom")),
                metadata.get(PORTS + index + ".label"),
                ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType.valueOf(
                    metadata.get(PORTS + index + ".bond")));
      } catch (final RuntimeException exception) {
        throw new IllegalArgumentException("Повреждена точка присоединения " + (index + 1) + ".");
      }
      if (!atoms.add(point.atomId()) || !labels.add(point.label())) {
        throw new IllegalArgumentException("Точки должны иметь разные атомы и подписи.");
      }
      if (entry.document().molecules().getFirst().atoms().stream()
          .noneMatch(atom -> atom.id().equals(point.atomId()))) {
        throw new IllegalArgumentException("Точка присоединения ссылается на отсутствующий атом.");
      }
      points.add(point);
    }
    if (metadata.keySet().stream()
        .anyMatch(key -> key.startsWith(PORTS) && !allowed.contains(key))) {
      throw new IllegalArgumentException("Неизвестные поля точек присоединения.");
    }
    return List.copyOf(points);
  }

  public ChemistryLibraryEntry structure(
      final MoleculeDrawing drawing,
      final String name,
      final String description,
      final List<String> tags,
      final List<String> references) {
    final var experiment =
        new LaboratoryExperiment(
            "library_" + UUID.randomUUID(),
            name,
            ReactionConditions.EMPTY,
            "",
            List.of(
                new LaboratoryComponent(
                    "structure", drawing.molecule(), ReactionComponentRole.REACTANT)));
    return entry(
        new LaboratoryDrawing(experiment, Map.of("structure", drawing)),
        name,
        description,
        tags,
        references,
        ChemistryLibraryEntryKind.MOLECULE);
  }

  public ChemistryLibraryEntry template(
      final LaboratoryDrawing source,
      final String name,
      final String description,
      final List<String> tags,
      final List<String> references) {
    return entry(
        LaboratoryEditor.plannedCopy(source, name),
        name,
        description,
        tags,
        references,
        ChemistryLibraryEntryKind.WORKFLOW_TEMPLATE);
  }

  private static ChemistryLibraryEntry entry(
      final LaboratoryDrawing drawing,
      final String name,
      final String description,
      final List<String> tags,
      final List<String> references,
      final ChemistryLibraryEntryKind kind) {
    final var stored = new ChemistryStorageService().read(new LaboratoryFileCodec().write(drawing));
    if (!stored.success()) {
      throw new IllegalArgumentException(
          "Не удалось подготовить библиотечную запись: " + stored.diagnostics());
    }
    return ChemistryLibraryEntry.of(
        "user_" + UUID.randomUUID(),
        name,
        kind,
        ChemistryLibraryCategory.GENERAL,
        ChemistryLibraryDifficulty.STANDARD,
        description,
        tags,
        references,
        stored.value());
  }

  public LaboratoryDrawing notebook(final ChemistryLibraryEntry entry) {
    final var written =
        new ChemistryStorageService().write(ChemistryLibraryFileRepository.payload(entry));
    if (!written.success()) {
      throw new IllegalArgumentException("Не удалось открыть шаблон: " + written.diagnostics());
    }
    return new LaboratoryFileCodec().read(written.value());
  }

  public LaboratoryDrawing instantiate(final ChemistryLibraryEntry entry) {
    if (entry.kind() != ChemistryLibraryEntryKind.WORKFLOW_TEMPLATE) {
      throw new IllegalArgumentException("Выберите шаблон опыта.");
    }
    return LaboratoryEditor.plannedCopy(this.notebook(entry), entry.displayName());
  }

  public MoleculeDrawing drawing(final ChemistryLibraryEntry entry, final Molecule molecule) {
    if (!entry.document().molecules().contains(molecule)) {
      throw new IllegalArgumentException("Структура отсутствует в записи.");
    }
    if (entry.document().laboratoryExperiment() != null) {
      final var notebook = this.notebook(entry);
      final var component =
          notebook.experiment().components().stream()
              .filter(item -> item.molecule().equals(molecule))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Для структуры нет рисунка."));
      return notebook.layouts().get(component.id());
    }
    return new LaboratoryMoleculePlacement().place(molecule, 180, 170);
  }

  public void save(final Path folder, final ChemistryLibraryEntry entry) throws IOException {
    final Path directory = folder.resolve(".chemistry-library");
    Files.createDirectories(directory);
    final Path staging = Files.createTempDirectory(directory, ".staging-");
    final Path file = staging.resolve(entry.id() + ".qchem");
    try {
      final var repository = new ChemistryLibraryFileRepository(staging);
      repository.save(entry);
      final var loaded = repository.load(file);
      if (!ChemistryLibraryFileRepository.payload(loaded)
              .equals(ChemistryLibraryFileRepository.payload(entry))
          || !loaded.displayName().equals(entry.displayName())
          || !loaded.tags().equals(entry.tags())
          || !loaded.references().equals(entry.references())
          || !loaded.summary().equals(entry.summary())
          || !this.attachmentPoints(loaded).equals(this.attachmentPoints(entry))
          || loaded.kind() != entry.kind()
          || loaded.category() != entry.category()
          || loaded.difficulty() != entry.difficulty()) {
        throw new IOException("Проверка библиотечной записи после сохранения не пройдена.");
      }
      try (final var channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
        channel.force(true);
      }
      Files.move(file, directory.resolve(file.getFileName()), StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(file);
      Files.deleteIfExists(staging);
    }
  }
}
