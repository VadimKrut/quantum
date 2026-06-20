/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.domain.validation;

import java.util.Objects;

/**
 * Одна диагностика drawing-документа с кодом, серьезностью и привязкой к объекту.
 */
public final class DrawingDiagnostic {

  private final DrawingDiagnosticSeverity severity;
  private final DrawingDiagnosticCode code;
  private final String targetId;
  private final String message;

  private DrawingDiagnostic(
      final DrawingDiagnosticSeverity severity,
      final DrawingDiagnosticCode code,
      final String targetId,
      final String message
  ) {
    this.severity = severity;
    this.code = code;
    this.targetId = targetId;
    this.message = message;
  }

  public static DrawingDiagnostic of(
      final DrawingDiagnosticSeverity severity,
      final DrawingDiagnosticCode code,
      final String targetId,
      final String message
  ) {
    if (severity == null) {
      throw new IllegalArgumentException("Drawing diagnostic severity must not be null.");
    }
    if (code == null) {
      throw new IllegalArgumentException("Drawing diagnostic code must not be null.");
    }
    if (message == null || message.trim().isEmpty()) {
      throw new IllegalArgumentException("Drawing diagnostic message must not be blank.");
    }
    return new DrawingDiagnostic(
        severity,
        code,
        targetId == null ? "" : targetId.trim(),
        message.trim()
    );
  }

  public DrawingDiagnosticSeverity severity() {
    return this.severity;
  }

  public DrawingDiagnosticCode code() {
    return this.code;
  }

  public String targetId() {
    return this.targetId;
  }

  public String message() {
    return this.message;
  }

  public boolean error() {
    return this.severity == DrawingDiagnosticSeverity.ERROR;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DrawingDiagnostic)) {
      return false;
    }
    final DrawingDiagnostic diagnostic = (DrawingDiagnostic) other;
    return this.severity == diagnostic.severity
        && this.code == diagnostic.code
        && Objects.equals(this.targetId, diagnostic.targetId)
        && Objects.equals(this.message, diagnostic.message);
  }

  public int hashCode() {
    int result = this.severity.hashCode();
    result = 31 * result + this.code.hashCode();
    result = 31 * result + this.targetId.hashCode();
    result = 31 * result + this.message.hashCode();
    return result;
  }
}