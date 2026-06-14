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

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrFileWriteResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Результат workflow: external text -> import -> validation -> JSON file.
 */
public final class QuantumImportJsonWorkflowResult {

    private final IntegrationFormat format;
    private final Path path;
    private final ImportResult importResult;
    private final ValidationResult validationResult;
    private final QuantumIrFileWriteResult writeResult;

    private QuantumImportJsonWorkflowResult(
        final IntegrationFormat format,
        final Path path,
        final ImportResult importResult,
        final ValidationResult validationResult,
        final QuantumIrFileWriteResult writeResult
    ) {
        if (format == null) {
            throw new IllegalArgumentException("Import workflow format must not be null.");
        }
        if (path == null) {
            throw new IllegalArgumentException("Import workflow JSON path must not be null.");
        }
        if (importResult == null) {
            throw new IllegalArgumentException("Import workflow import result must not be null.");
        }
        this.format = format;
        this.path = path;
        this.importResult = importResult;
        this.validationResult = validationResult;
        this.writeResult = writeResult;
    }

    /**
     * Создает результат workflow.
     *
     * @param format исходный формат
     * @param path путь JSON-файла
     * @param importResult результат import
     * @param validationResult результат валидации или null, если workflow остановлен раньше
     * @param writeResult результат записи или null, если workflow остановлен раньше
     * @return результат workflow
     */
    public static QuantumImportJsonWorkflowResult of(
        final IntegrationFormat format,
        final Path path,
        final ImportResult importResult,
        final ValidationResult validationResult,
        final QuantumIrFileWriteResult writeResult
    ) {
        return new QuantumImportJsonWorkflowResult(
            format,
            path,
            importResult,
            validationResult,
            writeResult
        );
    }

    public boolean isSuccess() {
        return importResult.isSuccess()
            && validationResult != null
            && validationResult.isValid()
            && writeResult != null
            && writeResult.isSuccess();
    }

    public IntegrationFormat format() {
        return format;
    }

    public Path path() {
        return path;
    }

    public ImportResult importResult() {
        return importResult;
    }

    public boolean hasProgram() {
        return importResult.hasProgram();
    }

    public QuantumProgram program() {
        return importResult.program();
    }

    public boolean hasValidationResult() {
        return validationResult != null;
    }

    public ValidationResult validationResult() {
        if (validationResult == null) {
            throw new IllegalStateException("Import workflow does not contain validation result.");
        }
        return validationResult;
    }

    public boolean hasWriteResult() {
        return writeResult != null;
    }

    public QuantumIrFileWriteResult writeResult() {
        if (writeResult == null) {
            throw new IllegalStateException("Import workflow does not contain JSON write result.");
        }
        return writeResult;
    }
}