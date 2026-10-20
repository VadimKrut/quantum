/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.PackedElectronRepulsionTensor;

/** Переводит AO-интегралы в MO-интегралы через матрицу коэффициентов SCF. */
public final class MolecularOrbitalIntegralTransformer {

  public MolecularIntegralTensors transform(
      final MolecularIntegralTensors atomicOrbitalTensors,
      final double[] molecularOrbitalCoefficients) {
    if (atomicOrbitalTensors == null) {
      throw new IllegalArgumentException("AO integral tensors must not be null.");
    }
    final int orbitalCount = atomicOrbitalTensors.orbitalCount();
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    final int tensorSize = Math.multiplyExact(matrixSize, matrixSize);
    MolecularOrbitalIntegralTransformer.requireCoefficientMatrix(
        molecularOrbitalCoefficients, matrixSize);
    final double[] overlap =
        MolecularOrbitalIntegralTransformer.transformOneElectron(
            orbitalCount, atomicOrbitalTensors.overlapCopy(), molecularOrbitalCoefficients);
    final double[] core =
        MolecularOrbitalIntegralTransformer.transformOneElectron(
            orbitalCount, atomicOrbitalTensors.coreHamiltonianCopy(), molecularOrbitalCoefficients);
    final double[] zero = new double[matrixSize];
    final double[] electronRepulsion =
        MolecularOrbitalIntegralTransformer.transformTwoElectron(
            orbitalCount,
            atomicOrbitalTensors.electronRepulsionCopy(),
            molecularOrbitalCoefficients);
    MolecularOrbitalIntegralTransformer.symmetrizeTwoElectronTensor(
        orbitalCount, electronRepulsion);
    if (electronRepulsion.length != tensorSize) {
      throw new IllegalStateException("MO ERI tensor length mismatch.");
    }
    return MolecularIntegralTensors.ofPacked(
        orbitalCount,
        atomicOrbitalTensors.nuclearRepulsionEnergy(),
        overlap,
        core,
        zero,
        MolecularOrbitalIntegralTransformer.packSymmetricTwoElectronTensor(
            orbitalCount, electronRepulsion));
  }

  public MolecularIntegralTensors transformLeadingOrbitalWindow(
      final MolecularIntegralTensors atomicOrbitalTensors,
      final double[] molecularOrbitalCoefficients,
      final int orbitalWindowCount) {
    return this.transformOrbitalWindow(
        atomicOrbitalTensors, molecularOrbitalCoefficients, 0, orbitalWindowCount);
  }

  public MolecularIntegralTensors transformOrbitalWindow(
      final MolecularIntegralTensors atomicOrbitalTensors,
      final double[] molecularOrbitalCoefficients,
      final int firstOrbital,
      final int orbitalWindowCount) {
    if (atomicOrbitalTensors == null) {
      throw new IllegalArgumentException("AO integral tensors must not be null.");
    }
    final int orbitalCount = atomicOrbitalTensors.orbitalCount();
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    if (firstOrbital < 0 || firstOrbital >= orbitalCount) {
      throw new IllegalArgumentException("MO first orbital must be in [0, orbital count).");
    }
    if (orbitalWindowCount <= 0 || Math.addExact(firstOrbital, orbitalWindowCount) > orbitalCount) {
      throw new IllegalArgumentException("MO orbital window must fit into orbital count.");
    }
    MolecularOrbitalIntegralTransformer.requireCoefficientMatrix(
        molecularOrbitalCoefficients, matrixSize);
    final int windowMatrixSize = Math.multiplyExact(orbitalWindowCount, orbitalWindowCount);
    final int windowTensorSize = Math.multiplyExact(windowMatrixSize, windowMatrixSize);
    final double[] overlap =
        MolecularOrbitalIntegralTransformer.transformOneElectronWindow(
            orbitalCount,
            firstOrbital,
            orbitalWindowCount,
            atomicOrbitalTensors.overlapCopy(),
            molecularOrbitalCoefficients);
    final double[] core =
        MolecularOrbitalIntegralTransformer.transformOneElectronWindow(
            orbitalCount,
            firstOrbital,
            orbitalWindowCount,
            atomicOrbitalTensors.coreHamiltonianCopy(),
            molecularOrbitalCoefficients);
    final double[] zero = new double[windowMatrixSize];
    final double[] electronRepulsion =
        MolecularOrbitalIntegralTransformer.transformTwoElectronWindow(
            orbitalCount,
            firstOrbital,
            orbitalWindowCount,
            atomicOrbitalTensors.electronRepulsionCopy(),
            molecularOrbitalCoefficients);
    MolecularOrbitalIntegralTransformer.symmetrizeTwoElectronTensor(
        orbitalWindowCount, electronRepulsion);
    if (electronRepulsion.length != windowTensorSize) {
      throw new IllegalStateException("Windowed MO ERI tensor length mismatch.");
    }
    return MolecularIntegralTensors.ofPacked(
        orbitalWindowCount,
        atomicOrbitalTensors.nuclearRepulsionEnergy(),
        overlap,
        core,
        zero,
        MolecularOrbitalIntegralTransformer.packSymmetricTwoElectronTensor(
            orbitalWindowCount, electronRepulsion));
  }

