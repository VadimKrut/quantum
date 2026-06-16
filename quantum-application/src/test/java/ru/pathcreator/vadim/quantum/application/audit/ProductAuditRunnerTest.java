/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.audit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductAuditRunnerTest {

    private static final String SOURCE = """
        OPENQASM 2.0;
        include "qelib1.inc";
        qreg q[2];
        creg c[2];
        h q[0];
        cx q[0],q[1];
        measure q[0] -> c[0];
        measure q[1] -> c[1];
        """;

    @TempDir
    private Path tempDir;

    @Test
    void reportsReadyWhenDoctorAndReadinessPass() throws IOException {
        final Path project = completeProject();

        final ProductAuditReport report = new ProductAuditRunner().run(
            project,
            cases(),
            integrations(),
            new FakeIntegration(IntegrationFormat.OPENQASM_3),
            options()
        );

        assertEquals(ProductAuditStatus.READY, report.status());
        assertTrue(report.isReady());
        assertTrue(report.isAcceptable());
        assertEquals(0, report.failedCheckCount());
        assertEquals(0, report.warningCheckCount());
        assertTrue(report.doctor().isHealthy());
        assertTrue(report.readiness().isReady());
    }

    @Test
    void reportsNotReadyWhenDoctorFails() throws IOException {
        final Path project = completeProject();
        Files.delete(project.resolve(".gitignore"));

        final ProductAuditReport report = new ProductAuditRunner().run(
            project,
            cases(),
            integrations(),
            new FakeIntegration(IntegrationFormat.OPENQASM_3),
            options()
        );

        assertEquals(ProductAuditStatus.NOT_READY, report.status());
        assertTrue(!report.isAcceptable());
        assertTrue(report.failedCheckCount() > 0);
    }

    private static List<CorpusRegressionCase> cases() {
        return List.of(CorpusRegressionCase.of(
            "bell-openqasm2",
            SOURCE,
            new FakeIntegration(IntegrationFormat.OPENQASM_2)
        ));
    }

    private static List<QuantumIntegration> integrations() {
        return List.of(
            new FakeIntegration(IntegrationFormat.OPENQASM_2),
            new FakeIntegration(IntegrationFormat.OPENQASM_3),
            new FakeIntegration(IntegrationFormat.QUIL)
        );
    }

    private static ProductBenchmarkOptions options() {
        return ProductBenchmarkOptions.builder()
            .warmupIterations(0)
            .measurementIterations(1)
            .workflowOptions(ProductWorkflowOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(32)
                    .seed(7L)
                    .captureStateVector(false)
                    .build())
                .build())
            .build();
    }

    private Path completeProject() throws IOException {
        final Path project = tempDir.resolve("product");
        Files.createDirectories(project);
        Files.writeString(project.resolve("pom.xml"), pomWithModules());
        Files.writeString(project.resolve("LICENSE"), "MPL-2.0");
        Files.writeString(project.resolve(".gitignore"), "/target/\ndocs/\n/tools/\n.idea/\n");
        createDirectories(
            project,
            new String[] {
                "quantum-core",
                "quantum-simulation",
                "quantum-application",
                "quantum-json",
                "quantum-openqasm2",
                "quantum-openqasm3",
                "quantum-quil",
                "quantum-api",
                "quantum-cli",
                "quantum-desktop"
            }
        );
        createFiles(
            project,
            new String[] {
                "smoke-corpus/README.md",
                "smoke-corpus/openqasm2/bell.qasm",
                "smoke-corpus/openqasm3/ghz.qasm",
                "smoke-corpus/quil/bell.quil",
                "quantum-cli/target/quantum-cli-0.1.0.jar",
                "quantum-desktop/target/quantum-desktop-0.1.0.jar"
            }
        );
        return project;
    }

    private static void createDirectories(
        final Path project,
        final String[] directories
    ) throws IOException {
        for (int index = 0; index < directories.length; index++) {
            Files.createDirectories(project.resolve(directories[index]));
        }
    }

    private static void createFiles(
        final Path project,
        final String[] paths
    ) throws IOException {
        for (int index = 0; index < paths.length; index++) {
            final Path path = project.resolve(paths[index]);
            Files.createDirectories(path.getParent());
            Files.writeString(path, "x");
        }
    }

    private static String pomWithModules() {
        return """
            <project>
                <modules>
                    <module>quantum-core</module>
                    <module>quantum-simulation</module>
                    <module>quantum-application</module>
                    <module>quantum-json</module>
                    <module>quantum-openqasm2</module>
                    <module>quantum-openqasm3</module>
                    <module>quantum-quil</module>
                    <module>quantum-api</module>
                    <module>quantum-cli</module>
                    <module>quantum-desktop</module>
                </modules>
            </project>
            """;
    }

    private static final class FakeIntegration implements QuantumIntegration {

        private final IntegrationFormat format;

        private FakeIntegration(final IntegrationFormat format) {
            this.format = format;
        }

        @Override
        public IntegrationFormat format() {
            return format;
        }

        @Override
        public IntegrationCapabilityProfile capabilityProfile() {
            return IntegrationCapabilityProfile.of(
                format,
                EnumSet.allOf(IntegrationCapability.class)
            );
        }

        @Override
        public ExportResult exportProgram(
            final QuantumProgram program,
            final ExportOptions options
        ) {
            return ExportResult.success(
                format,
                "fake"
            );
        }

        @Override
        public ImportResult importProgram(
            final String source,
            final ImportOptions options
        ) {
            return ImportResult.success(
                format,
                bellProgram()
            );
        }

        private static QuantumProgram bellProgram() {
            final QuantumProgram program = QuantumProgram.gateBased();
            final QuantumCircuit circuit = program.createCircuit("bell");
            final QuantumRegister q = circuit.createQuantumRegister(
                "q",
                2
            );
            final ClassicalRegister c = circuit.createClassicalRegister(
                "c",
                2
            );
            circuit.h(q.get(0))
                .cx(
                    q.get(0),
                    q.get(1)
                )
                .measure(
                    q.get(0),
                    c.get(0)
                )
                .measure(
                    q.get(1),
                    c.get(1)
                );
            return program;
        }
    }
}