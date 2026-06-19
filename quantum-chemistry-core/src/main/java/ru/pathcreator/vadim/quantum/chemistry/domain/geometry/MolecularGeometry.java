/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Геометрические расчёты по координатам молекулы: длины связей, валентные углы и торсионные углы.
 */
public final class MolecularGeometry {

  private static final double ZERO_VECTOR_EPSILON = 1.0E-12;

  private MolecularGeometry() {}

  public static BondLength bondLength(
      final Molecule molecule,
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final LengthUnit unit) {
    final CoordinateIndex coordinateIndex = CoordinateIndex.of(molecule);
    return MolecularGeometry.bondLength(coordinateIndex, firstAtomId, secondAtomId, unit);
  }

  private static BondLength bondLength(
      final CoordinateIndex coordinateIndex,
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final LengthUnit unit) {
    final LengthUnit checkedUnit = MolecularGeometry.requireUnit(unit);
    final Vector3D first = coordinateIndex.coordinateVectorOf(firstAtomId);
    final Vector3D second = coordinateIndex.coordinateVectorOf(secondAtomId);
    final Vector3D vector = second.subtract(first);
    final double angstromLength = vector.nonZeroLength();
    return BondLength.of(
        firstAtomId,
        secondAtomId,
        LengthValue.of(checkedUnit.fromAngstrom(angstromLength), checkedUnit));
  }

  public static List<BondLength> allBondLengths(
      final Molecule molecule,
      final LengthUnit unit
  ) {
    MolecularGeometry.requireMolecule(molecule);
    final CoordinateIndex coordinateIndex = CoordinateIndex.of(molecule);
    final List<Bond> bonds = molecule.bonds();
    final ArrayList<BondLength> lengths = new ArrayList<BondLength>(bonds.size());
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      lengths.add(
          MolecularGeometry.bondLength(
              coordinateIndex, bond.firstAtomId(), bond.secondAtomId(), unit));
    }
    return List.copyOf(lengths);
  }

  public static BondAngle bondAngle(
      final Molecule molecule,
      final AtomId firstAtomId,
      final AtomId centerAtomId,
      final AtomId thirdAtomId) {
    final CoordinateIndex coordinateIndex = CoordinateIndex.of(molecule);
    final Vector3D first = coordinateIndex.coordinateVectorOf(firstAtomId);
    final Vector3D center = coordinateIndex.coordinateVectorOf(centerAtomId);
    final Vector3D third = coordinateIndex.coordinateVectorOf(thirdAtomId);
    final Vector3D firstVector = first.subtract(center);
    final Vector3D secondVector = third.subtract(center);
    final double degrees =
        Math.toDegrees(
            Math.acos(
                MolecularGeometry.clampCosine(
                    firstVector.dot(secondVector)
                        / (firstVector.nonZeroLength() * secondVector.nonZeroLength()))));
    return BondAngle.of(firstAtomId, centerAtomId, thirdAtomId, degrees);
  }

  public static DihedralAngle dihedralAngle(
      final Molecule molecule,
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final AtomId thirdAtomId,
      final AtomId fourthAtomId) {
    final CoordinateIndex coordinateIndex = CoordinateIndex.of(molecule);
    final Vector3D first = coordinateIndex.coordinateVectorOf(firstAtomId);
    final Vector3D second = coordinateIndex.coordinateVectorOf(secondAtomId);
    final Vector3D third = coordinateIndex.coordinateVectorOf(thirdAtomId);
    final Vector3D fourth = coordinateIndex.coordinateVectorOf(fourthAtomId);
    final Vector3D firstBond = second.subtract(first);
    final Vector3D secondBond = third.subtract(second);
    final Vector3D thirdBond = fourth.subtract(third);
    final Vector3D firstNormal = firstBond.cross(secondBond);
    final Vector3D secondNormal = secondBond.cross(thirdBond);
    final Vector3D secondBondUnit = secondBond.normalized();
    final Vector3D signedReference = firstNormal.cross(secondBondUnit);
    final double radians =
        Math.atan2(signedReference.dot(secondNormal), firstNormal.dot(secondNormal));
    return DihedralAngle.of(
        firstAtomId,
        secondAtomId,
        thirdAtomId,
        fourthAtomId,
        MolecularGeometry.normalizeSignedDegrees(Math.toDegrees(radians)));
  }

  private static void requireMolecule(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Geometry molecule must not be null.");
    }
  }

  private static LengthUnit requireUnit(final LengthUnit unit) {
    if (unit == null) {
      throw new IllegalArgumentException("Geometry length unit must not be null.");
    }
    return unit;
  }

  private static double clampCosine(final double value) {
    if (value > 1.0) {
      return 1.0;
    }
    if (value < -1.0) {
      return -1.0;
    }
    return value;
  }

  private static double normalizeSignedDegrees(final double degrees) {
    if (degrees > 180.0) {
      return degrees - 360.0;
    }
    if (degrees <= -180.0) {
      return degrees + 360.0;
    }
    return degrees;
  }

  /** Индекс координат молекулы в ангстремах для расчётов без повторного поиска атомов. */
  private static final class CoordinateIndex {

    private final Map<AtomId, Vector3D> coordinatesByAtomId;

    private CoordinateIndex(
        final Map<AtomId, Vector3D> coordinatesByAtomId
    ) {
      this.coordinatesByAtomId = coordinatesByAtomId;
    }

    private static CoordinateIndex of(final Molecule molecule) {
      MolecularGeometry.requireMolecule(molecule);
      final List<Atom> atoms = molecule.atoms();
      final HashMap<AtomId, Vector3D> coordinatesByAtomId =
          new HashMap<AtomId, Vector3D>(atoms.size() * 2);
      for (int i = 0; i < atoms.size(); ++i) {
        final Atom atom = atoms.get(i);
        if (!atom.hasCoordinate()) {
          continue;
        }
        final Coordinate3D coordinate = atom.coordinate();
        coordinatesByAtomId.put(
            atom.id(),
            new Vector3D(
                coordinate.unit().toAngstrom(coordinate.x()),
                coordinate.unit().toAngstrom(coordinate.y()),
                coordinate.unit().toAngstrom(coordinate.z())));
      }
      return new CoordinateIndex(coordinatesByAtomId);
    }

    private Vector3D coordinateVectorOf(final AtomId atomId) {
      if (atomId == null) {
        throw new IllegalArgumentException("Geometry atom id must not be null.");
      }
      final Vector3D coordinate = this.coordinatesByAtomId.get(atomId);
      if (coordinate == null) {
        throw new IllegalArgumentException(
            "Geometry atom id is not present in molecule or does not have coordinates.");
      }
      return coordinate;
    }
  }

  private static final class Vector3D {

    private final double x;
    private final double y;
    private final double z;

    private Vector3D(
        final double x,
        final double y,
        final double z
    ) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    private Vector3D subtract(final Vector3D other) {
      return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    private Vector3D cross(final Vector3D other) {
      return new Vector3D(
          this.y * other.z - this.z * other.y,
          this.z * other.x - this.x * other.z,
          this.x * other.y - this.y * other.x);
    }

    private double dot(final Vector3D other) {
      return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    private double length() {
      return Math.sqrt(this.dot(this));
    }

    private double nonZeroLength() {
      final double length = this.length();
      if (length <= ZERO_VECTOR_EPSILON) {
        throw new IllegalArgumentException("Geometry vector must not have zero length.");
      }
      return length;
    }

    private Vector3D normalized() {
      final double length = this.nonZeroLength();
      return new Vector3D(this.x / length, this.y / length, this.z / length);
    }
  }
}