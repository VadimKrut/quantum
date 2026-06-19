/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

import java.util.Objects;

public final class ConvergenceCriteria {

  public static final ConvergenceCriteria DEFAULT = new ConvergenceCriteria(1.0E-8, 1.0E-8, 100);
  private final double energyTolerance;
  private final double densityTolerance;
  private final int maxIterations;

  private ConvergenceCriteria(
      final double energyTolerance,
      final double densityTolerance,
      final int maxIterations
  ) {
    this.energyTolerance = energyTolerance;
    this.densityTolerance = densityTolerance;
    this.maxIterations = maxIterations;
  }

  public static ConvergenceCriteria of(
      final double energyTolerance, final double densityTolerance, final int maxIterations) {
    ConvergenceCriteria.requirePositiveFinite(energyTolerance, "Energy tolerance");
    ConvergenceCriteria.requirePositiveFinite(densityTolerance, "Density tolerance");
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("Max iteration count must be positive.");
    }
    return new ConvergenceCriteria(energyTolerance, densityTolerance, maxIterations);
  }

  public double energyTolerance() {
    return this.energyTolerance;
  }

  public double densityTolerance() {
    return this.densityTolerance;
  }

  public int maxIterations() {
    return this.maxIterations;
  }

  private static void requirePositiveFinite(
      final double value,
      final String subjectName
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subjectName + " must be finite.");
    }
    if (value <= 0.0) {
      throw new IllegalArgumentException(subjectName + " must be positive.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ConvergenceCriteria)) {
      return false;
    }
    final ConvergenceCriteria criteria = (ConvergenceCriteria) other;
    return Double.compare(this.energyTolerance, criteria.energyTolerance) == 0
        && Double.compare(this.densityTolerance, criteria.densityTolerance) == 0
        && this.maxIterations == criteria.maxIterations;
  }

  public int hashCode() {
    return Objects.hash(this.energyTolerance, this.densityTolerance, this.maxIterations);
  }
}