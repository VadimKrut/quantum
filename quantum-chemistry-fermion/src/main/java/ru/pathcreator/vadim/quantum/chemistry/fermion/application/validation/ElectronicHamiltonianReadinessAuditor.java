/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.fermion.application.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.OneElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.TwoElectronIntegral;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianAuditOptions;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianAuditReport;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianDiagnosticSeverity;

/** Проверяет, что electronic Hamiltonian физически согласован перед fermion/qubit mapping. */
public final class ElectronicHamiltonianReadinessAuditor {

  public FermionHamiltonianAuditReport audit(final ElectronicHamiltonian hamiltonian) {
    return this.audit(hamiltonian, FermionHamiltonianAuditOptions.DEFAULT);
  }

  public FermionHamiltonianAuditReport audit(
      final ElectronicHamiltonian hamiltonian, final FermionHamiltonianAuditOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian audit target must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("Electronic Hamiltonian audit options must not be null.");
    }
    final ArrayList<FermionHamiltonianDiagnostic> diagnostics =
        new ArrayList<FermionHamiltonianDiagnostic>();
    final int electronCount = hamiltonian.electronCount();
    final int spatialOrbitalCount = hamiltonian.spatialOrbitalCount();
    final int spinOrbitalCount = hamiltonian.spinOrbitalCount();
    this.auditActiveSpace(electronCount, spatialOrbitalCount, spinOrbitalCount, diagnostics);
    final double maxOneElectronMagnitude =
        this.auditOneElectronIntegrals(hamiltonian, options, diagnostics);
    final TwoElectronAudit twoElectronAudit =
        this.auditTwoElectronIntegrals(hamiltonian, options, diagnostics);
    if (hamiltonian.emptyElectronicTerms()) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.WARNING,
              FermionHamiltonianDiagnosticCode.EMPTY_ELECTRONIC_TERMS,
              "hamiltonian.integrals",
              "Hamiltonian contains only the constant nuclear repulsion term."));
    }
    return FermionHamiltonianAuditReport.of(
        electronCount,
        spatialOrbitalCount,
        spinOrbitalCount,
        hamiltonian.oneElectronIntegrals().size(),
        hamiltonian.twoElectronIntegrals().size(),
        twoElectronAudit.missingCoulombDiagonalCount,
        maxOneElectronMagnitude,
        twoElectronAudit.maxMagnitude,
        twoElectronAudit.minCoulombDiagonal,
        diagnostics);
  }

  private void auditActiveSpace(
      final int electronCount,
      final int spatialOrbitalCount,
      final int spinOrbitalCount,
      final ArrayList<FermionHamiltonianDiagnostic> diagnostics) {
    if (spinOrbitalCount != Math.multiplyExact(spatialOrbitalCount, 2)) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.ERROR,
              FermionHamiltonianDiagnosticCode.SPIN_ORBITAL_COUNT_MISMATCH,
              "activeSpace.spinOrbitalCount",
              "Spin orbital count must be exactly two times the spatial orbital count."));
    }
    if (electronCount > spinOrbitalCount) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.ERROR,
              FermionHamiltonianDiagnosticCode.ELECTRON_SECTOR_EXCEEDS_SPIN_ORBITALS,
              "activeSpace.electronCount",
              "Electron count exceeds the available spin orbital capacity."));
    }
    if (electronCount % 2 != 0) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.INFO,
              FermionHamiltonianDiagnosticCode.ODD_ELECTRON_SECTOR,
              "activeSpace.electronCount",
              "Odd electron sector is open-shell and requires spin-aware downstream interpretation."));
    }
  }

  private double auditOneElectronIntegrals(
      final ElectronicHamiltonian hamiltonian,
      final FermionHamiltonianAuditOptions options,
      final ArrayList<FermionHamiltonianDiagnostic> diagnostics) {
    final List<OneElectronIntegral> integrals = hamiltonian.oneElectronIntegrals();
    if (integrals.isEmpty()) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.WARNING,
              FermionHamiltonianDiagnosticCode.MISSING_ONE_ELECTRON_TERMS,
              "hamiltonian.oneElectronIntegrals",
              "Hamiltonian does not contain one-electron integrals."));
    }
    double maxMagnitude = 0.0;
    final HashMap<Long, Double> symmetrySlots =
        new HashMap<Long, Double>(Math.max(16, integrals.size() * 2));
    for (int i = 0; i < integrals.size(); ++i) {
      final OneElectronIntegral integral = integrals.get(i);
      this.auditOneElectronIntegralIndex(hamiltonian, integral, i, diagnostics);
      this.auditOneElectronSymmetrySlot(integral, i, symmetrySlots, options, diagnostics);
      maxMagnitude = Math.max(maxMagnitude, Math.abs(integral.value()));
    }
    if (maxMagnitude > options.largeIntegralMagnitude()) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.WARNING,
              FermionHamiltonianDiagnosticCode.LARGE_INTEGRAL_MAGNITUDE,
              "hamiltonian.oneElectronIntegrals",
              "One-electron integral magnitude is unusually large."));
    }
    return maxMagnitude;
  }

  private TwoElectronAudit auditTwoElectronIntegrals(
      final ElectronicHamiltonian hamiltonian,
      final FermionHamiltonianAuditOptions options,
      final ArrayList<FermionHamiltonianDiagnostic> diagnostics) {
    final List<TwoElectronIntegral> integrals = hamiltonian.twoElectronIntegrals();
    if (integrals.isEmpty()) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.WARNING,
              FermionHamiltonianDiagnosticCode.MISSING_TWO_ELECTRON_TERMS,
              "hamiltonian.twoElectronIntegrals",
              "Hamiltonian does not contain two-electron integrals."));
    }
    final boolean[] coulombDiagonal = new boolean[hamiltonian.spatialOrbitalCount()];
    final HashMap<Long, Double> symmetrySlots =
        new HashMap<Long, Double>(Math.max(16, integrals.size() * 2));
    double maxMagnitude = 0.0;
    double minCoulombDiagonal = Double.POSITIVE_INFINITY;
    for (int i = 0; i < integrals.size(); ++i) {
      final TwoElectronIntegral integral = integrals.get(i);
      this.auditTwoElectronIntegralIndex(hamiltonian, integral, i, diagnostics);
      this.auditTwoElectronSymmetrySlot(integral, i, symmetrySlots, options, diagnostics);
      final double magnitude = Math.abs(integral.value());
      maxMagnitude = Math.max(maxMagnitude, magnitude);
      if (this.coulombDiagonal(integral)) {
        final int orbital = integral.firstOrbital().value();
        coulombDiagonal[orbital] = true;
        minCoulombDiagonal = Math.min(minCoulombDiagonal, integral.value());
        if (integral.value() < -options.negativeCoulombTolerance()) {
          diagnostics.add(
              ElectronicHamiltonianReadinessAuditor.diagnostic(
                  FermionHamiltonianDiagnosticSeverity.ERROR,
                  FermionHamiltonianDiagnosticCode.NEGATIVE_COULOMB_DIAGONAL,
                  "hamiltonian.twoElectronIntegrals[" + i + "]",
                  "Coulomb self-repulsion integral (pp|pp) must not be negative."));
        }
      }
    }
    if (maxMagnitude > options.largeIntegralMagnitude()) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.WARNING,
              FermionHamiltonianDiagnosticCode.LARGE_INTEGRAL_MAGNITUDE,
              "hamiltonian.twoElectronIntegrals",
              "Two-electron integral magnitude is unusually large."));
    }
    final int missingCoulombDiagonalCount =
        ElectronicHamiltonianReadinessAuditor.missingCount(coulombDiagonal);
    if (missingCoulombDiagonalCount > 0 && !integrals.isEmpty()) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.WARNING,
              FermionHamiltonianDiagnosticCode.MISSING_COULOMB_DIAGONALS,
              "hamiltonian.twoElectronIntegrals",
              "Some spatial orbitals do not have explicit (pp|pp) Coulomb diagonal terms."));
    }
    return new TwoElectronAudit(
        maxMagnitude,
        minCoulombDiagonal == Double.POSITIVE_INFINITY ? 0.0 : minCoulombDiagonal,
        missingCoulombDiagonalCount);
  }

  private void auditOneElectronIntegralIndex(
      final ElectronicHamiltonian hamiltonian,
      final OneElectronIntegral integral,
      final int index,
      final ArrayList<FermionHamiltonianDiagnostic> diagnostics) {
    if (!ElectronicHamiltonianReadinessAuditor.orbitalWithin(
            integral.firstOrbital().value(), hamiltonian.spatialOrbitalCount())
        || !ElectronicHamiltonianReadinessAuditor.orbitalWithin(
            integral.secondOrbital().value(), hamiltonian.spatialOrbitalCount())) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.ERROR,
              FermionHamiltonianDiagnosticCode.ORBITAL_INDEX_OUT_OF_RANGE,
              "hamiltonian.oneElectronIntegrals[" + index + "]",
              "One-electron integral references an orbital outside the active spatial orbital range."));
    }
  }

  private void auditTwoElectronIntegralIndex(
      final ElectronicHamiltonian hamiltonian,
      final TwoElectronIntegral integral,
      final int index,
      final ArrayList<FermionHamiltonianDiagnostic> diagnostics) {
    if (!ElectronicHamiltonianReadinessAuditor.orbitalWithin(
            integral.firstOrbital().value(), hamiltonian.spatialOrbitalCount())
        || !ElectronicHamiltonianReadinessAuditor.orbitalWithin(
            integral.secondOrbital().value(), hamiltonian.spatialOrbitalCount())
        || !ElectronicHamiltonianReadinessAuditor.orbitalWithin(
            integral.thirdOrbital().value(), hamiltonian.spatialOrbitalCount())
        || !ElectronicHamiltonianReadinessAuditor.orbitalWithin(
            integral.fourthOrbital().value(), hamiltonian.spatialOrbitalCount())) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.ERROR,
              FermionHamiltonianDiagnosticCode.ORBITAL_INDEX_OUT_OF_RANGE,
              "hamiltonian.twoElectronIntegrals[" + index + "]",
              "Two-electron integral references an orbital outside the active spatial orbital range."));
    }
  }

  private void auditOneElectronSymmetrySlot(
      final OneElectronIntegral integral,
      final int index,
      final Map<Long, Double> symmetrySlots,
      final FermionHamiltonianAuditOptions options,
      final ArrayList<FermionHamiltonianDiagnostic> diagnostics) {
    final long key = integral.symmetrySlotKey();
    final Double existing = symmetrySlots.putIfAbsent(key, integral.value());
    if (existing == null) {
      return;
    }
    diagnostics.add(
        ElectronicHamiltonianReadinessAuditor.diagnostic(
            FermionHamiltonianDiagnosticSeverity.WARNING,
            FermionHamiltonianDiagnosticCode.DUPLICATE_ONE_ELECTRON_SYMMETRY_SLOT,
            "hamiltonian.oneElectronIntegrals[" + index + "]",
            "One-electron integral repeats an already supplied Hermitian symmetry slot."));
    if (Math.abs(existing.doubleValue() - integral.value()) > options.symmetryValueTolerance()) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.ERROR,
              FermionHamiltonianDiagnosticCode.INCONSISTENT_ONE_ELECTRON_SYMMETRY_VALUE,
              "hamiltonian.oneElectronIntegrals[" + index + "]",
              "One-electron Hermitian symmetry-equivalent values differ beyond tolerance."));
    }
  }

  private void auditTwoElectronSymmetrySlot(
      final TwoElectronIntegral integral,
      final int index,
      final Map<Long, Double> symmetrySlots,
      final FermionHamiltonianAuditOptions options,
      final ArrayList<FermionHamiltonianDiagnostic> diagnostics) {
    final long key = integral.symmetrySlotKey();
    final Double existing = symmetrySlots.putIfAbsent(key, integral.value());
    if (existing == null) {
      return;
    }
    diagnostics.add(
        ElectronicHamiltonianReadinessAuditor.diagnostic(
            FermionHamiltonianDiagnosticSeverity.WARNING,
            FermionHamiltonianDiagnosticCode.DUPLICATE_TWO_ELECTRON_SYMMETRY_SLOT,
            "hamiltonian.twoElectronIntegrals[" + index + "]",
            "Two-electron integral repeats an already supplied ERI symmetry slot."));
    if (Math.abs(existing.doubleValue() - integral.value()) > options.symmetryValueTolerance()) {
      diagnostics.add(
          ElectronicHamiltonianReadinessAuditor.diagnostic(
              FermionHamiltonianDiagnosticSeverity.ERROR,
              FermionHamiltonianDiagnosticCode.INCONSISTENT_TWO_ELECTRON_SYMMETRY_VALUE,
              "hamiltonian.twoElectronIntegrals[" + index + "]",
              "Two-electron ERI symmetry-equivalent values differ beyond tolerance."));
    }
  }

  private static boolean orbitalWithin(final int orbital, final int orbitalCount) {
    return orbital >= 0 && orbital < orbitalCount;
  }

  private boolean coulombDiagonal(final TwoElectronIntegral integral) {
    final int first = integral.firstOrbital().value();
    return integral.secondOrbital().value() == first
        && integral.thirdOrbital().value() == first
        && integral.fourthOrbital().value() == first;
  }

  private static int missingCount(final boolean[] values) {
    int count = 0;
    for (int i = 0; i < values.length; ++i) {
      if (!values[i]) {
        ++count;
      }
    }
    return count;
  }

  private static FermionHamiltonianDiagnostic diagnostic(
      final FermionHamiltonianDiagnosticSeverity severity,
      final FermionHamiltonianDiagnosticCode code,
      final String target,
      final String message) {
    return FermionHamiltonianDiagnostic.of(severity, code, target, message);
  }

  private static final class TwoElectronAudit {

    private final double maxMagnitude;
    private final double minCoulombDiagonal;
    private final int missingCoulombDiagonalCount;

    private TwoElectronAudit(
        final double maxMagnitude,
        final double minCoulombDiagonal,
        final int missingCoulombDiagonalCount) {
      this.maxMagnitude = maxMagnitude;
      this.minCoulombDiagonal = minCoulombDiagonal;
      this.missingCoulombDiagonalCount = missingCoulombDiagonalCount;
    }
  }
}
