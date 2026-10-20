/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor;

public final class DipoleIntegralMatrices {

  private final int orbitalCount;
  private final double[] x;
  private final double[] y;
  private final double[] z;

  private DipoleIntegralMatrices(
      final int orbitalCount, final double[] x, final double[] y, final double[] z) {
    this.orbitalCount = orbitalCount;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public static DipoleIntegralMatrices of(
      final int orbitalCount, final double[] x, final double[] y, final double[] z) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Dipole integral orbital count must be positive.");
    }
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    DipoleIntegralMatrices.requireArray(x, matrixSize, "X dipole integral matrix");
    DipoleIntegralMatrices.requireArray(y, matrixSize, "Y dipole integral matrix");
    DipoleIntegralMatrices.requireArray(z, matrixSize, "Z dipole integral matrix");
    return new DipoleIntegralMatrices(orbitalCount, x.clone(), y.clone(), z.clone());
  }

  public int orbitalCount() {
    return this.orbitalCount;
  }

  public double[] xCopy() {
    return this.x.clone();
  }

  public double[] yCopy() {
    return this.y.clone();
  }

  public double[] zCopy() {
    return this.z.clone();
  }

  public double valueAt(final int axis, final int row, final int column) {
    if (row < 0 || row >= this.orbitalCount || column < 0 || column >= this.orbitalCount) {
      throw new IllegalArgumentException("Dipole integral orbital index is out of bounds.");
    }
    final int index = row * this.orbitalCount + column;
    if (axis == 0) {
      return this.x[index];
    }
    if (axis == 1) {
      return this.y[index];
    }
    if (axis == 2) {
      return this.z[index];
    }
    throw new IllegalArgumentException("Dipole integral axis must be 0, 1 or 2.");
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
