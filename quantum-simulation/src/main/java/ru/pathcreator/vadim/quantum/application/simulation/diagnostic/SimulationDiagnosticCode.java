/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.diagnostic;

/**
 * Стабильный код диагностики локального симулятора.
 */
public enum SimulationDiagnosticCode {

    EMPTY_PROGRAM,
    MULTIPLE_CIRCUITS,
    TOO_MANY_QUBITS,
    NON_STATIC_QUBIT_REFERENCE,
    UNSUPPORTED_GATE,
    UNSUPPORTED_OPERATION,
    UNBOUND_PARAMETER,
    CLASSICAL_CONDITION_UNAVAILABLE,
    STATE_VECTOR_AFTER_MEASUREMENT
}