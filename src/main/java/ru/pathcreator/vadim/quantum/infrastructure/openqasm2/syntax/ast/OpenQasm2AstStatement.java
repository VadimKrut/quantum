/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.ast;

/**
 * Один top-level statement OpenQASM 2 AST.
 */
public final class OpenQasm2AstStatement {

    private final OpenQasm2AstStatementKind kind;
    private final String text;
    private final int line;
    private final int column;

    public OpenQasm2AstStatement(
        final OpenQasm2AstStatementKind kind,
        final String text,
        final int line,
        final int column
    ) {
        if (kind == null) {
            throw new IllegalArgumentException("OpenQASM 2 AST statement kind must not be null.");
        }
        if (text == null) {
            throw new IllegalArgumentException("OpenQASM 2 AST statement text must not be null.");
        }
        this.kind = kind;
        this.text = text;
        this.line = line;
        this.column = column;
    }

    public OpenQasm2AstStatementKind kind() {
        return kind;
    }

    public String text() {
        return text;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }
}