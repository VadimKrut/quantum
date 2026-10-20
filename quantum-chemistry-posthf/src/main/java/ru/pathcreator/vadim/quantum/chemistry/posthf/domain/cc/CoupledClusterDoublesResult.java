/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc;

import java.util.List;

/**
 * Результат closed-shell CCD расчета.
 */
public final class CoupledClusterDoublesResult {

  private final boolean converged;
  private final int iterations;
  private final double correlationEnergy;
  private final double totalEnergy;
  private final int occupiedOrbitalCount;
  private final int virtualOrbitalCount;
  private final double[] doublesAmplitudes;
  private final List<CoupledClusterIterationRecord> iterationHistory;

  private CoupledClusterDoublesResult(
      final boolean converged,
      final int iterations,
      final double correlationEnergy,
      final double totalEnergy,
      final int occupiedOrbitalCount,
      final int virtualOrbitalCount,
      final double[] doublesAmplitudes,
      final List<CoupledClusterIterationRecord> iterationHistory) {
    this.converged = converged;
    this.iterations = iterations;
    this.correlationEnergy = correlationEnergy;
    this.totalEnergy = totalEnergy;
    this.occupiedOrbitalCount = occupiedOrbitalCount;
    this.virtualOrbitalCount = virtualOrbitalCount;
    this.doublesAmplitudes = doublesAmplitudes;
    this.iterationHistory = iterationHistory;
  }

  public static CoupledClusterDoublesResult of(
      final boolean converged,
      final int iterations,
      final double correlationEnergy,
      final double totalEnergy,
      final int occupiedOrbitalCount,
      final int virtualOrbitalCount,
      final double[] doublesAmplitudes,
      final List<CoupledClusterIterationRecord> iterationHistory) {
    if (iterations <= 0) {
      throw new IllegalArgumentException("CCD iteration count must be positive.");
    }
    if (!Double.isFinite(correlationEnergy) || !Double.isFinite(totalEnergy)) {
      throw new IllegalArgumentException("CCD energies must be finite.");
    }
    if (occupiedOrbitalCount <= 0 || virtualOrbitalCount <= 0) {
      throw new IllegalArgumentException(
          "CCD occupied and virtual orbital counts must be positive.");
    }
    final int expectedAmplitudeCount =
        Math.multiplyExact(
            Math.multiplyExact(occupiedOrbitalCount, occupiedOrbitalCount),
            Math.multiplyExact(virtualOrbitalCount, virtualOrbitalCount));
    if (doublesAmplitudes == null || doublesAmplitudes.length != expectedAmplitudeCount) {
      throw new IllegalArgumentException(
          "CCD doubles amplitude count must match orbital partition.");
    }
    for (int i = 0; i < doublesAmplitudes.length; ++i) {
      if (!Double.isFinite(doublesAmplitudes[i])) {
        throw new IllegalArgumentException("CCD doubles amplitudes contain non-finite value.");
      }
    }
    if (iterationHistory == null || iterationHistory.size() != iterations) {
      throw new IllegalArgumentException("CCD iteration history size must match iteration count.");
    }
    for (int i = 0; i < iterationHistory.size(); ++i) {
      final CoupledClusterIterationRecord record = iterationHistory.get(i);
      if (record == null || record.iteration() != i + 1) {
        throw new IllegalArgumentException("CCD iteration history must be ordered and non-null.");
      }
    }
    return new CoupledClusterDoublesResult(
        converged,
        iterations,
        correlationEnergy,
        totalEnergy,
        occupiedOrbitalCount,
        virtualOrbitalCount,
        doublesAmplitudes.clone(),
        List.copyOf(iterationHistory));
  }

  public boolean converged() {
    return this.converged;
  }

  public int iterations() {
    return this.iterations;
  }

  public double correlationEnergy() {
    return this.correlationEnergy;
  }

  public double totalEnergy() {
    return this.totalEnergy;
  }

  public int occupiedOrbitalCount() {
    return this.occupiedOrbitalCount;
  }

  public int virtualOrbitalCount() {
    return this.virtualOrbitalCount;
  }

  public double[] doublesAmplitudesCopy() {
    return this.doublesAmplitudes.clone();
  }

  public double doublesAmplitudeAt(
      final int firstOccupied,
      final int secondOccupied,
      final int firstVirtual,
      final int secondVirtual) {
    if (firstOccupied < 0
        || firstOccupied >= this.occupiedOrbitalCount
        || secondOccupied < 0
        || secondOccupied >= this.occupiedOrbitalCount
        || firstVirtual < 0
        || firstVirtual >= this.virtualOrbitalCount
        || secondVirtual < 0
        || secondVirtual >= this.virtualOrbitalCount) {
      throw new IndexOutOfBoundsException("CCD amplitude index is outside orbital partition.");
    }
    return this.doublesAmplitudes[
        CoupledClusterDoublesResult.amplitudeIndex(
            this.occupiedOrbitalCount,
            this.virtualOrbitalCount,
            firstOccupied,
            secondOccupied,
            firstVirtual,
            secondVirtual)];
  }

  public List<CoupledClusterIterationRecord> iterationHistory() {
    return this.iterationHistory;
  }

  public static int amplitudeIndex(
      final int occupiedCount,
      final int virtualCount,
      final int firstOccupied,
      final int secondOccupied,
      final int firstVirtual,
      final int secondVirtual) {
    return ((firstOccupied * occupiedCount + secondOccupied) * virtualCount + firstVirtual)
            * virtualCount
        + secondVirtual;
  }
}
