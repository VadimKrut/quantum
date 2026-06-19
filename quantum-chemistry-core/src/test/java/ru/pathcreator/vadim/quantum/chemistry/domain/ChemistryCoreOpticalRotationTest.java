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
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotationDirection;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptorFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;

final class ChemistryCoreOpticalRotationTest {

  ChemistryCoreOpticalRotationTest() {}

  @Test
  void relativeDlNotationIsNotOpticalRotationDirection() {
    Assertions.assertEquals(
        (Object) StereochemicalDescriptorFamily.RELATIVE_CONFIGURATION,
        (Object) StereochemicalDescriptor.D.family());
    Assertions.assertEquals(
        (Object) StereochemicalDescriptorFamily.RELATIVE_CONFIGURATION,
        (Object) StereochemicalDescriptor.L.family());
    Assertions.assertNotEquals(
        (Object) OpticalRotationDirection.DEXTROROTATORY.name(),
        (Object) StereochemicalDescriptor.D.name());
    Assertions.assertNotEquals(
        (Object) OpticalRotationDirection.LEVOROTATORY.name(),
        (Object) StereochemicalDescriptor.L.name());
  }

  @Test
  void moleculeKeepsOpticalRotationWithMeasurementConditions() {
    final OpticalRotation rotation =
        OpticalRotation.of(
            (OpticalRotationDirection) OpticalRotationDirection.DEXTROROTATORY,
            java.lang.Double.valueOf(13.5),
            (Temperature) Temperature.of((double) 20.0, (TemperatureUnit) TemperatureUnit.CELSIUS),
            java.lang.Double.valueOf(589.0));
    final Molecule molecule =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "optical.rotation.probe"),
            (String) "Optical rotation probe",
            List.of(ChemistryCoreOpticalRotationTest.atom("c")),
            List.of(),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET,
            null,
            null,
            (OpticalRotation) rotation,
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Assertions.assertEquals(
        (Object) OpticalRotationDirection.DEXTROROTATORY,
        (Object) molecule.opticalRotation().direction());
    Assertions.assertEquals((double) 13.5, molecule.opticalRotation().degrees());
    Assertions.assertEquals((double) 589.0, molecule.opticalRotation().wavelengthNanometers());
  }

  @Test
  void opticalRotationRejectsContradictoryDirectionAndAngle() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            OpticalRotation.of(
                (OpticalRotationDirection) OpticalRotationDirection.DEXTROROTATORY,
                java.lang.Double.valueOf(-1.0),
                null,
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            OpticalRotation.of(
                (OpticalRotationDirection) OpticalRotationDirection.LEVOROTATORY,
                java.lang.Double.valueOf(1.0),
                null,
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            OpticalRotation.of(
                (OpticalRotationDirection) OpticalRotationDirection.NONE,
                java.lang.Double.valueOf(1.0),
                null,
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            OpticalRotation.of(
                (OpticalRotationDirection) OpticalRotationDirection.UNKNOWN,
                java.lang.Double.valueOf(1.0),
                null,
                null));
  }

  private static Atom atom(final String id) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) "C"),
        (Coordinate3D)
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }
}