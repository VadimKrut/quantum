/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;

/**
 * Внутренний результат writer OpenQASM 2.
 */
public final class OpenQasm2WriterResult {

    /**
     * Сгенерированный текст или null.
     */
    private final String content;

    /**
     * Диагностика ошибки или null.
     */
    private final IntegrationDiagnostic diagnostic;

    private OpenQasm2WriterResult(
        final String content,
        final IntegrationDiagnostic diagnostic
    ) {
        this.content = content;
        this.diagnostic = diagnostic;
    }

    /**
     * Создает успешный результат writer.
     *
     * @param content OpenQASM 2 text
     * @return успешный результат
     */
    public static OpenQasm2WriterResult success(final String content) {
        if (content == null) {
            throw new IllegalArgumentException("OpenQASM 2 writer content must not be null.");
        }
        return new OpenQasm2WriterResult(
            content,
            null
        );
    }

    /**
     * Создает неуспешный результат writer.
     *
     * @param diagnostic диагностика
     * @return неуспешный результат
     */
    public static OpenQasm2WriterResult failure(final IntegrationDiagnostic diagnostic) {
        if (diagnostic == null) {
            throw new IllegalArgumentException("OpenQASM 2 writer diagnostic must not be null.");
        }
        return new OpenQasm2WriterResult(
            null,
            diagnostic
        );
    }

    /**
     * Проверяет успешность writer.
     *
     * @return true, если writer успешен
     */
    public boolean isSuccess() {
        return content != null;
    }

    /**
     * Возвращает OpenQASM 2 text.
     *
     * @return OpenQASM 2 text
     */
    public String content() {
        if (content == null) {
            throw new IllegalStateException("OpenQASM 2 writer result does not contain content.");
        }
        return content;
    }

    /**
     * Возвращает диагностику.
     *
     * @return диагностика
     */
    public IntegrationDiagnostic diagnostic() {
        if (diagnostic == null) {
            throw new IllegalStateException("OpenQASM 2 writer result does not contain diagnostic.");
        }
        return diagnostic;
    }
}