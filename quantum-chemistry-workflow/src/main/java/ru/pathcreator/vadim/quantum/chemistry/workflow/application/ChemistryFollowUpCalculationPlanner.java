/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.followup.ChemistryFollowUpCalculationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPriority;

/**
 * Превращает рекомендации workflow в конкретные расчетные запросы.
 */
public final class ChemistryFollowUpCalculationPlanner {

  private static final String OPTION_CANDIDATE_INDEX = "workflow_candidate_index";
  private static final String OPTION_RECOMMENDATION_INDEX = "workflow_recommendation_index";
  private static final String OPTION_RECOMMENDATION_KIND = "workflow_recommendation_kind";
  private static final String OPTION_RECOMMENDATION_PRIORITY = "workflow_recommendation_priority";
  private static final String OPTION_RECOMMENDATION_TARGET = "workflow_recommendation_target";
  private static final String OPTION_RECOMMENDATION_REASON = "workflow_recommendation_reason";
  private static final String OPTION_SUBJECT_SOURCE = "workflow_subject_source";
  private static final String OPTION_PRODUCT_INDEX = "workflow_product_index";
  private static final String OPTION_REACTION_ENERGY_MODEL = "reaction_energy_model";
  private static final String OPTION_REACTION_PATH_ENERGY_SOURCE = "reaction_path_energy_source";

  public ChemistryFollowUpCalculationPlan planHighPriority(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    return this.plan(job, recommendationPlan, ChemistryWorkflowRecommendationPriority.HIGH);
  }

  public ChemistryFollowUpCalculationPlan planHighPriority(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ReactionPredictionResult predictionResult) {
    return this.plan(
        job, recommendationPlan, ChemistryWorkflowRecommendationPriority.HIGH, predictionResult);
  }

  public ChemistryFollowUpCalculationPlan planAll(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan) {
    return this.plan(job, recommendationPlan, ChemistryWorkflowRecommendationPriority.LOW);
  }

  public ChemistryFollowUpCalculationPlan planAll(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ReactionPredictionResult predictionResult) {
    return this.plan(
        job, recommendationPlan, ChemistryWorkflowRecommendationPriority.LOW, predictionResult);
  }

  public ChemistryFollowUpCalculationPlan planAll(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ReactionPredictionResult predictionResult,
      final int maxRequestCount) {
    return this.plan(
        job,
        recommendationPlan,
        ChemistryWorkflowRecommendationPriority.LOW,
        predictionResult,
        maxRequestCount);
  }

  public ChemistryFollowUpCalculationPlan plan(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ChemistryWorkflowRecommendationPriority minimumPriority) {
    return this.plan(job, recommendationPlan, minimumPriority, null);
  }

  public ChemistryFollowUpCalculationPlan plan(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ChemistryWorkflowRecommendationPriority minimumPriority,
      final ReactionPredictionResult predictionResult) {
    return this.plan(job, recommendationPlan, minimumPriority, predictionResult, Integer.MAX_VALUE);
  }

