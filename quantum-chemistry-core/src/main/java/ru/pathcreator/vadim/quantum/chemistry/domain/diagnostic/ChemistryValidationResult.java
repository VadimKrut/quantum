/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic;

import java.util.List;

public final class ChemistryValidationResult {

  private static final ChemistryValidationResult EMPTY = new ChemistryValidationResult(List.of());
  private final List<ChemistryDiagnostic> diagnostics;

  private ChemistryValidationResult(final List<ChemistryDiagnostic> diagnostics) {
    this.diagnostics = diagnostics;
  }

  public static ChemistryValidationResult of(final List<ChemistryDiagnostic> diagnostics) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return EMPTY;
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i) != null) continue;
      throw new IllegalArgumentException("Chemistry diagnostic must not be null.");
    }
    return new ChemistryValidationResult(List.copyOf(diagnostics));
  }

  public List<ChemistryDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public boolean valid() {
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (this.diagnostics.get(i).severity() != ChemistryDiagnosticSeverity.ERROR) continue;
      return false;
    }
    return true;
  }
}