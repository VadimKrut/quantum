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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionSiteSelectivityAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionSiteSelectivityAssessment;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.LeavingGroupReplacementTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MolecularTransformProduct;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.LeavingGroupReplacementTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Правило, которое создает продукты заменой уходящих групп без мутации исходного реагента. */
public final class ReplacingLeavingGroupOrganicReactionRule implements OrganicReactionRule {

  private final OrganicReactionRuleId id;
  private final String displayName;
  private final OrganicReactionRuleCategory category;
  private final List<OrganicReactionDomain> domains;
  private final List<MolecularFeatureKind> requiredReactantFeatures;
  private final List<ReactionConditionFeatureKind> supportiveConditionFeatures;
  private final LeavingGroupReplacementTransform transform;
  private final double baseProbability;
  private final double expectedYieldPercent;
  private final String selectivityNote;
  private final String explanation;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final OrganicReactionConditionMatcher conditionMatcher;
  private final LeavingGroupReplacementTransformer transformer;
  private final ReactionSiteSelectivityAnalyzer siteSelectivityAnalyzer;

  private ReplacingLeavingGroupOrganicReactionRule(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredReactantFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final LeavingGroupReplacementTransform transform,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation) {
    this.id = id;
    this.displayName = displayName;
    this.category = category;
    this.domains = domains;
    this.requiredReactantFeatures = requiredReactantFeatures;
    this.supportiveConditionFeatures = supportiveConditionFeatures;
    this.transform = transform;
    this.baseProbability = baseProbability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.selectivityNote = selectivityNote;
    this.explanation = explanation;
    this.featureAnalyzer = new MolecularFeatureAnalyzer();
    this.conditionMatcher = new OrganicReactionConditionMatcher();
    this.transformer = new LeavingGroupReplacementTransformer();
    this.siteSelectivityAnalyzer = new ReactionSiteSelectivityAnalyzer();
  }

  public static ReplacingLeavingGroupOrganicReactionRule of(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredReactantFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final LeavingGroupReplacementTransform transform,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation) {
    if (id == null) {
      throw new IllegalArgumentException("Replacing organic rule id must not be null.");
    }
    if (category == null) {
      throw new IllegalArgumentException("Replacing organic rule category must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Replacing organic rule transform must not be null.");
    }
    if (!Double.isFinite(baseProbability) || baseProbability <= 0.0 || baseProbability > 1.0) {
      throw new IllegalArgumentException("Replacing organic rule probability must be in (0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Replacing organic rule yield percent must be in [0, 100].");
    }
    return new ReplacingLeavingGroupOrganicReactionRule(
        id,
        TextValue.requireText(displayName, "Replacing organic rule display name"),
        category,
        ReplacingLeavingGroupOrganicReactionRule.checkedDomains(domains),
        ReplacingLeavingGroupOrganicReactionRule.checkedFeatures(requiredReactantFeatures),
        ReplacingLeavingGroupOrganicReactionRule.checkedConditionFeatures(
            supportiveConditionFeatures),
        transform,
        baseProbability,
        expectedYieldPercent,
        TextValue.optionalText(selectivityNote),
        TextValue.requireText(explanation, "Replacing organic rule explanation"));
  }

  public OrganicReactionRuleId id() {
    return this.id;
  }

  public String displayName() {
    return this.displayName;
  }

