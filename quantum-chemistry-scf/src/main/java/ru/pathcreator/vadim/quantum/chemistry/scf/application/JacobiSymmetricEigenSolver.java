/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.DenseSquareMatrix;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.SymmetricEigenDecomposition;

/**
 * Deterministic Jacobi solver for small and medium symmetric matrices.
 */
public final class JacobiSymmetricEigenSolver {

  private static final int MAX_SWEEPS_FACTOR = 64;
  private static final double DEFAULT_TOLERANCE = 1.0E-12;

  public SymmetricEigenDecomposition solve(final DenseSquareMatrix matrix) {
    return this.solve(matrix, DEFAULT_TOLERANCE);
  }

  public SymmetricEigenDecomposition solve(final DenseSquareMatrix matrix, final double tolerance) {
    if (matrix == null) {
      throw new IllegalArgumentException("Eigen solver matrix must not be null.");
    }
    if (!Double.isFinite(tolerance) || tolerance <= 0.0) {
      throw new IllegalArgumentException("Eigen solver tolerance must be finite and positive.");
    }
    final int size = matrix.size();
    final double[] values = matrix.copyValues();
    final double[] vectors = DenseSquareMatrix.identity(size).copyValues();
    final int maxSweeps = Math.max(1, MAX_SWEEPS_FACTOR * size * size);
    for (int sweep = 0; sweep < maxSweeps; ++sweep) {
      int pivotRow = 0;
      int pivotColumn = 1;
      double maxOffDiagonal = 0.0;
      for (int row = 0; row < size; ++row) {
        for (int column = row + 1; column < size; ++column) {
          final double absolute = Math.abs(values[row * size + column]);
          if (absolute <= maxOffDiagonal) {
            continue;
          }
          maxOffDiagonal = absolute;
          pivotRow = row;
          pivotColumn = column;
        }
      }
      if (maxOffDiagonal <= tolerance) {
        break;
      }
      JacobiSymmetricEigenSolver.rotate(values, vectors, size, pivotRow, pivotColumn);
    }
    final double[] eigenvalues = new double[size];
    for (int i = 0; i < size; ++i) {
      eigenvalues[i] = values[i * size + i];
    }
    JacobiSymmetricEigenSolver.sort(eigenvalues, vectors, size);
    return SymmetricEigenDecomposition.of(size, eigenvalues, vectors);
  }

  private static void rotate(
      final double[] values,
      final double[] vectors,
      final int size,
      final int row,
      final int column) {
    final double diagonalDifference = values[column * size + column] - values[row * size + row];
    final double angle = 0.5 * Math.atan2(2.0 * values[row * size + column], diagonalDifference);
    final double cosine = Math.cos(angle);
    final double sine = Math.sin(angle);
    for (int k = 0; k < size; ++k) {
      final double rowValue = values[row * size + k];
      final double columnValue = values[column * size + k];
      values[row * size + k] = cosine * rowValue - sine * columnValue;
      values[column * size + k] = sine * rowValue + cosine * columnValue;
    }
    for (int k = 0; k < size; ++k) {
      final double rowValue = values[k * size + row];
      final double columnValue = values[k * size + column];
      values[k * size + row] = cosine * rowValue - sine * columnValue;
      values[k * size + column] = sine * rowValue + cosine * columnValue;
    }
    values[row * size + column] = 0.0;
    values[column * size + row] = 0.0;
    for (int k = 0; k < size; ++k) {
      final double rowValue = vectors[k * size + row];
      final double columnValue = vectors[k * size + column];
      vectors[k * size + row] = cosine * rowValue - sine * columnValue;
      vectors[k * size + column] = sine * rowValue + cosine * columnValue;
    }
  }

  private static void sort(
      final double[] eigenvalues, final double[] eigenvectors, final int size) {
    for (int i = 0; i < size - 1; ++i) {
      int best = i;
      for (int j = i + 1; j < size; ++j) {
        if (eigenvalues[j] < eigenvalues[best]) {
          best = j;
        }
      }
      if (best == i) {
        continue;
      }
      final double eigenvalue = eigenvalues[i];
      eigenvalues[i] = eigenvalues[best];
      eigenvalues[best] = eigenvalue;
      for (int row = 0; row < size; ++row) {
        final double component = eigenvectors[row * size + i];
        eigenvectors[row * size + i] = eigenvectors[row * size + best];
        eigenvectors[row * size + best] = component;
      }
    }
  }
}
