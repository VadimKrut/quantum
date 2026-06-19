/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.problem;

import java.util.List;
import java.util.Objects;

/**
 * Сводка формы и масштаба электронного Hamiltonian.
 *
 * <p>Сводка считается одним проходом по one-electron и two-electron integral lists и не создаёт
 * промежуточные коллекции. Она нужна валидатору и будущим preflight-оценкам сложности
 * квантово-химической задачи.
 */
public final class ElectronicHamiltonianSummary {

  private final int spatialOrbitalCount;
  private final int spinOrbitalCount;
  private final int electronCount;
  private final double nuclearRepulsionEnergy;
  private final int oneElectronTermCount;
  private final int twoElectronTermCount;
  private final int oneElectronDiagonalTermCount;
  private final int oneElectronCouplingTermCount;
  private final int twoElectronSamePairTermCount;
  private final int twoElectronExchangeLikeTermCount;
  private final int zeroIntegralTermCount;
  private final int nonZeroIntegralTermCount;
  private final int maxReferencedSpatialOrbitalIndex;
  private final long rawSpatialTensorSlotCount;
  private final double oneElectronL1Norm;
  private final double twoElectronL1Norm;
  private final double maxAbsoluteOneElectronIntegral;
  private final double maxAbsoluteTwoElectronIntegral;

  private ElectronicHamiltonianSummary(
      final int spatialOrbitalCount,
      final int spinOrbitalCount,
      final int electronCount,
      final double nuclearRepulsionEnergy,
      final int oneElectronTermCount,
      final int twoElectronTermCount,
      final int oneElectronDiagonalTermCount,
      final int oneElectronCouplingTermCount,
      final int twoElectronSamePairTermCount,
      final int twoElectronExchangeLikeTermCount,
      final int zeroIntegralTermCount,
      final int nonZeroIntegralTermCount,
      final int maxReferencedSpatialOrbitalIndex,
      final long rawSpatialTensorSlotCount,
      final double oneElectronL1Norm,
      final double twoElectronL1Norm,
      final double maxAbsoluteOneElectronIntegral,
      final double maxAbsoluteTwoElectronIntegral) {
    this.spatialOrbitalCount = spatialOrbitalCount;
    this.spinOrbitalCount = spinOrbitalCount;
    this.electronCount = electronCount;
    this.nuclearRepulsionEnergy = nuclearRepulsionEnergy;
    this.oneElectronTermCount = oneElectronTermCount;
    this.twoElectronTermCount = twoElectronTermCount;
    this.oneElectronDiagonalTermCount = oneElectronDiagonalTermCount;
    this.oneElectronCouplingTermCount = oneElectronCouplingTermCount;
    this.twoElectronSamePairTermCount = twoElectronSamePairTermCount;
    this.twoElectronExchangeLikeTermCount = twoElectronExchangeLikeTermCount;
    this.zeroIntegralTermCount = zeroIntegralTermCount;
    this.nonZeroIntegralTermCount = nonZeroIntegralTermCount;
    this.maxReferencedSpatialOrbitalIndex = maxReferencedSpatialOrbitalIndex;
    this.rawSpatialTensorSlotCount = rawSpatialTensorSlotCount;
    this.oneElectronL1Norm = oneElectronL1Norm;
    this.twoElectronL1Norm = twoElectronL1Norm;
    this.maxAbsoluteOneElectronIntegral = maxAbsoluteOneElectronIntegral;
    this.maxAbsoluteTwoElectronIntegral = maxAbsoluteTwoElectronIntegral;
  }

