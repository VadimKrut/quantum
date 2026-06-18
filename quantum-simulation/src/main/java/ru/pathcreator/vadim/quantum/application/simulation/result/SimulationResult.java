/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.result;

import java.util.List;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnostic;

/**
 * Неизменяемый результат локальной симуляции Quantum IR.
 */
public final class SimulationResult {

    private final int qubitCount;
    private final int classicalBitCount;
    private final int shots;
    private final Map<String, Long> counts;
    private final List<StateVectorAmplitude> stateVector;
    private final List<SimulationDiagnostic> diagnostics;

    private SimulationResult(
        final int qubitCount,
        final int classicalBitCount,
        final int shots,
        final Map<String, Long> counts,
        final List<StateVectorAmplitude> stateVector,
        final List<SimulationDiagnostic> diagnostics
    ) {
        this.qubitCount = qubitCount;
        this.classicalBitCount = classicalBitCount;
        this.shots = shots;
        this.counts = counts;
        this.stateVector = stateVector;
        this.diagnostics = diagnostics;
    }

    public static SimulationResult of(
        final int qubitCount,
        final int classicalBitCount,
        final int shots,
        final Map<String, Long> counts,
        final List<StateVectorAmplitude> stateVector,
        final List<SimulationDiagnostic> diagnostics
    ) {
        if (qubitCount < 0) {
            throw new IllegalArgumentException("Simulation qubit count must not be negative.");
        }
        if (classicalBitCount < 0) {
            throw new IllegalArgumentException("Simulation classical bit count must not be negative.");
        }
        if (shots < 0) {
            throw new IllegalArgumentException("Simulation shots must not be negative.");
        }
        if (counts == null) {
            throw new IllegalArgumentException("Simulation counts must not be null.");
        }
        if (stateVector == null) {
            throw new IllegalArgumentException("Simulation state vector must not be null.");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Simulation diagnostics must not be null.");
        }
        return new SimulationResult(
            qubitCount,
            classicalBitCount,
            shots,
            Map.copyOf(counts),
            List.copyOf(stateVector),
            List.copyOf(diagnostics)
        );
    }

    public int qubitCount() {
        return qubitCount;
    }

    public int classicalBitCount() {
        return classicalBitCount;
    }

    public int shots() {
        return shots;
    }

    public Map<String, Long> counts() {
        return counts;
    }

    public List<StateVectorAmplitude> stateVector() {
        return stateVector;
    }

    public List<SimulationDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean isSuccess() {
        for (int i = 0; i < diagnostics.size(); i++) {
            if (diagnostics.get(i).isError()) {
                return false;
            }
        }
        return true;
    }
}