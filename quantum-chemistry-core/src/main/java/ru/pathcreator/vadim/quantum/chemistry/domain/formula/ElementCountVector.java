/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.formula;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ChemicalElement;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;

/**
 * Неизменяемый вектор количества элементов, индексированный atomic number без boxing.
 *
 * <p>Формула, реакционный баланс и расчёты состава часто обращаются к количествам элементов,
 * поэтому внутри используется primitive-массив. Публичный API остаётся доменным: наружу не отдаётся
 * mutable-массив, а все изменения проходят через builder.
 */
public final class ElementCountVector {

  private static final int HYDROGEN_ATOMIC_NUMBER = 1;
  private static final int CARBON_ATOMIC_NUMBER = 6;

  private final int[] countsByAtomicNumber;
  private final int totalAtomCount;
  private final int elementKindCount;

  private ElementCountVector(
      final int[] countsByAtomicNumber, final int totalAtomCount, final int elementKindCount) {
    this.countsByAtomicNumber = countsByAtomicNumber;
    this.totalAtomCount = totalAtomCount;
    this.elementKindCount = elementKindCount;
  }

  public static ElementCountVector of(final List<MolecularFormulaTerm> terms) {
    if (terms == null || terms.isEmpty()) {
      throw new IllegalArgumentException("Element count vector terms must not be empty.");
    }
    final Builder builder = ElementCountVector.builder();
    for (int i = 0; i < terms.size(); ++i) {
      final MolecularFormulaTerm term = terms.get(i);
      if (term == null) {
        throw new IllegalArgumentException("Element count vector term must not be null.");
      }
      builder.add(term.symbol(), term.count());
    }
    return builder.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public int countOf(final ElementSymbol symbol) {
    if (symbol == null) {
      return 0;
    }
    return countByAtomicNumber(PeriodicTable.atomicNumberOf(symbol));
  }

  public int countByAtomicNumber(final int atomicNumber) {
    if (!PeriodicTable.containsAtomicNumber(atomicNumber)) {
      return 0;
    }
    return countsByAtomicNumber[atomicNumber];
  }

  public int totalAtomCount() {
    return totalAtomCount;
  }

  public int elementKindCount() {
    return elementKindCount;
  }

  public int hydrogenCount() {
    return countsByAtomicNumber[HYDROGEN_ATOMIC_NUMBER];
  }

  public int carbonCount() {
    return countsByAtomicNumber[CARBON_ATOMIC_NUMBER];
  }

  public int heavyAtomCount() {
    return Math.subtractExact(totalAtomCount, hydrogenCount());
  }

  public int heteroAtomCount() {
    return Math.subtractExact(heavyAtomCount(), carbonCount());
  }

  public boolean contains(final ElementSymbol symbol) {
    return countOf(symbol) > 0;
  }

  public List<MolecularFormulaTerm> termsAlphabetically() {
    final ArrayList<MolecularFormulaTerm> result =
        new ArrayList<MolecularFormulaTerm>(elementKindCount);
    for (int atomicNumber = PeriodicTable.MIN_ATOMIC_NUMBER;
        atomicNumber <= PeriodicTable.MAX_ATOMIC_NUMBER;
        ++atomicNumber) {
      final int count = countsByAtomicNumber[atomicNumber];
      if (count <= 0) {
        continue;
      }
      final ChemicalElement element = PeriodicTable.requireAtomicNumber(atomicNumber);
      result.add(MolecularFormulaTerm.of(element.symbol(), count));
    }
    ElementCountVector.sortTermsBySymbol(result);
    return List.copyOf(result);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElementCountVector)) {
      return false;
    }
    final ElementCountVector vector = (ElementCountVector) other;
    if (totalAtomCount != vector.totalAtomCount || elementKindCount != vector.elementKindCount) {
      return false;
    }
    for (int i = 0; i < countsByAtomicNumber.length; ++i) {
      if (countsByAtomicNumber[i] != vector.countsByAtomicNumber[i]) {
        return false;
      }
    }
    return true;
  }

  public int hashCode() {
    int result = totalAtomCount;
    result = 31 * result + elementKindCount;
    for (int i = 0; i < countsByAtomicNumber.length; ++i) {
      result = 31 * result + countsByAtomicNumber[i];
    }
    return result;
  }

  private static void sortTermsBySymbol(final ArrayList<MolecularFormulaTerm> terms) {
    for (int i = 1; i < terms.size(); ++i) {
      int position;
      final MolecularFormulaTerm current = terms.get(i);
      for (position = i - 1;
          position >= 0
              && terms.get(position).symbol().value().compareTo(current.symbol().value()) > 0;
          --position) {
        terms.set(position + 1, terms.get(position));
      }
      terms.set(position + 1, current);
    }
  }

  /** Mutable builder с primitive-массивом; наружу выходит только неизменяемый снимок. */
  public static final class Builder {

    private final int[] countsByAtomicNumber = new int[PeriodicTable.MAX_ATOMIC_NUMBER + 1];
    private int totalAtomCount;
    private int elementKindCount;

    private Builder() {}

    public Builder add(
        final ElementSymbol symbol,
        final int count
    ) {
      if (symbol == null) {
        throw new IllegalArgumentException("Element count vector symbol must not be null.");
      }
      if (count <= 0) {
        throw new IllegalArgumentException("Element count vector count must be positive.");
      }
      final int atomicNumber = PeriodicTable.atomicNumberOf(symbol);
      if (countsByAtomicNumber[atomicNumber] == 0) {
        elementKindCount = Math.addExact(elementKindCount, 1);
      }
      countsByAtomicNumber[atomicNumber] = Math.addExact(countsByAtomicNumber[atomicNumber], count);
      totalAtomCount = Math.addExact(totalAtomCount, count);
      return this;
    }

    public ElementCountVector build() {
      if (totalAtomCount <= 0) {
        throw new IllegalArgumentException("Element count vector must contain at least one atom.");
      }
      final int[] snapshot = new int[countsByAtomicNumber.length];
      System.arraycopy(countsByAtomicNumber, 0, snapshot, 0, countsByAtomicNumber.length);
      return new ElementCountVector(snapshot, totalAtomCount, elementKindCount);
    }
  }
}