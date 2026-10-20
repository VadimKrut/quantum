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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;

/** Durable computational artifacts always retain the input separately from the derived geometry. */
public final class LaboratoryComputationFiles {
  private LaboratoryComputationFiles() {}

  public static Molecule geometrySnapshot(
      final Molecule geometry, final String id, final String method) {
    return geometrySnapshot(geometry, id, method, null);
  }

  public static Molecule geometrySnapshot(
      final Molecule geometry, final String id, final String method, final Boolean converged) {
    final java.util.Map<String, String> attributes =
        new java.util.LinkedHashMap<>(geometry.metadata().attributes());
    attributes.put("laboratory.geometry.origin", method);
    attributes.remove("laboratory.geometry.method_version");
    if ("GRAPH_SEED".equals(method)) {
      attributes.put(
          "laboratory.geometry.method_version",
          ru.pathcreator.vadim.quantum.chemistry.api.geometry.ChemistryGeometrySupport
              .INITIAL_METHOD_VERSION);
    }
    attributes.put("laboratory.geometry.snapshot", id);
    attributes.remove("laboratory.geometry.converged");
    if (converged != null) {
      attributes.put("laboratory.geometry.converged", converged.toString());
    }
    return Molecule.of(
        MoleculeId.of(id),
        geometry.displayName(),
        geometry.atoms(),
        geometry.bonds(),
        geometry.charge(),
        geometry.spinMultiplicity(),
        geometry.stereochemistry(),
        geometry.conformation(),
        geometry.opticalRotation(),
        geometry.symmetry(),
        ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata.of(
            geometry.metadata().source(), geometry.metadata().location(), attributes));
  }

  public static Path write(
      final Path directory, final String id, final ChemistryStorageDocument document)
      throws IOException {
    ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue.requireIdentifier(
        id, "Calculation artifact id");
    final var storage = new ChemistryStorageService();
    final var written = storage.write(document);
    if (!written.success()) {
      throw new IOException("Расчёт не удалось записать: " + written.diagnostics());
    }
    final var checked = storage.read(written.value());
    if (!checked.success() || !checked.value().equals(document)) {
      throw new IOException("Расчёт не прошёл проверку обратного чтения.");
    }
    Files.createDirectories(directory);
    final Path target = directory.resolve(id + ".qchem");
    final Path temporary = Files.createTempFile(directory, ".calculation-", ".tmp");
    try {
      Files.writeString(temporary, written.value());
      try (final var channel =
          java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) {
        channel.force(true);
      }
      Files.move(
          temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
    return target;
  }
}
