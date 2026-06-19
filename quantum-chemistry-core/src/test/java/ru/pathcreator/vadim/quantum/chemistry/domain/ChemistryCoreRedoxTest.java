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
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.redox.ElectronTransferTransition;
import ru.pathcreator.vadim.quantum.chemistry.domain.redox.RedoxCenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.redox.RedoxCenterKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.redox.RedoxModel;
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

final class ChemistryCoreRedoxTest {

  ChemistryCoreRedoxTest() {}

  @Test
  void redoxModelAcceptsOneElectronOxidationWithoutFormulaChange() {
    RedoxModel model =
        RedoxModel.of(
            (String) "oxygen.redox",
            (MolecularMicrostateSet) ChemistryCoreRedoxTest.microstates(),
            List.of(
                ElectronTransferTransition.of(
                    (RedoxCenter) ChemistryCoreRedoxTest.oxygenBondCenter(),
                    (String) "superoxide",
                    (String) "oxygen",
                    (int) 1,
                    java.lang.Double.valueOf(-0.33))));
    Assertions.assertEquals((Object) "oxygen.redox", (Object) model.id());
    Assertions.assertEquals(
        (int) 1, (int) ((ElectronTransferTransition) model.transitions().get(0)).electronCount());
    Assertions.assertTrue(
        (boolean) ((ElectronTransferTransition) model.transitions().get(0)).hasFormalPotential());
  }

  @Test
  void validatorWarnsWhenRedoxModelHasNoFormalPotentials() {
    final RedoxModel model =
        RedoxModel.of(
            (String) "oxygen.redox.no_potential",
            (MolecularMicrostateSet) ChemistryCoreRedoxTest.microstates(),
            List.of(
                ElectronTransferTransition.of(
                    (RedoxCenter) ChemistryCoreRedoxTest.oxygenBondCenter(),
                    (String) "superoxide",
                    (String) "oxygen",
                    (int) 1,
                  null)));
    final ChemistryValidationResult result = new ChemistryCoreValidator().validateRedoxModel(model);
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreRedoxTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REDOX_MODEL_HAS_NO_FORMAL_POTENTIALS,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void redoxCenterValidatesAtomCountAndUniqueness() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            RedoxCenter.of(
                (String) "bad bond",
                (RedoxCenterKind) RedoxCenterKind.BOND,
                List.of(AtomId.of((String) "o1"))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            RedoxCenter.of(
                (String) "duplicate",
                (RedoxCenterKind) RedoxCenterKind.CLUSTER,
                List.of(AtomId.of((String) "o1"), AtomId.of((String) "o1"))));
    Assertions.assertEquals(
        (int) 0, (int) RedoxCenter.wholeMolecule((String) "whole").atomIds().size());
  }

