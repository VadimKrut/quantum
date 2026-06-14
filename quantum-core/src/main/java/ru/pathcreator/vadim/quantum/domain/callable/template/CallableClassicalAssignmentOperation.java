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

/**
 * Шаблон операции классического присваивания.
 */
public final class CallableClassicalAssignmentOperation implements CallableOperation {

    private final CallableClassicalAssignment assignment;

    /**
     * Создает операцию присваивания.
     *
     * @param assignment шаблонное присваивание
     */
    public CallableClassicalAssignmentOperation(final CallableClassicalAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Callable classical assignment operation value must not be null.");
        }
        this.assignment = assignment;
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.CLASSICAL_ASSIGNMENT;
    }

    /**
     * Возвращает шаблонное присваивание.
     *
     * @return присваивание
     */
    public CallableClassicalAssignment assignment() {
        return assignment;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableClassicalAssignmentOperation operation)) {
            return false;
        }
        return Objects.equals(
            assignment,
            operation.assignment
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignment);
    }
}