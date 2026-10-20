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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.condition.ReactionConditionScenarioEvaluationPlan;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationKind;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;

/** Собирает прикладную матрицу решений из полного prediction workflow. */
public final class OrganicReactionDecisionMatrixBuilder {

  private static final double COMPLETE_PROBABILITY_TOLERANCE = 1.0E-6;

  public OrganicReactionDecisionMatrix build(
      final OrganicReactionPredictionWorkflowResult workflowResult) {
    if (workflowResult == null) {
      throw new IllegalArgumentException(
          "Organic reaction prediction workflow result must not be null.");
    }
    if (!workflowResult.hasPredictionResult()) {
      return OrganicReactionDecisionMatrix.of(
          List.of(),
          OrganicReactionDecisionMatrixBuilder.programRecommendations(
              workflowResult.recommendationPlan(),
              workflowResult.hasConditionScenarioEvaluationPlan()
                  ? workflowResult.conditionScenarioEvaluationPlan()
                  : null),
          0.0,
          false);
    }
    return this.build(
        workflowResult.predictionResult(),
        workflowResult.recommendationPlan(),
        workflowResult.hasConditionScenarioEvaluationPlan()
            ? workflowResult.conditionScenarioEvaluationPlan()
            : null);
  }

  public OrganicReactionDecisionMatrix build(
      final ReactionPredictionResult predictionResult,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    if (predictionResult == null) {
      throw new IllegalArgumentException("Reaction prediction result must not be null.");
    }
    final List<ReactionProductCandidate> candidates = predictionResult.candidates();
    final ArrayList<OrganicReactionDecisionRow> rows =
        new ArrayList<OrganicReactionDecisionRow>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      rows.add(
          OrganicReactionDecisionMatrixBuilder.row(
              i,
              candidates.get(i),
              OrganicReactionDecisionMatrixBuilder.recommendationsForCandidate(
                  recommendationPlan, i)));
    }
    return OrganicReactionDecisionMatrix.of(
        rows,
        OrganicReactionDecisionMatrixBuilder.programRecommendations(recommendationPlan),
        predictionResult.probabilityMass(),
        predictionResult.probabilityMassComplete(COMPLETE_PROBABILITY_TOLERANCE));
  }

  private OrganicReactionDecisionMatrix build(
      final ReactionPredictionResult predictionResult,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ReactionConditionScenarioEvaluationPlan conditionEvaluationPlan) {
    if (predictionResult == null) {
      throw new IllegalArgumentException("Reaction prediction result must not be null.");
    }
    final List<ReactionProductCandidate> candidates = predictionResult.candidates();
    final ArrayList<OrganicReactionDecisionRow> rows =
        new ArrayList<OrganicReactionDecisionRow>(candidates.size());
    for (int i = 0; i < candidates.size(); ++i) {
      rows.add(
          OrganicReactionDecisionMatrixBuilder.row(
              i,
              candidates.get(i),
              OrganicReactionDecisionMatrixBuilder.recommendationsForCandidate(
                  recommendationPlan, i)));
    }
    return OrganicReactionDecisionMatrix.of(
        rows,
        OrganicReactionDecisionMatrixBuilder.programRecommendations(
            recommendationPlan, conditionEvaluationPlan),
        predictionResult.probabilityMass(),
        predictionResult.probabilityMassComplete(COMPLETE_PROBABILITY_TOLERANCE));
  }

  private static OrganicReactionDecisionRow row(
      final int candidateIndex,
      final ReactionProductCandidate candidate,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    return OrganicReactionDecisionRow.of(
        candidateIndex,
        OrganicReactionDecisionMatrixBuilder.productNames(candidate.products()),
        candidate.probability(),
        candidate.expectedYieldPercent(),
        OrganicReactionDecisionMatrixBuilder.mechanismFamilies(candidate.predictedMechanisms()),
        OrganicReactionDecisionMatrixBuilder.mechanismTrace(candidate.predictedMechanisms()),
        OrganicReactionDecisionMatrixBuilder.evidence(candidate),
        OrganicReactionDecisionMatrixBuilder.recommendedCalculations(recommendations),
        OrganicReactionDecisionMatrixBuilder.quantumTargets(recommendations),
        OrganicReactionDecisionMatrixBuilder.confidence(candidate),
        OrganicReactionDecisionMatrixBuilder.highRisk(candidate),
        OrganicReactionDecisionMatrixBuilder.hasRecommendationKind(
            recommendations, ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION),
        OrganicReactionDecisionMatrixBuilder.hasClassicalRecommendation(recommendations));
  }

  private static List<String> productNames(final ReactionSide products) {
    final List<ReactionParticipant> participants = products.participants();
    final ArrayList<String> names = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      names.add(participants.get(i).molecule().displayName());
    }
    if (names.isEmpty()) {
      names.add("unknown product set");
    }
    return names;
  }

  private static List<String> mechanismFamilies(final List<PredictedReactionMechanism> mechanisms) {
    if (mechanisms.isEmpty()) {
      return List.of("UNKNOWN");
    }
    final ArrayList<String> families = new ArrayList<String>(mechanisms.size());
    for (int i = 0; i < mechanisms.size(); ++i) {
      families.add(mechanisms.get(i).family().name());
    }
    return families;
  }

  private static List<String> mechanismTrace(final List<PredictedReactionMechanism> mechanisms) {
    if (mechanisms.isEmpty()) {
      return List.of("No mechanism assigned.");
    }
    final ArrayList<String> trace = new ArrayList<String>();
    for (int i = 0; i < mechanisms.size(); ++i) {
      final PredictedReactionMechanism mechanism = mechanisms.get(i);
      trace.add(
          mechanism.mechanismId()
              + " "
              + mechanism.family().name()
              + " p="
              + mechanism.pathwayProbability()
              + " yield="
              + mechanism.expectedYieldPercent());
      if (mechanism.hasDetailedMechanism()) {
        OrganicReactionDecisionMatrixBuilder.addDetailedMechanismTrace(
            trace, mechanism.detailedMechanism());
      }
    }
    return trace;
  }

  private static void addDetailedMechanismTrace(
      final List<String> trace, final ReactionMechanism mechanism) {
    final List<ElementaryReactionStep> steps = mechanism.steps();
    for (int i = 0; i < steps.size(); ++i) {
      final ElementaryReactionStep step = steps.get(i);
      trace.add(
          "step "
              + i
              + " "
              + step.id()
              + " forward_barrier="
              + OrganicReactionDecisionMatrixBuilder.energyText(step.forwardBarrier())
              + " reaction_energy="
              + OrganicReactionDecisionMatrixBuilder.energyText(step.reactionEnergy()));
      OrganicReactionDecisionMatrixBuilder.addEventTrace(trace, step.events());
    }
  }

  private static void addEventTrace(final List<String> trace, final List<MechanisticEvent> events) {
    for (int i = 0; i < events.size(); ++i) {
      final MechanisticEvent event = events.get(i);
      trace.add(
          "event "
              + event.orderIndex()
              + " "
              + event.kind().name()
              + " p="
              + event.probabilityWeight()
              + " "
              + event.description());
    }
  }

  private static String energyText(final EnergyValue value) {
    if (value == null) {
      return "unknown";
    }
    return Double.toString(value.value()) + " " + value.unit().name();
  }

  private static List<String> evidence(final ReactionProductCandidate candidate) {
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    if (evidence.isEmpty()) {
      return List.of("No evidence attached.");
    }
    final ArrayList<String> rows = new ArrayList<String>(evidence.size());
    for (int i = 0; i < evidence.size(); ++i) {
      final ReactionPredictionEvidence item = evidence.get(i);
      rows.add(item.kind().name() + " weight=" + item.weight() + " " + item.description());
    }
    return rows;
  }

  private static List<ChemistryWorkflowRecommendation> recommendationsForCandidate(
      final ChemistryWorkflowRecommendationPlan plan, final int candidateIndex) {
    if (plan == null) {
      return List.of();
    }
    return plan.recommendationsForCandidate(candidateIndex);
  }

  private static List<String> recommendedCalculations(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (recommendations.isEmpty()) {
      return List.of("No follow-up calculation required.");
    }
    final ArrayList<String> rows = new ArrayList<String>(recommendations.size());
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      rows.add(
          recommendation.priority().name()
              + " "
              + recommendation.kind().name()
              + " "
              + (recommendation.hasCalculationKind()
                  ? recommendation.calculationKind().name()
                  : "NO_CALCULATION_KIND")
              + " "
              + recommendation.reason());
    }
    return rows;
  }

  private static List<String> quantumTargets(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final ArrayList<String> targets = new ArrayList<String>();
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (recommendation.kind() == ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION) {
        targets.add(recommendation.target());
      }
    }
    if (targets.isEmpty()) {
      return List.of("No quantum target required.");
    }
    return targets;
  }

  private static double confidence(final ReactionProductCandidate candidate) {
    double score = candidate.probability();
    if (candidate.hasPredictedMechanisms()) {
      score += 0.15;
    }
    if (candidate.hasScoreBreakdown()) {
      score += 0.1;
    }
    if (candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.KINETIC_FACTOR)
        || candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR)
        || candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR)) {
      score += 0.1;
    }
    if (candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.SOLVENT_EFFECT)
        || candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT)
        || candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.CATALYST_EFFECT)) {
      score += 0.1;
    }
    if (OrganicReactionDecisionMatrixBuilder.highRisk(candidate)) {
      score -= 0.25;
    }
    return Math.max(0.0, Math.min(1.0, score));
  }

  private static boolean highRisk(final ReactionProductCandidate candidate) {
    return candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.WARNING)
        || candidate.hasWeightedEvidence(ReactionPredictionEvidenceKind.COMPETING_PATHWAY)
        || candidate.probability() < 0.05
        || !candidate.hasPredictedMechanisms();
  }

  private static boolean hasRecommendationKind(
      final List<ChemistryWorkflowRecommendation> recommendations,
      final ChemistryWorkflowRecommendationKind kind) {
    for (int i = 0; i < recommendations.size(); ++i) {
      if (recommendations.get(i).kind() == kind) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasClassicalRecommendation(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    for (int i = 0; i < recommendations.size(); ++i) {
      if (recommendations.get(i).hasCalculationKind()) {
        return true;
      }
    }
    return false;
  }

  private static List<String> programRecommendations(
      final ChemistryWorkflowRecommendationPlan plan) {
    return OrganicReactionDecisionMatrixBuilder.programRecommendations(plan, null);
  }

  private static List<String> programRecommendations(
      final ChemistryWorkflowRecommendationPlan plan,
      final ReactionConditionScenarioEvaluationPlan conditionEvaluationPlan) {
    final ArrayList<String> rows = new ArrayList<String>();
    OrganicReactionDecisionMatrixBuilder.addConditionScenarioRecommendations(
        rows, conditionEvaluationPlan);
    if (plan == null || plan.empty()) {
      if (rows.isEmpty()) {
        rows.add("No program-level follow-up recommendation.");
      }
      return List.copyOf(rows);
    }
    final List<ChemistryWorkflowRecommendation> recommendations =
        plan.programLevelRecommendations();
    if (recommendations.isEmpty()) {
      if (rows.isEmpty()) {
        rows.add("No program-level follow-up recommendation.");
      }
      return List.copyOf(rows);
    }
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      rows.add(
          recommendation.priority().name()
              + " "
              + recommendation.kind().name()
              + " "
              + recommendation.reason());
    }
    return List.copyOf(rows);
  }

  private static void addConditionScenarioRecommendations(
      final List<String> rows,
      final ReactionConditionScenarioEvaluationPlan conditionEvaluationPlan) {
    if (conditionEvaluationPlan == null || conditionEvaluationPlan.empty()) {
      return;
    }
    final ReactionConditionScenarioEvaluation best = conditionEvaluationPlan.bestEvaluation();
    rows.add(
        "CONDITION_SCREENING best="
            + best.scenario().kind().name()
            + " scenario="
            + best.scenario().id()
            + " probability="
            + best.bestProbability()
            + " yield="
            + best.bestExpectedYieldPercent()
            + " score="
            + best.rankingScore());
    rows.add(
        "CONDITION_SCREENING improving="
            + conditionEvaluationPlan.improvingEvaluations().size()
            + " worsening="
            + conditionEvaluationPlan.worseningEvaluations().size()
            + " evaluated="
            + conditionEvaluationPlan.evaluations().size());
  }
}
