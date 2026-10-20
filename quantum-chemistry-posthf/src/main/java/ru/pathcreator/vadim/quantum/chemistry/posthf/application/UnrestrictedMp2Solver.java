/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application;

import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2.UnrestrictedMp2Result;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;

/** Считает unrestricted spin-orbital MP2 correction поверх UHF alpha/beta reference. */
public final class UnrestrictedMp2Solver {

  private static final double DENOMINATOR_EPSILON = 1.0E-14;

  public UnrestrictedMp2Result solve(
      final MolecularIntegralTensors atomicOrbitalTensors, final UhfResult uhfResult) {
    if (atomicOrbitalTensors == null) {
      throw new IllegalArgumentException("UMP2 AO tensors must not be null.");
    }
    if (uhfResult == null) {
      throw new IllegalArgumentException("UMP2 UHF result must not be null.");
    }
    final int orbitalCount = atomicOrbitalTensors.orbitalCount();
    final int alphaOccupiedCount = uhfResult.alphaElectronCount();
    final int betaOccupiedCount = uhfResult.betaElectronCount();
    if (alphaOccupiedCount < 0 || betaOccupiedCount < 0) {
      throw new IllegalArgumentException("UMP2 spin electron counts must not be negative.");
    }
    if (alphaOccupiedCount > orbitalCount || betaOccupiedCount > orbitalCount) {
      throw new IllegalArgumentException("UMP2 occupied orbital count exceeds basis size.");
    }
    if (alphaOccupiedCount == orbitalCount && betaOccupiedCount == orbitalCount) {
      throw new IllegalArgumentException(
          "UMP2 requires at least one virtual alpha or beta orbital.");
    }
    final double[] alphaEnergies = uhfResult.alphaOrbitalEnergiesCopy();
    final double[] betaEnergies = uhfResult.betaOrbitalEnergiesCopy();
    final double[] alphaCoefficients = uhfResult.alphaMolecularOrbitalCoefficientsCopy();
    final double[] betaCoefficients = uhfResult.betaMolecularOrbitalCoefficientsCopy();
    UnrestrictedMp2Solver.requireOrbitalVector(
        alphaEnergies, orbitalCount, "UMP2 alpha orbital energies");
    UnrestrictedMp2Solver.requireOrbitalVector(
        betaEnergies, orbitalCount, "UMP2 beta orbital energies");
    UnrestrictedMp2Solver.requireCoefficientMatrix(
        alphaCoefficients, orbitalCount, "UMP2 alpha MO coefficients");
    UnrestrictedMp2Solver.requireCoefficientMatrix(
        betaCoefficients, orbitalCount, "UMP2 beta MO coefficients");
    final double[] alphaAlphaEri =
        UnrestrictedMp2Solver.transformElectronRepulsion(
            atomicOrbitalTensors,
            alphaCoefficients,
            alphaCoefficients,
            alphaCoefficients,
            alphaCoefficients);
    final double[] betaBetaEri =
        UnrestrictedMp2Solver.transformElectronRepulsion(
            atomicOrbitalTensors,
            betaCoefficients,
            betaCoefficients,
            betaCoefficients,
            betaCoefficients);
    final double[] alphaBetaEri =
        UnrestrictedMp2Solver.transformElectronRepulsion(
            atomicOrbitalTensors,
            alphaCoefficients,
            betaCoefficients,
            alphaCoefficients,
            betaCoefficients);
    double sameSpinCorrelation = 0.0;
    double oppositeSpinCorrelation = 0.0;
    sameSpinCorrelation +=
        UnrestrictedMp2Solver.sameSpinCorrelation(
            orbitalCount, alphaOccupiedCount, alphaEnergies, alphaAlphaEri);
    sameSpinCorrelation +=
        UnrestrictedMp2Solver.sameSpinCorrelation(
            orbitalCount, betaOccupiedCount, betaEnergies, betaBetaEri);
    oppositeSpinCorrelation =
        UnrestrictedMp2Solver.oppositeSpinCorrelation(
            orbitalCount,
            alphaOccupiedCount,
            betaOccupiedCount,
            alphaEnergies,
            betaEnergies,
            alphaBetaEri);
    final double correlationEnergy = sameSpinCorrelation + oppositeSpinCorrelation;
    return UnrestrictedMp2Result.of(
        correlationEnergy,
        uhfResult.totalEnergy() + correlationEnergy,
        sameSpinCorrelation,
        oppositeSpinCorrelation,
        alphaOccupiedCount,
        betaOccupiedCount,
        orbitalCount - alphaOccupiedCount,
        orbitalCount - betaOccupiedCount);
  }

