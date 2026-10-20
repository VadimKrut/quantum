/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf;

/**
 * Options for restricted Hartree-Fock iterations.
 */
public final class RhfOptions {

  public static final RhfOptions DEFAULT = RhfOptions.of(128, 1.0E-10, 1.0E-8, 0.35);

  private final int maxIterations;
  private final double convergenceTolerance;
  private final double densityTolerance;
  private final double densityMixing;

  private RhfOptions(
      final int maxIterations,
      final double convergenceTolerance,
      final double densityTolerance,
      final double densityMixing) {
    this.maxIterations = maxIterations;
    this.convergenceTolerance = convergenceTolerance;
    this.densityTolerance = densityTolerance;
    this.densityMixing = densityMixing;
  }

  public static RhfOptions of(
      final int maxIterations, final double convergenceTolerance, final double densityMixing) {
    return RhfOptions.of(
        maxIterations,
        convergenceTolerance,
        Math.max(Math.sqrt(convergenceTolerance), convergenceTolerance),
        densityMixing);
  }

  public static RhfOptions of(
      final int maxIterations,
      final double convergenceTolerance,
      final double densityTolerance,
      final double densityMixing) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("RHF max iterations must be positive.");
    }
    if (!Double.isFinite(convergenceTolerance) || convergenceTolerance <= 0.0) {
      throw new IllegalArgumentException("RHF convergence tolerance must be finite and positive.");
    }
    if (!Double.isFinite(densityTolerance) || densityTolerance <= 0.0) {
      throw new IllegalArgumentException("RHF density tolerance must be finite and positive.");
    }
    if (!Double.isFinite(densityMixing) || densityMixing < 0.0 || densityMixing > 1.0) {
      throw new IllegalArgumentException("RHF density mixing must be in [0, 1].");
    }
    return new RhfOptions(maxIterations, convergenceTolerance, densityTolerance, densityMixing);
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
