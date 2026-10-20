/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.result;

import java.util.ArrayList;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageItem;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCoverageStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;

/** Собирает quality summary из machine-readable quality values внутри classical result sections. */
public final class ClassicalResultQualityAnalyzer {

  public ClassicalResultQualityReport analyze(final ClassicalCalculationResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Classical result quality input must not be null.");
    }
    int exactCount = 0;
    int approximateCount = 0;
    int externalCount = 0;
    int fallbackCount = 0;
    int unsupportedCount = 0;
    int unknownCount = 0;
    final ArrayList<String> rows = new ArrayList<String>(result.sectionCount() + 1);
    rows.add(
        "section_kind|title|quality_level|quality_backend|quality_model|quality_scope|quality_limitation");
    for (int i = 0; i < result.sections().size(); ++i) {
      final ClassicalResultSection section = result.sections().get(i);
      final ClassicalCalculationCoverageItem coverageItem =
          ClassicalResultQualityAnalyzer.coverageItemOrNull(result, section);
      final String level = ClassicalResultQualityAnalyzer.level(section, coverageItem);
      final String normalizedLevel = level.toLowerCase(Locale.ROOT);
      if ("exact".equals(normalizedLevel)) {
        ++exactCount;
      } else if ("approximate".equals(normalizedLevel)) {
        ++approximateCount;
      } else if ("external".equals(normalizedLevel)) {
        ++externalCount;
      } else if ("fallback".equals(normalizedLevel)) {
        ++fallbackCount;
      } else if ("unsupported".equals(normalizedLevel)) {
        ++unsupportedCount;
      } else {
        ++unknownCount;
      }
      rows.add(
          section.kind().name()
              + "|"
              + section.title()
              + "|"
              + level
              + "|"
              + ClassicalResultQualityAnalyzer.backend(section, coverageItem)
              + "|"
              + ClassicalResultQualityAnalyzer.model(section, coverageItem)
              + "|"
              + ClassicalResultQualityAnalyzer.value(section, "quality_scope", "coverage_report")
              + "|"
              + ClassicalResultQualityAnalyzer.limitation(section, coverageItem));
    }
    return ClassicalResultQualityReport.of(
        result.sectionCount(),
        exactCount,
        approximateCount,
        externalCount,
        fallbackCount,
        unsupportedCount,
        unknownCount,
        rows);
  }

  private static String value(
      final ClassicalResultSection section, final String name, final String fallback) {
    final ClassicalResultValue value = section.valueOrNull(name);
    return value == null ? fallback : value.firstValue();
  }

  private static String level(
      final ClassicalResultSection section, final ClassicalCalculationCoverageItem coverageItem) {
    final ClassicalResultValue value = section.valueOrNull("quality_level");
    if (value != null) {
      return value.firstValue();
    }
    if (coverageItem == null) {
      return "unknown";
    }
    final String model = coverageItem.model().toLowerCase(Locale.ROOT);
    if (model.contains("pyscf")) {
      return "external";
    }
    if (model.contains("fallback")) {
      return "fallback";
    }
    if (coverageItem.status() == ClassicalCoverageStatus.EXACT) {
      return "exact";
    }
    if (coverageItem.status() == ClassicalCoverageStatus.APPROXIMATE) {
      return "approximate";
    }
    return "unsupported";
  }

  private static String backend(
      final ClassicalResultSection section, final ClassicalCalculationCoverageItem coverageItem) {
    final ClassicalResultValue value = section.valueOrNull("quality_backend");
    if (value != null) {
      return value.firstValue();
    }
    if (coverageItem != null && coverageItem.model().toLowerCase(Locale.ROOT).contains("pyscf")) {
      return "PySCF";
    }
    return coverageItem == null ? "unknown" : "java_native";
  }

  private static String model(
      final ClassicalResultSection section, final ClassicalCalculationCoverageItem coverageItem) {
    final ClassicalResultValue value = section.valueOrNull("quality_model");
    return value == null
        ? coverageItem == null ? "unknown" : coverageItem.model()
        : value.firstValue();
  }

  private static String limitation(
      final ClassicalResultSection section, final ClassicalCalculationCoverageItem coverageItem) {
    final ClassicalResultValue value = section.valueOrNull("quality_limitation");
    if (value != null) {
      return value.firstValue();
    }
    if (coverageItem == null || coverageItem.diagnostics().isEmpty()) {
      return "none";
    }
    return coverageItem.diagnostics().get(0).message();
  }

  private static ClassicalCalculationCoverageItem coverageItemOrNull(
      final ClassicalCalculationResult result, final ClassicalResultSection section) {
    for (int i = 0; i < result.coverageReport().items().size(); ++i) {
      final ClassicalCalculationCoverageItem item = result.coverageReport().items().get(i);
      if (item.kind() == section.kind()) {
        return item;
      }
    }
    return null;
  }
}
