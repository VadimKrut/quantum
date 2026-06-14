/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.ast;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;

/**
 * Syntax-level AST OpenQASM 2 source до построения Quantum IR.
 */
public final class OpenQasm2Ast {

    private final List<OpenQasm2AstStatement> statements;
    private final List<IntegrationDiagnostic> diagnostics;

    public OpenQasm2Ast(
        final List<OpenQasm2AstStatement> statements,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        if (statements == null) {
            throw new IllegalArgumentException("OpenQASM 2 AST statements must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("OpenQASM 2 AST diagnostics must not be null.");
        }
        this.statements = List.copyOf(statements);
        this.diagnostics = List.copyOf(diagnostics);
    }

    public List<OpenQasm2AstStatement> statements() {
        return statements;
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