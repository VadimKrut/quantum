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
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper.VerifiedElectronicHamiltonianMapping;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.StateVectorPauliVqeEnergyBackend;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe.VqeOptimizer;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.ChemistryAnsatzKind;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.ansatz.VariationalChemistryOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationTechnique;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.QuantumNoiseModel;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeIteration;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationRequest;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeOptimizationResult;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;

/**
 * Выполняет локальный Java-native VQE loop поверх подготовленного electronic Hamiltonian.
 */
public final class QuantumVqeOptimizationExecutor {

  private static final double DEFAULT_VERIFICATION_TOLERANCE = 1.0E-8;
  private static final int DEFAULT_MAX_ITERATIONS = 24;
  private static final int DEFAULT_BASE_SHOTS = 1024;
  private static final int DEFAULT_MAX_SHOTS = 8192;

  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final ElectronicHamiltonianQubitMapper qubitMapper;
  private final ActiveSpaceRequestResolver activeSpaceResolver;

  public QuantumVqeOptimizationExecutor() {
    this(new ElectronicStructurePreparationPipeline(), new ElectronicHamiltonianQubitMapper());
  }

  public QuantumVqeOptimizationExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final ElectronicHamiltonianQubitMapper qubitMapper) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException("Quantum VQE preparation pipeline must not be null.");
    }
    if (qubitMapper == null) {
      throw new IllegalArgumentException("Quantum VQE mapper must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.qubitMapper = qubitMapper;
    this.activeSpaceResolver =
        new ActiveSpaceRequestResolver(preparationPipeline, new ActiveSpaceSelector());
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Quantum VQE request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Quantum VQE diagnostics target must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION.name()),
              "Quantum VQE optimization requires molecule subject."));
      return null;
    }
    try {
      final ClassicalCalculationRequest resolvedRequest =
          this.resolvedRequest(request, diagnostics);
      final ElectronicStructurePreparationResult preparation =
          this.preparationPipeline.prepare(resolvedRequest);
      final FermionToQubitMapping mapping = QuantumVqeOptimizationExecutor.mapping(resolvedRequest);
      final double verificationTolerance =
          QuantumVqeOptimizationExecutor.doubleOption(
              resolvedRequest,
              "vqe_mapping_verification_tolerance",
              DEFAULT_VERIFICATION_TOLERANCE);
      final VerifiedElectronicHamiltonianMapping verifiedMapping =
          this.qubitMapper.mapNormalOrderedVerified(
              preparation.hamiltonian(), mapping, verificationTolerance);
      final VqeOptimizationRequest optimizationRequest =
          QuantumVqeOptimizationExecutor.optimizationRequest(
              resolvedRequest, preparation.hamiltonian().activeSpace(), verifiedMapping, mapping);
      final VqeOptimizationResult result =
          new VqeOptimizer(
                  new StateVectorPauliVqeEnergyBackend(
                      verifiedMapping.qubitOperator(),
                      new ru.pathcreator.vadim.quantum.chemistry.quantum.application.estimator
                          .StateVectorPauliExpectationEstimator(),
                      optimizationRequest.ansatzOptions().parameterPrefix(),
                      verifiedMapping.qubitOperator().requiredQubitCount()))
              .optimize(optimizationRequest);
      return QuantumVqeOptimizationExecutor.resultSection(
          resolvedRequest, preparation, verifiedMapping, result);
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.RESULT_FAILED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION.name()),
              exception.getMessage()));
      return null;
    }
  }

  private ClassicalCalculationRequest resolvedRequest(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (!request.hasActiveSpace()) {
      return this.activeSpaceResolver
          .resolve(request, diagnostics, ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION.name())
          .request();
    }
    final String unsupportedReason = this.preparationPipeline.unsupportedReason(request);
    if (unsupportedReason == null) {
      return request;
    }
    return this.activeSpaceResolver
        .resolve(request, diagnostics, ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION.name())
        .request();
  }

  private static VqeOptimizationRequest optimizationRequest(
      final ClassicalCalculationRequest request,
      final ActiveSpace activeSpace,
      final VerifiedElectronicHamiltonianMapping mapping,
      final FermionToQubitMapping requestedMapping) {
    final int parameterCount =
        QuantumVqeOptimizationExecutor.intOption(
            request, "vqe_parameter_count", Math.max(1, activeSpace.orbitalCount() * 2));
    final double initialParameter =
        QuantumVqeOptimizationExecutor.doubleOption(request, "vqe_initial_parameter", 0.0);
    final double[] initialParameters = new double[parameterCount];
    for (int i = 0; i < initialParameters.length; ++i) {
      initialParameters[i] = initialParameter;
    }
    return VqeOptimizationRequest.of(
        activeSpace,
        VariationalChemistryOptions.of(
            QuantumVqeOptimizationExecutor.ansatzKind(request),
            "quantum_vqe_optimization",
            "q",
            "c",
            "theta",
            QuantumVqeOptimizationExecutor.intOption(request, "vqe_ansatz_layers", 1),
            QuantumVqeOptimizationExecutor.intOption(request, "vqe_adapt_operator_limit", 8),
            true,
            requestedMapping),
        initialParameters,
        VqeOptimizationOptions.of(
            QuantumVqeOptimizationExecutor.intOption(
                request, "vqe_max_iterations", DEFAULT_MAX_ITERATIONS),
            QuantumVqeOptimizationExecutor.intOption(request, "vqe_base_shots", DEFAULT_BASE_SHOTS),
            QuantumVqeOptimizationExecutor.doubleOption(request, "vqe_initial_step", 0.25),
            QuantumVqeOptimizationExecutor.doubleOption(request, "vqe_minimum_step", 1.0E-4),
            QuantumVqeOptimizationExecutor.doubleOption(request, "vqe_energy_tolerance", 1.0E-6),
            QuantumVqeOptimizationExecutor.doubleOption(request, "vqe_parameter_tolerance", 1.0E-5),
            QuantumVqeOptimizationExecutor.doubleOption(request, "vqe_gradient_probe_step", 1.0E-3),
            QuantumVqeOptimizationExecutor.doubleOption(request, "vqe_gradient_tolerance", 1.0E-4),
            QuantumVqeOptimizationExecutor.intOption(request, "vqe_max_shots", DEFAULT_MAX_SHOTS),
            QuantumVqeOptimizationExecutor.doubleOption(request, "vqe_shot_growth_factor", 1.1),
            QuantumVqeOptimizationExecutor.intOption(request, "vqe_max_backend_retries", 1)),
        ErrorMitigationRequest.of(
            activeSpace,
            QuantumNoiseModel.ideal(),
            List.of(
                ErrorMitigationTechnique.READOUT_CALIBRATION,
                ErrorMitigationTechnique.SYMMETRY_VERIFICATION),
            QuantumVqeOptimizationExecutor.intOption(request, "vqe_base_shots", DEFAULT_BASE_SHOTS),
            true,
            activeSpace.electronCount() % 2 == 0
                && mapping.qubitOperator().requiredQubitCount() > 1));
  }

  private static ClassicalResultSection resultSection(
      final ClassicalCalculationRequest request,
      final ElectronicStructurePreparationResult preparation,
      final VerifiedElectronicHamiltonianMapping mapping,
      final VqeOptimizationResult result) {
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native_state_vector",
        "variational_quantum_eigensolver",
        "active_space_qubit_hamiltonian",
        "VQE expectation values use an exact local state-vector backend, but optimizer convergence is variational and depends on ansatz and iteration limits.");
    values.add(ClassicalResultValue.text("method", request.method().name().value()));
    values.add(ClassicalResultValue.text("basis_set", preparation.basisSetName()));
    values.add(ClassicalResultValue.text("preparation_mode", preparation.mode().name()));
    values.add(ClassicalResultValue.text("mapping", mapping.mapping().name()));
    values.add(ClassicalResultValue.text("mapping_verified", Boolean.toString(mapping.verified())));
    values.add(
        ClassicalResultValue.scalar(
            "pauli_term_count", mapping.qubitOperator().termCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "required_qubits", mapping.qubitOperator().requiredQubitCount(), "count"));
    values.add(ClassicalResultValue.text("vqe_status", result.status().name()));
    values.add(ClassicalResultValue.text("vqe_converged", Boolean.toString(result.converged())));
    values.add(ClassicalResultValue.scalar("vqe_best_energy", result.bestEnergy(), "hartree"));
    values.add(
        ClassicalResultValue.scalar("vqe_iteration_count", result.iterations().size(), "count"));
    values.add(
        ClassicalResultValue.scalar("vqe_evaluation_count", result.evaluationCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "vqe_failed_evaluation_count", result.failedEvaluationCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "vqe_failed_evaluation_ratio", result.failedEvaluationRatio(), "ratio"));
    values.add(ClassicalResultValue.scalar("vqe_total_shots", result.totalShots(), "shots"));
    values.add(
        ClassicalResultValue.scalar(
            "vqe_program_operation_count",
            QuantumVqeOptimizationExecutor.operationCount(result.program()),
            "count"));
    values.add(ClassicalResultValue.text("vqe_diagnostic", result.diagnostic()));
    values.add(
        ClassicalResultValue.of(
            "vqe_iterations",
            ClassicalResultValueKind.TABLE,
            "",
            QuantumVqeOptimizationExecutor.iterationRows(result.iterations())));
    return ClassicalResultSection.of(
        ClassicalCalculationKind.QUANTUM_VQE_OPTIMIZATION,
        "Java-native state-vector VQE optimization",
        values);
  }

  private static List<String> iterationRows(final List<VqeIteration> iterations) {
    final ArrayList<String> rows = new ArrayList<String>(iterations.size() + 1);
    rows.add("iteration\tenergy\tuncertainty\tstep\tgradient_norm\taccepted");
    for (int i = 0; i < iterations.size(); ++i) {
      final VqeIteration iteration = iterations.get(i);
      rows.add(
          iteration.index()
              + "\t"
              + iteration.energy()
              + "\t"
              + iteration.uncertainty()
              + "\t"
              + iteration.stepSize()
              + "\t"
              + iteration.gradientNorm()
              + "\t"
              + iteration.acceptedImprovement());
    }
    return rows;
  }

  private static long operationCount(
      final ru.pathcreator.vadim.quantum.domain.model.QuantumProgram program) {
    long count = 0L;
    for (int i = 0; i < program.circuitCount(); ++i) {
      count += program.circuit(i).operationCount();
    }
    return count;
  }

  private static ChemistryAnsatzKind ansatzKind(final ClassicalCalculationRequest request) {
    return ChemistryAnsatzKind.valueOf(
        QuantumVqeOptimizationExecutor.textOption(
                request, "vqe_ansatz", ChemistryAnsatzKind.HARDWARE_EFFICIENT_VQE.name())
            .toUpperCase());
  }

  private static FermionToQubitMapping mapping(final ClassicalCalculationRequest request) {
    return FermionToQubitMapping.valueOf(
        QuantumVqeOptimizationExecutor.textOption(
                request, "fermion_to_qubit_mapping", FermionToQubitMapping.JORDAN_WIGNER.name())
            .toUpperCase());
  }

  private static String textOption(
      final ClassicalCalculationRequest request, final String name, final String defaultValue) {
    final ClassicalCalculationOption option =
        QuantumVqeOptimizationExecutor.optionOrNull(request, name);
    return option == null ? defaultValue : option.value();
  }

  private static double doubleOption(
      final ClassicalCalculationRequest request, final String name, final double defaultValue) {
    final ClassicalCalculationOption option =
        QuantumVqeOptimizationExecutor.optionOrNull(request, name);
    return option == null ? defaultValue : Double.parseDouble(option.value());
  }

  private static int intOption(
      final ClassicalCalculationRequest request, final String name, final int defaultValue) {
    final ClassicalCalculationOption option =
        QuantumVqeOptimizationExecutor.optionOrNull(request, name);
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
