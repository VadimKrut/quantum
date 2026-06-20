/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Типизированная настройка расчета без привязки к конкретному внешнему движку.
 */
public final class ClassicalCalculationOption {

  private final String name;
  private final ClassicalCalculationOptionKind kind;
  private final String value;

  private ClassicalCalculationOption(
      final String name,
      final ClassicalCalculationOptionKind kind,
      final String value
  ) {
    this.name = name;
    this.kind = kind;
    this.value = value;
  }

  public static ClassicalCalculationOption text(
      final String name,
      final String value
  ) {
    return ClassicalCalculationOption.of(
        name,
        ClassicalCalculationOptionKind.TEXT,
        TextValue.requireText(
            value,
            "Classical calculation text option"));
  }

  public static ClassicalCalculationOption integer(
      final String name,
      final int value
  ) {
    return ClassicalCalculationOption.of(
        name,
        ClassicalCalculationOptionKind.INTEGER,
        Integer.toString(value));
  }

  public static ClassicalCalculationOption longValue(
      final String name,
      final long value
  ) {
    return ClassicalCalculationOption.of(
        name,
        ClassicalCalculationOptionKind.LONG,
        Long.toString(value));
  }

  public static ClassicalCalculationOption floating(
      final String name,
      final double value
  ) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Classical calculation double option must be finite.");
    }
    return ClassicalCalculationOption.of(
        name,
        ClassicalCalculationOptionKind.DOUBLE,
        Double.toString(value));
  }

  public static ClassicalCalculationOption bool(
      final String name,
      final boolean value
  ) {
    return ClassicalCalculationOption.of(
        name,
        ClassicalCalculationOptionKind.BOOLEAN,
        Boolean.toString(value));
  }

  public static ClassicalCalculationOption of(
      final String name,
      final ClassicalCalculationOptionKind kind,
      final String value
  ) {
    if (kind == null) {
      throw new IllegalArgumentException("Classical calculation option kind must not be null.");
    }
    return new ClassicalCalculationOption(
        IdentifierValue.requireIdentifier(
            name,
            "Classical calculation option name"),
        kind,
        TextValue.requireText(
            value,
            "Classical calculation option value"));
  }

  public String name() {
    return this.name;
  }

  public ClassicalCalculationOptionKind kind() {
    return this.kind;
  }

  public String value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalCalculationOption)) {
      return false;
    }
    final ClassicalCalculationOption option = (ClassicalCalculationOption) other;
    return Objects.equals(this.name, option.name)
        && this.kind == option.kind
        && Objects.equals(this.value, option.value);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.name);
    result = ChemistryHash.include(result, this.kind);
    result = ChemistryHash.include(result, this.value);
    return result;
  }
}