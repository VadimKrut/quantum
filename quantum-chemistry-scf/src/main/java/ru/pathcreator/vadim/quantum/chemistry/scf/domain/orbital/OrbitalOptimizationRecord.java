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
 * Одна итерация оптимизации molecular orbitals.
 */
public final class OrbitalOptimizationRecord {

  private final int iteration;
  private final int evaluatedRotationCount;
  private final int acceptedRotationCount;
  private final double stepRadians;
  private final double energy;
  private final double deltaEnergy;
  private final boolean converged;

  private OrbitalOptimizationRecord(
      final int iteration,
      final int evaluatedRotationCount,
      final int acceptedRotationCount,
      final double stepRadians,
      final double energy,
      final double deltaEnergy,
      final boolean converged) {
    this.iteration = iteration;
    this.evaluatedRotationCount = evaluatedRotationCount;
    this.acceptedRotationCount = acceptedRotationCount;
    this.stepRadians = stepRadians;
    this.energy = energy;
    this.deltaEnergy = deltaEnergy;
    this.converged = converged;
  }

  public static OrbitalOptimizationRecord of(
      final int iteration,
      final int evaluatedRotationCount,
      final int acceptedRotationCount,
      final double stepRadians,
      final double energy,
      final double deltaEnergy,
      final boolean converged) {
    if (iteration <= 0) {
      throw new IllegalArgumentException("Orbital optimization iteration must be positive.");
    }
    if (evaluatedRotationCount < 0 || acceptedRotationCount < 0) {
      throw new IllegalArgumentException(
          "Orbital optimization rotation counters must not be negative.");
    }
    if (acceptedRotationCount > evaluatedRotationCount) {
      throw new IllegalArgumentException(
          "Accepted orbital rotations cannot exceed evaluated rotations.");
    }
    if (!Double.isFinite(stepRadians) || stepRadians < 0.0) {
      throw new IllegalArgumentException(
          "Orbital optimization step must be finite and non-negative.");
    }
    if (!Double.isFinite(energy) || !Double.isFinite(deltaEnergy)) {
      throw new IllegalArgumentException("Orbital optimization record energies must be finite.");
    }
    return new OrbitalOptimizationRecord(
        iteration,
        evaluatedRotationCount,
        acceptedRotationCount,
        stepRadians,
        energy,
        deltaEnergy,
        converged);
  }

  public int iteration() {
    return this.iteration;
  }

  public int evaluatedRotationCount() {
    return this.evaluatedRotationCount;
  }

  public int acceptedRotationCount() {
    return this.acceptedRotationCount;
  }

  public double stepRadians() {
    return this.stepRadians;
  }

  public double energy() {
    return this.energy;
  }

  public double deltaEnergy() {
    return this.deltaEnergy;
  }

  public boolean converged() {
    return this.converged;
  }
}
