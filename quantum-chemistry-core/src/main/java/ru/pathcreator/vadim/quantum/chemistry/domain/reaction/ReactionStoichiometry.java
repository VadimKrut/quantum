/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ChemicalElement;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;

/** Стехиометрическая сводка реакции: баланс элементов, заряда, массы и mass number. */
public final class ReactionStoichiometry {

  private final ReactionSideSummary reactants;
  private final ReactionSideSummary products;
  private final List<ReactionElementDelta> elementDeltas;
  private final long chargeDelta;
  private final double averageMassDelta;
  private final long nominalMassNumberDelta;

  private ReactionStoichiometry(
      final ReactionSideSummary reactants,
      final ReactionSideSummary products,
      final List<ReactionElementDelta> elementDeltas) {
    this.reactants = reactants;
    this.products = products;
    this.elementDeltas = List.copyOf(elementDeltas);
    chargeDelta = Math.subtractExact(products.charge(), reactants.charge());
    averageMassDelta = products.averageMass() - reactants.averageMass();
    nominalMassNumberDelta =
        Math.subtractExact(products.nominalMassNumber(), reactants.nominalMassNumber());
  }

  public static ReactionStoichiometry of(final Reaction reaction) {
    if (reaction == null) {
      throw new IllegalArgumentException("Reaction stoichiometry reaction must not be null.");
    }
    final ReactionSideSummary reactants = ReactionSideSummary.of(reaction.reactants());
    final ReactionSideSummary products = ReactionSideSummary.of(reaction.products());
    return new ReactionStoichiometry(
        reactants, products, ReactionStoichiometry.elementDeltas(reactants, products));
  }

  public ReactionSideSummary reactants() {
    return reactants;
  }

  public ReactionSideSummary products() {
    return products;
  }

  public List<ReactionElementDelta> elementDeltas() {
    return elementDeltas;
  }

  public long chargeDelta() {
    return chargeDelta;
  }

  public double averageMassDelta() {
    return averageMassDelta;
  }

  public long nominalMassNumberDelta() {
    return nominalMassNumberDelta;
  }

  public boolean atomBalanced() {
    for (int i = 0; i < elementDeltas.size(); ++i) {
      if (elementDeltas.get(i).balanced()) {
        continue;
      }
      return false;
    }
    return true;
  }

  public boolean chargeBalanced() {
    return chargeDelta == 0L;
  }

  public boolean massNumberBalanced() {
    return nominalMassNumberDelta == 0L;
  }

  public boolean balanced() {
    return atomBalanced() && chargeBalanced();
  }

  private static List<ReactionElementDelta> elementDeltas(
      final ReactionSideSummary reactants, final ReactionSideSummary products) {
    final ArrayList<ReactionElementDelta> result =
        new ArrayList<ReactionElementDelta>(
            Math.max(reactants.elementKindCount(), products.elementKindCount()));
    for (int atomicNumber = PeriodicTable.MIN_ATOMIC_NUMBER;
        atomicNumber <= PeriodicTable.MAX_ATOMIC_NUMBER;
        ++atomicNumber) {
      final long reactantCount = reactants.elementCountByAtomicNumber(atomicNumber);
      final long productCount = products.elementCountByAtomicNumber(atomicNumber);
      if (reactantCount == 0L && productCount == 0L) {
        continue;
      }
      final ChemicalElement element = PeriodicTable.requireAtomicNumber(atomicNumber);
      result.add(ReactionElementDelta.of(element.symbol(), reactantCount, productCount));
    }
    return result;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionStoichiometry)) {
      return false;
    }
    final ReactionStoichiometry stoichiometry = (ReactionStoichiometry) other;
    return chargeDelta == stoichiometry.chargeDelta
        && Double.compare(averageMassDelta, stoichiometry.averageMassDelta) == 0
        && nominalMassNumberDelta == stoichiometry.nominalMassNumberDelta
        && Objects.equals(reactants, stoichiometry.reactants)
        && Objects.equals(products, stoichiometry.products)
        && Objects.equals(elementDeltas, stoichiometry.elementDeltas);
  }

  public int hashCode() {
    return Objects.hash(
        reactants, products, elementDeltas, chargeDelta, averageMassDelta, nominalMassNumberDelta);
  }
}