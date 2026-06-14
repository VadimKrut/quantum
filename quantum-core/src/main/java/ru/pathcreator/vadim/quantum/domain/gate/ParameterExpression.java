/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Дерево выражения параметра gate-based операции.
 */
public final class ParameterExpression {

    private static final String SUBJECT_NAME = "Parameter name";

    /**
     * Тип узла выражения.
     */
    private final ParameterExpressionKind kind;

    /**
     * Числовое значение для NUMERIC.
     */
    private final double numericValue;

    /**
     * Имя символа или известной константы.
     */
    private final String name;

    /**
     * Унарный оператор для UNARY.
     */
    private final ParameterUnaryOperator unaryOperator;

    /**
     * Бинарный оператор для BINARY.
     */
    private final ParameterBinaryOperator binaryOperator;

    /**
     * Левый дочерний узел или единственный operand унарного выражения.
     */
    private final ParameterExpression left;

    /**
     * Правый дочерний узел бинарного выражения.
     */
    private final ParameterExpression right;

    private ParameterExpression(
        final ParameterExpressionKind kind,
        final double numericValue,
        final String name,
        final ParameterUnaryOperator unaryOperator,
        final ParameterBinaryOperator binaryOperator,
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        this.kind = kind;
        this.numericValue = numericValue;
        this.name = name;
        this.unaryOperator = unaryOperator;
        this.binaryOperator = binaryOperator;
        this.left = left;
        this.right = right;
    }

