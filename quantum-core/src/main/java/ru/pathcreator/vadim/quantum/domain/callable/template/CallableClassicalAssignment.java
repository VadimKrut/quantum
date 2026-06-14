/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable.template;

import java.util.Objects;

/**
 * Классическое присваивание внутри шаблонного тела callable.
 */
public final class CallableClassicalAssignment {

    /**
     * Цель присваивания.
     */
    private final CallableClassicalExpression target;

    /**
     * Записываемое значение.
     */
    private final CallableClassicalExpression value;

    /**
     * Создает шаблонное классическое присваивание.
     *
     * @param target цель присваивания
     * @param value записываемое значение
     */
    public CallableClassicalAssignment(
        final CallableClassicalExpression target,
        final CallableClassicalExpression value
    ) {
        if (target == null) {
            throw new IllegalArgumentException("Callable classical assignment target must not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Callable classical assignment value must not be null.");
        }
        if (target.kind() == CallableClassicalExpressionKind.INTEGER) {
            throw new IllegalArgumentException("Callable classical assignment target must be an argument reference.");
        }
        this.target = target;
        this.value = value;
    }

    /**
     * Возвращает цель присваивания.
     *
     * @return цель
     */
    public CallableClassicalExpression target() {
        return target;
    }

    /**
     * Возвращает записываемое значение.
     *
     * @return значение
     */
    public CallableClassicalExpression value() {
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallableClassicalAssignment assignment)) {
            return false;
        }
        return Objects.equals(
            target,
            assignment.target
        )
            && Objects.equals(
                value,
                assignment.value
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            target,
            value
        );
    }
}