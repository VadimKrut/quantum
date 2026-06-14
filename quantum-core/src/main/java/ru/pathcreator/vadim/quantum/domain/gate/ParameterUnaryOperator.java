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
 * Унарный оператор параметрического выражения.
 */
public enum ParameterUnaryOperator {

    /**
     * Арифметическое отрицание.
     */
    NEGATE("-");

    private final String symbol;

    ParameterUnaryOperator(final String symbol) {
        this.symbol = symbol;
    }

    /**
     * Возвращает текстовый символ оператора.
     *
     * @return символ оператора
     */
    public String symbol() {
        return symbol;
    }
}