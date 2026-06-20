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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Раздел результата для одной расчетной задачи.
 */
public final class ClassicalResultSection {

  private final ClassicalCalculationKind kind;
  private final String title;
  private final List<ClassicalResultValue> values;

  private ClassicalResultSection(
      final ClassicalCalculationKind kind,
      final String title,
      final List<ClassicalResultValue> values
  ) {
    this.kind = kind;
    this.title = title;
    this.values = values;
  }

  public static ClassicalResultSection of(
      final ClassicalCalculationKind kind,
      final String title,
      final List<ClassicalResultValue> values
  ) {
    if (kind == null) {
      throw new IllegalArgumentException("Classical result section kind must not be null.");
    }
    return new ClassicalResultSection(
        kind,
        TextValue.requireText(
            title,
            "Classical result section title"),
        List.copyOf(ClassicalResultSection.requireValues(values)));
  }

  public ClassicalCalculationKind kind() {
    return this.kind;
  }

  public String title() {
    return this.title;
  }

  public List<ClassicalResultValue> values() {
    return this.values;
  }

  private static List<ClassicalResultValue> requireValues(
      final List<ClassicalResultValue> values
  ) {
    if (values == null) {
      return List.of();
    }
    for (int i = 0; i < values.size(); ++i) {
      if (values.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Classical result value must not be null.");
    }
    return values;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalResultSection)) {
      return false;
    }
    final ClassicalResultSection section = (ClassicalResultSection) other;
    return this.kind == section.kind
        && Objects.equals(this.title, section.title)
        && Objects.equals(this.values, section.values);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.kind);
    result = ChemistryHash.include(result, this.title);
    result = ChemistryHash.include(result, this.values);
    return result;
  }
}