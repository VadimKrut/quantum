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
 * Снимок IR-программы, которую можно показать и редактировать в графическом рабочем пространстве.
 */
public record DesktopIrProgramSnapshot(
    String circuitName,
    String quantumRegisterName,
    int quantumRegisterSize,
    String classicalRegisterName,
    int classicalRegisterSize,
    List<DesktopIrOperationSpec> operations,
    List<String> diagnostics
) {

    public DesktopIrProgramSnapshot {
        operations = List.copyOf(operations);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean isComplete() {
        return diagnostics.isEmpty();
    }
}