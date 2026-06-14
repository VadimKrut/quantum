/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.validation;

import java.util.List;

/**
 * Результат доменной валидации Quantum IR.
 */
public final class ValidationResult {

    /**
     * Неизменяемый снимок найденных ошибок.
     */
    private final List<ValidationError> errors;

    /**
     * Создает результат валидации из списка найденных ошибок.
     *
     * @param errors ошибки валидации
     */
    public ValidationResult(final List<ValidationError> errors) {
        if (errors == null) {
            throw new IllegalArgumentException("Validation errors must not be null.");
        }
        for (int i = 0; i < errors.size(); i++) {
            if (errors.get(i) == null) {
                throw new IllegalArgumentException("Validation error must not be null.");
            }
        }
        this.errors = List.copyOf(errors);
    }

    /**
     * Создает успешный результат без ошибок.
     *
     * @return успешный результат
     */
    public static ValidationResult valid() {
        return new ValidationResult(List.of());
    }

    /**
     * Проверяет, что ошибок нет.
     *
     * @return true, если ошибок нет
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Возвращает количество ошибок.
     *
     * @return количество ошибок
     */
    public int errorCount() {
        return errors.size();
    }

    /**
     * Возвращает ошибку по индексу.
     *
     * @param index индекс ошибки
     * @return ошибка валидации
     */
    public ValidationError error(final int index) {
        if (
            index < 0
            || index >= errors.size()
        ) {
            throw new IllegalArgumentException("Validation error index is outside of result bounds.");
        }
        return errors.get(index);
    }

    /**
     * Возвращает immutable список ошибок.
     *
     * @return список ошибок
     */
    public List<ValidationError> errors() {
        return errors;
    }
}