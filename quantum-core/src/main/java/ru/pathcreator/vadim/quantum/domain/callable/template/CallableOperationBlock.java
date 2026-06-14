/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Неизменяемый блок шаблонных операций callable.
 */
public final class CallableOperationBlock {

    /**
     * Операции блока в порядке выполнения.
     */
    private final List<CallableOperation> operations;

    private CallableOperationBlock(final List<CallableOperation> operations) {
        this.operations = validateAndCopyOperations(operations);
    }

    /**
     * Создает блок шаблонных операций.
     *
     * @param operations операции блока
     * @return блок операций
     */
    public static CallableOperationBlock of(final List<CallableOperation> operations) {
        return new CallableOperationBlock(operations);
    }

    /**
     * Создает блок шаблонных операций.
     *
     * @param operations операции блока
     * @return блок операций
     */
    public static CallableOperationBlock of(final CallableOperation... operations) {
        if (operations == null) {
            throw new IllegalArgumentException("Callable operation block operations must not be null.");
        }
        final ArrayList<CallableOperation> result = new ArrayList<>();
        for (int i = 0; i < operations.length; i++) {
            result.add(operations[i]);
        }
        return new CallableOperationBlock(result);
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
    public CallableOperation operation(final int index) {
        if (
            index < 0
            || index >= operations.size()
        ) {
            throw new IllegalArgumentException("Callable operation block index is outside of bounds.");
        }
        return operations.get(index);
    }

    /**
     * Возвращает неизменяемый список операций.
     *
     * @return операции блока
     */
    public List<CallableOperation> operations() {
        return operations;
    }

    private static List<CallableOperation> validateAndCopyOperations(final List<CallableOperation> operations) {
        if (operations == null) {
            throw new IllegalArgumentException("Callable operation block operations must not be null.");
        }
        final ArrayList<CallableOperation> result = new ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            final CallableOperation operation = operations.get(i);
            if (operation == null) {
                throw new IllegalArgumentException("Callable operation block operation must not be null.");
            }
            result.add(operation);
        }
        return List.copyOf(result);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableOperationBlock block)) {
            return false;
        }
        return Objects.equals(
            operations,
            block.operations
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(operations);
    }
}