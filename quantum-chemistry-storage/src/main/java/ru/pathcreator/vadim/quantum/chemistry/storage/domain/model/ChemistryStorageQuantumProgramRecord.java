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
 * Сохраненная quantum-программа, подготовленная из химического расчета или реакционного workflow.
 */
public final class ChemistryStorageQuantumProgramRecord {

  private final String id;
  private final String sourceRecordId;
  private final int historyIndex;
  private final String kind;
  private final String target;
  private final String nativePayload;
  private final String exportedPayload;
  private final List<String> diagnostics;
  private final String note;

  private ChemistryStorageQuantumProgramRecord(
      final String id,
      final String sourceRecordId,
      final int historyIndex,
      final String kind,
      final String target,
      final String nativePayload,
      final String exportedPayload,
      final List<String> diagnostics,
      final String note) {
    this.id = id;
    this.sourceRecordId = sourceRecordId;
    this.historyIndex = historyIndex;
    this.kind = kind;
    this.target = target;
    this.nativePayload = nativePayload;
    this.exportedPayload = exportedPayload;
    this.diagnostics = diagnostics;
    this.note = note;
  }

  public static ChemistryStorageQuantumProgramRecord of(
      final String id,
      final String sourceRecordId,
      final int historyIndex,
      final String kind,
      final String target,
      final String nativePayload,
      final String exportedPayload,
      final List<String> diagnostics,
      final String note) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException(
          "Quantum program record history index must be non-negative.");
    }
    return new ChemistryStorageQuantumProgramRecord(
        IdentifierValue.requireIdentifier(id, "Quantum program record id"),
        IdentifierValue.requireIdentifier(sourceRecordId, "Quantum program source record id"),
        historyIndex,
        IdentifierValue.requireIdentifier(kind, "Quantum program kind"),
        IdentifierValue.requireIdentifier(target, "Quantum program target"),
        TextValue.optionalText(nativePayload),
        TextValue.optionalText(exportedPayload),
        ChemistryStorageQuantumProgramRecord.checkedDiagnostics(diagnostics),
        TextValue.optionalText(note));
  }

  public String id() {
    return this.id;
  }

  public String sourceRecordId() {
    return this.sourceRecordId;
  }

  public int historyIndex() {
    return this.historyIndex;
  }

  public String kind() {
    return this.kind;
  }

  public String target() {
    return this.target;
  }

  public String nativePayload() {
    return this.nativePayload;
  }

  public boolean hasNativePayload() {
    return this.nativePayload != null;
  }

  public String exportedPayload() {
    return this.exportedPayload;
  }

  public boolean hasExportedPayload() {
    return this.exportedPayload != null;
  }

  public List<String> diagnostics() {
    return this.diagnostics;
  }

  public boolean hasDiagnostics() {
    return !this.diagnostics.isEmpty();
  }

  public String note() {
    return this.note;
  }

  public boolean hasNote() {
    return this.note != null;
  }

  private static List<String> checkedDiagnostics(final List<String> diagnostics) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return List.of();
    }
    final String[] checked = new String[diagnostics.size()];
    for (int i = 0; i < diagnostics.size(); ++i) {
      checked[i] = TextValue.requireText(diagnostics.get(i), "Quantum program diagnostic");
    }
    return List.of(checked);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageQuantumProgramRecord)) {
      return false;
    }
    final ChemistryStorageQuantumProgramRecord record =
        (ChemistryStorageQuantumProgramRecord) other;
    return this.historyIndex == record.historyIndex
        && Objects.equals(this.id, record.id)
        && Objects.equals(this.sourceRecordId, record.sourceRecordId)
        && Objects.equals(this.kind, record.kind)
        && Objects.equals(this.target, record.target)
        && Objects.equals(this.nativePayload, record.nativePayload)
        && Objects.equals(this.exportedPayload, record.exportedPayload)
        && Objects.equals(this.diagnostics, record.diagnostics)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.sourceRecordId,
        this.historyIndex,
        this.kind,
        this.target,
        this.nativePayload,
        this.exportedPayload,
        this.diagnostics,
        this.note);
  }
}
