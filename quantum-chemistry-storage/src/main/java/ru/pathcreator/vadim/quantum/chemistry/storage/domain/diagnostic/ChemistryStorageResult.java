/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic;

import java.util.List;
import java.util.Objects;

/**
 * Результат storage-операции без необходимости ловить исключения в нормальном workflow.
 */
public final class ChemistryStorageResult<T> {

  private final T value;
  private final List<ChemistryStorageDiagnostic> diagnostics;

  private ChemistryStorageResult(
      final T value,
      final List<ChemistryStorageDiagnostic> diagnostics) {
    this.value = value;
    this.diagnostics = diagnostics;
  }

  public static <T> ChemistryStorageResult<T> success(
      final T value,
      final List<ChemistryStorageDiagnostic> diagnostics) {
    if (value == null) {
      throw new IllegalArgumentException("Storage result value must not be null.");
    }
    return new ChemistryStorageResult<T>(
        value,
        ChemistryStorageResult.requireDiagnostics(diagnostics));
  }

  public static <T> ChemistryStorageResult<T> failure(
      final List<ChemistryStorageDiagnostic> diagnostics) {
    return new ChemistryStorageResult<T>(
        null,
        ChemistryStorageResult.requireDiagnostics(diagnostics));
  }

  public boolean success() {
    return this.value != null && !this.hasErrors();
  }

  public T value() {
    if (!this.success()) {
      throw new IllegalStateException("Storage result does not contain successful value.");
    }
    return this.value;
  }

  public List<ChemistryStorageDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public boolean hasErrors() {
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (!this.diagnostics.get(i).error()) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static List<ChemistryStorageDiagnostic> requireDiagnostics(
      final List<ChemistryStorageDiagnostic> diagnostics) {
    if (diagnostics == null) {
      throw new IllegalArgumentException("Storage diagnostics must not be null.");
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Storage diagnostic must not be null.");
    }
    return List.copyOf(diagnostics);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageResult<?>)) {
      return false;
    }
    final ChemistryStorageResult<?> result = (ChemistryStorageResult<?>) other;
    return Objects.equals(this.value, result.value)
        && Objects.equals(this.diagnostics, result.diagnostics);
  }

  public int hashCode() {
    return Objects.hash(this.value, this.diagnostics);
  }
}