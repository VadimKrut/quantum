/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate.modifier;

/**
 * Тип модификатора гейта.
 */
public enum GateModifierKind {

    /**
     * Обратный или dagger-вариант гейта.
     */
    INVERSE,

    /**
     * Квантово-управляемый вариант гейта.
     */
    CONTROLLED,

    /**
     * Возведение гейта в степень.
     */
    POWER,

    /**
     * Повторение гейта несколько раз.
     */
    REPEAT,

    /**
     * Неструктурная доменная аннотация гейта.
     */
    ANNOTATION
}