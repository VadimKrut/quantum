/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.metadata;

/**
 * Метаданные операции, не влияющие на равенство и смысл самой операции.
 */
public final class OperationMetadata {

    private static final OperationMetadata EMPTY = new OperationMetadata(
        null,
        null
    );

    /**
     * Внешний источник операции.
     */
    private final ExternalSource source;

    /**
     * Позиция операции во внешнем источнике.
     */
    private final SourceLocation location;

    /**
     * Создает метаданные операции.
     *
     * @param source внешний источник
     * @param location позиция во внешнем источнике
     */
    public OperationMetadata(
        final ExternalSource source,
        final SourceLocation location
    ) {
        this.source = source;
        this.location = location;
    }

    /**
     * Создает пустые метаданные.
     *
     * @return пустые метаданные
     */
    public static OperationMetadata empty() {
        return EMPTY;
    }

    /**
     * Проверяет, что метаданные не содержат источника и позиции.
     *
     * @return true, если метаданные пустые
     */
    public boolean isEmpty() {
        return source == null
            && location == null;
    }

    /**
     * Возвращает внешний источник операции.
     *
     * @return внешний источник или null
     */
    public ExternalSource source() {
        return source;
    }

    /**
     * Возвращает позицию операции во внешнем источнике.
     *
     * @return позиция или null
     */
    public SourceLocation location() {
        return location;
    }
}