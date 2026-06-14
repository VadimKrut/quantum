/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuantumProgramTest {

    @Test
    void createsGateBasedProgram() {
        final QuantumProgram program = QuantumProgram.gateBased();

        assertEquals(
            QuantumComputationModel.GATE_BASED_CIRCUIT,
            program.computationModel()
        );
        assertEquals(
            0,
            program.circuitCount()
        );
    }

    @Test
    void createsCircuitInsideProgram() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bell");

        assertEquals(
            1,
            program.circuitCount()
        );
        assertSame(
            circuit,
            program.circuit(0)
        );
        assertSame(
            program,
            circuit.program()
        );
        assertEquals(
            CircuitName.of("bell"),
            circuit.name()
        );
    }

    @Test
    void exposesImmutableCircuitSnapshots() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit firstCircuit = program.createCircuit("first");
        final List<QuantumCircuit> circuits = program.circuits();

        program.createCircuit("second");

        assertEquals(
            1,
            circuits.size()
        );
        assertSame(
            firstCircuit,
            circuits.get(0)
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> circuits.add(firstCircuit)
        );
        assertEquals(
            2,
            program.circuitCount()
        );
    }

    @Test
    void rejectsInvalidProgramModelAndCircuitAccess() {
        assertThrows(
            IllegalArgumentException.class,
            () -> QuantumProgram.create(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> QuantumProgram.gateBased().circuit(0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> QuantumProgram.gateBased().circuit(-1)
        );
    }

    @Test
    void rejectsCircuitCreationForUnsupportedModel() {
        final QuantumProgram program = QuantumProgram.create(QuantumComputationModel.PULSE_LEVEL);

        assertThrows(
            IllegalStateException.class,
            () -> program.createCircuit("pulse")
        );
    }
}