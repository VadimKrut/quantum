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

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;

/**
 * Локальное классическое объявление внутри потока операций.
 */
public final class ClassicalDeclarationOperation implements Operation {

    private final ClassicalDeclaration declaration;
    private final ClassicalExpression initializer;

    public ClassicalDeclarationOperation(
        final ClassicalDeclaration declaration,
        final ClassicalExpression initializer
    ) {
        if (declaration == null) {
            throw new IllegalArgumentException("Classical declaration must not be null.");
        }
        this.declaration = declaration;
        this.initializer = initializer;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.CLASSICAL_DECLARATION;
    }

    public ClassicalDeclaration declaration() {
        return declaration;
    }

    public boolean hasInitializer() {
        return initializer != null;
    }

    public ClassicalExpression initializer() {
        if (initializer == null) {
            throw new IllegalStateException("Classical declaration does not have initializer.");
        }
        return initializer;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalDeclarationOperation operation)) {
            return false;
        }
        return Objects.equals(
            declaration,
            operation.declaration
        )
            && Objects.equals(
                initializer,
                operation.initializer
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            declaration,
            initializer
        );
    }
}