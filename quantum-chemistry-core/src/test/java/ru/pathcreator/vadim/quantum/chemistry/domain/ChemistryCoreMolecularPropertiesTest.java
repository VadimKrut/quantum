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
import ru.pathcreator.vadim.quantum.chemistry.domain.property.MolecularPropertySet;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.NormalModeDisplacement;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.PartialAtomicCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.PartialChargeModel;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.VibrationalFrequency;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.VibrationalFrequencyKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.VibrationalFrequencyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.property.VibrationalMode;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreMolecularPropertiesTest {

  ChemistryCoreMolecularPropertiesTest() {}

  @Test
  void partialChargeModelComputesDipoleMomentAndPropertySetKeepsObservables() {
    final Molecule water = ChemistryCoreMolecularPropertiesTest.water();
    PartialChargeModel charges = ChemistryCoreMolecularPropertiesTest.waterCharges();
    final DipoleMomentVector dipole = charges.dipoleMomentFromCoordinates(water);
    MolecularPropertySet properties =
        MolecularPropertySet.of(
            (String) "water.properties",
            (Molecule) water,
            (DipoleMomentVector) dipole,
            (PartialChargeModel) charges,
            List.of(
                ChemistryCoreMolecularPropertiesTest.waterBendMode(),
                ChemistryCoreMolecularPropertiesTest.transitionStateMode()));
    Assertions.assertEquals((double) 0.0, (double) charges.totalCharge(), (double) 1.0E-12);
    Assertions.assertEquals((double) 0.38766, (double) dipole.x(), (double) 1.0E-12);
    Assertions.assertEquals((double) 0.50274, (double) dipole.y(), (double) 1.0E-12);
    Assertions.assertEquals((double) 3.049289, (double) dipole.magnitudeDebye(), (double) 1.0E-6);
    Assertions.assertTrue((boolean) properties.hasDipoleMoment());
    Assertions.assertTrue((boolean) properties.hasPartialChargeModel());
    Assertions.assertEquals((int) 2, (int) properties.vibrationalModes().size());
    Assertions.assertEquals((int) 1, (int) properties.imaginaryFrequencyCount());
  }

  @Test
  void dipoleVectorAndVibrationalValuesRejectInvalidInput() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            DipoleMomentVector.of(
                (double) Double.NaN,
                (double) 0.0,
                (double) 0.0,
                (DipoleMomentUnit) DipoleMomentUnit.DEBYE));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            VibrationalFrequency.of(
                (double) 0.0,
                (VibrationalFrequencyUnit) VibrationalFrequencyUnit.WAVENUMBER_CM_INVERSE,
                (VibrationalFrequencyKind) VibrationalFrequencyKind.REAL));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            NormalModeDisplacement.of(
                (AtomId) AtomId.of((String) "o"), (double) 0.0, (double) 0.0, (double) 0.0));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            VibrationalMode.of(
                (String) "bad",
                (VibrationalFrequency) ChemistryCoreMolecularPropertiesTest.waterFrequency(),
                (double) -1.0,
                (double) 0.0,
                ChemistryCoreMolecularPropertiesTest.waterDisplacements()));
  }

  @Test
  void partialChargeModelRejectsDuplicateUnknownIncompleteOrUnbalancedCharges() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            PartialChargeModel.of(
                (String) "bad",
                List.of(
                    PartialAtomicCharge.of((AtomId) AtomId.of((String) "o"), (double) -0.84),
                    PartialAtomicCharge.of((AtomId) AtomId.of((String) "o"), (double) 0.42))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            PartialChargeModel.of(
                    (String) "unknown",
                    List.of(
                        PartialAtomicCharge.of((AtomId) AtomId.of((String) "o"), (double) -0.84),
                        PartialAtomicCharge.of((AtomId) AtomId.of((String) "h1"), (double) 0.42),
                        PartialAtomicCharge.of(
                            (AtomId) AtomId.of((String) "missing"), (double) 0.42)))
                .validateAgainstMolecule(ChemistryCoreMolecularPropertiesTest.water()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            PartialChargeModel.of(
                    (String) "incomplete",
                    List.of(
                        PartialAtomicCharge.of((AtomId) AtomId.of((String) "o"), (double) -0.84),
                        PartialAtomicCharge.of((AtomId) AtomId.of((String) "h1"), (double) 0.42)))
                .validateAgainstMolecule(ChemistryCoreMolecularPropertiesTest.water()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            PartialChargeModel.of(
                    (String) "unbalanced",
                    List.of(
                        PartialAtomicCharge.of((AtomId) AtomId.of((String) "o"), (double) -0.84),
                        PartialAtomicCharge.of((AtomId) AtomId.of((String) "h1"), (double) 0.42),
                        PartialAtomicCharge.of((AtomId) AtomId.of((String) "h2"), (double) 0.41)))
                .validateAgainstMolecule(ChemistryCoreMolecularPropertiesTest.water()));
  }

  @Test
  void dipoleCalculationRequiresCoordinatesAndAngstromUnit() {
    final Molecule noCoordinate =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "h2.no_coordinate"),
            (String) "Hydrogen without coordinates",
            List.of(
                Atom.of(
                    (AtomId) AtomId.of((String) "h1"),
                    (ElementSymbol) ElementSymbol.of((String) "H"),
                    null),
                Atom.of(
                    (AtomId) AtomId.of((String) "h2"),
                    (ElementSymbol) ElementSymbol.of((String) "H"),
                    null)),
            List.of(
                Bond.of(
                    (AtomId) AtomId.of((String) "h1"),
                    (AtomId) AtomId.of((String) "h2"),
                    (BondType) BondType.SINGLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    final PartialChargeModel charges =
        PartialChargeModel.of(
            (String) "h2.charges",
            List.of(
                PartialAtomicCharge.of((AtomId) AtomId.of((String) "h1"), (double) -0.1),
                PartialAtomicCharge.of((AtomId) AtomId.of((String) "h2"), (double) 0.1)));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> charges.dipoleMomentFromCoordinates(noCoordinate));
  }

  @Test
  void propertySetRejectsEmptySetDuplicateModesAndUnknownModeAtoms() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularPropertySet.of(
                (String) "empty",
                (Molecule) ChemistryCoreMolecularPropertiesTest.water(),
                null,
                null,
                List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularPropertySet.of(
                (String) "duplicate.mode",
                (Molecule) ChemistryCoreMolecularPropertiesTest.water(),
                null,
                null,
                List.of(
                    ChemistryCoreMolecularPropertiesTest.waterBendMode(),
                    ChemistryCoreMolecularPropertiesTest.waterBendMode())));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularPropertySet.of(
                (String) "unknown.mode.atom",
                (Molecule) ChemistryCoreMolecularPropertiesTest.water(),
                null,
                null,
                List.of(
                    VibrationalMode.of(
                        (String) "unknown",
                        (VibrationalFrequency)
                            ChemistryCoreMolecularPropertiesTest.waterFrequency(),
                        (double) 1.0,
                        (double) 0.0,
                        List.of(
                            NormalModeDisplacement.of(
                                (AtomId) AtomId.of((String) "missing"),
                                (double) 1.0,
                                (double) 0.0,
                                (double) 0.0))))));
  }

  @Test
  void validatorReportsPropertySetInterpretationWarnings() {
    MolecularPropertySet properties =
        MolecularPropertySet.of(
            (String) "water.validator.properties",
            (Molecule) ChemistryCoreMolecularPropertiesTest.water(),
            null,
            (PartialChargeModel) ChemistryCoreMolecularPropertiesTest.waterCharges(),
            List.of(ChemistryCoreMolecularPropertiesTest.transitionStateMode()));
    ChemistryValidationResult result =
        new ChemistryCoreValidator().validateMolecularPropertySet(properties);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreMolecularPropertiesTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULAR_PROPERTY_SET_HAS_NO_DIPOLE_MOMENT,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreMolecularPropertiesTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULAR_PROPERTY_SET_HAS_IMAGINARY_FREQUENCIES,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void validatorReportsDipoleWithoutChargeModelAndMissingVibrationalModes() {
    final MolecularPropertySet properties =
        MolecularPropertySet.of(
            (String) "water.dipole_only.properties",
            (Molecule) ChemistryCoreMolecularPropertiesTest.water(),
            (DipoleMomentVector)
                DipoleMomentVector.of(
                    (double) 0.0,
                    (double) 1.85,
                    (double) 0.0,
                    (DipoleMomentUnit) DipoleMomentUnit.DEBYE),
            null,
            List.of());
    final ChemistryValidationResult result =
        new ChemistryCoreValidator().validateMolecularPropertySet(properties);
    Assertions.assertTrue((boolean) result.valid());
    ChemistryCoreMolecularPropertiesTest.assertDiagnostic(
        result.diagnostics(),
        ChemistryDiagnosticCode.MOLECULAR_PROPERTY_SET_HAS_NO_PARTIAL_CHARGE_MODEL,
        ChemistryDiagnosticSeverity.WARNING,
        "MOLECULAR_PROPERTY_SET",
        "water.dipole_only.properties");
    ChemistryCoreMolecularPropertiesTest.assertDiagnostic(
        result.diagnostics(),
        ChemistryDiagnosticCode.MOLECULAR_PROPERTY_SET_HAS_NO_VIBRATIONAL_MODES,
        ChemistryDiagnosticSeverity.WARNING,
        "MOLECULAR_PROPERTY_SET",
        "water.dipole_only.properties");
  }

  private static PartialChargeModel waterCharges() {
    return PartialChargeModel.of(
        (String) "resp",
        List.of(
            PartialAtomicCharge.of((AtomId) AtomId.of((String) "o"), (double) -0.84),
            PartialAtomicCharge.of((AtomId) AtomId.of((String) "h1"), (double) 0.42),
            PartialAtomicCharge.of((AtomId) AtomId.of((String) "h2"), (double) 0.42)));
  }

  private static VibrationalMode waterBendMode() {
    return VibrationalMode.of(
        (String) "bend",
        (VibrationalFrequency) ChemistryCoreMolecularPropertiesTest.waterFrequency(),
        (double) 22.5,
        (double) 1.2,
        ChemistryCoreMolecularPropertiesTest.waterDisplacements());
  }

  private static VibrationalMode transitionStateMode() {
    return VibrationalMode.of(
        (String) "ts_reaction_coordinate",
        (VibrationalFrequency)
            VibrationalFrequency.of(
                (double) 450.0,
                (VibrationalFrequencyUnit) VibrationalFrequencyUnit.WAVENUMBER_CM_INVERSE,
                (VibrationalFrequencyKind) VibrationalFrequencyKind.IMAGINARY),
        (double) 5.0,
        (double) 0.0,
        ChemistryCoreMolecularPropertiesTest.waterDisplacements());
  }

  private static VibrationalFrequency waterFrequency() {
    return VibrationalFrequency.of(
        (double) 1595.0,
        (VibrationalFrequencyUnit) VibrationalFrequencyUnit.WAVENUMBER_CM_INVERSE,
        (VibrationalFrequencyKind) VibrationalFrequencyKind.REAL);
  }

  private static List<NormalModeDisplacement> waterDisplacements() {
    return List.of(
        NormalModeDisplacement.of(
            (AtomId) AtomId.of((String) "o"), (double) 0.0, (double) 0.0, (double) 0.1),
        NormalModeDisplacement.of(
            (AtomId) AtomId.of((String) "h1"), (double) 0.2, (double) 0.1, (double) -0.1),
        NormalModeDisplacement.of(
            (AtomId) AtomId.of((String) "h2"), (double) -0.2, (double) 0.1, (double) -0.1));
  }

  private static Molecule water() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "water"),
        (String) "Water",
        List.of(
            ChemistryCoreMolecularPropertiesTest.atom("o", "O", 0.0, 0.0, 0.0),
            ChemistryCoreMolecularPropertiesTest.atom("h1", "H", 0.95, 0.0, 0.0),
            ChemistryCoreMolecularPropertiesTest.atom("h2", "H", -0.027, 1.197, 0.0)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "o"),
                (AtomId) AtomId.of((String) "h1"),
                (BondType) BondType.SINGLE),
            Bond.of(
                (AtomId) AtomId.of((String) "o"),
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