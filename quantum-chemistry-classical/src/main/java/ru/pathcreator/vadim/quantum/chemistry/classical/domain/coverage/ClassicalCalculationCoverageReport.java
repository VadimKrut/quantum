/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Сводный отчет о том, насколько выбранный расчетный путь покрыт локальным Java-native стеком.
 */
public final class ClassicalCalculationCoverageReport {

  private final ClassicalCoverageStatus status;
  private final List<ClassicalCalculationCoverageItem> items;
  private final ClassicalDiagnosticSet diagnostics;

  private ClassicalCalculationCoverageReport(
      final ClassicalCoverageStatus status,
      final List<ClassicalCalculationCoverageItem> items,
      final ClassicalDiagnosticSet diagnostics) {
    this.status = status;
    this.items = items;
    this.diagnostics = diagnostics;
  }

  public static ClassicalCalculationCoverageReport of(
      final List<ClassicalCalculationCoverageItem> items) {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("Classical coverage items must not be empty.");
    }
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();
    ClassicalCoverageStatus status = ClassicalCoverageStatus.EXACT;
    for (int i = 0; i < items.size(); ++i) {
      final ClassicalCalculationCoverageItem item = items.get(i);
      if (item == null) {
        throw new IllegalArgumentException("Classical coverage item must not be null.");
      }
      diagnostics.addAll(item.diagnostics());
      if (item.status() == ClassicalCoverageStatus.UNSUPPORTED) {
        status = ClassicalCoverageStatus.UNSUPPORTED;
        continue;
      }
      if (item.status() == ClassicalCoverageStatus.APPROXIMATE
          && status == ClassicalCoverageStatus.EXACT) {
        status = ClassicalCoverageStatus.APPROXIMATE;
      }
    }
    return new ClassicalCalculationCoverageReport(
        status, List.copyOf(items), ClassicalDiagnosticSet.of(diagnostics));
  }

  public ClassicalCoverageStatus status() {
    return this.status;
  }

  public List<ClassicalCalculationCoverageItem> items() {
    return this.items;
  }

  public ClassicalDiagnosticSet diagnostics() {
    return this.diagnostics;
  }

  public boolean exact() {
    return this.status == ClassicalCoverageStatus.EXACT;
  }

  public boolean executable() {
    return this.status != ClassicalCoverageStatus.UNSUPPORTED;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalCalculationCoverageReport)) {
      return false;
    }
    final ClassicalCalculationCoverageReport report = (ClassicalCalculationCoverageReport) other;
    return this.status == report.status
        && Objects.equals(this.items, report.items)
        && Objects.equals(this.diagnostics, report.diagnostics);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.status);
    result = ChemistryHash.include(result, this.items);
    result = ChemistryHash.include(result, this.diagnostics);
    return result;
  }
}