  private static double[] transformOneElectron(
      final int size, final double[] atomic, final double[] coefficients) {
    final double[] temporary = new double[size * size];
    final double[] transformed = new double[size * size];
    for (int mu = 0; mu < size; ++mu) {
      for (int q = 0; q < size; ++q) {
        double value = 0.0;
        for (int nu = 0; nu < size; ++nu) {
          value += atomic[mu * size + nu] * coefficients[nu * size + q];
        }
        temporary[mu * size + q] = value;
      }
    }
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        double value = 0.0;
        for (int mu = 0; mu < size; ++mu) {
          value += coefficients[mu * size + p] * temporary[mu * size + q];
        }
        transformed[p * size + q] = value;
      }
    }
    return transformed;
  }

  private static double[] transformOneElectronWindow(
      final int sourceSize,
      final int firstOrbital,
      final int targetSize,
      final double[] atomic,
      final double[] coefficients) {
    final double[] temporary = new double[sourceSize * targetSize];
    final double[] transformed = new double[targetSize * targetSize];
    for (int mu = 0; mu < sourceSize; ++mu) {
      for (int q = 0; q < targetSize; ++q) {
        final int sourceQ = firstOrbital + q;
        double value = 0.0;
        for (int nu = 0; nu < sourceSize; ++nu) {
          value += atomic[mu * sourceSize + nu] * coefficients[nu * sourceSize + sourceQ];
        }
        temporary[mu * targetSize + q] = value;
      }
    }
    for (int p = 0; p < targetSize; ++p) {
      final int sourceP = firstOrbital + p;
      for (int q = 0; q < targetSize; ++q) {
        double value = 0.0;
        for (int mu = 0; mu < sourceSize; ++mu) {
          value += coefficients[mu * sourceSize + sourceP] * temporary[mu * targetSize + q];
        }
        transformed[p * targetSize + q] = value;
      }
    }
    return transformed;
  }

  private static double[] transformTwoElectron(
      final int size, final double[] atomic, final double[] coefficients) {
    final int tensorSize =
        Math.multiplyExact(Math.multiplyExact(size, size), Math.multiplyExact(size, size));
    final double[] firstPass = new double[tensorSize];
    final double[] secondPass = new double[tensorSize];
    final double[] thirdPass = new double[tensorSize];
    final double[] transformed = new double[tensorSize];
    for (int p = 0; p < size; ++p) {
      for (int nu = 0; nu < size; ++nu) {
        for (int lambda = 0; lambda < size; ++lambda) {
          for (int sigma = 0; sigma < size; ++sigma) {
            double value = 0.0;
            for (int mu = 0; mu < size; ++mu) {
              value +=
                  coefficients[mu * size + p]
                      * atomic[
                          MolecularOrbitalIntegralTransformer.tensorIndex(
                              size, mu, nu, lambda, sigma)];
            }
            firstPass[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, nu, lambda, sigma)] =
                value;
          }
        }
      }
    }
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        for (int lambda = 0; lambda < size; ++lambda) {
          for (int sigma = 0; sigma < size; ++sigma) {
            double value = 0.0;
            for (int nu = 0; nu < size; ++nu) {
              value +=
                  coefficients[nu * size + q]
                      * firstPass[
                          MolecularOrbitalIntegralTransformer.tensorIndex(
                              size, p, nu, lambda, sigma)];
            }
            secondPass[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, lambda, sigma)] =
                value;
          }
        }
      }
    }
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        for (int r = 0; r < size; ++r) {
          for (int sigma = 0; sigma < size; ++sigma) {
            double value = 0.0;
            for (int lambda = 0; lambda < size; ++lambda) {
              value +=
                  coefficients[lambda * size + r]
                      * secondPass[
                          MolecularOrbitalIntegralTransformer.tensorIndex(
                              size, p, q, lambda, sigma)];
            }
            thirdPass[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, r, sigma)] =
                value;
          }
        }
      }
    }
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s < size; ++s) {
            double value = 0.0;
            for (int sigma = 0; sigma < size; ++sigma) {
              value +=
                  coefficients[sigma * size + s]
                      * thirdPass[
                          MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, r, sigma)];
            }
            transformed[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, r, s)] = value;
          }
        }
      }
    }
    return transformed;
  }

  private static double[] transformTwoElectronWindow(
      final int sourceSize,
      final int firstOrbital,
      final int targetSize,
      final double[] atomic,
      final double[] coefficients) {
    final int sourceMatrixSize = Math.multiplyExact(sourceSize, sourceSize);
    final int sourceTensorSize = Math.multiplyExact(sourceMatrixSize, sourceMatrixSize);
    if (atomic == null || atomic.length != sourceTensorSize) {
      throw new IllegalArgumentException("AO ERI tensor length must match orbital count.");
    }
    final double[] firstPass =
        new double
            [Math.multiplyExact(targetSize, Math.multiplyExact(sourceSize, sourceMatrixSize))];
    final double[] secondPass =
        new double
            [Math.multiplyExact(Math.multiplyExact(targetSize, targetSize), sourceMatrixSize)];
    final double[] thirdPass =
        new double
            [Math.multiplyExact(
                Math.multiplyExact(targetSize, targetSize),
                Math.multiplyExact(targetSize, sourceSize))];
    final double[] transformed =
        new double
            [Math.multiplyExact(
                Math.multiplyExact(targetSize, targetSize),
                Math.multiplyExact(targetSize, targetSize))];
    for (int p = 0; p < targetSize; ++p) {
      final int sourceP = firstOrbital + p;
      for (int nu = 0; nu < sourceSize; ++nu) {
        for (int lambda = 0; lambda < sourceSize; ++lambda) {
          for (int sigma = 0; sigma < sourceSize; ++sigma) {
            double value = 0.0;
            for (int mu = 0; mu < sourceSize; ++mu) {
              value +=
                  coefficients[mu * sourceSize + sourceP]
                      * atomic[
                          MolecularOrbitalIntegralTransformer.tensorIndex(
                              sourceSize, mu, nu, lambda, sigma)];
            }
            firstPass[
                    MolecularOrbitalIntegralTransformer.windowTensorIndex(
                        sourceSize, sourceSize, sourceSize, p, nu, lambda, sigma)] =
                value;
          }
        }
      }
    }
    for (int p = 0; p < targetSize; ++p) {
      for (int q = 0; q < targetSize; ++q) {
        final int sourceQ = firstOrbital + q;
        for (int lambda = 0; lambda < sourceSize; ++lambda) {
          for (int sigma = 0; sigma < sourceSize; ++sigma) {
            double value = 0.0;
            for (int nu = 0; nu < sourceSize; ++nu) {
              value +=
                  coefficients[nu * sourceSize + sourceQ]
                      * firstPass[
                          MolecularOrbitalIntegralTransformer.windowTensorIndex(
                              sourceSize, sourceSize, sourceSize, p, nu, lambda, sigma)];
            }
            secondPass[
                    MolecularOrbitalIntegralTransformer.windowTensorIndex(
                        targetSize, sourceSize, sourceSize, p, q, lambda, sigma)] =
                value;
          }
        }
      }
    }
    for (int p = 0; p < targetSize; ++p) {
      for (int q = 0; q < targetSize; ++q) {
        for (int r = 0; r < targetSize; ++r) {
          final int sourceR = firstOrbital + r;
          for (int sigma = 0; sigma < sourceSize; ++sigma) {
            double value = 0.0;
            for (int lambda = 0; lambda < sourceSize; ++lambda) {
              value +=
                  coefficients[lambda * sourceSize + sourceR]
                      * secondPass[
                          MolecularOrbitalIntegralTransformer.windowTensorIndex(
                              targetSize, sourceSize, sourceSize, p, q, lambda, sigma)];
            }
            thirdPass[
                    MolecularOrbitalIntegralTransformer.windowTensorIndex(
                        targetSize, targetSize, sourceSize, p, q, r, sigma)] =
                value;
          }
        }
      }
    }
    for (int p = 0; p < targetSize; ++p) {
      for (int q = 0; q < targetSize; ++q) {
        for (int r = 0; r < targetSize; ++r) {
          for (int s = 0; s < targetSize; ++s) {
            final int sourceS = firstOrbital + s;
            double value = 0.0;
            for (int sigma = 0; sigma < sourceSize; ++sigma) {
              value +=
                  coefficients[sigma * sourceSize + sourceS]
                      * thirdPass[
                          MolecularOrbitalIntegralTransformer.windowTensorIndex(
                              targetSize, targetSize, sourceSize, p, q, r, sigma)];
            }
            transformed[MolecularOrbitalIntegralTransformer.tensorIndex(targetSize, p, q, r, s)] =
                value;
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

  private static void symmetrizeTwoElectronTensor(final int size, final double[] values) {
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q <= p; ++q) {
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s <= r; ++s) {
            final int firstPair = MolecularOrbitalIntegralTransformer.pairIndex(p, q);
            final int secondPair = MolecularOrbitalIntegralTransformer.pairIndex(r, s);
            if (secondPair > firstPair) {
              continue;
            }
            final double value =
                0.125
                    * (values[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, r, s)]
                        + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, q, p, r, s)]
                        + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, s, r)]
                        + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, q, p, s, r)]
                        + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, r, s, p, q)]
                        + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, s, r, p, q)]
                        + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, r, s, q, p)]
                        + values[
                            MolecularOrbitalIntegralTransformer.tensorIndex(size, s, r, q, p)]);
            MolecularOrbitalIntegralTransformer.setEriSymmetryFamily(
                size, values, p, q, r, s, value);
          }
        }
      }
    }
  }

  private static PackedElectronRepulsionTensor packSymmetricTwoElectronTensor(
      final int size, final double[] values) {
    final PackedElectronRepulsionTensor.Builder builder =
        PackedElectronRepulsionTensor.builder(size);
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q <= p; ++q) {
        for (int r = 0; r < size; ++r) {
          for (int s = 0; s <= r; ++s) {
            final int firstPair = MolecularOrbitalIntegralTransformer.pairIndex(p, q);
            final int secondPair = MolecularOrbitalIntegralTransformer.pairIndex(r, s);
            if (secondPair > firstPair) {
              continue;
            }
            builder.set(
                p,
                q,
                r,
                s,
                MolecularOrbitalIntegralTransformer.symmetryFamilyAverage(
                    size, values, p, q, r, s));
          }
        }
      }
    }
    return builder.build();
  }

  private static double symmetryFamilyAverage(
      final int size, final double[] values, final int p, final int q, final int r, final int s) {
    return 0.125
        * (values[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, r, s)]
            + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, q, p, r, s)]
            + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, s, r)]
            + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, q, p, s, r)]
            + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, r, s, p, q)]
            + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, s, r, p, q)]
            + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, r, s, q, p)]
            + values[MolecularOrbitalIntegralTransformer.tensorIndex(size, s, r, q, p)]);
  }

  private static void setEriSymmetryFamily(
      final int size,
      final double[] values,
      final int p,
      final int q,
      final int r,
      final int s,
      final double value) {
    values[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, r, s)] = value;
    values[MolecularOrbitalIntegralTransformer.tensorIndex(size, q, p, r, s)] = value;
    values[MolecularOrbitalIntegralTransformer.tensorIndex(size, p, q, s, r)] = value;
    values[MolecularOrbitalIntegralTransformer.tensorIndex(size, q, p, s, r)] = value;
    values[MolecularOrbitalIntegralTransformer.tensorIndex(size, r, s, p, q)] = value;
    values[MolecularOrbitalIntegralTransformer.tensorIndex(size, s, r, p, q)] = value;
    values[MolecularOrbitalIntegralTransformer.tensorIndex(size, r, s, q, p)] = value;
    values[MolecularOrbitalIntegralTransformer.tensorIndex(size, s, r, q, p)] = value;
  }

  private static int pairIndex(final int first, final int second) {
    return first * (first + 1) / 2 + second;
  }

  private static int windowTensorIndex(
      final int secondDimension,
      final int thirdDimension,
      final int fourthDimension,
      final int first,
      final int second,
      final int third,
      final int fourth) {
    return ((first * secondDimension + second) * thirdDimension + third) * fourthDimension + fourth;
  }

  private static void requireCoefficientMatrix(
      final double[] coefficients, final int expectedLength) {
    if (coefficients == null || coefficients.length != expectedLength) {
      throw new IllegalArgumentException("MO coefficient matrix length must match orbital count.");
    }
    for (int i = 0; i < coefficients.length; ++i) {
      if (!Double.isFinite(coefficients[i])) {
        throw new IllegalArgumentException("MO coefficient matrix contains non-finite value.");
      }
    }
  }
}
