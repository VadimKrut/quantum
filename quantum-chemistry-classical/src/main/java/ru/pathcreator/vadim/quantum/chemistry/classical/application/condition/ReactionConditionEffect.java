/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;

/**
 * Одно объяснимое влияние условий реакции: численный вклад, риск и тип evidence.
 */
public final class ReactionConditionEffect {

  private final ReactionConditionEffectKind kind;
  private final ReactionPredictionEvidenceKind evidenceKind;
  private final String description;
  private final double scoreAdjustment;
  private final boolean risk;

  private ReactionConditionEffect(
      final ReactionConditionEffectKind kind,
      final ReactionPredictionEvidenceKind evidenceKind,
      final String description,
      final double scoreAdjustment,
      final boolean risk) {
    this.kind = kind;
    this.evidenceKind = evidenceKind;
    this.description = description;
    this.scoreAdjustment = scoreAdjustment;
    this.risk = risk;
  }

  public static ReactionConditionEffect of(
      final ReactionConditionEffectKind kind,
      final ReactionPredictionEvidenceKind evidenceKind,
      final String description,
      final double scoreAdjustment,
      final boolean risk) {
    if (kind == null) {
      throw new IllegalArgumentException("Reaction condition effect kind must not be null.");
    }
    if (evidenceKind == null) {
      throw new IllegalArgumentException(
          "Reaction condition effect evidence kind must not be null.");
    }
    if (!Double.isFinite(scoreAdjustment)) {
      throw new IllegalArgumentException(
          "Reaction condition effect score adjustment must be finite.");
    }
    return new ReactionConditionEffect(
        kind,
        evidenceKind,
        TextValue.requireText(description, "Reaction condition effect description"),
        scoreAdjustment,
        risk);
  }

  public ReactionConditionEffectKind kind() {
    return this.kind;
  }

  public ReactionPredictionEvidenceKind evidenceKind() {
    return this.evidenceKind;
  }

  public String description() {
    return this.description;
  }

  public double scoreAdjustment() {
    return this.scoreAdjustment;
  }

  public boolean risk() {
    return this.risk;
  }

  public boolean supportive() {
    return this.scoreAdjustment > 0.0 && !this.risk;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionEffect)) {
      return false;
    }
    final ReactionConditionEffect effect = (ReactionConditionEffect) other;
    return this.kind == effect.kind
        && this.evidenceKind == effect.evidenceKind
        && Objects.equals(this.description, effect.description)
        && Double.compare(this.scoreAdjustment, effect.scoreAdjustment) == 0
        && this.risk == effect.risk;
  }

  public int hashCode() {
    return Objects.hash(
        this.kind, this.evidenceKind, this.description, this.scoreAdjustment, this.risk);
  }
}
