/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

public final class BackendDiagnostic {

    private final BackendDiagnosticSeverity severity;
    private final BackendDiagnosticCode code;
    private final String message;

    private BackendDiagnostic(
        final BackendDiagnosticSeverity severity,
        final BackendDiagnosticCode code,
        final String message
    ) {
        if (severity == null) {
            throw new IllegalArgumentException("Backend diagnostic severity must not be null.");
        }
        if (code == null) {
            throw new IllegalArgumentException("Backend diagnostic code must not be null.");
        }
        if (
            message == null
            || message.isBlank()
        ) {
            throw new IllegalArgumentException("Backend diagnostic message must not be blank.");
        }
        this.severity = severity;
        this.code = code;
        this.message = message;
    }

    public static BackendDiagnostic error(
        final BackendDiagnosticCode code,
        final String message
    ) {
        return of(
            BackendDiagnosticSeverity.ERROR,
            code,
            message
        );
    }

    public static BackendDiagnostic warning(
        final BackendDiagnosticCode code,
        final String message
    ) {
        return of(
            BackendDiagnosticSeverity.WARNING,
            code,
            message
        );
    }

    public static BackendDiagnostic of(
        final BackendDiagnosticSeverity severity,
        final BackendDiagnosticCode code,
        final String message
    ) {
        return new BackendDiagnostic(
            severity,
            code,
            message
        );
    }

    public BackendDiagnosticSeverity severity() {
        return severity;
    }

    public BackendDiagnosticCode code() {
        return code;
    }

    public String message() {
        return message;
    }

    public boolean isError() {
        return severity == BackendDiagnosticSeverity.ERROR;
    }
}