/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.rule;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Результат проверки применимости правила к реакционной постановке. */
public final class OrganicReactionRuleMatch {

  public static final OrganicReactionRuleMatch NO_MATCH =
      new OrganicReactionRuleMatch(false, 0.0, "No match");

  private final boolean matched;
  private final double confidence;
  private final String explanation;

  private OrganicReactionRuleMatch(
      final boolean matched, final double confidence, final String explanation) {
    this.matched = matched;
    this.confidence = confidence;
    this.explanation = explanation;
  }

  public static OrganicReactionRuleMatch matched(
      final double confidence, final String explanation) {
    if (!Double.isFinite(confidence) || confidence <= 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("Rule match confidence must be in (0, 1].");
    }
    return new OrganicReactionRuleMatch(
        true,
        confidence,
        TextValue.requireText(explanation, "Organic reaction rule match explanation"));
  }

  public boolean matched() {
    return this.matched;
  }

  public double confidence() {
    return this.confidence;
  }

  public String explanation() {
    return this.explanation;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrganicReactionRuleMatch)) {
      return false;
    }
    final OrganicReactionRuleMatch match = (OrganicReactionRuleMatch) other;
    return this.matched == match.matched
        && Double.compare(this.confidence, match.confidence) == 0
        && Objects.equals(this.explanation, match.explanation);
  }

  public int hashCode() {
    return Objects.hash(this.matched, this.confidence, this.explanation);
  }
}
