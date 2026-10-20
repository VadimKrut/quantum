/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffect;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;

/**
 * Проверяет пригодность запроса к прогнозированию продуктов до генерации и ранжирования кандидатов.
 */
public final class ReactionPredictionRequestAuditor {

  private static final double COMPLETE_PROBABILITY_EPSILON = 1.0E-9;

  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final OrganicReactionDomainConditionAdvisor domainConditionAdvisor;
  private final ReactionConditionEffectAnalyzer conditionEffectAnalyzer;

  public ReactionPredictionRequestAuditor() {
    this(new MolecularFeatureAnalyzer());
  }

  public ReactionPredictionRequestAuditor(final MolecularFeatureAnalyzer featureAnalyzer) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException(
          "Reaction prediction request feature analyzer must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
    this.domainConditionAdvisor = new OrganicReactionDomainConditionAdvisor(featureAnalyzer);
    this.conditionEffectAnalyzer = new ReactionConditionEffectAnalyzer();
  }

  public List<ReactionPredictionEvidence> audit(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    final ArrayList<ReactionPredictionEvidence> diagnostics =
        new ArrayList<ReactionPredictionEvidence>(12);
    this.auditDomains(request, diagnostics);
    this.auditReactants(request, diagnostics);
    this.auditConditions(request.conditions(), diagnostics);
    this.auditConditionEffects(request, diagnostics);
    this.auditProductObservationContext(request, diagnostics);
    diagnostics.addAll(this.domainConditionAdvisor.advise(request));
    this.auditSeedCandidates(request, diagnostics);
    return List.copyOf(diagnostics);
  }

