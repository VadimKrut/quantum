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
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularElectronicConfiguration;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreElectronicStateTest {

  ChemistryCoreElectronicStateTest() {}

  @Test
  void electronicConfigurationExposesChargeSpinAndClosedShellState() {
    final Molecule water =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "electronic.water"),
            (String) "Water",
            List.of(
                ChemistryCoreElectronicStateTest.atom("o", "O"),
                ChemistryCoreElectronicStateTest.atom("h1", "H"),
                ChemistryCoreElectronicStateTest.atom("h2", "H")),
            List.of(
                ChemistryCoreElectronicStateTest.bond("o", "h1"),
                ChemistryCoreElectronicStateTest.bond("o", "h2")),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    MolecularElectronicConfiguration configuration = water.electronicConfiguration();
    Assertions.assertEquals((int) 10, (int) configuration.nuclearCharge());
    Assertions.assertEquals((int) 10, (int) configuration.electronCount());
    Assertions.assertEquals((int) 0, (int) configuration.formalChargeSum());
    Assertions.assertEquals((int) 0, (int) configuration.explicitUnpairedElectronCount());
    Assertions.assertTrue((boolean) configuration.closedShell());
    Assertions.assertFalse((boolean) configuration.openShell());
    Assertions.assertTrue((boolean) configuration.formalChargesMatchMolecularCharge());
  }

  @Test
  void electronicConfigurationTracksExplicitRadicalsAndSpinCompatibility() {
    final Molecule methylRadical =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "electronic.methyl_radical"),
            (String) "Methyl radical",
            List.of(
                Atom.of(
                    (AtomId) AtomId.of((String) "c"),
                    (ElementSymbol) ElementSymbol.of((String) "C"),
                    (Coordinate3D) ChemistryCoreElectronicStateTest.coordinate(),
                    (FormalCharge) FormalCharge.NEUTRAL,
                    null,
                    (RadicalState) RadicalState.of((int) 1),
                    (ChemistryMetadata) ChemistryMetadata.EMPTY),
                ChemistryCoreElectronicStateTest.atom("h1", "H"),
                ChemistryCoreElectronicStateTest.atom("h2", "H"),
                ChemistryCoreElectronicStateTest.atom("h3", "H")),
            List.of(
                ChemistryCoreElectronicStateTest.bond("c", "h1"),
                ChemistryCoreElectronicStateTest.bond("c", "h2"),
                ChemistryCoreElectronicStateTest.bond("c", "h3")),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.of((int) 2));
    final MolecularElectronicConfiguration configuration = methylRadical.electronicConfiguration();
    Assertions.assertEquals((int) 1, (int) configuration.explicitUnpairedElectronCount());
    Assertions.assertEquals((int) 1, (int) configuration.minimumUnpairedElectronCountForSpin());
    Assertions.assertTrue((boolean) configuration.openShell());
    Assertions.assertTrue((boolean) configuration.explicitRadicalsCompatibleWithSpin());
  }

  @Test
  void validatorWarnsWhenExplicitRadicalsDoNotMatchSpinMultiplicity() {
    final Molecule radicalSinglet =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "electronic.bad_radical_spin"),
            (String) "Bad radical spin",
            List.of(
                Atom.of(
                    (AtomId) AtomId.of((String) "n"),
                    (ElementSymbol) ElementSymbol.of((String) "N"),
                    (Coordinate3D) ChemistryCoreElectronicStateTest.coordinate(),
                    (FormalCharge) FormalCharge.NEUTRAL,
                    null,
                    (RadicalState) RadicalState.of((int) 1),
                    (ChemistryMetadata) ChemistryMetadata.EMPTY)),
            List.of(),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    final ChemistryValidationResult result =
        new ChemistryCoreValidator().validateMolecule(radicalSinglet);
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreElectronicStateTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULE_RADICAL_SPIN_NEEDS_REVIEW,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreElectronicStateTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULE_RADICAL_SPIN_MULTIPLICITY_MISMATCH,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void electronicConfigurationRejectsChargeThatRemovesTooManyElectrons() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            Molecule.of(
                    (MoleculeId) MoleculeId.of((String) "electronic.impossible_charge"),
                    (String) "Impossible charge",
                    List.of(ChemistryCoreElectronicStateTest.atom("h", "H")),
                    List.of(),
                    (MolecularCharge) MolecularCharge.of((int) 2),
                    (SpinMultiplicity) SpinMultiplicity.SINGLET)
                .electronicConfiguration());
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D) ChemistryCoreElectronicStateTest.coordinate());
  }

  private static Bond bond(final String firstAtomId, final String secondAtomId) {
    return Bond.of(
        (AtomId) AtomId.of((String) firstAtomId),
        (AtomId) AtomId.of((String) secondAtomId),
        (BondType) BondType.SINGLE);
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