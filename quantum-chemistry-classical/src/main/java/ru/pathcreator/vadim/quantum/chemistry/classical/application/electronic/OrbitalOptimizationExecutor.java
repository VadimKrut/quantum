/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.CompleteActiveSpaceSelfConsistentFieldExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf.CasscfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationRecord;

/**
 * Выполняет самостоятельную Java-native оптимизацию молекулярных орбиталей через CASSCF functional.
 */
public final class OrbitalOptimizationExecutor {

  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final CompleteActiveSpaceSelfConsistentFieldExecutor casscfExecutor;
  private final ActiveSpaceRequestResolver activeSpaceResolver;

  public OrbitalOptimizationExecutor() {
    this(
        new ElectronicStructurePreparationPipeline(),
        new CompleteActiveSpaceSelfConsistentFieldExecutor());
  }

  public OrbitalOptimizationExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(preparationPipeline, new CompleteActiveSpaceSelfConsistentFieldExecutor());
  }

  public OrbitalOptimizationExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final CompleteActiveSpaceSelfConsistentFieldExecutor casscfExecutor) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Orbital optimization preparation pipeline must not be null.");
    }
    if (casscfExecutor == null) {
      throw new IllegalArgumentException("Orbital optimization CASSCF executor must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.casscfExecutor = casscfExecutor;
    this.activeSpaceResolver =
        new ActiveSpaceRequestResolver(preparationPipeline, new ActiveSpaceSelector());
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Orbital optimization request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException(
          "Orbital optimization diagnostics target must not be null.");
    }
    if (!OrbitalOptimizationExecutor.casscfMethod(request)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name()),
              "Java-native orbital optimization requires CASSCF/MCSCF method."));
      return null;
    }
    final ResolvedActiveSpaceRequest resolvedActiveSpace;
    final ClassicalCalculationRequest resolvedRequest;
    try {
      resolvedActiveSpace =
          this.activeSpaceResolver.resolve(
              request, diagnostics, ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name());
      resolvedRequest = resolvedActiveSpace.request();
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name()),
              exception.getMessage()));
      return null;
    }
    final String unsupportedReason = this.restrictedClosedShellUnsupportedReason(resolvedRequest);
    if (unsupportedReason != null) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              OrbitalOptimizationExecutor.exceptionCode(unsupportedReason),
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name()),
              unsupportedReason));
      return null;
    }
    try {
      final ElectronicStructurePreparationResult preparationResult =
          this.preparationPipeline.prepare(resolvedRequest);
      final CasscfResult result = this.casscfExecutor.solve(resolvedRequest, preparationResult);
      if (!result.converged()) {
        diagnostics.add(
            ClassicalDiagnostic.warning(
                ClassicalDiagnosticCode.RESULT_PARTIAL,
                ClassicalDiagnosticTarget.of(
                    "calculationKind", ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name()),
                "Java-native orbital optimization stopped before energy convergence."));
      }
      return OrbitalOptimizationExecutor.resultSection(
          resolvedRequest, preparationResult, result, resolvedActiveSpace);
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              OrbitalOptimizationExecutor.exceptionCode(exception.getMessage()),
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name()),
              exception.getMessage()));
      return null;
    }
  }

  private String restrictedClosedShellUnsupportedReason(final ClassicalCalculationRequest request) {
    final String unsupportedReason =
        this.preparationPipeline.restrictedClosedShellUnsupportedReason(request);
    if (unsupportedReason == null) {
      return null;
    }
    if (this.preparationPipeline.canPrepareDirectActiveSpaceHamiltonian(request)) {
      return null;
    }
    return unsupportedReason;
  }

  private static ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult,
      final CasscfResult result,
      final ResolvedActiveSpaceRequest resolvedActiveSpace) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(
        ClassicalResultValue.text(
            "implemented_kernel", "active_space_casscf_orbital_optimization"));
    values.add(ClassicalResultValue.text("posthf_solver_family", "CASSCF_ORBITAL_OPTIMIZATION"));
    values.add(ClassicalResultValue.text("posthf_correlation_level", "ORBITAL_OPTIMIZATION"));
    values.add(ClassicalResultValue.text("posthf_reference_family", "RHF"));
    values.add(ClassicalResultValue.text("posthf_variational_bound", "true"));
    values.add(ClassicalResultValue.text("posthf_exact_diagonalization", "true"));
    values.add(ClassicalResultValue.text("java_native_posthf", "true"));
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "active_space_auto_selected", Boolean.toString(resolvedActiveSpace.autoSelected())));
    values.add(
        ClassicalResultValue.scalar(
            "first_active_orbital", resolvedActiveSpace.selection().firstActiveOrbital(), "index"));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.text("orbital_rotation_space", result.orbitalRotationSpace()));
    values.add(ClassicalResultValue.scalar("iterations", result.iterations(), "count"));
    values.add(
        ClassicalResultValue.scalar("first_active_orbital", result.firstActiveOrbital(), "index"));
    values.add(
        ClassicalResultValue.scalar(
            "active_space_electrons", result.activeElectronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("active_space_orbitals", result.activeOrbitalCount(), "count"));
    final int availableRotationPairCount =
        OrbitalOptimizationExecutor.addOrbitalPartition(values, preparationResult, result);
    values.add(
        ClassicalResultValue.scalar(
            "evaluated_orbital_rotations", result.evaluatedRotationCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "accepted_orbital_rotations", result.acceptedRotationCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("final_rotation_step", result.finalStepRadians(), "radian"));
    values.add(
        ClassicalResultValue.scalar(
            "initial_total_energy", result.initialTotalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "orbital_optimization_energy_improvement", result.totalEnergyImprovement(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "max_orbital_orthonormality_error", result.maxOrthonormalityError(), "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "max_residual_orbital_energy_improvement",
            result.maxResidualOrbitalEnergyImprovement(),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "max_residual_orbital_gradient",
            result.maxResidualOrbitalGradient(),
            "hartree_per_radian"));
    values.add(
        ClassicalResultValue.text(
            "orbital_energy_monotonic_nonincreasing",
            Boolean.toString(
                OrbitalOptimizationExecutor.monotonicNonIncreasing(
                    result.initialTotalEnergy(), result.orbitalOptimizationHistory()))));
    values.add(
        ClassicalResultValue.scalar(
            "orbital_rotation_acceptance_ratio",
            OrbitalOptimizationExecutor.acceptanceRatio(result),
            "ratio"));
    values.add(
        ClassicalResultValue.text(
            "orbital_residual_status", OrbitalOptimizationExecutor.residualStatus(result)));
    values.add(
        ClassicalResultValue.text(
            "orbital_optimization_quality", result.orbitalOptimizationQuality()));
    values.add(
        ClassicalResultValue.text(
            "orbital_optimization_stop_reason", result.orbitalOptimizationStopReason()));
    values.add(
        ClassicalResultValue.scalar(
            "orbital_rotation_pair_coverage_ratio",
            result.orbitalRotationPairCoverageRatio(),
            "ratio"));
    values.add(
        ClassicalResultValue.scalar(
            "orbital_history_row_count", result.orbitalOptimizationHistory().size(), "count"));
    values.add(
        ClassicalResultValue.scalar("optimized_total_energy", result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "optimized_electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "determinant_count", result.fciResult().determinantCount(), "count"));
    values.add(ClassicalResultValue.scalar("root_count", result.fciResult().rootCount(), "count"));
    values.add(
        ClassicalResultValue.of(
            "orbital_optimization_history",
            ClassicalResultValueKind.TABLE,
            "",
            OrbitalOptimizationExecutor.historyRows(result.orbitalOptimizationHistory())));
    values.add(
        ClassicalResultValue.of(
            "orbital_rotation_coverage",
            ClassicalResultValueKind.TABLE,
            "",
            OrbitalOptimizationExecutor.rotationCoverageRows(
                preparationResult, result, availableRotationPairCount)));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.ORBITAL_OPTIMIZATION, "CASSCF orbital optimization", values);
  }

  private static int addOrbitalPartition(
      final ArrayList<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult preparationResult,
      final CasscfResult result) {
    final int fullOrbitalCount = preparationResult.basisOrbitalCount();
    final int inactiveOrbitalCount = result.firstActiveOrbital();
    final int externalOrbitalCount =
        Math.max(0, fullOrbitalCount - result.firstActiveOrbital() - result.activeOrbitalCount());
    values.add(ClassicalResultValue.scalar("full_orbital_count", fullOrbitalCount, "count"));
    values.add(
        ClassicalResultValue.scalar("inactive_orbital_count", inactiveOrbitalCount, "count"));
    values.add(
        ClassicalResultValue.scalar("external_orbital_count", externalOrbitalCount, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "available_orbital_rotation_pair_count",
            OrbitalOptimizationExecutor.rotationPairCount(
                result.orbitalRotationSpace(),
                inactiveOrbitalCount,
                result.activeOrbitalCount(),
                externalOrbitalCount),
            "count"));
    values.add(
        ClassicalResultValue.text(
            "orbital_partition",
            inactiveOrbitalCount
                + " inactive / "
                + result.activeOrbitalCount()
                + " active / "
                + externalOrbitalCount
                + " external"));
    return OrbitalOptimizationExecutor.rotationPairCount(
        result.orbitalRotationSpace(),
        inactiveOrbitalCount,
        result.activeOrbitalCount(),
        externalOrbitalCount);
  }

  private static int rotationPairCount(
      final String orbitalRotationSpace,
      final int inactiveOrbitalCount,
      final int activeOrbitalCount,
      final int externalOrbitalCount) {
    if ("ACTIVE_EXTERNAL".equals(orbitalRotationSpace)) {
      return Math.multiplyExact(activeOrbitalCount, externalOrbitalCount);
    }
    if ("INACTIVE_ACTIVE".equals(orbitalRotationSpace)) {
      return Math.multiplyExact(inactiveOrbitalCount, activeOrbitalCount);
    }
    if ("INACTIVE_EXTERNAL".equals(orbitalRotationSpace)) {
      return Math.multiplyExact(inactiveOrbitalCount, externalOrbitalCount);
    }
    if ("ALL_NON_REDUNDANT".equals(orbitalRotationSpace)) {
      return Math.addExact(
          Math.addExact(
              Math.multiplyExact(activeOrbitalCount, externalOrbitalCount),
              Math.multiplyExact(inactiveOrbitalCount, activeOrbitalCount)),
          Math.multiplyExact(inactiveOrbitalCount, externalOrbitalCount));
    }
    return 0;
  }

  private static boolean monotonicNonIncreasing(
      final double initialEnergy, final List<OrbitalOptimizationRecord> history) {
    double previousEnergy = initialEnergy;
    for (int i = 0; i < history.size(); ++i) {
      final double energy = history.get(i).energy();
      if (energy > previousEnergy + 1.0E-12) {
        return false;
      }
      previousEnergy = energy;
    }
    return true;
  }

  private static double acceptanceRatio(final CasscfResult result) {
    if (result.evaluatedRotationCount() == 0) {
      return 0.0;
    }
    return (double) result.acceptedRotationCount() / (double) result.evaluatedRotationCount();
  }

  private static String residualStatus(final CasscfResult result) {
    if (result.converged()) {
      return "CONVERGED";
    }
    if (result.acceptedRotationCount() == 0) {
      return "STATIONARY_OR_NO_ACCEPTED_ROTATION";
    }
    return "RESIDUAL_REFINEMENT_REQUIRED";
  }

  private static List<String> rotationCoverageRows(
      final ElectronicStructurePreparationResult preparationResult,
      final CasscfResult result,
      final int availableRotationPairCount) {
    final int fullOrbitalCount = preparationResult.basisOrbitalCount();
    final int inactiveOrbitalCount = result.firstActiveOrbital();
    final int externalOrbitalCount =
        Math.max(0, fullOrbitalCount - result.firstActiveOrbital() - result.activeOrbitalCount());
    return List.of(
        "rotation_space|inactive_orbitals|active_orbitals|external_orbitals|available_pairs|evaluated_rotations|accepted_rotations|coverage_ratio|quality|stop_reason",
        result.orbitalRotationSpace()
            + "|"
            + inactiveOrbitalCount
            + "|"
            + result.activeOrbitalCount()
            + "|"
            + externalOrbitalCount
            + "|"
            + availableRotationPairCount
            + "|"
            + result.evaluatedRotationCount()
            + "|"
            + result.acceptedRotationCount()
            + "|"
            + result.orbitalRotationPairCoverageRatio()
            + "|"
            + result.orbitalOptimizationQuality()
            + "|"
            + result.orbitalOptimizationStopReason());
  }

  private static List<String> historyRows(final List<OrbitalOptimizationRecord> history) {
    final String header =
        "iteration|evaluated_rotations|accepted_rotations|step_rad|energy_hartree|delta_hartree|converged";
    if (history.isEmpty()) {
      return List.of(header);
    }
    final ArrayList<String> rows = new ArrayList<String>(history.size() + 1);
    rows.add(header);
    for (int i = 0; i < history.size(); ++i) {
      final OrbitalOptimizationRecord record = history.get(i);
      rows.add(
          record.iteration()
              + "|"
              + record.evaluatedRotationCount()
              + "|"
              + record.acceptedRotationCount()
              + "|"
              + record.stepRadians()
              + "|"
              + record.energy()
              + "|"
              + record.deltaEnergy()
              + "|"
              + record.converged());
    }
    return List.copyOf(rows);
  }

  private static boolean casscfMethod(final ClassicalCalculationRequest request) {
    if (request.method().type()
        != ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return false;
    }
    final String normalized = request.method().name().value().toLowerCase(Locale.ROOT);
    return normalized.equals("casscf")
        || normalized.contains("complete active space self-consistent field")
        || normalized.contains("mcscf");
  }

  private static ClassicalDiagnosticCode exceptionCode(final String message) {
    final String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
    if (normalized.contains("basis") || normalized.contains("gaussian")) {
      return ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED;
    }
    if (normalized.contains("active space") || normalized.contains("active-space")) {
      return ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED;
    }
    if (normalized.contains("geometry") || normalized.contains("coordinate")) {
      return ClassicalDiagnosticCode.GEOMETRY_REQUIRED;
    }
    if (normalized.contains("subject")) {
      return ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED;
    }
    return ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED;
  }
}
