/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

import java.util.Arrays;

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;

/**
 * Неизменяемая операция барьера для одного или нескольких кубитов.
 */
public final class BarrierOperation implements Operation {

    /**
     * Кубиты, для которых установлен барьер.
     */
    private final Qubit[] qubits;

    /**
     * Создает immutable операцию барьера.
     *
     * @param qubits кубиты барьера
     */
    public BarrierOperation(final Qubit... qubits) {
        validate(qubits);
        this.qubits = copyQubits(qubits);
    }

    @Override
    public OperationKind kind() {
        return OperationKind.BARRIER;
    }

    /**
     * Возвращает количество кубитов барьера.
     *
     * @return количество кубитов
     */
    public int qubitCount() {
        return qubits.length;
    }

    /**
     * Возвращает кубит барьера по индексу.
     *
     * @param index индекс кубита
     * @return кубит барьера
     */
    public Qubit qubit(final int index) {
        validateIndex(index);
        return qubits[index];
    }

    /**
     * Возвращает копию массива кубитов барьера.
     *
     * @return копия массива кубитов
     */
    public Qubit[] qubits() {
        return copyQubits(qubits);
    }

    private static void validate(final Qubit[] qubits) {
        if (qubits == null) {
            throw new IllegalArgumentException("Barrier qubits must not be null.");
        }
        if (qubits.length == 0) {
            throw new IllegalArgumentException("Barrier must contain at least one qubit.");
        }
        for (int i = 0; i < qubits.length; i++) {
            if (qubits[i] == null) {
                throw new IllegalArgumentException("Barrier qubit must not be null.");
            }
        }
    }

    private void validateIndex(final int index) {
        if (
            index < 0
            || index >= qubits.length
        ) {
            throw new IllegalArgumentException("Barrier qubit index is outside of operation bounds.");
        }
    }

    private static Qubit[] copyQubits(final Qubit[] source) {
        return Arrays.copyOf(
            source,
            source.length
        );
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BarrierOperation operation)) {
            return false;
        }
        return Arrays.equals(
            qubits,
            operation.qubits
        );
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(qubits);
    }
}