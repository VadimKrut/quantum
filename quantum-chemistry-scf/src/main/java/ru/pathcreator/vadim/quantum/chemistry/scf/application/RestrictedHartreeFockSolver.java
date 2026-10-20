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
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.OneElectronIntegralMatrices;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.convergence.ScfIterationRecord;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.DenseSquareMatrix;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.SymmetricEigenDecomposition;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;

/**
 * Restricted closed-shell Hartree-Fock solver for AO integral tensors.
 */
public final class RestrictedHartreeFockSolver {

  private static final int DIIS_MAX_HISTORY = 8;
  private static final int DIIS_START_ITERATION = 1;
  private static final double DENSITY_SCREENING_TOLERANCE = 1.0E-14;

  private final JacobiSymmetricEigenSolver eigenSolver;

  public RestrictedHartreeFockSolver() {
    this.eigenSolver = new JacobiSymmetricEigenSolver();
  }

  public RhfResult solve(final MolecularIntegralTensors tensors, final int electronCount) {
    return this.solve(tensors, electronCount, RhfOptions.DEFAULT);
  }

  public RhfResult solve(
      final OneElectronIntegralMatrices oneElectronMatrices,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final int electronCount) {
    return this.solve(oneElectronMatrices, electronRepulsion, electronCount, RhfOptions.DEFAULT);
  }

