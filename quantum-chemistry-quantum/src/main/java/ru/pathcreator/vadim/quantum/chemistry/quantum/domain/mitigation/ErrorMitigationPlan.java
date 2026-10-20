/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Результат планирования error mitigation для quantum chemistry запуска.
 */
public final class ErrorMitigationPlan {

  private final ErrorMitigationStatus status;
  private final List<ErrorMitigationTechnique> techniques;
  private final int recommendedShots;
  private final List<ErrorMitigationDiagnostic> diagnostics;

  private ErrorMitigationPlan(
      final ErrorMitigationStatus status,
      final List<ErrorMitigationTechnique> techniques,
      final int recommendedShots,
      final List<ErrorMitigationDiagnostic> diagnostics) {
    this.status = status;
    this.techniques = techniques;
    this.recommendedShots = recommendedShots;
    this.diagnostics = diagnostics;
  }

  public static ErrorMitigationPlan of(
      final ErrorMitigationStatus status,
      final List<ErrorMitigationTechnique> techniques,
      final int recommendedShots,
      final List<ErrorMitigationDiagnostic> diagnostics) {
    if (status == null) {
      throw new IllegalArgumentException("Error mitigation status must not be null.");
    }
    if (recommendedShots <= 0) {
      throw new IllegalArgumentException(
          "Error mitigation recommended shot count must be positive.");
    }
    return new ErrorMitigationPlan(
        status,
        List.copyOf(ErrorMitigationPlan.requireTechniques(techniques)),
        recommendedShots,
        List.copyOf(ErrorMitigationPlan.requireDiagnostics(diagnostics)));
  }

  public ErrorMitigationStatus status() {
    return this.status;
  }

  public List<ErrorMitigationTechnique> techniques() {
    return this.techniques;
  }

  public int recommendedShots() {
    return this.recommendedShots;
  }

  public List<ErrorMitigationDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public boolean hasErrors() {
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (this.diagnostics.get(i).error()) {
        return true;
      }
    }
    return false;
  }

  private static List<ErrorMitigationTechnique> requireTechniques(
      final List<ErrorMitigationTechnique> techniques) {
    if (techniques == null) {
      return List.of();
    }
    for (int i = 0; i < techniques.size(); ++i) {
      if (techniques.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Error mitigation plan technique list contains null value.");
    }
    return techniques;
  }

  private static List<ErrorMitigationDiagnostic> requireDiagnostics(
      final List<ErrorMitigationDiagnostic> diagnostics) {
    if (diagnostics == null) {
      return List.of();
    }
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (diagnostics.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Error mitigation diagnostic list contains null value.");
    }
    return diagnostics;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ErrorMitigationPlan)) {
      return false;
    }
    final ErrorMitigationPlan plan = (ErrorMitigationPlan) other;
    return this.recommendedShots == plan.recommendedShots
        && this.status == plan.status
        && Objects.equals(this.techniques, plan.techniques)
        && Objects.equals(this.diagnostics, plan.diagnostics);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.status);
    result = ChemistryHash.include(result, this.techniques);
    result = ChemistryHash.include(result, this.recommendedShots);
    result = ChemistryHash.include(result, this.diagnostics);
    return result;
  }
}
