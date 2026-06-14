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
 * Тип операции внутри шаблонного тела вызываемой сущности.
 */
public enum CallableOperationKind {

    /**
     * Применение гейта к формальным квантовым аргументам.
     */
    GATE,

    /**
     * Измерение формального квантового аргумента в формальный классический аргумент.
     */
    MEASURE,

    /**
     * Сброс формального квантового аргумента.
     */
    RESET,

    /**
     * Барьер над формальными квантовыми аргументами.
     */
    BARRIER,

    /**
     * Классическое присваивание по формальным классическим аргументам.
     */
    CLASSICAL_ASSIGNMENT,

    /**
     * Блок операций.
     */
    BLOCK,

    /**
     * Ветвление по шаблонному классическому предикату.
     */
    CONDITIONAL_BLOCK,

    /**
     * Цикл по целочисленному диапазону.
     */
    FOR_LOOP,

    /**
     * Цикл с классическим условием продолжения.
     */
    WHILE_LOOP,

    /**
     * Временная задержка на формальных квантовых аргументах.
     */
    DELAY,

    /**
     * Временной box-блок.
     */
    TIMING_BOX
}