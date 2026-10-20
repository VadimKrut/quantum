/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci;

/**
 * Результат exact active-space diagonalization с ground/excited roots и determinant statistics.
 */
public final class FciResult {

  private final int determinantCount;
  private final int rootCount;
  private final double[] electronicEnergies;
  private final double[] totalEnergies;
  private final double[] eigenvectors;

  private FciResult(
      final int determinantCount,
      final int rootCount,
      final double[] electronicEnergies,
      final double[] totalEnergies,
      final double[] eigenvectors) {
    this.determinantCount = determinantCount;
    this.rootCount = rootCount;
    this.electronicEnergies = electronicEnergies;
    this.totalEnergies = totalEnergies;
    this.eigenvectors = eigenvectors;
  }

  public static FciResult of(
      final int determinantCount,
      final double electronicEnergy,
      final double totalEnergy,
      final double[] eigenvector) {
    return FciResult.of(
        determinantCount, new double[] {electronicEnergy}, new double[] {totalEnergy}, eigenvector);
  }

  public static FciResult of(
      final int determinantCount,
      final double[] electronicEnergies,
      final double[] totalEnergies,
      final double[] eigenvectors) {
    if (determinantCount <= 0) {
      throw new IllegalArgumentException("FCI determinant count must be positive.");
    }
    if (electronicEnergies == null || electronicEnergies.length == 0) {
      throw new IllegalArgumentException("FCI electronic roots must not be empty.");
    }
    if (totalEnergies == null || totalEnergies.length != electronicEnergies.length) {
      throw new IllegalArgumentException("FCI total root count must match electronic root count.");
    }
    if (eigenvectors == null
        || eigenvectors.length != Math.multiplyExact(determinantCount, electronicEnergies.length)) {
      throw new IllegalArgumentException(
          "FCI eigenvector matrix length must match determinant and root count.");
    }
    for (int i = 0; i < electronicEnergies.length; ++i) {
      if (!Double.isFinite(electronicEnergies[i]) || !Double.isFinite(totalEnergies[i])) {
        throw new IllegalArgumentException("FCI energies must be finite.");
      }
    }
    return new FciResult(
        determinantCount,
        electronicEnergies.length,
        electronicEnergies.clone(),
        totalEnergies.clone(),
        eigenvectors.clone());
  }

  public int determinantCount() {
    return this.determinantCount;
  }

  public int rootCount() {
    return this.rootCount;
  }

  public double electronicEnergy() {
    return this.electronicEnergy(0);
  }

  public double totalEnergy() {
    return this.totalEnergy(0);
  }

  public double electronicEnergy(final int rootIndex) {
    this.requireRootIndex(rootIndex);
    return this.electronicEnergies[rootIndex];
  }

  public double totalEnergy(final int rootIndex) {
    this.requireRootIndex(rootIndex);
    return this.totalEnergies[rootIndex];
  }

  public double[] electronicEnergiesCopy() {
    return this.electronicEnergies.clone();
  }

  public double[] totalEnergiesCopy() {
    return this.totalEnergies.clone();
  }

  public double[] eigenvectorCopy() {
    return this.eigenvectorCopy(0);
  }

  public double[] eigenvectorCopy(final int rootIndex) {
    this.requireRootIndex(rootIndex);
    final double[] vector = new double[this.determinantCount];
    System.arraycopy(
        this.eigenvectors, rootIndex * this.determinantCount, vector, 0, this.determinantCount);
    return vector;
  }

  private void requireRootIndex(final int rootIndex) {
    if (rootIndex < 0 || rootIndex >= this.rootCount) {
      throw new IllegalArgumentException("FCI root index is out of bounds.");
    }
  }
}
