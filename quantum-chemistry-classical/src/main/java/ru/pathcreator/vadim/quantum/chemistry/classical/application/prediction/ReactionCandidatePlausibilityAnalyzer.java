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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.PeriodicTable;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSideSummary;

/**
 * Оценивает химическую правдоподобность кандидата относительно исходных реагентов и условий.
 */
public final class ReactionCandidatePlausibilityAnalyzer {

  private static final double EXACT_FORMULA_SUPPORT = 0.055;
  private static final double SMALL_IMBALANCE_PENALTY = -0.035;
  private static final double LARGE_IMBALANCE_PENALTY = -0.14;
  private static final double CHARGE_MISMATCH_PENALTY = -0.08;
  private static final double NO_STRUCTURAL_CHANGE_PENALTY = -0.06;
  private static final double HETEROCYCLE_FORMATION_SUPPORT = 0.09;
  private static final double HETEROCYCLE_RETENTION_SUPPORT = 0.045;
  private static final double LEAVING_GROUP_LOSS_SUPPORT = 0.07;
  private static final double CROSS_COUPLING_BORYLATION_SUPPORT = 0.35;
  private static final double OXIDATION_SUPPORT = 0.065;
  private static final double REDUCTION_SUPPORT = 0.055;
  private static final double ACID_BASE_SUPPORT = 0.04;
  private static final double MAX_FORMULA_DELTA_SMALL = 2.0;

  private final MolecularFeatureAnalyzer featureAnalyzer;

  public ReactionCandidatePlausibilityAnalyzer() {
    this(new MolecularFeatureAnalyzer());
  }

  public ReactionCandidatePlausibilityAnalyzer(final MolecularFeatureAnalyzer featureAnalyzer) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException("Molecular feature analyzer must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
  }

  public double analyze(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (candidate == null) {
      throw new IllegalArgumentException("Reaction product candidate must not be null.");
    }
    if (evidence == null) {
      throw new IllegalArgumentException("Reaction prediction evidence list must not be null.");
    }
    double score = 0.0;
    score += this.formulaAndChargeScore(request, candidate, evidence);
    score += this.noChangeScore(request.reactants(), candidate.products(), evidence);
    score += this.domainFeatureScore(request, candidate, evidence);
    score += this.elementTransferScore(request, candidate, evidence);
    return score;
  }

