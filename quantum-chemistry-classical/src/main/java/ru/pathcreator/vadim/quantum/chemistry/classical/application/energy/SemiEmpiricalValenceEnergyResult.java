/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.energy;

public final class SemiEmpiricalValenceEnergyResult {

  private final double totalEnergyHartree;
  private final double electronicEnergyHartree;
  private final double bondEnergyHartree;
  private final double strainEnergyHartree;
  private final double conjugationEnergyHartree;
  private final double chargePenaltyHartree;
  private final double radicalPenaltyHartree;
  private final double torsionPenaltyHartree;
  private final int atomCount;
  private final int bondCount;
  private final int aromaticBondCount;

  private SemiEmpiricalValenceEnergyResult(
      final double totalEnergyHartree,
      final double electronicEnergyHartree,
      final double bondEnergyHartree,
      final double strainEnergyHartree,
      final double conjugationEnergyHartree,
      final double chargePenaltyHartree,
      final double radicalPenaltyHartree,
      final double torsionPenaltyHartree,
      final int atomCount,
      final int bondCount,
      final int aromaticBondCount) {
    this.totalEnergyHartree = totalEnergyHartree;
    this.electronicEnergyHartree = electronicEnergyHartree;
    this.bondEnergyHartree = bondEnergyHartree;
    this.strainEnergyHartree = strainEnergyHartree;
    this.conjugationEnergyHartree = conjugationEnergyHartree;
    this.chargePenaltyHartree = chargePenaltyHartree;
    this.radicalPenaltyHartree = radicalPenaltyHartree;
    this.torsionPenaltyHartree = torsionPenaltyHartree;
    this.atomCount = atomCount;
    this.bondCount = bondCount;
    this.aromaticBondCount = aromaticBondCount;
  }

  public static SemiEmpiricalValenceEnergyResult of(
      final double totalEnergyHartree,
      final double electronicEnergyHartree,
      final double bondEnergyHartree,
      final double strainEnergyHartree,
      final double conjugationEnergyHartree,
      final double chargePenaltyHartree,
      final double radicalPenaltyHartree,
      final double torsionPenaltyHartree,
      final int atomCount,
      final int bondCount,
      final int aromaticBondCount) {
    SemiEmpiricalValenceEnergyResult.requireFinite(
        totalEnergyHartree, "Total semiempirical energy");
    SemiEmpiricalValenceEnergyResult.requireFinite(
        electronicEnergyHartree, "Electronic semiempirical energy");
    SemiEmpiricalValenceEnergyResult.requireFinite(bondEnergyHartree, "Bond semiempirical energy");
    SemiEmpiricalValenceEnergyResult.requireFinite(
        strainEnergyHartree, "Strain semiempirical energy");
    SemiEmpiricalValenceEnergyResult.requireFinite(
        conjugationEnergyHartree, "Conjugation semiempirical energy");
    SemiEmpiricalValenceEnergyResult.requireFinite(
        chargePenaltyHartree, "Charge semiempirical penalty");
    SemiEmpiricalValenceEnergyResult.requireFinite(
        radicalPenaltyHartree, "Radical semiempirical penalty");
    SemiEmpiricalValenceEnergyResult.requireFinite(
        torsionPenaltyHartree, "Torsion semiempirical penalty");
    if (atomCount < 1) {
      throw new IllegalArgumentException("Semiempirical atom count must be positive.");
    }
    if (bondCount < 0 || aromaticBondCount < 0 || aromaticBondCount > bondCount) {
      throw new IllegalArgumentException("Semiempirical bond counts must be consistent.");
    }
    return new SemiEmpiricalValenceEnergyResult(
        totalEnergyHartree,
        electronicEnergyHartree,
        bondEnergyHartree,
        strainEnergyHartree,
        conjugationEnergyHartree,
        chargePenaltyHartree,
        radicalPenaltyHartree,
        torsionPenaltyHartree,
        atomCount,
        bondCount,
        aromaticBondCount);
  }

  public double totalEnergyHartree() {
    return this.totalEnergyHartree;
  }

  public double electronicEnergyHartree() {
    return this.electronicEnergyHartree;
  }

  public double bondEnergyHartree() {
    return this.bondEnergyHartree;
  }

  public double strainEnergyHartree() {
    return this.strainEnergyHartree;
  }

  public double conjugationEnergyHartree() {
    return this.conjugationEnergyHartree;
  }

  public double chargePenaltyHartree() {
    return this.chargePenaltyHartree;
  }

  public double radicalPenaltyHartree() {
    return this.radicalPenaltyHartree;
  }

  public double torsionPenaltyHartree() {
    return this.torsionPenaltyHartree;
  }

  public int atomCount() {
    return this.atomCount;
  }

  public int bondCount() {
    return this.bondCount;
  }

  public int aromaticBondCount() {
    return this.aromaticBondCount;
  }

  private static void requireFinite(final double value, final String subject) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subject + " must be finite.");
    }
  }
}
