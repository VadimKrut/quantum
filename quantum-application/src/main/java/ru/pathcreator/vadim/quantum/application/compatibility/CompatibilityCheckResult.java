/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compatibility;

/**
 * Результат одного шага проверки совместимости.
 */
public final class CompatibilityCheckResult {

    private final String name;
    private final CompatibilityCheckStatus status;
    private final String message;
    private final int diagnosticCount;
    private final long elapsedNanos;

    private CompatibilityCheckResult(
        final String name,
        final CompatibilityCheckStatus status,
        final String message,
        final int diagnosticCount,
        final long elapsedNanos
    ) {
        this.name = name;
        this.status = status;
        this.message = message;
        this.diagnosticCount = diagnosticCount;
        this.elapsedNanos = elapsedNanos;
    }

    /**
     * Создает immutable результат проверки.
     *
     * @param name стабильное имя проверки
     * @param status статус проверки
     * @param message краткое объяснение результата
     * @param diagnosticCount количество diagnostic-сообщений
     * @param elapsedNanos длительность проверки
     * @return результат проверки
     */
    public static CompatibilityCheckResult of(
        final String name,
        final CompatibilityCheckStatus status,
        final String message,
        final int diagnosticCount,
        final long elapsedNanos
    ) {
        if (
            name == null
            || name.isBlank()
        ) {
            throw new IllegalArgumentException("Compatibility check name must not be blank.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Compatibility check status must not be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException("Compatibility check message must not be null.");
        }
        if (diagnosticCount < 0) {
            throw new IllegalArgumentException("Compatibility check diagnostic count must not be negative.");
        }
        if (elapsedNanos < 0L) {
            throw new IllegalArgumentException("Compatibility check elapsed nanos must not be negative.");
        }
        return new CompatibilityCheckResult(
            name,
            status,
            message,
            diagnosticCount,
            elapsedNanos
        );
    }

    public String name() {
        return name;
    }

    public CompatibilityCheckStatus status() {
        return status;
    }

    public boolean isSuccess() {
        return status == CompatibilityCheckStatus.SUCCESS
            || status == CompatibilityCheckStatus.WARNING;
    }

    public String message() {
        return message;
    }

    public int diagnosticCount() {
        return diagnosticCount;
    }

    public long elapsedNanos() {
        return elapsedNanos;
    }
}