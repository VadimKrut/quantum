/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.coefficient.ComplexCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.PauliAxis;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliOperator;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.pauli.QubitPauliTerm;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

final class PauliExpectationMeasurementProgramGeneratorTest {

  @Test
  void generatorCreatesOneValidatedMeasurementCircuitForEachNonIdentityPauliTerm() {
    final QubitPauliOperator hamiltonian =
        QubitPauliOperator.builder()
            .add(QubitPauliTerm.IDENTITY, ComplexCoefficient.real(-0.125))
            .add(QubitPauliTerm.of(new int[] {0}, new PauliAxis[] {PauliAxis.X}), 0.5)
            .add(QubitPauliTerm.of(new int[] {1}, new PauliAxis[] {PauliAxis.Y}), -0.25)
            .add(
                QubitPauliTerm.of(new int[] {0, 2}, new PauliAxis[] {PauliAxis.Z, PauliAxis.X}),
                0.75)
            .build();

    final QuantumProgram program =
        new PauliExpectationMeasurementProgramGenerator().generate(hamiltonian, "energy", "q", "c");

    assertEquals(3, program.circuitCount());
    assertTrue(program.circuit(0).operationCount() >= 2L);
    assertTrue(program.circuit(1).operationCount() >= 2L);
    assertTrue(program.circuit(2).operationCount() >= 3L);
    assertTrue(Quantum.validate(program).isValid());
  }

  @Test
  void identityOnlyHamiltonianStillProducesValidatedTraceableCircuit() {
    final QuantumProgram program =
        new PauliExpectationMeasurementProgramGenerator()
            .generate(
                QubitPauliOperator.identity(ComplexCoefficient.real(1.25)),
                "identity_energy",
                "q",
                "c");

    assertEquals(1, program.circuitCount());
    assertEquals(1L, program.circuit(0).operationCount());
    assertTrue(Quantum.validate(program).isValid());
  }
}
