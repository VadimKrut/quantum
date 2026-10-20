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
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;

/**
 * Снимок условий реакции на конкретном шаге истории проекта.
 */
public final class ChemistryStorageConditionSnapshotRecord {

  private final String id;
  private final String reactionId;
  private final int historyIndex;
  private final String label;
  private final ReactionConditions conditions;
  private final String note;

  private ChemistryStorageConditionSnapshotRecord(
      final String id,
      final String reactionId,
      final int historyIndex,
      final String label,
      final ReactionConditions conditions,
      final String note) {
    this.id = id;
    this.reactionId = reactionId;
    this.historyIndex = historyIndex;
    this.label = label;
    this.conditions = conditions;
    this.note = note;
  }

  public static ChemistryStorageConditionSnapshotRecord of(
      final String id,
      final String reactionId,
      final int historyIndex,
      final String label,
      final ReactionConditions conditions,
      final String note) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException("Condition snapshot history index must be non-negative.");
    }
    if (conditions == null) {
      throw new IllegalArgumentException("Condition snapshot conditions must not be null.");
    }
    return new ChemistryStorageConditionSnapshotRecord(
        IdentifierValue.requireIdentifier(id, "Condition snapshot id"),
        IdentifierValue.requireIdentifier(reactionId, "Condition snapshot reaction id"),
        historyIndex,
        TextValue.requireText(label, "Condition snapshot label"),
        conditions,
        TextValue.optionalText(note));
  }

  public String id() {
    return this.id;
  }

  public String reactionId() {
    return this.reactionId;
  }

  public int historyIndex() {
    return this.historyIndex;
  }

  public String label() {
    return this.label;
  }

  public ReactionConditions conditions() {
    return this.conditions;
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
    if (!(other instanceof ChemistryStorageConditionSnapshotRecord)) {
      return false;
    }
    final ChemistryStorageConditionSnapshotRecord record =
        (ChemistryStorageConditionSnapshotRecord) other;
    return this.historyIndex == record.historyIndex
        && Objects.equals(this.id, record.id)
        && Objects.equals(this.reactionId, record.reactionId)
        && Objects.equals(this.label, record.label)
        && Objects.equals(this.conditions, record.conditions)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id, this.reactionId, this.historyIndex, this.label, this.conditions, this.note);
  }
}
