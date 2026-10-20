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
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.job.ChemistryWorkflowJob;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendation;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationKind;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPlan;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.recommendation.ChemistryWorkflowRecommendationPriority;

/**
 * Планирует следующие расчетные действия после прогноза органической реакции.
 */
public final class ReactionPredictionFollowUpPlanner {

  private static final int PROGRAM_LEVEL = -1;
  private static final int MIN_COMPARISON_CANDIDATES = 5;
  private static final int MAX_COMPARISON_CANDIDATES = 32;
  private static final double QUANTUM_CANDIDATE_PROBABILITY_FLOOR = 0.03;

  public ChemistryWorkflowRecommendationPlan plan(
      final ChemistryWorkflowJob job, final ReactionPredictionResult predictionResult) {
    if (job == null) {
      throw new IllegalArgumentException("Chemistry workflow job must not be null.");
    }
    return this.plan(job.id().value(), predictionResult);
  }

  public ChemistryWorkflowRecommendationPlan plan(
      final String workflowJobId, final ReactionPredictionResult predictionResult) {
    if (workflowJobId == null || workflowJobId.isBlank()) {
      throw new IllegalArgumentException(
          "Chemistry workflow recommendation plan job id must not be blank.");
    }
    if (predictionResult == null || !predictionResult.hasCandidates()) {
      return ChemistryWorkflowRecommendationPlan.EMPTY;
    }
    final ArrayList<ChemistryWorkflowRecommendation> recommendations =
        new ArrayList<ChemistryWorkflowRecommendation>();
    this.addTopCandidateRefinement(predictionResult, recommendations);
    this.addCandidateComparison(predictionResult, recommendations);
    this.addConditionRepair(predictionResult, recommendations);
    this.addConditionSensitivity(predictionResult, recommendations);
    this.addPracticalConditionFollowUps(predictionResult, recommendations);
    this.addBenchmarkAndUncertaintyFollowUps(predictionResult, recommendations);
    this.addOrganicCharacterizationFollowUps(predictionResult, recommendations);
    this.addMechanismSpecificFollowUps(predictionResult, recommendations);
    this.addCompetingPathwayFollowUps(predictionResult, recommendations);
    this.addCandidateMaterializationFollowUps(predictionResult, recommendations);
    this.addCandidateResolvedElectronicFollowUps(predictionResult, recommendations);
    this.addQuantumPreparation(predictionResult, recommendations);
    this.addWarningReview(predictionResult, recommendations);
    return ChemistryWorkflowRecommendationPlan.of(workflowJobId, recommendations);
  }

