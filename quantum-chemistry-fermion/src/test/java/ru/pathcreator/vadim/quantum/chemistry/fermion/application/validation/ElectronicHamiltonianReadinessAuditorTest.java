/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianAuditReport;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianDiagnosticSeverity;

public final class ElectronicHamiltonianReadinessAuditorTest {

  @Test
  public void canonicalHydrogenHamiltonianIsReadyForFermionAndQubitMapping() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(2, 2),
            0.713753993687618,
            List.of(
                OneElectronIntegral.of(0, 0, -1.252463573565),
                OneElectronIntegral.of(1, 1, -0.475948715221),
                OneElectronIntegral.of(0, 1, 0.03451219)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, 0.674488766357),
                TwoElectronIntegral.of(0, 0, 1, 1, 0.663468096424),
                TwoElectronIntegral.of(0, 1, 0, 1, 0.181288808211),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.697393767423)));
    final FermionHamiltonianAuditReport report =
        new ElectronicHamiltonianReadinessAuditor().audit(hamiltonian);

    assertTrue(report.readyForFermionMapping());
    assertEquals(2, report.electronCount());
    assertEquals(2, report.spatialOrbitalCount());
    assertEquals(4, report.spinOrbitalCount());
    assertEquals(0, report.missingCoulombDiagonalCount());
    assertTrue(report.maxOneElectronMagnitude() > 1.0);
    assertTrue(report.maxTwoElectronMagnitude() > 0.6);
    assertFalse(
        report.diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.severity() == FermionHamiltonianDiagnosticSeverity.ERROR));
  }

  @Test
  public void auditRejectsNegativeCoulombDiagonalAsPhysicallyInvalidHamiltonian() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(2, 2),
            0.0,
            List.of(OneElectronIntegral.of(0, 0, -1.0)),
            List.of(
                TwoElectronIntegral.of(0, 0, 0, 0, -0.25),
                TwoElectronIntegral.of(1, 1, 1, 1, 0.5)));
    final FermionHamiltonianAuditReport report =
        new ElectronicHamiltonianReadinessAuditor().audit(hamiltonian);

    assertFalse(report.readyForFermionMapping());
    assertEquals(-0.25, report.minCoulombDiagonal(), 1.0E-12);
    assertTrue(
        report.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == FermionHamiltonianDiagnosticCode.NEGATIVE_COULOMB_DIAGONAL
                        && diagnostic.severity() == FermionHamiltonianDiagnosticSeverity.ERROR));
  }

  @Test
  public void auditReportsOpenShellAndMissingTwoElectronCompletenessWithoutUsingReferenceLookup() {
    final ElectronicHamiltonian hamiltonian =
        ElectronicHamiltonian.of(
            ActiveSpace.of(1, 2), 0.0, List.of(OneElectronIntegral.of(0, 0, -0.5)), List.of());
    final FermionHamiltonianAuditReport report =
        new ElectronicHamiltonianReadinessAuditor().audit(hamiltonian);

    assertTrue(report.readyForFermionMapping());
    assertEquals(1, report.electronCount());
    assertEquals(2, report.spatialOrbitalCount());
    assertEquals(0, report.twoElectronIntegralCount());
    assertTrue(
        report.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == FermionHamiltonianDiagnosticCode.ODD_ELECTRON_SECTOR));
    assertTrue(
        report.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code()
                        == FermionHamiltonianDiagnosticCode.MISSING_TWO_ELECTRON_TERMS));
  }

  @Test
  public void domainRejectsOutOfRangeOrbitalIndexesBeforeAuditAndMapping() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ElectronicHamiltonian.of(
                    ActiveSpace.of(2, 2),
                    0.0,
                    List.of(OneElectronIntegral.of(2, 0, -0.5)),
                    List.of(TwoElectronIntegral.of(0, 1, 1, 2, 0.1))));

    assertEquals(
        "Spatial orbital index exceeds active space orbital count.", exception.getMessage());
  }

  @Test
  public void domainRejectsDuplicateOneElectronHermitianSymmetrySlot() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ElectronicHamiltonian.of(
                    ActiveSpace.of(2, 2),
                    0.0,
                    List.of(OneElectronIntegral.of(0, 1, 0.25), OneElectronIntegral.of(1, 0, 0.35)),
                    List.of(
                        TwoElectronIntegral.of(0, 0, 0, 0, 0.4),
                        TwoElectronIntegral.of(1, 1, 1, 1, 0.5))));

    assertEquals(
        "One-electron integrals contain duplicate Hermitian slot.", exception.getMessage());
  }

  @Test
  public void domainRejectsDuplicateTwoElectronPermutationalSymmetrySlot() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ElectronicHamiltonian.of(
                    ActiveSpace.of(2, 2),
                    0.0,
                    List.of(OneElectronIntegral.of(0, 0, -0.5)),
                    List.of(
                        TwoElectronIntegral.of(0, 1, 0, 1, 0.2),
                        TwoElectronIntegral.of(1, 0, 0, 1, 0.31),
                        TwoElectronIntegral.of(0, 0, 0, 0, 0.4),
                        TwoElectronIntegral.of(1, 1, 1, 1, 0.5))));

    assertEquals(
        "Two-electron integrals contain duplicate permutational slot.", exception.getMessage());
  }
}
