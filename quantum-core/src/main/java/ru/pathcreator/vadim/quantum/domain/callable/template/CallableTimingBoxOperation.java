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

import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;

/**
 * Шаблон временного box-блока.
 */
public final class CallableTimingBoxOperation implements CallableOperation {

    private final DurationExpression duration;
    private final CallableOperationBlock body;

    /**
     * Создает шаблон timing box.
     *
     * @param duration длительность блока или null
     * @param body тело блока
     */
    public CallableTimingBoxOperation(
        final DurationExpression duration,
        final CallableOperationBlock body
    ) {
        if (body == null) {
            throw new IllegalArgumentException("Callable timing box body must not be null.");
        }
        this.duration = duration;
        this.body = body;
    }

    @Override
    public CallableOperationKind kind() {
        return CallableOperationKind.TIMING_BOX;
    }

    /**
     * Проверяет наличие явной длительности.
     *
     * @return true, если длительность задана
     */
    public boolean hasDuration() {
        return duration != null;
    }

    /**
     * Возвращает явную длительность.
     *
     * @return длительность
     */
    public DurationExpression duration() {
        if (duration == null) {
            throw new IllegalStateException("Callable timing box does not have explicit duration.");
        }
        return duration;
    }

    /**
     * Возвращает тело блока.
     *
     * @return тело блока
     */
    public CallableOperationBlock body() {
        return body;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableTimingBoxOperation operation)) {
            return false;
        }
        return Objects.equals(duration, operation.duration)
            && Objects.equals(body, operation.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            duration,
            body
        );
    }
}