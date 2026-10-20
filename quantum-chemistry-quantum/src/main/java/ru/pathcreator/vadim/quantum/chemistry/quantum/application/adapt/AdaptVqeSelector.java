/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.adapt;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ansatz.VariationalChemistryProgramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.mitigation.ErrorMitigationPlanner;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.VqeEnergyBackend;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeIteration;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionResult;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt.AdaptVqeSelectionStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitationPool;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Выполняет backend-neutral ADAPT-VQE отбор операторов из UCCSD pool.
 */
public final class AdaptVqeSelector {

  private final VariationalChemistryProgramGenerator programGenerator;
  private final ErrorMitigationPlanner mitigationPlanner;
  private final VqeEnergyBackend backend;

  public AdaptVqeSelector(final VqeEnergyBackend backend) {
    this(new VariationalChemistryProgramGenerator(), new ErrorMitigationPlanner(), backend);
  }

  public AdaptVqeSelector(
      final VariationalChemistryProgramGenerator programGenerator,
      final ErrorMitigationPlanner mitigationPlanner,
      final VqeEnergyBackend backend) {
    if (programGenerator == null) {
      throw new IllegalArgumentException("ADAPT-VQE program generator must not be null.");
    }
    if (mitigationPlanner == null) {
      throw new IllegalArgumentException("ADAPT-VQE mitigation planner must not be null.");
    }
    if (backend == null) {
      throw new IllegalArgumentException("ADAPT-VQE backend must not be null.");
    }
    this.programGenerator = programGenerator;
    this.mitigationPlanner = mitigationPlanner;
    this.backend = backend;
  }

