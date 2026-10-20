/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz;

import java.util.Arrays;

/**
 * Spin-conserving UCCSD excitation между occupied и virtual spin-orbitals.
 */
public final class UccsdExcitation {

  private final ChemistryExcitationRank rank;
  private final int[] occupiedSpinOrbitals;
  private final int[] virtualSpinOrbitals;

  private UccsdExcitation(
      final ChemistryExcitationRank rank,
      final int[] occupiedSpinOrbitals,
      final int[] virtualSpinOrbitals) {
    this.rank = rank;
    this.occupiedSpinOrbitals = occupiedSpinOrbitals;
    this.virtualSpinOrbitals = virtualSpinOrbitals;
  }

  public static UccsdExcitation single(
      final int occupiedSpinOrbital, final int virtualSpinOrbital) {
    UccsdExcitation.requireSpinConserving(occupiedSpinOrbital, virtualSpinOrbital);
    return new UccsdExcitation(
        ChemistryExcitationRank.SINGLE,
        new int[] {occupiedSpinOrbital},
        new int[] {virtualSpinOrbital});
  }

  public static UccsdExcitation doubleExcitation(
      final int firstOccupiedSpinOrbital,
      final int secondOccupiedSpinOrbital,
      final int firstVirtualSpinOrbital,
      final int secondVirtualSpinOrbital) {
    UccsdExcitation.requireDistinct(
        firstOccupiedSpinOrbital, secondOccupiedSpinOrbital, "occupied spin-orbitals");
    UccsdExcitation.requireDistinct(
        firstVirtualSpinOrbital, secondVirtualSpinOrbital, "virtual spin-orbitals");
    UccsdExcitation.requireSpinSetConserving(
        firstOccupiedSpinOrbital,
        secondOccupiedSpinOrbital,
        firstVirtualSpinOrbital,
        secondVirtualSpinOrbital);
    return new UccsdExcitation(
        ChemistryExcitationRank.DOUBLE,
        new int[] {firstOccupiedSpinOrbital, secondOccupiedSpinOrbital},
        new int[] {firstVirtualSpinOrbital, secondVirtualSpinOrbital});
  }

  public ChemistryExcitationRank rank() {
    return this.rank;
  }

  public int occupiedSpinOrbital(final int index) {
    return this.occupiedSpinOrbitals[index];
  }

  public int virtualSpinOrbital(final int index) {
    return this.virtualSpinOrbitals[index];
  }

  public int occupiedCount() {
    return this.occupiedSpinOrbitals.length;
  }

  public int virtualCount() {
    return this.virtualSpinOrbitals.length;
  }

  public int[] occupiedSpinOrbitalsCopy() {
    return this.occupiedSpinOrbitals.clone();
  }

  public int[] virtualSpinOrbitalsCopy() {
    return this.virtualSpinOrbitals.clone();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof UccsdExcitation)) {
      return false;
    }
    final UccsdExcitation excitation = (UccsdExcitation) other;
    return this.rank == excitation.rank
        && Arrays.equals(this.occupiedSpinOrbitals, excitation.occupiedSpinOrbitals)
        && Arrays.equals(this.virtualSpinOrbitals, excitation.virtualSpinOrbitals);
  }

  public int hashCode() {
    int result = this.rank.hashCode();
    result = 31 * result + Arrays.hashCode(this.occupiedSpinOrbitals);
    result = 31 * result + Arrays.hashCode(this.virtualSpinOrbitals);
    return result;
  }

  private static void requireSpinConserving(
      final int occupiedSpinOrbital, final int virtualSpinOrbital) {
    UccsdExcitation.requireNonNegative(occupiedSpinOrbital, "occupied spin-orbital");
    UccsdExcitation.requireNonNegative(virtualSpinOrbital, "virtual spin-orbital");
    if (UccsdExcitation.spinProjectionIndex(occupiedSpinOrbital)
        != UccsdExcitation.spinProjectionIndex(virtualSpinOrbital)) {
      throw new IllegalArgumentException("UCCSD single excitation must conserve spin projection.");
    }
  }

  private static void requireSpinSetConserving(
      final int firstOccupiedSpinOrbital,
      final int secondOccupiedSpinOrbital,
      final int firstVirtualSpinOrbital,
      final int secondVirtualSpinOrbital) {
    UccsdExcitation.requireNonNegative(firstOccupiedSpinOrbital, "first occupied spin-orbital");
    UccsdExcitation.requireNonNegative(secondOccupiedSpinOrbital, "second occupied spin-orbital");
    UccsdExcitation.requireNonNegative(firstVirtualSpinOrbital, "first virtual spin-orbital");
    UccsdExcitation.requireNonNegative(secondVirtualSpinOrbital, "second virtual spin-orbital");
    final int occupiedAlphaCount =
        UccsdExcitation.alphaContribution(firstOccupiedSpinOrbital)
            + UccsdExcitation.alphaContribution(secondOccupiedSpinOrbital);
    final int virtualAlphaCount =
        UccsdExcitation.alphaContribution(firstVirtualSpinOrbital)
            + UccsdExcitation.alphaContribution(secondVirtualSpinOrbital);
    if (occupiedAlphaCount != virtualAlphaCount) {
      throw new IllegalArgumentException(
          "UCCSD double excitation must conserve spin projection set.");
    }
  }

  private static void requireDistinct(final int first, final int second, final String subjectName) {
    if (first == second) {
      throw new IllegalArgumentException(
          "UCCSD double excitation requires distinct " + subjectName + ".");
    }
  }

  private static void requireNonNegative(final int value, final String subjectName) {
    if (value < 0) {
      throw new IllegalArgumentException("UCCSD " + subjectName + " must not be negative.");
    }
  }

  private static int alphaContribution(final int spinOrbital) {
    return UccsdExcitation.spinProjectionIndex(spinOrbital) == 0 ? 1 : 0;
  }

  private static int spinProjectionIndex(final int spinOrbital) {
    return spinOrbital & 1;
  }
}
