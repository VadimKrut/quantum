/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable.template;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Классическое выражение, которое ссылается на формальные аргументы callable.
 */
public final class CallableClassicalExpression {

    /**
     * Тип выражения.
     */
    private final CallableClassicalExpressionKind kind;

    /**
     * Целочисленное значение для INTEGER.
     */
    private final long integerValue;

    /**
     * Имя формального классического аргумента для ARGUMENT_REFERENCE.
     */
    private final IdentifierName argumentName;

    private CallableClassicalExpression(
        final CallableClassicalExpressionKind kind,
        final long integerValue,
        final IdentifierName argumentName
    ) {
        this.kind = kind;
        this.integerValue = integerValue;
        this.argumentName = argumentName;
    }

    /**
     * Создает целочисленное выражение.
     *
     * @param value целочисленное значение
     * @return классическое выражение
     */
    public static CallableClassicalExpression integer(final long value) {
        return new CallableClassicalExpression(
            CallableClassicalExpressionKind.INTEGER,
            value,
            null
        );
    }

    /**
     * Создает ссылку на формальный классический аргумент.
     *
     * @param argumentName имя аргумента
     * @return классическое выражение
     */
    public static CallableClassicalExpression argument(final String argumentName) {
        return new CallableClassicalExpression(
            CallableClassicalExpressionKind.ARGUMENT_REFERENCE,
            0L,
            IdentifierName.of(
                argumentName,
                "Callable classical argument reference"
            )
        );
    }

    /**
     * Возвращает тип выражения.
     *
     * @return тип выражения
     */
    public CallableClassicalExpressionKind kind() {
        return kind;
    }

    /**
     * Возвращает целочисленное значение.
     *
     * @return целочисленное значение
     */
    public long integerValue() {
        if (kind != CallableClassicalExpressionKind.INTEGER) {
            throw new IllegalStateException("Callable classical expression is not an integer literal.");
        }
        return integerValue;
    }

    /**
     * Возвращает имя формального аргумента.
     *
     * @return имя аргумента
     */
    public String argumentName() {
        if (kind != CallableClassicalExpressionKind.ARGUMENT_REFERENCE) {
            throw new IllegalStateException("Callable classical expression is not an argument reference.");
        }
        return argumentName.value();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableClassicalExpression expression)) {
            return false;
        }
        return kind == expression.kind
            && integerValue == expression.integerValue
            && Objects.equals(
                argumentName,
                expression.argumentName
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            kind,
            integerValue,
            argumentName
        );
    }
}