  /**
   * Выбирает операторы ADAPT-VQE и строит итоговую Quantum IR-программу.
   *
   * @param activeSpace active-space задача
   * @param ansatzOptions настройки регистров и измерений
   * @param selectionOptions настройки отбора
   * @param mitigationRequest запрос mitigation-плана
   * @return структурированный результат отбора
   */
  public AdaptVqeSelectionResult select(
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions ansatzOptions,
      final AdaptVqeSelectionOptions selectionOptions,
      final ErrorMitigationRequest mitigationRequest) {
    requireInputs(activeSpace, ansatzOptions, selectionOptions, mitigationRequest);
    final ErrorMitigationPlan mitigationPlan = this.mitigationPlanner.plan(mitigationRequest);
    final UccsdExcitationPool pool = UccsdExcitationPool.spinConserving(activeSpace);
    final int candidateCount =
        Math.min(selectionOptions.maxCandidateOperators(), pool.excitationCount());
    final ArrayList<UccsdExcitation> candidatePool = new ArrayList<UccsdExcitation>(candidateCount);
    for (int i = 0; i < candidateCount; ++i) {
      candidatePool.add(pool.excitation(i));
    }
    final ArrayList<UccsdExcitation> selectedExcitations = new ArrayList<UccsdExcitation>();
    final ArrayList<AdaptVqeIteration> iterations = new ArrayList<AdaptVqeIteration>();
    final ArrayList<Double> selectedParameters = new ArrayList<Double>();
    final QuantumProgram referenceProgram =
        this.programGenerator.generateSelectedExcitations(activeSpace, ansatzOptions, List.of());
    BackendEvaluation current =
        evaluateWithRetries(
            referenceProgram,
            new double[0],
            selectionOptions.shots(),
            mitigationPlan,
            selectionOptions.maxBackendRetries());
    long evaluationCount = current.evaluationCount();
    long failedEvaluationCount = current.failedEvaluationCount();
    long totalShots = current.totalShots();
    if (!current.successful()) {
      return result(
          AdaptVqeSelectionStatus.BACKEND_FAILED,
          referenceProgram,
          mitigationPlan,
          Double.MAX_VALUE,
          Double.MAX_VALUE,
          selectedParameters,
          selectedExcitations,
          iterations,
          evaluationCount,
          failedEvaluationCount,
          totalShots,
          current.evaluation().diagnostic());
    }
    final double referenceEnergy = current.evaluation().energy();
    double bestEnergy = referenceEnergy;
    if (candidatePool.isEmpty()) {
      return result(
          AdaptVqeSelectionStatus.OPERATOR_POOL_EXHAUSTED,
          referenceProgram,
          mitigationPlan,
          referenceEnergy,
          bestEnergy,
          selectedParameters,
          selectedExcitations,
          iterations,
          evaluationCount,
          failedEvaluationCount,
          totalShots,
          "ADAPT-VQE operator pool is empty for the selected active space.");
    }
    for (int iterationIndex = 0;
        iterationIndex < selectionOptions.maxSelectedOperators();
        ++iterationIndex) {
      final CandidateSearchResult searchResult =
          searchBestCandidate(
              activeSpace,
              ansatzOptions,
              selectionOptions,
              mitigationPlan,
              candidatePool,
              selectedExcitations,
              selectedParameters,
              bestEnergy);
      evaluationCount += searchResult.evaluationCount();
      failedEvaluationCount += searchResult.failedEvaluationCount();
      totalShots += searchResult.totalShots();
      if (!searchResult.hasSuccessfulCandidate()) {
        return result(
            AdaptVqeSelectionStatus.BACKEND_FAILED,
            selectedProgram(activeSpace, ansatzOptions, selectedExcitations),
            mitigationPlan,
            referenceEnergy,
            bestEnergy,
            selectedParameters,
            selectedExcitations,
            iterations,
            evaluationCount,
            failedEvaluationCount,
            totalShots,
            searchResult.diagnostic());
      }
      if (!searchResult.accepted()) {
        iterations.add(
            AdaptVqeIteration.of(
                iterationIndex,
                searchResult.bestExcitation(),
                bestEnergy,
                searchResult.bestEnergy(),
                bestEnergy - searchResult.bestEnergy(),
                selectionOptions.probeAmplitude(),
                false,
                searchResult.successfulCandidateCount()));
        return result(
            AdaptVqeSelectionStatus.CONVERGED,
            selectedProgram(activeSpace, ansatzOptions, selectedExcitations),
            mitigationPlan,
            referenceEnergy,
            bestEnergy,
            selectedParameters,
            selectedExcitations,
            iterations,
            evaluationCount,
            failedEvaluationCount,
            totalShots,
            "ADAPT-VQE converged because no remaining operator reduced energy beyond tolerance.");
      }
      selectedExcitations.add(searchResult.bestExcitation());
      selectedParameters.add(Double.valueOf(selectionOptions.probeAmplitude()));
      candidatePool.remove(searchResult.bestExcitation());
      iterations.add(
          AdaptVqeIteration.of(
              iterationIndex,
              searchResult.bestExcitation(),
              bestEnergy,
              searchResult.bestEnergy(),
              bestEnergy - searchResult.bestEnergy(),
              selectionOptions.probeAmplitude(),
              true,
              searchResult.successfulCandidateCount()));
      bestEnergy = searchResult.bestEnergy();
      if (candidatePool.isEmpty()) {
        return result(
            AdaptVqeSelectionStatus.OPERATOR_POOL_EXHAUSTED,
            selectedProgram(activeSpace, ansatzOptions, selectedExcitations),
            mitigationPlan,
            referenceEnergy,
            bestEnergy,
            selectedParameters,
            selectedExcitations,
            iterations,
            evaluationCount,
            failedEvaluationCount,
            totalShots,
            "ADAPT-VQE consumed the available operator pool.");
      }
    }
    return result(
        AdaptVqeSelectionStatus.MAX_OPERATORS_SELECTED,
        selectedProgram(activeSpace, ansatzOptions, selectedExcitations),
        mitigationPlan,
        referenceEnergy,
        bestEnergy,
        selectedParameters,
        selectedExcitations,
        iterations,
        evaluationCount,
        failedEvaluationCount,
        totalShots,
        "ADAPT-VQE stopped at selected operator limit.");
  }

