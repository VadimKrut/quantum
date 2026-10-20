/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Покрытие одного расчетного шага: точность, модель и диагностические ограничения.
 */
public final class ClassicalCalculationCoverageItem {

  private final ClassicalCalculationKind kind;
  private final ClassicalCoverageStatus status;
  private final String model;
  private final List<ClassicalDiagnostic> diagnostics;

  private ClassicalCalculationCoverageItem(
      final ClassicalCalculationKind kind,
      final ClassicalCoverageStatus status,
      final String model,
      final List<ClassicalDiagnostic> diagnostics) {
    this.kind = kind;
    this.status = status;
    this.model = model;
    this.diagnostics = diagnostics;
  }

  public static ClassicalCalculationCoverageItem of(
      final ClassicalCalculationKind kind,
      final ClassicalCoverageStatus status,
      final String model,
      final List<ClassicalDiagnostic> diagnostics) {
    if (kind == null) {
      throw new IllegalArgumentException("Classical coverage calculation kind must not be null.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Classical coverage status must not be null.");
    }
    final List<ClassicalDiagnostic> checkedDiagnostics =
        diagnostics == null ? List.of() : List.copyOf(diagnostics);
    for (int i = 0; i < checkedDiagnostics.size(); ++i) {
      if (checkedDiagnostics.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Classical coverage diagnostic must not be null.");
    }
    return new ClassicalCalculationCoverageItem(
        kind, status, TextValue.requireText(model, "Classical coverage model"), checkedDiagnostics);
  }

  public ClassicalCalculationKind kind() {
    return this.kind;
  }

  public ClassicalCoverageStatus status() {
    return this.status;
  }

  public String model() {
    return this.model;
  }

  public List<ClassicalDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalCalculationCoverageItem)) {
      return false;
    }
    final ClassicalCalculationCoverageItem item = (ClassicalCalculationCoverageItem) other;
    return this.kind == item.kind
        && this.status == item.status
        && Objects.equals(this.model, item.model)
        && Objects.equals(this.diagnostics, item.diagnostics);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.kind);
    result = ChemistryHash.include(result, this.status);
    result = ChemistryHash.include(result, this.model);
    result = ChemistryHash.include(result, this.diagnostics);
    return result;
  }
}
