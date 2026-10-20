/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.scf.application.validation;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.convergence.ScfIterationRecord;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rohf.RohfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfValidationOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfValidationReport;

/**
 * Проверяет физические инварианты SCF-результата поверх AO overlap-метрики.
 */
public final class ScfResultValidator {

  public ScfValidationReport validateRestricted(
      final RhfResult result,
      final double[] overlap,
      final int orbitalCount,
      final int electronCount) {
    return this.validateRestricted(
        result, overlap, orbitalCount, electronCount, ScfValidationOptions.DEFAULT);
  }

  public ScfValidationReport validateRestricted(
      final RhfResult result,
      final double[] overlap,
      final int orbitalCount,
      final int electronCount,
      final ScfValidationOptions options) {
    if (result == null) {
      throw new IllegalArgumentException("RHF result must not be null.");
    }
    final ScfValidationOptions checkedOptions =
        options == null ? ScfValidationOptions.DEFAULT : options;
    ScfResultValidator.requireMatrix(overlap, orbitalCount, "SCF overlap matrix");
    final ArrayList<ScfDiagnostic> diagnostics = new ArrayList<ScfDiagnostic>();
    this.validateConvergence(result.converged(), result.iterationHistory(), diagnostics);
    final double[] density = result.densityMatrixCopy();
    final double[] coefficients = result.molecularOrbitalCoefficientsCopy();
    final double traceDeviation =
        Math.abs(ScfResultValidator.traceProduct(density, overlap, orbitalCount) - electronCount);
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_TRACE_MISMATCH,
        "RHF density trace in AO metric must equal electron count.",
        traceDeviation,
        checkedOptions.electronTraceTolerance());
    final double densitySymmetryDeviation =
        ScfResultValidator.symmetryDeviation(density, orbitalCount);
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_NOT_SYMMETRIC,
        "RHF density matrix must be symmetric.",
        densitySymmetryDeviation,
        checkedOptions.densitySymmetryTolerance());
    final double orthonormalityDeviation =
        ScfResultValidator.orthonormalityDeviation(coefficients, overlap, orbitalCount);
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.MO_NOT_ORTHONORMAL,
        "RHF molecular orbitals must satisfy C^T S C = I.",
        orthonormalityDeviation,
        checkedOptions.molecularOrbitalOrthonormalityTolerance());
    this.validateSortedOrbitalEnergies(result.orbitalEnergiesCopy(), checkedOptions, diagnostics);
    return ScfValidationReport.of(
        traceDeviation, densitySymmetryDeviation, orthonormalityDeviation, diagnostics);
  }

  public ScfValidationReport validateUnrestricted(
      final UhfResult result, final double[] overlap, final int orbitalCount) {
    return this.validateUnrestricted(result, overlap, orbitalCount, ScfValidationOptions.DEFAULT);
  }

  public ScfValidationReport validateUnrestricted(
      final UhfResult result,
      final double[] overlap,
      final int orbitalCount,
      final ScfValidationOptions options) {
    if (result == null) {
      throw new IllegalArgumentException("UHF result must not be null.");
    }
    final ScfValidationOptions checkedOptions =
        options == null ? ScfValidationOptions.DEFAULT : options;
    ScfResultValidator.requireMatrix(overlap, orbitalCount, "SCF overlap matrix");
    final ArrayList<ScfDiagnostic> diagnostics = new ArrayList<ScfDiagnostic>();
    this.validateConvergence(result.converged(), result.iterationHistory(), diagnostics);
    final double[] alphaDensity = result.alphaDensityMatrixCopy();
    final double[] betaDensity = result.betaDensityMatrixCopy();
    final double alphaTrace = ScfResultValidator.traceProduct(alphaDensity, overlap, orbitalCount);
    final double betaTrace = ScfResultValidator.traceProduct(betaDensity, overlap, orbitalCount);
    final double traceDeviation =
        Math.max(
            Math.abs(alphaTrace - result.alphaElectronCount()),
            Math.abs(betaTrace - result.betaElectronCount()));
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_TRACE_MISMATCH,
        "UHF alpha/beta density traces in AO metric must equal spin electron counts.",
        traceDeviation,
        checkedOptions.electronTraceTolerance());
    final double densitySymmetryDeviation =
        Math.max(
            ScfResultValidator.symmetryDeviation(alphaDensity, orbitalCount),
            ScfResultValidator.symmetryDeviation(betaDensity, orbitalCount));
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_NOT_SYMMETRIC,
        "UHF alpha/beta density matrices must be symmetric.",
        densitySymmetryDeviation,
        checkedOptions.densitySymmetryTolerance());
    final double orthonormalityDeviation =
        Math.max(
            ScfResultValidator.orthonormalityDeviation(
                result.alphaMolecularOrbitalCoefficientsCopy(), overlap, orbitalCount),
            ScfResultValidator.orthonormalityDeviation(
                result.betaMolecularOrbitalCoefficientsCopy(), overlap, orbitalCount));
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.MO_NOT_ORTHONORMAL,
        "UHF alpha/beta molecular orbitals must satisfy C^T S C = I.",
        orthonormalityDeviation,
        checkedOptions.molecularOrbitalOrthonormalityTolerance());
    this.validateSortedOrbitalEnergies(
        result.alphaOrbitalEnergiesCopy(), checkedOptions, diagnostics);
    this.validateSortedOrbitalEnergies(
        result.betaOrbitalEnergiesCopy(), checkedOptions, diagnostics);
    if (result.spinContamination() > checkedOptions.spinContaminationWarningTolerance()) {
      diagnostics.add(
          ScfDiagnostic.of(
              ScfDiagnosticSeverity.WARNING,
              ScfDiagnosticCode.SPIN_CONTAMINATION_PRESENT,
              "UHF spin contamination is above the configured warning threshold.",
              result.spinContamination(),
              checkedOptions.spinContaminationWarningTolerance()));
    }
    return ScfValidationReport.of(
        traceDeviation, densitySymmetryDeviation, orthonormalityDeviation, diagnostics);
  }

  public ScfValidationReport validateKohnSham(
      final KohnShamDftResult result, final double[] overlap, final int orbitalCount) {
    return this.validateKohnSham(result, overlap, orbitalCount, ScfValidationOptions.DEFAULT);
  }

  public ScfValidationReport validateKohnSham(
      final KohnShamDftResult result,
      final double[] overlap,
      final int orbitalCount,
      final ScfValidationOptions options) {
    if (result == null) {
      throw new IllegalArgumentException("Kohn-Sham DFT result must not be null.");
    }
    if (result.unrestricted()) {
      return this.validateUnrestrictedKohnSham(result, overlap, orbitalCount, options);
    }
    final ScfValidationOptions checkedOptions =
        options == null ? ScfValidationOptions.DEFAULT : options;
    ScfResultValidator.requireMatrix(overlap, orbitalCount, "SCF overlap matrix");
    final ArrayList<ScfDiagnostic> diagnostics = new ArrayList<ScfDiagnostic>();
    this.validateConvergence(result.converged(), result.iterationHistory(), diagnostics);
    final double[] density = result.densityMatrixCopy();
    final double[] coefficients = result.molecularOrbitalCoefficientsCopy();
    final double traceDeviation =
        Math.abs(
            ScfResultValidator.traceProduct(density, overlap, orbitalCount)
                - result.alphaElectronCount()
                - result.betaElectronCount());
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_TRACE_MISMATCH,
        "Kohn-Sham DFT density trace in AO metric must equal electron count.",
        traceDeviation,
        checkedOptions.electronTraceTolerance());
    final double densitySymmetryDeviation =
        ScfResultValidator.symmetryDeviation(density, orbitalCount);
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_NOT_SYMMETRIC,
        "Kohn-Sham DFT density matrix must be symmetric.",
        densitySymmetryDeviation,
        checkedOptions.densitySymmetryTolerance());
    final double orthonormalityDeviation =
        ScfResultValidator.orthonormalityDeviation(coefficients, overlap, orbitalCount);
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.MO_NOT_ORTHONORMAL,
        "Kohn-Sham DFT molecular orbitals must satisfy C^T S C = I.",
        orthonormalityDeviation,
        checkedOptions.molecularOrbitalOrthonormalityTolerance());
    this.validateSortedOrbitalEnergies(result.orbitalEnergiesCopy(), checkedOptions, diagnostics);
    return ScfValidationReport.of(
        traceDeviation, densitySymmetryDeviation, orthonormalityDeviation, diagnostics);
  }

  private ScfValidationReport validateUnrestrictedKohnSham(
      final KohnShamDftResult result,
      final double[] overlap,
      final int orbitalCount,
      final ScfValidationOptions options) {
    final ScfValidationOptions checkedOptions =
        options == null ? ScfValidationOptions.DEFAULT : options;
    ScfResultValidator.requireMatrix(overlap, orbitalCount, "SCF overlap matrix");
    final ArrayList<ScfDiagnostic> diagnostics = new ArrayList<ScfDiagnostic>();
    this.validateConvergence(result.converged(), result.iterationHistory(), diagnostics);
    final double[] alphaDensity = result.alphaDensityMatrixCopy();
    final double[] betaDensity = result.betaDensityMatrixCopy();
    final double alphaTrace = ScfResultValidator.traceProduct(alphaDensity, overlap, orbitalCount);
    final double betaTrace = ScfResultValidator.traceProduct(betaDensity, overlap, orbitalCount);
    final double traceDeviation =
        Math.max(
            Math.abs(alphaTrace - result.alphaElectronCount()),
            Math.abs(betaTrace - result.betaElectronCount()));
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_TRACE_MISMATCH,
        "Unrestricted Kohn-Sham DFT alpha/beta density traces in AO metric must equal spin electron counts.",
        traceDeviation,
        checkedOptions.electronTraceTolerance());
    final double densitySymmetryDeviation =
        Math.max(
            ScfResultValidator.symmetryDeviation(alphaDensity, orbitalCount),
            ScfResultValidator.symmetryDeviation(betaDensity, orbitalCount));
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_NOT_SYMMETRIC,
        "Unrestricted Kohn-Sham DFT alpha/beta density matrices must be symmetric.",
        densitySymmetryDeviation,
        checkedOptions.densitySymmetryTolerance());
    final double orthonormalityDeviation =
        Math.max(
            ScfResultValidator.orthonormalityDeviation(
                result.alphaMolecularOrbitalCoefficientsCopy(), overlap, orbitalCount),
            ScfResultValidator.orthonormalityDeviation(
                result.betaMolecularOrbitalCoefficientsCopy(), overlap, orbitalCount));
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.MO_NOT_ORTHONORMAL,
        "Unrestricted Kohn-Sham DFT alpha/beta orbitals must satisfy C^T S C = I.",
        orthonormalityDeviation,
        checkedOptions.molecularOrbitalOrthonormalityTolerance());
    this.validateSortedOrbitalEnergies(
        result.alphaOrbitalEnergiesCopy(), checkedOptions, diagnostics);
    this.validateSortedOrbitalEnergies(
        result.betaOrbitalEnergiesCopy(), checkedOptions, diagnostics);
    return ScfValidationReport.of(
        traceDeviation, densitySymmetryDeviation, orthonormalityDeviation, diagnostics);
  }

  public ScfValidationReport validateRestrictedOpenShell(
      final RohfResult result, final double[] overlap, final int orbitalCount) {
    return this.validateRestrictedOpenShell(
        result, overlap, orbitalCount, ScfValidationOptions.DEFAULT);
  }

  public ScfValidationReport validateRestrictedOpenShell(
      final RohfResult result,
      final double[] overlap,
      final int orbitalCount,
      final ScfValidationOptions options) {
    if (result == null) {
      throw new IllegalArgumentException("ROHF result must not be null.");
    }
    final ScfValidationOptions checkedOptions =
        options == null ? ScfValidationOptions.DEFAULT : options;
    ScfResultValidator.requireMatrix(overlap, orbitalCount, "SCF overlap matrix");
    final ArrayList<ScfDiagnostic> diagnostics = new ArrayList<ScfDiagnostic>();
    this.validateConvergence(result.converged(), result.iterationHistory(), diagnostics);
    final double[] alphaDensity = result.alphaDensityMatrixCopy();
    final double[] betaDensity = result.betaDensityMatrixCopy();
    final double alphaTrace = ScfResultValidator.traceProduct(alphaDensity, overlap, orbitalCount);
    final double betaTrace = ScfResultValidator.traceProduct(betaDensity, overlap, orbitalCount);
    final double traceDeviation =
        Math.max(
            Math.abs(alphaTrace - result.alphaElectronCount()),
            Math.abs(betaTrace - result.betaElectronCount()));
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_TRACE_MISMATCH,
        "ROHF alpha/beta density traces in AO metric must equal spin electron counts.",
        traceDeviation,
        checkedOptions.electronTraceTolerance());
    final double densitySymmetryDeviation =
        Math.max(
            ScfResultValidator.symmetryDeviation(alphaDensity, orbitalCount),
            ScfResultValidator.symmetryDeviation(betaDensity, orbitalCount));
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.DENSITY_NOT_SYMMETRIC,
        "ROHF alpha/beta density matrices must be symmetric.",
        densitySymmetryDeviation,
        checkedOptions.densitySymmetryTolerance());
    final double orthonormalityDeviation =
        ScfResultValidator.orthonormalityDeviation(
            result.molecularOrbitalCoefficientsCopy(), overlap, orbitalCount);
    this.addIfExceeded(
        diagnostics,
        ScfDiagnosticCode.MO_NOT_ORTHONORMAL,
        "ROHF molecular orbitals must satisfy C^T S C = I.",
        orthonormalityDeviation,
        checkedOptions.molecularOrbitalOrthonormalityTolerance());
    this.validateSortedOrbitalEnergies(result.orbitalEnergiesCopy(), checkedOptions, diagnostics);
    if (result.spinContamination() > checkedOptions.spinContaminationWarningTolerance()) {
      diagnostics.add(
          ScfDiagnostic.of(
              ScfDiagnosticSeverity.WARNING,
              ScfDiagnosticCode.SPIN_CONTAMINATION_PRESENT,
              "ROHF spin contamination is above the configured warning threshold.",
              result.spinContamination(),
              checkedOptions.spinContaminationWarningTolerance()));
    }
    return ScfValidationReport.of(
        traceDeviation, densitySymmetryDeviation, orthonormalityDeviation, diagnostics);
  }

  private void validateConvergence(
      final boolean converged,
      final List<ScfIterationRecord> iterationHistory,
      final List<ScfDiagnostic> diagnostics) {
    if (!converged) {
      diagnostics.add(
          ScfDiagnostic.of(
              ScfDiagnosticSeverity.ERROR,
              ScfDiagnosticCode.RESULT_NOT_CONVERGED,
              "SCF result must be converged before downstream chemistry workflows use it as a reference.",
              1.0,
              0.0));
    }
    if (iterationHistory.isEmpty()) {
      diagnostics.add(
          ScfDiagnostic.of(
              ScfDiagnosticSeverity.WARNING,
              ScfDiagnosticCode.MISSING_ITERATION_HISTORY,
              "SCF result has no iteration history, so convergence cannot be audited.",
              1.0,
              0.0));
      return;
    }
    if (converged && !iterationHistory.get(iterationHistory.size() - 1).converged()) {
      diagnostics.add(
          ScfDiagnostic.of(
              ScfDiagnosticSeverity.ERROR,
              ScfDiagnosticCode.FINAL_ITERATION_NOT_CONVERGED,
              "SCF result is marked converged, but the final iteration record is not converged.",
              1.0,
              0.0));
    }
  }

  private void validateSortedOrbitalEnergies(
      final double[] orbitalEnergies,
      final ScfValidationOptions options,
      final List<ScfDiagnostic> diagnostics) {
    double maximumInversion = 0.0;
    for (int i = 1; i < orbitalEnergies.length; ++i) {
      maximumInversion = Math.max(maximumInversion, orbitalEnergies[i - 1] - orbitalEnergies[i]);
    }
    if (maximumInversion > options.orbitalEnergySortingTolerance()) {
      diagnostics.add(
          ScfDiagnostic.of(
              ScfDiagnosticSeverity.ERROR,
              ScfDiagnosticCode.ORBITAL_ENERGIES_NOT_SORTED,
              "SCF orbital energies must be sorted in nondecreasing order.",
              maximumInversion,
              options.orbitalEnergySortingTolerance()));
    }
  }

  private void addIfExceeded(
      final List<ScfDiagnostic> diagnostics,
      final ScfDiagnosticCode code,
      final String message,
      final double deviation,
      final double tolerance) {
    if (deviation > tolerance) {
      diagnostics.add(
          ScfDiagnostic.of(ScfDiagnosticSeverity.ERROR, code, message, deviation, tolerance));
    }
  }

  private static void requireMatrix(
      final double[] matrix, final int size, final String subjectName) {
    if (size <= 0) {
      throw new IllegalArgumentException(subjectName + " size must be positive.");
    }
    if (matrix == null || matrix.length != size * size) {
      throw new IllegalArgumentException(
          subjectName + " must be a square matrix with the requested size.");
    }
    for (int i = 0; i < matrix.length; ++i) {
      if (!Double.isFinite(matrix[i])) {
        throw new IllegalArgumentException(subjectName + " contains non-finite value.");
      }
    }
  }

  private static double traceProduct(
      final double[] density, final double[] overlap, final int size) {
    ScfResultValidator.requireMatrix(density, size, "SCF density matrix");
    double value = 0.0;
    for (int p = 0; p < size; ++p) {
      for (int q = 0; q < size; ++q) {
        value += density[p * size + q] * overlap[q * size + p];
      }
    }
    return value;
  }

  private static double symmetryDeviation(final double[] matrix, final int size) {
    ScfResultValidator.requireMatrix(matrix, size, "SCF symmetric matrix");
    double result = 0.0;
    for (int row = 0; row < size; ++row) {
      for (int column = row + 1; column < size; ++column) {
        result =
            Math.max(result, Math.abs(matrix[row * size + column] - matrix[column * size + row]));
      }
    }
    return result;
  }

  private static double orthonormalityDeviation(
      final double[] coefficients, final double[] overlap, final int size) {
    ScfResultValidator.requireMatrix(coefficients, size, "SCF MO coefficient matrix");
    double result = 0.0;
    for (int i = 0; i < size; ++i) {
      for (int j = 0; j < size; ++j) {
        double value = 0.0;
        for (int p = 0; p < size; ++p) {
          final double left = coefficients[p * size + i];
          for (int q = 0; q < size; ++q) {
            value += left * overlap[p * size + q] * coefficients[q * size + j];
          }
        }
        final double target = i == j ? 1.0 : 0.0;
        result = Math.max(result, Math.abs(value - target));
      }
    }
    return result;
  }
}
