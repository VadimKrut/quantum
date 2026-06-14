/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.register;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Имя квантового или классического регистра.
 */
public final class RegisterName {

    private static final String SUBJECT_NAME = "Register name";

    /**
     * Проверенное строковое имя регистра.
     */
    private final String value;

    private RegisterName(final String value) {
        this.value = value;
    }

    /**
     * Создает имя регистра из безопасного идентификатора.
     *
     * @param value строковое имя регистра
     * @return проверенное имя регистра
     */
    public static RegisterName of(final String value) {
        final IdentifierName identifierName = IdentifierName.of(
            value,
            SUBJECT_NAME
        );
        return new RegisterName(identifierName.value());
    }

    /**
     * Возвращает строковое имя регистра.
     *
     * @return строковое имя регистра
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RegisterName registerName)) {
            return false;
        }
        return Objects.equals(
            value,
            registerName.value
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