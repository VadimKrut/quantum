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
 * Описание внешнего источника, из которого была получена часть IR.
 */
public final class ExternalSource {

    /**
     * Имя внешнего формата или интеграции.
     */
    private final String format;

    /**
     * Дополнительное человекочитаемое описание источника.
     */
    private final String description;

    /**
     * Создает описание внешнего источника.
     *
     * @param format имя формата или интеграции
     * @param description дополнительное описание
     */
    public ExternalSource(
        final String format,
        final String description
    ) {
        if (
            format == null
            || format.isBlank()
        ) {
            throw new IllegalArgumentException("External source format must not be blank.");
        }
        this.format = format;
        this.description = description == null ? "" : description;
    }

    /**
     * Возвращает имя формата или интеграции.
     *
     * @return имя формата
     */
    public String format() {
        return format;
    }

    /**
     * Возвращает описание источника.
     *
     * @return описание источника
     */
    public String description() {
        return description;
    }
}