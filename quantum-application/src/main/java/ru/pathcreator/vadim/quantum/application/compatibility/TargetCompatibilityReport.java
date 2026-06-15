/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compatibility;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerResultStatus;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowStatus;

/**
 * Совместимость одной Quantum IR программы с одним внешним target.
 */
public final class TargetCompatibilityReport {

    private final IntegrationFormat targetFormat;
    private final IntegrationCapabilityProfile targetProfile;
    private final TargetCompatibilityStatus status;
    private final CapabilityPreflightStatus preflightStatus;
    private final CompilerResultStatus compilerStatus;
    private final ProductWorkflowStatus workflowStatus;
    private final List<CompatibilityCheckResult> checks;

    private TargetCompatibilityReport(
        final IntegrationFormat targetFormat,
        final IntegrationCapabilityProfile targetProfile,
        final TargetCompatibilityStatus status,
        final CapabilityPreflightStatus preflightStatus,
        final CompilerResultStatus compilerStatus,
        final ProductWorkflowStatus workflowStatus,
        final List<CompatibilityCheckResult> checks
    ) {
        this.targetFormat = targetFormat;
        this.targetProfile = targetProfile;
        this.status = status;
        this.preflightStatus = preflightStatus;
        this.compilerStatus = compilerStatus;
        this.workflowStatus = workflowStatus;
        this.checks = checks;
    }

    /**
     * Создает отчет совместимости с target.
     *
     * @param targetFormat внешний target format
     * @param targetProfile capability profile target-а
     * @param status итоговый статус
     * @param preflightStatus итоговый preflight status
     * @param compilerStatus итоговый compiler status
     * @param workflowStatus итоговый workflow status
     * @param checks подробные проверки
     * @return отчет совместимости
     */
    public static TargetCompatibilityReport of(
        final IntegrationFormat targetFormat,
        final IntegrationCapabilityProfile targetProfile,
        final TargetCompatibilityStatus status,
        final CapabilityPreflightStatus preflightStatus,
        final CompilerResultStatus compilerStatus,
        final ProductWorkflowStatus workflowStatus,
        final List<CompatibilityCheckResult> checks
    ) {
        if (targetFormat == null) {
            throw new IllegalArgumentException("Compatibility target format must not be null.");
        }
        if (targetProfile == null) {
            throw new IllegalArgumentException("Compatibility target profile must not be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Compatibility target status must not be null.");
        }
        if (checks == null) {
            throw new IllegalArgumentException("Compatibility checks must not be null.");
        }
        return new TargetCompatibilityReport(
            targetFormat,
            targetProfile,
            status,
            preflightStatus,
            compilerStatus,
            workflowStatus,
            List.copyOf(checks)
        );
    }

    public IntegrationFormat targetFormat() {
        return targetFormat;
    }

    public IntegrationCapabilityProfile targetProfile() {
        return targetProfile;
    }

    public TargetCompatibilityStatus status() {
        return status;
    }

    public boolean isSuccess() {
        return status == TargetCompatibilityStatus.EXPORTABLE;
    }

    public CapabilityPreflightStatus preflightStatus() {
        return preflightStatus;
    }

    public CompilerResultStatus compilerStatus() {
        return compilerStatus;
    }

    public ProductWorkflowStatus workflowStatus() {
        return workflowStatus;
    }

    public List<CompatibilityCheckResult> checks() {
        return checks;
    }
}