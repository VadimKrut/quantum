/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Каноническое имя гейта внутри Quantum IR.
 */
public final class GateName {

    private static final String SUBJECT_NAME = "Gate name";

    /**
     * Проверенное строковое имя гейта.
     */
    private final String value;

    private GateName(final String value) {
        this.value = value;
    }

    /**
     * Создает имя гейта из безопасного идентификатора.
     *
     * @param value строковое имя гейта
     * @return проверенное имя гейта
     */
    public static GateName of(final String value) {
        final IdentifierName identifierName = IdentifierName.of(
            value,
            SUBJECT_NAME
        );
        return new GateName(identifierName.value());
    }

    /**
     * Возвращает строковое имя гейта.
     *
     * @return строковое имя гейта
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GateName gateName)) {
            return false;
        }
        return Objects.equals(
            value,
            gateName.value
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