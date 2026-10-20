/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application.estimator;

import java.util.List;
import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnostic;
import ru.pathcreator.vadim.quantum.application.simulation.engine.QuantumSimulator;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings;

/**
 * Считает expectation value Pauli Hamiltonian на state-vector, полученном из Quantum IR программы.
 */
public final class StateVectorPauliExpectationEstimator {

  private static final double IMAGINARY_EXPECTATION_TOLERANCE = 1.0E-9;

  private final QuantumSimulator simulator;

  public StateVectorPauliExpectationEstimator() {
    this(new QuantumSimulator());
  }

  public StateVectorPauliExpectationEstimator(final QuantumSimulator simulator) {
    if (simulator == null) {
      throw new IllegalArgumentException(
          "State-vector Pauli estimator simulator must not be null.");
    }
    this.simulator = simulator;
  }

  public double estimate(final QuantumProgram program, final QubitPauliOperator operator) {
    return this.estimate(
        program,
        operator,
        ParameterBindings.empty(),
        Math.max(1, operator == null ? 1 : operator.requiredQubitCount()));
  }

  public double estimate(
      final QuantumProgram program,
      final QubitPauliOperator operator,
      final ParameterBindings parameterBindings,
      final int maxQubits) {
    if (program == null) {
      throw new IllegalArgumentException("Pauli expectation program must not be null.");
    }
    if (operator == null) {
      throw new IllegalArgumentException("Pauli expectation operator must not be null.");
    }
    if (parameterBindings == null) {
      throw new IllegalArgumentException("Pauli expectation parameter bindings must not be null.");
    }
    if (maxQubits <= 0) {
      throw new IllegalArgumentException("Pauli expectation max qubits must be positive.");
    }
    final SimulationResult result =
        this.simulator.simulate(
            program,
            SimulationOptions.builder()
                .shots(1)
                .maxQubits(maxQubits)
                .captureStateVector(true)
                .parameterBindings(parameterBindings)
                .build());
    if (!result.isSuccess()) {
      throw new IllegalArgumentException(
          "Pauli expectation simulation failed: "
              + StateVectorPauliExpectationEstimator.diagnostics(result.diagnostics()));
    }
    return this.expectation(result.stateVector(), result.qubitCount(), operator);
  }

  public double expectation(
      final List<StateVectorAmplitude> stateVector,
      final int qubitCount,
      final QubitPauliOperator operator) {
    if (stateVector == null || stateVector.isEmpty()) {
      throw new IllegalArgumentException("Pauli expectation state-vector must not be empty.");
    }
    if (qubitCount <= 0 || qubitCount > 30) {
      throw new IllegalArgumentException(
          "Pauli expectation qubit count is outside supported range.");
    }
    if (operator == null) {
      throw new IllegalArgumentException("Pauli expectation operator must not be null.");
    }
    if (operator.requiredQubitCount() > qubitCount) {
      throw new IllegalArgumentException(
          "Pauli operator requires more qubits than state-vector provides.");
    }
    final int size = 1 << qubitCount;
    final double[] real = new double[size];
    final double[] imaginary = new double[size];
    for (int i = 0; i < stateVector.size(); ++i) {
      final StateVectorAmplitude amplitude = stateVector.get(i);
      final int index =
          StateVectorPauliExpectationEstimator.basisIndex(amplitude.basisState(), qubitCount);
      real[index] = amplitude.real();
      imaginary[index] = amplitude.imaginary();
    }
    double expectationReal = 0.0;
    double expectationImaginary = 0.0;
    for (int i = 0; i < operator.termCount(); ++i) {
      final TermExpectation termExpectation =
          StateVectorPauliExpectationEstimator.termExpectation(
              real, imaginary, qubitCount, operator.term(i));
      final ComplexCoefficient coefficient = operator.coefficient(i);
      expectationReal +=
          coefficient.real() * termExpectation.real
              - coefficient.imaginary() * termExpectation.imaginary;
      expectationImaginary +=
          coefficient.real() * termExpectation.imaginary
              + coefficient.imaginary() * termExpectation.real;
    }
    if (Math.abs(expectationImaginary) > IMAGINARY_EXPECTATION_TOLERANCE) {
      throw new IllegalArgumentException(
          "Pauli expectation has non-real value: " + expectationImaginary + "i.");
    }
    return expectationReal;
  }

  private static TermExpectation termExpectation(
      final double[] real,
      final double[] imaginary,
      final int qubitCount,
      final QubitPauliTerm term) {
    double sumReal = 0.0;
    double sumImaginary = 0.0;
    for (int basis = 0; basis < real.length; ++basis) {
      int mappedBasis = basis;
      double phaseReal = 1.0;
      double phaseImaginary = 0.0;
      for (int i = 0; i < term.length(); ++i) {
        final int bitMask =
            1 << StateVectorPauliExpectationEstimator.bitPosition(qubitCount, term.qubit(i));
        final boolean one = (basis & bitMask) != 0;
        final PauliAxis axis = term.axis(i);
        if (axis == PauliAxis.X) {
          mappedBasis ^= bitMask;
        } else if (axis == PauliAxis.Y) {
          mappedBasis ^= bitMask;
          final double nextReal = one ? -phaseImaginary : phaseImaginary;
          final double nextImaginary = one ? phaseReal : -phaseReal;
          phaseReal = nextReal;
          phaseImaginary = nextImaginary;
        } else if (axis == PauliAxis.Z && one) {
          phaseReal = -phaseReal;
          phaseImaginary = -phaseImaginary;
        }
      }
      final double ketReal = real[mappedBasis];
      final double ketImaginary = imaginary[mappedBasis];
      final double operatedReal = phaseReal * ketReal - phaseImaginary * ketImaginary;
      final double operatedImaginary = phaseReal * ketImaginary + phaseImaginary * ketReal;
      sumReal += real[basis] * operatedReal + imaginary[basis] * operatedImaginary;
      sumImaginary += real[basis] * operatedImaginary - imaginary[basis] * operatedReal;
    }
    return new TermExpectation(sumReal, sumImaginary);
  }

  private static int basisIndex(final String basisState, final int qubitCount) {
    if (basisState == null || basisState.length() != qubitCount) {
      throw new IllegalArgumentException("State-vector basis label does not match qubit count.");
    }
    int index = 0;
    for (int i = 0; i < basisState.length(); ++i) {
      final char bit = basisState.charAt(i);
      if (bit == '1') {
        index |= 1 << (basisState.length() - 1 - i);
      } else if (bit != '0') {
        throw new IllegalArgumentException(
            "State-vector basis label contains non-binary character.");
      }
    }
    return index;
  }

  private static int bitPosition(final int qubitCount, final int qubit) {
    if (qubit < 0 || qubit >= qubitCount) {
      throw new IllegalArgumentException("Pauli qubit index is outside state-vector.");
    }
    return qubit;
  }

  private static String diagnostics(final List<SimulationDiagnostic> diagnostics) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < diagnostics.size(); ++i) {
      if (i > 0) {
        builder.append("; ");
      }
      final SimulationDiagnostic diagnostic = diagnostics.get(i);
      builder
          .append(diagnostic.severity())
          .append(' ')
          .append(diagnostic.code())
          .append(" circuit=")
          .append(diagnostic.circuitIndex())
          .append(" operation=")
          .append(diagnostic.operationIndex())
          .append(": ")
          .append(diagnostic.message());
    }
    return builder.toString();
  }

  private static final class TermExpectation {

    private final double real;
    private final double imaginary;

    private TermExpectation(final double real, final double imaginary) {
      this.real = real;
      this.imaginary = imaginary;
    }
  }
}
