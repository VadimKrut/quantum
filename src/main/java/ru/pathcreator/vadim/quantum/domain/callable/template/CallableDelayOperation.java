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
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;

/**
 * Шаблон временной задержки на формальных квантовых аргументах.
 */
public final class CallableDelayOperation implements CallableOperation {

    private final DurationExpression duration;
    private final IdentifierName[] qubitNames;

    /**
     * Создает шаблон задержки.
     *
     * @param duration длительность задержки
     * @param qubitNames имена квантовых аргументов
     */
    public CallableDelayOperation(
        final DurationExpression duration,
        final String... qubitNames
    ) {
        if (duration == null) {
            throw new IllegalArgumentException("Callable delay duration must not be null.");
        }
        if (qubitNames == null) {
            throw new IllegalArgumentException("Callable delay qubit names must not be null.");
        }
        this.duration = duration;
        this.qubitNames = new IdentifierName[qubitNames.length];
        for (int i = 0; i < qubitNames.length; i++) {
            this.qubitNames[i] = IdentifierName.of(
                qubitNames[i],
                "Callable delay qubit reference"
            );
        }
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.DELAY;
    }

    /**
     * Возвращает длительность задержки.
     *
     * @return длительность
     */
    public DurationExpression duration() {
        return duration;
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
            throw new IllegalArgumentException("Callable delay qubit index is outside of bounds.");
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
        if (!(other instanceof CallableDelayOperation operation)) {
            return false;
        }
        return Objects.equals(duration, operation.duration)
            && Arrays.equals(qubitNames, operation.qubitNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            duration,
            Arrays.hashCode(qubitNames)
        );
    }
}