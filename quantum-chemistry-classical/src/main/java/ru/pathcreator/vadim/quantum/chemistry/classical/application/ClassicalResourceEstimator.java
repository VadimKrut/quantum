/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application;

import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalResourceEstimate;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.resource.ClassicalScalingClass;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;

/**
 * Консервативный estimator ресурсов, независимый от конкретной реализации backend.
 */
public final class ClassicalResourceEstimator {

  private static final long BYTES_PER_COMPLEX_DOUBLE = 16L;
  private static final long MINIMUM_MEMORY_BYTES = 64L * 1024L * 1024L;

  public ClassicalResourceEstimate estimate(
      final ClassicalCalculationRequest request,
      final ClassicalSubjectSize size
  ) {
    if (request == null) {
      throw new IllegalArgumentException("Classical resource estimator request must not be null.");
    }
    if (size == null) {
      throw new IllegalArgumentException("Classical resource estimator subject size must not be null.");
    }
    final long activeElectronCount = request.hasActiveSpace()
        ? request.activeSpace().electronCount()
        : 0L;
    final long activeOrbitalCount = request.hasActiveSpace()
        ? request.activeSpace().orbitalCount()
        : 0L;
    final long basisFunctionCount = this.estimateBasisFunctionCount(request, size);
    final ClassicalScalingClass scalingClass = this.scalingClass(request);
    final long memoryBytes = this.estimateMemoryBytes(basisFunctionCount, scalingClass);
    final long diskBytes = this.multiplySaturated(memoryBytes, 4L);
    final long cpuMilliseconds = this.estimateCpuMilliseconds(basisFunctionCount, scalingClass);
    return ClassicalResourceEstimate.of(
        size.atomCount(),
        size.bondCount(),
        size.electronCount(),
        activeElectronCount,
        activeOrbitalCount,
        basisFunctionCount,
        memoryBytes,
        diskBytes,
        cpuMilliseconds,
        scalingClass);
  }

  private long estimateBasisFunctionCount(
      final ClassicalCalculationRequest request,
      final ClassicalSubjectSize size
  ) {
    final String basisName = request.basisSet().name().value().toLowerCase(java.util.Locale.ROOT);
    final long multiplier = this.basisFunctionMultiplier(basisName);
    final long atomWeighted = this.multiplySaturated(size.atomCount(), multiplier);
    return Math.max(atomWeighted, Math.max(size.electronCount(), 1L));
  }

  private long basisFunctionMultiplier(final String basisName) {
    if (basisName.contains("sto")) {
      return 2L;
    }
    if (
        basisName.contains("6-31")
        || basisName.contains("def2-svp")
    ) {
      return 6L;
    }
    if (
        basisName.contains("tz")
        || basisName.contains("triple")
    ) {
      return 10L;
    }
    if (
        basisName.contains("qz")
        || basisName.contains("quadruple")
    ) {
      return 16L;
    }
    return 4L;
  }

  private ClassicalScalingClass scalingClass(final ClassicalCalculationRequest request) {
    final ElectronicStructureMethodType type = request.method().type();
    if (type == ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION) {
      return ClassicalScalingClass.EXPONENTIAL;
    }
    if (type == ElectronicStructureMethodType.COUPLED_CLUSTER) {
      return ClassicalScalingClass.SEXTIC;
    }
    if (type == ElectronicStructureMethodType.CONFIGURATION_INTERACTION
        || type == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return ClassicalScalingClass.QUINTIC;
    }
    if (request.calculationKinds().contains(ClassicalCalculationKind.FREQUENCY_ANALYSIS)) {
      return ClassicalScalingClass.QUARTIC;
    }
    if (type == ElectronicStructureMethodType.HARTREE_FOCK
        || type == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
      return ClassicalScalingClass.CUBIC;
    }
    return ClassicalScalingClass.UNKNOWN;
  }

  private long estimateMemoryBytes(
      final long basisFunctionCount,
      final ClassicalScalingClass scalingClass
  ) {
    final long squared = this.multiplySaturated(basisFunctionCount, basisFunctionCount);
    final long base = this.multiplySaturated(squared, BYTES_PER_COMPLEX_DOUBLE);
    if (scalingClass == ClassicalScalingClass.EXPONENTIAL) {
      return this.multiplySaturated(base, 128L);
    }
    if (scalingClass == ClassicalScalingClass.SEXTIC || scalingClass == ClassicalScalingClass.QUINTIC) {
      return this.multiplySaturated(base, 16L);
    }
    return Math.max(base, MINIMUM_MEMORY_BYTES);
  }

  private long estimateCpuMilliseconds(
      final long basisFunctionCount,
      final ClassicalScalingClass scalingClass
  ) {
    final long power = this.cpuScalingPower(
        basisFunctionCount,
        scalingClass);
    long result = 1L;
    for (long i = 0L; i < power; ++i) {
      result = this.multiplySaturated(result, Math.max(basisFunctionCount, 1L));
    }
    return Math.max(result / 100L, 1L);
  }

  private long cpuScalingPower(
      final long basisFunctionCount,
      final ClassicalScalingClass scalingClass
  ) {
    if (scalingClass == ClassicalScalingClass.QUARTIC) {
      return 4L;
    }
    if (scalingClass == ClassicalScalingClass.QUINTIC) {
      return 5L;
    }
    if (scalingClass == ClassicalScalingClass.SEXTIC) {
      return 6L;
    }
    if (scalingClass == ClassicalScalingClass.EXPONENTIAL) {
      return Math.min(basisFunctionCount, 20L);
    }
    return 3L;
  }

  private long multiplySaturated(
      final long first,
      final long second
  ) {
    try {
      return Math.multiplyExact(first, second);
    } catch (final ArithmeticException exception) {
      return Long.MAX_VALUE;
    }
  }
}