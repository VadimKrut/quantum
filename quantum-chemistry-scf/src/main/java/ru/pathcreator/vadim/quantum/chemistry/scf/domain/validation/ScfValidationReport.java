/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation;

import java.util.List;

/**
 * Итог проверки SCF-результата по физическим и численным инвариантам.
 */
public final class ScfValidationReport {

  private final boolean valid;
  private final double electronTraceDeviation;
  private final double densitySymmetryDeviation;
  private final double molecularOrbitalOrthonormalityDeviation;
  private final List<ScfDiagnostic> diagnostics;

  private ScfValidationReport(
      final boolean valid,
      final double electronTraceDeviation,
      final double densitySymmetryDeviation,
      final double molecularOrbitalOrthonormalityDeviation,
      final List<ScfDiagnostic> diagnostics) {
    this.valid = valid;
    this.electronTraceDeviation = electronTraceDeviation;
    this.densitySymmetryDeviation = densitySymmetryDeviation;
    this.molecularOrbitalOrthonormalityDeviation = molecularOrbitalOrthonormalityDeviation;
    this.diagnostics = diagnostics;
  }

  public static ScfValidationReport of(
      final double electronTraceDeviation,
      final double densitySymmetryDeviation,
      final double molecularOrbitalOrthonormalityDeviation,
      final List<ScfDiagnostic> diagnostics) {
    if (!Double.isFinite(electronTraceDeviation) || electronTraceDeviation < 0.0) {
      throw new IllegalArgumentException(
          "SCF electron trace deviation must be finite and non-negative.");
    }
    if (!Double.isFinite(densitySymmetryDeviation) || densitySymmetryDeviation < 0.0) {
      throw new IllegalArgumentException(
          "SCF density symmetry deviation must be finite and non-negative.");
    }
    if (!Double.isFinite(molecularOrbitalOrthonormalityDeviation)
        || molecularOrbitalOrthonormalityDeviation < 0.0) {
      throw new IllegalArgumentException(
          "SCF MO orthonormality deviation must be finite and non-negative.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("SCF validation diagnostics must not be null.");
    }
    final List<ScfDiagnostic> checkedDiagnostics = List.copyOf(diagnostics);
    for (int i = 0; i < checkedDiagnostics.size(); ++i) {
      if (checkedDiagnostics.get(i) == null) {
        throw new IllegalArgumentException("SCF validation diagnostics must not contain null.");
      }
    }
    boolean hasError = false;
    for (int i = 0; i < checkedDiagnostics.size(); ++i) {
      if (checkedDiagnostics.get(i).severity() == ScfDiagnosticSeverity.ERROR) {
        hasError = true;
        break;
      }
    }
    return new ScfValidationReport(
        !hasError,
        electronTraceDeviation,
        densitySymmetryDeviation,
        molecularOrbitalOrthonormalityDeviation,
        checkedDiagnostics);
  }

  public boolean valid() {
    return this.valid;
  }

  public double electronTraceDeviation() {
    return this.electronTraceDeviation;
  }

  public double densitySymmetryDeviation() {
    return this.densitySymmetryDeviation;
  }

  public double molecularOrbitalOrthonormalityDeviation() {
    return this.molecularOrbitalOrthonormalityDeviation;
  }

  public List<ScfDiagnostic> diagnostics() {
    return this.diagnostics;
  }
}
