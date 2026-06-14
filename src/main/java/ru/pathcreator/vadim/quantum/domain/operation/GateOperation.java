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
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;

/**
 * Неизменяемая операция применения gate к набору qubit references.
 */
public final class GateOperation implements Operation {

    /**
     * Общий пустой массив qubit для старого статического API.
     */
    private static final Qubit[] EMPTY_QUBITS = new Qubit[0];

    /**
     * Общий пустой массив qubit references.
     */
    private static final QuantumReference[] EMPTY_REFERENCES = new QuantumReference[0];

    /**
     * Общий пустой массив параметров.
     */
    private static final ParameterExpression[] EMPTY_PARAMETERS = new ParameterExpression[0];

    /**
     * Gate, который применяется операцией.
     */
    private final Gate gate;

    /**
     * Ссылки на qubits операции.
     */
    private final QuantumReference[] qubitReferences;

    /**
     * Параметры gate.
     */
    private final ParameterExpression[] parameters;

    /**
     * Создает операцию со статическими qubits.
     *
     * @param gate gate операции
     * @param qubits статические qubits операции
     * @param parameters параметры операции
     */
    public GateOperation(
        final Gate gate,
        final Qubit[] qubits,
        final ParameterExpression[] parameters
    ) {
        this(
            gate,
            referencesFromQubits(qubits),
            parameters
        );
    }

    /**
     * Создает операцию с qubit references.
     *
     * @param gate gate операции
     * @param qubitReferences ссылки на qubits операции
     * @param parameters параметры операции
     */
    public GateOperation(
        final Gate gate,
        final QuantumReference[] qubitReferences,
        final ParameterExpression[] parameters
    ) {
        validate(
            gate,
            qubitReferences,
            parameters
        );
        this.gate = gate;
        this.qubitReferences = copyReferences(qubitReferences);
        this.parameters = copyParameters(parameters);
    }

    /**
     * Создает операцию для непараметризованного gate со статическими qubits.
     *
     * @param gate gate
     * @param qubits qubits операции
     * @return операция применения gate
     */
    public static GateOperation of(
        final Gate gate,
        final Qubit... qubits
    ) {
        return new GateOperation(
            gate,
            qubits,
            EMPTY_PARAMETERS
        );
    }

    /**
     * Создает операцию для параметризованного gate со статическими qubits.
     *
     * @param gate gate
     * @param parameters параметры gate
     * @param qubits qubits операции
     * @return операция применения gate
     */
    public static GateOperation parameterized(
        final Gate gate,
        final ParameterExpression[] parameters,
        final Qubit... qubits
    ) {
        return new GateOperation(
            gate,
            qubits,
            parameters
        );
    }

    /**
     * Создает операцию для непараметризованного gate с qubit references.
     *
     * @param gate gate
     * @param qubitReferences ссылки на qubits операции
     * @return операция применения gate
     */
    public static GateOperation ofReferences(
        final Gate gate,
        final QuantumReference... qubitReferences
    ) {
        return new GateOperation(
            gate,
            qubitReferences,
            EMPTY_PARAMETERS
        );
    }

    /**
     * Создает операцию для параметризованного gate с qubit references.
     *
     * @param gate gate
     * @param parameters параметры gate
     * @param qubitReferences ссылки на qubits операции
     * @return операция применения gate
     */
    public static GateOperation parameterizedReferences(
        final Gate gate,
        final ParameterExpression[] parameters,
        final QuantumReference... qubitReferences
    ) {
        return new GateOperation(
            gate,
            qubitReferences,
            parameters
        );
    }

    @Override
    public OperationKind kind() {
        return OperationKind.GATE;
    }

    /**
     * Возвращает gate операции.
     *
     * @return gate операции
     */
    public Gate gate() {
        return gate;
    }

    /**
     * Возвращает количество qubit references операции.
     *
     * @return количество qubit references
     */
    public int qubitCount() {
        return qubitReferences.length;
    }

    /**
     * Возвращает статический qubit по индексу.
     *
     * @param index индекс qubit
     * @return qubit операции
     */
    public Qubit qubit(final int index) {
        validateQubitIndex(index);
        return qubitReferences[index].qubit();
    }

