/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.prediction;

import java.util.List;
import java.util.Objects;

/** Полная расшифровка вероятностной оценки продукта до нормализации и после нее. */
public final class ReactionPredictionScoreBreakdown {

  private final double rawScore;
  private final double normalizedProbability;
  private final List<ReactionPredictionScoreComponent> components;

  private ReactionPredictionScoreBreakdown(
      final double rawScore,
      final double normalizedProbability,
      final List<ReactionPredictionScoreComponent> components) {
    this.rawScore = rawScore;
    this.normalizedProbability = normalizedProbability;
    this.components = components;
  }

  public static ReactionPredictionScoreBreakdown empty(final double normalizedProbability) {
    return ReactionPredictionScoreBreakdown.of(
        normalizedProbability, normalizedProbability, List.of());
  }

  public static ReactionPredictionScoreBreakdown of(
      final double rawScore,
      final double normalizedProbability,
      final List<ReactionPredictionScoreComponent> components) {
    if (!Double.isFinite(rawScore) || rawScore < 0.0) {
      throw new IllegalArgumentException(
          "Reaction score raw value must be finite and non-negative.");
    }
    if (!Double.isFinite(normalizedProbability)
        || normalizedProbability < 0.0
        || normalizedProbability > 1.0) {
      throw new IllegalArgumentException(
          "Reaction score normalized probability must be in [0, 1].");
    }
    return new ReactionPredictionScoreBreakdown(
        rawScore,
        normalizedProbability,
        ReactionPredictionScoreBreakdown.checkedComponents(components));
  }

  public double rawScore() {
    return this.rawScore;
  }

  public double normalizedProbability() {
    return this.normalizedProbability;
  }

  public List<ReactionPredictionScoreComponent> components() {
    return this.components;
  }

  public boolean hasComponents() {
    return !this.components.isEmpty();
  }

  public double positiveWeight() {
    double weight = 0.0;
    for (int i = 0; i < this.components.size(); ++i) {
      final ReactionPredictionScoreComponent component = this.components.get(i);
      if (component.positive()) {
        weight += component.weight();
      }
    }
    return weight;
  }

  public double negativeWeight() {
    double weight = 0.0;
    for (int i = 0; i < this.components.size(); ++i) {
      final ReactionPredictionScoreComponent component = this.components.get(i);
      if (component.negative()) {
        weight += component.weight();
      }
    }
    return weight;
  }

  public boolean hasComponent(final ReactionPredictionScoreComponentKind kind) {
    return this.componentCount(kind) > 0;
  }

  public int componentCount(final ReactionPredictionScoreComponentKind kind) {
    ReactionPredictionScoreBreakdown.requireComponentKind(kind);
    int count = 0;
    for (int i = 0; i < this.components.size(); ++i) {
      if (this.components.get(i).kind() == kind) {
        ++count;
      }
    }
    return count;
  }

  public double componentWeight(final ReactionPredictionScoreComponentKind kind) {
    ReactionPredictionScoreBreakdown.requireComponentKind(kind);
    double weight = 0.0;
    for (int i = 0; i < this.components.size(); ++i) {
      final ReactionPredictionScoreComponent component = this.components.get(i);
      if (component.kind() == kind) {
        weight += component.weight();
      }
    }
    return weight;
  }

  private static List<ReactionPredictionScoreComponent> checkedComponents(
      final List<ReactionPredictionScoreComponent> components) {
    if (components == null || components.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < components.size(); ++i) {
      if (components.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Reaction score component must not be null.");
    }
    return List.copyOf(components);
  }

  private static void requireComponentKind(final ReactionPredictionScoreComponentKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Reaction score component kind must not be null.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionScoreBreakdown)) {
      return false;
    }
    final ReactionPredictionScoreBreakdown breakdown = (ReactionPredictionScoreBreakdown) other;
    return Double.compare(this.rawScore, breakdown.rawScore) == 0
        && Double.compare(this.normalizedProbability, breakdown.normalizedProbability) == 0
        && Objects.equals(this.components, breakdown.components);
  }

  public int hashCode() {
    return Objects.hash(this.rawScore, this.normalizedProbability, this.components);
  }
}
