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
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereocenterKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptorFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ChemistryCoreStereochemistryTest {

  ChemistryCoreStereochemistryTest() {}

  @Test
  void descriptorFamiliesKeepConfigurationGeometryAndRelativeNotationSeparate() {
    Assertions.assertEquals(
        (Object) StereochemicalDescriptorFamily.ABSOLUTE_TETRAHEDRAL_CONFIGURATION,
        (Object) StereochemicalDescriptor.R.family());
    Assertions.assertEquals(
        (Object) StereochemicalDescriptorFamily.GEOMETRIC_CONFIGURATION,
        (Object) StereochemicalDescriptor.E.family());
    Assertions.assertEquals(
        (Object) StereochemicalDescriptorFamily.GEOMETRIC_CONFIGURATION,
        (Object) StereochemicalDescriptor.CIS.family());
    Assertions.assertEquals(
        (Object) StereochemicalDescriptorFamily.RELATIVE_CONFIGURATION,
        (Object) StereochemicalDescriptor.D.family());
    Assertions.assertTrue((boolean) StereochemicalDescriptor.S.absoluteConfiguration());
    Assertions.assertTrue((boolean) StereochemicalDescriptor.Z.geometricConfiguration());
    Assertions.assertTrue((boolean) StereochemicalDescriptor.L.relativeConfiguration());
  }

  @Test
  void tetrahedralStereocenterRequiresFourOrderedLigands() {
    Stereocenter center =
        Stereocenter.ofTetrahedralAtom(
            (AtomId) AtomId.of((String) "c"),
            (StereochemicalDescriptor) StereochemicalDescriptor.R,
            (AtomId) AtomId.of((String) "f"),
            (AtomId) AtomId.of((String) "cl"),
            (AtomId) AtomId.of((String) "br"),
            (AtomId) AtomId.of((String) "h"));
    Assertions.assertEquals((Object) StereocenterKind.TETRAHEDRAL_ATOM, (Object) center.kind());
    Assertions.assertEquals((int) 4, (int) center.referenceAtomIds().size());
    Assertions.assertTrue((boolean) center.references(AtomId.of((String) "br")));
    Assertions.assertFalse((boolean) center.references(AtomId.of((String) "x")));
  }

  @Test
  void doubleBondStereocenterSupportsGeometricDescriptors() {
    final Stereocenter eCenter =
        Stereocenter.ofDoubleBond(
            (AtomId) AtomId.of((String) "c1"),
            (AtomId) AtomId.of((String) "c2"),
            (StereochemicalDescriptor) StereochemicalDescriptor.E,
            (AtomId) AtomId.of((String) "cl"),
            (AtomId) AtomId.of((String) "br"));
    final Stereocenter cisCenter =
        Stereocenter.ofDoubleBond(
            (AtomId) AtomId.of((String) "c1"),
            (AtomId) AtomId.of((String) "c2"),
            (StereochemicalDescriptor) StereochemicalDescriptor.CIS,
            (AtomId) AtomId.of((String) "h1"),
            (AtomId) AtomId.of((String) "h2"));
    Assertions.assertTrue((boolean) eCenter.hasSecondaryAtom());
    Assertions.assertEquals((Object) StereochemicalDescriptor.CIS, (Object) cisCenter.descriptor());
  }

  @Test
  void axialAndHelicalStereocentersUseDedicatedDescriptors() {
    final Stereocenter axial =
        Stereocenter.ofAxial(
            (AtomId) AtomId.of((String) "a1"),
            (AtomId) AtomId.of((String) "a2"),
            (StereochemicalDescriptor) StereochemicalDescriptor.R_A,
            (AtomId) AtomId.of((String) "x1"),
            (AtomId) AtomId.of((String) "x2"));
    final Stereocenter helical =
        Stereocenter.ofHelical(
            (AtomId) AtomId.of((String) "h1"),
            (AtomId) AtomId.of((String) "h4"),
            (StereochemicalDescriptor) StereochemicalDescriptor.P,
            List.of(AtomId.of((String) "h2"), AtomId.of((String) "h3"), AtomId.of((String) "h5")));
    Assertions.assertEquals((Object) StereocenterKind.AXIAL, (Object) axial.kind());
    Assertions.assertEquals((Object) StereocenterKind.HELICAL, (Object) helical.kind());
  }

  @Test
  void stereocenterRejectsDescriptorThatDoesNotBelongToKind() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Stereocenter.ofTetrahedralAtom(
                (AtomId) AtomId.of((String) "c"),
                (StereochemicalDescriptor) StereochemicalDescriptor.E,
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b"),
                (AtomId) AtomId.of((String) "d"),
                (AtomId) AtomId.of((String) "e")));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Stereocenter.ofDoubleBond(
                (AtomId) AtomId.of((String) "c1"),
                (AtomId) AtomId.of((String) "c2"),
                (StereochemicalDescriptor) StereochemicalDescriptor.R,
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b")));
  }

  @Test
  void stereocenterRejectsIncompleteOrAmbiguousReferenceFrame() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Stereocenter.of(
                (StereocenterKind) StereocenterKind.TETRAHEDRAL_ATOM,
                (StereochemicalDescriptor) StereochemicalDescriptor.R,
                (AtomId) AtomId.of((String) "c"),
                null,
                List.of(
                    AtomId.of((String) "a"), AtomId.of((String) "b"), AtomId.of((String) "d"))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Stereocenter.ofTetrahedralAtom(
                (AtomId) AtomId.of((String) "c"),
                (StereochemicalDescriptor) StereochemicalDescriptor.S,
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b"),
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "d")));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Stereocenter.ofTetrahedralAtom(
                (AtomId) AtomId.of((String) "c"),
                (StereochemicalDescriptor) StereochemicalDescriptor.S,
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b"),
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "d")));
  }

  @Test
  void stereochemistryRejectsDuplicateFullCenters() {
    final Stereocenter center =
        Stereocenter.ofTetrahedralAtom(
            (AtomId) AtomId.of((String) "c"),
            (StereochemicalDescriptor) StereochemicalDescriptor.R,
            (AtomId) AtomId.of((String) "f"),
            (AtomId) AtomId.of((String) "cl"),
            (AtomId) AtomId.of((String) "br"),
            (AtomId) AtomId.of((String) "h"));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> Stereochemistry.of(List.of(center, center)));
  }

  @Test
  void stereochemistryRejectsConflictingCentersOnSameLocus() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Stereochemistry.of(
                List.of(
                    Stereocenter.ofTetrahedralAtom(
                        (AtomId) AtomId.of((String) "c"),
                        (StereochemicalDescriptor) StereochemicalDescriptor.R,
                        (AtomId) AtomId.of((String) "f"),
                        (AtomId) AtomId.of((String) "cl"),
                        (AtomId) AtomId.of((String) "br"),
                        (AtomId) AtomId.of((String) "h")),
                    Stereocenter.ofTetrahedralAtom(
                        (AtomId) AtomId.of((String) "c"),
                        (StereochemicalDescriptor) StereochemicalDescriptor.S,
                        (AtomId) AtomId.of((String) "f"),
                        (AtomId) AtomId.of((String) "cl"),
                        (AtomId) AtomId.of((String) "br"),
                        (AtomId) AtomId.of((String) "h")))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Stereochemistry.of(
                List.of(
                    Stereocenter.ofDoubleBond(
                        (AtomId) AtomId.of((String) "c1"),
                        (AtomId) AtomId.of((String) "c2"),
                        (StereochemicalDescriptor) StereochemicalDescriptor.E,
                        (AtomId) AtomId.of((String) "h1"),
                        (AtomId) AtomId.of((String) "h2")),
                    Stereocenter.ofDoubleBond(
                        (AtomId) AtomId.of((String) "c2"),
                        (AtomId) AtomId.of((String) "c1"),
                        (StereochemicalDescriptor) StereochemicalDescriptor.Z,
                        (AtomId) AtomId.of((String) "h2"),
                        (AtomId) AtomId.of((String) "h1")))));
  }

  @Test
  void moleculeAcceptsStereoOnlyWhenReferenceFrameMatchesBonds() {
    Atom c1 = ChemistryCoreStereochemistryTest.atom("c1", "C", 0.0);
    Atom c2 = ChemistryCoreStereochemistryTest.atom("c2", "C", 1.0);
    Atom h1 = ChemistryCoreStereochemistryTest.atom("h1", "H", -1.0);
    Atom h2 = ChemistryCoreStereochemistryTest.atom("h2", "H", 2.0);
    final Stereochemistry stereochemistry =
        Stereochemistry.of(
            List.of(
                Stereocenter.ofDoubleBond(
                    (AtomId) c1.id(),
                    (AtomId) c2.id(),
                    (StereochemicalDescriptor) StereochemicalDescriptor.TRANS,
                    (AtomId) h1.id(),
                    (AtomId) h2.id())));
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "trans.ethene.probe"),
            (String) "Trans ethene probe",
            List.of(c1, c2, h1, h2),
            List.of(
                Bond.of((AtomId) c1.id(), (AtomId) c2.id(), (BondType) BondType.DOUBLE),
                Bond.of((AtomId) c1.id(), (AtomId) h1.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) c2.id(), (AtomId) h2.id(), (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET,
            (Stereochemistry) stereochemistry,
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Assertions.assertEquals((Object) stereochemistry, (Object) molecule.stereochemistry());
  }

  @Test
  void moleculeRejectsStereoReferenceFrameThatDoesNotMatchBonds() {
    final Atom c1 = ChemistryCoreStereochemistryTest.atom("c1", "C", 0.0);
    final Atom c2 = ChemistryCoreStereochemistryTest.atom("c2", "C", 1.0);
    Atom h1 = ChemistryCoreStereochemistryTest.atom("h1", "H", -1.0);
    Atom h2 = ChemistryCoreStereochemistryTest.atom("h2", "H", 2.0);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "bad.stereo.bond"),
                (String) "Bad stereo bond",
                List.of(c1, c2, h1, h2),
                List.of(
                    Bond.of((AtomId) c1.id(), (AtomId) c2.id(), (BondType) BondType.SINGLE),
                    Bond.of((AtomId) c1.id(), (AtomId) h1.id(), (BondType) BondType.SINGLE),
                    Bond.of((AtomId) c2.id(), (AtomId) h2.id(), (BondType) BondType.SINGLE)),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET,
                (Stereochemistry)
                    Stereochemistry.of(
                        List.of(
                            Stereocenter.ofDoubleBond(
                                (AtomId) c1.id(),
                                (AtomId) c2.id(),
                                (StereochemicalDescriptor) StereochemicalDescriptor.E,
                                (AtomId) h1.id(),
                                (AtomId) h2.id()))),
                (ChemistryMetadata) ChemistryMetadata.EMPTY));
  }

  @Test
  void moleculeValidatesAxialHelicalAndPlanarStereoReferenceFrames() {
    Atom a1 = ChemistryCoreStereochemistryTest.atom("a1", "C", 0.0);
    Atom a2 = ChemistryCoreStereochemistryTest.atom("a2", "C", 1.0);
    Atom r1 = ChemistryCoreStereochemistryTest.atom("r1", "H", -1.0);
    Atom r2 = ChemistryCoreStereochemistryTest.atom("r2", "H", 2.0);
    final Atom r3 = ChemistryCoreStereochemistryTest.atom("r3", "H", 3.0);
    final Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "stereo.extended_frames"),
            (String) "Extended stereo frames",
            List.of(a1, a2, r1, r2, r3),
            List.of(
                Bond.of((AtomId) a1.id(), (AtomId) a2.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) a1.id(), (AtomId) r1.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) a2.id(), (AtomId) r2.id(), (BondType) BondType.SINGLE),
                Bond.of((AtomId) a1.id(), (AtomId) r3.id(), (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET,
            (Stereochemistry)
                Stereochemistry.of(
                    List.of(
                        Stereocenter.ofAxial(
                            (AtomId) a1.id(),
                            (AtomId) a2.id(),
                            (StereochemicalDescriptor) StereochemicalDescriptor.R_A,
                            (AtomId) r1.id(),
                            (AtomId) r2.id()),
                        Stereocenter.ofPlanar(
                            (AtomId) a1.id(),
                            (StereochemicalDescriptor) StereochemicalDescriptor.S,
                            (AtomId) r1.id(),
                            (AtomId) r3.id(),
                            (AtomId) a2.id()))),
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Assertions.assertEquals((int) 2, (int) molecule.stereochemistry().centers().size());
  }

  @Test
  void moleculeRejectsDisconnectedAxialStereoFrame() {
    final Atom a1 = ChemistryCoreStereochemistryTest.atom("a1", "C", 0.0);
    final Atom a2 = ChemistryCoreStereochemistryTest.atom("a2", "C", 1.0);
    final Atom r1 = ChemistryCoreStereochemistryTest.atom("r1", "H", -1.0);
    final Atom r2 = ChemistryCoreStereochemistryTest.atom("r2", "H", 2.0);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "bad.axial.frame"),
                (String) "Bad axial frame",
                List.of(a1, a2, r1, r2),
                List.of(
                    Bond.of((AtomId) a1.id(), (AtomId) r1.id(), (BondType) BondType.SINGLE),
                    Bond.of((AtomId) a2.id(), (AtomId) r2.id(), (BondType) BondType.SINGLE)),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET,
                (Stereochemistry)
                    Stereochemistry.of(
                        List.of(
                            Stereocenter.ofAxial(
                                (AtomId) a1.id(),
                                (AtomId) a2.id(),
                                (StereochemicalDescriptor) StereochemicalDescriptor.S_A,
                                (AtomId) r1.id(),
                                (AtomId) r2.id()))),
                (ChemistryMetadata) ChemistryMetadata.EMPTY));
  }

  @Test
  void moleculeRejectsBrokenHelicalStereoPath() {
    final Atom h1 = ChemistryCoreStereochemistryTest.atom("h1", "C", 0.0);
    final Atom h2 = ChemistryCoreStereochemistryTest.atom("h2", "C", 1.0);
    final Atom h3 = ChemistryCoreStereochemistryTest.atom("h3", "C", 2.0);
    final Atom h4 = ChemistryCoreStereochemistryTest.atom("h4", "C", 3.0);
    final Atom h5 = ChemistryCoreStereochemistryTest.atom("h5", "C", 4.0);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                (MoleculeId) MoleculeId.of((String) "bad.helical.path"),
                (String) "Bad helical path",
                List.of(h1, h2, h3, h4, h5),
                List.of(
                    Bond.of((AtomId) h1.id(), (AtomId) h2.id(), (BondType) BondType.SINGLE),
                    Bond.of((AtomId) h2.id(), (AtomId) h3.id(), (BondType) BondType.SINGLE),
                    Bond.of((AtomId) h4.id(), (AtomId) h5.id(), (BondType) BondType.SINGLE)),
                (MolecularCharge) MolecularCharge.NEUTRAL,
                (SpinMultiplicity) SpinMultiplicity.SINGLET,
                (Stereochemistry)
                    Stereochemistry.of(
                        List.of(
                            Stereocenter.ofHelical(
                                (AtomId) h1.id(),
                                (AtomId) h5.id(),
                                (StereochemicalDescriptor) StereochemicalDescriptor.P,
                                List.of(h2.id(), h3.id(), h4.id())))),
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