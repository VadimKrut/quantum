/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft;

/** Параметры self-consistent Kohn-Sham DFT итераций. */
public final class KohnShamDftOptions {

  public static final KohnShamDftOptions DEFAULT =
      KohnShamDftOptions.of(160, 1.0E-10, 1.0E-8, 0.35);

  private final int maxIterations;
  private final double convergenceTolerance;
  private final double densityTolerance;
  private final double densityMixing;

  private KohnShamDftOptions(
      final int maxIterations,
      final double convergenceTolerance,
      final double densityTolerance,
      final double densityMixing) {
    this.maxIterations = maxIterations;
    this.convergenceTolerance = convergenceTolerance;
    this.densityTolerance = densityTolerance;
    this.densityMixing = densityMixing;
  }

  public static KohnShamDftOptions of(
      final int maxIterations, final double convergenceTolerance, final double densityMixing) {
    return KohnShamDftOptions.of(
        maxIterations,
        convergenceTolerance,
        Math.max(Math.sqrt(convergenceTolerance), convergenceTolerance),
        densityMixing);
  }

  public static KohnShamDftOptions of(
      final int maxIterations,
      final double convergenceTolerance,
      final double densityTolerance,
      final double densityMixing) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("Kohn-Sham DFT max iterations must be positive.");
    }
    if (!Double.isFinite(convergenceTolerance) || convergenceTolerance <= 0.0) {
      throw new IllegalArgumentException(
          "Kohn-Sham DFT convergence tolerance must be finite and positive.");
    }
    if (!Double.isFinite(densityTolerance) || densityTolerance <= 0.0) {
      throw new IllegalArgumentException(
          "Kohn-Sham DFT density tolerance must be finite and positive.");
    }
    if (!Double.isFinite(densityMixing) || densityMixing < 0.0 || densityMixing > 1.0) {
      throw new IllegalArgumentException("Kohn-Sham DFT density mixing must be in [0, 1].");
    }
    return new KohnShamDftOptions(
        maxIterations, convergenceTolerance, densityTolerance, densityMixing);
  }

  public int maxIterations() {
    return this.maxIterations;
  }

  public double convergenceTolerance() {
    return this.convergenceTolerance;
  }

  public double densityTolerance() {
    return this.densityTolerance;
  }

  public double densityMixing() {
    return this.densityMixing;
  }
}
