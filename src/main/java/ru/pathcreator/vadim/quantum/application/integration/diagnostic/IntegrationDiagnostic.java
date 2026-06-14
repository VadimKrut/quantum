/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.diagnostic;

import java.util.Objects;

/**
 * Одно диагностическое сообщение внешней интеграции.
 */
public final class IntegrationDiagnostic {

    /**
     * Значение позиции, когда строка или колонка не применима.
     */
    public static final int NO_LOCATION = -1;

    /**
     * Уровень серьезности.
     */
    private final IntegrationDiagnosticSeverity severity;

    /**
     * Стабильный код диагностики.
     */
    private final IntegrationDiagnosticCode code;

    /**
     * Человекочитаемое описание.
     */
    private final String message;

    /**
     * Номер строки во внешнем input или NO_LOCATION.
     */
    private final int line;

    /**
     * Номер колонки во внешнем input или NO_LOCATION.
     */
    private final int column;

    /**
     * Создает диагностическое сообщение.
     *
     * @param severity уровень серьезности
     * @param code код диагностики
     * @param message описание
     * @param line номер строки или NO_LOCATION
     * @param column номер колонки или NO_LOCATION
     */
    public IntegrationDiagnostic(
        final IntegrationDiagnosticSeverity severity,
        final IntegrationDiagnosticCode code,
        final String message,
        final int line,
        final int column
    ) {
        validate(
            severity,
            code,
            message,
            line,
            column
        );
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.line = line;
        this.column = column;
    }

    /**
     * Создает ошибку без привязки к строке.
     *
     * @param code код диагностики
     * @param message описание
     * @return диагностическая ошибка
     */
    public static IntegrationDiagnostic error(
        final IntegrationDiagnosticCode code,
        final String message
    ) {
        return error(
            code,
            message,
            NO_LOCATION,
            NO_LOCATION
        );
    }

    /**
     * Создает ошибку с позицией во внешнем input.
     *
     * @param code код диагностики
     * @param message описание
     * @param line номер строки
     * @param column номер колонки или NO_LOCATION
     * @return диагностическая ошибка
     */
    public static IntegrationDiagnostic error(
        final IntegrationDiagnosticCode code,
        final String message,
        final int line,
        final int column
    ) {
        return new IntegrationDiagnostic(
            IntegrationDiagnosticSeverity.ERROR,
            code,
            message,
            line,
            column
        );
    }

    /**
     * Создает предупреждение без привязки к строке.
     *
     * @param code код диагностики
     * @param message описание
     * @return диагностическое предупреждение
     */
    public static IntegrationDiagnostic warning(
        final IntegrationDiagnosticCode code,
        final String message
    ) {
        return warning(
            code,
            message,
            NO_LOCATION,
            NO_LOCATION
        );
    }

    /**
     * Создает предупреждение с позицией во внешнем input.
     *
     * @param code код диагностики
     * @param message описание
     * @param line номер строки
     * @param column номер колонки или NO_LOCATION
     * @return диагностическое предупреждение
     */
    public static IntegrationDiagnostic warning(
        final IntegrationDiagnosticCode code,
        final String message,
        final int line,
        final int column
    ) {
        return new IntegrationDiagnostic(
            IntegrationDiagnosticSeverity.WARNING,
            code,
            message,
            line,
            column
        );
    }

    /**
     * Возвращает уровень серьезности.
     *
     * @return уровень серьезности
     */
    public IntegrationDiagnosticSeverity severity() {
        return severity;
    }

    /**
     * Возвращает код диагностики.
     *
     * @return код диагностики
     */
    public IntegrationDiagnosticCode code() {
        return code;
    }

    /**
     * Возвращает описание диагностики.
     *
     * @return описание
     */
    public String message() {
        return message;
    }

    /**
     * Возвращает номер строки или NO_LOCATION.
     *
     * @return номер строки
     */
    public int line() {
        return line;
    }

    /**
     * Возвращает номер колонки или NO_LOCATION.
     *
     * @return номер колонки
     */
    public int column() {
        return column;
    }

    /**
     * Проверяет, что диагностика является ошибкой.
     *
     * @return true для ошибки
     */
    public boolean isError() {
        return severity == IntegrationDiagnosticSeverity.ERROR;
    }

    /**
     * Проверяет, что диагностика является предупреждением.
     *
     * @return true для предупреждения
     */
    public boolean isWarning() {
        return severity == IntegrationDiagnosticSeverity.WARNING;
    }

    private static void validate(
        final IntegrationDiagnosticSeverity severity,
        final IntegrationDiagnosticCode code,
        final String message,
        final int line,
        final int column
    ) {
        if (severity == null) {
            throw new IllegalArgumentException("Integration diagnostic severity must not be null.");
        }
        if (code == null) {
            throw new IllegalArgumentException("Integration diagnostic code must not be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException("Integration diagnostic message must not be null.");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("Integration diagnostic message must not be blank.");
        }
        if (
            line != NO_LOCATION
            && line <= 0
        ) {
            throw new IllegalArgumentException("Integration diagnostic line must be positive or NO_LOCATION.");
        }
        if (
            column != NO_LOCATION
            && column <= 0
        ) {
            throw new IllegalArgumentException("Integration diagnostic column must be positive or NO_LOCATION.");
        }
        if (
            line == NO_LOCATION
            && column != NO_LOCATION
        ) {
            throw new IllegalArgumentException("Integration diagnostic column requires a line.");
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IntegrationDiagnostic diagnostic)) {
            return false;
        }
        return line == diagnostic.line
            && column == diagnostic.column
            && severity == diagnostic.severity
            && code == diagnostic.code
            && Objects.equals(
                message,
                diagnostic.message
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            severity,
            code,
            message,
            line,
            column
        );
    }
}