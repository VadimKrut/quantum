/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.persistence.result;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnostic;

/**
 * Результат записи Quantum IR в родной текстовый формат.
 */
public final class QuantumIrWriteResult {

    private final String content;
    private final List<PersistenceDiagnostic> diagnostics;

    private QuantumIrWriteResult(
        final String content,
        final List<PersistenceDiagnostic> diagnostics
    ) {
        validate(
            content,
            diagnostics
        );
        this.content = content;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public static QuantumIrWriteResult success(final String content) {
        return new QuantumIrWriteResult(
            content,
            List.of()
        );
    }

    public static QuantumIrWriteResult success(
        final String content,
        final List<PersistenceDiagnostic> diagnostics
    ) {
        return new QuantumIrWriteResult(
            content,
            diagnostics
        );
    }

    public static QuantumIrWriteResult failure(final List<PersistenceDiagnostic> diagnostics) {
        return new QuantumIrWriteResult(
            null,
            diagnostics
        );
    }

    public boolean isSuccess() {
        return content != null && !hasErrors();
    }

    public boolean hasContent() {
        return content != null;
    }

    public String content() {
        if (content == null) {
            throw new IllegalStateException("Quantum IR write result does not contain content.");
        }
        return content;
    }

    public boolean hasErrors() {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }

    public List<PersistenceDiagnostic> diagnostics() {
        return diagnostics;
    }

    private static void validate(
        final String content,
        final List<PersistenceDiagnostic> diagnostics
    ) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("Persistence diagnostics must not be null.");
        }
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i) == null) {
                throw new IllegalArgumentException("Persistence diagnostic must not be null.");
            }
        }
        if (
            content == null
            && !containsError(diagnostics)
        ) {
            throw new IllegalArgumentException("Failed write result must contain at least one error diagnostic.");
        }
    }

    private static boolean containsError(final List<PersistenceDiagnostic> diagnostics) {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }
}