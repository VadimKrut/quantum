/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.report.ChemistryReport;
import ru.pathcreator.vadim.quantum.chemistry.domain.report.ChemistryReportSection;
import ru.pathcreator.vadim.quantum.chemistry.domain.report.ChemistryReportSectionKind;

final class ChemistryCoreReportTest {

  ChemistryCoreReportTest() {}

  @Test
  void reportStoresStableSectionsAndSummaryMetrics() {
    final ChemistryReportSection summary =
        ChemistryReportSection.of(
            (String) "summary",
            (ChemistryReportSectionKind) ChemistryReportSectionKind.SUMMARY,
            (String) "Summary",
            (String) "Validated electronic structure result.");
    final ChemistryReportSection diagnostics =
        ChemistryReportSection.of(
            (String) "diagnostics",
            (ChemistryReportSectionKind) ChemistryReportSectionKind.DIAGNOSTICS,
            (String) "Diagnostics",
            (String) "No blocking diagnostics.");
    ChemistryReport report =
        ChemistryReport.of(
            (String) "report.h2", (String) "Hydrogen calculation", List.of(summary, diagnostics));
    Assertions.assertEquals((Object) "report.h2", (Object) report.id());
    Assertions.assertEquals((Object) "Hydrogen calculation", (Object) report.title());
    Assertions.assertEquals((int) 2, (int) report.sectionCount());
    Assertions.assertTrue((boolean) report.containsSection("summary"));
    Assertions.assertEquals((Object) summary, (Object) report.sectionById("summary"));
    Assertions.assertEquals(
        (int) 1, (int) report.sectionCountByKind(ChemistryReportSectionKind.SUMMARY));
    Assertions.assertEquals(
        (int) 1, (int) report.sectionCountByKind(ChemistryReportSectionKind.DIAGNOSTICS));
    Assertions.assertEquals(
        (int) (summary.bodyLength() + diagnostics.bodyLength()), (int) report.totalBodyLength());
    Assertions.assertEquals(
        (Object)
            ChemistryReport.of(
                (String) "report.h2",
                (String) "Hydrogen calculation",
                List.of(summary, diagnostics)),
        (Object) report);
    Assertions.assertEquals(
        (int)
            ChemistryReport.of(
                    (String) "report.h2",
                    (String) "Hydrogen calculation",
                    List.of(summary, diagnostics))
                .hashCode(),
        (int) report.hashCode());
  }

  @Test
  void reportLegacyFactoriesGenerateStableIdentifiers() {
    ChemistryReportSection section =
        ChemistryReportSection.of(
            (String) "Electronic Structure Summary", (String) "Active space: 2e, 2o.");
    final ChemistryReport report = ChemistryReport.of((String) "Hydrogen Report", List.of(section));
    Assertions.assertEquals((Object) "electronic_structure_summary", (Object) section.id());
    Assertions.assertEquals((Object) ChemistryReportSectionKind.CUSTOM, (Object) section.kind());
    Assertions.assertEquals((Object) "hydrogen_report", (Object) report.id());
    Assertions.assertEquals(
        (Object) section, (Object) report.sectionById("electronic_structure_summary"));
  }

  @Test
  void reportRejectsEmptyNullDuplicateAndInvalidSections() {
    final ChemistryReportSection section =
        ChemistryReportSection.of(
            (String) "summary",
            (ChemistryReportSectionKind) ChemistryReportSectionKind.SUMMARY,
            (String) "Summary",
            (String) "Body");
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ChemistryReport.of((String) "empty", (String) "Empty", List.of()));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryReport.of(
                (String) "null.section", (String) "Null section", Arrays.asList(section, null)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryReport.of(
                (String) "duplicate", (String) "Duplicate", List.of(section, section)));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryReportSection.of(
                (String) "bad id",
                (ChemistryReportSectionKind) ChemistryReportSectionKind.SUMMARY,
                (String) "Bad",
                (String) "Body"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ChemistryReportSection.of((String) "bad", null, (String) "Bad", (String) "Body"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryReportSection.of(
                (String) "bad",
                (ChemistryReportSectionKind) ChemistryReportSectionKind.SUMMARY,
                (String) " ",
                (String) "Body"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ChemistryReportSection.of(
                (String) "bad",
                (ChemistryReportSectionKind) ChemistryReportSectionKind.SUMMARY,
                (String) "Bad",
                (String) " "));
  }

  @Test
  void identifierValueBuildsReportFriendlyIdsFromText() {
    Assertions.assertEquals(
        (Object) "alpha_beta_2", (Object) IdentifierValue.fromText((String) " Alpha beta 2 "));
    Assertions.assertEquals(
        (Object) "id_2fast", (Object) IdentifierValue.fromText((String) "2fast"));
    Assertions.assertEquals((Object) "id", (Object) IdentifierValue.fromText((String) " !!! "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> IdentifierValue.fromText(null));
  }
}