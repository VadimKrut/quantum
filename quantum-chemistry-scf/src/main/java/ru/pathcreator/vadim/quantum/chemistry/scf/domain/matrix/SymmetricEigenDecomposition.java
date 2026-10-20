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
 * Eigenvalues and eigenvectors of a real symmetric matrix.
 */
public final class SymmetricEigenDecomposition {

  private final int size;
  private final double[] eigenvalues;
  private final double[] eigenvectors;

  private SymmetricEigenDecomposition(
      final int size, final double[] eigenvalues, final double[] eigenvectors) {
    this.size = size;
    this.eigenvalues = eigenvalues;
    this.eigenvectors = eigenvectors;
  }

  public static SymmetricEigenDecomposition of(
      final int size, final double[] eigenvalues, final double[] eigenvectors) {
    if (size <= 0) {
      throw new IllegalArgumentException("Eigen decomposition size must be positive.");
    }
    if (eigenvalues == null || eigenvalues.length != size) {
      throw new IllegalArgumentException("Eigenvalue count does not match size.");
    }
    if (eigenvectors == null || eigenvectors.length != Math.multiplyExact(size, size)) {
      throw new IllegalArgumentException("Eigenvector matrix size is invalid.");
    }
    return new SymmetricEigenDecomposition(size, eigenvalues.clone(), eigenvectors.clone());
  }

  public int size() {
    return this.size;
  }

  public double eigenvalue(final int index) {
    if (index < 0 || index >= this.size) {
      throw new IllegalArgumentException("Eigenvalue index is out of bounds.");
    }
    return this.eigenvalues[index];
  }

  public double eigenvectorComponent(final int row, final int column) {
    if (row < 0 || row >= this.size || column < 0 || column >= this.size) {
      throw new IllegalArgumentException("Eigenvector index is out of bounds.");
    }
    return this.eigenvectors[row * this.size + column];
  }

  public double[] eigenvaluesCopy() {
    return this.eigenvalues.clone();
  }

  public double[] eigenvectorsCopy() {
    return this.eigenvectors.clone();
  }
}
