/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.mechanism;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;

/**
 * Сбалансированная elementary step с барьерами, transition state и детальными событиями.
 */
public final class ElementaryReactionStep {

  private final String id;
  private final Reaction reaction;
  private final Molecule transitionState;
  private final EnergyValue forwardBarrier;
  private final EnergyValue reverseBarrier;
  private final EnergyValue reactionEnergy;
  private final List<MechanisticEvent> events;

  private ElementaryReactionStep(
      final String id,
      final Reaction reaction,
      final Molecule transitionState,
      final EnergyValue forwardBarrier,
      final EnergyValue reverseBarrier,
      final EnergyValue reactionEnergy,
      final List<MechanisticEvent> events) {
    this.id = id;
    this.reaction = reaction;
    this.transitionState = transitionState;
    this.forwardBarrier = forwardBarrier;
    this.reverseBarrier = reverseBarrier;
    this.reactionEnergy = reactionEnergy;
    this.events = events;
  }

  public static ElementaryReactionStep of(
      final String id,
      final Reaction reaction,
      final Molecule transitionState,
      final EnergyValue forwardBarrier,
      final EnergyValue reverseBarrier,
      final EnergyValue reactionEnergy) {
    return ElementaryReactionStep.of(
        id, reaction, transitionState, forwardBarrier, reverseBarrier, reactionEnergy, List.of());
  }

  public static ElementaryReactionStep of(
      final String id,
      final Reaction reaction,
      final Molecule transitionState,
      final EnergyValue forwardBarrier,
      final EnergyValue reverseBarrier,
      final EnergyValue reactionEnergy,
      final List<MechanisticEvent> events) {
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
        checkedId,
        reaction,
        transitionState,
        forwardBarrier,
        reverseBarrier,
        reactionEnergy,
        ElementaryReactionStep.requireEvents(events));
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

  public List<MechanisticEvent> events() {
    return this.events;
  }

  public boolean hasEvents() {
    return !this.events.isEmpty();
  }

  private static void requireBarrier(final EnergyValue barrier, final String subjectName) {
    if (barrier == null) {
      return;
    }
    if (barrier.value() < 0.0) {
      throw new IllegalArgumentException(subjectName + " must not be negative.");
    }
  }

  private static List<MechanisticEvent> requireEvents(final List<MechanisticEvent> events) {
    if (events == null || events.isEmpty()) {
      return List.of();
    }
    final String[] ids = new String[events.size()];
    int previousOrder = -1;
    for (int i = 0; i < events.size(); ++i) {
      final MechanisticEvent event = events.get(i);
      if (event == null) {
        throw new IllegalArgumentException("Mechanistic event must not be null.");
      }
      if (event.orderIndex() <= previousOrder) {
        throw new IllegalArgumentException("Mechanistic event order must strictly increase.");
      }
      previousOrder = event.orderIndex();
      ids[i] = event.id();
    }
    final String[] sortedIds = ids.clone();
    java.util.Arrays.sort(sortedIds);
    for (int i = 1; i < sortedIds.length; ++i) {
      if (!sortedIds[i - 1].equals(sortedIds[i])) {
        continue;
      }
      throw new IllegalArgumentException("Mechanistic event ids must be unique.");
    }
    return List.copyOf(events);
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
        && Objects.equals(this.reactionEnergy, step.reactionEnergy)
        && Objects.equals(this.events, step.events);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.reaction,
        this.transitionState,
        this.forwardBarrier,
        this.reverseBarrier,
        this.reactionEnergy,
        this.events);
  }
}
