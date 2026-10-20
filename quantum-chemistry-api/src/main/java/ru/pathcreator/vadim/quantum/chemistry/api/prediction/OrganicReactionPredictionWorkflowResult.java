/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.api.prediction;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;

/**
 * Полный результат сценария reactants + conditions -> products + explanation + follow-up plan.
 */
public final class OrganicReactionPredictionWorkflowResult {

  private final ReactionPredictionRequest request;
  private final OrganicReactionPredictionWorkflowStatus status;
  private final ChemistryValidationResult validationResult;
  private final ReactionPredictionResult predictionResult;
  private final ClassicalCalculationResult classicalCalculationResult;
  private final ReactionPredictionExplanation explanation;
  private final ReactionConditionEffectProfile conditionEffectProfile;
  private final ReactionConditionScenarioPlan conditionScenarioPlan;
  private final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan;
  private final ChemistryWorkflowRecommendationPlan recommendationPlan;

  private OrganicReactionPredictionWorkflowResult(
      final ReactionPredictionRequest request,
      final OrganicReactionPredictionWorkflowStatus status,
      final ChemistryValidationResult validationResult,
      final ReactionPredictionResult predictionResult,
      final ClassicalCalculationResult classicalCalculationResult,
      final ReactionPredictionExplanation explanation,
      final ReactionConditionEffectProfile conditionEffectProfile,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    this.request = request;
    this.status = status;
    this.validationResult = validationResult;
    this.predictionResult = predictionResult;
    this.classicalCalculationResult = classicalCalculationResult;
    this.explanation = explanation;
    this.conditionEffectProfile = conditionEffectProfile;
    this.conditionScenarioPlan = conditionScenarioPlan;
    this.conditionScenarioEvaluationPlan = conditionScenarioEvaluationPlan;
    this.recommendationPlan = recommendationPlan;
  }

  public static OrganicReactionPredictionWorkflowResult of(
      final ReactionPredictionRequest request,
      final OrganicReactionPredictionWorkflowStatus status,
      final ChemistryValidationResult validationResult,
      final ReactionPredictionResult predictionResult,
      final ClassicalCalculationResult classicalCalculationResult,
      final ReactionPredictionExplanation explanation,
      final ReactionConditionEffectProfile conditionEffectProfile,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    if (request == null) {
      throw new IllegalArgumentException(
          "Organic reaction prediction workflow request must not be null.");
    }
    if (status == null) {
      throw new IllegalArgumentException(
          "Organic reaction prediction workflow status must not be null.");
    }
    if (validationResult == null) {
      throw new IllegalArgumentException(
          "Organic reaction prediction workflow validation result must not be null.");
    }
    if (predictionResult == null && explanation != null) {
      throw new IllegalArgumentException(
          "Organic reaction prediction explanation requires prediction result.");
    }
    return new OrganicReactionPredictionWorkflowResult(
        request,
        status,
        validationResult,
        predictionResult,
        classicalCalculationResult,
        explanation,
        conditionEffectProfile,
        conditionScenarioPlan,
        conditionScenarioEvaluationPlan,
        recommendationPlan == null
            ? ChemistryWorkflowRecommendationPlan.EMPTY
            : recommendationPlan);
  }

  public static OrganicReactionPredictionWorkflowResult of(
      final ReactionPredictionRequest request,
      final OrganicReactionPredictionWorkflowStatus status,
      final ChemistryValidationResult validationResult,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    return OrganicReactionPredictionWorkflowResult.of(
        request,
        status,
        validationResult,
        predictionResult,
        null,
        explanation,
        null,
        null,
        null,
        recommendationPlan);
  }

  public static OrganicReactionPredictionWorkflowResult of(
      final ReactionPredictionRequest request,
      final OrganicReactionPredictionWorkflowStatus status,
      final ChemistryValidationResult validationResult,
      final ReactionPredictionResult predictionResult,
      final ReactionPredictionExplanation explanation,
      final ReactionConditionScenarioPlan conditionScenarioPlan,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    return OrganicReactionPredictionWorkflowResult.of(
        request,
        status,
        validationResult,
        predictionResult,
        null,
        explanation,
        null,
        conditionScenarioPlan,
        null,
        recommendationPlan);
  }

