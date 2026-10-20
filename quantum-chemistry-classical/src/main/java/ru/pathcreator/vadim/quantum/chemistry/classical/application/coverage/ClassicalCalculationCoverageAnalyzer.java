/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPreflight;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.PostHartreeFockExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.DensityFunctionalCoverageProfile;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryEnergyModelKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.backend.ClassicalBackendExecutionMode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageItem;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCoverageStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.profile.ClassicalBackendProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalDefinition;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Проверяет, где локальный Java-native стек считает строго, а где использует приближенную модель.
 */
public final class ClassicalCalculationCoverageAnalyzer {

  private static final String SINGLE_POINT_BACKEND_OPTION = "single_point_backend";
  private static final String REACTION_ENERGY_MODEL_OPTION = "reaction_energy_model";
  private static final String REACTION_PREDICTION_ELECTRONIC_RERANK_OPTION =
      "reaction_prediction_electronic_rerank";
  private static final String ADVANCED_PREDICTION_ELECTRONIC_RERANK_OPTION =
      "advanced_prediction_electronic_rerank";
  private static final String SCF_INTEGRAL_MODE_OPTION = "scf_integral_mode";
  private static final String HAMILTONIAN_ALLOW_ONE_ELECTRON_FALLBACK_OPTION =
      "hamiltonian_allow_one_electron_fallback";
  private static final String DIRECT_SCF_INTEGRAL_MODE = "direct";
  private static final String MAX_UMP2_DENSE_SPIN_ORBITALS_OPTION = "max_ump2_dense_spin_orbitals";
  private static final String BASIS_LIST_OPTION = "basis_set_list";
  private static final String METHOD_LIST_OPTION = "method_list";
  private static final String DEFAULT_BASIS_BENCHMARK_LIST = "STO-3G,6-31G";
  private static final String DEFAULT_METHOD_BENCHMARK_LIST = "HF,MP2,LDA,PBE,B3LYP";
  private static final int DEFAULT_MAX_UMP2_DENSE_SPIN_ORBITALS = 24;

  private final ElectronicStructurePreparationPipeline preparationPipeline;

  public ClassicalCalculationCoverageAnalyzer() {
    this(new ElectronicStructurePreparationPipeline());
  }

  public ClassicalCalculationCoverageAnalyzer(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Classical coverage preparation pipeline must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
  }

  public ClassicalCalculationCoverageReport analyze(
      final ClassicalCalculationRequest request, final ClassicalBackendProfile profile) {
    if (request == null) {
      throw new IllegalArgumentException("Classical coverage request must not be null.");
    }
    if (profile == null) {
      throw new IllegalArgumentException("Classical coverage profile must not be null.");
    }
    final ArrayList<ClassicalCalculationCoverageItem> items =
        new ArrayList<ClassicalCalculationCoverageItem>(request.calculationKinds().size());
    final List<ClassicalCalculationKind> kinds = request.calculationKinds();
    for (int i = 0; i < kinds.size(); ++i) {
      items.add(this.analyzeKind(request, profile, kinds.get(i)));
    }
    return ClassicalCalculationCoverageReport.of(items);
  }

