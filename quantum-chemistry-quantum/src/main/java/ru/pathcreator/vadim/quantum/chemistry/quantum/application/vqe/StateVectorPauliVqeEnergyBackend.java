/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.vqe;

import java.util.LinkedHashSet;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.application.estimator.StateVectorPauliExpectationEstimator;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.mitigation.ErrorMitigationPlan;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.vqe.VqeEnergyEvaluation;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings;

/**
 * Локальный VQE backend, который оценивает Pauli Hamiltonian через state-vector симуляцию Quantum IR.
 */
public final class StateVectorPauliVqeEnergyBackend implements VqeEnergyBackend {

  private final QubitPauliOperator hamiltonian;
  private final StateVectorPauliExpectationEstimator estimator;
  private final String parameterPrefix;
  private final int maxQubits;

  public StateVectorPauliVqeEnergyBackend(final QubitPauliOperator hamiltonian) {
    this(
        hamiltonian,
        new StateVectorPauliExpectationEstimator(),
        "theta",
        Math.max(1, hamiltonian == null ? 1 : hamiltonian.requiredQubitCount()));
  }

  public StateVectorPauliVqeEnergyBackend(
      final QubitPauliOperator hamiltonian,
      final StateVectorPauliExpectationEstimator estimator,
      final String parameterPrefix,
      final int maxQubits) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("State-vector VQE Hamiltonian must not be null.");
    }
    if (!hamiltonian.hermitianWithinTolerance(1.0E-12)) {
      throw new IllegalArgumentException("State-vector VQE Hamiltonian must be Hermitian.");
    }
    if (estimator == null) {
      throw new IllegalArgumentException("State-vector VQE estimator must not be null.");
    }
    if (parameterPrefix == null || parameterPrefix.isBlank()) {
      throw new IllegalArgumentException("State-vector VQE parameter prefix must not be blank.");
    }
    if (maxQubits <= 0) {
      throw new IllegalArgumentException("State-vector VQE max qubits must be positive.");
    }
    this.hamiltonian = hamiltonian;
    this.estimator = estimator;
    this.parameterPrefix = parameterPrefix;
    this.maxQubits = maxQubits;
  }

  public VqeEnergyEvaluation evaluate(
      final QuantumProgram program,
      final double[] parameters,
      final int shots,
      final ErrorMitigationPlan mitigationPlan) {
    if (program == null) {
      return VqeEnergyEvaluation.failure("State-vector VQE program is null.");
    }
    if (parameters == null) {
      return VqeEnergyEvaluation.failure("State-vector VQE parameters are null.");
    }
    if (shots <= 0) {
      return VqeEnergyEvaluation.failure("State-vector VQE shot count must be positive.");
    }
    if (mitigationPlan == null) {
      return VqeEnergyEvaluation.failure("State-vector VQE mitigation plan is null.");
    }
    try {
      final ParameterBindings bindings = this.bindings(program, parameters);
      final double energy =
          this.estimator.estimate(
              program,
              this.hamiltonian,
              bindings,
              Math.max(
                  this.maxQubits, StateVectorPauliVqeEnergyBackend.programQubitCount(program)));
      return VqeEnergyEvaluation.success(energy, this.stateVectorUncertainty(shots), shots);
    } catch (final IllegalArgumentException exception) {
      return VqeEnergyEvaluation.failure(exception.getMessage());
    }
  }

  private ParameterBindings bindings(final QuantumProgram program, final double[] parameters) {
    final ParameterBindings.Builder builder = ParameterBindings.builder();
    for (int i = 0; i < parameters.length; ++i) {
      if (!Double.isFinite(parameters[i])) {
        throw new IllegalArgumentException("State-vector VQE parameter must be finite.");
      }
      builder.put(this.parameterPrefix + i, parameters[i]);
    }
    final String[] symbols = StateVectorPauliVqeEnergyBackend.parameterSymbols(program);
    for (int i = 0; i < symbols.length; ++i) {
      builder.put(symbols[i], parameters[i % parameters.length]);
    }
    return builder.build();
  }

  private double stateVectorUncertainty(final int shots) {
    return 1.0 / Math.sqrt((double) shots);
  }

  private static int programQubitCount(final QuantumProgram program) {
    int count = 0;
    for (int circuitIndex = 0; circuitIndex < program.circuitCount(); ++circuitIndex) {
      for (int registerIndex = 0;
          registerIndex < program.circuit(circuitIndex).quantumRegisterCount();
          ++registerIndex) {
        count += program.circuit(circuitIndex).quantumRegister(registerIndex).size();
      }
    }
    return Math.max(1, count);
  }

  private static String[] parameterSymbols(final QuantumProgram program) {
    final LinkedHashSet<String> symbols = new LinkedHashSet<String>();
    for (int circuitIndex = 0; circuitIndex < program.circuitCount(); ++circuitIndex) {
      for (int operationIndex = 0;
          operationIndex < program.circuit(circuitIndex).operationCount();
          ++operationIndex) {
        StateVectorPauliVqeEnergyBackend.collectSymbols(
            program.circuit(circuitIndex).operation(operationIndex), symbols);
      }
    }
    return symbols.toArray(String[]::new);
  }

  private static void collectSymbols(final Operation operation, final Set<String> symbols) {
    if (operation.kind() != OperationKind.GATE) {
      return;
    }
    final GateOperation gateOperation = (GateOperation) operation;
    for (int i = 0; i < gateOperation.parameterCount(); ++i) {
      StateVectorPauliVqeEnergyBackend.collectSymbols(gateOperation.parameter(i), symbols);
    }
  }

  private static void collectSymbols(
      final ParameterExpression expression, final Set<String> symbols) {
    if (expression.isNamed()) {
      symbols.add(expression.name());
    } else if (expression.isUnary()) {
      StateVectorPauliVqeEnergyBackend.collectSymbols(expression.left(), symbols);
    } else if (expression.isBinary()) {
      StateVectorPauliVqeEnergyBackend.collectSymbols(expression.left(), symbols);
      StateVectorPauliVqeEnergyBackend.collectSymbols(expression.right(), symbols);
    }
  }
}
