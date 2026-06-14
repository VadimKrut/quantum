/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.persistence.result;

import java.nio.file.Path;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnostic;

/**
 * Результат потоковой записи Quantum IR в файл без удержания полного текста результата в памяти.
 */
public final class QuantumIrFileWriteResult {

    private final Path path;
    private final List<PersistenceDiagnostic> diagnostics;

    private QuantumIrFileWriteResult(
        final Path path,
        final List<PersistenceDiagnostic> diagnostics
    ) {
        validate(
            path,
            diagnostics
        );
        this.path = path;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public static QuantumIrFileWriteResult success(
        final Path path,
        final List<PersistenceDiagnostic> diagnostics
    ) {
        return new QuantumIrFileWriteResult(
            path,
            diagnostics
        );
    }

    public static QuantumIrFileWriteResult failure(final List<PersistenceDiagnostic> diagnostics) {
        return new QuantumIrFileWriteResult(
            null,
            diagnostics
        );
    }

    public boolean isSuccess() {
        return path != null && !hasErrors();
    }

    public boolean hasPath() {
        return path != null;
    }

    public Path path() {
        if (path == null) {
            throw new IllegalStateException("Quantum IR file write result does not contain path.");
        }
        return path;
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
        final Path path,
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
            path == null
            && !containsError(diagnostics)
        ) {
            throw new IllegalArgumentException("Failed file write result must contain at least one error diagnostic.");
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