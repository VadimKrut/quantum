/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.naming;

import java.util.Objects;

/**
 * Общий валидатор и контейнер для доменных имен, которые должны быть безопасными
 * идентификаторами в Java Quantum IR и будущих текстовых форматах.
 */
public final class IdentifierName {

    /**
     * Текстовое значение имени после проверки.
     */
    private final String value;

    private IdentifierName(final String value) {
        this.value = value;
    }

    /**
     * Создает проверенное доменное имя.
     *
     * @param value исходная строка имени
     * @param subjectName человекочитаемое название сущности для текста ошибки
     * @return проверенное имя
     */
    public static IdentifierName of(
        final String value,
        final String subjectName
    ) {
        validate(
            value,
            subjectName
        );
        return new IdentifierName(value);
    }

    /**
     * Возвращает исходное строковое значение имени.
     *
     * @return строковое значение имени
     */
    public String value() {
        return value;
    }

    private static void validate(
        final String value,
        final String subjectName
    ) {
        if (subjectName == null) {
            throw new IllegalArgumentException("Subject name must not be null.");
        }
        if (subjectName.isBlank()) {
            throw new IllegalArgumentException("Subject name must not be blank.");
        }
        if (value == null) {
            throw new IllegalArgumentException(subjectName + " must not be null.");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(subjectName + " must not be blank.");
        }
        if (!isValidIdentifier(value)) {
            throw new IllegalArgumentException(subjectName + " must start with a letter or underscore and contain only letters, digits, or underscores.");
        }
    }

    private static boolean isValidIdentifier(final String value) {
        final char first = value.charAt(0);
        if (!isIdentifierStart(first)) {
            return false;
        }

        for (int i = 1; i < value.length(); i++) {
            final char current = value.charAt(i);
            if (!isIdentifierPart(current)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isIdentifierStart(final char value) {
        return value == '_'
            || value >= 'A' && value <= 'Z'
            || value >= 'a' && value <= 'z';
    }

    private static boolean isIdentifierPart(final char value) {
        return isIdentifierStart(value)
            || value >= '0' && value <= '9';
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IdentifierName identifierName)) {
            return false;
        }
        return Objects.equals(
            value,
            identifierName.value
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}