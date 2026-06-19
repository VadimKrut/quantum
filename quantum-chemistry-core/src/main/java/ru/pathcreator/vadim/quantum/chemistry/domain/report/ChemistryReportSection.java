/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.report;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Раздел chemistry-отчёта со стабильным id, типом, заголовком и body. */
public final class ChemistryReportSection {

  private final String id;
  private final ChemistryReportSectionKind kind;
  private final String title;
  private final String body;

  private ChemistryReportSection(
      final String id,
      final ChemistryReportSectionKind kind,
      final String title,
      final String body) {
    this.id = id;
    this.kind = kind;
    this.title = title;
    this.body = body;
  }

  public static ChemistryReportSection of(
      final String title,
      final String body
  ) {
    return ChemistryReportSection.of(
        IdentifierValue.fromText(title), ChemistryReportSectionKind.CUSTOM, title, body);
  }

  public static ChemistryReportSection of(
      final String id,
      final ChemistryReportSectionKind kind,
      final String title,
      final String body) {
    final String checkedId = IdentifierValue.requireIdentifier(id, "Report section id");
    if (kind == null) {
      throw new IllegalArgumentException("Report section kind must not be null.");
    }
    return new ChemistryReportSection(
        checkedId,
        kind,
        TextValue.requireText(title, "Report section title"),
        TextValue.requireText(body, "Report section body"));
  }

  public String id() {
    return this.id;
  }

  public ChemistryReportSectionKind kind() {
    return this.kind;
  }

  public String title() {
    return this.title;
  }

  public String body() {
    return this.body;
  }

  public int bodyLength() {
    return this.body.length();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryReportSection)) {
      return false;
    }
    final ChemistryReportSection section = (ChemistryReportSection) other;
    return Objects.equals(this.id, section.id)
        && this.kind == section.kind
        && Objects.equals(this.title, section.title)
        && Objects.equals(this.body, section.body);
  }

  public int hashCode() {
    int result = this.id.hashCode();
    result = 31 * result + this.kind.hashCode();
    result = 31 * result + this.title.hashCode();
    result = 31 * result + this.body.hashCode();
    return result;
  }
}