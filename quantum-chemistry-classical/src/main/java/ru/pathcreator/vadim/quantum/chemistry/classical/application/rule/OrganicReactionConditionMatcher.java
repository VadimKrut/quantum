/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;

/**
 * Сравнивает реальные условия реакции с условиями, которые ожидает органическое правило.
 */
public final class OrganicReactionConditionMatcher {

  private static final double MATCHED_FEATURE_BONUS = 0.025;
  private static final double MISSING_FEATURE_PENALTY = -0.015;
  private static final double EMPTY_CONDITIONS_PENALTY = -0.05;
  private static final double CONTRADICTION_PENALTY = -0.20;
  private static final double MAXIMUM_BONUS = 0.10;
  private static final double MAXIMUM_MISSING_PENALTY = -0.08;

  public OrganicReactionConditionAssessment assess(
      final ReactionConditions conditions,
      final List<ReactionConditionFeatureKind> supportiveFeatures) {
    final List<ReactionConditionFeatureKind> checkedFeatures =
        OrganicReactionConditionMatcher.checkedFeatures(supportiveFeatures);
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(checkedFeatures.size() + 4);
    double score = 0.0;
    if (conditions == null || conditions.empty()) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Rule has no reaction conditions to compare against; product generation remains approximate.",
              EMPTY_CONDITIONS_PENALTY));
      return OrganicReactionConditionAssessment.of(EMPTY_CONDITIONS_PENALTY, false, evidence);
    }
    score +=
        OrganicReactionConditionMatcher.supportiveFeatureScore(
            conditions, checkedFeatures, evidence);
    score += OrganicReactionConditionMatcher.componentContextScore(conditions, evidence);
    final double contradictionScore =
        OrganicReactionConditionMatcher.contradictionScore(conditions, evidence);
    return OrganicReactionConditionAssessment.of(
        score + contradictionScore, contradictionScore <= CONTRADICTION_PENALTY, evidence);
  }

  private static double supportiveFeatureScore(
      final ReactionConditions conditions,
      final List<ReactionConditionFeatureKind> supportiveFeatures,
      final List<ReactionPredictionEvidence> evidence) {
    if (supportiveFeatures.isEmpty()) {
      return 0.0;
    }
    double bonus = 0.0;
    double penalty = 0.0;
    for (int i = 0; i < supportiveFeatures.size(); ++i) {
      final ReactionConditionFeatureKind feature = supportiveFeatures.get(i);
      if (OrganicReactionConditionMatcher.hasExplicitOrInferredFeature(conditions, feature)) {
        bonus += MATCHED_FEATURE_BONUS;
        evidence.add(
            ReactionPredictionEvidence.of(
                OrganicReactionConditionMatcher.kindForFeature(feature),
                "Condition feature supports this rule explicitly or by inferred context: "
                    + feature.name()
                    + ".",
                MATCHED_FEATURE_BONUS));
        continue;
      }
      penalty += MISSING_FEATURE_PENALTY;
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Condition feature expected by this rule is absent: " + feature.name() + ".",
              MISSING_FEATURE_PENALTY));
    }
    return Math.min(MAXIMUM_BONUS, bonus) + Math.max(MAXIMUM_MISSING_PENALTY, penalty);
  }

  private static double componentContextScore(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    score +=
        OrganicReactionConditionMatcher.roleContextScore(
            conditions,
            evidence,
            ReactionComponentRole.CATALYST,
            ReactionPredictionEvidenceKind.CATALYST_EFFECT,
            "Catalyst component supports rule-level product generation context.",
            0.020);
    score +=
        OrganicReactionConditionMatcher.roleContextScore(
            conditions,
            evidence,
            ReactionComponentRole.LIGAND,
            ReactionPredictionEvidenceKind.CATALYST_EFFECT,
            "Ligand component supports metal- or catalyst-sensitive rule context.",
            0.012);
    score +=
        OrganicReactionConditionMatcher.roleContextScore(
            conditions,
            evidence,
            ReactionComponentRole.BASE,
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Base component supports deprotonation or nucleophile-activation rule context.",
            0.014);
    score += OrganicReactionConditionMatcher.reactantLoadingScore(conditions, evidence);
    score += OrganicReactionConditionMatcher.solventComponentScore(conditions, evidence);
    score += OrganicReactionConditionMatcher.thermalResidenceScore(conditions, evidence);
    return Math.max(-0.04, Math.min(0.08, score));
  }

  private static double roleContextScore(
      final ReactionConditions conditions,
      final List<ReactionPredictionEvidence> evidence,
      final ReactionComponentRole role,
      final ReactionPredictionEvidenceKind kind,
      final String description,
      final double score) {
    if (conditions.componentsByRole(role).isEmpty()) {
      return 0.0;
    }
    evidence.add(ReactionPredictionEvidence.of(kind, description, score));
    return score;
  }

  private static double reactantLoadingScore(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    final List<ReactionConditionComponent> reactants =
        conditions.componentsByRole(ReactionComponentRole.REACTANT);
    boolean quantified = false;
    boolean massQuantified = false;
    for (int i = 0; i < reactants.size(); ++i) {
      final ReactionConditionComponent component = reactants.get(i);
      quantified |= component.hasAmount() || component.hasEquivalent() || component.hasPurity();
      massQuantified |= component.hasAmount() && component.amount().massQuantity();
    }
    if (!quantified) {
      return 0.0;
    }
    final double score = massQuantified ? 0.020 : 0.012;
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            massQuantified
                ? "Mass-quantified reactant loading supports scale-aware rule generation."
                : "Quantified reactant loading supports rule generation.",
            score));
    return score;
  }

  private static double solventComponentScore(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    final List<ReactionConditionComponent> solvents =
        conditions.componentsByRole(ReactionComponentRole.SOLVENT);
    double score = 0.0;
    for (int i = 0; i < solvents.size(); ++i) {
      final String solvent = solvents.get(i).name().toLowerCase(Locale.ROOT);
      if (OrganicReactionConditionMatcher.polyetherOrHighBoiling(solvent)) {
        evidence.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
                "Polyether or high-boiling solvent supports thermally driven heterocycle and substitution rules.",
                0.018));
        score += 0.018;
      }
    }
    return Math.min(0.036, score);
  }

  private static boolean polyetherOrHighBoiling(final String solvent) {
    return solvent.contains("peg")
        || solvent.contains("polyethylene glycol")
        || solvent.contains("poly(ethylene glycol)")
        || solvent.contains("glyme")
        || solvent.contains("diglyme")
        || solvent.contains("triglyme")
        || solvent.contains("tetraglyme")
        || solvent.contains("dmf")
        || solvent.contains("dmso")
        || solvent.contains("nmp")
        || solvent.contains("xylene")
        || solvent.contains("dioxane");
  }

  private static double thermalResidenceScore(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    if (conditions.temperature() == null || !conditions.hasDuration()) {
      return 0.0;
    }
    final double kelvin = conditions.temperature().kelvin();
    final double hours = conditions.duration().hours();
    if (kelvin >= 383.15 && kelvin <= 433.15 && hours >= 1.0 && hours <= 8.0) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Multi-hour high-temperature residence window supports slow heteroaryl substitution and amination without using outcome lookup.",
              0.024));
      return 0.024;
    }
    if (kelvin >= 323.15 && hours >= 0.5 && hours <= 24.0) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Heated finite reaction time supports conversion-sensitive organic rule generation.",
              0.014));
      return 0.014;
    }
    if (kelvin > 443.15 || hours > 48.0) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Extreme thermal residence may increase decomposition or side-product pressure.",
              -0.018));
      return -0.018;
    }
    return 0.0;
  }

  private static double contradictionScore(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    if (conditions.hasFeature(ReactionConditionFeatureKind.COOLING)
        && conditions.hasFeature(ReactionConditionFeatureKind.HEATING)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Cooling and heating are both present; condition model is contradictory.",
              CONTRADICTION_PENALTY));
      score += CONTRADICTION_PENALTY;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        && conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Acidic and basic medium are both present; product ranking is chemically ambiguous.",
              CONTRADICTION_PENALTY));
      score += CONTRADICTION_PENALTY;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM)
        && conditions.hasFeature(ReactionConditionFeatureKind.REDUCING_MEDIUM)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Oxidizing and reducing medium are both present; redox-sensitive product ranking is ambiguous.",
              CONTRADICTION_PENALTY));
      score += CONTRADICTION_PENALTY;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.AIR_SENSITIVE)
        && !conditions.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE)) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Air-sensitive setup lacks inert atmosphere; generated products are risk-marked.",
              -0.10));
      score -= 0.10;
    }
    return score;
  }

  private static boolean hasExplicitOrInferredFeature(
      final ReactionConditions conditions, final ReactionConditionFeatureKind feature) {
    if (conditions.hasFeature(feature)) {
      return true;
    }
    final String text = OrganicReactionConditionMatcher.conditionSearchText(conditions);
    if (feature == ReactionConditionFeatureKind.HEATING) {
      return OrganicReactionConditionMatcher.temperatureAtLeast(conditions, 323.15)
          || text.contains("heat")
          || text.contains("heated")
          || text.contains("120")
          || text.contains("oil bath");
    }
    if (feature == ReactionConditionFeatureKind.COOLING) {
      return OrganicReactionConditionMatcher.temperatureAtMost(conditions, 283.15)
          || text.contains("cool")
          || text.contains("ice bath")
          || text.contains("0 c");
    }
    if (feature == ReactionConditionFeatureKind.REFLUX) {
      return text.contains("reflux");
    }
    if (feature == ReactionConditionFeatureKind.CATALYST_PRESENT) {
      return !conditions.componentsByRole(ReactionComponentRole.CATALYST).isEmpty()
          || !conditions.componentsByRole(ReactionComponentRole.LIGAND).isEmpty()
          || OrganicReactionConditionMatcher.textContains(
              text,
              "palladium",
              "pd",
              "nickel",
              "ni",
              "copper",
              "cu",
              "rhodium",
              "rh",
              "iridium",
              "ir",
              "catalyst");
    }
    if (feature == ReactionConditionFeatureKind.ACIDIC_MEDIUM) {
      return !conditions.componentsByRole(ReactionComponentRole.ACID).isEmpty()
          || OrganicReactionConditionMatcher.textContains(
              text, "acid", "hcl", "h2so4", "tfa", "acoh");
    }
    if (feature == ReactionConditionFeatureKind.BASIC_MEDIUM) {
      return !conditions.componentsByRole(ReactionComponentRole.BASE).isEmpty()
          || OrganicReactionConditionMatcher.textContains(
              text,
              "base",
              "koh",
              "naoh",
              "k2co3",
              "na2co3",
              "triethylamine",
              "diisopropylethylamine",
              "dipea",
              "dbu");
    }
    if (feature == ReactionConditionFeatureKind.OXIDIZING_MEDIUM) {
      return OrganicReactionConditionMatcher.textContains(
          text, "oxid", "mcpba", "peroxide", "h2o2", "dmdo", "pcc", "dess-martin", "kmno4");
    }
    if (feature == ReactionConditionFeatureKind.REDUCING_MEDIUM) {
      return OrganicReactionConditionMatcher.textContains(
          text, "reduc", "nabh4", "lialh4", "hydride", "borane", "hantzsch ester", "zn", "sncl2");
    }
    if (feature == ReactionConditionFeatureKind.HYDROGENATION) {
      return OrganicReactionConditionMatcher.textContains(
          text, "hydrogen", "h2", "pd/c", "raney", "hydrogenation");
    }
    if (feature == ReactionConditionFeatureKind.PHOTOCHEMICAL) {
      return OrganicReactionConditionMatcher.textContains(
          text, "photo", "light", "uv", "led", "irradiation");
    }
    if (feature == ReactionConditionFeatureKind.ELECTROCHEMICAL) {
      return OrganicReactionConditionMatcher.textContains(
          text, "electro", "anode", "cathode", "current");
    }
    if (feature == ReactionConditionFeatureKind.MICROWAVE) {
      return text.contains("microwave");
    }
    if (feature == ReactionConditionFeatureKind.SEALED_VESSEL) {
      return OrganicReactionConditionMatcher.textContains(
          text, "sealed", "autoclave", "pressure tube");
    }
    if (feature == ReactionConditionFeatureKind.FLOW_REACTOR) {
      return text.contains("flow");
    }
    if (feature == ReactionConditionFeatureKind.ANHYDROUS) {
      return OrganicReactionConditionMatcher.textContains(text, "anhydrous", "dry ", "dry solvent");
    }
    if (feature == ReactionConditionFeatureKind.INERT_ATMOSPHERE) {
      return OrganicReactionConditionMatcher.textContains(
          text, "nitrogen", "argon", "inert", "schlenk");
    }
    if (feature == ReactionConditionFeatureKind.DEHYDRATING_AGENT) {
      return OrganicReactionConditionMatcher.textContains(
          text, "dehydrat", "molecular sieve", "ms 4a", "p2o5", "socl2", "dcci", "dcc");
    }
    if (feature == ReactionConditionFeatureKind.WORKUP) {
      return !conditions.componentsByRole(ReactionComponentRole.WORKUP).isEmpty()
          || OrganicReactionConditionMatcher.textContains(text, "workup", "quench", "aqueous");
    }
    if (feature == ReactionConditionFeatureKind.ADDITION_RATE) {
      return OrganicReactionConditionMatcher.textContains(
          text, "dropwise", "slow addition", "syringe pump");
    }
    return false;
  }

  private static boolean temperatureAtLeast(
      final ReactionConditions conditions, final double kelvin) {
    return conditions.temperature() != null && conditions.temperature().kelvin() >= kelvin;
  }

  private static boolean temperatureAtMost(
      final ReactionConditions conditions, final double kelvin) {
    return conditions.temperature() != null && conditions.temperature().kelvin() <= kelvin;
  }

  private static boolean textContains(final String text, final String... fragments) {
    for (int i = 0; i < fragments.length; ++i) {
      if (text.contains(fragments[i])) {
        return true;
      }
    }
    return false;
  }

  private static String conditionSearchText(final ReactionConditions conditions) {
    final StringBuilder text = new StringBuilder();
    if (conditions.note() != null) {
      text.append(conditions.note()).append(' ');
    }
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      text.append(component.name()).append(' ');
      text.append(component.role().name()).append(' ');
      if (component.hasNote()) {
        text.append(component.note()).append(' ');
      }
    }
    return text.toString().toLowerCase(Locale.ROOT);
  }

  private static ReactionPredictionEvidenceKind kindForFeature(
      final ReactionConditionFeatureKind feature) {
    if (feature == ReactionConditionFeatureKind.CATALYST_PRESENT) {
      return ReactionPredictionEvidenceKind.CATALYST_EFFECT;
    }
    if (feature == ReactionConditionFeatureKind.HEATING
        || feature == ReactionConditionFeatureKind.COOLING
        || feature == ReactionConditionFeatureKind.REFLUX) {
      return ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT;
    }
    if (feature == ReactionConditionFeatureKind.MICROWAVE
        || feature == ReactionConditionFeatureKind.FLOW_REACTOR
        || feature == ReactionConditionFeatureKind.ADDITION_RATE
        || feature == ReactionConditionFeatureKind.HYDROGENATION) {
      return ReactionPredictionEvidenceKind.KINETIC_FACTOR;
    }
    if (feature == ReactionConditionFeatureKind.ANHYDROUS
        || feature == ReactionConditionFeatureKind.INERT_ATMOSPHERE) {
      return ReactionPredictionEvidenceKind.SOLVENT_EFFECT;
    }
    if (feature == ReactionConditionFeatureKind.OXIDIZING_MEDIUM
        || feature == ReactionConditionFeatureKind.REDUCING_MEDIUM
        || feature == ReactionConditionFeatureKind.PHOTOCHEMICAL
        || feature == ReactionConditionFeatureKind.ELECTROCHEMICAL) {
      return ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR;
    }
    return ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR;
  }

  private static List<ReactionConditionFeatureKind> checkedFeatures(
      final List<ReactionConditionFeatureKind> features) {
    if (features == null || features.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < features.size(); ++i) {
      if (features.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Organic rule condition feature must not be null.");
    }
    return List.copyOf(features);
  }
}
