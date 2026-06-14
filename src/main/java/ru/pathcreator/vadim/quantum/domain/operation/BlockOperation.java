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
 * Явный scoped-блок операций.
 */
public final class BlockOperation implements Operation {

    /**
     * Тело блока.
     */
    private final OperationBlock body;

    /**
     * Создает scoped-блок.
     *
     * @param body тело блока
     */
    public BlockOperation(final OperationBlock body) {
        if (body == null) {
            throw new IllegalArgumentException("Block operation body must not be null.");
        }
        this.body = body;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.BLOCK;
    }

    /**
     * Возвращает тело блока.
     *
     * @return тело блока
     */
    public OperationBlock body() {
        return body;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockOperation operation)) {
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