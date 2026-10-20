/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry;

import java.util.List;

/** Сводка количественной рецептуры реакции после сопоставления компонентов с молекулами. */
public final class ReactionStoichiometryProfile {

  public static final ReactionStoichiometryProfile EMPTY =
      new ReactionStoichiometryProfile(List.of(), 0, 0, 0.0, 0.0, 0.0);
  private final List<ReactionStoichiometryComponent> matchedComponents;
  private final int quantifiedReactantCount;
  private final int unmatchedReactantCount;
  private final double minimumEquivalentRatio;
  private final double maximumEquivalentRatio;
  private final double totalGramScale;

  private ReactionStoichiometryProfile(
      final List<ReactionStoichiometryComponent> matchedComponents,
      final int quantifiedReactantCount,
      final int unmatchedReactantCount,
      final double minimumEquivalentRatio,
      final double maximumEquivalentRatio,
      final double totalGramScale) {
    this.matchedComponents = matchedComponents;
    this.quantifiedReactantCount = quantifiedReactantCount;
    this.unmatchedReactantCount = unmatchedReactantCount;
    this.minimumEquivalentRatio = minimumEquivalentRatio;
    this.maximumEquivalentRatio = maximumEquivalentRatio;
    this.totalGramScale = totalGramScale;
  }

  public static ReactionStoichiometryProfile of(
      final List<ReactionStoichiometryComponent> matchedComponents,
      final int quantifiedReactantCount,
      final int unmatchedReactantCount,
      final double minimumEquivalentRatio,
      final double maximumEquivalentRatio,
      final double totalGramScale) {
    if (matchedComponents == null || matchedComponents.isEmpty()) {
      return new ReactionStoichiometryProfile(
          List.of(),
          Math.max(0, quantifiedReactantCount),
          Math.max(0, unmatchedReactantCount),
          0.0,
          0.0,
          Math.max(0.0, totalGramScale));
    }
    return new ReactionStoichiometryProfile(
        List.copyOf(matchedComponents),
        Math.max(0, quantifiedReactantCount),
        Math.max(0, unmatchedReactantCount),
        minimumEquivalentRatio,
        maximumEquivalentRatio,
        Math.max(0.0, totalGramScale));
  }

  public List<ReactionStoichiometryComponent> matchedComponents() {
    return this.matchedComponents;
  }

  public int quantifiedReactantCount() {
    return this.quantifiedReactantCount;
  }

  public int matchedReactantCount() {
    return this.matchedComponents.size();
  }

  public int unmatchedReactantCount() {
    return this.unmatchedReactantCount;
  }

  public double minimumEquivalentRatio() {
    return this.minimumEquivalentRatio;
  }

  public double maximumEquivalentRatio() {
    return this.maximumEquivalentRatio;
  }

  public double totalGramScale() {
    return this.totalGramScale;
  }

  public boolean hasMatchedQuantities() {
    return !this.matchedComponents.isEmpty();
  }

  public boolean hasEquivalentExcess(final double thresholdRatio) {
    return this.matchedComponents.size() >= 2
        && this.minimumEquivalentRatio > 0.0
        && this.maximumEquivalentRatio / this.minimumEquivalentRatio >= thresholdRatio;
  }
}
