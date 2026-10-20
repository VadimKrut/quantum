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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.PredictedReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationKind;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;

/**
 * Собирает расчетную дорожную карту из результата прогноза без запуска внешних backend.
 */
public final class OrganicReactionComputationRoadmapBuilder {

  public OrganicReactionComputationRoadmap build(
      final OrganicReactionPredictionWorkflowResult result) {
    if (result == null) {
      throw new IllegalArgumentException(
          "Organic reaction prediction workflow result must not be null.");
    }
    final ChemistryWorkflowRecommendationPlan recommendationPlan = result.recommendationPlan();
    final List<ChemistryWorkflowRecommendation> recommendations =
        recommendationPlan.recommendations();
    final ArrayList<OrganicReactionCandidateRoadmap> candidates =
        new ArrayList<OrganicReactionCandidateRoadmap>();
    if (result.hasPredictionResult()) {
      final List<ReactionProductCandidate> productCandidates =
          result.predictionResult().candidates();
      for (int candidateIndex = 0; candidateIndex < productCandidates.size(); ++candidateIndex) {
        candidates.add(
            OrganicReactionComputationRoadmapBuilder.candidateRoadmap(
                candidateIndex, productCandidates.get(candidateIndex), recommendations));
      }
    }
    return OrganicReactionComputationRoadmap.of(
        result.request().id(),
        result.status(),
        result.hasConditionScenarioPlan() ? result.conditionScenarioPlan().scenarios().size() : 0,
        result.hasConditionScenarioEvaluationPlan()
            ? result.conditionScenarioEvaluationPlan().evaluations().size()
            : 0,
        candidates,
        OrganicReactionComputationRoadmapBuilder.programLevelRecommendations(recommendations));
  }

  private static OrganicReactionCandidateRoadmap candidateRoadmap(
      final int candidateIndex,
      final ReactionProductCandidate candidate,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final List<ChemistryWorkflowRecommendation> candidateRecommendations =
        OrganicReactionComputationRoadmapBuilder.candidateRecommendations(
            candidateIndex, recommendations);
    return OrganicReactionCandidateRoadmap.of(
        candidateIndex,
        candidate.probability(),
        candidate.expectedYieldPercent(),
        OrganicReactionComputationRoadmapBuilder.productMoleculeIds(candidate),
        OrganicReactionComputationRoadmapBuilder.productFormulaSignatures(candidate),
        OrganicReactionComputationRoadmapBuilder.productStoichiometrySignatures(candidate),
        OrganicReactionComputationRoadmapBuilder.mechanismIds(candidate),
        OrganicReactionComputationRoadmapBuilder.mechanismDescriptors(candidate),
        OrganicReactionComputationRoadmapBuilder.classicalCalculationKinds(
            candidateRecommendations),
        OrganicReactionComputationRoadmapBuilder.quantumTargets(candidateRecommendations),
        OrganicReactionComputationRoadmapBuilder.explicitProductGraph(candidate),
        OrganicReactionComputationRoadmapBuilder.electronicRerankReady(candidate),
        OrganicReactionComputationRoadmapBuilder.quantumWorkflowReady(candidate),
        OrganicReactionComputationRoadmapBuilder.readinessReason(candidate),
        candidateRecommendations);
  }

