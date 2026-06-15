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

import ru.pathcreator.vadim.quantum.application.doctor.ProductDoctorReport;
import ru.pathcreator.vadim.quantum.application.readiness.ReleaseReadinessReport;

/**
 * Единый продуктовый отчет для локальной структуры, corpus regression и release-readiness gate.
 */
public final class ProductAuditReport {

    private final Path projectRoot;
    private final ProductAuditStatus status;
    private final ProductDoctorReport doctor;
    private final ReleaseReadinessReport readiness;

    private ProductAuditReport(
        final Path projectRoot,
        final ProductAuditStatus status,
        final ProductDoctorReport doctor,
        final ReleaseReadinessReport readiness
    ) {
        this.projectRoot = projectRoot;
        this.status = status;
        this.doctor = doctor;
        this.readiness = readiness;
    }

    public static ProductAuditReport of(
        final Path projectRoot,
        final ProductDoctorReport doctor,
        final ReleaseReadinessReport readiness
    ) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("Product audit project root must not be null.");
        }
        if (doctor == null) {
            throw new IllegalArgumentException("Product audit doctor report must not be null.");
        }
        if (readiness == null) {
            throw new IllegalArgumentException("Product audit readiness report must not be null.");
        }
        return new ProductAuditReport(
            projectRoot,
            deriveStatus(
                doctor,
                readiness
            ),
            doctor,
            readiness
        );
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public ProductAuditStatus status() {
        return status;
    }

    public boolean isReady() {
        return status == ProductAuditStatus.READY;
    }

    public boolean isAcceptable() {
        return status == ProductAuditStatus.READY
            || status == ProductAuditStatus.READY_WITH_WARNINGS;
    }

    public int failedCheckCount() {
        return doctor.failedCheckCount() + readiness.failedCheckCount();
    }

    public int warningCheckCount() {
        return doctor.warningCheckCount() + readiness.warningCheckCount();
    }

    public ProductDoctorReport doctor() {
        return doctor;
    }

    public ReleaseReadinessReport readiness() {
        return readiness;
    }

    private static ProductAuditStatus deriveStatus(
        final ProductDoctorReport doctor,
        final ReleaseReadinessReport readiness
    ) {
        if (!doctor.isAcceptable() || !readiness.isAcceptable()) {
            return ProductAuditStatus.NOT_READY;
        }
        if (doctor.warningCheckCount() > 0 || readiness.warningCheckCount() > 0) {
            return ProductAuditStatus.READY_WITH_WARNINGS;
        }
        return ProductAuditStatus.READY;
    }
}