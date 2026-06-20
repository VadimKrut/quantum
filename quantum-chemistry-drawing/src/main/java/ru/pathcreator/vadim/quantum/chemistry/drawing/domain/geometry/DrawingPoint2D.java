/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry;

/**
 * Точка двумерного химического canvas в логических единицах редактора.
 */
public final class DrawingPoint2D {

  public static final DrawingPoint2D ORIGIN = new DrawingPoint2D(0.0, 0.0);
  private final double x;
  private final double y;

  private DrawingPoint2D(
      final double x,
      final double y
  ) {
    this.x = x;
    this.y = y;
  }

  public static DrawingPoint2D of(
      final double x,
      final double y
  ) {
    if (
        !Double.isFinite(x)
        || !Double.isFinite(y)
    ) {
      throw new IllegalArgumentException("Drawing 2D point coordinates must be finite.");
    }
    return new DrawingPoint2D(
        x,
        y
    );
  }

  public double x() {
    return this.x;
  }

  public double y() {
    return this.y;
  }

  public DrawingPoint2D translate(
      final double deltaX,
      final double deltaY
  ) {
    return DrawingPoint2D.of(
        this.x + deltaX,
        this.y + deltaY
    );
  }

  public DrawingPoint2D rotateAround(
      final DrawingPoint2D center,
      final double angleRadians
  ) {
    if (center == null) {
      throw new IllegalArgumentException("Rotation center must not be null.");
    }
    if (!Double.isFinite(angleRadians)) {
      throw new IllegalArgumentException("Rotation angle must be finite.");
    }
    final double sin = Math.sin(angleRadians);
    final double cos = Math.cos(angleRadians);
    final double localX = this.x - center.x;
    final double localY = this.y - center.y;
    return DrawingPoint2D.of(
        center.x + localX * cos - localY * sin,
        center.y + localX * sin + localY * cos
    );
  }

  public DrawingPoint2D mirrorX(final double axisX) {
    if (!Double.isFinite(axisX)) {
      throw new IllegalArgumentException("Mirror axis must be finite.");
    }
    return DrawingPoint2D.of(
        axisX + axisX - this.x,
        this.y
    );
  }

  public DrawingPoint2D mirrorY(final double axisY) {
    if (!Double.isFinite(axisY)) {
      throw new IllegalArgumentException("Mirror axis must be finite.");
    }
    return DrawingPoint2D.of(
        this.x,
        axisY + axisY - this.y
    );
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DrawingPoint2D)) {
      return false;
    }
    final DrawingPoint2D point = (DrawingPoint2D) other;
    return Double.compare(this.x, point.x) == 0
        && Double.compare(this.y, point.y) == 0;
  }

  public int hashCode() {
    int result = Double.hashCode(this.x);
    result = 31 * result + Double.hashCode(this.y);
    return result;
  }
}