  public ChemistryFollowUpCalculationPlan plan(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendationPlan recommendationPlan,
      final ChemistryWorkflowRecommendationPriority minimumPriority,
      final ReactionPredictionResult predictionResult,
      final int maxRequestCount) {
    if (job == null) {
      throw new IllegalArgumentException("Chemistry follow-up workflow job must not be null.");
    }
    if (recommendationPlan == null || recommendationPlan.empty()) {
      return ChemistryFollowUpCalculationPlan.EMPTY;
    }
    if (minimumPriority == null) {
      throw new IllegalArgumentException("Chemistry follow-up minimum priority must not be null.");
    }
    if (job.classicalRequest() == null) {
      return ChemistryFollowUpCalculationPlan.EMPTY;
    }
    if (maxRequestCount < 0) {
      throw new IllegalArgumentException("Chemistry follow-up request limit must be non-negative.");
    }
    if (maxRequestCount == 0) {
      return ChemistryFollowUpCalculationPlan.EMPTY;
    }
    final List<ChemistryWorkflowRecommendation> recommendations =
        recommendationPlan.recommendations();
    final ArrayList<ClassicalCalculationRequest> requests =
        new ArrayList<ClassicalCalculationRequest>();
    final HashSet<String> requestKeys = new HashSet<String>();
    for (int i = 0; i < recommendations.size(); ++i) {
      final ChemistryWorkflowRecommendation recommendation = recommendations.get(i);
      if (!recommendation.hasCalculationKind()
          || !ChemistryFollowUpCalculationPlanner.acceptsPriority(
              recommendation.priority(), minimumPriority)) {
        continue;
      }
      final String key =
          recommendation.candidateIndex()
              + ":"
              + recommendation.calculationKind().name()
              + ":"
              + recommendation.target();
      if (!requestKeys.add(key)) {
        continue;
      }
      final List<ClassicalCalculationRequest> generatedRequests =
          this.requests(job, recommendation, i, predictionResult);
      for (int requestIndex = 0; requestIndex < generatedRequests.size(); ++requestIndex) {
        requests.add(generatedRequests.get(requestIndex));
        if (requests.size() >= maxRequestCount) {
          break;
        }
      }
      if (requests.size() >= maxRequestCount) {
        break;
      }
    }
    return ChemistryFollowUpCalculationPlan.of(job.id().value(), requests);
  }

  private List<ClassicalCalculationRequest> requests(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendation recommendation,
      final int recommendationIndex,
      final ReactionPredictionResult predictionResult) {
    final int productCount =
        ChemistryFollowUpCalculationPlanner.candidateMoleculeProductCount(
            recommendation, predictionResult);
    if (productCount <= 1) {
      return List.of(this.request(job, recommendation, recommendationIndex, predictionResult, -1));
    }
    final ArrayList<ClassicalCalculationRequest> requests =
        new ArrayList<ClassicalCalculationRequest>(productCount);
    for (int productIndex = 0; productIndex < productCount; ++productIndex) {
      requests.add(
          this.request(job, recommendation, recommendationIndex, predictionResult, productIndex));
    }
    return List.copyOf(requests);
  }

