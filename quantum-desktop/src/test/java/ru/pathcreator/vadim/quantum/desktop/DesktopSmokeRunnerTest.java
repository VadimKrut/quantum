/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopSmokeRunnerTest {

    @TempDir
    private Path tempDir;

    @Test
    void runsAllDesktopWorkflowSteps() throws Exception {
        final Path project = completeProductProject();
        final Path corpus = corpus();

        final DesktopSmokeReport report = new DesktopSmokeRunner().run(
            project,
            corpus
        );

        assertTrue(report.isSuccess());
        assertEquals(
            20,
            report.stepCount()
        );
        assertEquals(
            0,
            report.failedStepCount()
        );
        assertTrue(containsStep(
            report,
            "backend-dry-run-openqasm3"
        ));
        assertTrue(containsStep(
            report,
            "product-report"
        ));
        assertTrue(containsStep(
            report,
            "product-distribution"
        ));
    }

    @Test
    void desktopSmokeApplicationWritesJsonAndReturnsSuccess() throws Exception {
        final Path project = completeProductProject();
        final Path corpus = corpus();
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final int exitCode = DesktopSmokeApplication.run(
            new String[] {
                "--project-root",
                project.toString(),
                "--corpus",
                corpus.toString()
            },
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

        final String output = stdout.toString(StandardCharsets.UTF_8);
        assertEquals(
            DesktopSmokeApplication.EXIT_SUCCESS,
            exitCode
        );
        assertTrue(output.contains("\"success\" : true"));
        assertTrue(output.contains("compile-openqasm3"));
        assertTrue(stderr.toString(StandardCharsets.UTF_8).isBlank());
    }

    @Test
    void desktopSmokeApplicationGeneratesCorpusWhenCorpusIsMissing() throws Exception {
        final Path project = completeProductProject();
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final int exitCode = DesktopSmokeApplication.run(
            new String[] {
                "--project-root",
                project.toString(),
                "--corpus",
                tempDir.resolve("missing-corpus").toString()
            },
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

        assertEquals(
            DesktopSmokeApplication.EXIT_SUCCESS,
            exitCode
        );
        assertFalse(stdout.toString(StandardCharsets.UTF_8).isBlank());
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("\"success\" : true"));
        assertTrue(Files.isDirectory(project.resolve("target").resolve("desktop-smoke-output").resolve("smoke-corpus")));
    }

    private Path corpus() throws Exception {
        final Path corpus = tempDir.resolve("corpus");
        Files.createDirectories(corpus);
        Files.writeString(
            corpus.resolve("bell.qasm"),
            """
                OPENQASM 2.0;
                include "qelib1.inc";
                qreg q[2];
                creg c[2];
                h q[0];
                cx q[0],q[1];
                measure q[0] -> c[0];
                measure q[1] -> c[1];
                """
        );
        return corpus;
    }

    private static boolean containsStep(
        final DesktopSmokeReport report,
        final String name
    ) {
        for (int i = 0; i < report.steps().size(); i++) {
            if (name.equals(report.steps().get(i).name())) {
                return true;
            }
        }
        return false;
    }

    private Path completeProductProject() throws Exception {
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
    ) throws Exception {
        for (int i = 0; i < directories.length; i++) {
            Files.createDirectories(project.resolve(directories[i]));
        }
    }

    private static void createFiles(
        final Path project,
        final String[] paths
    ) throws Exception {
        for (int i = 0; i < paths.length; i++) {
            final Path file = project.resolve(paths[i]);
            Files.createDirectories(file.getParent());
            Files.writeString(
                file,
                "x"
            );
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
}