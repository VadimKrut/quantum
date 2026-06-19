/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.application;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text.QchemTextReader;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.text.QchemTextWriter;

/**
 * Стабильный application facade для чтения и записи chemistry storage document.
 */
public final class ChemistryStorageService {

  private final QchemTextReader reader;
  private final QchemTextWriter writer;

  public ChemistryStorageService() {
    this(new QchemTextReader(), new QchemTextWriter());
  }

  public ChemistryStorageService(
      final QchemTextReader reader,
      final QchemTextWriter writer) {
    if (reader == null) {
      throw new IllegalArgumentException("Chemistry storage reader must not be null.");
    }
    if (writer == null) {
      throw new IllegalArgumentException("Chemistry storage writer must not be null.");
    }
    this.reader = reader;
    this.writer = writer;
  }

  public ChemistryStorageResult<ChemistryStorageDocument> read(final String content) {
    return this.reader.read(content);
  }

  public ChemistryStorageResult<String> write(final ChemistryStorageDocument document) {
    try {
      return ChemistryStorageResult.success(this.writer.write(document), List.of());
    } catch (final RuntimeException exception) {
      return ChemistryStorageResult.failure(
          List.of(
              ChemistryStorageDiagnostic.of(
                  ChemistryStorageDiagnosticSeverity.ERROR,
                  ChemistryStorageDiagnosticCode.DOMAIN_REJECTED_VALUE,
                  exception.getMessage() == null ? "Storage write failed." : exception.getMessage(),
                  0)));
    }
  }

  public ChemistryStorageResult<ChemistryStorageDocument> roundTrip(
      final ChemistryStorageDocument document) {
    final ChemistryStorageResult<String> writeResult = this.write(document);
    if (!writeResult.success()) {
      return ChemistryStorageResult.failure(writeResult.diagnostics());
    }
    return this.read(writeResult.value());
  }
}