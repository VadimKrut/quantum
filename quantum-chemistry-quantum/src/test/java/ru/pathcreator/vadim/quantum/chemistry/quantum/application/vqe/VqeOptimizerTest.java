/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationTechnique;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModel;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModelKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeConvergenceStatus;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

final class VqeOptimizerTest {

  @Test
  void coordinateSearchVqeImprovesEnergyAndCarriesMitigationPlan() {
    final VqeOptimizer optimizer = new VqeOptimizer(new QuadraticEnergyBackend());
    final VqeOptimizationRequest request =
        VqeOptimizationRequest.of(
            ActiveSpace.of(2, 2),
            VariationalChemistryOptions.of(
                ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE,
                "vqe_loop",
                "q",
                "c",
                "theta",
                1,
                4,
                true),
            new double[] {0.0, 0.0},
            VqeOptimizationOptions.of(32, 256, 0.25, 1.0E-4, 1.0E-8, 1.0E-4),
            ErrorMitigationRequest.of(
                ActiveSpace.of(2, 2),
                QuantumNoiseModel.of(QuantumNoiseModelKind.DEPOLARIZING, 0.001, 0.02, 0.03),
                List.of(
                    ErrorMitigationTechnique.READOUT_CALIBRATION,
                    ErrorMitigationTechnique.ZERO_NOISE_EXTRAPOLATION,
                    ErrorMitigationTechnique.SYMMETRY_VERIFICATION),
                256,
                true,
                false));

    final VqeOptimizationResult result = optimizer.optimize(request);

    Assertions.assertTrue(
        result.status() == VqeConvergenceStatus.CONVERGED
            || result.status() == VqeConvergenceStatus.MAX_ITERATIONS_REACHED);
    Assertions.assertTrue(result.bestEnergy() < 1.0);
    Assertions.assertTrue(result.iterations().size() > 1);
    Assertions.assertTrue(result.mitigationPlan().recommendedShots() >= 256);
    Assertions.assertTrue(result.evaluationCount() >= result.iterations().size());
    Assertions.assertEquals(0L, result.failedEvaluationCount());
    Assertions.assertTrue(result.totalShots() >= result.evaluationCount() * 256L);
    Assertions.assertTrue(result.program().circuitCount() > 0);
    Assertions.assertFalse(result.backendFailed());
    Assertions.assertFalse(result.hasFailedEvaluations());
    Assertions.assertEquals(0.0, result.failedEvaluationRatio());
    Assertions.assertTrue(result.lastGradientAvailable());
    Assertions.assertEquals(result.iterations().get(0), result.firstIteration());
    Assertions.assertEquals(
        result.iterations().get(result.iterations().size() - 1), result.lastIteration());
    Assertions.assertTrue(Double.isFinite(result.lastIteration().gradientNorm()));
    Assertions.assertTrue(
        result
            .mitigationPlan()
            .techniques()
            .contains(ErrorMitigationTechnique.READOUT_CALIBRATION));
  }

  @Test
  void vqeUsesFiniteDifferenceGradientAsConvergenceEvidence() {
    final VqeOptimizer optimizer =
        new VqeOptimizer(
            (program, parameters, shots, mitigationPlan) ->
                VqeEnergyEvaluation.success(parameters[0] * parameters[0], 0.0, shots));
    final VqeOptimizationResult result =
        optimizer.optimize(
            VqeOptimizationRequest.of(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.defaults(),
                new double[] {0.0},
                VqeOptimizationOptions.of(16, 256, 0.002, 1.0E-5, 1.0E-12, 0.001, 1.0E-3, 1.0E-8),
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.ideal(),
                    List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                    128,
                    false,
                    false)));

