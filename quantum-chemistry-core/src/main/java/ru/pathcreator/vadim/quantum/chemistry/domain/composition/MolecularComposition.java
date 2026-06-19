/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.composition;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.ElementCountVector;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormula;

/** Итоговый элементный состав молекулы: формула, счётчики атомов и молекулярная масса. */
public final class MolecularComposition {

  private final MolecularFormula formula;
  private final int atomCount;
  private final int hydrogenCount;
  private final int heavyAtomCount;
  private final int heteroAtomCount;
  private final boolean isotopicallyLabeled;
  private final MolecularMass mass;

  private MolecularComposition(
      final MolecularFormula formula,
      final int atomCount,
      final int hydrogenCount,
      final int heavyAtomCount,
      final int heteroAtomCount,
      final boolean isotopicallyLabeled,
      final MolecularMass mass) {
    this.formula = formula;
    this.atomCount = atomCount;
    this.hydrogenCount = hydrogenCount;
    this.heavyAtomCount = heavyAtomCount;
    this.heteroAtomCount = heteroAtomCount;
    this.isotopicallyLabeled = isotopicallyLabeled;
    this.mass = mass;
  }

  public static MolecularComposition of(
      final MolecularFormula formula,
      final int atomCount,
      final int hydrogenCount,
      final int heavyAtomCount,
      final int heteroAtomCount,
      final boolean isotopicallyLabeled,
      final MolecularMass mass) {
    if (formula == null) {
      throw new IllegalArgumentException("Molecular composition formula must not be null.");
    }
    if (atomCount <= 0) {
      throw new IllegalArgumentException("Molecular composition atom count must be positive.");
    }
    if (hydrogenCount < 0 || heavyAtomCount < 0 || heteroAtomCount < 0) {
      throw new IllegalArgumentException("Molecular composition counters must not be negative.");
    }
    if (Math.addExact(hydrogenCount, heavyAtomCount) != atomCount) {
      throw new IllegalArgumentException(
          "Molecular composition hydrogen and heavy atom counts must match atom count.");
    }
    if (heteroAtomCount > heavyAtomCount) {
      throw new IllegalArgumentException(
          "Molecular composition hetero atom count must not exceed heavy atom count.");
    }
    MolecularComposition.requireFormulaCounters(
        formula, atomCount, hydrogenCount, heavyAtomCount, heteroAtomCount);
    if (mass == null) {
      throw new IllegalArgumentException("Molecular composition mass must not be null.");
    }
    return new MolecularComposition(
        formula,
        atomCount,
        hydrogenCount,
        heavyAtomCount,
        heteroAtomCount,
        isotopicallyLabeled,
        mass);
  }

  private static void requireFormulaCounters(
      final MolecularFormula formula,
      final int atomCount,
      final int hydrogenCount,
      final int heavyAtomCount,
      final int heteroAtomCount) {
    final ElementCountVector vector = formula.countVector();
    if (vector.totalAtomCount() != atomCount) {
      throw new IllegalArgumentException(
          "Molecular composition atom count must match formula atom count.");
    }
    if (vector.hydrogenCount() != hydrogenCount) {
      throw new IllegalArgumentException(
          "Molecular composition hydrogen count must match formula hydrogen count.");
    }
    if (vector.heavyAtomCount() != heavyAtomCount) {
      throw new IllegalArgumentException(
          "Molecular composition heavy atom count must match formula heavy atom count.");
    }
    if (vector.heteroAtomCount() != heteroAtomCount) {
      throw new IllegalArgumentException(
          "Molecular composition hetero atom count must match formula hetero atom count.");
    }
  }

  public MolecularFormula formula() {
    return this.formula;
  }

  public int atomCount() {
    return this.atomCount;
  }

  public int hydrogenCount() {
    return this.hydrogenCount;
  }

  public int heavyAtomCount() {
    return this.heavyAtomCount;
  }

  public int heteroAtomCount() {
    return this.heteroAtomCount;
  }

  public boolean isotopicallyLabeled() {
    return this.isotopicallyLabeled;
  }

  public MolecularMass mass() {
    return this.mass;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularComposition)) {
      return false;
    }
    final MolecularComposition composition = (MolecularComposition) other;
    return this.atomCount == composition.atomCount
        && this.hydrogenCount == composition.hydrogenCount
        && this.heavyAtomCount == composition.heavyAtomCount
        && this.heteroAtomCount == composition.heteroAtomCount
        && this.isotopicallyLabeled == composition.isotopicallyLabeled
        && Objects.equals(this.formula, composition.formula)
        && Objects.equals(this.mass, composition.mass);
  }

  public int hashCode() {
    int result = this.formula.hashCode();
    result = 31 * result + this.atomCount;
    result = 31 * result + this.hydrogenCount;
    result = 31 * result + this.heavyAtomCount;
    result = 31 * result + this.heteroAtomCount;
    result = 31 * result + Boolean.hashCode(this.isotopicallyLabeled);
    result = 31 * result + this.mass.hashCode();
    return result;
  }
}