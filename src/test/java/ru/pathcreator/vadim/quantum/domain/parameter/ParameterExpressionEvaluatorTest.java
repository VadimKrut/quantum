/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.parameter;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterExpressionEvaluatorTest {

    @Test
    void evaluatesKnownConstantsAndArithmetic() {
        final ParameterExpression expression = ParameterExpression.divide(
            ParameterExpression.pi(),
            ParameterExpression.of(2.0)
        );

        final double value = new ParameterExpressionEvaluator().evaluate(
            expression,
            ParameterBindings.empty()
        );

        assertEquals(
            Math.PI / 2.0,
            value
        );
    }

    @Test
    void bindsNamedParametersWithoutMutatingOriginalExpression() {
        final ParameterExpression theta = ParameterExpression.named("theta");
        final ParameterExpression expression = ParameterExpression.add(
            theta,
            ParameterExpression.of(1.0)
        );
        final ParameterBindings bindings = ParameterBindings.builder()
            .put(
                "theta",
                2.0
            )
            .build();

        final ParameterBindingResult result = new ParameterExpressionEvaluator().bind(
            expression,
            bindings
        );

        assertTrue(result.isComplete());
        assertEquals(
            ParameterExpressionKind.NUMERIC,
            result.expression().kind()
        );
        assertEquals(
            3.0,
            result.expression().numericValue()
        );
        assertEquals(
            ParameterExpressionKind.BINARY,
            expression.kind()
        );
        assertEquals(
            "theta",
            theta.name()
        );
        assertNotSame(
            expression,
            result.expression()
        );
    }

    @Test
    void keepsUnboundSymbolsAndReportsThemOnce() {
        final ParameterExpression expression = ParameterExpression.add(
            ParameterExpression.named("theta"),
            ParameterExpression.named("theta")
        );

        final ParameterBindingResult result = new ParameterExpressionEvaluator().bind(
            expression,
            ParameterBindings.empty()
        );

        assertFalse(result.isComplete());
        assertEquals(
            1,
            result.missingSymbols().size()
        );
        assertEquals(
            "theta",
            result.missingSymbols().get(0)
        );
    }

    @Test
    void refusesEvaluationWhenRequiredSymbolIsMissing() {
        final ParameterExpression expression = ParameterExpression.named("theta");

        assertThrows(
            IllegalArgumentException.class,
            () -> new ParameterExpressionEvaluator().evaluate(
                expression,
                ParameterBindings.empty()
            )
        );
    }
}