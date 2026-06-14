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
 * Неизменяемая операция применения гейта к набору кубитов.
 */
public final class GateOperation implements Operation {

    /**
     * Общий пустой массив кубитов для операций без кубитов.
     */
    private static final Qubit[] EMPTY_QUBITS = new Qubit[0];

    /**
     * Общий пустой массив параметров для непараметризованных операций.
     */
    private static final ParameterExpression[] EMPTY_PARAMETERS = new ParameterExpression[0];

    /**
     * Гейт, который применяется операцией.
     */
    private final Gate gate;

    /**
     * Кубиты, к которым применяется гейт.
     */
    private final Qubit[] qubits;

    /**
     * Параметры гейта.
     */
    private final ParameterExpression[] parameters;

    /**
     * Создает immutable операцию применения гейта.
     *
     * @param gate гейт операции
     * @param qubits кубиты операции
     * @param parameters параметры операции
     */
    public GateOperation(
        final Gate gate,
        final Qubit[] qubits,
        final ParameterExpression[] parameters
    ) {
        validate(
            gate,
            qubits,
            parameters
        );
        this.gate = gate;
        this.qubits = copyQubits(qubits);
        this.parameters = copyParameters(parameters);
    }

    /**
     * Создает операцию для непараметризованного гейта.
     *
     * @param gate гейт
     * @param qubits кубиты операции
     * @return операция применения гейта
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
     * Создает операцию для параметризованного гейта.
     *
     * @param gate гейт
     * @param parameters параметры гейта
     * @param qubits кубиты операции
     * @return операция применения гейта
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

    @Override
    public OperationKind kind() {
        return OperationKind.GATE;
    }

    /**
     * Возвращает гейт операции.
     *
     * @return гейт операции
     */
    public Gate gate() {
        return gate;
    }

    /**
     * Возвращает количество кубитов операции.
     *
     * @return количество кубитов
     */
    public int qubitCount() {
        return qubits.length;
    }

    /**
     * Возвращает кубит по индексу внутри операции.
     *
     * @param index индекс кубита
     * @return кубит операции
     */
    public Qubit qubit(final int index) {
        validateQubitIndex(index);
        return qubits[index];
    }

    /**
     * Возвращает копию массива кубитов.
     *
     * @return копия массива кубитов
     */
    public Qubit[] qubits() {
        return copyQubits(qubits);
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
     * Возвращает параметр по индексу внутри операции.
     *
     * @param index индекс параметра
     * @return параметр операции
     */
    public ParameterExpression parameter(final int index) {
        validateParameterIndex(index);
        return parameters[index];
    }

    /**
     * Возвращает копию массива параметров.
     *
     * @return копия массива параметров
     */
    public ParameterExpression[] parameters() {
        return copyParameters(parameters);
    }

    private static void validate(
        final Gate gate,
        final Qubit[] qubits,
        final ParameterExpression[] parameters
    ) {
        if (gate == null) {
            throw new IllegalArgumentException("Gate must not be null.");
        }
        if (qubits == null) {
            throw new IllegalArgumentException("Gate operation qubits must not be null.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Gate operation parameters must not be null.");
        }
        if (qubits.length != gate.arity()) {
            throw new IllegalArgumentException("Gate operation qubit count does not match gate arity.");
        }
        if (parameters.length != gate.parameterCount()) {
            throw new IllegalArgumentException("Gate operation parameter count does not match gate definition.");
        }
        for (int i = 0; i < qubits.length; i++) {
            if (qubits[i] == null) {
                throw new IllegalArgumentException("Gate operation qubit must not be null.");
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
            || index >= qubits.length
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

    private static Qubit[] copyQubits(final Qubit[] source) {
        if (source.length == 0) {
            return EMPTY_QUBITS;
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
                qubits,
                operation.qubits
            )
            && Arrays.equals(
                parameters,
                operation.parameters
            );
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gate);
        result = 31 * result + Arrays.hashCode(qubits);
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }
}