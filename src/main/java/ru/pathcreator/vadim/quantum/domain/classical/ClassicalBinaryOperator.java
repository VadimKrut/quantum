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
 * Бинарный оператор classical expression для runtime-вычислений.
 */
public enum ClassicalBinaryOperator {

    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULO,
    BITWISE_AND,
    BITWISE_OR,
    BITWISE_XOR,
    SHIFT_LEFT,
    SHIFT_RIGHT
}