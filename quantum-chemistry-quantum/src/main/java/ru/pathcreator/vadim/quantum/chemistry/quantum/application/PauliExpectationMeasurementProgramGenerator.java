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
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/** Создает Quantum IR circuits для измерения Pauli terms при оценке энергии Hamiltonian. */
public final class PauliExpectationMeasurementProgramGenerator {

  private static final double HERMITIAN_TOLERANCE = 1.0E-10;

  public QuantumProgram generate(
      final QubitPauliOperator hamiltonian,
      final String circuitPrefix,
      final String quantumRegisterName,
      final String classicalRegisterName) {
    if (hamiltonian == null) {
      throw new IllegalArgumentException("Pauli measurement Hamiltonian must not be null.");
    }
    if (!hamiltonian.hermitianWithinTolerance(HERMITIAN_TOLERANCE)) {
      throw new IllegalArgumentException("Pauli measurement Hamiltonian must be Hermitian.");
    }
    final String checkedCircuitPrefix =
        circuitPrefix == null || circuitPrefix.isBlank() ? "pauli_measurement" : circuitPrefix;
    final String checkedQuantumRegisterName =
        quantumRegisterName == null || quantumRegisterName.isBlank() ? "q" : quantumRegisterName;
    final String checkedClassicalRegisterName =
        classicalRegisterName == null || classicalRegisterName.isBlank()
            ? "c"
            : classicalRegisterName;
    final int qubitCount = Math.max(1, hamiltonian.requiredQubitCount());
    final QuantumProgramBuilder programBuilder = Quantum.programBuilder();
    int circuitIndex = 0;
    for (int i = 0; i < hamiltonian.termCount(); ++i) {
      final QubitPauliTerm term = hamiltonian.term(i);
      if (term.identity()) {
        continue;
      }
      final ComplexCoefficient coefficient = hamiltonian.coefficient(i);
      if (!coefficient.effectivelyReal()) {
        throw new IllegalArgumentException("Pauli measurement term coefficient must be real.");
      }
      this.appendMeasurementCircuit(
          programBuilder,
          checkedCircuitPrefix + "_" + circuitIndex,
          checkedQuantumRegisterName,
          checkedClassicalRegisterName,
          qubitCount,
          term);
      ++circuitIndex;
    }
    if (circuitIndex == 0) {
      programBuilder
          .circuit(checkedCircuitPrefix + "_identity_only")
          .qreg(checkedQuantumRegisterName, qubitCount)
          .creg(checkedClassicalRegisterName, qubitCount)
          .barrier(checkedQuantumRegisterName + "[0]")
          .endCircuit();
    }
    return programBuilder.build();
  }

  private void appendMeasurementCircuit(
      final QuantumProgramBuilder programBuilder,
      final String circuitName,
      final String quantumRegisterName,
      final String classicalRegisterName,
      final int qubitCount,
      final QubitPauliTerm term) {
    final QuantumCircuitBuilder circuitBuilder =
        programBuilder
            .circuit(circuitName)
            .qreg(quantumRegisterName, qubitCount)
            .creg(classicalRegisterName, qubitCount);
    this.applyMeasurementBasis(circuitBuilder, term, quantumRegisterName);
    for (int i = 0; i < term.length(); ++i) {
      final int qubit = term.qubit(i);
      circuitBuilder.measure(
          PauliExpectationMeasurementProgramGenerator.reference(quantumRegisterName, qubit),
          PauliExpectationMeasurementProgramGenerator.reference(classicalRegisterName, qubit));
    }
    circuitBuilder.endCircuit();
  }

  private void applyMeasurementBasis(
      final QuantumCircuitBuilder circuitBuilder,
      final QubitPauliTerm term,
      final String quantumRegisterName) {
    for (int i = 0; i < term.length(); ++i) {
      final String qubit =
          PauliExpectationMeasurementProgramGenerator.reference(quantumRegisterName, term.qubit(i));
      final PauliAxis axis = term.axis(i);
      if (axis == PauliAxis.X) {
        circuitBuilder.h(qubit);
      } else if (axis == PauliAxis.Y) {
        circuitBuilder.rx(Math.PI / 2.0, qubit);
      }
    }
  }

  private static String reference(final String registerName, final int index) {
    return registerName + "[" + index + "]";
  }
}
