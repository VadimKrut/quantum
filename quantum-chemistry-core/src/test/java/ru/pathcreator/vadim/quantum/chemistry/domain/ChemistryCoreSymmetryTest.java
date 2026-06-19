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
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.MolecularSymmetry;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.PointGroupName;

final class ChemistryCoreSymmetryTest {

  ChemistryCoreSymmetryTest() {}

  @Test
  void moleculeKeepsMolecularSymmetry() {
    final MolecularSymmetry symmetry =
        MolecularSymmetry.of((PointGroupName) PointGroupName.of((String) "C2v"), (int) 2);
    final Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "symmetry.water"),
            (String) "Symmetry probe",
            List.of(ChemistryCoreSymmetryTest.atom("o")),
            List.of(),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET,
            (Stereochemistry) Stereochemistry.EMPTY,
            null,
            (OpticalRotation) OpticalRotation.UNKNOWN,
            (MolecularSymmetry) symmetry,
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Assertions.assertEquals((Object) "C2v", (Object) molecule.symmetry().pointGroupName().value());
    Assertions.assertEquals((int) 2, (int) molecule.symmetry().symmetryNumber());
  }

  @Test
  void molecularSymmetryRejectsInvalidPointGroupOrSymmetryNumber() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> PointGroupName.of((String) "C2v!"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> MolecularSymmetry.of((PointGroupName) PointGroupName.of((String) "D3h"), (int) 0));
  }

  private static Atom atom(final String id) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) "O"),
        (Coordinate3D)
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }
}