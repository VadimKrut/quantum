/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.posthf.domain.ci;

public final class CisdResult {

  private final int determinantCount;
  private final double electronicEnergy;
  private final double totalEnergy;

  private CisdResult(
      final int determinantCount, final double electronicEnergy, final double totalEnergy) {
    this.determinantCount = determinantCount;
    this.electronicEnergy = electronicEnergy;
    this.totalEnergy = totalEnergy;
  }

  public static CisdResult of(
      final int determinantCount, final double electronicEnergy, final double totalEnergy) {
    if (determinantCount <= 0) {
      throw new IllegalArgumentException("CISD determinant count must be positive.");
    }
    if (!Double.isFinite(electronicEnergy) || !Double.isFinite(totalEnergy)) {
      throw new IllegalArgumentException("CISD energies must be finite.");
    }
    return new CisdResult(determinantCount, electronicEnergy, totalEnergy);
  }

  public int determinantCount() {
    return this.determinantCount;
  }

  public double electronicEnergy() {
    return this.electronicEnergy;
  }

  public double totalEnergy() {
    return this.totalEnergy;
  }
}
