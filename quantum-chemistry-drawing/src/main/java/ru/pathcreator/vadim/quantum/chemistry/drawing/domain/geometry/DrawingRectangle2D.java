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
 * Прямоугольная область выбора или viewport на двумерной химической сцене.
 */
public final class DrawingRectangle2D {

  private final double minX;
  private final double minY;
  private final double maxX;
  private final double maxY;

  private DrawingRectangle2D(
      final double minX,
      final double minY,
      final double maxX,
      final double maxY
  ) {
    this.minX = minX;
    this.minY = minY;
    this.maxX = maxX;
    this.maxY = maxY;
  }

  public static DrawingRectangle2D fromCorners(
      final DrawingPoint2D first,
      final DrawingPoint2D second
  ) {
    if (
        first == null
        || second == null
    ) {
      throw new IllegalArgumentException("Rectangle points must not be null.");
    }
    return new DrawingRectangle2D(
        Math.min(first.x(), second.x()),
        Math.min(first.y(), second.y()),
        Math.max(first.x(), second.x()),
        Math.max(first.y(), second.y())
    );
  }

  public double minX() {
    return this.minX;
  }

  public double minY() {
    return this.minY;
  }

  public double maxX() {
    return this.maxX;
  }

  public double maxY() {
    return this.maxY;
  }

  public boolean contains(final DrawingPoint2D point) {
    if (point == null) {
      return false;
    }
    return point.x() >= this.minX
        && point.x() <= this.maxX
        && point.y() >= this.minY
        && point.y() <= this.maxY;
  }
}