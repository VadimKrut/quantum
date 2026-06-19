/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

import java.util.Objects;

public final class ActiveSpaceResourceEstimate {

  private final int electronCount;
  private final int spatialOrbitalCount;
  private final int spinOrbitalCount;
  private final int qubitCount;
  private final int virtualSpinOrbitalCount;
  private final long singleExcitationCount;
  private final long doubleExcitationCount;
  private final long uccsdParameterCount;
  private final CombinatorialCount determinantCount;

  private ActiveSpaceResourceEstimate(
      final int electronCount,
      final int spatialOrbitalCount,
      final int spinOrbitalCount,
      final int qubitCount,
      final int virtualSpinOrbitalCount,
      final long singleExcitationCount,
      final long doubleExcitationCount,
      final long uccsdParameterCount,
      final CombinatorialCount determinantCount) {
    this.electronCount = electronCount;
    this.spatialOrbitalCount = spatialOrbitalCount;
    this.spinOrbitalCount = spinOrbitalCount;
    this.qubitCount = qubitCount;
    this.virtualSpinOrbitalCount = virtualSpinOrbitalCount;
    this.singleExcitationCount = singleExcitationCount;
    this.doubleExcitationCount = doubleExcitationCount;
    this.uccsdParameterCount = uccsdParameterCount;
    this.determinantCount = determinantCount;
  }

  public static ActiveSpaceResourceEstimate of(final ActiveSpace activeSpace) {
    if (activeSpace == null) {
      throw new IllegalArgumentException(
          "Active space resource estimate active space must not be null.");
    }
    final int spinOrbitalCount = activeSpace.spinOrbitalCount();
    final int virtualSpinOrbitalCount = spinOrbitalCount - activeSpace.electronCount();
    final long singleExcitationCount =
        Math.multiplyExact((long) activeSpace.electronCount(), virtualSpinOrbitalCount);
    final long doubleExcitationCount =
        ActiveSpaceResourceEstimate.multiplySaturated(
            ActiveSpaceResourceEstimate.combinationExactOrSaturated(activeSpace.electronCount(), 2),
            ActiveSpaceResourceEstimate.combinationExactOrSaturated(virtualSpinOrbitalCount, 2));
    return new ActiveSpaceResourceEstimate(
        activeSpace.electronCount(),
        activeSpace.orbitalCount(),
        spinOrbitalCount,
        spinOrbitalCount,
        virtualSpinOrbitalCount,
        singleExcitationCount,
        doubleExcitationCount,
        ActiveSpaceResourceEstimate.addSaturated(singleExcitationCount, doubleExcitationCount),
        ActiveSpaceResourceEstimate.combinationExactOrSaturated(
            spinOrbitalCount, activeSpace.electronCount()));
  }

  public int electronCount() {
    return this.electronCount;
  }

  public int spatialOrbitalCount() {
    return this.spatialOrbitalCount;
  }

  public int spinOrbitalCount() {
    return this.spinOrbitalCount;
  }

  public int qubitCount() {
    return this.qubitCount;
  }

  public int virtualSpinOrbitalCount() {
    return this.virtualSpinOrbitalCount;
  }

  public long singleExcitationCount() {
    return this.singleExcitationCount;
  }

  public long doubleExcitationCount() {
    return this.doubleExcitationCount;
  }

  public long uccsdParameterCount() {
    return this.uccsdParameterCount;
  }

  public CombinatorialCount determinantCount() {
    return this.determinantCount;
  }

  private static CombinatorialCount combinationExactOrSaturated(
      final int n,
      final int k
  ) {
    if (k < 0 || k > n) {
      return CombinatorialCount.exact(0L);
    }
    int reducedK = k;
    if (reducedK > n - reducedK) {
      reducedK = n - reducedK;
    }
    long result = 1L;
    for (int i = 1; i <= reducedK; ++i) {
      long numerator = n - reducedK + i;
      long denominator = i;
      final long denominatorResultDivisor =
          ActiveSpaceResourceEstimate.greatestCommonDivisor(result, denominator);
      result /= denominatorResultDivisor;
      final long numeratorDenominatorDivisor =
          ActiveSpaceResourceEstimate.greatestCommonDivisor(
              numerator, denominator /= denominatorResultDivisor);
      numerator /= numeratorDenominatorDivisor;
      if ((denominator /= numeratorDenominatorDivisor) != 1L) {
        throw new IllegalStateException("Combination reduction failed.");
      }
      try {
        result = Math.multiplyExact(result, numerator);
        continue;
      } catch (final ArithmeticException exception) {
        return CombinatorialCount.saturated();
      }
    }
    return CombinatorialCount.exact(result);
  }

  private static long multiplySaturated(
      final CombinatorialCount first,
      final CombinatorialCount second
  ) {
    if (first.saturatedValue() || second.saturatedValue()) {
      return Long.MAX_VALUE;
    }
    try {
      return Math.multiplyExact(first.value(), second.value());
    } catch (final ArithmeticException exception) {
      return Long.MAX_VALUE;
    }
  }

  private static long addSaturated(
      final long first,
      final long second
  ) {
    try {
      return Math.addExact(first, second);
    } catch (final ArithmeticException exception) {
      return Long.MAX_VALUE;
    }
  }

  private static long greatestCommonDivisor(
      final long first,
      final long second
  ) {
    long a = Math.abs(first);
    long b = Math.abs(second);
    while (b != 0L) {
      final long next = a % b;
      a = b;
      b = next;
    }
    return a;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ActiveSpaceResourceEstimate)) {
      return false;
    }
    final ActiveSpaceResourceEstimate estimate = (ActiveSpaceResourceEstimate) other;
    return this.electronCount == estimate.electronCount
        && this.spatialOrbitalCount == estimate.spatialOrbitalCount
        && this.spinOrbitalCount == estimate.spinOrbitalCount
        && this.qubitCount == estimate.qubitCount
        && this.virtualSpinOrbitalCount == estimate.virtualSpinOrbitalCount
        && this.singleExcitationCount == estimate.singleExcitationCount
        && this.doubleExcitationCount == estimate.doubleExcitationCount
        && this.uccsdParameterCount == estimate.uccsdParameterCount
        && Objects.equals(this.determinantCount, estimate.determinantCount);
  }

  public int hashCode() {
    return Objects.hash(
        this.electronCount,
        this.spatialOrbitalCount,
        this.spinOrbitalCount,
        this.qubitCount,
        this.virtualSpinOrbitalCount,
        this.singleExcitationCount,
        this.doubleExcitationCount,
        this.uccsdParameterCount,
        this.determinantCount);
  }
}