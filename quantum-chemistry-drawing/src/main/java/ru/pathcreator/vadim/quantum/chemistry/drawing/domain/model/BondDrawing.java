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

/**
 * Визуальное отображение связи между атомами.
 */
public final class BondDrawing {

  private final AtomId firstAtomId;
  private final AtomId secondAtomId;
  private final BondDrawingStyle style;

  private BondDrawing(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final BondDrawingStyle style
  ) {
    this.firstAtomId = firstAtomId;
    this.secondAtomId = secondAtomId;
    this.style = style;
  }

  public static BondDrawing of(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final BondDrawingStyle style
  ) {
    if (
        firstAtomId == null
        || secondAtomId == null
    ) {
      throw new IllegalArgumentException("Bond drawing atom ids must not be null.");
    }
    if (firstAtomId.equals(secondAtomId)) {
      throw new IllegalArgumentException("Bond drawing must reference different atoms.");
    }
    return new BondDrawing(
        firstAtomId,
        secondAtomId,
        style == null ? BondDrawingStyle.PLAIN : style
    );
  }

  public AtomId firstAtomId() {
    return this.firstAtomId;
  }

  public AtomId secondAtomId() {
    return this.secondAtomId;
  }

  public BondDrawingStyle style() {
    return this.style;
  }

  public boolean connects(
      final AtomId first,
      final AtomId second
  ) {
    return this.firstAtomId.equals(first) && this.secondAtomId.equals(second)
        || this.firstAtomId.equals(second) && this.secondAtomId.equals(first);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BondDrawing)) {
      return false;
    }
    final BondDrawing drawing = (BondDrawing) other;
    return Objects.equals(this.firstAtomId, drawing.firstAtomId)
        && Objects.equals(this.secondAtomId, drawing.secondAtomId)
        && this.style == drawing.style;
  }

  public int hashCode() {
    int result = this.firstAtomId.hashCode();
    result = 31 * result + this.secondAtomId.hashCode();
    result = 31 * result + this.style.hashCode();
    return result;
  }
}