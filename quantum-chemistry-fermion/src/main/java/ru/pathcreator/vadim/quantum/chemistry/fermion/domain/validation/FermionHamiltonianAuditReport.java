/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation;

import java.util.List;

/** Итоговый отчет аудита Hamiltonian перед построением fermion/qubit operators. */
public final class FermionHamiltonianAuditReport {

  private final int electronCount;
  private final int spatialOrbitalCount;
  private final int spinOrbitalCount;
  private final int oneElectronIntegralCount;
  private final int twoElectronIntegralCount;
  private final int missingCoulombDiagonalCount;
  private final double maxOneElectronMagnitude;
  private final double maxTwoElectronMagnitude;
  private final double minCoulombDiagonal;
  private final List<FermionHamiltonianDiagnostic> diagnostics;

  private FermionHamiltonianAuditReport(
      final int electronCount,
      final int spatialOrbitalCount,
      final int spinOrbitalCount,
      final int oneElectronIntegralCount,
      final int twoElectronIntegralCount,
      final int missingCoulombDiagonalCount,
      final double maxOneElectronMagnitude,
      final double maxTwoElectronMagnitude,
      final double minCoulombDiagonal,
      final List<FermionHamiltonianDiagnostic> diagnostics) {
    this.electronCount = electronCount;
    this.spatialOrbitalCount = spatialOrbitalCount;
    this.spinOrbitalCount = spinOrbitalCount;
    this.oneElectronIntegralCount = oneElectronIntegralCount;
    this.twoElectronIntegralCount = twoElectronIntegralCount;
    this.missingCoulombDiagonalCount = missingCoulombDiagonalCount;
    this.maxOneElectronMagnitude = maxOneElectronMagnitude;
    this.maxTwoElectronMagnitude = maxTwoElectronMagnitude;
    this.minCoulombDiagonal = minCoulombDiagonal;
    this.diagnostics = diagnostics;
  }

  public static FermionHamiltonianAuditReport of(
      final int electronCount,
      final int spatialOrbitalCount,
      final int spinOrbitalCount,
      final int oneElectronIntegralCount,
      final int twoElectronIntegralCount,
      final int missingCoulombDiagonalCount,
      final double maxOneElectronMagnitude,
      final double maxTwoElectronMagnitude,
      final double minCoulombDiagonal,
      final List<FermionHamiltonianDiagnostic> diagnostics) {
    if (diagnostics == null) {
      throw new IllegalArgumentException("Hamiltonian audit diagnostics must not be null.");
    }
    return new FermionHamiltonianAuditReport(
        electronCount,
        spatialOrbitalCount,
        spinOrbitalCount,
        oneElectronIntegralCount,
        twoElectronIntegralCount,
        missingCoulombDiagonalCount,
        maxOneElectronMagnitude,
        maxTwoElectronMagnitude,
        minCoulombDiagonal,
        List.copyOf(diagnostics));
  }

  public boolean readyForFermionMapping() {
    for (int i = 0; i < this.diagnostics.size(); ++i) {
      if (this.diagnostics.get(i).severity() == FermionHamiltonianDiagnosticSeverity.ERROR) {
        return false;
      }
    }
    return true;
  }

  public int electronCount() {
    return this.electronCount;
  }

  public int spatialOrbitalCount() {
    return this.spatialOrbitalCount;
  }

  public int spinOrbitalCount() {
    return this.spinOrbitalCount;
  }

  public int oneElectronIntegralCount() {
    return this.oneElectronIntegralCount;
  }

  public int twoElectronIntegralCount() {
    return this.twoElectronIntegralCount;
  }

  public int missingCoulombDiagonalCount() {
    return this.missingCoulombDiagonalCount;
  }

  public double maxOneElectronMagnitude() {
    return this.maxOneElectronMagnitude;
  }

  public double maxTwoElectronMagnitude() {
    return this.maxTwoElectronMagnitude;
  }

  public double minCoulombDiagonal() {
    return this.minCoulombDiagonal;
  }

  public List<FermionHamiltonianDiagnostic> diagnostics() {
    return this.diagnostics;
  }

  public int diagnosticCount() {
    return this.diagnostics.size();
  }
}
