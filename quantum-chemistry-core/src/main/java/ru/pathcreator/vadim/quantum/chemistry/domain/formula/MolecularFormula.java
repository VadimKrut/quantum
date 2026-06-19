/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.formula;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Неизменяемая молекулярная формула с primitive-вектором количества элементов. */
public final class MolecularFormula {

  private static final ElementSymbol CARBON = ElementSymbol.of("C");
  private static final ElementSymbol HYDROGEN = ElementSymbol.of("H");

  private final List<MolecularFormulaTerm> terms;
  private final ElementCountVector countVector;

  private MolecularFormula(
      final List<MolecularFormulaTerm> terms, final ElementCountVector countVector) {
    this.terms = terms;
    this.countVector = countVector;
  }

  public static MolecularFormula of(final List<MolecularFormulaTerm> terms) {
    return MolecularFormula.fromVector(ElementCountVector.of(terms));
  }

  public static MolecularFormula fromMolecule(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Molecule must not be null.");
    }
    final ElementCountVector.Builder builder = ElementCountVector.builder();
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      builder.add(atoms.get(i).element().symbol(), 1);
    }
    return MolecularFormula.fromVector(builder.build());
  }

  public static MolecularFormula fromCountVector(final ElementCountVector countVector) {
    if (countVector == null) {
      throw new IllegalArgumentException("Formula count vector must not be null.");
    }
    return MolecularFormula.fromVector(countVector);
  }

  public List<MolecularFormulaTerm> terms() {
    return terms;
  }

  public ElementCountVector countVector() {
    return countVector;
  }

  public int countOf(final ElementSymbol symbol) {
    return countVector.countOf(symbol);
  }

  public int atomCount() {
    return countVector.totalAtomCount();
  }

  public int elementKindCount() {
    return countVector.elementKindCount();
  }

  public String hillNotation() {
    final StringBuilder builder = new StringBuilder();
    final boolean hasCarbon = countOf(CARBON) > 0;
    if (hasCarbon) {
      appendIfPresent(builder, CARBON);
      appendIfPresent(builder, HYDROGEN);
    }
    for (int i = 0; i < terms.size(); ++i) {
      final MolecularFormulaTerm term = terms.get(i);
      if (hasCarbon && MolecularFormula.isCarbonOrHydrogen(term.symbol())) {
        continue;
      }
      MolecularFormula.appendTerm(builder, term);
    }
    return builder.toString();
  }

  private static boolean isCarbonOrHydrogen(final ElementSymbol symbol) {
    return symbol.equals(CARBON) || symbol.equals(HYDROGEN);
  }

  private static MolecularFormula fromVector(final ElementCountVector countVector) {
    return new MolecularFormula(countVector.termsAlphabetically(), countVector);
  }

  private void appendIfPresent(
      final StringBuilder builder,
      final ElementSymbol symbol
  ) {
    for (int i = 0; i < terms.size(); ++i) {
      final MolecularFormulaTerm term = terms.get(i);
      if (!term.symbol().equals(symbol)) {
        continue;
      }
      MolecularFormula.appendTerm(builder, term);
      return;
    }
  }

  private static void appendTerm(
      final StringBuilder builder,
      final MolecularFormulaTerm term
  ) {
    builder.append(term.symbol().value());
    if (term.count() > 1) {
      builder.append(term.count());
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MolecularFormula)) {
      return false;
    }
    final MolecularFormula formula = (MolecularFormula) other;
    return Objects.equals(countVector, formula.countVector);
  }

  public int hashCode() {
    return countVector.hashCode();
  }
}