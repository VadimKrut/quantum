/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.persistence.diagnostic;

import java.util.Objects;

/**
 * Одно диагностическое сообщение родного persistence-формата Quantum IR.
 */
public final class PersistenceDiagnostic {

    /**
     * Уровень серьезности.
     */
    private final PersistenceDiagnosticSeverity severity;

    /**
     * Стабильный код диагностики.
     */
    private final PersistenceDiagnosticCode code;

    /**
     * Человекочитаемое описание.
     */
    private final String message;

    private PersistenceDiagnostic(
        final PersistenceDiagnosticSeverity severity,
        final PersistenceDiagnosticCode code,
        final String message
    ) {
        if (severity == null) {
            throw new IllegalArgumentException("Persistence diagnostic severity must not be null.");
        }
        if (code == null) {
            throw new IllegalArgumentException("Persistence diagnostic code must not be null.");
        }
        if (
            message == null
            || message.isBlank()
        ) {
            throw new IllegalArgumentException("Persistence diagnostic message must not be blank.");
        }
        this.severity = severity;
        this.code = code;
        this.message = message;
    }

    /**
     * Создает ошибку.
     *
     * @param code код диагностики
     * @param message описание
     * @return диагностика ошибки
     */
    public static PersistenceDiagnostic error(
        final PersistenceDiagnosticCode code,
        final String message
    ) {
        return new PersistenceDiagnostic(
            PersistenceDiagnosticSeverity.ERROR,
            code,
            message
        );
    }

    /**
     * Создает предупреждение.
     *
     * @param code код диагностики
     * @param message описание
     * @return диагностика предупреждения
     */
    public static PersistenceDiagnostic warning(
        final PersistenceDiagnosticCode code,
        final String message
    ) {
        return new PersistenceDiagnostic(
            PersistenceDiagnosticSeverity.WARNING,
            code,
            message
        );
    }

    public PersistenceDiagnosticSeverity severity() {
        return severity;
    }

    public PersistenceDiagnosticCode code() {
        return code;
    }

    public String message() {
        return message;
    }

    public boolean isError() {
        return severity == PersistenceDiagnosticSeverity.ERROR;
    }

    public boolean isWarning() {
        return severity == PersistenceDiagnosticSeverity.WARNING;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PersistenceDiagnostic diagnostic)) {
            return false;
        }
        return severity == diagnostic.severity
            && code == diagnostic.code
            && Objects.equals(
                message,
                diagnostic.message
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            severity,
            code,
            message
        );
    }
}