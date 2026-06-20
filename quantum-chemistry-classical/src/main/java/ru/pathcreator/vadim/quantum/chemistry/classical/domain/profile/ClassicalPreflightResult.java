/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;

/**
 * Результат проверки request под конкретный backend profile.
 */
public final class ClassicalPreflightResult {

  private final ClassicalPreflightStatus status;
  private final ClassicalBackendProfile profile;
  private final ClassicalDiagnosticSet diagnostics;

  private ClassicalPreflightResult(
      final ClassicalPreflightStatus status,
      final ClassicalBackendProfile profile,
      final ClassicalDiagnosticSet diagnostics
  ) {
    this.status = status;
    this.profile = profile;
    this.diagnostics = diagnostics;
  }

  public static ClassicalPreflightResult of(
      final ClassicalPreflightStatus status,
      final ClassicalBackendProfile profile,
      final ClassicalDiagnosticSet diagnostics
  ) {
    if (status == null) {
      throw new IllegalArgumentException("Classical preflight status must not be null.");
    }
    if (profile == null) {
      throw new IllegalArgumentException("Classical preflight profile must not be null.");
    }
    return new ClassicalPreflightResult(
        status,
        profile,
        diagnostics == null ? ClassicalDiagnosticSet.EMPTY : diagnostics);
  }

  public ClassicalPreflightStatus status() {
    return this.status;
  }

  public ClassicalBackendProfile profile() {
    return this.profile;
  }

  public ClassicalDiagnosticSet diagnostics() {
    return this.diagnostics;
  }

  public boolean supported() {
    return this.status == ClassicalPreflightStatus.SUPPORTED;
  }

  public boolean executableWithoutLoss() {
    return this.status == ClassicalPreflightStatus.SUPPORTED
        || this.status == ClassicalPreflightStatus.NEEDS_APPROXIMATION;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalPreflightResult)) {
      return false;
    }
    final ClassicalPreflightResult result = (ClassicalPreflightResult) other;
    return this.status == result.status
        && Objects.equals(this.profile, result.profile)
        && Objects.equals(this.diagnostics, result.diagnostics);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.status);
    result = ChemistryHash.include(result, this.profile);
    result = ChemistryHash.include(result, this.diagnostics);
    return result;
  }
}