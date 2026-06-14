/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.diagnostic;

/**
 * Код диагностического сообщения import/export adapter.
 */
public enum IntegrationDiagnosticCode {

    /**
     * На вход export не передана Quantum IR программа.
     */
    NULL_PROGRAM,

    /**
     * На вход import не передан внешний текст или объект.
     */
    NULL_INPUT,

    /**
     * Внешний текст или объект пустой.
     */
    EMPTY_INPUT,

    /**
     * Доменная валидация Quantum IR завершилась с ошибками.
     */
    DOMAIN_VALIDATION_FAILED,

    /**
     * Внешний формат не поддерживает конкретную вычислительную модель.
     */
    UNSUPPORTED_COMPUTATION_MODEL,

    /**
     * Внешний формат не поддерживает конкретную операцию.
     */
    UNSUPPORTED_OPERATION,

    /**
     * Внешний формат не поддерживает текущую структуру схем в программе.
     */
    UNSUPPORTED_CIRCUIT_STRUCTURE,

    /**
     * Внешний формат не поддерживает конкретный gate.
     */
    UNSUPPORTED_GATE,

    UNSUPPORTED_TARGET_CAPABILITY,

    INCLUDE_RESOLUTION_FAILED,

    OUTPUT_MODE_DOWNGRADED,

    /**
     * Внешний input содержит синтаксическую ошибку.
     */
    PARSE_ERROR,

    /**
     * Внешний input корректно разобран, но описывает неподдерживаемую конструкцию.
     */
    UNSUPPORTED_INPUT_FEATURE,

    /**
     * Import создал Quantum IR, который не прошел доменную валидацию.
     */
    IMPORT_VALIDATION_FAILED,

    /**
     * Adapter получил некорректные options.
     */
    INVALID_OPTIONS
}