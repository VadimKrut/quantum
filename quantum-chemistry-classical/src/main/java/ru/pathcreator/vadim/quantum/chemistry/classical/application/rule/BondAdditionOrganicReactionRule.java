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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.BondAdditionTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.HydrogenAdditionTransformer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.HydrogenAdditionDonor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.transform.MolecularTransformProduct;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.transform.BondAdditionTransform;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Правило, которое строит продукты присоединения через кратную связь молекулярного графа. */
public final class BondAdditionOrganicReactionRule implements OrganicReactionRule {

  private final OrganicReactionRuleId id;
  private final String displayName;
  private final OrganicReactionRuleCategory category;
  private final List<OrganicReactionDomain> domains;
  private final List<MolecularFeatureKind> requiredReactantFeatures;
  private final List<ReactionConditionFeatureKind> supportiveConditionFeatures;
  private final BondAdditionTransform transform;
  private final double baseProbability;
  private final double expectedYieldPercent;
  private final String selectivityNote;
  private final String explanation;
  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final OrganicReactionConditionMatcher conditionMatcher;
  private final BondAdditionTransformer transformer;
  private final ReactionSiteSelectivityAnalyzer siteSelectivityAnalyzer;
  private final HydrogenAdditionDonor donorKind;

  private BondAdditionOrganicReactionRule(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredReactantFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final BondAdditionTransform transform,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation,
      final HydrogenAdditionDonor donorKind) {
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
    this.transformer = new BondAdditionTransformer();
    this.siteSelectivityAnalyzer = new ReactionSiteSelectivityAnalyzer();
    this.donorKind = donorKind;
  }

  public static BondAdditionOrganicReactionRule of(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredReactantFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final BondAdditionTransform transform,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation) {
    if (id == null) {
      throw new IllegalArgumentException("Bond-addition organic rule id must not be null.");
    }
    if (category == null) {
      throw new IllegalArgumentException("Bond-addition organic rule category must not be null.");
    }
    if (transform == null) {
      throw new IllegalArgumentException("Bond-addition organic rule transform must not be null.");
    }
    if (!Double.isFinite(baseProbability) || baseProbability <= 0.0 || baseProbability > 1.0) {
      throw new IllegalArgumentException(
          "Bond-addition organic rule probability must be in (0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException(
          "Bond-addition organic rule yield percent must be in [0, 100].");
    }
    return new BondAdditionOrganicReactionRule(
        id,
        TextValue.requireText(displayName, "Bond-addition organic rule display name"),
        category,
        BondAdditionOrganicReactionRule.checkedDomains(domains),
        BondAdditionOrganicReactionRule.checkedFeatures(requiredReactantFeatures),
        BondAdditionOrganicReactionRule.checkedConditionFeatures(supportiveConditionFeatures),
        transform,
        baseProbability,
        expectedYieldPercent,
        TextValue.optionalText(selectivityNote),
        TextValue.requireText(explanation, "Bond-addition organic rule explanation"),
        null);
  }

  public BondAdditionOrganicReactionRule withExplicitDonor(final HydrogenAdditionDonor kind) {
    return new BondAdditionOrganicReactionRule(
        this.id,
        this.displayName,
        this.category,
        this.domains,
        this.requiredReactantFeatures,
        this.supportiveConditionFeatures,
        this.transform,
        this.baseProbability,
        this.expectedYieldPercent,
        this.selectivityNote,
        this.explanation,
        java.util.Objects.requireNonNull(kind));
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
        BondAdditionOrganicReactionRule.clampedConfidence(
            this.baseProbability + conditionAssessment.scoreAdjustment()),
        this.explanation
            + (this.donorKind == null
                ? ""
                : " Requires an explicit connected reactant graph of "
                    + this.donorKind.description()
                    + "; condition labels and unrelated atoms cannot replace this donor."));
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
    final OrganicReactionConditionAssessment conditionAssessment =
        this.conditionMatcher.assess(request.conditions(), this.supportiveConditionFeatures);
    final ArrayList<ReactionProductCandidate> candidates =
        new ArrayList<ReactionProductCandidate>();
    for (int reactantIndex = 0; reactantIndex < reactants.size(); ++reactantIndex) {
      final Molecule reactant = reactants.get(reactantIndex);
      final List<MolecularTransformProduct> products = this.products(request, reactant);
      for (int productIndex = 0; productIndex < products.size(); ++productIndex) {
        final MolecularTransformProduct product = products.get(productIndex);
        final ReactionSiteSelectivityAssessment siteAssessment =
            this.siteSelectivityAnalyzer.assess(
                request,
                reactant,
                reactant,
                product,
                this.baseProbability,
                this.expectedYieldPercent,
                this.antiMarkovnikovAddition());
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

  private List<MolecularTransformProduct> products(
      final ReactionPredictionRequest request, final Molecule reactant) {
    if (this.donorKind == null) {
      return this.transformer.transformAll(reactant, this.transform);
    }
    final var products = new ArrayList<MolecularTransformProduct>();
    for (final var participant : request.reactants().participants()) {
      if (participant.molecule() == reactant || participant.molecule().equals(reactant)) {
        continue;
      }
      products.addAll(
          new HydrogenAdditionTransformer()
              .transformAll(reactant, this.transform, participant.molecule(), this.donorKind));
    }
    return List.copyOf(products);
  }

  private List<ReactionPredictionEvidence> evidence(
      final OrganicReactionRuleMatch match,
      final OrganicReactionConditionAssessment conditionAssessment,
      final ReactionSiteSelectivityAssessment siteAssessment,
      final Molecule reactant,
      final MolecularTransformProduct product) {
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(
            4 + conditionAssessment.evidence().size() + siteAssessment.evidence().size());
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Bond-addition rule matched: " + this.id.value(),
            match.confidence()));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            this.transform.explanation(),
            Math.min(0.25, match.confidence() * 0.25)));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
            "Generated "
                + product.product().id().value()
                + " from "
                + reactant.id().value()
                + " by graph bond-addition at "
                + product.mappingDescription(),
            0.14));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.COMPETING_PATHWAY,
            "Bond order and both addition sites are represented in the generated molecular graph.",
            0.08));
    evidence.addAll(conditionAssessment.evidence());
    if (this.donorKind != null) {
      evidence.add(
          ReactionPredictionEvidence.of(
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Transferred explicit donor graph "
                  + product.product().metadata().attributes().get("reaction.donor")
                  + " by breaking "
                  + product.product().metadata().attributes().get("reaction.donor.broken-bond")
                  + "; all donor atoms, remaining bonds and isotopes are retained. Product stereochemistry requires review.",
              0.0));
    }
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

  private boolean antiMarkovnikovAddition() {
    final String suffix = this.transform.idSuffix().toLowerCase(java.util.Locale.ROOT);
    final String name = this.displayName.toLowerCase(java.util.Locale.ROOT);
    return suffix.contains("hydroboration")
        || suffix.contains("anti_markovnikov")
        || name.contains("hydroboration")
        || name.contains("anti-markovnikov");
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
    for (int i = 0; i < this.domains.size(); ++i) {
      final OrganicReactionDomain domain = this.domains.get(i);
      if (domain == OrganicReactionDomain.UNKNOWN || request.hasDomain(domain)) {
        return true;
      }
    }
    return false;
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
        throw new IllegalArgumentException("Bond-addition organic rule domain must not be null.");
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
        throw new IllegalArgumentException("Bond-addition organic rule feature must not be null.");
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
            "Bond-addition organic rule condition feature must not be null.");
      }
    }
    return List.copyOf(features);
  }
}
