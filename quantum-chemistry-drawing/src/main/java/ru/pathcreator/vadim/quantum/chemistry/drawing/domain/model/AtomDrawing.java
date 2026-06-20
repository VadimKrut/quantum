/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint3D;

/**
 * Визуальное размещение атома одновременно в 2D-canvas и 3D-сцене.
 */
public final class AtomDrawing {

  private final AtomId atomId;
  private final DrawingPoint2D point2D;
  private final DrawingPoint3D point3D;
  private final boolean labelVisible;

  private AtomDrawing(
      final AtomId atomId,
      final DrawingPoint2D point2D,
      final DrawingPoint3D point3D,
      final boolean labelVisible
  ) {
    this.atomId = atomId;
    this.point2D = point2D;
    this.point3D = point3D;
    this.labelVisible = labelVisible;
  }

  public static AtomDrawing of(
      final AtomId atomId,
      final DrawingPoint2D point2D,
      final DrawingPoint3D point3D,
      final boolean labelVisible
  ) {
    if (atomId == null) {
      throw new IllegalArgumentException("Atom drawing atom id must not be null.");
    }
    return new AtomDrawing(
        atomId,
        point2D == null ? DrawingPoint2D.ORIGIN : point2D,
        point3D == null ? DrawingPoint3D.ORIGIN : point3D,
        labelVisible
    );
  }

  public AtomId atomId() {
    return this.atomId;
  }

  public DrawingPoint2D point2D() {
    return this.point2D;
  }

  public DrawingPoint3D point3D() {
    return this.point3D;
  }

  public boolean labelVisible() {
    return this.labelVisible;
  }

  public AtomDrawing withPoint2D(final DrawingPoint2D point) {
    return AtomDrawing.of(
        this.atomId,
        point,
        this.point3D,
        this.labelVisible
    );
  }

  public AtomDrawing withPoint3D(final DrawingPoint3D point) {
    return AtomDrawing.of(
        this.atomId,
        this.point2D,
        point,
        this.labelVisible
    );
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AtomDrawing)) {
      return false;
    }
    final AtomDrawing drawing = (AtomDrawing) other;
    return this.labelVisible == drawing.labelVisible
        && Objects.equals(this.atomId, drawing.atomId)
        && Objects.equals(this.point2D, drawing.point2D)
        && Objects.equals(this.point3D, drawing.point3D);
  }

  public int hashCode() {
    int result = this.atomId.hashCode();
    result = 31 * result + this.point2D.hashCode();
    result = 31 * result + this.point3D.hashCode();
    result = 31 * result + Boolean.hashCode(this.labelVisible);
    return result;
  }
}