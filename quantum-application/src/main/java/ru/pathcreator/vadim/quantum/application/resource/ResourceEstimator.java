/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.resource;

import java.util.ArrayList;

import ru.pathcreator.vadim.quantum.application.inspection.CircuitInspectionSummary;
import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.inspection.QuantumProgramInspector;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

public final class ResourceEstimator {

    public static final int DEFAULT_LOCAL_SIMULATION_MAX_QUBITS = 20;

    public ResourceEstimate estimate(final QuantumProgram program) {
        return estimate(
            program,
            DEFAULT_LOCAL_SIMULATION_MAX_QUBITS
        );
    }

    public ResourceEstimate estimate(
        final QuantumProgram program,
        final int localSimulationMaxQubits
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Resource estimate program must not be null.");
        }
        if (localSimulationMaxQubits < 0) {
            throw new IllegalArgumentException("Local simulation max qubits must not be negative.");
        }
        final ProgramInspectionResult inspection = new QuantumProgramInspector().inspect(program);
        final ArrayList<CircuitResourceEstimate> circuits = new ArrayList<>(inspection.circuitCount());
        for (int i = 0; i < inspection.circuitCount(); i++) {
            circuits.add(circuitEstimate(inspection.circuitSummary(i)));
        }
        final long amplitudes = amplitudes(inspection.qubitCount());
        return new ResourceEstimate(
            inspection.computationModel(),
            inspection.circuitCount(),
            inspection.qubitCount(),
            inspection.classicalBitCount(),
            inspection.operationCount(),
            inspection.gateCount(),
            inspection.measurementCount(),
            amplitudes,
            safeMultiply(
                amplitudes,
                16L
            ),
            inspection.qubitCount() <= localSimulationMaxQubits,
            localSimulationMaxQubits,
            inspection.gateHistogram(),
            circuits
        );
    }

    private static CircuitResourceEstimate circuitEstimate(final CircuitInspectionSummary circuit) {
        return new CircuitResourceEstimate(
            circuit.name(),
            circuit.qubitCount(),
            circuit.classicalBitCount(),
            circuit.operationCount(),
            circuit.gateCount(),
            circuit.measurementCount(),
            circuit.twoQubitGateCount(),
            circuit.multiQubitGateCount(),
            circuit.parameterizedGateCount(),
            circuit.modifiedGateCount(),
            circuit.approximateDepth(),
            circuit.isDepthPrecise(),
            circuit.gateHistogram(),
            circuit.neverMeasuredQubits(),
            circuit.overwrittenClassicalBits()
        );
    }

    private static long amplitudes(final int qubitCount) {
        if (qubitCount >= Long.SIZE - 1) {
            return Long.MAX_VALUE;
        }
        return 1L << qubitCount;
    }

    private static long safeMultiply(
        final long left,
        final long right
    ) {
        if (
            left != 0L
            && right > Long.MAX_VALUE / left
        ) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }
}