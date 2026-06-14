/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.model;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Имя квантовой схемы внутри программы.
 */
public final class CircuitName {

    private static final String SUBJECT_NAME = "Circuit name";

    /**
     * Проверенное строковое имя схемы.
     */
    private final String value;

    private CircuitName(final String value) {
        this.value = value;
    }

    /**
     * Создает имя схемы из безопасного идентификатора.
     *
     * @param value строковое имя схемы
     * @return проверенное имя схемы
     */
    public static CircuitName of(final String value) {
        final IdentifierName identifierName = IdentifierName.of(
            value,
            SUBJECT_NAME
        );
        return new CircuitName(identifierName.value());
    }

    /**
     * Возвращает строковое имя схемы.
     *
     * @return строковое имя схемы
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CircuitName circuitName)) {
            return false;
        }
        return Objects.equals(
            value,
            circuitName.value
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