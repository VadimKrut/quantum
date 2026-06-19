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
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularRelationshipKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.isomer.MolecularVariant;
import ru.pathcreator.vadim.quantum.chemistry.domain.isomer.MolecularVariantKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.isomer.MolecularVariantRelation;
import ru.pathcreator.vadim.quantum.chemistry.domain.isomer.MolecularVariantSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ChemistryCoreIsomerTest {

  ChemistryCoreIsomerTest() {}

  @Test
  void variantSetKeepsOneFormulaAndClassifiesInternalRelations() {
    MolecularVariantSet set =
        MolecularVariantSet.of(
            (String) "c4.skeleton.variants",
            List.of(
                MolecularVariant.of(
                    (MolecularVariantKind) MolecularVariantKind.REFERENCE,
                    (Molecule) ChemistryCoreIsomerTest.chain("butane.anti", 180.0),
                    (String) "anti"),
                MolecularVariant.of(
                    (MolecularVariantKind) MolecularVariantKind.CONFORMER,
                    (Molecule) ChemistryCoreIsomerTest.chain("butane.gauche", 60.0),
                    (String) "gauche"),
                MolecularVariant.of(
                    (MolecularVariantKind) MolecularVariantKind.CONSTITUTIONAL_ISOMER,
                    (Molecule) ChemistryCoreIsomerTest.branched("isobutane"),
                    (String) "isobutane")));
    final List relations = set.relations();
    Assertions.assertEquals((int) 3, (int) relations.size());
    Assertions.assertEquals(
        (Object) ChemistryCoreIsomerTest.chain("formula.probe", 180.0).formula(),
        (Object) set.formula());
    Assertions.assertEquals((Object) "anti", (Object) set.referenceVariant().label());
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.CONFORMER,
        (Object) ((MolecularVariantRelation) relations.get(0)).comparison().relationshipKind());
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.CONSTITUTIONAL_ISOMER,
        (Object) ((MolecularVariantRelation) relations.get(1)).comparison().relationshipKind());
  }

  @Test
  void variantSetAcceptsEnantiomericPairWithoutConfusingItWithConformer() {
    final MolecularVariantSet set =
        MolecularVariantSet.of(
            (String) "chiral.variants",
            List.of(
                MolecularVariant.of(
                    (MolecularVariantKind) MolecularVariantKind.REFERENCE,
                    (Molecule)
                        ChemistryCoreIsomerTest.chiral("r.center", StereochemicalDescriptor.R),
                    (String) "r"),
                MolecularVariant.of(
                    (MolecularVariantKind) MolecularVariantKind.ENANTIOMER,
                    (Molecule)
                        ChemistryCoreIsomerTest.chiral("s.center", StereochemicalDescriptor.S),
                    (String) "s")));
    final MolecularVariantRelation relation = (MolecularVariantRelation) set.relations().get(0);
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.ENANTIOMER,
        (Object) relation.comparison().relationshipKind());
    Assertions.assertTrue((boolean) relation.comparison().enantiomeric());
    Assertions.assertFalse((boolean) relation.comparison().sameConformation());
  }

  @Test
  void variantSetRejectsDifferentFormulaAndAmbiguousMembers() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularVariantSet.of(
                (String) "mixed.formula",
                List.of(
                    MolecularVariant.of(
                        (MolecularVariantKind) MolecularVariantKind.REFERENCE,
                        (Molecule) ChemistryCoreIsomerTest.chain("butane", 180.0),
                        (String) "butane"),
                    MolecularVariant.of(
                        (MolecularVariantKind) MolecularVariantKind.UNSPECIFIED,
                        (Molecule) ChemistryCoreIsomerTest.atomMolecule("methane", "C"),
                        (String) "methane"))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularVariantSet.of(
                (String) "duplicate.label",
                List.of(
                    MolecularVariant.of(
                        (MolecularVariantKind) MolecularVariantKind.REFERENCE,
                        (Molecule) ChemistryCoreIsomerTest.chain("first", 180.0),
                        (String) "same"),
                    MolecularVariant.of(
                        (MolecularVariantKind) MolecularVariantKind.CONFORMER,
                        (Molecule) ChemistryCoreIsomerTest.chain("second", 60.0),
                        (String) "same"))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularVariantSet.of(
                (String) "no.reference",
                List.of(
                    MolecularVariant.of(
                        (MolecularVariantKind) MolecularVariantKind.CONFORMER,
                        (Molecule) ChemistryCoreIsomerTest.chain("first", 180.0),
                        (String) "first"))));
  }

  private static Molecule chain(final String id, final double torsionDegrees) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreIsomerTest.atom("c1", "C"),
            ChemistryCoreIsomerTest.atom("c2", "C"),
            ChemistryCoreIsomerTest.atom("c3", "C"),
            ChemistryCoreIsomerTest.atom("c4", "C")),
        List.of(
            ChemistryCoreIsomerTest.bond("c1", "c2"),
            ChemistryCoreIsomerTest.bond("c2", "c3"),
            ChemistryCoreIsomerTest.bond("c3", "c4")),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET,
        (Stereochemistry) Stereochemistry.EMPTY,
        (MolecularConformation)
            MolecularConformation.of(
                List.of(
                    TorsionAngle.of(
                        (AtomId) AtomId.of((String) "c1"),
                        (AtomId) AtomId.of((String) "c2"),
                        (AtomId) AtomId.of((String) "c3"),
                        (AtomId) AtomId.of((String) "c4"),
                        (double) torsionDegrees))),
        (ChemistryMetadata) ChemistryMetadata.EMPTY);
  }

  private static Molecule branched(final String id) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreIsomerTest.atom("center", "C"),
            ChemistryCoreIsomerTest.atom("c1", "C"),
            ChemistryCoreIsomerTest.atom("c2", "C"),
            ChemistryCoreIsomerTest.atom("c3", "C")),
        List.of(
            ChemistryCoreIsomerTest.bond("center", "c1"),
            ChemistryCoreIsomerTest.bond("center", "c2"),
            ChemistryCoreIsomerTest.bond("center", "c3")),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule chiral(final String id, final StereochemicalDescriptor descriptor) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreIsomerTest.atom("c", "C"),
            ChemistryCoreIsomerTest.atom("f", "F"),
            ChemistryCoreIsomerTest.atom("cl", "Cl"),
            ChemistryCoreIsomerTest.atom("br", "Br"),
            ChemistryCoreIsomerTest.atom("h", "H")),
        List.of(
            ChemistryCoreIsomerTest.bond("c", "f"),
            ChemistryCoreIsomerTest.bond("c", "cl"),
            ChemistryCoreIsomerTest.bond("c", "br"),
            ChemistryCoreIsomerTest.bond("c", "h")),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET,
        (Stereochemistry)
            Stereochemistry.of(
                List.of(
                    Stereocenter.ofTetrahedralAtom(
                        (AtomId) AtomId.of((String) "c"),
                        (StereochemicalDescriptor) descriptor,
                        (AtomId) AtomId.of((String) "f"),
                        (AtomId) AtomId.of((String) "cl"),
                        (AtomId) AtomId.of((String) "br"),
                        (AtomId) AtomId.of((String) "h")))),
        (ChemistryMetadata) ChemistryMetadata.EMPTY);
  }

  private static Molecule atomMolecule(final String id, final String symbol) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(ChemistryCoreIsomerTest.atom("atom", symbol)),
        List.of(),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D) ChemistryCoreIsomerTest.coordinate());
  }

  private static Bond bond(final String first, final String second) {
    return Bond.of(
        (AtomId) AtomId.of((String) first),
        (AtomId) AtomId.of((String) second),
        (BondType) BondType.SINGLE);
  }

  private static Coordinate3D coordinate() {
    return Coordinate3D.of(
        (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM);
  }
}