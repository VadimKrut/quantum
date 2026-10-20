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
 * Настройки closed-shell CCSD расчета с явным хранением singles и doubles амплитуд.
 */
public final class CoupledClusterSinglesDoublesOptions {

  public static final CoupledClusterSinglesDoublesOptions DEFAULT =
      CoupledClusterSinglesDoublesOptions.of(96, 1.0E-9, 0.35, 1.0E-12, true);

  private final CoupledClusterDoublesOptions doublesOptions;

  private CoupledClusterSinglesDoublesOptions(final CoupledClusterDoublesOptions doublesOptions) {
    this.doublesOptions = doublesOptions;
  }

  public static CoupledClusterSinglesDoublesOptions of(
      final int maxIterations,
      final double energyTolerance,
      final double damping,
      final double denominatorShift,
      final boolean startFromMp2) {
    return new CoupledClusterSinglesDoublesOptions(
        CoupledClusterDoublesOptions.of(
            maxIterations, energyTolerance, damping, denominatorShift, startFromMp2));
  }

  public CoupledClusterDoublesOptions doublesOptions() {
    return this.doublesOptions;
  }

  public int maxIterations() {
    return this.doublesOptions.maxIterations();
  }

  public double energyTolerance() {
    return this.doublesOptions.energyTolerance();
  }

  public double damping() {
    return this.doublesOptions.damping();
  }

  public double denominatorShift() {
    return this.doublesOptions.denominatorShift();
  }

  public boolean startFromMp2() {
    return this.doublesOptions.startFromMp2();
  }
}
