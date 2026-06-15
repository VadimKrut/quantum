/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.verification;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Отчет проверки сохранения программы при cross-format конвертациях.
 */
public final class CrossFormatVerificationReport {

    private final IntegrationFormat inputFormat;
    private final boolean importSuccess;
    private final int importDiagnosticCount;
    private final ValidationResult validation;
    private final SimulationResult simulation;
    private final List<CrossFormatTargetVerification> targets;

    private CrossFormatVerificationReport(
        final IntegrationFormat inputFormat,
        final boolean importSuccess,
        final int importDiagnosticCount,
        final ValidationResult validation,
        final SimulationResult simulation,
        final List<CrossFormatTargetVerification> targets
    ) {
        this.inputFormat = inputFormat;
        this.importSuccess = importSuccess;
        this.importDiagnosticCount = importDiagnosticCount;
        this.validation = validation;
        this.simulation = simulation;
        this.targets = targets;
    }

    public static CrossFormatVerificationReport of(
        final IntegrationFormat inputFormat,
        final boolean importSuccess,
        final int importDiagnosticCount,
        final ValidationResult validation,
        final SimulationResult simulation,
        final List<CrossFormatTargetVerification> targets
    ) {
        if (inputFormat == null) {
            throw new IllegalArgumentException("Cross-format input format must not be null.");
        }
        if (importDiagnosticCount < 0) {
            throw new IllegalArgumentException("Cross-format import diagnostic count must not be negative.");
        }
        if (targets == null) {
            throw new IllegalArgumentException("Cross-format targets must not be null.");
        }
        return new CrossFormatVerificationReport(
            inputFormat,
            importSuccess,
            importDiagnosticCount,
            validation,
            simulation,
            List.copyOf(targets)
        );
    }

    public IntegrationFormat inputFormat() {
        return inputFormat;
    }

    public boolean importSuccess() {
        return importSuccess;
    }

    public int importDiagnosticCount() {
        return importDiagnosticCount;
    }

    public ValidationResult validation() {
        return validation;
    }

    public SimulationResult simulation() {
        return simulation;
    }

    public List<CrossFormatTargetVerification> targets() {
        return targets;
    }

    public boolean isSuccess() {
        if (
            !importSuccess
            || validation == null
            || !validation.isValid()
            || simulation == null
            || !simulation.isSuccess()
        ) {
            return false;
        }
        for (int i = 0; i < targets.size(); i++) {
            if (!targets.get(i).isSuccess()) {
                return false;
            }
        }
        return true;
    }
}