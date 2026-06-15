/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;

class DesktopWorkflowServiceTest {

    private static final String BELL = """
        OPENQASM 2.0;
        include "qelib1.inc";
        qreg q[2];
        creg c[2];
        h q[0];
        cx q[0],q[1];
        measure q[0] -> c[0];
        measure q[1] -> c[1];
        """;

    private final DesktopWorkflowService service = new DesktopWorkflowService();

    @TempDir
    private Path tempDirectory;

    @Test
    void validatesImportedProgram() {
        final DesktopWorkflowResult result = service.validate(
            IntegrationFormat.OPENQASM_2,
            BELL
        );

        assertEquals(
            DesktopAction.VALIDATE,
            result.action()
        );
        assertTrue(result.isSuccess());
        assertEquals(
            "VALID",
            result.status()
        );
        assertTrue(result.content().contains("\"valid\""));
    }

    @Test
    void compilesImportedProgramToOpenQasm3() {
        final DesktopWorkflowResult result = service.compile(
            IntegrationFormat.OPENQASM_2,
            BELL,
            IntegrationFormat.OPENQASM_3
        );

        assertTrue(result.isSuccess());
        assertEquals(
            "EXPORTED",
            result.status()
        );
        assertTrue(result.generatedContent().contains("OPENQASM 3"));
    }

    @Test
    void simulatesBellProgram() {
        final DesktopWorkflowResult result = service.simulate(
            IntegrationFormat.OPENQASM_2,
            BELL,
            128,
            7L
        );

        assertTrue(result.isSuccess());
        assertEquals(
            "SIMULATED",
            result.status()
        );
        assertTrue(result.summary().contains("shots: 128"));
        assertFalse(result.content().isBlank());
    }

    @Test
    void runsProductLevelProgramActions() {
        final DesktopWorkflowResult resources = service.resources(
            IntegrationFormat.OPENQASM_2,
            BELL,
            20
        );
        final DesktopWorkflowResult circuit = service.circuit(
            IntegrationFormat.OPENQASM_2,
            BELL
        );
        final DesktopWorkflowResult workflow = service.workflow(
            IntegrationFormat.OPENQASM_2,
            BELL,
            IntegrationFormat.OPENQASM_3,
            64,
            5L
        );
        final DesktopWorkflowResult backend = service.backendDryRun(
            IntegrationFormat.OPENQASM_2,
            BELL,
            IntegrationFormat.OPENQASM_3,
            64,
            5L
        );

        assertEquals(
            DesktopAction.RESOURCES,
            resources.action()
        );
        assertTrue(resources.isSuccess());
        assertTrue(resources.content().contains("estimatedStateVectorBytes"));
        assertEquals(
            DesktopAction.CIRCUIT,
            circuit.action()
        );
        assertTrue(circuit.isSuccess());
        assertTrue(circuit.content().contains("circuits"));
        assertEquals(
            DesktopAction.WORKFLOW,
            workflow.action()
        );
        assertTrue(workflow.isSuccess());
        assertEquals(
            DesktopAction.BACKEND_DRY_RUN,
            backend.action()
        );
        assertTrue(backend.isSuccess());
        assertTrue(backend.content().contains("trackingId"));
    }

    @Test
    void runsDesktopCorpusActions() throws Exception {
        final Path corpus = createSmokeCorpus();
        final DesktopWorkflowResult regression = service.corpusRegression(
            corpus,
            64,
            5L
        );
        final DesktopWorkflowResult readiness = service.releaseReadiness(
            corpus,
            IntegrationFormat.OPENQASM_3,
            64,
            5L
        );
        final DesktopWorkflowResult doctor = service.doctor(Path.of("."));

        assertEquals(
            DesktopAction.REGRESSION,
            regression.action()
        );
        assertTrue(regression.isSuccess());
        assertEquals(
            DesktopAction.READINESS,
            readiness.action()
        );
        assertTrue(readiness.isSuccess());
        assertEquals(
            DesktopAction.DOCTOR,
            doctor.action()
        );
        assertFalse(doctor.content().isBlank());
    }

    @Test
    void reportsImportFailureWithoutThrowing() {
        final DesktopWorkflowResult result = service.preflight(
            IntegrationFormat.OPENQASM_2,
            "not qasm",
            IntegrationFormat.OPENQASM_3
        );

        assertFalse(result.isSuccess());
        assertEquals(
            "IMPORT_FAILED",
            result.status()
        );
        assertTrue(result.summary().contains("Diagnostics:"));
    }

    @Test
    void buildsProductAuditPayload() throws Exception {
        final Path corpus = createSmokeCorpus();
        final DesktopWorkflowResult result = service.productAudit(
            Path.of("."),
            corpus,
            IntegrationFormat.OPENQASM_3,
            64,
            5L
        );

        assertEquals(
            DesktopAction.PRODUCT_AUDIT,
            result.action()
        );
        assertTrue(result.content().contains("Quantum Product Report"));
        assertTrue(result.content().contains("\"audit\""));
    }

    private Path createSmokeCorpus() throws Exception {
        final Path corpus = tempDirectory.resolve("smoke-corpus");
        Files.createDirectories(corpus.resolve("openqasm2"));
        Files.writeString(
            corpus.resolve("openqasm2").resolve("bell.qasm"),
            BELL
        );
        return corpus;
    }
}