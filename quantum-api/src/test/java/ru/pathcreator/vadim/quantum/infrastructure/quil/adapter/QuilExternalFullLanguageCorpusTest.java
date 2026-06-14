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

import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.HaltOperation;
import ru.pathcreator.vadim.quantum.domain.operation.LabelOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.WaitOperation;
import ru.pathcreator.vadim.quantum.infrastructure.persistence.json.QuantumIrJsonReader;
import ru.pathcreator.vadim.quantum.infrastructure.persistence.json.QuantumIrJsonWriter;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.adapter.OpenQasm2Integration;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.adapter.OpenQasm3Integration;

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
                "unit",
                "data",
                "calibration_program_rx.quil"
            ),
            Path.of(
                "target",
                "external-quil",
                "pyquil",
                "test",
                "unit",
                "data",
                "calibration_program_xy.quil"
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
        final ImportResult imported = new QuilIntegration().importProgram(source);
        if (file.getFileName().toString().equals("test1.quil")) {
            assertFalse(
                imported.isSuccess(),
                file + " contains malformed/non-portable gate instructions and must not become raw source IR"
            );
            return;
        }

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

        final ExportResult exported = new QuilIntegration().exportProgram(fromJson.program());

        assertTrue(
            exported.isSuccess(),
            file + " must export back to Quil: " + diagnostics(exported)
        );
        final ImportResult importedAgain = new QuilIntegration().importProgram(exported.content());

        assertTrue(
            importedAgain.isSuccess(),
            file + " exported Quil must import again: " + diagnostics(importedAgain)
        );
        assertProgramHasContent(
            importedAgain.program(),
            file
        );
        verifyCrossFormatBoundary(
            fromJson.program(),
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
                || program.calibrationDefinitionCount() > 0,
            file + " must create non-empty IR"
        );
    }

    private static void verifyCrossFormatBoundary(
        final QuantumProgram program,
        final Path file
    ) {
        final ExportResult openQasm2 = new OpenQasm2Integration().exportProgram(program);
        final ExportResult openQasm3 = new OpenQasm3Integration().exportProgram(program);
        if (
            program.calibrationDefinitionCount() == 0
            && operationCount(program, BranchOperation.class) == 0
            && operationCount(program, LabelOperation.class) == 0
            && operationCount(program, HaltOperation.class) == 0
            && operationCount(program, WaitOperation.class) == 0
        ) {
            assertTrue(
                openQasm2.isSuccess(),
                file + " portable Quil IR must export to OpenQASM 2: " + diagnostics(openQasm2)
            );
            assertTrue(
                openQasm3.isSuccess(),
                file + " portable Quil IR must export to OpenQASM 3: " + diagnostics(openQasm3)
            );
        } else {
            assertFalse(
                openQasm2.isSuccess(),
                file + " non-portable Quil IR must not be silently exported to OpenQASM 2"
            );
            assertFalse(
                openQasm3.isSuccess(),
                file + " non-portable Quil IR must not be silently exported to OpenQASM 3"
            );
        }
    }

    private static int operationCount(
        final QuantumProgram program,
        final Class<?> operationClass
    ) {
        int count = 0;
        for (int i = 0; i < program.circuitCount(); i++) {
            for (int j = 0; j < program.circuit(i).operationCount(); j++) {
                final Operation operation = program.circuit(i).operation(j);
                if (operationClass.isInstance(operation)) {
                    count++;
                }
            }
        }
        return count;
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