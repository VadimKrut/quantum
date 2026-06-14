/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;

/**
 * Классическое условие для условного выполнения операции.
 */
public final class ClassicalCondition {

    private final ClassicalRegister register;
    private final long expectedValue;

    private ClassicalCondition(
        final ClassicalRegister register,
        final long expectedValue
    ) {
        this.register = register;
        this.expectedValue = expectedValue;
    }

    /**
     * Создает условие register == expectedValue.
     *
     * @param register классический регистр
     * @param expectedValue ожидаемое значение
     * @return классическое условие
     */
    public static ClassicalCondition equalTo(
        final ClassicalRegister register,
        final long expectedValue
    ) {
        if (register == null) {
            throw new IllegalArgumentException("Classical condition register must not be null.");
        }
        if (expectedValue < 0) {
            throw new IllegalArgumentException("Classical condition value must not be negative.");
        }
        return new ClassicalCondition(
            register,
            expectedValue
        );
    }

    /**
     * Возвращает классический регистр условия.
     *
     * @return классический регистр
     */
    public ClassicalRegister register() {
        return register;
    }

    /**
     * Возвращает ожидаемое значение.
     *
     * @return ожидаемое значение
     */
    public long expectedValue() {
        return expectedValue;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalCondition condition)) {
            return false;
        }
        return expectedValue == condition.expectedValue
            && register == condition.register;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            System.identityHashCode(register),
            expectedValue
        );
    }
}