/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismEnergyPoint;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismPointKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionCoordinateValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;

final class ChemistryCoreMechanismTest {

  @Test
  void reactionSideSignatureIsOrderIndependentAndRejectsDuplicateMoleculeIds() {
    final ReactionSide first =
        ReactionSide.of(
            List.of(
                participant(ChemistryCoreMechanismTest.hydrogenMolecule("molecule.ha")),
                participant(ChemistryCoreMechanismTest.hydrogenMolecule("molecule.hb"))));
    final ReactionSide second =
        ReactionSide.of(
            List.of(
                participant(ChemistryCoreMechanismTest.hydrogenMolecule("molecule.hb")),
                participant(ChemistryCoreMechanismTest.hydrogenMolecule("molecule.ha"))));

    assertTrue(first.sameParticipantsAs(second));
    assertEquals(first.signature(), second.signature());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionSide.of(
                List.of(
                    participant(ChemistryCoreMechanismTest.hydrogenMolecule("molecule.ha")),
                    participant(ChemistryCoreMechanismTest.hydrogenMolecule("molecule.ha")))));
  }

  @Test
  void reactionMechanismConnectsStepsByCanonicalSides() {
    final Molecule first = ChemistryCoreMechanismTest.hydrogenMolecule("molecule.first");
    final Molecule second = ChemistryCoreMechanismTest.hydrogenMolecule("molecule.second");
    final Molecule third = ChemistryCoreMechanismTest.hydrogenMolecule("molecule.third");
    final Molecule fourth = ChemistryCoreMechanismTest.hydrogenMolecule("molecule.fourth");
    final Reaction overall =
        ChemistryCoreMechanismTest.reaction(
            "reaction.overall",
            ReactionSide.of(List.of(participant(first), participant(second))),
            ReactionSide.of(List.of(participant(third), participant(fourth))));
    final ElementaryReactionStep firstStep =
        step(
            "step.first",
            ChemistryCoreMechanismTest.reaction(
                "reaction.step_first",
                ReactionSide.of(List.of(participant(first), participant(second))),
                ReactionSide.of(List.of(participant(fourth), participant(third)))));
    final ElementaryReactionStep secondStep =
        step(
            "step.second",
            ChemistryCoreMechanismTest.reaction(
                "reaction.step_second",
                ReactionSide.of(List.of(participant(third), participant(fourth))),
                ReactionSide.of(List.of(participant(third), participant(fourth)))));

    final ReactionMechanism mechanism =
        ReactionMechanism.of(
            "mechanism.order_independent",
            overall,
            List.of(firstStep, secondStep),
            List.of(
                energyPoint("point.reactants", MechanismPointKind.REACTANT_COMPLEX, 0.0, 0.0),
                energyPoint("point.ts", MechanismPointKind.TRANSITION_STATE, 0.5, 25.0),
                energyPoint("point.products", MechanismPointKind.PRODUCT_COMPLEX, 1.0, -5.0)));

    assertEquals(2, mechanism.steps().size());
    assertEquals(1, mechanism.transitionStateCount());
    assertEquals(
        EnergyValue.of(25.0, EnergyUnit.KILOJOULE_PER_MOLE), mechanism.highestRelativeEnergy());
  }

  @Test
  void reactionMechanismRejectsDuplicateStepAndEnergyPointIds() {
    final Molecule first = ChemistryCoreMechanismTest.hydrogenMolecule("molecule.first");
    final Molecule second = ChemistryCoreMechanismTest.hydrogenMolecule("molecule.second");
    final Reaction reaction =
        ChemistryCoreMechanismTest.reaction(
            "reaction.single",
            ReactionSide.of(List.of(participant(first))),
            ReactionSide.of(List.of(participant(second))));
    final ElementaryReactionStep step = step("step.same", reaction);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                "mechanism.duplicate_steps",
                reaction,
                List.of(step, step("step.same", reaction)),
                List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                "mechanism.duplicate_points",
                reaction,
                List.of(step),
                List.of(
                    energyPoint("point.same", MechanismPointKind.REACTANT_COMPLEX, 0.0, 0.0),
                    energyPoint("point.same", MechanismPointKind.PRODUCT_COMPLEX, 1.0, 0.0))));
  }

  private static Molecule hydrogenMolecule(final String id) {
    return Molecule.of(
        MoleculeId.of(id),
        id,
        List.of(Atom.of(AtomId.of(id.replace('.', '_')), ElementSymbol.of("H"), null)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static ReactionParticipant participant(final Molecule molecule) {
    return ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE);
  }

  private static Reaction reaction(
      final String id, final ReactionSide reactants, final ReactionSide products) {
    return Reaction.of(ReactionId.of(id), id, reactants, products);
  }

  private static ElementaryReactionStep step(final String id, final Reaction reaction) {
    return ElementaryReactionStep.of(id, reaction, null, null, null, null);
  }

  private static MechanismEnergyPoint energyPoint(
      final String id,
      final MechanismPointKind kind,
      final double coordinate,
      final double energyKiloJoulePerMole) {
    return MechanismEnergyPoint.of(
        id,
        kind,
        ReactionCoordinateValue.of(coordinate),
        EnergyValue.of(energyKiloJoulePerMole, EnergyUnit.KILOJOULE_PER_MOLE),
        null);
  }
}