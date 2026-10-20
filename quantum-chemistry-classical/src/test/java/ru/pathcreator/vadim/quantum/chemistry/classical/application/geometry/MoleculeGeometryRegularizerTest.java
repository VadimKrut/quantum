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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.MoleculeGeometryRegularizer.MoleculeGeometryRegularization;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class MoleculeGeometryRegularizerTest {

  @Test
  void leavesUsableGeometryUnchanged() {
    final Molecule molecule =
        MoleculeGeometryRegularizerTest.molecule(
            "usable_geometry",
            Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM),
            Coordinate3D.of(0.0, 0.0, 0.74, LengthUnit.ANGSTROM));

    final MoleculeGeometryRegularization regularization =
        MoleculeGeometryRegularizer.regularizeForElectronicPreparation(molecule);

    Assertions.assertFalse(regularization.regularized());
    Assertions.assertSame(molecule, regularization.molecule());
  }

  @Test
  void separatesOverlappingNucleiWithoutMutatingSourceMolecule() {
    final Molecule molecule =
        MoleculeGeometryRegularizerTest.molecule(
            "overlapping_geometry",
            Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM),
            Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM));

    final MoleculeGeometryRegularization regularization =
        MoleculeGeometryRegularizer.regularizeForElectronicPreparation(molecule);

    Assertions.assertTrue(regularization.regularized());
    Assertions.assertNotSame(molecule, regularization.molecule());
    Assertions.assertEquals(0.0, molecule.atoms().get(1).coordinate().z(), 0.0);
    Assertions.assertTrue(
        MoleculeGeometryRegularizerTest.distance(
                regularization.molecule().atoms().get(0).coordinate(),
                regularization.molecule().atoms().get(1).coordinate())
            > 0.20);
  }

  @Test
  void generatesCoordinatesForMissingGeometry() {
    final Molecule molecule =
        Molecule.of(
            MoleculeId.of("missing_geometry"),
            "Missing geometry",
            List.of(
                Atom.of(AtomId.of("a0"), ElementSymbol.of("C"), null),
                Atom.of(AtomId.of("a1"), ElementSymbol.of("N"), null)),
            List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.DOUBLE)),
            MolecularCharge.NEUTRAL,
            SpinMultiplicity.SINGLET);

    final MoleculeGeometryRegularization regularization =
        MoleculeGeometryRegularizer.regularizeForElectronicPreparation(molecule);

    Assertions.assertTrue(regularization.regularized());
    Assertions.assertTrue(regularization.molecule().atoms().get(0).hasCoordinate());
    Assertions.assertTrue(regularization.molecule().atoms().get(1).hasCoordinate());
    Assertions.assertTrue(
        MoleculeGeometryRegularizerTest.distance(
                regularization.molecule().atoms().get(0).coordinate(),
                regularization.molecule().atoms().get(1).coordinate())
            > 0.20);
  }

  private static Molecule molecule(
      final String id, final Coordinate3D firstCoordinate, final Coordinate3D secondCoordinate) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(
            Atom.of(AtomId.of("a0"), ElementSymbol.of("H"), firstCoordinate),
            Atom.of(AtomId.of("a1"), ElementSymbol.of("H"), secondCoordinate)),
        List.of(Bond.of(AtomId.of("a0"), AtomId.of("a1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static double distance(final Coordinate3D first, final Coordinate3D second) {
    final double dx = first.unit().toAngstrom(first.x()) - second.unit().toAngstrom(second.x());
    final double dy = first.unit().toAngstrom(first.y()) - second.unit().toAngstrom(second.y());
    final double dz = first.unit().toAngstrom(first.z()) - second.unit().toAngstrom(second.z());
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
