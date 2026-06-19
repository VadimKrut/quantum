/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.geometry;

/** Трёхмерная координата атома в указанной единице длины. */
public final class Coordinate3D {

  private final double x;
  private final double y;
  private final double z;
  private final LengthUnit unit;

  private Coordinate3D(
      final double x,
      final double y,
      final double z,
      final LengthUnit unit
  ) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.unit = unit;
  }

  public static Coordinate3D of(
      final double x, final double y, final double z, final LengthUnit unit) {
    if (!(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z))) {
      throw new IllegalArgumentException("Coordinate values must be finite.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Length unit must not be null.");
    }
    return new Coordinate3D(x, y, z, unit);
  }

  public double x() {
    return this.x;
  }

  public double y() {
    return this.y;
  }

  public double z() {
    return this.z;
  }

  public LengthUnit unit() {
    return this.unit;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Coordinate3D)) {
      return false;
    }
    final Coordinate3D coordinate = (Coordinate3D) other;
    return Double.compare(this.x, coordinate.x) == 0
        && Double.compare(this.y, coordinate.y) == 0
        && Double.compare(this.z, coordinate.z) == 0
        && this.unit == coordinate.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(this.x);
    result = 31 * result + Double.hashCode(this.y);
    result = 31 * result + Double.hashCode(this.z);
    result = 31 * result + this.unit.hashCode();
    return result;
  }
}