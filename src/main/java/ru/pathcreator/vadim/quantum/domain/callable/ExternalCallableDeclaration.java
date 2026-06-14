/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Объявление вызываемой сущности, тело которой находится вне IR.
 */
public final class ExternalCallableDeclaration {

    /**
     * Имя внешней сущности.
     */
    private final IdentifierName name;

    /**
     * Аргументы внешней сущности.
     */
    private final CallableArgument[] arguments;

    /**
     * Возвращаемый классический тип или null, если результата нет.
     */
    private final ClassicalType returnType;

    /**
     * Создает внешнее объявление.
     *
     * @param name имя внешней сущности
     * @param returnType возвращаемый тип или null
     * @param arguments аргументы
     */
    public ExternalCallableDeclaration(
        final String name,
        final ClassicalType returnType,
        final CallableArgument... arguments
    ) {
        if (arguments == null) {
            throw new IllegalArgumentException("External callable arguments must not be null.");
        }
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] == null) {
                throw new IllegalArgumentException("External callable argument must not be null.");
            }
        }
        this.name = IdentifierName.of(
            name,
            "External callable"
        );
        this.returnType = returnType;
        this.arguments = Arrays.copyOf(
            arguments,
            arguments.length
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
     * Проверяет, есть ли возвращаемый тип.
     *
     * @return true, если возвращаемый тип задан
     */
    public boolean hasReturnType() {
        return returnType != null;
    }

    /**
     * Возвращает тип результата.
     *
     * @return тип результата
     */
    public ClassicalType returnType() {
        if (returnType == null) {
            throw new IllegalStateException("External callable does not have return type.");
        }
        return returnType;
    }

    /**
     * Возвращает количество аргументов.
     *
     * @return количество аргументов
     */
    public int argumentCount() {
        return arguments.length;
    }

    /**
     * Возвращает аргумент по индексу.
     *
     * @param index индекс аргумента
     * @return аргумент
     */
    public CallableArgument argument(final int index) {
        if (
            index < 0
            || index >= arguments.length
        ) {
            throw new IllegalArgumentException("External callable argument index is outside of bounds.");
        }
        return arguments[index];
    }

    /**
     * Возвращает аргументы.
     *
     * @return аргументы
     */
    public List<CallableArgument> arguments() {
        return List.of(arguments);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExternalCallableDeclaration declaration)) {
            return false;
        }
        return Objects.equals(
            name,
            declaration.name
        )
            && Arrays.equals(
                arguments,
                declaration.arguments
            )
            && Objects.equals(
                returnType,
                declaration.returnType
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            Arrays.hashCode(arguments),
            returnType
        );
    }
}