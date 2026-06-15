/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.transformation;

/**
 * Именованный opt-in шаг conservative transformation pipeline.
 */
public enum TransformationStep {

    /**
     * Подстановка и вычисление parameter expressions.
     */
    PARAMETER_BINDING,

    /**
     * Каноникализация parameter expressions через вычисление известных констант и числовых поддеревьев.
     */
    PARAMETER_CANONICALIZATION,

    /**
     * Удаление явных identity gate operations.
     */
    IDENTITY_GATE_REMOVAL,

    /**
     * Разворачивание composite gate definitions в обычные gate operations.
     */
    COMPOSITE_GATE_INLINING,

    /**
     * Разложение gate operations только через явно переданные правила.
     */
    DECLARED_GATE_DECOMPOSITION,

    /**
     * Target-aware lowering preflight и применение объявленных правил, если target их требует.
     */
    TARGET_AWARE_LOWERING
}