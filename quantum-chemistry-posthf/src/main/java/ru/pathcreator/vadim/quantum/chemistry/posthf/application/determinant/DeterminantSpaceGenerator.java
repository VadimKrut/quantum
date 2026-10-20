/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.application.determinant;

import java.util.Arrays;

/**
 * Генерирует determinant basis для exact diagonalization и selected CI.
 */
public final class DeterminantSpaceGenerator {

  private DeterminantSpaceGenerator() {}

  public static long[] full(
      final int spinOrbitalCount, final int electronCount, final long maxDeterminants) {
    DeterminantSpaceGenerator.validateSpinOrbitalProblem(spinOrbitalCount, electronCount);
    final long capacity = DeterminantSpaceGenerator.combination(spinOrbitalCount, electronCount);
    if (capacity > maxDeterminants) {
      throw new IllegalArgumentException("Determinant space exceeds configured limit.");
    }
    final long[] determinants = new long[(int) capacity];
    final int[] cursor = new int[] {0};
    DeterminantSpaceGenerator.generateDeterminantsRecursive(
        spinOrbitalCount, electronCount, 0, 0L, determinants, cursor);
    return determinants;
  }

  public static long[] spinSector(
      final int spinOrbitalCount,
      final int electronCount,
      final int alphaElectronCount,
      final int betaElectronCount,
      final long maxDeterminants) {
    DeterminantSpaceGenerator.validateSpinOrbitalProblem(spinOrbitalCount, electronCount);
    if ((spinOrbitalCount & 1) != 0) {
      throw new IllegalArgumentException(
          "Spin-resolved determinant sector requires an even spin orbital count.");
    }
    if (alphaElectronCount + betaElectronCount != electronCount) {
      throw new IllegalArgumentException(
          "Alpha/beta electron counts must match total electron count.");
    }
    final int spatialOrbitalCount = spinOrbitalCount / 2;
    if (alphaElectronCount < 0
        || betaElectronCount < 0
        || alphaElectronCount > spatialOrbitalCount
        || betaElectronCount > spatialOrbitalCount) {
      throw new IllegalArgumentException(
          "Spin-sector electron count exceeds spatial orbital count.");
    }
    final long capacity =
        Math.multiplyExact(
            DeterminantSpaceGenerator.combination(spatialOrbitalCount, alphaElectronCount),
            DeterminantSpaceGenerator.combination(spatialOrbitalCount, betaElectronCount));
    if (capacity > maxDeterminants) {
      throw new IllegalArgumentException("Spin-sector determinant space exceeds configured limit.");
    }
    final long[] determinants = new long[(int) capacity];
    final int[] cursor = new int[] {0};
    DeterminantSpaceGenerator.generateSpinSectorRecursive(
        spatialOrbitalCount, alphaElectronCount, betaElectronCount, 0, 0L, determinants, cursor);
    Arrays.sort(determinants);
    return determinants;
  }

  public static long[] cisd(final int spinOrbitalCount, final int electronCount) {
    DeterminantSpaceGenerator.validateSpinOrbitalProblem(spinOrbitalCount, electronCount);
    final long reference = DeterminantSpaceGenerator.referenceDeterminant(electronCount);
    final LongArrayBuilder determinants = new LongArrayBuilder();
    determinants.add(reference);
    for (int occupied = 0; occupied < electronCount; ++occupied) {
      for (int virtual = electronCount; virtual < spinOrbitalCount; ++virtual) {
        determinants.add((reference & ~(1L << occupied)) | (1L << virtual));
      }
    }
    for (int firstOccupied = 0; firstOccupied < electronCount; ++firstOccupied) {
      for (int secondOccupied = firstOccupied + 1;
          secondOccupied < electronCount;
          ++secondOccupied) {
        for (int firstVirtual = electronCount; firstVirtual < spinOrbitalCount; ++firstVirtual) {
          for (int secondVirtual = firstVirtual + 1;
              secondVirtual < spinOrbitalCount;
              ++secondVirtual) {
            determinants.add(
                (reference & ~(1L << firstOccupied) & ~(1L << secondOccupied))
                    | (1L << firstVirtual)
                    | (1L << secondVirtual));
          }
        }
      }
    }
    final long[] unique = determinants.sortedUniqueArray();
    if (unique.length == 0) {
      throw new IllegalArgumentException("CISD determinant space is empty.");
    }
    return unique;
  }

  public static long referenceDeterminant(final int electronCount) {
    if (electronCount < 0 || electronCount > Long.SIZE - 1) {
      throw new IllegalArgumentException(
          "Reference determinant electron count must be in [0, 63].");
    }
    long determinant = 0L;
    for (int orbital = 0; orbital < electronCount; ++orbital) {
      determinant |= 1L << orbital;
    }
    return determinant;
  }

