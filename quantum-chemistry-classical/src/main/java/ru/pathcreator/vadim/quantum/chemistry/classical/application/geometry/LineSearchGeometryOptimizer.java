/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry;

import java.util.ArrayList;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Выполняет устойчивую line-search релаксацию координат для выбранной модели энергии.
 */
public final class LineSearchGeometryOptimizer {

  public GeometryOptimizationResult optimize(
      final Molecule molecule,
      final GeometryOptimizationOptions options,
      final GeometryEnergyModel energyModel) {
    if (molecule == null) {
      throw new IllegalArgumentException("Geometry optimization molecule must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("Geometry optimization options must not be null.");
    }
    if (energyModel == null) {
      throw new IllegalArgumentException("Geometry optimization energy model must not be null.");
    }
    final double[] coordinates = GeometryCoordinateSupport.coordinatesAngstrom(molecule);
    final double initialEnergy = energyModel.energy(molecule, coordinates);
    final double[] gradient = new double[coordinates.length];
    final double[] nextGradient = new double[coordinates.length];
    final double[] direction = new double[coordinates.length];
    final double[] stepVector = new double[coordinates.length];
    final double[] gradientChange = new double[coordinates.length];
    final double[] inverseHessian = LineSearchGeometryOptimizer.identity(coordinates.length);
    boolean converged = false;
    int iterations = 0;
    double energy = initialEnergy;
    boolean finalGradientKnown = false;
    final ArrayList<GeometryOptimizationRecord> history =
        new ArrayList<GeometryOptimizationRecord>(options.maxIterations() + 1);
    for (int iteration = 0; iteration < options.maxIterations(); ++iteration) {
      if (Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException("Geometry optimization interrupted.");
      }
      finalGradientKnown = false;
      energyModel.gradient(molecule, coordinates, options.finiteDifferenceStepAngstrom(), gradient);
      final double gradientNorm = GeometryCoordinateSupport.norm(gradient);
      final double maxGradientComponent = GeometryCoordinateSupport.maxAbsolute(gradient);
      if (gradientNorm <= options.gradientTolerance()) {
        converged = true;
        iterations = iteration;
        history.add(
            GeometryOptimizationRecord.of(
                iteration, energy, gradientNorm, maxGradientComponent, 0.0, true));
        break;
      }
      LineSearchGeometryOptimizer.direction(inverseHessian, gradient, direction);
      final GeometryStepResult stepResult =
          LineSearchGeometryOptimizer.applyLineSearchStep(
              molecule,
              coordinates,
              direction,
              energy,
              options.stepAngstrom() / Math.max(1.0, GeometryCoordinateSupport.norm(direction)),
              energyModel,
              options.maxLineSearchAttempts());
      for (int i = 0; i < coordinates.length; ++i) {
        stepVector[i] = stepResult.coordinates()[i] - coordinates[i];
      }
      if (stepResult.acceptedStep() > 0.0) {
        System.arraycopy(stepResult.coordinates(), 0, coordinates, 0, coordinates.length);
        if (iteration + 1 < options.maxIterations()) {
          energyModel.gradient(
              molecule, coordinates, options.finiteDifferenceStepAngstrom(), nextGradient);
          for (int i = 0; i < gradient.length; ++i) {
            gradientChange[i] = nextGradient[i] - gradient[i];
          }
          LineSearchGeometryOptimizer.updateInverseHessian(
              inverseHessian, stepVector, gradientChange);
        } else {
          finalGradientKnown = false;
        }
      } else {
        LineSearchGeometryOptimizer.resetIdentity(inverseHessian);
        finalGradientKnown = true;
      }
      energy = stepResult.energy();
      history.add(
          GeometryOptimizationRecord.of(
              iteration + 1,
              energy,
              gradientNorm,
              maxGradientComponent,
              stepResult.acceptedStep(),
              false));
      iterations = iteration + 1;
    }
    if (!finalGradientKnown) {
      energyModel.gradient(molecule, coordinates, options.finiteDifferenceStepAngstrom(), gradient);
    }
    final double finalGradientNorm = GeometryCoordinateSupport.norm(gradient);
    final double maxGradientComponent = GeometryCoordinateSupport.maxAbsolute(gradient);
    if (finalGradientNorm <= options.gradientTolerance()) {
      converged = true;
    }
    return GeometryOptimizationResult.of(
        converged,
        iterations,
        initialEnergy,
        energy,
        finalGradientNorm,
        maxGradientComponent,
        coordinates,
        history);
  }

  private static GeometryStepResult applyLineSearchStep(
      final Molecule molecule,
      final double[] coordinates,
      final double[] direction,
      final double currentEnergy,
      final double baseStep,
      final GeometryEnergyModel energyModel,
      final int maxLineSearchAttempts) {
    double step = baseStep;
    final double[] trialCoordinates = coordinates.clone();
    for (int attempt = 0; attempt < maxLineSearchAttempts; ++attempt) {
      for (int i = 0; i < coordinates.length; ++i) {
        trialCoordinates[i] = coordinates[i] + step * direction[i];
      }
      final double trialEnergy = energyModel.energy(molecule, trialCoordinates);
      if (trialEnergy <= currentEnergy) {
        return GeometryStepResult.of(trialEnergy, step, trialCoordinates);
      }
      step *= 0.5;
    }
    return GeometryStepResult.of(currentEnergy, 0.0, coordinates);
  }

  private static double[] identity(final int size) {
    final double[] matrix = new double[Math.multiplyExact(size, size)];
    for (int i = 0; i < size; ++i) {
      matrix[i * size + i] = 1.0;
    }
    return matrix;
  }

  private static void resetIdentity(final double[] matrix) {
    java.util.Arrays.fill(matrix, 0.0);
    final int size = (int) Math.round(Math.sqrt((double) matrix.length));
    for (int i = 0; i < size; ++i) {
      matrix[i * size + i] = 1.0;
    }
  }

  private static void direction(
      final double[] inverseHessian, final double[] gradient, final double[] direction) {
    final int size = gradient.length;
    for (int row = 0; row < size; ++row) {
      double value = 0.0;
      for (int column = 0; column < size; ++column) {
        value -= inverseHessian[row * size + column] * gradient[column];
      }
      direction[row] = value;
    }
    if (LineSearchGeometryOptimizer.dot(direction, gradient) >= 0.0) {
      for (int i = 0; i < size; ++i) {
        direction[i] = -gradient[i];
      }
      LineSearchGeometryOptimizer.resetIdentity(inverseHessian);
    }
  }

  private static void updateInverseHessian(
      final double[] inverseHessian, final double[] stepVector, final double[] gradientChange) {
    final double curvature = LineSearchGeometryOptimizer.dot(stepVector, gradientChange);
    if (!Double.isFinite(curvature) || curvature <= 1.0E-12) {
      LineSearchGeometryOptimizer.resetIdentity(inverseHessian);
      return;
    }
    final int size = stepVector.length;
    final double rho = 1.0 / curvature;
    final double[] hy = new double[size];
    for (int row = 0; row < size; ++row) {
      double value = 0.0;
      for (int column = 0; column < size; ++column) {
        value += inverseHessian[row * size + column] * gradientChange[column];
      }
      hy[row] = value;
    }
    final double yhy = LineSearchGeometryOptimizer.dot(gradientChange, hy);
    final double coefficient = (1.0 + yhy * rho) * rho;
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        final int index = row * size + column;
        inverseHessian[index] +=
            coefficient * stepVector[row] * stepVector[column]
                - rho * (stepVector[row] * hy[column] + hy[row] * stepVector[column]);
      }
    }
  }

  private static double dot(final double[] first, final double[] second) {
    double value = 0.0;
    for (int i = 0; i < first.length; ++i) {
      value += first[i] * second[i];
    }
    return value;
  }

  private static final class GeometryStepResult {

    private final double energy;
    private final double acceptedStep;
    private final double[] coordinates;

    private GeometryStepResult(
        final double energy, final double acceptedStep, final double[] coordinates) {
      this.energy = energy;
      this.acceptedStep = acceptedStep;
      this.coordinates = coordinates;
    }

    static GeometryStepResult of(
        final double energy, final double acceptedStep, final double[] coordinates) {
      if (!Double.isFinite(energy)) {
        throw new IllegalArgumentException("Geometry step energy must be finite.");
      }
      if (!Double.isFinite(acceptedStep) || acceptedStep < 0.0) {
        throw new IllegalArgumentException(
            "Geometry accepted step must be finite and non-negative.");
      }
      if (coordinates == null || coordinates.length == 0) {
        throw new IllegalArgumentException("Geometry step coordinates must not be empty.");
      }
      return new GeometryStepResult(energy, acceptedStep, coordinates.clone());
    }

    double energy() {
      return this.energy;
    }

    double acceptedStep() {
      return this.acceptedStep;
    }

    double[] coordinates() {
      return this.coordinates.clone();
    }
  }
}
