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
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GateOperationTest {

    @Test
    void createsNonParameterizedGateOperation() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );
        final GateOperation operation = GateOperation.of(
            StandardGate.CX,
            register.get(0),
            register.get(1)
        );

        assertEquals(
            OperationKind.GATE,
            operation.kind()
        );
        assertEquals(
            StandardGate.CX,
            operation.gate()
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
        assertEquals(
            0,
            operation.parameterCount()
        );
    }

    @Test
    void createsParameterizedGateOperation() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );
        final ParameterExpression theta = ParameterExpression.named("theta");
        final GateOperation operation = GateOperation.parameterized(
            StandardGate.RZ,
            new ParameterExpression[] {theta},
            register.get(0)
        );

        assertEquals(
            1,
            operation.qubitCount()
        );
        assertEquals(
            1,
            operation.parameterCount()
        );
        assertSame(
            theta,
            operation.parameter(0)
        );
    }

    @Test
    void rejectsWrongGateArity() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> GateOperation.of(
                StandardGate.CX,
                register.get(0)
            )
        );
    }

    @Test
    void rejectsWrongParameterCount() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> GateOperation.of(
                StandardGate.RZ,
                register.get(0)
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateOperation.parameterized(
                StandardGate.H,
                new ParameterExpression[] {ParameterExpression.of(1.0)},
                register.get(0)
            )
        );
    }

    @Test
    void rejectsNullGateQubitAndParameterValues() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> GateOperation.of(
                null,
                register.get(0)
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new GateOperation(
                StandardGate.H,
                null,
                new ParameterExpression[0]
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new GateOperation(
                StandardGate.H,
                new Qubit[] {register.get(0)},
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new GateOperation(
                StandardGate.H,
                new Qubit[] {null},
                new ParameterExpression[0]
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new GateOperation(
                StandardGate.RZ,
                new Qubit[] {register.get(0)},
                new ParameterExpression[] {null}
            )
        );
    }

    @Test
    void protectsInternalArraysFromMutation() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );
        final Qubit[] qubits = new Qubit[] {
            register.get(0),
            register.get(1)
        };
        final GateOperation operation = GateOperation.of(
            StandardGate.CX,
            qubits
        );

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
    void protectsParameterArrayFromMutation() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );
        final ParameterExpression theta = ParameterExpression.named("theta");
        final ParameterExpression alpha = ParameterExpression.named("alpha");
        final ParameterExpression[] parameters = new ParameterExpression[] {theta};
        final GateOperation operation = GateOperation.parameterized(
            StandardGate.RZ,
            parameters,
            register.get(0)
        );

        parameters[0] = alpha;
        final ParameterExpression[] returnedParameters = operation.parameters();
        returnedParameters[0] = alpha;

        assertSame(
            theta,
            operation.parameter(0)
        );
        assertNotSame(
            returnedParameters,
            operation.parameters()
        );
    }

    @Test
    void rejectsInvalidAccessIndexes() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            1
        );
        final GateOperation operation = GateOperation.parameterized(
            StandardGate.RZ,
            new ParameterExpression[] {ParameterExpression.named("theta")},
            register.get(0)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> operation.qubit(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> operation.qubit(1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> operation.parameter(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> operation.parameter(1)
        );
    }

    @Test
    void comparesByGateQubitsAndParameters() {
        final QuantumRegister register = QuantumRegister.create(
            "q",
            2
        );
        final ParameterExpression theta = ParameterExpression.named("theta");

        assertEquals(
            GateOperation.of(
                StandardGate.CX,
                register.get(0),
                register.get(1)
            ),
            GateOperation.of(
                StandardGate.CX,
                register.get(0),
                register.get(1)
            )
        );
        assertEquals(
            GateOperation.parameterized(
                StandardGate.RZ,
                new ParameterExpression[] {theta},
                register.get(0)
            ).hashCode(),
            GateOperation.parameterized(
                StandardGate.RZ,
                new ParameterExpression[] {theta},
                register.get(0)
            ).hashCode()
        );
        assertNotEquals(
            GateOperation.of(
                StandardGate.CX,
                register.get(0),
                register.get(1)
            ),
            GateOperation.of(
                StandardGate.CX,
                register.get(1),
                register.get(0)
            )
        );
        assertNotEquals(
            GateOperation.of(
                StandardGate.H,
                register.get(0)
            ),
            "h q[0]"
        );
    }
}