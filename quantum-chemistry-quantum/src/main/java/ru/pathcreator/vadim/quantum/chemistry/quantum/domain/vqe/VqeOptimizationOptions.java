/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe;

/**
 * Настройки VQE optimizer loop: итерации, допуски, шаги, gradient probe и shot budget.
 */
public final class VqeOptimizationOptions {

  private final int maxIterations;
  private final int baseShots;
  private final double initialStep;
  private final double minimumStep;
  private final double energyTolerance;
  private final double parameterTolerance;
  private final double gradientProbeStep;
  private final double gradientTolerance;
  private final int maxShots;
  private final double shotGrowthFactor;
  private final int maxBackendRetries;

  private VqeOptimizationOptions(
      final int maxIterations,
      final int baseShots,
      final double initialStep,
      final double minimumStep,
      final double energyTolerance,
      final double parameterTolerance,
      final double gradientProbeStep,
      final double gradientTolerance,
      final int maxShots,
      final double shotGrowthFactor,
      final int maxBackendRetries) {
    this.maxIterations = maxIterations;
    this.baseShots = baseShots;
    this.initialStep = initialStep;
    this.minimumStep = minimumStep;
    this.energyTolerance = energyTolerance;
    this.parameterTolerance = parameterTolerance;
    this.gradientProbeStep = gradientProbeStep;
    this.gradientTolerance = gradientTolerance;
    this.maxShots = maxShots;
    this.shotGrowthFactor = shotGrowthFactor;
    this.maxBackendRetries = maxBackendRetries;
  }

  public static VqeOptimizationOptions defaults() {
    return VqeOptimizationOptions.of(
        128, 4096, 0.25, 1.0E-4, 1.0E-6, 1.0E-5, 1.0E-3, 1.0E-4, 65_536, 1.25, 2);
  }

  public static VqeOptimizationOptions of(
      final int maxIterations,
      final int baseShots,
      final double initialStep,
      final double minimumStep,
      final double energyTolerance,
      final double parameterTolerance) {
    return VqeOptimizationOptions.of(
        maxIterations,
        baseShots,
        initialStep,
        minimumStep,
        energyTolerance,
        parameterTolerance,
        1.0E-3,
        Math.max(parameterTolerance, 1.0E-8));
  }

  public static VqeOptimizationOptions of(
      final int maxIterations,
      final int baseShots,
      final double initialStep,
      final double minimumStep,
      final double energyTolerance,
      final double parameterTolerance,
      final double gradientProbeStep,
      final double gradientTolerance) {
    return VqeOptimizationOptions.of(
        maxIterations,
        baseShots,
        initialStep,
        minimumStep,
        energyTolerance,
        parameterTolerance,
        gradientProbeStep,
        gradientTolerance,
        baseShots,
        1.0,
        0);
  }

  public static VqeOptimizationOptions of(
      final int maxIterations,
      final int baseShots,
      final double initialStep,
      final double minimumStep,
      final double energyTolerance,
      final double parameterTolerance,
      final double gradientProbeStep,
      final double gradientTolerance,
      final int maxShots,
      final double shotGrowthFactor,
      final int maxBackendRetries) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("VQE max iterations must be positive.");
    }
    if (baseShots <= 0) {
      throw new IllegalArgumentException("VQE base shots must be positive.");
    }
    if (!Double.isFinite(initialStep) || initialStep <= 0.0) {
      throw new IllegalArgumentException("VQE initial step must be finite and positive.");
    }
    if (!Double.isFinite(minimumStep) || minimumStep <= 0.0 || minimumStep > initialStep) {
      throw new IllegalArgumentException(
          "VQE minimum step must be finite, positive and not larger than initial step.");
    }
    if (!Double.isFinite(energyTolerance) || energyTolerance <= 0.0) {
      throw new IllegalArgumentException("VQE energy tolerance must be finite and positive.");
    }
    if (!Double.isFinite(parameterTolerance) || parameterTolerance <= 0.0) {
      throw new IllegalArgumentException("VQE parameter tolerance must be finite and positive.");
    }
    if (!Double.isFinite(gradientProbeStep) || gradientProbeStep <= 0.0) {
      throw new IllegalArgumentException("VQE gradient probe step must be finite and positive.");
    }
    if (!Double.isFinite(gradientTolerance) || gradientTolerance <= 0.0) {
      throw new IllegalArgumentException("VQE gradient tolerance must be finite and positive.");
    }
    if (maxShots < baseShots) {
      throw new IllegalArgumentException("VQE max shots must not be smaller than base shots.");
    }
    if (!Double.isFinite(shotGrowthFactor) || shotGrowthFactor < 1.0) {
      throw new IllegalArgumentException(
          "VQE shot growth factor must be finite and not smaller than one.");
    }
    if (maxBackendRetries < 0) {
      throw new IllegalArgumentException("VQE max backend retries must not be negative.");
    }
    return new VqeOptimizationOptions(
        maxIterations,
        baseShots,
        initialStep,
        minimumStep,
        energyTolerance,
        parameterTolerance,
        gradientProbeStep,
        gradientTolerance,
        maxShots,
        shotGrowthFactor,
        maxBackendRetries);
  }

  public int maxIterations() {
    return this.maxIterations;
  }

  public int baseShots() {
    return this.baseShots;
  }

  public double initialStep() {
    return this.initialStep;
  }

  public double minimumStep() {
    return this.minimumStep;
  }

  public double energyTolerance() {
    return this.energyTolerance;
  }

  public double parameterTolerance() {
    return this.parameterTolerance;
  }

  public double gradientProbeStep() {
    return this.gradientProbeStep;
  }

  public double gradientTolerance() {
    return this.gradientTolerance;
  }

  public int maxShots() {
    return this.maxShots;
  }

  public double shotGrowthFactor() {
    return this.shotGrowthFactor;
  }

  public int maxBackendRetries() {
    return this.maxBackendRetries;
  }
}
