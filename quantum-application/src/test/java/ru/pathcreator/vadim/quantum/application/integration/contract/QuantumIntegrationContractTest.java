/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.contract;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumIntegrationContractTest {

    @Test
    void defaultExporterMethodUsesDefaultOptions() {
        final RecordingIntegration integration = new RecordingIntegration();
        final QuantumProgram program = QuantumProgram.gateBased();

        final ExportResult result = integration.exportProgram(program);

        assertTrue(result.isSuccess());
        assertSame(
            program,
            integration.lastExportedProgram
        );
        assertEquals(
            ExportOptions.defaults(),
            integration.lastExportOptions
        );
    }

    @Test
    void defaultImporterMethodUsesDefaultOptions() {
        final RecordingIntegration integration = new RecordingIntegration();

        final ImportResult result = integration.importProgram("OPENQASM 2.0;");

        assertTrue(result.isSuccess());
        assertEquals(
            "OPENQASM 2.0;",
            integration.lastImportedSource
        );
        assertEquals(
            ImportOptions.defaults(),
            integration.lastImportOptions
        );
    }

    @Test
    void integrationExposesSingleFormatForBothDirections() {
        final QuantumIntegration integration = new RecordingIntegration();

        assertEquals(
            IntegrationFormat.OPENQASM_2,
            integration.format()
        );
    }

    private static final class RecordingIntegration implements QuantumIntegration {

        private QuantumProgram lastExportedProgram;
        private ExportOptions lastExportOptions;
        private String lastImportedSource;
        private ImportOptions lastImportOptions;

        @Override
        public IntegrationFormat format() {
            return IntegrationFormat.OPENQASM_2;
        }

        @Override
        public ExportResult exportProgram(
            final QuantumProgram program,
            final ExportOptions options
        ) {
            lastExportedProgram = program;
            lastExportOptions = options;
            return ExportResult.success(
                format(),
                "OPENQASM 2.0;",
                List.of()
            );
        }

        @Override
        public ImportResult importProgram(
            final String source,
            final ImportOptions options
        ) {
            lastImportedSource = source;
            lastImportOptions = options;
            return ImportResult.success(
                format(),
                QuantumProgram.gateBased(),
                List.of()
            );
        }
    }
}