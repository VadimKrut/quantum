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

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Шаблон сброса формального квантового аргумента.
 */
public final class CallableResetOperation implements CallableOperation {

    private final IdentifierName qubitName;

    /**
     * Создает шаблон сброса.
     *
     * @param qubitName имя квантового аргумента
     */
    public CallableResetOperation(final String qubitName) {
        this.qubitName = IdentifierName.of(
            qubitName,
            "Callable reset qubit reference"
        );
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.RESET;
    }

    /**
     * Возвращает имя квантового аргумента.
     *
     * @return имя квантового аргумента
     */
    public String qubitName() {
        return qubitName.value();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableResetOperation operation)) {
            return false;
        }
        return Objects.equals(
            qubitName,
            operation.qubitName
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(qubitName);
    }
}