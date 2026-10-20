/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor;

/** Плотные one-electron AO матрицы, рассчитанные для одного molecular basis. */
public final class OneElectronIntegralMatrices {

  private final int orbitalCount;
  private final double nuclearRepulsionEnergy;
  private final double[] overlap;
  private final double[] kinetic;
  private final double[] nuclearAttraction;
  private final double[] coreHamiltonian;

  private OneElectronIntegralMatrices(
      final int orbitalCount,
      final double nuclearRepulsionEnergy,
      final double[] overlap,
      final double[] kinetic,
      final double[] nuclearAttraction,
      final double[] coreHamiltonian) {
    this.orbitalCount = orbitalCount;
    this.nuclearRepulsionEnergy = nuclearRepulsionEnergy;
    this.overlap = overlap;
    this.kinetic = kinetic;
    this.nuclearAttraction = nuclearAttraction;
    this.coreHamiltonian = coreHamiltonian;
  }

  public static OneElectronIntegralMatrices of(
      final int orbitalCount,
      final double nuclearRepulsionEnergy,
      final double[] overlap,
      final double[] kinetic,
      final double[] nuclearAttraction) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("One-electron matrix orbital count must be positive.");
    }
    if (!Double.isFinite(nuclearRepulsionEnergy)) {
      throw new IllegalArgumentException("One-electron nuclear energy must be finite.");
    }
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    OneElectronIntegralMatrices.requireArray(overlap, matrixSize, "Overlap matrix");
    OneElectronIntegralMatrices.requireArray(kinetic, matrixSize, "Kinetic matrix");
    OneElectronIntegralMatrices.requireArray(
        nuclearAttraction, matrixSize, "Nuclear attraction matrix");
    final double[] coreHamiltonian = new double[matrixSize];
    for (int i = 0; i < matrixSize; ++i) {
      coreHamiltonian[i] = kinetic[i] + nuclearAttraction[i];
    }
    return new OneElectronIntegralMatrices(
        orbitalCount,
        nuclearRepulsionEnergy,
        overlap.clone(),
        kinetic.clone(),
        nuclearAttraction.clone(),
        coreHamiltonian);
  }

  public int orbitalCount() {
    return this.orbitalCount;
  }

  public double nuclearRepulsionEnergy() {
    return this.nuclearRepulsionEnergy;
  }

  public double[] overlapCopy() {
    return this.overlap.clone();
  }

  public double[] kineticCopy() {
    return this.kinetic.clone();
  }

  public double[] nuclearAttractionCopy() {
    return this.nuclearAttraction.clone();
  }

  public double[] coreHamiltonianCopy() {
    return this.coreHamiltonian.clone();
  }

  public double overlapAt(final int row, final int column) {
    return this.overlap[this.matrixIndex(row, column)];
  }

  public double kineticAt(final int row, final int column) {
    return this.kinetic[this.matrixIndex(row, column)];
  }

  public double nuclearAttractionAt(final int row, final int column) {
    return this.nuclearAttraction[this.matrixIndex(row, column)];
  }

  public double coreHamiltonianAt(final int row, final int column) {
    return this.coreHamiltonian[this.matrixIndex(row, column)];
  }

  public int matrixIndex(final int row, final int column) {
    this.requireOrbital(row);
    this.requireOrbital(column);
    return row * this.orbitalCount + column;
  }

  private void requireOrbital(final int orbital) {
    if (orbital < 0 || orbital >= this.orbitalCount) {
      throw new IllegalArgumentException("One-electron orbital index is out of bounds.");
    }
  }

  private static void requireArray(
      final double[] values, final int expectedLength, final String subjectName) {
    if (values == null || values.length != expectedLength) {
      throw new IllegalArgumentException(subjectName + " length does not match orbital count.");
    }
    for (int i = 0; i < values.length; ++i) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException(subjectName + " contains non-finite value.");
      }
    }
  }
}
