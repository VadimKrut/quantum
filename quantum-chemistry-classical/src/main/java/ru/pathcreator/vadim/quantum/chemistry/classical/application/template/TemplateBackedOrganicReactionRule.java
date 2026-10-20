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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRule;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleCategory;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule.OrganicReactionRuleMatch;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.template.OrganicReactionTemplate;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/** Rule-adapter для шаблонов, которые дают применимость и объяснение без генерации продукта. */
public final class TemplateBackedOrganicReactionRule implements OrganicReactionRule {

  private final OrganicReactionTemplate template;
  private final OrganicReactionTemplateMatcher matcher;

  public TemplateBackedOrganicReactionRule(
      final OrganicReactionTemplate template, final OrganicReactionTemplateMatcher matcher) {
    if (template == null) {
      throw new IllegalArgumentException("Organic reaction template must not be null.");
    }
    if (matcher == null) {
      throw new IllegalArgumentException("Organic reaction template matcher must not be null.");
    }
    this.template = template;
    this.matcher = matcher;
  }

  public OrganicReactionRuleId id() {
    return OrganicReactionRuleId.of(this.template.id().value());
  }

  public String displayName() {
    return this.template.displayName();
  }

  public OrganicReactionRuleCategory category() {
    return this.template.category();
  }

  public OrganicReactionRuleMatch match(final ReactionPredictionRequest request) {
    return this.matcher.match(this.template, request);
  }

  public List<ReactionProductCandidate> generate(
      final ReactionPredictionRequest request, final OrganicReactionRuleMatch match) {
    return List.of();
  }
}
