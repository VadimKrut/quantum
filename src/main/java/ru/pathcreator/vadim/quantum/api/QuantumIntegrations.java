/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm2.adapter.OpenQasm2Integration;
import ru.pathcreator.vadim.quantum.infrastructure.openqasm3.adapter.OpenQasm3Integration;
import ru.pathcreator.vadim.quantum.infrastructure.quil.adapter.QuilIntegration;

/**
 * Публичный фасад для получения внешних integration adapters.
 */
public final class QuantumIntegrations {

    private QuantumIntegrations() {
    }

    /**
     * Создает двунаправленную интеграцию OpenQASM 2.0.
     *
     * @return OpenQASM 2.0 integration adapter
     */
    public static QuantumIntegration openQasm2() {
        return new OpenQasm2Integration();
    }

    /**
     * Создает двунаправленную интеграцию OpenQASM 3.0.
     *
     * @return OpenQASM 3.0 integration adapter
     */
    public static QuantumIntegration openQasm3() {
        return new OpenQasm3Integration();
    }

    /**
     * Создает двунаправленную интеграцию Quil.
     *
     * @return Quil integration adapter
     */
    public static QuantumIntegration quil() {
        return new QuilIntegration();
    }
}