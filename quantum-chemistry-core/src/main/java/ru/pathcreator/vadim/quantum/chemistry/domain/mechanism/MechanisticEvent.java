/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.mechanism;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Детальная стадия электронного или атомного движения внутри elementary step.
 */
public final class MechanisticEvent {

  private final String id;
  private final int orderIndex;
  private final MechanisticEventKind kind;
  private final String description;
  private final List<String> participantReferences;
  private final double probabilityWeight;

  private MechanisticEvent(
      final String id,
      final int orderIndex,
      final MechanisticEventKind kind,
      final String description,
      final List<String> participantReferences,
      final double probabilityWeight) {
    this.id = id;
    this.orderIndex = orderIndex;
    this.kind = kind;
    this.description = description;
    this.participantReferences = participantReferences;
    this.probabilityWeight = probabilityWeight;
  }

  public static MechanisticEvent of(
      final String id,
      final int orderIndex,
      final MechanisticEventKind kind,
      final String description,
      final List<String> participantReferences,
      final double probabilityWeight) {
    if (orderIndex < 0) {
      throw new IllegalArgumentException("Mechanistic event order index must be non-negative.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Mechanistic event kind must not be null.");
    }
    if (!Double.isFinite(probabilityWeight) || probabilityWeight < 0.0 || probabilityWeight > 1.0) {
      throw new IllegalArgumentException("Mechanistic event probability weight must be in [0, 1].");
    }
    return new MechanisticEvent(
        IdentifierValue.requireIdentifier(id, "Mechanistic event id"),
        orderIndex,
        kind,
        TextValue.requireText(description, "Mechanistic event description"),
        MechanisticEvent.checkedReferences(participantReferences),
        probabilityWeight);
  }

  public String id() {
    return this.id;
  }

  public int orderIndex() {
    return this.orderIndex;
  }

  public MechanisticEventKind kind() {
    return this.kind;
  }

  public String description() {
    return this.description;
  }

  public List<String> participantReferences() {
    return this.participantReferences;
  }

  public boolean hasParticipantReferences() {
    return !this.participantReferences.isEmpty();
  }

  public double probabilityWeight() {
    return this.probabilityWeight;
  }

  private static List<String> checkedReferences(final List<String> references) {
    if (references == null || references.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[references.size()];
    for (int i = 0; i < references.size(); ++i) {
      checked[i] =
          IdentifierValue.requireIdentifier(
              references.get(i), "Mechanistic event participant reference");
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MechanisticEvent)) {
      return false;
    }
    final MechanisticEvent event = (MechanisticEvent) other;
    return this.orderIndex == event.orderIndex
        && Double.compare(this.probabilityWeight, event.probabilityWeight) == 0
        && Objects.equals(this.id, event.id)
        && this.kind == event.kind
        && Objects.equals(this.description, event.description)
        && Objects.equals(this.participantReferences, event.participantReferences);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.orderIndex,
        this.kind,
        this.description,
        this.participantReferences,
        this.probabilityWeight);
  }
}
