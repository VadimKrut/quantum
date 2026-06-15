/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ru.pathcreator.vadim.quantum.application.audit.ProductAuditReport;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorCheck;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessCheck;

/**
 * Пишет воспроизводимый product-report bundle: audit JSON, summary и manifest.
 */
public final class ProductReportBundleWriter {

    private static final String AUDIT_JSON_FILE = "product-audit.json";
    private static final String SUMMARY_FILE = "summary.md";
    private static final String MANIFEST_FILE = "manifest.properties";

    public ProductReportBundleResult write(
        final Path outputDirectory,
        final ProductAuditReport audit,
        final String auditJson
    ) throws IOException {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Product report output directory must not be null.");
        }
        if (audit == null) {
            throw new IllegalArgumentException("Product report audit must not be null.");
        }
        if (auditJson == null || auditJson.isBlank()) {
            throw new IllegalArgumentException("Product report audit JSON must not be blank.");
        }
        final Path directory = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        final Path auditJsonPath = directory.resolve(AUDIT_JSON_FILE);
        final Path summaryPath = directory.resolve(SUMMARY_FILE);
        final Path manifestPath = directory.resolve(MANIFEST_FILE);
        Files.writeString(
            auditJsonPath,
            auditJson,
            StandardCharsets.UTF_8
        );
        Files.writeString(
            summaryPath,
            summaryMarkdown(audit),
            StandardCharsets.UTF_8
        );
        Files.writeString(
            manifestPath,
            manifest(
                audit,
                auditJsonPath,
                summaryPath
            ),
            StandardCharsets.UTF_8
        );
        return ProductReportBundleResult.of(
            directory,
            auditJsonPath,
            summaryPath,
            manifestPath
        );
    }

    public String summaryMarkdown(final ProductAuditReport audit) {
        if (audit == null) {
            throw new IllegalArgumentException("Product report audit must not be null.");
        }
        final StringBuilder summary = new StringBuilder(2048);
        summary.append("# Quantum Product Report").append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("- Status: ").append(audit.status()).append(System.lineSeparator())
            .append("- Project root: ").append(audit.projectRoot()).append(System.lineSeparator())
            .append("- Failed checks: ").append(audit.failedCheckCount()).append(System.lineSeparator())
            .append("- Warnings: ").append(audit.warningCheckCount()).append(System.lineSeparator())
            .append(System.lineSeparator());
        appendDoctor(summary, audit);
        appendReadiness(summary, audit);
        return summary.toString();
    }

    private static void appendDoctor(
        final StringBuilder summary,
        final ProductAuditReport audit
    ) {
        summary.append("## Product Doctor").append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("- Status: ").append(audit.doctor().status()).append(System.lineSeparator())
            .append("- Failed checks: ").append(audit.doctor().failedCheckCount()).append(System.lineSeparator())
            .append("- Warnings: ").append(audit.doctor().warningCheckCount()).append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("| Check | Status | Message |").append(System.lineSeparator())
            .append("| --- | --- | --- |").append(System.lineSeparator());
        for (int index = 0; index < audit.doctor().checks().size(); index++) {
            final ProductDoctorCheck check = audit.doctor().checks().get(index);
            summary.append("| ")
                .append(escape(check.name()))
                .append(" | ")
                .append(check.status())
                .append(" | ")
                .append(escape(check.message()))
                .append(" |")
                .append(System.lineSeparator());
        }
        summary.append(System.lineSeparator());
    }

    private static void appendReadiness(
        final StringBuilder summary,
        final ProductAuditReport audit
    ) {
        summary.append("## Release Readiness").append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("- Status: ").append(audit.readiness().status()).append(System.lineSeparator())
            .append("- Corpus cases: ").append(audit.readiness().corpusRegression().caseCount()).append(System.lineSeparator())
            .append("- Corpus failures: ").append(audit.readiness().corpusRegression().failureCount()).append(System.lineSeparator());
        if (audit.readiness().hasBenchmark()) {
            summary.append("- Benchmark stages: ").append(audit.readiness().benchmark().stageCount()).append(System.lineSeparator());
        } else {
            summary.append("- Benchmark stages: not available").append(System.lineSeparator());
        }
        summary.append(System.lineSeparator())
            .append("| Check | Status | Message |").append(System.lineSeparator())
            .append("| --- | --- | --- |").append(System.lineSeparator());
        for (int index = 0; index < audit.readiness().checks().size(); index++) {
            final ReleaseReadinessCheck check = audit.readiness().checks().get(index);
            summary.append("| ")
                .append(escape(check.name()))
                .append(" | ")
                .append(check.status())
                .append(" | ")
                .append(escape(check.message()))
                .append(" |")
                .append(System.lineSeparator());
        }
    }

    private static String escape(final String value) {
        return value.replace(
            "|",
            "\\|"
        );
    }

    private static String manifest(
        final ProductAuditReport audit,
        final Path auditJsonPath,
        final Path summaryPath
    ) throws IOException {
        final StringBuilder manifest = new StringBuilder(512);
        manifest.append("format=quantum-product-report-bundle").append(System.lineSeparator())
            .append("version=1").append(System.lineSeparator())
            .append("status=").append(audit.status()).append(System.lineSeparator())
            .append("failedCheckCount=").append(audit.failedCheckCount()).append(System.lineSeparator())
            .append("warningCheckCount=").append(audit.warningCheckCount()).append(System.lineSeparator())
            .append("auditJsonFile=").append(AUDIT_JSON_FILE).append(System.lineSeparator())
            .append("auditJsonBytes=").append(Files.size(auditJsonPath)).append(System.lineSeparator())
            .append("auditJsonSha256=").append(sha256(auditJsonPath)).append(System.lineSeparator())
            .append("summaryFile=").append(SUMMARY_FILE).append(System.lineSeparator())
            .append("summaryBytes=").append(Files.size(summaryPath)).append(System.lineSeparator())
            .append("summarySha256=").append(sha256(summaryPath)).append(System.lineSeparator());
        return manifest.toString();
    }

    private static String sha256(final Path path) throws IOException {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(Files.readAllBytes(path));
            final StringBuilder value = new StringBuilder(hash.length * 2);
            for (int i = 0; i < hash.length; i++) {
                final String hex = Integer.toHexString(hash[i] & 0xff);
                if (hex.length() == 1) {
                    value.append('0');
                }
                value.append(hex);
            }
            return value.toString();
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 digest is not available.",
                exception
            );
        }
    }
}