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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.operator.FermionOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianAuditReport;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.validation.FermionHamiltonianDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.ChemistryQuantumProgramPackageGenerator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumPreparationProvenance;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramArtifact;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackage;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramPackageOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program.ChemistryQuantumProgramSkip;

/** Готовит Quantum IR программы из Java-native electronic Hamiltonian без внешнего расчетного backend. */
public final class QuantumProgramPreparationExecutor {

  private static final String DEFAULT_MAPPING = "JORDAN_WIGNER";
  private static final double DEFAULT_VERIFICATION_TOLERANCE = 1.0E-8;

  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final ChemistryQuantumProgramPackageGenerator packageGenerator;
  private final ElectronicHamiltonianQubitMapper qubitMapper;
  private final ActiveSpaceRequestResolver activeSpaceResolver;

  public QuantumProgramPreparationExecutor() {
    this(
        new ElectronicStructurePreparationPipeline(),
        new ChemistryQuantumProgramPackageGenerator(),
        new ElectronicHamiltonianQubitMapper());
  }

  public QuantumProgramPreparationExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(
        preparationPipeline,
        new ChemistryQuantumProgramPackageGenerator(),
        new ElectronicHamiltonianQubitMapper());
  }

  public QuantumProgramPreparationExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ChemistryQuantumProgramPackageGenerator packageGenerator,
      final ElectronicHamiltonianQubitMapper qubitMapper) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException("Quantum program preparation pipeline must not be null.");
    }
    if (packageGenerator == null) {
      throw new IllegalArgumentException("Quantum program package generator must not be null.");
    }
    if (qubitMapper == null) {
      throw new IllegalArgumentException(
          "Quantum program preparation qubit mapper must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.packageGenerator = packageGenerator;
    this.qubitMapper = qubitMapper;
    this.activeSpaceResolver =
        new ActiveSpaceRequestResolver(preparationPipeline, new ActiveSpaceSelector());
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Quantum program preparation request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException(
          "Quantum program preparation diagnostics target must not be null.");
    }
    try {
      final ClassicalCalculationRequest resolvedRequest =
          this.preparationRequest(request, diagnostics);
      final ElectronicStructurePreparationResult preparation =
          this.preparationPipeline.prepare(resolvedRequest);
      final FermionToQubitMapping requestedMapping =
          QuantumProgramPreparationExecutor.mapping(resolvedRequest);
      final double verificationTolerance =
          QuantumProgramPreparationExecutor.doubleOption(
              resolvedRequest,
              "quantum_program_verification_tolerance",
              DEFAULT_VERIFICATION_TOLERANCE);
      final VerifiedElectronicHamiltonianMapping verifiedMapping =
          this.qubitMapper.mapNormalOrderedVerified(
              preparation.hamiltonian(), requestedMapping, verificationTolerance);
      final ChemistryQuantumProgramPackage pack =
          this.packageGenerator.generate(
              resolvedRequest.id().value(),
              preparation.hamiltonian(),
              requestedMapping,
              QuantumProgramPreparationExecutor.provenance(
                  resolvedRequest, preparation, requestedMapping),
              QuantumProgramPreparationExecutor.packageOptions(
                  resolvedRequest, requestedMapping, verificationTolerance));
      return QuantumProgramPreparationExecutor.resultSection(
          resolvedRequest, preparation, verifiedMapping, verificationTolerance, pack);
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_FAILED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION.name()),
              exception.getMessage()));
      return null;
    }
  }

  private ClassicalCalculationRequest preparationRequest(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (!request.hasActiveSpace()) {
      return this.activeSpaceResolver
          .resolve(
              request, diagnostics, ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION.name())
          .request();
    }
    final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
    if (unsupportedReason == null) {
      return request;
    }
    return this.activeSpaceResolver
        .resolve(request, diagnostics, ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION.name())
        .request();
  }

  private static ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparation,
      final VerifiedElectronicHamiltonianMapping verifiedMapping,
      final double verificationTolerance,
      final ChemistryQuantumProgramPackage pack) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    final FermionOperator fermionOperator = verifiedMapping.fermionOperator();
    final QubitPauliOperator qubitOperator = verifiedMapping.qubitOperator();
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("basis_set", preparation.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "geometry_source",
            QuantumProgramPreparationExecutor.textOption(
                request, "quantum_preparation_geometry_source", "request_coordinates")));
    values.add(ClassicalResultValue.text("preparation_mode", preparation.mode().name()));
    values.add(
        ClassicalResultValue.text(
            "reference_method", QuantumProgramPreparationExecutor.referenceMethod(preparation)));
    values.add(
        ClassicalResultValue.text(
            "native_java_preparation",
            Boolean.toString(
                preparation.mode() != ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN)));
    values.add(
        ClassicalResultValue.text(
            "external_reference_route",
            Boolean.toString(
                preparation.mode() == ElectronicStructurePreparationMode.BACKEND_HAMILTONIAN)));
    values.add(
        ClassicalResultValue.text(
            "two_electron_integrals_available",
            Boolean.toString(preparation.twoElectronIntegralsAvailable())));
    values.add(
        ClassicalResultValue.text(
            "hamiltonian_completeness",
            preparation.twoElectronIntegralsAvailable()
                ? "FULL_TWO_ELECTRON"
                : "ONE_ELECTRON_ONLY"));
    if (preparation.hasLimitationReason()) {
      values.add(
          ClassicalResultValue.text(
              "preparation_limitation_reason", preparation.limitationReason()));
    }
    values.add(
        ClassicalResultValue.text(
            "orbital_optimized_reference", Boolean.toString(preparation.hasCasscfResult())));
    if (preparation.hasCasscfResult()) {
      values.add(
          ClassicalResultValue.text(
              "orbital_optimization_quality",
              preparation.casscfResult().orbitalOptimizationQuality()));
      values.add(
          ClassicalResultValue.text(
              "orbital_optimization_stop_reason",
              preparation.casscfResult().orbitalOptimizationStopReason()));
      values.add(
          ClassicalResultValue.scalar(
              "orbital_rotation_pair_coverage_ratio",
              preparation.casscfResult().orbitalRotationPairCoverageRatio(),
              "ratio"));
      values.add(
          ClassicalResultValue.scalar(
              "available_orbital_rotation_pair_count",
              preparation.casscfResult().availableRotationPairCount(),
              "count"));
    }
    values.add(ClassicalResultValue.text("source_id", pack.sourceId()));
    values.add(ClassicalResultValue.text("requested_mapping", pack.requestedMapping().name()));
    QuantumProgramPreparationExecutor.addQualityValues(request, preparation, values);
    values.add(
        ClassicalResultValue.text(
            "generation_complete", Boolean.toString(pack.generationComplete())));
    values.add(ClassicalResultValue.scalar("artifact_count", pack.artifactCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "skipped_artifact_count", pack.skippedArtifactCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("total_operation_count", pack.totalOperationCount(), "count"));
    values.add(
        ClassicalResultValue.text(
            "adapt_operator_limit",
            QuantumProgramPreparationExecutor.adaptOperatorLimitLabel(
                QuantumProgramPreparationExecutor.packageOptions(
                    request, pack.requestedMapping(), verificationTolerance))));
    values.add(
        ClassicalResultValue.text(
            "quantum_workflow_coverage_status",
            QuantumProgramPreparationExecutor.workflowCoverageStatus(pack)));
    values.add(
        ClassicalResultValue.text(
            "vqe_ready", Boolean.toString(QuantumProgramPreparationExecutor.vqeReady(pack))));
    values.add(
        ClassicalResultValue.text(
            "uccsd_ansatz_available",
            Boolean.toString(pack.hasArtifact(ChemistryQuantumProgramKind.UCCSD_ANSATZ))));
    values.add(
        ClassicalResultValue.text(
            "hardware_efficient_vqe_available",
            Boolean.toString(
                pack.hasArtifact(ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ))));
    values.add(
        ClassicalResultValue.text(
            "adapt_vqe_ansatz_available",
            Boolean.toString(pack.hasArtifact(ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ))));
    values.add(
        ClassicalResultValue.text(
            "pauli_evolution_available",
            Boolean.toString(pack.hasArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION))));
    values.add(
        ClassicalResultValue.text(
            "verified_pauli_evolution_available",
            Boolean.toString(
                pack.hasArtifact(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION))));
    values.add(
        ClassicalResultValue.text(
            "qpe_available",
            Boolean.toString(
                pack.hasArtifact(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION))));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_variant_count",
            QuantumProgramPreparationExecutor.mappingVariantCount(pack),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "verified_mapping_variant_count",
            QuantumProgramPreparationExecutor.verifiedMappingVariantCount(pack),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "vqe_ansatz_operation_count",
            QuantumProgramPreparationExecutor.operationCount(
                pack,
                ChemistryQuantumProgramKind.UCCSD_ANSATZ,
                ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ,
                ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ),
            "count"));
    values.add(ClassicalResultValue.scalar("electron_count", pack.electronCount(), "count"));
    values.add(ClassicalResultValue.scalar("spin_orbital_count", pack.spinOrbitalCount(), "count"));
    values.add(
        ClassicalResultValue.scalar("fermion_term_count", fermionOperator.termCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "requested_mapping_pauli_term_count", qubitOperator.termCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "requested_mapping_required_qubits", qubitOperator.requiredQubitCount(), "count"));
    values.add(
        ClassicalResultValue.text(
            "requested_mapping_hermitian",
            Boolean.toString(qubitOperator.hermitianWithinTolerance(verificationTolerance))));
    values.add(
        ClassicalResultValue.text(
            "requested_mapping_verified", Boolean.toString(verifiedMapping.verified())));
    QuantumProgramPreparationExecutor.addHamiltonianAuditValues(values, verifiedMapping);
    values.add(
        ClassicalResultValue.text(
            "mapping_verification_mode", verifiedMapping.spectralResult().verificationMode()));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_algebra_max_residual",
            verifiedMapping.algebraResult().maxResidual(),
            "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_spectral_max_difference",
            verifiedMapping.spectralResult().maxDifference(),
            "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_fermion_hermitian_deviation",
            verifiedMapping.spectralResult().fermionHermitianDeviation(),
            "dimensionless"));
    values.add(
        ClassicalResultValue.scalar(
            "mapping_qubit_hermitian_deviation",
            verifiedMapping.spectralResult().qubitHermitianDeviation(),
            "dimensionless"));
    values.add(ClassicalResultValue.text("artifact_ids", String.join(",", pack.artifactIds())));
    values.add(
        ClassicalResultValue.of(
            "requested_mapping_verification",
            ClassicalResultValueKind.TABLE,
            "",
            QuantumProgramPreparationExecutor.mappingVerificationRows(verifiedMapping)));
    values.add(
        ClassicalResultValue.of(
            "artifacts",
            ClassicalResultValueKind.TABLE,
            "",
            QuantumProgramPreparationExecutor.artifactRows(pack)));
    values.add(
        ClassicalResultValue.of(
            "vqe_artifacts",
            ClassicalResultValueKind.TABLE,
            "",
            QuantumProgramPreparationExecutor.vqeArtifactRows(pack)));
    values.add(
        ClassicalResultValue.of(
            "skipped_artifacts",
            ClassicalResultValueKind.TABLE,
            "",
            QuantumProgramPreparationExecutor.skipRows(pack)));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION,
        "Quantum IR program package preparation",
        values);
  }

  private static void addHamiltonianAuditValues(
      final ArrayList<ClassicalResultValue> values,
      final VerifiedElectronicHamiltonianMapping verifiedMapping) {
    if (!verifiedMapping.hasHamiltonianAudit()) {
      values.add(ClassicalResultValue.text("hamiltonian_audit_available", "false"));
      return;
    }
    final FermionHamiltonianAuditReport audit = verifiedMapping.hamiltonianAudit();
    values.add(ClassicalResultValue.text("hamiltonian_audit_available", "true"));
    values.add(
        ClassicalResultValue.text(
            "hamiltonian_ready_for_mapping", Boolean.toString(audit.readyForFermionMapping())));
    values.add(
        ClassicalResultValue.scalar(
            "hamiltonian_audit_diagnostic_count", audit.diagnosticCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "hamiltonian_missing_coulomb_diagonal_count",
            audit.missingCoulombDiagonalCount(),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "hamiltonian_max_one_electron_integral", audit.maxOneElectronMagnitude(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "hamiltonian_max_two_electron_integral", audit.maxTwoElectronMagnitude(), "hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "hamiltonian_min_coulomb_diagonal", audit.minCoulombDiagonal(), "hartree"));
    values.add(
        ClassicalResultValue.of(
            "hamiltonian_audit_diagnostics",
            ClassicalResultValueKind.TABLE,
            "",
            QuantumProgramPreparationExecutor.hamiltonianAuditRows(audit)));
  }

  private static List<String> hamiltonianAuditRows(final FermionHamiltonianAuditReport audit) {
    final ArrayList<String> rows = new ArrayList<String>(audit.diagnosticCount() + 1);
    rows.add("severity\tcode\ttarget\tmessage");
    final List<FermionHamiltonianDiagnostic> diagnostics = audit.diagnostics();
    for (int i = 0; i < diagnostics.size(); ++i) {
      final FermionHamiltonianDiagnostic diagnostic = diagnostics.get(i);
      rows.add(
          diagnostic.severity().name()
              + "\t"
              + diagnostic.code().name()
              + "\t"
              + diagnostic.target()
              + "\t"
              + diagnostic.message());
    }
    return rows;
  }

  private static List<String> mappingVerificationRows(
      final VerifiedElectronicHamiltonianMapping mapping) {
    return List.of(
        "mapping\tfermion_terms\tpauli_terms\trequired_qubits\talgebra_max_residual\tspectral_max_difference\tmode\tverified",
        mapping.mapping().name()
            + "\t"
            + mapping.fermionOperator().termCount()
            + "\t"
            + mapping.qubitOperator().termCount()
            + "\t"
            + mapping.qubitOperator().requiredQubitCount()
            + "\t"
            + mapping.algebraResult().maxResidual()
            + "\t"
            + mapping.spectralResult().maxDifference()
            + "\t"
            + mapping.spectralResult().verificationMode()
            + "\t"
            + mapping.verified());
  }

  private static List<String> artifactRows(final ChemistryQuantumProgramPackage pack) {
    final ArrayList<String> rows = new ArrayList<String>(pack.artifactCount() + 1);
    rows.add("id\tkind\tmapping\toperations\tdescription");
    final List<ChemistryQuantumProgramArtifact> artifacts = pack.artifacts();
    for (int i = 0; i < artifacts.size(); ++i) {
      final ChemistryQuantumProgramArtifact artifact = artifacts.get(i);
      rows.add(
          artifact.id()
              + "\t"
              + artifact.kind().name()
              + "\t"
              + QuantumProgramPreparationExecutor.mappingName(artifact.mapping())
              + "\t"
              + artifact.operationCount()
              + "\t"
              + artifact.description());
    }
    return rows;
  }

  private static List<String> vqeArtifactRows(final ChemistryQuantumProgramPackage pack) {
    final ArrayList<String> rows = new ArrayList<String>(pack.artifactCount() + 1);
    rows.add("id\tkind\tmapping\toperations\tvqe_role");
    final List<ChemistryQuantumProgramArtifact> artifacts = pack.artifacts();
    for (int i = 0; i < artifacts.size(); ++i) {
      final ChemistryQuantumProgramArtifact artifact = artifacts.get(i);
      final String role = QuantumProgramPreparationExecutor.vqeRole(artifact.kind());
      if (!role.isEmpty()) {
        rows.add(
            artifact.id()
                + "\t"
                + artifact.kind().name()
                + "\t"
                + QuantumProgramPreparationExecutor.mappingName(artifact.mapping())
                + "\t"
                + artifact.operationCount()
                + "\t"
                + role);
      }
    }
    return rows;
  }

  private static List<String> skipRows(final ChemistryQuantumProgramPackage pack) {
    final ArrayList<String> rows = new ArrayList<String>(pack.skippedArtifactCount() + 1);
    rows.add("id\tkind\tmapping\treason");
    final List<ChemistryQuantumProgramSkip> skips = pack.skippedArtifacts();
    for (int i = 0; i < skips.size(); ++i) {
      final ChemistryQuantumProgramSkip skip = skips.get(i);
      rows.add(
          skip.id()
              + "\t"
              + skip.kind().name()
              + "\t"
              + QuantumProgramPreparationExecutor.mappingName(skip.mapping())
              + "\t"
              + skip.reason());
    }
    return rows;
  }

  private static boolean vqeReady(final ChemistryQuantumProgramPackage pack) {
    return pack.hasArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION)
        && (pack.hasArtifact(ChemistryQuantumProgramKind.UCCSD_ANSATZ)
            || pack.hasArtifact(ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ)
            || pack.hasArtifact(ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ));
  }

  private static String workflowCoverageStatus(final ChemistryQuantumProgramPackage pack) {
    final boolean vqeReady = QuantumProgramPreparationExecutor.vqeReady(pack);
    final boolean qpeReady = pack.hasArtifact(ChemistryQuantumProgramKind.QUANTUM_PHASE_ESTIMATION);
    final boolean verifiedEvolution =
        pack.hasArtifact(ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION);
    final boolean allMappingsVerified =
        QuantumProgramPreparationExecutor.verifiedMappingVariantCount(pack)
            == FermionToQubitMapping.values().length;
    if (vqeReady
        && qpeReady
        && verifiedEvolution
        && allMappingsVerified
        && pack.skippedArtifactCount() == 0) {
      return "FULL_VQE_QPE_VERIFIED_PACKAGE";
    }
    if (vqeReady && verifiedEvolution) {
      return "VQE_READY_WITH_VERIFIED_EVOLUTION";
    }
    if (vqeReady) {
      return "VQE_READY";
    }
    if (pack.hasArtifact(ChemistryQuantumProgramKind.PAULI_EVOLUTION)) {
      return "EVOLUTION_ONLY";
    }
    if (pack.hasArtifacts()) {
      return "PARTIAL_QUANTUM_PACKAGE";
    }
    return "NO_QUANTUM_PROGRAMS";
  }

  private static void addQualityValues(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparation,
      final List<ClassicalResultValue> values) {
    final String geometrySource =
        QuantumProgramPreparationExecutor.textOption(
            request, "quantum_preparation_geometry_source", "request_coordinates");
    if (preparation.mode() == ElectronicStructurePreparationMode.SEMI_EMPIRICAL_VALENCE) {
      ClassicalResultQualityValues.addFallback(
          values,
          "java_native",
          "semiempirical_valence_quantum_workflow",
          "fermion_qubit_program_preparation",
          "Semiempirical valence Hamiltonian was used instead of ab initio Gaussian integrals.");
      return;
    }
    if (!"request_coordinates".equals(geometrySource)) {
      ClassicalResultQualityValues.addApproximate(
          values,
          "java_native",
          "ab_initio_quantum_workflow_with_generated_geometry",
          "fermion_qubit_program_preparation",
          "Input molecule geometry was regularized from graph coordinates before Hamiltonian preparation.");
      return;
    }
    if (preparation.twoElectronIntegralsAvailable()) {
      ClassicalResultQualityValues.addExact(
          values,
          "java_native",
          "ab_initio_two_electron_quantum_workflow",
          "fermion_qubit_program_preparation");
      return;
    }
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "one_electron_quantum_workflow",
        "fermion_qubit_program_preparation",
        "Prepared Hamiltonian does not contain full two-electron interaction tensor.");
  }

  private static int mappingVariantCount(final ChemistryQuantumProgramPackage pack) {
    final boolean[] found = new boolean[FermionToQubitMapping.values().length];
    final List<ChemistryQuantumProgramArtifact> artifacts = pack.artifacts();
    for (int i = 0; i < artifacts.size(); ++i) {
      final FermionToQubitMapping mapping = artifacts.get(i).mapping();
      if (mapping != null) {
        found[mapping.ordinal()] = true;
      }
    }
    return QuantumProgramPreparationExecutor.trueCount(found);
  }

  private static int verifiedMappingVariantCount(final ChemistryQuantumProgramPackage pack) {
    final boolean[] found = new boolean[FermionToQubitMapping.values().length];
    final List<ChemistryQuantumProgramArtifact> artifacts = pack.artifacts();
    for (int i = 0; i < artifacts.size(); ++i) {
      final ChemistryQuantumProgramArtifact artifact = artifacts.get(i);
      if (artifact.kind() == ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION
          && artifact.mapping() != null) {
        found[artifact.mapping().ordinal()] = true;
      }
    }
    return QuantumProgramPreparationExecutor.trueCount(found);
  }

  private static int trueCount(final boolean[] values) {
    int count = 0;
    for (int i = 0; i < values.length; ++i) {
      if (values[i]) {
        ++count;
      }
    }
    return count;
  }

  private static long operationCount(
      final ChemistryQuantumProgramPackage pack,
      final ChemistryQuantumProgramKind firstKind,
      final ChemistryQuantumProgramKind secondKind,
      final ChemistryQuantumProgramKind thirdKind) {
    long count = 0L;
    final List<ChemistryQuantumProgramArtifact> artifacts = pack.artifacts();
    for (int i = 0; i < artifacts.size(); ++i) {
      final ChemistryQuantumProgramKind kind = artifacts.get(i).kind();
      if (kind == firstKind || kind == secondKind || kind == thirdKind) {
        count += artifacts.get(i).operationCount();
      }
    }
    return count;
  }

  private static String vqeRole(final ChemistryQuantumProgramKind kind) {
    if (kind == ChemistryQuantumProgramKind.UCCSD_ANSATZ) {
      return "chemistry_excitation_ansatz";
    }
    if (kind == ChemistryQuantumProgramKind.HARDWARE_EFFICIENT_VQE_ANSATZ) {
      return "hardware_efficient_reference_ansatz";
    }
    if (kind == ChemistryQuantumProgramKind.ADAPT_VQE_ANSATZ) {
      return "adaptive_operator_pool_ansatz";
    }
    if (kind == ChemistryQuantumProgramKind.PAULI_EVOLUTION) {
      return "hamiltonian_measurement_evolution";
    }
    if (kind == ChemistryQuantumProgramKind.VERIFIED_PAULI_EVOLUTION) {
      return "verified_hamiltonian_evolution";
    }
    return "";
  }

  private static ChemistryQuantumProgramPackageOptions packageOptions(
      final ClassicalCalculationRequest request,
      final FermionToQubitMapping requestedMapping,
      final double tolerance) {
    final boolean requestedOnly =
        QuantumProgramPreparationExecutor.booleanOption(
            request, "quantum_program_requested_mapping_only", false);
    final EnumSet<ChemistryQuantumProgramKind> kinds =
        QuantumProgramPreparationExecutor.programKinds(request);
    final int adaptOperatorLimit =
        QuantumProgramPreparationExecutor.intOption(
            request, "quantum_program_adapt_operator_limit", Integer.MAX_VALUE);
    if (requestedOnly) {
      return ChemistryQuantumProgramPackageOptions.requestedMappingOnly(
          requestedMapping, kinds, tolerance, adaptOperatorLimit);
    }
    return ChemistryQuantumProgramPackageOptions.of(
        Arrays.asList(FermionToQubitMapping.values()), kinds, tolerance, adaptOperatorLimit);
  }

  private static ChemistryQuantumPreparationProvenance provenance(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparation,
      final FermionToQubitMapping requestedMapping) {
    return ChemistryQuantumPreparationProvenance.of(
        QuantumProgramPreparationExecutor.referenceMethod(preparation),
        preparation.mode().name(),
        preparation.hamiltonian().electronCount() % 2 == 0
            ? "CLOSED_SHELL_OR_EVEN_ACTIVE_SPACE"
            : "OPEN_SHELL",
        preparation.basisSetName(),
        request.hasActiveSpace()
            ? request.activeSpace().electronCount()
                + "e/"
                + request.activeSpace().orbitalCount()
                + "o"
            : "full_space",
        request.id().value(),
        preparation.hamiltonian().electronCount(),
        preparation.hamiltonian().spinOrbitalCount(),
        preparation.hamiltonian().spatialOrbitalCount(),
        requestedMapping,
        preparation.hasCasscfResult());
  }

  private static String referenceMethod(final ElectronicStructurePreparationResult preparation) {
    if (preparation.hasCasscfResult()) {
      return "CASSCF_ORBITAL_OPTIMIZED_RHF";
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
    if (preparation.coreHamiltonianOrbitalReference()) {
      return "CORE_HAMILTONIAN_ORBITALS";
    }
    return preparation.mode().name();
  }

  private static EnumSet<ChemistryQuantumProgramKind> programKinds(
      final ClassicalCalculationRequest request) {
    final String value =
        QuantumProgramPreparationExecutor.textOption(request, "quantum_program_kinds", "ALL");
    if ("ALL".equalsIgnoreCase(value)) {
      return EnumSet.allOf(ChemistryQuantumProgramKind.class);
    }
    final EnumSet<ChemistryQuantumProgramKind> kinds =
        EnumSet.noneOf(ChemistryQuantumProgramKind.class);
    final String[] parts = value.split(",");
    for (int i = 0; i < parts.length; ++i) {
      final String part = parts[i].trim();
      if (!part.isEmpty()) {
        kinds.add(ChemistryQuantumProgramKind.valueOf(part.toUpperCase()));
      }
    }
    if (kinds.isEmpty()) {
      throw new IllegalArgumentException("Quantum program kind list must not be empty.");
    }
    return kinds;
  }

  private static String mappingName(final FermionToQubitMapping mapping) {
    return mapping == null ? "NONE" : mapping.name();
  }

  private static String adaptOperatorLimitLabel(
      final ChemistryQuantumProgramPackageOptions options) {
    if (options.adaptOperatorLimit() == Integer.MAX_VALUE) {
      return "FULL_OPERATOR_POOL";
    }
    return Integer.toString(options.adaptOperatorLimit());
  }

  private static FermionToQubitMapping mapping(final ClassicalCalculationRequest request) {
    return FermionToQubitMapping.valueOf(
        QuantumProgramPreparationExecutor.textOption(
                request, "fermion_to_qubit_mapping", DEFAULT_MAPPING)
            .toUpperCase());
  }

  private static String textOption(
      final ClassicalCalculationRequest request, final String name, final String defaultValue) {
    final ClassicalCalculationOption option =
        QuantumProgramPreparationExecutor.optionOrNull(request, name);
    return option == null ? defaultValue : option.value();
  }

  private static boolean booleanOption(
      final ClassicalCalculationRequest request, final String name, final boolean defaultValue) {
    final ClassicalCalculationOption option =
        QuantumProgramPreparationExecutor.optionOrNull(request, name);
    return option == null ? defaultValue : Boolean.parseBoolean(option.value());
  }

  private static double doubleOption(
      final ClassicalCalculationRequest request, final String name, final double defaultValue) {
    final ClassicalCalculationOption option =
        QuantumProgramPreparationExecutor.optionOrNull(request, name);
    return option == null ? defaultValue : Double.parseDouble(option.value());
  }

  private static int intOption(
      final ClassicalCalculationRequest request, final String name, final int defaultValue) {
    final ClassicalCalculationOption option =
        QuantumProgramPreparationExecutor.optionOrNull(request, name);
    return option == null ? defaultValue : Integer.parseInt(option.value());
  }

  private static ClassicalCalculationOption optionOrNull(
      final ClassicalCalculationRequest request, final String name) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(name)) {
        return option;
      }
    }
    return null;
  }
}
