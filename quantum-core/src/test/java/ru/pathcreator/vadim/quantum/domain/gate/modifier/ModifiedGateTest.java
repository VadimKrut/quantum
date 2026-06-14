/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate.modifier;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationErrorCode;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModifiedGateTest {

    @Test
    void controlledModifierAddsQuantumControlArity() {
        final ModifiedGate gate = ModifiedGate.of(
            StandardGate.X,
            List.of(GateModifier.controlled(2))
        );

        assertSame(
            StandardGate.X,
            gate.baseGate()
        );
        assertEquals(
            3,
            gate.arity()
        );
        assertEquals(
            0,
            gate.parameterCount()
        );
    }

    @Test
    void inversePowerRepeatAndAnnotationDoNotChangeArity() {
        final ModifiedGate gate = ModifiedGate.of(
            StandardGate.RZ,
            List.of(
                GateModifier.inverse(),
                GateModifier.power(0.5),
                GateModifier.repeat(3),
                GateModifier.annotation("calibrated")
            )
        );

        assertEquals(
            StandardGate.RZ.arity(),
            gate.arity()
        );
        assertEquals(
            StandardGate.RZ.parameterCount(),
            gate.parameterCount()
        );
        assertEquals(
            "calibrated",
            gate.modifiers().get(3).annotationName()
        );
    }

    @Test
    void refusesInvalidModifiers() {
        assertThrows(
            IllegalArgumentException.class,
            () -> GateModifier.controlled(0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateModifier.repeat(0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> GateModifier.power(Double.NaN)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ModifiedGate.of(
                StandardGate.H,
                List.of()
            )
        );
    }

    @Test
    void controlledModifierUsesDistinctQubitValidationRule() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("modified_validation");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.gate(
            ModifiedGate.of(
                StandardGate.X,
                List.of(GateModifier.controlled(1))
            ),
            q.get(0),
            q.get(0)
        );

        final ValidationResult result = new QuantumProgramValidator().validate(program);

        assertEquals(
            ValidationErrorCode.DUPLICATE_QUBIT_IN_GATE_OPERATION,
            result.error(0).code()
        );
    }

    @Test
    void refusesArityOverflowFromControlModifiers() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ModifiedGate.of(
                StandardGate.X,
                List.of(GateModifier.controlled(Integer.MAX_VALUE))
            )
        );
    }
}