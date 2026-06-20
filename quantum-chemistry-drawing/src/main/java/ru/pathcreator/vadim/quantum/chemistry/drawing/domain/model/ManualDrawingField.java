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
 * Ручное поле редактора для химических данных, которые удобнее задать формой, а не жестом на canvas.
 */
public final class ManualDrawingField {

  private final ChemistryDrawingFeature feature;
  private final String key;
  private final String value;

  private ManualDrawingField(
      final ChemistryDrawingFeature feature,
      final String key,
      final String value
  ) {
    this.feature = feature;
    this.key = key;
    this.value = value;
  }

  public static ManualDrawingField of(
      final ChemistryDrawingFeature feature,
      final String key,
      final String value
  ) {
    if (feature == null) {
      throw new IllegalArgumentException("Manual drawing field feature must not be null.");
    }
    return new ManualDrawingField(
        feature,
        ManualDrawingField.requireText(key, "Manual drawing field key"),
        ManualDrawingField.requireText(value, "Manual drawing field value")
    );
  }

  public ChemistryDrawingFeature feature() {
    return this.feature;
  }

  public String key() {
    return this.key;
  }

  public String value() {
    return this.value;
  }

  private static String requireText(
      final String value,
      final String subject
  ) {
    if (value == null) {
      throw new IllegalArgumentException(subject + " must not be null.");
    }
    final String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(subject + " must not be blank.");
    }
    return trimmed;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ManualDrawingField)) {
      return false;
    }
    final ManualDrawingField field = (ManualDrawingField) other;
    return this.feature == field.feature
        && Objects.equals(this.key, field.key)
        && Objects.equals(this.value, field.value);
  }

  public int hashCode() {
    int result = this.feature.hashCode();
    result = 31 * result + this.key.hashCode();
    result = 31 * result + this.value.hashCode();
    return result;
  }
}