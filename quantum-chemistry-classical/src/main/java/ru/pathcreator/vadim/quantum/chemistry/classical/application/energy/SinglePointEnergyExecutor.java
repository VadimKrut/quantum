/* * Copyright 2026 Vadim Aleksandrovich Zaletaev * * This Source Code Form is subject to the terms of the Mozilla Public * License, v. 2.0. If a copy of the MPL was not distributed with this * file, You can obtain one at https://mozilla.org/MPL/2.0/. * SPDX-License-Identifier: MPL-2.0 */ package ru.pathcreator.vadim.quantum.chemistry.classical.application.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ActiveSpaceRequestResolver;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ActiveSpaceSelector;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationMode;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPreflight;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ResolvedActiveSpaceRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.MolecularIntegralTensors;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.ClosedShellCoupledClusterDoublesSolver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.ClosedShellMp2Solver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.ConfigurationInteractionSinglesDoublesSolver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.DeterminantSpaceCoupledClusterSinglesDoublesSolver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.FullConfigurationInteractionSolver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.UnrestrictedMp2Solver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterDoublesOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterIterationRecord;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.ci.CisdResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mcscf.CasscfResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2.Mp2Result;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2.UnrestrictedMp2Result;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.DirectGaussianEriStatistics;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.UnrestrictedHartreeFockSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.validation.ScfResultValidator;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.orbital.OrbitalOptimizationRecord;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rohf.RohfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.validation.ScfValidationReport;

/**
 * * Выполняет Java-native single-point energy для поддержанных молекулярных методов без внешнего
 * backend.
 */
public final class SinglePointEnergyExecutor {
  private static final String FCI_MAX_DETERMINANTS_OPTION = "fci_max_determinants";
  private static final String FCI_ROOT_COUNT_OPTION = "fci_root_count";
  private static final String FCI_ALPHA_ELECTRONS_OPTION = "fci_alpha_electrons";
  private static final String FCI_BETA_ELECTRONS_OPTION = "fci_beta_electrons";
  private static final String CCD_MAX_ITERATIONS_OPTION = "ccd_max_iterations";
  private static final String CCD_ENERGY_TOLERANCE_OPTION = "ccd_energy_tolerance";
  private static final String CCD_DAMPING_OPTION = "ccd_damping";
  private static final String CCD_DENOMINATOR_SHIFT_OPTION = "ccd_denominator_shift";
  private static final String SINGLE_POINT_BACKEND_OPTION = "single_point_backend";
  private static final String SCF_INTEGRAL_MODE_OPTION = "scf_integral_mode";
  private static final String DIRECT_SCF_INTEGRAL_MODE = "direct";
  private static final String MAX_AUTOMATIC_DIRECT_SCF_ORBITALS_OPTION =
      "max_automatic_direct_scf_orbitals";
  private static final String MAX_UMP2_DENSE_SPIN_ORBITALS_OPTION = "max_ump2_dense_spin_orbitals";
  private static final int DEFAULT_MAX_AUTOMATIC_DIRECT_SCF_ORBITALS = 36;
  private static final int DEFAULT_MAX_UMP2_DENSE_SPIN_ORBITALS = 24;
  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final ClosedShellMp2Solver mp2Solver;
  private final UnrestrictedMp2Solver unrestrictedMp2Solver;
  private final ClosedShellCoupledClusterDoublesSolver coupledClusterDoublesSolver;
  private final DeterminantSpaceCoupledClusterSinglesDoublesSolver determinantSpaceCcsdSolver;
  private final ConfigurationInteractionSinglesDoublesSolver cisdSolver;
  private final FullConfigurationInteractionSolver fciSolver;
  private final CompleteActiveSpaceSelfConsistentFieldExecutor casscfExecutor;
  private final DensityFunctionalEnergyExecutor densityFunctionalEnergyExecutor;
  private final UnrestrictedHartreeFockSolver unrestrictedHartreeFockSolver;
  private final PyScfSinglePointEnergyExecutor pyScfSinglePointEnergyExecutor;
  private final ActiveSpaceRequestResolver activeSpaceResolver;
  private final SemiEmpiricalValenceEnergyModel semiEmpiricalEnergyModel;

  public SinglePointEnergyExecutor() {
    this(
        new ElectronicStructurePreparationPipeline(),
        new ClosedShellMp2Solver(),
        new UnrestrictedMp2Solver(),
        new ClosedShellCoupledClusterDoublesSolver(),
        new DeterminantSpaceCoupledClusterSinglesDoublesSolver(),
        new ConfigurationInteractionSinglesDoublesSolver(),
        new FullConfigurationInteractionSolver(),
        new CompleteActiveSpaceSelfConsistentFieldExecutor(),
        new DensityFunctionalEnergyExecutor(),
        new UnrestrictedHartreeFockSolver(),
        new PyScfSinglePointEnergyExecutor());
  }

