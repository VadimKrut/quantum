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
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.MolecularConformation;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ChemistryCoreConformationTest {

  ChemistryCoreConformationTest() {}

  @Test
  void moleculeKeepsConformationAndRejectsUnknownTorsionAtoms() {
    final Atom first = ChemistryCoreConformationTest.atom("a1", "C", 0.0);
    final Atom second = ChemistryCoreConformationTest.atom("a2", "C", 1.0);
    final Atom third = ChemistryCoreConformationTest.atom("a3", "C", 2.0);
    final Atom fourth = ChemistryCoreConformationTest.atom("a4", "C", 3.0);
    final TorsionAngle torsionAngle =
        TorsionAngle.of(
            (AtomId) first.id(),
            (AtomId) second.id(),
            (AtomId) third.id(),
            (AtomId) fourth.id(),
            (double) -60.0);
    final MolecularConformation conformation = MolecularConformation.of(List.of(torsionAngle));
    final Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "conformation.probe"),
            (String) "Conformation probe",
            List.of(first, second, third, fourth),
            List.of(
                Bond.of((AtomId) first.id(), (AtomId) second.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) second.id(), (AtomId) third.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) third.id(), (AtomId) fourth.id(), (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET,
            (Stereochemistry) Stereochemistry.EMPTY,
            (MolecularConformation) conformation,
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Assertions.assertEquals(
        (double) -60.0,
        (double) ((TorsionAngle) molecule.conformation().torsionAngles().get(0)).degrees());
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> molecule.conformation().torsionAngles().clear());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            TorsionAngle.of(
                (AtomId) first.id(),
                (AtomId) second.id(),
                (AtomId) third.id(),
                (AtomId) fourth.id(),
                (double) 181.0));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            TorsionAngle.of(
                (AtomId) first.id(),
                (AtomId) first.id(),
                (AtomId) third.id(),
                (AtomId) fourth.id(),
                (double) 0.0));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "bad.conformation"),
                (String) "Bad conformation",
                List.of(first, second, third),
                List.of(),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET,
                (Stereochemistry) Stereochemistry.EMPTY,
                (MolecularConformation) conformation,
                (ChemistryMetadata) ChemistryMetadata.EMPTY));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "broken.torsion.path"),
                (String) "Broken torsion path",
                List.of(first, second, third, fourth),
                List.of(
                    Bond.of((AtomId) first.id(), (AtomId) second.id(), (BondType) BondType.SINGLE),
                    Bond.of((AtomId) third.id(), (AtomId) fourth.id(), (BondType) BondType.SINGLE)),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET,
                (Stereochemistry) Stereochemistry.EMPTY,
                (MolecularConformation) conformation,
                (ChemistryMetadata) ChemistryMetadata.EMPTY));
  }

  private static Atom atom(final String id, final String symbol, final double x) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of(
                (double) x, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }
}