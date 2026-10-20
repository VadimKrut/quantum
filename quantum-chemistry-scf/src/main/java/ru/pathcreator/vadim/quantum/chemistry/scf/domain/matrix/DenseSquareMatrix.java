/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix;

/**
 * Dense square matrix stored row-major in a primitive double array.
 */
public final class DenseSquareMatrix {

  private final int size;
  private final double[] values;

  private DenseSquareMatrix(final int size, final double[] values) {
    this.size = size;
    this.values = values;
  }

  public static DenseSquareMatrix of(final int size, final double[] values) {
    if (size <= 0) {
      throw new IllegalArgumentException("Matrix size must be positive.");
    }
    final int expectedLength = Math.multiplyExact(size, size);
    if (values == null || values.length != expectedLength) {
      throw new IllegalArgumentException("Matrix value length does not match size.");
    }
    for (int i = 0; i < values.length; ++i) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException("Matrix contains non-finite value.");
      }
    }
    return new DenseSquareMatrix(size, values.clone());
  }

  public static DenseSquareMatrix zero(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Matrix size must be positive.");
    }
    return new DenseSquareMatrix(size, new double[Math.multiplyExact(size, size)]);
  }

  public static DenseSquareMatrix identity(final int size) {
    final DenseSquareMatrix matrix = DenseSquareMatrix.zero(size);
    for (int i = 0; i < size; ++i) {
      matrix.values[i * size + i] = 1.0;
    }
    return matrix;
  }

  public int size() {
    return this.size;
  }

  public double at(final int row, final int column) {
    return this.values[this.index(row, column)];
  }

  public double[] copyValues() {
    return this.values.clone();
  }

  public int index(final int row, final int column) {
    if (row < 0 || row >= this.size || column < 0 || column >= this.size) {
      throw new IllegalArgumentException("Matrix index is out of bounds.");
    }
    return row * this.size + column;
  }
}
