/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.ast;

import java.util.ArrayList;
import java.util.regex.Pattern;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;

/**
 * Syntax-level parser OpenQASM 2, который строит statement AST с позициями.
 */
public final class OpenQasm2AstParser {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^OPENQASM\\s+2(?:\\.0)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("^include\\s+\"([^\"]+)\"$", Pattern.CASE_INSENSITIVE);
    private static final Pattern QREG_PATTERN = Pattern.compile("^qreg\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREG_PATTERN = Pattern.compile("^creg\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPAQUE_PATTERN = Pattern.compile("^opaque\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GATE_DEFINITION_PATTERN = Pattern.compile("^gate\\s+.+\\{.*}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s*\\(.+\\)\\s*.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEASURE_PATTERN = Pattern.compile("^measure\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESET_PATTERN = Pattern.compile("^reset\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARRIER_PATTERN = Pattern.compile("^barrier\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GATE_CALL_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(?:\\(.*\\))?\\s+.+$");

    public OpenQasm2Ast parse(final String source) {
        final ArrayList<OpenQasm2AstStatement> statements = new ArrayList<>();
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        int line = 1;
        int column = 1;
        int statementLine = 1;
        int statementColumn = 1;
        int braceDepth = 0;
        boolean comment = false;
        for (int i = 0; i < source.length(); i++) {
            final char current = source.charAt(i);
            final char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (comment) {
                if (current == '\n') {
                    comment = false;
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                continue;
            }
            if (
                current == '/'
                && next == '/'
            ) {
                comment = true;
                i++;
                column += 2;
                continue;
            }
            if (
                builder.length() == 0
                && !Character.isWhitespace(current)
            ) {
                statementLine = line;
                statementColumn = column;
            }
            final boolean leadingWhitespace = builder.length() == 0
                && Character.isWhitespace(current);
            if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
                if (braceDepth < 0) {
                    diagnostics.add(IntegrationDiagnostic.error(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "OpenQASM 2 source has an unexpected closing brace.",
                        line,
                        column
                    ));
                    return new OpenQasm2Ast(
                        statements,
                        diagnostics
                    );
                }
                builder.append(current);
                if (braceDepth == 0) {
                    addStatement(
                        statements,
                        builder.toString().trim(),
                        statementLine,
                        statementColumn
                    );
                    builder.setLength(0);
                    column++;
                    continue;
                }
            }
            if (
                current == ';'
                && braceDepth == 0
            ) {
                addStatement(
                    statements,
                    builder.toString().trim(),
                    statementLine,
                    statementColumn
                );
                builder.setLength(0);
            } else if (!leadingWhitespace) {
                builder.append(current);
            }
            if (current == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        if (!builder.toString().isBlank()) {
            addStatement(
                statements,
                builder.toString().trim(),
                statementLine,
                statementColumn
            );
        }
        if (braceDepth > 0) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 2 source has an unclosed gate body.",
                statementLine,
                statementColumn
            ));
        }
        return new OpenQasm2Ast(
            statements,
            diagnostics
        );
    }

    private static void addStatement(
        final ArrayList<OpenQasm2AstStatement> statements,
        final String text,
        final int line,
        final int column
    ) {
        statements.add(new OpenQasm2AstStatement(
            classify(text),
            text,
            line,
            column
        ));
    }

    private static OpenQasm2AstStatementKind classify(final String text) {
        if (VERSION_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.VERSION;
        }
        if (INCLUDE_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.INCLUDE;
        }
        if (QREG_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.QREG;
        }
        if (CREG_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.CREG;
        }
        if (OPAQUE_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.OPAQUE;
        }
        if (GATE_DEFINITION_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.GATE_DEFINITION;
        }
        if (IF_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.IF;
        }
        if (MEASURE_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.MEASURE;
        }
        if (RESET_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.RESET;
        }
        if (BARRIER_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.BARRIER;
        }
        if (GATE_CALL_PATTERN.matcher(text).matches()) {
            return OpenQasm2AstStatementKind.GATE_CALL;
        }
        return OpenQasm2AstStatementKind.UNKNOWN;
    }
}