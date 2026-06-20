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

/**
 * Один выбранный объект с типом и устойчивым идентификатором.
 */
public final class DrawingSelectionItem {

  private final DrawingSelectionKind kind;
  private final String id;

  private DrawingSelectionItem(
      final DrawingSelectionKind kind,
      final String id
  ) {
    this.kind = kind;
    this.id = id;
  }

  public static DrawingSelectionItem of(
      final DrawingSelectionKind kind,
      final String id
  ) {
    if (kind == null) {
      throw new IllegalArgumentException("Drawing selection kind must not be null.");
    }
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("Drawing selection id must not be blank.");
    }
    return new DrawingSelectionItem(
        kind,
        id.trim()
    );
  }

  public DrawingSelectionKind kind() {
    return this.kind;
  }

  public String id() {
    return this.id;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DrawingSelectionItem)) {
      return false;
    }
    final DrawingSelectionItem item = (DrawingSelectionItem) other;
    return this.kind == item.kind
        && Objects.equals(this.id, item.id);
  }

  public int hashCode() {
    int result = this.kind.hashCode();
    result = 31 * result + this.id.hashCode();
    return result;
  }
}