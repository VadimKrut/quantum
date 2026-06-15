/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.readiness;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkReport;
import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkRunner;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionReport;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionRunner;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Собирает release-readiness отчет из corpus regression, benchmark и target profile coverage.
 */
public final class ReleaseReadinessRunner {

    private final CorpusRegressionRunner corpusRunner;
    private final ProductBenchmarkRunner benchmarkRunner;

    public ReleaseReadinessRunner() {
        this(
            new CorpusRegressionRunner(),
            new ProductBenchmarkRunner()
        );
    }

    public ReleaseReadinessRunner(
        final CorpusRegressionRunner corpusRunner,
        final ProductBenchmarkRunner benchmarkRunner
    ) {
        if (corpusRunner == null) {
            throw new IllegalArgumentException("Release readiness corpus runner must not be null.");
        }
        if (benchmarkRunner == null) {
            throw new IllegalArgumentException("Release readiness benchmark runner must not be null.");
        }
        this.corpusRunner = corpusRunner;
        this.benchmarkRunner = benchmarkRunner;
    }

    public ReleaseReadinessReport run(
        final List<CorpusRegressionCase> cases,
        final List<QuantumIntegration> targetIntegrations,
        final QuantumIntegration benchmarkTarget,
        final ProductBenchmarkOptions benchmarkOptions
    ) {
        if (cases == null) {
            throw new IllegalArgumentException("Release readiness cases must not be null.");
        }
        if (targetIntegrations == null) {
            throw new IllegalArgumentException("Release readiness target integrations must not be null.");
        }
        if (benchmarkTarget == null) {
            throw new IllegalArgumentException("Release readiness benchmark target must not be null.");
        }
        if (benchmarkOptions == null) {
            throw new IllegalArgumentException("Release readiness benchmark options must not be null.");
        }
        final ArrayList<ReleaseReadinessCheck> checks = new ArrayList<>();
        final ArrayList<IntegrationCapabilityProfile> targetProfiles = targetProfiles(targetIntegrations);
        checks.add(checkTargetProfiles(targetProfiles));
        final CorpusRegressionReport corpus = corpusRunner.run(
            cases,
            targetIntegrations,
            benchmarkOptions.workflowOptions()
        );
        checks.add(checkCorpus(corpus));
        final ProductBenchmarkReport benchmark = benchmarkFirstCase(
            cases,
            benchmarkTarget,
            benchmarkOptions
        );
        checks.add(checkBenchmark(benchmark));
        return ReleaseReadinessReport.of(
            deriveStatus(checks),
            checks,
            corpus,
            benchmark,
            targetProfiles
        );
    }

    private ProductBenchmarkReport benchmarkFirstCase(
        final List<CorpusRegressionCase> cases,
        final QuantumIntegration benchmarkTarget,
        final ProductBenchmarkOptions benchmarkOptions
    ) {
        if (cases.isEmpty()) {
            return null;
        }
        final CorpusRegressionCase regressionCase = cases.get(0);
        if (regressionCase == null) {
            throw new IllegalArgumentException("Release readiness case must not be null.");
        }
        final ImportResult imported = regressionCase.inputIntegration().importProgram(
            regressionCase.source(),
            ImportOptions.defaults()
        );
        if (!imported.isSuccess()) {
            return null;
        }
        final QuantumProgram program = imported.program();
        return benchmarkRunner.run(
            program,
            benchmarkTarget,
            benchmarkOptions
        );
    }

    private static ArrayList<IntegrationCapabilityProfile> targetProfiles(
        final List<QuantumIntegration> targetIntegrations
    ) {
        final ArrayList<IntegrationCapabilityProfile> profiles = new ArrayList<>(targetIntegrations.size());
        for (int i = 0; i < targetIntegrations.size(); i++) {
            final QuantumIntegration integration = targetIntegrations.get(i);
            if (integration == null) {
                throw new IllegalArgumentException("Release readiness target integration must not be null.");
            }
            profiles.add(integration.capabilityProfile());
        }
        return profiles;
    }

    private static ReleaseReadinessCheck checkTargetProfiles(final List<IntegrationCapabilityProfile> profiles) {
        if (profiles.isEmpty()) {
            return ReleaseReadinessCheck.of(
                "target-profiles",
                ReleaseReadinessCheckStatus.FAIL,
                "No target profiles were provided."
            );
        }
        for (int i = 0; i < profiles.size(); i++) {
            final IntegrationCapabilityProfile profile = profiles.get(i);
            if (profile.capabilities().isEmpty()) {
                return ReleaseReadinessCheck.of(
                    "target-profiles",
                    ReleaseReadinessCheckStatus.WARN,
                    "At least one target profile has no declared capabilities."
                );
            }
        }
        return ReleaseReadinessCheck.of(
            "target-profiles",
            ReleaseReadinessCheckStatus.PASS,
            "Target profiles are present."
        );
    }

    private static ReleaseReadinessCheck checkCorpus(final CorpusRegressionReport corpus) {
        if (corpus.caseCount() == 0) {
            return ReleaseReadinessCheck.of(
                "corpus-regression",
                ReleaseReadinessCheckStatus.FAIL,
                "Corpus regression has no cases."
            );
        }
        if (!corpus.isSuccess()) {
            return ReleaseReadinessCheck.of(
                "corpus-regression",
                ReleaseReadinessCheckStatus.FAIL,
                "Corpus regression failed for " + corpus.failureCount() + " case(s)."
            );
        }
        return ReleaseReadinessCheck.of(
            "corpus-regression",
            ReleaseReadinessCheckStatus.PASS,
            "Corpus regression passed for " + corpus.caseCount() + " case(s)."
        );
    }

    private static ReleaseReadinessCheck checkBenchmark(final ProductBenchmarkReport benchmark) {
        if (benchmark == null) {
            return ReleaseReadinessCheck.of(
                "benchmark",
                ReleaseReadinessCheckStatus.WARN,
                "Benchmark was not available because the first corpus case did not import."
            );
        }
        if (!benchmark.isSuccess()) {
            return ReleaseReadinessCheck.of(
                "benchmark",
                ReleaseReadinessCheckStatus.FAIL,
                "Benchmark failed."
            );
        }
        return ReleaseReadinessCheck.of(
            "benchmark",
            ReleaseReadinessCheckStatus.PASS,
            "Benchmark passed with " + benchmark.stageCount() + " stage(s)."
        );
    }

    private static ReleaseReadinessStatus deriveStatus(final List<ReleaseReadinessCheck> checks) {
        boolean hasWarning = false;
        for (int i = 0; i < checks.size(); i++) {
            final ReleaseReadinessCheck check = checks.get(i);
            if (check.isFail()) {
                return ReleaseReadinessStatus.NOT_READY;
            }
            if (check.status() == ReleaseReadinessCheckStatus.WARN) {
                hasWarning = true;
            }
        }
        return hasWarning
            ? ReleaseReadinessStatus.READY_WITH_WARNINGS
            : ReleaseReadinessStatus.READY;
    }
}