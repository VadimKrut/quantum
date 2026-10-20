/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.diagnostic;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.stage.ChemistryWorkflowStage;

/**
 * Общее диагностическое сообщение, объединяющее ошибки всех chemistry-слоев.
 */
public final class ChemistryWorkflowDiagnostic {

  private final ChemistryWorkflowDiagnosticSeverity severity;
  private final ChemistryWorkflowDiagnosticCode code;
  private final ChemistryWorkflowStage stage;
  private final String sourceCode;
  private final String target;
  private final String message;

  private ChemistryWorkflowDiagnostic(
      final ChemistryWorkflowDiagnosticSeverity severity,
      final ChemistryWorkflowDiagnosticCode code,
      final ChemistryWorkflowStage stage,
      final String sourceCode,
      final String target,
      final String message) {
    this.severity = severity;
    this.code = code;
    this.stage = stage;
    this.sourceCode = sourceCode;
    this.target = target;
    this.message = message;
  }

  public static ChemistryWorkflowDiagnostic error(
      final ChemistryWorkflowDiagnosticCode code,
      final ChemistryWorkflowStage stage,
      final String sourceCode,
      final String target,
      final String message) {
    return ChemistryWorkflowDiagnostic.of(
        ChemistryWorkflowDiagnosticSeverity.ERROR, code, stage, sourceCode, target, message);
  }

  public static ChemistryWorkflowDiagnostic warning(
      final ChemistryWorkflowDiagnosticCode code,
      final ChemistryWorkflowStage stage,
      final String sourceCode,
      final String target,
      final String message) {
    return ChemistryWorkflowDiagnostic.of(
        ChemistryWorkflowDiagnosticSeverity.WARNING, code, stage, sourceCode, target, message);
  }

  public static ChemistryWorkflowDiagnostic of(
      final ChemistryWorkflowDiagnosticSeverity severity,
      final ChemistryWorkflowDiagnosticCode code,
      final ChemistryWorkflowStage stage,
      final String sourceCode,
      final String target,
      final String message) {
    if (severity == null) {
      throw new IllegalArgumentException(
          "Chemistry workflow diagnostic severity must not be null.");
    }
    if (code == null) {
      throw new IllegalArgumentException("Chemistry workflow diagnostic code must not be null.");
    }
    if (stage == null) {
      throw new IllegalArgumentException("Chemistry workflow diagnostic stage must not be null.");
    }
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException(
          "Chemistry workflow diagnostic message must not be blank.");
    }
    return new ChemistryWorkflowDiagnostic(
        severity,
        code,
        stage,
        sourceCode == null ? "" : sourceCode,
        target == null ? "" : target,
        message);
  }

  public ChemistryWorkflowDiagnosticSeverity severity() {
    return this.severity;
  }

  public ChemistryWorkflowDiagnosticCode code() {
    return this.code;
  }

  public ChemistryWorkflowStage stage() {
    return this.stage;
  }

  public String sourceCode() {
    return this.sourceCode;
  }

  public String target() {
    return this.target;
  }

  public String message() {
    return this.message;
  }

  public boolean error() {
    return this.severity == ChemistryWorkflowDiagnosticSeverity.ERROR;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryWorkflowDiagnostic)) {
      return false;
    }
    final ChemistryWorkflowDiagnostic diagnostic = (ChemistryWorkflowDiagnostic) other;
    return this.severity == diagnostic.severity
        && this.code == diagnostic.code
        && this.stage == diagnostic.stage
        && Objects.equals(this.sourceCode, diagnostic.sourceCode)
        && Objects.equals(this.target, diagnostic.target)
        && Objects.equals(this.message, diagnostic.message);
  }

  public int hashCode() {
    return Objects.hash(
        this.severity, this.code, this.stage, this.sourceCode, this.target, this.message);
  }

  public String toString() {
    return this.severity
        + " "
        + this.code
        + " "
        + this.stage
        + " "
        + this.sourceCode
        + " "
        + this.target
        + " "
        + this.message;
  }
}
