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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreReactionMechanismTest {

  ChemistryCoreReactionMechanismTest() {}

  @Test
  void reactionMechanismAcceptsConnectedElementaryStepsAndEnergyProfile() {
    ReactionMechanism mechanism =
        ReactionMechanism.of(
            (String) "mechanism.hydrogen_rearrangement",
            (Reaction) ChemistryCoreReactionMechanismTest.overallReaction(),
            List.of(
                ChemistryCoreReactionMechanismTest.firstStep(),
                ChemistryCoreReactionMechanismTest.secondStep()),
            ChemistryCoreReactionMechanismTest.energyProfile());
    Assertions.assertEquals((int) 2, (int) mechanism.steps().size());
    Assertions.assertTrue((boolean) mechanism.hasEnergyProfile());
    Assertions.assertEquals((int) 2, (int) mechanism.transitionStateCount());
    Assertions.assertEquals(
        (Object) EnergyValue.of((double) 55.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
        (Object) mechanism.highestRelativeEnergy());
    Assertions.assertTrue(
        (boolean) ((ElementaryReactionStep) mechanism.steps().get(0)).hasTransitionState());
    Assertions.assertTrue(
        (boolean) ((ElementaryReactionStep) mechanism.steps().get(0)).hasForwardBarrier());
    Assertions.assertTrue(
        (boolean) ((ElementaryReactionStep) mechanism.steps().get(1)).hasReactionEnergy());
  }

  @Test
  void elementaryStepRejectsUnbalancedReactionAndNegativeBarriers() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElementaryReactionStep.of(
                (String) "bad.unbalanced",
                (Reaction)
                    Reaction.of(
                        (ReactionId) ReactionId.of((String) "reaction.bad"),
                        (String) "Bad",
                        (ReactionSide)
                            ChemistryCoreReactionMechanismTest.side(
                                ChemistryCoreReactionMechanismTest.hydrogenReactant()),
                        (ReactionSide)
                            ChemistryCoreReactionMechanismTest.side(
                                ChemistryCoreReactionMechanismTest.hydrogenChloride())),
                null,
                null,
                null,
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ElementaryReactionStep.of(
                (String) "bad.barrier",
                (Reaction)
                    ChemistryCoreReactionMechanismTest.stepReaction(
                        "reaction.ok",
                        ChemistryCoreReactionMechanismTest.hydrogenReactant(),
                        ChemistryCoreReactionMechanismTest.hydrogenIntermediate()),
                (Molecule) ChemistryCoreReactionMechanismTest.transitionState("ts_bad"),
                (EnergyValue)
                    EnergyValue.of((double) -1.0, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE),
                null,
                null));
  }

  @Test
  void mechanismRejectsWrongOverallBoundaryOrDisconnectedSteps() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                (String) "wrong.start",
                (Reaction)
                    Reaction.of(
                        (ReactionId) ReactionId.of((String) "reaction.overall.wrong"),
                        (String) "Wrong overall",
                        (ReactionSide)
                            ChemistryCoreReactionMechanismTest.side(
                                ChemistryCoreReactionMechanismTest.hydrogenIntermediate()),
                        (ReactionSide)
                            ChemistryCoreReactionMechanismTest.side(
                                ChemistryCoreReactionMechanismTest.hydrogenProduct())),
                List.of(
                    ChemistryCoreReactionMechanismTest.firstStep(),
                    ChemistryCoreReactionMechanismTest.secondStep()),
                ChemistryCoreReactionMechanismTest.energyProfile()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                (String) "disconnected",
                (Reaction) ChemistryCoreReactionMechanismTest.overallReaction(),
                List.of(
                    ChemistryCoreReactionMechanismTest.firstStep(),
                    ChemistryCoreReactionMechanismTest.disconnectedStep()),
                ChemistryCoreReactionMechanismTest.energyProfile()));
  }

  @Test
  void mechanismRejectsDuplicateStepIdsAndUnbalancedOverallReaction() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                (String) "duplicate.steps",
                (Reaction) ChemistryCoreReactionMechanismTest.overallReaction(),
                List.of(
                    ChemistryCoreReactionMechanismTest.firstStep(),
                    ChemistryCoreReactionMechanismTest.firstStep()),
                ChemistryCoreReactionMechanismTest.energyProfile()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                (String) "unbalanced.overall",
                (Reaction)
                    Reaction.of(
                        (ReactionId) ReactionId.of((String) "reaction.unbalanced.overall"),
                        (String) "Unbalanced overall",
                        (ReactionSide)
                            ChemistryCoreReactionMechanismTest.side(
                                ChemistryCoreReactionMechanismTest.hydrogenReactant()),
                        (ReactionSide)
                            ChemistryCoreReactionMechanismTest.side(
                                ChemistryCoreReactionMechanismTest.hydrogenChloride())),
                List.of(ChemistryCoreReactionMechanismTest.firstStep()),
                List.of()));
  }

  @Test
  void energyProfileRejectsBadOrderingDuplicatesAndMissingBoundaryComplexes() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                (String) "bad.profile.order",
                (Reaction) ChemistryCoreReactionMechanismTest.overallReaction(),
                List.of(
                    ChemistryCoreReactionMechanismTest.firstStep(),
                    ChemistryCoreReactionMechanismTest.secondStep()),
                List.of(
                    ChemistryCoreReactionMechanismTest.energyPoint(
                        "reactants", MechanismPointKind.REACTANT_COMPLEX, 0.0, 0.0),
                    ChemistryCoreReactionMechanismTest.energyPoint(
                        "late", MechanismPointKind.INTERMEDIATE, 1.0, 10.0),
                    ChemistryCoreReactionMechanismTest.energyPoint(
                        "early", MechanismPointKind.PRODUCT_COMPLEX, 0.5, -15.0))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                (String) "bad.profile.duplicate",
                (Reaction) ChemistryCoreReactionMechanismTest.overallReaction(),
                List.of(
                    ChemistryCoreReactionMechanismTest.firstStep(),
                    ChemistryCoreReactionMechanismTest.secondStep()),
                List.of(
                    ChemistryCoreReactionMechanismTest.energyPoint(
                        "same", MechanismPointKind.REACTANT_COMPLEX, 0.0, 0.0),
                    ChemistryCoreReactionMechanismTest.energyPoint(
                        "same", MechanismPointKind.PRODUCT_COMPLEX, 1.0, -15.0))));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionMechanism.of(
                (String) "bad.profile.no.product",
                (Reaction) ChemistryCoreReactionMechanismTest.overallReaction(),
                List.of(
                    ChemistryCoreReactionMechanismTest.firstStep(),
                    ChemistryCoreReactionMechanismTest.secondStep()),
                List.of(
                    ChemistryCoreReactionMechanismTest.energyPoint(
                        "reactants", MechanismPointKind.REACTANT_COMPLEX, 0.0, 0.0),
                    ChemistryCoreReactionMechanismTest.energyPoint(
                        "intermediate", MechanismPointKind.INTERMEDIATE, 1.0, -5.0))));
  }

  @Test
  void mechanismValuesRejectInvalidInput() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ReactionCoordinateValue.of((double) Double.NaN));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MechanismEnergyPoint.of(
                (String) "bad",
                null,
                (ReactionCoordinateValue) ReactionCoordinateValue.of((double) 0.0),
                (EnergyValue) ChemistryCoreReactionMechanismTest.energy(0.0),
                null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MechanismEnergyPoint.of(
                (String) "bad.energy",
                (MechanismPointKind) MechanismPointKind.REACTANT_COMPLEX,
                (ReactionCoordinateValue) ReactionCoordinateValue.of((double) 0.0),
                null,
                null));
  }

  @Test
  void validatorReportsIncompleteMechanismMetadataAsWarnings() {
    ReactionMechanism mechanism =
        ReactionMechanism.of(
            (String) "mechanism.incomplete",
            (Reaction) ChemistryCoreReactionMechanismTest.overallReaction(),
            List.of(
                ElementaryReactionStep.of(
                    (String) "step_incomplete",
                    (Reaction)
                        ChemistryCoreReactionMechanismTest.stepReaction(
                            "reaction.step_incomplete",
                            ChemistryCoreReactionMechanismTest.hydrogenReactant(),
                            ChemistryCoreReactionMechanismTest.hydrogenProduct()),
                    null,
                    null,
                    null,
                    null)),
            List.of());
    ChemistryValidationResult result =
        new ChemistryCoreValidator().validateReactionMechanism(mechanism);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionMechanismTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REACTION_MECHANISM_HAS_NO_ENERGY_PROFILE,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionMechanismTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_TRANSITION_STATE,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionMechanismTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_FORWARD_BARRIER,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionMechanismTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_REVERSE_BARRIER,
                ChemistryDiagnosticSeverity.WARNING));
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionMechanismTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REACTION_MECHANISM_STEP_HAS_NO_REACTION_ENERGY,
                ChemistryDiagnosticSeverity.WARNING));
  }

  @Test
  void validatorReportsEnergyProfileTransitionStateMismatch() {
    final ReactionMechanism mechanism =
        ReactionMechanism.of(
            (String) "mechanism.profile_mismatch",
            (Reaction) ChemistryCoreReactionMechanismTest.overallReaction(),
            List.of(
                ElementaryReactionStep.of(
                    (String) "step_with_ts",
                    (Reaction)
                        ChemistryCoreReactionMechanismTest.stepReaction(
                            "reaction.step_with_ts",
                            ChemistryCoreReactionMechanismTest.hydrogenReactant(),
                            ChemistryCoreReactionMechanismTest.hydrogenProduct()),
                    (Molecule)
                        ChemistryCoreReactionMechanismTest.transitionStateKnownBond("ts_known"),
                    (EnergyValue) ChemistryCoreReactionMechanismTest.energy(15.0),
                    (EnergyValue) ChemistryCoreReactionMechanismTest.energy(18.0),
                    (EnergyValue) ChemistryCoreReactionMechanismTest.energy(-2.0))),
            List.of(
                ChemistryCoreReactionMechanismTest.energyPoint(
                    "reactants", MechanismPointKind.REACTANT_COMPLEX, 0.0, 0.0),
                ChemistryCoreReactionMechanismTest.energyPoint(
                    "ts_1", MechanismPointKind.TRANSITION_STATE, 0.3, 15.0),
                ChemistryCoreReactionMechanismTest.energyPoint(
                    "ts_2", MechanismPointKind.TRANSITION_STATE, 0.7, 20.0),
                ChemistryCoreReactionMechanismTest.energyPoint(
                    "products", MechanismPointKind.PRODUCT_COMPLEX, 1.0, -2.0)));
    final ChemistryValidationResult result =
        new ChemistryCoreValidator().validateReactionMechanism(mechanism);
    Assertions.assertTrue((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionMechanismTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REACTION_MECHANISM_ENERGY_PROFILE_TRANSITION_STATE_MISMATCH,
                ChemistryDiagnosticSeverity.WARNING));
  }

  private static ElementaryReactionStep firstStep() {
    return ElementaryReactionStep.of(
        (String) "step_1",
        (Reaction)
            ChemistryCoreReactionMechanismTest.stepReaction(
                "reaction.step_1",
                ChemistryCoreReactionMechanismTest.hydrogenReactant(),
                ChemistryCoreReactionMechanismTest.hydrogenIntermediate()),
        (Molecule) ChemistryCoreReactionMechanismTest.transitionState("ts_1"),
        (EnergyValue) ChemistryCoreReactionMechanismTest.energy(55.0),
        (EnergyValue) ChemistryCoreReactionMechanismTest.energy(78.0),
        (EnergyValue) ChemistryCoreReactionMechanismTest.energy(-23.0));
  }

  private static ElementaryReactionStep secondStep() {
    return ElementaryReactionStep.of(
        (String) "step_2",
        (Reaction)
            ChemistryCoreReactionMechanismTest.stepReaction(
                "reaction.step_2",
                ChemistryCoreReactionMechanismTest.hydrogenIntermediate(),
                ChemistryCoreReactionMechanismTest.hydrogenProduct()),
        (Molecule) ChemistryCoreReactionMechanismTest.transitionState("ts_2"),
        (EnergyValue) ChemistryCoreReactionMechanismTest.energy(32.0),
        (EnergyValue) ChemistryCoreReactionMechanismTest.energy(28.0),
        (EnergyValue) ChemistryCoreReactionMechanismTest.energy(-12.0));
  }

  private static ElementaryReactionStep disconnectedStep() {
    return ElementaryReactionStep.of(
        (String) "disconnected",
        (Reaction)
            ChemistryCoreReactionMechanismTest.stepReaction(
                "reaction.disconnected",
                ChemistryCoreReactionMechanismTest.hydrogenOtherIntermediate(),
                ChemistryCoreReactionMechanismTest.hydrogenProduct()),
        (Molecule) ChemistryCoreReactionMechanismTest.transitionState("ts_disconnected"),
        (EnergyValue) ChemistryCoreReactionMechanismTest.energy(10.0),
        null,
        null);
  }

  private static Reaction overallReaction() {
    return Reaction.of(
        (ReactionId) ReactionId.of((String) "reaction.hydrogen_rearrangement"),
        (String) "Hydrogen rearrangement",
        (ReactionSide)
            ChemistryCoreReactionMechanismTest.side(
                ChemistryCoreReactionMechanismTest.hydrogenReactant()),
        (ReactionSide)
            ChemistryCoreReactionMechanismTest.side(
                ChemistryCoreReactionMechanismTest.hydrogenProduct()));
  }

  private static Reaction stepReaction(final String id, final Molecule reactant, final Molecule product) {
    return Reaction.of(
        (ReactionId) ReactionId.of((String) id),
        (String) id,
        (ReactionSide) ChemistryCoreReactionMechanismTest.side(reactant),
        (ReactionSide) ChemistryCoreReactionMechanismTest.side(product));
  }

  private static List<MechanismEnergyPoint> energyProfile() {
    return List.of(
        ChemistryCoreReactionMechanismTest.energyPoint(
            "reactants", MechanismPointKind.REACTANT_COMPLEX, 0.0, 0.0),
        ChemistryCoreReactionMechanismTest.energyPoint(
            "ts_1", MechanismPointKind.TRANSITION_STATE, 0.3, 55.0),
        ChemistryCoreReactionMechanismTest.energyPoint(
            "intermediate", MechanismPointKind.INTERMEDIATE, 0.5, -23.0),
        ChemistryCoreReactionMechanismTest.energyPoint(
            "ts_2", MechanismPointKind.TRANSITION_STATE, 0.7, 9.0),
        ChemistryCoreReactionMechanismTest.energyPoint(
            "products", MechanismPointKind.PRODUCT_COMPLEX, 1.0, -35.0));
  }

  private static MechanismEnergyPoint energyPoint(
      final String id, final MechanismPointKind kind, final double coordinate, final double energy) {
    return MechanismEnergyPoint.of(
        (String) id,
        (MechanismPointKind) kind,
        (ReactionCoordinateValue) ReactionCoordinateValue.of((double) coordinate),
        (EnergyValue) ChemistryCoreReactionMechanismTest.energy(energy),
        null);
  }

  private static ReactionSide side(final Molecule molecule) {
    return ReactionSide.of(
        List.of(
            ReactionParticipant.of(
                (Molecule) molecule, (StoichiometricCoefficient) StoichiometricCoefficient.ONE)));
  }

  private static EnergyValue energy(final double value) {
    return EnergyValue.of((double) value, (EnergyUnit) EnergyUnit.KILOJOULE_PER_MOLE);
  }

  private static Molecule transitionState(final String id) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) id,
        List.of(
            ChemistryCoreReactionMechanismTest.atom("a", "H", 0.0),
            ChemistryCoreReactionMechanismTest.atom("b", "H", 1.0)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b"),
                (BondType) BondType.UNKNOWN)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule transitionStateKnownBond(final String id) {
    return ChemistryCoreReactionMechanismTest.diatomic(id, id, BondType.SINGLE);
  }

  private static Molecule hydrogenReactant() {
    return ChemistryCoreReactionMechanismTest.diatomic(
        "h2_reactant", "Hydrogen reactant complex", BondType.SINGLE);
  }

  private static Molecule hydrogenIntermediate() {
    return ChemistryCoreReactionMechanismTest.diatomic(
        "h2_intermediate", "Hydrogen intermediate", BondType.SINGLE);
  }

  private static Molecule hydrogenOtherIntermediate() {
    return ChemistryCoreReactionMechanismTest.diatomic(
        "h2_other_intermediate", "Other hydrogen intermediate", BondType.SINGLE);
  }

  private static Molecule hydrogenProduct() {
    return ChemistryCoreReactionMechanismTest.diatomic(
        "h2_product", "Hydrogen product complex", BondType.SINGLE);
  }

  private static Molecule hydrogenChloride() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "hcl"),
        (String) "Hydrogen chloride",
        List.of(
            ChemistryCoreReactionMechanismTest.atom("h", "H", 0.0),
            ChemistryCoreReactionMechanismTest.atom("cl", "Cl", 1.0)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "h"),
                (AtomId) AtomId.of((String) "cl"),
                (BondType) BondType.SINGLE)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
        (SpinMultiplicity) SpinMultiplicity.SINGLET);
  }

  private static Molecule diatomic(final String id, final String name, final BondType bondType) {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) id),
        (String) name,
        List.of(
            ChemistryCoreReactionMechanismTest.atom("a", "H", 0.0),
            ChemistryCoreReactionMechanismTest.atom("b", "H", 1.0)),
        List.of(
            Bond.of(
                (AtomId) AtomId.of((String) "a"),
                (AtomId) AtomId.of((String) "b"),
                (BondType) bondType)),
        (MolecularCharge) MolecularCharge.NEUTRAL,
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