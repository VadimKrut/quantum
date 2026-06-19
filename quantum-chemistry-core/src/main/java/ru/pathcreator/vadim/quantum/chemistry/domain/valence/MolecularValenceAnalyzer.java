/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.valence;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class MolecularValenceAnalyzer {

  private MolecularValenceAnalyzer() {}

  public static MolecularValence analyze(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Molecular valence molecule must not be null.");
    }
    final List<Atom> atoms = molecule.atoms();
    final ArrayList<AtomValence> atomValences = new ArrayList<AtomValence>(atoms.size());
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      atomValences.add(MolecularValenceAnalyzer.atomValenceOf(atom, molecule.bonds()));
    }
    return MolecularValence.of(atomValences);
  }

  private static AtomValence atomValenceOf(
      final Atom atom,
      final List<Bond> bonds
  ) {
    int bondCount = 0;
    double bondOrderSum = 0.0;
    boolean hasUnknownBondOrder = false;
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!MolecularValenceAnalyzer.references(bond, atom.id())) continue;
      bondCount = Math.addExact(bondCount, 1);
      if (bond.type().hasDefinedOrder()) {
        bondOrderSum += bond.type().orderValue();
        continue;
      }
      hasUnknownBondOrder = true;
    }
    return AtomValence.of(
        atom.id(), atom.element().symbol(), bondCount, bondOrderSum, hasUnknownBondOrder);
  }

  private static boolean references(
      final Bond bond,
      final AtomId atomId
  ) {
    return bond.firstAtomId().equals(atomId) || bond.secondAtomId().equals(atomId);
  }
}