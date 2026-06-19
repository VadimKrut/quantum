/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.MolecularValence;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.MolecularValenceAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.domain.valence.ValenceRule;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreValenceTest {

  ChemistryCoreValenceTest() {}

  @Test
  void bondTypeExposesDefinedNumericalOrder() {
    Assertions.assertEquals((double) 1.0, (double) BondType.SINGLE.orderValue());
    Assertions.assertEquals((double) 2.0, (double) BondType.DOUBLE.orderValue());
    Assertions.assertEquals((double) 3.0, (double) BondType.TRIPLE.orderValue());
    Assertions.assertEquals((double) 1.5, (double) BondType.AROMATIC.orderValue());
    Assertions.assertEquals((double) 1.0, (double) BondType.COORDINATE.orderValue());
    Assertions.assertFalse((boolean) BondType.UNKNOWN.hasDefinedOrder());
    Assertions.assertThrows(IllegalStateException.class, () -> BondType.UNKNOWN.orderValue());
  }

  @Test
  void molecularValenceSumsBondOrdersPerAtom() {
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "valence.formaldehyde"),
            (String) "Formaldehyde valence",
            List.of(
                ChemistryCoreValenceTest.atom("c", "C"),
                ChemistryCoreValenceTest.atom("o", "O"),
                ChemistryCoreValenceTest.atom("h1", "H"),
                ChemistryCoreValenceTest.atom("h2", "H")),
            List.of(
                ChemistryCoreValenceTest.bond("c", "o", BondType.DOUBLE),
                ChemistryCoreValenceTest.bond("c", "h1", BondType.SINGLE),
                ChemistryCoreValenceTest.bond("c", "h2", BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    final MolecularValence valence = MolecularValenceAnalyzer.analyze((Molecule) molecule);
    Assertions.assertEquals((Object) valence, (Object) molecule.valence());
    Assertions.assertEquals(
        (double) 4.0, (double) valence.atomValenceOf(AtomId.of((String) "c")).bondOrderSum());
    Assertions.assertEquals(
        (double) 2.0, (double) valence.atomValenceOf(AtomId.of((String) "o")).bondOrderSum());
    Assertions.assertEquals(
        (int) 3, (int) valence.atomValenceOf(AtomId.of((String) "c")).bondCount());
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> valence.atomValences().clear());
  }

  @Test
  void valenceRuleKeepsElementAndPositiveMaximumBondOrder() {
    final ValenceRule carbonRule =
        ValenceRule.of((ElementSymbol) ElementSymbol.of((String) "C"), (double) 4.0);
    Assertions.assertEquals((Object) ElementSymbol.of((String) "C"), (Object) carbonRule.symbol());
    Assertions.assertEquals((double) 4.0, (double) carbonRule.maximumBondOrderSum());
    Assertions.assertEquals(
        (Object) carbonRule,
        (Object) ValenceRule.of((ElementSymbol) ElementSymbol.of((String) "C"), (double) 4.0));
    Assertions.assertEquals(
        (int) carbonRule.hashCode(),
        (int)
            ValenceRule.of((ElementSymbol) ElementSymbol.of((String) "C"), (double) 4.0)
                .hashCode());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ValenceRule.of((ElementSymbol) ElementSymbol.of((String) "C"), (double) 0.0));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ValenceRule.of(null, (double) 4.0));
  }

  @Test
  void validatorWarnsAboutUnknownBondOrder() {
    Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "valence.unknown"),
            (String) "Unknown bond order",
            List.of(
                ChemistryCoreValenceTest.atom("a", "C"), ChemistryCoreValenceTest.atom("b", "C")),
            List.of(ChemistryCoreValenceTest.bond("a", "b", BondType.UNKNOWN)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    ChemistryValidationResult result = new ChemistryCoreValidator().validateMolecule(molecule);
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreValenceTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULE_UNKNOWN_BOND_ORDER,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void validatorWarnsWhenCommonValenceProfileIsExceeded() {
    final ArrayList<Atom> atoms = new ArrayList<Atom>();
    atoms.add(ChemistryCoreValenceTest.atom("c", "C"));
    final ArrayList<Bond> bonds = new ArrayList<Bond>();
    for (int i = 0; i < 5; ++i) {
      final String hydrogenId = "h" + i;
      atoms.add(ChemistryCoreValenceTest.atom(hydrogenId, "H"));
      bonds.add(ChemistryCoreValenceTest.bond("c", hydrogenId, BondType.SINGLE));
    }
    final Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "valence.exceeded"),
            (String) "Exceeded valence",
            atoms,
            bonds,
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.of((int) 2));
    final ChemistryValidationResult result = new ChemistryCoreValidator().validateMolecule(molecule);
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreValenceTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULE_VALENCE_EXCEEDS_PROFILE,
                ChemistryDiagnosticSeverity.WARNING));
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }

  private static Bond bond(final String firstAtomId, final String secondAtomId, final BondType type) {
    return Bond.of(
        (AtomId) AtomId.of((String) firstAtomId),
        (AtomId) AtomId.of((String) secondAtomId),
        (BondType) type);
  }

  private static boolean contains(
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticCode code,
      final ChemistryDiagnosticSeverity severity) {
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i).code() != code || diagnostics.get(i).severity() != severity) continue;
      return true;
    }
    return false;
  }
}