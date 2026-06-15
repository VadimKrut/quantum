/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.resource;

import java.util.List;
import java.util.Map;

import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;

public final class ResourceEstimate {

    private final QuantumComputationModel computationModel;
    private final int circuitCount;
    private final int qubitCount;
    private final int classicalBitCount;
    private final int operationCount;
    private final int gateCount;
    private final int measurementCount;
    private final long estimatedStateVectorAmplitudes;
    private final long estimatedStateVectorBytes;
    private final boolean localSimulationFeasible;
    private final int localSimulationMaxQubits;
    private final Map<String, Integer> gateHistogram;
    private final List<CircuitResourceEstimate> circuits;

    public ResourceEstimate(
        final QuantumComputationModel computationModel,
        final int circuitCount,
        final int qubitCount,
        final int classicalBitCount,
        final int operationCount,
        final int gateCount,
        final int measurementCount,
        final long estimatedStateVectorAmplitudes,
        final long estimatedStateVectorBytes,
        final boolean localSimulationFeasible,
        final int localSimulationMaxQubits,
        final Map<String, Integer> gateHistogram,
        final List<CircuitResourceEstimate> circuits
    ) {
        this.computationModel = computationModel;
        this.circuitCount = circuitCount;
        this.qubitCount = qubitCount;
        this.classicalBitCount = classicalBitCount;
        this.operationCount = operationCount;
        this.gateCount = gateCount;
        this.measurementCount = measurementCount;
        this.estimatedStateVectorAmplitudes = estimatedStateVectorAmplitudes;
        this.estimatedStateVectorBytes = estimatedStateVectorBytes;
        this.localSimulationFeasible = localSimulationFeasible;
        this.localSimulationMaxQubits = localSimulationMaxQubits;
        this.gateHistogram = Map.copyOf(gateHistogram);
        this.circuits = List.copyOf(circuits);
    }

    public QuantumComputationModel computationModel() {
        return computationModel;
    }

    public int circuitCount() {
        return circuitCount;
    }

    public int qubitCount() {
        return qubitCount;
    }

    public int classicalBitCount() {
        return classicalBitCount;
    }

    public int operationCount() {
        return operationCount;
    }

    public int gateCount() {
        return gateCount;
    }

    public int measurementCount() {
        return measurementCount;
    }

    public long estimatedStateVectorAmplitudes() {
        return estimatedStateVectorAmplitudes;
    }

    public long estimatedStateVectorBytes() {
        return estimatedStateVectorBytes;
    }

    public boolean isLocalSimulationFeasible() {
        return localSimulationFeasible;
    }

    public int localSimulationMaxQubits() {
        return localSimulationMaxQubits;
    }

    public Map<String, Integer> gateHistogram() {
        return gateHistogram;
    }

    public List<CircuitResourceEstimate> circuits() {
        return circuits;
    }
}