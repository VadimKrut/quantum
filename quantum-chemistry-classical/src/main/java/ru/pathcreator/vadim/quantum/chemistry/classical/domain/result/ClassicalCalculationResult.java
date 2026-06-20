/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.result;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
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

  private ClassicalCalculationResult(
      final ClassicalCalculationId calculationId,
      final ClassicalCalculationStatus status,
      final List<ClassicalResultSection> sections,
      final ClassicalDiagnosticSet diagnostics
  ) {
    this.calculationId = calculationId;
    this.status = status;
    this.sections = sections;
    this.diagnostics = diagnostics;
  }

  public static ClassicalCalculationResult of(
      final ClassicalCalculationId calculationId,
      final ClassicalCalculationStatus status,
      final List<ClassicalResultSection> sections,
      final ClassicalDiagnosticSet diagnostics
  ) {
    if (calculationId == null) {
      throw new IllegalArgumentException("Classical calculation result id must not be null.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Classical calculation result status must not be null.");
    }
    return new ClassicalCalculationResult(
        calculationId,
        status,
        List.copyOf(ClassicalCalculationResult.requireSections(sections)),
        diagnostics == null ? ClassicalDiagnosticSet.EMPTY : diagnostics);
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

  private static List<ClassicalResultSection> requireSections(
      final List<ClassicalResultSection> sections
  ) {
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
        && Objects.equals(this.diagnostics, result.diagnostics);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.calculationId);
    result = ChemistryHash.include(result, this.status);
    result = ChemistryHash.include(result, this.sections);
    result = ChemistryHash.include(result, this.diagnostics);
    return result;
  }
}