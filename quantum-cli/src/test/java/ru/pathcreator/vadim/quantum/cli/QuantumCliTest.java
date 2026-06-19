/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumCliTest {

    @TempDir
    private Path tempDir;

    @Test
    void validatesOpenQasm2InJsonMode() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "validate",
            "--input",
            source.toString(),
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"valid\" : true"));
    }

    @Test
    void inspectsOpenQasm2InTextMode() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "inspect",
            "--input",
            source.toString()
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("circuits=1"));
        assertTrue(result.stdout().contains("operations=4"));
    }

    @Test
    void preflightsTargetFormat() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "preflight",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("Preflight: EXPORTABLE"));
    }

    @Test
    void estimatesResourcesInJsonMode() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "resources",
            "--input",
            source.toString(),
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"estimatedStateVectorBytes\" : 64"));
        assertTrue(result.stdout().contains("\"localSimulationFeasible\" : true"));
        assertTrue(result.stdout().contains("\"twoQubitGateCount\" : 1"));
    }

    @Test
    void printsCircuitTimelineInJsonMode() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "circuit",
            "--input",
            source.toString(),
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"quantumWires\""));
        assertTrue(result.stdout().contains("\"label\" : \"cx\""));
        assertTrue(result.stdout().contains("\"classicalWires\""));
    }

    @Test
    void compilesAndWritesTargetFile() throws Exception {
        final Path source = writeBellOpenQasm2();
        final Path output = tempDir.resolve("bell.qasm3");

        final CliRunResult result = run(
            "compile",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3",
            "--output",
            output.toString()
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(Files.readString(output).contains("OPENQASM 3.0"));
    }

    @Test
    void compileFastModeSkipsOptionalCompilerStages() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "compile",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3",
            "--fast",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"status\" : \"EXPORTED\""));
        assertTrue(result.stdout().contains("\"status\" : \"SKIPPED\""));
        assertTrue(result.stdout().contains("\"stage\" : \"INITIAL_VALIDATION\""));
    }

    @Test
    void convertsExternalProgramToNativeJson() throws Exception {
        final Path source = writeBellOpenQasm2();
        final Path output = tempDir.resolve("bell.quantum.json");

        final CliRunResult result = run(
            "convert",
            "--input",
            source.toString(),
            "--output-format",
            "json",
            "--output",
            output.toString()
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(Files.readString(output).contains("\"computationModel\""));
    }

    @Test
    void simulatesBellProgram() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "simulate",
            "--input",
            source.toString(),
            "--shots",
            "128",
            "--seed",
            "9",
            "--state-vector",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"shots\" : 128"));
        assertTrue(result.stdout().contains("\"counts\""));
        assertTrue(result.stdout().contains("\"stateVectorSize\""));
    }

    @Test
    void simulatesNativeJsonWithClassicalControlSurface() throws Exception {
        final Path source = writeClassicalControlNativeJson();

        final CliRunResult result = run(
            "simulate",
            "--input",
            source.toString(),
            "--input-format",
            "json",
            "--format",
            "json",
            "--shots",
            "64",
            "--seed",
            "7"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"success\" : true"));
        assertTrue(result.stdout().contains("\"1\" : 64"));
    }

    @Test
    void runsProductWorkflowInJsonMode() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "workflow",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "64",
            "--seed",
            "3",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"workflow\""));
        assertTrue(result.stdout().contains("\"status\" : \"COMPLETED\""));
        assertTrue(result.stdout().contains("\"backendDryRun\""));
        assertTrue(result.stdout().contains("\"compile\""));
        assertTrue(result.stdout().contains("\"resources\""));
    }

    @Test
    void runsBenchmarkInJsonMode() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "benchmark",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3",
            "--warmup",
            "0",
            "--iterations",
            "1",
            "--shots",
            "32",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"benchmark\""));
        assertTrue(result.stdout().contains("\"stage\" : \"import\""));
        assertTrue(result.stdout().contains("\"stage\" : \"workflow\""));
        assertTrue(result.stdout().contains("\"totalAverageNanos\""));
    }

    @Test
    void runsCompatibilityMatrixInJsonMode() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "compatibility",
            "--input",
            source.toString(),
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"compatibility\""));
        assertTrue(result.stdout().contains("\"targets\""));
        assertTrue(result.stdout().contains("\"targetFormat\" : \"OPENQASM_3\""));
        assertTrue(result.stdout().contains("\"checks\""));
    }

    @Test
    void verifiesCrossFormatRoundTripsInJsonMode() throws Exception {
        final Path source = writeBellOpenQasm2();

        final CliRunResult result = run(
            "verify-cross-format",
            "--input",
            source.toString(),
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"verify-cross-format\""));
        assertTrue(result.stdout().contains("\"simulationEquivalent\" : true"));
        assertTrue(result.stdout().contains("\"targetFormat\" : \"OPENQASM_3\""));
    }

    @Test
    void runsCorpusRegressionForDirectoryInJsonMode() throws Exception {
        writeBellOpenQasm2();

        final CliRunResult result = run(
            "regress-corpus",
            "--input",
            tempDir.toString(),
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"regress-corpus\""));
        assertTrue(result.stdout().contains("\"caseCount\" : 1"));
        assertTrue(result.stdout().contains("\"failureCount\" : 0"));
        assertTrue(result.stdout().contains("\"crossFormat\""));
    }

    @Test
    void runsReleaseReadinessForDirectoryInJsonMode() throws Exception {
        writeBellOpenQasm2();

        final CliRunResult result = run(
            "release-readiness",
            "--input",
            tempDir.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"release-readiness\""));
        assertTrue(result.stdout().contains("\"status\" : \"READY\""));
        assertTrue(result.stdout().contains("\"corpusRegression\""));
        assertTrue(result.stdout().contains("\"benchmark\""));
    }

    @Test
    void printsTargetProfile() {
        final CliRunResult result = run(
            "target-profile",
            "--output-format",
            "quil",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"targetName\""));
        assertTrue(result.stdout().contains("\"nativeGates\""));
    }

    @Test
    void printsProductDoctorReport() throws Exception {
        final Path project = createCompleteProductProject();

        final CliRunResult result = run(
            "doctor",
            "--input",
            project.toString(),
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"doctor\""));
        assertTrue(result.stdout().contains("\"status\" : \"HEALTHY\""));
        assertTrue(result.stdout().contains("\"failedCheckCount\" : 0"));
    }

    @Test
    void printsProductAuditReport() throws Exception {
        final Path project = createCompleteProductProject();
        final Path corpus = tempDir.resolve("corpus");
        Files.createDirectories(corpus);
        Files.writeString(
            corpus.resolve("bell.qasm"),
            bellOpenQasm2Source()
        );

        final CliRunResult result = run(
            "product-audit",
            "--input",
            corpus.toString(),
            "--project-root",
            project.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"product-audit\""));
        assertTrue(result.stdout().contains("\"status\" : \"READY\""));
        assertTrue(result.stdout().contains("\"doctor\""));
        assertTrue(result.stdout().contains("\"readiness\""));
    }

    @Test
    void writesProductReportBundle() throws Exception {
        final Path project = createCompleteProductProject();
        final Path corpus = tempDir.resolve("report-corpus");
        final Path output = tempDir.resolve("report-output");
        Files.createDirectories(corpus);
        Files.writeString(
            corpus.resolve("bell.qasm"),
            bellOpenQasm2Source()
        );

        final CliRunResult result = run(
            "product-report",
            "--input",
            corpus.toString(),
            "--project-root",
            project.toString(),
            "--output",
            output.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"product-report\""));
        assertTrue(Files.isRegularFile(output.resolve("product-audit.json")));
        assertTrue(Files.isRegularFile(output.resolve("summary.md")));
        assertTrue(Files.isRegularFile(output.resolve("manifest.properties")));
        assertTrue(Files.readString(output.resolve("summary.md")).contains("Quantum Product Report"));
        assertTrue(Files.readString(output.resolve("manifest.properties")).contains("summarySha256="));
    }

    @Test
    void writesProductDistributionBundle() throws Exception {
        final Path project = createCompleteProductProject();
        final Path corpus = tempDir.resolve("distribution-corpus");
        final Path output = tempDir.resolve("distribution-output");
        Files.createDirectories(corpus);
        Files.writeString(
            corpus.resolve("bell.qasm"),
            bellOpenQasm2Source()
        );

        final CliRunResult result = run(
            "product-distribution",
            "--input",
            corpus.toString(),
            "--project-root",
            project.toString(),
            "--output",
            output.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            result.exitCode()
        );
        assertTrue(result.stdout().contains("\"command\" : \"product-distribution\""));
        assertTrue(result.stdout().contains("\"archivePath\""));
        assertTrue(result.stdout().contains("\"archiveSha256\""));
        assertTrue(Files.isRegularFile(output.resolve("README.md")));
        assertTrue(Files.isRegularFile(tempDir.resolve("distribution-output.zip")));
        assertTrue(Files.isRegularFile(output.resolve("manifest.properties")));
        assertTrue(Files.isRegularFile(output.resolve("lib").resolve("quantum-cli-test.jar")));
        assertTrue(Files.isRegularFile(output.resolve("report").resolve("product-audit.json")));
        assertTrue(Files.readString(output.resolve("manifest.properties")).contains("format=quantum-product-distribution"));
        final CliRunResult verify = run(
            "product-verify-distribution",
            "--input",
            output.toString(),
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            verify.exitCode()
        );
        assertTrue(verify.stdout().contains("\"command\" : \"product-verify-distribution\""));
        assertTrue(verify.stdout().contains("\"success\" : true"));
    }

    @Test
    void detectsTamperedDistributionBundle() throws Exception {
        final Path project = createCompleteProductProject();
        final Path corpus = tempDir.resolve("tampered-distribution-corpus");
        final Path output = tempDir.resolve("tampered-distribution-output");
        Files.createDirectories(corpus);
        Files.writeString(
            corpus.resolve("bell.qasm"),
            bellOpenQasm2Source()
        );
        final CliRunResult distribution = run(
            "product-distribution",
            "--input",
            corpus.toString(),
            "--project-root",
            project.toString(),
            "--output",
            output.toString(),
            "--output-format",
            "openqasm3",
            "--format",
            "json"
        );
        assertEquals(
            QuantumCli.EXIT_SUCCESS,
            distribution.exitCode()
        );
        Files.writeString(
            output.resolve("README.md"),
            "tampered"
        );

        final CliRunResult verify = run(
            "product-verify-distribution",
            "--input",
            output.toString(),
            "--format",
            "json"
        );

        assertEquals(
            QuantumCli.EXIT_WORKFLOW_ERROR,
            verify.exitCode()
        );
        assertTrue(verify.stdout().contains("MANIFEST_FILE_SIZE_MISMATCH"));
    }

    @Test
    void returnsUsageErrorForMissingInput() {
        final CliRunResult result = run("validate");

        assertEquals(
            QuantumCli.EXIT_USAGE_ERROR,
            result.exitCode()
        );
        assertTrue(result.stderr().contains("Missing required option"));
    }

    private Path writeBellOpenQasm2() throws Exception {
        final Path source = tempDir.resolve("bell.qasm");
        Files.writeString(
            source,
            bellOpenQasm2Source()
        );
        return source;
    }

    private static String bellOpenQasm2Source() {
        return """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            h q[0];
            cx q[0],q[1];
            measure q[0] -> c[0];
            measure q[1] -> c[1];
            """.trim();
    }

    private Path createCompleteProductProject() throws Exception {
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
                "quantum-cli/target/quantum-cli-test.jar",
                "quantum-desktop/target/quantum-desktop-test.jar"
            }
        );
        return project;
    }

    private Path writeClassicalControlNativeJson() throws Exception {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("classical_control");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        final ClassicalBit bit = c.get(0);
        circuit.assign(new ClassicalAssignment(
            ClassicalExpression.bit(bit),
            ClassicalExpression.integer(1L)
        ));
        circuit.classicallyControlled(
            ClassicalPredicate.compare(
                ClassicalExpression.bit(bit),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(1L)
            ),
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            )
        );
        circuit.controlled(
            ClassicalCondition.equalTo(
                c,
                1L
            ),
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            )
        );
        circuit.conditionalBlock(
            ClassicalPredicate.compare(
                ClassicalExpression.bit(bit),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(1L)
            ),
            OperationBlock.of(GateOperation.of(
                StandardGate.X,
                q.get(0)
            )),
            null
        );
        circuit.symbolicForLoop(new SymbolicForLoopOperation(
            "j",
            "int",
            ClassicalExpression.integer(0L),
            ClassicalExpression.integer(1L),
            ClassicalExpression.integer(1L),
            OperationBlock.of(
                GateOperation.of(
                    StandardGate.X,
                    q.get(0)
                ),
                GateOperation.of(
                    StandardGate.X,
                    q.get(0)
                )
            )
        ));
        circuit.measure(
            q.get(0),
            bit
        );
        final Path source = tempDir.resolve("classical-control.quantum.json");
        Files.writeString(
            source,
            Quantum.writeJson(program).content()
        );
        return source;
    }

    private static void createDirectories(
        final Path project,
        final String[] directories
    ) throws Exception {
        for (int index = 0; index < directories.length; index++) {
            Files.createDirectories(project.resolve(directories[index]));
        }
    }

    private static void createFiles(
        final Path project,
        final String[] paths
    ) throws Exception {
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

    private static CliRunResult run(final String... args) {
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final int exitCode = QuantumCli.run(
            args,
            new PrintStream(
                stdout,
                true,
                StandardCharsets.UTF_8
            ),
            new PrintStream(
                stderr,
                true,
                StandardCharsets.UTF_8
            )
        );
        return new CliRunResult(
            exitCode,
            stdout.toString(StandardCharsets.UTF_8),
            stderr.toString(StandardCharsets.UTF_8)
        );
    }

    private record CliRunResult(
        int exitCode,
        String stdout,
        String stderr
    ) {
    }
}