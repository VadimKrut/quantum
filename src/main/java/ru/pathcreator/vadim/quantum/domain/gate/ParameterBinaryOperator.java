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
 * Бинарный оператор параметрического выражения.
 */
public enum ParameterBinaryOperator {

    /**
     * Сложение.
     */
    ADD("+"),

    /**
     * Вычитание.
     */
    SUBTRACT("-"),

    /**
     * Умножение.
     */
    MULTIPLY("*"),

    /**
     * Деление.
     */
    DIVIDE("/");

    private final String symbol;

    ParameterBinaryOperator(final String symbol) {
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