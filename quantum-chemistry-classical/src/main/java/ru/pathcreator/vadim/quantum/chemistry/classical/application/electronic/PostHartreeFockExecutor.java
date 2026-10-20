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
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.ClosedShellCoupledClusterSinglesDoublesSolver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.ClosedShellMp2Solver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.ConfigurationInteractionSinglesDoublesSolver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.application.FullConfigurationInteractionSolver;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.cc.CoupledClusterSinglesDoublesResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.ci.CisdResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciOptions;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.fci.FciResult;
import ru.pathcreator.vadim.quantum.chemistry.posthf.domain.mp2.Mp2Result;

/**
 * Выполняет Java-native post-Hartree-Fock расчеты поверх подготовленного electronic Hamiltonian.
 */
public final class PostHartreeFockExecutor {

  private static final int DEFAULT_MAX_FCI_DETERMINANTS = 20_000;
  private static final int DEFAULT_FCI_ROOT_COUNT = 1;

  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final ClosedShellMp2Solver mp2Solver;
  private final ConfigurationInteractionSinglesDoublesSolver cisdSolver;
  private final ClosedShellCoupledClusterSinglesDoublesSolver ccsdSolver;
  private final FullConfigurationInteractionSolver fciSolver;

  public PostHartreeFockExecutor() {
    this(new ElectronicStructurePreparationPipeline());
  }

  public PostHartreeFockExecutor(final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(
        preparationPipeline,
        new ClosedShellMp2Solver(),
        new ConfigurationInteractionSinglesDoublesSolver(),
        new ClosedShellCoupledClusterSinglesDoublesSolver(),
        new FullConfigurationInteractionSolver());
  }