  public RhfResult solve(
      final OneElectronIntegralMatrices oneElectronMatrices,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final int electronCount,
      final RhfOptions options) {
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException("RHF one-electron matrices must not be null.");
    }
    if (electronRepulsion == null) {
      throw new IllegalArgumentException("RHF electron repulsion provider must not be null.");
    }
    return this.solveCore(
        oneElectronMatrices.orbitalCount(),
        oneElectronMatrices.nuclearRepulsionEnergy(),
        oneElectronMatrices.overlapCopy(),
        oneElectronMatrices.coreHamiltonianCopy(),
        electronRepulsion,
        electronCount,
        options);
  }

  public RhfResult solve(
      final MolecularIntegralTensors tensors, final int electronCount, final RhfOptions options) {
    if (tensors == null) {
      throw new IllegalArgumentException("RHF tensors must not be null.");
    }
    return this.solveCore(
        tensors.orbitalCount(),
        tensors.nuclearRepulsionEnergy(),
        tensors.overlapCopy(),
        tensors.coreHamiltonianCopy(),
        tensors::electronRepulsionAt,
        electronCount,
        options);
  }

  private RhfResult solveCore(
      final int size,
      final double nuclearRepulsionEnergy,
      final double[] overlap,
      final double[] core,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final int electronCount,
      final RhfOptions options) {
    final RhfOptions checkedOptions = options == null ? RhfOptions.DEFAULT : options;
    if (electronCount <= 0 || (electronCount & 1) != 0) {
      throw new IllegalArgumentException("RHF electron count must be positive and even.");
    }
    final int occupiedCount = electronCount / 2;
    if (occupiedCount > size) {
      throw new IllegalArgumentException("RHF occupied orbital count exceeds basis size.");
    }
    final double[] orthogonalizer = this.symmetricOrthogonalizer(overlap, size);
    double[] density = new double[size * size];
    double[] fock = core.clone();
    double previousEnergy = Double.POSITIVE_INFINITY;
    double electronicEnergy = 0.0;
    double[] coefficients = DenseSquareMatrix.identity(size).copyValues();
    double[] orbitalEnergies = new double[size];
    boolean converged = false;
    int iterations = 0;
    final ScfDiisAccelerator diisState = new ScfDiisAccelerator(size, DIIS_MAX_HISTORY);
    final ArrayList<ScfIterationRecord> iterationHistory = new ArrayList<ScfIterationRecord>();
    for (int iteration = 0; iteration < checkedOptions.maxIterations(); ++iteration) {
      iterations = iteration + 1;
      RestrictedHartreeFockSolver.buildFock(size, core, electronRepulsion, density, fock);
      if (iteration >= DIIS_START_ITERATION) {
        diisState.add(fock, ScfDiisAccelerator.commutatorError(size, fock, density, overlap));
        if (diisState.ready()) {
          fock = diisState.extrapolatedFock();
        }
      }
      final double[] transformedFock =
          RestrictedHartreeFockSolver.transformSymmetric(size, orthogonalizer, fock);
      final SymmetricEigenDecomposition decomposition =
          this.eigenSolver.solve(DenseSquareMatrix.of(size, transformedFock));
      orbitalEnergies = decomposition.eigenvaluesCopy();
      coefficients =
          RestrictedHartreeFockSolver.multiply(
              size, orthogonalizer, decomposition.eigenvectorsCopy());
      final double[] nextDensity =
          RestrictedHartreeFockSolver.buildDensity(size, occupiedCount, coefficients);
      if (checkedOptions.densityMixing() > 0.0 && iteration > 0) {
        RestrictedHartreeFockSolver.mixDensity(
            density, nextDensity, checkedOptions.densityMixing());
      }
      electronicEnergy =
          RestrictedHartreeFockSolver.electronicEnergy(size, nextDensity, core, fock);
      final double deltaEnergy = Math.abs(electronicEnergy - previousEnergy);
      final double densityChange =
          RestrictedHartreeFockSolver.maxAbsoluteDifference(density, nextDensity);
      final boolean iterationConverged =
          deltaEnergy <= checkedOptions.convergenceTolerance()
              && densityChange <= checkedOptions.densityTolerance();
      iterationHistory.add(
          ScfIterationRecord.of(
              iterations,
              electronicEnergy,
              RestrictedHartreeFockSolver.finiteDelta(deltaEnergy),
              densityChange,
              iterationConverged));
      density = nextDensity;
      if (iterationConverged) {
        converged = true;
        break;
      }
      previousEnergy = electronicEnergy;
    }
    return RhfResult.of(
        converged,
        iterations,
        electronicEnergy,
        electronicEnergy + nuclearRepulsionEnergy,
        orbitalEnergies,
        coefficients,
        density,
        List.copyOf(iterationHistory));
  }

  private double[] symmetricOrthogonalizer(final double[] overlap, final int size) {
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(size, overlap));
    final double[] vectors = decomposition.eigenvectorsCopy();
    final double[] scaled = new double[size * size];
    for (int column = 0; column < size; ++column) {
      final double eigenvalue = decomposition.eigenvalue(column);
      if (eigenvalue <= 1.0E-12) {
        throw new IllegalArgumentException("Overlap matrix is not positive definite.");
      }
      final double factor = 1.0 / Math.sqrt(eigenvalue);
      for (int row = 0; row < size; ++row) {
        scaled[row * size + column] = vectors[row * size + column] * factor;
      }
    }
    return RestrictedHartreeFockSolver.multiplyTransposedRight(size, scaled, vectors);
  }

  private static void buildFock(
      final int size,
      final double[] core,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final double[] density,
      final double[] fock) {
    System.arraycopy(core, 0, fock, 0, core.length);
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double correction = 0.0;
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s < size; ++s) {
            final double densityValue = density[r * size + s];
            if (Math.abs(densityValue) <= DENSITY_SCREENING_TOLERANCE) {
              continue;
            }
            final double coulomb =
                electronRepulsion.screenedOut(p, q, r, s)
                    ? 0.0
                    : electronRepulsion.valueAt(p, q, r, s);
            final double exchange =
                electronRepulsion.screenedOut(p, r, q, s)
                    ? 0.0
                    : electronRepulsion.valueAt(p, r, q, s);
            correction += densityValue * (coulomb - 0.5 * exchange);
          }
        }
        fock[p * size + q] += correction;
      }
    }
  }

  private static double[] buildDensity(
      final int size, final int occupiedCount, final double[] coefficients) {
    final double[] density = new double[size * size];
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double value = 0.0;
        for (int i = 0; i < occupiedCount; ++i) {
          value += 2.0 * coefficients[p * size + i] * coefficients[q * size + i];
        }
        density[p * size + q] = value;
      }
    }
    return density;
  }

  private static void mixDensity(
      final double[] oldDensity, final double[] nextDensity, final double mixing) {
    for (int i = 0; i < nextDensity.length; ++i) {
      nextDensity[i] = (1.0 - mixing) * nextDensity[i] + mixing * oldDensity[i];
    }
  }

  private static double electronicEnergy(
      final int size, final double[] newDensity, final double[] core, final double[] fock) {
    double energy = 0.0;
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        energy += 0.5 * newDensity[p * size + q] * (core[p * size + q] + fock[p * size + q]);
      }
    }
    return energy;
  }

  private static double finiteDelta(final double deltaEnergy) {
    if (Double.isFinite(deltaEnergy)) {
      return deltaEnergy;
    }
    return Double.MAX_VALUE;
  }

  private static double maxAbsoluteDifference(final double[] first, final double[] second) {
    double result = 0.0;
    for (int i = 0; i < first.length; ++i) {
      result = Math.max(result, Math.abs(first[i] - second[i]));
    }
    return result;
  }

  private static double[] transformSymmetric(
      final int size, final double[] transform, final double[] matrix) {
    final double[] temporary = RestrictedHartreeFockSolver.multiply(size, matrix, transform);
    return RestrictedHartreeFockSolver.multiplyTransposedLeft(size, transform, temporary);
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

  private static double[] multiplyTransposedLeft(
      final int size, final double[] left, final double[] right) {
    final double[] result = new double[size * size];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[k * size + row] * right[k * size + column];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }

  private static double[] multiplyTransposedRight(
      final int size, final double[] left, final double[] right) {
    final double[] result = new double[size * size];
    for (int row = 0; row < size; ++row) {
      for (int column = 0; column < size; ++column) {
        double value = 0.0;
        for (int k = 0; k < size; ++k) {
          value += left[row * size + k] * right[column * size + k];
        }
        result[row * size + column] = value;
      }
    }
    return result;
  }
}
