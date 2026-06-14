/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Неизменяемый блок операций для ветвлений, циклов и scoped-блоков.
 */
public final class OperationBlock {

    /**
     * Операции блока в порядке выполнения.
     */
    private final List<Operation> operations;

    private OperationBlock(final List<Operation> operations) {
        this.operations = validateAndCopyOperations(operations);
    }

    /**
     * Создает блок операций.
     *
     * @param operations операции блока
     * @return блок операций
     */
    public static OperationBlock of(final List<Operation> operations) {
        return new OperationBlock(operations);
    }

    /**
     * Создает блок операций.
     *
     * @param operations операции блока
     * @return блок операций
     */
    public static OperationBlock of(final Operation... operations) {
        if (operations == null) {
            throw new IllegalArgumentException("Operation block operations must not be null.");
        }
        final ArrayList<Operation> result = new ArrayList<>();
        for (int i = 0; i < operations.length; i++) {
            result.add(operations[i]);
        }
        return new OperationBlock(result);
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
    public Operation operation(final int index) {
        if (
            index < 0
            || index >= operations.size()
        ) {
            throw new IllegalArgumentException("Operation block index is outside of bounds.");
        }
        return operations.get(index);
    }

    /**
     * Возвращает неизменяемый список операций.
     *
     * @return операции блока
     */
    public List<Operation> operations() {
        return operations;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OperationBlock block)) {
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

    private static List<Operation> validateAndCopyOperations(final List<Operation> operations) {
        if (operations == null) {
            throw new IllegalArgumentException("Operation block operations must not be null.");
        }
        final ArrayList<Operation> result = new ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            final Operation operation = operations.get(i);
            if (operation == null) {
                throw new IllegalArgumentException("Operation block operation must not be null.");
            }
            result.add(operation);
        }
        return List.copyOf(result);
    }
}