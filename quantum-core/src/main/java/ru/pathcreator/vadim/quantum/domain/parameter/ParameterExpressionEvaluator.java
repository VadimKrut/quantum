/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.parameter;

import java.util.ArrayList;

import ru.pathcreator.vadim.quantum.domain.gate.ParameterBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterUnaryOperator;

/**
 * Сервис подстановки и вычисления деревьев параметрических выражений без мутации исходного выражения.
 */
public final class ParameterExpressionEvaluator {

    /**
     * Подставляет числовые значения и вычисляет выражение, если все символы известны.
     *
     * @param expression исходное выражение
     * @param bindings значения символов
     * @return результат подстановки и вычисления
     */
    public ParameterBindingResult bind(
        final ParameterExpression expression,
        final ParameterBindings bindings
    ) {
        if (expression == null) {
            throw new IllegalArgumentException("Parameter expression must not be null.");
        }
        if (bindings == null) {
            throw new IllegalArgumentException("Parameter bindings must not be null.");
        }
        final ArrayList<String> missingSymbols = new ArrayList<>();
        final ParameterExpression bound = bindExpression(
            expression,
            bindings,
            missingSymbols
        );
        return new ParameterBindingResult(
            bound,
            missingSymbols
        );
    }

    /**
     * Вычисляет выражение в double.
     *
     * @param expression исходное выражение
     * @param bindings значения символов
     * @return числовое значение
     */
    public double evaluate(
        final ParameterExpression expression,
        final ParameterBindings bindings
    ) {
        final ParameterBindingResult result = bind(
            expression,
            bindings
        );
        if (!result.isComplete()) {
            throw new IllegalArgumentException("Parameter expression has unbound symbols.");
        }
        return result.expression().numericValue();
    }

    private ParameterExpression bindExpression(
        final ParameterExpression expression,
        final ParameterBindings bindings,
        final ArrayList<String> missingSymbols
    ) {
        if (expression.kind() == ParameterExpressionKind.NUMERIC) {
            return expression;
        }
        if (expression.kind() == ParameterExpressionKind.KNOWN_CONSTANT) {
            return bindKnownConstant(expression);
        }
        if (expression.kind() == ParameterExpressionKind.NAMED) {
            return bindNamed(
                expression,
                bindings,
                missingSymbols
            );
        }
        if (expression.kind() == ParameterExpressionKind.UNARY) {
            return bindUnary(
                expression,
                bindings,
                missingSymbols
            );
        }
        return bindBinary(
            expression,
            bindings,
            missingSymbols
        );
    }

    private static ParameterExpression bindKnownConstant(final ParameterExpression expression) {
        if ("pi".equals(expression.name())) {
            return ParameterExpression.of(Math.PI);
        }
        throw new IllegalArgumentException("Unknown parameter constant: " + expression.name() + ".");
    }

    private static ParameterExpression bindNamed(
        final ParameterExpression expression,
        final ParameterBindings bindings,
        final ArrayList<String> missingSymbols
    ) {
        if (bindings.contains(expression.name())) {
            return ParameterExpression.of(bindings.value(expression.name()));
        }
        if (!missingSymbols.contains(expression.name())) {
            missingSymbols.add(expression.name());
        }
        return expression;
    }

    private ParameterExpression bindUnary(
        final ParameterExpression expression,
        final ParameterBindings bindings,
        final ArrayList<String> missingSymbols
    ) {
        final ParameterExpression bound = bindExpression(
            expression.left(),
            bindings,
            missingSymbols
        );
        if (
            bound.isNumeric()
            && expression.unaryOperator() == ParameterUnaryOperator.NEGATE
        ) {
            return ParameterExpression.of(-bound.numericValue());
        }
        return ParameterExpression.negate(bound);
    }

    private ParameterExpression bindBinary(
        final ParameterExpression expression,
        final ParameterBindings bindings,
        final ArrayList<String> missingSymbols
    ) {
        final ParameterExpression left = bindExpression(
            expression.left(),
            bindings,
            missingSymbols
        );
        final ParameterExpression right = bindExpression(
            expression.right(),
            bindings,
            missingSymbols
        );
        if (
            left.isNumeric()
            && right.isNumeric()
        ) {
            return ParameterExpression.of(applyBinaryOperator(
                expression.binaryOperator(),
                left.numericValue(),
                right.numericValue()
            ));
        }
        return rebuildBinary(
            expression.binaryOperator(),
            left,
            right
        );
    }

    private static double applyBinaryOperator(
        final ParameterBinaryOperator operator,
        final double left,
        final double right
    ) {
        if (operator == ParameterBinaryOperator.ADD) {
            return left + right;
        }
        if (operator == ParameterBinaryOperator.SUBTRACT) {
            return left - right;
        }
        if (operator == ParameterBinaryOperator.MULTIPLY) {
            return left * right;
        }
        return left / right;
    }

    private static ParameterExpression rebuildBinary(
        final ParameterBinaryOperator operator,
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        if (operator == ParameterBinaryOperator.ADD) {
            return ParameterExpression.add(
                left,
                right
            );
        }
        if (operator == ParameterBinaryOperator.SUBTRACT) {
            return ParameterExpression.subtract(
                left,
                right
            );
        }
        if (operator == ParameterBinaryOperator.MULTIPLY) {
            return ParameterExpression.multiply(
                left,
                right
            );
        }
        return ParameterExpression.divide(
            left,
            right
        );
    }
}