  private ClassicalCalculationRequest request(
      final ChemistryWorkflowJob job,
      final ChemistryWorkflowRecommendation recommendation,
      final int recommendationIndex,
      final ReactionPredictionResult predictionResult,
      final int productIndex) {
    final ClassicalCalculationRequest base = job.classicalRequest();
    final ClassicalCalculationKind kind = recommendation.calculationKind();
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(
            ChemistryFollowUpCalculationPlanner.safeId(
                job.id().value()
                    + "_followup_"
                    + ChemistryFollowUpCalculationPlanner.requestScope(recommendation)
                    + ChemistryFollowUpCalculationPlanner.productScope(productIndex)
                    + "_"
                    + recommendationIndex
                    + "_"
                    + kind.name().toLowerCase(Locale.ROOT))),
        ChemistryFollowUpCalculationPlanner.subject(
            base.subject(), recommendation, predictionResult, productIndex),
        base.method(),
        base.basisSet(),
        base.activeSpace(),
        List.of(kind),
        ChemistryFollowUpCalculationPlanner.options(
            base.options(), recommendation, recommendationIndex, predictionResult, productIndex),
        base.metadata());
  }

  private static List<ClassicalCalculationOption> options(
      final List<ClassicalCalculationOption> baseOptions,
      final ChemistryWorkflowRecommendation recommendation,
      final int recommendationIndex,
      final ReactionPredictionResult predictionResult,
      final int productIndex) {
    final ArrayList<ClassicalCalculationOption> options =
        new ArrayList<ClassicalCalculationOption>(baseOptions.size() + 8);
    options.addAll(baseOptions);
    ChemistryFollowUpCalculationPlanner.addOrReplace(
        options,
        ClassicalCalculationOption.integer(OPTION_RECOMMENDATION_INDEX, recommendationIndex));
    ChemistryFollowUpCalculationPlanner.addOrReplace(
        options,
        ClassicalCalculationOption.text(OPTION_RECOMMENDATION_KIND, recommendation.kind().name()));
    ChemistryFollowUpCalculationPlanner.addOrReplace(
        options,
        ClassicalCalculationOption.text(
            OPTION_RECOMMENDATION_PRIORITY, recommendation.priority().name()));
    ChemistryFollowUpCalculationPlanner.addOrReplace(
        options,
        ClassicalCalculationOption.text(OPTION_RECOMMENDATION_TARGET, recommendation.target()));
    ChemistryFollowUpCalculationPlanner.addOrReplace(
        options,
        ClassicalCalculationOption.text(OPTION_RECOMMENDATION_REASON, recommendation.reason()));
    if (recommendation.appliesToCandidate()) {
      ChemistryFollowUpCalculationPlanner.addOrReplace(
          options,
          ClassicalCalculationOption.integer(
              OPTION_CANDIDATE_INDEX, recommendation.candidateIndex()));
      if (productIndex >= 0) {
        ChemistryFollowUpCalculationPlanner.addOrReplace(
            options, ClassicalCalculationOption.integer(OPTION_PRODUCT_INDEX, productIndex));
      }
      ChemistryFollowUpCalculationPlanner.addOrReplace(
          options,
          ClassicalCalculationOption.text(
              OPTION_SUBJECT_SOURCE,
              ChemistryFollowUpCalculationPlanner.subjectSource(
                  recommendation, predictionResult, productIndex)));
    } else {
      ChemistryFollowUpCalculationPlanner.addOrReplace(
          options, ClassicalCalculationOption.text(OPTION_SUBJECT_SOURCE, "workflow_subject"));
    }
    if (recommendation.calculationKind() == ClassicalCalculationKind.REACTION_ENERGY) {
      ChemistryFollowUpCalculationPlanner.addOrReplace(
          options,
          ClassicalCalculationOption.text(OPTION_REACTION_ENERGY_MODEL, "first_principles"));
    }
    if (ChemistryFollowUpCalculationPlanner.electronicReactionPathCalculation(
        recommendation.calculationKind())) {
      ChemistryFollowUpCalculationPlanner.addOrReplace(
          options,
          ClassicalCalculationOption.text(OPTION_REACTION_PATH_ENERGY_SOURCE, "electronic"));
    }
    return List.copyOf(options);
  }

  private static String subjectSource(
      final ChemistryWorkflowRecommendation recommendation,
      final ReactionPredictionResult predictionResult,
      final int productIndex) {
    if (predictionResult == null
        || recommendation.candidateIndex() < 0
        || recommendation.candidateIndex() >= predictionResult.candidates().size()) {
      return "workflow_subject";
    }
    final ReactionProductCandidate candidate =
        predictionResult.candidates().get(recommendation.candidateIndex());
    if (productIndex >= 0
        && ChemistryFollowUpCalculationPlanner.candidateMoleculeCalculation(
            recommendation.calculationKind())
        && productIndex < candidate.products().participantCount()) {
      return "candidate_product_molecule";
    }
    if (!ChemistryFollowUpCalculationPlanner.candidateMoleculeCalculation(
            recommendation.calculationKind())
        || candidate.products().participantCount() != 1) {
      return "candidate_reaction_context";
    }
    return "candidate_product_molecule";
  }

  private static void addOrReplace(
      final ArrayList<ClassicalCalculationOption> options,
      final ClassicalCalculationOption option) {
    for (int i = 0; i < options.size(); ++i) {
      if (!options.get(i).name().equals(option.name())) {
        continue;
      }
      options.set(i, option);
      return;
    }
    options.add(option);
  }

  private static ChemistrySubject subject(
      final ChemistrySubject baseSubject,
      final ChemistryWorkflowRecommendation recommendation,
      final ReactionPredictionResult predictionResult,
      final int productIndex) {
    if (predictionResult == null || !recommendation.appliesToCandidate()) {
      return baseSubject;
    }
    final int candidateIndex = recommendation.candidateIndex();
    if (candidateIndex < 0 || candidateIndex >= predictionResult.candidates().size()) {
      return baseSubject;
    }
    final ReactionProductCandidate candidate = predictionResult.candidates().get(candidateIndex);
    if (ChemistryFollowUpCalculationPlanner.candidateMoleculeCalculation(
        recommendation.calculationKind())) {
      if (productIndex >= 0) {
        if (productIndex >= candidate.products().participantCount()) {
          return baseSubject;
        }
        final ReactionParticipant participant =
            candidate.products().participants().get(productIndex);
        return participant.molecule();
      }
      if (candidate.products().participantCount() == 1) {
        final ReactionParticipant participant = candidate.products().participants().get(0);
        return participant.molecule();
      }
      return baseSubject;
    }
    if (ChemistryFollowUpCalculationPlanner.candidateReactionCalculation(
            recommendation.calculationKind())
        && baseSubject.subjectKind()
            == ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind
                .REACTION) {
      return ChemistryFollowUpCalculationPlanner.candidateReactionContext(
          baseSubject, candidate, candidateIndex);
    }
    return baseSubject;
  }

  private static int candidateMoleculeProductCount(
      final ChemistryWorkflowRecommendation recommendation,
      final ReactionPredictionResult predictionResult) {
    if (predictionResult == null
        || !recommendation.appliesToCandidate()
        || !ChemistryFollowUpCalculationPlanner.candidateMoleculeCalculation(
            recommendation.calculationKind())) {
      return 0;
    }
    final int candidateIndex = recommendation.candidateIndex();
    if (candidateIndex < 0 || candidateIndex >= predictionResult.candidates().size()) {
      return 0;
    }
    return predictionResult.candidates().get(candidateIndex).products().participantCount();
  }

  private static String productScope(final int productIndex) {
    if (productIndex < 0) {
      return "";
    }
    return "_product_" + productIndex;
  }

  private static ChemistrySubject candidateReactionContext(
      final ChemistrySubject baseSubject,
      final ReactionProductCandidate candidate,
      final int candidateIndex) {
    if (baseSubject.subjectKind()
        != ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind.REACTION) {
      return baseSubject;
    }
    final Reaction baseReaction = (Reaction) baseSubject;
    return Reaction.of(
        ReactionId.of(
            ChemistryFollowUpCalculationPlanner.safeId(
                baseReaction.id().value() + "_candidate_" + candidateIndex)),
        baseReaction.displayName() + " candidate " + candidateIndex,
        baseReaction.reactants(),
        candidate.products(),
        baseReaction.conditions(),
        baseReaction.metadata());
  }

  private static boolean candidateMoleculeCalculation(final ClassicalCalculationKind kind) {
    return kind == ClassicalCalculationKind.CONFORMER_SEARCH
        || kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
        || kind == ClassicalCalculationKind.GEOMETRY_GRADIENT
        || kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
        || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
        || kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS
        || kind == ClassicalCalculationKind.SPECTROSCOPY_PREDICTION
        || kind == ClassicalCalculationKind.NMR_PREDICTION
        || kind == ClassicalCalculationKind.IR_RAMAN_PREDICTION
        || kind == ClassicalCalculationKind.MASS_SPECTROMETRY_PREDICTION
        || kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS
        || kind == ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS
        || kind == ClassicalCalculationKind.DESCRIPTOR_ANALYSIS
        || kind == ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS
        || kind == ClassicalCalculationKind.ENANTIOMER_DIASTEREOMER_ANALYSIS
        || kind == ClassicalCalculationKind.SOLVATION_MODELING
        || kind == ClassicalCalculationKind.PKA_PREDICTION
        || kind == ClassicalCalculationKind.REDOX_POTENTIAL
        || kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
        || kind == ClassicalCalculationKind.METHOD_BENCHMARK
        || kind == ClassicalCalculationKind.UNCERTAINTY_ANALYSIS
        || kind == ClassicalCalculationKind.ACTIVE_SPACE_SELECTION
        || kind == ClassicalCalculationKind.ORBITAL_OPTIMIZATION
        || kind == ClassicalCalculationKind.MP2_CORRELATION
        || kind == ClassicalCalculationKind.CISD_CORRELATION
        || kind == ClassicalCalculationKind.CCSD_CORRELATION
        || kind == ClassicalCalculationKind.FCI_DIAGONALIZATION
        || kind == ClassicalCalculationKind.POST_HF_CORRELATION
        || kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
        || kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION
        || kind == ClassicalCalculationKind.RELATIVISTIC_CORRECTION
        || kind == ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS;
  }

  private static boolean candidateReactionCalculation(final ClassicalCalculationKind kind) {
    return kind == ClassicalCalculationKind.REACTION_ENERGY
        || kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS
        || kind == ClassicalCalculationKind.KINETIC_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.IRC_ANALYSIS
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH
        || kind == ClassicalCalculationKind.SELECTIVITY_ANALYSIS
        || kind == ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_CONDITION_SCREENING
        || kind == ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION
        || kind == ClassicalCalculationKind.SOLVENT_SCREENING
        || kind == ClassicalCalculationKind.CATALYST_SCREENING
        || kind == ClassicalCalculationKind.CONCENTRATION_SCREENING
        || kind == ClassicalCalculationKind.MECHANISM_ENUMERATION
        || kind == ClassicalCalculationKind.REACTION_HAZARD_AND_STABILITY_ANALYSIS
        || kind == ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS
        || kind == ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION
        || kind == ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS
        || kind == ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS
        || kind == ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS
        || kind == ClassicalCalculationKind.MICROKINETIC_MODELING
        || kind == ClassicalCalculationKind.RETROSYNTHESIS_PLANNING
        || kind == ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS;
  }

  private static boolean electronicReactionPathCalculation(final ClassicalCalculationKind kind) {
    return kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.IRC_ANALYSIS
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH
        || kind == ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS;
  }

  private static boolean acceptsPriority(
      final ChemistryWorkflowRecommendationPriority candidate,
      final ChemistryWorkflowRecommendationPriority minimum) {
    return ChemistryFollowUpCalculationPlanner.rank(candidate)
        <= ChemistryFollowUpCalculationPlanner.rank(minimum);
  }

  private static int rank(final ChemistryWorkflowRecommendationPriority priority) {
    if (priority == ChemistryWorkflowRecommendationPriority.HIGH) {
      return 0;
    }
    if (priority == ChemistryWorkflowRecommendationPriority.MEDIUM) {
      return 1;
    }
    return 2;
  }

  private static String requestScope(final ChemistryWorkflowRecommendation recommendation) {
    if (recommendation.appliesToCandidate()) {
      return "candidate_" + recommendation.candidateIndex();
    }
    return "program";
  }

  private static String safeId(final String value) {
    final String lower = value.toLowerCase(Locale.ROOT);
    final StringBuilder builder = new StringBuilder(lower.length());
    for (int i = 0; i < lower.length(); ++i) {
      final char current = lower.charAt(i);
      if ((current >= 'a' && current <= 'z')
          || (current >= '0' && current <= '9')
          || current == '_') {
        builder.append(current);
      } else {
        builder.append('_');
      }
    }
    if (builder.length() == 0 || !Character.isLetter(builder.charAt(0))) {
      builder.insert(0, "followup_");
    }
    return builder.toString();
  }
}
