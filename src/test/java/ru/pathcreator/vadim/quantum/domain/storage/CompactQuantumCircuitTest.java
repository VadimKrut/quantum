/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.metadata.ExternalSource;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.metadata.SourceLocation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

class CompactQuantumCircuitTest {

    @Test
    void compactCircuitRestoresStaticGateBasedOperations() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.h(q.get(0));
        circuit.rz(
            ParameterExpression.pi(),
            q.get(0)
        );
        circuit.cx(
            q.get(0),
            q.get(1)
        );
        circuit.barrier(
            q.get(0),
            q.get(1)
        );
        circuit.reset(q.get(1));
        circuit.measure(
            q.get(0),
            c.get(0)
        );
        circuit.setOperationMetadata(
            1,
            new OperationMetadata(
                new ExternalSource(
                    "test",
                    "compact"
                ),
                new SourceLocation(
                    7,
                    3
                )
            )
        );

        final CompactQuantumCircuit compact = CompactQuantumCircuit.from(circuit);

        assertEquals(
            circuit.operationCount(),
            compact.operationCount()
        );
        assertEquals(
            circuit.operationCount(),
            compact.compactOperationCount()
        );
        assertEquals(
            0,
            compact.fallbackOperationCount()
        );
        for (int i = 0; i < circuit.operationCount(); i++) {
            assertEquals(
                circuit.operation(i),
                compact.operation(i)
            );
            assertEquals(
                circuit.operationMetadata(i),
                compact.operationMetadata(i)
            );
        }
    }

    @Test
    void compactCircuitMaterializesIntoEquivalentCircuit() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.h(q.get(0));
        circuit.cx(
            q.get(0),
            q.get(1)
        );
        circuit.measure(
            q.get(1),
            c.get(0)
        );

        final QuantumProgram restoredProgram = QuantumProgram.gateBased();
        final QuantumCircuit restored = CompactQuantumCircuit.from(circuit).toCircuit(restoredProgram);

        assertEquals(
            circuit.operationCount(),
            restored.operationCount()
        );
        final GateOperation restoredGate = assertInstanceOf(
            GateOperation.class,
            restored.operation(1)
        );
        assertEquals(
            q.get(1).index(),
            restoredGate.qubit(1).index()
        );
        assertEquals(
            "q",
            restoredGate.qubit(1).register().name().value()
        );
    }

    @Test
    void compactCircuitKeepsUnsupportedOperationAsFallback() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        final ClassicalPredicate predicate = ClassicalPredicate.compare(
            ClassicalExpression.bit(c.get(0)),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(1)
        );
        final ClassicallyControlledOperation operation = new ClassicallyControlledOperation(
            predicate,
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            )
        );
        circuit.classicallyControlled(
            operation.predicate(),
            operation.operation()
        );

        final CompactQuantumCircuit compact = CompactQuantumCircuit.from(circuit);

        assertEquals(
            1,
            compact.operationCount()
        );
        assertEquals(
            0,
            compact.compactOperationCount()
        );
        assertEquals(
            1,
            compact.fallbackOperationCount()
        );
        assertSame(
            circuit.operation(0),
            compact.operation(0)
        );
        assertFalse(compact.operation(0) instanceof GateOperation);
    }
}