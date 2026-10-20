/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.PackedElectronRepulsionTensor;

final class PackedElectronRepulsionTensorTest {

  @Test
  void packedTensorPreservesEightFoldElectronRepulsionSymmetry() {
    final int orbitalCount = 3;
    final double[] denseValues =
        PackedElectronRepulsionTensorTest.symmetricDenseTensor(orbitalCount);
    final PackedElectronRepulsionTensor tensor =
        PackedElectronRepulsionTensor.fromDense(orbitalCount, denseValues);

    assertEquals(6, tensor.pairCount());
    assertEquals(21, tensor.packedElementCount());
    assertEquals(
        denseValues[PackedElectronRepulsionTensorTest.denseIndex(orbitalCount, 2, 1, 1, 0)],
        tensor.valueAt(1, 2, 0, 1),
        1.0E-12);
    assertArrayEquals(denseValues, tensor.toDenseArray(), 1.0E-12);
  }

  @Test
  void molecularIntegralTensorsStoreElectronRepulsionInPackedForm() {
    final int orbitalCount = 2;
    final double[] overlap = new double[] {1.0, 0.2, 0.2, 1.0};
    final double[] kinetic = new double[] {0.5, 0.1, 0.1, 0.6};
    final double[] nuclear = new double[] {-1.0, -0.2, -0.2, -1.1};
    final double[] electronRepulsion =
        PackedElectronRepulsionTensorTest.symmetricDenseTensor(orbitalCount);
    final MolecularIntegralTensors tensors =
        MolecularIntegralTensors.of(
            orbitalCount, 0.7, overlap, kinetic, nuclear, electronRepulsion);

    assertEquals(6, tensors.electronRepulsionPackedElementCount());
    assertArrayEquals(electronRepulsion, tensors.electronRepulsionCopy(), 1.0E-12);
    assertEquals(
        electronRepulsion[PackedElectronRepulsionTensorTest.denseIndex(orbitalCount, 1, 0, 1, 1)],
        tensors.electronRepulsionAt(0, 1, 1, 1),
        1.0E-12);
  }

  @Test
  void packedBuilderCreatesTensorWithoutDenseSource() {
    final PackedElectronRepulsionTensor.Builder builder = PackedElectronRepulsionTensor.builder(3);
    builder.set(2, 1, 1, 0, 0.42);
    builder.set(2, 2, 0, 0, 0.11);
    final PackedElectronRepulsionTensor tensor = builder.build();

    assertEquals(21, tensor.packedElementCount());
    assertEquals(0.42, tensor.valueAt(1, 2, 0, 1), 1.0E-12);
    assertEquals(0.42, tensor.valueAt(0, 1, 2, 1), 1.0E-12);
    assertEquals(0.11, tensor.valueAt(0, 0, 2, 2), 1.0E-12);
  }

  @Test
  void gaussianIntegralEngineParallelCanonicalLoopWritesEachPackedSlotOnce() {
    final int orbitalCount = 8;
    final int pairCount = orbitalCount * (orbitalCount + 1) / 2;
    final int packedElementCount = pairCount * (pairCount + 1) / 2;
    final int[] writeCounts = new int[packedElementCount];

    for (int first = 0; first < orbitalCount; ++first) {
      for (int second = 0; second <= first; ++second) {
        final int firstPair = PackedElectronRepulsionTensorTest.pairIndex(first, second);
        for (int third = 0; third < orbitalCount; ++third) {
          for (int fourth = 0; fourth <= third; ++fourth) {
            final int secondPair = PackedElectronRepulsionTensorTest.pairIndex(third, fourth);
            if (secondPair > firstPair) {
              continue;
            }
            ++writeCounts[PackedElectronRepulsionTensorTest.packedIndex(firstPair, secondPair)];
          }
        }
      }
    }

    for (int i = 0; i < writeCounts.length; ++i) {
      assertEquals(
          1, writeCounts[i], "Canonical ERI loop must write each packed slot exactly once: " + i);
    }
  }

  @Test
  void packedTensorRejectsDenseSourceWithoutPermutationalSymmetry() {
    final int orbitalCount = 2;
    final double[] denseValues =
        PackedElectronRepulsionTensorTest.symmetricDenseTensor(orbitalCount);
    denseValues[PackedElectronRepulsionTensorTest.denseIndex(orbitalCount, 0, 1, 0, 0)] += 0.5;

    assertThrows(
        IllegalArgumentException.class,
        () -> PackedElectronRepulsionTensor.fromDense(orbitalCount, denseValues));
  }

  private static double[] symmetricDenseTensor(final int orbitalCount) {
    final int matrixSize = Math.multiplyExact(orbitalCount, orbitalCount);
    final double[] denseValues = new double[Math.multiplyExact(matrixSize, matrixSize)];
    for (int first = 0; first < orbitalCount; ++first) {
      for (int second = 0; second <= first; ++second) {
        final int firstPair = PackedElectronRepulsionTensorTest.pairIndex(first, second);
        for (int third = 0; third < orbitalCount; ++third) {
          for (int fourth = 0; fourth <= third; ++fourth) {
            final int secondPair = PackedElectronRepulsionTensorTest.pairIndex(third, fourth);
            if (secondPair > firstPair) {
              continue;
            }
            final double value = 0.01 * (1 + firstPair * 7 + secondPair);
            PackedElectronRepulsionTensorTest.setSymmetric(
                denseValues, orbitalCount, first, second, third, fourth, value);
          }
        }
      }
    }
    return denseValues;
  }

  private static void setSymmetric(
      final double[] denseValues,
      final int orbitalCount,
      final int first,
      final int second,
      final int third,
      final int fourth,
      final double value) {
    PackedElectronRepulsionTensorTest.set(
        denseValues, orbitalCount, first, second, third, fourth, value);
    PackedElectronRepulsionTensorTest.set(
        denseValues, orbitalCount, second, first, third, fourth, value);
    PackedElectronRepulsionTensorTest.set(
        denseValues, orbitalCount, first, second, fourth, third, value);
    PackedElectronRepulsionTensorTest.set(
        denseValues, orbitalCount, second, first, fourth, third, value);
    PackedElectronRepulsionTensorTest.set(
        denseValues, orbitalCount, third, fourth, first, second, value);
    PackedElectronRepulsionTensorTest.set(
        denseValues, orbitalCount, fourth, third, first, second, value);
    PackedElectronRepulsionTensorTest.set(
        denseValues, orbitalCount, third, fourth, second, first, value);
    PackedElectronRepulsionTensorTest.set(
        denseValues, orbitalCount, fourth, third, second, first, value);
  }

  private static void set(
      final double[] denseValues,
      final int orbitalCount,
      final int first,
      final int second,
      final int third,
      final int fourth,
      final double value) {
    denseValues[
            PackedElectronRepulsionTensorTest.denseIndex(
                orbitalCount, first, second, third, fourth)] =
        value;
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
}
