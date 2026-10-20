/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;

/**
 * Описывает ресурсные границы Java-native Gaussian integral engine.
 */
public final class GaussianIntegralCapability {

  private static final int MAX_ONE_ELECTRON_ANGULAR_MOMENTUM = 12;
  private static final int MAX_FULL_ERI_DERIVATIVE_ORDER = 48;
  private static final long MAX_PACKED_ERI_ELEMENTS = 2_000_000L;

  private GaussianIntegralCapability() {}

  public static GaussianIntegralPreflight preflightOneElectron(final GaussianBasisSet basisSet) {
    GaussianIntegralCapability.requireBasisSet(basisSet);
    return GaussianIntegralCapability.preflightOneElectron(
        basisSet.name(), basisSet.orbitalCount(), basisSet.maxAngularMomentum());
  }

  public static GaussianIntegralPreflight preflightOneElectron(
      final String basisName, final int orbitalCount, final int maxAngularMomentum) {
    GaussianIntegralCapability.requireBasisSummary(basisName, orbitalCount, maxAngularMomentum);
    final long denseTensorElements = GaussianIntegralCapability.denseEriElementCount(orbitalCount);
    final long packedTensorElements =
        GaussianIntegralCapability.packedEriElementCount(orbitalCount);
    if (maxAngularMomentum > MAX_ONE_ELECTRON_ANGULAR_MOMENTUM) {
      return GaussianIntegralPreflight.of(
          GaussianIntegralCapabilityStatus.UNSUPPORTED_ANGULAR_MOMENTUM,
          basisName,
          orbitalCount,
          maxAngularMomentum,
          GaussianIntegralCapability.maxFullEriDerivativeOrder(maxAngularMomentum),
          denseTensorElements,
          packedTensorElements,
          "One-electron analytic Gaussian matrices support Cartesian s/p/d/f/g/h/i/k/l/m/n/o/q shells; basis "
              + basisName
              + " contains max angular momentum "
              + maxAngularMomentum
              + ".");
    }
    return GaussianIntegralPreflight.of(
        GaussianIntegralCapabilityStatus.SUPPORTED,
        basisName,
        orbitalCount,
        maxAngularMomentum,
        GaussianIntegralCapability.maxFullEriDerivativeOrder(maxAngularMomentum),
        denseTensorElements,
        packedTensorElements,
        "");
  }

  public static GaussianIntegralPreflight preflightFullTensor(final GaussianBasisSet basisSet) {
    GaussianIntegralCapability.requireBasisSet(basisSet);
    return GaussianIntegralCapability.preflightFullTensor(
        basisSet.name(), basisSet.orbitalCount(), basisSet.maxAngularMomentum());
  }

  public static GaussianIntegralPreflight preflightFullTensor(
      final String basisName, final int orbitalCount, final int maxAngularMomentum) {
    GaussianIntegralCapability.requireBasisSummary(basisName, orbitalCount, maxAngularMomentum);
    final int maxDerivativeOrder =
        GaussianIntegralCapability.maxFullEriDerivativeOrder(maxAngularMomentum);
    final long denseTensorElements = GaussianIntegralCapability.denseEriElementCount(orbitalCount);
    final long packedTensorElements =
        GaussianIntegralCapability.packedEriElementCount(orbitalCount);
    if (maxDerivativeOrder > MAX_FULL_ERI_DERIVATIVE_ORDER) {
      final GaussianIntegralCapabilityStatus status =
          maxAngularMomentum <= MAX_ONE_ELECTRON_ANGULAR_MOMENTUM
              ? GaussianIntegralCapabilityStatus.PARTIAL_ONE_ELECTRON_ONLY
              : GaussianIntegralCapabilityStatus.UNSUPPORTED_ANGULAR_MOMENTUM;
      return GaussianIntegralPreflight.of(
          status,
          basisName,
          orbitalCount,
          maxAngularMomentum,
          maxDerivativeOrder,
          denseTensorElements,
          packedTensorElements,
          "Full analytic ERI supports Cartesian ERI quartets with total derivative order up to "
              + MAX_FULL_ERI_DERIVATIVE_ORDER
              + "; basis "
              + basisName
              + " can require derivative order "
              + maxDerivativeOrder
              + ".");
    }
    if (packedTensorElements > MAX_PACKED_ERI_ELEMENTS) {
      return GaussianIntegralPreflight.of(
          GaussianIntegralCapabilityStatus.UNSUPPORTED_RESOURCE_LIMIT,
          basisName,
          orbitalCount,
          maxAngularMomentum,
          maxDerivativeOrder,
          denseTensorElements,
          packedTensorElements,
          "Full packed ERI tensor for basis "
              + basisName
              + " requires "
              + packedTensorElements
              + " packed double elements; dense equivalent is "
              + denseTensorElements
              + " elements; configured packed in-memory limit is "
              + MAX_PACKED_ERI_ELEMENTS
              + ". Use active-space reduction or a streamed/sparse ERI path.");
    }
    return GaussianIntegralPreflight.of(
        GaussianIntegralCapabilityStatus.SUPPORTED,
        basisName,
        orbitalCount,
        maxAngularMomentum,
        maxDerivativeOrder,
        denseTensorElements,
        packedTensorElements,
        "");
  }

  public static String unsupportedOneElectronReason(final GaussianBasisSet basisSet) {
    final GaussianIntegralPreflight preflight =
        GaussianIntegralCapability.preflightOneElectron(basisSet);
    if (!preflight.supported()) {
      return preflight.reason();
    }
    return null;
  }

  public static String unsupportedFullTensorReason(final GaussianBasisSet basisSet) {
    final GaussianIntegralPreflight preflight =
        GaussianIntegralCapability.preflightFullTensor(basisSet);
    if (!preflight.supported()) {
      return preflight.reason();
    }
    return null;
  }

  public static long denseEriElementCount(final int orbitalCount) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Gaussian basis orbital count must be positive.");
    }
    final long count = orbitalCount;
    final long square = Math.multiplyExact(count, count);
    return Math.multiplyExact(square, square);
  }

  public static long packedEriElementCount(final int orbitalCount) {
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Gaussian basis orbital count must be positive.");
    }
    final long count = orbitalCount;
    final long pairCount = Math.multiplyExact(count, count + 1L) / 2L;
    return Math.multiplyExact(pairCount, pairCount + 1L) / 2L;
  }

  private static int maxFullEriDerivativeOrder(final int maxAngularMomentum) {
    return Math.multiplyExact(maxAngularMomentum, 4);
  }

  private static void requireBasisSet(final GaussianBasisSet basisSet) {
    if (basisSet == null) {
      throw new IllegalArgumentException("Gaussian basis set must not be null.");
    }
  }

  private static void requireBasisSummary(
      final String basisName, final int orbitalCount, final int maxAngularMomentum) {
    if (basisName == null || basisName.isBlank()) {
      throw new IllegalArgumentException("Gaussian basis name must not be blank.");
    }
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException("Gaussian basis orbital count must be positive.");
    }
    if (maxAngularMomentum < 0) {
      throw new IllegalArgumentException("Gaussian basis angular momentum must not be negative.");
    }
  }
}
