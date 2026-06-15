/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.verification;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;

/**
 * Результат проверки одного направления IR -> target -> IR.
 */
public final class CrossFormatTargetVerification {

    private final IntegrationFormat targetFormat;
    private final boolean exportSuccess;
    private final boolean reimportSuccess;
    private final boolean validationSuccess;
    private final boolean simulationSuccess;
    private final boolean simulationEquivalent;
    private final int exportDiagnosticCount;
    private final int reimportDiagnosticCount;
    private final int validationErrorCount;

    private CrossFormatTargetVerification(
        final IntegrationFormat targetFormat,
        final boolean exportSuccess,
        final boolean reimportSuccess,
        final boolean validationSuccess,
        final boolean simulationSuccess,
        final boolean simulationEquivalent,
        final int exportDiagnosticCount,
        final int reimportDiagnosticCount,
        final int validationErrorCount
    ) {
        this.targetFormat = targetFormat;
        this.exportSuccess = exportSuccess;
        this.reimportSuccess = reimportSuccess;
        this.validationSuccess = validationSuccess;
        this.simulationSuccess = simulationSuccess;
        this.simulationEquivalent = simulationEquivalent;
        this.exportDiagnosticCount = exportDiagnosticCount;
        this.reimportDiagnosticCount = reimportDiagnosticCount;
        this.validationErrorCount = validationErrorCount;
    }

    public static CrossFormatTargetVerification of(
        final IntegrationFormat targetFormat,
        final boolean exportSuccess,
        final boolean reimportSuccess,
        final boolean validationSuccess,
        final boolean simulationSuccess,
        final boolean simulationEquivalent,
        final int exportDiagnosticCount,
        final int reimportDiagnosticCount,
        final int validationErrorCount
    ) {
        if (targetFormat == null) {
            throw new IllegalArgumentException("Cross-format target format must not be null.");
        }
        if (exportDiagnosticCount < 0) {
            throw new IllegalArgumentException("Cross-format export diagnostic count must not be negative.");
        }
        if (reimportDiagnosticCount < 0) {
            throw new IllegalArgumentException("Cross-format reimport diagnostic count must not be negative.");
        }
        if (validationErrorCount < 0) {
            throw new IllegalArgumentException("Cross-format validation error count must not be negative.");
        }
        return new CrossFormatTargetVerification(
            targetFormat,
            exportSuccess,
            reimportSuccess,
            validationSuccess,
            simulationSuccess,
            simulationEquivalent,
            exportDiagnosticCount,
            reimportDiagnosticCount,
            validationErrorCount
        );
    }

    public IntegrationFormat targetFormat() {
        return targetFormat;
    }

    public boolean isSuccess() {
        return exportSuccess
            && reimportSuccess
            && validationSuccess
            && simulationSuccess
            && simulationEquivalent;
    }

    public boolean exportSuccess() {
        return exportSuccess;
    }

    public boolean reimportSuccess() {
        return reimportSuccess;
    }

    public boolean validationSuccess() {
        return validationSuccess;
    }

    public boolean simulationSuccess() {
        return simulationSuccess;
    }

    public boolean simulationEquivalent() {
        return simulationEquivalent;
    }

    public int exportDiagnosticCount() {
        return exportDiagnosticCount;
    }

    public int reimportDiagnosticCount() {
        return reimportDiagnosticCount;
    }

    public int validationErrorCount() {
        return validationErrorCount;
    }
}