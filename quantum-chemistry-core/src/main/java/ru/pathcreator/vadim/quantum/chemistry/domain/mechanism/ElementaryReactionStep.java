/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.mechanism;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;

public final class ElementaryReactionStep {

  private final String id;
  private final Reaction reaction;
  private final Molecule transitionState;
  private final EnergyValue forwardBarrier;
  private final EnergyValue reverseBarrier;
  private final EnergyValue reactionEnergy;

  private ElementaryReactionStep(
      final String id,
      final Reaction reaction,
      final Molecule transitionState,
      final EnergyValue forwardBarrier,
      final EnergyValue reverseBarrier,
      final EnergyValue reactionEnergy) {
    this.id = id;
    this.reaction = reaction;
    this.transitionState = transitionState;
    this.forwardBarrier = forwardBarrier;
    this.reverseBarrier = reverseBarrier;
    this.reactionEnergy = reactionEnergy;
  }

  public static ElementaryReactionStep of(
      final String id,
      final Reaction reaction,
      final Molecule transitionState,
      final EnergyValue forwardBarrier,
      final EnergyValue reverseBarrier,
      final EnergyValue reactionEnergy) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Elementary reaction step id");
    if (reaction == null) {
      throw new IllegalArgumentException("Elementary reaction step reaction must not be null.");
    }
    if (!reaction.balance().balanced()) {
      throw new IllegalArgumentException("Elementary reaction step must be balanced.");
    }
    ElementaryReactionStep.requireBarrier(forwardBarrier, "Forward barrier");
    ElementaryReactionStep.requireBarrier(reverseBarrier, "Reverse barrier");
    return new ElementaryReactionStep(
        checkedId, reaction, transitionState, forwardBarrier, reverseBarrier, reactionEnergy);
  }

  public String id() {
    return this.id;
  }

  public Reaction reaction() {
    return this.reaction;
  }

  public Molecule transitionState() {
    return this.transitionState;
  }

  public boolean hasTransitionState() {
    return this.transitionState != null;
  }

  public EnergyValue forwardBarrier() {
    return this.forwardBarrier;
  }

  public boolean hasForwardBarrier() {
    return this.forwardBarrier != null;
  }

  public EnergyValue reverseBarrier() {
    return this.reverseBarrier;
  }

  public boolean hasReverseBarrier() {
    return this.reverseBarrier != null;
  }

  public EnergyValue reactionEnergy() {
    return this.reactionEnergy;
  }

  public boolean hasReactionEnergy() {
    return this.reactionEnergy != null;
  }

  private static void requireBarrier(
      final EnergyValue barrier,
      final String subjectName
  ) {
    if (barrier == null) {
      return;
    }
    if (barrier.value() < 0.0) {
      throw new IllegalArgumentException(subjectName + " must not be negative.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElementaryReactionStep)) {
      return false;
    }
    final ElementaryReactionStep step = (ElementaryReactionStep) other;
    return Objects.equals(this.id, step.id)
        && Objects.equals(this.reaction, step.reaction)
        && Objects.equals(this.transitionState, step.transitionState)
        && Objects.equals(this.forwardBarrier, step.forwardBarrier)
        && Objects.equals(this.reverseBarrier, step.reverseBarrier)
        && Objects.equals(this.reactionEnergy, step.reactionEnergy);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.reaction,
        this.transitionState,
        this.forwardBarrier,
        this.reverseBarrier,
        this.reactionEnergy);
  }
}