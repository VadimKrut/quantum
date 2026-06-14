/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.gate;

/**
 * Тип узла дерева параметрического выражения.
 */
public enum ParameterExpressionKind {

    /**
     * Конечная числовая константа.
     */
    NUMERIC,

    /**
     * Пользовательский символический параметр.
     */
    NAMED,

    /**
     * Известная математическая константа.
     */
    KNOWN_CONSTANT,

    /**
     * Унарное выражение.
     */
    UNARY,

    /**
     * Бинарное выражение.
     */
    BINARY
}