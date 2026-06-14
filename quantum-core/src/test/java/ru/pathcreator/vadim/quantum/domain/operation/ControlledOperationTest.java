/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControlledOperationTest {

    @Test
    void addsControlledOperationToCircuit() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("controlled");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        final ResetOperation reset = new ResetOperation(q.get(0));
        final ClassicalCondition condition = ClassicalCondition.equalTo(
            c,
            1
        );

        circuit.controlled(
            condition,
            reset
        );

        final ControlledOperation operation = assertInstanceOf(
            ControlledOperation.class,
            circuit.operation(0)
        );
        assertSame(
            condition,
            operation.condition()
        );
        assertSame(
            reset,
            operation.operation()
        );
        assertEquals(
            OperationKind.CONTROLLED,
            operation.kind()
        );
    }

    @Test
    void rejectsInvalidConditionValue() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("invalid");
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> ClassicalCondition.equalTo(
                c,
                -1
            )
        );
    }
}