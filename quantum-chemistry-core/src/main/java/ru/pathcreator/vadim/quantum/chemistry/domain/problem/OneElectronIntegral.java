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

public final class OneElectronIntegral {

  private final SpatialOrbitalIndex firstOrbital;
  private final SpatialOrbitalIndex secondOrbital;
  private final double value;

  private OneElectronIntegral(
      final SpatialOrbitalIndex firstOrbital,
      final SpatialOrbitalIndex secondOrbital,
      final double value) {
    this.firstOrbital = firstOrbital;
    this.secondOrbital = secondOrbital;
    this.value = value;
  }

  public static OneElectronIntegral of(
      final int firstOrbital, final int secondOrbital, final double value) {
    return OneElectronIntegral.of(
        SpatialOrbitalIndex.of(firstOrbital), SpatialOrbitalIndex.of(secondOrbital), value);
  }

  public static OneElectronIntegral of(
      final SpatialOrbitalIndex firstOrbital,
      final SpatialOrbitalIndex secondOrbital,
      final double value) {
    if (firstOrbital == null) {
      throw new IllegalArgumentException("First orbital index must not be null.");
    }
    if (secondOrbital == null) {
      throw new IllegalArgumentException("Second orbital index must not be null.");
    }
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("One-electron integral value must be finite.");
    }
    return new OneElectronIntegral(firstOrbital, secondOrbital, value);
  }

  public SpatialOrbitalIndex firstOrbital() {
    return firstOrbital;
  }

  public SpatialOrbitalIndex secondOrbital() {
    return secondOrbital;
  }

  public double value() {
    return value;
  }

  public void requireWithin(final int orbitalCount) {
    firstOrbital.requireWithin(orbitalCount);
    secondOrbital.requireWithin(orbitalCount);
  }

  public boolean sameSymmetrySlot(final OneElectronIntegral other) {
    if (other == null) {
      return false;
    }
    return sameOrderedSlot(other.firstOrbital, other.secondOrbital)
        || sameOrderedSlot(other.secondOrbital, other.firstOrbital);
  }

  public long symmetrySlotKey() {
    return OneElectronIntegral.hermitianSlotKey(firstOrbital.value(), secondOrbital.value());
  }

  private boolean sameOrderedSlot(
      final SpatialOrbitalIndex first, final SpatialOrbitalIndex second) {
    return firstOrbital.equals(first) && secondOrbital.equals(second);
  }

  private static long hermitianSlotKey(
      final int firstOrbital,
      final int secondOrbital
  ) {
    final int lower = Math.min(firstOrbital, secondOrbital);
    final int upper = Math.max(firstOrbital, secondOrbital);
    return (long) upper * ((long) upper + 1L) / 2L + (long) lower;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OneElectronIntegral)) {
      return false;
    }
    final OneElectronIntegral integral = (OneElectronIntegral) other;
    return Double.compare(value, integral.value) == 0
        && Objects.equals(firstOrbital, integral.firstOrbital)
        && Objects.equals(secondOrbital, integral.secondOrbital);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, firstOrbital);
    result = ChemistryHash.include(result, secondOrbital);
    result = ChemistryHash.include(result, value);
    return result;
  }
}