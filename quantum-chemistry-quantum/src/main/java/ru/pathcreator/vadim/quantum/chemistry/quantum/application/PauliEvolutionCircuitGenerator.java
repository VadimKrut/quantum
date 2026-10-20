/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.api.QuantumCircuitBuilder;
import ru.pathcreator.vadim.quantum.api.QuantumProgramBuilder;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.chemistry.quantum.domain.circuit.PauliEvolutionOptions;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/** Строит gate-based Quantum IR circuit для first-order Trotter Pauli evolution. */
public final class PauliEvolutionCircuitGenerator {

  private static final double HERMITIAN_TOLERANCE = 1.0E-10;

  public QuantumProgram generate(
      final QubitPauliOperator hamiltonian,
      final double evolutionTime,
      final PauliEvolutionOptions options) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Pauli Hamiltonian must not be null.");
    }
    if (!Double.isFinite(evolutionTime)) {
      throw new IllegalArgumentException("Pauli evolution time must be finite.");
    }
    final PauliEvolutionOptions checkedOptions =
        options == null ? PauliEvolutionOptions.defaults() : options;
    if (!hamiltonian.hermitianWithinTolerance(HERMITIAN_TOLERANCE)) {
      throw new IllegalArgumentException(
          "Pauli Hamiltonian must be Hermitian for real-time evolution.");
    }
    final int qubitCount = Math.max(1, hamiltonian.requiredQubitCount());
    final QuantumProgramBuilder programBuilder = Quantum.programBuilder();
    final QuantumCircuitBuilder circuitBuilder =
        programBuilder
            .circuit(checkedOptions.circuitName())
            .qreg(checkedOptions.quantumRegisterName(), qubitCount)
            .creg(checkedOptions.classicalRegisterName(), qubitCount);
    final double stepTime = evolutionTime / (double) checkedOptions.trotterSteps();
    for (int step = 0; step < checkedOptions.trotterSteps(); ++step) {
      this.appendTrotterStep(circuitBuilder, hamiltonian, checkedOptions, stepTime);
    }
    if (checkedOptions.includeMeasurements()) {
      for (int i = 0; i < qubitCount; ++i) {
        circuitBuilder.measure(
            checkedOptions.quantumRegisterName() + "[" + i + "]",
            checkedOptions.classicalRegisterName() + "[" + i + "]");
      }
    }
    return circuitBuilder.endCircuit().build();
  }

  public void appendParameterizedEvolution(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliOperator hamiltonian,
      final ParameterExpression evolutionParameter,
      final String quantumRegisterName) {
    if (circuitBuilder == null) {
      throw new IllegalArgumentException("Pauli evolution circuit builder must not be null.");
    }
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Pauli Hamiltonian must not be null.");
    }
    if (evolutionParameter == null) {
      throw new IllegalArgumentException("Pauli evolution parameter must not be null.");
    }
    if (quantumRegisterName == null || quantumRegisterName.isBlank()) {
      throw new IllegalArgumentException(
          "Pauli evolution quantum register name must not be blank.");
    }
    if (!hamiltonian.hermitianWithinTolerance(HERMITIAN_TOLERANCE)) {
      throw new IllegalArgumentException(
          "Pauli Hamiltonian must be Hermitian for parameterized evolution.");
    }
    for (int i = 0; i < hamiltonian.termCount(); ++i) {
      final QubitPauliTerm term = hamiltonian.term(i);
      if (term.identity()) {
        continue;
      }
      final ComplexCoefficient coefficient = hamiltonian.coefficient(i);
      if (!coefficient.effectivelyReal()) {
        throw new IllegalArgumentException("Pauli evolution term coefficient must be real.");
      }
      this.appendPauliTermEvolution(
          circuitBuilder,
          term,
          ParameterExpression.multiply(
              ParameterExpression.of(2.0 * coefficient.real()), evolutionParameter),
          quantumRegisterName);
    }
  }

  private void appendTrotterStep(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliOperator hamiltonian,
      final PauliEvolutionOptions options,
      final double stepTime) {
    for (int i = 0; i < hamiltonian.termCount(); ++i) {
      final QubitPauliTerm term = hamiltonian.term(i);
      if (term.identity()) {
        continue;
      }
      final ComplexCoefficient coefficient = hamiltonian.coefficient(i);
      if (!coefficient.effectivelyReal()) {
        throw new IllegalArgumentException("Pauli evolution term coefficient must be real.");
      }
      this.appendPauliTermEvolution(
          circuitBuilder, term, 2.0 * coefficient.real() * stepTime, options.quantumRegisterName());
    }
  }

  private void appendPauliTermEvolution(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliTerm term,
      final double rzAngle,
      final String quantumRegisterName) {
    this.applyBasisChange(circuitBuilder, term, quantumRegisterName);
    for (int i = 0; i < term.length() - 1; ++i) {
      circuitBuilder.cx(
          this.qubit(quantumRegisterName, term.qubit(i)),
          this.qubit(quantumRegisterName, term.qubit(i + 1)));
    }
    circuitBuilder.rz(rzAngle, this.qubit(quantumRegisterName, term.qubit(term.length() - 1)));
    for (int i = term.length() - 2; i >= 0; --i) {
      circuitBuilder.cx(
          this.qubit(quantumRegisterName, term.qubit(i)),
          this.qubit(quantumRegisterName, term.qubit(i + 1)));
    }
    this.undoBasisChange(circuitBuilder, term, quantumRegisterName);
  }

  private void appendPauliTermEvolution(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliTerm term,
      final ParameterExpression rzAngle,
      final String quantumRegisterName) {
    this.applyBasisChange(circuitBuilder, term, quantumRegisterName);
    for (int i = 0; i < term.length() - 1; ++i) {
      circuitBuilder.cx(
          this.qubit(quantumRegisterName, term.qubit(i)),
          this.qubit(quantumRegisterName, term.qubit(i + 1)));
    }
    circuitBuilder.rz(rzAngle, this.qubit(quantumRegisterName, term.qubit(term.length() - 1)));
    for (int i = term.length() - 2; i >= 0; --i) {
      circuitBuilder.cx(
          this.qubit(quantumRegisterName, term.qubit(i)),
          this.qubit(quantumRegisterName, term.qubit(i + 1)));
    }
    this.undoBasisChange(circuitBuilder, term, quantumRegisterName);
  }

  private void applyBasisChange(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliTerm term,
      final String quantumRegisterName) {
    for (int i = 0; i < term.length(); ++i) {
      final String qubit = this.qubit(quantumRegisterName, term.qubit(i));
      if (term.axis(i) == PauliAxis.X) {
        circuitBuilder.h(qubit);
      } else if (term.axis(i) == PauliAxis.Y) {
        circuitBuilder.rx(Math.PI / 2.0, qubit);
      }
    }
  }

  private void undoBasisChange(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliTerm term,
      final String quantumRegisterName) {
    for (int i = term.length() - 1; i >= 0; --i) {
      final String qubit = this.qubit(quantumRegisterName, term.qubit(i));
      if (term.axis(i) == PauliAxis.X) {
        circuitBuilder.h(qubit);
      } else if (term.axis(i) == PauliAxis.Y) {
        circuitBuilder.rx(-Math.PI / 2.0, qubit);
      }
    }
  }

  private String qubit(final String quantumRegisterName, final int index) {
    return quantumRegisterName + "[" + index + "]";
  }
}
