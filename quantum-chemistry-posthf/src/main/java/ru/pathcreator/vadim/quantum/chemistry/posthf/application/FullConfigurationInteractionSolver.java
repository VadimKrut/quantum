/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application;

import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianFermionMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant.DeterminantSpaceGenerator;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant.DeterminantSpaceHamiltonianProjector;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.JacobiSymmetricEigenSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.DenseSquareMatrix;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.SymmetricEigenDecomposition;

/**
 * Exact FCI diagonalization by applying the sparse fermionic Hamiltonian to determinants.
 */
public final class FullConfigurationInteractionSolver {

  private final ElectronicHamiltonianFermionMapper fermionMapper;
  private final DeterminantSpaceHamiltonianProjector hamiltonianProjector;
  private final JacobiSymmetricEigenSolver eigenSolver;

  public FullConfigurationInteractionSolver() {
    this.fermionMapper = new ElectronicHamiltonianFermionMapper();
    this.hamiltonianProjector = new DeterminantSpaceHamiltonianProjector();
    this.eigenSolver = new JacobiSymmetricEigenSolver();
  }

  public FciResult solve(final ElectronicHamiltonian hamiltonian) {
    return this.solve(hamiltonian, FciOptions.DEFAULT);
  }

  public FciResult solve(final ElectronicHamiltonian hamiltonian, final FciOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("FCI Hamiltonian must not be null.");
    }
    final FciOptions checkedOptions = options == null ? FciOptions.DEFAULT : options;
    final long[] determinants =
        FullConfigurationInteractionSolver.generateDeterminants(
            hamiltonian.spinOrbitalCount(), hamiltonian.electronCount(), checkedOptions);
    final FermionOperator operator = this.fermionMapper.map(hamiltonian);
    final double[] matrix = this.hamiltonianProjector.projectSymmetric(operator, determinants);
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(determinants.length, matrix));
    final int rootCount = Math.min(checkedOptions.rootCount(), determinants.length);
    final double[] totalEnergies = new double[rootCount];
    final double[] electronicEnergies = new double[rootCount];
    for (int rootIndex = 0; rootIndex < rootCount; ++rootIndex) {
      totalEnergies[rootIndex] = decomposition.eigenvalue(rootIndex);
      electronicEnergies[rootIndex] =
          totalEnergies[rootIndex] - hamiltonian.nuclearRepulsionEnergy();
    }
    return FciResult.of(
        determinants.length,
        electronicEnergies,
        totalEnergies,
        FullConfigurationInteractionSolver.firstEigenvectors(
            determinants.length, rootCount, decomposition.eigenvectorsCopy()));
  }

  private static long[] generateDeterminants(
      final int spinOrbitalCount, final int electronCount, final FciOptions options) {
    if (spinOrbitalCount <= 0 || spinOrbitalCount > Long.SIZE - 1) {
      throw new IllegalArgumentException("FCI spin orbital count must be in [1, 63].");
    }
    if (electronCount <= 0 || electronCount > spinOrbitalCount) {
      throw new IllegalArgumentException("FCI electron count is outside spin orbital bounds.");
    }
    if (options.spinResolvedSector()) {
      return DeterminantSpaceGenerator.spinSector(
          spinOrbitalCount,
          electronCount,
          options.alphaElectronCount(),
          options.betaElectronCount(),
          options.maxDeterminants());
    }
    return DeterminantSpaceGenerator.full(
        spinOrbitalCount, electronCount, options.maxDeterminants());
  }

  private static double[] firstEigenvectors(
      final int size, final int rootCount, final double[] vectors) {
    final double[] result = new double[Math.multiplyExact(size, rootCount)];
    for (int rootIndex = 0; rootIndex < rootCount; ++rootIndex) {
      for (int row = 0; row < size; ++row) {
        result[rootIndex * size + row] = vectors[row * size + rootIndex];
      }
    }
    return result;
  }
}
