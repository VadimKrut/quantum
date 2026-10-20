/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/**
 * Расчетная оценка одного сценария условий реакции после повторного прогноза продуктов.
 */
public final class ReactionConditionScenarioEvaluation {

  private final ReactionConditionScenario scenario;
  private final ReactionPredictionResult predictionResult;
  private final double bestProbability;
  private final double bestExpectedYieldPercent;
  private final double probabilityMass;
  private final double baselineProbabilityDelta;
  private final double baselineExpectedYieldDelta;
  private final int diagnosticCount;

  private ReactionConditionScenarioEvaluation(
      final ReactionConditionScenario scenario,
      final ReactionPredictionResult predictionResult,
      final double bestProbability,
      final double bestExpectedYieldPercent,
      final double probabilityMass,
      final double baselineProbabilityDelta,
      final double baselineExpectedYieldDelta,
      final int diagnosticCount) {
    this.scenario = scenario;
    this.predictionResult = predictionResult;
    this.bestProbability = bestProbability;
    this.bestExpectedYieldPercent = bestExpectedYieldPercent;
    this.probabilityMass = probabilityMass;
    this.baselineProbabilityDelta = baselineProbabilityDelta;
    this.baselineExpectedYieldDelta = baselineExpectedYieldDelta;
    this.diagnosticCount = diagnosticCount;
  }

  public static ReactionConditionScenarioEvaluation of(
      final ReactionConditionScenario scenario, final ReactionPredictionResult predictionResult) {
    return ReactionConditionScenarioEvaluation.of(scenario, predictionResult, 0.0, 0.0);
  }

  public static ReactionConditionScenarioEvaluation of(
      final ReactionConditionScenario scenario,
      final ReactionPredictionResult predictionResult,
      final double baselineProbability,
      final double baselineExpectedYieldPercent) {
    if (scenario == null) {
      throw new IllegalArgumentException(
          "Reaction condition scenario evaluation scenario must not be null.");
    }
    if (predictionResult == null) {
      throw new IllegalArgumentException(
          "Reaction condition scenario evaluation prediction result must not be null.");
    }
    final double probability;
    final double yield;
    if (predictionResult.hasCandidates()) {
      final ReactionProductCandidate bestCandidate = predictionResult.bestCandidate();
      probability = bestCandidate.probability();
      yield = bestCandidate.expectedYieldPercent();
    } else {
      probability = 0.0;
      yield = 0.0;
    }
    return new ReactionConditionScenarioEvaluation(
        scenario,
        predictionResult,
        probability,
        yield,
        predictionResult.probabilityMass(),
        probability - baselineProbability,
        yield - baselineExpectedYieldPercent,
        predictionResult.diagnostics().size());
  }

  public ReactionConditionScenario scenario() {
    return this.scenario;
  }

  public ReactionPredictionResult predictionResult() {
    return this.predictionResult;
  }

  public boolean hasCandidates() {
    return this.predictionResult.hasCandidates();
  }

  public double bestProbability() {
    return this.bestProbability;
  }

  public double bestExpectedYieldPercent() {
    return this.bestExpectedYieldPercent;
  }

  public double probabilityMass() {
    return this.probabilityMass;
  }

  public double baselineProbabilityDelta() {
    return this.baselineProbabilityDelta;
  }

  public double baselineExpectedYieldDelta() {
    return this.baselineExpectedYieldDelta;
  }

  public boolean improvesBaseline() {
    return this.baselineProbabilityDelta > 0.01 || this.baselineExpectedYieldDelta > 1.0;
  }

  public boolean worsensBaseline() {
    return this.baselineProbabilityDelta < -0.01 || this.baselineExpectedYieldDelta < -1.0;
  }

  public int diagnosticCount() {
    return this.diagnosticCount;
  }

  public double rankingScore() {
    return this.bestProbability
        + this.bestExpectedYieldPercent / 100.0
        + Math.max(-0.15, Math.min(0.15, this.baselineProbabilityDelta))
        + Math.max(-0.10, Math.min(0.10, this.baselineExpectedYieldDelta / 100.0))
        + Math.min(1.0, this.probabilityMass) * 0.05
        - this.diagnosticCount * 0.01;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionConditionScenarioEvaluation)) {
      return false;
    }
    final ReactionConditionScenarioEvaluation evaluation =
        (ReactionConditionScenarioEvaluation) other;
    return Double.compare(this.bestProbability, evaluation.bestProbability) == 0
        && Double.compare(this.bestExpectedYieldPercent, evaluation.bestExpectedYieldPercent) == 0
        && Double.compare(this.probabilityMass, evaluation.probabilityMass) == 0
        && Double.compare(this.baselineProbabilityDelta, evaluation.baselineProbabilityDelta) == 0
        && Double.compare(this.baselineExpectedYieldDelta, evaluation.baselineExpectedYieldDelta)
            == 0
        && this.diagnosticCount == evaluation.diagnosticCount
        && Objects.equals(this.scenario, evaluation.scenario)
        && Objects.equals(this.predictionResult, evaluation.predictionResult);
  }

  public int hashCode() {
    return Objects.hash(
        this.scenario,
        this.predictionResult,
        this.bestProbability,
        this.bestExpectedYieldPercent,
        this.probabilityMass,
        this.baselineProbabilityDelta,
        this.baselineExpectedYieldDelta,
        this.diagnosticCount);
  }
}
