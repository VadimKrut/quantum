/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Ускоряет SCF-сходимость методом DIIS по ошибке коммутатора FDS-SDF.
 */
final class ScfDiisAccelerator {

  private static final double REGULARIZATION = 1.0E-14;

  private final int size;
  private final int maxHistory;
  private final ArrayList<double[]> fockHistory;
  private final ArrayList<double[]> errorHistory;

  ScfDiisAccelerator(final int size, final int maxHistory) {
    if (size <= 0) {
      throw new IllegalArgumentException("SCF DIIS matrix size must be positive.");
    }
    if (maxHistory < 2) {
      throw new IllegalArgumentException("SCF DIIS max history must be at least two.");
    }
    this.size = size;
    this.maxHistory = maxHistory;
    this.fockHistory = new ArrayList<double[]>(maxHistory);
    this.errorHistory = new ArrayList<double[]>(maxHistory);
  }

  void add(final double[] fock, final double[] error) {
    ScfDiisAccelerator.requireMatrix(fock, this.size, "SCF DIIS Fock matrix");
    ScfDiisAccelerator.requireMatrix(error, this.size, "SCF DIIS error matrix");
    if (this.fockHistory.size() == this.maxHistory) {
      this.fockHistory.remove(0);
      this.errorHistory.remove(0);
    }
    this.fockHistory.add(fock.clone());
    this.errorHistory.add(error.clone());
  }

  boolean ready() {
    return this.fockHistory.size() >= 2;
  }

  double[] extrapolatedFock() {
    if (!this.ready()) {
      throw new IllegalStateException("SCF DIIS accelerator needs at least two history records.");
    }
    final int historySize = this.fockHistory.size();
    final int equationSize = historySize + 1;
    final double[][] matrix = new double[equationSize][equationSize];
    final double[] rhs = new double[equationSize];
    for (int row = 0; row < historySize; ++row) {
      for (int column = 0; column < historySize; ++column) {
        matrix[row][column] =
            ScfDiisAccelerator.dot(this.errorHistory.get(row), this.errorHistory.get(column));
      }
      matrix[row][row] += REGULARIZATION;
      matrix[row][historySize] = -1.0;
      matrix[historySize][row] = -1.0;
    }
    rhs[historySize] = -1.0;
    final double[] coefficients = ScfDiisAccelerator.solveLinearSystem(matrix, rhs);
    final double[] result = new double[this.size * this.size];
    for (int i = 0; i < historySize; ++i) {
      final double coefficient = coefficients[i];
      final double[] fock = this.fockHistory.get(i);
      for (int valueIndex = 0; valueIndex < result.length; ++valueIndex) {
        result[valueIndex] += coefficient * fock[valueIndex];
      }
    }
    return result;
  }

  static double[] commutatorError(
      final int size, final double[] fock, final double[] density, final double[] overlap) {
    ScfDiisAccelerator.requireMatrix(fock, size, "SCF DIIS Fock matrix");
    ScfDiisAccelerator.requireMatrix(density, size, "SCF DIIS density matrix");
    ScfDiisAccelerator.requireMatrix(overlap, size, "SCF DIIS overlap matrix");
    final double[] fds =
        ScfDiisAccelerator.multiply(
            size, ScfDiisAccelerator.multiply(size, fock, density), overlap);
    final double[] sdf =
        ScfDiisAccelerator.multiply(
            size, ScfDiisAccelerator.multiply(size, overlap, density), fock);
    for (int i = 0; i < fds.length; ++i) {
      fds[i] -= sdf[i];
    }
    return fds;
  }

  private static void requireMatrix(
      final double[] matrix, final int size, final String subjectName) {
    if (matrix == null) {
      throw new IllegalArgumentException(subjectName + " must not be null.");
    }
    if (matrix.length != size * size) {
      throw new IllegalArgumentException(subjectName + " size is inconsistent.");
    }
    for (int i = 0; i < matrix.length; ++i) {
      if (!Double.isFinite(matrix[i])) {
        throw new IllegalArgumentException(subjectName + " contains non-finite value.");
      }
    }
  }

  private static double[] multiply(final int size, final double[] left, final double[] right) {
    final double[] result = new double[size * size];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[row * size + k] * right[k * size + column];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }

  private static double dot(final double[] first, final double[] second) {
    double value = 0.0;
    for (int i = 0; i < first.length; ++i) {
      value += first[i] * second[i];
    }
    return value;
  }

  private static double[] solveLinearSystem(
      final double[][] sourceMatrix, final double[] sourceRightHandSide) {
    final int size = sourceRightHandSide.length;
    final double[][] matrix = new double[size][size];
    final double[] rhs = sourceRightHandSide.clone();
    for (int row = 0; row < size; ++row) {
      matrix[row] = sourceMatrix[row].clone();
    }
    for (int pivot = 0; pivot < size; ++pivot) {
      int bestRow = pivot;
      double bestValue = Math.abs(matrix[pivot][pivot]);
      for (int row = pivot + 1; row < size; ++row) {
        final double candidate = Math.abs(matrix[row][pivot]);
        if (candidate > bestValue) {
          bestValue = candidate;
          bestRow = row;
        }
      }
      if (bestValue <= 1.0E-20) {
        return ScfDiisAccelerator.equalWeights(size - 1);
      }
      if (bestRow != pivot) {
        final double[] temporaryRow = matrix[pivot];
        matrix[pivot] = matrix[bestRow];
        matrix[bestRow] = temporaryRow;
        final double temporaryValue = rhs[pivot];
        rhs[pivot] = rhs[bestRow];
        rhs[bestRow] = temporaryValue;
      }
      final double pivotValue = matrix[pivot][pivot];
      for (int column = pivot; column < size; ++column) {
        matrix[pivot][column] /= pivotValue;
      }
      rhs[pivot] /= pivotValue;
      for (int row = 0; row < size; ++row) {
        if (row == pivot) {
          continue;
        }
        final double factor = matrix[row][pivot];
        if (factor == 0.0) {
          continue;
        }
        for (int column = pivot; column < size; ++column) {
          matrix[row][column] -= factor * matrix[pivot][column];
        }
        rhs[row] -= factor * rhs[pivot];
      }
    }
    return rhs;
  }

  private static double[] equalWeights(final int historySize) {
    final double[] weights = new double[historySize + 1];
    Arrays.fill(weights, 0, historySize, 1.0 / (double) historySize);
    return weights;
  }
}