  public SinglePointEnergyExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(
        preparationPipeline,
        new ClosedShellMp2Solver(),
        new UnrestrictedMp2Solver(),
        new ClosedShellCoupledClusterDoublesSolver(),
        new DeterminantSpaceCoupledClusterSinglesDoublesSolver(),
        new ConfigurationInteractionSinglesDoublesSolver(),
        new FullConfigurationInteractionSolver(),
        new CompleteActiveSpaceSelfConsistentFieldExecutor(),
        new DensityFunctionalEnergyExecutor(),
        new UnrestrictedHartreeFockSolver(),
        new PyScfSinglePointEnergyExecutor());
  }

  public SinglePointEnergyExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ClosedShellMp2Solver mp2Solver,
      final UnrestrictedMp2Solver unrestrictedMp2Solver,
      final ClosedShellCoupledClusterDoublesSolver coupledClusterDoublesSolver,
      final DeterminantSpaceCoupledClusterSinglesDoublesSolver determinantSpaceCcsdSolver,
      final ConfigurationInteractionSinglesDoublesSolver cisdSolver,
      final FullConfigurationInteractionSolver fciSolver,
      final CompleteActiveSpaceSelfConsistentFieldExecutor casscfExecutor,
      final DensityFunctionalEnergyExecutor densityFunctionalEnergyExecutor,
      final UnrestrictedHartreeFockSolver unrestrictedHartreeFockSolver,
      final PyScfSinglePointEnergyExecutor pyScfSinglePointEnergyExecutor) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException("Single-point preparation pipeline must not be null.");
    }
    if (mp2Solver == null) {
      throw new IllegalArgumentException("Single-point MP2 solver must not be null.");
    }
    if (unrestrictedMp2Solver == null) {
      throw new IllegalArgumentException("Single-point unrestricted MP2 solver must not be null.");
    }
    if (coupledClusterDoublesSolver == null) {
      throw new IllegalArgumentException(
          "Single-point coupled-cluster doubles solver must not be null.");
    }
    if (determinantSpaceCcsdSolver == null) {
      throw new IllegalArgumentException(
          "Single-point determinant-space CCSD solver must not be null.");
    }
    if (cisdSolver == null) {
      throw new IllegalArgumentException("Single-point CISD solver must not be null.");
    }
    if (fciSolver == null) {
      throw new IllegalArgumentException("Single-point FCI solver must not be null.");
    }
    if (casscfExecutor == null) {
      throw new IllegalArgumentException("Single-point CASSCF executor must not be null.");
    }
    if (densityFunctionalEnergyExecutor == null) {
      throw new IllegalArgumentException("Single-point DFT executor must not be null.");
    }
    if (unrestrictedHartreeFockSolver == null) {
      throw new IllegalArgumentException("Single-point UHF solver must not be null.");
    }
    if (pyScfSinglePointEnergyExecutor == null) {
      throw new IllegalArgumentException("Single-point PySCF executor must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.mp2Solver = mp2Solver;
    this.unrestrictedMp2Solver = unrestrictedMp2Solver;
    this.coupledClusterDoublesSolver = coupledClusterDoublesSolver;
    this.determinantSpaceCcsdSolver = determinantSpaceCcsdSolver;
    this.cisdSolver = cisdSolver;
    this.fciSolver = fciSolver;
    this.casscfExecutor = casscfExecutor;
    this.densityFunctionalEnergyExecutor = densityFunctionalEnergyExecutor;
    this.unrestrictedHartreeFockSolver = unrestrictedHartreeFockSolver;
    this.pyScfSinglePointEnergyExecutor = pyScfSinglePointEnergyExecutor;
    this.activeSpaceResolver =
        new ActiveSpaceRequestResolver(preparationPipeline, new ActiveSpaceSelector());
    this.semiEmpiricalEnergyModel = new SemiEmpiricalValenceEnergyModel();
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Single-point request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Single-point diagnostics target must not be null.");
    }
    if (SinglePointEnergyExecutor.pyScfBackend(request)) {
      return this.pyScfSinglePointEnergyExecutor.execute(request, diagnostics);
    }
    if (request.method().type() == ElectronicStructureMethodType.SEMI_EMPIRICAL) {
      return this.semiEmpiricalSection(request, diagnostics);
    }
    if (this.directRestrictedOpenShellHartreeFockAvailable(request)) {
      try {
        return SinglePointEnergyExecutor.rohfSection(
            this.preparationPipeline.prepareDirectRestrictedOpenShellHartreeFock(request));
      } catch (final IllegalArgumentException exception) {
        diagnostics.add(
            ClassicalDiagnostic.warning(
                SinglePointEnergyExecutor.exceptionCode(exception),
                ClassicalDiagnosticTarget.of(
                    "calculationKind", ClassicalCalculationKind.SINGLE_POINT_ENERGY.name()),
                exception.getMessage()));
        return null;
      }
    }
    if (this.directRestrictedHartreeFockAvailable(request)) {
      try {
        return SinglePointEnergyExecutor.rhfSection(
            this.preparationPipeline.prepareDirectRestrictedHartreeFock(request));
      } catch (final IllegalArgumentException exception) {
        diagnostics.add(
            ClassicalDiagnostic.warning(
                SinglePointEnergyExecutor.exceptionCode(exception),
                ClassicalDiagnosticTarget.of(
                    "calculationKind", ClassicalCalculationKind.SINGLE_POINT_ENERGY.name()),
                exception.getMessage()));
        return null;
      }
    }
    if (this.directUnrestrictedHartreeFockAvailable(request)) {
      try {
        return this.uhfSection(
            request, this.preparationPipeline.prepareDirectUnrestrictedHartreeFock(request));
      } catch (final IllegalArgumentException exception) {
        diagnostics.add(
            ClassicalDiagnostic.warning(
                SinglePointEnergyExecutor.exceptionCode(exception),
                ClassicalDiagnosticTarget.of(
                    "calculationKind", ClassicalCalculationKind.SINGLE_POINT_ENERGY.name()),
                exception.getMessage()));
        return null;
      }
    }
    if (this.openShellSmallSpaceExactCoupledClusterAvailable(request)) {
      try {
        return this.openShellSmallSpaceExactCoupledClusterSection(
            request, this.preparationPipeline.prepare(request));
      } catch (final IllegalArgumentException exception) {
        diagnostics.add(
            ClassicalDiagnostic.warning(
                SinglePointEnergyExecutor.exceptionCode(exception),
                ClassicalDiagnosticTarget.of(
                    "calculationKind", ClassicalCalculationKind.SINGLE_POINT_ENERGY.name()),
                exception.getMessage()));
        return null;
      }
    }
    if (this.openShellCoupledClusterUnsupported(request, diagnostics)) {
      return null;
    }
    if (SinglePointEnergyExecutor.quantumExecutionMethod(request.method().type())) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().name().value()),
              "Single-point energy does not execute quantum algorithms directly. Request "
                  + ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION.name()
                  + " to generate Quantum IR for VQE/QPE, then execute it through a quantum backend."));
      return null;
    }
    final ClassicalCalculationRequest effectiveRequest =
        this.activeSpaceEffectiveRequest(request, diagnostics);
    if (effectiveRequest == null) {
      return null;
    }
    if (!this.supportsCommonRequest(
        effectiveRequest, ClassicalCalculationKind.SINGLE_POINT_ENERGY, diagnostics)) {
      return null;
    }
    try {
      final ElectronicStructurePreparationResult preparationResult =
          this.preparationPipeline.prepare(effectiveRequest);
      if (effectiveRequest.method().type() == ElectronicStructureMethodType.HARTREE_FOCK) {
        if (preparationResult.hasRohfReference()) {
          return SinglePointEnergyExecutor.rohfSection(preparationResult);
        }
        if (this.useUnrestrictedReference(effectiveRequest)) {
          return this.uhfSection(effectiveRequest, preparationResult);
        }
        return SinglePointEnergyExecutor.rhfSection(preparationResult);
      }
      if (effectiveRequest.method().type()
          == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
        return this.densityFunctionalEnergyExecutor.execute(
            effectiveRequest, preparationResult, diagnostics);
      }
      if (this.mp2Method(effectiveRequest)) {
        if (this.unrestrictedMp2DenseKernelUnsupported(
            effectiveRequest, preparationResult, diagnostics)) {
          return null;
        }
        return this.mp2Section(effectiveRequest, preparationResult);
      }
      if (this.coupledClusterDoublesMethod(effectiveRequest)) {
        return this.coupledClusterDoublesSection(effectiveRequest, preparationResult);
      }
      if (this.coupledClusterSinglesDoublesMethod(effectiveRequest)) {
        return this.coupledClusterSinglesDoublesSection(effectiveRequest, preparationResult);
      }
      if (this.cisdMethod(effectiveRequest)) {
        return this.cisdSection(effectiveRequest, preparationResult);
      }
      if (this.casciMethod(effectiveRequest)) {
        return this.casciSection(effectiveRequest, preparationResult, diagnostics);
      }
      if (this.casscfMethod(effectiveRequest)) {
        return this.casscfSection(effectiveRequest, preparationResult, diagnostics);
      }
      if (effectiveRequest.method().type()
          == ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION) {
        return this.fciSection(effectiveRequest, preparationResult);
      }
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().name().value()),
              "Java-native single-point executor supports RHF, UHF, Kohn-Sham DFT, closed-shell"
                  + " MP2, unrestricted MP2, CCD, CCSD, CISD, CASCI, CASSCF and active-space FCI."));
      return null;
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              SinglePointEnergyExecutor.exceptionCode(exception),
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.SINGLE_POINT_ENERGY.name()),
              exception.getMessage()));
      return null;
    }
  }

  public ClassicalResultSection executeOrbitalOptimization(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Orbital optimization request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException(
          "Orbital optimization diagnostics target must not be null.");
    }
    if (!this.casscfMethod(request)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name()),
              "Java-native orbital optimization currently requires CASSCF/MCSCF method."));
      return null;
    }
    final ResolvedActiveSpaceRequest resolvedActiveSpace;
    try {
      resolvedActiveSpace =
          this.activeSpaceResolver.resolve(
              request, diagnostics, ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name());
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name()),
              exception.getMessage()));
      return null;
    }
    final ClassicalCalculationRequest resolvedRequest = resolvedActiveSpace.request();
    if (!this.supportsCommonRequest(
        resolvedRequest, ClassicalCalculationKind.ORBITAL_OPTIMIZATION, diagnostics)) {
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
      return SinglePointEnergyExecutor.orbitalOptimizationSection(
          resolvedRequest, preparationResult, result);
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              SinglePointEnergyExecutor.exceptionCode(exception),
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.ORBITAL_OPTIMIZATION.name()),
              exception.getMessage()));
      return null;
    }
  }

  private boolean supportsCommonRequest(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind diagnosticKind,
      final List<ClassicalDiagnostic> diagnostics) {
    final String unsupportedReason =
        request.method().type() == ElectronicStructureMethodType.HARTREE_FOCK
                || request.method().type()
                    == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
                || this.mp2Method(request)
            ? this.preparationPipeline.unsupportedReason(request)
            : this.preparationPipeline.restrictedClosedShellUnsupportedReason(request);
    if (unsupportedReason == null) {
      return true;
    }
    if (this.preparationPipeline.canPrepareDirectActiveSpaceHamiltonian(request)) {
      return true;
    }
    if (this.preparationPipeline.canPrepareDirectHartreeFock(request)) {
      return true;
    }
    final ClassicalDiagnosticCode code =
        unsupportedReason.contains("basis") || unsupportedReason.contains("Basis")
            ? ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED
            : ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED;
    diagnostics.add(
        ClassicalDiagnostic.warning(
            code,
            ClassicalDiagnosticTarget.of("calculationKind", diagnosticKind.name()),
            unsupportedReason));
    return false;
  }

  private ClassicalCalculationRequest activeSpaceEffectiveRequest(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (!SinglePointEnergyExecutor.requiresActiveSpaceResolution(request)) {
      return request;
    }
    try {
      return this.activeSpaceResolver
          .resolve(request, diagnostics, ClassicalCalculationKind.SINGLE_POINT_ENERGY.name())
          .request();
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().name().value()),
              exception.getMessage()));
      return null;
    }
  }

  private static boolean requiresActiveSpaceResolution(final ClassicalCalculationRequest request) {
    if (request.hasActiveSpace()) {
      return false;
    }
    final ElectronicStructureMethodType type = request.method().type();
    return type == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD
        || type == ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION;
  }

  private boolean openShellCoupledClusterUnsupported(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request.method().type() != ElectronicStructureMethodType.COUPLED_CLUSTER) {
      return false;
    }
    if (!(request.subject() instanceof Molecule)) {
      return false;
    }
    final Molecule molecule = (Molecule) request.subject();
    if (molecule.electronicConfiguration().closedShell()) {
      return false;
    }
    if (this.openShellSmallSpaceExactCoupledClusterAvailable(request)) {
      return false;
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
            ClassicalDiagnosticTarget.of("method", request.method().name().value()),
            "Java-native coupled-cluster covers closed-shell CCD/CCSD and open-shell one-/two-electron CCSD through exact determinant-space equivalence; larger open-shell CCSD requires a separate UHF/spin-orbital solver."));
    return true;
  }

  private boolean openShellSmallSpaceExactCoupledClusterAvailable(
      final ClassicalCalculationRequest request) {
    if (request == null || !this.coupledClusterSinglesDoublesMethod(request)) {
      return false;
    }
    if (!(request.subject() instanceof Molecule)) {
      return false;
    }
    final Molecule molecule = (Molecule) request.subject();
    if (molecule.electronicConfiguration().closedShell()) {
      return false;
    }
    final int electronCount =
        request.hasActiveSpace()
            ? request.activeSpace().electronCount()
            : molecule.electronicConfiguration().electronCount();
    return electronCount > 0 && electronCount <= 2;
  }

  private boolean unrestrictedMp2DenseKernelUnsupported(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult,
      final List<ClassicalDiagnostic> diagnostics) {
    if (!preparationResult.hasUhfReference()) {
      return false;
    }
    final int orbitalCount = preparationResult.basisOrbitalCount();
    final int maxOrbitals =
        SinglePointEnergyExecutor.integerOption(
            request, MAX_UMP2_DENSE_SPIN_ORBITALS_OPTION, DEFAULT_MAX_UMP2_DENSE_SPIN_ORBITALS);
    if (orbitalCount <= maxOrbitals) {
      return false;
    }
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.MOLECULE_TOO_LARGE,
            ClassicalDiagnosticTarget.of("method", request.method().name().value()),
            "UMP2 dense spin-block kernel requires three O(n^4) transformed ERI tensors for "
                + orbitalCount
                + " orbitals; configured max_ump2_dense_spin_orbitals is "
                + maxOrbitals
                + ". Use an active-space calculation or a direct/sparse post-HF kernel."));
    return true;
  }

  private boolean directRestrictedHartreeFockAvailable(final ClassicalCalculationRequest request) {
    if (request == null || request.method().type() != ElectronicStructureMethodType.HARTREE_FOCK) {
      return false;
    }
    if (this.useUnrestrictedReference(request)) {
      return false;
    }
    if (request.method().spinTreatment()
        == ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL) {
      return false;
    }
    if (SinglePointEnergyExecutor.directScfRequested(request)) {
      return this.preparationPipeline.oneElectronUnsupportedReason(request) == null;
    }
    return this.automaticDirectScfAllowed(request)
        && this.preparationPipeline.unsupportedReason(request) != null
        && this.preparationPipeline.oneElectronUnsupportedReason(request) == null;
  }

  private boolean directUnrestrictedHartreeFockAvailable(
      final ClassicalCalculationRequest request) {
    if (request == null || request.method().type() != ElectronicStructureMethodType.HARTREE_FOCK) {
      return false;
    }
    if (!this.useUnrestrictedReference(request)) {
      return false;
    }
    if (SinglePointEnergyExecutor.directScfRequested(request)) {
      return this.preparationPipeline.oneElectronUnsupportedReason(request) == null;
    }
    return this.automaticDirectScfAllowed(request)
        && this.preparationPipeline.unsupportedReason(request) != null
        && this.preparationPipeline.oneElectronUnsupportedReason(request) == null;
  }

  private boolean directRestrictedOpenShellHartreeFockAvailable(
      final ClassicalCalculationRequest request) {
    if (request == null || request.method().type() != ElectronicStructureMethodType.HARTREE_FOCK) {
      return false;
    }
    if (request.method().spinTreatment()
        != ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL) {
      return false;
    }
    if (SinglePointEnergyExecutor.directScfRequested(request)) {
      return this.preparationPipeline.oneElectronUnsupportedReason(request) == null;
    }
    return this.automaticDirectScfAllowed(request)
        && this.preparationPipeline.unsupportedReason(request) != null
        && this.preparationPipeline.oneElectronUnsupportedReason(request) == null;
  }

  private static boolean quantumExecutionMethod(final ElectronicStructureMethodType type) {
    return type == ElectronicStructureMethodType.QUANTUM_VARIATIONAL
        || type == ElectronicStructureMethodType.QUANTUM_PHASE_ESTIMATION;
  }

  private ClassicalResultSection semiEmpiricalSection(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (!(request.subject() instanceof Molecule)) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.SINGLE_POINT_ENERGY.name()),
              "Semiempirical single-point energy requires a molecule subject."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    final SemiEmpiricalValenceEnergyResult result =
        this.semiEmpiricalEnergyModel.evaluate(molecule);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "native_valence_graph_semiempirical",
        "fast_organic_valence_energy_ranking",
        "Approximate valence graph model; use ab initio HF/DFT/post-HF for final single-point energies.");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(
        ClassicalResultValue.text("implemented_kernel", "native_valence_graph_semiempirical"));
    values.add(ClassicalResultValue.text("basis_set", "not_required"));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(true)));
    values.add(ClassicalResultValue.scalar("atom_count", result.atomCount(), "count"));
    values.add(ClassicalResultValue.scalar("bond_count", result.bondCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("aromatic_bond_count", result.aromaticBondCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "electronic_energy", result.electronicEnergyHartree(), "hartree"));
    values.add(ClassicalResultValue.scalar("bond_energy", result.bondEnergyHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "conjugation_energy", result.conjugationEnergyHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("strain_energy", result.strainEnergyHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("charge_penalty", result.chargePenaltyHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("radical_penalty", result.radicalPenaltyHartree(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("torsion_penalty", result.torsionPenaltyHartree(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergyHartree(), "hartree"));
    values.add(
        ClassicalResultValue.text(
            "scope",
            "Java-native approximate valence model for fast organic ranking; use ab initio/DFT for final energies."));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY,
        "Semiempirical valence single-point energy",
        values);
  }

  private boolean automaticDirectScfAllowed(final ClassicalCalculationRequest request) {
    final ElectronicStructurePreparationPreflight preflight =
        this.preparationPipeline.preflight(request);
    if (!preflight.hasOneElectronPreflight()) {
      return false;
    }
    final int maxOrbitals =
        SinglePointEnergyExecutor.integerOption(
            request,
            MAX_AUTOMATIC_DIRECT_SCF_ORBITALS_OPTION,
            DEFAULT_MAX_AUTOMATIC_DIRECT_SCF_ORBITALS);
    return preflight.oneElectronPreflight().orbitalCount() <= maxOrbitals;
  }

  private static boolean directScfRequested(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(SCF_INTEGRAL_MODE_OPTION)) {
        return option.value().equalsIgnoreCase(DIRECT_SCF_INTEGRAL_MODE);
      }
    }
    return false;
  }

  private boolean mp2Method(final ClassicalCalculationRequest request) {
    if (request.method().type() != ElectronicStructureMethodType.PERTURBATION_THEORY) {
      return false;
    }
    final String methodName = request.method().name().value().toLowerCase(Locale.ROOT);
    return methodName.equals("mp2")
        || methodName.contains("moller")
        || methodName.contains("moeller")
        || methodName.contains("second order");
  }

  private boolean coupledClusterDoublesMethod(final ClassicalCalculationRequest request) {
    if (request.method().type() != ElectronicStructureMethodType.COUPLED_CLUSTER) {
      return false;
    }
    final String methodName = request.method().name().value().toLowerCase(Locale.ROOT);
    return methodName.equals("ccd")
        || methodName.contains("coupled cluster doubles")
        || methodName.contains("coupled-cluster doubles");
  }

  private boolean coupledClusterSinglesDoublesMethod(final ClassicalCalculationRequest request) {
    if (request.method().type() != ElectronicStructureMethodType.COUPLED_CLUSTER) {
      return false;
    }
    final String methodName = request.method().name().value().toLowerCase(Locale.ROOT);
    return methodName.equals("ccsd")
        || methodName.contains("coupled cluster singles doubles")
        || methodName.contains("coupled-cluster singles doubles")
        || methodName.contains("coupled cluster singles and doubles")
        || methodName.contains("coupled-cluster singles and doubles");
  }

  private boolean cisdMethod(final ClassicalCalculationRequest request) {
    if (request.method().type() != ElectronicStructureMethodType.CONFIGURATION_INTERACTION) {
      return false;
    }
    final String methodName = request.method().name().value().toLowerCase(Locale.ROOT);
    return methodName.equals("cisd")
        || methodName.contains("configuration interaction singles doubles")
        || methodName.contains("configuration interaction singles and doubles")
        || methodName.contains("configuration-interaction singles-doubles")
        || methodName.contains("configuration-interaction singles and doubles");
  }

  private boolean casciMethod(final ClassicalCalculationRequest request) {
    if (request.method().type()
        != ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return false;
    }
    final String methodName = request.method().name().value().toLowerCase(Locale.ROOT);
    return methodName.equals("casci")
        || methodName.contains("complete active space configuration interaction");
  }

  private boolean casscfMethod(final ClassicalCalculationRequest request) {
    if (request.method().type()
        != ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD) {
      return false;
    }
    final String methodName = request.method().name().value().toLowerCase(Locale.ROOT);
    return methodName.equals("casscf")
        || methodName.equals("mcscf")
        || methodName.contains("complete active space")
        || methodName.contains("multi configurational")
        || methodName.contains("multi-configurational");
  }

  private boolean useUnrestrictedReference(final ClassicalCalculationRequest request) {
    if (request.method().spinTreatment() == ElectronicStructureSpinTreatment.UNRESTRICTED) {
      return true;
    }
    if (request.method().spinTreatment()
        == ElectronicStructureSpinTreatment.RESTRICTED_OPEN_SHELL) {
      return false;
    }
    if (!(request.subject() instanceof Molecule)) {
      return false;
    }
    final Molecule molecule = (Molecule) request.subject();
    return molecule.electronicConfiguration().openShell();
  }

  private ClassicalResultSection uhfSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    final Molecule molecule = (Molecule) request.subject();
    final UhfResult result =
        preparationResult.hasUhfReference()
            ? preparationResult.uhfResult()
            : this.unrestrictedHartreeFockSolver.solve(
                preparationResult.atomicOrbitalTensors(),
                preparationResult.electronCount(),
                molecule.electronicConfiguration().spinMultiplicity().value());
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values, "java_native", "unrestricted_hartree_fock", "ab_initio_scf_single_point_energy");
    values.add(ClassicalResultValue.text("method", "UHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(ClassicalResultValue.text("preparation_mode", preparationResult.mode().name()));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("iterations", result.iterations(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "orbital_count", preparationResult.basisOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("alpha_electron_count", result.alphaElectronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("beta_electron_count", result.betaElectronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("spin_squared", result.spinSquared(), "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "spin_contamination", result.spinContamination(), "dimensionless"));
    SinglePointEnergyExecutor.addDirectEriStatistics(values, preparationResult);
    values.add(
        ClassicalResultValue.of(
            "alpha_orbital_energies",
            ClassicalResultValueKind.VECTOR,
            "hartree",
            SinglePointEnergyExecutor.doubleArrayValues(result.alphaOrbitalEnergiesCopy())));
    values.add(
        ClassicalResultValue.of(
            "beta_orbital_energies",
            ClassicalResultValueKind.VECTOR,
            "hartree",
            SinglePointEnergyExecutor.doubleArrayValues(result.betaOrbitalEnergiesCopy())));
    SinglePointEnergyExecutor.addScfValidationValues(
        values,
        new ScfResultValidator()
            .validateUnrestricted(
                result,
                SinglePointEnergyExecutor.overlapCopy(preparationResult),
                preparationResult.basisOrbitalCount()));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "UHF single-point energy", values);
  }

  private static ClassicalResultSection rohfSection(
      final ElectronicStructurePreparationResult preparationResult) {
    final RohfResult result = preparationResult.rohfResult();
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "restricted_open_shell_hartree_fock",
        "ab_initio_scf_single_point_energy");
    values.add(ClassicalResultValue.text("method", "ROHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(ClassicalResultValue.text("preparation_mode", preparationResult.mode().name()));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("iterations", result.iterations(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "orbital_count", preparationResult.basisOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("alpha_electron_count", result.alphaElectronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("beta_electron_count", result.betaElectronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "closed_shell_pair_count", result.closedShellElectronPairCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "open_shell_electron_count", result.openShellElectronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("spin_squared", result.spinSquared(), "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "spin_contamination", result.spinContamination(), "dimensionless"));
    SinglePointEnergyExecutor.addDirectEriStatistics(values, preparationResult);
    values.add(
        ClassicalResultValue.of(
            "orbital_energies",
            ClassicalResultValueKind.VECTOR,
            "hartree",
            SinglePointEnergyExecutor.doubleArrayValues(result.orbitalEnergiesCopy())));
    SinglePointEnergyExecutor.addScfValidationValues(
        values,
        new ScfResultValidator()
            .validateRestrictedOpenShell(
                result,
                SinglePointEnergyExecutor.overlapCopy(preparationResult),
                preparationResult.basisOrbitalCount()));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "ROHF single-point energy", values);
  }

  private ClassicalResultSection mp2Section(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasUhfReference()) {
      return this.unrestrictedMp2Section(request, preparationResult);
    }
    final Mp2Result mp2Result =
        this.mp2Solver.solve(
            preparationResult.molecularOrbitalTensors(),
            preparationResult.rhfResult(),
            preparationResult.electronCount());
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "closed_shell_mp2",
        "post_hartree_fock_perturbative_single_point_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("implemented_kernel", "closed_shell_mp2"));
    SinglePointEnergyExecutor.addPostHfContract(values, "MP2", "PERTURBATIVE", "RHF", false, false);
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(true)));
    values.add(
        ClassicalResultValue.scalar("electron_count", preparationResult.electronCount(), "count"));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(
        ClassicalResultValue.scalar(
            "reference_total_energy", preparationResult.rhfResult().totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "correlation_energy", mp2Result.correlationEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", mp2Result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "occupied_orbital_count", mp2Result.occupiedOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "virtual_orbital_count", mp2Result.virtualOrbitalCount(), "count"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "MP2 single-point energy", values);
  }

  private ClassicalResultSection unrestrictedMp2Section(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    final UnrestrictedMp2Result mp2Result =
        this.unrestrictedMp2Solver.solve(
            preparationResult.atomicOrbitalTensors(), preparationResult.uhfResult());
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "unrestricted_spin_orbital_mp2",
        "post_hartree_fock_perturbative_single_point_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("implemented_kernel", "unrestricted_spin_orbital_mp2"));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "UMP2", "PERTURBATIVE", "UHF", false, false);
    values.add(ClassicalResultValue.text("reference_method", "UHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.uhfResult().converged())));
    values.add(
        ClassicalResultValue.scalar(
            "reference_total_energy", preparationResult.uhfResult().totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "reference_spin_squared",
            preparationResult.uhfResult().spinSquared(),
            "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "reference_spin_contamination",
            preparationResult.uhfResult().spinContamination(),
            "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "correlation_energy", mp2Result.correlationEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "same_spin_correlation_energy", mp2Result.sameSpinCorrelationEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "opposite_spin_correlation_energy",
            mp2Result.oppositeSpinCorrelationEnergy(),
            "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", mp2Result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "alpha_occupied_orbital_count", mp2Result.alphaOccupiedOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "beta_occupied_orbital_count", mp2Result.betaOccupiedOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "alpha_virtual_orbital_count", mp2Result.alphaVirtualOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "beta_virtual_orbital_count", mp2Result.betaVirtualOrbitalCount(), "count"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "UMP2 single-point energy", values);
  }

  private ClassicalResultSection coupledClusterDoublesSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hamiltonian().electronCount() == 2) {
      return this.twoElectronExactCoupledClusterSection(
          request,
          preparationResult,
          "closed_shell_ccd_two_electron_fci_equivalent",
          "CCD two-electron exact single-point energy");
    }
    final CoupledClusterDoublesResult result =
        this.coupledClusterDoublesSolver.solve(
            preparationResult.molecularOrbitalTensors(),
            preparationResult.rhfResult(),
            preparationResult.electronCount(),
            SinglePointEnergyExecutor.ccdOptions(request));
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "closed_shell_ccd",
        "post_hartree_fock_projective_coupled_cluster_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("implemented_kernel", "closed_shell_ccd"));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "CCD", "PROJECTIVE_COUPLED_CLUSTER", "RHF", false, false);
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("iterations", result.iterations(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "iteration_history_count", result.iterationHistory().size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "last_energy_delta",
            SinglePointEnergyExecutor.lastEnergyDelta(result.iterationHistory()),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "last_amplitude_delta_norm",
            SinglePointEnergyExecutor.lastAmplitudeDeltaNorm(result.iterationHistory()),
            "norm"));
    values.add(
        ClassicalResultValue.scalar(
            "max_amplitude_delta_norm",
            SinglePointEnergyExecutor.maxAmplitudeDeltaNorm(result.iterationHistory()),
            "norm"));
    values.add(
        ClassicalResultValue.text(
            "correlation_energy_monotonic_nonincreasing",
            Boolean.toString(
                SinglePointEnergyExecutor.monotonicNonIncreasingCorrelationEnergy(
                    result.iterationHistory()))));
    values.add(
        ClassicalResultValue.scalar(
            "reference_total_energy", preparationResult.rhfResult().totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("correlation_energy", result.correlationEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "occupied_orbital_count", result.occupiedOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "virtual_orbital_count", result.virtualOrbitalCount(), "count"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "CCD single-point energy", values);
  }

  private ClassicalResultSection coupledClusterSinglesDoublesSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hamiltonian().electronCount() == 2) {
      return this.twoElectronExactCoupledClusterSection(
          request,
          preparationResult,
          "closed_shell_ccsd_two_electron_fci_equivalent",
          "CCSD two-electron exact single-point energy");
    }
    final CoupledClusterSinglesDoublesResult result =
        this.determinantSpaceCcsdSolver.solve(
            preparationResult.hamiltonian(),
            preparationResult.rhfResult(),
            SinglePointEnergyExecutor.ccsdOptions(request));
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "determinant_space_projective_ccsd",
        "post_hartree_fock_projective_coupled_cluster_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(
        ClassicalResultValue.text("implemented_kernel", "determinant_space_projective_ccsd"));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "CCSD", "PROJECTIVE_COUPLED_CLUSTER", "RHF", false, false);
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("iterations", result.iterations(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "reference_total_energy", preparationResult.rhfResult().totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("correlation_energy", result.correlationEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "occupied_orbital_count", result.occupiedOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "virtual_orbital_count", result.virtualOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "singles_amplitude_count", result.singlesAmplitudesCopy().length, "count"));
    values.add(
        ClassicalResultValue.text(
            "singles_amplitude_policy", "determinant_space_projective_solution"));
    values.add(
        ClassicalResultValue.scalar(
            "doubles_amplitude_count", result.doublesAmplitudesCopy().length, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "iteration_history_count", result.iterationHistory().size(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "last_energy_delta",
            SinglePointEnergyExecutor.lastEnergyDelta(result.iterationHistory()),
            "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "last_amplitude_delta_norm",
            SinglePointEnergyExecutor.lastAmplitudeDeltaNorm(result.iterationHistory()),
            "norm"));
    values.add(
        ClassicalResultValue.scalar(
            "max_amplitude_delta_norm",
            SinglePointEnergyExecutor.maxAmplitudeDeltaNorm(result.iterationHistory()),
            "norm"));
    values.add(
        ClassicalResultValue.text(
            "correlation_energy_monotonic_nonincreasing",
            Boolean.toString(
                SinglePointEnergyExecutor.monotonicNonIncreasingCorrelationEnergy(
                    result.iterationHistory()))));
    values.add(
        ClassicalResultValue.of(
            "ccsd_iteration_history",
            ClassicalResultValueKind.TABLE,
            "iteration|correlation_energy|energy_delta|amplitude_delta_norm",
            SinglePointEnergyExecutor.coupledClusterHistoryRows(result.iterationHistory())));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "CCSD single-point energy", values);
  }

  private ClassicalResultSection twoElectronExactCoupledClusterSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult,
      final String kernelName,
      final String sectionName) {
    final ElectronicHamiltonian hamiltonian = preparationResult.hamiltonian();
    final FciResult result =
        this.fciSolver.solve(
            hamiltonian,
            SinglePointEnergyExecutor.fciOptions(request, hamiltonian, preparationResult));
    final double correlationEnergy =
        result.totalEnergy() - preparationResult.rhfResult().totalEnergy();
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        kernelName,
        "two_electron_exact_diagonalization_single_point_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("implemented_kernel", kernelName));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "FCI_EQUIVALENT_CC", "EXACT_DIAGONALIZATION", "RHF", true, true);
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(true)));
    values.add(
        ClassicalResultValue.scalar(
            "reference_total_energy", preparationResult.rhfResult().totalEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("correlation_energy", correlationEnergy, "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("spin_orbital_count", hamiltonian.spinOrbitalCount(), "count"));
    values.add(ClassicalResultValue.scalar("electron_count", hamiltonian.electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("determinant_count", result.determinantCount(), "count"));
    values.add(ClassicalResultValue.scalar("root_count", result.rootCount(), "count"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, sectionName, values);
  }

  private ClassicalResultSection openShellSmallSpaceExactCoupledClusterSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    final ElectronicHamiltonian hamiltonian = preparationResult.hamiltonian();
    final FciResult result =
        this.fciSolver.solve(
            hamiltonian, SinglePointEnergyExecutor.fciOptions(request, hamiltonian));
    final double referenceTotalEnergy =
        SinglePointEnergyExecutor.referenceTotalEnergy(preparationResult);
    final double correlationEnergy = result.totalEnergy() - referenceTotalEnergy;
    final String referenceMethod = SinglePointEnergyExecutor.referenceMethod(preparationResult);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "open_shell_small_space_ccsd_fci_equivalent",
        "small_active_space_exact_diagonalization_single_point_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(
        ClassicalResultValue.text(
            "implemented_kernel", "open_shell_small_space_ccsd_fci_equivalent"));
    SinglePointEnergyExecutor.addPostHfContract(
        values,
        "FCI_EQUIVALENT_OPEN_SHELL_CCSD",
        "EXACT_DIAGONALIZATION",
        referenceMethod,
        true,
        true);
    values.add(ClassicalResultValue.text("reference_method", referenceMethod));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged",
            Boolean.toString(SinglePointEnergyExecutor.referenceConverged(preparationResult))));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(true)));
    values.add(
        ClassicalResultValue.text(
            "ccsd_equivalence_reason",
            "One-/two-electron open-shell active space is exactly represented by singles/doubles determinant diagonalization."));
    values.add(
        ClassicalResultValue.scalar("reference_total_energy", referenceTotalEnergy, "hartree"));
    values.add(ClassicalResultValue.scalar("correlation_energy", correlationEnergy, "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("spin_orbital_count", hamiltonian.spinOrbitalCount(), "count"));
    values.add(ClassicalResultValue.scalar("electron_count", hamiltonian.electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("determinant_count", result.determinantCount(), "count"));
    values.add(ClassicalResultValue.scalar("root_count", result.rootCount(), "count"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY,
        "Open-shell small-space CCSD exact single-point energy",
        values);
  }

  private ClassicalResultSection cisdSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    final CisdResult result = this.cisdSolver.solve(preparationResult.hamiltonian());
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "determinant_space_cisd",
        "truncated_configuration_interaction_single_point_energy",
        "CISD is a truncated CI model and is not size-extensive; compare against FCI/CC where feasible.");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("implemented_kernel", "determinant_space_cisd"));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "CISD", "TRUNCATED_CONFIGURATION_INTERACTION", "RHF", true, false);
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(
        ClassicalResultValue.scalar(
            "spin_orbital_count", preparationResult.hamiltonian().spinOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "electron_count", preparationResult.hamiltonian().electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("determinant_count", result.determinantCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "CISD single-point energy", values);
  }

  private ClassicalResultSection casciSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult,
      final List<ClassicalDiagnostic> diagnostics) {
    if (!request.hasActiveSpace()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().name().value()),
              "Java-native active-space multiconfigurational calculation requires active space."));
      return null;
    }
    final FciResult result =
        this.fciSolver.solve(
            preparationResult.hamiltonian(),
            SinglePointEnergyExecutor.fciOptions(request, preparationResult.hamiltonian()));
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "active_space_casci",
        "active_space_exact_diagonalization_single_point_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("implemented_kernel", "active_space_casci"));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "CASCI", "ACTIVE_SPACE_EXACT_DIAGONALIZATION", "RHF", true, true);
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(
        ClassicalResultValue.scalar(
            "active_space_electrons", preparationResult.hamiltonian().electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "active_space_orbitals",
            preparationResult.hamiltonian().spatialOrbitalCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar("determinant_count", result.determinantCount(), "count"));
    values.add(ClassicalResultValue.scalar("root_count", result.rootCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY,
        "CASCI active-space single-point energy",
        values);
  }

  private ClassicalResultSection casscfSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult,
      final List<ClassicalDiagnostic> diagnostics) {
    if (!request.hasActiveSpace()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.ACTIVE_SPACE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().name().value()),
              "Java-native CASSCF requires active space."));
      return null;
    }
    final CasscfResult result = this.casscfExecutor.solve(request, preparationResult);
    if (!result.converged()) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_PARTIAL,
              ClassicalDiagnosticTarget.of("method", request.method().name().value()),
              "Java-native CASSCF orbital optimization stopped before energy convergence."));
    }
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "active_space_casscf_orbital_optimization",
        "orbital_optimized_active_space_fci_single_point_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(
        ClassicalResultValue.text(
            "implemented_kernel", "active_space_casscf_orbital_optimization"));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "CASSCF", "ORBITAL_OPTIMIZED_ACTIVE_SPACE_FCI", "RHF", true, true);
    values.add(ClassicalResultValue.text("orbital_rotation_space", result.orbitalRotationSpace()));
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("iterations", result.iterations(), "count"));
    values.add(
        ClassicalResultValue.scalar("first_active_orbital", result.firstActiveOrbital(), "index"));
    values.add(
        ClassicalResultValue.scalar(
            "active_space_electrons", result.activeElectronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("active_space_orbitals", result.activeOrbitalCount(), "count"));
    final int availableRotationPairCount =
        SinglePointEnergyExecutor.addOrbitalAudit(values, preparationResult, result);
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
    SinglePointEnergyExecutor.addOrbitalAuditStatus(values, result, availableRotationPairCount);
    values.add(
        ClassicalResultValue.scalar(
            "determinant_count", result.fciResult().determinantCount(), "count"));
    values.add(ClassicalResultValue.scalar("root_count", result.fciResult().rootCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.of(
            "orbital_optimization_history",
            ClassicalResultValueKind.TABLE,
            "",
            SinglePointEnergyExecutor.orbitalOptimizationHistoryRows(
                result.orbitalOptimizationHistory())));
    values.add(
        ClassicalResultValue.of(
            "orbital_rotation_coverage",
            ClassicalResultValueKind.TABLE,
            "",
            SinglePointEnergyExecutor.orbitalRotationCoverageRows(
                preparationResult, result, availableRotationPairCount)));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY,
        "CASSCF active-space single-point energy",
        values);
  }

  private static ClassicalResultSection orbitalOptimizationSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult,
      final CasscfResult result) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values, "java_native", "active_space_casscf_orbital_optimization", "orbital_optimization");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(
        ClassicalResultValue.text(
            "implemented_kernel", "active_space_casscf_orbital_optimization"));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "CASSCF_ORBITAL_OPTIMIZATION", "ORBITAL_OPTIMIZATION", "RHF", true, true);
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
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
        SinglePointEnergyExecutor.addOrbitalAudit(values, preparationResult, result);
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
    SinglePointEnergyExecutor.addOrbitalAuditStatus(values, result, availableRotationPairCount);
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
            SinglePointEnergyExecutor.orbitalOptimizationHistoryRows(
                result.orbitalOptimizationHistory())));
    values.add(
        ClassicalResultValue.of(
            "orbital_rotation_coverage",
            ClassicalResultValueKind.TABLE,
            "",
            SinglePointEnergyExecutor.orbitalRotationCoverageRows(
                preparationResult, result, availableRotationPairCount)));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.ORBITAL_OPTIMIZATION, "CASSCF orbital optimization", values);
  }

  private static int addOrbitalAudit(
      final ArrayList<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult preparationResult,
      final CasscfResult result) {
    final int fullOrbitalCount = preparationResult.basisOrbitalCount();
    final int inactiveOrbitalCount = result.firstActiveOrbital();
    final int externalOrbitalCount =
        Math.max(0, fullOrbitalCount - result.firstActiveOrbital() - result.activeOrbitalCount());
    final int availableRotationPairCount =
        SinglePointEnergyExecutor.orbitalRotationPairCount(
            result.orbitalRotationSpace(),
            inactiveOrbitalCount,
            result.activeOrbitalCount(),
            externalOrbitalCount);
    values.add(ClassicalResultValue.scalar("full_orbital_count", fullOrbitalCount, "count"));
    values.add(
        ClassicalResultValue.scalar("inactive_orbital_count", inactiveOrbitalCount, "count"));
    values.add(
        ClassicalResultValue.scalar("external_orbital_count", externalOrbitalCount, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "available_orbital_rotation_pair_count", availableRotationPairCount, "count"));
    values.add(
        ClassicalResultValue.text(
            "orbital_partition",
            inactiveOrbitalCount
                + " inactive / "
                + result.activeOrbitalCount()
                + " active / "
                + externalOrbitalCount
                + " external"));
    return availableRotationPairCount;
  }

  private static void addOrbitalAuditStatus(
      final ArrayList<ClassicalResultValue> values,
      final CasscfResult result,
      final int availableRotationPairCount) {
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
  }

  private static List<String> orbitalOptimizationHistoryRows(
      final List<OrbitalOptimizationRecord> history) {
    if (history.isEmpty()) {
      return List.of(
          "iteration|evaluated_rotations|accepted_rotations|step_rad|energy_hartree|delta_hartree|converged");
    }
    final ArrayList<String> rows = new ArrayList<String>(history.size() + 1);
    rows.add(
        "iteration|evaluated_rotations|accepted_rotations|step_rad|energy_hartree|delta_hartree|converged");
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

  private static List<String> orbitalRotationCoverageRows(
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

  private static int orbitalRotationPairCount(
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

  private static double lastEnergyDelta(final List<CoupledClusterIterationRecord> history) {
    if (history.isEmpty()) {
      return 0.0;
    }
    return history.get(history.size() - 1).energyDelta();
  }

  private static double lastAmplitudeDeltaNorm(final List<CoupledClusterIterationRecord> history) {
    if (history.isEmpty()) {
      return 0.0;
    }
    return history.get(history.size() - 1).amplitudeDeltaNorm();
  }

  private static double maxAmplitudeDeltaNorm(final List<CoupledClusterIterationRecord> history) {
    double maxNorm = 0.0;
    for (int i = 0; i < history.size(); ++i) {
      maxNorm = Math.max(maxNorm, history.get(i).amplitudeDeltaNorm());
    }
    return maxNorm;
  }

  private static boolean monotonicNonIncreasingCorrelationEnergy(
      final List<CoupledClusterIterationRecord> history) {
    for (int i = 1; i < history.size(); ++i) {
      if (history.get(i).correlationEnergy() > history.get(i - 1).correlationEnergy()) {
        return false;
      }
    }
    return true;
  }

  private static List<String> coupledClusterHistoryRows(
      final List<CoupledClusterIterationRecord> history) {
    final ArrayList<String> rows = new ArrayList<String>(history.size() + 1);
    rows.add("iteration|correlation_energy|energy_delta|amplitude_delta_norm");
    for (int i = 0; i < history.size(); ++i) {
      final CoupledClusterIterationRecord record = history.get(i);
      rows.add(
          record.iteration()
              + "|"
              + record.correlationEnergy()
              + "|"
              + record.energyDelta()
              + "|"
              + record.amplitudeDeltaNorm());
    }
    return List.copyOf(rows);
  }

  private static void addPostHfContract(
      final ArrayList<ClassicalResultValue> values,
      final String solverFamily,
      final String correlationLevel,
      final String referenceFamily,
      final boolean variationalBound,
      final boolean exactDiagonalization) {
    values.add(ClassicalResultValue.text("posthf_solver_family", solverFamily));
    values.add(ClassicalResultValue.text("posthf_correlation_level", correlationLevel));
    values.add(ClassicalResultValue.text("posthf_reference_family", referenceFamily));
    values.add(
        ClassicalResultValue.text("posthf_variational_bound", Boolean.toString(variationalBound)));
    values.add(
        ClassicalResultValue.text(
            "posthf_exact_diagonalization", Boolean.toString(exactDiagonalization)));
    values.add(ClassicalResultValue.text("java_native_posthf", Boolean.toString(true)));
    values.add(
        ClassicalResultValue.text(
            "posthf_solver_contract", "java_native_" + solverFamily.toLowerCase(Locale.ROOT)));
    values.add(
        ClassicalResultValue.text("external_package_parity_claimed", Boolean.toString(false)));
    values.add(ClassicalResultValue.text("production_parity", Boolean.toString(false)));
  }

  private ClassicalResultSection fciSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparationResult) {
    final ElectronicHamiltonian hamiltonian = preparationResult.hamiltonian();
    final FciResult result =
        this.fciSolver.solve(
            hamiltonian, SinglePointEnergyExecutor.fciOptions(request, hamiltonian));
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "full_configuration_interaction",
        "full_active_hamiltonian_exact_diagonalization_single_point_energy");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("implemented_kernel", "full_configuration_interaction"));
    SinglePointEnergyExecutor.addPostHfContract(
        values, "FCI", "EXACT_DIAGONALIZATION", "RHF", true, true);
    values.add(ClassicalResultValue.text("reference_method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference_converged", Boolean.toString(preparationResult.rhfResult().converged())));
    values.add(
        ClassicalResultValue.scalar("spin_orbital_count", hamiltonian.spinOrbitalCount(), "count"));
    values.add(ClassicalResultValue.scalar("electron_count", hamiltonian.electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("determinant_count", result.determinantCount(), "count"));
    values.add(ClassicalResultValue.scalar("root_count", result.rootCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.of(
            "electronic_energies",
            ClassicalResultValueKind.VECTOR,
            "hartree",
            SinglePointEnergyExecutor.doubleArrayValues(result.electronicEnergiesCopy())));
    values.add(
        ClassicalResultValue.of(
            "total_energies",
            ClassicalResultValueKind.VECTOR,
            "hartree",
            SinglePointEnergyExecutor.doubleArrayValues(result.totalEnergiesCopy())));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.SINGLE_POINT_ENERGY, "FCI single-point energy", values);
  }

  private static ClassicalResultSection rhfSection(
      final ElectronicStructurePreparationResult preparationResult) {
    final RhfResult result = preparationResult.rhfResult();
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    if (preparationResult.mode() == ElectronicStructurePreparationMode.ONE_ELECTRON_FALLBACK) {
      ClassicalResultQualityValues.addFallback(
          values,
          "java_native",
          "restricted_hartree_fock_one_electron_fallback",
          "reduced_ab_initio_scf_single_point_energy",
          "Two-electron tensor route was unavailable and an explicit one-electron fallback was used.");
    } else {
      ClassicalResultQualityValues.addExact(
          values, "java_native", "restricted_hartree_fock", "ab_initio_scf_single_point_energy");
    }
    values.add(ClassicalResultValue.text("method", "RHF"));
    values.add(ClassicalResultValue.text("basis_set", preparationResult.basisSetName()));
    values.add(ClassicalResultValue.text("preparation_mode", preparationResult.mode().name()));
    if (preparationResult.hasLimitationReason()) {
      values.add(
          ClassicalResultValue.text(
              "preparation_limitation", preparationResult.limitationReason()));
    }
    values.add(ClassicalResultValue.text("converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("iterations", result.iterations(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "orbital_count", preparationResult.basisOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("electronic_energy", result.electronicEnergy(), "hartree"));
    values.add(ClassicalResultValue.scalar("total_energy", result.totalEnergy(), "hartree"));
    SinglePointEnergyExecutor.addDirectEriStatistics(values, preparationResult);
    values.add(
        ClassicalResultValue.of(
            "orbital_energies",
            ClassicalResultValueKind.VECTOR,
            "hartree",
            SinglePointEnergyExecutor.doubleArrayValues(result.orbitalEnergiesCopy())));
    SinglePointEnergyExecutor.addScfValidationValues(
        values,
        new ScfResultValidator()
            .validateRestricted(
                result,
                SinglePointEnergyExecutor.overlapCopy(preparationResult),
                preparationResult.basisOrbitalCount(),
                preparationResult.electronCount()));
    final String title =
        preparationResult.mode() == ElectronicStructurePreparationMode.DIRECT_HARTREE_FOCK
            ? "direct_gaussian_restricted_hartree_fock"
            : "RHF single-point energy";
    return ClassicalResultSection.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY, title, values);
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
            SinglePointEnergyExecutor.scfDiagnosticCodes(report.diagnostics())));
  }

  private static void addDirectEriStatistics(
      final List<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult preparationResult) {
    if (!preparationResult.hasDirectEriStatistics()) {
      return;
    }
    final DirectGaussianEriStatistics statistics = preparationResult.directEriStatistics();
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

  private static double[] overlapCopy(
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasAtomicOrbitalTensors()) {
      return preparationResult.atomicOrbitalTensors().overlapCopy();
    }
    if (preparationResult.hasOneElectronMatrices()) {
      return preparationResult.oneElectronMatrices().overlapCopy();
    }
    throw new IllegalStateException("SCF validation requires AO overlap matrix.");
  }

  private static List<String> doubleArrayValues(final double[] values) {
    final ArrayList<String> result = new ArrayList<String>(values.length);
    for (int i = 0; i < values.length; ++i) {
      result.add(Double.toString(values[i]));
    }
    return result;
  }

  private static double referenceTotalEnergy(
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasRhfReference()) {
      return preparationResult.rhfResult().totalEnergy();
    }
    if (preparationResult.hasRohfReference()) {
      return preparationResult.rohfResult().totalEnergy();
    }
    if (preparationResult.hasUhfReference()) {
      return preparationResult.uhfResult().totalEnergy();
    }
    if (preparationResult.hasKohnShamDftReference()) {
      return preparationResult.kohnShamDftResult().totalEnergy();
    }
    throw new IllegalStateException("Electronic preparation does not contain a reference energy.");
  }

  private static String referenceMethod(
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasRhfReference()) {
      return "RHF";
    }
    if (preparationResult.hasRohfReference()) {
      return "ROHF";
    }
    if (preparationResult.hasUhfReference()) {
      return "UHF";
    }
    if (preparationResult.hasKohnShamDftReference()) {
      return preparationResult.kohnShamDftResult().unrestricted()
          ? "UNRESTRICTED_KOHN_SHAM_DFT"
          : "KOHN_SHAM_DFT";
    }
    throw new IllegalStateException("Electronic preparation does not contain a reference method.");
  }

  private static boolean referenceConverged(
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasRhfReference()) {
      return preparationResult.rhfResult().converged();
    }
    if (preparationResult.hasRohfReference()) {
      return preparationResult.rohfResult().converged();
    }
    if (preparationResult.hasUhfReference()) {
      return preparationResult.uhfResult().converged();
    }
    if (preparationResult.hasKohnShamDftReference()) {
      return preparationResult.kohnShamDftResult().converged();
    }
    throw new IllegalStateException(
        "Electronic preparation does not contain reference convergence.");
  }

  private static FciOptions fciOptions(
      final ClassicalCalculationRequest request, final ElectronicHamiltonian hamiltonian) {
    final int maxDeterminants =
        SinglePointEnergyExecutor.integerOption(request, FCI_MAX_DETERMINANTS_OPTION, 20_000);
    final int rootCount =
        SinglePointEnergyExecutor.integerOption(request, FCI_ROOT_COUNT_OPTION, 1);
    final int alphaElectronCount =
        SinglePointEnergyExecutor.integerOption(request, FCI_ALPHA_ELECTRONS_OPTION, -1);
    final int betaElectronCount =
        SinglePointEnergyExecutor.integerOption(request, FCI_BETA_ELECTRONS_OPTION, -1);
    if (alphaElectronCount >= 0 || betaElectronCount >= 0) {
      if (alphaElectronCount < 0 || betaElectronCount < 0) {
        throw new IllegalArgumentException(
            "FCI spin-sector options require both alpha and beta electron counts.");
      }
      return FciOptions.spinSector(
          maxDeterminants, rootCount, alphaElectronCount, betaElectronCount);
    }
    return FciOptions.of(maxDeterminants, Math.min(rootCount, hamiltonian.spinOrbitalCount()));
  }

  private static FciOptions fciOptions(
      final ClassicalCalculationRequest request,
      final ElectronicHamiltonian hamiltonian,
      final ElectronicStructurePreparationResult preparationResult) {
    if (SinglePointEnergyExecutor.explicitFciSpinSectorRequested(request)) {
      return SinglePointEnergyExecutor.fciOptions(request, hamiltonian);
    }
    if (!SinglePointEnergyExecutor.openShellReference(preparationResult)) {
      return SinglePointEnergyExecutor.fciOptions(request, hamiltonian);
    }
    final int maxDeterminants =
        SinglePointEnergyExecutor.integerOption(request, FCI_MAX_DETERMINANTS_OPTION, 20_000);
    final int rootCount =
        SinglePointEnergyExecutor.integerOption(request, FCI_ROOT_COUNT_OPTION, 1);
    return FciOptions.spinSector(
        maxDeterminants,
        rootCount,
        SinglePointEnergyExecutor.referenceAlphaElectronCount(preparationResult),
        SinglePointEnergyExecutor.referenceBetaElectronCount(preparationResult));
  }

  private static boolean explicitFciSpinSectorRequested(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(FCI_ALPHA_ELECTRONS_OPTION)
          || option.name().equals(FCI_BETA_ELECTRONS_OPTION)) {
        return true;
      }
    }
    return false;
  }

  private static boolean openShellReference(
      final ElectronicStructurePreparationResult preparationResult) {
    return preparationResult.hasUhfReference()
        || preparationResult.hasRohfReference()
        || (preparationResult.hasKohnShamDftReference()
            && preparationResult.kohnShamDftResult().unrestricted());
  }

  private static int referenceAlphaElectronCount(
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasUhfReference()) {
      return preparationResult.uhfResult().alphaElectronCount();
    }
    if (preparationResult.hasRohfReference()) {
      return preparationResult.rohfResult().alphaElectronCount();
    }
    if (preparationResult.hasKohnShamDftReference()) {
      return preparationResult.kohnShamDftResult().alphaElectronCount();
    }
    return preparationResult.hamiltonian().electronCount() / 2;
  }

  private static int referenceBetaElectronCount(
      final ElectronicStructurePreparationResult preparationResult) {
    if (preparationResult.hasUhfReference()) {
      return preparationResult.uhfResult().betaElectronCount();
    }
    if (preparationResult.hasRohfReference()) {
      return preparationResult.rohfResult().betaElectronCount();
    }
    if (preparationResult.hasKohnShamDftReference()) {
      return preparationResult.kohnShamDftResult().betaElectronCount();
    }
    return preparationResult.hamiltonian().electronCount() / 2;
  }

  private static CoupledClusterDoublesOptions ccdOptions(
      final ClassicalCalculationRequest request) {
    return CoupledClusterDoublesOptions.of(
        SinglePointEnergyExecutor.integerOption(request, CCD_MAX_ITERATIONS_OPTION, 96),
        SinglePointEnergyExecutor.doubleOption(request, CCD_ENERGY_TOLERANCE_OPTION, 1.0E-9),
        SinglePointEnergyExecutor.doubleOption(request, CCD_DAMPING_OPTION, 0.35),
        SinglePointEnergyExecutor.doubleOption(request, CCD_DENOMINATOR_SHIFT_OPTION, 1.0E-12),
        true);
  }

  private static CoupledClusterSinglesDoublesOptions ccsdOptions(
      final ClassicalCalculationRequest request) {
    return CoupledClusterSinglesDoublesOptions.of(
        SinglePointEnergyExecutor.integerOption(request, CCD_MAX_ITERATIONS_OPTION, 96),
        SinglePointEnergyExecutor.doubleOption(request, CCD_ENERGY_TOLERANCE_OPTION, 1.0E-9),
        SinglePointEnergyExecutor.doubleOption(request, CCD_DAMPING_OPTION, 0.35),
        SinglePointEnergyExecutor.doubleOption(request, CCD_DENOMINATOR_SHIFT_OPTION, 1.0E-12),
        true);
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

  private static double doubleOption(
      final ClassicalCalculationRequest request, final String name, final double defaultValue) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(name)) {
        continue;
      }
      try {
        return Double.parseDouble(option.value());
      } catch (final NumberFormatException exception) {
        throw new IllegalArgumentException(
            "Classical calculation option " + name + " must be numeric.");
      }
    }
    return defaultValue;
  }

  private static boolean pyScfBackend(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!option.name().equals(SINGLE_POINT_BACKEND_OPTION)) {
        continue;
      }
      return option.value().equalsIgnoreCase("pyscf");
    }
    return false;
  }

  private static ClassicalDiagnosticCode exceptionCode(final IllegalArgumentException exception) {
    final String message = exception.getMessage().toLowerCase(Locale.ROOT);
    if (message.contains("coordinate") || message.contains("geometry")) {
      return ClassicalDiagnosticCode.GEOMETRY_REQUIRED;
    }
    if (message.contains("basis") || message.contains("orbital") || message.contains("gaussian")) {
      return ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED;
    }
    return ClassicalDiagnosticCode.RESULT_FAILED;
  }
}
