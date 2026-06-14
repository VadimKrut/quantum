/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api.workflow;

import java.nio.file.Path;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Результат workflow: JSON file -> read -> validation -> preflight -> export.
 */
public final class QuantumJsonExportWorkflowResult {

    private final IntegrationFormat format;
    private final Path path;
    private final QuantumIrReadResult readResult;
    private final ValidationResult validationResult;
    private final CapabilityPreflightResult preflightResult;
    private final ExportResult exportResult;

    private QuantumJsonExportWorkflowResult(
        final IntegrationFormat format,
        final Path path,
        final QuantumIrReadResult readResult,
        final ValidationResult validationResult,
        final CapabilityPreflightResult preflightResult,
        final ExportResult exportResult
    ) {
        if (format == null) {
            throw new IllegalArgumentException("JSON export workflow format must not be null.");
        }
        if (path == null) {
            throw new IllegalArgumentException("JSON export workflow path must not be null.");
        }
        if (readResult == null) {
            throw new IllegalArgumentException("JSON export workflow read result must not be null.");
        }
        this.format = format;
        this.path = path;
        this.readResult = readResult;
        this.validationResult = validationResult;
        this.preflightResult = preflightResult;
        this.exportResult = exportResult;
    }

    /**
     * Создает результат workflow.
     *
     * @param format целевой формат
     * @param path путь JSON-файла
     * @param readResult результат чтения JSON
     * @param validationResult результат валидации или null, если workflow остановлен раньше
     * @param preflightResult результат preflight или null, если workflow остановлен раньше
     * @param exportResult результат export или null, если workflow остановлен раньше
     * @return результат workflow
     */
    public static QuantumJsonExportWorkflowResult of(
        final IntegrationFormat format,
        final Path path,
        final QuantumIrReadResult readResult,
        final ValidationResult validationResult,
        final CapabilityPreflightResult preflightResult,
        final ExportResult exportResult
    ) {
        return new QuantumJsonExportWorkflowResult(
            format,
            path,
            readResult,
            validationResult,
            preflightResult,
            exportResult
        );
    }

    public boolean isSuccess() {
        return readResult.isSuccess()
            && validationResult != null
            && validationResult.isValid()
            && preflightResult != null
            && preflightResult.isSuccess()
            && exportResult != null
            && exportResult.isSuccess();
    }

    public IntegrationFormat format() {
        return format;
    }

    public Path path() {
        return path;
    }

    public QuantumIrReadResult readResult() {
        return readResult;
    }

    public boolean hasProgram() {
        return readResult.hasProgram();
    }

    public QuantumProgram program() {
        return readResult.program();
    }

    public boolean hasValidationResult() {
        return validationResult != null;
    }

    public ValidationResult validationResult() {
        if (validationResult == null) {
            throw new IllegalStateException("JSON export workflow does not contain validation result.");
        }
        return validationResult;
    }

    public boolean hasPreflightResult() {
        return preflightResult != null;
    }

    public CapabilityPreflightResult preflightResult() {
        if (preflightResult == null) {
            throw new IllegalStateException("JSON export workflow does not contain preflight result.");
        }
        return preflightResult;
    }

    public boolean hasExportResult() {
        return exportResult != null;
    }

    public ExportResult exportResult() {
        if (exportResult == null) {
            throw new IllegalStateException("JSON export workflow does not contain export result.");
        }
        return exportResult;
    }

    public boolean hasContent() {
        return exportResult != null && exportResult.hasContent();
    }

    public String content() {
        return exportResult().content();
    }
}