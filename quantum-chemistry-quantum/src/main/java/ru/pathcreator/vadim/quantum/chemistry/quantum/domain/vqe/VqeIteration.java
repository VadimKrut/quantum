/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe;

import java.util.Arrays;

/**
 * Снимок одной итерации VQE с параметрами, энергией, шагом optimizer и нормой gradient probe.
 */
public final class VqeIteration {

  private final int index;
  private final double energy;
  private final double uncertainty;
  private final double stepSize;
  private final double gradientNorm;
  private final boolean acceptedImprovement;
  private final double[] parameters;

  private VqeIteration(
      final int index,
      final double energy,
      final double uncertainty,
      final double stepSize,
      final double gradientNorm,
      final boolean acceptedImprovement,
      final double[] parameters) {
    this.index = index;
    this.energy = energy;
    this.uncertainty = uncertainty;
    this.stepSize = stepSize;
    this.gradientNorm = gradientNorm;
    this.acceptedImprovement = acceptedImprovement;
    this.parameters = parameters;
  }

  public static VqeIteration of(
      final int index,
      final double energy,
      final double uncertainty,
      final double stepSize,
      final boolean acceptedImprovement,
      final double[] parameters) {
    return VqeIteration.of(
        index, energy, uncertainty, stepSize, Double.NaN, acceptedImprovement, parameters);
  }

  public static VqeIteration of(
      final int index,
      final double energy,
      final double uncertainty,
      final double stepSize,
      final double gradientNorm,
      final boolean acceptedImprovement,
      final double[] parameters) {
    if (index < 0) {
      throw new IllegalArgumentException("VQE iteration index must be non-negative.");
    }
    if (!Double.isFinite(energy)) {
      throw new IllegalArgumentException("VQE iteration energy must be finite.");
    }
    if (!Double.isFinite(uncertainty) || uncertainty < 0.0) {
      throw new IllegalArgumentException(
          "VQE iteration uncertainty must be finite and non-negative.");
    }
    if (!Double.isFinite(stepSize) || stepSize < 0.0) {
      throw new IllegalArgumentException("VQE iteration step must be finite and non-negative.");
    }
    if (!(Double.isNaN(gradientNorm) || Double.isFinite(gradientNorm) && gradientNorm >= 0.0)) {
      throw new IllegalArgumentException(
          "VQE iteration gradient norm must be finite non-negative or NaN.");
    }
    if (parameters == null || parameters.length == 0) {
      throw new IllegalArgumentException("VQE iteration parameters must not be empty.");
    }
    return new VqeIteration(
        index,
        energy,
        uncertainty,
        stepSize,
        gradientNorm,
        acceptedImprovement,
        parameters.clone());
  }

  public int index() {
    return this.index;
  }

  public double energy() {
    return this.energy;
  }

  public double uncertainty() {
    return this.uncertainty;
  }

  public double stepSize() {
    return this.stepSize;
  }

  public double gradientNorm() {
    return this.gradientNorm;
  }

  public boolean acceptedImprovement() {
    return this.acceptedImprovement;
  }

  public double[] parametersCopy() {
    return this.parameters.clone();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof VqeIteration)) {
      return false;
    }
    final VqeIteration iteration = (VqeIteration) other;
    return this.index == iteration.index
        && Double.compare(this.energy, iteration.energy) == 0
        && Double.compare(this.uncertainty, iteration.uncertainty) == 0
        && Double.compare(this.stepSize, iteration.stepSize) == 0
        && Double.compare(this.gradientNorm, iteration.gradientNorm) == 0
        && this.acceptedImprovement == iteration.acceptedImprovement
        && Arrays.equals(this.parameters, iteration.parameters);
  }

  public int hashCode() {
    int result = Integer.hashCode(this.index);
    result = 31 * result + Double.hashCode(this.energy);
    result = 31 * result + Double.hashCode(this.uncertainty);
    result = 31 * result + Double.hashCode(this.stepSize);
    result = 31 * result + Double.hashCode(this.gradientNorm);
    result = 31 * result + Boolean.hashCode(this.acceptedImprovement);
    result = 31 * result + Arrays.hashCode(this.parameters);
    return result;
  }
}
