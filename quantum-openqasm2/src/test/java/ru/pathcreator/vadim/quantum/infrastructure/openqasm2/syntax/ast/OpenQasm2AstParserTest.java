/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax.ast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenQasm2AstParserTest {

    @Test
    void parsesTopLevelStatementsWithKindsAndLocations() {
        final OpenQasm2Ast ast = new OpenQasm2AstParser().parse("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            h q[0];
            """);

        assertFalse(ast.hasErrors());
        assertEquals(
            4,
            ast.statements().size()
        );
        assertEquals(
            OpenQasm2AstStatementKind.VERSION,
            ast.statements().get(0).kind()
        );
        assertEquals(
            OpenQasm2AstStatementKind.GATE_CALL,
            ast.statements().get(3).kind()
        );
        assertEquals(
            4,
            ast.statements().get(3).line()
        );
    }
}