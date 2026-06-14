/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.operation;

/**
 * Тип операции внутри gate-based Quantum IR.
 */
public enum OperationKind {

    /**
     * Применение квантового гейта.
     */
    GATE,

    /**
     * Измерение кубита в классический бит.
     */
    MEASURE,

    /**
     * Сброс кубита.
     */
    RESET,

    /**
     * Барьер для группы кубитов.
     */
    BARRIER,

    /**
     * Операция с классическим условием выполнения.
     */
    CONTROLLED,

    /**
     * Присваивание в классической части IR.
     */
    CLASSICAL_ASSIGNMENT,

    /**
     * Локальное классическое объявление внутри потока операций.
     */
    CLASSICAL_DECLARATION,

    /**
     * Объявление классического массива внутри потока операций.
     */
    CLASSICAL_ARRAY_DECLARATION,

    /**
     * Вызов callable/subroutine/extern как операция.
     */
    CALLABLE_INVOCATION,

    /**
     * Операция с предикатом над классической частью IR.
     */
    CLASSICALLY_CONTROLLED,

    /**
     * Лексический блок операций с собственной областью видимости.
     */
    BLOCK,

    /**
     * Ветвление по классическому предикату.
     */
    CONDITIONAL_BLOCK,

    /**
     * Цикл по дискретному диапазону классических значений.
     */
    FOR_LOOP,

    /**
     * Цикл по диапазону с runtime/symbolic границами.
     */
    SYMBOLIC_FOR_LOOP,

    /**
     * Цикл с классическим предикатом продолжения.
     */
    WHILE_LOOP,

    /**
     * Временная задержка на квантовых носителях.
     */
    DELAY,

    /**
     * Сгруппированный временной блок операций.
     */
    TIMING_BOX,

    LABEL,

    BRANCH,

    HALT,

    WAIT

    /**
     * Сохраненный фрагмент внешнего языка в потоке операций.
     */
}