  private void auditDomains(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> diagnostics) {
    if (request.domains().size() == 1 && request.hasDomain(OrganicReactionDomain.UNKNOWN)) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Reaction prediction domain is UNKNOWN; ranking will use only generic product evidence.",
              -0.2));
      return;
    }
    if (ReactionPredictionRequestAuditor.requestsHeterocycleDomain(request)
        && !this.reactantsContainHeterocycle(request)) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Heterocycle reaction domain was requested, but reactants contain no detected heterocycle.",
              -0.3));
    }
    if (request.hasDomain(OrganicReactionDomain.CROSS_COUPLING)
        && request.conditions().componentsByRole(ReactionComponentRole.CATALYST).isEmpty()
        && !request.conditions().hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.CATALYST_EFFECT,
              "Cross-coupling prediction has no catalyst component or catalyst-present feature.",
              -0.15));
    }
  }

  private void auditReactants(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> diagnostics) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    long heteroAtomFeatures = 0L;
    long chargedOrRadicalFeatures = 0L;
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      heteroAtomFeatures += features.countOf(MolecularFeatureKind.HETERO_ATOM_COUNT);
      chargedOrRadicalFeatures += features.countOf(MolecularFeatureKind.CHARGED_CENTER);
      chargedOrRadicalFeatures += features.countOf(MolecularFeatureKind.RADICAL_CENTER);
    }
    if (heteroAtomFeatures > 0L) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Reactants contain hetero atoms; organic prediction can use heteroatom-sensitive rules.",
              Math.min(0.1, heteroAtomFeatures * 0.01)));
    }
    if (chargedOrRadicalFeatures > 0L) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Reactants contain charged or radical centers; prediction should be interpreted with electronic-state risk.",
              -0.05));
    }
  }

  private void auditConditions(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> diagnostics) {
    if (conditions == null || conditions.empty()) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Reaction conditions are empty; temperature, time, solvent and catalyst effects cannot constrain prediction.",
              -0.25));
      return;
    }
    if (conditions.temperature() == null) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT,
              "Reaction temperature is not specified; thermal pathway ranking is approximate.",
              -0.05));
    }
    if (!conditions.hasDuration()) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Reaction duration is not specified; kinetic pathway ranking is approximate.",
              -0.05));
    }
    if (conditions.componentsByRole(ReactionComponentRole.SOLVENT).isEmpty()) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Solvent component is not specified; solvent-sensitive regioselectivity is approximate.",
              -0.05));
    }
    ReactionPredictionRequestAuditor.auditConditionContradictions(conditions, diagnostics);
  }

  private void auditConditionEffects(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> diagnostics) {
    final ReactionConditionEffectProfile profile = this.conditionEffectAnalyzer.analyze(request);
    if (profile.empty()) {
      return;
    }
    final List<ReactionConditionEffect> effects = profile.effects();
    int riskCount = 0;
    int supportCount = 0;
    for (int i = 0; i < effects.size(); ++i) {
      if (effects.get(i).risk()) {
        ++riskCount;
      } else {
        ++supportCount;
      }
    }
    diagnostics.add(
        ReactionPredictionEvidence.of(
            riskCount == 0
                ? ReactionPredictionEvidenceKind.KINETIC_FACTOR
                : ReactionPredictionEvidenceKind.WARNING,
            "Condition-effect profile contains "
                + supportCount
                + " support effect(s), "
                + riskCount
                + " risk effect(s), score adjustment "
                + profile.scoreAdjustment()
                + ".",
            Math.max(-0.20, Math.min(0.20, profile.scoreAdjustment()))));
  }

  private static void auditProductObservationContext(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> diagnostics) {
    if (request.conditions().componentsByRole(ReactionComponentRole.PRODUCT).isEmpty()) {
      return;
    }
    if (request.seedCandidates().isEmpty()) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Product-role condition components are stored as observed recipe context only; they are not used as prediction seed candidates without explicit product molecule candidates.",
              -0.04));
      return;
    }
    diagnostics.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Product-role condition components coexist with explicit seed candidates; stored observations and predictive candidates remain separated.",
            0.0));
  }

  private static void auditConditionContradictions(
      final ReactionConditions conditions, final List<ReactionPredictionEvidence> diagnostics) {
    if (conditions.hasFeature(ReactionConditionFeatureKind.COOLING)
        && conditions.hasFeature(ReactionConditionFeatureKind.HEATING)) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Reaction conditions contain both COOLING and HEATING features.",
              -0.2));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        && conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Reaction conditions contain both ACIDIC_MEDIUM and BASIC_MEDIUM features.",
              -0.2));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.AIR_SENSITIVE)
        && !conditions.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE)) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "AIR_SENSITIVE condition is present without INERT_ATMOSPHERE.",
              -0.1));
    }
  }

  private static void auditSeedCandidates(
      final ReactionPredictionRequest request, final List<ReactionPredictionEvidence> diagnostics) {
    final List<ReactionProductCandidate> candidates = request.seedCandidates();
    if (candidates.isEmpty()) {
      return;
    }
    double probabilityMass = 0.0;
    double yieldMass = 0.0;
    final var supplied = ReactionAtomInventory.of(request.reactants());
    for (int i = 0; i < candidates.size(); ++i) {
      probabilityMass += candidates.get(i).probability();
      yieldMass += candidates.get(i).expectedYieldPercent();
      final List<String> missing =
          ReactionAtomInventory.excess(supplied, candidates.get(i).products());
      if (!missing.isEmpty()) {
        diagnostics.add(
            ReactionPredictionEvidence.of(
                ReactionPredictionEvidenceKind.WARNING,
                "User-seeded product "
                    + (i + 1)
                    + " exceeds the explicit reactant atom/isotope inventory: "
                    + String.join(", ", missing)
                    + ". The drawing is retained as an unverified user hypothesis. "
                    + "Its normalized ranking weight does not establish feasibility; provide missing reagent graphs "
                    + "or correct participant coefficients. Condition labels do not supply atoms.",
                -0.25));
      }
    }
    if (probabilityMass > 1.0 + COMPLETE_PROBABILITY_EPSILON) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Seed product candidate probability mass exceeds 1.0 before normalization.",
              -0.2));
    } else if (probabilityMass < 1.0 - COMPLETE_PROBABILITY_EPSILON) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
              "Seed product candidates leave unassigned probability mass for unknown side products.",
              -0.05));
    }
    if (yieldMass <= 0.0) {
      diagnostics.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.WARNING,
              "Seed product candidates do not contain expected yield estimates.",
              -0.05));
    }
  }

  private boolean reactantsContainHeterocycle(final ReactionPredictionRequest request) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      if (features.contains(MolecularFeatureKind.HETEROCYCLE)
          || features.contains(MolecularFeatureKind.NITROGEN_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.OXYGEN_HETEROCYCLE)
          || features.contains(MolecularFeatureKind.SULFUR_HETEROCYCLE)) {
        return true;
      }
    }
    return false;
  }

  private static boolean requestsHeterocycleDomain(final ReactionPredictionRequest request) {
    return request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        || request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION);
  }
}
