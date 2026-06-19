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
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionElementDelta;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSideSummary;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionStoichiometry;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ChemistryCoreReactionStoichiometryTest {

  ChemistryCoreReactionStoichiometryTest() {}

  @Test
  void stoichiometryExplainsBalancedWaterReaction() {
    ReactionStoichiometry stoichiometry =
        ChemistryCoreReactionStoichiometryTest.balancedWaterReaction().stoichiometry();
    Assertions.assertTrue((boolean) stoichiometry.balanced());
    Assertions.assertTrue((boolean) stoichiometry.massNumberBalanced());
    Assertions.assertEquals((long) 6L, (long) stoichiometry.reactants().atomCount());
    Assertions.assertEquals((long) 6L, (long) stoichiometry.products().atomCount());
    Assertions.assertEquals(
        (long) 4L, (long) stoichiometry.reactants().elementCount(ElementSymbol.of((String) "H")));
    Assertions.assertEquals(
        (long) 2L, (long) stoichiometry.reactants().elementCount(ElementSymbol.of((String) "O")));
    Assertions.assertEquals(
        (long) 4L, (long) stoichiometry.products().elementCount(ElementSymbol.of((String) "H")));
    Assertions.assertEquals(
        (long) 2L, (long) stoichiometry.products().elementCount(ElementSymbol.of((String) "O")));
    Assertions.assertEquals((long) 0L, (long) stoichiometry.chargeDelta());
    Assertions.assertEquals((long) 0L, (long) stoichiometry.nominalMassNumberDelta());
    Assertions.assertEquals(
        (double) 0.0, (double) stoichiometry.averageMassDelta(), (double) 1.0E-11);
  }

  @Test
  void stoichiometryExposesElementDeltasForUnbalancedReaction() {
    Reaction reaction =
        Reaction.of(
            (ReactionId) ReactionId.of((String) "reaction.unbalanced_water"),
            (String) "Unbalanced water",
            (ReactionSide)
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            (Molecule) ChemistryCoreReactionStoichiometryTest.h2(),
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE),
                        ReactionParticipant.of(
                            (Molecule) ChemistryCoreReactionStoichiometryTest.o2(),
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE))),
            (ReactionSide)
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            (Molecule) ChemistryCoreReactionStoichiometryTest.water(),
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE))));
    ReactionStoichiometry stoichiometry = reaction.stoichiometry();
    Assertions.assertFalse((boolean) stoichiometry.balanced());
    Assertions.assertFalse((boolean) stoichiometry.massNumberBalanced());
    Assertions.assertEquals((int) 2, (int) stoichiometry.elementDeltas().size());
    ChemistryCoreReactionStoichiometryTest.assertElementDelta(
        (ReactionElementDelta) stoichiometry.elementDeltas().get(0), "H", 2L, 2L, 0L);
    ChemistryCoreReactionStoichiometryTest.assertElementDelta(
        (ReactionElementDelta) stoichiometry.elementDeltas().get(1), "O", 2L, 1L, -1L);
    Assertions.assertEquals((long) -16L, (long) stoichiometry.nominalMassNumberDelta());
  }

  @Test
  void stoichiometryKeepsChargeBalanceSeparateFromAtomBalance() {
    final Reaction reaction =
        Reaction.of(
            (ReactionId) ReactionId.of((String) "reaction.charge_transfer"),
            (String) "Charge transfer",
            (ReactionSide)
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            (Molecule) ChemistryCoreReactionStoichiometryTest.hydrogenCation(),
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE))),
            (ReactionSide)
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            (Molecule) ChemistryCoreReactionStoichiometryTest.hydrogenAtom(),
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE))));
    final ReactionStoichiometry stoichiometry = reaction.stoichiometry();
    Assertions.assertTrue((boolean) stoichiometry.atomBalanced());
    Assertions.assertFalse((boolean) stoichiometry.chargeBalanced());
    Assertions.assertFalse((boolean) stoichiometry.balanced());
    Assertions.assertEquals((long) -1L, (long) stoichiometry.chargeDelta());
    Assertions.assertFalse((boolean) reaction.balance().chargeBalanced());
    Assertions.assertTrue((boolean) reaction.balance().massNumberBalanced());
  }

  @Test
  void sideSummaryIsImmutableAndRejectsNullSide() {
    final ReactionSideSummary summary =
        ReactionSideSummary.of(
            (ReactionSide)
                ChemistryCoreReactionStoichiometryTest.balancedWaterReaction().reactants());
    Assertions.assertEquals((int) 2, (int) summary.participantCount());
    Assertions.assertEquals((long) 3L, (long) summary.totalCoefficient());
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> summary.elementCounts().put(ElementSymbol.of((String) "C"), 1L));
    Assertions.assertThrows(IllegalArgumentException.class, () -> ReactionSideSummary.of(null));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ReactionElementDelta.of(null, (long) 0L, (long) 0L));
  }

  private static Reaction balancedWaterReaction() {
    return Reaction.of(
        (ReactionId) ReactionId.of((String) "reaction.water"),
        (String) "Hydrogen combustion",
        (ReactionSide)
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreReactionStoichiometryTest.h2(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.of((int) 2)),
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreReactionStoichiometryTest.o2(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.ONE))),
        (ReactionSide)
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreReactionStoichiometryTest.water(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.of((int) 2)))));
  }

  private static Molecule h2() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "h2"),
        (String) "Hydrogen",
        List.of(
            ChemistryCoreReactionStoichiometryTest.atom("h1", "H", 0.0),
            ChemistryCoreReactionStoichiometryTest.atom("h2", "H", 0.74)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "h1"),
                (AtomId) AtomId.of((String) "h2"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule o2() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "o2"),
        (String) "Oxygen",
        List.of(
            ChemistryCoreReactionStoichiometryTest.atom("o1", "O", 0.0),
            ChemistryCoreReactionStoichiometryTest.atom("o2", "O", 1.21)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "o1"),
                (AtomId) AtomId.of((String) "o2"),
                (BondType) BondType.DOUBLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.of((int) 3));
  }

  private static Molecule water() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "water"),
        (String) "Water",
        List.of(
            ChemistryCoreReactionStoichiometryTest.atom("o", "O", 0.0),
            ChemistryCoreReactionStoichiometryTest.atom("h1", "H", 0.95),
            ChemistryCoreReactionStoichiometryTest.atom("h2", "H", -0.95)),
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

  private static Molecule hydrogenAtom() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "hydrogen_atom"),
        (String) "Hydrogen atom",
        List.of(ChemistryCoreReactionStoichiometryTest.atom("h", "H", 0.0)),
        List.of(),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.of((int) 2));
  }

  private static Molecule hydrogenCation() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "hydrogen_cation"),
        (String) "Hydrogen cation",
        List.of(
            Atom.of(
                (AtomId) AtomId.of((String) "h"),
                (ElementSymbol) ElementSymbol.of((String) "H"),
                (Coordinate3D)
                    Coordinate3D.of(
                        (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
                (FormalCharge) FormalCharge.of((int) 1),
                null,
                (RadicalState) RadicalState.CLOSED_SHELL,
                (ChemistryMetadata) ChemistryMetadata.EMPTY)),
        List.of(),
        (MolecularCharge) MolecularCharge.of((int) 1),
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol, final double x) {
    return Atom.of(
        (AtomId) AtomId.of((String) id),
        (ElementSymbol) ElementSymbol.of((String) symbol),
        (Coordinate3D)
            Coordinate3D.of(
                (double) x, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM));
  }

  private static void assertElementDelta(
      final ReactionElementDelta delta,
      final String symbol,
      final long reactantCount,
      final long productCount,
      final long expectedDelta) {
    Assertions.assertEquals((Object) ElementSymbol.of((String) symbol), (Object) delta.symbol());
    Assertions.assertEquals((long) reactantCount, (long) delta.reactantCount());
    Assertions.assertEquals((long) productCount, (long) delta.productCount());
    Assertions.assertEquals((long) expectedDelta, (long) delta.delta());
  }
}