/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Одна диагностическая запись classical-пайплайна.
 */
public final class ClassicalDiagnostic {

  private final ClassicalDiagnosticSeverity severity;
  private final ClassicalDiagnosticCode code;
  private final ClassicalDiagnosticTarget target;
  private final String message;

  private ClassicalDiagnostic(
      final ClassicalDiagnosticSeverity severity,
      final ClassicalDiagnosticCode code,
      final ClassicalDiagnosticTarget target,
      final String message
  ) {
    this.severity = severity;
    this.code = code;
    this.target = target;
    this.message = message;
  }

  public static ClassicalDiagnostic info(
      final ClassicalDiagnosticCode code,
      final ClassicalDiagnosticTarget target,
      final String message
  ) {
    return ClassicalDiagnostic.of(
        ClassicalDiagnosticSeverity.INFO,
        code,
        target,
        message);
  }

  public static ClassicalDiagnostic warning(
      final ClassicalDiagnosticCode code,
      final ClassicalDiagnosticTarget target,
      final String message
  ) {
    return ClassicalDiagnostic.of(
        ClassicalDiagnosticSeverity.WARNING,
        code,
        target,
        message);
  }

  public static ClassicalDiagnostic error(
      final ClassicalDiagnosticCode code,
      final ClassicalDiagnosticTarget target,
      final String message
  ) {
    return ClassicalDiagnostic.of(
        ClassicalDiagnosticSeverity.ERROR,
        code,
        target,
        message);
  }

  public static ClassicalDiagnostic of(
      final ClassicalDiagnosticSeverity severity,
      final ClassicalDiagnosticCode code,
      final ClassicalDiagnosticTarget target,
      final String message
  ) {
    if (severity == null) {
      throw new IllegalArgumentException("Classical diagnostic severity must not be null.");
    }
    if (code == null) {
      throw new IllegalArgumentException("Classical diagnostic code must not be null.");
    }
    final ClassicalDiagnosticTarget checkedTarget =
        target == null ? ClassicalDiagnosticTarget.REQUEST : target;
    return new ClassicalDiagnostic(
        severity,
        code,
        checkedTarget,
        TextValue.requireText(
            message,
            "Classical diagnostic message"));
  }

  public ClassicalDiagnosticSeverity severity() {
    return this.severity;
  }

  public ClassicalDiagnosticCode code() {
    return this.code;
  }

  public ClassicalDiagnosticTarget target() {
    return this.target;
  }

  public String message() {
    return this.message;
  }

  public boolean error() {
    return this.severity == ClassicalDiagnosticSeverity.ERROR;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalDiagnostic)) {
      return false;
    }
    final ClassicalDiagnostic diagnostic = (ClassicalDiagnostic) other;
    return this.severity == diagnostic.severity
        && this.code == diagnostic.code
        && Objects.equals(this.target, diagnostic.target)
        && Objects.equals(this.message, diagnostic.message);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.severity);
    result = ChemistryHash.include(result, this.code);
    result = ChemistryHash.include(result, this.target);
    result = ChemistryHash.include(result, this.message);
    return result;
  }
}