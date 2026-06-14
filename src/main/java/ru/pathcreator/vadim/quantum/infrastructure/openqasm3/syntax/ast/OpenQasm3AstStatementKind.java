/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.syntax.ast;

/**
 * Тип top-level statement OpenQASM 3.
 */
public enum OpenQasm3AstStatementKind {

    VERSION,
    INCLUDE,
    QREG,
    CREG,
    OPAQUE,
    GATE_DEFINITION,
    IF,
    MEASURE,
    RESET,
    BARRIER,
    GATE_CALL,
    UNKNOWN
}