/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.report;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class ChemistryReport {

  private final String id;
  private final String title;
  private final List<ChemistryReportSection> sections;

  private ChemistryReport(
      final String id,
      final String title,
      final List<ChemistryReportSection> sections
  ) {
    this.id = id;
    this.title = title;
    this.sections = sections;
  }

  public static ChemistryReport of(
      final String title,
      final List<ChemistryReportSection> sections
  ) {
    return ChemistryReport.of(IdentifierValue.fromText(title), title, sections);
  }

  public static ChemistryReport of(
      final String id,
      final String title,
      final List<ChemistryReportSection> sections
  ) {
    String checkedId = IdentifierValue.requireIdentifier(id, "Report id");
    if (sections == null) {
      throw new IllegalArgumentException("Report sections must not be null.");
    }
    if (sections.isEmpty()) {
      throw new IllegalArgumentException("Report sections must not be empty.");
    }
    for (int i = 0; i < sections.size(); ++i) {
      ChemistryReportSection section = sections.get(i);
      if (section == null) {
        throw new IllegalArgumentException("Report section must not be null.");
      }
      for (int j = i + 1; j < sections.size(); ++j) {
        ChemistryReportSection other = sections.get(j);
        if (other == null) {
          throw new IllegalArgumentException("Report section must not be null.");
        }
        if (!section.id().equals(other.id())) continue;
        throw new IllegalArgumentException("Report section ids must be unique.");
      }
    }
    return new ChemistryReport(
        checkedId, TextValue.requireText(title, "Report title"), List.copyOf(sections));
  }

  public String id() {
    return this.id;
  }

  public String title() {
    return this.title;
  }

  public List<ChemistryReportSection> sections() {
    return this.sections;
  }

  public int sectionCount() {
    return this.sections.size();
  }

  public boolean containsSection(final String sectionId) {
    String checkedId = IdentifierValue.requireIdentifier(sectionId, "Report section id");
    return this.sectionById(checkedId) != null;
  }

  public ChemistryReportSection sectionById(final String sectionId) {
    final String checkedId = IdentifierValue.requireIdentifier(sectionId, "Report section id");
    for (int i = 0; i < this.sections.size(); ++i) {
      if (!this.sections.get(i).id().equals(checkedId)) continue;
      return this.sections.get(i);
    }
    return null;
  }

  public int sectionCountByKind(final ChemistryReportSectionKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Report section kind must not be null.");
    }
    int count = 0;
    for (int i = 0; i < this.sections.size(); ++i) {
      if (this.sections.get(i).kind() != kind) continue;
      ++count;
    }
    return count;
  }

  public int totalBodyLength() {
    int length = 0;
    for (int i = 0; i < this.sections.size(); ++i) {
      length = Math.addExact(length, this.sections.get(i).bodyLength());
    }
    return length;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryReport)) {
      return false;
    }
    final ChemistryReport report = (ChemistryReport) other;
    return Objects.equals(this.id, report.id)
        && Objects.equals(this.title, report.title)
        && Objects.equals(this.sections, report.sections);
  }

  public int hashCode() {
    return Objects.hash(this.id, this.title, this.sections);
  }
}