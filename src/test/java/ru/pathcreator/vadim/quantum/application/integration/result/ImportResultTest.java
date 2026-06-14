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
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportResultTest {

    @Test
    void createsSuccessfulImportResult() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final ImportResult result = ImportResult.success(
            IntegrationFormat.OPENQASM_2,
            program
        );

        assertTrue(result.isSuccess());
        assertTrue(result.hasProgram());
        assertFalse(result.hasErrors());
        assertEquals(
            IntegrationFormat.OPENQASM_2,
            result.format()
        );
        assertSame(
            program,
            result.program()
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
            IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
            "Ignored pragma."
        ));

        final ImportResult result = ImportResult.success(
            IntegrationFormat.OPENQASM_2,
            QuantumProgram.gateBased(),
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
                IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                "Warn."
            ))
        );
    }

    @Test
    void createsFailedImportResult() {
        final IntegrationDiagnostic diagnostic = IntegrationDiagnostic.error(
            IntegrationDiagnosticCode.PARSE_ERROR,
            "Unexpected token.",
            1,
            9
        );
        final ImportResult result = ImportResult.failure(
            IntegrationFormat.OPENQASM_2,
            List.of(diagnostic)
        );

        assertFalse(result.isSuccess());
        assertFalse(result.hasProgram());
        assertTrue(result.hasErrors());
        assertSame(
            diagnostic,
            result.diagnostic(0)
        );
        assertThrows(
            IllegalStateException.class,
            result::program
        );
    }

    @Test
    void treatsProgramWithErrorDiagnosticsAsUnsuccessful() {
        final ImportResult result = ImportResult.success(
            IntegrationFormat.OPENQASM_2,
            QuantumProgram.gateBased(),
            List.of(IntegrationDiagnostic.error(
                IntegrationDiagnosticCode.IMPORT_VALIDATION_FAILED,
                "Imported program is invalid."
            ))
        );

        assertFalse(result.isSuccess());
        assertTrue(result.hasProgram());
        assertTrue(result.hasErrors());
    }

    @Test
    void rejectsInvalidImportResultState() {
        final ArrayList<IntegrationDiagnostic> diagnosticsWithNull = new ArrayList<>();
        diagnosticsWithNull.add(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> ImportResult.success(
                null,
                QuantumProgram.gateBased()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportResult.success(
                IntegrationFormat.OPENQASM_2,
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportResult.success(
                IntegrationFormat.OPENQASM_2,
                QuantumProgram.gateBased(),
                null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportResult.success(
                IntegrationFormat.OPENQASM_2,
                QuantumProgram.gateBased(),
                diagnosticsWithNull
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportResult.failure(
                IntegrationFormat.OPENQASM_2,
                List.of(IntegrationDiagnostic.warning(
                    IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
                    "Only warning."
                ))
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ImportResult.success(
                IntegrationFormat.OPENQASM_2,
                QuantumProgram.gateBased()
            ).diagnostic(0)
        );
    }
}