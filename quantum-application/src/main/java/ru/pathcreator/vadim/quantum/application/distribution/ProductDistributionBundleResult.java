/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.distribution;

import java.nio.file.Path;
import java.util.List;

/**
 * Product distribution contract object.
 */
public final class ProductDistributionBundleResult {

    private final Path outputDirectory;
    private final Path archivePath;
    private final String archiveSha256;
    private final Path manifestPath;
    private final Path quickstartPath;
    private final Path licensePath;
    private final Path examplesDirectory;
    private final Path toolsDirectory;
    private final Path librariesDirectory;
    private final Path reportDirectory;
    private final List<Path> packagedFiles;

    private ProductDistributionBundleResult(
        final Path outputDirectory,
        final Path archivePath,
        final String archiveSha256,
        final Path manifestPath,
        final Path quickstartPath,
        final Path licensePath,
        final Path examplesDirectory,
        final Path toolsDirectory,
        final Path librariesDirectory,
        final Path reportDirectory,
        final List<Path> packagedFiles
    ) {
        this.outputDirectory = outputDirectory;
        this.archivePath = archivePath;
        this.archiveSha256 = archiveSha256;
        this.manifestPath = manifestPath;
        this.quickstartPath = quickstartPath;
        this.licensePath = licensePath;
        this.examplesDirectory = examplesDirectory;
        this.toolsDirectory = toolsDirectory;
        this.librariesDirectory = librariesDirectory;
        this.reportDirectory = reportDirectory;
        this.packagedFiles = packagedFiles;
    }

    public static ProductDistributionBundleResult of(
        final Path outputDirectory,
        final Path archivePath,
        final String archiveSha256,
        final Path manifestPath,
        final Path quickstartPath,
        final Path licensePath,
        final Path examplesDirectory,
        final Path toolsDirectory,
        final Path librariesDirectory,
        final Path reportDirectory,
        final List<Path> packagedFiles
    ) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Product distribution output directory must not be null.");
        }
        if (archivePath == null) {
            throw new IllegalArgumentException("Product distribution archive path must not be null.");
        }
        if (archiveSha256 == null || archiveSha256.isBlank()) {
            throw new IllegalArgumentException("Product distribution archive SHA-256 must not be blank.");
        }
        if (manifestPath == null) {
            throw new IllegalArgumentException("Product distribution manifest path must not be null.");
        }
        if (quickstartPath == null) {
            throw new IllegalArgumentException("Product distribution quickstart path must not be null.");
        }
        if (licensePath == null) {
            throw new IllegalArgumentException("Product distribution license path must not be null.");
        }
        if (examplesDirectory == null) {
            throw new IllegalArgumentException("Product distribution examples directory must not be null.");
        }
        if (toolsDirectory == null) {
            throw new IllegalArgumentException("Product distribution tools directory must not be null.");
        }
        if (librariesDirectory == null) {
            throw new IllegalArgumentException("Product distribution libraries directory must not be null.");
        }
        if (reportDirectory == null) {
            throw new IllegalArgumentException("Product distribution report directory must not be null.");
        }
        if (packagedFiles == null) {
            throw new IllegalArgumentException("Product distribution packaged files must not be null.");
        }
        return new ProductDistributionBundleResult(
            outputDirectory,
            archivePath,
            archiveSha256,
            manifestPath,
            quickstartPath,
            licensePath,
            examplesDirectory,
            toolsDirectory,
            librariesDirectory,
            reportDirectory,
            List.copyOf(packagedFiles)
        );
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public Path archivePath() {
        return archivePath;
    }

    public String archiveSha256() {
        return archiveSha256;
    }

    public Path manifestPath() {
        return manifestPath;
    }

    public Path quickstartPath() {
        return quickstartPath;
    }

    public Path licensePath() {
        return licensePath;
    }

    public Path examplesDirectory() {
        return examplesDirectory;
    }

    public Path toolsDirectory() {
        return toolsDirectory;
    }

    public Path librariesDirectory() {
        return librariesDirectory;
    }

    public Path reportDirectory() {
        return reportDirectory;
    }

    public List<Path> packagedFiles() {
        return packagedFiles;
    }

    public int packagedFileCount() {
        return packagedFiles.size();
    }
}