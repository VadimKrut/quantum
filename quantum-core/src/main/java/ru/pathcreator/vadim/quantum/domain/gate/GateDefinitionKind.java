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
 * Вид определения gate-based гейта.
 */
public enum GateDefinitionKind {

    /**
     * Гейт известен IR по сигнатуре и правилам, тело не хранится.
     */
    INTRINSIC,

    /**
     * Внешний opaque gate без тела.
     */
    OPAQUE,

    /**
     * Составной gate с символическим телом.
     */
    COMPOSITE,

    /**
     * Gate, заданный символьной унитарной матрицей.
     */
    MATRIX
}