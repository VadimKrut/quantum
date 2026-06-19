/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ChemicalElement;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormulaTerm;

/** Сводка стороны реакции с быстрым primitive-доступом к количествам элементов. */
public final class ReactionSideSummary {

  private final int participantCount;
  private final long totalCoefficient;
  private final long atomCount;
  private final long[] elementCountsByAtomicNumber;
  private final int elementKindCount;
  private final Map<ElementSymbol, Long> elementCounts;
  private final long charge;
  private final double averageMass;
  private final long nominalMassNumber;

  private ReactionSideSummary(
      final int participantCount,
      final long totalCoefficient,
      final long atomCount,
      final long[] elementCountsByAtomicNumber,
      final int elementKindCount,
      final long charge,
      final double averageMass,
      final long nominalMassNumber) {
    this.participantCount = participantCount;
    this.totalCoefficient = totalCoefficient;
    this.atomCount = atomCount;
    this.elementCountsByAtomicNumber = elementCountsByAtomicNumber;
    this.elementKindCount = elementKindCount;
    this.elementCounts = ReactionSideSummary.toElementCountMap(elementCountsByAtomicNumber);
    this.charge = charge;
    this.averageMass = averageMass;
    this.nominalMassNumber = nominalMassNumber;
  }

  public static ReactionSideSummary of(final ReactionSide side) {
    if (side == null) {
      throw new IllegalArgumentException("Reaction side summary side must not be null.");
    }
    final long[] elementCountsByAtomicNumber = new long[PeriodicTable.MAX_ATOMIC_NUMBER + 1];
    final List<ReactionParticipant> participants = side.participants();
    long totalCoefficient = 0L;
    long atomCount = 0L;
    long charge = 0L;
    double averageMass = 0.0;
    long nominalMassNumber = 0L;
    int elementKindCount = 0;
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final long coefficient = participant.coefficient().value();
      totalCoefficient = Math.addExact(totalCoefficient, coefficient);
      atomCount =
          Math.addExact(
              atomCount,
              Math.multiplyExact(coefficient, participant.molecule().composition().atomCount()));
      charge =
          Math.addExact(
              charge, Math.multiplyExact(coefficient, participant.molecule().charge().value()));
      averageMass +=
          (double) coefficient * participant.molecule().composition().mass().averageAtomicMass();
      nominalMassNumber =
          Math.addExact(
              nominalMassNumber,
              Math.multiplyExact(
                  coefficient, participant.molecule().composition().mass().nominalMassNumber()));
      elementKindCount =
          ReactionSideSummary.appendFormulaTerms(
              elementCountsByAtomicNumber, elementKindCount, participant, coefficient);
    }
    return new ReactionSideSummary(
        participants.size(),
        totalCoefficient,
        atomCount,
        elementCountsByAtomicNumber,
        elementKindCount,
        charge,
        averageMass,
        nominalMassNumber);
  }

  public int participantCount() {
    return participantCount;
  }

  public long totalCoefficient() {
    return totalCoefficient;
  }

  public long atomCount() {
    return atomCount;
  }

  public Map<ElementSymbol, Long> elementCounts() {
    return elementCounts;
  }

  public int elementKindCount() {
    return elementKindCount;
  }

  public long elementCount(final ElementSymbol symbol) {
    if (symbol == null) {
      return 0L;
    }
    return elementCountByAtomicNumber(PeriodicTable.atomicNumberOf(symbol));
  }

  public long elementCountByAtomicNumber(final int atomicNumber) {
    if (!PeriodicTable.containsAtomicNumber(atomicNumber)) {
      return 0L;
    }
    return elementCountsByAtomicNumber[atomicNumber];
  }

  public long charge() {
    return charge;
  }

  public double averageMass() {
    return averageMass;
  }

  public long nominalMassNumber() {
    return nominalMassNumber;
  }

  private static int appendFormulaTerms(
      final long[] elementCountsByAtomicNumber,
      final int currentElementKindCount,
      final ReactionParticipant participant,
      final long coefficient) {
    int elementKindCount = currentElementKindCount;
    final List<MolecularFormulaTerm> terms = participant.molecule().formula().terms();
    for (int i = 0; i < terms.size(); ++i) {
      final MolecularFormulaTerm term = terms.get(i);
      final int atomicNumber = PeriodicTable.atomicNumberOf(term.symbol());
      final long amount = Math.multiplyExact(coefficient, term.count());
      if (elementCountsByAtomicNumber[atomicNumber] == 0L) {
        elementKindCount = Math.addExact(elementKindCount, 1);
      }
      elementCountsByAtomicNumber[atomicNumber] =
          Math.addExact(elementCountsByAtomicNumber[atomicNumber], amount);
    }
    return elementKindCount;
  }

  private static Map<ElementSymbol, Long> toElementCountMap(final long[] countsByAtomicNumber) {
    final LinkedHashMap<ElementSymbol, Long> result = new LinkedHashMap<ElementSymbol, Long>();
    for (int atomicNumber = PeriodicTable.MIN_ATOMIC_NUMBER;
        atomicNumber <= PeriodicTable.MAX_ATOMIC_NUMBER;
        ++atomicNumber) {
      final long count = countsByAtomicNumber[atomicNumber];
      if (count <= 0L) {
        continue;
      }
      final ChemicalElement element = PeriodicTable.requireAtomicNumber(atomicNumber);
      result.put(element.symbol(), count);
    }
    return Collections.unmodifiableMap(result);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionSideSummary)) {
      return false;
    }
    final ReactionSideSummary summary = (ReactionSideSummary) other;
    return participantCount == summary.participantCount
        && totalCoefficient == summary.totalCoefficient
        && atomCount == summary.atomCount
        && elementKindCount == summary.elementKindCount
        && charge == summary.charge
        && Double.compare(averageMass, summary.averageMass) == 0
        && nominalMassNumber == summary.nominalMassNumber
        && Objects.equals(elementCounts, summary.elementCounts);
  }

  public int hashCode() {
    return Objects.hash(
        participantCount,
        totalCoefficient,
        atomCount,
        elementKindCount,
        elementCounts,
        charge,
        averageMass,
        nominalMassNumber);
  }
}