  public OrganicReactionRuleCategory category() {
    return this.category;
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (!this.domainMatches(request)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    if (this.matchingReactants(request).isEmpty()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(request.conditions(), this.supportiveConditionFeatures);
    if (conditionAssessment.stronglyIncompatible()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        ReplacingLeavingGroupOrganicReactionRule.clampedConfidence(
            this.baseProbability + conditionAssessment.scoreAdjustment()),
        this.explanation);
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (match == null || !match.matched()) {
      return List.of();
    }
    final List<Molecule> reactants = this.matchingReactants(request);
    if (reactants.isEmpty()) {
      return List.of();
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(request.conditions(), this.supportiveConditionFeatures);
    final ArrayList<ReactionProductCandidate> candidates =
        new ArrayList<ReactionProductCandidate>();
    for (int reactantIndex = 0; reactantIndex < reactants.size(); ++reactantIndex) {
      final Molecule reactant = reactants.get(reactantIndex);
      final List<MolecularTransformProduct> products =
          this.transformer.transformAll(reactant, this.transform);
      for (int productIndex = 0; productIndex < products.size(); ++productIndex) {
        final MolecularTransformProduct product = products.get(productIndex);
        final ReactionSiteSelectivityAssessment siteAssessment =
            this.siteSelectivityAnalyzer.assess(
                request, reactant, product, this.baseProbability, this.expectedYieldPercent);
        candidates.add(
            ReactionProductCandidate.of(
                ReactionProductSideFactory.fromTransformProduct(product),
                siteAssessment.probability(),
                siteAssessment.expectedYieldPercent(),
                this.selectivityNote,
                this.evidence(match, conditionAssessment, siteAssessment, reactant, product)));
      }
    }
    return List.copyOf(candidates);
  }

  private List<ReactionPredictionEvidence> evidence(
      final OrganicReactionRuleMatch match,
      final OrganicReactionConditionAssessment conditionAssessment,
      final ReactionSiteSelectivityAssessment siteAssessment,
      final Molecule reactant,
      final MolecularTransformProduct product) {
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(
            3 + conditionAssessment.evidence().size() + siteAssessment.evidence().size());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Replacement rule matched: " + this.id.value(),
            match.confidence()));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            this.transform.explanation(),
            Math.min(0.25, match.confidence() * 0.25)));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
            "Generated "
                + product.product().id().value()
                + " from "
                + reactant.id().value()
                + " by leaving-group replacement at "
                + product.mappingDescription(),
            0.12));
    evidence.addAll(conditionAssessment.evidence());
    evidence.addAll(siteAssessment.evidence());
    return List.copyOf(evidence);
  }

  private List<Molecule> matchingReactants(final ReactionPredictionRequest request) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    final ArrayList<Molecule> reactants = new ArrayList<Molecule>();
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      if (this.requiredFeaturesMatch(molecule)) {
        reactants.add(molecule);
      }
    }
    return List.copyOf(reactants);
  }

  private boolean requiredFeaturesMatch(final Molecule molecule) {
    final MolecularFeatureSet features = this.featureAnalyzer.analyze(molecule);
    for (int i = 0; i < this.requiredReactantFeatures.size(); ++i) {
      if (features.contains(this.requiredReactantFeatures.get(i))) {
        continue;
      }
      return false;
    }
    return true;
  }

  private boolean domainMatches(final ReactionPredictionRequest request) {
    if (request.hasDomain(OrganicReactionDomain.UNKNOWN)) {
      return true;
    }
    final OrganicReactionDomain requiredDomain =
        ReplacingLeavingGroupOrganicReactionRule.requiredDomain(this.category);
    if (requiredDomain != OrganicReactionDomain.UNKNOWN
        && this.domains.contains(requiredDomain)
        && !request.hasDomain(requiredDomain)) {
      return false;
    }
    for (int i = 0; i < this.domains.size(); ++i) {
      final OrganicReactionDomain domain = this.domains.get(i);
      if (domain == OrganicReactionDomain.UNKNOWN || request.hasDomain(domain)) {
        return true;
      }
    }
    return false;
  }

  private static OrganicReactionDomain requiredDomain(final OrganicReactionRuleCategory category) {
    if (category == OrganicReactionRuleCategory.COUPLING) {
      return OrganicReactionDomain.CROSS_COUPLING;
    }
    if (category == OrganicReactionRuleCategory.SUBSTITUTION) {
      return OrganicReactionDomain.NUCLEOPHILIC_SUBSTITUTION;
    }
    if (category == OrganicReactionRuleCategory.ADDITION) {
      return OrganicReactionDomain.ADDITION;
    }
    if (category == OrganicReactionRuleCategory.ELIMINATION) {
      return OrganicReactionDomain.ELIMINATION;
    }
    if (category == OrganicReactionRuleCategory.OXIDATION
        || category == OrganicReactionRuleCategory.REDUCTION) {
      return OrganicReactionDomain.OXIDATION_REDUCTION;
    }
    if (category == OrganicReactionRuleCategory.ACID_BASE) {
      return OrganicReactionDomain.ACID_BASE;
    }
    if (category == OrganicReactionRuleCategory.PROTECTION
        || category == OrganicReactionRuleCategory.DEPROTECTION) {
      return OrganicReactionDomain.PROTECTION_DEPROTECTION;
    }
    if (category == OrganicReactionRuleCategory.REARRANGEMENT) {
      return OrganicReactionDomain.REARRANGEMENT;
    }
    if (category == OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS) {
      return OrganicReactionDomain.HETEROCYCLE_SYNTHESIS;
    }
    if (category == OrganicReactionRuleCategory.RADICAL_FUNCTIONALIZATION) {
      return OrganicReactionDomain.RADICAL_FUNCTIONALIZATION;
    }
    return OrganicReactionDomain.UNKNOWN;
  }

  private static double clampedConfidence(final double confidence) {
    if (confidence <= 0.0) {
      return 0.01;
    }
    return Math.min(1.0, confidence);
  }

  private static List<OrganicReactionDomain> checkedDomains(
      final List<OrganicReactionDomain> domains) {
    if (domains == null || domains.isEmpty()) {
      return List.of(OrganicReactionDomain.UNKNOWN);
    }
    for (int i = 0; i < domains.size(); ++i) {
      if (domains.get(i) == null) {
        throw new IllegalArgumentException("Replacing organic rule domain must not be null.");
      }
    }
    return List.copyOf(domains);
  }

  private static List<MolecularFeatureKind> checkedFeatures(
      final List<MolecularFeatureKind> features) {
    if (features == null || features.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < features.size(); ++i) {
      if (features.get(i) == null) {
        throw new IllegalArgumentException("Replacing organic rule feature must not be null.");
      }
    }
    return List.copyOf(features);
  }

  private static List<ReactionConditionFeatureKind> checkedConditionFeatures(
      final List<ReactionConditionFeatureKind> features) {
    if (features == null || features.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < features.size(); ++i) {
      if (features.get(i) == null) {
        throw new IllegalArgumentException(
            "Replacing organic rule condition feature must not be null.");
      }
    }
    return List.copyOf(features);
  }
}
