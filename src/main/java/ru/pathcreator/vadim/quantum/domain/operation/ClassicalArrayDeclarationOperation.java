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
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Объявление классического массива в универсальном IR.
 */
public final class ClassicalArrayDeclarationOperation implements Operation {

    private final IdentifierName name;
    private final ClassicalType elementType;
    private final List<ClassicalExpression> dimensions;
    private final String initializerText;

    public ClassicalArrayDeclarationOperation(
        final String name,
        final ClassicalType elementType,
        final List<ClassicalExpression> dimensions,
        final String initializerText
    ) {
        if (elementType == null) {
            throw new IllegalArgumentException("Classical array element type must not be null.");
        }
        if (
            dimensions == null
            || dimensions.isEmpty()
        ) {
            throw new IllegalArgumentException("Classical array dimensions must not be empty.");
        }
        final ArrayList<ClassicalExpression> copiedDimensions = new ArrayList<>();
        for (int i = 0; i < dimensions.size(); i++) {
            if (dimensions.get(i) == null) {
                throw new IllegalArgumentException("Classical array dimension must not be null.");
            }
            copiedDimensions.add(dimensions.get(i));
        }
        this.name = IdentifierName.of(
            name,
            "Classical array"
        );
        this.elementType = elementType;
        this.dimensions = List.copyOf(copiedDimensions);
        this.initializerText = initializerText == null || initializerText.isBlank()
            ? null
            : initializerText.trim();
    }

    @Override
    public OperationKind kind() {
        return OperationKind.CLASSICAL_ARRAY_DECLARATION;
    }

    public String name() {
        return name.value();
    }

    public ClassicalType elementType() {
        return elementType;
    }

    public int dimensionCount() {
        return dimensions.size();
    }

    public ClassicalExpression dimension(final int index) {
        if (
            index < 0
            || index >= dimensions.size()
        ) {
            throw new IllegalArgumentException("Classical array dimension index is outside of bounds.");
        }
        return dimensions.get(index);
    }

    public List<ClassicalExpression> dimensions() {
        return dimensions;
    }

    public boolean hasInitializerText() {
        return initializerText != null;
    }

    public String initializerText() {
        if (initializerText == null) {
            throw new IllegalStateException("Classical array declaration does not have initializer.");
        }
        return initializerText;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalArrayDeclarationOperation operation)) {
            return false;
        }
        return Objects.equals(
            name,
            operation.name
        )
            && Objects.equals(
                elementType,
                operation.elementType
            )
            && Objects.equals(
                dimensions,
                operation.dimensions
            )
            && Objects.equals(
                initializerText,
                operation.initializerText
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            elementType,
            dimensions,
            initializerText
        );
    }
}