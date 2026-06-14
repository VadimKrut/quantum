/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.composition;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositeGateInlinerTest {

    @Test
    void expandsCompositeGateIntoTargetCircuitOperations() {
        final GateDefinition gate = GateDefinition.composite(
            "phase_pair",
            List.of("theta"),
            List.of(
                "left",
                "right"
            ),
            List.of(
                GateBodyOperation.of(
                    StandardGate.RZ,
                    new ParameterExpression[] {ParameterExpression.named("theta")},
                    "left"
                ),
                GateBodyOperation.of(
                    StandardGate.CX,
                    new ParameterExpression[0],
                    "left",
                    "right"
                )
            )
        );
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("inline");
        final QuantumRegister register = circuit.createQuantumRegister(
            "q",
            2
        );
        final ParameterExpression angle = ParameterExpression.divide(
            ParameterExpression.pi(),
            ParameterExpression.of(2.0)
        );

        new CompositeGateInliner().appendExpandedGateOperation(
            circuit,
            GateOperation.parameterized(
                gate,
                new ParameterExpression[] {angle},
                register.get(0),
                register.get(1)
            )
        );

        assertEquals(
            2,
            circuit.operationCount()
        );
        assertSame(
            StandardGate.RZ,
            ((GateOperation) circuit.operation(0)).gate()
        );
        assertSame(
            angle,
            ((GateOperation) circuit.operation(0)).parameter(0)
        );
        assertSame(
            StandardGate.CX,
            ((GateOperation) circuit.operation(1)).gate()
        );
        assertSame(
            register.get(1),
            ((GateOperation) circuit.operation(1)).qubit(1)
        );
    }

    @Test
    void appendsOpaqueOrIntrinsicGateWithoutInlining() {
        final GateDefinition opaque = GateDefinition.opaque(
            "hardware_native",
            List.of(),
            List.of("q")
        );
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("opaque_inline");
        final QuantumRegister register = circuit.createQuantumRegister(
            "q",
            1
        );

        new CompositeGateInliner().appendExpandedGateOperation(
            circuit,
            GateOperation.of(
                opaque,
                register.get(0)
            )
        );

        assertEquals(
            1,
            circuit.operationCount()
        );
        assertSame(
            opaque,
            ((GateOperation) circuit.operation(0)).gate()
        );
    }

    @Test
    void rejectsInvalidBodyBindingAtInliningTime() {
        final GateDefinition gate = GateDefinition.composite(
            "bad_inline",
            List.of("theta"),
            List.of("q"),
            List.of(GateBodyOperation.of(
                StandardGate.RZ,
                new ParameterExpression[] {ParameterExpression.named("missing")},
                "q"
            ))
        );
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bad_inline");
        final QuantumRegister register = circuit.createQuantumRegister(
            "q",
            1
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new CompositeGateInliner().appendExpandedGateOperation(
                circuit,
                GateOperation.parameterized(
                    gate,
                    new ParameterExpression[] {ParameterExpression.of(1.0)},
                    register.get(0)
                )
            )
        );
    }
}