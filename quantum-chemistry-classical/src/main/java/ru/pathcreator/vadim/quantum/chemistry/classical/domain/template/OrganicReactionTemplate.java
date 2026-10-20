/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.template;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;

/** Декларативный шаблон, который описывает применимость семейства органических реакций. */
public final class OrganicReactionTemplate {

  private final OrganicReactionTemplateId id;
  private final String displayName;
  private final OrganicReactionRuleCategory category;
  private final List<OrganicReactionDomain> domains;
  private final List<MolecularFeatureKind> requiredMolecularFeatures;
  private final List<ReactionConditionFeatureKind> supportiveConditionFeatures;
  private final double baseConfidence;
  private final String explanation;

  private OrganicReactionTemplate(
      final OrganicReactionTemplateId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredMolecularFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final double baseConfidence,
      final String explanation) {
    this.id = id;
    this.displayName = displayName;
    this.category = category;
    this.domains = domains;
    this.requiredMolecularFeatures = requiredMolecularFeatures;
    this.supportiveConditionFeatures = supportiveConditionFeatures;
    this.baseConfidence = baseConfidence;
    this.explanation = explanation;
  }

  public static OrganicReactionTemplate of(
      final OrganicReactionTemplateId id,
      final String displayName,
      final OrganicReactionRuleCategory category,
      final List<OrganicReactionDomain> domains,
      final List<MolecularFeatureKind> requiredMolecularFeatures,
      final List<ReactionConditionFeatureKind> supportiveConditionFeatures,
      final double baseConfidence,
      final String explanation) {
    if (id == null) {
      throw new IllegalArgumentException("Organic reaction template id must not be null.");
    }
    final String checkedName = TextValue.requireText(displayName, "Organic reaction template name");
    if (category == null) {
      throw new IllegalArgumentException("Organic reaction template category must not be null.");
    }
    final List<OrganicReactionDomain> checkedDomains =
        OrganicReactionTemplate.checkedDomains(domains);
    final List<MolecularFeatureKind> checkedFeatures =
        OrganicReactionTemplate.checkedFeatures(requiredMolecularFeatures);
    final List<ReactionConditionFeatureKind> checkedConditionFeatures =
        OrganicReactionTemplate.checkedConditionFeatures(supportiveConditionFeatures);
    if (!Double.isFinite(baseConfidence) || baseConfidence <= 0.0 || baseConfidence > 1.0) {
      throw new IllegalArgumentException("Organic reaction template confidence must be in (0, 1].");
    }
    return new OrganicReactionTemplate(
        id,
        checkedName,
        category,
        checkedDomains,
        checkedFeatures,
        checkedConditionFeatures,
        baseConfidence,
        TextValue.requireText(explanation, "Organic reaction template explanation"));
  }

  public OrganicReactionTemplateId id() {
    return this.id;
  }

  public String displayName() {
    return this.displayName;
  }

  public OrganicReactionRuleCategory category() {
    return this.category;
  }

  public List<OrganicReactionDomain> domains() {
    return this.domains;
  }

  public List<MolecularFeatureKind> requiredMolecularFeatures() {
    return this.requiredMolecularFeatures;
  }

  public List<ReactionConditionFeatureKind> supportiveConditionFeatures() {
    return this.supportiveConditionFeatures;
  }

  public double baseConfidence() {
    return this.baseConfidence;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrganicReactionTemplate)) {
      return false;
    }
    final OrganicReactionTemplate template = (OrganicReactionTemplate) other;
    return Double.compare(this.baseConfidence, template.baseConfidence) == 0
        && Objects.equals(this.id, template.id)
        && Objects.equals(this.displayName, template.displayName)
        && this.category == template.category
        && Objects.equals(this.domains, template.domains)
        && Objects.equals(this.requiredMolecularFeatures, template.requiredMolecularFeatures)
        && Objects.equals(this.supportiveConditionFeatures, template.supportiveConditionFeatures)
        && Objects.equals(this.explanation, template.explanation);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.displayName,
        this.category,
        this.domains,
        this.requiredMolecularFeatures,
        this.supportiveConditionFeatures,
        this.baseConfidence,
        this.explanation);
  }

  private static List<OrganicReactionDomain> checkedDomains(
      final List<OrganicReactionDomain> domains) {
    if (domains == null || domains.isEmpty()) {
      return List.of(OrganicReactionDomain.UNKNOWN);
    }
    for (int i = 0; i < domains.size(); ++i) {
      if (domains.get(i) == null) {
        throw new IllegalArgumentException("Organic reaction template domain must not be null.");
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
        throw new IllegalArgumentException("Organic reaction template feature must not be null.");
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
            "Organic reaction template condition feature must not be null.");
      }
    }
    return List.copyOf(features);
  }
}
