/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Описывает Cartesian-степени Gaussian orbital для s/p/d/f/g shells.
 */
public final class GaussianAngularMomentum {

  public static final GaussianAngularMomentum S = new GaussianAngularMomentum(0, 0, 0);
  public static final GaussianAngularMomentum PX = new GaussianAngularMomentum(1, 0, 0);
  public static final GaussianAngularMomentum PY = new GaussianAngularMomentum(0, 1, 0);
  public static final GaussianAngularMomentum PZ = new GaussianAngularMomentum(0, 0, 1);
  public static final GaussianAngularMomentum DXX = new GaussianAngularMomentum(2, 0, 0);
  public static final GaussianAngularMomentum DYY = new GaussianAngularMomentum(0, 2, 0);
  public static final GaussianAngularMomentum DZZ = new GaussianAngularMomentum(0, 0, 2);
  public static final GaussianAngularMomentum DXY = new GaussianAngularMomentum(1, 1, 0);
  public static final GaussianAngularMomentum DXZ = new GaussianAngularMomentum(1, 0, 1);
  public static final GaussianAngularMomentum DYZ = new GaussianAngularMomentum(0, 1, 1);

  private final int xPower;
  private final int yPower;
  private final int zPower;

  private GaussianAngularMomentum(final int xPower, final int yPower, final int zPower) {
    this.xPower = xPower;
    this.yPower = yPower;
    this.zPower = zPower;
  }

  public static GaussianAngularMomentum of(final int xPower, final int yPower, final int zPower) {
    if (xPower < 0 || yPower < 0 || zPower < 0) {
      throw new IllegalArgumentException("Gaussian angular powers must not be negative.");
    }
    return new GaussianAngularMomentum(xPower, yPower, zPower);
  }

  public static List<GaussianAngularMomentum> cartesianShell(final GaussianShellType shellType) {
    if (shellType == null) {
      throw new IllegalArgumentException("Gaussian shell type must not be null.");
    }
    final int totalPower = shellType.angularMomentum();
    final ArrayList<GaussianAngularMomentum> momenta =
        new ArrayList<GaussianAngularMomentum>(shellType.cartesianFunctionCount());
    for (int xPower = totalPower; xPower >= 0; --xPower) {
      for (int yPower = totalPower - xPower; yPower >= 0; --yPower) {
        final int zPower = totalPower - xPower - yPower;
        momenta.add(GaussianAngularMomentum.of(xPower, yPower, zPower));
      }
    }
    return List.copyOf(momenta);
  }

  public int xPower() {
    return this.xPower;
  }

  public int yPower() {
    return this.yPower;
  }

  public int zPower() {
    return this.zPower;
  }

  public int totalPower() {
    return this.xPower + this.yPower + this.zPower;
  }

  public boolean sType() {
    return this.xPower == 0 && this.yPower == 0 && this.zPower == 0;
  }

  public GaussianShellType shellType() {
    return GaussianShellType.ofAngularMomentum(this.totalPower());
  }

  public String label() {
    return this.shellType().symbol()
        + "("
        + this.xPower
        + ","
        + this.yPower
        + ","
        + this.zPower
        + ")";
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof GaussianAngularMomentum)) {
      return false;
    }
    final GaussianAngularMomentum momentum = (GaussianAngularMomentum) other;
    return this.xPower == momentum.xPower
        && this.yPower == momentum.yPower
        && this.zPower == momentum.zPower;
  }

  public int hashCode() {
    return Objects.hash(this.xPower, this.yPower, this.zPower);
  }
}
