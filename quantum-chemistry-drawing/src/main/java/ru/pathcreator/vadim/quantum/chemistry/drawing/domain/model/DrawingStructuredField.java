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
 * Типизированное поле ручного редактора для областей chemistry-core, которые не всегда являются canvas-жестом.
 */
public final class DrawingStructuredField {

  private final String key;
  private final String label;
  private final DrawingFieldKind kind;
  private final String value;
  private final String unit;
  private final boolean required;
  private final boolean editable;
  private final List<DrawingFieldOption> options;

  private DrawingStructuredField(
      final String key,
      final String label,
      final DrawingFieldKind kind,
      final String value,
      final String unit,
      final boolean required,
      final boolean editable,
      final List<DrawingFieldOption> options
  ) {
    this.key = key;
    this.label = label;
    this.kind = kind;
    this.value = value;
    this.unit = unit;
    this.required = required;
    this.editable = editable;
    this.options = options;
  }

  public static DrawingStructuredField of(
      final String key,
      final String label,
      final DrawingFieldKind kind,
      final String value,
      final String unit,
      final boolean required,
      final boolean editable,
      final List<DrawingFieldOption> options
  ) {
    if (kind == null) {
      throw new IllegalArgumentException("Drawing field kind must not be null.");
    }
    return new DrawingStructuredField(
        DrawingText.require(
            key,
            "Drawing structured field key"
        ),
        DrawingText.require(
            label,
            "Drawing structured field label"
        ),
        kind,
        value == null ? "" : value.trim(),
        unit == null ? "" : unit.trim(),
        required,
        editable,
        options == null ? List.of() : List.copyOf(options)
    );
  }

  public String key() {
    return this.key;
  }

  public String label() {
    return this.label;
  }

  public DrawingFieldKind kind() {
    return this.kind;
  }

  public String value() {
    return this.value;
  }

  public String unit() {
    return this.unit;
  }

  public boolean required() {
    return this.required;
  }

  public boolean editable() {
    return this.editable;
  }

  public List<DrawingFieldOption> options() {
    return this.options;
  }

  public boolean missingRequiredValue() {
    return this.required && this.value.isEmpty();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DrawingStructuredField)) {
      return false;
    }
    final DrawingStructuredField field = (DrawingStructuredField) other;
    return this.required == field.required
        && this.editable == field.editable
        && Objects.equals(this.key, field.key)
        && Objects.equals(this.label, field.label)
        && this.kind == field.kind
        && Objects.equals(this.value, field.value)
        && Objects.equals(this.unit, field.unit)
        && Objects.equals(this.options, field.options);
  }

  public int hashCode() {
    int result = this.key.hashCode();
    result = 31 * result + this.label.hashCode();
    result = 31 * result + this.kind.hashCode();
    result = 31 * result + this.value.hashCode();
    result = 31 * result + this.unit.hashCode();
    result = 31 * result + Boolean.hashCode(this.required);
    result = 31 * result + Boolean.hashCode(this.editable);
    result = 31 * result + this.options.hashCode();
    return result;
  }
}