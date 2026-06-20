/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation;

import java.util.List;

/**
 * Итог проверки drawing-документа.
 */
public final class DrawingValidationResult {

  private final List<DrawingDiagnostic> diagnostics;

  private DrawingValidationResult(final List<DrawingDiagnostic> diagnostics) {
    this.diagnostics = diagnostics;
  }

  public static DrawingValidationResult of(final List<DrawingDiagnostic> diagnostics) {
    return new DrawingValidationResult(diagnostics == null ? List.of() : List.copyOf(diagnostics));
  }

  public List<DrawingDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public boolean valid() {
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (!this.diagnostics.get(i).error()) {
        continue;
      }
      return false;
    }
    return true;
  }
}