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

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;

/**
 * Операция присваивания в классической части Quantum IR.
 */
public final class ClassicalAssignmentOperation implements Operation {

    /**
     * Описание классического присваивания.
     */
    private final ClassicalAssignment assignment;

    /**
     * Создает операцию классического присваивания.
     *
     * @param assignment классическое присваивание
     */
    public ClassicalAssignmentOperation(final ClassicalAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Classical assignment must not be null.");
        }
        this.assignment = assignment;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.CLASSICAL_ASSIGNMENT;
    }

    /**
     * Возвращает классическое присваивание.
     *
     * @return классическое присваивание
     */
    public ClassicalAssignment assignment() {
        return assignment;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalAssignmentOperation operation)) {
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