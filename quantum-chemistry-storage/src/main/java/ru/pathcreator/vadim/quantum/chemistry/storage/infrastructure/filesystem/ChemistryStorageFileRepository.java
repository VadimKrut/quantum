/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;

/**
 * File-system repository для `.qchem` файлов без привязки UI к parser/writer.
 */
public final class ChemistryStorageFileRepository {

  private final ChemistryStorageService storageService;

  public ChemistryStorageFileRepository() {
    this(new ChemistryStorageService());
  }

  public ChemistryStorageFileRepository(final ChemistryStorageService storageService) {
    if (storageService == null) {
      throw new IllegalArgumentException("Chemistry storage service must not be null.");
    }
    this.storageService = storageService;
  }

  public ChemistryStorageResult<ChemistryStorageDocument> read(final Path path) {
    if (path == null) {
      return ChemistryStorageFileRepository.ioFailure("Storage path must not be null.");
    }
    try {
      return this.storageService.read(Files.readString(path, StandardCharsets.UTF_8));
    } catch (final IOException exception) {
      return ChemistryStorageFileRepository.ioFailure(exception.getMessage());
    }
  }

  public ChemistryStorageResult<Path> write(
      final Path path,
      final ChemistryStorageDocument document) {
    if (path == null) {
      return ChemistryStorageFileRepository.ioFailure("Storage path must not be null.");
    }
    final ChemistryStorageResult<String> result = this.storageService.write(document);
    if (!result.success()) {
      return ChemistryStorageResult.failure(result.diagnostics());
    }
    try {
      final Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(path, result.value(), StandardCharsets.UTF_8);
      return ChemistryStorageResult.success(path, List.of());
    } catch (final IOException exception) {
      return ChemistryStorageFileRepository.ioFailure(exception.getMessage());
    }
  }

  private static <T> ChemistryStorageResult<T> ioFailure(final String message) {
    return ChemistryStorageResult.failure(
        List.of(
            ChemistryStorageDiagnostic.of(
                ChemistryStorageDiagnosticSeverity.ERROR,
                ChemistryStorageDiagnosticCode.IO_FAILURE,
                message == null ? "Storage IO failed." : message,
                0)));
  }
}