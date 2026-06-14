/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.adapter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.infrastructure.persistence.json.QuantumIrJsonReader;
import ru.pathcreator.vadim.quantum.infrastructure.persistence.json.QuantumIrJsonWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenQasm3ExternalCorpusTest {

    @Test
    void importsOfficialOpenQasm3ExamplesThroughJsonAndOpenQasm3Again() throws IOException {
        final Path examples = Path.of(
            "target",
            "openqasm3-external",
            "examples"
        );

        Assumptions.assumeTrue(
            Files.isDirectory(examples),
            "External OpenQASM 3 examples are not downloaded in this checkout."
        );

        final ArrayList<Path> files = new ArrayList<>();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(examples)) {
            for (final Path path : stream) {
                if (path.getFileName().toString().endsWith(".qasm")) {
                    files.add(path);
                }
            }
        }
        files.sort(Path::compareTo);

        assertFalse(
            files.isEmpty(),
            "External OpenQASM 3 examples corpus must contain .qasm files."
        );

        for (final Path file : files) {
            verifyFile(file);
        }
    }

    private static void verifyFile(final Path file) throws IOException {
        final String source = Files.readString(file);
        final ImportResult imported = new OpenQasm3Integration().importProgram(source);

        assertTrue(
            imported.isSuccess(),
            file + " must import: " + diagnostics(imported)
        );
        assertProgramHasContent(
            imported.program(),
            file
        );

        final QuantumIrWriteResult json = new QuantumIrJsonWriter().write(imported.program());

        assertTrue(
            json.isSuccess(),
            file + " must write to native JSON"
        );
        final QuantumIrReadResult fromJson = new QuantumIrJsonReader().read(json.content());

        assertTrue(
            fromJson.isSuccess(),
            file + " must read from native JSON"
        );

        final ExportResult exported = new OpenQasm3Integration().exportProgram(fromJson.program());

        assertTrue(
            exported.isSuccess(),
            file + " must export back to OpenQASM 3: " + diagnostics(exported)
        );
        final ImportResult importedAgain = new OpenQasm3Integration().importProgram(exported.content());

        assertTrue(
            importedAgain.isSuccess(),
            file + " exported OpenQASM 3 must import again: " + diagnostics(importedAgain)
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
            program.gateDefinitionCount() > 0
                || program.calibrationDefinitionCount() > 0
                || program.circuit(0).quantumRegisterCount() > 0
                || program.circuit(0).classicalRegisterCount() > 0
                || program.circuit(0).operationCount() > 0,
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