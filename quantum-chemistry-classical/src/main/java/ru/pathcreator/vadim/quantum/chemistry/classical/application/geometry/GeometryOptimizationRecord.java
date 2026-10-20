/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

public final class GeometryOptimizationRecord {

  private final int iteration;
  private final double energy;
  private final double gradientNorm;
  private final double maxGradientComponent;
  private final double acceptedStepAngstrom;
  private final boolean converged;

  private GeometryOptimizationRecord(
      final int iteration,
      final double energy,
      final double gradientNorm,
      final double maxGradientComponent,
      final double acceptedStepAngstrom,
      final boolean converged) {
    this.iteration = iteration;
    this.energy = energy;
    this.gradientNorm = gradientNorm;
    this.maxGradientComponent = maxGradientComponent;
    this.acceptedStepAngstrom = acceptedStepAngstrom;
    this.converged = converged;
  }

  public static GeometryOptimizationRecord of(
      final int iteration,
      final double energy,
      final double gradientNorm,
      final double maxGradientComponent,
      final double acceptedStepAngstrom,
      final boolean converged) {
    if (iteration < 0) {
      throw new IllegalArgumentException(
          "Geometry optimization record iteration must not be negative.");
    }
    GeometryOptimizationRecord.requireFinite(energy, "Geometry optimization record energy");
    GeometryOptimizationRecord.requireFinite(
        gradientNorm, "Geometry optimization record gradient norm");
    GeometryOptimizationRecord.requireFinite(
        maxGradientComponent, "Geometry optimization record max gradient");
    if (!Double.isFinite(acceptedStepAngstrom) || acceptedStepAngstrom < 0.0) {
      throw new IllegalArgumentException(
          "Geometry optimization record accepted step must be finite and non-negative.");
    }
    return new GeometryOptimizationRecord(
        iteration, energy, gradientNorm, maxGradientComponent, acceptedStepAngstrom, converged);
  }

  public int iteration() {
    return this.iteration;
  }

  public double energy() {
    return this.energy;
  }

  public double gradientNorm() {
    return this.gradientNorm;
  }

  public double maxGradientComponent() {
    return this.maxGradientComponent;
  }

  public double acceptedStepAngstrom() {
    return this.acceptedStepAngstrom;
  }

  public boolean converged() {
    return this.converged;
  }

  private static void requireFinite(final double value, final String subject) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subject + " must be finite.");
    }
  }
}
