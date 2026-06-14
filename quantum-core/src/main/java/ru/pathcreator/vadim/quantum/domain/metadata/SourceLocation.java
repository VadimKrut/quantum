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
 * Позиция исходного фрагмента во внешнем представлении программы.
 */
public final class SourceLocation {

    /**
     * Значение для отсутствующей строки или колонки.
     */
    public static final int NO_LOCATION = -1;

    /**
     * Номер строки во внешнем источнике.
     */
    private final int line;

    /**
     * Номер колонки во внешнем источнике.
     */
    private final int column;

    /**
     * Создает позицию во внешнем источнике.
     *
     * @param line номер строки или NO_LOCATION
     * @param column номер колонки или NO_LOCATION
     */
    public SourceLocation(
        final int line,
        final int column
    ) {
        if (
            (line != NO_LOCATION && line <= 0)
            || (column != NO_LOCATION && column <= 0)
        ) {
            throw new IllegalArgumentException("Source location line and column must be positive or NO_LOCATION.");
        }
        this.line = line;
        this.column = column;
    }

    /**
     * Возвращает номер строки.
     *
     * @return номер строки или NO_LOCATION
     */
    public int line() {
        return line;
    }

    /**
     * Возвращает номер колонки.
     *
     * @return номер колонки или NO_LOCATION
     */
    public int column() {
        return column;
    }
}