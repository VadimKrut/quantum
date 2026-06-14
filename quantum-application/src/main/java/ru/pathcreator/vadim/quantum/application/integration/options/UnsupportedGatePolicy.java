/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.options;

/**
 * Политика export для gate, который target adapter не умеет записать напрямую.
 */
public enum UnsupportedGatePolicy {

    /**
     * Если возможно, объявить gate как opaque.
     */
    OPAQUE_IF_POSSIBLE,

    /**
     * Требовать явное правило decomposition.
     */
    REQUIRE_DECOMPOSITION,

    /**
     * Сразу завершать export ошибкой.
     */
    FAIL_FAST
}