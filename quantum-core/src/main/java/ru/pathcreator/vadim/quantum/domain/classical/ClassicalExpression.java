/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.classical;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;

/**
 * Значение или ссылка в классической части универсального Quantum IR.
 */
public final class ClassicalExpression {

    /**
     * Тип классического выражения.
     */
    private final ClassicalExpressionKind kind;

    /**
     * Целочисленное значение для INTEGER.
     */
    private final long integerValue;

    /**
     * Имя runtime classical value для VARIABLE_REFERENCE.
     */
    private final String variableName;

    /**
     * Бинарный оператор для BINARY_OPERATION.
     */
    private final ClassicalBinaryOperator binaryOperator;

    /**
     * Левая часть бинарного выражения.
     */
    private final ClassicalExpression leftExpression;

    /**
     * Правая часть бинарного выражения.
     */
    private final ClassicalExpression rightExpression;

    /**
     * Ссылка на классический бит для BIT_REFERENCE.
     */
    private final ClassicalBit bit;

    /**
     * Ссылка на классический регистр для REGISTER_REFERENCE.
     */
    private final ClassicalRegister register;

    /**
     * Текст символической ссылки для array/slice/foreign classical cell.
     */
    private final String symbolicText;

    /**
     * Имя callable для CALL.
     */
    private final String callableName;

    /**
     * Аргументы вызова для CALL.
     */
    private final List<ClassicalExpression> callArguments;

    private ClassicalExpression(
        final ClassicalExpressionKind kind,
        final long integerValue,
        final String variableName,
        final ClassicalBinaryOperator binaryOperator,
        final ClassicalExpression leftExpression,
        final ClassicalExpression rightExpression,
        final ClassicalBit bit,
        final ClassicalRegister register,
        final String symbolicText,
        final String callableName,
        final List<ClassicalExpression> callArguments
    ) {
        this.kind = kind;
        this.integerValue = integerValue;
        this.variableName = variableName;
        this.binaryOperator = binaryOperator;
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
        this.bit = bit;
        this.register = register;
        this.symbolicText = symbolicText;
        this.callableName = callableName;
        this.callArguments = callArguments == null
            ? List.of()
            : List.copyOf(callArguments);
    }

    /**
     * Создает целочисленное выражение.
     *
     * @param value целочисленное значение
     * @return классическое выражение
     */
    public static ClassicalExpression integer(final long value) {
        return new ClassicalExpression(
            ClassicalExpressionKind.INTEGER,
            value,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()
        );
    }

