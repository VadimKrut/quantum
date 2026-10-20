/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.periodic;

/** Сводка Monkhorst-Pack sampling и простого free-electron band probe для периодической ячейки. */
public final class PeriodicKPointSamplingResult {

  private final int meshA;
  private final int meshB;
  private final int meshC;
  private final long kPointCount;
  private final double reciprocalAInverseAngstrom;
  private final double reciprocalBInverseAngstrom;
  private final double reciprocalCInverseAngstrom;
  private final double freeElectronBandMinimumHartree;
  private final double freeElectronBandMaximumHartree;
  private final double freeElectronBandWidthHartree;

  private PeriodicKPointSamplingResult(
      final int meshA,
      final int meshB,
      final int meshC,
      final long kPointCount,
      final double reciprocalAInverseAngstrom,
      final double reciprocalBInverseAngstrom,
      final double reciprocalCInverseAngstrom,
      final double freeElectronBandMinimumHartree,
      final double freeElectronBandMaximumHartree,
      final double freeElectronBandWidthHartree) {
    this.meshA = meshA;
    this.meshB = meshB;
    this.meshC = meshC;
    this.kPointCount = kPointCount;
    this.reciprocalAInverseAngstrom = reciprocalAInverseAngstrom;
    this.reciprocalBInverseAngstrom = reciprocalBInverseAngstrom;
    this.reciprocalCInverseAngstrom = reciprocalCInverseAngstrom;
    this.freeElectronBandMinimumHartree = freeElectronBandMinimumHartree;
    this.freeElectronBandMaximumHartree = freeElectronBandMaximumHartree;
    this.freeElectronBandWidthHartree = freeElectronBandWidthHartree;
  }

  public static PeriodicKPointSamplingResult of(
      final int meshA,
      final int meshB,
      final int meshC,
      final double reciprocalAInverseAngstrom,
      final double reciprocalBInverseAngstrom,
      final double reciprocalCInverseAngstrom,
      final double freeElectronBandMinimumHartree,
      final double freeElectronBandMaximumHartree) {
    PeriodicKPointSamplingResult.requirePositiveMesh(meshA, "Periodic k-point mesh a");
    PeriodicKPointSamplingResult.requirePositiveMesh(meshB, "Periodic k-point mesh b");
    PeriodicKPointSamplingResult.requirePositiveMesh(meshC, "Periodic k-point mesh c");
    PeriodicKPointSamplingResult.requireFiniteNonNegative(
        reciprocalAInverseAngstrom, "Periodic reciprocal a length");
    PeriodicKPointSamplingResult.requireFiniteNonNegative(
        reciprocalBInverseAngstrom, "Periodic reciprocal b length");
    PeriodicKPointSamplingResult.requireFiniteNonNegative(
        reciprocalCInverseAngstrom, "Periodic reciprocal c length");
    PeriodicKPointSamplingResult.requireFiniteNonNegative(
        freeElectronBandMinimumHartree, "Periodic free-electron band minimum");
    PeriodicKPointSamplingResult.requireFiniteNonNegative(
        freeElectronBandMaximumHartree, "Periodic free-electron band maximum");
    if (freeElectronBandMaximumHartree < freeElectronBandMinimumHartree) {
      throw new IllegalArgumentException(
          "Periodic free-electron band maximum must not be below minimum.");
    }
    return new PeriodicKPointSamplingResult(
        meshA,
        meshB,
        meshC,
        Math.multiplyExact(Math.multiplyExact((long) meshA, (long) meshB), (long) meshC),
        reciprocalAInverseAngstrom,
        reciprocalBInverseAngstrom,
        reciprocalCInverseAngstrom,
        freeElectronBandMinimumHartree,
        freeElectronBandMaximumHartree,
        freeElectronBandMaximumHartree - freeElectronBandMinimumHartree);
  }

  public int meshA() {
    return this.meshA;
  }

  public int meshB() {
    return this.meshB;
  }

  public int meshC() {
    return this.meshC;
  }

  public long kPointCount() {
    return this.kPointCount;
  }

  public double reciprocalAInverseAngstrom() {
    return this.reciprocalAInverseAngstrom;
  }

  public double reciprocalBInverseAngstrom() {
    return this.reciprocalBInverseAngstrom;
  }

  public double reciprocalCInverseAngstrom() {
    return this.reciprocalCInverseAngstrom;
  }

  public double freeElectronBandMinimumHartree() {
    return this.freeElectronBandMinimumHartree;
  }

  public double freeElectronBandMaximumHartree() {
    return this.freeElectronBandMaximumHartree;
  }

  public double freeElectronBandWidthHartree() {
    return this.freeElectronBandWidthHartree;
  }

  private static void requirePositiveMesh(final int value, final String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive.");
    }
  }

  private static void requireFiniteNonNegative(final double value, final String name) {
    if (!Double.isFinite(value) || value < 0.0) {
      throw new IllegalArgumentException(name + " must be finite and non-negative.");
    }
  }
}
