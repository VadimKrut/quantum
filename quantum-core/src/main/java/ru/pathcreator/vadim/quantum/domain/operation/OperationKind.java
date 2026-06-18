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
 * Тип операции внутри Quantum IR.
 */
public enum OperationKind {

    /**
     * Применение квантового gate.
     */
    GATE,

    /**
     * Измерение qubit в classical bit.
     */
    MEASURE,

    /**
     * Сброс qubit.
     */
    RESET,

    /**
     * Барьер для группы quantum references.
     */
    BARRIER,

    /**
     * Quantum-controlled операция.
     */
    CONTROLLED,

    /**
     * Присваивание в classical части IR.
     */
    CLASSICAL_ASSIGNMENT,

    /**
     * Локальное classical declaration внутри operation stream.
     */
    CLASSICAL_DECLARATION,

    /**
     * Локальное declaration classical array внутри operation stream.
     */
    CLASSICAL_ARRAY_DECLARATION,

    /**
     * Вызов callable, subroutine или extern declaration.
     */
    CALLABLE_INVOCATION,

    /**
     * Операция под classical predicate.
     */
    CLASSICALLY_CONTROLLED,

    /**
     * Лексический блок с вложенным operation block.
     */
    BLOCK,

    /**
     * Условный block по classical predicate.
     */
    CONDITIONAL_BLOCK,

    /**
     * Цикл по дискретному classical range.
     */
    FOR_LOOP,

    /**
     * Цикл с symbolic/runtime границами.
     */
    SYMBOLIC_FOR_LOOP,

    /**
     * Цикл с classical predicate продолжения.
     */
    WHILE_LOOP,

    /**
     * Timing delay на quantum references.
     */
    DELAY,

    /**
     * Timing box с вложенным operation block.
     */
    TIMING_BOX,

    /**
     * Label для branch/control-flow навигации.
     */
    LABEL,

    /**
     * Branch к label.
     */
    BRANCH,

    /**
     * Остановка выполнения программы.
     */
    HALT,

    /**
     * Ожидание runtime/backend события.
     */
    WAIT
}