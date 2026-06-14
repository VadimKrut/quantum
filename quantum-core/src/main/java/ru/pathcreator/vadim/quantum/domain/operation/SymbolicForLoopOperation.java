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

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Цикл по диапазону, границы которого задаются classical expressions.
 */
public final class SymbolicForLoopOperation implements Operation {

    private final IdentifierName variableName;
    private final String variableTypeText;
    private final ClassicalExpression startInclusive;
    private final ClassicalExpression step;
    private final ClassicalExpression endInclusive;
    private final OperationBlock body;

    public SymbolicForLoopOperation(
        final String variableName,
        final String variableTypeText,
        final ClassicalExpression startInclusive,
        final ClassicalExpression step,
        final ClassicalExpression endInclusive,
        final OperationBlock body
    ) {
        if (startInclusive == null) {
            throw new IllegalArgumentException("Symbolic for-loop start must not be null.");
        }
        if (step == null) {
            throw new IllegalArgumentException("Symbolic for-loop step must not be null.");
        }
        if (endInclusive == null) {
            throw new IllegalArgumentException("Symbolic for-loop end must not be null.");
        }
        if (body == null) {
            throw new IllegalArgumentException("Symbolic for-loop body must not be null.");
        }
        this.variableName = IdentifierName.of(
            variableName,
            "Symbolic for-loop variable"
        );
        this.variableTypeText = variableTypeText == null || variableTypeText.isBlank()
            ? null
            : variableTypeText.trim();
        this.startInclusive = startInclusive;
        this.step = step;
        this.endInclusive = endInclusive;
        this.body = body;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.SYMBOLIC_FOR_LOOP;
    }

    public String variableName() {
        return variableName.value();
    }

    public boolean hasVariableTypeText() {
        return variableTypeText != null;
    }

    public String variableTypeText() {
        if (variableTypeText == null) {
            throw new IllegalStateException("Symbolic for-loop does not have variable type text.");
        }
        return variableTypeText;
    }

    public ClassicalExpression startInclusive() {
        return startInclusive;
    }

    public ClassicalExpression step() {
        return step;
    }

    public ClassicalExpression endInclusive() {
        return endInclusive;
    }

    public OperationBlock body() {
        return body;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SymbolicForLoopOperation operation)) {
            return false;
        }
        return Objects.equals(
            variableName,
            operation.variableName
        )
            && Objects.equals(
                variableTypeText,
                operation.variableTypeText
            )
            && Objects.equals(
                startInclusive,
                operation.startInclusive
            )
            && Objects.equals(
                step,
                operation.step
            )
            && Objects.equals(
                endInclusive,
                operation.endInclusive
            )
            && Objects.equals(
                body,
                operation.body
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            variableName,
            variableTypeText,
            startInclusive,
            step,
            endInclusive,
            body
        );
    }
}