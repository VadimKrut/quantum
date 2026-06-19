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
import ru.pathcreator.vadim.quantum.chemistry.domain.composition.MolecularComposition;
import ru.pathcreator.vadim.quantum.chemistry.domain.composition.MolecularCompositionAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.domain.composition.MolecularMass;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ChemicalElement;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.ElementCountVector;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormula;
import ru.pathcreator.vadim.quantum.chemistry.domain.formula.MolecularFormulaTerm;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ChemistryCoreCompositionTest {

  private static final double EPSILON = 1.0E-9;

  ChemistryCoreCompositionTest() {}

  @Test
  void compositionCountsAtomsAndUsesHillFormula() {
    final Molecule ethanol =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "composition.ethanol"),
            (String) "Ethanol",
            List.of(
                ChemistryCoreCompositionTest.atom("c1", "C"),
                ChemistryCoreCompositionTest.atom("c2", "C"),
                ChemistryCoreCompositionTest.atom("o", "O"),
                ChemistryCoreCompositionTest.atom("h1", "H"),
                ChemistryCoreCompositionTest.atom("h2", "H"),
                ChemistryCoreCompositionTest.atom("h3", "H"),
                ChemistryCoreCompositionTest.atom("h4", "H"),
                ChemistryCoreCompositionTest.atom("h5", "H"),
                ChemistryCoreCompositionTest.atom("h6", "H")),
            List.of(
                ChemistryCoreCompositionTest.bond("c1", "c2"),
                ChemistryCoreCompositionTest.bond("c2", "o")),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    MolecularComposition composition = MolecularCompositionAnalyzer.analyze((Molecule) ethanol);
    Assertions.assertEquals((Object) composition, (Object) ethanol.composition());
    Assertions.assertEquals((Object) "C2H6O", (Object) composition.formula().hillNotation());
    Assertions.assertEquals((int) 9, (int) composition.atomCount());
    Assertions.assertEquals((int) 6, (int) composition.hydrogenCount());
    Assertions.assertEquals((int) 3, (int) composition.heavyAtomCount());
    Assertions.assertEquals((int) 1, (int) composition.heteroAtomCount());
    Assertions.assertFalse((boolean) composition.isotopicallyLabeled());
    Assertions.assertEquals(
        (double) 46.069, (double) composition.mass().averageAtomicMass(), (double) 1.0E-9);
    Assertions.assertEquals((int) 46, (int) composition.mass().nominalMassNumber());
  }

  @Test
  void formulaMergesDuplicateTermsAndExposesPrimitiveCountVector() {
    final MolecularFormula formula =
        MolecularFormula.of(
            List.of(
                MolecularFormulaTerm.of(ElementSymbol.of("O"), 1),
                MolecularFormulaTerm.of(ElementSymbol.of("C"), 1),
                MolecularFormulaTerm.of(ElementSymbol.of("H"), 2),
                MolecularFormulaTerm.of(ElementSymbol.of("C"), 1),
                MolecularFormulaTerm.of(ElementSymbol.of("H"), 4)));
    final ElementCountVector vector = formula.countVector();
    Assertions.assertEquals("C2H6O", formula.hillNotation());
    Assertions.assertEquals(9, formula.atomCount());
    Assertions.assertEquals(3, formula.elementKindCount());
    Assertions.assertEquals(2, formula.countOf(ElementSymbol.of("C")));
    Assertions.assertEquals(6, formula.countOf(ElementSymbol.of("H")));
    Assertions.assertEquals(1, formula.countOf(ElementSymbol.of("O")));
    Assertions.assertEquals(6, vector.hydrogenCount());
    Assertions.assertEquals(2, vector.carbonCount());
    Assertions.assertEquals(3, vector.heavyAtomCount());
    Assertions.assertEquals(1, vector.heteroAtomCount());
    Assertions.assertEquals(2, vector.countByAtomicNumber(6));
    Assertions.assertEquals(0, vector.countByAtomicNumber(119));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> formula.terms().clear());
  }

  @Test
  void formulaCanBeBuiltFromPrimitiveCountVectorWithoutLosingCounters() {
    final ElementCountVector vector =
        ElementCountVector.builder()
            .add(ElementSymbol.of("N"), 2)
            .add(ElementSymbol.of("H"), 4)
            .add(ElementSymbol.of("C"), 3)
            .build();

    final MolecularFormula formula = MolecularFormula.fromCountVector(vector);

    Assertions.assertEquals("C3H4N2", formula.hillNotation());
    Assertions.assertEquals(9, formula.atomCount());
    Assertions.assertEquals(3, formula.elementKindCount());
    Assertions.assertEquals(vector, formula.countVector());
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> MolecularFormula.fromCountVector(null));
  }

  @Test
  void formulaUsesAlphabeticalHillNotationWithoutCarbon() {
    final MolecularFormula formula =
        MolecularFormula.of(
            List.of(
                MolecularFormulaTerm.of(ElementSymbol.of("O"), 1),
                MolecularFormulaTerm.of(ElementSymbol.of("Na"), 1),
                MolecularFormulaTerm.of(ElementSymbol.of("Cl"), 1),
                MolecularFormulaTerm.of(ElementSymbol.of("H"), 1)));
    Assertions.assertEquals("ClHNaO", formula.hillNotation());
    Assertions.assertEquals(4, formula.countVector().totalAtomCount());
    Assertions.assertEquals(3, formula.countVector().heavyAtomCount());
    Assertions.assertEquals(3, formula.countVector().heteroAtomCount());
  }

  @Test
  void periodicTableSupportsAtomicNumberLookupForEveryElement() {
    Assertions.assertEquals(118, PeriodicTable.elementCount());
    for (int atomicNumber = PeriodicTable.MIN_ATOMIC_NUMBER;
        atomicNumber <= PeriodicTable.MAX_ATOMIC_NUMBER;
        ++atomicNumber) {
      final ChemicalElement element = PeriodicTable.requireAtomicNumber(atomicNumber);
      Assertions.assertEquals(atomicNumber, element.atomicNumber());
      Assertions.assertEquals(atomicNumber, PeriodicTable.atomicNumberOf(element.symbol()));
      Assertions.assertTrue(PeriodicTable.contains(element.symbol()));
      Assertions.assertTrue(PeriodicTable.containsAtomicNumber(atomicNumber));
    }
    Assertions.assertFalse(PeriodicTable.containsAtomicNumber(0));
    Assertions.assertFalse(PeriodicTable.containsAtomicNumber(119));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> PeriodicTable.requireAtomicNumber(0));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> PeriodicTable.requireAtomicNumber(119));
  }

  @Test
  void elementCountVectorRejectsInvalidAndOverflowingInput() {
    final ElementCountVector.Builder builder = ElementCountVector.builder();
    Assertions.assertThrows(IllegalArgumentException.class, () -> builder.build());
    Assertions.assertThrows(IllegalArgumentException.class, () -> builder.add(null, 1));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> builder.add(ElementSymbol.of("H"), 0));
    final ElementCountVector.Builder overflowBuilder = ElementCountVector.builder();
    overflowBuilder.add(ElementSymbol.of("H"), Integer.MAX_VALUE);
    Assertions.assertThrows(
        ArithmeticException.class, () -> overflowBuilder.add(ElementSymbol.of("H"), 1));
  }

  @Test
  void compositionKeepsIsotopicLabelsInNominalMass() {
    final Molecule labeledCarbonMonoxide =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "composition.labeled_co"),
            (String) "Labeled carbon monoxide",
            List.of(
                Atom.of(
                    (AtomId) AtomId.of((String) "c13"),
                    (ElementSymbol) ElementSymbol.of((String) "C"),
                    (Coordinate3D) ChemistryCoreCompositionTest.coordinate(),
                    (FormalCharge) FormalCharge.NEUTRAL,
                    (Isotope) Isotope.of((int) 13),
                    (RadicalState) RadicalState.CLOSED_SHELL,
                    null),
                ChemistryCoreCompositionTest.atom("o", "O")),
            List.of(
                Bond.of(
                    (AtomId) AtomId.of((String) "c13"),
                    (AtomId) AtomId.of((String) "o"),
                    (BondType) BondType.TRIPLE)),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    final MolecularComposition composition = labeledCarbonMonoxide.composition();
    Assertions.assertTrue((boolean) composition.isotopicallyLabeled());
    Assertions.assertEquals((int) 29, (int) composition.mass().nominalMassNumber());
    Assertions.assertEquals(
        (double) 28.999, (double) composition.mass().averageAtomicMass(), (double) 1.0E-9);
    Assertions.assertEquals((int) 2, (int) composition.heavyAtomCount());
    Assertions.assertEquals((int) 1, (int) composition.heteroAtomCount());
  }

  @Test
  void compositionRejectsInconsistentManualValues() {
    MolecularMass mass = MolecularMass.of((double) 18.015, (int) 18);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolecularComposition.of(
                (MolecularFormula) ChemistryCoreCompositionTest.water().formula(),
                (int) 3,
                (int) 1,
                (int) 1,
                (int) 1,
                (boolean) false,
                (MolecularMass) mass));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> MolecularMass.of((double) 0.0, (int) 18));
  }

  @Test
  void compositionRejectsCountersThatDoNotMatchFormulaVector() {
    final MolecularMass mass = MolecularMass.of((double) 18.015, (int) 18);
    final MolecularFormula waterFormula = ChemistryCoreCompositionTest.water().formula();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> MolecularComposition.of(waterFormula, 3, 1, 2, 1, false, mass));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> MolecularComposition.of(waterFormula, 3, 2, 1, 0, false, mass));
  }

  private static Molecule water() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "composition.water"),
        (String) "Water",
        List.of(
            ChemistryCoreCompositionTest.atom("o", "O"),
            ChemistryCoreCompositionTest.atom("h1", "H"),
            ChemistryCoreCompositionTest.atom("h2", "H")),
        List.of(
            ChemistryCoreCompositionTest.bond("o", "h1"),
            ChemistryCoreCompositionTest.bond("o", "h2")),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D) ChemistryCoreCompositionTest.coordinate());
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
}