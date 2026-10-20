/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation;

/** Одна структурированная диагностика Hamiltonian audit. */
public final class FermionHamiltonianDiagnostic {

  private final FermionHamiltonianDiagnosticSeverity severity;
  private final FermionHamiltonianDiagnosticCode code;
  private final String target;
  private final String message;

  private FermionHamiltonianDiagnostic(
      final FermionHamiltonianDiagnosticSeverity severity,
      final FermionHamiltonianDiagnosticCode code,
      final String target,
      final String message) {
    this.severity = severity;
    this.code = code;
    this.target = target;
    this.message = message;
  }

  public static FermionHamiltonianDiagnostic of(
      final FermionHamiltonianDiagnosticSeverity severity,
      final FermionHamiltonianDiagnosticCode code,
      final String target,
      final String message) {
    if (severity == null) {
      throw new IllegalArgumentException("Hamiltonian diagnostic severity must not be null.");
    }
    if (code == null) {
      throw new IllegalArgumentException("Hamiltonian diagnostic code must not be null.");
    }
    if (target == null || target.isBlank()) {
      throw new IllegalArgumentException("Hamiltonian diagnostic target must not be blank.");
    }
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Hamiltonian diagnostic message must not be blank.");
    }
    return new FermionHamiltonianDiagnostic(severity, code, target, message);
  }

  public FermionHamiltonianDiagnosticSeverity severity() {
    return this.severity;
  }

  public FermionHamiltonianDiagnosticCode code() {
    return this.code;
  }

  public String target() {
    return this.target;
  }

  public String message() {
    return this.message;
  }
}
