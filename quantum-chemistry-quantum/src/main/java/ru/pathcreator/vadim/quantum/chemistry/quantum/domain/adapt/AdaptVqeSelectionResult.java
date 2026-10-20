/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.adapt;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.UccsdExcitation;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Результат ADAPT-VQE отбора с готовой Quantum IR-программой выбранного ansatz.
 */
public final class AdaptVqeSelectionResult {

  private final AdaptVqeSelectionStatus status;
  private final QuantumProgram selectedProgram;
  private final ErrorMitigationPlan mitigationPlan;
  private final double referenceEnergy;
  private final double bestEnergy;
  private final double[] selectedParameters;
  private final List<UccsdExcitation> selectedExcitations;
  private final List<AdaptVqeIteration> iterations;
  private final long evaluationCount;
  private final long failedEvaluationCount;
  private final long totalShots;
  private final String diagnostic;

  private AdaptVqeSelectionResult(
      final AdaptVqeSelectionStatus status,
      final QuantumProgram selectedProgram,
      final ErrorMitigationPlan mitigationPlan,
      final double referenceEnergy,
      final double bestEnergy,
      final double[] selectedParameters,
      final List<UccsdExcitation> selectedExcitations,
      final List<AdaptVqeIteration> iterations,
      final long evaluationCount,
      final long failedEvaluationCount,
      final long totalShots,
      final String diagnostic) {
    this.status = status;
    this.selectedProgram = selectedProgram;
    this.mitigationPlan = mitigationPlan;
    this.referenceEnergy = referenceEnergy;
    this.bestEnergy = bestEnergy;
    this.selectedParameters = selectedParameters;
    this.selectedExcitations = selectedExcitations;
    this.iterations = iterations;
    this.evaluationCount = evaluationCount;
    this.failedEvaluationCount = failedEvaluationCount;
    this.totalShots = totalShots;
    this.diagnostic = diagnostic;
  }

  public static AdaptVqeSelectionResult of(
      final AdaptVqeSelectionStatus status,
      final QuantumProgram selectedProgram,
      final ErrorMitigationPlan mitigationPlan,
      final double referenceEnergy,
      final double bestEnergy,
      final double[] selectedParameters,
      final List<UccsdExcitation> selectedExcitations,
      final List<AdaptVqeIteration> iterations,
      final long evaluationCount,
      final long failedEvaluationCount,
      final long totalShots,
      final String diagnostic) {
    if (status == null) {
      throw new IllegalArgumentException("ADAPT-VQE result status must not be null.");
    }
    if (selectedProgram == null) {
      throw new IllegalArgumentException("ADAPT-VQE selected program must not be null.");
    }
    if (mitigationPlan == null) {
      throw new IllegalArgumentException("ADAPT-VQE mitigation plan must not be null.");
    }
    if (!Double.isFinite(referenceEnergy)) {
      throw new IllegalArgumentException("ADAPT-VQE reference energy must be finite.");
    }
    if (!Double.isFinite(bestEnergy)) {
      throw new IllegalArgumentException("ADAPT-VQE best energy must be finite.");
    }
    if (selectedParameters == null) {
      throw new IllegalArgumentException("ADAPT-VQE selected parameter array must not be null.");
    }
    if (selectedExcitations == null) {
      throw new IllegalArgumentException("ADAPT-VQE selected excitation list must not be null.");
    }
    if (iterations == null) {
      throw new IllegalArgumentException("ADAPT-VQE iteration list must not be null.");
    }
    if (evaluationCount < 0L || failedEvaluationCount < 0L || totalShots < 0L) {
      throw new IllegalArgumentException("ADAPT-VQE counters must not be negative.");
    }
    if (failedEvaluationCount > evaluationCount) {
      throw new IllegalArgumentException(
          "ADAPT-VQE failed evaluation count must not exceed total count.");
    }
    if (diagnostic == null || diagnostic.isBlank()) {
      throw new IllegalArgumentException("ADAPT-VQE diagnostic must not be blank.");
    }
    final double[] checkedParameters = selectedParameters.clone();
    for (int i = 0; i < checkedParameters.length; ++i) {
      if (Double.isFinite(checkedParameters[i])) {
        continue;
      }
      throw new IllegalArgumentException("ADAPT-VQE selected parameter must be finite.");
    }
    return new AdaptVqeSelectionResult(
        status,
        selectedProgram,
        mitigationPlan,
        referenceEnergy,
        bestEnergy,
        checkedParameters,
        List.copyOf(selectedExcitations),
        List.copyOf(iterations),
        evaluationCount,
        failedEvaluationCount,
        totalShots,
        diagnostic);
  }

  public AdaptVqeSelectionStatus status() {
    return this.status;
  }

  public QuantumProgram selectedProgram() {
    return this.selectedProgram;
  }

  public ErrorMitigationPlan mitigationPlan() {
    return this.mitigationPlan;
  }

  public double referenceEnergy() {
    return this.referenceEnergy;
  }

  public double bestEnergy() {
    return this.bestEnergy;
  }

  public double energyImprovement() {
    return this.referenceEnergy - this.bestEnergy;
  }

  public double[] selectedParametersCopy() {
    return this.selectedParameters.clone();
  }

  public List<UccsdExcitation> selectedExcitations() {
    return this.selectedExcitations;
  }

  public List<AdaptVqeIteration> iterations() {
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
    return this.status == AdaptVqeSelectionStatus.CONVERGED;
  }

  public boolean backendFailed() {
    return this.status == AdaptVqeSelectionStatus.BACKEND_FAILED;
  }

  public boolean hasFailedEvaluations() {
    return this.failedEvaluationCount > 0L;
  }
}
