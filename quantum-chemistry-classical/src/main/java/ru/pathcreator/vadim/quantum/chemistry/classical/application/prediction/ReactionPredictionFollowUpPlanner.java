/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.prediction;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionApplicabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.prediction.ReactionPredictionReadinessLevel;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidence;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionProductCandidate;

/**
 * Строит расчетный маршрут для доведения прогноза реакции до проверяемого решения.
 */
public final class ReactionPredictionFollowUpPlanner {

  public ReactionPredictionFollowUpPlan plan(
      final ReactionPredictionResult result,
      final ReactionPredictionApplicabilityReport applicability,
      final ReactionStoichiometryProfile stoichiometry) {
    if (result == null) {
      throw new IllegalArgumentException("Reaction prediction follow-up result must not be null.");
    }
    if (applicability == null) {
      throw new IllegalArgumentException(
          "Reaction prediction follow-up applicability must not be null.");
    }
    if (stoichiometry == null) {
      throw new IllegalArgumentException(
          "Reaction prediction follow-up stoichiometry must not be null.");
    }
    final ArrayList<ReactionPredictionFollowUpStep> steps =
        new ArrayList<ReactionPredictionFollowUpStep>();
    int order = 1;
    if (!result.hasCandidates() && result.hasMechanismChannels()) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION,
              ReactionPredictionFollowUpUrgency.REQUIRED,
              "Reaction mechanisms were recognized but no explicit product graph was materialized.",
              "Apply graph transforms, seed products or drawing-captured product sides before electronic or quantum follow-up.");
    } else if (!result.hasCandidates()) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.MECHANISM_ENUMERATION,
              ReactionPredictionFollowUpUrgency.REQUIRED,
              "No product hypothesis was generated from reactants and conditions.",
              "Enumerate elementary mechanism families before any yield or quantum calculation.");
    }
    if (result.hasCandidates() && result.hasMechanismChannels()) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION,
              ReactionPredictionFollowUpUrgency.RECOMMENDED,
              "Some recognized mechanism channels are not yet represented by explicit product graphs.",
              "Materialize missing channels so product probabilities, impurity ranking and quantum follow-up cover the full route map.");
    }
    if (applicability.missingCriticalConditionCount() > 0) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.REACTION_CONDITION_SCREENING,
              ReactionPredictionFollowUpUrgency.REQUIRED,
              "Reaction record misses critical condition context.",
              "Resolve temperature, time, solvent, base/catalyst or loading before trusting probabilities.");
    }
    if (ReactionPredictionFollowUpPlanner.hasMechanismConditionConflict(result)) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.REACTION_CONDITION_SCREENING,
              ReactionPredictionFollowUpUrgency.RECOMMENDED,
              "At least one candidate mechanism is penalized by the declared reaction-condition profile.",
              "Screen solvent, catalyst, acid/base, redox, radical and thermal regimes before treating the product distribution as final.");
    }
    if (!stoichiometry.hasMatchedQuantities()) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.CONCENTRATION_SCREENING,
              ReactionPredictionFollowUpUrgency.RECOMMENDED,
              "Reactant quantities were not matched to molecular participants.",
              "Compute equivalent ratios and concentration windows for realistic product distribution.");
    } else if (stoichiometry.hasEquivalentExcess(2.0)) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.CONCENTRATION_SCREENING,
              ReactionPredictionFollowUpUrgency.RECOMMENDED,
              "At least one reactant is present in strong excess.",
              "Check whether excess reagent shifts selectivity, quench behavior or side-product formation.");
    }
    if (applicability.readinessLevel()
            == ReactionPredictionReadinessLevel.COMPETING_PATHWAYS_REQUIRE_RERANK
        || applicability.closeCompetitorCount() > 0) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS,
              ReactionPredictionFollowUpUrgency.REQUIRED,
              "Product distribution contains close or risky competing channels.",
              "Separate kinetically plausible products before selecting a main route.");
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.REACTION_PATH,
              ReactionPredictionFollowUpUrgency.RECOMMENDED,
              "Competing candidates need a mechanistic barrier check.",
              "Compare elementary pathway barriers, intermediates and transition-state ordering.");
    }
    if (applicability.electronicRerankRecommended()) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.CONFORMER_SEARCH,
              ReactionPredictionFollowUpUrgency.RECOMMENDED,
              "Electronic and steric selectivity are important for this substrate set.",
              "Generate relevant conformers before single-point or reaction-energy reranking.");
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.REACTION_ENERGY,
              ReactionPredictionFollowUpUrgency.REQUIRED,
              "Prediction needs Java-native electronic reranking for reliable ordering.",
              "Compute reaction energies for top candidates and re-normalize probabilities.");
    }
    if (applicability.quantumFollowupRecommended()) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.ACTIVE_SPACE_SELECTION,
              ReactionPredictionFollowUpUrgency.RECOMMENDED,
              "Heterocycles, radicals or electron-poor aromatic systems may need correlated treatment.",
              "Resolve active orbitals automatically and allow an explicit chemist override for sensitive fragments.");
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.HAMILTONIAN_PREPARATION,
              ReactionPredictionFollowUpUrgency.RECOMMENDED,
              "Quantum follow-up is useful for the electronically sensitive part of the mechanism.",
              "Prepare fermionic/qubit Hamiltonian for candidate fragments or transition-state models.");
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION,
              ReactionPredictionFollowUpUrgency.OPTIONAL,
              "Prepared Hamiltonian can be converted into the project Quantum IR.",
              "Generate Quantum IR programs for VQE/estimation with the resolved active space.");
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION,
              ReactionPredictionFollowUpUrgency.OPTIONAL,
              "Prepared Hamiltonian and Quantum IR can be evaluated by the local Java-native VQE loop.",
              "Run VQE to obtain an executable quantum-energy reference and compare it with classical reranking.");
    }
    if (result.hasCandidates()) {
      order =
          ReactionPredictionFollowUpPlanner.add(
              steps,
              order,
              ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION,
              ReactionPredictionFollowUpUrgency.OPTIONAL,
              "Ranked products are available but practical yield still depends on workup and scale.",
              "Estimate yield range, impurity pressure and isolation risk.");
    }
    return ReactionPredictionFollowUpPlan.of(steps);
  }

  private static boolean hasMechanismConditionConflict(final ReactionPredictionResult result) {
    final List<ReactionProductCandidate> candidates = result.candidates();
    for (int i = 0; i < candidates.size(); ++i) {
      final List<ReactionPredictionEvidence> evidence = candidates.get(i).evidence();
      for (int j = 0; j < evidence.size(); ++j) {
        if (evidence.get(j).description().startsWith("Condition mechanism profile penalizes ")) {
          return true;
        }
      }
    }
    return false;
  }

  private static int add(
      final ArrayList<ReactionPredictionFollowUpStep> steps,
      final int order,
      final ClassicalCalculationKind kind,
      final ReactionPredictionFollowUpUrgency urgency,
      final String reason,
      final String expectedDecision) {
    steps.add(ReactionPredictionFollowUpStep.of(order, kind, urgency, reason, expectedDecision));
    return order + 1;
  }
}
