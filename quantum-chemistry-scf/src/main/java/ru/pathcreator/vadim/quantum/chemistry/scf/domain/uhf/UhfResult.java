/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.convergence.ScfIterationRecord;

/** Результат unrestricted Hartree-Fock с alpha/beta орбиталями, плотностями и trace сходимости. */
public final class UhfResult {

  private final boolean converged;
  private final int iterations;
  private final int alphaElectronCount;
  private final int betaElectronCount;
  private final double electronicEnergy;
  private final double totalEnergy;
  private final double spinSquared;
  private final double spinContamination;
  private final double[] alphaOrbitalEnergies;
  private final double[] betaOrbitalEnergies;
  private final double[] alphaMolecularOrbitalCoefficients;
  private final double[] betaMolecularOrbitalCoefficients;
  private final double[] alphaDensityMatrix;
  private final double[] betaDensityMatrix;
  private final List<ScfIterationRecord> iterationHistory;

  private UhfResult(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final double electronicEnergy,
      final double totalEnergy,
      final double spinSquared,
      final double spinContamination,
      final double[] alphaOrbitalEnergies,
      final double[] betaOrbitalEnergies,
      final double[] alphaMolecularOrbitalCoefficients,
      final double[] betaMolecularOrbitalCoefficients,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    this.converged = converged;
    this.iterations = iterations;
    this.alphaElectronCount = alphaElectronCount;
    this.betaElectronCount = betaElectronCount;
    this.electronicEnergy = electronicEnergy;
    this.totalEnergy = totalEnergy;
    this.spinSquared = spinSquared;
    this.spinContamination = spinContamination;
    this.alphaOrbitalEnergies = alphaOrbitalEnergies;
    this.betaOrbitalEnergies = betaOrbitalEnergies;
    this.alphaMolecularOrbitalCoefficients = alphaMolecularOrbitalCoefficients;
    this.betaMolecularOrbitalCoefficients = betaMolecularOrbitalCoefficients;
    this.alphaDensityMatrix = alphaDensityMatrix;
    this.betaDensityMatrix = betaDensityMatrix;
    this.iterationHistory = iterationHistory;
  }

  public static UhfResult of(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final double electronicEnergy,
      final double totalEnergy,
      final double[] alphaOrbitalEnergies,
      final double[] betaOrbitalEnergies,
      final double[] alphaMolecularOrbitalCoefficients,
      final double[] betaMolecularOrbitalCoefficients,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix) {
    return UhfResult.of(
        converged,
        iterations,
        alphaElectronCount,
        betaElectronCount,
        electronicEnergy,
        totalEnergy,
        UhfResult.defaultSpinSquared(alphaElectronCount, betaElectronCount),
        0.0,
        alphaOrbitalEnergies,
        betaOrbitalEnergies,
        alphaMolecularOrbitalCoefficients,
        betaMolecularOrbitalCoefficients,
        alphaDensityMatrix,
        betaDensityMatrix,
        List.of());
  }

  public static UhfResult of(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final double electronicEnergy,
      final double totalEnergy,
      final double spinSquared,
      final double spinContamination,
      final double[] alphaOrbitalEnergies,
      final double[] betaOrbitalEnergies,
      final double[] alphaMolecularOrbitalCoefficients,
      final double[] betaMolecularOrbitalCoefficients,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix) {
    return UhfResult.of(
        converged,
        iterations,
        alphaElectronCount,
        betaElectronCount,
        electronicEnergy,
        totalEnergy,
        spinSquared,
        spinContamination,
        alphaOrbitalEnergies,
        betaOrbitalEnergies,
        alphaMolecularOrbitalCoefficients,
        betaMolecularOrbitalCoefficients,
        alphaDensityMatrix,
        betaDensityMatrix,
        List.of());
  }

