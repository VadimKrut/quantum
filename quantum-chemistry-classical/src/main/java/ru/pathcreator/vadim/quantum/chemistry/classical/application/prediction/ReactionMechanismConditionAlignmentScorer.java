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
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeature;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;

public final class ReactionMechanismConditionAlignmentScorer {

  private static final double HEATING_KELVIN = 323.15;
  private static final double STRONG_HEATING_KELVIN = 383.15;

  public double score(
      final ReactionConditions conditions,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction product candidate must not be null.");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction prediction evidence list must not be null.");
    }
    if (conditions == null || conditions.empty() || !candidate.hasPredictedMechanisms()) {
      return 0.0;
    }
    final ConditionMechanismProfile profile = ConditionMechanismProfile.of(conditions);
    double score = 0.0;
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    for (int i = 0; i < mechanisms.size(); ++i) {
      final ReactionMechanismFamily family = mechanisms.get(i).family();
      final double contribution = profile.contribution(family);
      if (contribution == 0.0) {
        continue;
      }
      evidence.add(
          ReactionPredictionEvidence.of(
              contribution > 0.0
                  ? ReactionPredictionEvidenceKind.KINETIC_FACTOR
                  : ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
              profile.description(family, contribution),
              contribution));
      score += contribution;
    }
    return Math.max(-0.22, Math.min(0.32, score));
  }

  private static final class ConditionMechanismProfile {

    private final boolean polarAprotic;
    private final boolean proticOrAcidic;
    private final boolean basic;
    private final boolean highBoilingOrViscous;
    private final boolean heated;
    private final boolean stronglyHeated;
    private final boolean catalystMetalLigand;
    private final boolean radicalActivation;
    private final boolean oxidizing;
    private final boolean reducing;
    private final boolean dryOrInert;
    private final boolean concentratedOrScaleControlled;

    private ConditionMechanismProfile(
        final boolean polarAprotic,
        final boolean proticOrAcidic,
        final boolean basic,
        final boolean highBoilingOrViscous,
        final boolean heated,
        final boolean stronglyHeated,
        final boolean catalystMetalLigand,
        final boolean radicalActivation,
        final boolean oxidizing,
        final boolean reducing,
        final boolean dryOrInert,
        final boolean concentratedOrScaleControlled) {
      this.polarAprotic = polarAprotic;
      this.proticOrAcidic = proticOrAcidic;
      this.basic = basic;
      this.highBoilingOrViscous = highBoilingOrViscous;
      this.heated = heated;
      this.stronglyHeated = stronglyHeated;
      this.catalystMetalLigand = catalystMetalLigand;
      this.radicalActivation = radicalActivation;
      this.oxidizing = oxidizing;
      this.reducing = reducing;
      this.dryOrInert = dryOrInert;
      this.concentratedOrScaleControlled = concentratedOrScaleControlled;
    }

    static ConditionMechanismProfile of(final ReactionConditions conditions) {
      boolean polarAprotic = false;
      boolean proticOrAcidic = conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM);
      boolean basic = conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM);
      boolean highBoilingOrViscous = false;
      boolean catalystMetalLigand =
          conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT);
      boolean radicalActivation =
          conditions.hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL)
              || conditions.hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL);
      boolean oxidizing = conditions.hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM);
      boolean reducing =
          conditions.hasFeature(ReactionConditionFeatureKind.REDUCING_MEDIUM)
              || conditions.hasFeature(ReactionConditionFeatureKind.HYDROGENATION);
      boolean dryOrInert =
          conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
              || conditions.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE);
      boolean concentratedOrScaleControlled = false;
      final StringBuilder text = new StringBuilder();
      ReactionMechanismConditionAlignmentScorer.appendConditions(text, conditions);
      final String normalized = text.toString().toLowerCase(Locale.ROOT);
      polarAprotic |=
          ConditionMechanismProfile.containsAny(
              normalized,
              "dmf",
              "dmso",
              "nmp",
              "dma",
              "acetonitrile",
              "aprotic",
              "peg",
              "polyether",
              "polyol");
      proticOrAcidic |=
          ConditionMechanismProfile.containsAny(
              normalized,
              "methanol",
              "ethanol",
              "water",
              "aqueous",
              "acetic acid",
              "trifluoroacetic",
              "hydrochloric",
              "sulfuric",
              "protic");
      highBoilingOrViscous |=
          ConditionMechanismProfile.containsAny(
              normalized,
              "peg",
              "polyether",
              "polyol",
              "viscous",
              "high-boiling",
              "high boiling",
              "reflux",
              "sealed");
      catalystMetalLigand |=
          ConditionMechanismProfile.containsAny(
              normalized,
              "palladium",
              "nickel",
              "copper",
              "iron",
              "ruthenium",
              "rhodium",
              "iridium",
              "ligand",
              "phosphine",
              "pd",
              "ni",
              "cu");
      radicalActivation |=
          ConditionMechanismProfile.containsAny(
              normalized,
              "radical",
              "initiator",
              "peroxide",
              "aibn",
              "light",
              "photo",
              "photoredox",
              "electro");
      oxidizing |=
          ConditionMechanismProfile.containsAny(
              normalized, "oxidant", "oxygen", "peroxide", "mcpba", "nbs", "ncs", "ddq");
      reducing |=
          ConditionMechanismProfile.containsAny(
              normalized,
              "reductant",
              "hydride",
              "borohydride",
              "aluminium hydride",
              "hydrogen",
              "h2");
      concentratedOrScaleControlled |=
          ConditionMechanismProfile.containsAny(
              normalized,
              "concentrated",
              "equivalent",
              "scale",
              "limiting",
              "loading",
              "addition rate");
      final Temperature temperature = conditions.temperature();
      final boolean featureHeated =
          conditions.hasFeature(ReactionConditionFeatureKind.HEATING)
              || conditions.hasFeature(ReactionConditionFeatureKind.REFLUX)
              || conditions.hasFeature(ReactionConditionFeatureKind.MICROWAVE);
      final boolean heated =
          featureHeated || temperature != null && temperature.kelvin() >= HEATING_KELVIN;
      final boolean stronglyHeated =
          conditions.hasFeature(ReactionConditionFeatureKind.REFLUX)
              || conditions.hasFeature(ReactionConditionFeatureKind.MICROWAVE)
              || temperature != null && temperature.kelvin() >= STRONG_HEATING_KELVIN;
      return new ConditionMechanismProfile(
          polarAprotic,
          proticOrAcidic,
          basic,
          highBoilingOrViscous,
          heated,
          stronglyHeated,
          catalystMetalLigand,
          radicalActivation,
          oxidizing,
          reducing,
          dryOrInert,
          concentratedOrScaleControlled);
    }

    double contribution(final ReactionMechanismFamily family) {
      if (family == ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION) {
        return this.snarContribution();
      }
      if (family == ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE) {
        return this.anrorcContribution();
      }
      if (ConditionMechanismProfile.crossCoupling(family)) {
        return this.crossCouplingContribution();
      }
      if (ConditionMechanismProfile.radical(family)) {
        return this.radicalContribution();
      }
      if (ConditionMechanismProfile.oxidation(family)) {
        return this.oxidationContribution();
      }
      if (ConditionMechanismProfile.reduction(family)) {
        return this.reductionContribution();
      }
      if (ConditionMechanismProfile.cycloaddition(family)) {
        return this.cycloadditionContribution();
      }
      if (ConditionMechanismProfile.condensation(family)) {
        return this.condensationContribution();
      }
      return 0.0;
    }

    String description(final ReactionMechanismFamily family, final double contribution) {
      final String direction = contribution > 0.0 ? "supports" : "penalizes";
      return "Condition mechanism profile "
          + direction
          + ' '
          + family.name()
          + " using solvent/catalyst/temperature/redox/radical signals.";
    }

    private double snarContribution() {
      double score = 0.0;
      if (this.polarAprotic) {
        score += 0.030;
      }
      if (this.basic) {
        score += 0.024;
      }
      if (this.heated) {
        score += 0.020;
      }
      if (this.highBoilingOrViscous) {
        score += 0.012;
      }
      if (this.proticOrAcidic) {
        score -= 0.050;
      }
      if (this.radicalActivation) {
        score -= 0.018;
      }
      return score;
    }

    private double anrorcContribution() {
      double score = 0.0;
      if (this.basic) {
        score += 0.020;
      }
      if (this.stronglyHeated) {
        score += 0.028;
      } else if (this.heated) {
        score += 0.018;
      }
      if (this.highBoilingOrViscous) {
        score += 0.022;
      }
      if (this.concentratedOrScaleControlled) {
        score += 0.012;
      }
      if (this.proticOrAcidic) {
        score -= 0.034;
      }
      if (this.radicalActivation) {
        score -= 0.016;
      }
      return score;
    }

    private double crossCouplingContribution() {
      double score = 0.0;
      if (this.catalystMetalLigand) {
        score += 0.052;
      } else {
        score -= 0.038;
      }
      if (this.basic) {
        score += 0.018;
      }
      if (this.dryOrInert) {
        score += 0.020;
      }
      if (this.proticOrAcidic) {
        score -= 0.022;
      }
      return score;
    }

    private double radicalContribution() {
      double score = 0.0;
      if (this.radicalActivation) {
        score += 0.060;
      } else {
        score -= 0.030;
      }
      if (this.oxidizing) {
        score += 0.016;
      }
      if (this.dryOrInert) {
        score += 0.010;
      }
      if (this.proticOrAcidic) {
        score -= 0.012;
      }
      return score;
    }

    private double oxidationContribution() {
      if (this.oxidizing && !this.reducing) {
        return 0.060;
      }
      if (this.reducing) {
        return -0.052;
      }
      return 0.0;
    }

    private double reductionContribution() {
      if (this.reducing && !this.oxidizing) {
        return 0.060;
      }
      if (this.oxidizing) {
        return -0.052;
      }
      return 0.0;
    }

    private double cycloadditionContribution() {
      double score = 0.0;
      if (this.heated || this.radicalActivation) {
        score += 0.026;
      }
      if (this.concentratedOrScaleControlled) {
        score += 0.014;
      }
      if (this.proticOrAcidic) {
        score -= 0.010;
      }
      return score;
    }

    private double condensationContribution() {
      double score = 0.0;
      if (this.proticOrAcidic || this.basic) {
        score += 0.022;
      }
      if (this.heated) {
        score += 0.016;
      }
      if (this.highBoilingOrViscous) {
        score -= 0.012;
      }
      return score;
    }

    private static boolean crossCoupling(final ReactionMechanismFamily family) {
      return family == ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING
          || family == ReactionMechanismFamily.SUZUKI_COUPLING
          || family == ReactionMechanismFamily.HECK_COUPLING
          || family == ReactionMechanismFamily.SONOGASHIRA_COUPLING
          || family == ReactionMechanismFamily.NEGISHI_COUPLING
          || family == ReactionMechanismFamily.STILLE_COUPLING
          || family == ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION
          || family == ReactionMechanismFamily.ORGANOMETALLIC_INSERTION
          || family == ReactionMechanismFamily.C_H_ACTIVATION;
    }

    private static boolean radical(final ReactionMechanismFamily family) {
      return family == ReactionMechanismFamily.RADICAL_SUBSTITUTION
          || family == ReactionMechanismFamily.RADICAL_CHAIN
          || family == ReactionMechanismFamily.RADICAL_CYCLIZATION
          || family == ReactionMechanismFamily.PHOTOCHEMICAL;
    }

    private static boolean oxidation(final ReactionMechanismFamily family) {
      return family == ReactionMechanismFamily.OXIDATION
          || family == ReactionMechanismFamily.SWERN_OXIDATION
          || family == ReactionMechanismFamily.DESS_MARTIN_OXIDATION
          || family == ReactionMechanismFamily.JONES_OXIDATION
          || family == ReactionMechanismFamily.PCC_OXIDATION
          || family == ReactionMechanismFamily.OZONOLYSIS
          || family == ReactionMechanismFamily.BAEYER_VILLIGER
          || family == ReactionMechanismFamily.EPOXIDATION
          || family == ReactionMechanismFamily.DIHYDROXYLATION;
    }

    private static boolean reduction(final ReactionMechanismFamily family) {
      return family == ReactionMechanismFamily.REDUCTION
          || family == ReactionMechanismFamily.HYDROGENATION
          || family == ReactionMechanismFamily.HYDRIDE_REDUCTION
          || family == ReactionMechanismFamily.BIRCH_REDUCTION
          || family == ReactionMechanismFamily.WOLFF_KISHNER_REDUCTION
          || family == ReactionMechanismFamily.CLEMMENSEN_REDUCTION;
    }

    private static boolean cycloaddition(final ReactionMechanismFamily family) {
      return family == ReactionMechanismFamily.PERICYCLIC
          || family == ReactionMechanismFamily.CYCLOADDITION
          || family == ReactionMechanismFamily.CLICK_CYCLOADDITION
          || family == ReactionMechanismFamily.DIELS_ALDER
          || family == ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION
          || family == ReactionMechanismFamily.THIOL_ENE_CLICK;
    }

    private static boolean condensation(final ReactionMechanismFamily family) {
      return family == ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION
          || family == ReactionMechanismFamily.UGI_REACTION
          || family == ReactionMechanismFamily.PASSERINI_REACTION
          || family == ReactionMechanismFamily.BIGINELLI_REACTION
          || family == ReactionMechanismFamily.HANTZSCH_SYNTHESIS
          || family == ReactionMechanismFamily.KNOEVENAGEL_CONDENSATION
          || family == ReactionMechanismFamily.HENRY_REACTION
          || family == ReactionMechanismFamily.CLAISEN_CONDENSATION
          || family == ReactionMechanismFamily.DIECKMANN_CONDENSATION;
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

  private static void appendConditions(
      final StringBuilder builder, final ReactionConditions conditions) {
    final List<ReactionConditionFeature> features = conditions.features();
    for (int i = 0; i < features.size(); ++i) {
      builder.append(' ');
      builder.append(features.get(i).kind().name().replace('_', ' '));
      if (features.get(i).note() != null) {
        builder.append(' ');
        builder.append(features.get(i).note());
      }
    }
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      builder.append(' ');
      builder.append(component.role().name().replace('_', ' '));
      builder.append(' ');
      builder.append(component.name());
      if (component.note() != null) {
        builder.append(' ');
        builder.append(component.note());
      }
      if (component.role() == ReactionComponentRole.CATALYST
          || component.role() == ReactionComponentRole.LIGAND) {
        builder.append(" catalyst ligand");
      }
    }
    if (conditions.note() != null) {
      builder.append(' ');
      builder.append(conditions.note());
    }
  }
}
