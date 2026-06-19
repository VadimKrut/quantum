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
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostate;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostateKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.state.MolecularMicrostateSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreMicrostateTest {

  ChemistryCoreMicrostateTest() {}

  @Test
  void microstateSetAcceptsResonanceFormsWithSameFormulaChargeAndSpin() {
    MolecularMicrostateSet set =
        MolecularMicrostateSet.of(
            (String) "nitrite.resonance",
            List.of(
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                    (Molecule) ChemistryCoreMicrostateTest.nitrite("left", true),
                    (String) "left"),
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.RESONANCE_FORM,
                    (Molecule) ChemistryCoreMicrostateTest.nitrite("right", false),
                    (String) "right")));
    Assertions.assertEquals((Object) "left", (Object) set.reference().label());
    Assertions.assertEquals((int) 2, (int) set.states().size());
  }

  @Test
  void resonanceFormRejectsChangedChargeOrSpin() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularMicrostateSet.of(
                (String) "bad.resonance.charge",
                List.of(
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                        (Molecule) ChemistryCoreMicrostateTest.nitrite("left", true),
                        (String) "left"),
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.RESONANCE_FORM,
                        (Molecule)
                            ChemistryCoreMicrostateTest.nitriteWithState(
                                "wrong.charge",
                                false,
                                MolecularCharge.NEUTRAL,
                                SpinMultiplicity.SINGLET),
                        (String) "wrong"))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularMicrostateSet.of(
                (String) "bad.resonance.spin",
                List.of(
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                        (Molecule) ChemistryCoreMicrostateTest.nitrite("left", true),
                        (String) "left"),
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.RESONANCE_FORM,
                        (Molecule)
                            ChemistryCoreMicrostateTest.nitriteWithState(
                                "wrong.spin",
                                false,
                                MolecularCharge.of((int) -1),
                                SpinMultiplicity.of((int) 3)),
                        (String) "wrong"))));
  }

  @Test
  void microstateSetAcceptsTautomersWithSameFormulaAndCharge() {
    MolecularMicrostateSet set =
        MolecularMicrostateSet.of(
            (String) "c2h4o.tautomer",
            List.of(
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                    (Molecule) ChemistryCoreMicrostateTest.aldehyde("aldehyde"),
                    (String) "aldehyde"),
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.TAUTOMER,
                    (Molecule) ChemistryCoreMicrostateTest.enol("enol"),
                    (String) "enol")));
    Assertions.assertEquals((int) 2, (int) set.states().size());
    Assertions.assertEquals(
        (Object) set.reference().molecule().formula(),
        (Object) ((MolecularMicrostate) set.states().get(1)).molecule().formula());
  }

  @Test
  void tautomerRejectsFormulaOrChargeChange() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularMicrostateSet.of(
                (String) "bad.tautomer.formula",
                List.of(
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                        (Molecule) ChemistryCoreMicrostateTest.aldehyde("aldehyde"),
                        (String) "aldehyde"),
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.TAUTOMER,
                        (Molecule)
                            ChemistryCoreMicrostateTest.atomMolecule(
                                "carbon", "C", MolecularCharge.NEUTRAL),
                        (String) "bad"))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularMicrostateSet.of(
                (String) "bad.tautomer.charge",
                List.of(
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                        (Molecule) ChemistryCoreMicrostateTest.aldehyde("aldehyde"),
                        (String) "aldehyde"),
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.TAUTOMER,
                        (Molecule)
                            ChemistryCoreMicrostateTest.molecule(
                                "charged.enol",
                                ChemistryCoreMicrostateTest.enolAtoms(),
                                ChemistryCoreMicrostateTest.enolBonds(),
                                MolecularCharge.of((int) 1),
                                SpinMultiplicity.SINGLET),
                        (String) "bad"))));
  }

  @Test
  void microstateSetAcceptsIonizationStateWhenHydrogenAndChargeDeltasMatch() {
    MolecularMicrostateSet set =
        MolecularMicrostateSet.of(
            (String) "acetic.acid.ionization",
            List.of(
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                    (Molecule) ChemistryCoreMicrostateTest.aceticAcid("acid"),
                    (String) "acid"),
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.IONIZATION_STATE,
                    (Molecule) ChemistryCoreMicrostateTest.acetate("acetate"),
                    (String) "acetate")));
    Assertions.assertEquals(
        (int) -1, (int) ((MolecularMicrostate) set.states().get(1)).molecule().charge().value());
  }

  @Test
  void validatorWarnsWhenAcidBaseMicrostateSetHasNoEnvironment() {
    final MolecularMicrostateSet set =
        MolecularMicrostateSet.of(
            (String) "acetic.acid.ionization.no_environment",
            List.of(
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                    (Molecule) ChemistryCoreMicrostateTest.aceticAcid("acid"),
                    (String) "acid"),
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.IONIZATION_STATE,
                    (Molecule) ChemistryCoreMicrostateTest.acetate("acetate"),
                    (String) "acetate")));
    final ChemistryValidationResult result =
        new ChemistryCoreValidator().validateMolecularMicrostateSet(set);
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreMicrostateTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MICROSTATE_SET_HAS_NO_ENVIRONMENT,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void ionizationStateRejectsNonHydrogenFormulaChangeOrBrokenChargeCoupling() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularMicrostateSet.of(
                (String) "bad.ionization.element",
                List.of(
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                        (Molecule) ChemistryCoreMicrostateTest.aceticAcid("acid"),
                        (String) "acid"),
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.IONIZATION_STATE,
                        (Molecule)
                            ChemistryCoreMicrostateTest.atomMolecule(
                                "nitrogen", "N", MolecularCharge.of((int) -1)),
                        (String) "bad"))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularMicrostateSet.of(
                (String) "bad.ionization.charge",
                List.of(
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                        (Molecule) ChemistryCoreMicrostateTest.aceticAcid("acid"),
                        (String) "acid"),
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.IONIZATION_STATE,
                        (Molecule)
                            ChemistryCoreMicrostateTest.molecule(
                                "neutral.acetate",
                                ChemistryCoreMicrostateTest.acetateAtoms(),
                                ChemistryCoreMicrostateTest.acetateBonds(),
                                MolecularCharge.NEUTRAL,
                                SpinMultiplicity.SINGLET),
                        (String) "bad"))));
  }

  private static Molecule nitrite(final String id, final boolean leftDoubleBond) {
    return ChemistryCoreMicrostateTest.nitriteWithState(
        id, leftDoubleBond, MolecularCharge.of((int) -1), SpinMultiplicity.SINGLET);
  }

  private static Molecule nitriteWithState(
      final String id,
      final boolean leftDoubleBond,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity) {
    List<Atom> atoms =
        List.of(
            ChemistryCoreMicrostateTest.atom("n", "N"),
            ChemistryCoreMicrostateTest.atom("o1", "O"),
            ChemistryCoreMicrostateTest.atom("o2", "O"));
    final List<Bond> bonds =
        leftDoubleBond
            ? List.of(
                ChemistryCoreMicrostateTest.bond("n", "o1", BondType.DOUBLE),
                ChemistryCoreMicrostateTest.bond("n", "o2", BondType.SINGLE))
            : List.of(
                ChemistryCoreMicrostateTest.bond("n", "o1", BondType.SINGLE),
                ChemistryCoreMicrostateTest.bond("n", "o2", BondType.DOUBLE));
    return ChemistryCoreMicrostateTest.molecule(id, atoms, bonds, charge, spinMultiplicity);
  }

  private static Molecule aldehyde(final String id) {
    return ChemistryCoreMicrostateTest.molecule(
        id,
        ChemistryCoreMicrostateTest.aldehydeAtoms(),
        ChemistryCoreMicrostateTest.aldehydeBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static List<Atom> aldehydeAtoms() {
    return ChemistryCoreMicrostateTest.atoms("c1:C", "c2:C", "o:O", "h1:H", "h2:H", "h3:H", "h4:H");
  }

  private static List<Bond> aldehydeBonds() {
    return List.of(
        ChemistryCoreMicrostateTest.bond("c1", "c2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c2", "o", BondType.DOUBLE),
        ChemistryCoreMicrostateTest.bond("c1", "h1", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h3", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c2", "h4", BondType.SINGLE));
  }

  private static Molecule enol(final String id) {
    return ChemistryCoreMicrostateTest.molecule(
        id,
        ChemistryCoreMicrostateTest.enolAtoms(),
        ChemistryCoreMicrostateTest.enolBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static List<Atom> enolAtoms() {
    return ChemistryCoreMicrostateTest.atoms("c1:C", "c2:C", "o:O", "h1:H", "h2:H", "h3:H", "h4:H");
  }

  private static List<Bond> enolBonds() {
    return List.of(
        ChemistryCoreMicrostateTest.bond("c1", "c2", BondType.DOUBLE),
        ChemistryCoreMicrostateTest.bond("c2", "o", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("o", "h4", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h1", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c2", "h3", BondType.SINGLE));
  }

  private static Molecule aceticAcid(final String id) {
    return ChemistryCoreMicrostateTest.molecule(
        id,
        ChemistryCoreMicrostateTest.aceticAcidAtoms(),
        ChemistryCoreMicrostateTest.aceticAcidBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static List<Atom> aceticAcidAtoms() {
    return ChemistryCoreMicrostateTest.atoms(
        "c1:C", "c2:C", "o1:O", "o2:O", "h1:H", "h2:H", "h3:H", "h4:H");
  }

  private static List<Bond> aceticAcidBonds() {
    return List.of(
        ChemistryCoreMicrostateTest.bond("c1", "c2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c2", "o1", BondType.DOUBLE),
        ChemistryCoreMicrostateTest.bond("c2", "o2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("o2", "h4", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h1", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h3", BondType.SINGLE));
  }

  private static Molecule acetate(final String id) {
    return ChemistryCoreMicrostateTest.molecule(
        id,
        ChemistryCoreMicrostateTest.acetateAtoms(),
        ChemistryCoreMicrostateTest.acetateBonds(),
        MolecularCharge.of((int) -1),
        SpinMultiplicity.SINGLET);
  }

  private static List<Atom> acetateAtoms() {
    return ChemistryCoreMicrostateTest.atoms(
        "c1:C", "c2:C", "o1:O", "o2:O", "h1:H", "h2:H", "h3:H");
  }

  private static List<Bond> acetateBonds() {
    return List.of(
        ChemistryCoreMicrostateTest.bond("c1", "c2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c2", "o1", BondType.DOUBLE),
        ChemistryCoreMicrostateTest.bond("c2", "o2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h1", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h2", BondType.SINGLE),
        ChemistryCoreMicrostateTest.bond("c1", "h3", BondType.SINGLE));
  }

  private static Molecule atomMolecule(final String id, final String symbol, final MolecularCharge charge) {
    return ChemistryCoreMicrostateTest.molecule(
        id,
        List.of(ChemistryCoreMicrostateTest.atom("atom", symbol)),
        List.of(),
        charge,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule molecule(
      final String id,
      final List<Atom> atoms,
      final List<Bond> bonds,
      final MolecularCharge charge,
      final SpinMultiplicity spinMultiplicity) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        atoms,
        bonds,
        (MolecularCharge) charge,
        (SpinMultiplicity) spinMultiplicity);
  }

  private static List<Atom> atoms(final String... specs) {
    final ArrayList<Atom> atoms = new ArrayList<Atom>();
    for (int i = 0; i < specs.length; ++i) {
      final String spec = specs[i];
      final int separator = spec.indexOf(58);
      atoms.add(
          ChemistryCoreMicrostateTest.atom(
              spec.substring(0, separator), spec.substring(separator + 1)));
    }
    return List.copyOf(atoms);
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D) ChemistryCoreMicrostateTest.coordinate());
  }

  private static Bond bond(final String first, final String second, final BondType type) {
    return Bond.of(
        (AtomId) AtomId.of((String) first), (AtomId) AtomId.of((String) second), (BondType) type);
  }

  private static Coordinate3D coordinate() {
    return Coordinate3D.of(
        (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM);
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