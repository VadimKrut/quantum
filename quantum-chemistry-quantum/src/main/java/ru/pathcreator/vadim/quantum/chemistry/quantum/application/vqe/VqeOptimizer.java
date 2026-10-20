/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe;

import java.util.ArrayList;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ansatz.VariationalChemistryProgramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.mitigation.ErrorMitigationPlanner;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeConvergenceStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeIteration;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Выполняет backend-neutral VQE цикл с convergence control, gradient probe и error mitigation planning.
 */
public final class VqeOptimizer {

  private final VariationalChemistryProgramGenerator programGenerator;
  private final ErrorMitigationPlanner mitigationPlanner;
  private final VqeEnergyBackend backend;

  public VqeOptimizer(final VqeEnergyBackend backend) {
    this(new VariationalChemistryProgramGenerator(), new ErrorMitigationPlanner(), backend);
  }

  public VqeOptimizer(
      final VariationalChemistryProgramGenerator programGenerator,
      final ErrorMitigationPlanner mitigationPlanner,
      final VqeEnergyBackend backend) {
    if (programGenerator == null) {
      throw new IllegalArgumentException("VQE program generator must not be null.");
    }
    if (mitigationPlanner == null) {
      throw new IllegalArgumentException("VQE mitigation planner must not be null.");
    }
    if (backend == null) {
      throw new IllegalArgumentException("VQE backend must not be null.");
    }
    this.programGenerator = programGenerator;
    this.mitigationPlanner = mitigationPlanner;
    this.backend = backend;
  }

