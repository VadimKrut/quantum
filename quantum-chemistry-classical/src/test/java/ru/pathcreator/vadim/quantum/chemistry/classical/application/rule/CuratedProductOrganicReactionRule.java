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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;

/** Явное curated-правило, которое возвращает только заранее заданный набор продуктов. */
public final class CuratedProductOrganicReactionRule implements OrganicReactionRule {

  private final OrganicReactionRuleId id;
  private final String displayName;
  private final OrganicReactionRuleCategory category;
  private final List<OrganicReactionDomain> domains;
  private final List<MolecularFeatureKind> requiredReactantFeatures;
  private final List<ReactionConditionFeatureKind> supportiveConditionFeatures;
  private final ReactionSide products;
  private final double baseProbability;
  private final double expectedYieldPercent;
  private final String selectivityNote;
  private final String explanation;
  private final MolecularFeatureAnalyzer featureAnalyzer;

  private CuratedProductOrganicReactionRule(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredReactantFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final ReactionSide products,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation,
      final MolecularFeatureAnalyzer featureAnalyzer) {
    this.id = id;
    this.displayName = displayName;
    this.category = category;
    this.domains = domains;
    this.requiredReactantFeatures = requiredReactantFeatures;
    this.supportiveConditionFeatures = supportiveConditionFeatures;
    this.products = products;
    this.baseProbability = baseProbability;
    this.expectedYieldPercent = expectedYieldPercent;
    this.selectivityNote = selectivityNote;
    this.explanation = explanation;
    this.featureAnalyzer = featureAnalyzer;
  }

  public static CuratedProductOrganicReactionRule of(
      final OrganicReactionRuleId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredReactantFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final ReactionSide products,
      final double baseProbability,
      final double expectedYieldPercent,
      final String selectivityNote,
      final String explanation) {
    if (id == null) {
      throw new IllegalArgumentException("Curated organic rule id must not be null.");
    }
    if (category == null) {
      throw new IllegalArgumentException("Curated organic rule category must not be null.");
    }
    if (products == null) {
      throw new IllegalArgumentException("Curated organic rule products must not be null.");
    }
    if (!Double.isFinite(baseProbability) || baseProbability <= 0.0 || baseProbability > 1.0) {
      throw new IllegalArgumentException("Curated organic rule probability must be in (0, 1].");
    }
    if (!Double.isFinite(expectedYieldPercent)
        || expectedYieldPercent < 0.0
        || expectedYieldPercent > 100.0) {
      throw new IllegalArgumentException("Curated organic rule yield percent must be in [0, 100].");
    }
    return new CuratedProductOrganicReactionRule(
        id,
        TextValue.requireText(displayName, "Curated organic rule display name"),
        category,
        CuratedProductOrganicReactionRule.checkedDomains(domains),
        CuratedProductOrganicReactionRule.checkedFeatures(requiredReactantFeatures),
        CuratedProductOrganicReactionRule.checkedConditionFeatures(supportiveConditionFeatures),
        products,
        baseProbability,
        expectedYieldPercent,
        TextValue.optionalText(selectivityNote),
        TextValue.requireText(explanation, "Curated organic rule explanation"),
        new MolecularFeatureAnalyzer());
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
    if (!this.domainMatches(request) || !this.requiredFeaturesMatch(request)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    return OrganicReactionRuleMatch.matched(
        Math.min(1.0, this.baseProbability + this.conditionBonus(request)), this.explanation);
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (match == null || !match.matched()) {
      return List.of();
    }
    return List.of(
        ReactionProductCandidate.of(
            this.products,
            this.baseProbability,
            this.expectedYieldPercent,
            this.selectivityNote,
            this.evidence(match)));
  }

  private List<ReactionPredictionEvidence> evidence(final OrganicReactionRuleMatch match) {
    final ArrayList<ReactionPredictionEvidence> evidence =
        new ArrayList<ReactionPredictionEvidence>(3);
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.USER_RULE,
            "Curated product rule matched: " + this.id.value(),
            match.confidence()));
    evidence.add(
        ReactionPredictionEvidence.of(
            ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
            this.explanation,
            Math.min(0.25, match.confidence() * 0.25)));
    return List.copyOf(evidence);
  }

  private boolean domainMatches(final ReactionPredictionRequest request) {
    for (int i = 0; i < this.domains.size(); ++i) {
      final OrganicReactionDomain domain = this.domains.get(i);
      if (domain == OrganicReactionDomain.UNKNOWN || request.hasDomain(domain)) {
        return true;
      }
    }
    return false;
  }

  private boolean requiredFeaturesMatch(final ReactionPredictionRequest request) {
    for (int i = 0; i < this.requiredReactantFeatures.size(); ++i) {
      if (this.reactantsContainFeature(request, this.requiredReactantFeatures.get(i))) {
        continue;
      }
      return false;
    }
    return true;
  }

  private boolean reactantsContainFeature(
      final ReactionPredictionRequest request, final MolecularFeatureKind feature) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    for (int i = 0; i < participants.size(); ++i) {
      final MolecularFeatureSet features =
          this.featureAnalyzer.analyze(participants.get(i).molecule());
      if (features.contains(feature)) {
        return true;
      }
    }
    return false;
  }

  private double conditionBonus(final ReactionPredictionRequest request) {
    double bonus = 0.0;
    for (int i = 0; i < this.supportiveConditionFeatures.size(); ++i) {
      if (request.conditions().hasFeature(this.supportiveConditionFeatures.get(i))) {
        bonus += 0.025;
      }
    }
    return Math.min(0.10, bonus);
  }

  private static List<OrganicReactionDomain> checkedDomains(
      final List<OrganicReactionDomain> domains) {
    if (domains == null || domains.isEmpty()) {
      return List.of(OrganicReactionDomain.UNKNOWN);
    }
    for (int i = 0; i < domains.size(); ++i) {
      if (domains.get(i) == null) {
        throw new IllegalArgumentException("Curated organic rule domain must not be null.");
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
        throw new IllegalArgumentException("Curated organic rule feature must not be null.");
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
            "Curated organic rule condition feature must not be null.");
      }
    }
    return List.copyOf(features);
  }
}
