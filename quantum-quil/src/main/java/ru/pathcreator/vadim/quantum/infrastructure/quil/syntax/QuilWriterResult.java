/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.syntax;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;

/**
 * Результат writer Quil.
 */
public final class QuilWriterResult {

    private final String content;
    private final IntegrationDiagnostic diagnostic;

    private QuilWriterResult(
        final String content,
        final IntegrationDiagnostic diagnostic
    ) {
        this.content = content;
        this.diagnostic = diagnostic;
    }

    public static QuilWriterResult success(final String content) {
        if (content == null) {
            throw new IllegalArgumentException("Quil writer content must not be null.");
        }
        return new QuilWriterResult(
            content,
            null
        );
    }

    public static QuilWriterResult failure(final IntegrationDiagnostic diagnostic) {
        if (diagnostic == null) {
            throw new IllegalArgumentException("Quil writer diagnostic must not be null.");
        }
        return new QuilWriterResult(
            null,
            diagnostic
        );
    }

    public boolean isSuccess() {
        return content != null;
    }

    public String content() {
        if (content == null) {
            throw new IllegalStateException("Quil writer result does not contain content.");
        }
        return content;
    }

    public IntegrationDiagnostic diagnostic() {
        if (diagnostic == null) {
            throw new IllegalStateException("Quil writer result does not contain diagnostic.");
        }
        return diagnostic;
    }
}