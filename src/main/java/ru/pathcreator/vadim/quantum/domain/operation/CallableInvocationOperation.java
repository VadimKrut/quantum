/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Вызов callable/subroutine/extern в потоке операций.
 */
public final class CallableInvocationOperation implements Operation {

    private final IdentifierName callableName;
    private final ClassicalExpression target;
    private final List<ClassicalExpression> classicalArguments;
    private final List<QuantumReference> quantumArguments;

    public CallableInvocationOperation(
        final String callableName,
        final ClassicalExpression target,
        final List<ClassicalExpression> classicalArguments,
        final List<QuantumReference> quantumArguments
    ) {
        if (classicalArguments == null) {
            throw new IllegalArgumentException("Callable invocation classical arguments must not be null.");
        }
        if (quantumArguments == null) {
            throw new IllegalArgumentException("Callable invocation quantum arguments must not be null.");
        }
        this.callableName = IdentifierName.of(
            callableName,
            "Callable invocation"
        );
        this.target = target;
        this.classicalArguments = copyClassicalArguments(classicalArguments);
        this.quantumArguments = copyQuantumArguments(quantumArguments);
    }

    private static List<ClassicalExpression> copyClassicalArguments(final List<ClassicalExpression> arguments) {
        final ArrayList<ClassicalExpression> result = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i) == null) {
                throw new IllegalArgumentException("Callable invocation classical argument must not be null.");
            }
            result.add(arguments.get(i));
        }
        return List.copyOf(result);
    }

    private static List<QuantumReference> copyQuantumArguments(final List<QuantumReference> arguments) {
        final ArrayList<QuantumReference> result = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i) == null) {
                throw new IllegalArgumentException("Callable invocation quantum argument must not be null.");
            }
            result.add(arguments.get(i));
        }
        return List.copyOf(result);
    }

    @Override
    public OperationKind kind() {
        return OperationKind.CALLABLE_INVOCATION;
    }

    public String callableName() {
        return callableName.value();
    }

    public boolean hasTarget() {
        return target != null;
    }

    public ClassicalExpression target() {
        if (target == null) {
            throw new IllegalStateException("Callable invocation does not have a target.");
        }
        return target;
    }

    public List<ClassicalExpression> classicalArguments() {
        return classicalArguments;
    }

    public List<QuantumReference> quantumArguments() {
        return quantumArguments;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableInvocationOperation operation)) {
            return false;
        }
        return Objects.equals(
            callableName,
            operation.callableName
        )
            && Objects.equals(
                target,
                operation.target
            )
            && Objects.equals(
                classicalArguments,
                operation.classicalArguments
            )
            && Objects.equals(
                quantumArguments,
                operation.quantumArguments
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            callableName,
            target,
            classicalArguments,
            quantumArguments
        );
    }
}