  public VqeOptimizationResult optimize(final VqeOptimizationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("VQE optimization request must not be null.");
    }
    final QuantumProgram program =
        this.programGenerator.generate(request.activeSpace(), request.ansatzOptions());
    final ErrorMitigationPlan mitigationPlan =
        this.mitigationPlanner.plan(request.mitigationRequest());
    final VqeOptimizationOptions options = request.optimizationOptions();
    final int initialShots =
        this.shotsForIteration(
            Math.max(options.baseShots(), mitigationPlan.recommendedShots()), options, 0);
    final double[] parameters = request.initialParametersCopy();
    final ArrayList<VqeIteration> iterations = new ArrayList<VqeIteration>();
    BackendEvaluation current =
        this.evaluateWithRetries(
            program, parameters.clone(), initialShots, mitigationPlan, options);
    long evaluationCount = current.evaluationCount();
    long failedEvaluationCount = current.failedEvaluationCount();
    long totalShots = current.totalShots();
    if (!current.successful()) {
      return VqeOptimizationResult.of(
          VqeConvergenceStatus.BACKEND_FAILED,
          program,
          mitigationPlan,
          Double.MAX_VALUE,
          parameters,
          java.util.List.of(VqeIteration.of(0, Double.MAX_VALUE, 0.0, 0.0, false, parameters)),
          evaluationCount,
          failedEvaluationCount,
          totalShots,
          current.evaluation().diagnostic());
    }
    double bestEnergy = current.evaluation().energy();
    double previousEnergy = bestEnergy;
    double step = options.initialStep();
    GradientProbeResult gradientProbe =
        this.probeGradient(
            program,
            mitigationPlan,
            initialShots,
            parameters,
            options.gradientProbeStep(),
            options);
    evaluationCount += gradientProbe.evaluationCount();
    failedEvaluationCount += gradientProbe.failedEvaluationCount();
    totalShots += gradientProbe.totalShots();
    iterations.add(
        VqeIteration.of(
            0,
            current.evaluation().energy(),
            current.evaluation().uncertainty(),
            step,
            gradientProbe.gradientNorm(),
            true,
            parameters));
    for (int iteration = 1; iteration <= options.maxIterations(); ++iteration) {
      final int iterationShots = this.shotsForIteration(initialShots, options, iteration);
      final SearchResult searchResult =
          this.searchCoordinateStep(
              program, mitigationPlan, iterationShots, parameters, bestEnergy, step, options);
      evaluationCount += searchResult.evaluationCount();
      failedEvaluationCount += searchResult.failedEvaluationCount();
      totalShots += searchResult.totalShots();
      if (searchResult.accepted()) {
        bestEnergy = searchResult.energy();
        System.arraycopy(searchResult.parameters(), 0, parameters, 0, parameters.length);
      } else {
        step *= 0.5;
      }
      gradientProbe =
          this.probeGradient(
              program,
              mitigationPlan,
              iterationShots,
              parameters,
              options.gradientProbeStep(),
              options);
      evaluationCount += gradientProbe.evaluationCount();
      failedEvaluationCount += gradientProbe.failedEvaluationCount();
      totalShots += gradientProbe.totalShots();
      iterations.add(
          VqeIteration.of(
              iteration,
              bestEnergy,
              searchResult.uncertainty(),
              step,
              gradientProbe.gradientNorm(),
              searchResult.accepted(),
              parameters));
      if (Math.abs(previousEnergy - bestEnergy) <= options.energyTolerance()
          && step <= options.parameterTolerance()
          && gradientProbe.successful()
          && gradientProbe.gradientNorm() <= options.gradientTolerance()) {
        return VqeOptimizationResult.of(
            VqeConvergenceStatus.CONVERGED,
            program,
            mitigationPlan,
            bestEnergy,
            parameters,
            iterations,
            evaluationCount,
            failedEvaluationCount,
            totalShots,
            "VQE converged by energy, parameter and gradient tolerances.");
      }
      if (step <= options.minimumStep()
          && gradientProbe.successful()
          && gradientProbe.gradientNorm() <= options.gradientTolerance()) {
        return VqeOptimizationResult.of(
            VqeConvergenceStatus.CONVERGED,
            program,
            mitigationPlan,
            bestEnergy,
            parameters,
            iterations,
            evaluationCount,
            failedEvaluationCount,
            totalShots,
            "VQE converged because optimizer step reached minimum step and gradient tolerance.");
      }
      previousEnergy = bestEnergy;
    }
    return VqeOptimizationResult.of(
        VqeConvergenceStatus.MAX_ITERATIONS_REACHED,
        program,
        mitigationPlan,
        bestEnergy,
        parameters,
        iterations,
        evaluationCount,
        failedEvaluationCount,
        totalShots,
        "VQE stopped at max iteration count.");
  }

  private GradientProbeResult probeGradient(
      final QuantumProgram program,
      final ErrorMitigationPlan mitigationPlan,
      final int shots,
      final double[] currentParameters,
      final double probeStep,
      final VqeOptimizationOptions options) {
    double squaredNorm = 0.0;
    long evaluationCount = 0L;
    long failedEvaluationCount = 0L;
    long totalShots = 0L;
    boolean successful = true;
    for (int parameterIndex = 0; parameterIndex < currentParameters.length; ++parameterIndex) {
      final double[] positiveParameters = currentParameters.clone();
      final double[] negativeParameters = currentParameters.clone();
      positiveParameters[parameterIndex] += probeStep;
      negativeParameters[parameterIndex] -= probeStep;
      final BackendEvaluation positive =
          this.evaluateWithRetries(program, positiveParameters, shots, mitigationPlan, options);
      final BackendEvaluation negative =
          this.evaluateWithRetries(program, negativeParameters, shots, mitigationPlan, options);
      evaluationCount += positive.evaluationCount() + negative.evaluationCount();
      failedEvaluationCount += positive.failedEvaluationCount() + negative.failedEvaluationCount();
      totalShots += positive.totalShots() + negative.totalShots();
      if (!positive.successful() || !negative.successful()) {
        successful = false;
        continue;
      }
      final double gradient =
          (positive.evaluation().energy() - negative.evaluation().energy()) / (2.0 * probeStep);
      squaredNorm += gradient * gradient;
    }
    return GradientProbeResult.of(
        successful,
        successful ? Math.sqrt(squaredNorm) : Double.NaN,
        evaluationCount,
        failedEvaluationCount,
        totalShots);
  }

  private SearchResult searchCoordinateStep(
      final QuantumProgram program,
      final ErrorMitigationPlan mitigationPlan,
      final int shots,
      final double[] currentParameters,
      final double currentEnergy,
      final double step,
      final VqeOptimizationOptions options) {
    double bestEnergy = currentEnergy;
    double bestUncertainty = 0.0;
    double[] bestParameters = currentParameters.clone();
    boolean accepted = false;
    long evaluationCount = 0L;
    long failedEvaluationCount = 0L;
    long totalShots = 0L;
    for (int parameterIndex = 0; parameterIndex < currentParameters.length; ++parameterIndex) {
      final ParameterSearchResult parameterSearch =
          this.searchParameterStep(
              program,
              mitigationPlan,
              shots,
              currentParameters,
              parameterIndex,
              currentEnergy,
              step,
              options);
      evaluationCount += parameterSearch.evaluationCount();
      failedEvaluationCount += parameterSearch.failedEvaluationCount();
      totalShots += parameterSearch.totalShots();
      if (!parameterSearch.accepted()) {
        continue;
      }
      if (parameterSearch.energy() < bestEnergy) {
        bestEnergy = parameterSearch.energy();
        bestUncertainty = parameterSearch.uncertainty();
        bestParameters = parameterSearch.parameters();
        accepted = true;
      }
    }
    return SearchResult.of(
        bestEnergy,
        bestUncertainty,
        bestParameters,
        accepted,
        evaluationCount,
        failedEvaluationCount,
        totalShots);
  }

  private ParameterSearchResult searchParameterStep(
      final QuantumProgram program,
      final ErrorMitigationPlan mitigationPlan,
      final int shots,
      final double[] currentParameters,
      final int parameterIndex,
      final double currentEnergy,
      final double step,
      final VqeOptimizationOptions options) {
    long evaluationCount = 0L;
    long failedEvaluationCount = 0L;
    long totalShots = 0L;
    double bestEnergy = currentEnergy;
    double bestUncertainty = 0.0;
    double[] bestParameters = currentParameters.clone();
    boolean accepted = false;
    final double[] negativeParameters = currentParameters.clone();
    negativeParameters[parameterIndex] -= step;
    final BackendEvaluation negative =
        this.evaluateWithRetries(program, negativeParameters, shots, mitigationPlan, options);
    evaluationCount += negative.evaluationCount();
    failedEvaluationCount += negative.failedEvaluationCount();
    totalShots += negative.totalShots();
    if (negative.successful() && negative.evaluation().energy() < bestEnergy) {
      bestEnergy = negative.evaluation().energy();
      bestUncertainty = negative.evaluation().uncertainty();
      bestParameters = negativeParameters;
      accepted = true;
    }
    final double[] positiveParameters = currentParameters.clone();
    positiveParameters[parameterIndex] += step;
    final BackendEvaluation positive =
        this.evaluateWithRetries(program, positiveParameters, shots, mitigationPlan, options);
    evaluationCount += positive.evaluationCount();
    failedEvaluationCount += positive.failedEvaluationCount();
    totalShots += positive.totalShots();
    if (positive.successful() && positive.evaluation().energy() < bestEnergy) {
      bestEnergy = positive.evaluation().energy();
      bestUncertainty = positive.evaluation().uncertainty();
      bestParameters = positiveParameters;
      accepted = true;
    }
    final double candidateStep =
        VqeOptimizer.quadraticMinimumStep(
            negative.successful() ? negative.evaluation().energy() : Double.NaN,
            currentEnergy,
            positive.successful() ? positive.evaluation().energy() : Double.NaN,
            step);
    if (candidateStep != 0.0 && Math.abs(Math.abs(candidateStep) - step) > step * 1.0E-9) {
      final double[] quadraticParameters = currentParameters.clone();
      quadraticParameters[parameterIndex] += candidateStep;
      final BackendEvaluation quadratic =
          this.evaluateWithRetries(program, quadraticParameters, shots, mitigationPlan, options);
      evaluationCount += quadratic.evaluationCount();
      failedEvaluationCount += quadratic.failedEvaluationCount();
      totalShots += quadratic.totalShots();
      if (quadratic.successful() && quadratic.evaluation().energy() < bestEnergy) {
        bestEnergy = quadratic.evaluation().energy();
        bestUncertainty = quadratic.evaluation().uncertainty();
        bestParameters = quadraticParameters;
        accepted = true;
      }
    }
    return ParameterSearchResult.of(
        bestEnergy,
        bestUncertainty,
        bestParameters,
        accepted,
        evaluationCount,
        failedEvaluationCount,
        totalShots);
  }

  private static double quadraticMinimumStep(
      final double negativeEnergy,
      final double centerEnergy,
      final double positiveEnergy,
      final double step) {
    if (!Double.isFinite(negativeEnergy)
        || !Double.isFinite(centerEnergy)
        || !Double.isFinite(positiveEnergy)) {
      return 0.0;
    }
    final double curvature = positiveEnergy + negativeEnergy - 2.0 * centerEnergy;
    if (!Double.isFinite(curvature) || curvature <= 0.0) {
      return 0.0;
    }
    final double gradientTimesStep = positiveEnergy - negativeEnergy;
    final double candidate = -0.5 * step * gradientTimesStep / curvature;
    if (!Double.isFinite(candidate)) {
      return 0.0;
    }
    if (candidate >= step) {
      return step;
    }
    if (candidate <= -step) {
      return -step;
    }
    return candidate;
  }

  private BackendEvaluation evaluateWithRetries(
      final QuantumProgram program,
      final double[] parameters,
      final int shots,
      final ErrorMitigationPlan mitigationPlan,
      final VqeOptimizationOptions options) {
    VqeEnergyEvaluation lastEvaluation = null;
    long evaluationCount = 0L;
    long failedEvaluationCount = 0L;
    long totalShots = 0L;
    for (int attempt = 0; attempt <= options.maxBackendRetries(); ++attempt) {
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

  private int shotsForIteration(
      final int initialShots, final VqeOptimizationOptions options, final int iteration) {
    double grownShots = (double) initialShots;
    for (int i = 0; i < iteration; ++i) {
      grownShots *= options.shotGrowthFactor();
      if (grownShots >= options.maxShots()) {
        return options.maxShots();
      }
    }
    return Math.min(options.maxShots(), Math.max(initialShots, (int) Math.ceil(grownShots)));
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
        throw new IllegalArgumentException("VQE backend evaluation must not be null.");
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

  private static final class SearchResult {

    private final double energy;
    private final double uncertainty;
    private final double[] parameters;
    private final boolean accepted;
    private final long evaluationCount;
    private final long failedEvaluationCount;
    private final long totalShots;

    private SearchResult(
        final double energy,
        final double uncertainty,
        final double[] parameters,
        final boolean accepted,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots) {
      this.energy = energy;
      this.uncertainty = uncertainty;
      this.parameters = parameters;
      this.accepted = accepted;
      this.evaluationCount = evaluationCount;
      this.failedEvaluationCount = failedEvaluationCount;
      this.totalShots = totalShots;
    }

    private static SearchResult of(
        final double energy,
        final double uncertainty,
        final double[] parameters,
        final boolean accepted,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots) {
      return new SearchResult(
          energy,
          uncertainty,
          parameters.clone(),
          accepted,
          evaluationCount,
          failedEvaluationCount,
          totalShots);
    }

    private double energy() {
      return this.energy;
    }

    private double uncertainty() {
      return this.uncertainty;
    }

    private double[] parameters() {
      return this.parameters.clone();
    }

    private boolean accepted() {
      return this.accepted;
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

  private static final class ParameterSearchResult {

    private final double energy;
    private final double uncertainty;
    private final double[] parameters;
    private final boolean accepted;
    private final long evaluationCount;
    private final long failedEvaluationCount;
    private final long totalShots;

    private ParameterSearchResult(
        final double energy,
        final double uncertainty,
        final double[] parameters,
        final boolean accepted,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots) {
      this.energy = energy;
      this.uncertainty = uncertainty;
      this.parameters = parameters;
      this.accepted = accepted;
      this.evaluationCount = evaluationCount;
      this.failedEvaluationCount = failedEvaluationCount;
      this.totalShots = totalShots;
    }

    private static ParameterSearchResult of(
        final double energy,
        final double uncertainty,
        final double[] parameters,
        final boolean accepted,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots) {
      return new ParameterSearchResult(
          energy,
          uncertainty,
          parameters.clone(),
          accepted,
          evaluationCount,
          failedEvaluationCount,
          totalShots);
    }

    private double energy() {
      return this.energy;
    }

    private double uncertainty() {
      return this.uncertainty;
    }

    private double[] parameters() {
      return this.parameters.clone();
    }

    private boolean accepted() {
      return this.accepted;
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

  private static final class GradientProbeResult {

    private final boolean successful;
    private final double gradientNorm;
    private final long evaluationCount;
    private final long failedEvaluationCount;
    private final long totalShots;

    private GradientProbeResult(
        final boolean successful,
        final double gradientNorm,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots) {
      this.successful = successful;
      this.gradientNorm = gradientNorm;
      this.evaluationCount = evaluationCount;
      this.failedEvaluationCount = failedEvaluationCount;
      this.totalShots = totalShots;
    }

    private static GradientProbeResult of(
        final boolean successful,
        final double gradientNorm,
        final long evaluationCount,
        final long failedEvaluationCount,
        final long totalShots) {
      return new GradientProbeResult(
          successful, gradientNorm, evaluationCount, failedEvaluationCount, totalShots);
    }

    private boolean successful() {
      return this.successful;
    }

    private double gradientNorm() {
      return this.gradientNorm;
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
}
