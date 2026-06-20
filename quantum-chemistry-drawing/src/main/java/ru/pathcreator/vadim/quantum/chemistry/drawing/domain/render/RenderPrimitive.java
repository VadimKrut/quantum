/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.render;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.DrawingPoint2D;

/**
 * Один UI-независимый render primitive с координатами и текстовой payload.
 */
public final class RenderPrimitive {

  private final RenderPrimitiveKind kind;
  private final String id;
  private final DrawingPoint2D start;
  private final DrawingPoint2D end;
  private final String text;

  private RenderPrimitive(
      final RenderPrimitiveKind kind,
      final String id,
      final DrawingPoint2D start,
      final DrawingPoint2D end,
      final String text
  ) {
    this.kind = kind;
    this.id = id;
    this.start = start;
    this.end = end;
    this.text = text;
  }

  public static RenderPrimitive of(
      final RenderPrimitiveKind kind,
      final String id,
      final DrawingPoint2D start,
      final DrawingPoint2D end,
      final String text
  ) {
    if (kind == null) {
      throw new IllegalArgumentException("Render primitive kind must not be null.");
    }
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("Render primitive id must not be blank.");
    }
    return new RenderPrimitive(
        kind,
        id.trim(),
        start == null ? DrawingPoint2D.ORIGIN : start,
        end,
        text == null ? "" : text
    );
  }

  public RenderPrimitiveKind kind() {
    return this.kind;
  }

  public String id() {
    return this.id;
  }

  public DrawingPoint2D start() {
    return this.start;
  }

  public DrawingPoint2D end() {
    return this.end;
  }

  public String text() {
    return this.text;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RenderPrimitive)) {
      return false;
    }
    final RenderPrimitive primitive = (RenderPrimitive) other;
    return this.kind == primitive.kind
        && Objects.equals(this.id, primitive.id)
        && Objects.equals(this.start, primitive.start)
        && Objects.equals(this.end, primitive.end)
        && Objects.equals(this.text, primitive.text);
  }

  public int hashCode() {
    int result = this.kind.hashCode();
    result = 31 * result + this.id.hashCode();
    result = 31 * result + this.start.hashCode();
    result = 31 * result + (this.end == null ? 0 : this.end.hashCode());
    result = 31 * result + this.text.hashCode();
    return result;
  }
}