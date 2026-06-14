/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.composition;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitComposerTest {

    @Test
    void copiesCircuitRegistersAndOperationsWithoutRegisterAliasing() {
        final QuantumProgram sourceProgram = QuantumProgram.gateBased();
        final QuantumCircuit source = sourceProgram.createCircuit("source");
        final QuantumRegister q = source.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = source.createClassicalRegister(
            "c",
            2
        );
        source.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(1),
                c.get(1)
            );
        final QuantumProgram targetProgram = QuantumProgram.gateBased();

        final QuantumCircuit target = new CircuitComposer().copyCircuit(
            source,
            targetProgram,
            "target"
        );

        assertEquals(
            source.operationCount(),
            target.operationCount()
        );
        assertNotSame(
            source.quantumRegister(0),
            target.quantumRegister(0)
        );
        assertNotSame(
            source.classicalRegister(0),
            target.classicalRegister(0)
        );
        assertSame(
            targetProgram,
            target.program()
        );
        assertEquals(
            OperationKind.GATE,
            target.operation(0).kind()
        );
        assertSame(
            target.quantumRegister(0).get(0),
            ((GateOperation) target.operation(0)).qubit(0)
        );
    }

    @Test
    void appendsOperationsThroughExplicitRemap() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit source = program.createCircuit("source");
        final QuantumRegister sourceQ = source.createQuantumRegister(
            "q",
            1
        );
        source.h(sourceQ.get(0));
        final QuantumCircuit target = program.createCircuit("target");
        final QuantumRegister targetQ = target.createQuantumRegister(
            "mapped_q",
            1
        );
        final CircuitComposer.Remap remap = CircuitComposer.Remap.builder()
            .mapQuantumRegister(
                sourceQ,
                targetQ
            )
            .build();

        new CircuitComposer().appendOperations(
            source,
            target,
            remap
        );

        assertEquals(
            1,
            target.operationCount()
        );
        assertSame(
            targetQ.get(0),
            ((GateOperation) target.operation(0)).qubit(0)
        );
    }

    @Test
    void copiesClassicalAssignmentsAndPredicateControlsWithoutAliasing() {
        final QuantumProgram sourceProgram = QuantumProgram.gateBased();
        final QuantumCircuit source = sourceProgram.createCircuit("source_classical");
        final QuantumRegister q = source.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = source.createClassicalRegister(
            "c",
            1
        );
        source.assign(new ClassicalAssignment(
            ClassicalExpression.bit(c.get(0)),
            ClassicalExpression.integer(1)
        ))
            .classicallyControlled(
                ClassicalPredicate.compare(
                    ClassicalExpression.register(c),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(1)
                ),
                GateOperation.of(
                    StandardGate.X,
                    q.get(0)
                )
            );
        final QuantumProgram targetProgram = QuantumProgram.gateBased();

        final QuantumCircuit target = new CircuitComposer().copyCircuit(
            source,
            targetProgram,
            "target_classical"
        );

        final ClassicalAssignmentOperation assignmentOperation = (ClassicalAssignmentOperation) target.operation(0);
        final ClassicallyControlledOperation controlledOperation = (ClassicallyControlledOperation) target.operation(1);

        assertSame(
            target.classicalRegister(0).get(0),
            assignmentOperation.assignment().target().bit()
        );
        assertSame(
            target.classicalRegister(0),
            controlledOperation.predicate().leftExpression().register()
        );
        assertSame(
            target.quantumRegister(0).get(0),
            ((GateOperation) controlledOperation.operation()).qubit(0)
        );
    }

    @Test
    void refusesIncompleteRemap() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit source = program.createCircuit("source");
        final QuantumRegister sourceQ = source.createQuantumRegister(
            "q",
            1
        );
        source.h(sourceQ.get(0));
        final QuantumCircuit target = program.createCircuit("target");
        target.createQuantumRegister(
            "mapped_q",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new CircuitComposer().appendOperations(
                source,
                target,
                CircuitComposer.Remap.builder().build()
            )
        );
    }
}