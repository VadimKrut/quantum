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

/**
 * Результат export из Quantum IR во внешний формат.
 */
public final class ExportResult {

    /**
     * Формат результата.
     */
    private final IntegrationFormat format;

    /**
     * Содержимое внешнего формата или null при неуспешном export.
     */
    private final String content;

    /**
     * Immutable snapshot диагностик export.
     */
    private final List<IntegrationDiagnostic> diagnostics;

    /**
     * Создает результат export.
     *
     * @param format внешний формат
     * @param content содержимое внешнего формата или null
     * @param diagnostics диагностики export
     */
    public ExportResult(
        final IntegrationFormat format,
        final String content,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        validate(
            format,
            content,
            diagnostics
        );
        this.format = format;
        this.content = content;
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Создает успешный результат export без диагностик.
     *
     * @param format внешний формат
     * @param content содержимое внешнего формата
     * @return успешный результат export
     */
    public static ExportResult success(
        final IntegrationFormat format,
        final String content
    ) {
        return success(
            format,
            content,
            List.of()
        );
    }

    /**
     * Создает результат export с содержимым и диагностикой.
     *
     * @param format внешний формат
     * @param content содержимое внешнего формата
     * @param diagnostics диагностики export
     * @return результат export
     */
    public static ExportResult success(
        final IntegrationFormat format,
        final String content,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        return new ExportResult(
            format,
            content,
            diagnostics
        );
    }

    /**
     * Создает неуспешный результат export.
     *
     * @param format внешний формат
     * @param diagnostics диагностики export
     * @return неуспешный результат export
     */
    public static ExportResult failure(
        final IntegrationFormat format,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        return new ExportResult(
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
     * Проверяет, завершился ли export успешно.
     *
     * @return true, если есть content и нет ошибок
     */
    public boolean isSuccess() {
        return hasContent() && !hasErrors();
    }

    /**
     * Проверяет, есть ли внешний content.
     *
     * @return true, если content доступен
     */
    public boolean hasContent() {
        return content != null;
    }

    /**
     * Возвращает внешний content.
     *
     * @return содержимое внешнего формата
     */
    public String content() {
        if (content == null) {
            throw new IllegalStateException("Export result does not contain content.");
        }
        return content;
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
     * @return диагностики export
     */
    public List<IntegrationDiagnostic> diagnostics() {
        return diagnostics;
    }

    private static void validate(
        final IntegrationFormat format,
        final String content,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        if (format == null) {
            throw new IllegalArgumentException("Export result format must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Export diagnostics must not be null.");
        }
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i) == null) {
                throw new IllegalArgumentException("Export diagnostic must not be null.");
            }
        }
        if (
            content == null
            && !containsError(diagnostics)
        ) {
            throw new IllegalArgumentException("Failed export result must contain at least one error diagnostic.");
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
            throw new IllegalArgumentException("Export diagnostic index is outside of result bounds.");
        }
    }
}