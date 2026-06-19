/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.distribution;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductDistributionBundleWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    void writesDistributionBundleWithManifestAndReport() throws Exception {
        final Path project = completeProject();
        final Path report = tempDir.resolve("staged-report");
        Files.createDirectories(report);
        Files.writeString(
            report.resolve("summary.md"),
            "# Report"
        );

        final ProductDistributionBundleResult result = new ProductDistributionBundleWriter().write(
            tempDir.resolve("distribution"),
            project,
            report
        );

        assertTrue(Files.isRegularFile(result.quickstartPath()));
        assertTrue(Files.isRegularFile(result.archivePath()));
        assertTrue(Files.size(result.archivePath()) > 0L);
        assertTrue(result.archiveSha256().length() == 64);
        assertTrue(Files.isRegularFile(result.licensePath()));
        assertTrue(Files.isRegularFile(result.manifestPath()));
        assertTrue(Files.isRegularFile(result.librariesDirectory().resolve("quantum-cli-test.jar")));
        assertTrue(Files.isRegularFile(result.librariesDirectory().resolve("quantum-desktop-test.jar")));
        assertTrue(Files.isRegularFile(result.toolsDirectory().resolve("quantum.ps1")));
        assertTrue(Files.isRegularFile(result.toolsDirectory().resolve("verify-distribution.ps1")));
        assertTrue(Files.readString(result.toolsDirectory().resolve("quantum.ps1")).contains("lib\\quantum-cli-test.jar"));
        assertTrue(Files.readString(result.toolsDirectory().resolve("verify-distribution.ps1")).contains("Manifest SHA-256 mismatch"));
        assertTrue(Files.readString(result.toolsDirectory().resolve("product-smoke.ps1")).contains("Quantum distribution smoke passed."));
        assertTrue(Files.readString(result.toolsDirectory().resolve("product-smoke.ps1")).contains("verify-distribution.ps1"));
        assertTrue(Files.isRegularFile(result.examplesDirectory().resolve("openqasm2").resolve("bell.qasm")));
        assertTrue(Files.isRegularFile(result.reportDirectory().resolve("summary.md")));
        final String manifest = Files.readString(result.manifestPath());
        assertTrue(manifest.contains("format=quantum-product-distribution"));
        assertTrue(manifest.contains("file.0.path="));
        assertTrue(manifest.contains("sha256="));
        assertTrue(result.packagedFileCount() > 10);
        final ProductDistributionVerificationResult verification = new ProductDistributionVerifier().verify(result.outputDirectory());
        assertTrue(verification.isSuccess());
        assertTrue(verification.archivePresent());
        assertTrue(verification.verifiedFileCount() > 10L);
        try (ZipFile zip = new ZipFile(result.archivePath().toFile())) {
            assertTrue(zip.getEntry("distribution/README.md") != null);
            assertTrue(zip.getEntry("distribution/manifest.properties") != null);
            assertTrue(zip.getEntry("distribution/tools/verify-distribution.ps1") != null);
            assertTrue(zip.getEntry("distribution/lib/quantum-cli-test.jar") != null);
        }
    }

    @Test
    void failsWhenRequiredJarIsMissing() throws Exception {
        final Path project = completeProject();
        Files.delete(project.resolve("quantum-desktop").resolve("target").resolve("quantum-desktop-test.jar"));

        final IOException exception = assertThrows(
            IOException.class,
            () -> new ProductDistributionBundleWriter().write(
                tempDir.resolve("broken-distribution"),
                project
            )
        );

        assertTrue(exception.getMessage().contains("quantum-desktop-*.jar"));
    }

    @Test
    void verifierDetectsTamperedDistributionFile() throws Exception {
        final ProductDistributionBundleResult result = new ProductDistributionBundleWriter().write(
            tempDir.resolve("tamper-distribution"),
            completeProject()
        );
        Files.writeString(
            result.quickstartPath(),
            "tampered"
        );

        final ProductDistributionVerificationResult verification = new ProductDistributionVerifier().verify(result.outputDirectory());

        assertTrue(!verification.isSuccess());
        boolean hasSizeMismatch = false;
        for (int index = 0; index < verification.issues().size(); index++) {
            if (verification.issues().get(index).code().equals("MANIFEST_FILE_SIZE_MISMATCH")) {
                hasSizeMismatch = true;
            }
        }
        assertTrue(hasSizeMismatch);
    }

    private Path completeProject() throws Exception {
        final Path project = tempDir.resolve("project");
        Files.createDirectories(project);
        Files.writeString(
            project.resolve("LICENSE"),
            "MPL-2.0"
        );
        Files.writeString(
            project.resolve("README.md"),
            "# Quantum"
        );
        Files.createDirectories(project.resolve("examples").resolve("openqasm2"));
        Files.writeString(
            project.resolve("examples").resolve("openqasm2").resolve("bell.qasm"),
            "OPENQASM 2.0;"
        );
        writeTool(
            project,
            "quantum.ps1"
        );
        writeTool(
            project,
            "quantum-desktop.ps1"
        );
        writeTool(
            project,
            "product-smoke.ps1"
        );
        writeJar(
            project,
            "quantum-cli"
        );
        writeJar(
            project,
            "quantum-desktop"
        );
        return project;
    }

    private static void writeTool(
        final Path project,
        final String name
    ) throws Exception {
        final Path tool = project.resolve("tools").resolve(name);
        Files.createDirectories(tool.getParent());
        Files.writeString(
            tool,
            "Write-Output Quantum"
        );
    }

    private static void writeJar(
        final Path project,
        final String module
    ) throws Exception {
        final Path jar = project.resolve(module).resolve("target").resolve(module + "-test.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(
            jar,
            "jar"
        );
    }
}