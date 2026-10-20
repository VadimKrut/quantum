/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.template;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.template.OrganicReactionTemplate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;

/** Проверяет применимость декларативного шаблона к запросу прогноза реакции. */
public final class OrganicReactionTemplateMatcher {

  private final MolecularFeatureAnalyzer featureAnalyzer;

  public OrganicReactionTemplateMatcher() {
    this(new MolecularFeatureAnalyzer());
  }

  public OrganicReactionTemplateMatcher(final MolecularFeatureAnalyzer featureAnalyzer) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException("Molecular feature analyzer must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
  }

  public OrganicReactionRuleMatch match(
      final OrganicReactionTemplate template, final ReactionPredictionRequest request) {
    if (template == null) {
      throw new IllegalArgumentException("Organic reaction template must not be null.");
    }
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    if (!this.domainMatches(template, request)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    if (!this.requiredFeaturesMatch(template, request)) {
      return OrganicReactionRuleMatch.NO_MATCH;
    }
    final double confidence =
        Math.min(1.0, template.baseConfidence() + this.conditionBonus(template, request));
    return OrganicReactionRuleMatch.matched(confidence, template.explanation());
  }

  private boolean domainMatches(
      final OrganicReactionTemplate template, final ReactionPredictionRequest request) {
    final List<OrganicReactionDomain> domains = template.domains();
    for (int i = 0; i < domains.size(); ++i) {
      final OrganicReactionDomain domain = domains.get(i);
      if (domain == OrganicReactionDomain.UNKNOWN || request.hasDomain(domain)) {
        return true;
      }
    }
    return false;
  }

  private boolean requiredFeaturesMatch(
      final OrganicReactionTemplate template, final ReactionPredictionRequest request) {
    final List<MolecularFeatureKind> requiredFeatures = template.requiredMolecularFeatures();
    for (int i = 0; i < requiredFeatures.size(); ++i) {
      if (this.reactantsContainFeature(request, requiredFeatures.get(i))) {
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

  private double conditionBonus(
      final OrganicReactionTemplate template, final ReactionPredictionRequest request) {
    double bonus = 0.0;
    final List<ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind>
        features = template.supportiveConditionFeatures();
    for (int i = 0; i < features.size(); ++i) {
      if (request.conditions().hasFeature(features.get(i))) {
        bonus += 0.025;
      }
    }
    return Math.min(0.10, bonus);
  }
}
