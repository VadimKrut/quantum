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
 * Классический тип с необязательной шириной в битах.
 */
public final class ClassicalType {

    /**
     * Семейство типа.
     */
    private final ClassicalTypeKind kind;

    /**
     * Ширина типа в битах или 0, если ширина не задается.
     */
    private final int bitWidth;

    private ClassicalType(
        final ClassicalTypeKind kind,
        final int bitWidth
    ) {
        this.kind = kind;
        this.bitWidth = bitWidth;
    }

    /**
     * Создает тип без явной ширины.
     *
     * @param kind семейство типа
     * @return классический тип
     */
    public static ClassicalType of(final ClassicalTypeKind kind) {
        return sized(
            kind,
            0
        );
    }

    /**
     * Создает тип с явной шириной.
     *
     * @param kind семейство типа
     * @param bitWidth ширина в битах
     * @return классический тип
     */
    public static ClassicalType sized(
        final ClassicalTypeKind kind,
        final int bitWidth
    ) {
        if (kind == null) {
            throw new IllegalArgumentException("Classical type kind must not be null.");
        }
        if (bitWidth < 0) {
            throw new IllegalArgumentException("Classical type bit width must not be negative.");
        }
        if (
            bitWidth == 0
            && requiresWidth(kind)
        ) {
            throw new IllegalArgumentException("Classical type requires explicit bit width.");
        }
        return new ClassicalType(
            kind,
            bitWidth
        );
    }

    /**
     * Возвращает семейство типа.
     *
     * @return семейство типа
     */
    public ClassicalTypeKind kind() {
        return kind;
    }

    /**
     * Проверяет, задана ли явная ширина.
     *
     * @return true, если ширина задана
     */
    public boolean hasBitWidth() {
        return bitWidth > 0;
    }

    /**
     * Возвращает ширину в битах.
     *
     * @return ширина в битах
     */
    public int bitWidth() {
        if (bitWidth == 0) {
            throw new IllegalStateException("Classical type does not have explicit bit width.");
        }
        return bitWidth;
    }

    private static boolean requiresWidth(final ClassicalTypeKind kind) {
        return kind == ClassicalTypeKind.SIGNED_INTEGER
            || kind == ClassicalTypeKind.UNSIGNED_INTEGER
            || kind == ClassicalTypeKind.FLOAT
            || kind == ClassicalTypeKind.ANGLE;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassicalType type)) {
            return false;
        }
        return kind == type.kind
            && bitWidth == type.bitWidth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            kind,
            bitWidth
        );
    }
}