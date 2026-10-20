/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.result;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/** Сводка качества classical chemistry результата для API, CLI и UI без парсинга diagnostics. */
public final class ClassicalResultQualityReport {

  private final int sectionCount;
  private final int exactCount;
  private final int approximateCount;
  private final int externalCount;
  private final int fallbackCount;
  private final int unsupportedCount;
  private final int unknownCount;
  private final List<String> rows;

  private ClassicalResultQualityReport(
      final int sectionCount,
      final int exactCount,
      final int approximateCount,
      final int externalCount,
      final int fallbackCount,
      final int unsupportedCount,
      final int unknownCount,
      final List<String> rows) {
    this.sectionCount = sectionCount;
    this.exactCount = exactCount;
    this.approximateCount = approximateCount;
    this.externalCount = externalCount;
    this.fallbackCount = fallbackCount;
    this.unsupportedCount = unsupportedCount;
    this.unknownCount = unknownCount;
    this.rows = rows;
  }

  public static ClassicalResultQualityReport of(
      final int sectionCount,
      final int exactCount,
      final int approximateCount,
      final int externalCount,
      final int fallbackCount,
      final int unsupportedCount,
      final int unknownCount,
      final List<String> rows) {
    if (sectionCount < 0
        || exactCount < 0
        || approximateCount < 0
        || externalCount < 0
        || fallbackCount < 0
        || unsupportedCount < 0
        || unknownCount < 0) {
      throw new IllegalArgumentException("Classical result quality counters must be non-negative.");
    }
    if (rows == null) {
      throw new IllegalArgumentException("Classical result quality rows must not be null.");
    }
    return new ClassicalResultQualityReport(
        sectionCount,
        exactCount,
        approximateCount,
        externalCount,
        fallbackCount,
        unsupportedCount,
        unknownCount,
        List.copyOf(rows));
  }

  public int sectionCount() {
    return this.sectionCount;
  }

  public int exactCount() {
    return this.exactCount;
  }

  public int approximateCount() {
    return this.approximateCount;
  }

  public int externalCount() {
    return this.externalCount;
  }

  public int fallbackCount() {
    return this.fallbackCount;
  }

  public int unsupportedCount() {
    return this.unsupportedCount;
  }

  public int unknownCount() {
    return this.unknownCount;
  }

  public List<String> rows() {
    return this.rows;
  }

  public boolean fullyExact() {
    return this.sectionCount > 0
        && this.exactCount == this.sectionCount
        && this.approximateCount == 0
        && this.externalCount == 0
        && this.fallbackCount == 0
        && this.unsupportedCount == 0
        && this.unknownCount == 0;
  }

  public boolean hasApproximationOrFallback() {
    return this.approximateCount > 0
        || this.fallbackCount > 0
        || this.unsupportedCount > 0
        || this.unknownCount > 0;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalResultQualityReport)) {
      return false;
    }
    final ClassicalResultQualityReport report = (ClassicalResultQualityReport) other;
    return this.sectionCount == report.sectionCount
        && this.exactCount == report.exactCount
        && this.approximateCount == report.approximateCount
        && this.externalCount == report.externalCount
        && this.fallbackCount == report.fallbackCount
        && this.unsupportedCount == report.unsupportedCount
        && this.unknownCount == report.unknownCount
        && this.rows.equals(report.rows);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.sectionCount);
    result = ChemistryHash.include(result, this.exactCount);
    result = ChemistryHash.include(result, this.approximateCount);
    result = ChemistryHash.include(result, this.externalCount);
    result = ChemistryHash.include(result, this.fallbackCount);
    result = ChemistryHash.include(result, this.unsupportedCount);
    result = ChemistryHash.include(result, this.unknownCount);
    result = ChemistryHash.include(result, this.rows);
    return result;
  }
}
