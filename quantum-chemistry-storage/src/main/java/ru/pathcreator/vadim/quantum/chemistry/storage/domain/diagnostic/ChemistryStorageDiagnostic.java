/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Структурированная storage-диагностика с координатой строки.
 */
public final class ChemistryStorageDiagnostic {

  private final ChemistryStorageDiagnosticSeverity severity;
  private final ChemistryStorageDiagnosticCode code;
  private final String message;
  private final int line;

  private ChemistryStorageDiagnostic(
      final ChemistryStorageDiagnosticSeverity severity,
      final ChemistryStorageDiagnosticCode code,
      final String message,
      final int line) {
    this.severity = severity;
    this.code = code;
    this.message = message;
    this.line = line;
  }

  public static ChemistryStorageDiagnostic of(
      final ChemistryStorageDiagnosticSeverity severity,
      final ChemistryStorageDiagnosticCode code,
      final String message,
      final int line) {
    if (severity == null) {
      throw new IllegalArgumentException("Storage diagnostic severity must not be null.");
    }
    if (code == null) {
      throw new IllegalArgumentException("Storage diagnostic code must not be null.");
    }
    if (line < 0) {
      throw new IllegalArgumentException("Storage diagnostic line must not be negative.");
    }
    return new ChemistryStorageDiagnostic(
        severity,
        code,
        TextValue.requireText(message, "Storage diagnostic message"),
        line);
  }

  public ChemistryStorageDiagnosticSeverity severity() {
    return this.severity;
  }

  public ChemistryStorageDiagnosticCode code() {
    return this.code;
  }

  public String message() {
    return this.message;
  }

  public int line() {
    return this.line;
  }

  public boolean error() {
    return this.severity == ChemistryStorageDiagnosticSeverity.ERROR;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageDiagnostic)) {
      return false;
    }
    final ChemistryStorageDiagnostic diagnostic = (ChemistryStorageDiagnostic) other;
    return this.line == diagnostic.line
        && this.severity == diagnostic.severity
        && this.code == diagnostic.code
        && Objects.equals(this.message, diagnostic.message);
  }

  public int hashCode() {
    return Objects.hash(this.severity, this.code, this.message, this.line);
  }
}