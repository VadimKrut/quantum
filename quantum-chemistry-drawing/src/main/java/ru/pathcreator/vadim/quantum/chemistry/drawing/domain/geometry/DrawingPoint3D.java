/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry;

import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;

/**
 * Точка трехмерной сцены, из которой можно получать координату химического ядра.
 */
public final class DrawingPoint3D {

  public static final DrawingPoint3D ORIGIN = new DrawingPoint3D(0.0, 0.0, 0.0);
  private final double x;
  private final double y;
  private final double z;

  private DrawingPoint3D(
      final double x,
      final double y,
      final double z
  ) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public static DrawingPoint3D of(
      final double x,
      final double y,
      final double z
  ) {
    if (
        !Double.isFinite(x)
        || !Double.isFinite(y)
        || !Double.isFinite(z)
    ) {
      throw new IllegalArgumentException("Drawing 3D point coordinates must be finite.");
    }
    return new DrawingPoint3D(
        x,
        y,
        z
    );
  }

  public static DrawingPoint3D fromCoordinate(final Coordinate3D coordinate) {
    if (coordinate == null) {
      return ORIGIN;
    }
    return DrawingPoint3D.of(
        coordinate.x(),
        coordinate.y(),
        coordinate.z()
    );
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

  public Coordinate3D toCoordinate(final LengthUnit unit) {
    return Coordinate3D.of(
        this.x,
        this.y,
        this.z,
        unit
    );
  }

  public DrawingPoint3D translate(
      final double deltaX,
      final double deltaY,
      final double deltaZ
  ) {
    return DrawingPoint3D.of(
        this.x + deltaX,
        this.y + deltaY,
        this.z + deltaZ
    );
  }

  public DrawingPoint3D rotate(
      final double yawRadians,
      final double pitchRadians,
      final double rollRadians
  ) {
    DrawingPoint3D rotated = this.rotateAroundZ(yawRadians);
    rotated = rotated.rotateAroundX(pitchRadians);
    return rotated.rotateAroundY(rollRadians);
  }

  private DrawingPoint3D rotateAroundX(final double angleRadians) {
    final double sin = Math.sin(angleRadians);
    final double cos = Math.cos(angleRadians);
    return DrawingPoint3D.of(
        this.x,
        this.y * cos - this.z * sin,
        this.y * sin + this.z * cos
    );
  }

  private DrawingPoint3D rotateAroundY(final double angleRadians) {
    final double sin = Math.sin(angleRadians);
    final double cos = Math.cos(angleRadians);
    return DrawingPoint3D.of(
        this.x * cos + this.z * sin,
        this.y,
        -this.x * sin + this.z * cos
    );
  }

  private DrawingPoint3D rotateAroundZ(final double angleRadians) {
    final double sin = Math.sin(angleRadians);
    final double cos = Math.cos(angleRadians);
    return DrawingPoint3D.of(
        this.x * cos - this.y * sin,
        this.x * sin + this.y * cos,
        this.z
    );
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DrawingPoint3D)) {
      return false;
    }
    final DrawingPoint3D point = (DrawingPoint3D) other;
    return Double.compare(this.x, point.x) == 0
        && Double.compare(this.y, point.y) == 0
        && Double.compare(this.z, point.z) == 0;
  }

  public int hashCode() {
    int result = Double.hashCode(this.x);
    result = 31 * result + Double.hashCode(this.y);
    result = 31 * result + Double.hashCode(this.z);
    return result;
  }
}