/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.normalization;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Результат adapter-level нормализации Quantum IR перед записью OpenQASM 2.
 */
public final class OpenQasm2ExportNormalizationResult {

    /**
     * Нормализованная программа, готовая для OpenQASM 2 writer.
     */
    private final QuantumProgram program;

    /**
     * Диагностики, найденные во время lowering pass.
     */
    private final List<IntegrationDiagnostic> diagnostics;

    private OpenQasm2ExportNormalizationResult(
        final QuantumProgram program,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        this.program = program;
        this.diagnostics = diagnostics;
    }

    /**
     * Создает успешный результат нормализации.
     *
     * @param program нормализованная программа
     * @param diagnostics предупреждения нормализации
     * @return успешный результат
     */
    public static OpenQasm2ExportNormalizationResult success(
        final QuantumProgram program,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Normalized program must not be null.");
        }
        return new OpenQasm2ExportNormalizationResult(
            program,
            copyDiagnostics(diagnostics)
        );
    }

    /**
     * Создает результат с ошибками нормализации.
     *
     * @param diagnostics ошибки нормализации
     * @return неуспешный результат
     */
    public static OpenQasm2ExportNormalizationResult failure(final List<IntegrationDiagnostic> diagnostics) {
        return new OpenQasm2ExportNormalizationResult(
            null,
            copyDiagnostics(diagnostics)
        );
    }

    /**
     * Проверяет, завершилась ли нормализация без ошибок.
     *
     * @return true, если нормализованная программа доступна
     */
    public boolean isSuccess() {
        return program != null && !hasErrors();
    }

    /**
     * Возвращает нормализованную программу.
     *
     * @return нормализованная программа
     */
    public QuantumProgram program() {
        if (!isSuccess()) {
            throw new IllegalStateException("OpenQASM 2 normalization did not produce a program.");
        }
        return program;
    }

    /**
     * Возвращает diagnostics нормализации.
     *
     * @return diagnostics
     */
    public List<IntegrationDiagnostic> diagnostics() {
        return diagnostics;
    }

    private boolean hasErrors() {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }

    private static List<IntegrationDiagnostic> copyDiagnostics(final List<IntegrationDiagnostic> diagnostics) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("Normalization diagnostics must not be null.");
        }
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i) == null) {
                throw new IllegalArgumentException("Normalization diagnostic must not be null.");
            }
        }
        return List.copyOf(diagnostics);
    }
}