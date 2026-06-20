/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.render;

import java.util.List;

/**
 * Готовый план отрисовки, который не зависит от JavaFX, Swing или web canvas.
 */
public final class DrawingRenderPlan {

  private final List<RenderPrimitive> primitives;

  private DrawingRenderPlan(final List<RenderPrimitive> primitives) {
    this.primitives = primitives;
  }

  public static DrawingRenderPlan of(final List<RenderPrimitive> primitives) {
    return new DrawingRenderPlan(primitives == null ? List.of() : List.copyOf(primitives));
  }

  public List<RenderPrimitive> primitives() {
    return this.primitives;
  }

  public int primitiveCount() {
    return this.primitives.size();
  }
}