  private static double sameSpinCorrelation(
      final int orbitalCount,
      final int occupiedCount,
      final double[] orbitalEnergies,
      final double[] transformedEri) {
    if (occupiedCount == 0 || occupiedCount == orbitalCount) {
      return 0.0;
    }
    double result = 0.0;
    for (int i = 0; i < occupiedCount; ++i) {
      for (int j = 0; j < occupiedCount; ++j) {
        for (int a = occupiedCount; a < orbitalCount; ++a) {
          for (int b = occupiedCount; b < orbitalCount; ++b) {
            final double direct =
                transformedEri[UnrestrictedMp2Solver.tensorIndex(orbitalCount, i, j, a, b)];
            final double exchange =
                transformedEri[UnrestrictedMp2Solver.tensorIndex(orbitalCount, i, j, b, a)];
            final double antisymmetrized = direct - exchange;
            final double denominator =
                orbitalEnergies[i] + orbitalEnergies[j] - orbitalEnergies[a] - orbitalEnergies[b];
            UnrestrictedMp2Solver.requireDenominator(denominator);
            result += 0.25 * antisymmetrized * antisymmetrized / denominator;
          }
        }
      }
    }
    return result;
  }

  private static double oppositeSpinCorrelation(
      final int orbitalCount,
      final int alphaOccupiedCount,
      final int betaOccupiedCount,
      final double[] alphaEnergies,
      final double[] betaEnergies,
      final double[] alphaBetaEri) {
    if (alphaOccupiedCount == 0
        || betaOccupiedCount == 0
        || alphaOccupiedCount == orbitalCount
        || betaOccupiedCount == orbitalCount) {
      return 0.0;
    }
    double result = 0.0;
    for (int i = 0; i < alphaOccupiedCount; ++i) {
      for (int j = 0; j < betaOccupiedCount; ++j) {
        for (int a = alphaOccupiedCount; a < orbitalCount; ++a) {
          for (int b = betaOccupiedCount; b < orbitalCount; ++b) {
            final double direct =
                alphaBetaEri[UnrestrictedMp2Solver.tensorIndex(orbitalCount, i, j, a, b)];
            final double denominator =
                alphaEnergies[i] + betaEnergies[j] - alphaEnergies[a] - betaEnergies[b];
            UnrestrictedMp2Solver.requireDenominator(denominator);
            result += direct * direct / denominator;
          }
        }
      }
    }
    return result;
  }

  private static double[] transformElectronRepulsion(
      final MolecularIntegralTensors atomicOrbitalTensors,
      final double[] firstCoefficients,
      final double[] secondCoefficients,
      final double[] thirdCoefficients,
      final double[] fourthCoefficients) {
    final int size = atomicOrbitalTensors.orbitalCount();
    final double[] transformed = new double[UnrestrictedMp2Solver.denseSpinBlockTensorSize(size)];
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s < size; ++s) {
            double value = 0.0;
            for (int mu = 0; mu < size; ++mu) {
              final double first = firstCoefficients[mu * size + p];
              if (first == 0.0) {
                continue;
              }
              for (int nu = 0; nu < size; ++nu) {
                final double second = secondCoefficients[nu * size + q];
                if (second == 0.0) {
                  continue;
                }
                for (int lambda = 0; lambda < size; ++lambda) {
                  final double third = thirdCoefficients[lambda * size + r];
                  if (third == 0.0) {
                    continue;
                  }
                  for (int sigma = 0; sigma < size; ++sigma) {
                    final double fourth = fourthCoefficients[sigma * size + s];
                    if (fourth == 0.0) {
                      continue;
                    }
                    value +=
                        first
                            * second
                            * third
                            * fourth
                            * atomicOrbitalTensors.electronRepulsionAt(mu, nu, lambda, sigma);
                  }
                }
              }
            }
            transformed[UnrestrictedMp2Solver.tensorIndex(size, p, q, r, s)] = value;
          }
        }
      }
    }
    return transformed;
  }

  private static int tensorIndex(
      final int size, final int first, final int second, final int third, final int fourth) {
    return ((first * size + second) * size + third) * size + fourth;
  }

  private static int denseSpinBlockTensorSize(final int orbitalCount) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("UMP2 orbital count must be positive.");
    }
    final long count = orbitalCount;
    final long pair = Math.multiplyExact(count, count);
    final long tensorSize = Math.multiplyExact(pair, pair);
    if (tensorSize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "UMP2 dense spin-block transformed ERI tensor requires "
              + tensorSize
              + " double elements; use active-space reduction or a direct/sparse post-HF path.");
    }
    return (int) tensorSize;
  }

  private static void requireDenominator(final double denominator) {
    if (!Double.isFinite(denominator) || Math.abs(denominator) <= DENOMINATOR_EPSILON) {
      throw new IllegalArgumentException("UMP2 denominator is numerically singular.");
    }
  }

  private static void requireOrbitalVector(
      final double[] values, final int orbitalCount, final String subjectName) {
    if (values == null || values.length != orbitalCount) {
      throw new IllegalArgumentException(subjectName + " length must match orbital count.");
    }
    for (int i = 0; i < values.length; ++i) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException(subjectName + " contains non-finite value.");
      }
    }
  }

  private static void requireCoefficientMatrix(
      final double[] values, final int orbitalCount, final String subjectName) {
    if (values == null || values.length != orbitalCount * orbitalCount) {
      throw new IllegalArgumentException(subjectName + " size must match orbital count.");
    }
    for (int i = 0; i < values.length; ++i) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException(subjectName + " contains non-finite value.");
      }
    }
  }
}
