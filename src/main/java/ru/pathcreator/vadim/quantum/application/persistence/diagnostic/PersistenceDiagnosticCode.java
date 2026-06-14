/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.persistence.diagnostic;

/**
 * Стабильный код диагностики родного persistence-формата Quantum IR.
 */
public enum PersistenceDiagnosticCode {

    /**
     * Входной текст или программа отсутствует.
     */
    NULL_INPUT,

    /**
     * Входной текст пустой.
     */
    EMPTY_INPUT,

    /**
     * JSON не может быть разобран.
     */
    MALFORMED_JSON,

    /**
     * Формат файла не совпадает с родным Quantum IR JSON.
     */
    UNSUPPORTED_FORMAT,

    /**
     * Версия формата не поддерживается.
     */
    UNSUPPORTED_VERSION,

    /**
     * Обязательное поле отсутствует или имеет неверный тип.
     */
    INVALID_STRUCTURE,

    /**
     * Значение поля не может быть преобразовано в доменную модель.
     */
    INVALID_VALUE,

    /**
     * JSON ссылается на несуществующий регистр, бит, кубит или gate definition.
     */
    UNKNOWN_REFERENCE,

    /**
     * Объект текущей Java-модели не имеет переносимого JSON-представления.
     */
    UNSUPPORTED_MODEL_FEATURE,

    /**
     * Ошибка чтения или записи файла.
     */
    IO_ERROR
}