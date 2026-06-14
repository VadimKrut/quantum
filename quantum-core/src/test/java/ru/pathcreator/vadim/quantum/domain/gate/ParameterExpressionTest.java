/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterExpressionTest {

    @Test
    void createsNumericParameter() {
        final ParameterExpression expression = ParameterExpression.of(3.141592653589793);

        assertTrue(expression.isNumeric());
        assertFalse(expression.isNamed());
        assertEquals(
            3.141592653589793,
            expression.numericValue()
        );
        assertEquals(
            "3.141592653589793",
            expression.toString()
        );
    }

    @Test
    void createsNamedParameter() {
        final ParameterExpression expression = ParameterExpression.named("theta");

        assertTrue(expression.isNamed());
        assertFalse(expression.isNumeric());
        assertEquals(
            "theta",
            expression.name()
        );
        assertEquals(
            "theta",
            expression.toString()
        );
    }

    @Test
    void rejectsInvalidNumericParameter() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ParameterExpression.of(Double.NaN)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ParameterExpression.of(Double.POSITIVE_INFINITY)
        );
    }

    @Test
    void rejectsInvalidNamedParameter() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ParameterExpression.named(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ParameterExpression.named("")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ParameterExpression.named("1theta")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ParameterExpression.named("theta-value")
        );
    }

    @Test
    void rejectsReadingWrongParameterKind() {
        assertThrows(
            IllegalStateException.class,
            () -> ParameterExpression.named("theta").numericValue()
        );
        assertThrows(
            IllegalStateException.class,
            () -> ParameterExpression.of(1.0).name()
        );
    }

    @Test
    void comparesByValueAndKind() {
        assertEquals(
            ParameterExpression.of(1.0),
            ParameterExpression.of(1.0)
        );
        assertEquals(
            ParameterExpression.named("theta"),
            ParameterExpression.named("theta")
        );
        assertNotEquals(
            ParameterExpression.of(1.0),
            ParameterExpression.named("theta")
        );
    }

    @Test
    void createsExpressionTree() {
        final ParameterExpression expression = ParameterExpression.divide(
            ParameterExpression.pi(),
            ParameterExpression.of(2.0)
        );

        assertTrue(expression.isBinary());
        assertEquals(
            ParameterExpressionKind.BINARY,
            expression.kind()
        );
        assertEquals(
            ParameterBinaryOperator.DIVIDE,
            expression.binaryOperator()
        );
        assertEquals(
            "pi/2.0",
            expression.toString()
        );
    }

    @Test
    void createsUnaryExpression() {
        final ParameterExpression expression = ParameterExpression.negate(ParameterExpression.named("lambda"));

        assertTrue(expression.isUnary());
        assertEquals(
            ParameterUnaryOperator.NEGATE,
            expression.unaryOperator()
        );
        assertEquals(
            "-lambda",
            expression.toString()
        );
    }
}