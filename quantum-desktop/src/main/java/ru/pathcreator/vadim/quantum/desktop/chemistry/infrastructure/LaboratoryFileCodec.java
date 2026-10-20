/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure;

import java.util.LinkedHashMap;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;

/** A notebook is a native chemistry document with explicitly versioned layout extensions. */
public final class LaboratoryFileCodec {
  public static final int MAX_DOCUMENT_BYTES = 64 * 1024 * 1024;

  private final ChemistryStorageService storage = new ChemistryStorageService();

  public String write(final LaboratoryDrawing drawing) {
    return this.write(drawing, 1);
  }

  private String write(final LaboratoryDrawing drawing, final int minimumVersion) {
    final var experiment = drawing.experiment();
    final var builder =
        ChemistryStorageDocument.builder(experiment.id(), experiment.title())
            .version(
                ru.pathcreator.vadim.quantum.chemistry.storage.domain.model
                    .ChemistryStorageFormatVersion.of(minimumVersion))
            .laboratoryExperiment(experiment);
    final LinkedHashMap<String, Molecule> molecules = new LinkedHashMap<>();
    for (final LaboratoryComponent component : experiment.components()) {
      final Molecule previous =
          molecules.putIfAbsent(component.molecule().id().value(), component.molecule());
      if (previous != null && !previous.equals(component.molecule())) {
        throw new IllegalArgumentException("Two different molecules have the same identifier.");
      }
      builder.extension(
          LaboratoryLayoutCodec.write(component.id(), drawing.layouts().get(component.id())));
    }
    for (final var analysis : experiment.analyses()) {
      final Molecule previous =
          molecules.putIfAbsent(analysis.molecule().id().value(), analysis.molecule());
      if (previous != null && !previous.equals(analysis.molecule())) {
        throw new IllegalArgumentException("Conflicting analysis snapshot molecule identifier.");
      }
    }
    for (final var sample : experiment.samples()) {
      final Molecule previous =
          molecules.putIfAbsent(sample.molecule().id().value(), sample.molecule());
      if (previous != null && !previous.equals(sample.molecule())) {
        throw new IllegalArgumentException("Conflicting sample snapshot molecule identifier.");
      }
    }
    for (final var evidence : experiment.analyticalEvidence().entries()) {
      final Molecule previous =
          molecules.putIfAbsent(evidence.molecule().id().value(), evidence.molecule());
      if (previous != null && !previous.equals(evidence.molecule())) {
        throw new IllegalArgumentException(
            "Conflicting analytical evidence snapshot molecule identifier.");
      }
    }
    for (final var outcome : experiment.operations().stageOutcomes()) {
      for (final var product :
          java.util.stream.Stream.concat(outcome.targets().stream(), outcome.byproducts().stream())
              .toList()) {
        final Molecule previous =
            molecules.putIfAbsent(product.snapshot().id().value(), product.snapshot());
        if (previous != null && !previous.equals(product.snapshot())) {
          throw new IllegalArgumentException(
              "Conflicting stage outcome snapshot molecule identifier.");
        }
      }
    }
    molecules.values().forEach(builder::molecule);
    final var result = this.storage.write(builder.build());
    if (!result.success()) {
      throw new IllegalArgumentException("QCHEM: " + diagnostics(result.diagnostics()));
    }
    if (result.value().length() > MAX_DOCUMENT_BYTES) {
      throw new IllegalArgumentException("Текст опыта превышает 64 МиБ.");
    }
    return result.value();
  }

  public LaboratoryDrawing read(final String text) {
    if (text == null || text.length() > MAX_DOCUMENT_BYTES) {
      throw new IllegalArgumentException("Текст опыта отсутствует или превышает 64 МиБ.");
    }
    final var result = this.storage.read(text);
    if (!result.success()) {
      throw new IllegalArgumentException(
          "Не удалось прочитать QCHEM: " + diagnostics(result.diagnostics()));
    }
    final ChemistryStorageDocument source = result.value();
    if (source.laboratoryExperiment() == null) {
      throw new IllegalArgumentException(
          "В файле нет лабораторного опыта. Импорт обычного QCHEM ещё не подключён.");
    }
    final LinkedHashMap<String, MoleculeDrawing> layouts = new LinkedHashMap<>();
    for (final LaboratoryComponent component : source.laboratoryExperiment().components()) {
      final var matching =
          source.extensions().stream()
              .filter(
                  extension ->
                      extension.kind().equals(LaboratoryLayoutCodec.KIND)
                          && extension.id().equals(component.id()))
              .toList();
      if (matching.size() != 1) {
        throw new IllegalArgumentException("Отсутствует или дублируется раскладка компонента.");
      }
      layouts.put(
          component.id(), LaboratoryLayoutCodec.read(matching.getFirst(), component.molecule()));
    }
    final LaboratoryDrawing drawing = new LaboratoryDrawing(source.laboratoryExperiment(), layouts);
    // Refuse unsupported sections rather than silently discard data when saving in this editor.
    final var reconstructed = this.storage.read(this.write(drawing, source.version().number()));
    if (!reconstructed.success() || !source.equals(reconstructed.value())) {
      throw new IllegalArgumentException(
          "Файл содержит дополнительные данные, которые этот редактор пока не поддерживает.");
    }
    return drawing;
  }

  private static String diagnostics(
      final java.util.List<
              ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic
                  .ChemistryStorageDiagnostic>
          items) {
    return items.stream()
        .map(
            item ->
                (item.line() > 0 ? "Строка " + item.line() + " · " : "")
                    + item.code()
                    + ": "
                    + item.message())
        .collect(java.util.stream.Collectors.joining("\n"));
  }
}
