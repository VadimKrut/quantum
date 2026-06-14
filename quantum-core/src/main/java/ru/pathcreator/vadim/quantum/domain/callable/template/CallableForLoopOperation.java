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
 * Шаблон цикла по целочисленному диапазону.
 */
public final class CallableForLoopOperation implements CallableOperation {

    private final IdentifierName variableName;
    private final long startInclusive;
    private final long step;
    private final long endInclusive;
    private final CallableOperationBlock body;

    /**
     * Создает шаблон цикла по диапазону.
     *
     * @param variableName имя переменной цикла
     * @param startInclusive начало диапазона
     * @param step шаг диапазона
     * @param endInclusive конец диапазона включительно
     * @param body тело цикла
     */
    public CallableForLoopOperation(
        final String variableName,
        final long startInclusive,
        final long step,
        final long endInclusive,
        final CallableOperationBlock body
    ) {
        if (step == 0L) {
            throw new IllegalArgumentException("Callable for-loop step must not be zero.");
        }
        if (body == null) {
            throw new IllegalArgumentException("Callable for-loop body must not be null.");
        }
        this.variableName = IdentifierName.of(
            variableName,
            "Callable for-loop variable"
        );
        this.startInclusive = startInclusive;
        this.step = step;
        this.endInclusive = endInclusive;
        this.body = body;
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.FOR_LOOP;
    }

    /**
     * Возвращает имя переменной цикла.
     *
     * @return имя переменной
     */
    public String variableName() {
        return variableName.value();
    }

    /**
     * Возвращает начало диапазона.
     *
     * @return начало диапазона
     */
    public long startInclusive() {
        return startInclusive;
    }

    /**
     * Возвращает шаг диапазона.
     *
     * @return шаг
     */
    public long step() {
        return step;
    }

    /**
     * Возвращает конец диапазона включительно.
     *
     * @return конец диапазона
     */
    public long endInclusive() {
        return endInclusive;
    }

    /**
     * Возвращает тело цикла.
     *
     * @return тело цикла
     */
    public CallableOperationBlock body() {
        return body;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableForLoopOperation operation)) {
            return false;
        }
        return startInclusive == operation.startInclusive
            && step == operation.step
            && endInclusive == operation.endInclusive
            && Objects.equals(variableName, operation.variableName)
            && Objects.equals(body, operation.body);
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