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
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeasureOperationTest {

    @Test
    void createsMeasureOperation() {
        final QuantumRegister quantumRegister = QuantumRegister.create(
            "q",
            1
        );
        final ClassicalRegister classicalRegister = ClassicalRegister.create(
            "c",
            1
        );
        final MeasureOperation operation = new MeasureOperation(
            quantumRegister.get(0),
            classicalRegister.get(0)
        );

        assertEquals(
            OperationKind.MEASURE,
            operation.kind()
        );
        assertSame(
            quantumRegister.get(0),
            operation.qubit()
        );
        assertSame(
            classicalRegister.get(0),
            operation.bit()
        );
    }

    @Test
    void rejectsNullMeasureArguments() {
        final QuantumRegister quantumRegister = QuantumRegister.create(
            "q",
            1
        );
        final ClassicalRegister classicalRegister = ClassicalRegister.create(
            "c",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new MeasureOperation(
                (Qubit) null,
                classicalRegister.get(0)
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new MeasureOperation(
                quantumRegister.get(0),
                null
            )
        );
    }

    @Test
    void comparesByMeasuredQubitAndClassicalBit() {
        final QuantumRegister quantumRegister = QuantumRegister.create(
            "q",
            2
        );
        final ClassicalRegister classicalRegister = ClassicalRegister.create(
            "c",
            2
        );

        assertEquals(
            new MeasureOperation(
                quantumRegister.get(0),
                classicalRegister.get(0)
            ),
            new MeasureOperation(
                quantumRegister.get(0),
                classicalRegister.get(0)
            )
        );
        assertEquals(
            new MeasureOperation(
                quantumRegister.get(0),
                classicalRegister.get(0)
            ).hashCode(),
            new MeasureOperation(
                quantumRegister.get(0),
                classicalRegister.get(0)
            ).hashCode()
        );
        assertNotEquals(
            new MeasureOperation(
                quantumRegister.get(0),
                classicalRegister.get(0)
            ),
            new MeasureOperation(
                quantumRegister.get(1),
                classicalRegister.get(0)
            )
        );
        assertNotEquals(
            new MeasureOperation(
                quantumRegister.get(0),
                classicalRegister.get(0)
            ),
            "measure q[0] -> c[0]"
        );
    }
}