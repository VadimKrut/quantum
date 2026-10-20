/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.time.Instant;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Append-only laboratory record; a correction retains the original record. */
public record LaboratoryEntry(
    String id, Instant recordedAt, Kind kind, String text, String correctsId) {
  public enum Kind {
    OBSERVATION("Наблюдение"),
    RESULT("Результат / измерение"),
    ACTION("Действие"),
    CORRECTION("Исправление");
    private final String label;

    Kind(final String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public LaboratoryEntry {
    id = IdentifierValue.requireIdentifier(id, "Journal entry id");
    Objects.requireNonNull(recordedAt, "Entry timestamp");
    Objects.requireNonNull(kind, "Entry kind");
    text = TextValue.requireText(text, "Запись журнала");
    if (correctsId != null) {
      correctsId = IdentifierValue.requireIdentifier(correctsId, "Corrected entry");
    }
    if ((kind == Kind.CORRECTION) != (correctsId != null)) {
      throw new IllegalArgumentException("Исправление должно ссылаться на исходную запись.");
    }
  }
}
