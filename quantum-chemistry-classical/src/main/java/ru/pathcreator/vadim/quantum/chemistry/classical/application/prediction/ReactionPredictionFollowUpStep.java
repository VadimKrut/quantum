/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Описывает один следующий расчет, который нужен для проверки или усиления прогноза реакции.
 */
public final class ReactionPredictionFollowUpStep {

  private final int order;
  private final ClassicalCalculationKind calculationKind;
  private final ReactionPredictionFollowUpUrgency urgency;
  private final String reason;
  private final String expectedDecision;

  private ReactionPredictionFollowUpStep(
      final int order,
      final ClassicalCalculationKind calculationKind,
      final ReactionPredictionFollowUpUrgency urgency,
      final String reason,
      final String expectedDecision) {
    this.order = order;
    this.calculationKind = calculationKind;
    this.urgency = urgency;
    this.reason = reason;
    this.expectedDecision = expectedDecision;
  }

  public static ReactionPredictionFollowUpStep of(
      final int order,
      final ClassicalCalculationKind calculationKind,
      final ReactionPredictionFollowUpUrgency urgency,
      final String reason,
      final String expectedDecision) {
    if (order <= 0) {
      throw new IllegalArgumentException(
          "Reaction prediction follow-up step order must be positive.");
    }
    if (calculationKind == null) {
      throw new IllegalArgumentException(
          "Reaction prediction follow-up calculation kind must not be null.");
    }
    if (urgency == null) {
      throw new IllegalArgumentException("Reaction prediction follow-up urgency must not be null.");
    }
    return new ReactionPredictionFollowUpStep(
        order,
        calculationKind,
        urgency,
        TextValue.requireText(reason, "Reaction prediction follow-up reason"),
        TextValue.requireText(expectedDecision, "Reaction prediction follow-up expected decision"));
  }

  public int order() {
    return this.order;
  }

  public ClassicalCalculationKind calculationKind() {
    return this.calculationKind;
  }

  public ReactionPredictionFollowUpUrgency urgency() {
    return this.urgency;
  }

  public String reason() {
    return this.reason;
  }

  public String expectedDecision() {
    return this.expectedDecision;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionPredictionFollowUpStep)) {
      return false;
    }
    final ReactionPredictionFollowUpStep step = (ReactionPredictionFollowUpStep) other;
    return this.order == step.order
        && this.calculationKind == step.calculationKind
        && this.urgency == step.urgency
        && Objects.equals(this.reason, step.reason)
        && Objects.equals(this.expectedDecision, step.expectedDecision);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.order);
    result = ChemistryHash.include(result, this.calculationKind);
    result = ChemistryHash.include(result, this.urgency);
    result = ChemistryHash.include(result, this.reason);
    result = ChemistryHash.include(result, this.expectedDecision);
    return result;
  }
}
