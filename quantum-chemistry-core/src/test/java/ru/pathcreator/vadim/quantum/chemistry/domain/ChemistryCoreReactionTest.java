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
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExperiment;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryExperimentId;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTask;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistryTaskType;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentPurity;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionElementDelta;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSideSummary;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionStoichiometry;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricEquivalent;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmountUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;

final class ChemistryCoreReactionTest {

  ChemistryCoreReactionTest() {}

  @Test
  void reactionBalanceDetectsBalancedAndUnbalancedReactions() {
    final Reaction balanced = ChemistryCoreReactionTest.balancedWaterReaction();
    final Reaction unbalanced =
        Reaction.of(
            (ReactionId) ReactionId.of((String) "reaction.unbalanced_water"),
            (String) "Unbalanced water",
            (ReactionSide)
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            (Molecule) ChemistryCoreReactionTest.h2(),
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE),
                        ReactionParticipant.of(
                            (Molecule) ChemistryCoreReactionTest.o2(),
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE))),
            (ReactionSide)
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            (Molecule) ChemistryCoreReactionTest.water(),
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE))));
    Assertions.assertTrue((boolean) balanced.balance().balanced());
    Assertions.assertFalse((boolean) unbalanced.balance().balanced());
    ChemistryValidationResult result = new ChemistryCoreValidator().validateReaction(unbalanced);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REACTION_ATOMS_NOT_BALANCED,
                ChemistryDiagnosticSeverity.ERROR));
    Assertions.assertTrue(
        (boolean) ((ChemistryDiagnostic) result.diagnostics().get(0)).hasTarget());
  }

  @Test
  void reactionKeepsConditionsAndRejectsBlankConditionText() {
    ReactionConditions conditions =
        ReactionConditions.of(
            (Temperature) Temperature.of((double) 298.15, (TemperatureUnit) TemperatureUnit.KELVIN),
            (Pressure) Pressure.of((double) 1.0, (PressureUnit) PressureUnit.ATMOSPHERE),
            (String) "water",
            (String) "platinum",
            (String) "reference condition");
    Reaction reaction =
        Reaction.of(
            (ReactionId) ReactionId.of((String) "reaction.conditioned_water"),
            (String) "Conditioned hydrogen combustion",
            (ReactionSide) ChemistryCoreReactionTest.balancedWaterReaction().reactants(),
            (ReactionSide) ChemistryCoreReactionTest.balancedWaterReaction().products(),
            (ReactionConditions) conditions,
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    Assertions.assertEquals((Object) conditions, (Object) reaction.conditions());
    Assertions.assertEquals((Object) "water", (Object) conditions.solvent());
    Assertions.assertEquals((Object) "platinum", (Object) conditions.catalyst());
    Assertions.assertEquals((int) 2, (int) conditions.components().size());
    Assertions.assertEquals(
        (int) 1, (int) conditions.componentsByRole(ReactionComponentRole.SOLVENT).size());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ReactionConditions.of(null, null, (String) " ", null, null));
  }

  @Test
  void reactionConditionsKeepStructuredComponents() {
    final ReactionConditionComponent solvent =
        ReactionConditionComponent.of(
            (ReactionComponentRole) ReactionComponentRole.SOLVENT,
            (String) "acetonitrile",
            null,
            (MolarConcentration)
                MolarConcentration.of(
                    (double) 0.5, (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER),
            (ReactionPhase) ReactionPhase.LIQUID,
            (String) "dry");
    final ReactionConditionComponent catalyst =
        ReactionConditionComponent.of(
            (ReactionComponentRole) ReactionComponentRole.CATALYST,
            (String) "palladium acetate",
            (SubstanceAmount)
                SubstanceAmount.of(
                    (double) 0.05, (SubstanceAmountUnit) SubstanceAmountUnit.MILLIMOLE),
            null,
            (StoichiometricEquivalent) StoichiometricEquivalent.of((double) 0.05),
            (ReactionComponentLoading) ReactionComponentLoading.percent((double) 5.0),
            (ReactionComponentPurity) ReactionComponentPurity.percent((double) 98.0),
            (ReactionPhase) ReactionPhase.SOLID,
            (String) "fresh batch");
    final ReactionConditions conditions =
        ReactionConditions.of(
            (Temperature) Temperature.of((double) 80.0, (TemperatureUnit) TemperatureUnit.CELSIUS),
            null,
            List.of(solvent, catalyst),
            (String) "sealed tube");
    Assertions.assertEquals((Object) "acetonitrile", (Object) conditions.solvent());
    Assertions.assertEquals((Object) "palladium acetate", (Object) conditions.catalyst());
    Assertions.assertEquals(
        (Object) ReactionPhase.LIQUID,
        (Object) ((ReactionConditionComponent) conditions.components().get(0)).phase());
    Assertions.assertTrue(
        (boolean) ((ReactionConditionComponent) conditions.components().get(0)).hasConcentration());
    Assertions.assertTrue(
        (boolean) ((ReactionConditionComponent) conditions.components().get(1)).hasAmount());
    Assertions.assertTrue(
        (boolean) ((ReactionConditionComponent) conditions.components().get(1)).hasEquivalent());
    Assertions.assertTrue(
        (boolean) ((ReactionConditionComponent) conditions.components().get(1)).hasLoading());
    Assertions.assertTrue(
        (boolean) ((ReactionConditionComponent) conditions.components().get(1)).hasPurity());
    Assertions.assertEquals(
        (double) 5.0,
        (double) ((ReactionConditionComponent) conditions.components().get(1)).loading().percent());
    Assertions.assertEquals(
        (double) 98.0,
        (double) ((ReactionConditionComponent) conditions.components().get(1)).purity().percent());
    Assertions.assertEquals(
        (int) 1, (int) conditions.componentsByRole(ReactionComponentRole.CATALYST).size());
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> conditions.components().clear());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ReactionConditions.of(null, null, List.of(solvent, solvent), null));
  }

  @Test
  void reactionConditionComponentsRejectInvalidPhysicalQuantities() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactionConditionComponent.named(
                (ReactionComponentRole) ReactionComponentRole.SOLVENT, (String) " "));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> SubstanceAmount.of((double) 0.0, (SubstanceAmountUnit) SubstanceAmountUnit.MOLE));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MolarConcentration.of(
                (double) -1.0, (MolarConcentrationUnit) MolarConcentrationUnit.MOLE_PER_LITER));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> StoichiometricEquivalent.of((double) Double.NaN));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ReactionComponentLoading.percent((double) 101.0));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ReactionComponentPurity.percent((double) 0.0));
  }

  @Test
  void reactionStoichiometryKeepsPrimitiveElementCountsAndStableDeltas() {
    Reaction reaction = ChemistryCoreReactionTest.balancedWaterReaction();
    final ReactionStoichiometry stoichiometry = reaction.stoichiometry();
    final ReactionSideSummary reactants = stoichiometry.reactants();
    final ReactionSideSummary products = stoichiometry.products();

    Assertions.assertEquals((int) 2, (int) reactants.elementKindCount());
    Assertions.assertEquals((long) 4L, (long) reactants.elementCount(ElementSymbol.of("H")));
    Assertions.assertEquals((long) 4L, (long) reactants.elementCountByAtomicNumber(1));
    Assertions.assertEquals((long) 2L, (long) reactants.elementCountByAtomicNumber(8));
    Assertions.assertEquals((long) 4L, (long) products.elementCountByAtomicNumber(1));
    Assertions.assertEquals((long) 2L, (long) products.elementCountByAtomicNumber(8));
    Assertions.assertEquals((int) 2, (int) stoichiometry.elementDeltas().size());

    final ReactionElementDelta hydrogenDelta = stoichiometry.elementDeltas().get(0);
    final ReactionElementDelta oxygenDelta = stoichiometry.elementDeltas().get(1);
    Assertions.assertEquals((Object) ElementSymbol.of("H"), (Object) hydrogenDelta.symbol());
    Assertions.assertEquals((long) 0L, (long) hydrogenDelta.delta());
    Assertions.assertEquals((Object) ElementSymbol.of("O"), (Object) oxygenDelta.symbol());
    Assertions.assertEquals((long) 0L, (long) oxygenDelta.delta());
    Assertions.assertTrue((boolean) stoichiometry.balanced());
  }

  @Test
  void reactionEnergyTaskRequiresReactionSubject() {
    final ChemistryTask task =
        ChemistryTask.of(
            (ChemistryTaskType) ChemistryTaskType.REACTION_ENERGY,
            (BasisSet) BasisSet.STO_3G,
            (ActiveSpace) ActiveSpace.of((int) 2, (int) 2));
    final ChemistryExperiment experiment =
        ChemistryExperiment.of(
            (ChemistryExperimentId)
                ChemistryExperimentId.of((String) "experiment.invalid_reaction_subject"),
            (ChemistrySubject) ChemistryCoreReactionTest.h2(),
            (ChemistryTask) task,
            (ChemistryExecutionMode) ChemistryExecutionMode.CLASSICAL_AND_QUANTUM);
    ChemistryValidationResult result = new ChemistryCoreValidator().validateExperiment(experiment);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.REACTION_TASK_REQUIRES_REACTION_SUBJECT,
                ChemistryDiagnosticSeverity.ERROR));
  }

  @Test
  void reactionValidatorIncludesParticipantMoleculeDiagnostics() {
    final Atom chargedNitrogen =
        Atom.of(
            (AtomId) AtomId.of((String) "n"),
            (ElementSymbol) ElementSymbol.of((String) "N"),
            (Coordinate3D)
                Coordinate3D.of(
                    (double) 0.0, (double) 0.0, (double) 0.0, (LengthUnit) LengthUnit.ANGSTROM),
            (FormalCharge) FormalCharge.of((int) 1),
            null,
            (RadicalState) RadicalState.CLOSED_SHELL,
            (ChemistryMetadata) ChemistryMetadata.EMPTY);
    final Molecule invalidParticipant =
        Molecule.of(
            (MoleculeId) MoleculeId.of((String) "invalid.nitrogen"),
            (String) "Invalid nitrogen",
            List.of(chargedNitrogen),
            List.of(),
            (MolecularCharge) MolecularCharge.NEUTRAL,
            (SpinMultiplicity) SpinMultiplicity.SINGLET);
    final Reaction reaction =
        Reaction.of(
            (ReactionId) ReactionId.of((String) "reaction.invalid_participant"),
            (String) "Invalid participant reaction",
            (ReactionSide)
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            (Molecule) invalidParticipant,
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE))),
            (ReactionSide)
                ReactionSide.of(
                    List.of(
                        ReactionParticipant.of(
                            (Molecule) invalidParticipant,
                            (StoichiometricCoefficient) StoichiometricCoefficient.ONE))));
    final ChemistryValidationResult result = new ChemistryCoreValidator().validateReaction(reaction);
    Assertions.assertFalse((boolean) result.valid());
    Assertions.assertTrue(
        (boolean)
            ChemistryCoreReactionTest.contains(
                result.diagnostics(),
                ChemistryDiagnosticCode.MOLECULE_CHARGE_DOES_NOT_MATCH_ATOMS,
                ChemistryDiagnosticSeverity.ERROR));
  }

  private static Reaction balancedWaterReaction() {
    return Reaction.of(
        (ReactionId) ReactionId.of((String) "reaction.water"),
        (String) "Hydrogen combustion",
        (ReactionSide)
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreReactionTest.h2(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.of((int) 2)),
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreReactionTest.o2(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.ONE))),
        (ReactionSide)
            ReactionSide.of(
                List.of(
                    ReactionParticipant.of(
                        (Molecule) ChemistryCoreReactionTest.water(),
                        (StoichiometricCoefficient) StoichiometricCoefficient.of((int) 2)))));
  }

  private static Molecule h2() {
    return Molecule.of(
        (MoleculeId) MoleculeId.of((String) "h2"),
        (String) "Hydrogen",
        List.of(
            ChemistryCoreReactionTest.atom("h1", "H", 0.0),
            ChemistryCoreReactionTest.atom("h2", "H", 0.74)),
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
            ChemistryCoreReactionTest.atom("o1", "O", 0.0),
            ChemistryCoreReactionTest.atom("o2", "O", 1.21)),
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
            ChemistryCoreReactionTest.atom("o", "O", 0.0),
            ChemistryCoreReactionTest.atom("h1", "H", 0.95),
            ChemistryCoreReactionTest.atom("h2", "H", -0.95)),
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