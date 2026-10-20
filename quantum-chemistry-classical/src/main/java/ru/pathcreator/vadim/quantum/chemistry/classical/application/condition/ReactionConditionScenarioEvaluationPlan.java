/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Результат condition screening: исходная матрица условий и прогнозы для каждого сценария.
 */
public final class ReactionConditionScenarioEvaluationPlan {

  private final String requestId;
  private final List<ReactionConditionScenarioEvaluation> evaluations;

  private ReactionConditionScenarioEvaluationPlan(
      final String requestId, final List<ReactionConditionScenarioEvaluation> evaluations) {
    this.requestId = requestId;
    this.evaluations = evaluations;
  }

  public static ReactionConditionScenarioEvaluationPlan of(
      final String requestId, final List<ReactionConditionScenarioEvaluation> evaluations) {
    return new ReactionConditionScenarioEvaluationPlan(
        TextValue.requireText(requestId, "Reaction condition scenario evaluation plan request id"),
        ReactionConditionScenarioEvaluationPlan.checkedEvaluations(evaluations));
  }

  public String requestId() {
    return this.requestId;
  }

  public List<ReactionConditionScenarioEvaluation> evaluations() {
    return this.evaluations;
  }

  public boolean empty() {
    return this.evaluations.isEmpty();
  }

  public ReactionConditionScenarioEvaluation bestEvaluation() {
    if (this.evaluations.isEmpty()) {
      throw new IllegalStateException("Reaction condition scenario evaluation plan is empty.");
    }
    ReactionConditionScenarioEvaluation bestEvaluation = this.evaluations.get(0);
    for (int i = 1; i < this.evaluations.size(); ++i) {
      final ReactionConditionScenarioEvaluation currentEvaluation = this.evaluations.get(i);
      if (currentEvaluation.rankingScore() > bestEvaluation.rankingScore()) {
        bestEvaluation = currentEvaluation;
      }
    }
    return bestEvaluation;
  }

  public List<ReactionConditionScenarioEvaluation> successfulEvaluations() {
    if (this.evaluations.isEmpty()) {
      return List.of();
    }
    final ArrayList<ReactionConditionScenarioEvaluation> result =
        new ArrayList<ReactionConditionScenarioEvaluation>(this.evaluations.size());
    for (int i = 0; i < this.evaluations.size(); ++i) {
      final ReactionConditionScenarioEvaluation evaluation = this.evaluations.get(i);
      if (evaluation.hasCandidates()) {
        result.add(evaluation);
      }
    }
    return List.copyOf(result);
  }

  public List<ReactionConditionScenarioEvaluation> rankedEvaluations() {
    if (this.evaluations.isEmpty()) {
      return List.of();
    }
    final ArrayList<ReactionConditionScenarioEvaluation> result =
        new ArrayList<ReactionConditionScenarioEvaluation>(this.evaluations);
    result.sort((first, second) -> Double.compare(second.rankingScore(), first.rankingScore()));
    return List.copyOf(result);
  }

  public List<ReactionConditionScenarioEvaluation> topEvaluations(final int maximumCount) {
    if (maximumCount <= 0 || this.evaluations.isEmpty()) {
      return List.of();
    }
    final List<ReactionConditionScenarioEvaluation> ranked = this.rankedEvaluations();
    final int count = Math.min(maximumCount, ranked.size());
    final ArrayList<ReactionConditionScenarioEvaluation> result =
        new ArrayList<ReactionConditionScenarioEvaluation>(count);
    for (int i = 0; i < count; ++i) {
      result.add(ranked.get(i));
    }
    return List.copyOf(result);
  }

  public List<ReactionConditionScenarioEvaluation> improvingEvaluations() {
    if (this.evaluations.isEmpty()) {
      return List.of();
    }
    final ArrayList<ReactionConditionScenarioEvaluation> result =
        new ArrayList<ReactionConditionScenarioEvaluation>();
    for (int i = 0; i < this.evaluations.size(); ++i) {
      final ReactionConditionScenarioEvaluation evaluation = this.evaluations.get(i);
      if (evaluation.improvesBaseline()) {
        result.add(evaluation);
      }
    }
    result.sort((first, second) -> Double.compare(second.rankingScore(), first.rankingScore()));
    return List.copyOf(result);
  }

  public List<ReactionConditionScenarioEvaluation> worseningEvaluations() {
    if (this.evaluations.isEmpty()) {
      return List.of();
    }
    final ArrayList<ReactionConditionScenarioEvaluation> result =
        new ArrayList<ReactionConditionScenarioEvaluation>();
    for (int i = 0; i < this.evaluations.size(); ++i) {
      final ReactionConditionScenarioEvaluation evaluation = this.evaluations.get(i);
      if (evaluation.worsensBaseline()) {
        result.add(evaluation);
      }
    }
    result.sort((first, second) -> Double.compare(first.rankingScore(), second.rankingScore()));
    return List.copyOf(result);
  }

  private static List<ReactionConditionScenarioEvaluation> checkedEvaluations(
      final List<ReactionConditionScenarioEvaluation> evaluations) {
    if (evaluations == null || evaluations.isEmpty()) {
      return List.of();
    }
    for (int i = 0; i < evaluations.size(); ++i) {
      if (evaluations.get(i) == null) {
        throw new IllegalArgumentException(
            "Reaction condition scenario evaluation must not be null.");
      }
      for (int j = i + 1; j < evaluations.size(); ++j) {
        if (Objects.equals(
            evaluations.get(i).scenario().id(), evaluations.get(j).scenario().id())) {
          throw new IllegalArgumentException(
              "Reaction condition scenario evaluation ids must be unique.");
        }
      }
    }
    return List.copyOf(evaluations);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionScenarioEvaluationPlan)) {
      return false;
    }
    final ReactionConditionScenarioEvaluationPlan plan =
        (ReactionConditionScenarioEvaluationPlan) other;
    return Objects.equals(this.requestId, plan.requestId)
        && Objects.equals(this.evaluations, plan.evaluations);
  }

  public int hashCode() {
    return Objects.hash(this.requestId, this.evaluations);
  }
}
