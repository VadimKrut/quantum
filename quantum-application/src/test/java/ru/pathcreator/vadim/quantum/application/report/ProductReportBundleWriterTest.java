/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.report;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.application.audit.ProductAuditReport;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorCheck;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorCheckStatus;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessCheck;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessCheckStatus;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessStatus;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionReport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductReportBundleWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    void writesAuditJsonAndMarkdownSummary() throws Exception {
        final ProductAuditReport audit = auditReport();

        final ProductReportBundleResult result = new ProductReportBundleWriter().write(
            tempDir.resolve("report"),
            audit,
            "{\"status\":\"READY\"}"
        );

        assertTrue(Files.isRegularFile(result.auditJsonPath()));
        assertTrue(Files.isRegularFile(result.summaryMarkdownPath()));
        assertTrue(Files.isRegularFile(result.manifestPath()));
        assertEquals(
            "{\"status\":\"READY\"}",
            Files.readString(result.auditJsonPath())
        );
        final String summary = Files.readString(result.summaryMarkdownPath());
        assertTrue(summary.contains("# Quantum Product Report"));
        assertTrue(summary.contains("Status: READY"));
        assertTrue(summary.contains("Product Doctor"));
        assertTrue(summary.contains("Release Readiness"));
        final String manifest = Files.readString(result.manifestPath());
        assertTrue(manifest.contains("format=quantum-product-report-bundle"));
        assertTrue(manifest.contains("auditJsonSha256="));
        assertTrue(manifest.contains("summarySha256="));
    }

    @Test
    void buildsMarkdownSummaryWithoutWriting() {
        final String summary = new ProductReportBundleWriter().summaryMarkdown(auditReport());

        assertTrue(summary.contains("| Check | Status | Message |"));
        assertTrue(summary.contains("pom"));
        assertTrue(summary.contains("corpus-regression"));
    }

    private static ProductAuditReport auditReport() {
        final ProductDoctorReport doctor = ProductDoctorReport.of(
            Path.of("project").toAbsolutePath(),
            List.of(ProductDoctorCheck.of(
                "pom",
                ProductDoctorCheckStatus.PASS,
                "pom.xml is present."
            ))
        );
        final ReleaseReadinessReport readiness = ReleaseReadinessReport.of(
            ReleaseReadinessStatus.READY,
            List.of(ReleaseReadinessCheck.of(
                "corpus-regression",
                ReleaseReadinessCheckStatus.PASS,
                "Corpus regression passed."
            )),
            CorpusRegressionReport.of(List.of()),
            null,
            List.of()
        );
        return ProductAuditReport.of(
            Path.of("project").toAbsolutePath(),
            doctor,
            readiness
        );
    }
}