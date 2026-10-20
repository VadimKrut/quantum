/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.parity;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Одна понятная причина расхождения между обычным расчетом и Quantum IR-подготовкой.
 */
public final class ChemistryComputationParityDiagnostic {

  private final ChemistryComputationParityDiagnosticCode code;
  private final ChemistryComputationParityDiagnosticSeverity severity;
  private final String message;

  private ChemistryComputationParityDiagnostic(
      final ChemistryComputationParityDiagnosticCode code,
      final ChemistryComputationParityDiagnosticSeverity severity,
      final String message) {
    this.code = code;
    this.severity = severity;
    this.message = message;
  }

  public static ChemistryComputationParityDiagnostic of(
      final ChemistryComputationParityDiagnosticCode code,
      final ChemistryComputationParityDiagnosticSeverity severity,
      final String message) {
    if (code == null) {
      throw new IllegalArgumentException(
          "Chemistry computation parity diagnostic code must not be null.");
    }
    if (severity == null) {
      throw new IllegalArgumentException(
          "Chemistry computation parity diagnostic severity must not be null.");
    }
    return new ChemistryComputationParityDiagnostic(
        code,
        severity,
        TextValue.requireText(message, "Chemistry computation parity diagnostic message"));
  }

  public ChemistryComputationParityDiagnosticCode code() {
    return this.code;
  }

  public ChemistryComputationParityDiagnosticSeverity severity() {
    return this.severity;
  }

  public String message() {
    return this.message;
  }

  public boolean error() {
    return this.severity == ChemistryComputationParityDiagnosticSeverity.ERROR;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryComputationParityDiagnostic)) {
      return false;
    }
    final ChemistryComputationParityDiagnostic diagnostic =
        (ChemistryComputationParityDiagnostic) other;
    return this.code == diagnostic.code
        && this.severity == diagnostic.severity
        && Objects.equals(this.message, diagnostic.message);
  }

  public int hashCode() {
    return Objects.hash(this.code, this.severity, this.message);
  }
}