  private static boolean explicitProductGraph(final ReactionProductCandidate candidate) {
    return !candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)
        && candidate.products().participantCount() > 0;
  }

  private static boolean electronicRerankReady(final ReactionProductCandidate candidate) {
    return OrganicReactionComputationRoadmapBuilder.explicitProductGraph(candidate)
        && candidate.products().participantCount() > 0;
  }

  private static boolean quantumWorkflowReady(final ReactionProductCandidate candidate) {
    return OrganicReactionComputationRoadmapBuilder.electronicRerankReady(candidate)
        && candidate.hasPredictedMechanisms()
        && candidate.hasScoreBreakdown();
  }

  private static String readinessReason(final ReactionProductCandidate candidate) {
    if (!OrganicReactionComputationRoadmapBuilder.explicitProductGraph(candidate)) {
      return "mechanism-only product channel has no explicit product graph";
    }
    if (!candidate.hasPredictedMechanisms()) {
      return "candidate has no traced mechanism";
    }
    if (!candidate.hasScoreBreakdown()) {
      return "candidate has no score breakdown";
    }
    return "ready";
  }

  private static List<String> productMoleculeIds(final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    final ArrayList<String> ids = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      ids.add(participants.get(i).molecule().id().value());
    }
    return List.copyOf(ids);
  }

  private static List<String> productFormulaSignatures(final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    if (participants.isEmpty()) {
      return List.of();
    }
    final ArrayList<String> formulas = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      formulas.add(participants.get(i).molecule().formula().hillNotation());
    }
    return List.copyOf(formulas);
  }

  private static List<String> productStoichiometrySignatures(
      final ReactionProductCandidate candidate) {
    final List<ReactionParticipant> participants = candidate.products().participants();
    if (participants.isEmpty()) {
      return List.of();
    }
    final ArrayList<String> signatures = new ArrayList<String>(participants.size());
    for (int i = 0; i < participants.size(); ++i) {
      final ReactionParticipant participant = participants.get(i);
      final String formula = participant.molecule().formula().hillNotation();
      signatures.add(
          participant.coefficient().value()
              + "*"
              + participant.molecule().id().value()
              + ":"
              + formula);
    }
    return List.copyOf(signatures);
  }

  private static List<String> mechanismIds(final ReactionProductCandidate candidate) {
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    if (mechanisms.isEmpty()) {
      return List.of();
    }
    final ArrayList<String> ids = new ArrayList<String>(mechanisms.size());
    for (int i = 0; i < mechanisms.size(); ++i) {
      ids.add(mechanisms.get(i).mechanismId());
    }
    return List.copyOf(ids);
  }

  private static List<String> mechanismDescriptors(final ReactionProductCandidate candidate) {
    final List<PredictedReactionMechanism> mechanisms = candidate.predictedMechanisms();
    if (mechanisms.isEmpty()) {
      return List.of();
    }
    final ArrayList<String> descriptors = new ArrayList<String>(mechanisms.size());
    for (int i = 0; i < mechanisms.size(); ++i) {
      descriptors.add(
          OrganicReactionComputationRoadmapBuilder.mechanismDescriptor(mechanisms.get(i)));
    }
    return List.copyOf(descriptors);
  }

  private static String mechanismDescriptor(final PredictedReactionMechanism mechanism) {
    final StringBuilder builder =
        new StringBuilder(mechanism.mechanismId())
            .append(":")
            .append(mechanism.family().name())
            .append(":p=")
            .append(mechanism.pathwayProbability())
            .append(":yield=")
            .append(mechanism.expectedYieldPercent());
    if (!mechanism.hasDetailedMechanism()) {
      return builder.toString();
    }
    final ReactionMechanism detailed = mechanism.detailedMechanism();
    builder
        .append(":steps=")
        .append(detailed.steps().size())
        .append(":events=")
        .append(detailed.eventCount())
        .append(":energyPoints=")
        .append(detailed.energyProfile().size())
        .append(":trace=")
        .append(OrganicReactionComputationRoadmapBuilder.mechanismTrace(detailed));
    return builder.toString();
  }

  private static String mechanismTrace(final ReactionMechanism mechanism) {
    final StringBuilder builder = new StringBuilder();
    final List<ElementaryReactionStep> steps = mechanism.steps();
    for (int stepIndex = 0; stepIndex < steps.size(); ++stepIndex) {
      if (stepIndex > 0) {
        builder.append(">");
      }
      final ElementaryReactionStep step = steps.get(stepIndex);
      builder.append(step.id());
      final List<MechanisticEvent> events = step.events();
      if (events.isEmpty()) {
        continue;
      }
      builder.append("[");
      for (int eventIndex = 0; eventIndex < events.size(); ++eventIndex) {
        if (eventIndex > 0) {
          builder.append(",");
        }
        builder.append(events.get(eventIndex).kind().name());
      }
      builder.append("]");
    }
    return builder.toString();
  }

  private static List<ChemistryWorkflowRecommendation> candidateRecommendations(
      final int candidateIndex, final List<ChemistryWorkflowRecommendation> recommendations) {
    if (recommendations.isEmpty()) {
      return List.of();
    }
    final ArrayList<ChemistryWorkflowRecommendation> result =
        new ArrayList<ChemistryWorkflowRecommendation>();
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (recommendation.appliesToCandidate()
          && recommendation.candidateIndex() == candidateIndex) {
        result.add(recommendation);
      }
    }
    return List.copyOf(result);
  }

  private static List<ChemistryWorkflowRecommendation> programLevelRecommendations(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (recommendations.isEmpty()) {
      return List.of();
    }
    final ArrayList<ChemistryWorkflowRecommendation> result =
        new ArrayList<ChemistryWorkflowRecommendation>();
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (!recommendation.appliesToCandidate()) {
        result.add(recommendation);
      }
    }
    return List.copyOf(result);
  }

  private static List<ClassicalCalculationKind> classicalCalculationKinds(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (recommendations.isEmpty()) {
      return List.of();
    }
    final ArrayList<ClassicalCalculationKind> kinds = new ArrayList<ClassicalCalculationKind>();
    for (int i = 0; i < recommendations.size(); ++i) {
      final ClassicalCalculationKind kind = recommendations.get(i).calculationKind();
      if (kind == null || kinds.contains(kind)) {
        continue;
      }
      kinds.add(kind);
    }
    return List.copyOf(kinds);
  }

  private static List<String> quantumTargets(
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (recommendations.isEmpty()) {
      return List.of();
    }
    final ArrayList<String> targets = new ArrayList<String>();
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (recommendation.kind() != ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION) {
        continue;
      }
      if (targets.contains(recommendation.target())) {
        continue;
      }
      targets.add(recommendation.target());
    }
    return List.copyOf(targets);
  }
}
