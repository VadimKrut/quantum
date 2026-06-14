/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.validation;

import java.util.Objects;

/**
 * Одна ошибка доменной валидации Quantum IR.
 */
public final class ValidationError {

    /**
     * Значение индекса, когда ошибка не привязана к схеме или операции.
     */
    public static final int NO_INDEX = -1;

    /**
     * Код ошибки.
     */
    private final ValidationErrorCode code;

    /**
     * Человекочитаемое описание ошибки.
     */
    private final String message;

    /**
     * Индекс схемы в программе или NO_INDEX.
     */
    private final int circuitIndex;

    /**
     * Индекс операции в схеме или NO_INDEX.
     */
    private final int operationIndex;

    /**
     * Создает одну ошибку доменной валидации.
     *
     * @param code код ошибки
     * @param message человекочитаемое описание ошибки
     * @param circuitIndex индекс схемы или NO_INDEX
     * @param operationIndex индекс операции или NO_INDEX
     */
    public ValidationError(
        final ValidationErrorCode code,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        if (code == null) {
            throw new IllegalArgumentException("Validation error code must not be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException("Validation error message must not be null.");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("Validation error message must not be blank.");
        }
        this.code = code;
        this.message = message;
        this.circuitIndex = circuitIndex;
        this.operationIndex = operationIndex;
    }

    /**
     * Возвращает код ошибки.
     *
     * @return код ошибки
     */
    public ValidationErrorCode code() {
        return code;
    }

    /**
     * Возвращает описание ошибки.
     *
     * @return описание ошибки
     */
    public String message() {
        return message;
    }

    /**
     * Возвращает индекс схемы или NO_INDEX.
     *
     * @return индекс схемы
     */
    public int circuitIndex() {
        return circuitIndex;
    }

    /**
     * Возвращает индекс операции или NO_INDEX.
     *
     * @return индекс операции
     */
    public int operationIndex() {
        return operationIndex;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValidationError validationError)) {
            return false;
        }
        return circuitIndex == validationError.circuitIndex
            && operationIndex == validationError.operationIndex
            && code == validationError.code
            && Objects.equals(
                message,
                validationError.message
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            code,
            message,
            circuitIndex,
            operationIndex
        );
    }
}