  private double formulaAndChargeScore(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    final ReactionSideSummary reactants = ReactionSideSummary.of(request.reactants());
    final ReactionSideSummary products = ReactionSideSummary.of(candidate.products());
    final long formulaDelta =
        ReactionCandidatePlausibilityAnalyzer.absoluteFormulaDelta(reactants, products);
    final long chargeDelta = Math.abs(products.charge() - reactants.charge());
    double score = 0.0;
    if (formulaDelta == 0L && chargeDelta == 0L) {
      final String balanceDescription =
          candidate.products().participants().size() > 1
              ? "Candidate conserves molecular formula and net charge through an explicit multi-product side."
              : "Candidate conserves molecular formula and net charge against the declared reactant side.";
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
              balanceDescription,
              EXACT_FORMULA_SUPPORT));
      score += EXACT_FORMULA_SUPPORT;
    } else {
      score += this.imbalanceScore(request, formulaDelta, chargeDelta, evidence);
    }
    if (chargeDelta > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Candidate changes declared net charge without explicit counter-ion/product balancing.",
              CHARGE_MISMATCH_PENALTY));
      score += CHARGE_MISMATCH_PENALTY;
    }
    return score;
  }

  private double imbalanceScore(
      final ReactionPredictionRequest request,
      final long formulaDelta,
      final long chargeDelta,
      final List<ReactionPredictionEvidence> evidence) {
    if (formulaDelta == 0L && chargeDelta == 0L) {
      return 0.0;
    }
    if (formulaDelta <= MAX_FORMULA_DELTA_SMALL
        && ReactionCandidatePlausibilityAnalyzer.hasMassTransferContext(request.conditions())) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
              "Small formula imbalance is compatible with declared reagent/additive or leaving-group context.",
              SMALL_IMBALANCE_PENALTY));
      return SMALL_IMBALANCE_PENALTY;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.WARNING,
            "Candidate formula is not balanced against declared reactants; missing reagent, byproduct or counter-ion is likely.",
            LARGE_IMBALANCE_PENALTY));
    return LARGE_IMBALANCE_PENALTY;
  }

  private double noChangeScore(
      final ReactionSide reactants,
      final ReactionSide products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!reactants.sameParticipantsAs(products)) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.WARNING,
            "Candidate product side repeats the same molecular participants as reactants; predicted conversion is weak.",
            NO_STRUCTURAL_CHANGE_PENALTY));
    return NO_STRUCTURAL_CHANGE_PENALTY;
  }

  private double domainFeatureScore(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    double score = 0.0;
    final FeatureTotals reactantFeatures = this.featureTotals(request.reactants());
    final FeatureTotals productFeatures = this.featureTotals(candidate.products());
    score += this.heterocycleScore(request, reactantFeatures, productFeatures, evidence);
    score += this.leavingGroupScore(request, reactantFeatures, productFeatures, evidence);
    score += this.redoxScore(request, reactantFeatures, productFeatures, evidence);
    score += this.acidBaseScore(request, reactantFeatures, productFeatures, evidence);
    return score;
  }

  private double elementTransferScore(
      final ReactionPredictionRequest request,
      final ReactionProductCandidate candidate,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.CROSS_COUPLING)) {
      return 0.0;
    }
    final ReactionSideSummary reactants = ReactionSideSummary.of(request.reactants());
    final ReactionSideSummary products = ReactionSideSummary.of(candidate.products());
    final long reactantBoron =
        reactants.elementCount(PeriodicTable.requireAtomicNumber(5).symbol());
    final long productBoron = products.elementCount(PeriodicTable.requireAtomicNumber(5).symbol());
    if (productBoron <= reactantBoron) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.CATALYST_EFFECT,
            "Candidate introduces a boron handle in a cross-coupling/borylation context.",
            CROSS_COUPLING_BORYLATION_SUPPORT));
    return CROSS_COUPLING_BORYLATION_SUPPORT;
  }

  private double heterocycleScore(
      final ReactionPredictionRequest request,
      final FeatureTotals reactants,
      final FeatureTotals products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      return 0.0;
    }
    if (products.heterocycleCount > reactants.heterocycleCount) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Candidate increases heterocycle count for a heterocycle reaction domain.",
              HETEROCYCLE_FORMATION_SUPPORT));
      return HETEROCYCLE_FORMATION_SUPPORT;
    }
    if (products.heterocycleCount > 0L && reactants.heterocycleCount > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Candidate preserves heterocycle scaffold under requested heterocycle transformation.",
              HETEROCYCLE_RETENTION_SUPPORT));
      return HETEROCYCLE_RETENTION_SUPPORT;
    }
    return 0.0;
  }

  private double leavingGroupScore(
      final ReactionPredictionRequest request,
      final FeatureTotals reactants,
      final FeatureTotals products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION)
        && !request.hasDomain(OrganicReactionDomain.CROSS_COUPLING)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      return 0.0;
    }
    if (reactants.halogenCount <= products.halogenCount) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            "Candidate consumes a halogenated leaving-group handle in a substitution or coupling context.",
            LEAVING_GROUP_LOSS_SUPPORT));
    return LEAVING_GROUP_LOSS_SUPPORT;
  }

  private double redoxScore(
      final ReactionPredictionRequest request,
      final FeatureTotals reactants,
      final FeatureTotals products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)) {
      return 0.0;
    }
    final long oxidationSignals =
        products.carbonylCount
            + products.sulfurOxideCount
            + products.nitroCount
            - reactants.carbonylCount
            - reactants.sulfurOxideCount
            - reactants.nitroCount;
    if (oxidationSignals > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Candidate increases oxidized functional-group signals.",
              OXIDATION_SUPPORT));
      return OXIDATION_SUPPORT;
    }
    final long reductionSignals =
        reactants.nitroCount
            + reactants.carbonylCount
            - products.nitroCount
            - products.carbonylCount;
    if (reductionSignals > 0L) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Candidate decreases oxidized functional-group signals, compatible with reduction context.",
              REDUCTION_SUPPORT));
      return REDUCTION_SUPPORT;
    }
    return 0.0;
  }

  private double acidBaseScore(
      final ReactionPredictionRequest request,
      final FeatureTotals reactants,
      final FeatureTotals products,
      final List<ReactionPredictionEvidence> evidence) {
    if (!request.hasDomain(OrganicReactionDomain.ACID_BASE)) {
      return 0.0;
    }
    if (reactants.chargeCenterCount == products.chargeCenterCount) {
      return 0.0;
    }
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Candidate changes charged-center count in an acid/base transformation context.",
            ACID_BASE_SUPPORT));
    return ACID_BASE_SUPPORT;
  }

  private FeatureTotals featureTotals(final ReactionSide side) {
    long heterocycleCount = 0L;
    long halogenCount = 0L;
    long carbonylCount = 0L;
    long sulfurOxideCount = 0L;
    long nitroCount = 0L;
    long chargeCenterCount = 0L;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final long coefficient = participants.get(i).coefficient().value();
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      heterocycleCount += coefficient * features.countOf(MolecularFeatureKind.HETEROCYCLE);
      halogenCount += coefficient * features.countOf(MolecularFeatureKind.HALOGENATED);
      carbonylCount += coefficient * features.countOf(MolecularFeatureKind.CARBONYL);
      sulfurOxideCount += coefficient * features.countOf(MolecularFeatureKind.SULFUR_OXIDE);
      nitroCount += coefficient * features.countOf(MolecularFeatureKind.NITRO);
      chargeCenterCount += coefficient * features.countOf(MolecularFeatureKind.CHARGED_CENTER);
    }
    return new FeatureTotals(
        heterocycleCount,
        halogenCount,
        carbonylCount,
        sulfurOxideCount,
        nitroCount,
        chargeCenterCount);
  }

  private static boolean hasMassTransferContext(final ReactionConditions conditions) {
    if (conditions == null || conditions.empty()) {
      return false;
    }
    return !conditions.componentsByRole(ReactionComponentRole.BASE).isEmpty()
        || !conditions.componentsByRole(ReactionComponentRole.ACID).isEmpty()
        || !conditions.componentsByRole(ReactionComponentRole.ADDITIVE).isEmpty()
        || conditions.hasFeature(ReactionConditionFeatureKind.DEHYDRATING_AGENT)
        || conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM);
  }

  private static long absoluteFormulaDelta(
      final ReactionSideSummary reactants, final ReactionSideSummary products) {
    long delta = 0L;
    for (int atomicNumber = PeriodicTable.MIN_ATOMIC_NUMBER;
        atomicNumber <= PeriodicTable.MAX_ATOMIC_NUMBER;
        ++atomicNumber) {
      if (!PeriodicTable.containsAtomicNumber(atomicNumber)) {
        continue;
      }
      final ElementSymbol symbol = PeriodicTable.requireAtomicNumber(atomicNumber).symbol();
      delta += Math.abs(products.elementCount(symbol) - reactants.elementCount(symbol));
    }
    return delta;
  }

  private static final class FeatureTotals {

    private final long heterocycleCount;
    private final long halogenCount;
    private final long carbonylCount;
    private final long sulfurOxideCount;
    private final long nitroCount;
    private final long chargeCenterCount;

    private FeatureTotals(
        final long heterocycleCount,
        final long halogenCount,
        final long carbonylCount,
        final long sulfurOxideCount,
        final long nitroCount,
        final long chargeCenterCount) {
      this.heterocycleCount = heterocycleCount;
      this.halogenCount = halogenCount;
      this.carbonylCount = carbonylCount;
      this.sulfurOxideCount = sulfurOxideCount;
      this.nitroCount = nitroCount;
      this.chargeCenterCount = chargeCenterCount;
    }
  }
}