  private CandidateSearchResult searchBestCandidate(
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions ansatzOptions,
      final AdaptVqeSelectionOptions selectionOptions,
      final ErrorMitigationPlan mitigationPlan,
      final List<UccsdExcitation> candidatePool,
      final List<UccsdExcitation> selectedExcitations,
      final List<Double> selectedParameters,
      final double currentEnergy) {
    UccsdExcitation bestExcitation = null;
    double bestEnergy = Double.MAX_VALUE;
    long evaluationCount = 0L;
    long failedEvaluationCount = 0L;
    long totalShots = 0L;
    long successfulCandidateCount = 0L;
    String diagnostic = "No ADAPT-VQE candidate was evaluated.";
    for (int i = 0; i < candidatePool.size(); ++i) {
      final ArrayList<UccsdExcitation> trialExcitations =
          new ArrayList<UccsdExcitation>(selectedExcitations);
      trialExcitations.add(candidatePool.get(i));
      final QuantumProgram trialProgram =
          this.programGenerator.generateSelectedExcitations(
              activeSpace, ansatzOptions, trialExcitations);
      final double[] trialParameters =
          parametersWithCandidate(selectedParameters, selectionOptions.probeAmplitude());
      final BackendEvaluation evaluation =
          evaluateWithRetries(
              trialProgram,
              trialParameters,
              selectionOptions.shots(),
              mitigationPlan,
              selectionOptions.maxBackendRetries());
      evaluationCount += evaluation.evaluationCount();
      failedEvaluationCount += evaluation.failedEvaluationCount();
      totalShots += evaluation.totalShots();
      if (!evaluation.successful()) {
        diagnostic = evaluation.evaluation().diagnostic();
        continue;
      }
      ++successfulCandidateCount;
      if (evaluation.evaluation().energy() >= bestEnergy) {
        continue;
      }
      bestEnergy = evaluation.evaluation().energy();
      bestExcitation = candidatePool.get(i);
      diagnostic = "ok";
    }
    return CandidateSearchResult.of(
        bestExcitation,
        bestEnergy,
        currentEnergy - bestEnergy > selectionOptions.energyImprovementTolerance(),
        successfulCandidateCount,
        evaluationCount,
        failedEvaluationCount,
        totalShots,
        diagnostic);
  }

  private BackendEvaluation evaluateWithRetries(
      final QuantumProgram program,
      final double[] parameters,
      final int shots,
      final ErrorMitigationPlan mitigationPlan,
      final int maxBackendRetries) {
    VqeEnergyEvaluation lastEvaluation = null;
    long evaluationCount = 0L;
    long failedEvaluationCount = 0L;
    long totalShots = 0L;
    for (int attempt = 0; attempt <= maxBackendRetries; ++attempt) {
      lastEvaluation = this.backend.evaluate(program, parameters.clone(), shots, mitigationPlan);
      ++evaluationCount;
      totalShots += shots;
      if (lastEvaluation.successful()) {
        return BackendEvaluation.of(
            lastEvaluation, evaluationCount, failedEvaluationCount, totalShots);
      }
      ++failedEvaluationCount;
    }
    return BackendEvaluation.of(lastEvaluation, evaluationCount, failedEvaluationCount, totalShots);
  }

  private QuantumProgram selectedProgram(
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions ansatzOptions,
      final List<UccsdExcitation> selectedExcitations) {
    return this.programGenerator.generateSelectedExcitations(
        activeSpace, ansatzOptions, selectedExcitations);
  }

  private static AdaptVqeSelectionResult result(
      final AdaptVqeSelectionStatus status,
      final QuantumProgram program,
      final ErrorMitigationPlan mitigationPlan,
      final double referenceEnergy,
      final double bestEnergy,
      final List<Double> selectedParameters,
      final List<UccsdExcitation> selectedExcitations,
      final List<AdaptVqeIteration> iterations,
      final long evaluationCount,
      final long failedEvaluationCount,
      final long totalShots,
      final String diagnostic) {
    return AdaptVqeSelectionResult.of(
        status,
        program,
        mitigationPlan,
        referenceEnergy,
        bestEnergy,
        doubleArray(selectedParameters),
        selectedExcitations,
        iterations,
        evaluationCount,
        failedEvaluationCount,
        totalShots,
        diagnostic);
  }

  private static double[] parametersWithCandidate(
      final List<Double> selectedParameters, final double candidateAmplitude) {
    final double[] parameters = new double[selectedParameters.size() + 1];
    for (int i = 0; i < selectedParameters.size(); ++i) {
      parameters[i] = selectedParameters.get(i).doubleValue();
    }
    parameters[parameters.length - 1] = candidateAmplitude;
    return parameters;
  }

  private static double[] doubleArray(final List<Double> selectedParameters) {
    final double[] parameters = new double[selectedParameters.size()];
    for (int i = 0; i < selectedParameters.size(); ++i) {
      parameters[i] = selectedParameters.get(i).doubleValue();
    }
    return parameters;
  }

