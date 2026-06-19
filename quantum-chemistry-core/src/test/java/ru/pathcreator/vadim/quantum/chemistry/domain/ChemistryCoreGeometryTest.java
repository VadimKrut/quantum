/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.BondAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.BondLength;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.DihedralAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.MolecularGeometry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ChemistryCoreGeometryTest {

  private static final double EPSILON = 1.0E-9;

  ChemistryCoreGeometryTest() {}

  @Test
  void lengthValueConvertsBetweenAngstromAndBohr() {
    final LengthValue oneBohr = LengthValue.of((double) 1.0, (LengthUnit) LengthUnit.BOHR);
    Assertions.assertEquals((double) 0.529177210903, (double) oneBohr.angstroms(), (double) 1.0E-9);
    Assertions.assertEquals(
        (double) 1.0,
        (double)
            LengthValue.of((double) 0.529177210903, (LengthUnit) LengthUnit.ANGSTROM)
                .in(LengthUnit.BOHR),
        (double) 1.0E-9);
  }

  @Test
  void molecularGeometryCalculatesBondLengthsForAllBonds() {
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "geometry.h2"),
            (String) "Hydrogen geometry",
            List.of(
                ChemistryCoreGeometryTest.atom(
                    "h1",
                    "H",
                    Coordinate3D.of(
                        (double) 0.0,
                        (double) 0.0,
                        (double) 0.0,
                        (LengthUnit) LengthUnit.ANGSTROM)),
                ChemistryCoreGeometryTest.atom(
                    "h2",
                    "H",
                    Coordinate3D.of(
                        (double) 1.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.BOHR))),
            List.of(
                Bond.of(
                    (AtomId) AtomId.of((String) "h1"),
                    (AtomId) AtomId.of((String) "h2"),
                    (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    final BondLength length =
        MolecularGeometry.bondLength(
            (Molecule) molecule,
            (AtomId) AtomId.of((String) "h1"),
            (AtomId) AtomId.of((String) "h2"),
            (LengthUnit) LengthUnit.ANGSTROM);
    List<BondLength> lengths =
        MolecularGeometry.allBondLengths((Molecule) molecule, (LengthUnit) LengthUnit.BOHR);
    Assertions.assertEquals(
        (double) 0.529177210903, (double) length.length().value(), (double) 1.0E-9);
    Assertions.assertEquals((int) 1, (int) lengths.size());
    Assertions.assertEquals(
        (double) 1.0, (double) lengths.get(0).length().value(), (double) 1.0E-9);
  }

  @Test
  void allBondLengthsUsesOneCoordinateIndexAndKeepsBondOrder() {
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "geometry.chain"),
            (String) "Geometry chain",
            List.of(
                ChemistryCoreGeometryTest.atom(
                    "a",
                    "C",
                    Coordinate3D.of(
                        (double) 0.0,
                        (double) 0.0,
                        (double) 0.0,
                        (LengthUnit) LengthUnit.ANGSTROM)),
                ChemistryCoreGeometryTest.atom(
                    "b",
                    "C",
                    Coordinate3D.of(
                        (double) 1.0,
                        (double) 0.0,
                        (double) 0.0,
                        (LengthUnit) LengthUnit.ANGSTROM)),
                ChemistryCoreGeometryTest.atom(
                    "c",
                    "C",
                    Coordinate3D.of(
                        (double) 1.0, (double) 1.0, (double) 0.0, (LengthUnit) LengthUnit.BOHR)),
                ChemistryCoreGeometryTest.atom(
                    "d",
                    "H",
                    Coordinate3D.of(
                        (double) 1.0, (double) 1.0, (double) 1.0, (LengthUnit) LengthUnit.BOHR))),
            List.of(
                Bond.of(
                    (AtomId) AtomId.of((String) "a"),
                    (AtomId) AtomId.of((String) "b"),
                    (BondType) BondType.SINGLE),
                Bond.of(
                    (AtomId) AtomId.of((String) "b"),
                    (AtomId) AtomId.of((String) "c"),
                    (BondType) BondType.SINGLE),
                Bond.of(
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "d"),
                    (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);

    final List<BondLength> lengths =
        MolecularGeometry.allBondLengths((Molecule) molecule, (LengthUnit) LengthUnit.ANGSTROM);

    Assertions.assertEquals((int) 3, (int) lengths.size());
    Assertions.assertEquals(
        (Object) AtomId.of((String) "a"), (Object) lengths.get(0).firstAtomId());
    Assertions.assertEquals(
        (Object) AtomId.of((String) "b"), (Object) lengths.get(0).secondAtomId());
    Assertions.assertEquals(
        (Object) AtomId.of((String) "b"), (Object) lengths.get(1).firstAtomId());
    Assertions.assertEquals(
        (Object) AtomId.of((String) "c"), (Object) lengths.get(1).secondAtomId());
    Assertions.assertEquals(
        (Object) AtomId.of((String) "c"), (Object) lengths.get(2).firstAtomId());
    Assertions.assertEquals(
        (Object) AtomId.of((String) "d"), (Object) lengths.get(2).secondAtomId());
    Assertions.assertEquals((double) 1.0, (double) lengths.get(0).length().value(), EPSILON);
    Assertions.assertEquals(
        (double) Math.sqrt(Math.pow(1.0 - 0.529177210903, 2.0) + Math.pow(0.529177210903, 2.0)),
        (double) lengths.get(1).length().value(),
        EPSILON);
    Assertions.assertEquals(
        (double) 0.529177210903, (double) lengths.get(2).length().value(), EPSILON);
  }

  @Test
  void molecularGeometryCalculatesBondAngle() {
    Molecule molecule =
        ChemistryCoreGeometryTest.moleculeWithCoordinates(
            Coordinate3D.of(
                (double) 1.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 1.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 1.0, (double) 1.0, (LengthUnit) LengthUnit.ANGSTROM));
    BondAngle angle =
        MolecularGeometry.bondAngle(
            (Molecule) molecule,
            (AtomId) AtomId.of((String) "a"),
            (AtomId) AtomId.of((String) "b"),
            (AtomId) AtomId.of((String) "c"));
    Assertions.assertEquals((double) 90.0, (double) angle.degrees(), (double) 1.0E-9);
  }

  @Test
  void molecularGeometryCalculatesSignedDihedralAngle() {
    final Molecule molecule =
        ChemistryCoreGeometryTest.moleculeWithCoordinates(
            Coordinate3D.of(
                (double) 1.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 1.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 1.0, (double) 1.0, (LengthUnit) LengthUnit.ANGSTROM));
    final DihedralAngle angle =
        MolecularGeometry.dihedralAngle(
            (Molecule) molecule,
            (AtomId) AtomId.of((String) "a"),
            (AtomId) AtomId.of((String) "b"),
            (AtomId) AtomId.of((String) "c"),
            (AtomId) AtomId.of((String) "d"));
    Assertions.assertEquals((double) 90.0, (double) angle.degrees(), (double) 1.0E-9);
  }

  @Test
  void molecularGeometryRejectsMissingCoordinateUnknownAtomAndZeroVector() {
    final Molecule missingCoordinate =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "geometry.missing"),
            (String) "Missing coordinate",
            List.of(
                ChemistryCoreGeometryTest.atom(
                    "a",
                    "C",
                    Coordinate3D.of(
                        (double) 0.0,
                        (double) 0.0,
                        (double) 0.0,
                        (LengthUnit) LengthUnit.ANGSTROM)),
                Atom.of(
                    (AtomId) AtomId.of((String) "b"),
                    (ElementSymbol) ElementSymbol.of((String) "H"),
                    null)),
            List.of(
                Bond.of(
                    (AtomId) AtomId.of((String) "a"),
                    (AtomId) AtomId.of((String) "b"),
                    (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    final Molecule zeroVector =
        ChemistryCoreGeometryTest.moleculeWithCoordinates(
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 1.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            Coordinate3D.of(
                (double) 0.0, (double) 1.0, (double) 1.0, (LengthUnit) LengthUnit.ANGSTROM));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularGeometry.bondLength(
                (Molecule) missingCoordinate,
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b"),
                (LengthUnit) LengthUnit.ANGSTROM));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularGeometry.bondLength(
                (Molecule) missingCoordinate,
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "missing"),
                (LengthUnit) LengthUnit.ANGSTROM));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularGeometry.bondAngle(
                (Molecule) zeroVector,
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b"),
                (AtomId) AtomId.of((String) "c")));
  }

  private static Molecule moleculeWithCoordinates(
      final Coordinate3D first, final Coordinate3D second, final Coordinate3D third, final Coordinate3D fourth) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "geometry.probe"),
        (String) "Geometry probe",
        List.of(
            ChemistryCoreGeometryTest.atom("a", "C", first),
            ChemistryCoreGeometryTest.atom("b", "C", second),
            ChemistryCoreGeometryTest.atom("c", "C", third),
            ChemistryCoreGeometryTest.atom("d", "H", fourth)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "b"),
                (AtomId) AtomId.of((String) "c"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "d"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol, final Coordinate3D coordinate) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D) coordinate);
  }
}