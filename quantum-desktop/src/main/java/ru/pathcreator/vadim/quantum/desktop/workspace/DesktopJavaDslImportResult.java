/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

import java.util.List;

/**
 * Результат загрузки Java DSL, сохраненного desktop-студией.
 */
public record DesktopJavaDslImportResult(
    String circuitName,
    String quantumRegisterName,
    int quantumRegisterSize,
    String classicalRegisterName,
    int classicalRegisterSize,
    List<DesktopIrOperationSpec> operations,
    List<String> diagnostics
) {

    public DesktopJavaDslImportResult {
        operations = operations == null
            ? List.of()
            : List.copyOf(operations);
        diagnostics = diagnostics == null
            ? List.of()
            : List.copyOf(diagnostics);
    }

    public boolean isSuccess() {
        return diagnostics.isEmpty();
    }
}