    /**
     * Возвращает ссылку на qubit по индексу.
     *
     * @param index индекс ссылки
     * @return qubit reference операции
     */
    public QuantumReference qubitReference(final int index) {
        validateQubitIndex(index);
        return qubitReferences[index];
    }

    /**
     * Возвращает копию статических qubits.
     *
     * @return копия массива qubits
     */
    public Qubit[] qubits() {
        final Qubit[] qubits = new Qubit[qubitReferences.length];
        for (int i = 0; i < qubitReferences.length; i++) {
            qubits[i] = qubitReferences[i].qubit();
        }
        return copyQubits(qubits);
    }

    /**
     * Возвращает копию qubit references.
     *
     * @return копия массива qubit references
     */
    public QuantumReference[] qubitReferences() {
        return copyReferences(qubitReferences);
    }

    /**
     * Возвращает количество параметров операции.
     *
     * @return количество параметров
     */
    public int parameterCount() {
        return parameters.length;
    }

    /**
     * Возвращает параметр по индексу.
     *
     * @param index индекс параметра
     * @return параметр операции
     */
    public ParameterExpression parameter(final int index) {
        validateParameterIndex(index);
        return parameters[index];
    }

    /**
     * Возвращает копию параметров.
     *
     * @return копия массива параметров
     */
    public ParameterExpression[] parameters() {
        return copyParameters(parameters);
    }

    private static void validate(
        final Gate gate,
        final QuantumReference[] qubitReferences,
        final ParameterExpression[] parameters
    ) {
        if (gate == null) {
            throw new IllegalArgumentException("Gate must not be null.");
        }
        if (qubitReferences == null) {
            throw new IllegalArgumentException("Gate operation qubit references must not be null.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Gate operation parameters must not be null.");
        }
        if (qubitReferences.length != gate.arity()) {
            throw new IllegalArgumentException("Gate operation qubit count does not match gate arity.");
        }
        if (parameters.length != gate.parameterCount()) {
            throw new IllegalArgumentException("Gate operation parameter count does not match gate definition.");
        }
        for (int i = 0; i < qubitReferences.length; i++) {
            if (qubitReferences[i] == null) {
                throw new IllegalArgumentException("Gate operation qubit reference must not be null.");
            }
        }
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                throw new IllegalArgumentException("Gate operation parameter must not be null.");
            }
        }
    }

    private void validateQubitIndex(final int index) {
        if (
            index < 0
            || index >= qubitReferences.length
        ) {
            throw new IllegalArgumentException("Gate operation qubit index is outside of operation bounds.");
        }
    }

    private void validateParameterIndex(final int index) {
        if (
            index < 0
            || index >= parameters.length
        ) {
            throw new IllegalArgumentException("Gate operation parameter index is outside of operation bounds.");
        }
    }

    private static QuantumReference[] referencesFromQubits(final Qubit[] qubits) {
        if (qubits == null) {
            throw new IllegalArgumentException("Gate operation qubits must not be null.");
        }
        final QuantumReference[] references = new QuantumReference[qubits.length];
        for (int i = 0; i < qubits.length; i++) {
            references[i] = QuantumReference.staticQubit(qubits[i]);
        }
        return references;
    }

    private static Qubit[] copyQubits(final Qubit[] source) {
        if (source.length == 0) {
            return EMPTY_QUBITS;
        }
        return Arrays.copyOf(
            source,
            source.length
        );
    }

    private static QuantumReference[] copyReferences(final QuantumReference[] source) {
        if (source.length == 0) {
            return EMPTY_REFERENCES;
        }
        return Arrays.copyOf(
            source,
            source.length
        );
    }

    private static ParameterExpression[] copyParameters(final ParameterExpression[] source) {
        if (source.length == 0) {
            return EMPTY_PARAMETERS;
        }
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
        if (!(other instanceof GateOperation operation)) {
            return false;
        }
        return Objects.equals(
            gate,
            operation.gate
        )
            && Arrays.equals(
                qubitReferences,
                operation.qubitReferences
            )
            && Arrays.equals(
                parameters,
                operation.parameters
            );
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gate);
        result = 31 * result + Arrays.hashCode(qubitReferences);
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }
}