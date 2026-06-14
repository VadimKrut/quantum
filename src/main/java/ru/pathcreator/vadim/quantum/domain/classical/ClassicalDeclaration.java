/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.classical;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Именованное классическое значение уровня программы.
 */
public final class ClassicalDeclaration {

    /**
     * Имя значения.
     */
    private final IdentifierName name;

    /**
     * Тип значения.
     */
    private final ClassicalType type;

    /**
     * Создает классическое объявление.
     *
     * @param name имя значения
     * @param type тип значения
     */
    public ClassicalDeclaration(
        final String name,
        final ClassicalType type
    ) {
        if (type == null) {
            throw new IllegalArgumentException("Classical declaration type must not be null.");
        }
        this.name = IdentifierName.of(
            name,
            "Classical declaration"
        );
        this.type = type;
    }

    /**
     * Возвращает имя.
     *
     * @return имя
     */
    public String name() {
        return name.value();
    }

    /**
     * Возвращает тип.
     *
     * @return тип
     */
    public ClassicalType type() {
        return type;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalDeclaration declaration)) {
            return false;
        }
        return Objects.equals(
            name,
            declaration.name
        )
            && Objects.equals(
                type,
                declaration.type
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            type
        );
    }
}