    /**
     * Создает числовую константу.
     *
     * @param value конечное числовое значение
     * @return числовое выражение
     */
    public static ParameterExpression of(final double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Numeric parameter must be finite.");
        }
        return new ParameterExpression(
            ParameterExpressionKind.NUMERIC,
            value,
            null,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Создает символический параметр.
     *
     * @param name имя параметра
     * @return символическое выражение
     */
    public static ParameterExpression named(final String name) {
        final IdentifierName identifierName = IdentifierName.of(
            name,
            SUBJECT_NAME
        );
        return new ParameterExpression(
            ParameterExpressionKind.NAMED,
            0.0,
            identifierName.value(),
            null,
            null,
            null,
            null
        );
    }

    /**
     * Создает известную константу pi.
     *
     * @return выражение pi
     */
    public static ParameterExpression pi() {
        return knownConstant("pi");
    }

    /**
     * Создает известную именованную константу.
     *
     * @param name имя константы
     * @return выражение константы
     */
    public static ParameterExpression knownConstant(final String name) {
        final IdentifierName identifierName = IdentifierName.of(
            name,
            SUBJECT_NAME
        );
        return new ParameterExpression(
            ParameterExpressionKind.KNOWN_CONSTANT,
            0.0,
            identifierName.value(),
            null,
            null,
            null,
            null
        );
    }

    /**
     * Создает унарное отрицание.
     *
     * @param expression выражение operand
     * @return унарное выражение
     */
    public static ParameterExpression negate(final ParameterExpression expression) {
        return unary(
            ParameterUnaryOperator.NEGATE,
            expression
        );
    }

    /**
     * Создает сумму.
     *
     * @param left левый operand
     * @param right правый operand
     * @return бинарное выражение
     */
    public static ParameterExpression add(
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        return binary(
            ParameterBinaryOperator.ADD,
            left,
            right
        );
    }

    /**
     * Создает разность.
     *
     * @param left левый operand
     * @param right правый operand
     * @return бинарное выражение
     */
    public static ParameterExpression subtract(
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        return binary(
            ParameterBinaryOperator.SUBTRACT,
            left,
            right
        );
    }

    /**
     * Создает произведение.
     *
     * @param left левый operand
     * @param right правый operand
     * @return бинарное выражение
     */
    public static ParameterExpression multiply(
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        return binary(
            ParameterBinaryOperator.MULTIPLY,
            left,
            right
        );
    }

    /**
     * Создает деление.
     *
     * @param left левый operand
     * @param right правый operand
     * @return бинарное выражение
     */
    public static ParameterExpression divide(
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        return binary(
            ParameterBinaryOperator.DIVIDE,
            left,
            right
        );
    }

    private static ParameterExpression unary(
        final ParameterUnaryOperator operator,
        final ParameterExpression expression
    ) {
        if (operator == null) {
            throw new IllegalArgumentException("Parameter unary operator must not be null.");
        }
        if (expression == null) {
            throw new IllegalArgumentException("Parameter unary operand must not be null.");
        }
        return new ParameterExpression(
            ParameterExpressionKind.UNARY,
            0.0,
            null,
            operator,
            null,
            expression,
            null
        );
    }

    private static ParameterExpression binary(
        final ParameterBinaryOperator operator,
        final ParameterExpression left,
        final ParameterExpression right
    ) {
        if (operator == null) {
            throw new IllegalArgumentException("Parameter binary operator must not be null.");
        }
        if (left == null) {
            throw new IllegalArgumentException("Parameter left operand must not be null.");
        }
        if (right == null) {
            throw new IllegalArgumentException("Parameter right operand must not be null.");
        }
        return new ParameterExpression(
            ParameterExpressionKind.BINARY,
            0.0,
            null,
            null,
            operator,
            left,
            right
        );
    }

    /**
     * Возвращает тип выражения.
     *
     * @return тип выражения
     */
    public ParameterExpressionKind kind() {
        return kind;
    }

    public boolean isNumeric() {
        return kind == ParameterExpressionKind.NUMERIC;
    }

    public boolean isNamed() {
        return kind == ParameterExpressionKind.NAMED;
    }

    public boolean isKnownConstant() {
        return kind == ParameterExpressionKind.KNOWN_CONSTANT;
    }

    public boolean isUnary() {
        return kind == ParameterExpressionKind.UNARY;
    }

    public boolean isBinary() {
        return kind == ParameterExpressionKind.BINARY;
    }

    public double numericValue() {
        if (!isNumeric()) {
            throw new IllegalStateException("Parameter expression is not numeric.");
        }
        return numericValue;
    }

    public String name() {
        if (
            !isNamed()
            && !isKnownConstant()
        ) {
            throw new IllegalStateException("Parameter expression does not have a name.");
        }
        return name;
    }

    public ParameterUnaryOperator unaryOperator() {
        if (!isUnary()) {
            throw new IllegalStateException("Parameter expression is not unary.");
        }
        return unaryOperator;
    }

    public ParameterBinaryOperator binaryOperator() {
        if (!isBinary()) {
            throw new IllegalStateException("Parameter expression is not binary.");
        }
        return binaryOperator;
    }

    public ParameterExpression left() {
        if (
            !isUnary()
            && !isBinary()
        ) {
            throw new IllegalStateException("Parameter expression does not have a left operand.");
        }
        return left;
    }

    public ParameterExpression right() {
        if (!isBinary()) {
            throw new IllegalStateException("Parameter expression does not have a right operand.");
        }
        return right;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ParameterExpression expression)) {
            return false;
        }
        return kind == expression.kind
            && Double.compare(
                numericValue,
                expression.numericValue
            ) == 0
            && Objects.equals(
                name,
                expression.name
            )
            && unaryOperator == expression.unaryOperator
            && binaryOperator == expression.binaryOperator
            && Objects.equals(
                left,
                expression.left
            )
            && Objects.equals(
                right,
                expression.right
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            kind,
            numericValue,
            name,
            unaryOperator,
            binaryOperator,
            left,
            right
        );
    }

    @Override
    public String toString() {
        if (isNumeric()) {
            return Double.toString(numericValue);
        }
        if (
            isNamed()
            || isKnownConstant()
        ) {
            return name;
        }
        if (isUnary()) {
            return unaryOperator.symbol() + parenthesized(left);
        }
        return parenthesized(left) + binaryOperator.symbol() + parenthesized(right);
    }

    private static String parenthesized(final ParameterExpression expression) {
        if (
            expression.isNumeric()
            || expression.isNamed()
            || expression.isKnownConstant()
        ) {
            return expression.toString();
        }
        return "(" + expression + ")";
    }
}