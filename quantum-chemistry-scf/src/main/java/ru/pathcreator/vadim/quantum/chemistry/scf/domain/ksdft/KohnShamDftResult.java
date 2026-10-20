/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft;

import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.convergence.ScfIterationRecord;

/** Результат closed-shell self-consistent Kohn-Sham DFT в AO basis. */
public final class KohnShamDftResult {

  private final boolean converged;
  private final int iterations;
  private final KohnShamFunctionalFamily functionalFamily;
  private final double electronicEnergy;
  private final double totalEnergy;
  private final double exchangeCorrelationEnergy;
  private final double exchangeEnergy;
  private final double correlationEnergy;
  private final double exactExchangeFraction;
  private final double densityTrace;
  private final double xcGridIntegratedElectronCount;
  private final boolean unrestricted;
  private final int alphaElectronCount;
  private final int betaElectronCount;
  private final double[] orbitalEnergies;
  private final double[] molecularOrbitalCoefficients;
  private final double[] densityMatrix;
  private final double[] betaOrbitalEnergies;
  private final double[] betaMolecularOrbitalCoefficients;
  private final double[] betaDensityMatrix;
  private final List<ScfIterationRecord> iterationHistory;

  private KohnShamDftResult(
      final boolean converged,
      final int iterations,
      final KohnShamFunctionalFamily functionalFamily,
      final double electronicEnergy,
      final double totalEnergy,
      final double exchangeCorrelationEnergy,
      final double exchangeEnergy,
      final double correlationEnergy,
      final double exactExchangeFraction,
      final double densityTrace,
      final double xcGridIntegratedElectronCount,
      final boolean unrestricted,
      final int alphaElectronCount,
      final int betaElectronCount,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] densityMatrix,
      final double[] betaOrbitalEnergies,
      final double[] betaMolecularOrbitalCoefficients,
      final double[] betaDensityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    this.converged = converged;
    this.iterations = iterations;
    this.functionalFamily = functionalFamily;
    this.electronicEnergy = electronicEnergy;
    this.totalEnergy = totalEnergy;
    this.exchangeCorrelationEnergy = exchangeCorrelationEnergy;
    this.exchangeEnergy = exchangeEnergy;
    this.correlationEnergy = correlationEnergy;
    this.exactExchangeFraction = exactExchangeFraction;
    this.densityTrace = densityTrace;
    this.xcGridIntegratedElectronCount = xcGridIntegratedElectronCount;
    this.unrestricted = unrestricted;
    this.alphaElectronCount = alphaElectronCount;
    this.betaElectronCount = betaElectronCount;
    this.orbitalEnergies = orbitalEnergies;
    this.molecularOrbitalCoefficients = molecularOrbitalCoefficients;
    this.densityMatrix = densityMatrix;
    this.betaOrbitalEnergies = betaOrbitalEnergies;
    this.betaMolecularOrbitalCoefficients = betaMolecularOrbitalCoefficients;
    this.betaDensityMatrix = betaDensityMatrix;
    this.iterationHistory = iterationHistory;
  }

  public static KohnShamDftResult of(
      final boolean converged,
      final int iterations,
      final KohnShamFunctionalFamily functionalFamily,
      final double electronicEnergy,
      final double totalEnergy,
      final double exchangeCorrelationEnergy,
      final double exchangeEnergy,
      final double correlationEnergy,
      final double exactExchangeFraction,
      final double densityTrace,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] densityMatrix) {
    return KohnShamDftResult.of(
        converged,
        iterations,
        functionalFamily,
        electronicEnergy,
        totalEnergy,
        exchangeCorrelationEnergy,
        exchangeEnergy,
        correlationEnergy,
        exactExchangeFraction,
        densityTrace,
        orbitalEnergies,
        molecularOrbitalCoefficients,
        densityMatrix,
        List.of());
  }

  public static KohnShamDftResult of(
      final boolean converged,
      final int iterations,
      final KohnShamFunctionalFamily functionalFamily,
      final double electronicEnergy,
      final double totalEnergy,
      final double exchangeCorrelationEnergy,
      final double exchangeEnergy,
      final double correlationEnergy,
      final double exactExchangeFraction,
      final double densityTrace,
      final double xcGridIntegratedElectronCount,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] densityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    if (iterations < 0) {
      throw new IllegalArgumentException("Kohn-Sham DFT iteration count must not be negative.");
    }
    if (functionalFamily == null) {
      throw new IllegalArgumentException("Kohn-Sham DFT functional family must not be null.");
    }
    KohnShamDftResult.requireFinite(electronicEnergy, "Kohn-Sham DFT electronic energy");
    KohnShamDftResult.requireFinite(totalEnergy, "Kohn-Sham DFT total energy");
    KohnShamDftResult.requireFinite(exchangeCorrelationEnergy, "Kohn-Sham DFT XC energy");
    KohnShamDftResult.requireFinite(exchangeEnergy, "Kohn-Sham DFT exchange energy");
    KohnShamDftResult.requireFinite(correlationEnergy, "Kohn-Sham DFT correlation energy");
    KohnShamDftResult.requireFinite(exactExchangeFraction, "Kohn-Sham DFT exact exchange fraction");
    KohnShamDftResult.requireFinite(densityTrace, "Kohn-Sham DFT density trace");
    KohnShamDftResult.requireFinite(
        xcGridIntegratedElectronCount, "Kohn-Sham DFT XC grid integrated electron count");
    KohnShamDftResult.requireArray(orbitalEnergies, "Kohn-Sham DFT orbital energies");
    KohnShamDftResult.requireArray(molecularOrbitalCoefficients, "Kohn-Sham DFT MO coefficients");
    KohnShamDftResult.requireArray(densityMatrix, "Kohn-Sham DFT density matrix");
    KohnShamDftResult.requireIterationHistory(iterations, iterationHistory);
    return new KohnShamDftResult(
        converged,
        iterations,
        functionalFamily,
        electronicEnergy,
        totalEnergy,
        exchangeCorrelationEnergy,
        exchangeEnergy,
        correlationEnergy,
        exactExchangeFraction,
        densityTrace,
        xcGridIntegratedElectronCount,
        false,
        (int) Math.round(densityTrace * 0.5),
        (int) Math.round(densityTrace * 0.5),
        orbitalEnergies.clone(),
        molecularOrbitalCoefficients.clone(),
        densityMatrix.clone(),
        new double[0],
        new double[0],
        new double[0],
        List.copyOf(iterationHistory));
  }

  public static KohnShamDftResult unrestricted(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final KohnShamFunctionalFamily functionalFamily,
      final double electronicEnergy,
      final double totalEnergy,
      final double exchangeCorrelationEnergy,
      final double exchangeEnergy,
      final double correlationEnergy,
      final double exactExchangeFraction,
      final double densityTrace,
      final double[] alphaOrbitalEnergies,
      final double[] betaOrbitalEnergies,
      final double[] alphaMolecularOrbitalCoefficients,
      final double[] betaMolecularOrbitalCoefficients,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    return KohnShamDftResult.unrestricted(
        converged,
        iterations,
        alphaElectronCount,
        betaElectronCount,
        functionalFamily,
        electronicEnergy,
        totalEnergy,
        exchangeCorrelationEnergy,
        exchangeEnergy,
        correlationEnergy,
        exactExchangeFraction,
        densityTrace,
        densityTrace,
        alphaOrbitalEnergies,
        betaOrbitalEnergies,
        alphaMolecularOrbitalCoefficients,
        betaMolecularOrbitalCoefficients,
        alphaDensityMatrix,
        betaDensityMatrix,
        iterationHistory);
  }

  public static KohnShamDftResult unrestricted(
      final boolean converged,
      final int iterations,
      final int alphaElectronCount,
      final int betaElectronCount,
      final KohnShamFunctionalFamily functionalFamily,
      final double electronicEnergy,
      final double totalEnergy,
      final double exchangeCorrelationEnergy,
      final double exchangeEnergy,
      final double correlationEnergy,
      final double exactExchangeFraction,
      final double densityTrace,
      final double xcGridIntegratedElectronCount,
      final double[] alphaOrbitalEnergies,
      final double[] betaOrbitalEnergies,
      final double[] alphaMolecularOrbitalCoefficients,
      final double[] betaMolecularOrbitalCoefficients,
      final double[] alphaDensityMatrix,
      final double[] betaDensityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    if (alphaElectronCount < 0 || betaElectronCount < 0 || alphaElectronCount < betaElectronCount) {
      throw new IllegalArgumentException(
          "Unrestricted Kohn-Sham DFT spin electron counts are invalid.");
    }
    KohnShamDftResult.requireArray(
        betaOrbitalEnergies, "Unrestricted Kohn-Sham DFT beta orbital energies");
    KohnShamDftResult.requireArray(
        betaMolecularOrbitalCoefficients, "Unrestricted Kohn-Sham DFT beta MO coefficients");
    KohnShamDftResult.requireArray(
        betaDensityMatrix, "Unrestricted Kohn-Sham DFT beta density matrix");
    final KohnShamDftResult checked =
        KohnShamDftResult.of(
            converged,
            iterations,
            functionalFamily,
            electronicEnergy,
            totalEnergy,
            exchangeCorrelationEnergy,
            exchangeEnergy,
            correlationEnergy,
            exactExchangeFraction,
            densityTrace,
            xcGridIntegratedElectronCount,
            alphaOrbitalEnergies,
            alphaMolecularOrbitalCoefficients,
            alphaDensityMatrix,
            iterationHistory);
    return new KohnShamDftResult(
        checked.converged,
        checked.iterations,
        checked.functionalFamily,
        checked.electronicEnergy,
        checked.totalEnergy,
        checked.exchangeCorrelationEnergy,
        checked.exchangeEnergy,
        checked.correlationEnergy,
        checked.exactExchangeFraction,
        checked.densityTrace,
        checked.xcGridIntegratedElectronCount,
        true,
        alphaElectronCount,
        betaElectronCount,
        checked.orbitalEnergies.clone(),
        checked.molecularOrbitalCoefficients.clone(),
        checked.densityMatrix.clone(),
        betaOrbitalEnergies.clone(),
        betaMolecularOrbitalCoefficients.clone(),
        betaDensityMatrix.clone(),
        checked.iterationHistory);
  }

  public static KohnShamDftResult of(
      final boolean converged,
      final int iterations,
      final KohnShamFunctionalFamily functionalFamily,
      final double electronicEnergy,
      final double totalEnergy,
      final double exchangeCorrelationEnergy,
      final double exchangeEnergy,
      final double correlationEnergy,
      final double exactExchangeFraction,
      final double densityTrace,
      final double[] orbitalEnergies,
      final double[] molecularOrbitalCoefficients,
      final double[] densityMatrix,
      final List<ScfIterationRecord> iterationHistory) {
    if (iterations < 0) {
      throw new IllegalArgumentException("Kohn-Sham DFT iteration count must not be negative.");
    }
    if (functionalFamily == null) {
      throw new IllegalArgumentException("Kohn-Sham DFT functional family must not be null.");
    }
    KohnShamDftResult.requireFinite(electronicEnergy, "Kohn-Sham DFT electronic energy");
    KohnShamDftResult.requireFinite(totalEnergy, "Kohn-Sham DFT total energy");
    KohnShamDftResult.requireFinite(exchangeCorrelationEnergy, "Kohn-Sham DFT XC energy");
    KohnShamDftResult.requireFinite(exchangeEnergy, "Kohn-Sham DFT exchange energy");
    KohnShamDftResult.requireFinite(correlationEnergy, "Kohn-Sham DFT correlation energy");
    KohnShamDftResult.requireFinite(exactExchangeFraction, "Kohn-Sham DFT exact exchange fraction");
    KohnShamDftResult.requireFinite(densityTrace, "Kohn-Sham DFT density trace");
    KohnShamDftResult.requireArray(orbitalEnergies, "Kohn-Sham DFT orbital energies");
    KohnShamDftResult.requireArray(molecularOrbitalCoefficients, "Kohn-Sham DFT MO coefficients");
    KohnShamDftResult.requireArray(densityMatrix, "Kohn-Sham DFT density matrix");
    KohnShamDftResult.requireIterationHistory(iterations, iterationHistory);
    return new KohnShamDftResult(
        converged,
        iterations,
        functionalFamily,
        electronicEnergy,
        totalEnergy,
        exchangeCorrelationEnergy,
        exchangeEnergy,
        correlationEnergy,
        exactExchangeFraction,
        densityTrace,
        densityTrace,
        false,
        (int) Math.round(densityTrace * 0.5),
        (int) Math.round(densityTrace * 0.5),
        orbitalEnergies.clone(),
        molecularOrbitalCoefficients.clone(),
        densityMatrix.clone(),
        new double[0],
        new double[0],
        new double[0],
        List.copyOf(iterationHistory));
  }

  public boolean converged() {
    return this.converged;
  }

  public int iterations() {
    return this.iterations;
  }

  public KohnShamFunctionalFamily functionalFamily() {
    return this.functionalFamily;
  }

  public double electronicEnergy() {
    return this.electronicEnergy;
  }

  public double totalEnergy() {
    return this.totalEnergy;
  }

  public double exchangeCorrelationEnergy() {
    return this.exchangeCorrelationEnergy;
  }

  public double exchangeEnergy() {
    return this.exchangeEnergy;
  }

  public double correlationEnergy() {
    return this.correlationEnergy;
  }

  public double exactExchangeFraction() {
    return this.exactExchangeFraction;
  }

  public double densityTrace() {
    return this.densityTrace;
  }

  public double xcGridIntegratedElectronCount() {
    return this.xcGridIntegratedElectronCount;
  }

  public double xcGridElectronCountError() {
    return Math.abs(this.xcGridIntegratedElectronCount - this.densityTrace);
  }

  public boolean unrestricted() {
    return this.unrestricted;
  }

  public int alphaElectronCount() {
    return this.alphaElectronCount;
  }

  public int betaElectronCount() {
    return this.betaElectronCount;
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

  public double[] alphaOrbitalEnergiesCopy() {
    return this.orbitalEnergiesCopy();
  }

  public double[] betaOrbitalEnergiesCopy() {
    if (!this.unrestricted) {
      return this.orbitalEnergiesCopy();
    }
    return this.betaOrbitalEnergies.clone();
  }

  public double[] alphaMolecularOrbitalCoefficientsCopy() {
    return this.molecularOrbitalCoefficientsCopy();
  }

  public double[] betaMolecularOrbitalCoefficientsCopy() {
    if (!this.unrestricted) {
      return this.molecularOrbitalCoefficientsCopy();
    }
    return this.betaMolecularOrbitalCoefficients.clone();
  }

  public double[] alphaDensityMatrixCopy() {
    return this.densityMatrixCopy();
  }

  public double[] betaDensityMatrixCopy() {
    if (!this.unrestricted) {
      return this.densityMatrixCopy();
    }
    return this.betaDensityMatrix.clone();
  }

  public List<ScfIterationRecord> iterationHistory() {
    return this.iterationHistory;
  }

  private static void requireFinite(final double value, final String subjectName) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(subjectName + " must be finite.");
    }
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
      throw new IllegalArgumentException("Kohn-Sham DFT iteration history must not be null.");
    }
    if (!iterationHistory.isEmpty() && iterationHistory.size() != iterations) {
      throw new IllegalArgumentException(
          "Kohn-Sham DFT iteration history size must match iteration count.");
    }
    for (int i = 0; i < iterationHistory.size(); ++i) {
      final ScfIterationRecord record = iterationHistory.get(i);
      if (record == null) {
        throw new IllegalArgumentException("Kohn-Sham DFT iteration history contains null record.");
      }
      if (record.iteration() != i + 1) {
        throw new IllegalArgumentException("Kohn-Sham DFT iteration history is not ordered.");
      }
    }
  }
}