  public PostHartreeFockExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ClosedShellMp2Solver mp2Solver,
      final ConfigurationInteractionSinglesDoublesSolver cisdSolver,
      final ClosedShellCoupledClusterSinglesDoublesSolver ccsdSolver,
      final FullConfigurationInteractionSolver fciSolver) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException("Post-HF preparation pipeline must not be null.");
    }
    if (mp2Solver == null) {
      throw new IllegalArgumentException("Post-HF MP2 solver must not be null.");
    }
    if (cisdSolver == null) {
      throw new IllegalArgumentException("Post-HF CISD solver must not be null.");
    }
    if (ccsdSolver == null) {
      throw new IllegalArgumentException("Post-HF CCSD solver must not be null.");
    }
    if (fciSolver == null) {
      throw new IllegalArgumentException("Post-HF FCI solver must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.mp2Solver = mp2Solver;
    this.cisdSolver = cisdSolver;
    this.ccsdSolver = ccsdSolver;
    this.fciSolver = fciSolver;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Post-HF request must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Post-HF calculation kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Post-HF diagnostics target must not be null.");
    }
    if (!PostHartreeFockExecutor.supportsKind(kind)) {
      throw new IllegalArgumentException("Post-HF calculation kind is unsupported.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
              "Post-HF correlation requires a molecule subject."));
      return null;
    }
    final ElectronicStructurePreparationResult preparation;
    try {
      preparation = this.preparationPipeline.prepare(request);
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_FAILED,
              ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
              exception.getMessage()));
      return null;
    }
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("basis_set", preparation.basisSetName()));
    values.add(ClassicalResultValue.text("preparation_mode", preparation.mode().name()));
    values.add(
        ClassicalResultValue.text(
            "reference_method", PostHartreeFockExecutor.referenceMethod(preparation)));
    values.add(
        ClassicalResultValue.scalar(
            "electron_count", preparation.hamiltonian().electronCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "spatial_orbital_count", preparation.hamiltonian().spatialOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "spin_orbital_count", preparation.hamiltonian().spinOrbitalCount(), "count"));
    values.add(ClassicalResultValue.text("native_java_posthf", "true"));
    if (kind == ClassicalCalculationKind.MP2_CORRELATION
        || kind == ClassicalCalculationKind.POST_HF_CORRELATION) {
      this.addMp2Values(preparation, values, diagnostics, kind);
    }
    if (kind == ClassicalCalculationKind.CCSD_CORRELATION
        || kind == ClassicalCalculationKind.POST_HF_CORRELATION) {
      this.addCcsdValues(preparation, values, diagnostics, kind);
    }
    if (kind == ClassicalCalculationKind.CISD_CORRELATION
        || kind == ClassicalCalculationKind.POST_HF_CORRELATION) {
      this.addCisdValues(preparation, values, diagnostics, kind);
    }
    if (kind == ClassicalCalculationKind.FCI_DIAGONALIZATION
        || kind == ClassicalCalculationKind.POST_HF_CORRELATION) {
      this.addFciValues(request, preparation, values, diagnostics, kind);
    }
    PostHartreeFockExecutor.addQualityValues(kind, values);
    return ClassicalResultSection.of(kind, PostHartreeFockExecutor.title(kind), values);
  }

  public static boolean supportsKind(final ClassicalCalculationKind kind) {
    return kind == ClassicalCalculationKind.MP2_CORRELATION
        || kind == ClassicalCalculationKind.CISD_CORRELATION
        || kind == ClassicalCalculationKind.CCSD_CORRELATION
        || kind == ClassicalCalculationKind.FCI_DIAGONALIZATION
        || kind == ClassicalCalculationKind.POST_HF_CORRELATION;
  }

  private void addMp2Values(
      final ElectronicStructurePreparationResult preparation,
      final List<ClassicalResultValue> values,
      final List<ClassicalDiagnostic> diagnostics,
      final ClassicalCalculationKind kind) {
    if (!PostHartreeFockExecutor.closedShellReferenceAvailable(preparation)) {
      PostHartreeFockExecutor.addSkippedDiagnostic(
          diagnostics, kind, "MP2 requires RHF reference and MO tensors.");
      values.add(ClassicalResultValue.text("mp2_available", "false"));
      return;
    }
    try {
      final Mp2Result result =
          this.mp2Solver.solve(
              preparation.molecularOrbitalTensors(),
              preparation.rhfResult(),
              preparation.electronCount());
      values.add(ClassicalResultValue.text("mp2_available", "true"));
      values.add(
          ClassicalResultValue.scalar(
              "mp2_correlation_energy", result.correlationEnergy(), "hartree"));
      values.add(ClassicalResultValue.scalar("mp2_total_energy", result.totalEnergy(), "hartree"));
      values.add(
          ClassicalResultValue.scalar(
              "mp2_occupied_orbitals", result.occupiedOrbitalCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "mp2_virtual_orbitals", result.virtualOrbitalCount(), "count"));
    } catch (final IllegalArgumentException exception) {
      PostHartreeFockExecutor.addFailedDiagnostic(
          diagnostics, kind, "MP2 failed: " + exception.getMessage());
      values.add(ClassicalResultValue.text("mp2_available", "false"));
    }
  }

  private void addCcsdValues(
      final ElectronicStructurePreparationResult preparation,
      final List<ClassicalResultValue> values,
      final List<ClassicalDiagnostic> diagnostics,
      final ClassicalCalculationKind kind) {
    if (!PostHartreeFockExecutor.closedShellReferenceAvailable(preparation)) {
      PostHartreeFockExecutor.addSkippedDiagnostic(
          diagnostics, kind, "CCSD requires RHF reference and MO tensors.");
      values.add(ClassicalResultValue.text("ccsd_available", "false"));
      return;
    }
    try {
      final CoupledClusterSinglesDoublesResult result =
          this.ccsdSolver.solve(
              preparation.molecularOrbitalTensors(),
              preparation.rhfResult(),
              preparation.electronCount());
      values.add(ClassicalResultValue.text("ccsd_available", "true"));
      values.add(ClassicalResultValue.text("ccsd_converged", Boolean.toString(result.converged())));
      values.add(ClassicalResultValue.scalar("ccsd_iterations", result.iterations(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "ccsd_correlation_energy", result.correlationEnergy(), "hartree"));
      values.add(ClassicalResultValue.scalar("ccsd_total_energy", result.totalEnergy(), "hartree"));
      values.add(
          ClassicalResultValue.scalar(
              "ccsd_occupied_orbitals", result.occupiedOrbitalCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "ccsd_virtual_orbitals", result.virtualOrbitalCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "ccsd_singles_amplitude_count", result.singlesAmplitudesCopy().length, "count"));
      values.add(
          ClassicalResultValue.scalar(
              "ccsd_doubles_amplitude_count", result.doublesAmplitudesCopy().length, "count"));
      if (!result.converged()) {
        PostHartreeFockExecutor.addSkippedDiagnostic(
            diagnostics, kind, "CCSD stopped before convergence.");
      }
    } catch (final IllegalArgumentException exception) {
      PostHartreeFockExecutor.addFailedDiagnostic(
          diagnostics, kind, "CCSD failed: " + exception.getMessage());
      values.add(ClassicalResultValue.text("ccsd_available", "false"));
    }
  }

  private void addCisdValues(
      final ElectronicStructurePreparationResult preparation,
      final List<ClassicalResultValue> values,
      final List<ClassicalDiagnostic> diagnostics,
      final ClassicalCalculationKind kind) {
    if (!preparation.twoElectronIntegralsAvailable()) {
      PostHartreeFockExecutor.addSkippedDiagnostic(
          diagnostics, kind, "CISD requires a two-electron Hamiltonian.");
      values.add(ClassicalResultValue.text("cisd_available", "false"));
      return;
    }
    try {
      final CisdResult result = this.cisdSolver.solve(preparation.hamiltonian());
      values.add(ClassicalResultValue.text("cisd_available", "true"));
      values.add(
          ClassicalResultValue.scalar(
              "cisd_determinant_count", result.determinantCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "cisd_electronic_energy", result.electronicEnergy(), "hartree"));
      values.add(ClassicalResultValue.scalar("cisd_total_energy", result.totalEnergy(), "hartree"));
    } catch (final IllegalArgumentException exception) {
      PostHartreeFockExecutor.addFailedDiagnostic(
          diagnostics, kind, "CISD failed: " + exception.getMessage());
      values.add(ClassicalResultValue.text("cisd_available", "false"));
    }
  }

  private void addFciValues(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparation,
      final List<ClassicalResultValue> values,
      final List<ClassicalDiagnostic> diagnostics,
      final ClassicalCalculationKind kind) {
    if (!preparation.twoElectronIntegralsAvailable()) {
      PostHartreeFockExecutor.addSkippedDiagnostic(
          diagnostics, kind, "FCI requires a two-electron Hamiltonian.");
      values.add(ClassicalResultValue.text("fci_available", "false"));
      return;
    }
    try {
      final FciResult result =
          this.fciSolver.solve(
              preparation.hamiltonian(),
              FciOptions.of(
                  PostHartreeFockExecutor.intOption(
                      request, "fci_max_determinants", DEFAULT_MAX_FCI_DETERMINANTS),
                  PostHartreeFockExecutor.intOption(
                      request, "fci_root_count", DEFAULT_FCI_ROOT_COUNT)));
      values.add(ClassicalResultValue.text("fci_available", "true"));
      values.add(
          ClassicalResultValue.scalar("fci_determinant_count", result.determinantCount(), "count"));
      values.add(ClassicalResultValue.scalar("fci_root_count", result.rootCount(), "count"));
      values.add(
          ClassicalResultValue.scalar(
              "fci_ground_electronic_energy", result.electronicEnergy(), "hartree"));
      values.add(
          ClassicalResultValue.scalar("fci_ground_total_energy", result.totalEnergy(), "hartree"));
    } catch (final IllegalArgumentException exception) {
      PostHartreeFockExecutor.addFailedDiagnostic(
          diagnostics, kind, "FCI failed: " + exception.getMessage());
      values.add(ClassicalResultValue.text("fci_available", "false"));
    }
  }

  private static boolean closedShellReferenceAvailable(
      final ElectronicStructurePreparationResult preparation) {
    return preparation.hasRhfReference() && preparation.hasMolecularOrbitalTensors();
  }

  private static String referenceMethod(final ElectronicStructurePreparationResult preparation) {
    if (preparation.hasCasscfResult()) {
      return "CASSCF";
    }
    if (preparation.hasRhfReference()) {
      return "RHF";
    }
    if (preparation.hasRohfReference()) {
      return "ROHF";
    }
    if (preparation.hasUhfReference()) {
      return "UHF";
    }
    if (preparation.hasKohnShamDftReference()) {
      return "KOHN_SHAM_DFT";
    }
    return "HAMILTONIAN_ONLY";
  }

  private static String title(final ClassicalCalculationKind kind) {
    if (kind == ClassicalCalculationKind.MP2_CORRELATION) {
      return "Java-native MP2 correlation";
    }
    if (kind == ClassicalCalculationKind.CISD_CORRELATION) {
      return "Java-native CISD correlation";
    }
    if (kind == ClassicalCalculationKind.CCSD_CORRELATION) {
      return "Java-native CCSD correlation";
    }
    if (kind == ClassicalCalculationKind.FCI_DIAGONALIZATION) {
      return "Java-native FCI diagonalization";
    }
    return "Java-native post-Hartree-Fock correlation suite";
  }

  private static void addQualityValues(
      final ClassicalCalculationKind kind, final List<ClassicalResultValue> values) {
    final boolean exact;
    if (kind == ClassicalCalculationKind.MP2_CORRELATION) {
      exact = PostHartreeFockExecutor.textValue(values, "mp2_available", "false").equals("true");
    } else if (kind == ClassicalCalculationKind.CCSD_CORRELATION) {
      exact =
          PostHartreeFockExecutor.textValue(values, "ccsd_available", "false").equals("true")
              && PostHartreeFockExecutor.textValue(values, "ccsd_converged", "true").equals("true");
    } else if (kind == ClassicalCalculationKind.CISD_CORRELATION) {
      exact = PostHartreeFockExecutor.textValue(values, "cisd_available", "false").equals("true");
    } else if (kind == ClassicalCalculationKind.FCI_DIAGONALIZATION) {
      exact = PostHartreeFockExecutor.textValue(values, "fci_available", "false").equals("true");
    } else {
      exact =
          PostHartreeFockExecutor.textValue(values, "mp2_available", "false").equals("true")
              && PostHartreeFockExecutor.textValue(values, "ccsd_available", "false").equals("true")
              && PostHartreeFockExecutor.textValue(values, "ccsd_converged", "true").equals("true")
              && PostHartreeFockExecutor.textValue(values, "cisd_available", "false").equals("true")
              && PostHartreeFockExecutor.textValue(values, "fci_available", "false").equals("true");
    }
    if (exact) {
      ClassicalResultQualityValues.addExact(
          values,
          "java_native",
          PostHartreeFockExecutor.qualityModel(kind),
          "dense_molecular_orbital_posthf");
      return;
    }
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        PostHartreeFockExecutor.qualityModel(kind),
        "partial_posthf_suite",
        "At least one requested post-HF component was unavailable, unconverged, or skipped for the prepared reference.");
  }

  private static String qualityModel(final ClassicalCalculationKind kind) {
    if (kind == ClassicalCalculationKind.MP2_CORRELATION) {
      return "closed_shell_mp2";
    }
    if (kind == ClassicalCalculationKind.CCSD_CORRELATION) {
      return "closed_shell_ccsd";
    }
    if (kind == ClassicalCalculationKind.CISD_CORRELATION) {
      return "determinant_space_cisd";
    }
    if (kind == ClassicalCalculationKind.FCI_DIAGONALIZATION) {
      return "determinant_space_fci";
    }
    return "post_hartree_fock_suite";
  }

  private static String textValue(
      final List<ClassicalResultValue> values, final String name, final String fallback) {
    for (int i = 0; i < values.size(); ++i) {
      final ClassicalResultValue value = values.get(i);
      if (value.name().equals(name) && value.valueCount() > 0) {
        return value.firstValue();
      }
    }
    return fallback;
  }

  private static int intOption(
      final ClassicalCalculationRequest request, final String optionName, final int defaultValue) {
    for (final ClassicalCalculationOption option : request.options()) {
      if (option.name().equals(optionName)) {
        final long value;
        try {
          value = Long.parseLong(option.value());
        } catch (final NumberFormatException exception) {
          throw new IllegalArgumentException(optionName + " must be a positive int value.");
        }
        if (value <= 0L || value > Integer.MAX_VALUE) {
          throw new IllegalArgumentException(optionName + " must be a positive int value.");
        }
        return (int) value;
      }
    }
    return defaultValue;
  }

  private static void addSkippedDiagnostic(
      final List<ClassicalDiagnostic> diagnostics,
      final ClassicalCalculationKind kind,
      final String message) {
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_PARTIAL,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            message));
  }

  private static void addFailedDiagnostic(
      final List<ClassicalDiagnostic> diagnostics,
      final ClassicalCalculationKind kind,
      final String message) {
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.RESULT_FAILED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            message));
  }
}
