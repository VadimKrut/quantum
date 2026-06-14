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
 * Неизменяемая операция измерения qubit reference в classical bit.
 */
public final class MeasureOperation implements Operation {

    /**
     * Измеряемая ссылка на qubit.
     */
    private final QuantumReference qubitReference;

    /**
     * Classical bit, в который записывается результат измерения.
     */
    private final ClassicalBit bit;

    /**
     * Создает измерение статического qubit.
     *
     * @param qubit измеряемый qubit
     * @param bit classical bit результата
     */
    public MeasureOperation(
        final Qubit qubit,
        final ClassicalBit bit
    ) {
        this(
            QuantumReference.staticQubit(qubit),
            bit
        );
    }

    /**
     * Создает измерение qubit reference.
     *
     * @param qubitReference измеряемая ссылка на qubit
     * @param bit classical bit результата
     */
    public MeasureOperation(
        final QuantumReference qubitReference,
        final ClassicalBit bit
    ) {
        if (qubitReference == null) {
            throw new IllegalArgumentException("Measured qubit reference must not be null.");
        }
        if (bit == null) {
            throw new IllegalArgumentException("Measurement classical bit must not be null.");
        }
        this.qubitReference = qubitReference;
        this.bit = bit;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.MEASURE;
    }

    /**
     * Возвращает статический измеряемый qubit.
     *
     * @return измеряемый qubit
     */
    public Qubit qubit() {
        return qubitReference.qubit();
    }

    /**
     * Возвращает измеряемую ссылку на qubit.
     *
     * @return ссылка на qubit
     */
    public QuantumReference qubitReference() {
        return qubitReference;
    }

    /**
     * Возвращает classical bit результата.
     *
     * @return classical bit результата
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
            qubitReference,
            operation.qubitReference
        )
            && Objects.equals(
                bit,
                operation.bit
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            qubitReference,
            bit
        );
    }
}