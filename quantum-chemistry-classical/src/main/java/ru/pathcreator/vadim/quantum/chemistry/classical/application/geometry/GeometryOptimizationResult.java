/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import java.util.List;

/**
 * Численный результат локальной оптимизации геометрии в компактных массивах.
 */
public final class GeometryOptimizationResult {

  private final boolean converged;
  private final int iterationCount;
  private final double initialEnergy;
  private final double finalEnergy;
  private final double gradientNorm;
  private final double maxGradientComponent;
  private final double[] coordinatesAngstrom;
  private final List<GeometryOptimizationRecord> history;

  private GeometryOptimizationResult(
      final boolean converged,
      final int iterationCount,
      final double initialEnergy,
      final double finalEnergy,
      final double gradientNorm,
      final double maxGradientComponent,
      final double[] coordinatesAngstrom,
      final List<GeometryOptimizationRecord> history) {
    this.converged = converged;
    this.iterationCount = iterationCount;
    this.initialEnergy = initialEnergy;
    this.finalEnergy = finalEnergy;
    this.gradientNorm = gradientNorm;
    this.maxGradientComponent = maxGradientComponent;
    this.coordinatesAngstrom = coordinatesAngstrom;
    this.history = history;
  }

  public static GeometryOptimizationResult of(
      final boolean converged,
      final int iterationCount,
      final double initialEnergy,
      final double finalEnergy,
      final double gradientNorm,
      final double maxGradientComponent,
      final double[] coordinatesAngstrom,
      final List<GeometryOptimizationRecord> history) {
    if (iterationCount < 0) {
      throw new IllegalArgumentException("Geometry iteration count must not be negative.");
    }
    GeometryOptimizationResult.requireFinite(initialEnergy, "Initial geometry energy");
    GeometryOptimizationResult.requireFinite(finalEnergy, "Final geometry energy");
    GeometryOptimizationResult.requireFinite(gradientNorm, "Geometry gradient norm");
    GeometryOptimizationResult.requireFinite(maxGradientComponent, "Geometry max gradient");
    if (coordinatesAngstrom == null
        || coordinatesAngstrom.length == 0
        || coordinatesAngstrom.length % 3 != 0) {
      throw new IllegalArgumentException("Geometry coordinates must contain xyz triples.");
    }
    for (int i = 0; i < coordinatesAngstrom.length; ++i) {
      GeometryOptimizationResult.requireFinite(coordinatesAngstrom[i], "Geometry coordinate");
    }
    if (history == null) {
      throw new IllegalArgumentException("Geometry optimization history must not be null.");
    }
    for (int i = 0; i < history.size(); ++i) {
      if (history.get(i) == null) {
        throw new IllegalArgumentException("Geometry optimization history contains null record.");
      }
    }
    return new GeometryOptimizationResult(
        converged,
        iterationCount,
        initialEnergy,
        finalEnergy,
        gradientNorm,
        maxGradientComponent,
        coordinatesAngstrom.clone(),
        List.copyOf(history));
  }

  public boolean converged() {
    return this.converged;
  }

  public int iterationCount() {
    return this.iterationCount;
  }

  public double initialEnergy() {
    return this.initialEnergy;
  }

  public double finalEnergy() {
    return this.finalEnergy;
  }

  public double gradientNorm() {
    return this.gradientNorm;
  }

  public double maxGradientComponent() {
    return this.maxGradientComponent;
  }

  public double[] coordinatesAngstromCopy() {
    return this.coordinatesAngstrom.clone();
  }

  public List<GeometryOptimizationRecord> history() {
    return this.history;
  }

  private static void requireFinite(final double value, final String subject) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subject + " must be finite.");
    }
  }
}
