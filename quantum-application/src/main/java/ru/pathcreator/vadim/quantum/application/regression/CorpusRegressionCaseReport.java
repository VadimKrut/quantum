/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.regression;

import ru.pathcreator.vadim.quantum.application.compatibility.ProductCompatibilityMatrix;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.verification.CrossFormatVerificationReport;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Regression-результат одного corpus case.
 */
public final class CorpusRegressionCaseReport {

    private final String name;
    private final IntegrationFormat inputFormat;
    private final boolean importSuccess;
    private final int importDiagnosticCount;
    private final ValidationResult validation;
    private final ProductCompatibilityMatrix compatibilityMatrix;
    private final CrossFormatVerificationReport crossFormatVerification;

    private CorpusRegressionCaseReport(
        final String name,
        final IntegrationFormat inputFormat,
        final boolean importSuccess,
        final int importDiagnosticCount,
        final ValidationResult validation,
        final ProductCompatibilityMatrix compatibilityMatrix,
        final CrossFormatVerificationReport crossFormatVerification
    ) {
        this.name = name;
        this.inputFormat = inputFormat;
        this.importSuccess = importSuccess;
        this.importDiagnosticCount = importDiagnosticCount;
        this.validation = validation;
        this.compatibilityMatrix = compatibilityMatrix;
        this.crossFormatVerification = crossFormatVerification;
    }

    public static CorpusRegressionCaseReport of(
        final String name,
        final IntegrationFormat inputFormat,
        final boolean importSuccess,
        final int importDiagnosticCount,
        final ValidationResult validation,
        final ProductCompatibilityMatrix compatibilityMatrix,
        final CrossFormatVerificationReport crossFormatVerification
    ) {
        if (
            name == null
            || name.isBlank()
        ) {
            throw new IllegalArgumentException("Corpus regression report name must not be blank.");
        }
        if (inputFormat == null) {
            throw new IllegalArgumentException("Corpus regression input format must not be null.");
        }
        if (importDiagnosticCount < 0) {
            throw new IllegalArgumentException("Corpus regression import diagnostic count must not be negative.");
        }
        return new CorpusRegressionCaseReport(
            name,
            inputFormat,
            importSuccess,
            importDiagnosticCount,
            validation,
            compatibilityMatrix,
            crossFormatVerification
        );
    }

    public String name() {
        return name;
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

    public ProductCompatibilityMatrix compatibilityMatrix() {
        return compatibilityMatrix;
    }

    public CrossFormatVerificationReport crossFormatVerification() {
        return crossFormatVerification;
    }

    public boolean isSuccess() {
        return importSuccess
            && validation != null
            && validation.isValid()
            && compatibilityMatrix != null
            && compatibilityMatrix.isSuccess()
            && crossFormatVerification != null
            && crossFormatVerification.isSuccess();
    }
}