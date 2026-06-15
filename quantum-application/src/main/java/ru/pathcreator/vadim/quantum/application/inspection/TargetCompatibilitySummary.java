/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.inspection;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;

/**
 * Сводка совместимости программы с target profile.
 */
public final class TargetCompatibilitySummary {

    private final String targetName;
    private final String targetVersion;
    private final CapabilityPreflightStatus status;
    private final int diagnosticCount;
    private final boolean exportable;
    private final boolean loweringRequired;

    private TargetCompatibilitySummary(
        final String targetName,
        final String targetVersion,
        final CapabilityPreflightStatus status,
        final int diagnosticCount,
        final boolean exportable,
        final boolean loweringRequired
    ) {
        this.targetName = targetName;
        this.targetVersion = targetVersion;
        this.status = status;
        this.diagnosticCount = diagnosticCount;
        this.exportable = exportable;
        this.loweringRequired = loweringRequired;
    }

    public static TargetCompatibilitySummary of(
        final IntegrationCapabilityProfile profile,
        final CapabilityPreflightResult preflightResult
    ) {
        if (profile == null) {
            throw new IllegalArgumentException("Target compatibility profile must not be null.");
        }
        if (preflightResult == null) {
            throw new IllegalArgumentException("Target compatibility preflight result must not be null.");
        }
        return new TargetCompatibilitySummary(
            profile.targetName(),
            profile.targetVersion(),
            preflightResult.status(),
            preflightResult.diagnostics().size(),
            preflightResult.isSuccess(),
            preflightResult.requiresLowering()
        );
    }

    public String targetName() {
        return targetName;
    }

    public String targetVersion() {
        return targetVersion;
    }

    public CapabilityPreflightStatus status() {
        return status;
    }

    public int diagnosticCount() {
        return diagnosticCount;
    }

    public boolean isExportable() {
        return exportable;
    }

    public boolean requiresLowering() {
        return loweringRequired;
    }
}