  private ClassicalCalculationCoverageItem analyzeKind(
      final ClassicalCalculationRequest request,
      final ClassicalBackendProfile profile,
      final ClassicalCalculationKind kind) {
    if (profile.executionMode() == ClassicalBackendExecutionMode.PLANNING_ONLY) {
      return this.exact(kind, "planning_only_capability_contract");
    }
    final var environmentDiagnostics =
        ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalEnvironmentOptions
            .validate(request, kind);
    if (!environmentDiagnostics.isEmpty()) {
      return ClassicalCalculationCoverageItem.of(
          kind,
          ClassicalCoverageStatus.UNSUPPORTED,
          "environment_options_not_applied",
          environmentDiagnostics);
    }
    if (kind == ClassicalCalculationKind.SINGLE_POINT_ENERGY) {
      return this.singlePointCoverage(request);
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_GRADIENT) {
      return this.geometryDerivativeCoverage(request, kind);
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION) {
      return this.geometryDerivativeCoverage(request, kind);
    }
    if (kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
        || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
        || kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS) {
      if (kind == ClassicalCalculationKind.THERMODYNAMIC_ANALYSIS
          && request.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
        return this.reactionEnergyCoverage(request, kind);
      }
      if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
        return this.unsupported(
            kind,
            "molecule_subject_required",
            ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
            "Hessian, frequency and molecule thermodynamic analysis require molecule subject.");
      }
      final GeometryOptimizationOptions options =
          GeometryOptimizationOptions.fromOptions(request.options());
      final Molecule molecule = (Molecule) request.subject();
      if ((kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
              || kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS)
          && this.pyScfHessianFrequencyBackendRequested(request)) {
        if (request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
            || request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
          return this.externalReference(kind, "pyscf_analytic_hessian_frequency_backend");
        }
        return this.unsupported(
            kind,
            "pyscf_analytic_hessian_frequency_backend",
            ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
            "PySCF Hessian/frequency backend supports HF and DFT Hessian workflows through this adapter.");
      }
      final boolean electronicFiniteDifference =
          options.energyModelKind()
                  == GeometryEnergyModelKind.ELECTRONIC_STRUCTURE_FINITE_DIFFERENCE
              || (options.energyModelKind() == GeometryEnergyModelKind.AUTO
                  && molecule.atomCount() <= options.autoElectronicMaxAtoms());
      if (electronicFiniteDifference) {
        final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
        if (unsupportedReason != null) {
          if (this.directHartreeFockAvailable(request)) {
            return this.exact(kind, "direct_gaussian_hartree_fock_finite_difference_hessian");
          }
          return this.unsupported(
              kind,
              "electronic_structure_finite_difference_hessian",
              this.unsupportedReasonCode(unsupportedReason),
              unsupportedReason);
        }
        return this.exact(kind, "electronic_structure_finite_difference_hessian");
      }
      return this.approximate(
          kind,
          "harmonic_force_field_thermochemistry",
          "Hessian, frequency and thermodynamic analysis uses harmonic local models, not full ab initio analytic Hessians.");
    }
    if (kind == ClassicalCalculationKind.SPECTROSCOPY_PREDICTION
        || kind == ClassicalCalculationKind.NMR_PREDICTION
        || kind == ClassicalCalculationKind.IR_RAMAN_PREDICTION
        || kind == ClassicalCalculationKind.MASS_SPECTROMETRY_PREDICTION
        || kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS) {
      if ((kind == ClassicalCalculationKind.SPECTROSCOPY_PREDICTION
              || kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS)
          && this.pyScfExcitedStateBackendRequested(request)) {
        if (request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
            || request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
          return this.externalReference(kind, "pyscf_tdhf_tddft_excited_state_backend");
        }
        return this.unsupported(
            kind,
            "pyscf_tdhf_tddft_excited_state_backend",
            ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
            "PySCF excited-state backend supports TDHF and TDDFT workflows through this adapter.");
      }
      if (kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS
          || this.nativeExcitedStateBackendRequested(request)) {
        final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
        if (unsupportedReason != null) {
          if (this.directHartreeFockAvailable(request)) {
            return this.approximate(
                kind,
                "direct_gaussian_hartree_fock_cis_tda_excited_state",
                "Excited-state analysis can use direct Java-native Hartree-Fock reference data, but the excited-state model is CIS/TDA-style approximate.");
          }
          return this.unsupported(
              kind,
              "java_native_cis_tda_excited_state",
              this.unsupportedReasonCode(unsupportedReason),
              unsupportedReason);
        }
        return this.approximate(
            kind,
            "java_native_cis_tda_excited_state",
            "Excited-state analysis uses Java-native CIS/TDA-style single excitations over the SCF reference, not full TDHF/TDDFT response.");
      }
      return this.approximate(
          kind,
          "descriptor_based_spectroscopy_and_excited_state_prediction",
          "Spectroscopy and excited-state prediction uses deterministic descriptors and condition proxies; production TD-DFT or spectra fitting remains backend-level.");
    }
    if (kind == ClassicalCalculationKind.CONFORMER_SEARCH) {
      return this.approximate(
          kind,
          "rule_based_conformer_sampling",
          "Conformer search uses deterministic local sampling and ranking, not a full global conformational search engine.");
    }
    if (kind == ClassicalCalculationKind.REACTION_PRODUCT_PREDICTION) {
      if (ClassicalCalculationCoverageAnalyzer.electronicPredictionRerankRequested(request)) {
        return this.approximate(
            kind,
            "topology_condition_and_java_native_electronic_rerank",
            "Reaction product prediction uses staged mechanism patterns, molecular graph topology, quantified condition and stoichiometry effects plus optional Java-native reaction-energy reranking; it remains a mechanistic predictor, not an outcome lookup table.");
      }
      return this.approximate(
          kind,
          "topology_condition_based_organic_prediction",
          "Reaction product prediction uses staged mechanism patterns, molecular graph topology, substrate features, quantified conditions and stoichiometry; it is not a hardcoded experimental outcome table.");
    }
    if (kind == ClassicalCalculationKind.REACTION_ENERGY) {
      return this.reactionEnergyCoverage(request, kind);
    }
    if (kind == ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS) {
      return this.molecularPropertyCoverage(request, kind);
    }
    if (kind == ClassicalCalculationKind.KINETIC_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_PATH
        || kind == ClassicalCalculationKind.IRC_ANALYSIS
        || kind == ClassicalCalculationKind.TRANSITION_STATE_SEARCH) {
      return this.approximate(
          kind,
          "internal_reaction_bond_change_path_model",
          "Reaction energetics, kinetics and paths use internal bond-change, condition and transition-state-theory models; full transition-state search remains a separate electronic-structure workflow.");
    }
    if (kind == ClassicalCalculationKind.SOLVATION_MODELING
        || kind == ClassicalCalculationKind.PKA_PREDICTION
        || kind == ClassicalCalculationKind.REDOX_POTENTIAL
        || kind == ClassicalCalculationKind.SELECTIVITY_ANALYSIS
        || kind == ClassicalCalculationKind.COMPETING_PATHWAY_ANALYSIS
        || kind == ClassicalCalculationKind.STEREOCHEMICAL_OUTCOME_ANALYSIS
        || kind == ClassicalCalculationKind.ENANTIOMER_DIASTEREOMER_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_CONDITION_SCREENING
        || kind == ClassicalCalculationKind.REACTION_CONDITION_OPTIMIZATION
        || kind == ClassicalCalculationKind.SOLVENT_SCREENING
        || kind == ClassicalCalculationKind.CATALYST_SCREENING
        || kind == ClassicalCalculationKind.CONCENTRATION_SCREENING
        || kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
        || kind == ClassicalCalculationKind.METHOD_BENCHMARK
        || kind == ClassicalCalculationKind.UNCERTAINTY_ANALYSIS
        || kind == ClassicalCalculationKind.MECHANISM_ENUMERATION
        || kind == ClassicalCalculationKind.REACTION_HAZARD_AND_STABILITY_ANALYSIS
        || kind == ClassicalCalculationKind.PRODUCT_DISTRIBUTION_ANALYSIS
        || kind == ClassicalCalculationKind.YIELD_WINDOW_ESTIMATION
        || kind == ClassicalCalculationKind.IMPURITY_AND_SIDE_PRODUCT_ANALYSIS
        || kind == ClassicalCalculationKind.SCALE_UP_RISK_ANALYSIS
        || kind == ClassicalCalculationKind.QUENCH_AND_WORKUP_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_HISTORY_TIMELINE_ANALYSIS
        || kind == ClassicalCalculationKind.REACTION_NETWORK_ANALYSIS
        || kind == ClassicalCalculationKind.MICROKINETIC_MODELING
        || kind == ClassicalCalculationKind.TRANSITION_STATE_ENSEMBLE_ANALYSIS
        || kind == ClassicalCalculationKind.RETROSYNTHESIS_PLANNING) {
      if (kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
          || kind == ClassicalCalculationKind.METHOD_BENCHMARK) {
        if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
          return this.unsupported(
              kind,
              this.pyScfBenchmarkBackendRequested(request, kind)
                  ? "pyscf_batch_single_point_benchmark_backend"
                  : ClassicalCalculationCoverageAnalyzer.nativeBenchmarkModel(kind),
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              "Benchmark calculation requires molecule subject.");
        }
        if (this.pyScfBenchmarkBackendRequested(request, kind)) {
          return this.externalReference(kind, "pyscf_batch_single_point_benchmark_backend");
        }
        return this.nativeBenchmarkCoverage(request, kind);
      }
      return this.approximate(
          kind,
          ClassicalCalculationCoverageAnalyzer.electronicPredictionRerankRequested(request)
              ? "deterministic_reaction_model_with_java_native_product_energy_rerank"
              : "deterministic_reaction_and_property_model",
          ClassicalCalculationCoverageAnalyzer.electronicPredictionRerankRequested(request)
              ? "This calculation uses Java-native molecular/reaction descriptors, topology-aware product candidates, quantified condition effects and candidate reaction-energy reranking where product candidates are generated."
              : "This calculation uses Java-native molecular/reaction descriptors, topology-aware product candidates and quantified condition effects to rank practical organic chemistry hypotheses.");
    }
    if (kind == ClassicalCalculationKind.ACTIVE_SPACE_SELECTION) {
      return this.approximate(
          kind,
          "orbital_energy_active_space_selection",
          "Active-space selection uses deterministic orbital-energy heuristics; chemically complete active-space choice still requires expert/backend validation.");
    }
    if (kind == ClassicalCalculationKind.ORBITAL_OPTIMIZATION) {
      return this.orbitalOptimizationCoverage(request, kind);
    }
    if (PostHartreeFockExecutor.supportsKind(kind)) {
      return this.postHartreeFockCoverage(request, kind);
    }
    if (kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
        || kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
        || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION) {
      return this.electronicPreparationCoverage(request, kind);
    }
    if (kind == ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION) {
      return this.quantumVqeOptimizationCoverage(request, kind);
    }
    if (kind == ClassicalCalculationKind.RELATIVISTIC_CORRECTION) {
      return this.approximate(
          kind,
          "scalar_spin_free_relativistic_screening",
          "Relativistic correction is scalar and spin-free; spin-orbit and four-component Dirac workflows require a backend.");
    }
    if (kind == ClassicalCalculationKind.PERIODIC_BOUNDARY_ANALYSIS) {
      return this.exact(kind, "cell_and_supercell_metadata_analysis");
    }
    if (kind == ClassicalCalculationKind.DESCRIPTOR_ANALYSIS) {
      return this.exact(kind, "deterministic_structure_descriptor_analysis");
    }
    return this.unsupported(
        kind,
        "unmapped_calculation_kind",
        ClassicalDiagnosticCode.CALCULATION_KIND_UNSUPPORTED,
        "No Java-native coverage rule exists for requested calculation kind.");
  }

  private static String nativeBenchmarkModel(final ClassicalCalculationKind kind) {
    if (kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE) {
      return "java_native_basis_convergence";
    }
    if (kind == ClassicalCalculationKind.METHOD_BENCHMARK) {
      return "java_native_method_benchmark";
    }
    throw new IllegalArgumentException("Unsupported native benchmark coverage kind.");
  }

  private ClassicalCalculationCoverageItem nativeBenchmarkCoverage(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    final String model = ClassicalCalculationCoverageAnalyzer.nativeBenchmarkModel(kind);
    final List<ClassicalCalculationCoverageItem> points =
        kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
            ? this.nativeBasisBenchmarkPointCoverage(request)
            : this.nativeMethodBenchmarkPointCoverage(request);
    int approximateCount = 0;
    int unsupportedCount = 0;
    final ArrayList<ClassicalDiagnostic> diagnostics = new ArrayList<ClassicalDiagnostic>();
    for (int i = 0; i < points.size(); ++i) {
      final ClassicalCalculationCoverageItem point = points.get(i);
      if (point.status() == ClassicalCoverageStatus.APPROXIMATE) {
        ++approximateCount;
      }
      if (point.status() == ClassicalCoverageStatus.UNSUPPORTED) {
        ++unsupportedCount;
      }
      diagnostics.addAll(point.diagnostics());
    }
    if (unsupportedCount == 0 && approximateCount == 0) {
      return this.exact(kind, model);
    }
    final String message =
        "Java-native benchmark coverage is mixed: "
            + unsupportedCount
            + " unsupported point(s), "
            + approximateCount
            + " approximate point(s). Unsupported rows will be reported explicitly by execution"
            + " and must not be treated as exact production-parity coverage.";
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("coverage", kind.name()),
            message));
    return ClassicalCalculationCoverageItem.of(
        kind, ClassicalCoverageStatus.APPROXIMATE, model, diagnostics);
  }

  private List<ClassicalCalculationCoverageItem> nativeBasisBenchmarkPointCoverage(
      final ClassicalCalculationRequest request) {
    final List<BasisSet> basisSets =
        ClassicalCalculationCoverageAnalyzer.basisSets(
            ClassicalCalculationCoverageAnalyzer.textOption(
                request.options(), BASIS_LIST_OPTION, DEFAULT_BASIS_BENCHMARK_LIST));
    final ArrayList<ClassicalCalculationCoverageItem> points =
        new ArrayList<ClassicalCalculationCoverageItem>(basisSets.size());
    for (int i = 0; i < basisSets.size(); ++i) {
      points.add(
          this.singlePointCoverage(
              this.syntheticSinglePointRequest(
                  request, request.method(), basisSets.get(i), "basis_coverage_" + i)));
    }
    return points;
  }

  private List<ClassicalCalculationCoverageItem> nativeMethodBenchmarkPointCoverage(
      final ClassicalCalculationRequest request) {
    final List<ElectronicStructureMethod> methods =
        ClassicalCalculationCoverageAnalyzer.methods(
            ClassicalCalculationCoverageAnalyzer.textOption(
                request.options(), METHOD_LIST_OPTION, DEFAULT_METHOD_BENCHMARK_LIST));
    final ArrayList<ClassicalCalculationCoverageItem> points =
        new ArrayList<ClassicalCalculationCoverageItem>(methods.size());
    for (int i = 0; i < methods.size(); ++i) {
      final ElectronicStructureMethod method =
          request.method().spinTreatment() == ElectronicStructureSpinTreatment.UNSPECIFIED
              ? methods.get(i)
              : methods.get(i).withSpinTreatment(request.method().spinTreatment());
      points.add(
          this.singlePointCoverage(
              this.syntheticSinglePointRequest(
                  request, method, request.basisSet(), "method_coverage_" + i)));
    }
    return points;
  }

  private ClassicalCalculationRequest syntheticSinglePointRequest(
      final ClassicalCalculationRequest request,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final String suffix) {
    return ClassicalCalculationRequest.of(
        ClassicalCalculationId.of(request.id().value() + "_" + suffix),
        request.subject(),
        method,
        basisSet,
        request.activeSpace(),
        List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
        ClassicalCalculationCoverageAnalyzer.nativeSinglePointOptions(request.options()),
        ChemistryMetadata.EMPTY);
  }

  private static boolean openShellSmallSpaceExactCcsdAvailable(
      final ClassicalCalculationRequest request, final Molecule molecule) {
    final int electronCount =
        request.hasActiveSpace()
            ? request.activeSpace().electronCount()
            : molecule.electronicConfiguration().electronCount();
    return electronCount > 0 && electronCount <= 2;
  }

  private ClassicalCalculationCoverageItem molecularPropertyCoverage(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    if (this.pyScfMolecularPropertyBackendRequested(request)) {
      if (request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
          || request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
        return this.externalReference(kind, "pyscf_molecular_property_backend");
      }
      return this.unsupported(
          kind,
          "pyscf_molecular_property_backend",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "PySCF molecular property backend supports HF and DFT property workflows through this adapter.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return this.unsupported(
          kind,
          "java_native_scf_molecular_properties",
          ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
          "Java-native molecular property analysis requires molecule subject.");
    }
    final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
    if (unsupportedReason != null) {
      if (this.directHartreeFockAvailable(request)) {
        return this.approximate(
            kind,
            "direct_gaussian_hartree_fock_molecular_properties",
            "Java-native molecular property analysis can use direct Hartree-Fock reference data; frontier, population and vertical IP/EA values remain model approximations.");
      }
      return this.unsupported(
          kind,
          "java_native_scf_molecular_properties",
          this.unsupportedReasonCode(unsupportedReason),
          unsupportedReason);
    }
    return this.approximate(
        kind,
        "java_native_scf_density_moment_mulliken_frontier_property_analysis",
        "Java-native molecular property analysis uses SCF frontier orbitals, Mulliken charges, density-moment dipole, point-charge dipole and Koopmans vertical IP/EA estimates.");
  }

  private ClassicalCalculationCoverageItem singlePointCoverage(
      final ClassicalCalculationRequest request) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return this.unsupported(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "molecular_electronic_structure_only",
          ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
          "Single-point electronic energy requires molecule subject.");
    }
    if (this.pyScfSinglePointBackendRequested(request)) {
      return this.pyScfSinglePointCoverage(request);
    }
    final ElectronicStructureMethodType type = request.method().type();
    if (ClassicalCalculationCoverageAnalyzer.quantumExecutionMethod(type)) {
      return this.unsupported(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "quantum_backend_execution_required",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "Single-point energy cannot execute VQE/QPE directly. Use quantum program preparation"
              + " to produce Quantum IR, then run that program through a quantum backend.");
    }
    final String unsupportedReason =
        type == ElectronicStructureMethodType.HARTREE_FOCK
                || (type == ElectronicStructureMethodType.PERTURBATION_THEORY
                    && this.mp2Method(request.method().name().value()))
            ? this.preparationPipeline.unsupportedReason(request)
            : this.preparationPipeline.restrictedClosedShellUnsupportedReason(request);
    if (type == ElectronicStructureMethodType.HARTREE_FOCK
        && this.directRestrictedHartreeFockAvailable(request, unsupportedReason)) {
      return this.exact(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY, "direct_gaussian_restricted_hartree_fock");
    }
    if (type == ElectronicStructureMethodType.HARTREE_FOCK
        && this.directUnrestrictedHartreeFockAvailable(request, unsupportedReason)) {
      return this.exact(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "direct_gaussian_unrestricted_hartree_fock");
    }
    if (unsupportedReason != null) {
      return this.unsupported(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "gaussian_integral_preparation",
          this.unsupportedReasonCode(unsupportedReason),
          unsupportedReason);
    }
    if (type == ElectronicStructureMethodType.HARTREE_FOCK) {
      return this.exact(ClassicalCalculationKind.SINGLE_POINT_ENERGY, "dense_gaussian_rhf_or_uhf");
    }
    if (type == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
      final DensityFunctionalDefinition functionalDefinition =
          request.method().hasDensityFunctionalDefinition()
              ? request.method().densityFunctionalDefinition()
              : DensityFunctionalDefinition.resolve(request.method().name().value());
      final DensityFunctionalCoverageProfile coverageProfile =
          DensityFunctionalCoverageProfile.of(functionalDefinition);
      if (coverageProfile.completeLocalDensityGrid()) {
        return this.exact(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            coverageProfile.solverModel().toLowerCase(Locale.ROOT));
      }
      if (coverageProfile.gridModelAvailable()) {
        return this.approximate(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            coverageProfile.solverModel().toLowerCase(Locale.ROOT),
            coverageProfile.approximationScope());
      }
      return this.approximate(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          coverageProfile.solverModel().toLowerCase(Locale.ROOT),
          coverageProfile.approximationScope());
    }
    if (type == ElectronicStructureMethodType.PERTURBATION_THEORY) {
      if (this.mp2Method(request.method().name().value())) {
        final Molecule molecule = (Molecule) request.subject();
        if (!molecule.electronicConfiguration().closedShell()) {
          final String denseLimitReason = this.unrestrictedMp2DenseKernelUnsupportedReason(request);
          if (denseLimitReason != null) {
            return this.unsupported(
                ClassicalCalculationKind.SINGLE_POINT_ENERGY,
                "unrestricted_spin_orbital_mp2_dense_resource_guard",
                ClassicalDiagnosticCode.MOLECULE_TOO_LARGE,
                denseLimitReason);
          }
          return this.exact(
              ClassicalCalculationKind.SINGLE_POINT_ENERGY,
              "unrestricted_spin_orbital_mp2_over_dense_gaussian_integrals");
        }
        return this.exact(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            "closed_shell_mp2_over_dense_gaussian_integrals");
      }
      return this.unsupported(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "unsupported_perturbation_theory",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "Java-native perturbation theory currently covers closed-shell MP2 and unrestricted MP2.");
    }
    if (type == ElectronicStructureMethodType.COUPLED_CLUSTER) {
      final Molecule molecule = (Molecule) request.subject();
      if (!molecule.electronicConfiguration().closedShell()) {
        if (this.ccsdMethod(request.method().name().value())
            && ClassicalCalculationCoverageAnalyzer.openShellSmallSpaceExactCcsdAvailable(
                request, molecule)) {
          return this.exact(
              ClassicalCalculationKind.SINGLE_POINT_ENERGY,
              "open_shell_small_space_ccsd_exact_diagonalization");
        }
        return this.unsupported(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            "open_shell_coupled_cluster_requires_spin_orbital_solver",
            ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
            "Java-native coupled-cluster covers closed-shell CCD/CCSD and open-shell one-/two-electron CCSD through exact determinant-space equivalence; larger open-shell CCSD requires a separate UHF/spin-orbital solver.");
      }
      if (this.ccdMethod(request.method().name().value())) {
        return this.exact(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            "closed_shell_coupled_cluster_doubles_over_dense_gaussian_integrals");
      }
      if (this.ccsdMethod(request.method().name().value())) {
        return this.exact(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            "closed_shell_ccsd_singles_doubles_over_dense_gaussian_integrals");
      }
      return this.unsupported(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "unsupported_coupled_cluster_method",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "Java-native coupled-cluster currently covers closed-shell CCD and CCSD.");
    }
    if (type == ElectronicStructureMethodType.CONFIGURATION_INTERACTION) {
      if (this.cisdMethod(request.method().name().value())) {
        return this.exact(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            "determinant_space_cisd_over_dense_hamiltonian");
      }
      return this.unsupported(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "unsupported_configuration_interaction_method",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "Java-native configuration interaction currently covers CISD.");
    }
    if (type == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      if (!request.hasActiveSpace()) {
        return this.approximate(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            "automatic_active_space_selection_required",
            "Java-native multiconfigurational calculation will first select active space from orbital energies.");
      }
      if (this.casciMethod(request.method().name().value())) {
        return this.exact(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            "active_space_casci_over_dense_hamiltonian");
      }
      if (this.casscfMethod(request.method().name().value())) {
        return this.exact(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY,
            "active_space_casscf_orbital_optimization");
      }
      return this.unsupported(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "unsupported_multiconfigurational_method",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "Java-native multiconfigurational calculation currently covers CASCI and CASSCF.");
    }
    if (type == ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION) {
      return this.exact(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY, "active_space_fci_over_dense_hamiltonian");
    }
    return this.unsupported(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY,
        "unsupported_single_point_method",
        ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
        "Java-native single-point executor supports RHF, UHF, Kohn-Sham DFT, closed-shell MP2, unrestricted MP2, CCD, CCSD, CISD, CASCI, CASSCF and active-space FCI.");
  }

  private ClassicalCalculationCoverageItem reactionEnergyCoverage(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.REACTION) {
      return this.unsupported(
          kind,
          "reaction_subject_required",
          ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
          "Reaction energy requires reaction subject.");
    }
    if (ClassicalCalculationCoverageAnalyzer.forceBondEnergyModel(request)) {
      return this.approximate(
          kind,
          "internal_reaction_bond_energy_model",
          "Reaction energy was explicitly requested through the internal bond-energy model.");
    }
    final Reaction reaction = (Reaction) request.subject();
    final ReactionSideCoverage reactants =
        this.reactionSideSinglePointCoverage(request, reaction.reactants(), "reactant");
    final ReactionSideCoverage products =
        this.reactionSideSinglePointCoverage(request, reaction.products(), "product");
    if (reactants.unsupported || products.unsupported) {
      return this.approximate(
          kind,
          "internal_reaction_bond_energy_fallback",
          "Java-native first-principles reaction energy is unavailable for at least one"
              + " participant; execution falls back to internal bond-energy thermochemistry.");
    }
    if (reactants.approximate || products.approximate) {
      return this.approximate(
          kind,
          "stoichiometric_sum_of_approximate_java_native_single_point_energies",
          "Reaction energy sums Java-native participant single-point energies, but at least one"
              + " participant single-point model is approximate.");
    }
    return this.exact(kind, "stoichiometric_sum_of_java_native_single_point_energies");
  }

  private ClassicalCalculationCoverageItem orbitalOptimizationCoverage(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return this.unsupported(
          kind,
          "molecular_electronic_structure_only",
          ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
          "Orbital optimization requires molecule subject.");
    }
    if (!request.hasActiveSpace()) {
      return this.approximate(
          kind,
          "automatic_active_space_selection_required",
          "Orbital optimization will first select active space from Java-native orbital energies.");
    }
    if (request.method().type()
            != ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD
        || !this.casscfMethod(request.method().name().value())) {
      return this.unsupported(
          kind,
          "casscf_method_required",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "Java-native orbital optimization currently uses CASSCF/MCSCF active-space functional.");
    }
    final String unsupportedReason =
        this.preparationPipeline.restrictedClosedShellUnsupportedReason(request);
    if (unsupportedReason != null) {
      if (this.preparationPipeline.canPrepareDirectActiveSpaceHamiltonian(request)) {
        return this.exact(kind, "direct_active_space_casscf_orbital_optimization");
      }
      return this.unsupported(
          kind,
          "gaussian_integral_casscf_orbital_optimization",
          this.unsupportedReasonCode(unsupportedReason),
          unsupportedReason);
    }
    return this.exact(kind, "active_space_casscf_orbital_optimization");
  }

  private ClassicalCalculationCoverageItem postHartreeFockCoverage(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return this.unsupported(
          kind,
          "molecular_post_hartree_fock_only",
          ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
          "Post-Hartree-Fock correlation requires molecule subject.");
    }
    final Molecule molecule = (Molecule) request.subject();
    final ElectronicStructurePreparationPreflight preflight =
        this.preparationPipeline.preflight(request);
    if (!preflight.supported()) {
      final String unsupportedReason = preflight.unsupportedReason();
      if (this.preparationPipeline.canPrepareDirectActiveSpaceHamiltonian(request)) {
        return this.exact(kind, "direct_active_space_post_hartree_fock");
      }
      return this.unsupported(
          kind,
          "dense_gaussian_post_hartree_fock_preparation",
          this.unsupportedReasonCode(unsupportedReason),
          unsupportedReason);
    }
    if ((kind == ClassicalCalculationKind.MP2_CORRELATION
            || kind == ClassicalCalculationKind.CCSD_CORRELATION
            || kind == ClassicalCalculationKind.POST_HF_CORRELATION)
        && !molecule.electronicConfiguration().closedShell()) {
      return this.unsupported(
          kind,
          "closed_shell_reference_post_hartree_fock",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "Java-native MP2 and CCSD post-HF executors require closed-shell RHF reference; use single-point unrestricted MP2 for open-shell MP2 energy.");
    }
    if ((kind == ClassicalCalculationKind.CISD_CORRELATION
            || kind == ClassicalCalculationKind.FCI_DIAGONALIZATION
            || kind == ClassicalCalculationKind.POST_HF_CORRELATION)
        && !preflight.hasFullTensorPreflight()
        && !preflight.directActiveSpaceSupported()
        && !preflight.backendHamiltonianSupported()) {
      return this.unsupported(
          kind,
          "two_electron_hamiltonian_post_hartree_fock",
          ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED,
          "CISD, FCI and complete post-HF suite require a two-electron Hamiltonian.");
    }
    if (kind == ClassicalCalculationKind.MP2_CORRELATION) {
      return this.exact(kind, "closed_shell_mp2_over_dense_mo_integrals");
    }
    if (kind == ClassicalCalculationKind.CCSD_CORRELATION) {
      return this.exact(kind, "closed_shell_ccsd_over_dense_mo_integrals");
    }
    if (kind == ClassicalCalculationKind.CISD_CORRELATION) {
      return this.exact(kind, "determinant_space_cisd_over_two_electron_hamiltonian");
    }
    if (kind == ClassicalCalculationKind.FCI_DIAGONALIZATION) {
      return this.exact(kind, "determinant_space_fci_over_two_electron_hamiltonian");
    }
    return this.exact(kind, "java_native_post_hartree_fock_suite");
  }

  private ReactionSideCoverage reactionSideSinglePointCoverage(
      final ClassicalCalculationRequest request, final ReactionSide side, final String sideName) {
    boolean approximate = false;
    final List<ReactionParticipant> participants = side.participants();
    for (int i = 0; i < participants.size(); ++i) {
      final ClassicalCalculationCoverageItem item =
          this.singlePointCoverage(
              ClassicalCalculationRequest.of(
                  ClassicalCalculationId.of(request.id().value() + "_" + sideName + "_" + i),
                  participants.get(i).molecule(),
                  request.method(),
                  request.basisSet(),
                  request.activeSpace(),
                  List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
                  ClassicalCalculationCoverageAnalyzer.nativeSinglePointOptions(request.options()),
                  ChemistryMetadata.EMPTY));
      if (item.status() == ClassicalCoverageStatus.UNSUPPORTED) {
        return ReactionSideCoverage.unsupported();
      }
      if (item.status() == ClassicalCoverageStatus.APPROXIMATE) {
        approximate = true;
      }
    }
    return approximate ? ReactionSideCoverage.approximate() : ReactionSideCoverage.exact();
  }

  private static List<ClassicalCalculationOption> nativeSinglePointOptions(
      final List<ClassicalCalculationOption> options) {
    final ArrayList<ClassicalCalculationOption> result =
        new ArrayList<ClassicalCalculationOption>();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (SINGLE_POINT_BACKEND_OPTION.equals(option.name())) {
        continue;
      }
      result.add(option);
    }
    return List.copyOf(result);
  }

  private static boolean forceBondEnergyModel(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!REACTION_ENERGY_MODEL_OPTION.equals(option.name())) {
        continue;
      }
      return option.value().equalsIgnoreCase("bond")
          || option.value().equalsIgnoreCase("bond_energy")
          || option.value().equalsIgnoreCase("heuristic");
    }
    return false;
  }

  private static boolean quantumExecutionMethod(final ElectronicStructureMethodType type) {
    return type == ElectronicStructureMethodType.QUANTUM_VARIATIONAL
        || type == ElectronicStructureMethodType.QUANTUM_PHASE_ESTIMATION;
  }

  private static boolean electronicPredictionRerankRequested(
      final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (REACTION_PREDICTION_ELECTRONIC_RERANK_OPTION.equals(option.name())
          || ADVANCED_PREDICTION_ELECTRONIC_RERANK_OPTION.equals(option.name())) {
        return Boolean.parseBoolean(option.value());
      }
    }
    return true;
  }

  private ClassicalCalculationCoverageItem pyScfSinglePointCoverage(
      final ClassicalCalculationRequest request) {
    final ElectronicStructureMethodType type = request.method().type();
    if (type == ElectronicStructureMethodType.HARTREE_FOCK
        || type == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
        || type == ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION) {
      return this.externalReference(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY, "pyscf_single_point_backend");
    }
    if (type == ElectronicStructureMethodType.PERTURBATION_THEORY
        && this.mp2Method(request.method().name().value())) {
      return this.externalReference(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY, "pyscf_single_point_backend");
    }
    if (type == ElectronicStructureMethodType.COUPLED_CLUSTER
        && this.ccsdMethod(request.method().name().value())) {
      return this.externalReference(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY, "pyscf_single_point_backend");
    }
    if (type == ElectronicStructureMethodType.CONFIGURATION_INTERACTION
        && this.cisdMethod(request.method().name().value())) {
      return this.externalReference(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY, "pyscf_cisd_single_point_backend");
    }
    if (type == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD
        && this.casscfMethod(request.method().name().value())) {
      if (request.hasActiveSpace()) {
        return this.externalReference(
            ClassicalCalculationKind.SINGLE_POINT_ENERGY, "pyscf_casscf_single_point_backend");
      }
      return this.unsupported(
          ClassicalCalculationKind.SINGLE_POINT_ENERGY,
          "pyscf_casscf_single_point_backend",
          ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
          "PySCF CASSCF backend requires an explicit active space.");
    }
    return this.unsupported(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY,
        "pyscf_single_point_backend",
        ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
        "PySCF single-point backend supports HF, DFT, MP2, CCSD, CISD, FCI and active-space CASSCF through this adapter.");
  }

  private ClassicalCalculationCoverageItem geometryDerivativeCoverage(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return this.unsupported(
          kind,
          "molecular_geometry_only",
          ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
          "Geometry derivative calculation requires molecule subject.");
    }
    final Molecule molecule = (Molecule) request.subject();
    final String coordinateError =
        ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry
            .GeometryCoordinateSupport.coordinateError(molecule);
    if (coordinateError != null) {
      return this.unsupported(
          kind,
          "molecular_geometry_coordinates",
          ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
          coordinateError);
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_GRADIENT
        && this.pyScfGeometryGradientBackendRequested(request)) {
      if (request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
          || request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
          || (request.method().type() == ElectronicStructureMethodType.PERTURBATION_THEORY
              && this.mp2Method(request.method().name().value()))
          || (request.method().type() == ElectronicStructureMethodType.COUPLED_CLUSTER
              && this.ccsdMethod(request.method().name().value()))) {
        return this.externalReference(kind, "pyscf_analytic_geometry_gradient_backend");
      }
      return this.unsupported(
          kind,
          "pyscf_analytic_geometry_gradient_backend",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "PySCF analytic gradient backend supports HF, DFT, MP2 and CCSD gradients through this adapter.");
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_OPTIMIZATION
        && this.pyScfGeometryOptimizationBackendRequested(request)) {
      if (request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
          || request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
        return this.externalReference(kind, "pyscf_geometric_geometry_optimization_backend");
      }
      return this.unsupported(
          kind,
          "pyscf_geometric_geometry_optimization_backend",
          ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
          "PySCF geometry optimization backend supports HF and DFT workflows through this adapter.");
    }
    final GeometryOptimizationOptions options =
        GeometryOptimizationOptions.fromOptions(request.options());
    final boolean electronicFiniteDifference =
        options.energyModelKind() == GeometryEnergyModelKind.ELECTRONIC_STRUCTURE_FINITE_DIFFERENCE
            || (options.energyModelKind() == GeometryEnergyModelKind.AUTO
                && molecule.atomCount() <= options.autoElectronicMaxAtoms());
    if (electronicFiniteDifference) {
      final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
      if (unsupportedReason != null) {
        if (this.directHartreeFockAvailable(request)) {
          return this.exact(kind, "direct_gaussian_hartree_fock_finite_difference_geometry");
        }
        return this.unsupported(
            kind,
            "electronic_structure_finite_difference_geometry",
            this.unsupportedReasonCode(unsupportedReason),
            unsupportedReason);
      }
      return this.exact(kind, "electronic_structure_finite_difference_geometry");
    }
    if (kind == ClassicalCalculationKind.GEOMETRY_GRADIENT) {
      return this.approximate(
          kind,
          "bonded_force_field_gradient",
          "Geometry gradient uses deterministic bonded force-field derivatives for larger auto-mode molecules unless electronic finite-difference is requested.");
    }
    return this.approximate(
        kind,
        "bonded_force_field_bfgs_line_search_geometry_optimization",
        "Geometry optimization uses local BFGS line-search over bonded force-field gradients for larger auto-mode molecules unless electronic finite-difference is requested.");
  }

  private ClassicalCalculationCoverageItem electronicPreparationCoverage(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    final ElectronicStructurePreparationPreflight preflight =
        this.preparationPipeline.preflight(request);
    if (!preflight.supported()) {
      final String unsupportedReason = preflight.unsupportedReason();
      final String oneElectronUnsupportedReason =
          this.preparationPipeline.oneElectronUnsupportedReason(request);
      if (kind == ClassicalCalculationKind.ELECTRONIC_STRUCTURE_PREPARATION
          && this.directHartreeFockAvailable(request)) {
        return this.exact(kind, "direct_gaussian_hartree_fock_reference_preparation");
      }
      if ((kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
              || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION)
          && this.preparationPipeline.canPrepareDirectActiveSpaceHamiltonian(request)) {
        return this.exact(kind, "exact_direct_active_space_hamiltonian_without_full_ao_tensor");
      }
      if ((kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
              || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION)
          && !request.hasActiveSpace()
          && this.preparationPipeline.oneElectronUnsupportedReason(request) == null) {
        return this.approximate(
            kind,
            "automatic_active_space_selection_then_direct_hamiltonian",
            "Active space will be selected from Java-native orbital energies before direct Hamiltonian preparation.");
      }
      if ((kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
              || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION)
          && oneElectronUnsupportedReason == null
          && this.booleanOption(request, HAMILTONIAN_ALLOW_ONE_ELECTRON_FALLBACK_OPTION, false)) {
        return this.approximate(
            kind,
            "one_electron_hamiltonian_fallback",
            "Full two-electron Hamiltonian preparation is unavailable, but one-electron Hamiltonian fallback is available: "
                + unsupportedReason);
      }
      return this.unsupported(
          kind,
          "gaussian_integral_hamiltonian_preparation",
          this.unsupportedReasonCode(unsupportedReason),
          unsupportedReason);
    }
    if ((kind == ClassicalCalculationKind.HAMILTONIAN_PREPARATION
            || kind == ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION)
        && preflight.directActiveSpaceSupported()) {
      return this.exact(kind, "exact_direct_active_space_hamiltonian_without_full_ao_tensor");
    }
    if (request.hasActiveSpace()
        && preflight.hasFullTensorPreflight()
        && request.activeSpace().orbitalCount() < preflight.fullTensorPreflight().orbitalCount()) {
      return this.approximate(
          kind,
          "active_space_projected_hamiltonian",
          "Hamiltonian preparation projects into the requested active space; frozen-core correlation outside the active space is intentionally not represented.");
    }
    if (preflight.backendHamiltonianSupported()) {
      return this.externalReference(
          kind,
          preflight.backendDescription().toLowerCase(java.util.Locale.ROOT).replace(' ', '_'));
    }
    return this.exact(kind, "dense_gaussian_electronic_hamiltonian_preparation");
  }

  private ClassicalCalculationCoverageItem quantumVqeOptimizationCoverage(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      return this.unsupported(
          kind,
          "molecular_vqe_optimization_only",
          ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
          "Quantum VQE optimization requires molecule subject.");
    }
    final ClassicalCalculationCoverageItem preparation =
        this.electronicPreparationCoverage(
            request, ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION);
    if (preparation.status() == ClassicalCoverageStatus.UNSUPPORTED) {
      return this.unsupported(
          kind,
          "vqe_hamiltonian_preparation",
          preparation.diagnostics().isEmpty()
              ? ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED
              : preparation.diagnostics().get(0).code(),
          preparation.diagnostics().isEmpty()
              ? "VQE requires a preparable electronic Hamiltonian."
              : preparation.diagnostics().get(0).message());
    }
    if (preparation.status() == ClassicalCoverageStatus.APPROXIMATE) {
      return this.approximate(
          kind,
          "state_vector_vqe_over_approximate_hamiltonian",
          "VQE optimizer is Java-native, but Hamiltonian preparation requires approximation: "
              + preparation.model());
    }
    return this.exact(kind, "state_vector_vqe_optimizer_over_verified_pauli_hamiltonian");
  }

  private ClassicalCalculationCoverageItem exact(
      final ClassicalCalculationKind kind, final String model) {
    return ClassicalCalculationCoverageItem.of(
        kind, ClassicalCoverageStatus.EXACT, model, List.of());
  }

  private ClassicalCalculationCoverageItem approximate(
      final ClassicalCalculationKind kind, final String model, final String message) {
    return ClassicalCalculationCoverageItem.of(
        kind,
        ClassicalCoverageStatus.APPROXIMATE,
        model,
        List.of(
            ClassicalDiagnostic.warning(
                ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
                ClassicalDiagnosticTarget.of("coverage", kind.name()),
                message)));
  }

  private ClassicalCalculationCoverageItem externalReference(
      final ClassicalCalculationKind kind, final String model) {
    return this.approximate(
        kind,
        model,
        "This route uses an explicitly requested external reference adapter. It is useful for"
            + " comparison/regression, but it is not Java-native production parity.");
  }

  private ClassicalCalculationCoverageItem unsupported(
      final ClassicalCalculationKind kind,
      final String model,
      final ClassicalDiagnosticCode code,
      final String message) {
    return ClassicalCalculationCoverageItem.of(
        kind,
        ClassicalCoverageStatus.UNSUPPORTED,
        model,
        List.of(
            ClassicalDiagnostic.error(
                code, ClassicalDiagnosticTarget.of("coverage", kind.name()), message)));
  }

  private ClassicalDiagnosticCode unsupportedReasonCode(final String reason) {
    final String message = reason.toLowerCase(Locale.ROOT);
    if (message.contains("active space") || message.contains("active-space")) {
      return ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED;
    }
    if (message.contains("ecp") || message.contains("pseudopotential")) {
      return ClassicalDiagnosticCode.BASIS_ECP_UNSUPPORTED;
    }
    if (message.contains("angular momentum") || message.contains("derivative order")) {
      return ClassicalDiagnosticCode.BASIS_ANGULAR_MOMENTUM_UNSUPPORTED;
    }
    if (message.contains("basis") || message.contains("gaussian") || message.contains("orbital")) {
      return ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED;
    }
    if (message.contains("coordinate") || message.contains("geometry")) {
      return ClassicalDiagnosticCode.GEOMETRY_REQUIRED;
    }
    if (message.contains("molecule subject")) {
      return ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED;
    }
    return ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED;
  }

  private boolean mp2Method(final String methodName) {
    final String normalized = methodName.toLowerCase(Locale.ROOT);
    return normalized.equals("mp2")
        || normalized.contains("moller")
        || normalized.contains("moeller")
        || normalized.contains("second order");
  }

  private String unrestrictedMp2DenseKernelUnsupportedReason(
      final ClassicalCalculationRequest request) {
    final ElectronicStructurePreparationPreflight preflight =
        this.preparationPipeline.preflight(request);
    if (!preflight.hasFullTensorPreflight()) {
      return null;
    }
    final int orbitalCount = preflight.fullTensorPreflight().orbitalCount();
    final int maxOrbitals =
        ClassicalCalculationCoverageAnalyzer.integerOption(
            request, MAX_UMP2_DENSE_SPIN_ORBITALS_OPTION, DEFAULT_MAX_UMP2_DENSE_SPIN_ORBITALS);
    if (orbitalCount <= maxOrbitals) {
      return null;
    }
    return "UMP2 dense spin-block kernel requires three O(n^4) transformed ERI tensors for "
        + orbitalCount
        + " orbitals; configured max_ump2_dense_spin_orbitals is "
        + maxOrbitals
        + ". Use an active-space calculation or a direct/sparse post-HF kernel.";
  }

  private static int integerOption(
      final ClassicalCalculationRequest request, final String name, final int defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(name)) {
        continue;
      }
      try {
        return Integer.parseInt(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(
            "Classical calculation option " + name + " must be an integer.");
      }
    }
    return defaultValue;
  }

  private static String textOption(
      final List<ClassicalCalculationOption> options,
      final String name,
      final String defaultValue) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(name)) {
        return option.value();
      }
    }
    return defaultValue;
  }

  private static List<BasisSet> basisSets(final String text) {
    final String[] parts = text.split(",");
    final ArrayList<BasisSet> basisSets = new ArrayList<BasisSet>(parts.length);
    for (int i = 0; i < parts.length; ++i) {
      final String name = parts[i].trim();
      if (name.isEmpty()) {
        continue;
      }
      basisSets.add(BasisSet.of(BasisSetName.of(name)));
    }
    if (basisSets.isEmpty()) {
      throw new IllegalArgumentException(
          "Native benchmark basis_set_list must contain at least one basis set.");
    }
    return basisSets;
  }

  private static List<ElectronicStructureMethod> methods(final String text) {
    final String[] parts = text.split(",");
    final ArrayList<ElectronicStructureMethod> methods =
        new ArrayList<ElectronicStructureMethod>(parts.length);
    for (int i = 0; i < parts.length; ++i) {
      final String name = parts[i].trim();
      if (name.isEmpty()) {
        continue;
      }
      methods.add(ClassicalCalculationCoverageAnalyzer.method(name));
    }
    if (methods.isEmpty()) {
      throw new IllegalArgumentException(
          "Native benchmark method_list must contain at least one method.");
    }
    return methods;
  }

  private static ElectronicStructureMethod method(final String name) {
    final String normalized = name.toLowerCase(Locale.ROOT);
    if ("hf".equals(normalized) || "hartree-fock".equals(normalized)) {
      return ElectronicStructureMethod.HARTREE_FOCK;
    }
    if ("lda".equals(normalized) || "b3lyp".equals(normalized) || "pbe".equals(normalized)) {
      return ElectronicStructureMethod.densityFunctional(name);
    }
    if ("mp2".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.PERTURBATION_THEORY,
          ElectronicStructureMethodName.of("MP2"));
    }
    if ("ccd".equals(normalized) || "ccsd".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.COUPLED_CLUSTER,
          ElectronicStructureMethodName.of(name.toUpperCase(Locale.ROOT)));
    }
    if ("cisd".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.CONFIGURATION_INTERACTION,
          ElectronicStructureMethodName.of("CISD"));
    }
    if ("casci".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
          ElectronicStructureMethodName.of("CASCI"));
    }
    if ("casscf".equals(normalized) || "mcscf".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
          ElectronicStructureMethodName.of(name.toUpperCase(Locale.ROOT)));
    }
    if ("fci".equals(normalized) || "full-ci".equals(normalized) || "full ci".equals(normalized)) {
      return ElectronicStructureMethod.of(
          ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION,
          ElectronicStructureMethodName.of("FCI"));
    }
    return ElectronicStructureMethod.of(
        ElectronicStructureMethodType.CUSTOM, ElectronicStructureMethodName.of(name));
  }

  private boolean ccdMethod(final String methodName) {
    final String normalized = methodName.toLowerCase(Locale.ROOT);
    return normalized.equals("ccd")
        || normalized.contains("coupled cluster doubles")
        || normalized.contains("coupled-cluster doubles");
  }

  private boolean ccsdMethod(final String methodName) {
    return methodName.toLowerCase(Locale.ROOT).equals("ccsd");
  }

  private boolean cisdMethod(final String methodName) {
    final String normalized = methodName.toLowerCase(Locale.ROOT);
    return normalized.equals("cisd")
        || normalized.contains("configuration interaction singles doubles")
        || normalized.contains("configuration interaction singles and doubles")
        || normalized.contains("configuration-interaction singles-doubles")
        || normalized.contains("configuration-interaction singles and doubles");
  }

  private boolean casciMethod(final String methodName) {
    final String normalized = methodName.toLowerCase(Locale.ROOT);
    return normalized.equals("casci")
        || normalized.contains("complete active space configuration interaction");
  }

  private boolean casscfMethod(final String methodName) {
    final String normalized = methodName.toLowerCase(Locale.ROOT);
    return normalized.equals("casscf")
        || normalized.equals("mcscf")
        || normalized.contains("multi configurational")
        || normalized.contains("multi-configurational");
  }

  private boolean pyScfSinglePointBackendRequested(final ClassicalCalculationRequest request) {
    final List<
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                .ClassicalCalculationOption>
        options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationOption
          option = options.get(i);
      if (!option.name().equals("single_point_backend")) {
        continue;
      }
      return option.value().equalsIgnoreCase("pyscf");
    }
    return false;
  }

  private boolean directRestrictedHartreeFockAvailable(
      final ClassicalCalculationRequest request, final String fullTensorUnsupportedReason) {
    final Molecule molecule = (Molecule) request.subject();
    if (!molecule.electronicConfiguration().closedShell()) {
      return false;
    }
    if (this.preparationPipeline.oneElectronUnsupportedReason(request) != null) {
      return false;
    }
    return this.directScfRequested(request) || fullTensorUnsupportedReason != null;
  }

  private boolean directUnrestrictedHartreeFockAvailable(
      final ClassicalCalculationRequest request, final String fullTensorUnsupportedReason) {
    final Molecule molecule = (Molecule) request.subject();
    if (!molecule.electronicConfiguration().openShell()
        && request.method().spinTreatment() != ElectronicStructureSpinTreatment.UNRESTRICTED) {
      return false;
    }
    if (this.preparationPipeline.oneElectronUnsupportedReason(request) != null) {
      return false;
    }
    return this.directScfRequested(request) || fullTensorUnsupportedReason != null;
  }

  private boolean directHartreeFockAvailable(final ClassicalCalculationRequest request) {
    return request.subject().subjectKind() == ChemistrySubjectKind.MOLECULE
        && request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
        && this.preparationPipeline.canPrepareDirectHartreeFock(request);
  }

  private boolean directScfRequested(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!SCF_INTEGRAL_MODE_OPTION.equals(option.name())) {
        continue;
      }
      return DIRECT_SCF_INTEGRAL_MODE.equalsIgnoreCase(option.value());
    }
    return false;
  }

  private boolean booleanOption(
      final ClassicalCalculationRequest request, final String name, final boolean defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!name.equals(option.name())) {
        continue;
      }
      return Boolean.parseBoolean(option.value());
    }
    return defaultValue;
  }

  private boolean pyScfGeometryGradientBackendRequested(final ClassicalCalculationRequest request) {
    final List<
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                .ClassicalCalculationOption>
        options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationOption
          option = options.get(i);
      if (!option.name().equals("geometry_gradient_backend")
          && !option.name().equals("gradient_backend")) {
        continue;
      }
      return option.value().equalsIgnoreCase("pyscf");
    }
    return false;
  }

  private boolean pyScfHessianFrequencyBackendRequested(final ClassicalCalculationRequest request) {
    final List<
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                .ClassicalCalculationOption>
        options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationOption
          option = options.get(i);
      if (!option.name().equals("hessian_backend")
          && !option.name().equals("frequency_backend")
          && !option.name().equals("vibrational_backend")) {
        continue;
      }
      return option.value().equalsIgnoreCase("pyscf");
    }
    return false;
  }

  private boolean pyScfGeometryOptimizationBackendRequested(
      final ClassicalCalculationRequest request) {
    final List<
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                .ClassicalCalculationOption>
        options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationOption
          option = options.get(i);
      if (!option.name().equals("geometry_optimization_backend")
          && !option.name().equals("optimization_backend")) {
        continue;
      }
      return option.value().equalsIgnoreCase("pyscf");
    }
    return false;
  }

  private boolean pyScfExcitedStateBackendRequested(final ClassicalCalculationRequest request) {
    final List<
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                .ClassicalCalculationOption>
        options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationOption
          option = options.get(i);
      if (!option.name().equals("excited_state_backend")
          && !option.name().equals("spectroscopy_backend")) {
        continue;
      }
      return option.value().equalsIgnoreCase("pyscf");
    }
    return false;
  }

  private boolean nativeExcitedStateBackendRequested(final ClassicalCalculationRequest request) {
    final List<
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                .ClassicalCalculationOption>
        options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationOption
          option = options.get(i);
      if (!option.name().equals("excited_state_backend")
          && !option.name().equals("spectroscopy_backend")) {
        continue;
      }
      return option.value().equalsIgnoreCase("native")
          || option.value().equalsIgnoreCase("java_native");
    }
    return false;
  }

  private boolean pyScfMolecularPropertyBackendRequested(
      final ClassicalCalculationRequest request) {
    final List<
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                .ClassicalCalculationOption>
        options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationOption
          option = options.get(i);
      if (!option.name().equals("property_backend")
          && !option.name().equals("molecular_property_backend")
          && !option.name().equals("dipole_backend")
          && !option.name().equals("charge_backend")) {
        continue;
      }
      return option.value().equalsIgnoreCase("pyscf");
    }
    return false;
  }

  private boolean pyScfBenchmarkBackendRequested(
      final ClassicalCalculationRequest request, final ClassicalCalculationKind kind) {
    final List<
            ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
                .ClassicalCalculationOption>
        options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation
              .ClassicalCalculationOption
          option = options.get(i);
      if (kind == ClassicalCalculationKind.BASIS_SET_CONVERGENCE
          && (option.name().equals("basis_convergence_backend")
              || option.name().equals("benchmark_backend"))
          && option.value().equalsIgnoreCase("pyscf")) {
        return true;
      }
      if (kind == ClassicalCalculationKind.METHOD_BENCHMARK
          && (option.name().equals("method_benchmark_backend")
              || option.name().equals("benchmark_backend"))
          && option.value().equalsIgnoreCase("pyscf")) {
        return true;
      }
    }
    return false;
  }

  private static final class ReactionSideCoverage {

    private final boolean unsupported;
    private final boolean approximate;

    private ReactionSideCoverage(final boolean unsupported, final boolean approximate) {
      this.unsupported = unsupported;
      this.approximate = approximate;
    }

    private static ReactionSideCoverage exact() {
      return new ReactionSideCoverage(false, false);
    }

    private static ReactionSideCoverage approximate() {
      return new ReactionSideCoverage(false, true);
    }

    private static ReactionSideCoverage unsupported() {
      return new ReactionSideCoverage(true, false);
    }
  }
}
