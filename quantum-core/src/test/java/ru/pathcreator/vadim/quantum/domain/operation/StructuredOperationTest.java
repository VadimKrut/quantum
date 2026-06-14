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

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredOperationTest {

    @Test
    void createsStructuredOperationsAndValidatesNestedOwnership() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("structured");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
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
        final OperationBlock thenBlock = OperationBlock.of(GateOperation.of(
            StandardGate.X,
            q.get(0)
        ));
        final OperationBlock elseBlock = OperationBlock.of(new ResetOperation(q.get(1)));

        circuit.block(OperationBlock.of(GateOperation.of(
            StandardGate.H,
            q.get(0)
        )))
            .conditionalBlock(
                predicate,
                thenBlock,
                elseBlock
            )
            .forLoop(
                "i",
                0,
                1,
                3,
                OperationBlock.of(GateOperation.of(
                    StandardGate.Z,
                    q.get(0)
                ))
            )
            .whileLoop(
                predicate,
                OperationBlock.of(new BarrierOperation(
                    q.get(0),
                    q.get(1)
                ))
            )
            .delay(
                DurationExpression.duration(
                    20,
                    DurationUnit.NS
                ),
                q.get(0)
            )
            .timingBox(
                DurationExpression.stretch("duration_symbol"),
                OperationBlock.of(GateOperation.of(
                    StandardGate.Y,
                    q.get(1)
                ))
            );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertTrue(result.isValid());
        assertEquals(
            6,
            circuit.operationCount()
        );
    }

    @Test
    void rejectsBlockOperationWithForeignQubitAtCircuitBoundary() {
        final QuantumProgram firstProgram = QuantumProgram.gateBased();
        final QuantumCircuit firstCircuit = firstProgram.createCircuit("first");
        final QuantumRegister firstRegister = firstCircuit.createQuantumRegister(
            "a",
            1
        );
        final QuantumProgram secondProgram = QuantumProgram.gateBased();
        final QuantumCircuit secondCircuit = secondProgram.createCircuit("second");
        secondCircuit.createQuantumRegister(
            "b",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> secondCircuit.block(OperationBlock.of(GateOperation.of(
                StandardGate.H,
                firstRegister.get(0)
            )))
        );
    }

    @Test
    void rejectsInvalidLoopStepAndDuration() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ForLoopOperation(
                "i",
                0,
                0,
                1,
                OperationBlock.of()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> DurationExpression.duration(
                -1,
                DurationUnit.NS
            )
        );
    }
}