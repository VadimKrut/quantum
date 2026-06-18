/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

class DesktopDiagnosticListRendererTest {

    private final DesktopDiagnosticListRenderer renderer = new DesktopDiagnosticListRenderer();

    @Test
    void rendersOperationNavigationItemsForSmallVisualPrograms() {
        final List<String> items = renderer.render(
            ValidationResult.valid(),
            IntegrationFormat.OPENQASM_3,
            "EXPORTABLE",
            0,
            false,
            false,
            List.of(new DesktopIrOperationSpec(
                "H",
                "q[0]",
                "q[1]",
                "q[0]",
                "c[0]",
                0.5
            ))
        );

        assertTrue(items.contains("VALIDATION OK"));
        assertTrue(items.contains("PREFLIGHT OPENQASM_3 EXPORTABLE diagnostics=0"));
        boolean hasOperation = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).startsWith("OP #0 H")) {
                hasOperation = true;
            }
        }
        assertTrue(hasOperation);
    }

    @Test
    void rendersDeferredAndJsonStatesWithoutOperationExpansion() {
        assertTrue(renderer.render(
            ValidationResult.valid(),
            IntegrationFormat.QUIL,
            "EXPORTABLE",
            0,
            false,
            true,
            List.of(new DesktopIrOperationSpec(
                "X",
                "q[0]",
                "q[1]",
                "q[0]",
                "c[0]",
                0.5
            ))
        ).contains("OP LIST DEFERRED 1 operation(s)"));

        assertTrue(renderer.render(
            ValidationResult.valid(),
            IntegrationFormat.OPENQASM_2,
            "EXPORTABLE",
            0,
            true,
            false,
            List.of()
        ).contains("ACTIVE JSON Source: native JSON text"));
    }

    @Test
    void parsesOperationIndexOnlyFromOperationRows() {
        assertEquals(
            12,
            renderer.operationIndexFromItem("OP #12 CX q[0], q[1]")
        );
        assertEquals(
            -1,
            renderer.operationIndexFromItem("VALIDATION OK")
        );
        assertEquals(
            -1,
            renderer.operationIndexFromItem("OP #bad CX")
        );
    }
}