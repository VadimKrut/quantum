/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Диагностическое сообщение chemistry-core с severity, стабильным кодом и optional target. */
public final class ChemistryDiagnostic {

  private final ChemistryDiagnosticSeverity severity;
  private final ChemistryDiagnosticCode code;
  private final String message;
  private final ChemistryDiagnosticTarget target;

  private ChemistryDiagnostic(
      final ChemistryDiagnosticSeverity severity,
      final ChemistryDiagnosticCode code,
      final String message,
      final ChemistryDiagnosticTarget target) {
    this.severity = severity;
    this.code = code;
    this.message = message;
    this.target = target;
  }

  public static ChemistryDiagnostic of(
      final ChemistryDiagnosticSeverity severity,
      final ChemistryDiagnosticCode code,
      final String message) {
    return ChemistryDiagnostic.of(severity, code, message, null);
  }

  public static ChemistryDiagnostic of(
      final ChemistryDiagnosticSeverity severity,
      final ChemistryDiagnosticCode code,
      final String message,
      final ChemistryDiagnosticTarget target) {
    if (severity == null) {
      throw new IllegalArgumentException("Diagnostic severity must not be null.");
    }
    if (code == null) {
      throw new IllegalArgumentException("Diagnostic code must not be null.");
    }
    return new ChemistryDiagnostic(
        severity, code, TextValue.requireText(message, "Diagnostic message"), target);
  }

  public ChemistryDiagnosticSeverity severity() {
    return this.severity;
  }

  public ChemistryDiagnosticCode code() {
    return this.code;
  }

  public String message() {
    return this.message;
  }

  public ChemistryDiagnosticTarget target() {
    return this.target;
  }

  public boolean hasTarget() {
    return this.target != null;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryDiagnostic)) {
      return false;
    }
    final ChemistryDiagnostic diagnostic = (ChemistryDiagnostic) other;
    return this.severity == diagnostic.severity
        && this.code == diagnostic.code
        && Objects.equals(this.message, diagnostic.message)
        && Objects.equals(this.target, diagnostic.target);
  }

  public int hashCode() {
    int result = this.severity.hashCode();
    result = 31 * result + this.code.hashCode();
    result = 31 * result + this.message.hashCode();
    result = 31 * result + Objects.hashCode(this.target);
    return result;
  }
}