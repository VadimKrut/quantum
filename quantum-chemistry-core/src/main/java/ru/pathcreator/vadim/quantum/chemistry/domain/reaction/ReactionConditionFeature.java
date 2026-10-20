/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Структурированная особенность условий, например инертная атмосфера или фотохимия. */
public final class ReactionConditionFeature {

  private final ReactionConditionFeatureKind kind;
  private final String value;
  private final String note;

  private ReactionConditionFeature(
      final ReactionConditionFeatureKind kind, final String value, final String note) {
    this.kind = kind;
    this.value = value;
    this.note = note;
  }

  public static ReactionConditionFeature of(
      final ReactionConditionFeatureKind kind, final String value, final String note) {
    if (kind == null) {
      throw new IllegalArgumentException("Reaction condition feature kind must not be null.");
    }
    final String checkedValue = TextValue.optionalText(value);
    final String checkedNote = TextValue.optionalText(note);
    if (kind == ReactionConditionFeatureKind.CUSTOM && checkedValue == null) {
      throw new IllegalArgumentException("Custom reaction condition feature must have value.");
    }
    return new ReactionConditionFeature(kind, checkedValue, checkedNote);
  }

  public static ReactionConditionFeature named(final ReactionConditionFeatureKind kind) {
    return ReactionConditionFeature.of(kind, null, null);
  }

  public ReactionConditionFeatureKind kind() {
    return this.kind;
  }

  public String value() {
    return this.value;
  }

  public boolean hasValue() {
    return this.value != null;
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
    if (!(other instanceof ReactionConditionFeature)) {
      return false;
    }
    final ReactionConditionFeature feature = (ReactionConditionFeature) other;
    return this.kind == feature.kind
        && Objects.equals(this.value, feature.value)
        && Objects.equals(this.note, feature.note);
  }

  public int hashCode() {
    return Objects.hash(this.kind, this.value, this.note);
  }
}
