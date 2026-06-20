/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Консервативная оценка ресурсов без обещания точного времени конкретного backend.
 */
public final class ClassicalResourceEstimate {

  private final long atomCount;
  private final long bondCount;
  private final long electronCount;
  private final long activeElectronCount;
  private final long activeOrbitalCount;
  private final long estimatedBasisFunctionCount;
  private final long estimatedMemoryBytes;
  private final long estimatedDiskBytes;
  private final long estimatedCpuMilliseconds;
  private final ClassicalScalingClass scalingClass;

  private ClassicalResourceEstimate(
      final long atomCount,
      final long bondCount,
      final long electronCount,
      final long activeElectronCount,
      final long activeOrbitalCount,
      final long estimatedBasisFunctionCount,
      final long estimatedMemoryBytes,
      final long estimatedDiskBytes,
      final long estimatedCpuMilliseconds,
      final ClassicalScalingClass scalingClass
  ) {
    this.atomCount = atomCount;
    this.bondCount = bondCount;
    this.electronCount = electronCount;
    this.activeElectronCount = activeElectronCount;
    this.activeOrbitalCount = activeOrbitalCount;
    this.estimatedBasisFunctionCount = estimatedBasisFunctionCount;
    this.estimatedMemoryBytes = estimatedMemoryBytes;
    this.estimatedDiskBytes = estimatedDiskBytes;
    this.estimatedCpuMilliseconds = estimatedCpuMilliseconds;
    this.scalingClass = scalingClass;
  }

  public static ClassicalResourceEstimate of(
      final long atomCount,
      final long bondCount,
      final long electronCount,
      final long activeElectronCount,
      final long activeOrbitalCount,
      final long estimatedBasisFunctionCount,
      final long estimatedMemoryBytes,
      final long estimatedDiskBytes,
      final long estimatedCpuMilliseconds,
      final ClassicalScalingClass scalingClass
  ) {
    ClassicalResourceEstimate.requireNonNegative(atomCount, "atom count");
    ClassicalResourceEstimate.requireNonNegative(bondCount, "bond count");
    ClassicalResourceEstimate.requireNonNegative(electronCount, "electron count");
    ClassicalResourceEstimate.requireNonNegative(activeElectronCount, "active electron count");
    ClassicalResourceEstimate.requireNonNegative(activeOrbitalCount, "active orbital count");
    ClassicalResourceEstimate.requireNonNegative(estimatedBasisFunctionCount, "basis function count");
    ClassicalResourceEstimate.requireNonNegative(estimatedMemoryBytes, "memory bytes");
    ClassicalResourceEstimate.requireNonNegative(estimatedDiskBytes, "disk bytes");
    ClassicalResourceEstimate.requireNonNegative(estimatedCpuMilliseconds, "cpu milliseconds");
    return new ClassicalResourceEstimate(
        atomCount,
        bondCount,
        electronCount,
        activeElectronCount,
        activeOrbitalCount,
        estimatedBasisFunctionCount,
        estimatedMemoryBytes,
        estimatedDiskBytes,
        estimatedCpuMilliseconds,
        scalingClass == null ? ClassicalScalingClass.UNKNOWN : scalingClass);
  }

  public long atomCount() {
    return this.atomCount;
  }

  public long bondCount() {
    return this.bondCount;
  }

  public long electronCount() {
    return this.electronCount;
  }

  public long activeElectronCount() {
    return this.activeElectronCount;
  }

  public long activeOrbitalCount() {
    return this.activeOrbitalCount;
  }

  public long estimatedBasisFunctionCount() {
    return this.estimatedBasisFunctionCount;
  }

  public long estimatedMemoryBytes() {
    return this.estimatedMemoryBytes;
  }

  public long estimatedDiskBytes() {
    return this.estimatedDiskBytes;
  }

  public long estimatedCpuMilliseconds() {
    return this.estimatedCpuMilliseconds;
  }

  public ClassicalScalingClass scalingClass() {
    return this.scalingClass;
  }

  private static void requireNonNegative(
      final long value,
      final String name
  ) {
    if (value < 0L) {
      throw new IllegalArgumentException("Classical resource " + name + " must be non-negative.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalResourceEstimate)) {
      return false;
    }
    final ClassicalResourceEstimate estimate = (ClassicalResourceEstimate) other;
    return this.atomCount == estimate.atomCount
        && this.bondCount == estimate.bondCount
        && this.electronCount == estimate.electronCount
        && this.activeElectronCount == estimate.activeElectronCount
        && this.activeOrbitalCount == estimate.activeOrbitalCount
        && this.estimatedBasisFunctionCount == estimate.estimatedBasisFunctionCount
        && this.estimatedMemoryBytes == estimate.estimatedMemoryBytes
        && this.estimatedDiskBytes == estimate.estimatedDiskBytes
        && this.estimatedCpuMilliseconds == estimate.estimatedCpuMilliseconds
        && this.scalingClass == estimate.scalingClass;
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.atomCount);
    result = ChemistryHash.include(result, this.bondCount);
    result = ChemistryHash.include(result, this.electronCount);
    result = ChemistryHash.include(result, this.activeElectronCount);
    result = ChemistryHash.include(result, this.activeOrbitalCount);
    result = ChemistryHash.include(result, this.estimatedBasisFunctionCount);
    result = ChemistryHash.include(result, this.estimatedMemoryBytes);
    result = ChemistryHash.include(result, this.estimatedDiskBytes);
    result = ChemistryHash.include(result, this.estimatedCpuMilliseconds);
    result = ChemistryHash.include(result, this.scalingClass);
    return result;
  }
}