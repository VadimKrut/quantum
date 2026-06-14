/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.adapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.api.QuantumIntegrations;
import ru.pathcreator.vadim.quantum.api.QuantumIrFiles;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuilExternalFullLanguageCorpusTest {

    @Test
    void importsRealPyquilFilesThroughJsonAndQuilAgain() throws IOException {
        final List<Path> files = List.of(
            Path.of(
                "target",
                "external-quil",
                "pyquil",
                "test",
                "unit",
                "data",
                "test1.quil"
            ),
            Path.of(
                "target",
                "external-quil",
                "pyquil",
                "test",
                "unit",
                "data",
                "calibration_program_cz.quil"
            ),
            Path.of(
                "target",
                "external-quil",
                "pyquil",
                "test",
                "unit",
                "data",
                "calibration_program_cz_cphase.quil"
            ),
            Path.of(
                "target",
                "external-quil",
                "pyquil",
                "test",
                "unit",
                "data",
                "calibration_program_measure.quil"
            ),
            Path.of(
                "target",
                "external-quil",
                "pyquil",
                "test",
                "benchmarks",
                "fixtures",
                "over-9000.quil"
            ),
            Path.of(
                "target",
                "external-quil",
                "pyquil",
                "test",
                "benchmarks",
                "fixtures",
                "large_with_calibrations.quil"
            )
        );

        boolean allFilesExist = true;
        for (int i = 0; i < files.size(); i++) {
            if (!Files.isRegularFile(files.get(i))) {
                allFilesExist = false;
                break;
            }
        }
        Assumptions.assumeTrue(
            allFilesExist,
            "External Quil corpus is not downloaded in this checkout."
        );
        assertFalse(
            files.isEmpty(),
            "External Quil corpus must not be empty."
        );

        for (final Path file : files) {
            verifyFile(file);
        }
    }

    private static void verifyFile(final Path file) throws IOException {
        final String source = Files.readString(file);
        final ImportResult imported = QuantumIntegrations.quil().importProgram(source);

        assertTrue(
            imported.isSuccess(),
            file + " must import: " + diagnostics(imported)
        );
        assertProgramHasContent(
            imported.program(),
            file
        );

        final QuantumIrWriteResult json = QuantumIrFiles.writeToString(imported.program());

        assertTrue(
            json.isSuccess(),
            file + " must write to native JSON"
        );
        final QuantumIrReadResult fromJson = QuantumIrFiles.readFromString(json.content());

        assertTrue(
            fromJson.isSuccess(),
            file + " must read from native JSON"
        );

        final ExportResult exported = QuantumIntegrations.quil().exportProgram(fromJson.program());

        assertTrue(
            exported.isSuccess(),
            file + " must export back to Quil: " + diagnostics(exported)
        );
        final ImportResult importedAgain = QuantumIntegrations.quil().importProgram(exported.content());

        assertTrue(
            importedAgain.isSuccess(),
            file + " exported Quil must import again: " + diagnostics(importedAgain)
        );
        assertProgramHasContent(
            importedAgain.program(),
            file
        );
    }

    private static void assertProgramHasContent(
        final QuantumProgram program,
        final Path file
    ) {
        assertTrue(
            program.circuitCount() > 0,
            file + " must create a circuit"
        );
        assertTrue(
            program.circuit(0).quantumRegisterCount() > 0
                || program.circuit(0).classicalRegisterCount() > 0
                || program.circuit(0).operationCount() > 0
                || program.sourceFragmentCount() > 0,
            file + " must create non-empty IR"
        );
    }

    private static String diagnostics(final ImportResult result) {
        final ArrayList<String> messages = new ArrayList<>();
        for (int i = 0; i < result.diagnostics().size(); i++) {
            messages.add(result.diagnostics().get(i).code()
                + " line "
                + result.diagnostics().get(i).line()
                + ": "
                + result.diagnostics().get(i).message());
        }
        return messages.toString();
    }

    private static String diagnostics(final ExportResult result) {
        final ArrayList<String> messages = new ArrayList<>();
        for (int i = 0; i < result.diagnostics().size(); i++) {
            messages.add(result.diagnostics().get(i).code()
                + " line "
                + result.diagnostics().get(i).line()
                + ": "
                + result.diagnostics().get(i).message());
        }
        return messages.toString();
    }
}