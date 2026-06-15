/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.audit;

import java.nio.file.Path;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorReport;
import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorRunner;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessRunner;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;

/**
 * Собирает общий продуктовый audit gate из локальной структуры и release-readiness pipeline.
 */
public final class ProductAuditRunner {

    private final ProductDoctorRunner doctorRunner;
    private final ReleaseReadinessRunner readinessRunner;

    public ProductAuditRunner() {
        this(
            new ProductDoctorRunner(),
            new ReleaseReadinessRunner()
        );
    }

    public ProductAuditRunner(
        final ProductDoctorRunner doctorRunner,
        final ReleaseReadinessRunner readinessRunner
    ) {
        if (doctorRunner == null) {
            throw new IllegalArgumentException("Product audit doctor runner must not be null.");
        }
        if (readinessRunner == null) {
            throw new IllegalArgumentException("Product audit readiness runner must not be null.");
        }
        this.doctorRunner = doctorRunner;
        this.readinessRunner = readinessRunner;
    }

    public ProductAuditReport run(
        final Path projectRoot,
        final List<CorpusRegressionCase> cases,
        final List<QuantumIntegration> targetIntegrations,
        final QuantumIntegration benchmarkTarget,
        final ProductBenchmarkOptions benchmarkOptions
    ) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("Product audit project root must not be null.");
        }
        final Path root = projectRoot.toAbsolutePath().normalize();
        final ProductDoctorReport doctor = doctorRunner.run(root);
        final ReleaseReadinessReport readiness = readinessRunner.run(
            cases,
            targetIntegrations,
            benchmarkTarget,
            benchmarkOptions
        );
        return ProductAuditReport.of(
            root,
            doctor,
            readiness
        );
    }
}