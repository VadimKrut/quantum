/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.result;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Универсальное переносимое значение результата без зависимости от конкретного backend.
 */
public final class ClassicalResultValue {

  private final String name;
  private final ClassicalResultValueKind kind;
  private final String unit;
  private final List<String> values;

  private ClassicalResultValue(
      final String name,
      final ClassicalResultValueKind kind,
      final String unit,
      final List<String> values
  ) {
    this.name = name;
    this.kind = kind;
    this.unit = unit;
    this.values = values;
  }

  public static ClassicalResultValue scalar(
      final String name,
      final double value,
      final String unit
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Classical result scalar must be finite.");
    }
    return ClassicalResultValue.of(
        name,
        ClassicalResultValueKind.SCALAR,
        unit,
        List.of(Double.toString(value)));
  }

  public static ClassicalResultValue text(
      final String name,
      final String value
  ) {
    return ClassicalResultValue.of(
        name,
        ClassicalResultValueKind.TEXT,
        "",
        List.of(TextValue.requireText(value, "Classical result text")));
  }

  public static ClassicalResultValue of(
      final String name,
      final ClassicalResultValueKind kind,
      final String unit,
      final List<String> values
  ) {
    if (kind == null) {
      throw new IllegalArgumentException("Classical result value kind must not be null.");
    }
    return new ClassicalResultValue(
        IdentifierValue.requireIdentifier(
            name,
            "Classical result value name"),
        kind,
        unit == null ? "" : unit,
        List.copyOf(ClassicalResultValue.requireValues(values)));
  }

  public String name() {
    return this.name;
  }

  public ClassicalResultValueKind kind() {
    return this.kind;
  }

  public String unit() {
    return this.unit;
  }

  public List<String> values() {
    return this.values;
  }

  private static List<String> requireValues(final List<String> values) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("Classical result values must not be empty.");
    }
    for (int i = 0; i < values.size(); ++i) {
      TextValue.requireText(
          values.get(i),
          "Classical result value item");
    }
    return values;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalResultValue)) {
      return false;
    }
    final ClassicalResultValue value = (ClassicalResultValue) other;
    return Objects.equals(this.name, value.name)
        && this.kind == value.kind
        && Objects.equals(this.unit, value.unit)
        && Objects.equals(this.values, value.values);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.name);
    result = ChemistryHash.include(result, this.kind);
    result = ChemistryHash.include(result, this.unit);
    result = ChemistryHash.include(result, this.values);
    return result;
  }
}