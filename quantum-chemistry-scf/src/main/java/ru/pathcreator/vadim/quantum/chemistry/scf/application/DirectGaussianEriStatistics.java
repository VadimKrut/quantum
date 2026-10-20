/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application;

/**
 * Снимок счетчиков direct AO ERI provider без доступа к внутреннему cache.
 */
public final class DirectGaussianEriStatistics {

  private final long valueQueryCount;
  private final long screenedOutQueryCount;
  private final long cacheHitCount;
  private final long computedIntegralCount;
  private final long pairBoundIntegralCount;
  private final long pairBoundBuildCount;
  private final int cacheSize;
  private final int cacheCapacity;
  private final double screeningTolerance;

  private DirectGaussianEriStatistics(
      final long valueQueryCount,
      final long screenedOutQueryCount,
      final long cacheHitCount,
      final long computedIntegralCount,
      final long pairBoundIntegralCount,
      final long pairBoundBuildCount,
      final int cacheSize,
      final int cacheCapacity,
      final double screeningTolerance) {
    this.valueQueryCount = valueQueryCount;
    this.screenedOutQueryCount = screenedOutQueryCount;
    this.cacheHitCount = cacheHitCount;
    this.computedIntegralCount = computedIntegralCount;
    this.pairBoundIntegralCount = pairBoundIntegralCount;
    this.pairBoundBuildCount = pairBoundBuildCount;
    this.cacheSize = cacheSize;
    this.cacheCapacity = cacheCapacity;
    this.screeningTolerance = screeningTolerance;
  }

  public static DirectGaussianEriStatistics of(
      final long valueQueryCount,
      final long screenedOutQueryCount,
      final long cacheHitCount,
      final long computedIntegralCount,
      final long pairBoundIntegralCount,
      final long pairBoundBuildCount,
      final int cacheSize,
      final int cacheCapacity,
      final double screeningTolerance) {
    DirectGaussianEriStatistics.requireNonNegative(valueQueryCount, "value query count");
    DirectGaussianEriStatistics.requireNonNegative(
        screenedOutQueryCount, "screened out query count");
    DirectGaussianEriStatistics.requireNonNegative(cacheHitCount, "cache hit count");
    DirectGaussianEriStatistics.requireNonNegative(
        computedIntegralCount, "computed integral count");
    DirectGaussianEriStatistics.requireNonNegative(
        pairBoundIntegralCount, "pair bound integral count");
    DirectGaussianEriStatistics.requireNonNegative(pairBoundBuildCount, "pair bound build count");
    if (cacheSize < 0) {
      throw new IllegalArgumentException("Direct Gaussian ERI cache size must be non-negative.");
    }
    if (cacheCapacity <= 0) {
      throw new IllegalArgumentException("Direct Gaussian ERI cache capacity must be positive.");
    }
    if (cacheSize > cacheCapacity) {
      throw new IllegalArgumentException(
          "Direct Gaussian ERI cache size must not exceed capacity.");
    }
    if (!Double.isFinite(screeningTolerance) || screeningTolerance < 0.0) {
      throw new IllegalArgumentException(
          "Direct Gaussian ERI screening tolerance must be finite and non-negative.");
    }
    return new DirectGaussianEriStatistics(
        valueQueryCount,
        screenedOutQueryCount,
        cacheHitCount,
        computedIntegralCount,
        pairBoundIntegralCount,
        pairBoundBuildCount,
        cacheSize,
        cacheCapacity,
        screeningTolerance);
  }

  public long valueQueryCount() {
    return this.valueQueryCount;
  }

  public long screenedOutQueryCount() {
    return this.screenedOutQueryCount;
  }

  public long cacheHitCount() {
    return this.cacheHitCount;
  }

  public long computedIntegralCount() {
    return this.computedIntegralCount;
  }

  public long pairBoundIntegralCount() {
    return this.pairBoundIntegralCount;
  }

  public long pairBoundBuildCount() {
    return this.pairBoundBuildCount;
  }

  public int cacheSize() {
    return this.cacheSize;
  }

  public int cacheCapacity() {
    return this.cacheCapacity;
  }

  public double screeningTolerance() {
    return this.screeningTolerance;
  }

  public double screenedOutRatio() {
    if (this.valueQueryCount == 0L) {
      return 0.0;
    }
    return (double) this.screenedOutQueryCount / (double) this.valueQueryCount;
  }

  public double cacheHitRatio() {
    final long resolvedQueryCount = this.cacheHitCount + this.computedIntegralCount;
    if (resolvedQueryCount == 0L) {
      return 0.0;
    }
    return (double) this.cacheHitCount / (double) resolvedQueryCount;
  }

  private static void requireNonNegative(final long value, final String subject) {
    if (value < 0L) {
      throw new IllegalArgumentException(
          "Direct Gaussian ERI " + subject + " must be non-negative.");
    }
  }
}
