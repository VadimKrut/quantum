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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.FragmentTransferTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MolecularTransformProduct;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.FragmentTransferTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Правило, которое строит продукт переносом фрагмента между двумя реальными молекулярными графами. */
public final class FragmentTransferOrganicReactionRule implements OrganicReactionRule {

  private final OrganicReactionRuleId id;
  private final String displayName;
  private final OrganicReactionRuleCategory category;
  private final List<OrganicReactionDomain> domains;
  private final List<MolecularFeatureKind> requiredScaffoldFeatures;
  private final List<MolecularFeatureKind> requiredFragmentFeatures;
  private final List<ReactionConditionFeatureKind> supportiveConditionFeatures;
  private final FragmentTransferTransform transform;
  private final double baseProbability;
  private final double expectedYieldPercent;
  private final String selectivityNote;
  private final String explanation;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final OrganicReactionConditionMatcher conditionMatcher;
  private final FragmentTransferTransformer transformer;
  private final ReactionSiteSelectivityAnalyzer siteSelectivityAnalyzer;

  private FragmentTransferOrganicReactionRule(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredScaffoldFeatures,
      final List<MolecularFeatureKind> requiredFragmentFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final FragmentTransferTransform transform,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation) {
    this.id = id;
    this.displayName = displayName;
    this.category = category;
    this.domains = domains;
    this.requiredScaffoldFeatures = requiredScaffoldFeatures;
    this.requiredFragmentFeatures = requiredFragmentFeatures;
    this.supportiveConditionFeatures = supportiveConditionFeatures;
    this.transform = transform;
    this.baseProbability = baseProbability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.selectivityNote = selectivityNote;
    this.explanation = explanation;
    this.featureAnalyzer = new MolecularFeatureAnalyzer();
    this.conditionMatcher = new OrganicReactionConditionMatcher();
    this.transformer = new FragmentTransferTransformer();
    this.siteSelectivityAnalyzer = new ReactionSiteSelectivityAnalyzer();
  }

  public static FragmentTransferOrganicReactionRule of(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredScaffoldFeatures,
      final List<MolecularFeatureKind> requiredFragmentFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final FragmentTransferTransform transform,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation) {
    if (id == null) {
      throw new IllegalArgumentException("Fragment-transfer organic rule id must not be null.");
    }
    if (category == null) {
      throw new IllegalArgumentException(
          "Fragment-transfer organic rule category must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException(
          "Fragment-transfer organic rule transform must not be null.");
    }
    if (!Double.isFinite(baseProbability) || baseProbability <= 0.0 || baseProbability > 1.0) {
      throw new IllegalArgumentException(
          "Fragment-transfer organic rule probability must be in (0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Fragment-transfer organic rule yield percent must be in [0, 100].");
    }
    return new FragmentTransferOrganicReactionRule(
        id,
        TextValue.requireText(displayName, "Fragment-transfer organic rule display name"),
        category,
        FragmentTransferOrganicReactionRule.checkedDomains(domains),
        FragmentTransferOrganicReactionRule.checkedFeatures(requiredScaffoldFeatures),
        FragmentTransferOrganicReactionRule.checkedFeatures(requiredFragmentFeatures),
        FragmentTransferOrganicReactionRule.checkedConditionFeatures(supportiveConditionFeatures),
        transform,
        baseProbability,
        expectedYieldPercent,
        TextValue.optionalText(selectivityNote),
        TextValue.requireText(explanation, "Fragment-transfer organic rule explanation"));
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
    if (this.matchingPairs(request).isEmpty()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(request.conditions(), this.supportiveConditionFeatures);
    if (conditionAssessment.stronglyIncompatible()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        FragmentTransferOrganicReactionRule.clampedConfidence(
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
    final List<ReactantPair> pairs = this.matchingPairs(request);
    if (pairs.isEmpty()) {
      return List.of();
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(request.conditions(), this.supportiveConditionFeatures);
    final ArrayList<ReactionProductCandidate> candidates =
        new ArrayList<ReactionProductCandidate>();
    for (int pairIndex = 0; pairIndex < pairs.size(); ++pairIndex) {
      final ReactantPair pair = pairs.get(pairIndex);
      final List<MolecularTransformProduct> products =
          this.transformer.transformAll(pair.scaffold(), pair.fragment(), this.transform);
      for (int productIndex = 0; productIndex < products.size(); ++productIndex) {
        final MolecularTransformProduct product = products.get(productIndex);
        final ReactionSiteSelectivityAssessment siteAssessment =
            this.siteSelectivityAnalyzer.assess(
                request, pair.scaffold(), product, this.baseProbability, this.expectedYieldPercent);
        candidates.add(
            ReactionProductCandidate.of(
                ReactionProductSideFactory.fromTransformProduct(product),
                siteAssessment.probability(),
                siteAssessment.expectedYieldPercent(),
                this.selectivityNote,
                this.evidence(match, conditionAssessment, siteAssessment, pair, product)));
      }
    }
    return List.copyOf(candidates);
  }

  private List<ReactionPredictionEvidence> evidence(
      final OrganicReactionRuleMatch match,
      final OrganicReactionConditionAssessment conditionAssessment,
      final ReactionSiteSelectivityAssessment siteAssessment,
      final ReactantPair pair,
      final MolecularTransformProduct product) {
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(
            4 + conditionAssessment.evidence().size() + siteAssessment.evidence().size());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Fragment-transfer rule matched: " + this.id.value(),
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
                + " by graph fragment transfer from "
                + pair.fragment().id().value()
                + " onto "
                + pair.scaffold().id().value()
                + " at "
                + product.mappingDescription(),
            0.14));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Product was assembled from reactant graphs, not from a benchmark outcome lookup.",
            0.08));
    evidence.addAll(conditionAssessment.evidence());
    evidence.addAll(siteAssessment.evidence());
    return List.copyOf(evidence);
  }

