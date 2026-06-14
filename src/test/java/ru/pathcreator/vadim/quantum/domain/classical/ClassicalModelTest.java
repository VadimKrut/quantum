/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.classical;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassicalModelTest {

    @Test
    void modelsBitRegisterAndIntegerExpressions() {
        final ClassicalRegister register = createClassicalRegister();

        final ClassicalExpression bit = ClassicalExpression.bit(register.get(0));
        final ClassicalExpression wholeRegister = ClassicalExpression.register(register);
        final ClassicalExpression integer = ClassicalExpression.integer(3);
        final ClassicalExpression variable = ClassicalExpression.variable("address");
        final ClassicalExpression binary = ClassicalExpression.binary(
            ClassicalBinaryOperator.ADD,
            variable,
            ClassicalExpression.integer(1)
        );

        assertEquals(
            ClassicalExpressionKind.BIT_REFERENCE,
            bit.kind()
        );
        assertSame(
            register.get(0),
            bit.bit()
        );
        assertEquals(
            ClassicalExpressionKind.REGISTER_REFERENCE,
            wholeRegister.kind()
        );
        assertSame(
            register,
            wholeRegister.register()
        );
        assertEquals(
            3,
            integer.integerValue()
        );
        assertEquals(
            ClassicalExpressionKind.VARIABLE_REFERENCE,
            variable.kind()
        );
        assertEquals(
            "address",
            variable.variableName()
        );
        assertEquals(
            ClassicalExpressionKind.BINARY_OPERATION,
            binary.kind()
        );
        assertEquals(
            ClassicalBinaryOperator.ADD,
            binary.binaryOperator()
        );
        assertSame(
            variable,
            binary.leftExpression()
        );
    }

    @Test
    void modelsComparisonsAndBooleanPredicates() {
        final ClassicalRegister register = createClassicalRegister();
        final ClassicalPredicate low = ClassicalPredicate.compare(
            ClassicalExpression.register(register),
            ClassicalComparisonOperator.GREATER_THAN_OR_EQUAL,
            ClassicalExpression.integer(1)
        );
        final ClassicalPredicate high = ClassicalPredicate.compare(
            ClassicalExpression.register(register),
            ClassicalComparisonOperator.LESS_THAN,
            ClassicalExpression.integer(3)
        );

        final ClassicalPredicate predicate = ClassicalPredicate.and(
            low,
            ClassicalPredicate.not(high)
        );

        assertEquals(
            ClassicalPredicateKind.BOOLEAN,
            predicate.kind()
        );
        assertEquals(
            ClassicalBooleanOperator.AND,
            predicate.booleanOperator()
        );
        assertSame(
            low,
            predicate.leftPredicate()
        );
        assertEquals(
            ClassicalPredicateKind.NOT,
            predicate.rightPredicate().kind()
        );
    }

    @Test
    void refusesAssignmentToLiteral() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ClassicalAssignment(
                ClassicalExpression.integer(1),
                ClassicalExpression.integer(0)
            )
        );
    }

    @Test
    void treatsAssignmentsAsValueObjects() {
        final ClassicalRegister register = createClassicalRegister();

        final ClassicalAssignment left = new ClassicalAssignment(
            ClassicalExpression.bit(register.get(0)),
            ClassicalExpression.integer(1)
        );
        final ClassicalAssignment right = new ClassicalAssignment(
            ClassicalExpression.bit(register.get(0)),
            ClassicalExpression.integer(1)
        );

        assertEquals(
            left,
            right
        );
        assertEquals(
            left.hashCode(),
            right.hashCode()
        );
    }

    private static ClassicalRegister createClassicalRegister() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("classical_model");
        return circuit.createClassicalRegister(
            "c",
            2
        );
    }
}