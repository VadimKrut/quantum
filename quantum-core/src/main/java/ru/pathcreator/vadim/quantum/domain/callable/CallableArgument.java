/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Аргумент вызываемой сущности: квантовый или классический.
 */
public final class CallableArgument {

    /**
     * Имя аргумента.
     */
    private final IdentifierName name;

    /**
     * Род аргумента.
     */
    private final CallableArgumentKind kind;

    /**
     * Тип классического аргумента или null для квантового аргумента.
     */
    private final ClassicalType classicalType;

    private CallableArgument(
        final String name,
        final CallableArgumentKind kind,
        final ClassicalType classicalType
    ) {
        this.name = IdentifierName.of(
            name,
            "Callable argument"
        );
        this.kind = kind;
        this.classicalType = classicalType;
    }

    /**
     * Создает квантовый аргумент.
     *
     * @param name имя аргумента
     * @return аргумент
     */
    public static CallableArgument qubit(final String name) {
        return new CallableArgument(
            name,
            CallableArgumentKind.QUBIT,
            null
        );
    }

    /**
     * Создает классический аргумент.
     *
     * @param name имя аргумента
     * @param classicalType тип аргумента
     * @return аргумент
     */
    public static CallableArgument classical(
        final String name,
        final ClassicalType classicalType
    ) {
        if (classicalType == null) {
            throw new IllegalArgumentException("Callable classical argument type must not be null.");
        }
        return new CallableArgument(
            name,
            CallableArgumentKind.CLASSICAL,
            classicalType
        );
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
     * Возвращает род аргумента.
     *
     * @return род аргумента
     */
    public CallableArgumentKind kind() {
        return kind;
    }

    /**
     * Возвращает классический тип.
     *
     * @return классический тип
     */
    public ClassicalType classicalType() {
        if (kind != CallableArgumentKind.CLASSICAL) {
            throw new IllegalStateException("Callable argument is not classical.");
        }
        return classicalType;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableArgument argument)) {
            return false;
        }
        return Objects.equals(
            name,
            argument.name
        )
            && kind == argument.kind
            && Objects.equals(
                classicalType,
                argument.classicalType
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            kind,
            classicalType
        );
    }
}