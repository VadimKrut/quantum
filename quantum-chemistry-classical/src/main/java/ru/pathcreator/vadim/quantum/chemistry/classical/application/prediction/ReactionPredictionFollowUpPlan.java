/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Хранит структурированный план расчетов, которые уточняют прогноз реакции.
 */
public final class ReactionPredictionFollowUpPlan {

  private final List<ReactionPredictionFollowUpStep> steps;

  private ReactionPredictionFollowUpPlan(final List<ReactionPredictionFollowUpStep> steps) {
    this.steps = steps;
  }

  public static ReactionPredictionFollowUpPlan of(
      final List<ReactionPredictionFollowUpStep> steps) {
    if (steps == null) {
      throw new IllegalArgumentException("Reaction prediction follow-up steps must not be null.");
    }
    for (int i = 0; i < steps.size(); ++i) {
      if (steps.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Reaction prediction follow-up step must not be null.");
    }
    return new ReactionPredictionFollowUpPlan(List.copyOf(steps));
  }

  public List<ReactionPredictionFollowUpStep> steps() {
    return this.steps;
  }

  public int stepCount() {
    return this.steps.size();
  }

  public boolean hasRequiredSteps() {
    for (int i = 0; i < this.steps.size(); ++i) {
      if (this.steps.get(i).urgency() == ReactionPredictionFollowUpUrgency.REQUIRED) {
        return true;
      }
    }
    return false;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionFollowUpPlan)) {
      return false;
    }
    final ReactionPredictionFollowUpPlan plan = (ReactionPredictionFollowUpPlan) other;
    return Objects.equals(this.steps, plan.steps);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.steps);
    return result;
  }
}
