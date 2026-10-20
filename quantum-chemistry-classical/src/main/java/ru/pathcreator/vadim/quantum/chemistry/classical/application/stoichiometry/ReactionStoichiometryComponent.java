/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry;

import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;

/** Компонент рецепта, сопоставленный с молекулярным участником реакции и пересчитанный в моли. */
public final class ReactionStoichiometryComponent {

  private final ReactionConditionComponent component;
  private final ReactionParticipant participant;
  private final double moleAmount;
  private final double equivalentRatio;

  private ReactionStoichiometryComponent(
      final ReactionConditionComponent component,
      final ReactionParticipant participant,
      final double moleAmount,
      final double equivalentRatio) {
    this.component = component;
    this.participant = participant;
    this.moleAmount = moleAmount;
    this.equivalentRatio = equivalentRatio;
  }

  public static ReactionStoichiometryComponent of(
      final ReactionConditionComponent component,
      final ReactionParticipant participant,
      final double moleAmount,
      final double equivalentRatio) {
    if (component == null) {
      throw new IllegalArgumentException("Stoichiometry component must not be null.");
    }
    if (participant == null) {
      throw new IllegalArgumentException("Stoichiometry participant must not be null.");
    }
    if (!Double.isFinite(moleAmount) || moleAmount <= 0.0) {
      throw new IllegalArgumentException("Stoichiometry mole amount must be positive and finite.");
    }
    if (!Double.isFinite(equivalentRatio) || equivalentRatio <= 0.0) {
      throw new IllegalArgumentException(
          "Stoichiometry equivalent ratio must be positive and finite.");
    }
    return new ReactionStoichiometryComponent(component, participant, moleAmount, equivalentRatio);
  }

  public ReactionConditionComponent component() {
    return this.component;
  }

  public ReactionParticipant participant() {
    return this.participant;
  }

  public double moleAmount() {
    return this.moleAmount;
  }

  public double equivalentRatio() {
    return this.equivalentRatio;
  }
}