  private static void requireInputs(
      final ActiveSpace activeSpace,
      final VariationalChemistryOptions ansatzOptions,
      final AdaptVqeSelectionOptions selectionOptions,
      final ErrorMitigationRequest mitigationRequest) {
    if (activeSpace == null) {
      throw new IllegalArgumentException("ADAPT-VQE active space must not be null.");
    }
    if (ansatzOptions == null) {
      throw new IllegalArgumentException("ADAPT-VQE ansatz options must not be null.");
    }
    if (ansatzOptions.ansatzKind() != ChemistryAnsatzKind.ADAPT_VQE_OPERATOR_POOL) {
      throw new IllegalArgumentException(
          "ADAPT-VQE selection requires ADAPT_VQE_OPERATOR_POOL ansatz kind.");
    }
    if (selectionOptions == null) {
      throw new IllegalArgumentException("ADAPT-VQE selection options must not be null.");
    }
    if (mitigationRequest == null) {
      throw new IllegalArgumentException("ADAPT-VQE mitigation request must not be null.");
    }
    if (!activeSpace.equals(mitigationRequest.activeSpace())) {
      throw new IllegalArgumentException(
          "ADAPT-VQE mitigation active space must match selected active space.");
    }
  }

  private static final class BackendEvaluation {

    private final VqeEnergyEvaluation evaluation;
    private final long evaluationCount;
    private final long failedEvaluationCount;
    private final long totalShots;

    private BackendEvaluation(
        final VqeEnergyEvaluation evaluation,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots) {
      this.evaluation = evaluation;
      this.evaluationCount = evaluationCount;
      this.failedEvaluationCount = failedEvaluationCount;
      this.totalShots = totalShots;
    }

    private static BackendEvaluation of(
        final VqeEnergyEvaluation evaluation,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots) {
      if (evaluation == null) {
        throw new IllegalArgumentException("ADAPT-VQE backend evaluation must not be null.");
      }
      return new BackendEvaluation(evaluation, evaluationCount, failedEvaluationCount, totalShots);
    }

    private VqeEnergyEvaluation evaluation() {
      return this.evaluation;
    }

    private boolean successful() {
      return this.evaluation.successful();
    }

    private long evaluationCount() {
      return this.evaluationCount;
    }

    private long failedEvaluationCount() {
      return this.failedEvaluationCount;
    }

    private long totalShots() {
      return this.totalShots;
    }
  }

  private static final class CandidateSearchResult {

    private final UccsdExcitation bestExcitation;
    private final double bestEnergy;
    private final boolean accepted;
    private final long successfulCandidateCount;
    private final long evaluationCount;
    private final long failedEvaluationCount;
    private final long totalShots;
    private final String diagnostic;

    private CandidateSearchResult(
        final UccsdExcitation bestExcitation,
        final double bestEnergy,
        final boolean accepted,
        final long successfulCandidateCount,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots,
        final String diagnostic) {
      this.bestExcitation = bestExcitation;
      this.bestEnergy = bestEnergy;
      this.accepted = accepted;
      this.successfulCandidateCount = successfulCandidateCount;
      this.evaluationCount = evaluationCount;
      this.failedEvaluationCount = failedEvaluationCount;
      this.totalShots = totalShots;
      this.diagnostic = diagnostic;
    }

    private static CandidateSearchResult of(
        final UccsdExcitation bestExcitation,
        final double bestEnergy,
        final boolean accepted,
        final long successfulCandidateCount,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots,
        final String diagnostic) {
      final double checkedEnergy = bestExcitation == null ? Double.MAX_VALUE : bestEnergy;
      return new CandidateSearchResult(
          bestExcitation,
          checkedEnergy,
          accepted && bestExcitation != null,
          successfulCandidateCount,
          evaluationCount,
          failedEvaluationCount,
          totalShots,
          diagnostic);
    }

    private UccsdExcitation bestExcitation() {
      return this.bestExcitation;
    }

    private double bestEnergy() {
      return this.bestEnergy;
    }

    private boolean accepted() {
      return this.accepted;
    }

    private boolean hasSuccessfulCandidate() {
      return this.successfulCandidateCount > 0L;
    }

    private long successfulCandidateCount() {
      return this.successfulCandidateCount;
    }

    private long evaluationCount() {
      return this.evaluationCount;
    }

    private long failedEvaluationCount() {
      return this.failedEvaluationCount;
    }

    private long totalShots() {
      return this.totalShots;
    }

    private String diagnostic() {
      return this.diagnostic;
    }
  }
}
