/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2;

/** Результат unrestricted MP2 с разложением correlation energy на same-spin и opposite-spin вклады. */
public final class UnrestrictedMp2Result {

  private final double correlationEnergy;
  private final double totalEnergy;
  private final double sameSpinCorrelationEnergy;
  private final double oppositeSpinCorrelationEnergy;
  private final int alphaOccupiedOrbitalCount;
  private final int betaOccupiedOrbitalCount;
  private final int alphaVirtualOrbitalCount;
  private final int betaVirtualOrbitalCount;

  private UnrestrictedMp2Result(
      final double correlationEnergy,
      final double totalEnergy,
      final double sameSpinCorrelationEnergy,
      final double oppositeSpinCorrelationEnergy,
      final int alphaOccupiedOrbitalCount,
      final int betaOccupiedOrbitalCount,
      final int alphaVirtualOrbitalCount,
      final int betaVirtualOrbitalCount) {
    this.correlationEnergy = correlationEnergy;
    this.totalEnergy = totalEnergy;
    this.sameSpinCorrelationEnergy = sameSpinCorrelationEnergy;
    this.oppositeSpinCorrelationEnergy = oppositeSpinCorrelationEnergy;
    this.alphaOccupiedOrbitalCount = alphaOccupiedOrbitalCount;
    this.betaOccupiedOrbitalCount = betaOccupiedOrbitalCount;
    this.alphaVirtualOrbitalCount = alphaVirtualOrbitalCount;
    this.betaVirtualOrbitalCount = betaVirtualOrbitalCount;
  }

  public static UnrestrictedMp2Result of(
      final double correlationEnergy,
      final double totalEnergy,
      final double sameSpinCorrelationEnergy,
      final double oppositeSpinCorrelationEnergy,
      final int alphaOccupiedOrbitalCount,
      final int betaOccupiedOrbitalCount,
      final int alphaVirtualOrbitalCount,
      final int betaVirtualOrbitalCount) {
    if (!Double.isFinite(correlationEnergy)
        || !Double.isFinite(totalEnergy)
        || !Double.isFinite(sameSpinCorrelationEnergy)
        || !Double.isFinite(oppositeSpinCorrelationEnergy)) {
      throw new IllegalArgumentException("UMP2 energies must be finite.");
    }
    if (alphaOccupiedOrbitalCount < 0 || betaOccupiedOrbitalCount < 0) {
      throw new IllegalArgumentException("UMP2 occupied orbital counts must not be negative.");
    }
    if (alphaVirtualOrbitalCount < 0 || betaVirtualOrbitalCount < 0) {
      throw new IllegalArgumentException("UMP2 virtual orbital counts must not be negative.");
    }
    if (alphaOccupiedOrbitalCount + betaOccupiedOrbitalCount <= 0) {
      throw new IllegalArgumentException("UMP2 requires at least one occupied spin orbital.");
    }
    if (alphaVirtualOrbitalCount + betaVirtualOrbitalCount <= 0) {
      throw new IllegalArgumentException("UMP2 requires at least one virtual spin orbital.");
    }
    final double sum = sameSpinCorrelationEnergy + oppositeSpinCorrelationEnergy;
    if (Math.abs(sum - correlationEnergy) > 1.0E-9) {
      throw new IllegalArgumentException(
          "UMP2 correlation energy must equal same-spin plus opposite-spin parts.");
    }
    return new UnrestrictedMp2Result(
        correlationEnergy,
        totalEnergy,
        sameSpinCorrelationEnergy,
        oppositeSpinCorrelationEnergy,
        alphaOccupiedOrbitalCount,
        betaOccupiedOrbitalCount,
        alphaVirtualOrbitalCount,
        betaVirtualOrbitalCount);
  }

  public double correlationEnergy() {
    return this.correlationEnergy;
  }

  public double totalEnergy() {
    return this.totalEnergy;
  }

  public double sameSpinCorrelationEnergy() {
    return this.sameSpinCorrelationEnergy;
  }

  public double oppositeSpinCorrelationEnergy() {
    return this.oppositeSpinCorrelationEnergy;
  }

  public int alphaOccupiedOrbitalCount() {
    return this.alphaOccupiedOrbitalCount;
  }

  public int betaOccupiedOrbitalCount() {
    return this.betaOccupiedOrbitalCount;
  }

  public int alphaVirtualOrbitalCount() {
    return this.alphaVirtualOrbitalCount;
  }

  public int betaVirtualOrbitalCount() {
    return this.betaVirtualOrbitalCount;
  }
}
