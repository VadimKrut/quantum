/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.convergence;

/** Запись одной SCF-итерации с численными признаками сходимости. */
public final class ScfIterationRecord {

  private final int iteration;
  private final double electronicEnergy;
  private final double deltaEnergy;
  private final double densityChange;
  private final boolean converged;

  private ScfIterationRecord(
      final int iteration,
      final double electronicEnergy,
      final double deltaEnergy,
      final double densityChange,
      final boolean converged) {
    this.iteration = iteration;
    this.electronicEnergy = electronicEnergy;
    this.deltaEnergy = deltaEnergy;
    this.densityChange = densityChange;
    this.converged = converged;
  }

  public static ScfIterationRecord of(
      final int iteration,
      final double electronicEnergy,
      final double deltaEnergy,
      final double densityChange,
      final boolean converged) {
    if (iteration <= 0) {
      throw new IllegalArgumentException("SCF iteration must be positive.");
    }
    ScfIterationRecord.requireFinite(electronicEnergy, "SCF electronic energy");
    ScfIterationRecord.requireFinite(deltaEnergy, "SCF delta energy");
    ScfIterationRecord.requireFinite(densityChange, "SCF density change");
    if (deltaEnergy < 0.0) {
      throw new IllegalArgumentException("SCF delta energy must not be negative.");
    }
    if (densityChange < 0.0) {
      throw new IllegalArgumentException("SCF density change must not be negative.");
    }
    return new ScfIterationRecord(
        iteration, electronicEnergy, deltaEnergy, densityChange, converged);
  }

  public int iteration() {
    return this.iteration;
  }

  public double electronicEnergy() {
    return this.electronicEnergy;
  }

  public double deltaEnergy() {
    return this.deltaEnergy;
  }

  public double densityChange() {
    return this.densityChange;
  }

  public boolean converged() {
    return this.converged;
  }

  private static void requireFinite(final double value, final String subjectName) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subjectName + " must be finite.");
    }
  }
}
