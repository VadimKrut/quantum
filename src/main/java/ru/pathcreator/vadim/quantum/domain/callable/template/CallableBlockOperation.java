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
 * Шаблон scoped-блока операций.
 */
public final class CallableBlockOperation implements CallableOperation {

    private final CallableOperationBlock body;

    /**
     * Создает scoped-блок.
     *
     * @param body тело блока
     */
    public CallableBlockOperation(final CallableOperationBlock body) {
        if (body == null) {
            throw new IllegalArgumentException("Callable block body must not be null.");
        }
        this.body = body;
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.BLOCK;
    }

    /**
     * Возвращает тело блока.
     *
     * @return тело блока
     */
    public CallableOperationBlock body() {
        return body;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableBlockOperation operation)) {
            return false;
        }
        return Objects.equals(
            body,
            operation.body
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(body);
    }
}