    /**
     * Создает ссылку на именованное runtime classical value.
     *
     * @param name имя classical value
     * @return классическое выражение-ссылка
     */
    public static ClassicalExpression variable(final String name) {
        final IdentifierName identifierName = IdentifierName.of(
            name,
            "Classical variable"
        );
        return new ClassicalExpression(
            ClassicalExpressionKind.VARIABLE_REFERENCE,
            0,
            identifierName.value(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()
        );
    }

    /**
     * Создает runtime binary expression.
     *
     * @param operator бинарный оператор
     * @param left левая часть выражения
     * @param right правая часть выражения
     * @return classical binary expression
     */
    public static ClassicalExpression binary(
        final ClassicalBinaryOperator operator,
        final ClassicalExpression left,
        final ClassicalExpression right
    ) {
        if (operator == null) {
            throw new IllegalArgumentException("Classical binary operator must not be null.");
        }
        if (left == null) {
            throw new IllegalArgumentException("Classical binary left expression must not be null.");
        }
        if (right == null) {
            throw new IllegalArgumentException("Classical binary right expression must not be null.");
        }
        return new ClassicalExpression(
            ClassicalExpressionKind.BINARY_OPERATION,
            0,
            null,
            operator,
            left,
            right,
            null,
            null,
            null,
            null,
            List.of()
        );
    }

    /**
     * Создает выражение-ссылку на классический бит.
     *
     * @param bit классический бит
     * @return классическое выражение
     */
    public static ClassicalExpression bit(final ClassicalBit bit) {
        if (bit == null) {
            throw new IllegalArgumentException("Classical expression bit must not be null.");
        }
        return new ClassicalExpression(
            ClassicalExpressionKind.BIT_REFERENCE,
            0,
            null,
            null,
            null,
            null,
            bit,
            null,
            null,
            null,
            List.of()
        );
    }

    /**
     * Создает выражение-ссылку на классический регистр.
     *
     * @param register классический регистр
     * @return классическое выражение
     */
    public static ClassicalExpression register(final ClassicalRegister register) {
        if (register == null) {
            throw new IllegalArgumentException("Classical expression register must not be null.");
        }
        return new ClassicalExpression(
            ClassicalExpressionKind.REGISTER_REFERENCE,
            0,
            null,
            null,
            null,
            null,
            null,
            register,
            null,
            null,
            List.of()
        );
    }

    /**
     * Создает символическую ссылку на array/slice/foreign classical cell.
     *
     * @param text текст ссылки
     * @return символическое классическое выражение
     */
    public static ClassicalExpression symbolicReference(final String text) {
        if (
            text == null
            || text.isBlank()
        ) {
            throw new IllegalArgumentException("Classical symbolic reference must not be blank.");
        }
        return new ClassicalExpression(
            ClassicalExpressionKind.SYMBOLIC_REFERENCE,
            0,
            null,
            null,
            null,
            null,
            null,
            null,
            text.trim(),
            null,
            List.of()
        );
    }

    /**
     * Создает выражение-вызов classical/extern callable.
     *
     * @param name имя вызываемой сущности
     * @param arguments аргументы вызова
     * @return выражение-вызов
     */
    public static ClassicalExpression call(
        final String name,
        final List<ClassicalExpression> arguments
    ) {
        final IdentifierName identifierName = IdentifierName.of(
            name,
            "Classical callable"
        );
        if (arguments == null) {
            throw new IllegalArgumentException("Classical call arguments must not be null.");
        }
        final ArrayList<ClassicalExpression> copiedArguments = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i) == null) {
                throw new IllegalArgumentException("Classical call argument must not be null.");
            }
            copiedArguments.add(arguments.get(i));
        }
        return new ClassicalExpression(
            ClassicalExpressionKind.CALL,
            0,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            identifierName.value(),
            copiedArguments
        );
    }

    public ClassicalExpressionKind kind() {
        return kind;
    }

    public long integerValue() {
        if (kind != ClassicalExpressionKind.INTEGER) {
            throw new IllegalStateException("Classical expression is not an integer literal.");
        }
        return integerValue;
    }

    public String variableName() {
        if (kind != ClassicalExpressionKind.VARIABLE_REFERENCE) {
            throw new IllegalStateException("Classical expression is not a variable reference.");
        }
        return variableName;
    }

    public ClassicalBinaryOperator binaryOperator() {
        if (kind != ClassicalExpressionKind.BINARY_OPERATION) {
            throw new IllegalStateException("Classical expression is not a binary operation.");
        }
        return binaryOperator;
    }

    public ClassicalExpression leftExpression() {
        if (kind != ClassicalExpressionKind.BINARY_OPERATION) {
            throw new IllegalStateException("Classical expression is not a binary operation.");
        }
        return leftExpression;
    }

    public ClassicalExpression rightExpression() {
        if (kind != ClassicalExpressionKind.BINARY_OPERATION) {
            throw new IllegalStateException("Classical expression is not a binary operation.");
        }
        return rightExpression;
    }

    public ClassicalBit bit() {
        if (kind != ClassicalExpressionKind.BIT_REFERENCE) {
            throw new IllegalStateException("Classical expression is not a bit reference.");
        }
        return bit;
    }

    public ClassicalRegister register() {
        if (kind != ClassicalExpressionKind.REGISTER_REFERENCE) {
            throw new IllegalStateException("Classical expression is not a register reference.");
        }
        return register;
    }

    public String symbolicText() {
        if (kind != ClassicalExpressionKind.SYMBOLIC_REFERENCE) {
            throw new IllegalStateException("Classical expression is not a symbolic reference.");
        }
        return symbolicText;
    }

    public String callableName() {
        if (kind != ClassicalExpressionKind.CALL) {
            throw new IllegalStateException("Classical expression is not a call.");
        }
        return callableName;
    }

    public int callArgumentCount() {
        if (kind != ClassicalExpressionKind.CALL) {
            throw new IllegalStateException("Classical expression is not a call.");
        }
        return callArguments.size();
    }

    public ClassicalExpression callArgument(final int index) {
        if (kind != ClassicalExpressionKind.CALL) {
            throw new IllegalStateException("Classical expression is not a call.");
        }
        if (
            index < 0
            || index >= callArguments.size()
        ) {
            throw new IllegalArgumentException("Classical call argument index is outside of bounds.");
        }
        return callArguments.get(index);
    }

    public List<ClassicalExpression> callArguments() {
        if (kind != ClassicalExpressionKind.CALL) {
            throw new IllegalStateException("Classical expression is not a call.");
        }
        return callArguments;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalExpression expression)) {
            return false;
        }
        return kind == expression.kind
            && integerValue == expression.integerValue
            && Objects.equals(
                variableName,
                expression.variableName
            )
            && binaryOperator == expression.binaryOperator
            && Objects.equals(
                leftExpression,
                expression.leftExpression
            )
            && Objects.equals(
                rightExpression,
                expression.rightExpression
            )
            && bit == expression.bit
            && register == expression.register
            && Objects.equals(
                symbolicText,
                expression.symbolicText
            )
            && Objects.equals(
                callableName,
                expression.callableName
            )
            && Objects.equals(
                callArguments,
                expression.callArguments
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            kind,
            integerValue,
            variableName,
            binaryOperator,
            leftExpression,
            rightExpression,
            System.identityHashCode(bit),
            System.identityHashCode(register),
            symbolicText,
            callableName,
            callArguments
        );
    }
}