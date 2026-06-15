/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.resource;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceEstimatorTest {

    @Test
    void estimatesBellProgramResources() {
        final ResourceEstimate estimate = new ResourceEstimator().estimate(
            bellProgram(),
            20
        );

        assertEquals(
            1,
            estimate.circuitCount()
        );
        assertEquals(
            2,
            estimate.qubitCount()
        );
        assertEquals(
            64L,
            estimate.estimatedStateVectorBytes()
        );
        assertTrue(estimate.isLocalSimulationFeasible());
        assertEquals(
            1,
            estimate.circuits().get(0).twoQubitGateCount()
        );
        assertEquals(
            2,
            estimate.circuits().get(0).measurementCount()
        );
    }

    @Test
    void marksProgramAboveLocalSimulationLimit() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("wide");
        circuit.createQuantumRegister(
            "q",
            5
        );

        final ResourceEstimate estimate = new ResourceEstimator().estimate(
            program,
            4
        );

        assertFalse(estimate.isLocalSimulationFeasible());
        assertEquals(
            512L,
            estimate.estimatedStateVectorBytes()
        );
    }

    private static QuantumProgram bellProgram() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bell");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(0),
                c.get(0)
            )
            .measure(
                q.get(1),
                c.get(1)
            );
        return program;
    }
}