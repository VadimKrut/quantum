/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.result;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportResultTest {

    @Test
    void createsSuccessfulExportResult() {
        final ExportResult result = ExportResult.success(
            IntegrationFormat.OPENQASM_2,
            "OPENQASM 2.0;"
        );

        assertTrue(result.isSuccess());
        assertTrue(result.hasContent());
        assertFalse(result.hasErrors());
        assertEquals(
            IntegrationFormat.OPENQASM_2,
            result.format()
        );
        assertEquals(
            "OPENQASM 2.0;",
            result.content()
        );
        assertEquals(
            0,
            result.diagnosticCount()
        );
    }

    @Test
    void protectsDiagnosticsFromExternalMutation() {
        final ArrayList<IntegrationDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.add(IntegrationDiagnostic.warning(
            IntegrationDiagnosticCode.UNSUPPORTED_GATE,
            "Gate will be decomposed."
        ));

        final ExportResult result = ExportResult.success(
            IntegrationFormat.OPENQASM_2,
            "OPENQASM 2.0;",
            diagnostics
        );
        diagnostics.clear();

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.diagnosticCount()
        );
        assertSame(
            result.diagnostic(0),
            result.diagnostics().get(0)
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> result.diagnostics().add(IntegrationDiagnostic.warning(
                IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
                "Warn."
            ))
        );
    }

    @Test
    void createsFailedExportResult() {
        final IntegrationDiagnostic diagnostic = IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.DOMAIN_VALIDATION_FAILED,
            "Program is invalid."
        );
        final ExportResult result = ExportResult.failure(
            IntegrationFormat.OPENQASM_2,
            List.of(diagnostic)
        );

        assertFalse(result.isSuccess());
        assertFalse(result.hasContent());
        assertTrue(result.hasErrors());
        assertEquals(
            1,
            result.diagnosticCount()
        );
        assertSame(
            diagnostic,
            result.diagnostic(0)
        );
        assertThrows(
            IllegalStateException.class,
            result::content
        );
    }

    @Test
    void treatsContentWithErrorDiagnosticsAsUnsuccessful() {
        final ExportResult result = ExportResult.success(
            IntegrationFormat.OPENQASM_2,
            "OPENQASM 2.0;",
            List.of(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                "Gate is unsupported."
            ))
        );

        assertFalse(result.isSuccess());
        assertTrue(result.hasContent());
        assertTrue(result.hasErrors());
    }

    @Test
    void rejectsInvalidExportResultState() {
        final ArrayList<IntegrationDiagnostic> diagnosticsWithNull = new ArrayList<>();
        diagnosticsWithNull.add(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> ExportResult.success(
                null,
                "OPENQASM 2.0;"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ExportResult.success(
                IntegrationFormat.OPENQASM_2,
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ExportResult.success(
                IntegrationFormat.OPENQASM_2,
                "OPENQASM 2.0;",
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ExportResult.success(
                IntegrationFormat.OPENQASM_2,
                "OPENQASM 2.0;",
                diagnosticsWithNull
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ExportResult.failure(
                IntegrationFormat.OPENQASM_2,
                List.of(IntegrationDiagnostic.warning(
                    IntegrationDiagnosticCode.UNSUPPORTED_GATE,
                    "Only warning."
                ))
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ExportResult.success(
                IntegrationFormat.OPENQASM_2,
                "OPENQASM 2.0;"
            ).diagnostic(0)
        );
    }
}