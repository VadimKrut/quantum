/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffect;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionCandidateScorer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionConditionCompatibilityScorer;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

public final class OrganicReactionConditionCompatibilityScorerTest {

  @Test
  public void anrorcReceivesMechanismSpecificSupportAndRiskWithoutObservedLookup() {
    final OrganicReactionConditionCompatibilityScorer scorer =
        new OrganicReactionConditionCompatibilityScorer();
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ReactionConditionEffectProfile profile =
        ReactionConditionEffectProfile.of(
            List.of(
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.REACTANT_AMINE_NUCLEOPHILE, 0.070, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.THERMAL_ACTIVATION, 0.060, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.BRONSTED_ACID_ACTIVATION, 0.040, false)));
    final ReactionProductCandidate candidate =
        OrganicReactionConditionCompatibilityScorerTest.candidate(
            ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE,
            "ANRORC candidate generated from staged ring-opening mechanism.");

    final double score = scorer.score(candidate, profile, evidence);

    Assertions.assertTrue(score > 0.05);
    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(
                item ->
                    item.description()
                        .contains("addition/ring-opening/ring-closure pathway is compatible")));
    Assertions.assertTrue(
        evidence.stream().anyMatch(item -> item.description().contains("proton trapping")));
  }

  @Test
  public void directSnarIsPenalizedWhereAnrorcCanCompete() {
    final OrganicReactionConditionCompatibilityScorer scorer =
        new OrganicReactionConditionCompatibilityScorer();
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ReactionConditionEffectProfile profile =
        ReactionConditionEffectProfile.of(
            List.of(
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.SOLVENT_POLAR_PROTIC, 0.035, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.WEAK_NUCLEOPHILE_SOURCE, -0.040, true)));
    final ReactionProductCandidate candidate =
        OrganicReactionConditionCompatibilityScorerTest.candidate(
            ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
            "Direct SNAr leaving-group retained-ring substitution candidate.");

    final double score = scorer.score(candidate, profile, evidence);

    Assertions.assertTrue(score < 0.0);
    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(
                item ->
                    item.description().contains("direct retained-ring substitution is weakened")));
  }

  @Test
  public void radicalAndMulticomponentPathsUseDifferentConditionSignals() {
    final OrganicReactionConditionCompatibilityScorer scorer =
        new OrganicReactionConditionCompatibilityScorer();
    final ArrayList<ReactionPredictionEvidence> radicalEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> multicomponentEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ReactionConditionEffectProfile profile =
        ReactionConditionEffectProfile.of(
            List.of(
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.CATALYST_LOADING_WINDOW, 0.035, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.CONCENTRATION_REGIME, 0.025, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.EXCESS_REAGENT_DRIVING_FORCE, 0.045, false)));

    final double radicalScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                "Minisci radical heterocycle functionalization candidate."),
            profile,
            radicalEvidence);
    final double multicomponentScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.BIGINELLI_REACTION,
                "Biginelli multicomponent condensation candidate."),
            profile,
            multicomponentEvidence);

    Assertions.assertTrue(radicalScore > 0.0);
    Assertions.assertTrue(multicomponentScore > 0.0);
    Assertions.assertTrue(
        radicalEvidence.stream()
            .anyMatch(item -> item.description().contains("radical functionalization pathway")));
    Assertions.assertTrue(
        multicomponentEvidence.stream()
            .anyMatch(item -> item.description().contains("multicomponent condensation pathway")));
  }

  @Test
  public void singleElectronModeSupportsRadicalsAndPenalizesNonRadicalCompetitors() {
    final OrganicReactionConditionCompatibilityScorer scorer =
        new OrganicReactionConditionCompatibilityScorer();
    final ArrayList<ReactionPredictionEvidence> radicalEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> directSnarEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ReactionConditionEffectProfile profile =
        ReactionConditionEffectProfile.of(
            List.of(
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.PHOTOCHEMICAL_ACTIVATION, 0.065, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.RADICAL_INITIATOR_PRESENT, 0.070, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.OXYGEN_RADICAL_TRAP, -0.050, true)));

    final double radicalScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.RADICAL_CHAIN, "Minisci radical chain candidate."),
            profile,
            radicalEvidence);
    final double directSnarScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                "Direct SNAr retained-ring substitution candidate."),
            profile,
            directSnarEvidence);

    Assertions.assertTrue(radicalScore > 0.0);
    Assertions.assertTrue(directSnarScore < 0.0);
    Assertions.assertTrue(
        radicalEvidence.stream()
            .anyMatch(item -> item.description().contains("radical functionalization pathway")));
    Assertions.assertTrue(
        radicalEvidence.stream()
            .anyMatch(item -> item.description().contains("oxygen or radical-trap conditions")));
    Assertions.assertTrue(
        directSnarEvidence.stream()
            .anyMatch(
                item ->
                    item.description()
                        .contains("single-electron or radical-initiation activation")));
  }

  @Test
  public void
      polyetherMediumSupportsIonicHeterocyclePathsButPenalizesMassTransferLimitedCondensation() {
    final OrganicReactionConditionCompatibilityScorer scorer =
        new OrganicReactionConditionCompatibilityScorer();
    final ArrayList<ReactionPredictionEvidence> snarEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> anrorcEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> condensationEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ReactionConditionEffectProfile profile =
        ReactionConditionEffectProfile.of(
            List.of(
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM, 0.030, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION, 0.025, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.SUSTAINED_TIME_CONVERSION, 0.022, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.SOLVENT_HIGH_BOILING, 0.040, false)));

    final double snarScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                "SNAr leaving-group displacement candidate."),
            profile,
            snarEvidence);
    final double anrorcScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE,
                "ANRORC heterocycle amination candidate."),
            profile,
            anrorcEvidence);
    final double condensationScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.BIGINELLI_REACTION,
                "Multicomponent condensation candidate."),
            profile,
            condensationEvidence);

    Assertions.assertTrue(snarScore > 0.0);
    Assertions.assertTrue(anrorcScore > 0.0);
    Assertions.assertTrue(condensationScore > 0.0);
    Assertions.assertTrue(
        snarEvidence.stream()
            .anyMatch(item -> item.description().contains("SNAr/leaving-group evidence")));
    Assertions.assertTrue(
        anrorcEvidence.stream()
            .anyMatch(
                item -> item.description().contains("addition/ring-opening/ring-closure pathway")));
    Assertions.assertTrue(
        condensationEvidence.stream()
            .anyMatch(item -> item.description().contains("viscous-medium mass transfer")));
  }

  @Test
  public void physicalRegimeEffectsAreConsumedByCandidateCompatibilityScoring() {
    final OrganicReactionConditionCompatibilityScorer scorer =
        new OrganicReactionConditionCompatibilityScorer();
    final ArrayList<ReactionPredictionEvidence> snarEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ArrayList<ReactionPredictionEvidence> radicalEvidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ReactionConditionEffectProfile profile =
        ReactionConditionEffectProfile.of(
            List.of(
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.THERMAL_DOSE_REGIME, 0.055, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME, 0.035, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.DILUTE_CONDITION_REGIME, -0.030, true),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.PRESSURIZED_REACTION_REGIME, 0.030, false)));

    final double snarScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                "SNAr leaving-group displacement candidate."),
            profile,
            snarEvidence);
    final double radicalScore =
        scorer.score(
            OrganicReactionConditionCompatibilityScorerTest.candidate(
                ReactionMechanismFamily.RADICAL_CHAIN, "Radical chain candidate."),
            profile,
            radicalEvidence);

    Assertions.assertTrue(snarScore > 0.0);
    Assertions.assertTrue(radicalScore > 0.0);
    Assertions.assertTrue(
        snarEvidence.stream()
            .anyMatch(item -> item.description().contains("dilute condition regime")));
    Assertions.assertTrue(
        radicalEvidence.stream()
            .anyMatch(item -> item.description().contains("concentrated conditions")));
  }

  @Test
  public void cycloadditionPathsUseOrbitalAndPhysicalRegimeConditionSignals() {
    final OrganicReactionConditionCompatibilityScorer scorer =
        new OrganicReactionConditionCompatibilityScorer();
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>();
    final ReactionConditionEffectProfile profile =
        ReactionConditionEffectProfile.of(
            List.of(
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.THERMAL_ACTIVATION, 0.060, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.PRESSURIZED_REACTION_REGIME, 0.030, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME, 0.035, false),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.DILUTE_CONDITION_REGIME, -0.030, true),
                OrganicReactionConditionCompatibilityScorerTest.effect(
                    ReactionConditionEffectKind.OXYGEN_RADICAL_TRAP, -0.050, true)));
    final ReactionProductCandidate candidate =
        OrganicReactionConditionCompatibilityScorerTest.candidate(
            ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION,
            "1,3-dipolar cycloaddition candidate with orbital matching evidence.");

    final double score = scorer.score(candidate, profile, evidence);

    Assertions.assertTrue(score > 0.0);
    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(
                item ->
                    item.description().contains("cycloaddition/pericyclic pathway is compatible")));
    Assertions.assertTrue(
        evidence.stream()
            .anyMatch(
                item ->
                    item.description().contains("cycloaddition/pericyclic pathway is weakened")));
  }

  @Test
  public void calibrationPriorEvidenceNeverChangesPredictionWeightEvenIfStoredWithWeight() {
    final ReactionPredictionEvidence calibration =
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.CALIBRATION_PRIOR,
            "Observed product distribution is an audit-only validation row.",
            0.9);
    final ReactionPredictionEvidence mechanistic =
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Mechanistic evidence remains causal scoring input.",
            0.12);

    Assertions.assertEquals(0.0, OrganicReactionCandidateScorer.predictionWeight(calibration));
    Assertions.assertEquals(0.12, OrganicReactionCandidateScorer.predictionWeight(mechanistic));
  }

  private static ReactionConditionEffect effect(
      final ReactionConditionEffectKind kind, final double scoreAdjustment, final boolean risk) {
    return ReactionConditionEffect.of(
        kind, ReactionPredictionEvidenceKind.KINETIC_FACTOR, kind.name(), scoreAdjustment, risk);
  }

  private static ReactionProductCandidate candidate(
      final ReactionMechanismFamily family, final String description) {
    return ReactionProductCandidate.of(
        ReactionSide.of(
            List.of(
                ReactionParticipant.of(
                    OrganicReactionConditionCompatibilityScorerTest.singleCarbonMolecule(),
                    StoichiometricCoefficient.ONE))),
        0.5,
        50.0,
        "Mechanism condition compatibility probe.",
        List.of(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.USER_RULE, description, 0.0)),
        List.of(
            PredictedReactionMechanism.of(
                "compatibility_probe_" + family.name(), family, 0.5, 50.0)));
  }

  private static Molecule singleCarbonMolecule() {
    return Molecule.of(
        MoleculeId.of("compatibility_probe_molecule"),
        "Compatibility probe molecule",
        List.of(Atom.of(AtomId.of("c0"), ElementSymbol.of("C"), null)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }
}
