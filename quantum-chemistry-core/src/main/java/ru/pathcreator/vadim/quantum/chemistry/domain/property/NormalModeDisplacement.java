/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.property;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

public final class NormalModeDisplacement {

  private final AtomId atomId;
  private final double dx;
  private final double dy;
  private final double dz;

  private NormalModeDisplacement(
      final AtomId atomId,
      final double dx,
      final double dy,
      final double dz
  ) {
    this.atomId = atomId;
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
  }

  public static NormalModeDisplacement of(
      final AtomId atomId,
      final double dx,
      final double dy,
      final double dz
  ) {
    if (atomId == null) {
      throw new IllegalArgumentException("Normal mode displacement atom id must not be null.");
    }
    if (!(Double.isFinite(dx) && Double.isFinite(dy) && Double.isFinite(dz))) {
      throw new IllegalArgumentException("Normal mode displacement components must be finite.");
    }
    if (Double.compare(dx, 0.0) == 0
        && Double.compare(dy, 0.0) == 0
        && Double.compare(dz, 0.0) == 0) {
      throw new IllegalArgumentException("Normal mode displacement vector must not be zero.");
    }
    return new NormalModeDisplacement(atomId, dx, dy, dz);
  }

  public AtomId atomId() {
    return this.atomId;
  }

  public double dx() {
    return this.dx;
  }

  public double dy() {
    return this.dy;
  }

  public double dz() {
    return this.dz;
  }

  public double magnitude() {
    return Math.sqrt(this.dx * this.dx + this.dy * this.dy + this.dz * this.dz);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof NormalModeDisplacement)) {
      return false;
    }
    final NormalModeDisplacement displacement = (NormalModeDisplacement) other;
    return Double.compare(this.dx, displacement.dx) == 0
        && Double.compare(this.dy, displacement.dy) == 0
        && Double.compare(this.dz, displacement.dz) == 0
        && Objects.equals(this.atomId, displacement.atomId);
  }

  public int hashCode() {
    return Objects.hash(this.atomId, this.dx, this.dy, this.dz);
  }
}