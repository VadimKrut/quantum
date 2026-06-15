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
 * Typed integrity report for an unpacked Quantum product distribution.
 */
public final class ProductDistributionVerificationResult {

    private final Path distributionDirectory;
    private final Path archivePath;
    private final boolean archivePresent;
    private final long verifiedFileCount;
    private final List<ProductDistributionVerificationIssue> issues;

    private ProductDistributionVerificationResult(
        final Path distributionDirectory,
        final Path archivePath,
        final boolean archivePresent,
        final long verifiedFileCount,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        this.distributionDirectory = distributionDirectory;
        this.archivePath = archivePath;
        this.archivePresent = archivePresent;
        this.verifiedFileCount = verifiedFileCount;
        this.issues = issues;
    }

    public static ProductDistributionVerificationResult of(
        final Path distributionDirectory,
        final Path archivePath,
        final boolean archivePresent,
        final long verifiedFileCount,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        if (distributionDirectory == null) {
            throw new IllegalArgumentException("Product distribution directory must not be null.");
        }
        if (archivePath == null) {
            throw new IllegalArgumentException("Product distribution archive path must not be null.");
        }
        if (verifiedFileCount < 0L) {
            throw new IllegalArgumentException("Verified file count must not be negative.");
        }
        if (issues == null) {
            throw new IllegalArgumentException("Product distribution verification issues must not be null.");
        }
        return new ProductDistributionVerificationResult(
            distributionDirectory,
            archivePath,
            archivePresent,
            verifiedFileCount,
            List.copyOf(issues)
        );
    }

    public Path distributionDirectory() {
        return distributionDirectory;
    }

    public Path archivePath() {
        return archivePath;
    }

    public boolean archivePresent() {
        return archivePresent;
    }

    public long verifiedFileCount() {
        return verifiedFileCount;
    }

    public List<ProductDistributionVerificationIssue> issues() {
        return issues;
    }

    public int issueCount() {
        return issues.size();
    }

    public boolean isSuccess() {
        return issues.isEmpty();
    }
}