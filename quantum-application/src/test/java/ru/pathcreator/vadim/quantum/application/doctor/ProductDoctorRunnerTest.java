/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.doctor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductDoctorRunnerTest {

    @TempDir
    private Path tempDir;

    @Test
    void reportsHealthyWhenProductStructureIsComplete() throws IOException {
        final Path project = completeProject(true);

        final ProductDoctorReport report = new ProductDoctorRunner().run(project);

        assertEquals(ProductDoctorStatus.HEALTHY, report.status());
        assertEquals(0, report.failedCheckCount());
        assertEquals(0, report.warningCheckCount());
        assertTrue(report.isHealthy());
        assertTrue(report.isAcceptable());
        assertEquals(7, report.checkCount());
    }

    @Test
    void keepsMissingPackagedJarsAsWarningBeforePackagePhase() throws IOException {
        final Path project = completeProject(false);

        final ProductDoctorReport report = new ProductDoctorRunner().run(project);

        assertEquals(ProductDoctorStatus.HEALTHY_WITH_WARNINGS, report.status());
        assertEquals(0, report.failedCheckCount());
        assertEquals(1, report.warningCheckCount());
        assertTrue(report.isSuccess());
        assertTrue(report.isAcceptable());
    }

    @Test
    void reportsBrokenWhenRequiredProjectFilesAreMissing() throws IOException {
        final Path project = completeProject(true);
        Files.delete(project.resolve(".gitignore"));

        final ProductDoctorReport report = new ProductDoctorRunner().run(project);

        assertEquals(ProductDoctorStatus.BROKEN, report.status());
        assertEquals(1, report.failedCheckCount());
    }

    private Path completeProject(final boolean includeJars) throws IOException {
        final Path project = tempDir.resolve("quantum");
        Files.createDirectories(project);
        Files.writeString(project.resolve("pom.xml"), pomWithModules());
        Files.writeString(project.resolve("README.md"), "# Quantum");
        Files.writeString(project.resolve(".gitignore"), "/target/\ndocs/\n.idea/\n");
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
                "tools/quantum.ps1",
                "tools/quantum-desktop.ps1",
                "tools/product-smoke.ps1",
                "smoke-corpus/README.md",
                "smoke-corpus/openqasm2/bell.qasm",
                "smoke-corpus/openqasm3/ghz.qasm",
                "smoke-corpus/quil/bell.quil"
            }
        );
        if (includeJars) {
            createFiles(
                project,
                new String[] {
                    "quantum-cli/target/quantum-cli-0.1.0.jar",
                    "quantum-desktop/target/quantum-desktop-0.1.0.jar"
                }
            );
        }
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
            final Path file = project.resolve(paths[index]);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "x");
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