  private void addTopCandidateRefinement(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final ReactionProductCandidate bestCandidate = result.bestCandidate();
    final double probability = bestCandidate.probability();
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.HIGH,
            0,
            ClassicalCalculationKind.CONFORMER_SEARCH,
            "best_product_conformers",
            "Find low-energy conformers before comparing reaction candidates."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.HIGH,
            0,
            ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
            "best_product_geometry",
            "Optimize the best product candidate geometry before energy or spectroscopy checks."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            probability >= 0.5
                ? ChemistryWorkflowRecommendationPriority.MEDIUM
                : ChemistryWorkflowRecommendationPriority.HIGH,
            0,
            ClassicalCalculationKind.REACTION_ENERGY,
            "best_product_reaction_energy",
            "Estimate reaction energy to verify that ranking is thermodynamically plausible."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            0,
            ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS,
            "best_product_thermodynamics",
            "Estimate Gibbs/enthalpy/entropy context for condition-dependent product stability."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.LOW,
            0,
            ClassicalCalculationKind.SPECTROSCOPY_PREDICTION,
            "best_product_spectroscopy",
            "Prepare spectral signatures for later experimental or reference comparison."));
  }

  private void addCandidateComparison(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final int candidateCount = ReactionPredictionFollowUpPlanner.candidateFollowUpLimit(result);
    for (int i = 1; i < candidateCount; ++i) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
              ChemistryWorkflowRecommendationPriority.MEDIUM,
              i,
              ClassicalCalculationKind.DESCRIPTOR_ANALYSIS,
              "candidate_" + i + "_descriptor_comparison",
              "Compare descriptors against the leading product candidate."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
              ChemistryWorkflowRecommendationPriority.MEDIUM,
              i,
              ClassicalCalculationKind.REACTION_ENERGY,
              "candidate_" + i + "_reaction_energy",
              "Check whether this alternative product is competitive by reaction energy."));
    }
  }

  private void addConditionSensitivity(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (result.request().conditions().temperature() == null
        && !result.request().conditions().hasDuration()
        && !result
            .request()
            .conditions()
            .hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)) {
      return;
    }
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
            ChemistryWorkflowRecommendationPriority.HIGH,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.KINETIC_ANALYSIS,
            "condition_sensitivity_kinetics",
            "Run kinetic sensitivity because temperature, duration, or catalyst context is present."));
  }

  private void addPracticalConditionFollowUps(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (ReactionPredictionFollowUpPlanner.polyetherOrHighBoilingSolvent(result)
        || result.request().conditions().hasFeature(ReactionConditionFeatureKind.HEATING)
        || result.request().conditions().hasFeature(ReactionConditionFeatureKind.REFLUX)) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
              ChemistryWorkflowRecommendationPriority.HIGH,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.SOLVENT_SCREENING,
              "conditions/polyether-high-boiling-solvent-screen",
              "Screen high-boiling and polyether media because viscosity, ion pairing and mass transfer can change organic product distribution."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
              ChemistryWorkflowRecommendationPriority.MEDIUM,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.MICROKINETIC_MODELING,
              "kinetics/high-boiling-condition-window",
              "Model hot-condition branch weights before trusting a single predicted product probability."));
    }
    if (ReactionPredictionFollowUpPlanner.massQuantified(result)) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.SCALE_UP_RISK_ANALYSIS,
              ChemistryWorkflowRecommendationPriority.HIGH,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS,
              "scale/mass-transfer-risk",
              "Quantified gram-scale recipe inputs require scale, mixing and heat-transfer review before route selection."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
              ChemistryWorkflowRecommendationPriority.MEDIUM,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS,
              "conditions/addition-heating-timeline",
              "Plan addition, heating and sampling timeline because time and mass-transfer history can change product ratios."));
    }
  }

  private void addMechanismSpecificFollowUps(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final int candidateCount = ReactionPredictionFollowUpPlanner.candidateFollowUpLimit(result);
    for (int candidateIndex = 0; candidateIndex < candidateCount; ++candidateIndex) {
      boolean transitionStateAdded = false;
      boolean reactionPathAdded = false;
      boolean kineticAdded = false;
      boolean descriptorAdded = false;
      boolean quantumHamiltonianAdded = false;
      final List<ReactionPredictionEvidence> evidence =
          result.candidates().get(candidateIndex).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        final String description =
            evidence.get(evidenceIndex).description().toLowerCase(Locale.ROOT);
        if (!transitionStateAdded
            && ReactionPredictionFollowUpPlanner.requiresTransitionStateSearch(description)) {
          recommendations.add(
              ReactionPredictionFollowUpPlanner.recommendation(
                  ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                  ChemistryWorkflowRecommendationPriority.HIGH,
                  candidateIndex,
                  ClassicalCalculationKind.TRANSITION_STATE_SEARCH,
                  "candidate_" + candidateIndex + "_mechanism_transition_state",
                  "Locate a transition-state model because the predicted mechanism is stepwise or selectivity-critical."));
          transitionStateAdded = true;
        }
        if (!reactionPathAdded
            && ReactionPredictionFollowUpPlanner.requiresReactionPath(description)) {
          recommendations.add(
              ReactionPredictionFollowUpPlanner.recommendation(
                  ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                  ChemistryWorkflowRecommendationPriority.HIGH,
                  candidateIndex,
                  ClassicalCalculationKind.REACTION_PATH,
                  "candidate_" + candidateIndex + "_mechanism_reaction_path",
                  "Trace the reaction path to replay the predicted multi-step mechanism."));
          reactionPathAdded = true;
        }
        if (!kineticAdded && ReactionPredictionFollowUpPlanner.requiresKineticCheck(description)) {
          recommendations.add(
              ReactionPredictionFollowUpPlanner.recommendation(
                  ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
                  ChemistryWorkflowRecommendationPriority.MEDIUM,
                  candidateIndex,
                  ClassicalCalculationKind.KINETIC_ANALYSIS,
                  "candidate_" + candidateIndex + "_mechanism_kinetics",
                  "Check kinetic competition between mechanism branches under the supplied conditions."));
          kineticAdded = true;
        }
        if (!descriptorAdded
            && ReactionPredictionFollowUpPlanner.requiresDescriptorCheck(description)) {
          recommendations.add(
              ReactionPredictionFollowUpPlanner.recommendation(
                  ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
                  ChemistryWorkflowRecommendationPriority.MEDIUM,
                  candidateIndex,
                  ClassicalCalculationKind.DESCRIPTOR_ANALYSIS,
                  "candidate_" + candidateIndex + "_mechanism_regioselectivity_descriptors",
                  "Compute descriptors for site-selectivity and directing-group reasoning."));
          descriptorAdded = true;
        }
        if (!quantumHamiltonianAdded
            && ReactionPredictionFollowUpPlanner.requiresQuantumHamiltonianCheck(description)) {
          recommendations.add(
              ReactionPredictionFollowUpPlanner.recommendation(
                  ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
                  ChemistryWorkflowRecommendationPriority.MEDIUM,
                  candidateIndex,
                  ClassicalCalculationKind.HAMILTONIAN_PREPARATION,
                  "candidate_" + candidateIndex + "_mechanism_quantum_hamiltonian",
                  "Prepare a Hamiltonian-level reference for electronic or organometallic mechanism validation."));
          quantumHamiltonianAdded = true;
        }
      }
    }
  }

  private void addOrganicCharacterizationFollowUps(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            0,
            ClassicalCalculationKind.NMR_PREDICTION,
            "best_product/nmr-confirmation",
            "Predict NMR signatures for the leading organic product candidate."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            0,
            ClassicalCalculationKind.IR_RAMAN_PREDICTION,
            "best_product/ir-raman-confirmation",
            "Predict IR/Raman functional-group signatures for product confirmation."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.LOW,
            0,
            ClassicalCalculationKind.MASS_SPECTROMETRY_PREDICTION,
            "best_product/mass-spectrometry-fragments",
            "Estimate mass-spectrometry fragments for later analytical comparison."));
    if (ReactionPredictionFollowUpPlanner.hasAcidBaseOrAmineSignal(result)) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
              ChemistryWorkflowRecommendationPriority.HIGH,
              0,
              ClassicalCalculationKind.PKA_PREDICTION,
              "best_product/pka-protonation-state",
              "Check product pKa and protonation states because amines, heteroatoms, acid/base or ion-pairing can change route probabilities."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
              ChemistryWorkflowRecommendationPriority.MEDIUM,
              0,
              ClassicalCalculationKind.SOLVATION_MODELING,
              "best_product/solvation-ion-pairing",
              "Model product solvation and ion-pairing effects before trusting neutral-structure product probabilities."));
    }
    if (ReactionPredictionFollowUpPlanner.hasRedoxOrElectronTransferSignal(result)) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.MECHANISM_DISAMBIGUATION,
              ChemistryWorkflowRecommendationPriority.HIGH,
              0,
              ClassicalCalculationKind.REDOX_POTENTIAL,
              "best_product/redox-window",
              "Estimate product redox window because electron-transfer, oxidation or reduction routes are plausible."));
    }
    if (ReactionPredictionFollowUpPlanner.hasStereoSignal(result)) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
              ChemistryWorkflowRecommendationPriority.HIGH,
              0,
              ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS,
              "best_product/stereochemical-outcome",
              "Analyze stereochemical outcome because product ranking can split across faces, E/Z or R/S centers."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
              ChemistryWorkflowRecommendationPriority.MEDIUM,
              0,
              ClassicalCalculationKind.ENANTIOMER_DIASTEREOMER_ANALYSIS,
              "best_product/enantiomer-diastereomer-window",
              "Compare enantiomeric and diastereomeric channels when stereochemical signals are present."));
    }
    if (result.request().hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        || result.request().hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
              ChemistryWorkflowRecommendationPriority.MEDIUM,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.ACTIVE_SPACE_SELECTION,
              "quantum/active-space-selection",
              "Resolve a heterocycle-focused active space automatically before preparing quantum programs."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
              ChemistryWorkflowRecommendationPriority.LOW,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.ORBITAL_OPTIMIZATION,
              "quantum/orbital-optimization",
              "Optimize orbitals for heteroatom-rich pi systems when electronic rearrangement affects ranking."));
    }
  }

  private void addBenchmarkAndUncertaintyFollowUps(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final double bestProbability = result.bestCandidate().probability();
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            0,
            ClassicalCalculationKind.BASIS_SET_CONVERGENCE,
            "best_product_basis_convergence",
            "Check whether the leading product ranking is stable across basis-set quality."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            0,
            ClassicalCalculationKind.METHOD_BENCHMARK,
            "best_product_method_benchmark",
            "Compare method families before trusting final reaction energetics."));
    if (bestProbability < 0.75 || result.candidates().size() > 1) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
              ChemistryWorkflowRecommendationPriority.HIGH,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.UNCERTAINTY_ANALYSIS,
              "prediction_uncertainty",
              "Quantify structure, method, condition and mechanism uncertainty before choosing a route."));
    }
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.MECHANISM_DISAMBIGUATION,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.MECHANISM_ENUMERATION,
            "mechanism/enumeration",
            "Enumerate plausible named mechanism buckets from structure and condition signals."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
            ChemistryWorkflowRecommendationPriority.HIGH,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS,
            "products/distribution",
            "Estimate major/minor product distribution before committing to a synthetic route."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION,
            "products/yield-window",
            "Estimate conservative and optimistic yield windows from mechanism and condition uncertainty."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS,
            "products/impurity-side-products",
            "Track likely impurity families and side-product pathways for route review."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.SOLVENT_SCREENING,
            "conditions/solvent-screen",
            "Screen solvent classes because solvent changes can swap ionic, radical and neutral routes."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.CATALYST_SCREENING,
            "conditions/catalyst-screen",
            "Screen catalyst families instead of assuming the current catalyst is route-optimal."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
            ChemistryWorkflowRecommendationPriority.LOW,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.CONCENTRATION_SCREENING,
            "conditions/concentration-screen",
            "Check concentration regimes for intra/intermolecular competition and side paths."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS,
            "conditions/quench-workup",
            "Check quench and workup sensitivity because post-reaction handling can change isolated outcome."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
            ChemistryWorkflowRecommendationPriority.LOW,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS,
            "conditions/reaction-history",
            "Plan timeline checkpoints for addition order, heating, sampling and endpoint confirmation."));
    if (ReactionPredictionFollowUpPlanner.warningEvidenceCount(result) > 0L
        || ReactionPredictionFollowUpPlanner.competingPathwayEvidenceCount(result) > 0L) {
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.WARNING_REVIEW,
              ChemistryWorkflowRecommendationPriority.HIGH,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.REACTION_HAZARD_AND_STABILITY_ANALYSIS,
              "safety/hazard-stability",
              "Review hazard and stability signals before scaling or trusting a condition-sensitive route."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.WARNING_REVIEW,
              ChemistryWorkflowRecommendationPriority.HIGH,
              PROGRAM_LEVEL,
              ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS,
              "safety/scale-up-risk",
              "Estimate thermal, mixing and handling risks before translating the route to larger scale."));
    }
  }

  private void addConditionRepair(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    boolean catalystRecommendationAdded = false;
    boolean baseRecommendationAdded = false;
    boolean acidBaseRecommendationAdded = false;
    boolean activationRecommendationAdded = false;
    boolean leavingGroupRecommendationAdded = false;
    final List<ReactionPredictionEvidence> evidence = result.diagnostics();
    for (int i = 0; i < evidence.size(); ++i) {
      final String description = evidence.get(i).description().toLowerCase(Locale.ROOT);
      if (!catalystRecommendationAdded
          && (description.contains("no explicit catalyst")
              || description.contains("no catalyst"))) {
        recommendations.add(
            ReactionPredictionFollowUpPlanner.recommendation(
                ChemistryWorkflowRecommendationKind.CONDITION_REPAIR,
                ChemistryWorkflowRecommendationPriority.HIGH,
                PROGRAM_LEVEL,
                null,
                "conditions/catalyst",
                "Add an explicit catalyst component or mark the route as catalyst-free before trusting the ranking."));
        catalystRecommendationAdded = true;
      }
      if (!baseRecommendationAdded && description.contains("no base component")) {
        recommendations.add(
            ReactionPredictionFollowUpPlanner.recommendation(
                ChemistryWorkflowRecommendationKind.CONDITION_REPAIR,
                ChemistryWorkflowRecommendationPriority.HIGH,
                PROGRAM_LEVEL,
                null,
                "conditions/base",
                "Add an explicit base component when the requested domain depends on base-assisted activation."));
        baseRecommendationAdded = true;
      }
      if (!acidBaseRecommendationAdded && description.contains("no acid/base component")) {
        recommendations.add(
            ReactionPredictionFollowUpPlanner.recommendation(
                ChemistryWorkflowRecommendationKind.CONDITION_REPAIR,
                ChemistryWorkflowRecommendationPriority.HIGH,
                PROGRAM_LEVEL,
                null,
                "conditions/acid-base",
                "Add acid or base context for a proton-transfer route, or remove the acid-base domain."));
        acidBaseRecommendationAdded = true;
      }
      if (!activationRecommendationAdded
          && description.contains("no explicit activation condition")) {
        recommendations.add(
            ReactionPredictionFollowUpPlanner.recommendation(
                ChemistryWorkflowRecommendationKind.CONDITION_REPAIR,
                ChemistryWorkflowRecommendationPriority.MEDIUM,
                PROGRAM_LEVEL,
                null,
                "conditions/activation",
                "Specify thermal, photochemical, acid, or other activation conditions for this reaction family."));
        activationRecommendationAdded = true;
      }
      if (!leavingGroupRecommendationAdded
          && (description.contains("no detected leaving group")
              || description.contains("no detected leaving-group")
              || description.contains("no detected leaving-group/halogen"))) {
        recommendations.add(
            ReactionPredictionFollowUpPlanner.recommendation(
                ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
                ChemistryWorkflowRecommendationPriority.HIGH,
                PROGRAM_LEVEL,
                null,
                "reactants/leaving-group",
                "Review reactant structure: this route family usually requires a leaving-group or halogen handle."));
        leavingGroupRecommendationAdded = true;
      }
    }
  }

  private void addQuantumPreparation(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (!ReactionPredictionFollowUpPlanner.electronicallyComplexDomain(result)) {
      return;
    }
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            0,
            ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION,
            "best_product_electronic_structure",
            "Prepare electronic structure data for the leading electronically complex product candidate."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            0,
            ClassicalCalculationKind.HAMILTONIAN_PREPARATION,
            "best_product_hamiltonian",
            "Build a fermionic Hamiltonian that can be mapped into Quantum IR."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
            ChemistryWorkflowRecommendationPriority.LOW,
            0,
            ClassicalCalculationKind.RELATIVISTIC_CORRECTION,
            "best_product_relativistic_correction",
            "Estimate scalar relativistic correction before comparing heavy-atom or catalyst-sensitive routes."));
  }

  private void addCandidateResolvedElectronicFollowUps(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final int candidateCount = ReactionPredictionFollowUpPlanner.candidateFollowUpLimit(result);
    for (int candidateIndex = 0; candidateIndex < candidateCount; ++candidateIndex) {
      final ReactionProductCandidate candidate = result.candidates().get(candidateIndex);
      if (!ReactionPredictionFollowUpPlanner.requiresCandidateElectronicWorkflow(
          result, candidate, candidateIndex)) {
        continue;
      }
      final ChemistryWorkflowRecommendationPriority priority =
          candidateIndex == 0 || candidate.probability() >= 0.15
              ? ChemistryWorkflowRecommendationPriority.HIGH
              : ChemistryWorkflowRecommendationPriority.MEDIUM;
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              priority,
              candidateIndex,
              ClassicalCalculationKind.CONFORMER_SEARCH,
              "candidate_" + candidateIndex + "/conformer-ensemble",
              "Build a candidate-specific conformer ensemble before electronic ranking."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              priority,
              candidateIndex,
              ClassicalCalculationKind.GEOMETRY_OPTIMIZATION,
              "candidate_" + candidateIndex + "/geometry-optimization",
              "Optimize this candidate geometry before active-space and energy calculations."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              priority,
              candidateIndex,
              ClassicalCalculationKind.SPECTROSCOPY_PREDICTION,
              "candidate_" + candidateIndex + "/spectroscopy-signature",
              "Generate spectroscopy signatures so candidate identity can be checked against analytical data."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              candidateIndex == 0
                  ? ChemistryWorkflowRecommendationPriority.MEDIUM
                  : ChemistryWorkflowRecommendationPriority.LOW,
              candidateIndex,
              ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS,
              "candidate_" + candidateIndex + "/molecular-property-panel",
              "Compute molecular property panel for candidate-specific risk and route comparison."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
              priority,
              candidateIndex,
              ClassicalCalculationKind.ACTIVE_SPACE_SELECTION,
              "candidate_" + candidateIndex + "/active-space-selection",
              "Resolve a candidate-specific active space and allow explicit override for sensitive product channels."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
              priority,
              candidateIndex,
              ClassicalCalculationKind.ORBITAL_OPTIMIZATION,
              "candidate_" + candidateIndex + "/orbital-optimization",
              "Optimize orbitals for this candidate when pi, heteroatom or radical rearrangement controls ranking."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              priority,
              candidateIndex,
              ClassicalCalculationKind.MP2_CORRELATION,
              "candidate_" + candidateIndex + "/mp2-correlation",
              "Compute MP2 correlation as the first correlated-energy check for this candidate."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              priority,
              candidateIndex,
              ClassicalCalculationKind.CCSD_CORRELATION,
              "candidate_" + candidateIndex + "/ccsd-correlation",
              "Compute CCSD correlation when active orbital optimization indicates a sensitive electronic structure."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              candidateIndex == 0 ? priority : ChemistryWorkflowRecommendationPriority.MEDIUM,
              candidateIndex,
              ClassicalCalculationKind.CISD_CORRELATION,
              "candidate_" + candidateIndex + "/cisd-correlation",
              "Run CISD as an independent configuration-interaction cross-check for candidate ordering."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              candidateIndex == 0
                  ? ChemistryWorkflowRecommendationPriority.MEDIUM
                  : ChemistryWorkflowRecommendationPriority.LOW,
              candidateIndex,
              ClassicalCalculationKind.FCI_DIAGONALIZATION,
              "candidate_" + candidateIndex + "/small-active-space-fci",
              "Diagonalize the selected active-space Hamiltonian when the determinant count is tractable."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
              priority,
              candidateIndex,
              ClassicalCalculationKind.HAMILTONIAN_PREPARATION,
              "candidate_" + candidateIndex + "/hamiltonian-preparation",
              "Prepare a candidate-specific fermionic Hamiltonian for classical and quantum parity checks."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
              candidateIndex == 0
                  ? ChemistryWorkflowRecommendationPriority.MEDIUM
                  : ChemistryWorkflowRecommendationPriority.LOW,
              candidateIndex,
              ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION,
              "candidate_" + candidateIndex + "/quantum-program-preparation",
              "Generate a candidate-specific Quantum IR program package after Hamiltonian preparation."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.QUANTUM_PREPARATION,
              candidateIndex == 0
                  ? ChemistryWorkflowRecommendationPriority.MEDIUM
                  : ChemistryWorkflowRecommendationPriority.LOW,
              candidateIndex,
              ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION,
              "candidate_" + candidateIndex + "/quantum-vqe-optimization",
              "Run Java-native VQE for a candidate-specific active-space Hamiltonian and compare it with classical energies."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_COMPARISON,
              priority,
              candidateIndex,
              ClassicalCalculationKind.REACTION_ENERGY,
              "candidate_" + candidateIndex + "/first-principles-reaction-energy",
              "Compare this candidate against reactants with Java-native first-principles energy when feasible."));
    }
  }

  private static boolean requiresCandidateElectronicWorkflow(
      final ReactionPredictionResult result,
      final ReactionProductCandidate candidate,
      final int candidateIndex) {
    if (candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
      return false;
    }
    if (candidate.products().participantCount() == 0) {
      return false;
    }
    if (candidateIndex == 0) {
      return true;
    }
    if (candidate.probability() >= QUANTUM_CANDIDATE_PROBABILITY_FLOOR) {
      return true;
    }
    if (ReactionPredictionFollowUpPlanner.electronicallyComplexDomain(result)) {
      return true;
    }
    final List<ReactionPredictionEvidence> evidence = candidate.evidence();
    for (int i = 0; i < evidence.size(); ++i) {
      final String description = evidence.get(i).description().toLowerCase(Locale.ROOT);
      if (ReactionPredictionFollowUpPlanner.requiresQuantumHamiltonianCheck(description)
          || ReactionPredictionFollowUpPlanner.requiresTransitionStateSearch(description)
          || ReactionPredictionFollowUpPlanner.requiresKineticCheck(description)) {
        return true;
      }
    }
    return false;
  }

  private static int candidateFollowUpLimit(final ReactionPredictionResult result) {
    final List<ReactionProductCandidate> candidates = result.candidates();
    int limit = Math.min(candidates.size(), MIN_COMPARISON_CANDIDATES);
    for (int i = limit; i < candidates.size() && i < MAX_COMPARISON_CANDIDATES; ++i) {
      final ReactionProductCandidate candidate = candidates.get(i);
      if (candidate.probability() >= QUANTUM_CANDIDATE_PROBABILITY_FLOOR
          || ReactionPredictionFollowUpPlanner.requiresCandidateElectronicWorkflow(
              result, candidate, i)) {
        limit = i + 1;
      }
    }
    return Math.min(limit, MAX_COMPARISON_CANDIDATES);
  }

  private static boolean electronicallyComplexDomain(final ReactionPredictionResult result) {
    return result.request().hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        || result.request().hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)
        || result.request().hasDomain(OrganicReactionDomain.CROSS_COUPLING)
        || result.request().hasDomain(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION)
        || result.request().hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)
        || result.request().hasDomain(OrganicReactionDomain.REARRANGEMENT)
        || ReactionPredictionFollowUpPlanner.hasElectronicStructureSensitiveAdditionSignal(result);
  }

  private void addCompetingPathwayFollowUps(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (ReactionPredictionFollowUpPlanner.competingPathwayEvidenceCount(result) == 0) {
      return;
    }
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.MECHANISM_DISAMBIGUATION,
            ChemistryWorkflowRecommendationPriority.HIGH,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS,
            "mechanism/competing-pathways",
            "Rank competing named mechanism families before selecting the route."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.MECHANISM_DISAMBIGUATION,
            ChemistryWorkflowRecommendationPriority.HIGH,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS,
            "mechanism/transition-state-ensemble",
            "Build transition-state candidates for the main and side routes."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION,
            "conditions/selectivity-optimization",
            "Optimize conditions to suppress competing pathways without changing the target reaction."));
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.CONDITION_SENSITIVITY,
            ChemistryWorkflowRecommendationPriority.MEDIUM,
            PROGRAM_LEVEL,
            ClassicalCalculationKind.MICROKINETIC_MODELING,
            "kinetics/competing-pathways",
            "Estimate microkinetic branch weights across the competing mechanism network."));
  }

  private void addWarningReview(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    if (ReactionPredictionFollowUpPlanner.warningEvidenceCount(result) == 0) {
      return;
    }
    recommendations.add(
        ReactionPredictionFollowUpPlanner.recommendation(
            ChemistryWorkflowRecommendationKind.WARNING_REVIEW,
            ChemistryWorkflowRecommendationPriority.HIGH,
            PROGRAM_LEVEL,
            null,
            "prediction_warning_review",
            "Review warning evidence before treating the prediction as a reliable route."));
  }

  private void addCandidateMaterializationFollowUps(
      final ReactionPredictionResult result,
      final List<ChemistryWorkflowRecommendation> recommendations) {
    final int candidateCount = ReactionPredictionFollowUpPlanner.candidateFollowUpLimit(result);
    for (int candidateIndex = 0; candidateIndex < candidateCount; ++candidateIndex) {
      final ReactionProductCandidate candidate = result.candidates().get(candidateIndex);
      if (!candidate.hasEvidence(ReactionPredictionEvidenceKind.SURROGATE_PRODUCT)) {
        continue;
      }
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.PRODUCT_REFINEMENT,
              candidateIndex == 0
                  ? ChemistryWorkflowRecommendationPriority.HIGH
                  : ChemistryWorkflowRecommendationPriority.MEDIUM,
              candidateIndex,
              null,
              "candidate_" + candidateIndex + "/explicit-product-graph-materialization",
              "Materialize this mechanism-only product channel into an explicit product molecule graph before energy, Hamiltonian or Quantum IR preparation."));
      recommendations.add(
          ReactionPredictionFollowUpPlanner.recommendation(
              ChemistryWorkflowRecommendationKind.MECHANISM_DISAMBIGUATION,
              ChemistryWorkflowRecommendationPriority.MEDIUM,
              candidateIndex,
              ClassicalCalculationKind.REACTION_PATH,
              "candidate_" + candidateIndex + "/surrogate-mechanism-to-structure-path",
              "Trace the mechanism steps that define which atoms, bonds, charges and stereochemical labels must be changed to build an explicit product graph."));
    }
  }

  private static long warningEvidenceCount(final ReactionPredictionResult result) {
    long count = 0L;
    final List<ReactionProductCandidate> candidates = result.candidates();
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(candidateIndex).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        if (ReactionPredictionFollowUpPlanner.isWarningEvidence(
            evidence.get(evidenceIndex).kind())) {
          ++count;
        }
      }
    }
    final List<ReactionPredictionEvidence> diagnostics = result.diagnostics();
    for (int diagnosticIndex = 0; diagnosticIndex < diagnostics.size(); ++diagnosticIndex) {
      if (ReactionPredictionFollowUpPlanner.isWarningEvidence(
          diagnostics.get(diagnosticIndex).kind())) {
        ++count;
      }
    }
    return count;
  }

  private static long competingPathwayEvidenceCount(final ReactionPredictionResult result) {
    long count = 0L;
    final List<ReactionProductCandidate> candidates = result.candidates();
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(candidateIndex).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        if (evidence.get(evidenceIndex).kind()
            == ReactionPredictionEvidenceKind.COMPETING_PATHWAY) {
          ++count;
        }
      }
    }
    final List<ReactionPredictionEvidence> diagnostics = result.diagnostics();
    for (int diagnosticIndex = 0; diagnosticIndex < diagnostics.size(); ++diagnosticIndex) {
      if (diagnostics.get(diagnosticIndex).kind()
          == ReactionPredictionEvidenceKind.COMPETING_PATHWAY) {
        ++count;
      }
    }
    return count;
  }

  private static boolean isWarningEvidence(final ReactionPredictionEvidenceKind kind) {
    return kind == ReactionPredictionEvidenceKind.WARNING
        || kind == ReactionPredictionEvidenceKind.COMPETING_PATHWAY;
  }

  private static boolean requiresTransitionStateSearch(final String description) {
    return description.contains("sandmeyer")
        || description.contains("grignard")
        || description.contains("organolithium")
        || description.contains("c-h activation")
        || description.contains("solvolysis")
        || description.contains("fragmentation")
        || description.contains("cycloaddition")
        || description.contains("rearrangement")
        || description.contains("addition-elimination")
        || description.contains("sigmatropic")
        || description.contains("radical cyclization");
  }

  private static boolean requiresReactionPath(final String description) {
    return description.contains("detailed staged events")
        || description.contains("reaction path")
        || description.contains("polymerization")
        || description.contains("hydrolysis")
        || description.contains("esterification")
        || description.contains("transesterification")
        || description.contains("iminium")
        || description.contains("sigma-adduct")
        || description.contains("addition/dehydration")
        || description.contains("single-electron-transfer");
  }

  private static boolean requiresKineticCheck(final String description) {
    return description.contains("radical")
        || description.contains("polymerization")
        || description.contains("solvolysis")
        || description.contains("c-h activation")
        || description.contains("diazotization")
        || description.contains("compete")
        || description.contains("chain")
        || description.contains("branch weights");
  }

  private static boolean requiresDescriptorCheck(final String description) {
    return description.contains("heterocycle")
        || description.contains("c-h activation")
        || description.contains("sandmeyer")
        || description.contains("regio");
  }

  private static boolean requiresQuantumHamiltonianCheck(final String description) {
    return description.contains("organometallic")
        || description.contains("grignard")
        || description.contains("organolithium")
        || description.contains("c-h activation")
        || description.contains("carbonyl")
        || description.contains("imine")
        || description.contains("iminium")
        || description.contains("enolate")
        || description.contains("conjugate")
        || description.contains("epoxide")
        || description.contains("nitrile")
        || description.contains("acyl")
        || description.contains("electron transfer")
        || description.contains("single-electron-transfer")
        || description.contains("photoredox")
        || description.contains("redox")
        || description.contains("heteroatom-rich pi");
  }

  private static boolean hasElectronicStructureSensitiveAdditionSignal(
      final ReactionPredictionResult result) {
    if (!result.request().hasDomain(OrganicReactionDomain.ADDITION)
        && !result.request().hasDomain(OrganicReactionDomain.ACID_BASE)) {
      return false;
    }
    return ReactionPredictionFollowUpPlanner.requiresQuantumHamiltonianCheck(
        ReactionPredictionFollowUpPlanner.predictionText(result));
  }

  private static boolean polyetherOrHighBoilingSolvent(final ReactionPredictionResult result) {
    final List<ReactionConditionComponent> solvents =
        result.request().conditions().componentsByRole(ReactionComponentRole.SOLVENT);
    for (int i = 0; i < solvents.size(); ++i) {
      final String name = solvents.get(i).name().toLowerCase(Locale.ROOT);
      if (name.contains("peg")
          || name.contains("polyethylene glycol")
          || name.contains("polyether")
          || name.contains("glyme")
          || name.contains("diglyme")
          || name.contains("triglyme")
          || name.contains("high-boiling")
          || name.contains("high boiling")) {
        return true;
      }
    }
    return result.request().conditions().note() != null
        && ReactionPredictionFollowUpPlanner.polyetherSignal(
            result.request().conditions().note().toLowerCase(Locale.ROOT));
  }

  private static boolean polyetherSignal(final String value) {
    return value.contains("peg")
        || value.contains("polyethylene glycol")
        || value.contains("polyether")
        || value.contains("glyme")
        || value.contains("high-boiling")
        || value.contains("high boiling");
  }

  private static boolean massQuantified(final ReactionPredictionResult result) {
    final List<ReactionConditionComponent> components = result.request().conditions().components();
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      if (component.hasAmount() && component.amount().massQuantity()) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAcidBaseOrAmineSignal(final ReactionPredictionResult result) {
    if (!result.request().conditions().componentsByRole(ReactionComponentRole.ACID).isEmpty()
        || !result.request().conditions().componentsByRole(ReactionComponentRole.BASE).isEmpty()
        || result.request().conditions().hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || result.request().conditions().hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      return true;
    }
    final String text = ReactionPredictionFollowUpPlanner.predictionText(result);
    return text.contains("amine")
        || text.contains("amino")
        || text.contains("proton")
        || text.contains("acid")
        || text.contains("base")
        || text.contains("ion pair")
        || text.contains("ion-pair");
  }

  private static boolean hasRedoxOrElectronTransferSignal(final ReactionPredictionResult result) {
    final String text = ReactionPredictionFollowUpPlanner.predictionText(result);
    return text.contains("redox")
        || text.contains("electron transfer")
        || text.contains("electron-transfer")
        || text.contains("oxidation")
        || text.contains("reduction")
        || text.contains("radical")
        || result.request().hasDomain(OrganicReactionDomain.OXIDATION_REDUCTION)
        || result.request().hasDomain(OrganicReactionDomain.RADICAL_FUNCTIONALIZATION);
  }

  private static boolean hasStereoSignal(final ReactionPredictionResult result) {
    final String text = ReactionPredictionFollowUpPlanner.predictionText(result);
    return text.contains("stereo")
        || text.contains("enantio")
        || text.contains("diastereo")
        || text.contains("cis")
        || text.contains("trans")
        || text.contains("e/z")
        || text.contains("r/s")
        || text.contains("chiral");
  }

  private static String predictionText(final ReactionPredictionResult result) {
    final StringBuilder builder = new StringBuilder(512);
    final List<ReactionProductCandidate> candidates = result.candidates();
    for (int candidateIndex = 0; candidateIndex < candidates.size(); ++candidateIndex) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(candidateIndex).evidence();
      for (int evidenceIndex = 0; evidenceIndex < evidence.size(); ++evidenceIndex) {
        builder.append(evidence.get(evidenceIndex).description().toLowerCase(Locale.ROOT));
        builder.append('\n');
      }
    }
    final List<ReactionPredictionEvidence> diagnostics = result.diagnostics();
    for (int diagnosticIndex = 0; diagnosticIndex < diagnostics.size(); ++diagnosticIndex) {
      builder.append(diagnostics.get(diagnosticIndex).description().toLowerCase(Locale.ROOT));
      builder.append('\n');
    }
    if (result.request().hasNote()) {
      builder.append(result.request().note().toLowerCase(Locale.ROOT));
    }
    return builder.toString();
  }

  private static ChemistryWorkflowRecommendation recommendation(
      final ChemistryWorkflowRecommendationKind kind,
      final ChemistryWorkflowRecommendationPriority priority,
      final int candidateIndex,
      final ClassicalCalculationKind calculationKind,
      final String target,
      final String reason) {
    return ChemistryWorkflowRecommendation.of(
        kind, priority, candidateIndex, calculationKind, target, reason);
  }
}
