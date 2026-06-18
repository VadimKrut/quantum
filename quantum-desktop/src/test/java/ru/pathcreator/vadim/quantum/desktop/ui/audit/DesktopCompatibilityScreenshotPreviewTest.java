/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopAction;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopWorkflowResult;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrWorkspaceService;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

class DesktopCompatibilityScreenshotPreviewTest {

    private final DesktopIrWorkspaceService workspaceService = new DesktopIrWorkspaceService();
    private final DesktopCompatibilityScreenshotPreview preview = new DesktopCompatibilityScreenshotPreview(workspaceService);

    @Test
    void rendersPreflightStatusesForAllSupportedFormats() {
        final List<DesktopIrOperationSpec> operations = List.of(
            new DesktopIrOperationSpec("H", "q[0]", "q[1]", "q[0]", "c[0]", 0.0),
            new DesktopIrOperationSpec("CX", "q[0]", "q[1]", "q[0]", "c[0]", 0.0),
            new DesktopIrOperationSpec("MEASURE", "q[0]", "q[1]", "q[0]", "c[0]", 0.0),
            new DesktopIrOperationSpec("MEASURE", "q[1]", "q[1]", "q[0]", "c[1]", 0.0)
        );
        final QuantumProgram program = workspaceService.buildProgram(
            "main",
            "q",
            2,
            "c",
            2,
            operations
        );

        final DesktopWorkflowResult result = preview.preview(
            program,
            operations.size()
        );

        assertEquals(DesktopAction.COMPATIBILITY, result.action());
        assertEquals("COMPATIBLE", result.status());
        assertTrue(result.content().contains("OPENQASM_2 -> EXPORTABLE diagnostics=0"));
        assertTrue(result.content().contains("OPENQASM_3 -> EXPORTABLE diagnostics=0"));
        assertTrue(result.content().contains("QUIL -> EXPORTABLE diagnostics=0"));
        assertTrue(result.content().contains("program operations: 4"));
    }
}