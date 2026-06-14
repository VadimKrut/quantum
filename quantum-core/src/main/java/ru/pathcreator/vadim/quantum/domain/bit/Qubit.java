/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.bit;

import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Кубит внутри конкретного квантового регистра.
 */
public final class Qubit {

    /**
     * Регистр, которому принадлежит кубит.
     */
    private final QuantumRegister register;

    /**
     * Индекс кубита внутри регистра.
     */
    private final int index;

    private Qubit(
        final QuantumRegister register,
        final int index
    ) {
        this.register = register;
        this.index = index;
    }

    /**
     * Создает кубит для существующего квантового регистра.
     *
     * @param register регистр-владелец
     * @param index индекс кубита внутри регистра
     * @return кубит с проверенной принадлежностью и индексом
     */
    public static Qubit of(
        final QuantumRegister register,
        final int index
    ) {
        if (register == null) {
            throw new IllegalArgumentException("Quantum register must not be null.");
        }
        if (
            index < 0
            || index >= register.size()
        ) {
            throw new IllegalArgumentException("Qubit index is outside of quantum register bounds.");
        }
        return new Qubit(
            register,
            index
        );
    }

    /**
     * Возвращает регистр, которому принадлежит кубит.
     *
     * @return регистр-владелец
     */
    public QuantumRegister register() {
        return register;
    }

    /**
     * Возвращает индекс кубита внутри регистра.
     *
     * @return индекс кубита
     */
    public int index() {
        return index;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Qubit qubit)) {
            return false;
        }
        return register == qubit.register
            && index == qubit.index;
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