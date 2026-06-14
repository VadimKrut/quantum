/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.callable.template;

/**
 * Тип классического выражения внутри шаблонного тела callable.
 */
public enum CallableClassicalExpressionKind {

    /**
     * Целочисленный литерал.
     */
    INTEGER,

    /**
     * Ссылка на формальный классический аргумент.
     */
    ARGUMENT_REFERENCE
}