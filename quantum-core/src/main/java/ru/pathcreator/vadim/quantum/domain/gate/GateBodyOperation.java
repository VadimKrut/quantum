/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import java.util.Arrays;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Символическая операция гейта внутри тела composite gate definition.
 */
public final class GateBodyOperation {

    private static final String QUBIT_NAME_SUBJECT = "Gate body qubit name";

    private final Gate gate;
    private final ParameterExpression[] parameters;
    private final String[] qubitNames;

    private GateBodyOperation(
        final Gate gate,
        final ParameterExpression[] parameters,
        final String[] qubitNames
    ) {
        this.gate = gate;
        this.parameters = parameters;
        this.qubitNames = qubitNames;
    }

    /**
     * Создает symbolic operation внутри тела composite gate.
     *
     * @param gate гейт операции
     * @param parameters параметры операции
     * @param qubitNames имена symbolic qubit arguments
     * @return операция тела gate definition
     */
    public static GateBodyOperation of(
        final Gate gate,
        final ParameterExpression[] parameters,
        final String... qubitNames
    ) {
        validate(
            gate,
            parameters,
            qubitNames
        );
        return new GateBodyOperation(
            gate,
            copyParameters(parameters),
            copyQubitNames(qubitNames)
        );
    }

    public Gate gate() {
        return gate;
    }

    public int parameterCount() {
        return parameters.length;
    }

    public ParameterExpression parameter(final int index) {
        return parameters[index];
    }

    public ParameterExpression[] parameters() {
        return copyParameters(parameters);
    }

    public int qubitCount() {
        return qubitNames.length;
    }

    public String qubitName(final int index) {
        return qubitNames[index];
    }

    public String[] qubitNames() {
        return copyQubitNames(qubitNames);
    }

    private static void validate(
        final Gate gate,
        final ParameterExpression[] parameters,
        final String[] qubitNames
    ) {
        if (gate == null) {
            throw new IllegalArgumentException("Gate body operation gate must not be null.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Gate body operation parameters must not be null.");
        }
        if (qubitNames == null) {
            throw new IllegalArgumentException("Gate body operation qubit names must not be null.");
        }
        if (parameters.length != gate.parameterCount()) {
            throw new IllegalArgumentException("Gate body operation parameter count does not match gate definition.");
        }
        if (qubitNames.length != gate.arity()) {
            throw new IllegalArgumentException("Gate body operation qubit count does not match gate arity.");
        }
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                throw new IllegalArgumentException("Gate body operation parameter must not be null.");
            }
        }
        for (int i = 0; i < qubitNames.length; i++) {
            IdentifierName.of(
                qubitNames[i],
                QUBIT_NAME_SUBJECT
            );
        }
    }

    private static ParameterExpression[] copyParameters(final ParameterExpression[] source) {
        return Arrays.copyOf(
            source,
            source.length
        );
    }

    private static String[] copyQubitNames(final String[] source) {
        final String[] copy = Arrays.copyOf(
            source,
            source.length
        );
        for (int i = 0; i < copy.length; i++) {
            copy[i] = IdentifierName.of(
                copy[i],
                QUBIT_NAME_SUBJECT
            ).value();
        }
        return copy;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GateBodyOperation operation)) {
            return false;
        }
        return Objects.equals(
            gate,
            operation.gate
        )
            && Arrays.equals(
                parameters,
                operation.parameters
            )
            && Arrays.equals(
                qubitNames,
                operation.qubitNames
            );
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gate);
        result = 31 * result + Arrays.hashCode(parameters);
        result = 31 * result + Arrays.hashCode(qubitNames);
        return result;
    }
}