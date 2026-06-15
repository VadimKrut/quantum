/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.cli;

import java.util.List;
import java.util.Map;

import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

final class ProgramLoadResult {

    private final QuantumProgram program;
    private final List<Map<String, Object>> diagnostics;

    private ProgramLoadResult(
        final QuantumProgram program,
        final List<Map<String, Object>> diagnostics
    ) {
        this.program = program;
        this.diagnostics = List.copyOf(diagnostics);
    }

    static ProgramLoadResult success(final QuantumProgram program) {
        return new ProgramLoadResult(
            program,
            List.of()
        );
    }

    static ProgramLoadResult failure(final List<Map<String, Object>> diagnostics) {
        return new ProgramLoadResult(
            null,
            diagnostics
        );
    }

    boolean success() {
        return program != null;
    }

    QuantumProgram program() {
        if (program == null) {
            throw new IllegalStateException("Program load result does not contain a program.");
        }
        return program;
    }

    List<Map<String, Object>> diagnostics() {
        return diagnostics;
    }
}