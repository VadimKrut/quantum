/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.syntax;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Результат parser Quil.
 */
public final class QuilParserResult {

    private final QuantumProgram program;
    private final List<IntegrationDiagnostic> diagnostics;

    public QuilParserResult(
        final QuantumProgram program,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("Quil parser diagnostics must not be null.");
        }
        this.program = program;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public QuantumProgram program() {
        if (program == null) {
            throw new IllegalStateException("Quil parser result does not contain a program.");
        }
        return program;
    }

    public List<IntegrationDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean hasErrors() {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }
}