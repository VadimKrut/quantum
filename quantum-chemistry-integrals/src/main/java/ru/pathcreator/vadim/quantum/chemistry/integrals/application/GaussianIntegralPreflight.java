/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import java.util.Objects;

/** Структурированный preflight для интегрального движка без парсинга текстов ошибок. */
public final class GaussianIntegralPreflight {

  private final GaussianIntegralCapabilityStatus status;
  private final String basisName;
  private final int orbitalCount;
  private final int maxAngularMomentum;
  private final int maxFullEriDerivativeOrder;
  private final long denseEriElementCount;
  private final long packedEriElementCount;
  private final String reason;

  private GaussianIntegralPreflight(
      final GaussianIntegralCapabilityStatus status,
      final String basisName,
      final int orbitalCount,
      final int maxAngularMomentum,
      final int maxFullEriDerivativeOrder,
      final long denseEriElementCount,
      final long packedEriElementCount,
      final String reason) {
    this.status = status;
    this.basisName = basisName;
    this.orbitalCount = orbitalCount;
    this.maxAngularMomentum = maxAngularMomentum;
    this.maxFullEriDerivativeOrder = maxFullEriDerivativeOrder;
    this.denseEriElementCount = denseEriElementCount;
    this.packedEriElementCount = packedEriElementCount;
    this.reason = reason;
  }

  public static GaussianIntegralPreflight of(
      final GaussianIntegralCapabilityStatus status,
      final String basisName,
      final int orbitalCount,
      final int maxAngularMomentum,
      final int maxFullEriDerivativeOrder,
      final long denseEriElementCount,
      final long packedEriElementCount,
      final String reason) {
    if (status == null) {
      throw new IllegalArgumentException("Gaussian integral capability status must not be null.");
    }
    if (basisName == null || basisName.isBlank()) {
      throw new IllegalArgumentException(
          "Gaussian integral preflight basis name must not be blank.");
    }
    if (orbitalCount <= 0) {
      throw new IllegalArgumentException(
          "Gaussian integral preflight orbital count must be positive.");
    }
    if (maxAngularMomentum < 0) {
      throw new IllegalArgumentException(
          "Gaussian integral preflight angular momentum must not be negative.");
    }
    if (maxFullEriDerivativeOrder < 0) {
      throw new IllegalArgumentException(
          "Gaussian integral preflight derivative order must not be negative.");
    }
    if (denseEriElementCount <= 0L) {
      throw new IllegalArgumentException(
          "Gaussian integral preflight dense ERI element count must be positive.");
    }
    if (packedEriElementCount <= 0L) {
      throw new IllegalArgumentException(
          "Gaussian integral preflight packed ERI element count must be positive.");
    }
    final String checkedReason = reason == null ? "" : reason;
    if (status != GaussianIntegralCapabilityStatus.SUPPORTED && checkedReason.isBlank()) {
      throw new IllegalArgumentException(
          "Unsupported Gaussian integral preflight must contain reason.");
    }
    return new GaussianIntegralPreflight(
        status,
        basisName,
        orbitalCount,
        maxAngularMomentum,
        maxFullEriDerivativeOrder,
        denseEriElementCount,
        packedEriElementCount,
        checkedReason);
  }

  public boolean supported() {
    return this.status == GaussianIntegralCapabilityStatus.SUPPORTED;
  }

  public boolean partial() {
    return this.status == GaussianIntegralCapabilityStatus.PARTIAL_ONE_ELECTRON_ONLY;
  }

  public boolean oneElectronFallbackAvailable() {
    return this.status == GaussianIntegralCapabilityStatus.SUPPORTED
        || this.status == GaussianIntegralCapabilityStatus.PARTIAL_ONE_ELECTRON_ONLY;
  }

  public boolean resourceLimited() {
    return this.status == GaussianIntegralCapabilityStatus.UNSUPPORTED_RESOURCE_LIMIT;
  }

  public boolean angularMomentumLimited() {
    return this.status == GaussianIntegralCapabilityStatus.UNSUPPORTED_ANGULAR_MOMENTUM
        || this.status == GaussianIntegralCapabilityStatus.PARTIAL_ONE_ELECTRON_ONLY;
  }

  public GaussianIntegralCapabilityStatus status() {
    return this.status;
  }

  public String basisName() {
    return this.basisName;
  }

  public int orbitalCount() {
    return this.orbitalCount;
  }

  public int maxAngularMomentum() {
    return this.maxAngularMomentum;
  }

  public int maxFullEriDerivativeOrder() {
    return this.maxFullEriDerivativeOrder;
  }

  public long denseEriElementCount() {
    return this.denseEriElementCount;
  }

  public long packedEriElementCount() {
    return this.packedEriElementCount;
  }

  public String reason() {
    return this.reason;
  }

  public String requireSupportedReason() {
    if (this.supported()) {
      throw new IllegalStateException(
          "Supported Gaussian integral preflight does not contain failure reason.");
    }
    return this.reason;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof GaussianIntegralPreflight)) {
      return false;
    }
    final GaussianIntegralPreflight preflight = (GaussianIntegralPreflight) other;
    return this.orbitalCount == preflight.orbitalCount
        && this.maxAngularMomentum == preflight.maxAngularMomentum
        && this.maxFullEriDerivativeOrder == preflight.maxFullEriDerivativeOrder
        && this.denseEriElementCount == preflight.denseEriElementCount
        && this.packedEriElementCount == preflight.packedEriElementCount
        && this.status == preflight.status
        && this.basisName.equals(preflight.basisName)
        && this.reason.equals(preflight.reason);
  }

  public int hashCode() {
    return Objects.hash(
        this.status,
        this.basisName,
        Integer.valueOf(this.orbitalCount),
        Integer.valueOf(this.maxAngularMomentum),
        Integer.valueOf(this.maxFullEriDerivativeOrder),
        Long.valueOf(this.denseEriElementCount),
        Long.valueOf(this.packedEriElementCount),
        this.reason);
  }
}
