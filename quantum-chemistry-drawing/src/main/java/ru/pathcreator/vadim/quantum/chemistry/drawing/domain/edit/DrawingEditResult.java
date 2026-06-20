/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.edit;

import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.ChemistryDrawingDocument;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation.DrawingValidationResult;

/**
 * Результат действия редактора: новый документ и проверка его согласованности.
 */
public final class DrawingEditResult {

  private final ChemistryDrawingDocument document;
  private final DrawingValidationResult validationResult;

  private DrawingEditResult(
      final ChemistryDrawingDocument document,
      final DrawingValidationResult validationResult
  ) {
    this.document = document;
    this.validationResult = validationResult;
  }

  public static DrawingEditResult of(
      final ChemistryDrawingDocument document,
      final DrawingValidationResult validationResult
  ) {
    if (document == null) {
      throw new IllegalArgumentException("Drawing edit result document must not be null.");
    }
    if (validationResult == null) {
      throw new IllegalArgumentException("Drawing edit result validation must not be null.");
    }
    return new DrawingEditResult(
        document,
        validationResult
    );
  }

  public ChemistryDrawingDocument document() {
    return this.document;
  }

  public DrawingValidationResult validationResult() {
    return this.validationResult;
  }
}