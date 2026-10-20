/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc;

/**
 * Одна итерация coupled-cluster solver с энергией и нормой изменения амплитуд.
 */
public final class CoupledClusterIterationRecord {

  private final int iteration;
  private final double correlationEnergy;
  private final double energyDelta;
  private final double amplitudeDeltaNorm;

  private CoupledClusterIterationRecord(
      final int iteration,
      final double correlationEnergy,
      final double energyDelta,
      final double amplitudeDeltaNorm) {
    this.iteration = iteration;
    this.correlationEnergy = correlationEnergy;
    this.energyDelta = energyDelta;
    this.amplitudeDeltaNorm = amplitudeDeltaNorm;
  }

  public static CoupledClusterIterationRecord of(
      final int iteration,
      final double correlationEnergy,
      final double energyDelta,
      final double amplitudeDeltaNorm) {
    if (iteration <= 0) {
      throw new IllegalArgumentException("Coupled-cluster iteration must be positive.");
    }
    if (!Double.isFinite(correlationEnergy)
        || !Double.isFinite(energyDelta)
        || !Double.isFinite(amplitudeDeltaNorm)
        || amplitudeDeltaNorm < 0.0) {
      throw new IllegalArgumentException("Coupled-cluster iteration values must be finite.");
    }
    return new CoupledClusterIterationRecord(
        iteration, correlationEnergy, energyDelta, amplitudeDeltaNorm);
  }

  public int iteration() {
    return this.iteration;
  }

  public double correlationEnergy() {
    return this.correlationEnergy;
  }

  public double energyDelta() {
    return this.energyDelta;
  }

  public double amplitudeDeltaNorm() {
    return this.amplitudeDeltaNorm;
  }
}
