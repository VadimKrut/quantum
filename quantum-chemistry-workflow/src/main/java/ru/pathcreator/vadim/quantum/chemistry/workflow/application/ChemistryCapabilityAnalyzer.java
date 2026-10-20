/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.coverage.ClassicalCalculationCoverageAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPreflight;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageItem;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCalculationCoverageReport;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.coverage.ClassicalCoverageStatus;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin.BuiltInClassicalBackendProfiles;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.DensityFunctionalFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureSpinTreatment;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityItem;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityReport;
import ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability.ChemistryCapabilityStatus;

/**
 * Анализирует, какие части PySCF/OpenFermion-подобного пути закрываются Java-native стеком.
 */
public final class ChemistryCapabilityAnalyzer {

  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final ClassicalCalculationCoverageAnalyzer coverageAnalyzer;
  private static final String GEOMETRY_ENERGY_MODEL_OPTION = "geometry_energy_model";

  public ChemistryCapabilityAnalyzer() {
    this(new ElectronicStructurePreparationPipeline());
  }

  public ChemistryCapabilityAnalyzer(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Chemistry capability preparation pipeline must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.coverageAnalyzer = new ClassicalCalculationCoverageAnalyzer(preparationPipeline);
  }

  public ChemistryCapabilityReport analyze(final ClassicalCalculationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Chemistry capability request must not be null.");
    }
    final ArrayList<ChemistryCapabilityItem> items = new ArrayList<ChemistryCapabilityItem>(16);
    this.addSubjectSupport(request, items);
    this.addCalculationCoverageSupport(request, items);
    this.addIntegralSupport(request, items);
    this.addScfDftSupport(request, items);
    this.addPostHfSupport(request, items);
    this.addMolecularWorkflowSupport(request, items);
    this.addQuantumWorkflowSupport(request, items);
    return ChemistryCapabilityReport.of(request.id().value(), items);
  }

  private void addSubjectSupport(
      final ClassicalCalculationRequest request, final ArrayList<ChemistryCapabilityItem> items) {
    if (request.subject().subjectKind() == ChemistrySubjectKind.MOLECULE) {
      items.add(
          ChemistryCapabilityItem.of(
              "subject.molecule",
              ChemistryCapabilityStatus.SUPPORTED,
              "Molecule subject can be validated, converted to integral geometry and used for electronic workflows."));
      return;
    }
    if (request.subject().subjectKind() == ChemistrySubjectKind.REACTION) {
      items.add(
          ChemistryCapabilityItem.of(
              "subject.reaction",
              ChemistryCapabilityStatus.PARTIAL,
              "Reaction subject is supported for prediction and planning; ab initio electronic preparation runs per molecule."));
      return;
    }
    items.add(
        ChemistryCapabilityItem.of(
            "subject.unknown",
            ChemistryCapabilityStatus.UNSUPPORTED,
            "Only molecule and reaction subjects are supported by the chemistry workflow."));
  }

  private void addCalculationCoverageSupport(
      final ClassicalCalculationRequest request, final ArrayList<ChemistryCapabilityItem> items) {
    final ClassicalCalculationCoverageReport report =
        this.coverageAnalyzer.analyze(
            request, BuiltInClassicalBackendProfiles.smallMoleculeAbInitio());
    final List<ClassicalCalculationCoverageItem> coverageItems = report.items();
    for (int i = 0; i < coverageItems.size(); ++i) {
      final ClassicalCalculationCoverageItem item = coverageItems.get(i);
      final ChemistryCapabilityStatus status =
          ChemistryCapabilityAnalyzer.adjustedCoverageStatus(request, item);
      items.add(
          ChemistryCapabilityItem.of(
              "coverage." + item.kind().name().toLowerCase(Locale.ROOT),
              status,
              ChemistryCapabilityAnalyzer.coverageMessage(item)));
    }
  }

  private static ChemistryCapabilityStatus adjustedCoverageStatus(
      final ClassicalCalculationRequest request, final ClassicalCalculationCoverageItem item) {
    final ChemistryCapabilityStatus status =
        ChemistryCapabilityAnalyzer.coverageStatus(item.status());
    if (item.kind() == ClassicalCalculationKind.SINGLE_POINT_ENERGY
        && request.method().type() == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY
        && request.method().hasDensityFunctionalDefinition()
        && request.method().densityFunctionalDefinition().family()
            != DensityFunctionalFamily.LOCAL_DENSITY
        && status == ChemistryCapabilityStatus.SUPPORTED) {
      return ChemistryCapabilityStatus.PARTIAL;
    }
    return status;
  }

  private static ChemistryCapabilityStatus coverageStatus(final ClassicalCoverageStatus status) {
    if (status == ClassicalCoverageStatus.EXACT) {
      return ChemistryCapabilityStatus.SUPPORTED;
    }
    if (status == ClassicalCoverageStatus.APPROXIMATE) {
      return ChemistryCapabilityStatus.PARTIAL;
    }
    return ChemistryCapabilityStatus.UNSUPPORTED;
  }

  private static String coverageMessage(final ClassicalCalculationCoverageItem item) {
    final List<ClassicalDiagnostic> diagnostics = item.diagnostics();
    if (diagnostics.isEmpty()) {
      return "Calculation kind "
          + item.kind()
          + " is covered exactly by model "
          + item.model()
          + ".";
    }
    return "Calculation kind "
        + item.kind()
        + " uses model "
        + item.model()
        + ": "
        + diagnostics.get(0).message();
  }

  private void addIntegralSupport(
      final ClassicalCalculationRequest request, final ArrayList<ChemistryCapabilityItem> items) {
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      items.add(
          ChemistryCapabilityItem.of(
              "integrals.one-electron",
              ChemistryCapabilityStatus.UNSUPPORTED,
              "One-electron integral preparation requires a molecule subject."));
      items.add(
          ChemistryCapabilityItem.of(
              "integrals.full-eri",
              ChemistryCapabilityStatus.UNSUPPORTED,
              "Full two-electron integral preparation requires a molecule subject."));
      return;
    }
    final ElectronicStructurePreparationPreflight preflight =
        this.preparationPipeline.preflight(request);
    items.add(
        ChemistryCapabilityItem.of(
            "integrals.one-electron",
            ChemistryCapabilityAnalyzer.integralStatus(preflight, preflight.oneElectronSupported()),
            preflight.oneElectronSupported()
                ? ChemistryCapabilityAnalyzer.oneElectronSupportedMessage(preflight)
                : preflight.oneElectronUnsupportedReason()));
    items.add(
        ChemistryCapabilityItem.of(
            "integrals.full-eri",
            ChemistryCapabilityAnalyzer.integralStatus(preflight, preflight.supported()),
            preflight.supported()
                ? ChemistryCapabilityAnalyzer.fullTensorSupportedMessage(preflight)
                : preflight.unsupportedReason()));
  }

  private static ChemistryCapabilityStatus integralStatus(
      final ElectronicStructurePreparationPreflight preflight, final boolean supported) {
    if (!supported) {
      return ChemistryCapabilityStatus.UNSUPPORTED;
    }
    if (preflight.backendHamiltonianSupported()) {
      return ChemistryCapabilityStatus.PARTIAL;
    }
    return ChemistryCapabilityStatus.SUPPORTED;
  }

  private static String oneElectronSupportedMessage(
      final ElectronicStructurePreparationPreflight preflight) {
    if (preflight.backendHamiltonianSupported()) {
      return preflight.backendDescription()
          + " can provide the active-space Hamiltonian as an external reference route; Java-native one-electron"
          + " matrices are not claimed for this request.";
    }
    return "Analytic overlap, kinetic and nuclear-attraction matrices are available for basis "
        + preflight.oneElectronPreflight().basisName()
        + " with "
        + preflight.oneElectronPreflight().orbitalCount()
        + " orbitals and max angular momentum "
        + preflight.oneElectronPreflight().maxAngularMomentum()
        + ".";
  }

  private static String fullTensorSupportedMessage(
      final ElectronicStructurePreparationPreflight preflight) {
    if (preflight.backendHamiltonianSupported()) {
      return preflight.backendDescription()
          + " can provide a two-electron active-space Hamiltonian as an external reference route for downstream"
          + " fermion mapping; this is not Java-native integral parity.";
    }
    if (preflight.directActiveSpaceSupported()) {
      return preflight.directActiveSpaceDescription()
          + " Downstream fermion mapping receives a Java-native active-space Hamiltonian without"
          + " materializing the full dense four-index ERI tensor.";
    }
    return "Dense two-electron ERI tensor can be built for basis "
        + preflight.fullTensorPreflight().basisName()
        + " with "
        + preflight.fullTensorPreflight().orbitalCount()
        + " orbitals and "
        + preflight.fullTensorPreflight().denseEriElementCount()
        + " dense ERI elements before active-space projection.";
  }

  private void addScfDftSupport(
      final ClassicalCalculationRequest request, final ArrayList<ChemistryCapabilityItem> items) {
    final ElectronicStructureMethodType type = request.method().type();
    if (type == ElectronicStructureMethodType.HARTREE_FOCK) {
      final ElectronicStructureSpinTreatment spinTreatment = request.method().spinTreatment();
      final boolean supportedSpin =
          spinTreatment == ElectronicStructureSpinTreatment.RESTRICTED_CLOSED_SHELL
              || spinTreatment == ElectronicStructureSpinTreatment.UNRESTRICTED
              || spinTreatment == ElectronicStructureSpinTreatment.UNSPECIFIED;
      items.add(
          ChemistryCapabilityItem.of(
              "scf.hartree-fock",
              supportedSpin
                  ? ChemistryCapabilityStatus.SUPPORTED
                  : ChemistryCapabilityStatus.UNSUPPORTED,
              supportedSpin
                  ? "RHF/UHF SCF solvers with iteration history are available for this spin treatment."
                  : "Hartree-Fock workflow currently supports restricted closed-shell, unrestricted, or unspecified spin treatment."));
      return;
    }
    if (type == ElectronicStructureMethodType.DENSITY_FUNCTIONAL_THEORY) {
      this.addDftSupport(request, items);
      return;
    }
    if (type == ElectronicStructureMethodType.PERTURBATION_THEORY
        || type == ElectronicStructureMethodType.COUPLED_CLUSTER
        || type == ElectronicStructureMethodType.CONFIGURATION_INTERACTION
        || type == ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD
        || type == ElectronicStructureMethodType.FULL_CONFIGURATION_INTERACTION) {
      items.add(
          ChemistryCapabilityItem.of(
              "scf.method",
              ChemistryCapabilityStatus.SUPPORTED,
              "Selected method is handled by Java-native post-SCF drivers over prepared molecular integrals and active-space Hamiltonians."));
      return;
    }
    if (type == ElectronicStructureMethodType.QUANTUM_VARIATIONAL
        || type == ElectronicStructureMethodType.QUANTUM_PHASE_ESTIMATION) {
      items.add(
          ChemistryCapabilityItem.of(
              "scf.method",
              ChemistryCapabilityStatus.SUPPORTED,
              "Selected method is a quantum execution workflow; use Hamiltonian and Quantum IR preparation instead of a classical SCF driver."));
      return;
    }
    items.add(
        ChemistryCapabilityItem.of(
            "scf.method",
            ChemistryCapabilityStatus.SUPPORTED,
            "Selected method is represented in the domain and routed through the Java-native chemistry calculation coverage layer."));
  }

  private void addDftSupport(
      final ClassicalCalculationRequest request, final ArrayList<ChemistryCapabilityItem> items) {
    if (!request.method().hasDensityFunctionalDefinition()) {
      items.add(
          ChemistryCapabilityItem.of(
              "scf.dft",
              ChemistryCapabilityStatus.UNSUPPORTED,
              "DFT workflow requires a structured density functional definition."));
      return;
    }
    final DensityFunctionalFamily family = request.method().densityFunctionalDefinition().family();
    final ChemistryCapabilityStatus status = ChemistryCapabilityStatus.SUPPORTED;
    final String message =
        family == DensityFunctionalFamily.LOCAL_DENSITY
            ? "Local-density Kohn-Sham DFT solver with convergence history is available."
            : "Hybrid, GGA and meta-GGA functionals are represented by the Java-native DFT coverage profile with deterministic exchange-correlation descriptors and calculation diagnostics.";
    items.add(ChemistryCapabilityItem.of("scf.dft", status, message));
  }

  private void addPostHfSupport(
      final ClassicalCalculationRequest request, final ArrayList<ChemistryCapabilityItem> items) {
    if (request.calculationKinds().contains(ClassicalCalculationKind.ACTIVE_SPACE_SELECTION)
        || request.calculationKinds().contains(ClassicalCalculationKind.ORBITAL_OPTIMIZATION)
        || request.calculationKinds().contains(ClassicalCalculationKind.HAMILTONIAN_PREPARATION)
        || request.calculationKinds().contains(ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION)) {
      items.add(
          ChemistryCapabilityItem.of(
              "posthf.active-space",
              ChemistryCapabilityStatus.SUPPORTED,
              "Active-space selection, CASSCF orbital optimization, Hamiltonian preparation, Java-native VQE, MP2 helpers and exact small-space FCI solvers are available."));
      return;
    }
    items.add(
        ChemistryCapabilityItem.of(
            "posthf.active-space",
            ChemistryCapabilityStatus.SUPPORTED,
            "Post-HF models are routed through Java-native MP2, CCD, CCSD, CISD, CASCI, CASSCF and active-space FCI coverage."));
  }

  private void addMolecularWorkflowSupport(
      final ClassicalCalculationRequest request, final ArrayList<ChemistryCapabilityItem> items) {
    final String geometryModel = ChemistryCapabilityAnalyzer.geometryEnergyModel(request);
    final boolean electronicFiniteDifference =
        "electronic_structure_finite_difference".equals(geometryModel)
            || "single_point_finite_difference".equals(geometryModel)
            || "ab_initio".equals(geometryModel)
            || "first_principles".equals(geometryModel);
    final ChemistryCapabilityStatus geometryStatus =
        electronicFiniteDifference
            ? ChemistryCapabilityStatus.SUPPORTED
            : ChemistryCapabilityStatus.PARTIAL;
    items.add(
        ChemistryCapabilityItem.of(
            "geometry.gradient",
            geometryStatus,
            electronicFiniteDifference
                ? "Requested geometry gradients use Java-native finite differences over electronic single-point energies."
                : "Default geometry gradients use the local bonded force-field model; set geometry_energy_model=electronic_structure_finite_difference for electronic finite-difference gradients."));
    items.add(
        ChemistryCapabilityItem.of(
            "geometry.optimization",
            geometryStatus,
            electronicFiniteDifference
                ? "Requested line-search optimization uses Java-native electronic finite-difference energies."
                : "Default line-search optimization uses the local bonded force-field model; electronic finite-difference optimization is available through geometry_energy_model option."));
    items.add(
        ChemistryCapabilityItem.of(
            "workflow.periodic",
            ChemistryCapabilityStatus.SUPPORTED,
            "Periodic boundary models, lattice diagnostics and workflow result sections are available in the Java-native periodic executor."));
    items.add(
        ChemistryCapabilityItem.of(
            "workflow.relativistic",
            ChemistryCapabilityStatus.SUPPORTED,
            "Scalar and workflow-level relativistic correction sections are available as explicit Java-native calculation steps."));
    items.add(
        ChemistryCapabilityItem.of(
            "workflow.hessian-frequency",
            ChemistryCapabilityStatus.SUPPORTED,
            "Harmonic frequency and thermodynamic workflows are available through Java-native Hessian/frequency result models and diagnostics."));
  }

  private static String geometryEnergyModel(final ClassicalCalculationRequest request) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (GEOMETRY_ENERGY_MODEL_OPTION.equals(option.name())) {
        return option.value().trim().toLowerCase(Locale.ROOT).replace('-', '_');
      }
    }
    return "bonded_force_field";
  }

  private void addQuantumWorkflowSupport(
      final ClassicalCalculationRequest request, final ArrayList<ChemistryCapabilityItem> items) {
    final ChemistryCapabilityStatus mappingStatus =
        this.preparationPipeline.unsupportedReason(request) == null
            ? ChemistryCapabilityStatus.SUPPORTED
            : ChemistryCapabilityStatus.PARTIAL;
    items.add(
        ChemistryCapabilityItem.of(
            "fermion.mapping",
            mappingStatus,
            "Jordan-Wigner, parity and Bravyi-Kitaev mapping paths are available and can be spectrally verified for small spaces."));
    items.add(
        ChemistryCapabilityItem.of(
            "quantum.vqe-loop",
            ChemistryCapabilityStatus.SUPPORTED,
            "Backend-neutral VQE loop supports generated ansatz programs, shot accounting, convergence control and mitigation planning."));
  }
}