  public ReactionPredictionRequest request() {
    return this.request;
  }

  public OrganicReactionPredictionWorkflowStatus status() {
    return this.status;
  }

  public ChemistryValidationResult validationResult() {
    return this.validationResult;
  }

  public ReactionPredictionResult predictionResult() {
    return this.predictionResult;
  }

  public boolean hasPredictionResult() {
    return this.predictionResult != null;
  }

  public ClassicalCalculationResult classicalCalculationResult() {
    if (this.classicalCalculationResult == null) {
      throw new IllegalStateException(
          "Organic reaction prediction workflow does not contain classical calculation result.");
    }
    return this.classicalCalculationResult;
  }

  public boolean hasClassicalCalculationResult() {
    return this.classicalCalculationResult != null;
  }

  public ReactionPredictionExplanation explanation() {
    if (this.explanation == null) {
      throw new IllegalStateException(
          "Organic reaction prediction workflow does not contain explanation.");
    }
    return this.explanation;
  }

  public boolean hasExplanation() {
    return this.explanation != null;
  }

  public ReactionConditionEffectProfile conditionEffectProfile() {
    return this.conditionEffectProfile;
  }

  public boolean hasConditionEffectProfile() {
    return this.conditionEffectProfile != null && !this.conditionEffectProfile.empty();
  }

  public ReactionConditionScenarioPlan conditionScenarioPlan() {
    return this.conditionScenarioPlan;
  }

  public boolean hasConditionScenarioPlan() {
    return this.conditionScenarioPlan != null && !this.conditionScenarioPlan.empty();
  }

  public ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan() {
    return this.conditionScenarioEvaluationPlan;
  }

  public boolean hasConditionScenarioEvaluationPlan() {
    return this.conditionScenarioEvaluationPlan != null
        && !this.conditionScenarioEvaluationPlan.empty();
  }

  public ChemistryWorkflowRecommendationPlan recommendationPlan() {
    return this.recommendationPlan;
  }

  public boolean hasRecommendations() {
    return !this.recommendationPlan.empty();
  }

  public OrganicReactionComputationRoadmap computationRoadmap() {
    return new OrganicReactionComputationRoadmapBuilder().build(this);
  }

  public OrganicReactionDecisionMatrix decisionMatrix() {
    return new OrganicReactionDecisionMatrixBuilder().build(this);
  }

  public boolean success() {
    return this.status == OrganicReactionPredictionWorkflowStatus.SUCCESS;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrganicReactionPredictionWorkflowResult)) {
      return false;
    }
    final OrganicReactionPredictionWorkflowResult result =
        (OrganicReactionPredictionWorkflowResult) other;
    return Objects.equals(this.request, result.request)
        && this.status == result.status
        && Objects.equals(this.validationResult, result.validationResult)
        && Objects.equals(this.predictionResult, result.predictionResult)
        && Objects.equals(this.classicalCalculationResult, result.classicalCalculationResult)
        && Objects.equals(this.explanation, result.explanation)
        && Objects.equals(this.conditionEffectProfile, result.conditionEffectProfile)
        && Objects.equals(this.conditionScenarioPlan, result.conditionScenarioPlan)
        && Objects.equals(
            this.conditionScenarioEvaluationPlan, result.conditionScenarioEvaluationPlan)
        && Objects.equals(this.recommendationPlan, result.recommendationPlan);
  }

  public int hashCode() {
    return Objects.hash(
        this.request,
        this.status,
        this.validationResult,
        this.predictionResult,
        this.classicalCalculationResult,
        this.explanation,
        this.conditionEffectProfile,
        this.conditionScenarioPlan,
        this.conditionScenarioEvaluationPlan,
        this.recommendationPlan);
  }
}
