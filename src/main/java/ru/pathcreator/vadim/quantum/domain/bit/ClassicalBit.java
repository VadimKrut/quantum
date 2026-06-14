/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.bit;

import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;

/**
 * Классический бит внутри конкретного классического регистра.
 */
public final class ClassicalBit {

    /**
     * Регистр, которому принадлежит бит.
     */
    private final ClassicalRegister register;

    /**
     * Индекс бита внутри регистра.
     */
    private final int index;

    private ClassicalBit(
        final ClassicalRegister register,
        final int index
    ) {
        this.register = register;
        this.index = index;
    }

    /**
     * Создает классический бит для существующего классического регистра.
     *
     * @param register регистр-владелец
     * @param index индекс бита внутри регистра
     * @return классический бит с проверенной принадлежностью и индексом
     */
    public static ClassicalBit of(
        final ClassicalRegister register,
        final int index
    ) {
        if (register == null) {
            throw new IllegalArgumentException("Classical register must not be null.");
        }
        if (
            index < 0
            || index >= register.size()
        ) {
            throw new IllegalArgumentException("Classical bit index is outside of classical register bounds.");
        }
        return new ClassicalBit(
            register,
            index
        );
    }

    /**
     * Возвращает регистр, которому принадлежит бит.
     *
     * @return регистр-владелец
     */
    public ClassicalRegister register() {
        return register;
    }

    /**
     * Возвращает индекс бита внутри регистра.
     *
     * @return индекс бита
     */
    public int index() {
        return index;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalBit classicalBit)) {
            return false;
        }
        return register == classicalBit.register
            && index == classicalBit.index;
    }

    @Override
    public int hashCode() {
        int result = System.identityHashCode(register);
        result = 31 * result + index;
        return result;
    }

    @Override
    public String toString() {
        return register.name().value() + "[" + index + "]";
    }
}