  public static ElectronicHamiltonianSummary of(final ElectronicHamiltonian hamiltonian) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian must not be null.");
    }
    final int spatialOrbitalCount = hamiltonian.spatialOrbitalCount();
    final int spinOrbitalCount = hamiltonian.spinOrbitalCount();
    final int electronCount = hamiltonian.electronCount();
    final long rawSpatialTensorSlotCount =
        ElectronicHamiltonianSummary.rawSpatialTensorSlotCount(spatialOrbitalCount);
    int oneElectronDiagonalTermCount = 0;
    int oneElectronCouplingTermCount = 0;
    int twoElectronSamePairTermCount = 0;
    int twoElectronExchangeLikeTermCount = 0;
    int zeroIntegralTermCount = 0;
    int nonZeroIntegralTermCount = 0;
    int maxReferencedSpatialOrbitalIndex = -1;
    double oneElectronL1Norm = 0.0;
    double twoElectronL1Norm = 0.0;
    double maxAbsoluteOneElectronIntegral = 0.0;
    double maxAbsoluteTwoElectronIntegral = 0.0;
    final List<OneElectronIntegral> oneElectronIntegrals = hamiltonian.oneElectronIntegrals();
    final List<TwoElectronIntegral> twoElectronIntegrals = hamiltonian.twoElectronIntegrals();
    for (int i = 0; i < oneElectronIntegrals.size(); ++i) {
      final OneElectronIntegral integral = oneElectronIntegrals.get(i);
      if (integral.firstOrbital().equals(integral.secondOrbital())) {
        ++oneElectronDiagonalTermCount;
      } else {
        ++oneElectronCouplingTermCount;
      }
      final double absoluteValue = Math.abs(integral.value());
      oneElectronL1Norm += absoluteValue;
      maxAbsoluteOneElectronIntegral = Math.max(maxAbsoluteOneElectronIntegral, absoluteValue);
      maxReferencedSpatialOrbitalIndex =
          Math.max(
              maxReferencedSpatialOrbitalIndex,
              Math.max(integral.firstOrbital().value(), integral.secondOrbital().value()));
      if (absoluteValue == 0.0) {
        ++zeroIntegralTermCount;
      } else {
        ++nonZeroIntegralTermCount;
      }
    }
    for (int i = 0; i < twoElectronIntegrals.size(); ++i) {
      final TwoElectronIntegral integral = twoElectronIntegrals.get(i);
      if (ElectronicHamiltonianSummary.samePair(integral)) {
        ++twoElectronSamePairTermCount;
      }
      if (ElectronicHamiltonianSummary.exchangeLike(integral)) {
        ++twoElectronExchangeLikeTermCount;
      }
      final double absoluteValue = Math.abs(integral.value());
      twoElectronL1Norm += absoluteValue;
      maxAbsoluteTwoElectronIntegral = Math.max(maxAbsoluteTwoElectronIntegral, absoluteValue);
      maxReferencedSpatialOrbitalIndex =
          Math.max(
              maxReferencedSpatialOrbitalIndex, ElectronicHamiltonianSummary.maxIndex(integral));
      if (absoluteValue == 0.0) {
        ++zeroIntegralTermCount;
      } else {
        ++nonZeroIntegralTermCount;
      }
    }
    return new ElectronicHamiltonianSummary(
        spatialOrbitalCount,
        spinOrbitalCount,
        electronCount,
        hamiltonian.nuclearRepulsionEnergy(),
        oneElectronIntegrals.size(),
        twoElectronIntegrals.size(),
        oneElectronDiagonalTermCount,
        oneElectronCouplingTermCount,
        twoElectronSamePairTermCount,
        twoElectronExchangeLikeTermCount,
        zeroIntegralTermCount,
        nonZeroIntegralTermCount,
        maxReferencedSpatialOrbitalIndex,
        rawSpatialTensorSlotCount,
        oneElectronL1Norm,
        twoElectronL1Norm,
        maxAbsoluteOneElectronIntegral,
        maxAbsoluteTwoElectronIntegral);
  }

  public int spatialOrbitalCount() {
    return spatialOrbitalCount;
  }

  public int spinOrbitalCount() {
    return spinOrbitalCount;
  }

  public int electronCount() {
    return electronCount;
  }

  public double nuclearRepulsionEnergy() {
    return nuclearRepulsionEnergy;
  }

  public int oneElectronTermCount() {
    return oneElectronTermCount;
  }

  public int twoElectronTermCount() {
    return twoElectronTermCount;
  }

  public int totalElectronicTermCount() {
    return Math.addExact(oneElectronTermCount, twoElectronTermCount);
  }

  public int oneElectronDiagonalTermCount() {
    return oneElectronDiagonalTermCount;
  }

  public int oneElectronCouplingTermCount() {
    return oneElectronCouplingTermCount;
  }

  public int twoElectronSamePairTermCount() {
    return twoElectronSamePairTermCount;
  }

  public int twoElectronExchangeLikeTermCount() {
    return twoElectronExchangeLikeTermCount;
  }

  public int zeroIntegralTermCount() {
    return zeroIntegralTermCount;
  }

  public int nonZeroIntegralTermCount() {
    return nonZeroIntegralTermCount;
  }

  public int maxReferencedSpatialOrbitalIndex() {
    return maxReferencedSpatialOrbitalIndex;
  }

  public long rawSpatialTensorSlotCount() {
    return rawSpatialTensorSlotCount;
  }

  public double oneElectronL1Norm() {
    return oneElectronL1Norm;
  }

  public double twoElectronL1Norm() {
    return twoElectronL1Norm;
  }

  public double electronicL1Norm() {
    return oneElectronL1Norm + twoElectronL1Norm;
  }

  public double totalAbsoluteEnergyScale() {
    return Math.abs(nuclearRepulsionEnergy) + electronicL1Norm();
  }

  public double maxAbsoluteOneElectronIntegral() {
    return maxAbsoluteOneElectronIntegral;
  }

  public double maxAbsoluteTwoElectronIntegral() {
    return maxAbsoluteTwoElectronIntegral;
  }

  public double rawSpatialTensorDensity() {
    if (rawSpatialTensorSlotCount == 0L) {
      return 0.0;
    }
    return (double) totalElectronicTermCount() / (double) rawSpatialTensorSlotCount;
  }

  public boolean hasNuclearRepulsion() {
    return nuclearRepulsionEnergy != 0.0;
  }

  public boolean hasElectronicTerms() {
    return this.totalElectronicTermCount() > 0;
  }

  public boolean hasOrbitalCouplings() {
    return oneElectronCouplingTermCount > 0 || twoElectronExchangeLikeTermCount > 0;
  }

  private static boolean samePair(final TwoElectronIntegral integral) {
    return integral.firstOrbital().equals(integral.thirdOrbital())
        && integral.secondOrbital().equals(integral.fourthOrbital());
  }

  private static boolean exchangeLike(final TwoElectronIntegral integral) {
    return !integral.firstOrbital().equals(integral.secondOrbital())
        && integral.firstOrbital().equals(integral.fourthOrbital())
        && integral.secondOrbital().equals(integral.thirdOrbital());
  }

  private static int maxIndex(final TwoElectronIntegral integral) {
    return Math.max(
        Math.max(integral.firstOrbital().value(), integral.secondOrbital().value()),
        Math.max(integral.thirdOrbital().value(), integral.fourthOrbital().value()));
  }

  private static long rawSpatialTensorSlotCount(final int spatialOrbitalCount) {
    final long orbitalCount = spatialOrbitalCount;
    final long oneElectronSlotCount = Math.multiplyExact(orbitalCount, orbitalCount);
    final long twoElectronPairCount = Math.multiplyExact(orbitalCount, orbitalCount);
    final long twoElectronSlotCount =
        Math.multiplyExact(twoElectronPairCount, twoElectronPairCount);
    return Math.addExact(oneElectronSlotCount, twoElectronSlotCount);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronicHamiltonianSummary)) {
      return false;
    }
    final ElectronicHamiltonianSummary summary = (ElectronicHamiltonianSummary) other;
    return spatialOrbitalCount == summary.spatialOrbitalCount
        && spinOrbitalCount == summary.spinOrbitalCount
        && electronCount == summary.electronCount
        && Double.compare(nuclearRepulsionEnergy, summary.nuclearRepulsionEnergy) == 0
        && oneElectronTermCount == summary.oneElectronTermCount
        && twoElectronTermCount == summary.twoElectronTermCount
        && oneElectronDiagonalTermCount == summary.oneElectronDiagonalTermCount
        && oneElectronCouplingTermCount == summary.oneElectronCouplingTermCount
        && twoElectronSamePairTermCount == summary.twoElectronSamePairTermCount
        && twoElectronExchangeLikeTermCount == summary.twoElectronExchangeLikeTermCount
        && zeroIntegralTermCount == summary.zeroIntegralTermCount
        && nonZeroIntegralTermCount == summary.nonZeroIntegralTermCount
        && maxReferencedSpatialOrbitalIndex == summary.maxReferencedSpatialOrbitalIndex
        && rawSpatialTensorSlotCount == summary.rawSpatialTensorSlotCount
        && Double.compare(oneElectronL1Norm, summary.oneElectronL1Norm) == 0
        && Double.compare(twoElectronL1Norm, summary.twoElectronL1Norm) == 0
        && Double.compare(maxAbsoluteOneElectronIntegral, summary.maxAbsoluteOneElectronIntegral)
            == 0
        && Double.compare(maxAbsoluteTwoElectronIntegral, summary.maxAbsoluteTwoElectronIntegral)
            == 0;
  }

  public int hashCode() {
    return Objects.hash(
        spatialOrbitalCount,
        spinOrbitalCount,
        electronCount,
        nuclearRepulsionEnergy,
        oneElectronTermCount,
        twoElectronTermCount,
        oneElectronDiagonalTermCount,
        oneElectronCouplingTermCount,
        twoElectronSamePairTermCount,
        twoElectronExchangeLikeTermCount,
        zeroIntegralTermCount,
        nonZeroIntegralTermCount,
        maxReferencedSpatialOrbitalIndex,
        rawSpatialTensorSlotCount,
        oneElectronL1Norm,
        twoElectronL1Norm,
        maxAbsoluteOneElectronIntegral,
        maxAbsoluteTwoElectronIntegral);
  }
}