/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.classical;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;

/**
 * Значение или ссылка в классической части IR.
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
     * Ссылка на классический бит для BIT_REFERENCE.
     */
    private final ClassicalBit bit;

    /**
     * Ссылка на классический регистр для REGISTER_REFERENCE.
     */
    private final ClassicalRegister register;

    private ClassicalExpression(
        final ClassicalExpressionKind kind,
        final long integerValue,
        final ClassicalBit bit,
        final ClassicalRegister register
    ) {
        this.kind = kind;
        this.integerValue = integerValue;
        this.bit = bit;
        this.register = register;
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
            null
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
            bit,
            null
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
            register
        );
    }

    /**
     * Возвращает тип выражения.
     *
     * @return тип выражения
     */
    public ClassicalExpressionKind kind() {
        return kind;
    }

    /**
     * Возвращает целочисленное значение.
     *
     * @return целочисленное значение
     */
    public long integerValue() {
        if (kind != ClassicalExpressionKind.INTEGER) {
            throw new IllegalStateException("Classical expression is not an integer literal.");
        }
        return integerValue;
    }

    /**
     * Возвращает классический бит.
     *
     * @return классический бит
     */
    public ClassicalBit bit() {
        if (kind != ClassicalExpressionKind.BIT_REFERENCE) {
            throw new IllegalStateException("Classical expression is not a bit reference.");
        }
        return bit;
    }

    /**
     * Возвращает классический регистр.
     *
     * @return классический регистр
     */
    public ClassicalRegister register() {
        if (kind != ClassicalExpressionKind.REGISTER_REFERENCE) {
            throw new IllegalStateException("Classical expression is not a register reference.");
        }
        return register;
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
            && bit == expression.bit
            && register == expression.register;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            kind,
            integerValue,
            System.identityHashCode(bit),
            System.identityHashCode(register)
        );
    }
}