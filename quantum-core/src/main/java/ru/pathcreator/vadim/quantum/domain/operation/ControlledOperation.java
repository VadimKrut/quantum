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

/**
 * Операция, выполняемая только при выполнении классического условия.
 */
public final class ControlledOperation implements Operation {

    private final ClassicalCondition condition;
    private final Operation operation;

    /**
     * Создает условную операцию.
     *
     * @param condition классическое условие
     * @param operation операция под условием
     */
    public ControlledOperation(
        final ClassicalCondition condition,
        final Operation operation
    ) {
        if (condition == null) {
            throw new IllegalArgumentException("Controlled operation condition must not be null.");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Controlled operation body must not be null.");
        }
        if (
            operation instanceof ControlledOperation
            || operation instanceof ClassicallyControlledOperation
        ) {
            throw new IllegalArgumentException("Nested controlled operations are not supported.");
        }
        this.condition = condition;
        this.operation = operation;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.CONTROLLED;
    }

    /**
     * Возвращает классическое условие.
     *
     * @return классическое условие
     */
    public ClassicalCondition condition() {
        return condition;
    }

    /**
     * Возвращает операцию под условием.
     *
     * @return вложенная операция
     */
    public Operation operation() {
        return operation;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ControlledOperation controlledOperation)) {
            return false;
        }
        return Objects.equals(
            condition,
            controlledOperation.condition
        )
            && Objects.equals(
                operation,
                controlledOperation.operation
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            condition,
            operation
        );
    }
}