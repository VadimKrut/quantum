/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable.template;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Шаблон измерения квантового аргумента в классический аргумент.
 */
public final class CallableMeasureOperation implements CallableOperation {

    private final IdentifierName qubitName;
    private final IdentifierName classicalName;

    /**
     * Создает шаблон измерения.
     *
     * @param qubitName имя квантового аргумента
     * @param classicalName имя классического аргумента
     */
    public CallableMeasureOperation(
        final String qubitName,
        final String classicalName
    ) {
        this.qubitName = IdentifierName.of(
            qubitName,
            "Callable measurement qubit reference"
        );
        this.classicalName = IdentifierName.of(
            classicalName,
            "Callable measurement classical reference"
        );
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.MEASURE;
    }

    /**
     * Возвращает имя квантового аргумента.
     *
     * @return имя квантового аргумента
     */
    public String qubitName() {
        return qubitName.value();
    }

    /**
     * Возвращает имя классического аргумента результата.
     *
     * @return имя классического аргумента
     */
    public String classicalName() {
        return classicalName.value();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableMeasureOperation operation)) {
            return false;
        }
        return Objects.equals(qubitName, operation.qubitName)
            && Objects.equals(classicalName, operation.classicalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            qubitName,
            classicalName
        );
    }
}