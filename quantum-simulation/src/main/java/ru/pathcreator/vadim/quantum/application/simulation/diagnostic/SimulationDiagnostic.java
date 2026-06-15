/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.diagnostic;

import java.util.Objects;

/**
 * Immutable диагностика локального симулятора.
 */
public final class SimulationDiagnostic {

    public static final int NO_INDEX = -1;

    private final SimulationDiagnosticSeverity severity;
    private final SimulationDiagnosticCode code;
    private final String message;
    private final int circuitIndex;
    private final int operationIndex;

    private SimulationDiagnostic(
        final SimulationDiagnosticSeverity severity,
        final SimulationDiagnosticCode code,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.circuitIndex = circuitIndex;
        this.operationIndex = operationIndex;
    }

    public static SimulationDiagnostic error(
        final SimulationDiagnosticCode code,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        return of(
            SimulationDiagnosticSeverity.ERROR,
            code,
            message,
            circuitIndex,
            operationIndex
        );
    }

    public static SimulationDiagnostic warning(
        final SimulationDiagnosticCode code,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        return of(
            SimulationDiagnosticSeverity.WARNING,
            code,
            message,
            circuitIndex,
            operationIndex
        );
    }

    public static SimulationDiagnostic of(
        final SimulationDiagnosticSeverity severity,
        final SimulationDiagnosticCode code,
        final String message,
        final int circuitIndex,
        final int operationIndex
    ) {
        if (severity == null) {
            throw new IllegalArgumentException("Simulation diagnostic severity must not be null.");
        }
        if (code == null) {
            throw new IllegalArgumentException("Simulation diagnostic code must not be null.");
        }
        if (
            message == null
            || message.isBlank()
        ) {
            throw new IllegalArgumentException("Simulation diagnostic message must not be blank.");
        }
        return new SimulationDiagnostic(
            severity,
            code,
            message,
            circuitIndex,
            operationIndex
        );
    }

    public SimulationDiagnosticSeverity severity() {
        return severity;
    }

    public SimulationDiagnosticCode code() {
        return code;
    }

    public String message() {
        return message;
    }

    public int circuitIndex() {
        return circuitIndex;
    }

    public int operationIndex() {
        return operationIndex;
    }

    public boolean isError() {
        return severity == SimulationDiagnosticSeverity.ERROR;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimulationDiagnostic diagnostic)) {
            return false;
        }
        return circuitIndex == diagnostic.circuitIndex
            && operationIndex == diagnostic.operationIndex
            && severity == diagnostic.severity
            && code == diagnostic.code
            && Objects.equals(
                message,
                diagnostic.message
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            severity,
            code,
            message,
            circuitIndex,
            operationIndex
        );
    }
}