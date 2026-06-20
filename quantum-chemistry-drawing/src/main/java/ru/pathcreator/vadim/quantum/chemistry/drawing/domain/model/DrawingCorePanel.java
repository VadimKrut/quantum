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
 * Полноценная inspector/form-панель для одной области chemistry-core.
 */
public final class DrawingCorePanel {

  private final DrawingCoreArea area;
  private final DrawingCoreReference owner;
  private final String title;
  private final List<DrawingStructuredField> fields;

  private DrawingCorePanel(
      final DrawingCoreArea area,
      final DrawingCoreReference owner,
      final String title,
      final List<DrawingStructuredField> fields
  ) {
    this.area = area;
    this.owner = owner;
    this.title = title;
    this.fields = fields;
  }

  public static DrawingCorePanel of(
      final DrawingCoreArea area,
      final DrawingCoreReference owner,
      final String title,
      final List<DrawingStructuredField> fields
  ) {
    if (area == null) {
      throw new IllegalArgumentException("Drawing core panel area must not be null.");
    }
    if (owner == null) {
      throw new IllegalArgumentException("Drawing core panel owner must not be null.");
    }
    return new DrawingCorePanel(
        area,
        owner,
        DrawingText.require(
            title,
            "Drawing core panel title"
        ),
        fields == null ? List.of() : List.copyOf(fields)
    );
  }

  public DrawingCoreArea area() {
    return this.area;
  }

  public DrawingCoreReference owner() {
    return this.owner;
  }

  public String title() {
    return this.title;
  }

  public List<DrawingStructuredField> fields() {
    return this.fields;
  }

  public boolean hasMissingRequiredField() {
    for (int i = 0; i < this.fields.size(); ++i) {
      if (this.fields.get(i).missingRequiredValue()) {
        return true;
      }
    }
    return false;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DrawingCorePanel)) {
      return false;
    }
    final DrawingCorePanel panel = (DrawingCorePanel) other;
    return this.area == panel.area
        && Objects.equals(this.owner, panel.owner)
        && Objects.equals(this.title, panel.title)
        && Objects.equals(this.fields, panel.fields);
  }

  public int hashCode() {
    int result = this.area.hashCode();
    result = 31 * result + this.owner.hashCode();
    result = 31 * result + this.title.hashCode();
    result = 31 * result + this.fields.hashCode();
    return result;
  }
}