/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application.matrix;

import java.util.Arrays;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;

/**
 * Плотная комплексная матрица для малых независимых проверок операторов.
 */
public final class DenseComplexMatrix {

  private final int size;
  private final double[] real;
  private final double[] imaginary;

  private DenseComplexMatrix(final int size, final double[] real, final double[] imaginary) {
    this.size = size;
    this.real = real;
    this.imaginary = imaginary;
  }

  public static DenseComplexMatrix zero(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Dense complex matrix size must be positive.");
    }
    return new DenseComplexMatrix(size, new double[size * size], new double[size * size]);
  }

  public int size() {
    return this.size;
  }

  public ComplexCoefficient value(final int row, final int column) {
    this.requireIndex(row);
    this.requireIndex(column);
    final int offset = this.offset(row, column);
    return ComplexCoefficient.of(this.real[offset], this.imaginary[offset]);
  }

  public void add(final int row, final int column, final ComplexCoefficient value) {
    this.requireIndex(row);
    this.requireIndex(column);
    if (value == null) {
      throw new IllegalArgumentException("Dense complex matrix value must not be null.");
    }
    final int offset = this.offset(row, column);
    this.real[offset] += value.real();
    this.imaginary[offset] += value.imaginary();
  }

  public DenseComplexMatrix multiply(final DenseComplexMatrix other) {
    if (other == null) {
      throw new IllegalArgumentException("Dense complex matrix multiplier must not be null.");
    }
    if (this.size != other.size) {
      throw new IllegalArgumentException("Dense complex matrix sizes must match.");
    }
    final DenseComplexMatrix result = DenseComplexMatrix.zero(this.size);
    for (int row = 0; row < this.size; ++row) {
      for (int middle = 0; middle < this.size; ++middle) {
        final int leftOffset = this.offset(row, middle);
        final double leftReal = this.real[leftOffset];
        final double leftImaginary = this.imaginary[leftOffset];
        if (leftReal == 0.0 && leftImaginary == 0.0) {
          continue;
        }
        for (int column = 0; column < this.size; ++column) {
          final int rightOffset = other.offset(middle, column);
          final double rightReal = other.real[rightOffset];
          final double rightImaginary = other.imaginary[rightOffset];
          if (rightReal == 0.0 && rightImaginary == 0.0) {
            continue;
          }
          final int resultOffset = result.offset(row, column);
          result.real[resultOffset] += leftReal * rightReal - leftImaginary * rightImaginary;
          result.imaginary[resultOffset] += leftReal * rightImaginary + leftImaginary * rightReal;
        }
      }
    }
    return result;
  }

  public ComplexCoefficient trace() {
    double traceReal = 0.0;
    double traceImaginary = 0.0;
    for (int i = 0; i < this.size; ++i) {
      final int offset = this.offset(i, i);
      traceReal += this.real[offset];
      traceImaginary += this.imaginary[offset];
    }
    return ComplexCoefficient.of(traceReal, traceImaginary);
  }

  public double[] realSpectralMoments(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("Spectral moment count must be positive.");
    }
    final double[] moments = new double[count];
    DenseComplexMatrix power = this;
    for (int i = 0; i < count; ++i) {
      final ComplexCoefficient trace = power.trace();
      moments[i] = trace.real();
      power = power.multiply(this);
    }
    return moments;
  }

  public double maxHermitianDeviation() {
    double maxDeviation = 0.0;
    for (int row = 0; row < this.size; ++row) {
      for (int column = 0; column < this.size; ++column) {
        final int leftOffset = this.offset(row, column);
        final int rightOffset = this.offset(column, row);
        maxDeviation =
            Math.max(maxDeviation, Math.abs(this.real[leftOffset] - this.real[rightOffset]));
        maxDeviation =
            Math.max(
                maxDeviation, Math.abs(this.imaginary[leftOffset] + this.imaginary[rightOffset]));
      }
    }
    return maxDeviation;
  }

  public double maxAbsoluteEntry() {
    double maxValue = 0.0;
    for (int i = 0; i < this.real.length; ++i) {
      maxValue = Math.max(maxValue, Math.hypot(this.real[i], this.imaginary[i]));
    }
    return maxValue;
  }

  public double maxAbsoluteDifference(final DenseComplexMatrix other) {
    if (other == null) {
      throw new IllegalArgumentException("Dense complex matrix comparator must not be null.");
    }
    if (this.size != other.size) {
      throw new IllegalArgumentException("Dense complex matrix sizes must match.");
    }
    double maxDifference = 0.0;
    for (int i = 0; i < this.real.length; ++i) {
      maxDifference =
          Math.max(
              maxDifference,
              Math.hypot(this.real[i] - other.real[i], this.imaginary[i] - other.imaginary[i]));
    }
    return maxDifference;
  }

  public boolean equalsWithinTolerance(final DenseComplexMatrix other, final double tolerance) {
    if (other == null) {
      throw new IllegalArgumentException("Dense complex matrix comparator must not be null.");
    }
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "Dense complex matrix tolerance must be finite and non-negative.");
    }
    if (this.size != other.size) {
      return false;
    }
    for (int i = 0; i < this.real.length; ++i) {
      if (Math.abs(this.real[i] - other.real[i]) > tolerance) {
        return false;
      }
      if (Math.abs(this.imaginary[i] - other.imaginary[i]) > tolerance) {
        return false;
      }
    }
    return true;
  }

  private int offset(final int row, final int column) {
    return row * this.size + column;
  }

  private void requireIndex(final int index) {
    if (index < 0 || index >= this.size) {
      throw new IndexOutOfBoundsException("Dense complex matrix index is outside matrix.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DenseComplexMatrix)) {
      return false;
    }
    final DenseComplexMatrix matrix = (DenseComplexMatrix) other;
    return this.size == matrix.size
        && Arrays.equals(this.real, matrix.real)
        && Arrays.equals(this.imaginary, matrix.imaginary);
  }

  public int hashCode() {
    int result = this.size;
    result = 31 * result + Arrays.hashCode(this.real);
    result = 31 * result + Arrays.hashCode(this.imaginary);
    return result;
  }
}
