/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable.template;

import java.util.Arrays;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Шаблон барьера над формальными квантовыми аргументами.
 */
public final class CallableBarrierOperation implements CallableOperation {

    private final IdentifierName[] qubitNames;

    /**
     * Создает шаблон барьера.
     *
     * @param qubitNames имена квантовых аргументов
     */
    public CallableBarrierOperation(final String... qubitNames) {
        if (qubitNames == null) {
            throw new IllegalArgumentException("Callable barrier qubit names must not be null.");
        }
        if (qubitNames.length == 0) {
            throw new IllegalArgumentException("Callable barrier must contain at least one qubit.");
        }
        this.qubitNames = new IdentifierName[qubitNames.length];
        for (int i = 0; i < qubitNames.length; i++) {
            this.qubitNames[i] = IdentifierName.of(
                qubitNames[i],
                "Callable barrier qubit reference"
            );
        }
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.BARRIER;
    }

    /**
     * Возвращает количество квантовых аргументов.
     *
     * @return количество квантовых аргументов
     */
    public int qubitCount() {
        return qubitNames.length;
    }

    /**
     * Возвращает имя квантового аргумента по индексу.
     *
     * @param index индекс аргумента
     * @return имя аргумента
     */
    public String qubitName(final int index) {
        if (
            index < 0
            || index >= qubitNames.length
        ) {
            throw new IllegalArgumentException("Callable barrier qubit index is outside of bounds.");
        }
        return qubitNames[index].value();
    }

    /**
     * Возвращает копию имен квантовых аргументов.
     *
     * @return имена аргументов
     */
    public String[] qubitNames() {
        final String[] result = new String[qubitNames.length];
        for (int i = 0; i < qubitNames.length; i++) {
            result[i] = qubitNames[i].value();
        }
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableBarrierOperation operation)) {
            return false;
        }
        return Arrays.equals(
            qubitNames,
            operation.qubitNames
        );
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(qubitNames);
    }
}