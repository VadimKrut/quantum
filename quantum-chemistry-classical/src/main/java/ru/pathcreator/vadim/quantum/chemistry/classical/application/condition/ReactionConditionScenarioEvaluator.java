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
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.OrganicReactionPredictionService;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;

/**
 * Прогоняет прогноз продуктов по каждому сценарию условий и ранжирует варианты условий.
 */
public final class ReactionConditionScenarioEvaluator {

  private final ReactionConditionScenarioPlanner scenarioPlanner;
  private final OrganicReactionPredictionService predictionService;

  public ReactionConditionScenarioEvaluator() {
    this(new ReactionConditionScenarioPlanner(), new OrganicReactionPredictionService());
  }

  public ReactionConditionScenarioEvaluator(
      final ReactionConditionScenarioPlanner scenarioPlanner,
      final OrganicReactionPredictionService predictionService) {
    if (scenarioPlanner == null) {
      throw new IllegalArgumentException("Reaction condition scenario planner must not be null.");
    }
    if (predictionService == null) {
      throw new IllegalArgumentException(
          "Reaction condition scenario prediction service must not be null.");
    }
    this.scenarioPlanner = scenarioPlanner;
    this.predictionService = predictionService;
  }

  public ReactionConditionScenarioEvaluationPlan evaluate(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    return this.evaluate(this.scenarioPlanner.plan(request), request);
  }

  public ReactionConditionScenarioPlan plan(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    return this.scenarioPlanner.plan(request);
  }

  public ReactionConditionScenarioEvaluationPlan evaluate(
      final ReactionConditionScenarioPlan plan, final ReactionPredictionRequest originalRequest) {
    if (plan == null) {
      throw new IllegalArgumentException("Reaction condition scenario plan must not be null.");
    }
    if (originalRequest == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    final List<ReactionConditionScenario> scenarios = plan.scenarios();
    final ArrayList<ScenarioPrediction> predictions =
        new ArrayList<ScenarioPrediction>(scenarios.size());
    for (int i = 0; i < scenarios.size(); ++i) {
      final ReactionConditionScenario scenario = scenarios.get(i);
      predictions.add(
          new ScenarioPrediction(
              scenario,
              this.predictionService.predict(
                  ReactionConditionScenarioEvaluator.withScenarioConditions(
                      originalRequest, scenario))));
    }
    final BaselineMetrics baseline =
        ReactionConditionScenarioEvaluator.baselineMetrics(predictions);
    final ArrayList<ReactionConditionScenarioEvaluation> evaluations =
        new ArrayList<ReactionConditionScenarioEvaluation>(predictions.size());
    for (int i = 0; i < predictions.size(); ++i) {
      final ScenarioPrediction prediction = predictions.get(i);
      evaluations.add(
          ReactionConditionScenarioEvaluation.of(
              prediction.scenario,
              prediction.result,
              baseline.bestProbability,
              baseline.bestExpectedYieldPercent));
    }
    return ReactionConditionScenarioEvaluationPlan.of(plan.requestId(), evaluations);
  }

  private static BaselineMetrics baselineMetrics(final List<ScenarioPrediction> predictions) {
    if (predictions.isEmpty()) {
      return BaselineMetrics.EMPTY;
    }
    ScenarioPrediction selected = predictions.get(0);
    for (int i = 0; i < predictions.size(); ++i) {
      final ScenarioPrediction prediction = predictions.get(i);
      if (prediction.scenario.kind() != ReactionConditionScenarioKind.BASELINE) {
        continue;
      }
      selected = prediction;
      break;
    }
    if (!selected.result.hasCandidates()) {
      return BaselineMetrics.EMPTY;
    }
    return new BaselineMetrics(
        selected.result.bestCandidate().probability(),
        selected.result.bestCandidate().expectedYieldPercent());
  }

  private static ReactionPredictionRequest withScenarioConditions(
      final ReactionPredictionRequest request, final ReactionConditionScenario scenario) {
    return ReactionPredictionRequest.of(
        ReactionConditionScenarioEvaluator.predictionRequestId(request, scenario),
        request.reactants(),
        scenario.conditions(),
        request.domains(),
        request.seedCandidates(),
        request.hasNote() ? request.note() : scenario.rationale());
  }

  private static String predictionRequestId(
      final ReactionPredictionRequest request, final ReactionConditionScenario scenario) {
    return request.id() + "_condition_" + scenario.kind().name().toLowerCase(Locale.ROOT);
  }

  /** Хранит результат одного прогона сценария до вычисления дельт относительно baseline. */
  private static final class ScenarioPrediction {

    private final ReactionConditionScenario scenario;
    private final ReactionPredictionResult result;

    private ScenarioPrediction(
        final ReactionConditionScenario scenario, final ReactionPredictionResult result) {
      this.scenario = scenario;
      this.result = result;
    }
  }

  /** Базовые метрики исходных условий, относительно которых ранжируются альтернативы. */
  private static final class BaselineMetrics {

    private static final BaselineMetrics EMPTY = new BaselineMetrics(0.0, 0.0);
    private final double bestProbability;
    private final double bestExpectedYieldPercent;

    private BaselineMetrics(final double bestProbability, final double bestExpectedYieldPercent) {
      this.bestProbability = bestProbability;
      this.bestExpectedYieldPercent = bestExpectedYieldPercent;
    }
  }
}