  public static long combination(final int n, final int k) {
    if (k < 0 || k > n) {
      return 0L;
    }
    final int selected = Math.min(k, n - k);
    long result = 1L;
    for (int i = 1; i <= selected; ++i) {
      result = Math.multiplyExact(result, (long) n - selected + i) / i;
    }
    return result;
  }

  private static void validateSpinOrbitalProblem(
      final int spinOrbitalCount, final int electronCount) {
    if (spinOrbitalCount <= 0 || spinOrbitalCount > Long.SIZE - 1) {
      throw new IllegalArgumentException("Spin orbital count must be in [1, 63].");
    }
    if (electronCount <= 0 || electronCount > spinOrbitalCount) {
      throw new IllegalArgumentException("Electron count is outside spin orbital bounds.");
    }
  }

  private static void generateDeterminantsRecursive(
      final int spinOrbitalCount,
      final int remainingElectrons,
      final int startOrbital,
      final long current,
      final long[] determinants,
      final int[] cursor) {
    if (remainingElectrons == 0) {
      determinants[cursor[0]] = current;
      ++cursor[0];
      return;
    }
    for (int orbital = startOrbital; orbital <= spinOrbitalCount - remainingElectrons; ++orbital) {
      DeterminantSpaceGenerator.generateDeterminantsRecursive(
          spinOrbitalCount,
          remainingElectrons - 1,
          orbital + 1,
          current | (1L << orbital),
          determinants,
          cursor);
    }
  }

  private static void generateSpinSectorRecursive(
      final int spatialOrbitalCount,
      final int remainingAlphaElectrons,
      final int remainingBetaElectrons,
      final int spatialOrbital,
      final long current,
      final long[] determinants,
      final int[] cursor) {
    if (spatialOrbital == spatialOrbitalCount) {
      if (remainingAlphaElectrons == 0 && remainingBetaElectrons == 0) {
        determinants[cursor[0]] = current;
        ++cursor[0];
      }
      return;
    }
    final int remainingSpatialOrbitals = spatialOrbitalCount - spatialOrbital - 1;
    DeterminantSpaceGenerator.generateSpinSectorRecursive(
        spatialOrbitalCount,
        remainingAlphaElectrons,
        remainingBetaElectrons,
        spatialOrbital + 1,
        current,
        determinants,
        cursor);
    if (remainingAlphaElectrons > 0 && remainingAlphaElectrons - 1 <= remainingSpatialOrbitals) {
      DeterminantSpaceGenerator.generateSpinSectorRecursive(
          spatialOrbitalCount,
          remainingAlphaElectrons - 1,
          remainingBetaElectrons,
          spatialOrbital + 1,
          current | (1L << (spatialOrbital * 2)),
          determinants,
          cursor);
    }
    if (remainingBetaElectrons > 0 && remainingBetaElectrons - 1 <= remainingSpatialOrbitals) {
      DeterminantSpaceGenerator.generateSpinSectorRecursive(
          spatialOrbitalCount,
          remainingAlphaElectrons,
          remainingBetaElectrons - 1,
          spatialOrbital + 1,
          current | (1L << (spatialOrbital * 2 + 1)),
          determinants,
          cursor);
    }
    if (remainingAlphaElectrons > 0
        && remainingBetaElectrons > 0
        && remainingAlphaElectrons - 1 <= remainingSpatialOrbitals
        && remainingBetaElectrons - 1 <= remainingSpatialOrbitals) {
      DeterminantSpaceGenerator.generateSpinSectorRecursive(
          spatialOrbitalCount,
          remainingAlphaElectrons - 1,
          remainingBetaElectrons - 1,
          spatialOrbital + 1,
          current | (1L << (spatialOrbital * 2)) | (1L << (spatialOrbital * 2 + 1)),
          determinants,
          cursor);
    }
  }

  private static final class LongArrayBuilder {

    private long[] values = new long[32];
    private int size;

    private void add(final long value) {
      if (this.size == this.values.length) {
        this.values = Arrays.copyOf(this.values, this.values.length << 1);
      }
      this.values[this.size] = value;
      ++this.size;
    }

    private long[] sortedUniqueArray() {
      final long[] copy = Arrays.copyOf(this.values, this.size);
      Arrays.sort(copy);
      int uniqueCount = 0;
      for (int i = 0; i < copy.length; ++i) {
        if (i > 0 && copy[i] == copy[i - 1]) {
          continue;
        }
        copy[uniqueCount] = copy[i];
        ++uniqueCount;
      }
      return Arrays.copyOf(copy, uniqueCount);
    }
  }
}
