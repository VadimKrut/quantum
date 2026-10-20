/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2;

/** Результат closed-shell MP2 с correlation и total energy. */
public final class Mp2Result {

  private final double correlationEnergy;
  private final double totalEnergy;
  private final int occupiedOrbitalCount;
  private final int virtualOrbitalCount;

  private Mp2Result(
      final double correlationEnergy,
      final double totalEnergy,
      final int occupiedOrbitalCount,
      final int virtualOrbitalCount) {
    this.correlationEnergy = correlationEnergy;
    this.totalEnergy = totalEnergy;
    this.occupiedOrbitalCount = occupiedOrbitalCount;
    this.virtualOrbitalCount = virtualOrbitalCount;
  }

  public static Mp2Result of(
      final double correlationEnergy,
      final double totalEnergy,
      final int occupiedOrbitalCount,
      final int virtualOrbitalCount) {
    if (!Double.isFinite(correlationEnergy) || !Double.isFinite(totalEnergy)) {
      throw new IllegalArgumentException("MP2 energies must be finite.");
    }
    if (occupiedOrbitalCount <= 0) {
      throw new IllegalArgumentException("MP2 occupied orbital count must be positive.");
    }
    if (virtualOrbitalCount <= 0) {
      throw new IllegalArgumentException("MP2 virtual orbital count must be positive.");
    }
    return new Mp2Result(correlationEnergy, totalEnergy, occupiedOrbitalCount, virtualOrbitalCount);
  }

  public double correlationEnergy() {
    return this.correlationEnergy;
  }

  public double totalEnergy() {
    return this.totalEnergy;
  }

  public int occupiedOrbitalCount() {
    return this.occupiedOrbitalCount;
  }

  public int virtualOrbitalCount() {
    return this.virtualOrbitalCount;
  }
}
