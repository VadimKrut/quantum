/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.energy;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalDefinition;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.KohnShamDftSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.validation.ScfResultValidator;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftOptions;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.ksdft.KohnShamDftResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfValidationReport;

/** Запускает Java-native self-consistent Kohn-Sham DFT поверх AO integral tensors. */
public final class DensityFunctionalEnergyExecutor {
  private final KohnShamDftSolver solver;

  public DensityFunctionalEnergyExecutor() {
    this(new KohnShamDftSolver());
  }

  public DensityFunctionalEnergyExecutor(final KohnShamDftSolver solver) {
    if (solver == null) {
      throw new IllegalArgumentException("DFT solver must not be null.");
    }
    this.solver = solver;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("DFT request must not be null.");
    }
    if (preparationResult == null) {
      throw new IllegalArgumentException("DFT preparation result must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("DFT diagnostics target must not be null.");
    }
    final DensityFunctionalDefinition functionalDefinition =
        request.method().hasDensityFunctionalDefinition()
            ? request.method().densityFunctionalDefinition()
            : DensityFunctionalDefinition.resolve(request.method().name().value());
    final DensityFunctionalCoverageProfile coverageProfile =
        DensityFunctionalCoverageProfile.of(functionalDefinition);
    DensityFunctionalEnergyExecutor.addApproximationDiagnostic(
        functionalDefinition, coverageProfile, diagnostics);
    final KohnShamDftResult result =
        preparationResult.hasKohnShamDftReference()
            ? preparationResult.kohnShamDftResult()
            : this.solver.solve(
                preparationResult.integralMolecule(),
                preparationResult.basisSet(),
                preparationResult.atomicOrbitalTensors(),
                preparationResult.electronCount(),
                functionalDefinition,
                KohnShamDftOptions.DEFAULT);
    if (!result.converged()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_PARTIAL,
              ClassicalDiagnosticTarget.of("method", request.method().name().value()),
              "Kohn-Sham DFT reached iteration limit before convergence."));
    }
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "kohn_sham_dft_" + functionalDefinition.name(),
        "self_consistent_density_functional_single_point_energy",
        coverageProfile.approximationScope());
    values.add(ClassicalResultValue.text("method", "DFT_KOHN_SHAM"));
    values.add(ClassicalResultValue.text("functional", functionalDefinition.name()));
    values.add(
        ClassicalResultValue.text("functional_family", functionalDefinition.family().name()));
    values.add(
        ClassicalResultValue.text("functional_description", functionalDefinition.description()));
    values.add(ClassicalResultValue.text("functional_solver_model", coverageProfile.solverModel()));
    values.add(ClassicalResultValue.text("parity_level", coverageProfile.parityLevel()));
    values.add(
        ClassicalResultValue.text("approximation_scope", coverageProfile.approximationScope()));
    values.add(
        ClassicalResultValue.text(
            "java_native_grid_contract", coverageProfile.javaNativeGridContract()));
    values.add(
        ClassicalResultValue.text(
            "grid_model_available", Boolean.toString(coverageProfile.gridModelAvailable())));
    values.add(
        ClassicalResultValue.text(
            "production_parity", Boolean.toString(coverageProfile.externalPackageParityClaimed())));
    values.add(
        ClassicalResultValue.text(
            "external_package_parity_claimed",
            Boolean.toString(coverageProfile.externalPackageParityClaimed())));
    values.add(
        ClassicalResultValue.text(
            "gradient_corrected", Boolean.toString(functionalDefinition.gradientCorrected())));
    values.add(
        ClassicalResultValue.text(
            "dispersion_corrected", Boolean.toString(functionalDefinition.dispersionCorrected())));
    values.add(
        ClassicalResultValue.text(
            "reference_method",
            result.unrestricted() ? "UNRESTRICTED_KOHN_SHAM_DFT" : "KOHN_SHAM_DFT"));
    values.add(
        ClassicalResultValue.text(
            "unrestricted_reference", Boolean.toString(result.unrestricted())));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("iterations", result.iterations(), "count"));
    values.add(ClassicalResultValue.scalar("density_trace", result.densityTrace(), "electron"));
    values.add(
        ClassicalResultValue.scalar(
            "xc_grid_integrated_electron_count",
            result.xcGridIntegratedElectronCount(),
            "electron"));
    values.add(
        ClassicalResultValue.scalar(
            "xc_grid_electron_count_error", result.xcGridElectronCountError(), "electron"));
    if (result.unrestricted()) {
      values.add(
          ClassicalResultValue.scalar(
              "alpha_electron_count", result.alphaElectronCount(), "electron"));
      values.add(
          ClassicalResultValue.scalar(
              "beta_electron_count", result.betaElectronCount(), "electron"));
      values.add(
          ClassicalResultValue.text(
              "spin_sector",
              result.betaElectronCount() == 0 ? "OPEN_SHELL_DFT" : "UNRESTRICTED_DFT"));
    }
    values.add(
        ClassicalResultValue.scalar(
            "exact_exchange_fraction", functionalDefinition.exactExchangeFraction(), "fraction"));
    values.add(
        ClassicalResultValue.scalar(
            "perturbative_correlation_fraction",
            functionalDefinition.perturbativeCorrelationFraction(),
            "fraction"));
    values.add(
        ClassicalResultValue.scalar("electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("exchange_energy", result.exchangeEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("correlation_energy", result.correlationEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "exchange_correlation_energy", result.exchangeCorrelationEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "orbital_count", preparationResult.atomicOrbitalTensors().orbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("electron_count", preparationResult.electronCount(), "count"));
    DensityFunctionalEnergyExecutor.addOrbitalEnergyValues(values, result);
    DensityFunctionalEnergyExecutor.addScfValidationValues(
        values,
        new ScfResultValidator()
            .validateKohnSham(
                result,
                preparationResult.atomicOrbitalTensors().overlapCopy(),
                preparationResult.atomicOrbitalTensors().orbitalCount()));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "Kohn-Sham DFT single-point energy", values);
  }

  private static void addOrbitalEnergyValues(
      final ArrayList<ClassicalResultValue> values, final KohnShamDftResult result) {
    if (!result.unrestricted()) {
      values.add(
          ClassicalResultValue.of(
              "orbital_energies",
              ClassicalResultValueKind.VECTOR,
              "hartree",
              DensityFunctionalEnergyExecutor.doubleArrayValues(result.orbitalEnergiesCopy())));
      return;
    }
    values.add(
        ClassicalResultValue.of(
            "alpha_orbital_energies",
            ClassicalResultValueKind.VECTOR,
            "hartree",
            DensityFunctionalEnergyExecutor.doubleArrayValues(result.alphaOrbitalEnergiesCopy())));
    values.add(
        ClassicalResultValue.of(
            "beta_orbital_energies",
            ClassicalResultValueKind.VECTOR,
            "hartree",
            DensityFunctionalEnergyExecutor.doubleArrayValues(result.betaOrbitalEnergiesCopy())));
  }

  private static List<String> doubleArrayValues(final double[] values) {
    final ArrayList<String> result = new ArrayList<String>(values.length);
    for (int i = 0; i < values.length; ++i) {
      result.add(Double.toString(values[i]));
    }
    return result;
  }

  private static void addScfValidationValues(
      final List<ClassicalResultValue> values, final ScfValidationReport report) {
    values.add(ClassicalResultValue.text("scf_validation_valid", Boolean.toString(report.valid())));
    values.add(
        ClassicalResultValue.scalar(
            "scf_density_trace_deviation", report.electronTraceDeviation(), "electron"));
    values.add(
        ClassicalResultValue.scalar(
            "scf_density_symmetry_deviation", report.densitySymmetryDeviation(), "absolute"));
    values.add(
        ClassicalResultValue.scalar(
            "scf_mo_orthonormality_deviation",
            report.molecularOrbitalOrthonormalityDeviation(),
            "absolute"));
    values.add(
        ClassicalResultValue.scalar("scf_diagnostic_count", report.diagnostics().size(), "count"));
    values.add(
        ClassicalResultValue.of(
            "scf_diagnostic_codes",
            ClassicalResultValueKind.VECTOR,
            "",
            DensityFunctionalEnergyExecutor.scfDiagnosticCodes(report.diagnostics())));
  }

  private static List<String> scfDiagnosticCodes(final List<ScfDiagnostic> diagnostics) {
    if (diagnostics.isEmpty()) {
      return List.of("NONE");
    }
    final ArrayList<String> result = new ArrayList<String>(diagnostics.size());
    for (int i = 0; i < diagnostics.size(); ++i) {
      result.add(diagnostics.get(i).code().name());
    }
    return result;
  }

  private static void addApproximationDiagnostic(
      final DensityFunctionalDefinition functionalDefinition,
      final DensityFunctionalCoverageProfile coverageProfile,
      final List<ClassicalDiagnostic> diagnostics) {
    if (!coverageProfile.resultPartialDiagnosticRequired()) {
      return;
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_PARTIAL,
            ClassicalDiagnosticTarget.of("functional", functionalDefinition.name()),
            coverageProfile.partialDiagnosticMessage(functionalDefinition)));
  }
}
