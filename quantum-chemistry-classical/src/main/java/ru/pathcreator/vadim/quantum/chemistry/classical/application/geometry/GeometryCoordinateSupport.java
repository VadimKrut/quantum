/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Общие проверки и compact-array преобразования геометрии для локальных расчетов.
 */
public final class GeometryCoordinateSupport {

  private GeometryCoordinateSupport() {}

  public static double[] coordinatesAngstrom(final Molecule molecule) {
    final double[] coordinates = new double[molecule.atomCount() * 3];
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final int offset = i * 3;
      coordinates[offset] =
          atoms.get(i).coordinate().unit().toAngstrom(atoms.get(i).coordinate().x());
      coordinates[offset + 1] =
          atoms.get(i).coordinate().unit().toAngstrom(atoms.get(i).coordinate().y());
      coordinates[offset + 2] =
          atoms.get(i).coordinate().unit().toAngstrom(atoms.get(i).coordinate().z());
    }
    return coordinates;
  }

  public static Molecule withCoordinatesAngstrom(
      final Molecule molecule, final double[] coordinatesAngstrom) {
    if (coordinatesAngstrom == null || coordinatesAngstrom.length != molecule.atomCount() * 3) {
      throw new IllegalArgumentException(
          "Geometry coordinate count must match molecule atom count.");
    }
    final List<Atom> atoms = molecule.atoms();
    final ArrayList<Atom> displacedAtoms = new ArrayList<Atom>(atoms.size());
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final int offset = i * 3;
      displacedAtoms.add(
          Atom.of(
              atom.id(),
              atom.element().symbol(),
              Coordinate3D.of(
                  coordinatesAngstrom[offset],
                  coordinatesAngstrom[offset + 1],
                  coordinatesAngstrom[offset + 2],
                  LengthUnit.ANGSTROM),
              atom.formalCharge(),
              atom.isotope(),
              atom.radicalState(),
              atom.metadata()));
    }
    return Molecule.of(
        molecule.id(),
        molecule.displayName(),
        displacedAtoms,
        molecule.bonds(),
        molecule.charge(),
        molecule.spinMultiplicity(),
        molecule.stereochemistry(),
        molecule.conformation(),
        molecule.opticalRotation(),
        molecule.symmetry(),
        molecule.metadata());
  }

  public static String coordinateError(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (!atoms.get(i).hasCoordinate()) {
        return "Geometry calculation requires coordinates for atom "
            + atoms.get(i).id().value()
            + ".";
      }
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      if (!bonds.get(i).type().hasDefinedOrder()) {
        return "Geometry calculation requires defined bond orders; unknown bond found.";
      }
    }
    return null;
  }

  public static double norm(final double[] values) {
    double sum = 0.0;
    for (int i = 0; i < values.length; ++i) {
      sum += values[i] * values[i];
    }
    return Math.sqrt(sum);
  }

  public static double maxAbsolute(final double[] values) {
    double max = 0.0;
    for (int i = 0; i < values.length; ++i) {
      max = Math.max(max, Math.abs(values[i]));
    }
    return max;
  }
}
