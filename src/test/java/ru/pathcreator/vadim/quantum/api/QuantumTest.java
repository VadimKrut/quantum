/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumTest {

    @Test
    void exposesStableFacadeForCoreWorkflow() {
        final QuantumProgram program = Quantum.gateBasedProgram();
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
            );

        assertTrue(Quantum.validate(program).isValid());
        assertEquals(
            CapabilityPreflightStatus.EXPORTABLE,
            Quantum.preflight(
                IntegrationFormat.OPENQASM_3,
                program
            ).status()
        );

        final String json = Quantum.writeJson(program).content();
        final QuantumProgram restored = Quantum.readJson(json).program();

        assertTrue(Quantum.exportProgram(
            IntegrationFormat.OPENQASM_2,
            restored
        ).isSuccess());
    }
}