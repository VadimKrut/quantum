/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Сводка расчетных влияний условий реакции для scoring, diagnostics и объяснения.
 */
public final class ReactionConditionEffectProfile {

  public static final ReactionConditionEffectProfile EMPTY =
      new ReactionConditionEffectProfile(List.of(), 0.0, 0, 0);
  private final List<ReactionConditionEffect> effects;
  private final double scoreAdjustment;
  private final int riskCount;
  private final int supportCount;

  private ReactionConditionEffectProfile(
      final List<ReactionConditionEffect> effects,
      final double scoreAdjustment,
      final int riskCount,
      final int supportCount) {
    this.effects = effects;
    this.scoreAdjustment = scoreAdjustment;
    this.riskCount = riskCount;
    this.supportCount = supportCount;
  }

  public static ReactionConditionEffectProfile of(final List<ReactionConditionEffect> effects) {
    final List<ReactionConditionEffect> checkedEffects =
        ReactionConditionEffectProfile.checkedEffects(effects);
    if (checkedEffects.isEmpty()) {
      return EMPTY;
    }
    double score = 0.0;
    int risks = 0;
    int supports = 0;
    for (int i = 0; i < checkedEffects.size(); ++i) {
      final ReactionConditionEffect effect = checkedEffects.get(i);
      score += effect.scoreAdjustment();
      if (effect.risk()) {
        ++risks;
      }
      if (effect.supportive()) {
        ++supports;
      }
    }
    return new ReactionConditionEffectProfile(checkedEffects, score, risks, supports);
  }

  public List<ReactionConditionEffect> effects() {
    return this.effects;
  }

  public boolean empty() {
    return this.effects.isEmpty();
  }

  public double scoreAdjustment() {
    return this.scoreAdjustment;
  }

  public int riskCount() {
    return this.riskCount;
  }

  public int supportCount() {
    return this.supportCount;
  }

  public boolean hasEffect(final ReactionConditionEffectKind kind) {
    if (kind == null) {
      return false;
    }
    for (int i = 0; i < this.effects.size(); ++i) {
      if (this.effects.get(i).kind() == kind) {
        return true;
      }
    }
    return false;
  }

  public List<ReactionConditionEffect> riskEffects() {
    if (this.effects.isEmpty()) {
      return List.of();
    }
    final ArrayList<ReactionConditionEffect> result =
        new ArrayList<ReactionConditionEffect>(this.effects.size());
    for (int i = 0; i < this.effects.size(); ++i) {
      final ReactionConditionEffect effect = this.effects.get(i);
      if (effect.risk()) {
        result.add(effect);
      }
    }
    return List.copyOf(result);
  }

  private static List<ReactionConditionEffect> checkedEffects(
      final List<ReactionConditionEffect> effects) {
    if (effects == null || effects.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < effects.size(); ++i) {
      if (effects.get(i) == null) {
        throw new IllegalArgumentException("Reaction condition effect must not be null.");
      }
    }
    return List.copyOf(effects);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionEffectProfile)) {
      return false;
    }
    final ReactionConditionEffectProfile profile = (ReactionConditionEffectProfile) other;
    return Objects.equals(this.effects, profile.effects)
        && Double.compare(this.scoreAdjustment, profile.scoreAdjustment) == 0
        && this.riskCount == profile.riskCount
        && this.supportCount == profile.supportCount;
  }

  public int hashCode() {
    return Objects.hash(this.effects, this.scoreAdjustment, this.riskCount, this.supportCount);
  }
}
