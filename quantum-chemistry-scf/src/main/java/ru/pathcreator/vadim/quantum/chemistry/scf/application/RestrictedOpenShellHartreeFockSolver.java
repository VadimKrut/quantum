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
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rohf.RohfResult;

/**
 * Restricted open-shell Hartree-Fock solver с общей пространственной MO-базой и разными alpha/beta occupation.
 */
public final class RestrictedOpenShellHartreeFockSolver {

  private static final int DIIS_MAX_HISTORY = 8;
  private static final int DIIS_START_ITERATION = 1;
  private static final double DENSITY_SCREENING_TOLERANCE = 1.0E-14;
  private static final double OVERLAP_EIGENVALUE_FLOOR = 1.0E-12;

  private final JacobiSymmetricEigenSolver eigenSolver;

  public RestrictedOpenShellHartreeFockSolver() {
    this.eigenSolver = new JacobiSymmetricEigenSolver();
  }

  public RohfResult solve(
      final MolecularIntegralTensors tensors, final int electronCount, final int spinMultiplicity) {
    return this.solve(tensors, electronCount, spinMultiplicity, RhfOptions.DEFAULT);
  }

  public RohfResult solve(
      final MolecularIntegralTensors tensors,
      final int electronCount,
      final int spinMultiplicity,
      final RhfOptions options) {
    if (tensors == null) {
      throw new IllegalArgumentException("ROHF tensors must not be null.");
    }
    return this.solveCore(
        tensors.orbitalCount(),
        tensors.nuclearRepulsionEnergy(),
        tensors.overlapCopy(),
        tensors.coreHamiltonianCopy(),
        tensors::electronRepulsionAt,
        electronCount,
        spinMultiplicity,
        options);
  }

  public RohfResult solve(
      final OneElectronIntegralMatrices oneElectronMatrices,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final int electronCount,
      final int spinMultiplicity) {
    return this.solve(
        oneElectronMatrices,
        electronRepulsion,
        electronCount,
        spinMultiplicity,
        RhfOptions.DEFAULT);
  }

  public RohfResult solve(
      final OneElectronIntegralMatrices oneElectronMatrices,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final int electronCount,
      final int spinMultiplicity,
      final RhfOptions options) {
    if (oneElectronMatrices == null) {
      throw new IllegalArgumentException("ROHF one-electron matrices must not be null.");
    }
    if (electronRepulsion == null) {
      throw new IllegalArgumentException("ROHF electron repulsion provider must not be null.");
    }
    return this.solveCore(
        oneElectronMatrices.orbitalCount(),
        oneElectronMatrices.nuclearRepulsionEnergy(),
        oneElectronMatrices.overlapCopy(),
        oneElectronMatrices.coreHamiltonianCopy(),
        electronRepulsion,
        electronCount,
        spinMultiplicity,
        options);
  }

