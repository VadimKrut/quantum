/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

public final class TwoElectronIntegral {

  private final SpatialOrbitalIndex firstOrbital;
  private final SpatialOrbitalIndex secondOrbital;
  private final SpatialOrbitalIndex thirdOrbital;
  private final SpatialOrbitalIndex fourthOrbital;
  private final double value;

  private TwoElectronIntegral(
      final SpatialOrbitalIndex firstOrbital,
      final SpatialOrbitalIndex secondOrbital,
      final SpatialOrbitalIndex thirdOrbital,
      final SpatialOrbitalIndex fourthOrbital,
      final double value) {
    this.firstOrbital = firstOrbital;
    this.secondOrbital = secondOrbital;
    this.thirdOrbital = thirdOrbital;
    this.fourthOrbital = fourthOrbital;
    this.value = value;
  }

  public static TwoElectronIntegral of(
      final int firstOrbital,
      final int secondOrbital,
      final int thirdOrbital,
      final int fourthOrbital,
      final double value) {
    return TwoElectronIntegral.of(
        SpatialOrbitalIndex.of(firstOrbital),
        SpatialOrbitalIndex.of(secondOrbital),
        SpatialOrbitalIndex.of(thirdOrbital),
        SpatialOrbitalIndex.of(fourthOrbital),
        value);
  }

  public static TwoElectronIntegral of(
      final SpatialOrbitalIndex firstOrbital,
      final SpatialOrbitalIndex secondOrbital,
      final SpatialOrbitalIndex thirdOrbital,
      final SpatialOrbitalIndex fourthOrbital,
      final double value) {
    TwoElectronIntegral.requireOrbital(firstOrbital, "First orbital index");
    TwoElectronIntegral.requireOrbital(secondOrbital, "Second orbital index");
    TwoElectronIntegral.requireOrbital(thirdOrbital, "Third orbital index");
    TwoElectronIntegral.requireOrbital(fourthOrbital, "Fourth orbital index");
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Two-electron integral value must be finite.");
    }
    return new TwoElectronIntegral(firstOrbital, secondOrbital, thirdOrbital, fourthOrbital, value);
  }

  public SpatialOrbitalIndex firstOrbital() {
    return firstOrbital;
  }

  public SpatialOrbitalIndex secondOrbital() {
    return secondOrbital;
  }

  public SpatialOrbitalIndex thirdOrbital() {
    return thirdOrbital;
  }

  public SpatialOrbitalIndex fourthOrbital() {
    return fourthOrbital;
  }

  public double value() {
    return value;
  }

  public void requireWithin(final int orbitalCount) {
    firstOrbital.requireWithin(orbitalCount);
    secondOrbital.requireWithin(orbitalCount);
    thirdOrbital.requireWithin(orbitalCount);
    fourthOrbital.requireWithin(orbitalCount);
  }

  public boolean sameSymmetrySlot(final TwoElectronIntegral other) {
    if (other == null) {
      return false;
    }
    return TwoElectronIntegral.pairEqual(
                firstOrbital, secondOrbital, other.firstOrbital, other.secondOrbital)
            && TwoElectronIntegral.pairEqual(
                thirdOrbital, fourthOrbital, other.thirdOrbital, other.fourthOrbital)
        || TwoElectronIntegral.pairEqual(
                firstOrbital, secondOrbital, other.thirdOrbital, other.fourthOrbital)
            && TwoElectronIntegral.pairEqual(
                thirdOrbital, fourthOrbital, other.firstOrbital, other.secondOrbital);
  }

  public long symmetrySlotKey() {
    final long firstPairKey =
        TwoElectronIntegral.hermitianPairKey(firstOrbital.value(), secondOrbital.value());
    final long secondPairKey =
        TwoElectronIntegral.hermitianPairKey(thirdOrbital.value(), fourthOrbital.value());
    final long lower = Math.min(firstPairKey, secondPairKey);
    final long upper = Math.max(firstPairKey, secondPairKey);
    return Math.addExact(Math.multiplyExact(upper, Math.addExact(upper, 1L)) / 2L, lower);
  }

  private static boolean pairEqual(
      final SpatialOrbitalIndex leftFirst,
      final SpatialOrbitalIndex leftSecond,
      final SpatialOrbitalIndex rightFirst,
      final SpatialOrbitalIndex rightSecond) {
    return leftFirst.equals(rightFirst) && leftSecond.equals(rightSecond)
        || leftFirst.equals(rightSecond) && leftSecond.equals(rightFirst);
  }

  private static long hermitianPairKey(
      final int firstOrbital,
      final int secondOrbital
  ) {
    final int lower = Math.min(firstOrbital, secondOrbital);
    final int upper = Math.max(firstOrbital, secondOrbital);
    return (long) upper * ((long) upper + 1L) / 2L + (long) lower;
  }

  private static void requireOrbital(
      final SpatialOrbitalIndex orbital,
      final String subjectName
  ) {
    if (orbital == null) {
      throw new IllegalArgumentException(subjectName + " must not be null.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TwoElectronIntegral)) {
      return false;
    }
    final TwoElectronIntegral integral = (TwoElectronIntegral) other;
    return Double.compare(value, integral.value) == 0
        && Objects.equals(firstOrbital, integral.firstOrbital)
        && Objects.equals(secondOrbital, integral.secondOrbital)
        && Objects.equals(thirdOrbital, integral.thirdOrbital)
        && Objects.equals(fourthOrbital, integral.fourthOrbital);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, firstOrbital);
    result = ChemistryHash.include(result, secondOrbital);
    result = ChemistryHash.include(result, thirdOrbital);
    result = ChemistryHash.include(result, fourthOrbital);
    result = ChemistryHash.include(result, value);
    return result;
  }
}