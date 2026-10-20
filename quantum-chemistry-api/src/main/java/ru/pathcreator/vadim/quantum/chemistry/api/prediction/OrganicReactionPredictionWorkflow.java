/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.api.prediction;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionEffectProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlan;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioPlanner;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction.ReactionPredictionExplainer;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic.ChemistryValidationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionExplanation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator;
import ru.pathcreator.vadim.quantum.chemistry.workflow.application.ReactionPredictionFollowUpPlanner;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;

/**
 * Пользовательский workflow прогноза продуктов органической реакции.
 */
public final class OrganicReactionPredictionWorkflow {

  private final ChemistryCoreValidator validator;
  private final ClassicalChemistryService classicalChemistryService;
  private final ReactionPredictionExplainer explainer;
  private final ReactionPredictionFollowUpPlanner followUpPlanner;
  private final ReactionConditionScenarioPlanner conditionScenarioPlanner;

  public OrganicReactionPredictionWorkflow() {
    this(
        new ChemistryCoreValidator(),
        new ClassicalChemistryService(),
        new ReactionPredictionExplainer(),
        new ReactionPredictionFollowUpPlanner(),
        new ReactionConditionScenarioPlanner());
  }

  public OrganicReactionPredictionWorkflow(
      final ChemistryCoreValidator validator,
      final ClassicalChemistryService classicalChemistryService,
      final ReactionPredictionExplainer explainer,
      final ReactionPredictionFollowUpPlanner followUpPlanner,
      final ReactionConditionScenarioPlanner conditionScenarioPlanner) {
    if (validator == null) {
      throw new IllegalArgumentException("Chemistry API validator must not be null.");
    }
    if (classicalChemistryService == null) {
      throw new IllegalArgumentException("Chemistry API classical service must not be null.");
    }
    if (explainer == null) {
      throw new IllegalArgumentException("Chemistry API prediction explainer must not be null.");
    }
    if (followUpPlanner == null) {
      throw new IllegalArgumentException("Chemistry API follow-up planner must not be null.");
    }
    if (conditionScenarioPlanner == null) {
      throw new IllegalArgumentException(
          "Chemistry API condition scenario planner must not be null.");
    }
    this.validator = validator;
    this.classicalChemistryService = classicalChemistryService;
    this.explainer = explainer;
    this.followUpPlanner = followUpPlanner;
    this.conditionScenarioPlanner = conditionScenarioPlanner;
  }

  public OrganicReactionPredictionWorkflowResult predict(final ReactionPredictionRequest request) {
    return this.predict(request, null);
  }

  public OrganicReactionPredictionWorkflowResult predict(
      final ReactionPredictionRequest request,
      final ClassicalCalculationRequest calculationRequest) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    final ChemistryValidationResult validationResult =
        this.validator.validateReactionPredictionRequest(request);
    if (!validationResult.valid()) {
      return OrganicReactionPredictionWorkflowResult.of(
          request,
          OrganicReactionPredictionWorkflowStatus.FAILED,
          validationResult,
          null,
          null,
          ChemistryWorkflowRecommendationPlan.EMPTY);
    }
    try {
      final ArrayList<ClassicalDiagnostic> classicalDiagnostics =
          new ArrayList<ClassicalDiagnostic>();
      final ClassicalCalculationResult classicalCalculationResult =
          calculationRequest == null
              ? null
              : this.classicalChemistryService.execute(calculationRequest);
      final ReactionPredictionResult predictionResult =
          calculationRequest == null
              ? this.classicalChemistryService.predictReactionProducts(request)
              : this.classicalChemistryService.predictReactionProducts(
                  calculationRequest, request, classicalDiagnostics);
      final ReactionPredictionExplanation explanation = this.explainer.explain(predictionResult);
      final ReactionConditionEffectProfile conditionEffectProfile =
          this.classicalChemistryService.analyzeReactionConditionEffects(request);
      final ReactionConditionScenarioPlan conditionScenarioPlan =
          this.conditionScenarioPlanner.plan(request);
      final ReactionConditionScenarioEvaluationPlan conditionScenarioEvaluationPlan =
          this.classicalChemistryService.evaluateReactionConditionScenarios(
              conditionScenarioPlan, request);
      final ChemistryWorkflowRecommendationPlan recommendationPlan =
          this.followUpPlanner.plan(request.id(), predictionResult);
      return OrganicReactionPredictionWorkflowResult.of(
          request,
          OrganicReactionPredictionWorkflow.status(predictionResult, recommendationPlan),
          validationResult,
          predictionResult,
          classicalCalculationResult,
          explanation,
          conditionEffectProfile,
          conditionScenarioPlan,
          conditionScenarioEvaluationPlan,
          recommendationPlan);
    } catch (final RuntimeException exception) {
      return OrganicReactionPredictionWorkflowResult.of(
          request,
          OrganicReactionPredictionWorkflowStatus.FAILED,
          OrganicReactionPredictionWorkflow.failedValidationResult(
              validationResult, request, exception),
          null,
          null,
          ChemistryWorkflowRecommendationPlan.EMPTY);
    }
  }

  private static OrganicReactionPredictionWorkflowStatus status(
      final ReactionPredictionResult predictionResult,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    if (predictionResult == null || !predictionResult.hasCandidates()) {
      return OrganicReactionPredictionWorkflowStatus.PARTIAL;
    }
    if (recommendationPlan == null || recommendationPlan.empty()) {
      return OrganicReactionPredictionWorkflowStatus.PARTIAL;
    }
    return OrganicReactionPredictionWorkflowStatus.SUCCESS;
  }

  private static ChemistryValidationResult failedValidationResult(
      final ChemistryValidationResult validationResult,
      final ReactionPredictionRequest request,
      final RuntimeException exception) {
    final ArrayList<ChemistryDiagnostic> diagnostics =
        new ArrayList<ChemistryDiagnostic>(validationResult.diagnostics());
    diagnostics.add(
        ChemistryDiagnostic.of(
            ChemistryDiagnosticSeverity.ERROR,
            ChemistryDiagnosticCode.REACTION_PREDICTION_HAS_NO_CANDIDATES,
            exception.getMessage() == null ? "Reaction prediction failed." : exception.getMessage(),
            ChemistryDiagnosticTarget.of("reaction_prediction_request", request.id())));
    return ChemistryValidationResult.of(List.copyOf(diagnostics));
  }
}
