/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.convergence.ScfIterationRecord;

/**
 * Restricted Hartree-Fock result with MO coefficients and orbital energies.
 */
public final class RhfResult {

  private final boolean converged;
  private final int iterations;
  private final double electronicEnergy;
  private final double totalEnergy;
  private final double[] orbitalEnergies;
  private final double[] molecularOrbitalCoefficients;
  private final double[] densityMatrix;
  private final List<ScfIterationRecord> iterationHistory;

  private RhfResult(
      final boolean converged,
      final int iterations,
      final double electronicEnergy,
      final double totalEnergy,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] densityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    this.converged = converged;
    this.iterations = iterations;
    this.electronicEnergy = electronicEnergy;
    this.totalEnergy = totalEnergy;
    this.orbitalEnergies = orbitalEnergies;
    this.molecularOrbitalCoefficients = molecularOrbitalCoefficients;
    this.densityMatrix = densityMatrix;
    this.iterationHistory = iterationHistory;
  }

  public static RhfResult of(
      final boolean converged,
      final int iterations,
      final double electronicEnergy,
      final double totalEnergy,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] densityMatrix) {
    return RhfResult.of(
        converged,
        iterations,
        electronicEnergy,
        totalEnergy,
        orbitalEnergies,
        molecularOrbitalCoefficients,
        densityMatrix,
        List.of());
  }

  public static RhfResult of(
      final boolean converged,
      final int iterations,
      final double electronicEnergy,
      final double totalEnergy,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] densityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    if (iterations < 0) {
      throw new IllegalArgumentException("RHF iteration count must not be negative.");
    }
    if (!Double.isFinite(electronicEnergy) || !Double.isFinite(totalEnergy)) {
      throw new IllegalArgumentException("RHF energies must be finite.");
    }
    RhfResult.requireArray(orbitalEnergies, "RHF orbital energies");
    RhfResult.requireArray(molecularOrbitalCoefficients, "RHF MO coefficients");
    RhfResult.requireArray(densityMatrix, "RHF density matrix");
    RhfResult.requireIterationHistory(iterations, iterationHistory, "RHF");
    return new RhfResult(
        converged,
        iterations,
        electronicEnergy,
        totalEnergy,
        orbitalEnergies.clone(),
        molecularOrbitalCoefficients.clone(),
        densityMatrix.clone(),
        List.copyOf(iterationHistory));
  }

  public boolean converged() {
    return this.converged;
  }

  public int iterations() {
    return this.iterations;
  }

  public double electronicEnergy() {
    return this.electronicEnergy;
  }

  public double totalEnergy() {
    return this.totalEnergy;
  }

  public double[] orbitalEnergiesCopy() {
    return this.orbitalEnergies.clone();
  }

  public double[] molecularOrbitalCoefficientsCopy() {
    return this.molecularOrbitalCoefficients.clone();
  }

  public double[] densityMatrixCopy() {
    return this.densityMatrix.clone();
  }

  public List<ScfIterationRecord> iterationHistory() {
    return this.iterationHistory;
  }

  private static void requireArray(final double[] values, final String subjectName) {
    if (values == null) {
      throw new IllegalArgumentException(subjectName + " must not be null.");
    }
    for (int i = 0; i < values.length; ++i) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException(subjectName + " contains non-finite value.");
      }
    }
  }

  private static void requireIterationHistory(
      final int iterations,
      final List<ScfIterationRecord> iterationHistory,
      final String subjectName) {
    if (iterationHistory == null) {
      throw new IllegalArgumentException(subjectName + " iteration history must not be null.");
    }
    if (!iterationHistory.isEmpty() && iterationHistory.size() != iterations) {
      throw new IllegalArgumentException(
          subjectName + " iteration history size must match iteration count.");
    }
    for (int i = 0; i < iterationHistory.size(); ++i) {
      final ScfIterationRecord record = iterationHistory.get(i);
      if (record == null) {
        throw new IllegalArgumentException(
            subjectName + " iteration history contains null record.");
      }
      if (record.iteration() != i + 1) {
        throw new IllegalArgumentException(subjectName + " iteration history is not ordered.");
      }
    }
  }
}
