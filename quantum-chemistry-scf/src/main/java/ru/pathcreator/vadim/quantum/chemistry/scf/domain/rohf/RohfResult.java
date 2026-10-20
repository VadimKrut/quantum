/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.rohf;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.convergence.ScfIterationRecord;

/**
 * Результат restricted open-shell Hartree-Fock с общей пространственной MO-базой для alpha/beta электронов.
 */
public final class RohfResult {

  private final boolean converged;
  private final int iterations;
  private final int alphaElectronCount;
  private final int betaElectronCount;
  private final int closedShellElectronPairCount;
  private final int openShellElectronCount;
  private final double electronicEnergy;
  private final double totalEnergy;
  private final double spinSquared;
  private final double spinContamination;
  private final double[] orbitalEnergies;
  private final double[] molecularOrbitalCoefficients;
  private final double[] closedShellDensityMatrix;
  private final double[] openShellDensityMatrix;
  private final double[] alphaDensityMatrix;
  private final double[] betaDensityMatrix;
  private final List<ScfIterationRecord> iterationHistory;

  private RohfResult(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final int closedShellElectronPairCount,
      final int openShellElectronCount,
      final double electronicEnergy,
      final double totalEnergy,
      final double spinSquared,
      final double spinContamination,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] closedShellDensityMatrix,
      final double[] openShellDensityMatrix,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    this.converged = converged;
    this.iterations = iterations;
    this.alphaElectronCount = alphaElectronCount;
    this.betaElectronCount = betaElectronCount;
    this.closedShellElectronPairCount = closedShellElectronPairCount;
    this.openShellElectronCount = openShellElectronCount;
    this.electronicEnergy = electronicEnergy;
    this.totalEnergy = totalEnergy;
    this.spinSquared = spinSquared;
    this.spinContamination = spinContamination;
    this.orbitalEnergies = orbitalEnergies;
    this.molecularOrbitalCoefficients = molecularOrbitalCoefficients;
    this.closedShellDensityMatrix = closedShellDensityMatrix;
    this.openShellDensityMatrix = openShellDensityMatrix;
    this.alphaDensityMatrix = alphaDensityMatrix;
    this.betaDensityMatrix = betaDensityMatrix;
    this.iterationHistory = iterationHistory;
  }

  public static RohfResult of(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final int closedShellElectronPairCount,
      final int openShellElectronCount,
      final double electronicEnergy,
      final double totalEnergy,
      final double spinSquared,
      final double spinContamination,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] closedShellDensityMatrix,
      final double[] openShellDensityMatrix,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    if (iterations < 0) {
      throw new IllegalArgumentException("ROHF iteration count must not be negative.");
    }
    if (alphaElectronCount <= 0
        || betaElectronCount < 0
        || alphaElectronCount < betaElectronCount) {
      throw new IllegalArgumentException("ROHF spin electron counts are invalid.");
    }
    if (closedShellElectronPairCount < 0 || openShellElectronCount <= 0) {
      throw new IllegalArgumentException("ROHF open-shell occupation counts are invalid.");
    }
    if (betaElectronCount != closedShellElectronPairCount
        || alphaElectronCount != closedShellElectronPairCount + openShellElectronCount) {
      throw new IllegalArgumentException("ROHF occupation counts are inconsistent.");
    }
    if (!Double.isFinite(electronicEnergy) || !Double.isFinite(totalEnergy)) {
      throw new IllegalArgumentException("ROHF energies must be finite.");
    }
    if (!Double.isFinite(spinSquared)
        || !Double.isFinite(spinContamination)
        || spinContamination < 0.0) {
      throw new IllegalArgumentException("ROHF spin values must be finite and non-negative.");
    }
    RohfResult.requireArray(orbitalEnergies, "ROHF orbital energies");
    RohfResult.requireArray(molecularOrbitalCoefficients, "ROHF MO coefficients");
    RohfResult.requireArray(closedShellDensityMatrix, "ROHF closed-shell density matrix");
    RohfResult.requireArray(openShellDensityMatrix, "ROHF open-shell density matrix");
    RohfResult.requireArray(alphaDensityMatrix, "ROHF alpha density matrix");
    RohfResult.requireArray(betaDensityMatrix, "ROHF beta density matrix");
    RohfResult.requireIterationHistory(iterations, iterationHistory);
    return new RohfResult(
        converged,
        iterations,
        alphaElectronCount,
        betaElectronCount,
        closedShellElectronPairCount,
        openShellElectronCount,
        electronicEnergy,
        totalEnergy,
        spinSquared,
        spinContamination,
        orbitalEnergies.clone(),
        molecularOrbitalCoefficients.clone(),
        closedShellDensityMatrix.clone(),
        openShellDensityMatrix.clone(),
        alphaDensityMatrix.clone(),
        betaDensityMatrix.clone(),
        List.copyOf(iterationHistory));
  }

  public boolean converged() {
    return this.converged;
  }

  public int iterations() {
    return this.iterations;
  }

  public int alphaElectronCount() {
    return this.alphaElectronCount;
  }

  public int betaElectronCount() {
    return this.betaElectronCount;
  }

  public int closedShellElectronPairCount() {
    return this.closedShellElectronPairCount;
  }

  public int openShellElectronCount() {
    return this.openShellElectronCount;
  }

  public double electronicEnergy() {
    return this.electronicEnergy;
  }

  public double totalEnergy() {
    return this.totalEnergy;
  }

  public double spinSquared() {
    return this.spinSquared;
  }

  public double spinContamination() {
    return this.spinContamination;
  }

  public double[] orbitalEnergiesCopy() {
    return this.orbitalEnergies.clone();
  }

  public double[] molecularOrbitalCoefficientsCopy() {
    return this.molecularOrbitalCoefficients.clone();
  }

  public double[] closedShellDensityMatrixCopy() {
    return this.closedShellDensityMatrix.clone();
  }

  public double[] openShellDensityMatrixCopy() {
    return this.openShellDensityMatrix.clone();
  }

  public double[] alphaDensityMatrixCopy() {
    return this.alphaDensityMatrix.clone();
  }

  public double[] betaDensityMatrixCopy() {
    return this.betaDensityMatrix.clone();
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
      final int iterations, final List<ScfIterationRecord> iterationHistory) {
    if (iterationHistory == null) {
      throw new IllegalArgumentException("ROHF iteration history must not be null.");
    }
    if (!iterationHistory.isEmpty() && iterationHistory.size() != iterations) {
      throw new IllegalArgumentException("ROHF iteration history size must match iteration count.");
    }
    for (int i = 0; i < iterationHistory.size(); ++i) {
      final ScfIterationRecord record = iterationHistory.get(i);
      if (record == null) {
        throw new IllegalArgumentException("ROHF iteration history contains null record.");
      }
      if (record.iteration() != i + 1) {
        throw new IllegalArgumentException("ROHF iteration history is not ordered.");
      }
    }
  }
}
