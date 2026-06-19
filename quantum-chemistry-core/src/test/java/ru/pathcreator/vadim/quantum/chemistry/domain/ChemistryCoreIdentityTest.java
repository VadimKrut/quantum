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
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularComparisonResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentityComparator;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularRelationshipKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ChemistryCoreIdentityTest {

  ChemistryCoreIdentityTest() {}

  @Test
  void comparatorRecognizesSameMoleculeWithDifferentAtomIdsAndOrder() {
    MolecularComparisonResult result =
        MolecularIdentityComparator.compare(
            (Molecule) ChemistryCoreIdentityTest.water("left", "o", "h1", "h2"),
            (Molecule)
                ChemistryCoreIdentityTest.water("right", "oxygen", "hydrogen_b", "hydrogen_a"));
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.SAME, (Object) result.relationshipKind());
    Assertions.assertTrue((boolean) result.sameFormula());
    Assertions.assertTrue((boolean) result.sameConnectivity());
    Assertions.assertTrue((boolean) result.sameStereochemistry());
    Assertions.assertTrue((boolean) result.sameConformation());
  }

  @Test
  void comparatorDoesNotDependOnSymmetricAtomMappingOrder() {
    MolecularComparisonResult result =
        MolecularIdentityComparator.compare(
            (Molecule) ChemistryCoreIdentityTest.methane("methane.left", "h1", "h2", "h3", "h4"),
            (Molecule) ChemistryCoreIdentityTest.methane("methane.right", "d", "c", "b", "a"));
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.SAME, (Object) result.relationshipKind());
    Assertions.assertTrue((boolean) result.sameConnectivity());
  }

  @Test
  void comparatorDistinguishesConstitutionalIsomers() {
    MolecularComparisonResult result =
        MolecularIdentityComparator.compare(
            (Molecule) ChemistryCoreIdentityTest.heavyAtomChain("ethanol.skeleton", "C", "C", "O"),
            (Molecule)
                ChemistryCoreIdentityTest.heavyAtomChain("dimethyl_ether.skeleton", "C", "O", "C"));
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.CONSTITUTIONAL_ISOMER,
        (Object) result.relationshipKind());
    Assertions.assertTrue((boolean) result.sameFormula());
    Assertions.assertFalse((boolean) result.sameConnectivity());
  }

  @Test
  void comparatorDistinguishesElectronicStateFromConstitution() {
    final Molecule neutral =
        ChemistryCoreIdentityTest.chargedAtomPair(
            "neutral.pair", FormalCharge.NEUTRAL, MolecularCharge.NEUTRAL);
    final Molecule cationic =
        ChemistryCoreIdentityTest.chargedAtomPair(
            "charged.pair", FormalCharge.of((int) 1), MolecularCharge.of((int) 1));
    MolecularComparisonResult result =
        MolecularIdentityComparator.compare((Molecule) neutral, (Molecule) cationic);
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.SAME_FORMULA_DIFFERENT_ELECTRONIC_STATE,
        (Object) result.relationshipKind());
    Assertions.assertTrue((boolean) result.sameFormula());
    Assertions.assertFalse((boolean) result.sameConnectivity());
  }

  @Test
  void comparatorRecognizesEnantiomers() {
    MolecularComparisonResult result =
        MolecularIdentityComparator.compare(
            (Molecule)
                ChemistryCoreIdentityTest.chiralSingleCenter(
                    "r.center", StereochemicalDescriptor.R),
            (Molecule)
                ChemistryCoreIdentityTest.chiralSingleCenter(
                    "s.center", StereochemicalDescriptor.S));
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.ENANTIOMER, (Object) result.relationshipKind());
    Assertions.assertTrue((boolean) result.sameConnectivity());
    Assertions.assertFalse((boolean) result.sameStereochemistry());
    Assertions.assertTrue((boolean) result.enantiomeric());
  }

  @Test
  void comparatorRecognizesDiastereomers() {
    MolecularComparisonResult result =
        MolecularIdentityComparator.compare(
            (Molecule)
                ChemistryCoreIdentityTest.twoCenterMolecule(
                    "rr", StereochemicalDescriptor.R, StereochemicalDescriptor.R),
            (Molecule)
                ChemistryCoreIdentityTest.twoCenterMolecule(
                    "rs", StereochemicalDescriptor.R, StereochemicalDescriptor.S));
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.DIASTEREOMER, (Object) result.relationshipKind());
    Assertions.assertTrue((boolean) result.sameConnectivity());
    Assertions.assertFalse((boolean) result.sameStereochemistry());
    Assertions.assertFalse((boolean) result.enantiomeric());
  }

  @Test
  void comparatorRecognizesConformers() {
    final MolecularComparisonResult result =
        MolecularIdentityComparator.compare(
            (Molecule) ChemistryCoreIdentityTest.chainWithTorsion("gauche", 60.0),
            (Molecule) ChemistryCoreIdentityTest.chainWithTorsion("anti", 180.0));
    Assertions.assertEquals(
        (Object) MolecularRelationshipKind.CONFORMER, (Object) result.relationshipKind());
    Assertions.assertTrue((boolean) result.sameConnectivity());
    Assertions.assertTrue((boolean) result.sameStereochemistry());
    Assertions.assertFalse((boolean) result.sameConformation());
  }

  private static Molecule water(
      final String id, final String oxygenId, final String firstHydrogenId, final String secondHydrogenId) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) "Water",
        List.of(
            ChemistryCoreIdentityTest.atom(firstHydrogenId, "H"),
            ChemistryCoreIdentityTest.atom(oxygenId, "O"),
            ChemistryCoreIdentityTest.atom(secondHydrogenId, "H")),
        List.of(
            ChemistryCoreIdentityTest.bond(oxygenId, firstHydrogenId),
            ChemistryCoreIdentityTest.bond(oxygenId, secondHydrogenId)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule methane(
      final String id,
      final String firstHydrogenId,
      final String secondHydrogenId,
      final String thirdHydrogenId,
      final String fourthHydrogenId) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) "Methane",
        List.of(
            ChemistryCoreIdentityTest.atom("carbon", "C"),
            ChemistryCoreIdentityTest.atom(firstHydrogenId, "H"),
            ChemistryCoreIdentityTest.atom(secondHydrogenId, "H"),
            ChemistryCoreIdentityTest.atom(thirdHydrogenId, "H"),
            ChemistryCoreIdentityTest.atom(fourthHydrogenId, "H")),
        List.of(
            ChemistryCoreIdentityTest.bond("carbon", firstHydrogenId),
            ChemistryCoreIdentityTest.bond("carbon", secondHydrogenId),
            ChemistryCoreIdentityTest.bond("carbon", thirdHydrogenId),
            ChemistryCoreIdentityTest.bond("carbon", fourthHydrogenId)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule heavyAtomChain(
      final String id, final String firstSymbol, final String secondSymbol, final String thirdSymbol) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreIdentityTest.atom("a", firstSymbol),
            ChemistryCoreIdentityTest.atom("b", secondSymbol),
            ChemistryCoreIdentityTest.atom("c", thirdSymbol)),
        List.of(ChemistryCoreIdentityTest.bond("a", "b"), ChemistryCoreIdentityTest.bond("b", "c")),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule chargedAtomPair(
      final String id, final FormalCharge carbonCharge, final MolecularCharge molecularCharge) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            Atom.of(
                (AtomId) AtomId.of((String) "c"),
                (ElementSymbol) ElementSymbol.of((String) "C"),
                (Coordinate3D) ChemistryCoreIdentityTest.coordinate(),
                (FormalCharge) carbonCharge,
                null,
                (RadicalState) RadicalState.CLOSED_SHELL,
                (ChemistryMetadata) ChemistryMetadata.EMPTY),
            ChemistryCoreIdentityTest.atom("o", "O")),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "o"),
                (BondType) BondType.DOUBLE)),
        (MolecularCharge) molecularCharge,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule chiralSingleCenter(final String id, final StereochemicalDescriptor descriptor) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreIdentityTest.atom("c", "C"),
            ChemistryCoreIdentityTest.atom("f", "F"),
            ChemistryCoreIdentityTest.atom("cl", "Cl"),
            ChemistryCoreIdentityTest.atom("br", "Br"),
            ChemistryCoreIdentityTest.atom("h", "H")),
        List.of(
            ChemistryCoreIdentityTest.bond("c", "f"),
            ChemistryCoreIdentityTest.bond("c", "cl"),
            ChemistryCoreIdentityTest.bond("c", "br"),
            ChemistryCoreIdentityTest.bond("c", "h")),
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

  private static Molecule twoCenterMolecule(
      final String id,
      final StereochemicalDescriptor firstDescriptor,
      final StereochemicalDescriptor secondDescriptor) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreIdentityTest.atom("c1", "C"),
            ChemistryCoreIdentityTest.atom("c2", "C"),
            ChemistryCoreIdentityTest.atom("f", "F"),
            ChemistryCoreIdentityTest.atom("cl", "Cl"),
            ChemistryCoreIdentityTest.atom("br", "Br"),
            ChemistryCoreIdentityTest.atom("o", "O"),
            ChemistryCoreIdentityTest.atom("n", "N"),
            ChemistryCoreIdentityTest.atom("s", "S")),
        List.of(
            ChemistryCoreIdentityTest.bond("c1", "c2"),
            ChemistryCoreIdentityTest.bond("c1", "f"),
            ChemistryCoreIdentityTest.bond("c1", "cl"),
            ChemistryCoreIdentityTest.bond("c1", "br"),
            ChemistryCoreIdentityTest.bond("c2", "o"),
            ChemistryCoreIdentityTest.bond("c2", "n"),
            ChemistryCoreIdentityTest.bond("c2", "s")),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET,
        (Stereochemistry)
            Stereochemistry.of(
                List.of(
                    Stereocenter.ofTetrahedralAtom(
                        (AtomId) AtomId.of((String) "c1"),
                        (StereochemicalDescriptor) firstDescriptor,
                        (AtomId) AtomId.of((String) "f"),
                        (AtomId) AtomId.of((String) "cl"),
                        (AtomId) AtomId.of((String) "br"),
                        (AtomId) AtomId.of((String) "c2")),
                    Stereocenter.ofTetrahedralAtom(
                        (AtomId) AtomId.of((String) "c2"),
                        (StereochemicalDescriptor) secondDescriptor,
                        (AtomId) AtomId.of((String) "o"),
                        (AtomId) AtomId.of((String) "n"),
                        (AtomId) AtomId.of((String) "s"),
                        (AtomId) AtomId.of((String) "c1")))),
        (ChemistryMetadata) ChemistryMetadata.EMPTY);
  }

  private static Molecule chainWithTorsion(final String id, final double torsionDegrees) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreIdentityTest.atom("c1", "C"),
            ChemistryCoreIdentityTest.atom("c2", "C"),
            ChemistryCoreIdentityTest.atom("c3", "C"),
            ChemistryCoreIdentityTest.atom("c4", "C")),
        List.of(
            ChemistryCoreIdentityTest.bond("c1", "c2"),
            ChemistryCoreIdentityTest.bond("c2", "c3"),
            ChemistryCoreIdentityTest.bond("c3", "c4")),
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

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D) ChemistryCoreIdentityTest.coordinate());
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