/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc;

/**
 * Настройки итерационного closed-shell coupled-cluster doubles расчета.
 */
public final class CoupledClusterDoublesOptions {

  public static final CoupledClusterDoublesOptions DEFAULT =
      CoupledClusterDoublesOptions.of(96, 1.0E-9, 0.35, 1.0E-12, true);

  private final int maxIterations;
  private final double energyTolerance;
  private final double damping;
  private final double denominatorShift;
  private final boolean startFromMp2;

  private CoupledClusterDoublesOptions(
      final int maxIterations,
      final double energyTolerance,
      final double damping,
      final double denominatorShift,
      final boolean startFromMp2) {
    this.maxIterations = maxIterations;
    this.energyTolerance = energyTolerance;
    this.damping = damping;
    this.denominatorShift = denominatorShift;
    this.startFromMp2 = startFromMp2;
  }

  public static CoupledClusterDoublesOptions of(
      final int maxIterations,
      final double energyTolerance,
      final double damping,
      final double denominatorShift,
      final boolean startFromMp2) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("CCD max iterations must be positive.");
    }
    if (!Double.isFinite(energyTolerance) || energyTolerance <= 0.0) {
      throw new IllegalArgumentException("CCD energy tolerance must be finite and positive.");
    }
    if (!Double.isFinite(damping) || damping < 0.0 || damping > 1.0) {
      throw new IllegalArgumentException("CCD damping must be finite and in [0, 1].");
    }
    if (!Double.isFinite(denominatorShift) || denominatorShift < 0.0) {
      throw new IllegalArgumentException("CCD denominator shift must be finite and non-negative.");
    }
    return new CoupledClusterDoublesOptions(
        maxIterations, energyTolerance, damping, denominatorShift, startFromMp2);
  }

  public int maxIterations() {
    return this.maxIterations;
  }

  public double energyTolerance() {
    return this.energyTolerance;
  }

  public double damping() {
    return this.damping;
  }

  public double denominatorShift() {
    return this.denominatorShift;
  }

  public boolean startFromMp2() {
    return this.startFromMp2;
  }
}
