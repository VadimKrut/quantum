/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.periodic;

/** Сводка периодической постановки для молекулярного или реакционного расчета. */
public final class PeriodicBoundaryResult {

  private final PeriodicBoundaryModel model;
  private final PeriodicCell cell;
  private final int supercellA;
  private final int supercellB;
  private final int supercellC;
  private final long imageCount;
  private final long expandedAtomCount;
  private final double densityAtomsPerAngstrom3;
  private final double minimumImageCutoffAngstrom;
  private final boolean finiteMoleculeFitsCell;
  private final PeriodicKPointSamplingResult kPointSamplingResult;

  private PeriodicBoundaryResult(
      final PeriodicBoundaryModel model,
      final PeriodicCell cell,
      final int supercellA,
      final int supercellB,
      final int supercellC,
      final long imageCount,
      final long expandedAtomCount,
      final double densityAtomsPerAngstrom3,
      final double minimumImageCutoffAngstrom,
      final boolean finiteMoleculeFitsCell,
      final PeriodicKPointSamplingResult kPointSamplingResult) {
    this.model = model;
    this.cell = cell;
    this.supercellA = supercellA;
    this.supercellB = supercellB;
    this.supercellC = supercellC;
    this.imageCount = imageCount;
    this.expandedAtomCount = expandedAtomCount;
    this.densityAtomsPerAngstrom3 = densityAtomsPerAngstrom3;
    this.minimumImageCutoffAngstrom = minimumImageCutoffAngstrom;
    this.finiteMoleculeFitsCell = finiteMoleculeFitsCell;
    this.kPointSamplingResult = kPointSamplingResult;
  }

  public static PeriodicBoundaryResult of(
      final PeriodicBoundaryModel model,
      final PeriodicCell cell,
      final int supercellA,
      final int supercellB,
      final int supercellC,
      final long expandedAtomCount,
      final double densityAtomsPerAngstrom3,
      final double minimumImageCutoffAngstrom,
      final boolean finiteMoleculeFitsCell,
      final PeriodicKPointSamplingResult kPointSamplingResult) {
    if (model == null) {
      throw new IllegalArgumentException("Periodic boundary model must not be null.");
    }
    if (cell == null) {
      throw new IllegalArgumentException("Periodic cell must not be null.");
    }
    PeriodicBoundaryResult.requirePositive(supercellA, "Periodic supercell a");
    PeriodicBoundaryResult.requirePositive(supercellB, "Periodic supercell b");
    PeriodicBoundaryResult.requirePositive(supercellC, "Periodic supercell c");
    if (expandedAtomCount <= 0L) {
      throw new IllegalArgumentException("Periodic expanded atom count must be positive.");
    }
    if (!Double.isFinite(densityAtomsPerAngstrom3) || densityAtomsPerAngstrom3 <= 0.0) {
      throw new IllegalArgumentException("Periodic density must be positive and finite.");
    }
    if (!Double.isFinite(minimumImageCutoffAngstrom) || minimumImageCutoffAngstrom <= 0.0) {
      throw new IllegalArgumentException(
          "Periodic minimum image cutoff must be positive and finite.");
    }
    if (kPointSamplingResult == null) {
      throw new IllegalArgumentException("Periodic k-point sampling result must not be null.");
    }
    return new PeriodicBoundaryResult(
        model,
        cell,
        supercellA,
        supercellB,
        supercellC,
        (long) supercellA * (long) supercellB * (long) supercellC,
        expandedAtomCount,
        densityAtomsPerAngstrom3,
        minimumImageCutoffAngstrom,
        finiteMoleculeFitsCell,
        kPointSamplingResult);
  }

  public PeriodicBoundaryModel model() {
    return this.model;
  }

  public PeriodicCell cell() {
    return this.cell;
  }

  public int supercellA() {
    return this.supercellA;
  }

  public int supercellB() {
    return this.supercellB;
  }

  public int supercellC() {
    return this.supercellC;
  }

  public long imageCount() {
    return this.imageCount;
  }

  public long expandedAtomCount() {
    return this.expandedAtomCount;
  }

  public double densityAtomsPerAngstrom3() {
    return this.densityAtomsPerAngstrom3;
  }

  public double minimumImageCutoffAngstrom() {
    return this.minimumImageCutoffAngstrom;
  }

  public boolean finiteMoleculeFitsCell() {
    return this.finiteMoleculeFitsCell;
  }

  public PeriodicKPointSamplingResult kPointSamplingResult() {
    return this.kPointSamplingResult;
  }

  private static void requirePositive(final int value, final String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive.");
    }
  }
}
