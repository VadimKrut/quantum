/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Результат VQE цикла с лучшей энергией, параметрами, историей и mitigation plan.
 */
public final class VqeOptimizationResult {

  private final VqeConvergenceStatus status;
  private final QuantumProgram program;
  private final ErrorMitigationPlan mitigationPlan;
  private final double bestEnergy;
  private final double[] bestParameters;
  private final List<VqeIteration> iterations;
  private final long evaluationCount;
  private final long failedEvaluationCount;
  private final long totalShots;
  private final String diagnostic;

  private VqeOptimizationResult(
      final VqeConvergenceStatus status,
      final QuantumProgram program,
      final ErrorMitigationPlan mitigationPlan,
      final double bestEnergy,
      final double[] bestParameters,
      final List<VqeIteration> iterations,
      final long evaluationCount,
      final long failedEvaluationCount,
      final long totalShots,
      final String diagnostic) {
    this.status = status;
    this.program = program;
    this.mitigationPlan = mitigationPlan;
    this.bestEnergy = bestEnergy;
    this.bestParameters = bestParameters;
    this.iterations = iterations;
    this.evaluationCount = evaluationCount;
    this.failedEvaluationCount = failedEvaluationCount;
    this.totalShots = totalShots;
    this.diagnostic = diagnostic;
  }

  public static VqeOptimizationResult of(
      final VqeConvergenceStatus status,
      final QuantumProgram program,
      final ErrorMitigationPlan mitigationPlan,
      final double bestEnergy,
      final double[] bestParameters,
      final List<VqeIteration> iterations,
      final String diagnostic) {
    return VqeOptimizationResult.of(
        status,
        program,
        mitigationPlan,
        bestEnergy,
        bestParameters,
        iterations,
        iterations.size(),
        0L,
        VqeOptimizationResult.estimateShots(iterations, mitigationPlan.recommendedShots()),
        diagnostic);
  }

  public static VqeOptimizationResult of(
      final VqeConvergenceStatus status,
      final QuantumProgram program,
      final ErrorMitigationPlan mitigationPlan,
      final double bestEnergy,
      final double[] bestParameters,
      final List<VqeIteration> iterations,
      final long evaluationCount,
      final long failedEvaluationCount,
      final long totalShots,
      final String diagnostic) {
    if (status == null) {
      throw new IllegalArgumentException("VQE convergence status must not be null.");
    }
    if (program == null) {
      throw new IllegalArgumentException("VQE program must not be null.");
    }
    if (mitigationPlan == null) {
      throw new IllegalArgumentException("VQE mitigation plan must not be null.");
    }
    if (!Double.isFinite(bestEnergy)) {
      throw new IllegalArgumentException("VQE best energy must be finite.");
    }
    if (bestParameters == null || bestParameters.length == 0) {
      throw new IllegalArgumentException("VQE best parameters must not be empty.");
    }
    if (iterations == null || iterations.isEmpty()) {
      throw new IllegalArgumentException("VQE iteration history must not be empty.");
    }
    if (evaluationCount <= 0L) {
      throw new IllegalArgumentException("VQE evaluation count must be positive.");
    }
    if (failedEvaluationCount < 0L || failedEvaluationCount > evaluationCount) {
      throw new IllegalArgumentException("VQE failed evaluation count is outside of bounds.");
    }
    if (totalShots <= 0L) {
      throw new IllegalArgumentException("VQE total shots must be positive.");
    }
    if (diagnostic == null || diagnostic.isBlank()) {
      throw new IllegalArgumentException("VQE diagnostic must not be blank.");
    }
    return new VqeOptimizationResult(
        status,
        program,
        mitigationPlan,
        bestEnergy,
        bestParameters.clone(),
        List.copyOf(iterations),
        evaluationCount,
        failedEvaluationCount,
        totalShots,
        diagnostic);
  }

  public VqeConvergenceStatus status() {
    return this.status;
  }

  public QuantumProgram program() {
    return this.program;
  }

  public ErrorMitigationPlan mitigationPlan() {
    return this.mitigationPlan;
  }

  public double bestEnergy() {
    return this.bestEnergy;
  }

  public double[] bestParametersCopy() {
    return this.bestParameters.clone();
  }

  public List<VqeIteration> iterations() {
    return this.iterations;
  }

  public long evaluationCount() {
    return this.evaluationCount;
  }

  public long failedEvaluationCount() {
    return this.failedEvaluationCount;
  }

  public long totalShots() {
    return this.totalShots;
  }

  public String diagnostic() {
    return this.diagnostic;
  }

  public boolean converged() {
    return this.status == VqeConvergenceStatus.CONVERGED;
  }

  public boolean backendFailed() {
    return this.status == VqeConvergenceStatus.BACKEND_FAILED;
  }

  public boolean reachedMaxIterations() {
    return this.status == VqeConvergenceStatus.MAX_ITERATIONS_REACHED;
  }

  public boolean hasFailedEvaluations() {
    return this.failedEvaluationCount > 0L;
  }

  public double failedEvaluationRatio() {
    return (double) this.failedEvaluationCount / (double) this.evaluationCount;
  }

  public VqeIteration firstIteration() {
    return this.iterations.get(0);
  }

  public VqeIteration lastIteration() {
    return this.iterations.get(this.iterations.size() - 1);
  }

  public boolean lastGradientAvailable() {
    return !Double.isNaN(this.lastIteration().gradientNorm());
  }

  private static long estimateShots(final List<VqeIteration> iterations, final int shots) {
    return (long) iterations.size() * (long) shots;
  }
}
