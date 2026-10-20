/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation;

/**
 * Одна диагностика проверки SCF-результата с измеренной ошибкой инварианта.
 */
public final class ScfDiagnostic {

  private final ScfDiagnosticSeverity severity;
  private final ScfDiagnosticCode code;
  private final String message;
  private final double observedDeviation;
  private final double tolerance;

  private ScfDiagnostic(
      final ScfDiagnosticSeverity severity,
      final ScfDiagnosticCode code,
      final String message,
      final double observedDeviation,
      final double tolerance) {
    this.severity = severity;
    this.code = code;
    this.message = message;
    this.observedDeviation = observedDeviation;
    this.tolerance = tolerance;
  }

  public static ScfDiagnostic of(
      final ScfDiagnosticSeverity severity,
      final ScfDiagnosticCode code,
      final String message,
      final double observedDeviation,
      final double tolerance) {
    if (severity == null) {
      throw new IllegalArgumentException("SCF diagnostic severity must not be null.");
    }
    if (code == null) {
      throw new IllegalArgumentException("SCF diagnostic code must not be null.");
    }
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("SCF diagnostic message must not be blank.");
    }
    if (!Double.isFinite(observedDeviation) || observedDeviation < 0.0) {
      throw new IllegalArgumentException(
          "SCF diagnostic observed deviation must be finite and non-negative.");
    }
    if (!Double.isFinite(tolerance) || tolerance < 0.0) {
      throw new IllegalArgumentException(
          "SCF diagnostic tolerance must be finite and non-negative.");
    }
    return new ScfDiagnostic(severity, code, message, observedDeviation, tolerance);
  }

  public ScfDiagnosticSeverity severity() {
    return this.severity;
  }

  public ScfDiagnosticCode code() {
    return this.code;
  }

  public String message() {
    return this.message;
  }

  public double observedDeviation() {
    return this.observedDeviation;
  }

  public double tolerance() {
    return this.tolerance;
  }
}
