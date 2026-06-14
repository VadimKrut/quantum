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
     * Операция с предикатом над классической частью IR.
     */
    CLASSICALLY_CONTROLLED
}