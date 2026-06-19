/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.kinetics;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;

public final class ReactionRateLaw {

  private final String id;
  private final RateConstant rateConstant;
  private final List<ReactionOrderTerm> orderTerms;

  private ReactionRateLaw(
      final String id, final RateConstant rateConstant, final List<ReactionOrderTerm> orderTerms) {
    this.id = id;
    this.rateConstant = rateConstant;
    this.orderTerms = orderTerms;
  }

  public static ReactionRateLaw of(
      final String id, final RateConstant rateConstant, final List<ReactionOrderTerm> orderTerms) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Reaction rate law id");
    if (rateConstant == null) {
      throw new IllegalArgumentException("Reaction rate constant must not be null.");
    }
    final List<ReactionOrderTerm> checkedTerms =
        List.copyOf(ReactionRateLaw.requireOrderTerms(orderTerms));
    final double totalOrder = ReactionRateLaw.totalOrder(checkedTerms);
    if (!rateConstant.unit().matchesOrder(totalOrder)) {
      throw new IllegalArgumentException("Rate constant unit does not match reaction order.");
    }
    return new ReactionRateLaw(checkedId, rateConstant, checkedTerms);
  }

  public String id() {
    return this.id;
  }

  public RateConstant rateConstant() {
    return this.rateConstant;
  }

  public List<ReactionOrderTerm> orderTerms() {
    return this.orderTerms;
  }

  public double totalOrder() {
    return ReactionRateLaw.totalOrder(this.orderTerms);
  }

  public void validateAgainstReaction(final Reaction reaction) {
    if (reaction == null) {
      throw new IllegalArgumentException("Reaction must not be null.");
    }
    for (int i = 0; i < this.orderTerms.size(); ++i) {
      if (ReactionRateLaw.reactionContainsMolecule(reaction, this.orderTerms.get(i).moleculeId()))
        continue;
      throw new IllegalArgumentException("Reaction order references molecule outside reaction.");
    }
  }

  public double rateMolePerLiterSecond(final List<ConcentrationPoint> concentrations) {
    final List<ConcentrationPoint> checkedConcentrations =
        ReactionRateLaw.requireConcentrations(concentrations);
    double rate = this.rateConstant.value();
    for (int i = 0; i < this.orderTerms.size(); ++i) {
      ReactionOrderTerm term = this.orderTerms.get(i);
      ConcentrationPoint point =
          ReactionRateLaw.concentrationByMoleculeId(checkedConcentrations, term.moleculeId());
      if (point == null) {
        throw new IllegalArgumentException("Concentration set does not cover reaction order term.");
      }
      rate *=
          Math.pow(
              KineticUnitConverter.concentrationMolePerLiter(point.concentration()), term.order());
    }
    return rate;
  }

  private static List<ReactionOrderTerm> requireOrderTerms(final List<ReactionOrderTerm> orderTerms) {
    if (orderTerms == null) {
      throw new IllegalArgumentException("Reaction order terms must not be null.");
    }
    for (int i = 0; i < orderTerms.size(); ++i) {
      ReactionOrderTerm term = orderTerms.get(i);
      if (term == null) {
        throw new IllegalArgumentException("Reaction order term must not be null.");
      }
      for (int j = i + 1; j < orderTerms.size(); ++j) {
        ReactionOrderTerm other = orderTerms.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Reaction order term must not be null.");
        }
        if (!term.moleculeId().equals(other.moleculeId())) continue;
        throw new IllegalArgumentException("Reaction order terms must use unique molecule ids.");
      }
    }
    return orderTerms;
  }

  private static List<ConcentrationPoint> requireConcentrations(
      final List<ConcentrationPoint> concentrations) {
    if (concentrations == null) {
      throw new IllegalArgumentException("Concentration set must not be null.");
    }
    for (int i = 0; i < concentrations.size(); ++i) {
      ConcentrationPoint point = concentrations.get(i);
      if (point == null) {
        throw new IllegalArgumentException("Concentration point must not be null.");
      }
      for (int j = i + 1; j < concentrations.size(); ++j) {
        ConcentrationPoint other = concentrations.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Concentration point must not be null.");
        }
        if (!point.moleculeId().equals(other.moleculeId())) continue;
        throw new IllegalArgumentException("Concentration points must use unique molecule ids.");
      }
    }
    return concentrations;
  }

  private static double totalOrder(final List<ReactionOrderTerm> orderTerms) {
    double sum = 0.0;
    for (int i = 0; i < orderTerms.size(); ++i) {
      sum += orderTerms.get(i).order();
    }
    return sum;
  }

  private static ConcentrationPoint concentrationByMoleculeId(
      final List<ConcentrationPoint> concentrations, final MoleculeId moleculeId) {
    for (int i = 0; i < concentrations.size(); ++i) {
      final ConcentrationPoint point = concentrations.get(i);
      if (!point.moleculeId().equals(moleculeId)) continue;
      return point;
    }
    return null;
  }

  private static boolean reactionContainsMolecule(
      final Reaction reaction,
      final MoleculeId id
  ) {
    return ReactionRateLaw.sideContainsMolecule(reaction.reactants(), id)
        || ReactionRateLaw.sideContainsMolecule(reaction.products(), id);
  }

  private static boolean sideContainsMolecule(
      final ReactionSide side,
      final MoleculeId id
  ) {
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      if (!participants.get(i).molecule().id().equals(id)) continue;
      return true;
    }
    return false;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionRateLaw)) {
      return false;
    }
    final ReactionRateLaw law = (ReactionRateLaw) other;
    return Objects.equals(this.id, law.id)
        && Objects.equals(this.rateConstant, law.rateConstant)
        && Objects.equals(this.orderTerms, law.orderTerms);
  }

  public int hashCode() {
    return Objects.hash(this.id, this.rateConstant, this.orderTerms);
  }
}