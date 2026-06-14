/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalAssignmentOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuantumCircuitTest {

    @Test
    void buildsBellCircuitIrShape() {
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

        final QuantumCircuit returnedCircuit = circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(0),
                c.get(0)
            )
            .measure(
                q.get(1),
                c.get(1)
            );

        assertSame(
            circuit,
            returnedCircuit
        );
        assertEquals(
            1,
            circuit.quantumRegisterCount()
        );
        assertEquals(
            1,
            circuit.classicalRegisterCount()
        );
        assertEquals(
            4,
            circuit.operationCount()
        );

        final GateOperation h = assertInstanceOf(
            GateOperation.class,
            circuit.operation(0)
        );
        final GateOperation cx = assertInstanceOf(
            GateOperation.class,
            circuit.operation(1)
        );
        final MeasureOperation firstMeasure = assertInstanceOf(
            MeasureOperation.class,
            circuit.operation(2)
        );
        final MeasureOperation secondMeasure = assertInstanceOf(
            MeasureOperation.class,
            circuit.operation(3)
        );

        assertEquals(
            StandardGate.H,
            h.gate()
        );
        assertSame(
            q.get(0),
            h.qubit(0)
        );
        assertEquals(
            StandardGate.CX,
            cx.gate()
        );
        assertSame(
            q.get(0),
            cx.qubit(0)
        );
        assertSame(
            q.get(1),
            cx.qubit(1)
        );
        assertSame(
            q.get(0),
            firstMeasure.qubit()
        );
        assertSame(
            c.get(0),
            firstMeasure.bit()
        );
        assertSame(
            q.get(1),
            secondMeasure.qubit()
        );
        assertSame(
            c.get(1),
            secondMeasure.bit()
        );
    }

    @Test
    void appendsParameterizedResetAndBarrierOperations() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("ops");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ParameterExpression theta = ParameterExpression.named("theta");

        circuit.rz(
            theta,
            q.get(0)
        )
            .reset(q.get(0))
            .barrier(
                q.get(0),
                q.get(1)
            );

        final GateOperation rz = assertInstanceOf(
            GateOperation.class,
            circuit.operation(0)
        );
        final ResetOperation reset = assertInstanceOf(
            ResetOperation.class,
            circuit.operation(1)
        );
        final BarrierOperation barrier = assertInstanceOf(
            BarrierOperation.class,
            circuit.operation(2)
        );

        assertEquals(
            StandardGate.RZ,
            rz.gate()
        );
        assertSame(
            theta,
            rz.parameter(0)
        );
        assertSame(
            q.get(0),
            reset.qubit()
        );
        assertEquals(
            2,
            barrier.qubitCount()
        );
    }

    @Test
    void appendsClassicalAssignmentAndPredicateControlOperations() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("classical_ops");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        final ClassicalAssignment assignment = new ClassicalAssignment(
            ClassicalExpression.bit(c.get(0)),
            ClassicalExpression.integer(1)
        );
        final ClassicalPredicate predicate = ClassicalPredicate.compare(
            ClassicalExpression.register(c),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(1)
        );

        circuit.assign(assignment)
            .classicallyControlled(
                predicate,
                GateOperation.of(
                    StandardGate.X,
                    q.get(0)
                )
            );

        final ClassicalAssignmentOperation assignmentOperation = assertInstanceOf(
            ClassicalAssignmentOperation.class,
            circuit.operation(0)
        );
        final ClassicallyControlledOperation controlledOperation = assertInstanceOf(
            ClassicallyControlledOperation.class,
            circuit.operation(1)
        );

        assertSame(
            assignment,
            assignmentOperation.assignment()
        );
        assertSame(
            predicate,
            controlledOperation.predicate()
        );
    }

    @Test
    void exposesFluentApiForAllStandardGates() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("standard_gates");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        final ParameterExpression theta = ParameterExpression.named("theta");

        circuit.h(q.get(0))
            .x(q.get(0))
            .y(q.get(0))
            .z(q.get(0))
            .s(q.get(0))
            .sdg(q.get(0))
            .t(q.get(0))
            .tdg(q.get(0))
            .rx(
                theta,
                q.get(0)
            )
            .ry(
                theta,
                q.get(0)
            )
            .rz(
                theta,
                q.get(0)
            )
            .cx(
                q.get(0),
                q.get(1)
            )
            .cy(
                q.get(0),
                q.get(1)
            )
            .cz(
                q.get(0),
                q.get(1)
            )
            .ch(
                q.get(0),
                q.get(1)
            )
            .swap(
                q.get(0),
                q.get(1)
            )
            .ccx(
                q.get(0),
                q.get(1),
                q.get(2)
            )
            .phase(
                theta,
                q.get(0)
            )
            .id(q.get(0));

        assertGate(
            circuit,
            0,
            StandardGate.H
        );
        assertGate(
            circuit,
            1,
            StandardGate.X
        );
        assertGate(
            circuit,
            2,
            StandardGate.Y
        );
        assertGate(
            circuit,
            3,
            StandardGate.Z
        );
        assertGate(
            circuit,
            4,
            StandardGate.S
        );
        assertGate(
            circuit,
            5,
            StandardGate.SDG
        );
        assertGate(
            circuit,
            6,
            StandardGate.T
        );
        assertGate(
            circuit,
            7,
            StandardGate.TDG
        );
        assertParameterizedGate(
            circuit,
            8,
            StandardGate.RX,
            theta
        );
        assertParameterizedGate(
            circuit,
            9,
            StandardGate.RY,
            theta
        );
        assertParameterizedGate(
            circuit,
            10,
            StandardGate.RZ,
            theta
        );
        assertGate(
            circuit,
            11,
            StandardGate.CX
        );
        assertGate(
            circuit,
            12,
            StandardGate.CY
        );
        assertGate(
            circuit,
            13,
            StandardGate.CZ
        );
        assertGate(
            circuit,
            14,
            StandardGate.CH
        );
        assertGate(
            circuit,
            15,
            StandardGate.SWAP
        );
        assertGate(
            circuit,
            16,
            StandardGate.CCX
        );
        assertParameterizedGate(
            circuit,
            17,
            StandardGate.PHASE,
            theta
        );
        assertGate(
            circuit,
            18,
            StandardGate.ID
        );
        assertEquals(
            19,
            circuit.operationCount()
        );
    }

    @Test
    void appendsOpenGateDefinitionsThroughPublicApi() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("open_gates");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ParameterExpression alpha = ParameterExpression.named("alpha");
        final ParameterExpression beta = ParameterExpression.named("beta");
        final GateDefinition customGate = GateDefinition.of(
            "custom_u",
            2,
            2
        );

        circuit.parameterizedGate(
            customGate,
            new ParameterExpression[] {
                alpha,
                beta
            },
            q.get(0),
            q.get(1)
        );

        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            circuit.operation(0)
        );

        assertEquals(
            customGate,
            operation.gate()
        );
        assertSame(
            alpha,
            operation.parameter(0)
        );
        assertSame(
            beta,
            operation.parameter(1)
        );
        assertSame(
            q.get(0),
            operation.qubit(0)
        );
        assertSame(
            q.get(1),
            operation.qubit(1)
        );
    }

    @Test
    void rejectsDuplicateRegisterNamesAcrossQuantumAndClassicalRegisters() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("duplicates");

        circuit.createQuantumRegister(
            "q",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.createQuantumRegister(
                "q",
                1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.createClassicalRegister(
                "q",
                1
            )
        );
    }

    @Test
    void rejectsOperationsWithForeignBits() {
        final QuantumCircuit leftCircuit = QuantumProgram.gateBased().createCircuit("left");
        final QuantumCircuit rightCircuit = QuantumProgram.gateBased().createCircuit("right");
        final QuantumRegister leftQ = leftCircuit.createQuantumRegister(
            "q",
            1
        );
        final QuantumRegister rightQ = rightCircuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister rightC = rightCircuit.createClassicalRegister(
            "c",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> leftCircuit.h(rightQ.get(0))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> leftCircuit.measure(
                leftQ.get(0),
                rightC.get(0)
            )
        );
    }

    @Test
    void exposesImmutableSnapshots() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("snapshots");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.x(q.get(0));

        final List<QuantumRegister> quantumRegisters = circuit.quantumRegisters();
        final List<Operation> operations = circuit.operations();

        circuit.createQuantumRegister(
            "extra_q",
            1
        );
        assertEquals(
            1,
            quantumRegisters.size()
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> quantumRegisters.add(q)
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> operations.add(circuit.operation(0))
        );
    }

    @Test
    void rejectsInvalidIndexes() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("indexes");

        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.quantumRegister(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.quantumRegister(0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.classicalRegister(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.classicalRegister(0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.operation(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.operation(0)
        );
    }

    @Test
    void rejectsNullCustomGateArgumentsThroughCircuit() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("custom_invalid");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "custom",
            1,
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.gate(
                null,
                q.get(0)
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.parameterizedGate(
                customGate,
                null,
                q.get(0)
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.parameterizedGate(
                customGate,
                new ParameterExpression[] {null},
                q.get(0)
            )
        );
    }

    @Test
    void rejectsNullAndEmptyBarrierArgumentsThroughCircuit() {
        final QuantumCircuit circuit = QuantumProgram.gateBased().createCircuit("barriers");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.barrier((Qubit[]) null)
        );
        assertThrows(
            IllegalArgumentException.class,
            circuit::barrier
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.barrier(
                q.get(0),
                null
            )
        );
    }

    private static void assertGate(
        final QuantumCircuit circuit,
        final int operationIndex,
        final StandardGate expectedGate
    ) {
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            circuit.operation(operationIndex)
        );

        assertEquals(
            expectedGate,
            operation.gate()
        );
        assertEquals(
            expectedGate.arity(),
            operation.qubitCount()
        );
        assertEquals(
            expectedGate.parameterCount(),
            operation.parameterCount()
        );
    }

    private static void assertParameterizedGate(
        final QuantumCircuit circuit,
        final int operationIndex,
        final StandardGate expectedGate,
        final ParameterExpression expectedParameter
    ) {
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            circuit.operation(operationIndex)
        );

        assertEquals(
            expectedGate,
            operation.gate()
        );
        assertSame(
            expectedParameter,
            operation.parameter(0)
        );
    }
}