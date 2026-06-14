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
 * Цикл с классическим условием продолжения.
 */
public final class WhileLoopOperation implements Operation {

    private final ClassicalPredicate predicate;
    private final OperationBlock body;

    public WhileLoopOperation(
        final ClassicalPredicate predicate,
        final OperationBlock body
    ) {
        if (predicate == null) {
            throw new IllegalArgumentException("While-loop predicate must not be null.");
        }
        if (body == null) {
            throw new IllegalArgumentException("While-loop body must not be null.");
        }
        this.predicate = predicate;
        this.body = body;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.WHILE_LOOP;
    }

    public ClassicalPredicate predicate() {
        return predicate;
    }

    public OperationBlock body() {
        return body;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WhileLoopOperation operation)) {
            return false;
        }
        return Objects.equals(
            predicate,
            operation.predicate
        )
            && Objects.equals(
                body,
                operation.body
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            predicate,
            body
        );
    }
}