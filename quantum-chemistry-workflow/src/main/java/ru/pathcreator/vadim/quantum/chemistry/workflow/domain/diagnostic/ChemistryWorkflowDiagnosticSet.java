/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic;

import java.util.List;
import java.util.Objects;

/**
 * Immutable-набор workflow-диагностик.
 */
public final class ChemistryWorkflowDiagnosticSet {

  public static final ChemistryWorkflowDiagnosticSet EMPTY =
      new ChemistryWorkflowDiagnosticSet(List.of());

  private final List<ChemistryWorkflowDiagnostic> diagnostics;

  private ChemistryWorkflowDiagnosticSet(final List<ChemistryWorkflowDiagnostic> diagnostics) {
    this.diagnostics = diagnostics;
  }

  public static ChemistryWorkflowDiagnosticSet of(
      final List<ChemistryWorkflowDiagnostic> diagnostics) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return ChemistryWorkflowDiagnosticSet.EMPTY;
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i) == null) {
        throw new IllegalArgumentException("Chemistry workflow diagnostic must not be null.");
      }
    }
    return new ChemistryWorkflowDiagnosticSet(List.copyOf(diagnostics));
  }

  public List<ChemistryWorkflowDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public boolean empty() {
    return this.diagnostics.isEmpty();
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
      if (this.diagnostics.get(i).severity() == ChemistryWorkflowDiagnosticSeverity.WARNING) {
        return true;
      }
    }
    return false;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowDiagnosticSet)) {
      return false;
    }
    final ChemistryWorkflowDiagnosticSet set = (ChemistryWorkflowDiagnosticSet) other;
    return Objects.equals(this.diagnostics, set.diagnostics);
  }

  public int hashCode() {
    return Objects.hash(this.diagnostics);
  }
}
