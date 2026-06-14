/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.syntax.ast;

import java.util.ArrayList;
import java.util.regex.Pattern;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;

/**
 * Syntax-level parser OpenQASM 3, который строит statement AST с позициями.
 */
public final class OpenQasm3AstParser {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^OPENQASM\\s+3(?:\\.0)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("^include\\s+\"([^\"]+)\"$", Pattern.CASE_INSENSITIVE);
    private static final Pattern QREG_PATTERN = Pattern.compile("^qubit(?:\\[\\d+])?\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREG_PATTERN = Pattern.compile("^bit(?:\\[\\d+])?\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPAQUE_PATTERN = Pattern.compile("^opaque\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GATE_DEFINITION_PATTERN = Pattern.compile("^gate\\s+.+\\{.*}$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s*\\(.+\\)\\s*.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEASURE_PATTERN = Pattern.compile("^(?:measure\\s+.+|.+\\s*=\\s*measure\\s+.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESET_PATTERN = Pattern.compile("^reset\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARRIER_PATTERN = Pattern.compile("^barrier\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GATE_CALL_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(?:\\(.*\\))?\\s+.+$");

    public OpenQasm3Ast parse(final String source) {
        final ArrayList<OpenQasm3AstStatement> statements = new ArrayList<>();
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        int line = 1;
        int column = 1;
        int statementLine = 1;
        int statementColumn = 1;
        int braceDepth = 0;
        int parenthesisDepth = 0;
        int bracketDepth = 0;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int i = 0; i < source.length(); i++) {
            final char current = source.charAt(i);
            final char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                continue;
            }
            if (blockComment) {
                if (
                    current == '*'
                    && next == '/'
                ) {
                    blockComment = false;
                    i++;
                    column += 2;
                } else if (current == '\n') {
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
                lineComment = true;
                i++;
                column += 2;
                continue;
            }
            if (
                current == '/'
                && next == '*'
            ) {
                blockComment = true;
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
            if (current == '(') {
                parenthesisDepth++;
            } else if (current == ')') {
                parenthesisDepth--;
                if (parenthesisDepth < 0) {
                    diagnostics.add(IntegrationDiagnostic.error(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "OpenQASM 3 source has an unexpected closing parenthesis.",
                        line,
                        column
                    ));
                    return new OpenQasm3Ast(
                        statements,
                        diagnostics
                    );
                }
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
                if (bracketDepth < 0) {
                    diagnostics.add(IntegrationDiagnostic.error(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "OpenQASM 3 source has an unexpected closing bracket.",
                        line,
                        column
                    ));
                    return new OpenQasm3Ast(
                        statements,
                        diagnostics
                    );
                }
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
                if (braceDepth < 0) {
                    diagnostics.add(IntegrationDiagnostic.error(
                        IntegrationDiagnosticCode.PARSE_ERROR,
                        "OpenQASM 3 source has an unexpected closing brace.",
                        line,
                        column
                    ));
                    return new OpenQasm3Ast(
                        statements,
                        diagnostics
                    );
                }
                builder.append(current);
                if (
                    braceDepth == 0
                    && parenthesisDepth == 0
                    && bracketDepth == 0
                ) {
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
                && parenthesisDepth == 0
                && bracketDepth == 0
            ) {
                addStatement(
                    statements,
                    builder.toString().trim(),
                    statementLine,
                    statementColumn
                );
                builder.setLength(0);
            } else if (
                !leadingWhitespace
                && current != '}'
            ) {
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
                "OpenQASM 3 source has an unclosed gate body.",
                statementLine,
                statementColumn
            ));
        }
        if (parenthesisDepth > 0) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 3 source has an unclosed parenthesis.",
                statementLine,
                statementColumn
            ));
        }
        if (bracketDepth > 0) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 3 source has an unclosed bracket.",
                statementLine,
                statementColumn
            ));
        }
        if (blockComment) {
            diagnostics.add(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.PARSE_ERROR,
                "OpenQASM 3 source has an unclosed block comment.",
                line,
                column
            ));
        }
        return new OpenQasm3Ast(
            statements,
            diagnostics
        );
    }

    private static void addStatement(
        final ArrayList<OpenQasm3AstStatement> statements,
        final String text,
        final int line,
        final int column
    ) {
        statements.add(new OpenQasm3AstStatement(
            classify(text),
            text,
            line,
            column
        ));
    }

    private static OpenQasm3AstStatementKind classify(final String text) {
        if (VERSION_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.VERSION;
        }
        if (INCLUDE_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.INCLUDE;
        }
        if (QREG_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.QREG;
        }
        if (CREG_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.CREG;
        }
        if (OPAQUE_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.OPAQUE;
        }
        if (GATE_DEFINITION_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.GATE_DEFINITION;
        }
        if (IF_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.IF;
        }
        if (MEASURE_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.MEASURE;
        }
        if (RESET_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.RESET;
        }
        if (BARRIER_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.BARRIER;
        }
        if (GATE_CALL_PATTERN.matcher(text).matches()) {
            return OpenQasm3AstStatementKind.GATE_CALL;
        }
        return OpenQasm3AstStatementKind.UNKNOWN;
    }
}