/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.visualization;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumProgramTimelineBuilderTest {

    @Test
    void buildsTimelineForStaticBellCircuit() {
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
            );

        final ProgramTimeline timeline = new QuantumProgramTimelineBuilder().build(program);
        final CircuitTimeline bell = timeline.circuits().get(0);

        assertEquals(
            "bell",
            bell.circuitName()
        );
        assertEquals(
            2,
            bell.quantumWires().size()
        );
        assertEquals(
            2,
            bell.classicalWires().size()
        );
        assertEquals(
            3,
            bell.steps().size()
        );
        assertEquals(
            "cx",
            bell.steps().get(1).label()
        );
        assertTrue(bell.steps().get(2).classicalWires().contains("c[0]"));
    }
}