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
 * Неизменяемая операция сброса qubit reference.
 */
public final class ResetOperation implements Operation {

    /**
     * Ссылка на qubit, который нужно сбросить.
     */
    private final QuantumReference qubitReference;

    /**
     * Создает reset статического qubit.
     *
     * @param qubit сбрасываемый qubit
     */
    public ResetOperation(final Qubit qubit) {
        this(QuantumReference.staticQubit(qubit));
    }

    /**
     * Создает reset qubit reference.
     *
     * @param qubitReference сбрасываемая ссылка на qubit
     */
    public ResetOperation(final QuantumReference qubitReference) {
        if (qubitReference == null) {
            throw new IllegalArgumentException("Reset qubit reference must not be null.");
        }
        this.qubitReference = qubitReference;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.RESET;
    }

    /**
     * Возвращает статический сбрасываемый qubit.
     *
     * @return сбрасываемый qubit
     */
    public Qubit qubit() {
        return qubitReference.qubit();
    }

    /**
     * Возвращает ссылку на сбрасываемый qubit.
     *
     * @return ссылка на qubit
     */
    public QuantumReference qubitReference() {
        return qubitReference;
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
            qubitReference,
            operation.qubitReference
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(qubitReference);
    }
}