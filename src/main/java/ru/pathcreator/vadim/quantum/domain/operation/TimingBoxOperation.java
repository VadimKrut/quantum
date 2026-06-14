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

import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;

/**
 * Временной box-блок с необязательной длительностью.
 */
public final class TimingBoxOperation implements Operation {

    private final DurationExpression duration;
    private final OperationBlock body;

    public TimingBoxOperation(
        final DurationExpression duration,
        final OperationBlock body
    ) {
        if (body == null) {
            throw new IllegalArgumentException("Timing box body must not be null.");
        }
        this.duration = duration;
        this.body = body;
    }

    @Override
    public OperationKind kind() {
        return OperationKind.TIMING_BOX;
    }

    public boolean hasDuration() {
        return duration != null;
    }

    public DurationExpression duration() {
        if (duration == null) {
            throw new IllegalStateException("Timing box does not have explicit duration.");
        }
        return duration;
    }

    public OperationBlock body() {
        return body;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimingBoxOperation operation)) {
            return false;
        }
        return Objects.equals(
            duration,
            operation.duration
        )
            && Objects.equals(
                body,
                operation.body
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            duration,
            body
        );
    }
}