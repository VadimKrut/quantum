/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.readiness;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkReport;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionReport;

/**
 * Единый release-readiness отчет для продуктового smoke/release corpus.
 */
public final class ReleaseReadinessReport {

    private final ReleaseReadinessStatus status;
    private final List<ReleaseReadinessCheck> checks;
    private final CorpusRegressionReport corpusRegression;
    private final ProductBenchmarkReport benchmark;
    private final List<IntegrationCapabilityProfile> targetProfiles;

    private ReleaseReadinessReport(
        final ReleaseReadinessStatus status,
        final List<ReleaseReadinessCheck> checks,
        final CorpusRegressionReport corpusRegression,
        final ProductBenchmarkReport benchmark,
        final List<IntegrationCapabilityProfile> targetProfiles
    ) {
        this.status = status;
        this.checks = checks;
        this.corpusRegression = corpusRegression;
        this.benchmark = benchmark;
        this.targetProfiles = targetProfiles;
    }

    public static ReleaseReadinessReport of(
        final ReleaseReadinessStatus status,
        final List<ReleaseReadinessCheck> checks,
        final CorpusRegressionReport corpusRegression,
        final ProductBenchmarkReport benchmark,
        final List<IntegrationCapabilityProfile> targetProfiles
    ) {
        if (status == null) {
            throw new IllegalArgumentException("Release readiness status must not be null.");
        }
        if (checks == null) {
            throw new IllegalArgumentException("Release readiness checks must not be null.");
        }
        if (corpusRegression == null) {
            throw new IllegalArgumentException("Release readiness corpus regression must not be null.");
        }
        if (targetProfiles == null) {
            throw new IllegalArgumentException("Release readiness target profiles must not be null.");
        }
        return new ReleaseReadinessReport(
            status,
            List.copyOf(checks),
            corpusRegression,
            benchmark,
            List.copyOf(targetProfiles)
        );
    }

    public ReleaseReadinessStatus status() {
        return status;
    }

    public boolean isReady() {
        return status == ReleaseReadinessStatus.READY;
    }

    public boolean isAcceptable() {
        return status == ReleaseReadinessStatus.READY
            || status == ReleaseReadinessStatus.READY_WITH_WARNINGS;
    }

    public List<ReleaseReadinessCheck> checks() {
        return checks;
    }

    public int failedCheckCount() {
        int count = 0;
        for (int i = 0; i < checks.size(); i++) {
            if (checks.get(i).isFail()) {
                count++;
            }
        }
        return count;
    }

    public int warningCheckCount() {
        int count = 0;
        for (int i = 0; i < checks.size(); i++) {
            if (checks.get(i).status() == ReleaseReadinessCheckStatus.WARN) {
                count++;
            }
        }
        return count;
    }

    public CorpusRegressionReport corpusRegression() {
        return corpusRegression;
    }

    public boolean hasBenchmark() {
        return benchmark != null;
    }

    public ProductBenchmarkReport benchmark() {
        if (benchmark == null) {
            throw new IllegalStateException("Release readiness report does not contain benchmark.");
        }
        return benchmark;
    }

    public List<IntegrationCapabilityProfile> targetProfiles() {
        return targetProfiles;
    }
}