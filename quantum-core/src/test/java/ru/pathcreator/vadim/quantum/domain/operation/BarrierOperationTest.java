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

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BarrierOperationTest {

    @Test
    void createsBarrierOperation() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );
        final BarrierOperation operation = new BarrierOperation(
            register.get(0),
            register.get(1)
        );

        assertEquals(
            OperationKind.BARRIER,
            operation.kind()
        );
        assertEquals(
            2,
            operation.qubitCount()
        );
        assertSame(
            register.get(0),
            operation.qubit(0)
        );
        assertSame(
            register.get(1),
            operation.qubit(1)
        );
    }

    @Test
    void protectsInternalArrayFromMutation() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );
        final Qubit[] qubits = new Qubit[] {
            register.get(0),
            register.get(1)
        };
        final BarrierOperation operation = new BarrierOperation(qubits);

        qubits[0] = register.get(1);
        final Qubit[] returnedQubits = operation.qubits();
        returnedQubits[0] = register.get(1);

        assertSame(
            register.get(0),
            operation.qubit(0)
        );
        assertNotSame(
            returnedQubits,
            operation.qubits()
        );
    }

    @Test
    void rejectsInvalidBarrierArguments() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new BarrierOperation((Qubit[]) null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BarrierOperation()
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BarrierOperation(
                register.get(0),
                null
            )
        );
    }

    @Test
    void rejectsInvalidQubitIndex() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );
        final BarrierOperation operation = new BarrierOperation(register.get(0));

        assertThrows(
            IllegalArgumentException.class,
            () -> operation.qubit(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> operation.qubit(1)
        );
    }

    @Test
    void comparesByQubitOrder() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );

        assertEquals(
            new BarrierOperation(
                register.get(0),
                register.get(1)
            ),
            new BarrierOperation(
                register.get(0),
                register.get(1)
            )
        );
        assertEquals(
            new BarrierOperation(
                register.get(0),
                register.get(1)
            ).hashCode(),
            new BarrierOperation(
                register.get(0),
                register.get(1)
            ).hashCode()
        );
        assertNotEquals(
            new BarrierOperation(
                register.get(0),
                register.get(1)
            ),
            new BarrierOperation(
                register.get(1),
                register.get(0)
            )
        );
        assertNotEquals(
            new BarrierOperation(register.get(0)),
            "barrier q[0]"
        );
    }
}