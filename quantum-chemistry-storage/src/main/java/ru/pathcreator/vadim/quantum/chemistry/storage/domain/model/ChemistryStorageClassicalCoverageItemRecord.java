/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Снимок coverage одного classical-расчетного шага: расчетный тип, статус покрытия и модель исполнения.
 */
public final class ChemistryStorageClassicalCoverageItemRecord {

  private final String kind;
  private final String status;
  private final String model;

  private ChemistryStorageClassicalCoverageItemRecord(
      final String kind, final String status, final String model) {
    this.kind = kind;
    this.status = status;
    this.model = model;
  }

  public static ChemistryStorageClassicalCoverageItemRecord of(
      final String kind, final String status, final String model) {
    return new ChemistryStorageClassicalCoverageItemRecord(
        IdentifierValue.requireIdentifier(kind, "Storage classical coverage kind"),
        IdentifierValue.requireIdentifier(status, "Storage classical coverage status"),
        TextValue.requireText(model, "Storage classical coverage model"));
  }

  public String kind() {
    return this.kind;
  }

  public String status() {
    return this.status;
  }

  public String model() {
    return this.model;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageClassicalCoverageItemRecord)) {
      return false;
    }
    final ChemistryStorageClassicalCoverageItemRecord record =
        (ChemistryStorageClassicalCoverageItemRecord) other;
    return Objects.equals(this.kind, record.kind)
        && Objects.equals(this.status, record.status)
        && Objects.equals(this.model, record.model);
  }

  public int hashCode() {
    return Objects.hash(this.kind, this.status, this.model);
  }
}
