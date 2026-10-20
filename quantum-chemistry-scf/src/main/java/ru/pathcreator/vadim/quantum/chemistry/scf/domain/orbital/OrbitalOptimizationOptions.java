/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital;

/**
 * Опции консервативной оптимизации molecular orbitals через вращения пар колонок.
 */
public final class OrbitalOptimizationOptions {

  public static final OrbitalOptimizationOptions DEFAULT =
      OrbitalOptimizationOptions.of(24, 512, 0.04, 1.0E-5, 1.0E-8, 1.0E-10);

  private final int maxIterations;
  private final int maxPairsPerIteration;
  private final double initialStepRadians;
  private final double minimumStepRadians;
  private final double energyTolerance;
  private final double orthonormalityTolerance;

  private OrbitalOptimizationOptions(
      final int maxIterations,
      final int maxPairsPerIteration,
      final double initialStepRadians,
      final double minimumStepRadians,
      final double energyTolerance,
      final double orthonormalityTolerance) {
    this.maxIterations = maxIterations;
    this.maxPairsPerIteration = maxPairsPerIteration;
    this.initialStepRadians = initialStepRadians;
    this.minimumStepRadians = minimumStepRadians;
    this.energyTolerance = energyTolerance;
    this.orthonormalityTolerance = orthonormalityTolerance;
  }

  public static OrbitalOptimizationOptions of(
      final int maxIterations,
      final int maxPairsPerIteration,
      final double initialStepRadians,
      final double minimumStepRadians,
      final double energyTolerance,
      final double orthonormalityTolerance) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("Orbital optimization max iterations must be positive.");
    }
    if (maxPairsPerIteration <= 0) {
      throw new IllegalArgumentException("Orbital optimization pair limit must be positive.");
    }
    if (!Double.isFinite(initialStepRadians) || initialStepRadians <= 0.0) {
      throw new IllegalArgumentException(
          "Orbital optimization initial step must be positive and finite.");
    }
    if (!Double.isFinite(minimumStepRadians) || minimumStepRadians <= 0.0) {
      throw new IllegalArgumentException(
          "Orbital optimization minimum step must be positive and finite.");
    }
    if (minimumStepRadians > initialStepRadians) {
      throw new IllegalArgumentException(
          "Orbital optimization minimum step must not exceed initial step.");
    }
    if (!Double.isFinite(energyTolerance) || energyTolerance <= 0.0) {
      throw new IllegalArgumentException(
          "Orbital optimization energy tolerance must be positive and finite.");
    }
    if (!Double.isFinite(orthonormalityTolerance) || orthonormalityTolerance <= 0.0) {
      throw new IllegalArgumentException(
          "Orbital optimization orthonormality tolerance must be positive.");
    }
    return new OrbitalOptimizationOptions(
        maxIterations,
        maxPairsPerIteration,
        initialStepRadians,
        minimumStepRadians,
        energyTolerance,
        orthonormalityTolerance);
  }

  public int maxIterations() {
    return this.maxIterations;
  }

  public int maxPairsPerIteration() {
    return this.maxPairsPerIteration;
  }

  public double initialStepRadians() {
    return this.initialStepRadians;
  }

  public double minimumStepRadians() {
    return this.minimumStepRadians;
  }

  public double energyTolerance() {
    return this.energyTolerance;
  }

  public double orthonormalityTolerance() {
    return this.orthonormalityTolerance;
  }
}
