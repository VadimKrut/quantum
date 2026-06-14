/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.format;

/**
 * Внешний формат или SDK-интеграция, с которой умеет работать adapter.
 */
public enum IntegrationFormat {

    /**
     * Текстовый формат OpenQASM 2.0.
     */
    OPENQASM_2(
        "openqasm2",
        "OpenQASM 2.0"
    );

    /**
     * Стабильный машинный идентификатор формата.
     */
    private final String id;

    /**
     * Человекочитаемое имя формата.
     */
    private final String displayName;

    IntegrationFormat(
        final String id,
        final String displayName
    ) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Возвращает стабильный машинный идентификатор формата.
     *
     * @return идентификатор формата
     */
    public String id() {
        return id;
    }

    /**
     * Возвращает человекочитаемое имя формата.
     *
     * @return имя формата
     */
    public String displayName() {
        return displayName;
    }
}