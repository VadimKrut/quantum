/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;

/**
 * Converts rich chemistry-core molecules to compact atomic-unit snapshots.
 */
public final class IntegralMoleculeFactory {

  private IntegralMoleculeFactory() {}

  public static IntegralMolecule fromMolecule(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Integral molecule source must not be null.");
    }
    final List<Atom> atoms = molecule.atoms();
    final ArrayList<IntegralAtom> integralAtoms = new ArrayList<IntegralAtom>(atoms.size());
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (!atom.hasCoordinate()) {
        throw new IllegalArgumentException("Integral molecule atom must have coordinates.");
      }
      final Coordinate3D coordinate = atom.coordinate();
      integralAtoms.add(
          IntegralAtom.of(
              atom.element().atomicNumber(),
              LengthUnit.BOHR.fromAngstrom(coordinate.unit().toAngstrom(coordinate.x())),
              LengthUnit.BOHR.fromAngstrom(coordinate.unit().toAngstrom(coordinate.y())),
              LengthUnit.BOHR.fromAngstrom(coordinate.unit().toAngstrom(coordinate.z()))));
    }
    return IntegralMolecule.of(integralAtoms, molecule.charge().value());
  }
}
