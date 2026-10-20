/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class MoleculeGeometryRegularizer {

  public static final String ALGORITHM_VERSION = "topology-seed-2";

  private static final double MINIMUM_NUCLEAR_DISTANCE_ANGSTROM = 0.20;

  private MoleculeGeometryRegularizer() {}

  public static MoleculeGeometryRegularization regularizeForElectronicPreparation(
      final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException(
          "Molecule geometry regularization molecule must not be null.");
    }
    if (MoleculeGeometryRegularizer.geometryUsable(molecule)) {
      return MoleculeGeometryRegularization.unchanged(molecule);
    }
    return MoleculeGeometryRegularization.regularized(
        MoleculeGeometryRegularizer.withGeneratedCoordinates(molecule));
  }

  private static boolean geometryUsable(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (!atoms.get(i).hasCoordinate()) {
        return false;
      }
    }
    for (int i = 0; i < atoms.size(); ++i) {
      final Coordinate3D first = atoms.get(i).coordinate();
      for (int j = i + 1; j < atoms.size(); ++j) {
        if (MoleculeGeometryRegularizer.distanceAngstrom(first, atoms.get(j).coordinate())
            < MINIMUM_NUCLEAR_DISTANCE_ANGSTROM) {
          return false;
        }
      }
    }
    return true;
  }

  private static Molecule withGeneratedCoordinates(final Molecule molecule) {
    return GeometryCoordinateSupport.withCoordinatesAngstrom(
        molecule,
        ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEmbedding
            .spatialCoordinates(
                molecule,
                bond ->
                    equilibriumDistanceAngstrom(
                        molecule.atoms().stream()
                            .filter(atom -> atom.id().equals(bond.firstAtomId()))
                            .findFirst()
                            .orElseThrow(),
                        molecule.atoms().stream()
                            .filter(atom -> atom.id().equals(bond.secondAtomId()))
                            .findFirst()
                            .orElseThrow(),
                        bond.type()),
                MoleculeGeometryRegularizer::covalentRadiusAngstrom));
  }

  private static double equilibriumDistanceAngstrom(
      final Atom firstAtom, final Atom secondAtom, final BondType bondType) {
    final double radiusSum =
        MoleculeGeometryRegularizer.covalentRadiusAngstrom(firstAtom)
            + MoleculeGeometryRegularizer.covalentRadiusAngstrom(secondAtom);
    if (bondType == BondType.TRIPLE) {
      return radiusSum * 0.78;
    }
    if (bondType == BondType.DOUBLE) {
      return radiusSum * 0.87;
    }
    if (bondType == BondType.AROMATIC) {
      return radiusSum * 0.92;
    }
    return radiusSum;
  }

  private static double covalentRadiusAngstrom(final Atom atom) {
    switch (atom.element().atomicNumber()) {
      case 1:
        return 0.31;
      case 5:
        return 0.84;
      case 6:
        return 0.76;
      case 7:
        return 0.71;
      case 8:
        return 0.66;
      case 9:
        return 0.57;
      case 15:
        return 1.07;
      case 16:
        return 1.05;
      case 17:
        return 1.02;
      case 35:
        return 1.20;
      case 53:
        return 1.39;
      default:
        return Math.max(0.35, 0.25 + 0.012 * (double) atom.element().atomicNumber());
    }
  }

  private static double distanceAngstrom(final Coordinate3D first, final Coordinate3D second) {
    final double dx = first.unit().toAngstrom(first.x()) - second.unit().toAngstrom(second.x());
    final double dy = first.unit().toAngstrom(first.y()) - second.unit().toAngstrom(second.y());
    final double dz = first.unit().toAngstrom(first.z()) - second.unit().toAngstrom(second.z());
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  public static final class MoleculeGeometryRegularization {

    private final Molecule molecule;
    private final boolean regularized;

    private MoleculeGeometryRegularization(final Molecule molecule, final boolean regularized) {
      this.molecule = molecule;
      this.regularized = regularized;
    }

    private static MoleculeGeometryRegularization unchanged(final Molecule molecule) {
      return new MoleculeGeometryRegularization(molecule, false);
    }

    private static MoleculeGeometryRegularization regularized(final Molecule molecule) {
      return new MoleculeGeometryRegularization(molecule, true);
    }

    public Molecule molecule() {
      return this.molecule;
    }

    public boolean regularized() {
      return this.regularized;
    }
  }
}