    Assertions.assertEquals(VqeConvergenceStatus.CONVERGED, result.status());
    Assertions.assertTrue(result.converged());
    Assertions.assertFalse(result.reachedMaxIterations());
    Assertions.assertTrue(result.diagnostic().contains("gradient"));
    Assertions.assertEquals(0.0, result.bestEnergy(), 1.0E-15);
    Assertions.assertEquals(0.0, result.lastIteration().gradientNorm(), 1.0E-12);
    Assertions.assertTrue(result.evaluationCount() > result.iterations().size());
  }

  @Test
  void vqeUsesInteriorQuadraticLineSearchForParameterMinimum() {
    final VqeOptimizer optimizer =
        new VqeOptimizer(
            (program, parameters, shots, mitigationPlan) ->
                VqeEnergyEvaluation.success(
                    -2.0 + (parameters[0] - 0.17) * (parameters[0] - 0.17), 0.0, shots));

    final VqeOptimizationResult result =
        optimizer.optimize(
            VqeOptimizationRequest.of(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.defaults(),
                new double[] {0.0},
                VqeOptimizationOptions.of(8, 256, 0.5, 1.0E-5, 1.0E-12, 1.0E-6),
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.ideal(),
                    List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                    128,
                    false,
                    false)));

    Assertions.assertTrue(result.bestEnergy() < -1.999999);
    Assertions.assertEquals(0.17, result.bestParametersCopy()[0], 1.0E-5);
    Assertions.assertTrue(result.evaluationCount() >= 4L);
  }

  @Test
  void vqeUsesBaseShotBudgetWhenItExceedsMitigationRecommendation() {
    final VqeOptimizer optimizer = new VqeOptimizer(new QuadraticEnergyBackend());
    final VqeOptimizationResult result =
        optimizer.optimize(
            VqeOptimizationRequest.of(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.of(
                    ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE,
                    "vqe_budget",
                    "q",
                    "c",
                    "theta",
                    1,
                    4,
                    true),
                new double[] {0.0, 0.0},
                VqeOptimizationOptions.of(4, 8192, 0.25, 1.0E-4, 1.0E-8, 1.0E-4),
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.ideal(),
                    List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                    128,
                    false,
                    false)));

    Assertions.assertTrue(result.evaluationCount() > 0L);
    Assertions.assertTrue(result.totalShots() >= result.evaluationCount() * 8192L);
  }

  @Test
  void vqeCountsFailedTrialEvaluationsWithoutLosingBestState() {
    final VqeOptimizer optimizer =
        new VqeOptimizer(
            (program, parameters, shots, mitigationPlan) -> {
              if (parameters[0] > 0.0) {
                return VqeEnergyEvaluation.failure("positive branch unavailable");
              }
              return VqeEnergyEvaluation.success(parameters[0] * parameters[0], 0.01, shots);
            });
    final VqeOptimizationResult result =
        optimizer.optimize(
            VqeOptimizationRequest.of(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.defaults(),
                new double[] {0.0},
                VqeOptimizationOptions.of(3, 512, 0.25, 1.0E-4, 1.0E-8, 1.0E-4),
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.ideal(),
                    List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                    128,
                    false,
                    false)));

    Assertions.assertTrue(result.failedEvaluationCount() > 0L);
    Assertions.assertTrue(result.hasFailedEvaluations());
    Assertions.assertTrue(result.failedEvaluationRatio() > 0.0);
    Assertions.assertTrue(result.failedEvaluationRatio() < 1.0);
    Assertions.assertTrue(result.evaluationCount() > result.failedEvaluationCount());
    Assertions.assertEquals(0.0, result.bestEnergy());
  }

  @Test
  void vqeReportsBackendFailureAsStructuredResult() {
    final VqeOptimizer optimizer =
        new VqeOptimizer(
            (program, parameters, shots, mitigationPlan) ->
                VqeEnergyEvaluation.failure("backend unavailable"));
    final VqeOptimizationResult result =
        optimizer.optimize(
            VqeOptimizationRequest.of(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.defaults(),
                new double[] {0.0},
                VqeOptimizationOptions.defaults(),
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.ideal(),
                    List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                    128,
                    false,
                    false)));

    Assertions.assertEquals(VqeConvergenceStatus.BACKEND_FAILED, result.status());
    Assertions.assertTrue(result.backendFailed());
    Assertions.assertFalse(result.converged());
    Assertions.assertTrue(result.hasFailedEvaluations());
    Assertions.assertEquals(1.0, result.failedEvaluationRatio());
    Assertions.assertEquals("backend unavailable", result.diagnostic());
    Assertions.assertEquals(3L, result.evaluationCount());
    Assertions.assertEquals(3L, result.failedEvaluationCount());
    Assertions.assertFalse(result.iterations().isEmpty());
  }

  @Test
  void vqeRetriesTransientBackendFailureAndGrowsShotsWithinCap() {
    final AtomicInteger calls = new AtomicInteger();
    final VqeOptimizer optimizer =
        new VqeOptimizer(
            (program, parameters, shots, mitigationPlan) -> {
              final int call = calls.incrementAndGet();
              if (call == 1) {
                return VqeEnergyEvaluation.failure("transient backend timeout");
              }
              return VqeEnergyEvaluation.success(
                  (parameters[0] - 0.5) * (parameters[0] - 0.5),
                  1.0 / Math.sqrt((double) shots),
                  shots);
            });

    final VqeOptimizationResult result =
        optimizer.optimize(
            VqeOptimizationRequest.of(
                ActiveSpace.of(2, 2),
                VariationalChemistryOptions.defaults(),
                new double[] {0.0},
                VqeOptimizationOptions.of(
                    4, 128, 0.25, 1.0E-4, 1.0E-8, 1.0E-4, 1.0E-3, 1.0E-4, 512, 2.0, 1),
                ErrorMitigationRequest.of(
                    ActiveSpace.of(2, 2),
                    QuantumNoiseModel.ideal(),
                    List.of(ErrorMitigationTechnique.READOUT_CALIBRATION),
                    128,
                    false,
                    false)));

    Assertions.assertTrue(result.status() != VqeConvergenceStatus.BACKEND_FAILED);
    Assertions.assertTrue(result.failedEvaluationCount() >= 1L);
    Assertions.assertTrue(result.evaluationCount() > result.iterations().size());
    Assertions.assertTrue(result.totalShots() > result.evaluationCount() * 128L);
    Assertions.assertTrue(result.totalShots() <= result.evaluationCount() * 512L);
  }

  private static final class QuadraticEnergyBackend implements VqeEnergyBackend {

    @Override
    public VqeEnergyEvaluation evaluate(
        final QuantumProgram program,
        final double[] parameters,
        final int shots,
        final ErrorMitigationPlan mitigationPlan) {
      double energy = -1.25;
      energy += (parameters[0] - 0.75) * (parameters[0] - 0.75);
      energy += (parameters[1] + 0.25) * (parameters[1] + 0.25);
      return VqeEnergyEvaluation.success(
          energy, 1.0 / Math.sqrt((double) mitigationPlan.recommendedShots()), shots);
    }
  }
}
