/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.capability;

/**
 * Обобщенная возможность внешнего target adapter.
 */
public enum IntegrationCapability {

    /**
     * Adapter может писать квантовые регистры.
     */
    QUANTUM_REGISTERS,

    /**
     * Adapter может писать классические регистры.
     */
    CLASSICAL_REGISTERS,

    /**
     * Adapter может писать измерения.
     */
    MEASUREMENTS,

    /**
     * Adapter может писать reset.
     */
    RESET,

    /**
     * Adapter может писать barrier.
     */
    BARRIER,

    /**
     * Adapter может писать opaque gate declarations.
     */
    OPAQUE_GATES,

    /**
     * Adapter может писать composite gates напрямую или через semantic lowering.
     */
    COMPOSITE_GATES,

    /**
     * Adapter может писать условное выполнение по классическому регистру.
     */
    CLASSICAL_REGISTER_CONDITIONS,

    /**
     * Adapter может применять правила decomposition/lowering.
     */
    GATE_DECOMPOSITION,

    /**
     * Adapter может писать gate modifiers напрямую или через lowering.
     */
    GATE_MODIFIERS
}