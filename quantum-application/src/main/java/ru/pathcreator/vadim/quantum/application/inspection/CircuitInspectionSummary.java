/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.inspection;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;

/**
 * Immutable сводка одной circuit.
 */
public final class CircuitInspectionSummary {

    private final String name;
    private final int qubitCount;
    private final int classicalBitCount;
    private final int operationCount;
    private final int gateCount;
    private final int measurementCount;
    private final int parameterizedGateCount;
    private final int modifiedGateCount;
    private final int twoQubitGateCount;
    private final int multiQubitGateCount;
    private final int approximateDepth;
    private final boolean depthPrecise;
    private final Map<OperationKind, Integer> operationKindHistogram;
    private final Map<String, Integer> gateHistogram;
    private final List<String> neverMeasuredQubits;
    private final List<String> overwrittenClassicalBits;

    public CircuitInspectionSummary(
        final String name,
        final int qubitCount,
        final int classicalBitCount,
        final int operationCount,
        final int gateCount,
        final int measurementCount,
        final int parameterizedGateCount,
        final int modifiedGateCount,
        final int twoQubitGateCount,
        final int multiQubitGateCount,
        final int approximateDepth,
        final boolean depthPrecise,
        final Map<OperationKind, Integer> operationKindHistogram,
        final Map<String, Integer> gateHistogram,
        final List<String> neverMeasuredQubits,
        final List<String> overwrittenClassicalBits
    ) {
        this.name = name;
        this.qubitCount = qubitCount;
        this.classicalBitCount = classicalBitCount;
        this.operationCount = operationCount;
        this.gateCount = gateCount;
        this.measurementCount = measurementCount;
        this.parameterizedGateCount = parameterizedGateCount;
        this.modifiedGateCount = modifiedGateCount;
        this.twoQubitGateCount = twoQubitGateCount;
        this.multiQubitGateCount = multiQubitGateCount;
        this.approximateDepth = approximateDepth;
        this.depthPrecise = depthPrecise;
        this.operationKindHistogram = new EnumMap<>(operationKindHistogram);
        this.gateHistogram = Map.copyOf(gateHistogram);
        this.neverMeasuredQubits = List.copyOf(neverMeasuredQubits);
        this.overwrittenClassicalBits = List.copyOf(overwrittenClassicalBits);
    }

    public String name() {
        return name;
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

    public int parameterizedGateCount() {
        return parameterizedGateCount;
    }

    public int modifiedGateCount() {
        return modifiedGateCount;
    }

    public int twoQubitGateCount() {
        return twoQubitGateCount;
    }

    public int multiQubitGateCount() {
        return multiQubitGateCount;
    }

    public int approximateDepth() {
        return approximateDepth;
    }

    public boolean isDepthPrecise() {
        return depthPrecise;
    }

    public Map<OperationKind, Integer> operationKindHistogram() {
        return Map.copyOf(operationKindHistogram);
    }

    public Map<String, Integer> gateHistogram() {
        return gateHistogram;
    }

    public List<String> neverMeasuredQubits() {
        return neverMeasuredQubits;
    }

    public List<String> overwrittenClassicalBits() {
        return overwrittenClassicalBits;
    }
}