/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor;

/** Хранит двухэлектронные AO-интегралы в pair-pair packed форме с учетом перестановочной симметрии. */
public final class PackedElectronRepulsionTensor {

  private static final double SYMMETRY_TOLERANCE = 1.0E-8;

  private final int orbitalCount;
  private final int pairCount;
  private final double[] values;

  private PackedElectronRepulsionTensor(
      final int orbitalCount, final int pairCount, final double[] values) {
    this.orbitalCount = orbitalCount;
    this.pairCount = pairCount;
    this.values = values;
  }

  public static PackedElectronRepulsionTensor fromDense(
      final int orbitalCount, final double[] denseValues) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Packed ERI orbital count must be positive.");
    }
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    final int denseSize = Math.multiplyExact(matrixSize, matrixSize);
    if (denseValues == null || denseValues.length != denseSize) {
      throw new IllegalArgumentException(
          "Packed ERI dense source length does not match orbital count.");
    }
    final int pairCount = orbitalCount * (orbitalCount + 1) / 2;
    final int packedSize = pairCount * (pairCount + 1) / 2;
    final double[] packedValues = new double[packedSize];
    for (int first = 0; first < orbitalCount; ++first) {
      for (int second = 0; second <= first; ++second) {
        final int firstPair = PackedElectronRepulsionTensor.pairIndex(first, second);
        for (int third = 0; third < orbitalCount; ++third) {
          for (int fourth = 0; fourth <= third; ++fourth) {
            final int secondPair = PackedElectronRepulsionTensor.pairIndex(third, fourth);
            if (secondPair > firstPair) {
              continue;
            }
            final double value =
                denseValues[
                    PackedElectronRepulsionTensor.denseIndex(
                        orbitalCount, first, second, third, fourth)];
            PackedElectronRepulsionTensor.requireFinite(value);
            PackedElectronRepulsionTensor.requireDenseSymmetry(
                orbitalCount, denseValues, first, second, third, fourth, value);
            packedValues[PackedElectronRepulsionTensor.packedIndex(firstPair, secondPair)] = value;
          }
        }
      }
    }
    return new PackedElectronRepulsionTensor(orbitalCount, pairCount, packedValues);
  }

  public static Builder builder(final int orbitalCount) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Packed ERI orbital count must be positive.");
    }
    final int pairCount = orbitalCount * (orbitalCount + 1) / 2;
    final int packedSize = pairCount * (pairCount + 1) / 2;
    return new Builder(orbitalCount, pairCount, new double[packedSize]);
  }

  public static PackedElectronRepulsionTensor fromPackedValues(
      final int orbitalCount, final double[] packedValues) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Packed ERI orbital count must be positive.");
    }
    final int pairCount = orbitalCount * (orbitalCount + 1) / 2;
    final int packedSize = pairCount * (pairCount + 1) / 2;
    if (packedValues == null || packedValues.length != packedSize) {
      throw new IllegalArgumentException("Packed ERI source length does not match orbital count.");
    }
    for (int i = 0; i < packedValues.length; ++i) {
      PackedElectronRepulsionTensor.requireFinite(packedValues[i]);
    }
    return new PackedElectronRepulsionTensor(orbitalCount, pairCount, packedValues.clone());
  }

  public int orbitalCount() {
    return this.orbitalCount;
  }

  public int pairCount() {
    return this.pairCount;
  }

  public int packedElementCount() {
    return this.values.length;
  }

  public double valueAt(final int first, final int second, final int third, final int fourth) {
    this.requireOrbital(first);
    this.requireOrbital(second);
    this.requireOrbital(third);
    this.requireOrbital(fourth);
    final int firstPair = PackedElectronRepulsionTensor.normalizedPairIndex(first, second);
    final int secondPair = PackedElectronRepulsionTensor.normalizedPairIndex(third, fourth);
    final int left = Math.max(firstPair, secondPair);
    final int right = Math.min(firstPair, secondPair);
    return this.values[PackedElectronRepulsionTensor.packedIndex(left, right)];
  }

  public double[] toDenseArray() {
    final int matrixSize = Math.multiplyExact(this.orbitalCount, this.orbitalCount);
    final double[] denseValues = new double[Math.multiplyExact(matrixSize, matrixSize)];
    for (int first = 0; first < this.orbitalCount; ++first) {
      for (int second = 0; second < this.orbitalCount; ++second) {
        for (int third = 0; third < this.orbitalCount; ++third) {
          for (int fourth = 0; fourth < this.orbitalCount; ++fourth) {
            denseValues[
                    PackedElectronRepulsionTensor.denseIndex(
                        this.orbitalCount, first, second, third, fourth)] =
                this.valueAt(first, second, third, fourth);
          }
        }
      }
    }
    return denseValues;
  }

  private void requireOrbital(final int orbital) {
    if (orbital < 0 || orbital >= this.orbitalCount) {
      throw new IllegalArgumentException("Packed ERI orbital index is out of bounds.");
    }
  }

  private static int normalizedPairIndex(final int first, final int second) {
    return first >= second
        ? PackedElectronRepulsionTensor.pairIndex(first, second)
        : PackedElectronRepulsionTensor.pairIndex(second, first);
  }

  private static int pairIndex(final int first, final int second) {
    return first * (first + 1) / 2 + second;
  }

  private static int packedIndex(final int firstPair, final int secondPair) {
    return firstPair * (firstPair + 1) / 2 + secondPair;
  }

  private static int denseIndex(
      final int orbitalCount,
      final int first,
      final int second,
      final int third,
      final int fourth) {
    return ((first * orbitalCount + second) * orbitalCount + third) * orbitalCount + fourth;
  }

  private static void requireFinite(final double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Packed ERI dense source contains non-finite value.");
    }
  }

  private static void requireDenseSymmetry(
      final int orbitalCount,
      final double[] denseValues,
      final int first,
      final int second,
      final int third,
      final int fourth,
      final double expectedValue) {
    PackedElectronRepulsionTensor.requireSymmetricValue(
        orbitalCount, denseValues, second, first, third, fourth, expectedValue);
    PackedElectronRepulsionTensor.requireSymmetricValue(
        orbitalCount, denseValues, first, second, fourth, third, expectedValue);
    PackedElectronRepulsionTensor.requireSymmetricValue(
        orbitalCount, denseValues, third, fourth, first, second, expectedValue);
  }

  private static void requireSymmetricValue(
      final int orbitalCount,
      final double[] denseValues,
      final int first,
      final int second,
      final int third,
      final int fourth,
      final double expectedValue) {
    final double actualValue =
        denseValues[
            PackedElectronRepulsionTensor.denseIndex(orbitalCount, first, second, third, fourth)];
    PackedElectronRepulsionTensor.requireFinite(actualValue);
    if (Math.abs(actualValue - expectedValue) > SYMMETRY_TOLERANCE) {
      throw new IllegalArgumentException(
          "Packed ERI dense source violates permutational symmetry.");
    }
  }

  /** Заполняет canonical ERI quartets без временного dense n^4 массива. */
  public static final class Builder {

    private final int orbitalCount;
    private final int pairCount;
    private final double[] values;

    private Builder(final int orbitalCount, final int pairCount, final double[] values) {
      this.orbitalCount = orbitalCount;
      this.pairCount = pairCount;
      this.values = values;
    }

    public void set(
        final int first, final int second, final int third, final int fourth, final double value) {
      this.requireOrbital(first);
      this.requireOrbital(second);
      this.requireOrbital(third);
      this.requireOrbital(fourth);
      PackedElectronRepulsionTensor.requireFinite(value);
      final int firstPair = PackedElectronRepulsionTensor.normalizedPairIndex(first, second);
      final int secondPair = PackedElectronRepulsionTensor.normalizedPairIndex(third, fourth);
      final int left = Math.max(firstPair, secondPair);
      final int right = Math.min(firstPair, secondPair);
      this.values[PackedElectronRepulsionTensor.packedIndex(left, right)] = value;
    }

    public PackedElectronRepulsionTensor build() {
      return new PackedElectronRepulsionTensor(
          this.orbitalCount, this.pairCount, this.values.clone());
    }

    private void requireOrbital(final int orbital) {
      if (orbital < 0 || orbital >= this.orbitalCount) {
        throw new IllegalArgumentException("Packed ERI orbital index is out of bounds.");
      }
    }
  }
}