  private List<ReactantPair> matchingPairs(final ReactionPredictionRequest request) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    final ArrayList<ReactantPair> pairs = new ArrayList<ReactantPair>();
    for (int scaffoldIndex = 0; scaffoldIndex < participants.size(); ++scaffoldIndex) {
      final Molecule scaffold = participants.get(scaffoldIndex).molecule();
      if (!this.requiredFeaturesMatch(scaffold, this.requiredScaffoldFeatures)) {
        continue;
      }
      for (int fragmentIndex = 0; fragmentIndex < participants.size(); ++fragmentIndex) {
        if (scaffoldIndex == fragmentIndex) {
          continue;
        }
        final Molecule fragment = participants.get(fragmentIndex).molecule();
        if (this.requiredFeaturesMatch(fragment, this.requiredFragmentFeatures)) {
          pairs.add(new ReactantPair(scaffold, fragment));
        }
      }
    }
    return List.copyOf(pairs);
  }

  private boolean requiredFeaturesMatch(
      final Molecule molecule, final List<MolecularFeatureKind> requiredFeatures) {
    final MolecularFeatureSet features = this.featureAnalyzer.analyze(molecule);
    for (int i = 0; i < requiredFeatures.size(); ++i) {
      if (features.contains(requiredFeatures.get(i))) {
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
        FragmentTransferOrganicReactionRule.requiredDomain(this.category);
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
    if (category == OrganicReactionRuleCategory.HETEROCYCLE_SYNTHESIS) {
      return OrganicReactionDomain.HETEROCYCLE_SYNTHESIS;
    }
    if (category == OrganicReactionRuleCategory.HETEROCYCLE_FUNCTIONALIZATION) {
      return OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION;
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
        throw new IllegalArgumentException(
            "Fragment-transfer organic rule domain must not be null.");
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
        throw new IllegalArgumentException(
            "Fragment-transfer organic rule feature must not be null.");
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
            "Fragment-transfer organic rule condition feature must not be null.");
      }
    }
    return List.copyOf(features);
  }

  private static final class ReactantPair {

    private final Molecule scaffold;
    private final Molecule fragment;

    private ReactantPair(final Molecule scaffold, final Molecule fragment) {
      this.scaffold = scaffold;
      this.fragment = fragment;
    }

    private Molecule scaffold() {
      return this.scaffold;
    }

    private Molecule fragment() {
      return this.fragment;
    }
  }
}
