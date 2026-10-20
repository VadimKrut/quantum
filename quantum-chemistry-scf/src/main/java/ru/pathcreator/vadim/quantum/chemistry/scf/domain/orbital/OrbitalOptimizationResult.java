/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital;

import java.util.List;

/**
 * Результат orbital optimization с лучшими коэффициентами и полной историей.
 */
public final class OrbitalOptimizationResult {

  private final boolean converged;
  private final int iterations;
  private final int evaluatedRotationCount;
  private final int acceptedRotationCount;
  private final double finalStepRadians;
  private final double initialEnergy;
  private final double finalEnergy;
  private final double maxResidualEnergyImprovement;
  private final double maxResidualOrbitalGradient;
  private final double[] optimizedCoefficients;
  private final List<OrbitalOptimizationRecord> iterationHistory;

  private OrbitalOptimizationResult(
      final boolean converged,
      final int iterations,
      final int evaluatedRotationCount,
      final int acceptedRotationCount,
      final double finalStepRadians,
      final double initialEnergy,
      final double finalEnergy,
      final double maxResidualEnergyImprovement,
      final double maxResidualOrbitalGradient,
      final double[] optimizedCoefficients,
      final List<OrbitalOptimizationRecord> iterationHistory) {
    this.converged = converged;
    this.iterations = iterations;
    this.evaluatedRotationCount = evaluatedRotationCount;
    this.acceptedRotationCount = acceptedRotationCount;
    this.finalStepRadians = finalStepRadians;
    this.initialEnergy = initialEnergy;
    this.finalEnergy = finalEnergy;
    this.maxResidualEnergyImprovement = maxResidualEnergyImprovement;
    this.maxResidualOrbitalGradient = maxResidualOrbitalGradient;
    this.optimizedCoefficients = optimizedCoefficients;
    this.iterationHistory = iterationHistory;
  }

  public static OrbitalOptimizationResult of(
      final boolean converged,
      final int iterations,
      final int evaluatedRotationCount,
      final int acceptedRotationCount,
      final double finalStepRadians,
      final double initialEnergy,
      final double finalEnergy,
      final double maxResidualEnergyImprovement,
      final double maxResidualOrbitalGradient,
      final double[] optimizedCoefficients,
      final List<OrbitalOptimizationRecord> iterationHistory) {
    if (iterations < 0) {
      throw new IllegalArgumentException(
          "Orbital optimization iteration count must not be negative.");
    }
    if (evaluatedRotationCount < 0 || acceptedRotationCount < 0) {
      throw new IllegalArgumentException(
          "Orbital optimization rotation counters must not be negative.");
    }
    if (acceptedRotationCount > evaluatedRotationCount) {
      throw new IllegalArgumentException(
          "Accepted orbital rotations cannot exceed evaluated rotations.");
    }
    if (!Double.isFinite(finalStepRadians) || finalStepRadians < 0.0) {
      throw new IllegalArgumentException(
          "Orbital optimization final step must be finite and non-negative.");
    }
    if (!Double.isFinite(initialEnergy) || !Double.isFinite(finalEnergy)) {
      throw new IllegalArgumentException("Orbital optimization energies must be finite.");
    }
    if (!Double.isFinite(maxResidualEnergyImprovement) || maxResidualEnergyImprovement < 0.0) {
      throw new IllegalArgumentException(
          "Orbital optimization residual energy improvement must be finite and non-negative.");
    }
    if (!Double.isFinite(maxResidualOrbitalGradient) || maxResidualOrbitalGradient < 0.0) {
      throw new IllegalArgumentException(
          "Orbital optimization residual orbital gradient must be finite and non-negative.");
    }
    if (optimizedCoefficients == null || optimizedCoefficients.length == 0) {
      throw new IllegalArgumentException("Orbital optimization coefficients must not be empty.");
    }
    if (iterationHistory == null) {
      throw new IllegalArgumentException("Orbital optimization history must not be null.");
    }
    for (int i = 0; i < optimizedCoefficients.length; ++i) {
      if (!Double.isFinite(optimizedCoefficients[i])) {
        throw new IllegalArgumentException(
            "Orbital optimization coefficients contain non-finite value.");
      }
    }
    if (!iterationHistory.isEmpty() && iterationHistory.size() != iterations) {
      throw new IllegalArgumentException(
          "Orbital optimization history size must match iteration count.");
    }
    return new OrbitalOptimizationResult(
        converged,
        iterations,
        evaluatedRotationCount,
        acceptedRotationCount,
        finalStepRadians,
        initialEnergy,
        finalEnergy,
        maxResidualEnergyImprovement,
        maxResidualOrbitalGradient,
        optimizedCoefficients.clone(),
        List.copyOf(iterationHistory));
  }

  public boolean converged() {
    return this.converged;
  }

  public int iterations() {
    return this.iterations;
  }

  public int evaluatedRotationCount() {
    return this.evaluatedRotationCount;
  }

  public int acceptedRotationCount() {
    return this.acceptedRotationCount;
  }

  public double finalStepRadians() {
    return this.finalStepRadians;
  }

  public double initialEnergy() {
    return this.initialEnergy;
  }

  public double finalEnergy() {
    return this.finalEnergy;
  }

  public double energyImprovement() {
    return this.initialEnergy - this.finalEnergy;
  }

  public double maxResidualEnergyImprovement() {
    return this.maxResidualEnergyImprovement;
  }

  public double maxResidualOrbitalGradient() {
    return this.maxResidualOrbitalGradient;
  }

  public double[] optimizedCoefficientsCopy() {
    return this.optimizedCoefficients.clone();
  }

  public List<OrbitalOptimizationRecord> iterationHistory() {
    return this.iterationHistory;
  }
}
