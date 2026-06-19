/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.property;

/** Вектор дипольного момента с конвертацией magnitude в Debye. */
public final class DipoleMomentVector {

  public static final double ELECTRON_ANGSTROM_TO_DEBYE = 4.80320471257;
  private final double x;
  private final double y;
  private final double z;
  private final DipoleMomentUnit unit;

  private DipoleMomentVector(
      final double x, final double y, final double z, final DipoleMomentUnit unit) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.unit = unit;
  }

  public static DipoleMomentVector of(
      final double x, final double y, final double z, final DipoleMomentUnit unit) {
    if (!(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z))) {
      throw new IllegalArgumentException("Dipole moment components must be finite.");
    }
    if (unit == null) {
      throw new IllegalArgumentException("Dipole moment unit must not be null.");
    }
    return new DipoleMomentVector(x, y, z, unit);
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

  public DipoleMomentUnit unit() {
    return this.unit;
  }

  public double magnitude() {
    return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
  }

  public double magnitudeDebye() {
    if (this.unit == DipoleMomentUnit.DEBYE) {
      return this.magnitude();
    }
    if (this.unit == DipoleMomentUnit.ELECTRON_ANGSTROM) {
      return this.magnitude() * 4.80320471257;
    }
    throw new IllegalStateException("Unsupported dipole moment unit.");
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DipoleMomentVector)) {
      return false;
    }
    final DipoleMomentVector vector = (DipoleMomentVector) other;
    return Double.compare(this.x, vector.x) == 0
        && Double.compare(this.y, vector.y) == 0
        && Double.compare(this.z, vector.z) == 0
        && this.unit == vector.unit;
  }

  public int hashCode() {
    int result = Double.hashCode(this.x);
    result = 31 * result + Double.hashCode(this.y);
    result = 31 * result + Double.hashCode(this.z);
    result = 31 * result + this.unit.hashCode();
    return result;
  }
}