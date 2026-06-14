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

import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResetOperationTest {

    @Test
    void createsResetOperation() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );
        final ResetOperation operation = new ResetOperation(register.get(0));

        assertEquals(
            OperationKind.RESET,
            operation.kind()
        );
        assertSame(
            register.get(0),
            operation.qubit()
        );
    }

    @Test
    void rejectsNullResetQubit() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ResetOperation(null)
        );
    }

    @Test
    void comparesByResetQubit() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );

        assertEquals(
            new ResetOperation(register.get(0)),
            new ResetOperation(register.get(0))
        );
        assertEquals(
            new ResetOperation(register.get(0)).hashCode(),
            new ResetOperation(register.get(0)).hashCode()
        );
        assertNotEquals(
            new ResetOperation(register.get(0)),
            new ResetOperation(register.get(1))
        );
        assertNotEquals(
            new ResetOperation(register.get(0)),
            "reset q[0]"
        );
    }
}