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
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;

/**
 * Временная задержка на одном или нескольких кубитах.
 */
public final class DelayOperation implements Operation {

    private final DurationExpression duration;
    private final QuantumReference[] references;

    public DelayOperation(
        final DurationExpression duration,
        final Qubit... qubits
    ) {
        this(
            duration,
            referencesFromQubits(qubits)
        );
    }

    public DelayOperation(
        final DurationExpression duration,
        final QuantumReference... references
    ) {
        if (duration == null) {
            throw new IllegalArgumentException("Delay duration must not be null.");
        }
        if (references == null) {
            throw new IllegalArgumentException("Delay quantum references must not be null.");
        }
        for (int i = 0; i < references.length; i++) {
            if (references[i] == null) {
                throw new IllegalArgumentException("Delay quantum reference must not be null.");
            }
        }
        this.duration = duration;
        this.references = Arrays.copyOf(
            references,
            references.length
        );
    }

    private static QuantumReference[] referencesFromQubits(final Qubit[] qubits) {
        if (qubits == null) {
            throw new IllegalArgumentException("Delay qubits must not be null.");
        }
        final QuantumReference[] references = new QuantumReference[qubits.length];
        for (int i = 0; i < qubits.length; i++) {
            if (qubits[i] == null) {
                throw new IllegalArgumentException("Delay qubit must not be null.");
            }
            references[i] = QuantumReference.staticQubit(qubits[i]);
        }
        return references;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.DELAY;
    }

    public DurationExpression duration() {
        return duration;
    }

    public int qubitCount() {
        return references.length;
    }

    public Qubit qubit(final int index) {
        return reference(index).qubit();
    }

    public QuantumReference reference(final int index) {
        if (
            index < 0
            || index >= references.length
        ) {
            throw new IllegalArgumentException("Delay quantum reference index is outside of bounds.");
        }
        return references[index];
    }

    public Qubit[] qubits() {
        final Qubit[] qubits = new Qubit[references.length];
        for (int i = 0; i < references.length; i++) {
            qubits[i] = references[i].qubit();
        }
        return qubits;
    }

    public QuantumReference[] references() {
        return Arrays.copyOf(
            references,
            references.length
        );
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DelayOperation operation)) {
            return false;
        }
        return Objects.equals(
            duration,
            operation.duration
        )
            && Arrays.equals(
                references,
                operation.references
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            duration,
            Arrays.hashCode(references)
        );
    }
}