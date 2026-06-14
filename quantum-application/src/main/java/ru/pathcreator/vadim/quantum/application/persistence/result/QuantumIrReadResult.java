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
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Результат чтения Quantum IR из родного текстового формата.
 */
public final class QuantumIrReadResult {

    private final QuantumProgram program;
    private final List<PersistenceDiagnostic> diagnostics;

    private QuantumIrReadResult(
        final QuantumProgram program,
        final List<PersistenceDiagnostic> diagnostics
    ) {
        validate(
            program,
            diagnostics
        );
        this.program = program;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public static QuantumIrReadResult success(final QuantumProgram program) {
        return new QuantumIrReadResult(
            program,
            List.of()
        );
    }

    public static QuantumIrReadResult success(
        final QuantumProgram program,
        final List<PersistenceDiagnostic> diagnostics
    ) {
        return new QuantumIrReadResult(
            program,
            diagnostics
        );
    }

    public static QuantumIrReadResult failure(final List<PersistenceDiagnostic> diagnostics) {
        return new QuantumIrReadResult(
            null,
            diagnostics
        );
    }

    public boolean isSuccess() {
        return program != null && !hasErrors();
    }

    public boolean hasProgram() {
        return program != null;
    }

    public QuantumProgram program() {
        if (program == null) {
            throw new IllegalStateException("Quantum IR read result does not contain a program.");
        }
        return program;
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
        final QuantumProgram program,
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
            program == null
            && !containsError(diagnostics)
        ) {
            throw new IllegalArgumentException("Failed read result must contain at least one error diagnostic.");
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