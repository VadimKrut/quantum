/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Переносимое значение classical-результата внутри QCHEM storage без зависимости от расчетного модуля.
 */
public final class ChemistryStorageClassicalResultValueRecord {

  private final String name;
  private final String kind;
  private final String unit;
  private final List<String> values;

  private ChemistryStorageClassicalResultValueRecord(
      final String name, final String kind, final String unit, final List<String> values) {
    this.name = name;
    this.kind = kind;
    this.unit = unit;
    this.values = values;
  }

  public static ChemistryStorageClassicalResultValueRecord of(
      final String name, final String kind, final String unit, final List<String> values) {
    return new ChemistryStorageClassicalResultValueRecord(
        IdentifierValue.requireIdentifier(name, "Storage classical result value name"),
        IdentifierValue.requireIdentifier(kind, "Storage classical result value kind"),
        unit == null ? "" : unit,
        List.copyOf(ChemistryStorageClassicalResultValueRecord.requireValues(values)));
  }

  public String name() {
    return this.name;
  }

  public String kind() {
    return this.kind;
  }

  public String unit() {
    return this.unit;
  }

  public List<String> values() {
    return this.values;
  }

  public int valueCount() {
    return this.values.size();
  }

  public String firstValue() {
    return this.values.get(0);
  }

  private static List<String> requireValues(final List<String> values) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("Storage classical result value list must not be empty.");
    }
    for (int i = 0; i < values.size(); ++i) {
      TextValue.requireText(values.get(i), "Storage classical result value item");
    }
    return values;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageClassicalResultValueRecord)) {
      return false;
    }
    final ChemistryStorageClassicalResultValueRecord record =
        (ChemistryStorageClassicalResultValueRecord) other;
    return Objects.equals(this.name, record.name)
        && Objects.equals(this.kind, record.kind)
        && Objects.equals(this.unit, record.unit)
        && Objects.equals(this.values, record.values);
  }

  public int hashCode() {
    return Objects.hash(this.name, this.kind, this.unit, this.values);
  }
}
