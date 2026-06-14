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

/**
 * Присваивание в классической части IR для будущих форматов с изменяемым классическим состоянием.
 */
public final class ClassicalAssignment {

    /**
     * Классическая ссылка, в которую записывается значение.
     */
    private final ClassicalExpression target;

    /**
     * Значение, которое записывается в цель.
     */
    private final ClassicalExpression value;

    /**
     * Создает классическое присваивание.
     *
     * @param target цель присваивания
     * @param value записываемое значение
     */
    public ClassicalAssignment(
        final ClassicalExpression target,
        final ClassicalExpression value
    ) {
        if (target == null) {
            throw new IllegalArgumentException("Classical assignment target must not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Classical assignment value must not be null.");
        }
        if (target.kind() == ClassicalExpressionKind.INTEGER) {
            throw new IllegalArgumentException("Classical assignment target must be a classical reference.");
        }
        this.target = target;
        this.value = value;
    }

    /**
     * Возвращает цель присваивания.
     *
     * @return цель присваивания
     */
    public ClassicalExpression target() {
        return target;
    }

    /**
     * Возвращает записываемое значение.
     *
     * @return значение
     */
    public ClassicalExpression value() {
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalAssignment assignment)) {
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