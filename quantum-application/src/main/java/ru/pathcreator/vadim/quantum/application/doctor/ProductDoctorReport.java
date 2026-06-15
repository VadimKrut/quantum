/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.doctor;

import java.nio.file.Path;
import java.util.List;

/**
 * Отчет о готовности локальной структуры Quantum продукта.
 */
public final class ProductDoctorReport {

    private final Path projectRoot;
    private final List<ProductDoctorCheck> checks;

    private ProductDoctorReport(
        final Path projectRoot,
        final List<ProductDoctorCheck> checks
    ) {
        this.projectRoot = projectRoot;
        this.checks = checks;
    }

    public static ProductDoctorReport of(
        final Path projectRoot,
        final List<ProductDoctorCheck> checks
    ) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("Product doctor project root must not be null.");
        }
        if (checks == null) {
            throw new IllegalArgumentException("Product doctor checks must not be null.");
        }
        return new ProductDoctorReport(
            projectRoot,
            List.copyOf(checks)
        );
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public List<ProductDoctorCheck> checks() {
        return checks;
    }

    public int checkCount() {
        return checks.size();
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
            if (checks.get(i).isWarn()) {
                count++;
            }
        }
        return count;
    }

    public ProductDoctorStatus status() {
        if (failedCheckCount() > 0) {
            return ProductDoctorStatus.BROKEN;
        }
        if (warningCheckCount() > 0) {
            return ProductDoctorStatus.HEALTHY_WITH_WARNINGS;
        }
        return ProductDoctorStatus.HEALTHY;
    }

    public boolean isSuccess() {
        return failedCheckCount() == 0;
    }

    public boolean isHealthy() {
        return status() == ProductDoctorStatus.HEALTHY;
    }

    public boolean isAcceptable() {
        return status() != ProductDoctorStatus.BROKEN;
    }
}