  private RohfResult solveCore(
      final int size,
      final double nuclearRepulsionEnergy,
      final double[] overlap,
      final double[] core,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final int electronCount,
      final int spinMultiplicity,
      final RhfOptions options) {
    final RhfOptions checkedOptions = options == null ? RhfOptions.DEFAULT : options;
    final int[] occupation =
        RestrictedOpenShellHartreeFockSolver.spinElectronCounts(
            electronCount, spinMultiplicity, size);
    final int alphaElectronCount = occupation[0];
    final int betaElectronCount = occupation[1];
    final int openShellElectronCount = alphaElectronCount - betaElectronCount;
    final double[] orthogonalizer = this.symmetricOrthogonalizer(overlap, size);
    double[] alphaDensity = new double[size * size];
    double[] betaDensity = new double[size * size];
    double[] closedShellDensity = new double[size * size];
    double[] openShellDensity = new double[size * size];
    double[] alphaFock = core.clone();
    double[] betaFock = core.clone();
    double[] effectiveFock = core.clone();
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
      RestrictedOpenShellHartreeFockSolver.buildFockPair(
          size, core, electronRepulsion, alphaDensity, betaDensity, alphaFock, betaFock);
      RestrictedOpenShellHartreeFockSolver.buildEffectiveFock(alphaFock, betaFock, effectiveFock);
      if (iteration >= DIIS_START_ITERATION) {
        diisState.add(
            effectiveFock,
            ScfDiisAccelerator.commutatorError(
                size,
                effectiveFock,
                RestrictedOpenShellHartreeFockSolver.totalDensity(alphaDensity, betaDensity),
                overlap));
        if (diisState.ready()) {
          effectiveFock = diisState.extrapolatedFock();
        }
      }
      final SymmetricEigenDecomposition decomposition =
          this.eigenSolver.solve(
              DenseSquareMatrix.of(
                  size,
                  RestrictedOpenShellHartreeFockSolver.transformSymmetric(
                      size, orthogonalizer, effectiveFock)));
      orbitalEnergies = decomposition.eigenvaluesCopy();
      coefficients =
          RestrictedOpenShellHartreeFockSolver.multiply(
              size, orthogonalizer, decomposition.eigenvectorsCopy());
      final double[] nextClosedShellDensity =
          RestrictedOpenShellHartreeFockSolver.buildDensity(size, betaElectronCount, coefficients);
      final double[] nextOpenShellDensity =
          RestrictedOpenShellHartreeFockSolver.buildDensityRange(
              size, betaElectronCount, openShellElectronCount, coefficients);
      final double[] nextBetaDensity = nextClosedShellDensity;
      final double[] nextAlphaDensity =
          RestrictedOpenShellHartreeFockSolver.sumDensity(
              nextClosedShellDensity, nextOpenShellDensity);
      if (checkedOptions.densityMixing() > 0.0 && iteration > 0) {
        RestrictedOpenShellHartreeFockSolver.mixDensity(
            alphaDensity, nextAlphaDensity, checkedOptions.densityMixing());
        RestrictedOpenShellHartreeFockSolver.mixDensity(
            betaDensity, nextBetaDensity, checkedOptions.densityMixing());
        RestrictedOpenShellHartreeFockSolver.mixDensity(
            closedShellDensity, nextClosedShellDensity, checkedOptions.densityMixing());
        RestrictedOpenShellHartreeFockSolver.mixDensity(
            openShellDensity, nextOpenShellDensity, checkedOptions.densityMixing());
      }
      electronicEnergy =
          RestrictedOpenShellHartreeFockSolver.electronicEnergy(
              size, nextAlphaDensity, nextBetaDensity, core, alphaFock, betaFock);
      final double deltaEnergy = Math.abs(electronicEnergy - previousEnergy);
      final double densityChange =
          Math.max(
              RestrictedOpenShellHartreeFockSolver.maxAbsoluteDifference(
                  alphaDensity, nextAlphaDensity),
              RestrictedOpenShellHartreeFockSolver.maxAbsoluteDifference(
                  betaDensity, nextBetaDensity));
      final boolean iterationConverged =
          deltaEnergy <= checkedOptions.convergenceTolerance()
              && densityChange <= checkedOptions.densityTolerance();
      iterationHistory.add(
          ScfIterationRecord.of(
              iterations,
              electronicEnergy,
              RestrictedOpenShellHartreeFockSolver.finiteDelta(deltaEnergy),
              densityChange,
              iterationConverged));
      alphaDensity = nextAlphaDensity;
      betaDensity = nextBetaDensity;
      closedShellDensity = nextClosedShellDensity;
      openShellDensity = nextOpenShellDensity;
      if (iterationConverged) {
        converged = true;
        break;
      }
      previousEnergy = electronicEnergy;
    }
    final double exactSpinSquared =
        RestrictedOpenShellHartreeFockSolver.exactSpinSquared(
            alphaElectronCount, betaElectronCount);
    return RohfResult.of(
        converged,
        iterations,
        alphaElectronCount,
        betaElectronCount,
        betaElectronCount,
        openShellElectronCount,
        electronicEnergy,
        electronicEnergy + nuclearRepulsionEnergy,
        exactSpinSquared,
        0.0,
        orbitalEnergies,
        coefficients,
        closedShellDensity,
        openShellDensity,
        alphaDensity,
        betaDensity,
        List.copyOf(iterationHistory));
  }

  private static int[] spinElectronCounts(
      final int electronCount, final int spinMultiplicity, final int orbitalCount) {
    if (electronCount <= 0) {
      throw new IllegalArgumentException("ROHF electron count must be positive.");
    }
    if (spinMultiplicity <= 1) {
      throw new IllegalArgumentException(
          "ROHF spin multiplicity must describe an open-shell state.");
    }
    final int unpairedCount = spinMultiplicity - 1;
    if (unpairedCount > electronCount || ((electronCount - unpairedCount) & 1) != 0) {
      throw new IllegalArgumentException(
          "ROHF spin multiplicity is incompatible with electron count.");
    }
    final int betaElectronCount = (electronCount - unpairedCount) / 2;
    final int alphaElectronCount = betaElectronCount + unpairedCount;
    if (alphaElectronCount > orbitalCount || betaElectronCount > orbitalCount) {
      throw new IllegalArgumentException("ROHF occupied orbital count exceeds basis size.");
    }
    return new int[] {alphaElectronCount, betaElectronCount};
  }

  private double[] symmetricOrthogonalizer(final double[] overlap, final int size) {
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(size, overlap));
    final double[] vectors = decomposition.eigenvectorsCopy();
    final double[] scaled = new double[size * size];
    for (int column = 0; column < size; ++column) {
      final double eigenvalue = decomposition.eigenvalue(column);
      if (eigenvalue <= OVERLAP_EIGENVALUE_FLOOR) {
        throw new IllegalArgumentException("Overlap matrix is not positive definite.");
      }
      final double factor = 1.0 / Math.sqrt(eigenvalue);
      for (int row = 0; row < size; ++row) {
        scaled[row * size + column] = vectors[row * size + column] * factor;
      }
    }
    return RestrictedOpenShellHartreeFockSolver.multiplyTransposedRight(size, scaled, vectors);
  }

  private static void buildFockPair(
      final int size,
      final double[] core,
      final ElectronRepulsionIntegralProvider electronRepulsion,
      final double[] alphaDensity,
      final double[] betaDensity,
      final double[] alphaFock,
      final double[] betaFock) {
    System.arraycopy(core, 0, alphaFock, 0, core.length);
    System.arraycopy(core, 0, betaFock, 0, core.length);
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double alphaCorrection = 0.0;
        double betaCorrection = 0.0;
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s < size; ++s) {
            final int densityIndex = r * size + s;
            final double totalDensity = alphaDensity[densityIndex] + betaDensity[densityIndex];
            final double alphaDensityValue = alphaDensity[densityIndex];
            final double betaDensityValue = betaDensity[densityIndex];
            if (Math.abs(totalDensity) <= DENSITY_SCREENING_TOLERANCE
                && Math.abs(alphaDensityValue) <= DENSITY_SCREENING_TOLERANCE
                && Math.abs(betaDensityValue) <= DENSITY_SCREENING_TOLERANCE) {
              continue;
            }
            final double coulomb =
                Math.abs(totalDensity) <= DENSITY_SCREENING_TOLERANCE
                        || electronRepulsion.screenedOut(p, q, r, s)
                    ? 0.0
                    : electronRepulsion.valueAt(p, q, r, s);
            final double exchange =
                Math.abs(alphaDensityValue) <= DENSITY_SCREENING_TOLERANCE
                            && Math.abs(betaDensityValue) <= DENSITY_SCREENING_TOLERANCE
                        || electronRepulsion.screenedOut(p, r, q, s)
                    ? 0.0
                    : electronRepulsion.valueAt(p, r, q, s);
            alphaCorrection += totalDensity * coulomb - alphaDensityValue * exchange;
            betaCorrection += totalDensity * coulomb - betaDensityValue * exchange;
          }
        }
        alphaFock[p * size + q] += alphaCorrection;
        betaFock[p * size + q] += betaCorrection;
      }
    }
  }

  private static void buildEffectiveFock(
      final double[] alphaFock, final double[] betaFock, final double[] effectiveFock) {
    for (int i = 0; i < effectiveFock.length; ++i) {
      effectiveFock[i] = 0.5 * (alphaFock[i] + betaFock[i]);
    }
  }

  private static double[] buildDensity(
      final int size, final int occupiedCount, final double[] coefficients) {
    return RestrictedOpenShellHartreeFockSolver.buildDensityRange(
        size, 0, occupiedCount, coefficients);
  }

  private static double[] buildDensityRange(
      final int size,
      final int firstOccupiedOrbital,
      final int occupiedCount,
      final double[] coefficients) {
    final double[] density = new double[size * size];
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double value = 0.0;
        for (int i = 0; i < occupiedCount; ++i) {
          final int orbital = firstOccupiedOrbital + i;
          value += coefficients[p * size + orbital] * coefficients[q * size + orbital];
        }
        density[p * size + q] = value;
      }
    }
    return density;
  }

  private static double[] sumDensity(final double[] first, final double[] second) {
    final double[] result = new double[first.length];
    for (int i = 0; i < result.length; ++i) {
      result[i] = first[i] + second[i];
    }
    return result;
  }

  private static double[] totalDensity(final double[] alphaDensity, final double[] betaDensity) {
    return RestrictedOpenShellHartreeFockSolver.sumDensity(alphaDensity, betaDensity);
  }

  private static void mixDensity(
      final double[] oldDensity, final double[] nextDensity, final double mixing) {
    for (int i = 0; i < nextDensity.length; ++i) {
      nextDensity[i] = (1.0 - mixing) * nextDensity[i] + mixing * oldDensity[i];
    }
  }

  private static double electronicEnergy(
      final int size,
      final double[] alphaDensity,
      final double[] betaDensity,
      final double[] core,
      final double[] alphaFock,
      final double[] betaFock) {
    double energy = 0.0;
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        final int index = p * size + q;
        final double totalDensity = alphaDensity[index] + betaDensity[index];
        energy +=
            0.5
                * (totalDensity * core[index]
                    + alphaDensity[index] * alphaFock[index]
                    + betaDensity[index] * betaFock[index]);
      }
    }
    return energy;
  }

  private static double exactSpinSquared(
      final int alphaElectronCount, final int betaElectronCount) {
    final double spin = 0.5 * Math.abs(alphaElectronCount - betaElectronCount);
    return spin * (spin + 1.0);
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
    final double[] temporary =
        RestrictedOpenShellHartreeFockSolver.multiply(size, matrix, transform);
    return RestrictedOpenShellHartreeFockSolver.multiplyTransposedLeft(size, transform, temporary);
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
