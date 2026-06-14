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

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Цикл по целочисленному диапазону.
 */
public final class ForLoopOperation implements Operation {

    /**
     * Имя переменной цикла.
     */
    private final IdentifierName variableName;

    /**
     * Начальное значение диапазона.
     */
    private final long startInclusive;

    /**
     * Шаг диапазона.
     */
    private final long step;

    /**
     * Конечное значение диапазона включительно.
     */
    private final long endInclusive;

    /**
     * Тело цикла.
     */
    private final OperationBlock body;

    /**
     * Создает цикл по диапазону.
     *
     * @param variableName имя переменной цикла
     * @param startInclusive начало диапазона
     * @param step шаг диапазона
     * @param endInclusive конец диапазона
     * @param body тело цикла
     */
    public ForLoopOperation(
        final String variableName,
        final long startInclusive,
        final long step,
        final long endInclusive,
        final OperationBlock body
    ) {
        if (step == 0L) {
            throw new IllegalArgumentException("For-loop step must not be zero.");
        }
        if (body == null) {
            throw new IllegalArgumentException("For-loop body must not be null.");
        }
        this.variableName = IdentifierName.of(
            variableName,
            "For-loop variable"
        );
        this.startInclusive = startInclusive;
        this.step = step;
        this.endInclusive = endInclusive;
        this.body = body;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.FOR_LOOP;
    }

    public String variableName() {
        return variableName.value();
    }

    public long startInclusive() {
        return startInclusive;
    }

    public long step() {
        return step;
    }

    public long endInclusive() {
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
        if (!(other instanceof ForLoopOperation operation)) {
            return false;
        }
        return startInclusive == operation.startInclusive
            && step == operation.step
            && endInclusive == operation.endInclusive
            && Objects.equals(
                variableName,
                operation.variableName
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
            startInclusive,
            step,
            endInclusive,
            body
        );
    }
}