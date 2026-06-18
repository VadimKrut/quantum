/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DesktopJavaDslImporterTest {

    private final DesktopIrWorkspaceService workspaceService = new DesktopIrWorkspaceService();
    private final DesktopJavaDslImporter importer = new DesktopJavaDslImporter();

    @Test
    void importsSavedDesktopJavaDslBackToVisualWorkspace() {
        final List<DesktopIrOperationSpec> operations = List.of(
            new DesktopIrOperationSpec("H", "q[0]", "q[0]", "q[0]", "c[0]", Math.PI / 2.0),
            new DesktopIrOperationSpec("RX", "q[1]", "q[1]", "q[1]", "c[0]", Math.PI / 3.0),
            new DesktopIrOperationSpec("U", "q[2]", "q[2]", "q[2]", "c[0]", 0.1, 0.2, 0.3, 20.0, "NS", "entry"),
            new DesktopIrOperationSpec("CX", "q[0]", "q[1]", "q[0]", "c[0]", Math.PI / 2.0),
            new DesktopIrOperationSpec("CCX", "q[0]", "q[1]", "q[2]", "c[0]", Math.PI / 2.0),
            new DesktopIrOperationSpec("MEASURE", "q[2]", "q[2]", "q[2]", "c[2]", Math.PI / 2.0),
            new DesktopIrOperationSpec("DELAY", "q[0]", "q[2]", "q[0]", "c[0]", Math.PI / 2.0, 0.0, 0.0, 25.0, "NS", "entry"),
            new DesktopIrOperationSpec("LABEL", "q[0]", "q[0]", "q[0]", "c[0]", 0.0, 0.0, 0.0, 20.0, "NS", "entry_label")
        );
        final String code = workspaceService.generateJavaDsl(
            "loaded",
            "q",
            3,
            "c",
            3,
            operations
        );

        final DesktopJavaDslImportResult result = importer.importDsl(code);

        assertTrue(result.isSuccess(), String.join("\n", result.diagnostics()));
        assertEquals("loaded", result.circuitName());
        assertEquals("q", result.quantumRegisterName());
        assertEquals(3, result.quantumRegisterSize());
        assertEquals("c", result.classicalRegisterName());
        assertEquals(3, result.classicalRegisterSize());
        assertEquals(operations.size(), result.operations().size());
        assertEquals("H", result.operations().get(0).gate());
        assertEquals("RX", result.operations().get(1).gate());
        assertEquals(Math.PI / 3.0, result.operations().get(1).angle());
        assertEquals("U", result.operations().get(2).gate());
        assertEquals(0.2, result.operations().get(2).secondAngle());
        assertEquals("CCX", result.operations().get(4).gate());
        assertEquals("c[2]", result.operations().get(5).classicalBit());
        assertEquals(25.0, result.operations().get(6).durationValue());
        assertEquals("entry_label", result.operations().get(7).labelName());
    }

    @Test
    void reportsUnsupportedJavaCodeWithoutExecutingIt() {
        final DesktopJavaDslImportResult result = importer.importDsl("""
            final QuantumProgram program = Quantum.programBuilder()
                .circuit("bad")
                .qreg("q", 1)
                .creg("c", 1)
                .someArbitraryJavaMethod()
                .build();
            """);

        assertEquals(1, result.diagnostics().size());
        assertTrue(result.diagnostics().get(0).contains("Unsupported saved Java DSL line"));
    }
}