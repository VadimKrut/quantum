/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.report;

import java.nio.file.Path;

/**
 * Результат записи продуктового report bundle на диск.
 */
public final class ProductReportBundleResult {

    private final Path outputDirectory;
    private final Path auditJsonPath;
    private final Path summaryMarkdownPath;
    private final Path manifestPath;

    private ProductReportBundleResult(
        final Path outputDirectory,
        final Path auditJsonPath,
        final Path summaryMarkdownPath,
        final Path manifestPath
    ) {
        this.outputDirectory = outputDirectory;
        this.auditJsonPath = auditJsonPath;
        this.summaryMarkdownPath = summaryMarkdownPath;
        this.manifestPath = manifestPath;
    }

    public static ProductReportBundleResult of(
        final Path outputDirectory,
        final Path auditJsonPath,
        final Path summaryMarkdownPath,
        final Path manifestPath
    ) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Product report output directory must not be null.");
        }
        if (auditJsonPath == null) {
            throw new IllegalArgumentException("Product report audit JSON path must not be null.");
        }
        if (summaryMarkdownPath == null) {
            throw new IllegalArgumentException("Product report summary markdown path must not be null.");
        }
        if (manifestPath == null) {
            throw new IllegalArgumentException("Product report manifest path must not be null.");
        }
        return new ProductReportBundleResult(
            outputDirectory,
            auditJsonPath,
            summaryMarkdownPath,
            manifestPath
        );
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public Path auditJsonPath() {
        return auditJsonPath;
    }

    public Path summaryMarkdownPath() {
        return summaryMarkdownPath;
    }

    public Path manifestPath() {
        return manifestPath;
    }
}