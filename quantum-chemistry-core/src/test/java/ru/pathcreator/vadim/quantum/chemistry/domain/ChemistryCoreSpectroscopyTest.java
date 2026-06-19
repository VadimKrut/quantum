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
import ru.pathcreator.vadim.quantum.chemistry.domain.property.DipoleMomentUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.DipoleMomentVector;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.ElectronicTransition;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.ExcitedStateKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.MolecularSpectroscopySet;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrChemicalShift;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrShieldingTensor;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.OscillatorStrength;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.SpinSpinCoupling;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.Wavelength;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreSpectroscopyTest {

  ChemistryCoreSpectroscopyTest() {}

  @Test
  void spectroscopySetKeepsElectronicAndNmrObservables() {
    MolecularSpectroscopySet set =
        MolecularSpectroscopySet.of(
            (String) "formaldehyde.spectra",
            (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
            List.of(
                ChemistryCoreSpectroscopyTest.allowedTransition(),
                ChemistryCoreSpectroscopyTest.darkTransition()),
            List.of(
                ChemistryCoreSpectroscopyTest.carbonShift(),
                ChemistryCoreSpectroscopyTest.protonShift()),
            List.of(
                SpinSpinCoupling.of(
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "h1"),
                    (double) 172.0)));
    Assertions.assertEquals(
        (Object) ChemistryCoreSpectroscopyTest.formaldehyde().id(), (Object) set.moleculeId());
    Assertions.assertEquals((int) 2, (int) set.electronicTransitions().size());
    Assertions.assertEquals((int) 1, (int) set.opticallyAllowedTransitionCount());
    Assertions.assertTrue(
        (boolean)
            ((ElectronicTransition) set.electronicTransitions().get(0))
                .hasTransitionDipoleMoment());
    Assertions.assertEquals(
        (double) 180.0,
        (double) ((NmrChemicalShift) set.nmrChemicalShifts().get(0)).ppm(),
        (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 120.0,
        (double) ChemistryCoreSpectroscopyTest.carbonShift().shieldingTensor().isotropicPpm(),
        (double) 1.0E-12);
    Assertions.assertEquals(
        (double) 172.0,
        (double) ((SpinSpinCoupling) set.spinSpinCouplings().get(0)).hertz(),
        (double) 1.0E-12);
  }

  @Test
  void electronicTransitionValuesRejectInvalidInput() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> Wavelength.nanometers((double) 0.0));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> OscillatorStrength.of((double) -0.1));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronicTransition.of(
                (String) "bad",
                (int) 0,
                (ExcitedStateKind) ExcitedStateKind.SINGLET,
                (EnergyValue) EnergyValue.of((double) 4.1, (EnergyUnit) EnergyUnit.ELECTRON_VOLT),
                (Wavelength) Wavelength.nanometers((double) 300.0),
                (OscillatorStrength) OscillatorStrength.of((double) 0.1),
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElectronicTransition.of(
                (String) "bad.no_energy",
                (int) 1,
                (ExcitedStateKind) ExcitedStateKind.SINGLET,
                null,
                (Wavelength) Wavelength.nanometers((double) 300.0),
                (OscillatorStrength) OscillatorStrength.of((double) 0.1),
                null));
  }

  @Test
  void nmrValuesRejectInvalidInput() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> NmrShieldingTensor.of((double) Double.NaN, (double) 1.0, (double) 2.0));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> NmrChemicalShift.of((AtomId) AtomId.of((String) "c"), null, (double) 10.0, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            NmrChemicalShift.of(
                (AtomId) AtomId.of((String) "c"),
                (Isotope) Isotope.of((int) 13),
                (double) Double.NaN,
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            SpinSpinCoupling.of(
                (AtomId) AtomId.of((String) "c"), (AtomId) AtomId.of((String) "c"), (double) 1.0));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            SpinSpinCoupling.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "h"),
                (double) Double.NaN));
  }

  @Test
  void spectroscopySetRejectsEmptyDuplicateOrUnknownReferences() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularSpectroscopySet.of(
                (String) "empty",
                (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
                List.of(),
                List.of(),
                List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularSpectroscopySet.of(
                (String) "duplicate.transition",
                (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
                List.of(
                    ChemistryCoreSpectroscopyTest.allowedTransition(),
                    ChemistryCoreSpectroscopyTest.allowedTransition()),
                List.of(),
                List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularSpectroscopySet.of(
                (String) "duplicate.state.index",
                (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
                List.of(
                    ChemistryCoreSpectroscopyTest.allowedTransition(),
                    ElectronicTransition.of(
                        (String) "s1.other",
                        (int) 1,
                        (ExcitedStateKind) ExcitedStateKind.TRIPLET,
                        (EnergyValue)
                            EnergyValue.of((double) 3.0, (EnergyUnit) EnergyUnit.ELECTRON_VOLT),
                        (Wavelength) Wavelength.nanometers((double) 410.0),
                        (OscillatorStrength) OscillatorStrength.of((double) 0.0),
                        null)),
                List.of(),
                List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularSpectroscopySet.of(
                (String) "unknown.shift",
                (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
                List.of(),
                List.of(
                    NmrChemicalShift.of(
                        (AtomId) AtomId.of((String) "missing"),
                        (Isotope) Isotope.of((int) 1),
                        (double) 1.0,
                        null)),
                List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularSpectroscopySet.of(
                (String) "unknown.coupling",
                (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
                List.of(),
                List.of(),
                List.of(
                    SpinSpinCoupling.of(
                        (AtomId) AtomId.of((String) "c"),
                        (AtomId) AtomId.of((String) "missing"),
                        (double) 1.0))));
  }

  @Test
  void spectroscopySetRejectsDuplicateNmrAtomIsotopeAndCouplingPairs() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularSpectroscopySet.of(
                (String) "duplicate.shift",
                (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
                List.of(),
                List.of(
                    ChemistryCoreSpectroscopyTest.carbonShift(),
                    ChemistryCoreSpectroscopyTest.carbonShift()),
                List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularSpectroscopySet.of(
                (String) "duplicate.coupling",
                (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
                List.of(),
                List.of(),
                List.of(
                    SpinSpinCoupling.of(
                        (AtomId) AtomId.of((String) "c"),
                        (AtomId) AtomId.of((String) "h1"),
                        (double) 172.0),
                    SpinSpinCoupling.of(
                        (AtomId) AtomId.of((String) "h1"),
                        (AtomId) AtomId.of((String) "c"),
                        (double) 172.0))));
  }

  @Test
  void validatorReportsSpectroscopyInterpretationWarnings() {
    MolecularSpectroscopySet set =
        MolecularSpectroscopySet.of(
            (String) "formaldehyde.dark.spectra",
            (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
            List.of(ChemistryCoreSpectroscopyTest.darkTransition()),
            List.of(),
            List.of());
    ChemistryValidationResult result =
        new ChemistryCoreValidator().validateMolecularSpectroscopySet(set);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreSpectroscopyTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.SPECTROSCOPY_SET_HAS_NO_OPTICALLY_ALLOWED_TRANSITIONS,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreSpectroscopyTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.SPECTROSCOPY_SET_HAS_NO_NMR_OBSERVABLES,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void validatorReportsNmrOnlySpectroscopyWithoutElectronicTransitions() {
    final MolecularSpectroscopySet set =
        MolecularSpectroscopySet.of(
            (String) "formaldehyde.nmr_only.spectra",
            (Molecule) ChemistryCoreSpectroscopyTest.formaldehyde(),
            List.of(),
            List.of(ChemistryCoreSpectroscopyTest.carbonShift()),
            List.of(
                SpinSpinCoupling.of(
                    (AtomId) AtomId.of((String) "c"),
                    (AtomId) AtomId.of((String) "h1"),
                    (double) 172.0)));
    final ChemistryValidationResult result =
        new ChemistryCoreValidator().validateMolecularSpectroscopySet(set);
    Assertions.assertTrue((boolean) result.valid());
    ChemistryCoreSpectroscopyTest.assertDiagnostic(
        result.diagnostics(),
        ChemistryDiagnosticCode.SPECTROSCOPY_SET_HAS_NO_ELECTRONIC_TRANSITIONS,
        ChemistryDiagnosticSeverity.WARNING,
        "MOLECULAR_SPECTROSCOPY_SET",
        "formaldehyde.nmr_only.spectra");
    Assertions.assertFalse(
        (boolean)
            ChemistryCoreSpectroscopyTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.SPECTROSCOPY_SET_HAS_NO_NMR_OBSERVABLES,
                ChemistryDiagnosticSeverity.WARNING));
  }

  private static ElectronicTransition allowedTransition() {
    return ElectronicTransition.of(
        (String) "s1",
        (int) 1,
        (ExcitedStateKind) ExcitedStateKind.SINGLET,
        (EnergyValue) EnergyValue.of((double) 4.1, (EnergyUnit) EnergyUnit.ELECTRON_VOLT),
        (Wavelength) Wavelength.nanometers((double) 302.4),
        (OscillatorStrength) OscillatorStrength.of((double) 0.15),
        (DipoleMomentVector)
            DipoleMomentVector.of(
                (double) 0.1,
                (double) 0.0,
                (double) 0.2,
                (DipoleMomentUnit) DipoleMomentUnit.DEBYE));
  }

  private static ElectronicTransition darkTransition() {
    return ElectronicTransition.of(
        (String) "t1",
        (int) 2,
        (ExcitedStateKind) ExcitedStateKind.TRIPLET,
        (EnergyValue) EnergyValue.of((double) 3.0, (EnergyUnit) EnergyUnit.ELECTRON_VOLT),
        (Wavelength) Wavelength.nanometers((double) 413.2),
        (OscillatorStrength) OscillatorStrength.of((double) 0.0),
        null);
  }

  private static NmrChemicalShift carbonShift() {
    return NmrChemicalShift.of(
        (AtomId) AtomId.of((String) "c"),
        (Isotope) Isotope.of((int) 13),
        (double) 180.0,
        (NmrShieldingTensor) NmrShieldingTensor.of((double) 100.0, (double) 120.0, (double) 140.0));
  }

  private static NmrChemicalShift protonShift() {
    return NmrChemicalShift.of(
        (AtomId) AtomId.of((String) "h1"), (Isotope) Isotope.of((int) 1), (double) 9.7, null);
  }

  private static Molecule formaldehyde() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "formaldehyde"),
        (String) "Formaldehyde",
        List.of(
            ChemistryCoreSpectroscopyTest.atom("c", "C", 0.0, 0.0, 0.0),
            ChemistryCoreSpectroscopyTest.atom("o", "O", 1.2, 0.0, 0.0),
            ChemistryCoreSpectroscopyTest.atom("h1", "H", -0.6, 0.9, 0.0),
            ChemistryCoreSpectroscopyTest.atom("h2", "H", -0.6, -0.9, 0.0)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "o"),
                (BondType) BondType.DOUBLE),
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "h1"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "c"),
                (AtomId) AtomId.of((String) "h2"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol, final double x, final double y, final double z) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of((double) x, (double) y, (double) z, (LengthUnit) LengthUnit.ANGSTROM));
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

  private static void assertDiagnostic(
      final List<ChemistryDiagnostic> diagnostics,
      final ChemistryDiagnosticCode code,
      final ChemistryDiagnosticSeverity severity,
      final String targetKind,
      final String targetId) {
    for (int i = 0; i < diagnostics.size(); ++i) {
      final ChemistryDiagnostic diagnostic = diagnostics.get(i);
      if (diagnostic.code() != code || diagnostic.severity() != severity) {
        continue;
      }
      Assertions.assertTrue((boolean) diagnostic.hasTarget());
      Assertions.assertEquals((Object) targetKind, (Object) diagnostic.target().kind());
      Assertions.assertEquals((Object) targetId, (Object) diagnostic.target().id());
      Assertions.assertFalse((boolean) diagnostic.message().isBlank());
      return;
    }
    Assertions.fail("Expected diagnostic " + code + " was not reported.");
  }
}