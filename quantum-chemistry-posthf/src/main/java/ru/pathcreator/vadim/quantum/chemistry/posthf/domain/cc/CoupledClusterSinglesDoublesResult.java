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
 * Результат closed-shell CCSD расчета: энергия, singles амплитуды T1, doubles амплитуды T2 и история итераций.
 */
public final class CoupledClusterSinglesDoublesResult {

  private final boolean converged;
  private final int iterations;
  private final double correlationEnergy;
  private final double totalEnergy;
  private final int occupiedOrbitalCount;
  private final int virtualOrbitalCount;
  private final double[] singlesAmplitudes;
  private final double[] doublesAmplitudes;
  private final List<CoupledClusterIterationRecord> iterationHistory;

  private CoupledClusterSinglesDoublesResult(
      final boolean converged,
      final int iterations,
      final double correlationEnergy,
      final double totalEnergy,
      final int occupiedOrbitalCount,
      final int virtualOrbitalCount,
      final double[] singlesAmplitudes,
      final double[] doublesAmplitudes,
      final List<CoupledClusterIterationRecord> iterationHistory) {
    this.converged = converged;
    this.iterations = iterations;
    this.correlationEnergy = correlationEnergy;
    this.totalEnergy = totalEnergy;
    this.occupiedOrbitalCount = occupiedOrbitalCount;
    this.virtualOrbitalCount = virtualOrbitalCount;
    this.singlesAmplitudes = singlesAmplitudes;
    this.doublesAmplitudes = doublesAmplitudes;
    this.iterationHistory = iterationHistory;
  }

  public static CoupledClusterSinglesDoublesResult of(
      final boolean converged,
      final int iterations,
      final double correlationEnergy,
      final double totalEnergy,
      final int occupiedOrbitalCount,
      final int virtualOrbitalCount,
      final double[] singlesAmplitudes,
      final double[] doublesAmplitudes,
      final List<CoupledClusterIterationRecord> iterationHistory) {
    if (iterations <= 0) {
      throw new IllegalArgumentException("CCSD iteration count must be positive.");
    }
    if (!Double.isFinite(correlationEnergy) || !Double.isFinite(totalEnergy)) {
      throw new IllegalArgumentException("CCSD energies must be finite.");
    }
    if (occupiedOrbitalCount <= 0 || virtualOrbitalCount <= 0) {
      throw new IllegalArgumentException(
          "CCSD occupied and virtual orbital counts must be positive.");
    }
    final int expectedSinglesCount = Math.multiplyExact(occupiedOrbitalCount, virtualOrbitalCount);
    final int expectedDoublesCount =
        Math.multiplyExact(
            Math.multiplyExact(occupiedOrbitalCount, occupiedOrbitalCount),
            Math.multiplyExact(virtualOrbitalCount, virtualOrbitalCount));
    CoupledClusterSinglesDoublesResult.requireAmplitudeArray(
        singlesAmplitudes, expectedSinglesCount, "CCSD singles amplitudes");
    CoupledClusterSinglesDoublesResult.requireAmplitudeArray(
        doublesAmplitudes, expectedDoublesCount, "CCSD doubles amplitudes");
    CoupledClusterSinglesDoublesResult.requireHistory(iterations, iterationHistory);
    return new CoupledClusterSinglesDoublesResult(
        converged,
        iterations,
        correlationEnergy,
        totalEnergy,
        occupiedOrbitalCount,
        virtualOrbitalCount,
        singlesAmplitudes.clone(),
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

  public double[] singlesAmplitudesCopy() {
    return this.singlesAmplitudes.clone();
  }

  public double[] doublesAmplitudesCopy() {
    return this.doublesAmplitudes.clone();
  }

  public double singlesAmplitudeAt(final int occupied, final int virtual) {
    if (occupied < 0
        || occupied >= this.occupiedOrbitalCount
        || virtual < 0
        || virtual >= this.virtualOrbitalCount) {
      throw new IndexOutOfBoundsException(
          "CCSD singles amplitude index is outside orbital partition.");
    }
    return this.singlesAmplitudes[
        CoupledClusterSinglesDoublesResult.singlesAmplitudeIndex(
            this.virtualOrbitalCount, occupied, virtual)];
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
      throw new IndexOutOfBoundsException(
          "CCSD doubles amplitude index is outside orbital partition.");
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

  public static int singlesAmplitudeIndex(
      final int virtualCount, final int occupied, final int virtual) {
    return occupied * virtualCount + virtual;
  }

  private static void requireAmplitudeArray(
      final double[] amplitudes, final int expectedLength, final String subjectName) {
    if (amplitudes == null || amplitudes.length != expectedLength) {
      throw new IllegalArgumentException(subjectName + " count must match orbital partition.");
    }
    for (int i = 0; i < amplitudes.length; ++i) {
      if (!Double.isFinite(amplitudes[i])) {
        throw new IllegalArgumentException(subjectName + " contain non-finite value.");
      }
    }
  }

  private static void requireHistory(
      final int iterations, final List<CoupledClusterIterationRecord> iterationHistory) {
    if (iterationHistory == null || iterationHistory.size() != iterations) {
      throw new IllegalArgumentException("CCSD iteration history size must match iteration count.");
    }
    for (int i = 0; i < iterationHistory.size(); ++i) {
      final CoupledClusterIterationRecord record = iterationHistory.get(i);
      if (record == null || record.iteration() != i + 1) {
        throw new IllegalArgumentException("CCSD iteration history must be ordered and non-null.");
      }
    }
  }
}
