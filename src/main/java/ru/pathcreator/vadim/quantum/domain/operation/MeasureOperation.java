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

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;

/**
 * Неизменяемая операция измерения кубита в классический бит.
 */
public final class MeasureOperation implements Operation {

    /**
     * Измеряемый кубит.
     */
    private final Qubit qubit;

    /**
     * Классический бит, в который записывается результат измерения.
     */
    private final ClassicalBit bit;

    /**
     * Создает immutable операцию измерения.
     *
     * @param qubit измеряемый кубит
     * @param bit классический бит результата
     */
    public MeasureOperation(
        final Qubit qubit,
        final ClassicalBit bit
    ) {
        if (qubit == null) {
            throw new IllegalArgumentException("Measured qubit must not be null.");
        }
        if (bit == null) {
            throw new IllegalArgumentException("Measurement classical bit must not be null.");
        }
        this.qubit = qubit;
        this.bit = bit;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.MEASURE;
    }

    /**
     * Возвращает измеряемый кубит.
     *
     * @return измеряемый кубит
     */
    public Qubit qubit() {
        return qubit;
    }

    /**
     * Возвращает классический бит результата.
     *
     * @return классический бит результата
     */
    public ClassicalBit bit() {
        return bit;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MeasureOperation operation)) {
            return false;
        }
        return Objects.equals(
            qubit,
            operation.qubit
        )
            && Objects.equals(
                bit,
                operation.bit
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            qubit,
            bit
        );
    }
}