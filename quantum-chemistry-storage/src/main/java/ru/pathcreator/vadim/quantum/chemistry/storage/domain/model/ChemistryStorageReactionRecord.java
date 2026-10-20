/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;

/**
 * Запись истории: план, наблюдение или расчет для конкретной реакции.
 */
public final class ChemistryStorageReactionRecord {

  private final String id;
  private final Reaction reaction;
  private final ChemistryStorageReactionRecordStatus status;
  private final int historyIndex;
  private final Double observedYieldPercent;
  private final String note;

  private ChemistryStorageReactionRecord(
      final String id,
      final Reaction reaction,
      final ChemistryStorageReactionRecordStatus status,
      final int historyIndex,
      final Double observedYieldPercent,
      final String note) {
    this.id = id;
    this.reaction = reaction;
    this.status = status;
    this.historyIndex = historyIndex;
    this.observedYieldPercent = observedYieldPercent;
    this.note = note;
  }

  public static ChemistryStorageReactionRecord of(
      final String id,
      final Reaction reaction,
      final ChemistryStorageReactionRecordStatus status,
      final int historyIndex,
      final Double observedYieldPercent,
      final String note) {
    if (reaction == null) {
      throw new IllegalArgumentException("Reaction record reaction must not be null.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Reaction record status must not be null.");
    }
    if (historyIndex < 0) {
      throw new IllegalArgumentException("Reaction record history index must be non-negative.");
    }
    if (observedYieldPercent != null
        && (!Double.isFinite(observedYieldPercent)
            || observedYieldPercent < 0.0
            || observedYieldPercent > 100.0)) {
      throw new IllegalArgumentException(
          "Reaction record observed yield percent must be in [0, 100].");
    }
    return new ChemistryStorageReactionRecord(
        IdentifierValue.requireIdentifier(id, "Reaction record id"),
        reaction,
        status,
        historyIndex,
        observedYieldPercent,
        TextValue.optionalText(note));
  }

  public String id() {
    return this.id;
  }

  public Reaction reaction() {
    return this.reaction;
  }

  public ChemistryStorageReactionRecordStatus status() {
    return this.status;
  }

  public int historyIndex() {
    return this.historyIndex;
  }

  public Double observedYieldPercent() {
    return this.observedYieldPercent;
  }

  public boolean hasObservedYieldPercent() {
    return this.observedYieldPercent != null;
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageReactionRecord)) {
      return false;
    }
    final ChemistryStorageReactionRecord record = (ChemistryStorageReactionRecord) other;
    return this.historyIndex == record.historyIndex
        && Objects.equals(this.id, record.id)
        && Objects.equals(this.reaction, record.reaction)
        && this.status == record.status
        && Objects.equals(this.observedYieldPercent, record.observedYieldPercent)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.reaction,
        this.status,
        this.historyIndex,
        this.observedYieldPercent,
        this.note);
  }
}
