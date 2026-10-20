/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffect;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/** Оценивает, насколько конкретный candidate согласован с найденными эффектами условий реакции. */
public final class OrganicReactionConditionCompatibilityScorer {

  public double score(
      final ReactionProductCandidate candidate,
      final ReactionConditionEffectProfile profile,
      final List<ReactionPredictionEvidence> evidence) {
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction product candidate must not be null.");
    }
    if (profile == null) {
      throw new IllegalArgumentException("Reaction condition effect profile must not be null.");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction prediction evidence list must not be null.");
    }
    if (profile.empty()) {
      return 0.0;
    }
    double score = 0.0;
    final CandidateMechanismSignals signals = CandidateMechanismSignals.of(candidate, evidence);
    final List<ReactionConditionEffect> effects = profile.effects();
    for (int i = 0; i < effects.size(); ++i) {
      score +=
          OrganicReactionConditionCompatibilityScorer.effectScore(
              signals, effects.get(i), evidence);
    }
    return score;
  }

  private static double effectScore(
      final CandidateMechanismSignals signals,
      final ReactionConditionEffect effect,
      final List<ReactionPredictionEvidence> evidence) {
    final ReactionConditionEffectKind kind = effect.kind();
    if (kind == ReactionConditionEffectKind.CONTRADICTORY_ACID_BASE
        || kind == ReactionConditionEffectKind.CONTRADICTORY_THERMAL_MODE
        || kind == ReactionConditionEffectKind.AIR_MOISTURE_RISK
        || kind == ReactionConditionEffectKind.MASS_RECIPE_REACTANT_UNMATCHED
        || kind == ReactionConditionEffectKind.PURIFICATION_OBSERVABILITY) {
      return OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
          evidence,
          "Condition risk is applied to this candidate: " + effect.description(),
          effect.scoreAdjustment());
    }
    double score = 0.0;
    if (signals.crossCoupling()
        && OrganicReactionConditionCompatibilityScorer.crossCouplingEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate cross-coupling evidence is compatible with catalyst/inert/dry conditions.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.8);
    }
    if (signals.nucleophilicAromaticSubstitution()
        && OrganicReactionConditionCompatibilityScorer.snarEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate SNAr/leaving-group evidence is compatible with base, polar aprotic solvent or heating.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.75);
    }
    if (signals.nucleophilicAromaticSubstitution()
        && OrganicReactionConditionCompatibilityScorer.directSnarConflictEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate direct retained-ring substitution is weakened by acidic, protic or weak-nucleophile conditions that can open competing addition/ring-opening pathways.",
              -Math.max(0.015, Math.abs(effect.scoreAdjustment()) * 0.55));
    }
    if (signals.nucleophilicAromaticSubstitution()
        && kind == ReactionConditionEffectKind.DILUTE_CONDITION_REGIME) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate direct bimolecular substitution is weakened by dilute condition regime.",
              -Math.max(0.010, Math.abs(effect.scoreAdjustment()) * 0.65));
    }
    if (signals.additionRingOpeningRingClosure()
        && OrganicReactionConditionCompatibilityScorer.anrorcEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate addition/ring-opening/ring-closure pathway is compatible with activated heterocycle, amine nucleophile, thermal and solvation effects.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.90);
    }
    if (signals.additionRingOpeningRingClosure()
        && OrganicReactionConditionCompatibilityScorer.anrorcRiskEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate addition/ring-opening/ring-closure pathway is weakened by conditions that favor proton trapping, weak nucleophile capture or uncontrolled hydrolysis.",
              -Math.max(0.015, Math.abs(effect.scoreAdjustment()) * 0.45));
    }
    if (signals.nucleophilicAromaticSubstitution()
        && kind == ReactionConditionEffectKind.BULKY_BASE_SELECTIVITY) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate direct substitution is weakened by bulky-base selectivity pressure.",
              effect.scoreAdjustment() * 0.8);
    }
    if (signals.oxidation() && OrganicReactionConditionCompatibilityScorer.oxidationEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate oxidation evidence is compatible with controlled dry or low-temperature conditions.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.7);
    }
    if (signals.alkylation()
        && OrganicReactionConditionCompatibilityScorer.alkylationEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate N-alkylation evidence is compatible with base or thermal activation.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.65);
    }
    if (signals.radicalFunctionalization()
        && OrganicReactionConditionCompatibilityScorer.radicalEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate radical functionalization pathway is compatible with photochemical/electrochemical activation, controlled oxygen exposure or thermal initiation context.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.80);
    }
    if (signals.radicalFunctionalization()
        && kind == ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate radical pathway is penalized because concentrated conditions can increase uncontrolled radical coupling.",
              -Math.max(0.010, Math.abs(effect.scoreAdjustment()) * 0.45));
    }
    if (signals.radicalFunctionalization()
        && OrganicReactionConditionCompatibilityScorer.radicalTrapEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate radical functionalization is weakened by oxygen or radical-trap conditions that intercept radical chains.",
              -Math.max(0.018, Math.abs(effect.scoreAdjustment()) * 0.85));
    }
    if (!signals.radicalFunctionalization()
        && OrganicReactionConditionCompatibilityScorer.radicalSelectiveEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Non-radical candidate is penalized because the condition profile contains single-electron or radical-initiation activation.",
              -Math.max(0.010, Math.abs(effect.scoreAdjustment()) * 0.35));
    }
    if (signals.oxidation() && kind == ReactionConditionEffectKind.OXIDANT_PRESENT) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate oxidation evidence is directly supported by oxidant conditions.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.85);
    }
    if (signals.oxidation() && kind == ReactionConditionEffectKind.REDUCTANT_PRESENT) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate oxidation evidence is weakened by reductive condition signals.",
              -Math.max(0.014, Math.abs(effect.scoreAdjustment()) * 0.70));
    }
    if (signals.multicomponentCondensation()
        && OrganicReactionConditionCompatibilityScorer.multicomponentCondensationEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate multicomponent condensation pathway is compatible with dehydrating, acid/base and concentration effects.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.70);
    }
    if (signals.multicomponentCondensation()
        && kind == ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate multicomponent condensation is limited by viscous-medium mass transfer and local concentration gradients.",
              -Math.max(0.012, Math.abs(effect.scoreAdjustment()) * 0.45));
    }
    if (signals.cycloaddition()
        && OrganicReactionConditionCompatibilityScorer.cycloadditionEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate cycloaddition/pericyclic pathway is compatible with thermal, pressure, concentration and orbital-matching condition effects.",
              Math.max(0.0, effect.scoreAdjustment()) * 0.78);
    }
    if (signals.cycloaddition()
        && OrganicReactionConditionCompatibilityScorer.cycloadditionRiskEffect(kind)) {
      score +=
          OrganicReactionConditionCompatibilityScorer.addCompatibilityEvidence(
              evidence,
              "Candidate cycloaddition/pericyclic pathway is weakened by dilution, viscosity, moisture or radical/acid-base side-channel pressure.",
              -Math.max(0.012, Math.abs(effect.scoreAdjustment()) * 0.55));
    }
    return score;
  }

  private static boolean crossCouplingEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.PALLADIUM_CATALYSIS
        || kind == ReactionConditionEffectKind.COPPER_CATALYSIS
        || kind == ReactionConditionEffectKind.NICKEL_CATALYSIS
        || kind == ReactionConditionEffectKind.INERT_DRY_CONTROL
        || kind == ReactionConditionEffectKind.THERMAL_ACTIVATION
        || kind == ReactionConditionEffectKind.THERMAL_DOSE_REGIME
        || kind == ReactionConditionEffectKind.THERMAL_MASS_TRANSFER_WINDOW
        || kind == ReactionConditionEffectKind.MICROWAVE_ACCELERATION
        || kind == ReactionConditionEffectKind.SEALED_VESSEL_RETENTION
        || kind == ReactionConditionEffectKind.FLOW_REACTOR_CONTROL
        || kind == ReactionConditionEffectKind.PRESSURIZED_REACTION_REGIME
        || kind == ReactionConditionEffectKind.SOLVENT_HIGH_BOILING;
  }

  private static boolean snarEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC
        || kind == ReactionConditionEffectKind.BASE_PROMOTION
        || kind == ReactionConditionEffectKind.STRONG_BASE_PROMOTION
        || kind == ReactionConditionEffectKind.INORGANIC_BASE_BUFFERING
        || kind == ReactionConditionEffectKind.AMINE_NUCLEOPHILE_SOURCE
        || kind == ReactionConditionEffectKind.REACTANT_AMINE_NUCLEOPHILE
        || kind == ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM
        || kind == ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION
        || kind == ReactionConditionEffectKind.THERMAL_ACTIVATION
        || kind == ReactionConditionEffectKind.THERMAL_DOSE_REGIME
        || kind == ReactionConditionEffectKind.THERMAL_MASS_TRANSFER_WINDOW
        || kind == ReactionConditionEffectKind.MICROWAVE_ACCELERATION
        || kind == ReactionConditionEffectKind.SEALED_VESSEL_RETENTION
        || kind == ReactionConditionEffectKind.FLOW_REACTOR_CONTROL
        || kind == ReactionConditionEffectKind.ADDITION_RATE_SELECTIVITY
        || kind == ReactionConditionEffectKind.SUSTAINED_TIME_CONVERSION
        || kind == ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME
        || kind == ReactionConditionEffectKind.MASS_QUANTIFIED_REACTION_SCALE
        || kind == ReactionConditionEffectKind.MASS_TO_MOLE_STOICHIOMETRY
        || kind == ReactionConditionEffectKind.HETEROCYCLE_COMPATIBILITY;
  }

  private static boolean directSnarConflictEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.BRONSTED_ACID_ACTIVATION
        || kind == ReactionConditionEffectKind.SOLVENT_POLAR_PROTIC
        || kind == ReactionConditionEffectKind.WEAK_NUCLEOPHILE_SOURCE;
  }

  private static boolean anrorcEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.REACTANT_AMINE_NUCLEOPHILE
        || kind == ReactionConditionEffectKind.AMINE_NUCLEOPHILE_SOURCE
        || kind == ReactionConditionEffectKind.BASE_PROMOTION
        || kind == ReactionConditionEffectKind.STRONG_BASE_PROMOTION
        || kind == ReactionConditionEffectKind.THERMAL_ACTIVATION
        || kind == ReactionConditionEffectKind.THERMAL_DOSE_REGIME
        || kind == ReactionConditionEffectKind.THERMAL_MASS_TRANSFER_WINDOW
        || kind == ReactionConditionEffectKind.MICROWAVE_ACCELERATION
        || kind == ReactionConditionEffectKind.SEALED_VESSEL_RETENTION
        || kind == ReactionConditionEffectKind.ADDITION_RATE_SELECTIVITY
        || kind == ReactionConditionEffectKind.SUSTAINED_TIME_CONVERSION
        || kind == ReactionConditionEffectKind.SOLVENT_HIGH_BOILING
        || kind == ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM
        || kind == ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION
        || kind == ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME
        || kind == ReactionConditionEffectKind.HETEROCYCLE_COMPATIBILITY
        || kind == ReactionConditionEffectKind.QUANTIFIED_STOICHIOMETRY_CONTROL
        || kind == ReactionConditionEffectKind.MASS_QUANTIFIED_REACTION_SCALE
        || kind == ReactionConditionEffectKind.MASS_TO_MOLE_STOICHIOMETRY
        || kind == ReactionConditionEffectKind.EXCESS_REAGENT_DRIVING_FORCE;
  }

  private static boolean anrorcRiskEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.BRONSTED_ACID_ACTIVATION
        || kind == ReactionConditionEffectKind.WEAK_NUCLEOPHILE_SOURCE
        || kind == ReactionConditionEffectKind.DILUTE_CONDITION_REGIME
        || kind == ReactionConditionEffectKind.AIR_MOISTURE_RISK
        || kind == ReactionConditionEffectKind.PURITY_LIMITATION
        || kind == ReactionConditionEffectKind.CONTRADICTORY_ACID_BASE;
  }

  private static boolean oxidationEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.LOW_TEMPERATURE_SELECTIVITY
        || kind == ReactionConditionEffectKind.INERT_DRY_CONTROL
        || kind == ReactionConditionEffectKind.SOLVENT_HALOGENATED;
  }

  private static boolean alkylationEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.BASE_PROMOTION
        || kind == ReactionConditionEffectKind.THERMAL_ACTIVATION
        || kind == ReactionConditionEffectKind.THERMAL_DOSE_REGIME
        || kind == ReactionConditionEffectKind.THERMAL_MASS_TRANSFER_WINDOW
        || kind == ReactionConditionEffectKind.MICROWAVE_ACCELERATION
        || kind == ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC
        || kind == ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION;
  }

  private static boolean radicalEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.THERMAL_ACTIVATION
        || kind == ReactionConditionEffectKind.THERMAL_DOSE_REGIME
        || kind == ReactionConditionEffectKind.THERMAL_MASS_TRANSFER_WINDOW
        || kind == ReactionConditionEffectKind.LOW_TEMPERATURE_SELECTIVITY
        || kind == ReactionConditionEffectKind.SEALED_VESSEL_RETENTION
        || kind == ReactionConditionEffectKind.FLOW_REACTOR_CONTROL
        || kind == ReactionConditionEffectKind.CATALYST_LOADING_WINDOW
        || kind == ReactionConditionEffectKind.CONCENTRATION_REGIME
        || kind == ReactionConditionEffectKind.PRESSURIZED_REACTION_REGIME
        || kind == ReactionConditionEffectKind.PHOTOCHEMICAL_ACTIVATION
        || kind == ReactionConditionEffectKind.ELECTROCHEMICAL_ACTIVATION
        || kind == ReactionConditionEffectKind.RADICAL_INITIATOR_PRESENT
        || kind == ReactionConditionEffectKind.OXIDANT_PRESENT
        || kind == ReactionConditionEffectKind.REDUCTANT_PRESENT;
  }

  private static boolean radicalSelectiveEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.PHOTOCHEMICAL_ACTIVATION
        || kind == ReactionConditionEffectKind.ELECTROCHEMICAL_ACTIVATION
        || kind == ReactionConditionEffectKind.RADICAL_INITIATOR_PRESENT;
  }

  private static boolean radicalTrapEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.OXYGEN_RADICAL_TRAP
        || kind == ReactionConditionEffectKind.AIR_MOISTURE_RISK;
  }

  private static boolean multicomponentCondensationEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.BRONSTED_ACID_ACTIVATION
        || kind == ReactionConditionEffectKind.BASE_PROMOTION
        || kind == ReactionConditionEffectKind.THERMAL_ACTIVATION
        || kind == ReactionConditionEffectKind.THERMAL_DOSE_REGIME
        || kind == ReactionConditionEffectKind.THERMAL_MASS_TRANSFER_WINDOW
        || kind == ReactionConditionEffectKind.MICROWAVE_ACCELERATION
        || kind == ReactionConditionEffectKind.FLOW_REACTOR_CONTROL
        || kind == ReactionConditionEffectKind.ADDITION_RATE_SELECTIVITY
        || kind == ReactionConditionEffectKind.SUSTAINED_TIME_CONVERSION
        || kind == ReactionConditionEffectKind.CONCENTRATION_REGIME
        || kind == ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME
        || kind == ReactionConditionEffectKind.SOLVENT_HIGH_BOILING
        || kind == ReactionConditionEffectKind.QUANTIFIED_STOICHIOMETRY_CONTROL
        || kind == ReactionConditionEffectKind.MASS_QUANTIFIED_REACTION_SCALE
        || kind == ReactionConditionEffectKind.MASS_TO_MOLE_STOICHIOMETRY
        || kind == ReactionConditionEffectKind.EXCESS_REAGENT_DRIVING_FORCE;
  }

  private static boolean cycloadditionEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.THERMAL_ACTIVATION
        || kind == ReactionConditionEffectKind.THERMAL_DOSE_REGIME
        || kind == ReactionConditionEffectKind.THERMAL_MASS_TRANSFER_WINDOW
        || kind == ReactionConditionEffectKind.MICROWAVE_ACCELERATION
        || kind == ReactionConditionEffectKind.SEALED_VESSEL_RETENTION
        || kind == ReactionConditionEffectKind.FLOW_REACTOR_CONTROL
        || kind == ReactionConditionEffectKind.PRESSURIZED_REACTION_REGIME
        || kind == ReactionConditionEffectKind.CONCENTRATION_REGIME
        || kind == ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME
        || kind == ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC
        || kind == ReactionConditionEffectKind.SOLVENT_HIGH_BOILING
        || kind == ReactionConditionEffectKind.LOW_TEMPERATURE_SELECTIVITY
        || kind == ReactionConditionEffectKind.QUANTIFIED_STOICHIOMETRY_CONTROL
        || kind == ReactionConditionEffectKind.MASS_QUANTIFIED_REACTION_SCALE
        || kind == ReactionConditionEffectKind.MASS_TO_MOLE_STOICHIOMETRY
        || kind == ReactionConditionEffectKind.CATALYST_LOADING_WINDOW;
  }

  private static boolean cycloadditionRiskEffect(final ReactionConditionEffectKind kind) {
    return kind == ReactionConditionEffectKind.DILUTE_CONDITION_REGIME
        || kind == ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM
        || kind == ReactionConditionEffectKind.AIR_MOISTURE_RISK
        || kind == ReactionConditionEffectKind.PURITY_LIMITATION
        || kind == ReactionConditionEffectKind.CONTRADICTORY_ACID_BASE
        || kind == ReactionConditionEffectKind.RADICAL_INITIATOR_PRESENT
        || kind == ReactionConditionEffectKind.OXYGEN_RADICAL_TRAP;
  }

  private static double addCompatibilityEvidence(
      final List<ReactionPredictionEvidence> evidence,
      final String description,
      final double score) {
    if (score == 0.0) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            description,
            Math.max(-1.0, Math.min(1.0, score))));
    return score;
  }

  private static final class CandidateMechanismSignals {

    private final boolean crossCoupling;
    private final boolean nucleophilicAromaticSubstitution;
    private final boolean additionRingOpeningRingClosure;
    private final boolean oxidation;
    private final boolean alkylation;
    private final boolean radicalFunctionalization;
    private final boolean multicomponentCondensation;
    private final boolean cycloaddition;

    private CandidateMechanismSignals(
        final boolean crossCoupling,
        final boolean nucleophilicAromaticSubstitution,
        final boolean additionRingOpeningRingClosure,
        final boolean oxidation,
        final boolean alkylation,
        final boolean radicalFunctionalization,
        final boolean multicomponentCondensation,
        final boolean cycloaddition) {
      this.crossCoupling = crossCoupling;
      this.nucleophilicAromaticSubstitution = nucleophilicAromaticSubstitution;
      this.additionRingOpeningRingClosure = additionRingOpeningRingClosure;
      this.oxidation = oxidation;
      this.alkylation = alkylation;
      this.radicalFunctionalization = radicalFunctionalization;
      this.multicomponentCondensation = multicomponentCondensation;
      this.cycloaddition = cycloaddition;
    }

    static CandidateMechanismSignals of(
        final ReactionProductCandidate candidate,
        final List<ReactionPredictionEvidence> accumulatedEvidence) {
      boolean crossCoupling = false;
      boolean snar = false;
      boolean anrorc = false;
      boolean oxidation = false;
      boolean alkylation = false;
      boolean radical = false;
      boolean multicomponent = false;
      boolean cycloaddition = false;
      final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
      for (int i = 0; i < mechanisms.size(); ++i) {
        final ReactionMechanismFamily family = mechanisms.get(i).family();
        crossCoupling |= CandidateMechanismSignals.crossCouplingFamily(family);
        snar |= family == ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION;
        anrorc |= family == ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE;
        oxidation |= family == ReactionMechanismFamily.OXIDATION;
        alkylation |=
            family == ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN1
                || family == ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2;
        radical |=
            family == ReactionMechanismFamily.RADICAL_SUBSTITUTION
                || family == ReactionMechanismFamily.RADICAL_CHAIN
                || family == ReactionMechanismFamily.RADICAL_CYCLIZATION
                || family == ReactionMechanismFamily.PHOTOCHEMICAL;
        multicomponent |=
            family == ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION
                || family == ReactionMechanismFamily.UGI_REACTION
                || family == ReactionMechanismFamily.PASSERINI_REACTION
                || family == ReactionMechanismFamily.BIGINELLI_REACTION
                || family == ReactionMechanismFamily.HANTZSCH_SYNTHESIS;
        cycloaddition |= CandidateMechanismSignals.cycloadditionFamily(family);
      }
      final List<ReactionPredictionEvidence> candidateEvidence = candidate.evidence();
      for (int i = 0; i < candidateEvidence.size(); ++i) {
        final String description = candidateEvidence.get(i).description().toLowerCase(Locale.ROOT);
        crossCoupling |=
            CandidateMechanismSignals.containsAny(
                description,
                "suzuki",
                "coupling",
                "organoboron",
                "organometallic",
                "boronate",
                "boronic");
        snar |=
            CandidateMechanismSignals.containsAny(
                description,
                "snar",
                "nucleophilic aromatic",
                "leaving-group",
                "leaving group",
                "fluoride");
        anrorc |=
            CandidateMechanismSignals.containsAny(
                description,
                "anrorc",
                "ring-opening",
                "ring opening",
                "ring closure",
                "addition/ring-opening/ring-closure");
        oxidation |=
            CandidateMechanismSignals.containsAny(description, "oxidation", "n-oxide", "oxide");
        alkylation |= CandidateMechanismSignals.containsAny(description, "alkylation", "n-alkyl");
        radical |=
            CandidateMechanismSignals.containsAny(
                description, "radical", "minisci", "photochemical");
        multicomponent |=
            CandidateMechanismSignals.containsAny(
                description,
                "multicomponent",
                "ugi",
                "passerini",
                "biginelli",
                "hantzsch",
                "isocyanide",
                "isocyanate");
        cycloaddition |=
            CandidateMechanismSignals.containsAny(
                description,
                "cycloaddition",
                "pericyclic",
                "dipolar",
                "diels",
                "click",
                "orbital matching");
      }
      for (int i = 0; i < accumulatedEvidence.size(); ++i) {
        final String description =
            accumulatedEvidence.get(i).description().toLowerCase(Locale.ROOT);
        crossCoupling |=
            CandidateMechanismSignals.containsAny(
                description,
                "suzuki",
                "coupling",
                "organoboron",
                "organometallic",
                "boronate",
                "boronic");
        snar |=
            CandidateMechanismSignals.containsAny(
                description,
                "snar",
                "nucleophilic aromatic",
                "leaving-group",
                "leaving group",
                "fluoride");
        anrorc |=
            CandidateMechanismSignals.containsAny(
                description,
                "anrorc",
                "ring-opening",
                "ring opening",
                "ring closure",
                "addition/ring-opening/ring-closure");
        oxidation |=
            CandidateMechanismSignals.containsAny(description, "oxidation", "n-oxide", "oxide");
        alkylation |= CandidateMechanismSignals.containsAny(description, "alkylation", "n-alkyl");
        radical |=
            CandidateMechanismSignals.containsAny(
                description, "radical", "minisci", "photochemical");
        multicomponent |=
            CandidateMechanismSignals.containsAny(
                description,
                "multicomponent",
                "ugi",
                "passerini",
                "biginelli",
                "hantzsch",
                "isocyanide",
                "isocyanate");
        cycloaddition |=
            CandidateMechanismSignals.containsAny(
                description,
                "cycloaddition",
                "pericyclic",
                "dipolar",
                "diels",
                "click",
                "orbital matching");
      }
      return new CandidateMechanismSignals(
          crossCoupling,
          snar,
          anrorc,
          oxidation,
          alkylation,
          radical,
          multicomponent,
          cycloaddition);
    }

    boolean crossCoupling() {
      return this.crossCoupling;
    }

    boolean nucleophilicAromaticSubstitution() {
      return this.nucleophilicAromaticSubstitution;
    }

    boolean additionRingOpeningRingClosure() {
      return this.additionRingOpeningRingClosure;
    }

    boolean oxidation() {
      return this.oxidation;
    }

    boolean alkylation() {
      return this.alkylation;
    }

    boolean radicalFunctionalization() {
      return this.radicalFunctionalization;
    }

    boolean multicomponentCondensation() {
      return this.multicomponentCondensation;
    }

    boolean cycloaddition() {
      return this.cycloaddition;
    }

    private static boolean crossCouplingFamily(final ReactionMechanismFamily family) {
      return family == ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING
          || family == ReactionMechanismFamily.SUZUKI_COUPLING
          || family == ReactionMechanismFamily.HECK_COUPLING
          || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
          || family == ReactionMechanismFamily.NEGISHI_COUPLING
          || family == ReactionMechanismFamily.STILLE_COUPLING
          || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION;
    }

    private static boolean cycloadditionFamily(final ReactionMechanismFamily family) {
      return family == ReactionMechanismFamily.PERICYCLIC
          || family == ReactionMechanismFamily.CYCLOADDITION
          || family == ReactionMechanismFamily.CLICK_CYCLOADDITION
          || family == ReactionMechanismFamily.DIELS_ALDER
          || family == ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION
          || family == ReactionMechanismFamily.THIOL_ENE_CLICK;
    }

    private static boolean containsAny(final String text, final String... markers) {
      for (int i = 0; i < markers.length; ++i) {
        if (text.contains(markers[i])) {
          return true;
        }
      }
      return false;
    }
  }
}
