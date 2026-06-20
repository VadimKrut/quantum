/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Immutable-набор diagnostics с быстрыми флагами ошибок и предупреждений.
 */
public final class ClassicalDiagnosticSet {

  public static final ClassicalDiagnosticSet EMPTY = new ClassicalDiagnosticSet(List.of());

  private final List<ClassicalDiagnostic> diagnostics;

  private ClassicalDiagnosticSet(final List<ClassicalDiagnostic> diagnostics) {
    this.diagnostics = diagnostics;
  }

  public static ClassicalDiagnosticSet of(final List<ClassicalDiagnostic> diagnostics) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return EMPTY;
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Classical diagnostic must not be null.");
    }
    return new ClassicalDiagnosticSet(List.copyOf(diagnostics));
  }

  public List<ClassicalDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public boolean hasErrors() {
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (this.diagnostics.get(i).error()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasWarnings() {
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (this.diagnostics.get(i).severity() == ClassicalDiagnosticSeverity.WARNING) {
        return true;
      }
    }
    return false;
  }

  public boolean empty() {
    return this.diagnostics.isEmpty();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalDiagnosticSet)) {
      return false;
    }
    final ClassicalDiagnosticSet set = (ClassicalDiagnosticSet) other;
    return Objects.equals(this.diagnostics, set.diagnostics);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.diagnostics);
    return result;
  }
}