  public static UhfResult of(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final double electronicEnergy,
      final double totalEnergy,
      final double[] alphaOrbitalEnergies,
      final double[] betaOrbitalEnergies,
      final double[] alphaMolecularOrbitalCoefficients,
      final double[] betaMolecularOrbitalCoefficients,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    final double spinSquared = UhfResult.defaultSpinSquared(alphaElectronCount, betaElectronCount);
    return UhfResult.of(
        converged,
        iterations,
        alphaElectronCount,
        betaElectronCount,
        electronicEnergy,
        totalEnergy,
        spinSquared,
        0.0,
        alphaOrbitalEnergies,
        betaOrbitalEnergies,
        alphaMolecularOrbitalCoefficients,
        betaMolecularOrbitalCoefficients,
        alphaDensityMatrix,
        betaDensityMatrix,
        iterationHistory);
  }

  public static UhfResult of(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final double electronicEnergy,
      final double totalEnergy,
      final double spinSquared,
      final double spinContamination,
      final double[] alphaOrbitalEnergies,
      final double[] betaOrbitalEnergies,
      final double[] alphaMolecularOrbitalCoefficients,
      final double[] betaMolecularOrbitalCoefficients,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    if (iterations < 0) {
      throw new IllegalArgumentException("UHF iteration count must not be negative.");
    }
    if (alphaElectronCount < 0 || betaElectronCount < 0) {
      throw new IllegalArgumentException("UHF spin electron counts must not be negative.");
    }
    if (!Double.isFinite(electronicEnergy) || !Double.isFinite(totalEnergy)) {
      throw new IllegalArgumentException("UHF energies must be finite.");
    }
    if (!Double.isFinite(spinSquared) || spinSquared < 0.0) {
      throw new IllegalArgumentException("UHF spin squared value must be finite and non-negative.");
    }
    if (!Double.isFinite(spinContamination) || spinContamination < -1.0E-12) {
      throw new IllegalArgumentException("UHF spin contamination must be finite and non-negative.");
    }
    UhfResult.requireArray(alphaOrbitalEnergies, "UHF alpha orbital energies");
    UhfResult.requireArray(betaOrbitalEnergies, "UHF beta orbital energies");
    UhfResult.requireArray(alphaMolecularOrbitalCoefficients, "UHF alpha MO coefficients");
    UhfResult.requireArray(betaMolecularOrbitalCoefficients, "UHF beta MO coefficients");
    UhfResult.requireArray(alphaDensityMatrix, "UHF alpha density matrix");
    UhfResult.requireArray(betaDensityMatrix, "UHF beta density matrix");
    UhfResult.requireIterationHistory(iterations, iterationHistory);
    return new UhfResult(
        converged,
        iterations,
        alphaElectronCount,
        betaElectronCount,
        electronicEnergy,
        totalEnergy,
        spinSquared,
        Math.max(0.0, spinContamination),
        alphaOrbitalEnergies.clone(),
        betaOrbitalEnergies.clone(),
        alphaMolecularOrbitalCoefficients.clone(),
        betaMolecularOrbitalCoefficients.clone(),
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

  public double[] alphaOrbitalEnergiesCopy() {
    return this.alphaOrbitalEnergies.clone();
  }

  public double[] betaOrbitalEnergiesCopy() {
    return this.betaOrbitalEnergies.clone();
  }

  public double[] alphaMolecularOrbitalCoefficientsCopy() {
    return this.alphaMolecularOrbitalCoefficients.clone();
  }

  public double[] betaMolecularOrbitalCoefficientsCopy() {
    return this.betaMolecularOrbitalCoefficients.clone();
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
      throw new IllegalArgumentException("UHF iteration history must not be null.");
    }
    if (!iterationHistory.isEmpty() && iterationHistory.size() != iterations) {
      throw new IllegalArgumentException("UHF iteration history size must match iteration count.");
    }
    for (int i = 0; i < iterationHistory.size(); ++i) {
      final ScfIterationRecord record = iterationHistory.get(i);
      if (record == null) {
        throw new IllegalArgumentException("UHF iteration history contains null record.");
      }
      if (record.iteration() != i + 1) {
        throw new IllegalArgumentException("UHF iteration history is not ordered.");
      }
    }
  }

  private static double defaultSpinSquared(
      final int alphaElectronCount, final int betaElectronCount) {
    final double spin = 0.5 * Math.abs(alphaElectronCount - betaElectronCount);
    return spin * (spin + 1.0);
  }
}
