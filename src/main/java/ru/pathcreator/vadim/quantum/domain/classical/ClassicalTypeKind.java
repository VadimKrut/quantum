/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.classical;

/**
 * Род семейства классического типа в универсальном IR.
 */
public enum ClassicalTypeKind {

    /**
     * Одиночный классический бит или битовый контейнер.
     */
    BIT,

    /**
     * Логическое значение.
     */
    BOOLEAN,

    /**
     * Целое число со знаком.
     */
    SIGNED_INTEGER,

    /**
     * Целое число без знака.
     */
    UNSIGNED_INTEGER,

    /**
     * Вещественное число.
     */
    FLOAT,

    /**
     * Угол для параметров квантовых операций.
     */
    ANGLE,

    /**
     * Длительность.
     */
    DURATION,

    /**
     * Символическая растяжимая длительность.
     */
    STRETCH
}