  @Test
  void redoxModelRejectsUnknownStateLabelsDuplicateTransitionsAndMissingCenterAtom() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            RedoxModel.of(
                (String) "unknown",
                (MolecularMicrostateSet) ChemistryCoreRedoxTest.microstates(),
                List.of(
                    ElectronTransferTransition.of(
                        (RedoxCenter) ChemistryCoreRedoxTest.oxygenBondCenter(),
                        (String) "missing",
                        (String) "oxygen",
                        (int) 1,
                        null))));
    final ElectronTransferTransition transition =
        ElectronTransferTransition.of(
            (RedoxCenter) ChemistryCoreRedoxTest.oxygenBondCenter(),
            (String) "superoxide",
            (String) "oxygen",
            (int) 1,
            null);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            RedoxModel.of(
                (String) "duplicate",
                (MolecularMicrostateSet) ChemistryCoreRedoxTest.microstates(),
                List.of(transition, transition)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            RedoxModel.of(
                (String) "missing.center",
                (MolecularMicrostateSet) ChemistryCoreRedoxTest.microstates(),
                List.of(
                    ElectronTransferTransition.of(
                        (RedoxCenter)
                            RedoxCenter.of(
                                (String) "missing atom",
                                (RedoxCenterKind) RedoxCenterKind.ATOM,
                                List.of(AtomId.of((String) "missing"))),
                        (String) "superoxide",
                        (String) "oxygen",
                        (int) 1,
                        null))));
  }

  @Test
  void redoxModelRejectsWrongChargeDeltaOrFormulaChange() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            RedoxModel.of(
                (String) "wrong.electrons",
                (MolecularMicrostateSet) ChemistryCoreRedoxTest.microstates(),
                List.of(
                    ElectronTransferTransition.of(
                        (RedoxCenter) ChemistryCoreRedoxTest.oxygenBondCenter(),
                        (String) "superoxide",
                        (String) "oxygen",
                        (int) 2,
                        null))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            RedoxModel.of(
                (String) "formula.change",
                (MolecularMicrostateSet)
                    MolecularMicrostateSet.of(
                        (String) "bad.redox.set",
                        List.of(
                            MolecularMicrostate.of(
                                (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                                (Molecule)
                                    ChemistryCoreRedoxTest.oxygen(
                                        "oxygen", MolecularCharge.NEUTRAL),
                                (String) "oxygen"),
                            MolecularMicrostate.of(
                                (MolecularMicrostateKind) MolecularMicrostateKind.REDOX_STATE,
                                (Molecule)
                                    ChemistryCoreRedoxTest.atomMolecule(
                                        "oxide", "O", MolecularCharge.of((int) -1)),
                                (String) "oxide"))),
                List.of(
                    ElectronTransferTransition.of(
                        (RedoxCenter) RedoxCenter.wholeMolecule((String) "whole"),
                        (String) "oxide",
                        (String) "oxygen",
                        (int) 1,
                        null))));
  }

  @Test
  void electronTransferTransitionRejectsInvalidNumerics() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronTransferTransition.of(
                (RedoxCenter) ChemistryCoreRedoxTest.oxygenBondCenter(),
                (String) "a",
                (String) "b",
                (int) 0,
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronTransferTransition.of(
                (RedoxCenter) ChemistryCoreRedoxTest.oxygenBondCenter(),
                (String) "a",
                (String) "a",
                (int) 1,
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronTransferTransition.of(
                (RedoxCenter) ChemistryCoreRedoxTest.oxygenBondCenter(),
                (String) "a",
                (String) "b",
                (int) 1,
                java.lang.Double.valueOf(java.lang.Double.NaN)));
  }

  private static MolecularMicrostateSet microstates() {
    return MolecularMicrostateSet.of(
        (String) "oxygen.redox.states",
        List.of(
            MolecularMicrostate.of(
                (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                (Molecule) ChemistryCoreRedoxTest.oxygen("oxygen", MolecularCharge.NEUTRAL),
                (String) "oxygen"),
            MolecularMicrostate.of(
                (MolecularMicrostateKind) MolecularMicrostateKind.REDOX_STATE,
                (Molecule)
                    ChemistryCoreRedoxTest.oxygen("superoxide", MolecularCharge.of((int) -1)),
                (String) "superoxide")));
  }

  private static RedoxCenter oxygenBondCenter() {
    return RedoxCenter.of(
        (String) "oxygen bond",
        (RedoxCenterKind) RedoxCenterKind.BOND,
        List.of(AtomId.of((String) "o1"), AtomId.of((String) "o2")));
  }

  private static Molecule oxygen(final String id, final MolecularCharge charge) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(ChemistryCoreRedoxTest.atom("o1", "O"), ChemistryCoreRedoxTest.atom("o2", "O")),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "o1"),
                (AtomId) AtomId.of((String) "o2"),
                (BondType) BondType.DOUBLE)),
        (MolecularCharge) charge,
        (SpinMultiplicity) SpinMultiplicity.of((int) 3));
  }

  private static Molecule atomMolecule(final String id, final String symbol, final MolecularCharge charge) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(ChemistryCoreRedoxTest.atom("atom", symbol)),
        List.of(),
        (MolecularCharge) charge,
        (SpinMultiplicity) SpinMultiplicity.of((int) 2));
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
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