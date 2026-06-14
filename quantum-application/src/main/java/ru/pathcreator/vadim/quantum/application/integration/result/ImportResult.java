/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.result;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Результат import из внешнего формата в Quantum IR.
 */
public final class ImportResult {

    /**
     * Формат внешнего input.
     */
    private final IntegrationFormat format;

    /**
     * Созданная Quantum IR программа или null при неуспешном import.
     */
    private final QuantumProgram program;

    /**
     * Immutable snapshot диагностик import.
     */
    private final List<IntegrationDiagnostic> diagnostics;

    /**
     * Создает результат import.
     *
     * @param format внешний формат
     * @param program созданная программа или null
     * @param diagnostics диагностики import
     */
    public ImportResult(
        final IntegrationFormat format,
        final QuantumProgram program,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        validate(
            format,
            program,
            diagnostics
        );
        this.format = format;
        this.program = program;
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Создает успешный результат import без диагностик.
     *
     * @param format внешний формат
     * @param program созданная программа
     * @return успешный результат import
     */
    public static ImportResult success(
        final IntegrationFormat format,
        final QuantumProgram program
    ) {
        return success(
            format,
            program,
            List.of()
        );
    }

    /**
     * Создает результат import с программой и диагностикой.
     *
     * @param format внешний формат
     * @param program созданная программа
     * @param diagnostics диагностики import
     * @return результат import
     */
    public static ImportResult success(
        final IntegrationFormat format,
        final QuantumProgram program,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        return new ImportResult(
            format,
            program,
            diagnostics
        );
    }

    /**
     * Создает неуспешный результат import.
     *
     * @param format внешний формат
     * @param diagnostics диагностики import
     * @return неуспешный результат import
     */
    public static ImportResult failure(
        final IntegrationFormat format,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        return new ImportResult(
            format,
            null,
            diagnostics
        );
    }

    /**
     * Возвращает внешний формат.
     *
     * @return внешний формат
     */
    public IntegrationFormat format() {
        return format;
    }

    /**
     * Проверяет, завершился ли import успешно.
     *
     * @return true, если есть программа и нет ошибок
     */
    public boolean isSuccess() {
        return hasProgram() && !hasErrors();
    }

    /**
     * Проверяет, есть ли созданная программа.
     *
     * @return true, если программа доступна
     */
    public boolean hasProgram() {
        return program != null;
    }

    /**
     * Возвращает созданную Quantum IR программу.
     *
     * @return созданная программа
     */
    public QuantumProgram program() {
        if (program == null) {
            throw new IllegalStateException("Import result does not contain a program.");
        }
        return program;
    }

    /**
     * Проверяет, содержит ли результат ошибки.
     *
     * @return true, если есть хотя бы одна ошибка
     */
    public boolean hasErrors() {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает количество диагностик.
     *
     * @return количество диагностик
     */
    public int diagnosticCount() {
        return diagnostics.size();
    }

    /**
     * Возвращает диагностику по индексу.
     *
     * @param index индекс диагностики
     * @return диагностика
     */
    public IntegrationDiagnostic diagnostic(final int index) {
        validateDiagnosticIndex(index);
        return diagnostics.get(index);
    }

    /**
     * Возвращает immutable список диагностик.
     *
     * @return диагностики import
     */
    public List<IntegrationDiagnostic> diagnostics() {
        return diagnostics;
    }

    private static void validate(
        final IntegrationFormat format,
        final QuantumProgram program,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        if (format == null) {
            throw new IllegalArgumentException("Import result format must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Import diagnostics must not be null.");
        }
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i) == null) {
                throw new IllegalArgumentException("Import diagnostic must not be null.");
            }
        }
        if (
            program == null
            && !containsError(diagnostics)
        ) {
            throw new IllegalArgumentException("Failed import result must contain at least one error diagnostic.");
        }
    }

    private static boolean containsError(final List<IntegrationDiagnostic> diagnostics) {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return true;
            }
        }
        return false;
    }

    private void validateDiagnosticIndex(final int index) {
        if (
            index < 0
            || index >= diagnostics.size()
        ) {
            throw new IllegalArgumentException("Import diagnostic index is outside of result bounds.");
        }
    }
}