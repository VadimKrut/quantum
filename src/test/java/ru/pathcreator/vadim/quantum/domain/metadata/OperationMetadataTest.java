/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.metadata;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationMetadataTest {

    @Test
    void attachesMetadataWithoutChangingOperationObject() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("metadata");
        final QuantumRegister register = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(register.get(0));
        final Operation operation = circuit.operation(0);
        final OperationMetadata metadata = new OperationMetadata(
            new ExternalSource(
                "fixture_source",
                "test fixture"
            ),
            new SourceLocation(
                7,
                3
            )
        );

        circuit.setOperationMetadata(
            0,
            metadata
        );

        assertSame(
            operation,
            circuit.operation(0)
        );
        assertSame(
            metadata,
            circuit.operationMetadata(0)
        );
        assertFalse(circuit.operationMetadata(0).isEmpty());
        assertEquals(
            7,
            circuit.operationMetadata(0).location().line()
        );
    }

    @Test
    void createsEmptyMetadataForNewOperations() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("empty_metadata");
        final QuantumRegister register = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.h(register.get(0));

        assertTrue(circuit.operationMetadata(0).isEmpty());
    }

    @Test
    void validatesSourceLocationAndExternalSource() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SourceLocation(
                0,
                1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExternalSource(
                " ",
                "blank format"
            )
        );
    }
}