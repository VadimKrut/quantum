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
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.thermodynamics.ThermodynamicUnitConverter;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;

/** Описывает согласованную цепочку elementary steps для общей сбалансированной реакции. */
public final class ReactionMechanism {

  private final String id;
  private final Reaction overallReaction;
  private final List<ElementaryReactionStep> steps;
  private final List<MechanismEnergyPoint> energyProfile;

  private ReactionMechanism(
      final String id,
      final Reaction overallReaction,
      final List<ElementaryReactionStep> steps,
      final List<MechanismEnergyPoint> energyProfile) {
    this.id = id;
    this.overallReaction = overallReaction;
    this.steps = steps;
    this.energyProfile = energyProfile;
  }

  public static ReactionMechanism of(
      final String id,
      final Reaction overallReaction,
      final List<ElementaryReactionStep> steps,
      final List<MechanismEnergyPoint> energyProfile) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Reaction mechanism id");
    if (overallReaction == null) {
      throw new IllegalArgumentException("Reaction mechanism overall reaction must not be null.");
    }
    if (!overallReaction.balance().balanced()) {
      throw new IllegalArgumentException("Reaction mechanism overall reaction must be balanced.");
    }
    final List<ElementaryReactionStep> checkedSteps =
        List.copyOf(ReactionMechanism.requireSteps(overallReaction, steps));
    final List<MechanismEnergyPoint> checkedEnergyProfile =
        List.copyOf(ReactionMechanism.requireEnergyProfile(energyProfile));
    return new ReactionMechanism(checkedId, overallReaction, checkedSteps, checkedEnergyProfile);
  }

  public String id() {
    return this.id;
  }

  public Reaction overallReaction() {
    return this.overallReaction;
  }

  public List<ElementaryReactionStep> steps() {
    return this.steps;
  }

  public List<MechanismEnergyPoint> energyProfile() {
    return this.energyProfile;
  }

  public boolean hasEnergyProfile() {
    return !energyProfile.isEmpty();
  }

  public int transitionStateCount() {
    int count = 0;
    for (int i = 0; i < energyProfile.size(); ++i) {
      if (!energyProfile.get(i).transitionState()) {
        continue;
      }
      ++count;
    }
    return count;
  }

  public EnergyValue highestRelativeEnergy() {
    if (energyProfile.isEmpty()) {
      throw new IllegalStateException("Reaction mechanism has no energy profile.");
    }
    MechanismEnergyPoint highest = energyProfile.get(0);
    double highestKiloJoulePerMole =
        ThermodynamicUnitConverter.energyKiloJoulePerMole(highest.relativeEnergy());
    for (int i = 1; i < energyProfile.size(); ++i) {
      final MechanismEnergyPoint point = energyProfile.get(i);
      final double value =
          ThermodynamicUnitConverter.energyKiloJoulePerMole(point.relativeEnergy());
      if (value <= highestKiloJoulePerMole) {
        continue;
      }
      highest = point;
      highestKiloJoulePerMole = value;
    }
    return highest.relativeEnergy();
  }

  private static List<ElementaryReactionStep> requireSteps(
      final Reaction overallReaction, final List<ElementaryReactionStep> steps) {
    if (steps == null || steps.isEmpty()) {
      throw new IllegalArgumentException("Reaction mechanism steps must not be empty.");
    }
    final String[] stepIds = new String[steps.size()];
    for (int i = 0; i < steps.size(); ++i) {
      final ElementaryReactionStep step = steps.get(i);
      if (step == null) {
        throw new IllegalArgumentException("Elementary reaction step must not be null.");
      }
      stepIds[i] = step.id();
    }
    ReactionMechanism.requireUniqueSortedIds(stepIds, "Reaction mechanism step ids");
    if (!steps.get(0).reaction().reactants().sameParticipantsAs(overallReaction.reactants())) {
      throw new IllegalArgumentException(
          "Reaction mechanism first step must start from overall reactants.");
    }
    if (!steps
        .get(steps.size() - 1)
        .reaction()
        .products()
        .sameParticipantsAs(overallReaction.products())) {
      throw new IllegalArgumentException(
          "Reaction mechanism last step must end at overall products.");
    }
    for (int i = 0; i < steps.size() - 1; ++i) {
      final ReactionSide products = steps.get(i).reaction().products();
      final ReactionSide reactants = steps.get(i + 1).reaction().reactants();
      if (products.sameParticipantsAs(reactants)) {
        continue;
      }
      throw new IllegalArgumentException("Reaction mechanism steps are not connected.");
    }
    return steps;
  }

  private static List<MechanismEnergyPoint> requireEnergyProfile(
      final List<MechanismEnergyPoint> energyProfile) {
    if (energyProfile == null || energyProfile.isEmpty()) {
      return List.of();
    }
    boolean hasReactantComplex = false;
    boolean hasProductComplex = false;
    double previousCoordinate = Double.NEGATIVE_INFINITY;
    final String[] pointIds = new String[energyProfile.size()];
    for (int i = 0; i < energyProfile.size(); ++i) {
      final MechanismEnergyPoint point = energyProfile.get(i);
      if (point == null) {
        throw new IllegalArgumentException("Mechanism energy point must not be null.");
      }
      if (point.coordinate().value() <= previousCoordinate) {
        throw new IllegalArgumentException("Mechanism energy profile coordinates must increase.");
      }
      previousCoordinate = point.coordinate().value();
      if (point.kind() == MechanismPointKind.REACTANT_COMPLEX) {
        hasReactantComplex = true;
      }
      if (point.kind() == MechanismPointKind.PRODUCT_COMPLEX) {
        hasProductComplex = true;
      }
      pointIds[i] = point.id();
    }
    ReactionMechanism.requireUniqueSortedIds(pointIds, "Mechanism energy point ids");
    if (!hasReactantComplex || !hasProductComplex) {
      throw new IllegalArgumentException(
          "Mechanism energy profile must contain reactant and product complexes.");
    }
    return energyProfile;
  }

  private static void requireUniqueSortedIds(
      final String[] ids,
      final String subjectName
  ) {
    final String[] sortedIds = ids.clone();
    java.util.Arrays.sort(sortedIds);
    for (int i = 1; i < sortedIds.length; ++i) {
      if (!sortedIds[i - 1].equals(sortedIds[i])) {
        continue;
      }
      throw new IllegalArgumentException(subjectName + " must be unique.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionMechanism)) {
      return false;
    }
    final ReactionMechanism mechanism = (ReactionMechanism) other;
    return Objects.equals(id, mechanism.id)
        && Objects.equals(overallReaction, mechanism.overallReaction)
        && Objects.equals(steps, mechanism.steps)
        && Objects.equals(energyProfile, mechanism.energyProfile);
  }

  public int hashCode() {
    return Objects.hash(id, overallReaction, steps, energyProfile);
  }
}