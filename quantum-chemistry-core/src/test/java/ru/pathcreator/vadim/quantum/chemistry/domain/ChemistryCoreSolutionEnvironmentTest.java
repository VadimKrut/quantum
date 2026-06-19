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
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.IonicStrength;
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.PHValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.SolutionEnvironment;
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.Solvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.solution.SolventPolarityClass;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;

final class ChemistryCoreSolutionEnvironmentTest {

  ChemistryCoreSolutionEnvironmentTest() {}

  @Test
  void phValueKeepsAcidBaseClassificationWithoutAssumingOnlyZeroToFourteenRange() {
    Assertions.assertTrue((boolean) PHValue.of((double) 2.5).acidic());
    Assertions.assertTrue((boolean) PHValue.of((double) 7.0).neutral());
    Assertions.assertTrue((boolean) PHValue.of((double) 12.5).basic());
    Assertions.assertEquals((double) -1.0, (double) PHValue.of((double) -1.0).value());
    Assertions.assertThrows(IllegalArgumentException.class, () -> PHValue.of((double) Double.NaN));
    Assertions.assertThrows(IllegalArgumentException.class, () -> PHValue.of((double) 31.0));
  }

  @Test
  void solutionEnvironmentRequiresAqueousSolventForPh() {
    final SolutionEnvironment water =
        SolutionEnvironment.of(
            (Solvent) Solvent.WATER,
            (PHValue) PHValue.of((double) 7.4),
            (IonicStrength)
                IonicStrength.of(
                    (double) 0.15, (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER),
            null,
            null);
    Assertions.assertTrue((boolean) water.hasPH());
    Assertions.assertTrue((boolean) water.hasIonicStrength());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            SolutionEnvironment.of(
                (Solvent)
                    Solvent.of(
                        (String) "dmso",
                        (SolventPolarityClass) SolventPolarityClass.POLAR_APROTIC,
                        (Double) 46.7),
                (PHValue) PHValue.of((double) 7.0),
                null,
                null,
                null));
  }

  @Test
  void vacuumEnvironmentRejectsSolutionSpecificValues() {
    Assertions.assertSame(
        (Object) SolutionEnvironment.VACUUM,
        (Object) SolutionEnvironment.of(null, null, null, null, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            SolutionEnvironment.of(
                (Solvent) Solvent.VACUUM, (PHValue) PHValue.of((double) 7.0), null, null, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            SolutionEnvironment.of(
                (Solvent) Solvent.VACUUM,
                null,
                (IonicStrength)
                    IonicStrength.of(
                        (double) 0.1,
                        (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER),
                null,
                null));
  }

  @Test
  void acidBaseMicrostateSetAcceptsEnvironmentWithPh() {
    final MolecularMicrostateSet set =
        MolecularMicrostateSet.of(
            (String) "acid.base.environment",
            List.of(
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                    (Molecule) ChemistryCoreSolutionEnvironmentTest.aceticAcid("acid"),
                    (String) "acid"),
                MolecularMicrostate.of(
                    (MolecularMicrostateKind) MolecularMicrostateKind.IONIZATION_STATE,
                    (Molecule) ChemistryCoreSolutionEnvironmentTest.acetate("acetate"),
                    (String) "acetate")),
            (SolutionEnvironment) SolutionEnvironment.aqueous((PHValue) PHValue.of((double) 4.76)));
    Assertions.assertTrue((boolean) set.hasEnvironment());
    Assertions.assertTrue((boolean) set.environment().hasPH());
  }

  @Test
  void acidBaseMicrostateSetRejectsEnvironmentWithoutPh() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularMicrostateSet.of(
                (String) "acid.base.no.ph",
                List.of(
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.REFERENCE,
                        (Molecule) ChemistryCoreSolutionEnvironmentTest.aceticAcid("acid"),
                        (String) "acid"),
                    MolecularMicrostate.of(
                        (MolecularMicrostateKind) MolecularMicrostateKind.IONIZATION_STATE,
                        (Molecule) ChemistryCoreSolutionEnvironmentTest.acetate("acetate"),
                        (String) "acetate")),
                (SolutionEnvironment)
                    SolutionEnvironment.of(
                        (Solvent) Solvent.WATER,
                        null,
                        (IonicStrength) IonicStrength.ZERO,
                        null,
                        null)));
  }

  private static Molecule aceticAcid(final String id) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreSolutionEnvironmentTest.atom("c1", "C"),
            ChemistryCoreSolutionEnvironmentTest.atom("c2", "C"),
            ChemistryCoreSolutionEnvironmentTest.atom("o1", "O"),
            ChemistryCoreSolutionEnvironmentTest.atom("o2", "O"),
            ChemistryCoreSolutionEnvironmentTest.atom("h1", "H"),
            ChemistryCoreSolutionEnvironmentTest.atom("h2", "H"),
            ChemistryCoreSolutionEnvironmentTest.atom("h3", "H"),
            ChemistryCoreSolutionEnvironmentTest.atom("h4", "H")),
        List.of(
            ChemistryCoreSolutionEnvironmentTest.bond("c1", "c2", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c2", "o1", BondType.DOUBLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c2", "o2", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("o2", "h4", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c1", "h1", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c1", "h2", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c1", "h3", BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule acetate(final String id) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreSolutionEnvironmentTest.atom("c1", "C"),
            ChemistryCoreSolutionEnvironmentTest.atom("c2", "C"),
            ChemistryCoreSolutionEnvironmentTest.atom("o1", "O"),
            ChemistryCoreSolutionEnvironmentTest.atom("o2", "O"),
            ChemistryCoreSolutionEnvironmentTest.atom("h1", "H"),
            ChemistryCoreSolutionEnvironmentTest.atom("h2", "H"),
            ChemistryCoreSolutionEnvironmentTest.atom("h3", "H")),
        List.of(
            ChemistryCoreSolutionEnvironmentTest.bond("c1", "c2", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c2", "o1", BondType.DOUBLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c2", "o2", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c1", "h1", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c1", "h2", BondType.SINGLE),
            ChemistryCoreSolutionEnvironmentTest.bond("c1", "h3", BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.of((int) -1),
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of(
                (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }

  private static Bond bond(final String first, final String second, final BondType type) {
    return Bond.of(
        (AtomId) AtomId.of((String) first), (AtomId) AtomId.of((String) second), (BondType) type);
  }
}