/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Одна рекомендация, связывающая результат workflow с последующим расчетом или проверкой.
 */
public final class ChemistryWorkflowRecommendation {

  private static final int PROGRAM_LEVEL_CANDIDATE_INDEX = -1;

  private final ChemistryWorkflowRecommendationKind kind;
  private final ChemistryWorkflowRecommendationPriority priority;
  private final int candidateIndex;
  private final ClassicalCalculationKind calculationKind;
  private final String target;
  private final String reason;

  private ChemistryWorkflowRecommendation(
      final ChemistryWorkflowRecommendationKind kind,
      final ChemistryWorkflowRecommendationPriority priority,
      final int candidateIndex,
      final ClassicalCalculationKind calculationKind,
      final String target,
      final String reason) {
    this.kind = kind;
    this.priority = priority;
    this.candidateIndex = candidateIndex;
    this.calculationKind = calculationKind;
    this.target = target;
    this.reason = reason;
  }

  public static ChemistryWorkflowRecommendation of(
      final ChemistryWorkflowRecommendationKind kind,
      final ChemistryWorkflowRecommendationPriority priority,
      final int candidateIndex,
      final ClassicalCalculationKind calculationKind,
      final String target,
      final String reason) {
    if (kind == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow recommendation kind must not be null.");
    }
    if (priority == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow recommendation priority must not be null.");
    }
    if (candidateIndex < PROGRAM_LEVEL_CANDIDATE_INDEX) {
      throw new IllegalArgumentException(
          "Chemistry workflow recommendation candidate index is invalid.");
    }
    return new ChemistryWorkflowRecommendation(
        kind,
        priority,
        candidateIndex,
        calculationKind,
        TextValue.requireText(target, "Chemistry workflow recommendation target"),
        TextValue.requireText(reason, "Chemistry workflow recommendation reason"));
  }

  public ChemistryWorkflowRecommendationKind kind() {
    return this.kind;
  }

  public ChemistryWorkflowRecommendationPriority priority() {
    return this.priority;
  }

  public int candidateIndex() {
    return this.candidateIndex;
  }

  public boolean appliesToCandidate() {
    return this.candidateIndex >= 0;
  }

  public ClassicalCalculationKind calculationKind() {
    return this.calculationKind;
  }

  public boolean hasCalculationKind() {
    return this.calculationKind != null;
  }

  public String target() {
    return this.target;
  }

  public String reason() {
    return this.reason;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowRecommendation)) {
      return false;
    }
    final ChemistryWorkflowRecommendation recommendation = (ChemistryWorkflowRecommendation) other;
    return this.candidateIndex == recommendation.candidateIndex
        && this.kind == recommendation.kind
        && this.priority == recommendation.priority
        && this.calculationKind == recommendation.calculationKind
        && Objects.equals(this.target, recommendation.target)
        && Objects.equals(this.reason, recommendation.reason);
  }

  public int hashCode() {
    return Objects.hash(
        this.kind,
        this.priority,
        this.candidateIndex,
        this.calculationKind,
        this.target,
        this.reason);
  }

  public String toString() {
    return "ChemistryWorkflowRecommendation{"
        + "kind="
        + this.kind
        + ", priority="
        + this.priority
        + ", candidateIndex="
        + this.candidateIndex
        + ", calculationKind="
        + this.calculationKind
        + ", target='"
        + this.target
        + "', reason='"
        + this.reason
        + "'}";
  }
}
