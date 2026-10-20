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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.FragmentAttachmentTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MolecularTransformProduct;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.FragmentAttachmentTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Правило присоединения цельного фрагмента второго реагента к выбранному атому scaffold-графа. */
public final class FragmentAttachmentOrganicReactionRule implements OrganicReactionRule {

  private final OrganicReactionRuleId id;
  private final String displayName;
  private final OrganicReactionRuleCategory category;
  private final List<OrganicReactionDomain> domains;
  private final List<MolecularFeatureKind> requiredScaffoldFeatures;
  private final List<MolecularFeatureKind> requiredFragmentFeatures;
  private final List<ReactionConditionFeatureKind> supportiveConditionFeatures;
  private final FragmentAttachmentTransform transform;
  private final double baseProbability;
  private final double expectedYieldPercent;
  private final String selectivityNote;
  private final String explanation;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final OrganicReactionConditionMatcher conditionMatcher;
  private final FragmentAttachmentTransformer transformer;
  private final ReactionSiteSelectivityAnalyzer siteSelectivityAnalyzer;

  private FragmentAttachmentOrganicReactionRule(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredScaffoldFeatures,
      final List<MolecularFeatureKind> requiredFragmentFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final FragmentAttachmentTransform transform,
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
    this.transformer = new FragmentAttachmentTransformer();
    this.siteSelectivityAnalyzer = new ReactionSiteSelectivityAnalyzer();
  }

  public static FragmentAttachmentOrganicReactionRule of(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredScaffoldFeatures,
      final List<MolecularFeatureKind> requiredFragmentFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final FragmentAttachmentTransform transform,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation) {
    if (id == null) {
      throw new IllegalArgumentException("Fragment-attachment organic rule id must not be null.");
    }
    if (category == null) {
      throw new IllegalArgumentException(
          "Fragment-attachment organic rule category must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException(
          "Fragment-attachment organic rule transform must not be null.");
    }
    if (!Double.isFinite(baseProbability) || baseProbability <= 0.0 || baseProbability > 1.0) {
      throw new IllegalArgumentException(
          "Fragment-attachment organic rule probability must be in (0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Fragment-attachment organic rule yield percent must be in [0, 100].");
    }
    return new FragmentAttachmentOrganicReactionRule(
        id,
        TextValue.requireText(displayName, "Fragment-attachment organic rule display name"),
        category,
        FragmentAttachmentOrganicReactionRule.checkedDomains(domains),
        FragmentAttachmentOrganicReactionRule.checkedFeatures(requiredScaffoldFeatures),
        FragmentAttachmentOrganicReactionRule.checkedFeatures(requiredFragmentFeatures),
        FragmentAttachmentOrganicReactionRule.checkedConditionFeatures(supportiveConditionFeatures),
        transform,
        baseProbability,
        expectedYieldPercent,
        TextValue.optionalText(selectivityNote),
        TextValue.requireText(explanation, "Fragment-attachment organic rule explanation"));
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
    if (!this.domainMatches(request) || this.matchingPairs(request).isEmpty()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(request.conditions(), this.supportiveConditionFeatures);
    if (conditionAssessment.stronglyIncompatible()) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        Math.min(1.0, this.baseProbability + conditionAssessment.scoreAdjustment()),
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
                request,
                pair.scaffold(),
                pair.fragment(),
                product,
                this.baseProbability,
                this.expectedYieldPercent);
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
            5 + conditionAssessment.evidence().size() + siteAssessment.evidence().size());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Fragment-attachment rule matched: " + this.id.value(),
            match.confidence()));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            this.transform.explanation(),
            Math.min(0.24, match.confidence() * 0.24)));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Generated "
                + product.product().id().value()
                + " by graph fragment attachment from "
                + pair.fragment().id().value()
                + " onto "
                + pair.scaffold().id().value()
                + " at "
                + product.mappingDescription(),
            0.14));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
            "Product was assembled from reactant graphs, not from a benchmark outcome lookup.",
            0.08));
    evidence.addAll(conditionAssessment.evidence());
    evidence.addAll(siteAssessment.evidence());
    return List.copyOf(evidence);
  }

  private List<ReactantPair> matchingPairs(final ReactionPredictionRequest request) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    final ArrayList<Molecule> scaffolds = new ArrayList<Molecule>();
    final ArrayList<Molecule> fragments = new ArrayList<Molecule>();
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      if (this.requiredFeaturesMatch(molecule, this.requiredScaffoldFeatures)) {
        scaffolds.add(molecule);
      }
      if (this.requiredFeaturesMatch(molecule, this.requiredFragmentFeatures)) {
        fragments.add(molecule);
      }
    }
    final ArrayList<ReactantPair> pairs = new ArrayList<ReactantPair>();
    for (int scaffoldIndex = 0; scaffoldIndex < scaffolds.size(); ++scaffoldIndex) {
      final Molecule scaffold = scaffolds.get(scaffoldIndex);
      for (int fragmentIndex = 0; fragmentIndex < fragments.size(); ++fragmentIndex) {
        final Molecule fragment = fragments.get(fragmentIndex);
        if (!scaffold.equals(fragment)) {
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
      if (!features.contains(requiredFeatures.get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean domainMatches(final ReactionPredictionRequest request) {
    if (request.hasDomain(OrganicReactionDomain.UNKNOWN)) {
      return true;
    }
    for (int i = 0; i < this.domains.size(); ++i) {
      final OrganicReactionDomain domain = this.domains.get(i);
      if (domain == OrganicReactionDomain.UNKNOWN || request.hasDomain(domain)) {
        return true;
      }
    }
    return false;
  }

  private static List<OrganicReactionDomain> checkedDomains(
      final List<OrganicReactionDomain> domains) {
    if (domains == null || domains.isEmpty()) {
      return List.of(OrganicReactionDomain.UNKNOWN);
    }
    for (int i = 0; i < domains.size(); ++i) {
      if (domains.get(i) == null) {
        throw new IllegalArgumentException(
            "Fragment-attachment organic rule domain must not be null.");
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
            "Fragment-attachment organic rule feature must not be null.");
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
            "Fragment-attachment organic rule condition feature must not be null.");
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
