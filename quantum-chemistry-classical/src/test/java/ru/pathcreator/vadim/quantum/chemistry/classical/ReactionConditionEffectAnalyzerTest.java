/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffect;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentPurity;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricEquivalent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
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
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDuration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TimeDurationUnit;

final class ReactionConditionEffectAnalyzerTest {

  @Test
  void pegSolventIsClassifiedByGenericPolyetherAndBoilingProperties() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "peg_condition_probe",
            ReactionSide.of(
                List.of(
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.carbonProbe()))),
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, "PEG4000")),
                List.of(),
                "polyether solvent probe"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionConditionEffectProfile profile =
        new ReactionConditionEffectAnalyzer().analyze(request);

    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SOLVENT_HIGH_BOILING));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(profile, "polyether"));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(profile, "mass transfer"));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(profile, "phase-transfer-like"));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(profile, "High-boiling"));
  }

  @Test
  void heatedPolyetherMediumCreatesGenericMechanisticConditionProfile() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "heated_polyether_heterocycle_probe",
            ReactionSide.of(
                List.of(
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.carbonProbe()),
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.namedCarbonNitrogenProbe(
                            "heated_polyether_amine_probe", "Generic amine nucleophile")))),
            ReactionConditions.of(
                Temperature.of(120.0, TemperatureUnit.CELSIUS),
                null,
                TimeDuration.of(3.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.named(
                        ReactionComponentRole.SOLVENT, "polyethylene glycol 4000")),
                List.of(),
                "heated viscous polyether condition probe without observed outcome lookup"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionConditionEffectProfile profile =
        new ReactionConditionEffectAnalyzer().analyze(request);

    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SOLVENT_HIGH_BOILING));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.THERMAL_ACTIVATION));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SUSTAINED_TIME_CONVERSION));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.THERMAL_DOSE_REGIME));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.REACTANT_AMINE_NUCLEOPHILE));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.HETEROCYCLE_COMPATIBILITY));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(
            profile, "without using outcome lookup"));
  }

  @Test
  void freeTextLaboratoryConditionNoteIsNormalizedIntoGenericPhysicalSignals() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "free_text_condition_probe",
            ReactionSide.of(
                List.of(
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.carbonProbe()),
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.namedCarbonNitrogenProbe(
                            "free_text_condition_amine", "Benzylamine")))),
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(),
                List.of(),
                "PEG4000 medium, 120 C, 3 h, atmospheric pressure; generic lab note without product lookup"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionConditionEffectProfile profile =
        new ReactionConditionEffectAnalyzer().analyze(request);

    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.THERMAL_ACTIVATION));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.SUSTAINED_TIME_CONVERSION));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.ATMOSPHERIC_REACTION_REGIME));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.REACTANT_AMINE_NUCLEOPHILE));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.NOTE_DERIVED_CONDITION_SIGNAL));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(
            profile, "no product outcome lookup"));
  }

  @Test
  void genericSolventScenarioNamesAreClassifiedWithoutConcreteSolventLookup() {
    final ReactionConditionEffectAnalyzer analyzer = new ReactionConditionEffectAnalyzer();

    final ReactionConditionEffectProfile aproticProfile =
        analyzer.analyze(
            ReactionConditionEffectAnalyzerTest.requestWithSolvent("polar aprotic solvent class"));
    final ReactionConditionEffectProfile proticProfile =
        analyzer.analyze(
            ReactionConditionEffectAnalyzerTest.requestWithSolvent("polar protic solvent class"));
    final ReactionConditionEffectProfile nonpolarProfile =
        analyzer.analyze(
            ReactionConditionEffectAnalyzerTest.requestWithSolvent("nonpolar solvent class"));

    Assertions.assertTrue(
        aproticProfile.hasEffect(ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC));
    Assertions.assertTrue(
        proticProfile.hasEffect(ReactionConditionEffectKind.SOLVENT_POLAR_PROTIC));
    Assertions.assertTrue(nonpolarProfile.hasEffect(ReactionConditionEffectKind.SOLVENT_NONPOLAR));
  }

  @Test
  void activationModeComponentsCreateGenericSingleElectronConditionEffects() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "single_electron_activation_condition_probe",
            ReactionSide.of(
                List.of(
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.carbonProbe()))),
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(
                    ReactionConditionComponent.named(
                        ReactionComponentRole.CATALYST, "Ir photocatalyst under blue LED"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "K2S2O8 oxidant"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "Hantzsch ester reductant"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.OTHER, "TEMPO radical trap"),
                    ReactionConditionComponent.named(
                        ReactionComponentRole.OTHER, "constant current electrolysis")),
                List.of(),
                "photoelectrochemical radical activation probe"),
            List.of(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionConditionEffectProfile profile =
        new ReactionConditionEffectAnalyzer().analyze(request);

    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.PHOTOCHEMICAL_ACTIVATION));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.ELECTROCHEMICAL_ACTIVATION));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.OXIDANT_PRESENT));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.REDUCTANT_PRESENT));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.RADICAL_INITIATOR_PRESENT));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.OXYGEN_RADICAL_TRAP));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(profile, "single-electron"));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(profile, "radical-chain"));
  }

  @Test
  void quantifiedComponentsCreateConditionAwareEffectsWithoutOutcomeLookup() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "quantified_condition_probe",
            ReactionSide.of(
                List.of(
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.carbonProbe()))),
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.BASE,
                        "Potassium carbonate",
                        null,
                        MolarConcentration.of(0.75, MolarConcentrationUnit.MOLE_PER_LITER),
                        StoichiometricEquivalent.of(1.6),
                        null,
                        ReactionComponentPurity.percent(98.0),
                        ReactionPhase.SOLUTION,
                        "quantified base"),
                    ReactionConditionComponent.of(
                        ReactionComponentRole.CATALYST,
                        "Palladium acetate",
                        null,
                        null,
                        null,
                        ReactionComponentLoading.percent(5.0),
                        ReactionComponentPurity.percent(92.0),
                        ReactionPhase.SOLUTION,
                        "quantified catalyst")),
                List.of(),
                "quantified condition probe"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionConditionEffectProfile profile =
        new ReactionConditionEffectAnalyzer().analyze(request);

    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.QUANTIFIED_STOICHIOMETRY_CONTROL));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.EXCESS_REAGENT_DRIVING_FORCE));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.CATALYST_LOADING_WINDOW));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.PURITY_LIMITATION));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.CONCENTRATION_REGIME));
    Assertions.assertFalse(profile.hasEffect(ReactionConditionEffectKind.DILUTE_CONDITION_REGIME));
    Assertions.assertFalse(
        profile.hasEffect(ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME));
    Assertions.assertTrue(profile.scoreAdjustment() > 0.0);
    Assertions.assertEquals(1, profile.riskCount());
  }

  @Test
  void physicalRegimeEffectsComeFromConditionsNotProductHistory() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "physical_condition_regime_probe",
            ReactionSide.of(
                List.of(
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.carbonProbe()))),
            ReactionConditions.of(
                Temperature.of(145.0, TemperatureUnit.CELSIUS),
                Pressure.of(4.0, PressureUnit.BAR),
                TimeDuration.of(6.0, TimeDurationUnit.HOUR),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.ADDITIVE,
                        "generic concentrated nucleophile",
                        null,
                        MolarConcentration.of(1.25, MolarConcentrationUnit.MOLE_PER_LITER),
                        null,
                        null,
                        null,
                        ReactionPhase.SOLUTION,
                        "condition-only concentration probe"),
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "generic diluted co-solvent stream",
                        null,
                        MolarConcentration.of(15.0, MolarConcentrationUnit.MILLIMOLE_PER_LITER),
                        null,
                        null,
                        null,
                        ReactionPhase.SOLUTION,
                        "condition-only dilution probe")),
                List.of(),
                "physical regime validation without observed product data"),
            List.of(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION),
            List.of(),
            null);

    final ReactionConditionEffectProfile profile =
        new ReactionConditionEffectAnalyzer().analyze(request);

    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.THERMAL_DOSE_REGIME));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.PRESSURIZED_REACTION_REGIME));
    Assertions.assertTrue(
        profile.hasEffect(ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME));
    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.DILUTE_CONDITION_REGIME));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(
            profile, "temperature-time thermal dose"));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(
            profile, "Pressurized reaction regime"));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(
                profile, "without observed product data")
            || ReactionConditionEffectAnalyzerTest.hasDescriptionPart(
                profile, "bimolecular collision"));
  }

  @Test
  void amineSourceIsClassifiedByGeneralNamePattern() {
    final ReactionPredictionRequest request =
        ReactionPredictionRequest.of(
            "generic_amine_condition_probe",
            ReactionSide.of(
                List.of(
                    ReactionConditionEffectAnalyzerTest.participant(
                        ReactionConditionEffectAnalyzerTest.carbonProbe()))),
            ReactionConditions.of(
                null,
                null,
                null,
                List.of(
                    ReactionConditionComponent.named(
                        ReactionComponentRole.ADDITIVE, "cyclohexylmethyl amine")),
                List.of(),
                "generic amine source probe"),
            List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
            List.of(),
            null);

    final ReactionConditionEffectProfile profile =
        new ReactionConditionEffectAnalyzer().analyze(request);

    Assertions.assertTrue(profile.hasEffect(ReactionConditionEffectKind.AMINE_NUCLEOPHILE_SOURCE));
  }

  @Test
  void reactantBenzylamineNameAndSkeletalNitrogenAreClassifiedAsNucleophileSource() {
    final ReactionConditionEffectAnalyzer analyzer = new ReactionConditionEffectAnalyzer();
    final ReactionConditionEffectProfile namedProfile =
        analyzer.analyze(
            ReactionConditionEffectAnalyzerTest.requestWithReactant(
                ReactionConditionEffectAnalyzerTest.namedCarbonNitrogenProbe(
                    "benzylamine_named_probe", "Benzylamine skeletal input")));
    final ReactionConditionEffectProfile skeletalProfile =
        analyzer.analyze(
            ReactionConditionEffectAnalyzerTest.requestWithReactant(
                ReactionConditionEffectAnalyzerTest.namedCarbonNitrogenProbe(
                    "skeletal_amine_probe", "Skeletal neutral nitrogen reagent")));

    Assertions.assertTrue(
        namedProfile.hasEffect(ReactionConditionEffectKind.REACTANT_AMINE_NUCLEOPHILE));
    Assertions.assertTrue(
        skeletalProfile.hasEffect(ReactionConditionEffectKind.REACTANT_AMINE_NUCLEOPHILE));
    Assertions.assertTrue(
        ReactionConditionEffectAnalyzerTest.hasDescriptionPart(
            skeletalProfile, "amine-like nitrogen nucleophile"));
  }

  private static boolean hasDescriptionPart(
      final ReactionConditionEffectProfile profile, final String text) {
    final List<ReactionConditionEffect> effects = profile.effects();
    for (int i = 0; i < effects.size(); ++i) {
      if (effects.get(i).description().contains(text)) {
        return true;
      }
    }
    return false;
  }

  private static ReactionParticipant participant(final Molecule molecule) {
    return ReactionParticipant.of(molecule, StoichiometricCoefficient.ONE);
  }

  private static ReactionPredictionRequest requestWithReactant(final Molecule molecule) {
    return ReactionPredictionRequest.of(
        molecule.id().value() + "_condition_probe",
        ReactionSide.of(List.of(ReactionConditionEffectAnalyzerTest.participant(molecule))),
        ReactionConditions.of(
            null, null, null, List.of(), List.of(), "reactant identity nucleophile probe"),
        List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(),
        null);
  }

  private static ReactionPredictionRequest requestWithSolvent(final String solventName) {
    return ReactionPredictionRequest.of(
        solventName.replace(' ', '_') + "_condition_probe",
        ReactionSide.of(
            List.of(
                ReactionConditionEffectAnalyzerTest.participant(
                    ReactionConditionEffectAnalyzerTest.carbonProbe()))),
        ReactionConditions.of(
            null,
            null,
            null,
            List.of(ReactionConditionComponent.named(ReactionComponentRole.SOLVENT, solventName)),
            List.of(),
            "generic solvent class probe"),
        List.of(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION),
        List.of(),
        null);
  }

  private static Molecule carbonProbe() {
    return Molecule.of(
        MoleculeId.of("carbon_probe"),
        "Carbon probe",
        List.of(ReactionConditionEffectAnalyzerTest.atom("c0", "C")),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule namedCarbonNitrogenProbe(final String id, final String name) {
    return Molecule.of(
        MoleculeId.of(id),
        name,
        List.of(
            ReactionConditionEffectAnalyzerTest.atom("n0", "N"),
            ReactionConditionEffectAnalyzerTest.atom("c1", "C")),
        List.of(Bond.of(AtomId.of("n0"), AtomId.of("c1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Atom atom(final String id, final String symbol) {
    return Atom.of(AtomId.of(id), ElementSymbol.of(symbol), null);
  }
}
