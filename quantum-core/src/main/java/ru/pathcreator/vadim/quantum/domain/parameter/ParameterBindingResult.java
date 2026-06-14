/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.parameter;

import java.util.List;

import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;

/**
 * Результат подстановки и вычисления параметрического выражения.
 */
public final class ParameterBindingResult {

    /**
     * Выражение после подстановки известных значений.
     */
    private final ParameterExpression expression;

    /**
     * Символы, для которых не хватило значений.
     */
    private final List<String> missingSymbols;

    /**
     * Создает результат подстановки и вычисления.
     *
     * @param expression выражение после подстановки
     * @param missingSymbols отсутствующие символы
     */
    public ParameterBindingResult(
        final ParameterExpression expression,
        final List<String> missingSymbols
    ) {
        if (expression == null) {
            throw new IllegalArgumentException("Parameter binding result expression must not be null.");
        }
        if (missingSymbols == null) {
            throw new IllegalArgumentException("Parameter binding result missing symbols must not be null.");
        }
        this.expression = expression;
        this.missingSymbols = List.copyOf(missingSymbols);
    }

    /**
     * Проверяет, что все символы были связаны со значениями.
     *
     * @return true, если отсутствующих символов нет
     */
    public boolean isComplete() {
        return missingSymbols.isEmpty();
    }

    /**
     * Возвращает выражение после подстановки известных значений.
     *
     * @return выражение после подстановки
     */
    public ParameterExpression expression() {
        return expression;
    }

    /**
     * Возвращает отсутствующие символы.
     *
     * @return immutable список отсутствующих символов
     */
    public List<String> missingSymbols() {
        return missingSymbols;
    }
}