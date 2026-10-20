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
 * Раздел сохраненного classical-результата: расчетный kind, заголовок и значения.
 */
public final class ChemistryStorageClassicalResultSectionRecord {

  private final String kind;
  private final String title;
  private final List<ChemistryStorageClassicalResultValueRecord> values;

  private ChemistryStorageClassicalResultSectionRecord(
      final String kind,
      final String title,
      final List<ChemistryStorageClassicalResultValueRecord> values) {
    this.kind = kind;
    this.title = title;
    this.values = values;
  }

  public static ChemistryStorageClassicalResultSectionRecord of(
      final String kind,
      final String title,
      final List<ChemistryStorageClassicalResultValueRecord> values) {
    return new ChemistryStorageClassicalResultSectionRecord(
        IdentifierValue.requireIdentifier(kind, "Storage classical result section kind"),
        TextValue.requireText(title, "Storage classical result section title"),
        List.copyOf(ChemistryStorageClassicalResultSectionRecord.requireValues(values)));
  }

  public String kind() {
    return this.kind;
  }

  public String title() {
    return this.title;
  }

  public List<ChemistryStorageClassicalResultValueRecord> values() {
    return this.values;
  }

  public int valueCount() {
    return this.values.size();
  }

  public boolean hasValue(final String name) {
    return this.valueOrNull(name) != null;
  }

  public ChemistryStorageClassicalResultValueRecord requireValue(final String name) {
    final ChemistryStorageClassicalResultValueRecord value = this.valueOrNull(name);
    if (value == null) {
      throw new IllegalArgumentException("Storage classical result value not found: " + name + ".");
    }
    return value;
  }

  public ChemistryStorageClassicalResultValueRecord valueOrNull(final String name) {
    final String checkedName =
        IdentifierValue.requireIdentifier(name, "Storage classical result value lookup name");
    for (int i = 0; i < this.values.size(); ++i) {
      final ChemistryStorageClassicalResultValueRecord value = this.values.get(i);
      if (value.name().equals(checkedName)) {
        return value;
      }
    }
    return null;
  }

  private static List<ChemistryStorageClassicalResultValueRecord> requireValues(
      final List<ChemistryStorageClassicalResultValueRecord> values) {
    if (values == null) {
      return List.of();
    }
    for (int i = 0; i < values.size(); ++i) {
      if (values.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Storage classical result value record must not be null.");
    }
    return values;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageClassicalResultSectionRecord)) {
      return false;
    }
    final ChemistryStorageClassicalResultSectionRecord record =
        (ChemistryStorageClassicalResultSectionRecord) other;
    return Objects.equals(this.kind, record.kind)
        && Objects.equals(this.title, record.title)
        && Objects.equals(this.values, record.values);
  }

  public int hashCode() {
    return Objects.hash(this.kind, this.title, this.values);
  }
}
