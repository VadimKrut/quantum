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

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;

/**
 * Операция, выполняемая при истинности классического предиката.
 */
public final class ClassicallyControlledOperation implements Operation {

    /**
     * Предикат выполнения операции.
     */
    private final ClassicalPredicate predicate;

    /**
     * Операция под классическим предикатом.
     */
    private final Operation operation;

    /**
     * Создает операцию с классическим предикатом.
     *
     * @param predicate предикат выполнения
     * @param operation операция под предикатом
     */
    public ClassicallyControlledOperation(
        final ClassicalPredicate predicate,
        final Operation operation
    ) {
        if (predicate == null) {
            throw new IllegalArgumentException("Classical predicate must not be null.");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Classically controlled operation body must not be null.");
        }
        if (
            operation instanceof ControlledOperation
            || operation instanceof ClassicallyControlledOperation
        ) {
            throw new IllegalArgumentException("Nested controlled operations are not supported.");
        }
        this.predicate = predicate;
        this.operation = operation;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.CLASSICALLY_CONTROLLED;
    }

    /**
     * Возвращает предикат выполнения.
     *
     * @return предикат выполнения
     */
    public ClassicalPredicate predicate() {
        return predicate;
    }

    /**
     * Возвращает операцию под предикатом.
     *
     * @return операция под предикатом
     */
    public Operation operation() {
        return operation;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicallyControlledOperation controlledOperation)) {
            return false;
        }
        return Objects.equals(
            predicate,
            controlledOperation.predicate
        )
            && Objects.equals(
                operation,
                controlledOperation.operation
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            predicate,
            operation
        );
    }
}