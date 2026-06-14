/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable.template;

import java.util.Arrays;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Шаблон применения гейта к формальным квантовым аргументам.
 */
public final class CallableGateOperation implements CallableOperation {

    private static final ParameterExpression[] EMPTY_PARAMETERS = new ParameterExpression[0];

    /**
     * Гейт операции.
     */
    private final Gate gate;

    /**
     * Параметры гейта.
     */
    private final ParameterExpression[] parameters;

    /**
     * Имена формальных квантовых аргументов.
     */
    private final IdentifierName[] qubitNames;

    /**
     * Создает шаблон применения гейта.
     *
     * @param gate гейт
     * @param parameters параметры гейта
     * @param qubitNames имена формальных квантовых аргументов
     */
    public CallableGateOperation(
        final Gate gate,
        final ParameterExpression[] parameters,
        final String... qubitNames
    ) {
        validate(
            gate,
            parameters,
            qubitNames
        );
        this.gate = gate;
        this.parameters = copyParameters(parameters);
        this.qubitNames = copyNames(qubitNames);
    }

    /**
     * Создает непараметризованное применение гейта.
     *
     * @param gate гейт
     * @param qubitNames имена формальных квантовых аргументов
     * @return операция
     */
    public static CallableGateOperation of(
        final Gate gate,
        final String... qubitNames
    ) {
        return new CallableGateOperation(
            gate,
            EMPTY_PARAMETERS,
            qubitNames
        );
    }

    /**
     * Создает параметризованное применение гейта.
     *
     * @param gate гейт
     * @param parameters параметры гейта
     * @param qubitNames имена формальных квантовых аргументов
     * @return операция
     */
    public static CallableGateOperation parameterized(
        final Gate gate,
        final ParameterExpression[] parameters,
        final String... qubitNames
    ) {
        return new CallableGateOperation(
            gate,
            parameters,
            qubitNames
        );
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.GATE;
    }

    /**
     * Возвращает гейт.
     *
     * @return гейт
     */
    public Gate gate() {
        return gate;
    }

    /**
     * Возвращает количество параметров.
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
     * @return параметр
     */
    public ParameterExpression parameter(final int index) {
        if (
            index < 0
            || index >= parameters.length
        ) {
            throw new IllegalArgumentException("Callable gate parameter index is outside of bounds.");
        }
        return parameters[index];
    }

    /**
     * Возвращает копию массива параметров.
     *
     * @return параметры
     */
    public ParameterExpression[] parameters() {
        return copyParameters(parameters);
    }

    /**
     * Возвращает количество квантовых аргументов.
     *
     * @return количество квантовых аргументов
     */
    public int qubitCount() {
        return qubitNames.length;
    }

    /**
     * Возвращает имя квантового аргумента по индексу.
     *
     * @param index индекс аргумента
     * @return имя аргумента
     */
    public String qubitName(final int index) {
        if (
            index < 0
            || index >= qubitNames.length
        ) {
            throw new IllegalArgumentException("Callable gate qubit index is outside of bounds.");
        }
        return qubitNames[index].value();
    }

    /**
     * Возвращает копию имен квантовых аргументов.
     *
     * @return имена аргументов
     */
    public String[] qubitNames() {
        return copyNameValues(qubitNames);
    }

    private static void validate(
        final Gate gate,
        final ParameterExpression[] parameters,
        final String[] qubitNames
    ) {
        if (gate == null) {
            throw new IllegalArgumentException("Callable gate must not be null.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Callable gate parameters must not be null.");
        }
        if (qubitNames == null) {
            throw new IllegalArgumentException("Callable gate qubit names must not be null.");
        }
        if (parameters.length != gate.parameterCount()) {
            throw new IllegalArgumentException("Callable gate parameter count does not match gate definition.");
        }
        if (qubitNames.length != gate.arity()) {
            throw new IllegalArgumentException("Callable gate qubit count does not match gate arity.");
        }
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                throw new IllegalArgumentException("Callable gate parameter must not be null.");
            }
        }
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

    private static IdentifierName[] copyNames(final String[] source) {
        final IdentifierName[] result = new IdentifierName[source.length];
        for (int i = 0; i < source.length; i++) {
            result[i] = IdentifierName.of(
                source[i],
                "Callable gate qubit reference"
            );
        }
        return result;
    }

    private static String[] copyNameValues(final IdentifierName[] source) {
        final String[] result = new String[source.length];
        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].value();
        }
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableGateOperation operation)) {
            return false;
        }
        return Objects.equals(gate, operation.gate)
            && Arrays.equals(parameters, operation.parameters)
            && Arrays.equals(qubitNames, operation.qubitNames);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gate);
        result = 31 * result + Arrays.hashCode(parameters);
        result = 31 * result + Arrays.hashCode(qubitNames);
        return result;
    }
}