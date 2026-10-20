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

/** Весовой аргумент прогноза, пригодный для UI-объяснения и аудита результата. */
public final class ReactionPredictionEvidence {

  private final ReactionPredictionEvidenceKind kind;
  private final String description;
  private final double weight;

  private ReactionPredictionEvidence(
      final ReactionPredictionEvidenceKind kind, final String description, final double weight) {
    this.kind = kind;
    this.description = description;
    this.weight = weight;
  }

  public static ReactionPredictionEvidence of(
      final ReactionPredictionEvidenceKind kind, final String description, final double weight) {
    if (kind == null) {
      throw new IllegalArgumentException("Reaction prediction evidence kind must not be null.");
    }
    if (!Double.isFinite(weight) || weight < -1.0 || weight > 1.0) {
      throw new IllegalArgumentException("Reaction prediction evidence weight must be in [-1, 1].");
    }
    return new ReactionPredictionEvidence(
        kind,
        TextValue.requireText(description, "Reaction prediction evidence description"),
        weight);
  }

  public ReactionPredictionEvidenceKind kind() {
    return this.kind;
  }

  public String description() {
    return this.description;
  }

  public double weight() {
    return this.weight;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionEvidence)) {
      return false;
    }
    final ReactionPredictionEvidence evidence = (ReactionPredictionEvidence) other;
    return this.kind == evidence.kind
        && Objects.equals(this.description, evidence.description)
        && Double.compare(this.weight, evidence.weight) == 0;
  }

  public int hashCode() {
    return Objects.hash(this.kind, this.description, this.weight);
  }
}
