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
 * Снимок classical-расчета для истории реакции: статус, diagnostics-счетчики и переносимые секции.
 */
public final class ChemistryStorageClassicalResultRecord {

  private final String id;
  private final String calculationId;
  private final int historyIndex;
  private final String status;
  private final int diagnosticCount;
  private final int errorCount;
  private final int warningCount;
  private final String coverageStatus;
  private final List<ChemistryStorageClassicalCoverageItemRecord> coverageItems;
  private final List<ChemistryStorageClassicalResultSectionRecord> sections;
  private final String note;

  private ChemistryStorageClassicalResultRecord(
      final String id,
      final String calculationId,
      final int historyIndex,
      final String status,
      final int diagnosticCount,
      final int errorCount,
      final int warningCount,
      final String coverageStatus,
      final List<ChemistryStorageClassicalCoverageItemRecord> coverageItems,
      final List<ChemistryStorageClassicalResultSectionRecord> sections,
      final String note) {
    this.id = id;
    this.calculationId = calculationId;
    this.historyIndex = historyIndex;
    this.status = status;
    this.diagnosticCount = diagnosticCount;
    this.errorCount = errorCount;
    this.warningCount = warningCount;
    this.coverageStatus = coverageStatus;
    this.coverageItems = coverageItems;
    this.sections = sections;
    this.note = note;
  }

  public static ChemistryStorageClassicalResultRecord of(
      final String id,
      final String calculationId,
      final int historyIndex,
      final String status,
      final int diagnosticCount,
      final int errorCount,
      final int warningCount,
      final String coverageStatus,
      final List<ChemistryStorageClassicalCoverageItemRecord> coverageItems,
      final List<ChemistryStorageClassicalResultSectionRecord> sections,
      final String note) {
    if (historyIndex < 0) {
      throw new IllegalArgumentException(
          "Storage classical result history index must be non-negative.");
    }
    if (diagnosticCount < 0 || errorCount < 0 || warningCount < 0) {
      throw new IllegalArgumentException(
          "Storage classical result diagnostic counters must be non-negative.");
    }
    if (errorCount + warningCount > diagnosticCount) {
      throw new IllegalArgumentException(
          "Storage classical result diagnostic counters are inconsistent.");
    }
    return new ChemistryStorageClassicalResultRecord(
        IdentifierValue.requireIdentifier(id, "Storage classical result record id"),
        IdentifierValue.requireIdentifier(calculationId, "Storage classical result calculation id"),
        historyIndex,
        IdentifierValue.requireIdentifier(status, "Storage classical result status"),
        diagnosticCount,
        errorCount,
        warningCount,
        IdentifierValue.requireIdentifier(
            coverageStatus, "Storage classical result coverage status"),
        List.copyOf(ChemistryStorageClassicalResultRecord.requireCoverageItems(coverageItems)),
        List.copyOf(ChemistryStorageClassicalResultRecord.requireSections(sections)),
        TextValue.optionalText(note));
  }

  public String id() {
    return this.id;
  }

  public String calculationId() {
    return this.calculationId;
  }

  public int historyIndex() {
    return this.historyIndex;
  }

  public String status() {
    return this.status;
  }

  public int diagnosticCount() {
    return this.diagnosticCount;
  }

  public int errorCount() {
    return this.errorCount;
  }

  public int warningCount() {
    return this.warningCount;
  }

  public String coverageStatus() {
    return this.coverageStatus;
  }

  public List<ChemistryStorageClassicalCoverageItemRecord> coverageItems() {
    return this.coverageItems;
  }

  public List<ChemistryStorageClassicalResultSectionRecord> sections() {
    return this.sections;
  }

  public String note() {
    return this.note;
  }

  public boolean hasSections() {
    return !this.sections.isEmpty();
  }

  public boolean hasDiagnostics() {
    return this.diagnosticCount > 0;
  }

  public boolean hasCoverageItems() {
    return !this.coverageItems.isEmpty();
  }

  public ChemistryStorageClassicalResultSectionRecord requireSection(final String kind) {
    final ChemistryStorageClassicalResultSectionRecord section = this.sectionOrNull(kind);
    if (section == null) {
      throw new IllegalArgumentException(
          "Storage classical result section not found: " + kind + ".");
    }
    return section;
  }

  public ChemistryStorageClassicalResultSectionRecord sectionOrNull(final String kind) {
    final String checkedKind =
        IdentifierValue.requireIdentifier(kind, "Storage classical result section lookup kind");
    for (int i = 0; i < this.sections.size(); ++i) {
      final ChemistryStorageClassicalResultSectionRecord section = this.sections.get(i);
      if (section.kind().equals(checkedKind)) {
        return section;
      }
    }
    return null;
  }

  private static List<ChemistryStorageClassicalResultSectionRecord> requireSections(
      final List<ChemistryStorageClassicalResultSectionRecord> sections) {
    if (sections == null) {
      return List.of();
    }
    for (int i = 0; i < sections.size(); ++i) {
      if (sections.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage classical result section record must not be null.");
    }
    return sections;
  }

  private static List<ChemistryStorageClassicalCoverageItemRecord> requireCoverageItems(
      final List<ChemistryStorageClassicalCoverageItemRecord> coverageItems) {
    if (coverageItems == null || coverageItems.isEmpty()) {
      throw new IllegalArgumentException(
          "Storage classical result coverage items must not be empty.");
    }
    for (int i = 0; i < coverageItems.size(); ++i) {
      if (coverageItems.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Storage classical result coverage item must not be null.");
    }
    return coverageItems;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageClassicalResultRecord)) {
      return false;
    }
    final ChemistryStorageClassicalResultRecord record =
        (ChemistryStorageClassicalResultRecord) other;
    return this.historyIndex == record.historyIndex
        && this.diagnosticCount == record.diagnosticCount
        && this.errorCount == record.errorCount
        && this.warningCount == record.warningCount
        && Objects.equals(this.id, record.id)
        && Objects.equals(this.calculationId, record.calculationId)
        && Objects.equals(this.status, record.status)
        && Objects.equals(this.coverageStatus, record.coverageStatus)
        && Objects.equals(this.coverageItems, record.coverageItems)
        && Objects.equals(this.sections, record.sections)
        && Objects.equals(this.note, record.note);
  }

  public int hashCode() {
    return Objects.hash(
        this.id,
        this.calculationId,
        this.historyIndex,
        this.status,
        this.diagnosticCount,
        this.errorCount,
        this.warningCount,
        this.coverageStatus,
        this.coverageItems,
        this.sections,
        this.note);
  }
}
