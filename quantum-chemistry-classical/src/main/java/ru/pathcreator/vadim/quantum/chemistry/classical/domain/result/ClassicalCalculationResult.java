/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Переносимый результат выполнения или планирования классического расчета.
 */
public final class ClassicalCalculationResult {

  private final ClassicalCalculationId calculationId;
  private final ClassicalCalculationStatus status;
  private final List<ClassicalResultSection> sections;
  private final ClassicalDiagnosticSet diagnostics;
  private final ClassicalCalculationCoverageReport coverageReport;

  private ClassicalCalculationResult(
      final ClassicalCalculationId calculationId,
      final ClassicalCalculationStatus status,
      final List<ClassicalResultSection> sections,
      final ClassicalDiagnosticSet diagnostics,
      final ClassicalCalculationCoverageReport coverageReport) {
    this.calculationId = calculationId;
    this.status = status;
    this.sections = sections;
    this.diagnostics = diagnostics;
    this.coverageReport = coverageReport;
  }

  public static ClassicalCalculationResult of(
      final ClassicalCalculationId calculationId,
      final ClassicalCalculationStatus status,
      final List<ClassicalResultSection> sections,
      final ClassicalDiagnosticSet diagnostics,
      final ClassicalCalculationCoverageReport coverageReport) {
    if (calculationId == null) {
      throw new IllegalArgumentException("Classical calculation result id must not be null.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Classical calculation result status must not be null.");
    }
    if (coverageReport == null) {
      throw new IllegalArgumentException(
          "Classical calculation result coverage report must not be null.");
    }
    return new ClassicalCalculationResult(
        calculationId,
        status,
        List.copyOf(ClassicalCalculationResult.requireSections(sections)),
        diagnostics == null ? ClassicalDiagnosticSet.EMPTY : diagnostics,
        coverageReport);
  }

  public ClassicalCalculationId calculationId() {
    return this.calculationId;
  }

  public ClassicalCalculationStatus status() {
    return this.status;
  }

  public List<ClassicalResultSection> sections() {
    return this.sections;
  }

  public ClassicalDiagnosticSet diagnostics() {
    return this.diagnostics;
  }

  public ClassicalCalculationCoverageReport coverageReport() {
    return this.coverageReport;
  }

  public boolean successful() {
    return this.status == ClassicalCalculationStatus.SUCCEEDED;
  }

  public boolean partial() {
    return this.status == ClassicalCalculationStatus.PARTIAL;
  }

  public boolean failed() {
    return this.status == ClassicalCalculationStatus.FAILED;
  }

  public boolean unsupported() {
    return this.status == ClassicalCalculationStatus.UNSUPPORTED;
  }

  public int sectionCount() {
    return this.sections.size();
  }

  public boolean hasSections() {
    return !this.sections.isEmpty();
  }

  public boolean hasSection(final ClassicalCalculationKind kind) {
    return this.sectionOrNull(kind) != null;
  }

  public int sectionCount(final ClassicalCalculationKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Classical result section kind lookup must not be null.");
    }
    int count = 0;
    for (int i = 0; i < this.sections.size(); ++i) {
      if (this.sections.get(i).kind() == kind) {
        ++count;
      }
    }
    return count;
  }

  public List<ClassicalResultSection> sections(final ClassicalCalculationKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Classical result section kind lookup must not be null.");
    }
    final ArrayList<ClassicalResultSection> filtered = new ArrayList<ClassicalResultSection>();
    for (int i = 0; i < this.sections.size(); ++i) {
      final ClassicalResultSection section = this.sections.get(i);
      if (section.kind() == kind) {
        filtered.add(section);
      }
    }
    return List.copyOf(filtered);
  }

  public ClassicalResultSection requireSection(final ClassicalCalculationKind kind) {
    final ClassicalResultSection section = this.sectionOrNull(kind);
    if (section == null) {
      throw new IllegalArgumentException("Classical result section not found: " + kind + ".");
    }
    return section;
  }

  public ClassicalResultSection sectionOrNull(final ClassicalCalculationKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Classical result section kind lookup must not be null.");
    }
    for (int i = 0; i < this.sections.size(); ++i) {
      final ClassicalResultSection section = this.sections.get(i);
      if (section.kind() == kind) {
        return section;
      }
    }
    return null;
  }

  public boolean hasValue(final String name) {
    return this.valueOrNull(name) != null;
  }

  public ClassicalResultValue requireValue(final String name) {
    final ClassicalResultValue value = this.valueOrNull(name);
    if (value == null) {
      throw new IllegalArgumentException("Classical result value not found: " + name + ".");
    }
    return value;
  }

  public ClassicalResultValue valueOrNull(final String name) {
    for (int i = 0; i < this.sections.size(); ++i) {
      final ClassicalResultValue value = this.sections.get(i).valueOrNull(name);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  public String requireFirstValue(final String name) {
    return this.requireValue(name).firstValue();
  }

  public List<String> requireValues(final String name) {
    return this.requireValue(name).values();
  }

  public double requireScalar(final String name) {
    return this.requireValue(name).scalarValue();
  }

  private static List<ClassicalResultSection> requireSections(
      final List<ClassicalResultSection> sections) {
    if (sections == null) {
      return List.of();
    }
    for (int i = 0; i < sections.size(); ++i) {
      if (sections.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Classical result section must not be null.");
    }
    return sections;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalCalculationResult)) {
      return false;
    }
    final ClassicalCalculationResult result = (ClassicalCalculationResult) other;
    return Objects.equals(this.calculationId, result.calculationId)
        && this.status == result.status
        && Objects.equals(this.sections, result.sections)
        && Objects.equals(this.diagnostics, result.diagnostics)
        && Objects.equals(this.coverageReport, result.coverageReport);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.calculationId);
    result = ChemistryHash.include(result, this.status);
    result = ChemistryHash.include(result, this.sections);
    result = ChemistryHash.include(result, this.diagnostics);
    result = ChemistryHash.include(result, this.coverageReport);
    return result;
  }
}
