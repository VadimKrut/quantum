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

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;

/**
 * Неизменяемая операция сброса кубита.
 */
public final class ResetOperation implements Operation {

    /**
     * Кубит, который нужно сбросить.
     */
    private final Qubit qubit;

    /**
     * Создает immutable операцию сброса.
     *
     * @param qubit сбрасываемый кубит
     */
    public ResetOperation(final Qubit qubit) {
        if (qubit == null) {
            throw new IllegalArgumentException("Reset qubit must not be null.");
        }
        this.qubit = qubit;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.RESET;
    }

    /**
     * Возвращает сбрасываемый кубит.
     *
     * @return сбрасываемый кубит
     */
    public Qubit qubit() {
        return qubit;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ResetOperation operation)) {
            return false;
        }
        return Objects.equals(
            qubit,
            operation.qubit
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(qubit);
    }
}