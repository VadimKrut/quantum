/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.composition;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.ElementCountVector;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormula;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Анализирует элементный состав молекулы, массовые характеристики и изотопные метки. */
public final class MolecularCompositionAnalyzer {

  private MolecularCompositionAnalyzer() {}

  public static MolecularComposition analyze(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Molecular composition molecule must not be null.");
    }
    final List<Atom> atoms = molecule.atoms();
    int hydrogenCount = 0;
    int heavyAtomCount = 0;
    int heteroAtomCount = 0;
    int nominalMassNumber = 0;
    double averageAtomicMass = 0.0;
    boolean isotopicallyLabeled = false;
    final ElementCountVector.Builder formulaBuilder = ElementCountVector.builder();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final ElementSymbol symbol = atom.element().symbol();
      formulaBuilder.add(symbol, 1);
      if (atom.hasIsotope()) {
        averageAtomicMass += (double) atom.isotope().massNumber();
        nominalMassNumber = Math.addExact(nominalMassNumber, atom.isotope().massNumber());
        isotopicallyLabeled = true;
        continue;
      }
      averageAtomicMass += atom.element().atomicMass();
      nominalMassNumber =
          Math.addExact(
              nominalMassNumber,
              MolecularCompositionAnalyzer.roundedNominalMass(atom.element().atomicMass()));
    }
    final ElementCountVector countVector = formulaBuilder.build();
    hydrogenCount = countVector.hydrogenCount();
    heavyAtomCount = countVector.heavyAtomCount();
    heteroAtomCount = countVector.heteroAtomCount();
    return MolecularComposition.of(
        MolecularFormula.fromCountVector(countVector),
        atoms.size(),
        hydrogenCount,
        heavyAtomCount,
        heteroAtomCount,
        isotopicallyLabeled,
        MolecularMass.of(averageAtomicMass, nominalMassNumber));
  }

  private static int roundedNominalMass(final double atomicMass) {
    return Math.toIntExact(Math.round(atomicMass));
  }
}