/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Стабильная выборка объектов, которую UI может использовать для inspector, delete, move и context menu.
 */
public final class DrawingSelection {

  public static final DrawingSelection EMPTY = new DrawingSelection(List.of());
  private final List<DrawingSelectionItem> items;

  private DrawingSelection(final List<DrawingSelectionItem> items) {
    this.items = items;
  }

  public static DrawingSelection of(final List<DrawingSelectionItem> items) {
    if (items == null || items.isEmpty()) {
      return EMPTY;
    }
    return new DrawingSelection(List.copyOf(items));
  }

  public List<DrawingSelectionItem> items() {
    return this.items;
  }

  public boolean empty() {
    return this.items.isEmpty();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DrawingSelection)) {
      return false;
    }
    final DrawingSelection selection = (DrawingSelection) other;
    return Objects.equals(this.items, selection.items);
  }

  public int hashCode() {
    return this.items.hashCode();
  }
}