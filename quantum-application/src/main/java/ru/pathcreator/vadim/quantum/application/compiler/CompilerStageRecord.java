/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compiler;

import java.util.Objects;

/**
 * Immutable запись о выполнении одного этапа compiler pipeline.
 */
public final class CompilerStageRecord {

    private final CompilerStage stage;
    private final CompilerStageStatus status;
    private final long elapsedNanos;
    private final String message;

    private CompilerStageRecord(
        final CompilerStage stage,
        final CompilerStageStatus status,
        final long elapsedNanos,
        final String message
    ) {
        this.stage = stage;
        this.status = status;
        this.elapsedNanos = elapsedNanos;
        this.message = message;
    }

    public static CompilerStageRecord of(
        final CompilerStage stage,
        final CompilerStageStatus status,
        final long elapsedNanos,
        final String message
    ) {
        if (stage == null) {
            throw new IllegalArgumentException("Compiler stage must not be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Compiler stage status must not be null.");
        }
        if (elapsedNanos < 0L) {
            throw new IllegalArgumentException("Compiler stage elapsed nanos must not be negative.");
        }
        if (
            message == null
            || message.isBlank()
        ) {
            throw new IllegalArgumentException("Compiler stage message must not be blank.");
        }
        return new CompilerStageRecord(
            stage,
            status,
            elapsedNanos,
            message
        );
    }

    public CompilerStage stage() {
        return stage;
    }

    public CompilerStageStatus status() {
        return status;
    }

    public long elapsedNanos() {
        return elapsedNanos;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CompilerStageRecord record)) {
            return false;
        }
        return elapsedNanos == record.elapsedNanos
            && stage == record.stage
            && status == record.status
            && Objects.equals(
                message,
                record.message
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            stage,
            status,
            elapsedNanos,
            message
        );
    }
}