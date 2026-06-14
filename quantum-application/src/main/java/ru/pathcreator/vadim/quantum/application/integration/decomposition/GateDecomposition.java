/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.decomposition;

import java.util.List;

import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;

/**
 * Immutable результат разложения gate operation в набор более простых gate operations.
 */
public final class GateDecomposition {

    /**
     * Операции разложения в порядке выполнения.
     */
    private final List<GateOperation> operations;

    private GateDecomposition(final List<GateOperation> operations) {
        this.operations = operations;
    }

    /**
     * Создает разложение из операций.
     *
     * @param operations операции разложения
     * @return разложение
     */
    public static GateDecomposition of(final List<GateOperation> operations) {
        if (operations == null) {
            throw new IllegalArgumentException("Gate decomposition operations must not be null.");
        }
        if (operations.isEmpty()) {
            throw new IllegalArgumentException("Gate decomposition must not be empty.");
        }
        for (GateOperation operation : operations) {
            if (operation == null) {
                throw new IllegalArgumentException("Gate decomposition operation must not be null.");
            }
        }
        return new GateDecomposition(List.copyOf(operations));
    }

    /**
     * Возвращает количество операций.
     *
     * @return количество операций
     */
    public int operationCount() {
        return operations.size();
    }

    /**
     * Возвращает операцию по индексу.
     *
     * @param index индекс операции
     * @return операция
     */
    public GateOperation operation(final int index) {
        if (
            index < 0
            || index >= operations.size()
        ) {
            throw new IllegalArgumentException("Gate decomposition operation index is outside of bounds.");
        }
        return operations.get(index);
    }

    /**
     * Возвращает immutable список операций.
     *
     * @return операции разложения
     */
    public List<GateOperation> operations() {
        return operations;
    }
}