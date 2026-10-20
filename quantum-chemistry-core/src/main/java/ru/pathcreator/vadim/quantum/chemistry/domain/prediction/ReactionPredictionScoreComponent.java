/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Один численный и текстовый вклад в итоговую оценку кандидата продукта. */
public final class ReactionPredictionScoreComponent {

  private final ReactionPredictionScoreComponentKind kind;
  private final String description;
  private final double weight;

  private ReactionPredictionScoreComponent(
      final ReactionPredictionScoreComponentKind kind,
      final String description,
      final double weight) {
    this.kind = kind;
    this.description = description;
    this.weight = weight;
  }

  public static ReactionPredictionScoreComponent of(
      final ReactionPredictionScoreComponentKind kind,
      final String description,
      final double weight) {
    if (kind == null) {
      throw new IllegalArgumentException("Reaction score component kind must not be null.");
    }
    if (!Double.isFinite(weight) || weight < -10.0 || weight > 10.0) {
      throw new IllegalArgumentException("Reaction score component weight must be in [-10, 10].");
    }
    return new ReactionPredictionScoreComponent(
        kind, TextValue.requireText(description, "Reaction score component description"), weight);
  }

  public ReactionPredictionScoreComponentKind kind() {
    return this.kind;
  }

  public String description() {
    return this.description;
  }

  public double weight() {
    return this.weight;
  }

  public boolean positive() {
    return this.weight > 0.0;
  }

  public boolean negative() {
    return this.weight < 0.0;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionScoreComponent)) {
      return false;
    }
    final ReactionPredictionScoreComponent component = (ReactionPredictionScoreComponent) other;
    return this.kind == component.kind
        && Objects.equals(this.description, component.description)
        && Double.compare(this.weight, component.weight) == 0;
  }

  public int hashCode() {
    return Objects.hash(this.kind, this.description, this.weight);
  }
}
