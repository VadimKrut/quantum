/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api.workflow;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Результат workflow: Quantum IR -> validation -> preflight -> export.
 */
public final class QuantumExportWorkflowResult {

    private final IntegrationFormat format;
    private final QuantumProgram program;
    private final ValidationResult validationResult;
    private final CapabilityPreflightResult preflightResult;
    private final ExportResult exportResult;

    private QuantumExportWorkflowResult(
        final IntegrationFormat format,
        final QuantumProgram program,
        final ValidationResult validationResult,
        final CapabilityPreflightResult preflightResult,
        final ExportResult exportResult
    ) {
        if (format == null) {
            throw new IllegalArgumentException("Export workflow format must not be null.");
        }
        if (program == null) {
            throw new IllegalArgumentException("Export workflow program must not be null.");
        }
        if (validationResult == null) {
            throw new IllegalArgumentException("Export workflow validation result must not be null.");
        }
        this.format = format;
        this.program = program;
        this.validationResult = validationResult;
        this.preflightResult = preflightResult;
        this.exportResult = exportResult;
    }

    /**
     * Создает результат workflow.
     *
     * @param format целевой формат
     * @param program Quantum IR программа
     * @param validationResult результат доменной валидации
     * @param preflightResult результат preflight или null, если workflow остановлен раньше
     * @param exportResult результат export или null, если workflow остановлен раньше
     * @return результат workflow
     */
    public static QuantumExportWorkflowResult of(
        final IntegrationFormat format,
        final QuantumProgram program,
        final ValidationResult validationResult,
        final CapabilityPreflightResult preflightResult,
        final ExportResult exportResult
    ) {
        return new QuantumExportWorkflowResult(
            format,
            program,
            validationResult,
            preflightResult,
            exportResult
        );
    }

    /**
     * Проверяет, что workflow полностью завершился успешно.
     *
     * @return true, если validation/preflight/export успешны
     */
    public boolean isSuccess() {
        return validationResult.isValid()
            && preflightResult != null
            && preflightResult.isSuccess()
            && exportResult != null
            && exportResult.isSuccess();
    }

    public IntegrationFormat format() {
        return format;
    }

    public QuantumProgram program() {
        return program;
    }

    public ValidationResult validationResult() {
        return validationResult;
    }

    public boolean hasPreflightResult() {
        return preflightResult != null;
    }

    public CapabilityPreflightResult preflightResult() {
        if (preflightResult == null) {
            throw new IllegalStateException("Export workflow does not contain preflight result.");
        }
        return preflightResult;
    }

    public boolean hasExportResult() {
        return exportResult != null;
    }

    public ExportResult exportResult() {
        if (exportResult == null) {
            throw new IllegalStateException("Export workflow does not contain export result.");
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