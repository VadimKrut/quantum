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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.FermionSlaterDeterminantExpectationEstimator;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.DirectGaussianEriStatistics;

/** Возвращает переносимое описание подготовленного electronic Hamiltonian без внешнего runtime. */
public final class HamiltonianPreparationExecutor {

  private static final String ALLOW_ONE_ELECTRON_FALLBACK_OPTION =
      "hamiltonian_allow_one_electron_fallback";
  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final FermionSlaterDeterminantExpectationEstimator slaterEstimator;
  private final ActiveSpaceRequestResolver activeSpaceResolver;

  public HamiltonianPreparationExecutor() {
    this(
        new ElectronicStructurePreparationPipeline(),
        new FermionSlaterDeterminantExpectationEstimator());
  }

  public HamiltonianPreparationExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(preparationPipeline, new FermionSlaterDeterminantExpectationEstimator());
  }

  public HamiltonianPreparationExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final FermionSlaterDeterminantExpectationEstimator slaterEstimator) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException("Hamiltonian preparation pipeline must not be null.");
    }
    if (slaterEstimator == null) {
      throw new IllegalArgumentException(
          "Hamiltonian preparation Slater estimator must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.slaterEstimator = slaterEstimator;
    this.activeSpaceResolver =
        new ActiveSpaceRequestResolver(preparationPipeline, new ActiveSpaceSelector());
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    return this.execute(request, ClassicalCalculationKind.HAMILTONIAN_PREPARATION, diagnostics);
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind resultKind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Hamiltonian preparation request must not be null.");
    }
    if (resultKind == null) {
      throw new IllegalArgumentException("Hamiltonian preparation result kind must not be null.");
    }
    if (resultKind != ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
        && resultKind != ClassicalCalculationKind.HAMILTONIAN_PREPARATION) {
      throw new IllegalArgumentException("Hamiltonian preparation result kind is unsupported.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException(
          "Hamiltonian preparation diagnostics target must not be null.");
    }
    final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
    if (unsupportedReason != null) {
      final ClassicalResultSection directActiveSpaceSection =
          this.tryDirectActiveSpaceHamiltonian(request, resultKind, diagnostics);
      if (directActiveSpaceSection != null) {
        return directActiveSpaceSection;
      }
      final ClassicalResultSection oneElectronSection =
          this.tryOneElectronFallback(request, resultKind, diagnostics, unsupportedReason);
      if (oneElectronSection != null) {
        return oneElectronSection;
      }
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("calculationKind", resultKind.name()),
              unsupportedReason));
      return null;
    }
    try {
      final ElectronicStructurePreparationResult result = this.preparationPipeline.prepare(request);
      return this.resultSection(
          resultKind,
          "Electronic Hamiltonian preparation",
          request,
          result,
          HamiltonianPreparationExecutor.referenceMethod(result));
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_FAILED,
              ClassicalDiagnosticTarget.of("calculationKind", resultKind.name()),
              exception.getMessage()));
      return null;
    }
  }

  private ClassicalResultSection tryDirectActiveSpaceHamiltonian(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind resultKind,
      final List<ClassicalDiagnostic> diagnostics) {
    try {
      final ClassicalCalculationRequest resolvedRequest =
          request.hasActiveSpace()
              ? request
              : this.activeSpaceResolver.resolve(request, diagnostics, resultKind.name()).request();
      final ElectronicStructurePreparationResult result =
          this.preparationPipeline.prepare(resolvedRequest);
      if (!result.twoElectronIntegralsAvailable()) {
        return null;
      }
      return this.resultSection(
          resultKind,
          "Direct active-space electronic Hamiltonian preparation",
          resolvedRequest,
          result,
          HamiltonianPreparationExecutor.referenceMethod(result));
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_FAILED,
              ClassicalDiagnosticTarget.of("calculationKind", resultKind.name()),
              "Direct active-space Hamiltonian preparation failed: " + exception.getMessage()));
      return null;
    }
  }

  private ClassicalResultSection tryOneElectronFallback(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind resultKind,
      final List<ClassicalDiagnostic> diagnostics,
      final String fullUnsupportedReason) {
    if (!HamiltonianPreparationExecutor.booleanOption(
        request, ALLOW_ONE_ELECTRON_FALLBACK_OPTION, false)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_FAILED,
              ClassicalDiagnosticTarget.of("calculationKind", resultKind.name()),
              "Full two-electron Hamiltonian preparation is unavailable: "
                  + fullUnsupportedReason
                  + " One-electron fallback is not used unless "
                  + ALLOW_ONE_ELECTRON_FALLBACK_OPTION
                  + "=true is explicitly set."));
      return null;
    }
    final String oneElectronUnsupportedReason =
        this.preparationPipeline.oneElectronUnsupportedReason(request);
    if (oneElectronUnsupportedReason != null) {
      return null;
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_PARTIAL,
            ClassicalDiagnosticTarget.of("calculationKind", resultKind.name()),
            "Full two-electron Hamiltonian preparation is unavailable: " + fullUnsupportedReason));
    final ElectronicStructurePreparationResult result =
        this.preparationPipeline.prepareOneElectron(request);
    return this.resultSection(
        resultKind,
        "One-electron electronic Hamiltonian preparation",
        request,
        result,
        "AO_ONE_ELECTRON");
  }

  private ClassicalResultSection resultSection(
      final ClassicalCalculationKind resultKind,
      final String title,
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult result,
      final String referenceMethod) {
    final ElectronicHamiltonian hamiltonian = result.hamiltonian();
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("basis_set", result.basisSetName()));
    values.add(ClassicalResultValue.text("preparation_mode", result.mode().name()));
    values.add(
        ClassicalResultValue.scalar("basis_orbital_count", result.basisOrbitalCount(), "count"));
    if (request.basisSet().hasStructuredDefinition()) {
      values.add(
          ClassicalResultValue.scalar(
              "basis_atom_definition_count",
              request.basisSet().definition().atomDefinitionCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "basis_shell_count", request.basisSet().definition().shellCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "basis_primitive_count", request.basisSet().definition().primitiveCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "basis_cartesian_function_count",
              request.basisSet().definition().cartesianFunctionCount(),
              "count"));
    }
    values.add(ClassicalResultValue.text("reference_method", referenceMethod));
    values.add(
        ClassicalResultValue.text(
            "one_electron_fallback_explicitly_allowed",
            Boolean.toString(
                HamiltonianPreparationExecutor.booleanOption(
                    request, ALLOW_ONE_ELECTRON_FALLBACK_OPTION, false))));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", HamiltonianPreparationExecutor.referenceConverged(result)));
    values.add(
        ClassicalResultValue.text(
            "two_electron_integrals_available",
            Boolean.toString(result.twoElectronIntegralsAvailable())));
    values.add(
        ClassicalResultValue.text(
            "native_java_preparation",
            Boolean.toString(
                result.mode() != ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN)));
    values.add(
        ClassicalResultValue.text(
            "external_reference_route",
            Boolean.toString(
                result.mode() == ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN)));
    values.add(
        ClassicalResultValue.text(
            "orbital_optimized_reference", Boolean.toString(result.hasCasscfResult())));
    if (result.hasLimitationReason()) {
      values.add(
          ClassicalResultValue.text("preparation_limitation_reason", result.limitationReason()));
    }
    values.add(ClassicalResultValue.scalar("electron_count", hamiltonian.electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "spatial_orbital_count", hamiltonian.spatialOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("spin_orbital_count", hamiltonian.spinOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "one_electron_integral_count", hamiltonian.oneElectronIntegrals().size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "two_electron_integral_count", hamiltonian.twoElectronIntegrals().size(), "count"));
    values.add(
        ClassicalResultValue.scalar("total_integral_count", hamiltonian.integralCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "nuclear_repulsion_energy", hamiltonian.nuclearRepulsionEnergy(), "hartree"));
    HamiltonianPreparationExecutor.addDirectEriStatistics(values, result);
    this.addSlaterReferenceValues(values, result, hamiltonian);
    HamiltonianPreparationExecutor.addQuantumPreparationContract(
        values, request, result, hamiltonian);
    if (result.hasCasscfResult()) {
      values.add(
          ClassicalResultValue.scalar(
              "reference_total_energy", result.casscfResult().totalEnergy(), "hartree"));
      values.add(
          ClassicalResultValue.text(
              "orbital_rotation_space", result.casscfResult().orbitalRotationSpace()));
      values.add(
          ClassicalResultValue.text(
              "orbital_optimization_quality", result.casscfResult().orbitalOptimizationQuality()));
      values.add(
          ClassicalResultValue.text(
              "orbital_optimization_stop_reason",
              result.casscfResult().orbitalOptimizationStopReason()));
      values.add(
          ClassicalResultValue.scalar(
              "orbital_rotation_pair_coverage_ratio",
              result.casscfResult().orbitalRotationPairCoverageRatio(),
              "ratio"));
      values.add(
          ClassicalResultValue.scalar(
              "available_orbital_rotation_pair_count",
              result.casscfResult().availableRotationPairCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "max_orbital_orthonormality_error",
              result.casscfResult().maxOrthonormalityError(),
              "dimensionless"));
      values.add(
          ClassicalResultValue.scalar(
              "orbital_optimization_energy_improvement",
              result.casscfResult().totalEnergyImprovement(),
              "hartree"));
      values.add(
          ClassicalResultValue.scalar(
              "max_residual_orbital_energy_improvement",
              result.casscfResult().maxResidualOrbitalEnergyImprovement(),
              "hartree"));
      values.add(
          ClassicalResultValue.scalar(
              "max_residual_orbital_gradient",
              result.casscfResult().maxResidualOrbitalGradient(),
              "hartree_per_radian"));
      values.add(
          ClassicalResultValue.scalar(
              "evaluated_orbital_rotations",
              result.casscfResult().evaluatedRotationCount(),
              "count"));
      values.add(
          ClassicalResultValue.scalar(
              "accepted_orbital_rotations",
              result.casscfResult().acceptedRotationCount(),
              "count"));
    } else if (result.hasRhfReference()) {
      values.add(
          ClassicalResultValue.scalar(
              "reference_total_energy", result.rhfResult().totalEnergy(), "hartree"));
    } else if (result.hasRohfReference()) {
      values.add(
          ClassicalResultValue.scalar(
              "reference_total_energy", result.rohfResult().totalEnergy(), "hartree"));
      values.add(
          ClassicalResultValue.scalar(
              "reference_spin_squared", result.rohfResult().spinSquared(), "dimensionless"));
      values.add(
          ClassicalResultValue.scalar(
              "reference_spin_contamination",
              result.rohfResult().spinContamination(),
              "dimensionless"));
    } else if (result.hasKohnShamDftReference()) {
      values.add(
          ClassicalResultValue.scalar(
              "reference_total_energy", result.kohnShamDftResult().totalEnergy(), "hartree"));
      values.add(
          ClassicalResultValue.scalar(
              "exchange_correlation_energy",
              result.kohnShamDftResult().exchangeCorrelationEnergy(),
              "hartree"));
      values.add(
          ClassicalResultValue.scalar(
              "density_trace", result.kohnShamDftResult().densityTrace(), "electron"));
    } else if (result.hasUhfReference()) {
      values.add(
          ClassicalResultValue.scalar(
              "reference_total_energy", result.uhfResult().totalEnergy(), "hartree"));
    }
    return ClassicalResultSection.of(resultKind, title, values);
  }

  private void addSlaterReferenceValues(
      final ArrayList<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult result,
      final ElectronicHamiltonian hamiltonian) {
    final double slaterEnergy = this.slaterEstimator.hartreeFockReferenceEnergy(hamiltonian);
    values.add(
        ClassicalResultValue.scalar("fermion_slater_reference_energy", slaterEnergy, "hartree"));
    final double referenceEnergy = HamiltonianPreparationExecutor.referenceEnergyOrNaN(result);
    if (Double.isFinite(referenceEnergy)) {
      values.add(
          ClassicalResultValue.scalar(
              "fermion_slater_reference_energy_deviation",
              slaterEnergy - referenceEnergy,
              "hartree"));
    }
  }

  private static void addDirectEriStatistics(
      final ArrayList<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult result) {
    if (!result.hasDirectEriStatistics()) {
      return;
    }
    final DirectGaussianEriStatistics statistics = result.directEriStatistics();
    values.add(ClassicalResultValue.text("direct_eri_provider", "gaussian_screened_cached"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_value_queries", statistics.valueQueryCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_screened_out_queries", statistics.screenedOutQueryCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_screened_out_ratio", statistics.screenedOutRatio(), "ratio"));
    values.add(
        ClassicalResultValue.scalar("direct_eri_cache_hits", statistics.cacheHitCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_cache_hit_ratio", statistics.cacheHitRatio(), "ratio"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_computed_integrals", statistics.computedIntegralCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_pair_bound_integrals", statistics.pairBoundIntegralCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_pair_bound_builds", statistics.pairBoundBuildCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("direct_eri_cache_size", statistics.cacheSize(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_cache_capacity", statistics.cacheCapacity(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "direct_eri_screening_tolerance", statistics.screeningTolerance(), "absolute"));
  }

  private static void addQuantumPreparationContract(
      final ArrayList<ClassicalResultValue> values,
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult result,
      final ElectronicHamiltonian hamiltonian) {
    final boolean hasOneElectronTerms = !hamiltonian.oneElectronIntegrals().isEmpty();
    final boolean hasTwoElectronTerms = !hamiltonian.twoElectronIntegrals().isEmpty();
    final boolean hasAnyHamiltonianTerm =
        hasOneElectronTerms
            || hasTwoElectronTerms
            || Double.compare(hamiltonian.nuclearRepulsionEnergy(), 0.0) != 0;
    final boolean mappingReady =
        hasAnyHamiltonianTerm
            && hamiltonian.spinOrbitalCount() > 0
            && hamiltonian.electronCount() >= 0;
    values.add(ClassicalResultValue.text("quantum_preparation_contract", "fermion_qubit_mapping"));
    values.add(ClassicalResultValue.text("fermion_operator_ready", Boolean.toString(mappingReady)));
    values.add(ClassicalResultValue.text("qubit_mapping_ready", Boolean.toString(mappingReady)));
    values.add(
        ClassicalResultValue.text(
            "hamiltonian_completeness", HamiltonianPreparationExecutor.completeness(result)));
    values.add(
        ClassicalResultValue.text(
            "hamiltonian_contains_one_electron_terms", Boolean.toString(hasOneElectronTerms)));
    values.add(
        ClassicalResultValue.text(
            "hamiltonian_contains_two_electron_terms", Boolean.toString(hasTwoElectronTerms)));
    values.add(
        ClassicalResultValue.text(
            "electron_correlation_terms_complete",
            Boolean.toString(
                hasTwoElectronTerms
                    || result.mode() == ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN)));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_electron_count", hamiltonian.electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_spatial_orbital_count", hamiltonian.spatialOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_qubit_count", hamiltonian.spinOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_integral_count", hamiltonian.integralCount(), "count"));
    values.add(
        ClassicalResultValue.text(
            "spin_sector", HamiltonianPreparationExecutor.spinSector(result, hamiltonian)));
    HamiltonianPreparationExecutor.addElectronSector(values, result, hamiltonian);
    values.add(
        ClassicalResultValue.text(
            "active_space_requested", Boolean.toString(request.hasActiveSpace())));
    if (request.hasActiveSpace()) {
      final ActiveSpace activeSpace = request.activeSpace();
      values.add(
          ClassicalResultValue.scalar(
              "active_space_electrons", activeSpace.electronCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "active_space_orbitals", activeSpace.orbitalCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "active_space_spin_orbitals", activeSpace.spinOrbitalCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "active_space_first_orbital",
              HamiltonianPreparationExecutor.longOption(request, "active_space_first_orbital", 0L),
              "index"));
    }
  }

  private static void addElectronSector(
      final ArrayList<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult result,
      final ElectronicHamiltonian hamiltonian) {
    if (result.hasUhfReference()) {
      values.add(
          ClassicalResultValue.scalar(
              "alpha_electron_count", result.uhfResult().alphaElectronCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "beta_electron_count", result.uhfResult().betaElectronCount(), "count"));
      return;
    }
    if (result.hasRohfReference()) {
      values.add(
          ClassicalResultValue.scalar(
              "alpha_electron_count", result.rohfResult().alphaElectronCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "beta_electron_count", result.rohfResult().betaElectronCount(), "count"));
      return;
    }
    if (result.hasKohnShamDftReference() && result.kohnShamDftResult().unrestricted()) {
      values.add(
          ClassicalResultValue.scalar(
              "alpha_electron_count", result.kohnShamDftResult().alphaElectronCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "beta_electron_count", result.kohnShamDftResult().betaElectronCount(), "count"));
      return;
    }
    if (hamiltonian.electronCount() % 2 == 0) {
      values.add(
          ClassicalResultValue.scalar(
              "alpha_electron_count", hamiltonian.electronCount() / 2, "count"));
      values.add(
          ClassicalResultValue.scalar(
              "beta_electron_count", hamiltonian.electronCount() / 2, "count"));
    }
  }

  private static String completeness(final ElectronicStructurePreparationResult result) {
    if (result.mode() == ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN) {
      return "EXTERNAL_REFERENCE";
    }
    if (result.twoElectronIntegralsAvailable()) {
      return "FULL_TWO_ELECTRON";
    }
    return "ONE_ELECTRON_ONLY";
  }

  private static String spinSector(
      final ElectronicStructurePreparationResult result, final ElectronicHamiltonian hamiltonian) {
    if (result.hasUhfReference()) {
      return result.uhfResult().alphaElectronCount() == result.uhfResult().betaElectronCount()
          ? "UNRESTRICTED_CLOSED_SHELL"
          : "OPEN_SHELL";
    }
    if (result.hasRohfReference()) {
      return result.rohfResult().alphaElectronCount() == result.rohfResult().betaElectronCount()
          ? "RESTRICTED_OPEN_SHELL_PAIRED"
          : "RESTRICTED_OPEN_SHELL";
    }
    if (result.hasKohnShamDftReference() && result.kohnShamDftResult().unrestricted()) {
      return result.kohnShamDftResult().alphaElectronCount()
              == result.kohnShamDftResult().betaElectronCount()
          ? "UNRESTRICTED_DFT_CLOSED_SHELL"
          : "OPEN_SHELL_DFT";
    }
    if (hamiltonian.electronCount() % 2 == 0) {
      return "CLOSED_SHELL";
    }
    return "ODD_ELECTRON";
  }

  private static long longOption(
      final ClassicalCalculationRequest request, final String name, final long defaultValue) {
    for (int i = 0; i < request.options().size(); ++i) {
      if (!name.equals(request.options().get(i).name())) {
        continue;
      }
      return Long.parseLong(request.options().get(i).value());
    }
    return defaultValue;
  }

  private static boolean booleanOption(
      final ClassicalCalculationRequest request, final String name, final boolean defaultValue) {
    for (int i = 0; i < request.options().size(); ++i) {
      if (!name.equals(request.options().get(i).name())) {
        continue;
      }
      return Boolean.parseBoolean(request.options().get(i).value());
    }
    return defaultValue;
  }

  private static String referenceMethod(final ElectronicStructurePreparationResult result) {
    if (result.hasCasscfResult()) {
      return "CASSCF_ORBITAL_OPTIMIZED_RHF";
    }
    if (result.hasRhfReference()) {
      return "RHF";
    }
    if (result.hasRohfReference()) {
      return "ROHF";
    }
    if (result.hasUhfReference()) {
      return "UHF";
    }
    if (result.hasKohnShamDftReference()) {
      if (result.kohnShamDftResult().unrestricted()) {
        return "UNRESTRICTED_KOHN_SHAM_DFT";
      }
      return "KOHN_SHAM_DFT";
    }
    if (result.coreHamiltonianOrbitalReference()) {
      return "CORE_HAMILTONIAN_ORBITALS";
    }
    if (result.mode() == ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN) {
      return "EXPLICIT_EXTERNAL_HAMILTONIAN_BACKEND";
    }
    return "AO";
  }

  private static String referenceConverged(final ElectronicStructurePreparationResult result) {
    if (result.hasCasscfResult()) {
      return Boolean.toString(result.casscfResult().converged());
    }
    if (result.hasRhfReference()) {
      return Boolean.toString(result.rhfResult().converged());
    }
    if (result.hasRohfReference()) {
      return Boolean.toString(result.rohfResult().converged());
    }
    if (result.hasUhfReference()) {
      return Boolean.toString(result.uhfResult().converged());
    }
    if (result.hasKohnShamDftReference()) {
      return Boolean.toString(result.kohnShamDftResult().converged());
    }
    return "not_applicable";
  }

  private static double referenceEnergyOrNaN(final ElectronicStructurePreparationResult result) {
    if (result.hasCasscfResult()) {
      return result.casscfResult().totalEnergy();
    }
    if (result.hasRhfReference()) {
      return result.rhfResult().totalEnergy();
    }
    if (result.hasRohfReference()) {
      return result.rohfResult().totalEnergy();
    }
    if (result.hasUhfReference()) {
      return result.uhfResult().totalEnergy();
    }
    if (result.hasKohnShamDftReference()) {
      return result.kohnShamDftResult().totalEnergy();
    }
    return Double.NaN;
  }
}
