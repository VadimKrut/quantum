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
 * Шаблон цикла с классическим условием продолжения.
 */
public final class CallableWhileLoopOperation implements CallableOperation {

    private final CallableClassicalPredicate predicate;
    private final CallableOperationBlock body;

    /**
     * Создает шаблон while-цикла.
     *
     * @param predicate условие продолжения
     * @param body тело цикла
     */
    public CallableWhileLoopOperation(
        final CallableClassicalPredicate predicate,
        final CallableOperationBlock body
    ) {
        if (predicate == null) {
            throw new IllegalArgumentException("Callable while-loop predicate must not be null.");
        }
        if (body == null) {
            throw new IllegalArgumentException("Callable while-loop body must not be null.");
        }
        this.predicate = predicate;
        this.body = body;
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.WHILE_LOOP;
    }

    /**
     * Возвращает условие продолжения.
     *
     * @return предикат
     */
    public CallableClassicalPredicate predicate() {
        return predicate;
    }

    /**
     * Возвращает тело цикла.
     *
     * @return тело цикла
     */
    public CallableOperationBlock body() {
        return body;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableWhileLoopOperation operation)) {
            return false;
        }
        return Objects.equals(predicate, operation.predicate)
            && Objects.equals(body, operation.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            predicate,
            body
        );
    }
}