/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.syntax;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Внутренний результат parser OpenQASM 2.
 */
public final class OpenQasm2ParserResult {

    /**
     * Созданная программа или null.
     */
    private final QuantumProgram program;

    /**
     * Immutable snapshot диагностик parser.
     */
    private final List<IntegrationDiagnostic> diagnostics;

    /**
     * Создает результат parser.
     *
     * @param program созданная программа или null
     * @param diagnostics диагностики parser
     */
    public OpenQasm2ParserResult(
        final QuantumProgram program,
        final List<IntegrationDiagnostic> diagnostics
    ) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("OpenQASM 2 parser diagnostics must not be null.");
        }
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i) == null) {
                throw new IllegalArgumentException("OpenQASM 2 parser diagnostic must not be null.");
            }
        }
        this.program = program;
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Проверяет, создана ли программа.
     *
     * @return true, если программа создана
     */
    public boolean hasProgram() {
        return program != null;
    }

    /**
     * Возвращает созданную программу.
     *
     * @return созданная программа
     */
    public QuantumProgram program() {
        if (program == null) {
            throw new IllegalStateException("OpenQASM 2 parser result does not contain a program.");
        }
        return program;
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
        if (
            index < 0
            || index >= diagnostics.size()
        ) {
            throw new IllegalArgumentException("OpenQASM 2 parser diagnostic index is outside of result bounds.");
        }
        return diagnostics.get(index);
    }

    /**
     * Возвращает immutable список диагностик.
     *
     * @return диагностики parser
     */
    public List<IntegrationDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Проверяет, есть ли ошибки parser.
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
}