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

import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;

/**
 * Immutable результат inspection всей Quantum IR программы.
 */
public final class ProgramInspectionResult {

    private final QuantumComputationModel computationModel;
    private final int circuitCount;
    private final int qubitCount;
    private final int classicalBitCount;
    private final int operationCount;
    private final int gateCount;
    private final int measurementCount;
    private final int timingOperationCount;
    private final int controlOperationCount;
    private final int callableDefinitionCount;
    private final int callableInvocationCount;
    private final int calibrationDefinitionCount;
    private final List<CircuitInspectionSummary> circuitSummaries;
    private final Map<OperationKind, Integer> operationKindHistogram;
    private final Map<String, Integer> gateHistogram;
    private final List<TargetCompatibilitySummary> targetCompatibilitySummaries;
    private final List<InspectionDiagnostic> diagnostics;

    public ProgramInspectionResult(
        final QuantumComputationModel computationModel,
        final int circuitCount,
        final int qubitCount,
        final int classicalBitCount,
        final int operationCount,
        final int gateCount,
        final int measurementCount,
        final int timingOperationCount,
        final int controlOperationCount,
        final int callableDefinitionCount,
        final int callableInvocationCount,
        final int calibrationDefinitionCount,
        final List<CircuitInspectionSummary> circuitSummaries,
        final Map<OperationKind, Integer> operationKindHistogram,
        final Map<String, Integer> gateHistogram,
        final List<TargetCompatibilitySummary> targetCompatibilitySummaries,
        final List<InspectionDiagnostic> diagnostics
    ) {
        this.computationModel = computationModel;
        this.circuitCount = circuitCount;
        this.qubitCount = qubitCount;
        this.classicalBitCount = classicalBitCount;
        this.operationCount = operationCount;
        this.gateCount = gateCount;
        this.measurementCount = measurementCount;
        this.timingOperationCount = timingOperationCount;
        this.controlOperationCount = controlOperationCount;
        this.callableDefinitionCount = callableDefinitionCount;
        this.callableInvocationCount = callableInvocationCount;
        this.calibrationDefinitionCount = calibrationDefinitionCount;
        this.circuitSummaries = List.copyOf(circuitSummaries);
        this.operationKindHistogram = new EnumMap<>(operationKindHistogram);
        this.gateHistogram = Map.copyOf(gateHistogram);
        this.targetCompatibilitySummaries = List.copyOf(targetCompatibilitySummaries);
        this.diagnostics = List.copyOf(diagnostics);
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

    public int timingOperationCount() {
        return timingOperationCount;
    }

    public int controlOperationCount() {
        return controlOperationCount;
    }

    public int callableDefinitionCount() {
        return callableDefinitionCount;
    }

    public int callableInvocationCount() {
        return callableInvocationCount;
    }

    public int calibrationDefinitionCount() {
        return calibrationDefinitionCount;
    }

    public List<CircuitInspectionSummary> circuitSummaries() {
        return circuitSummaries;
    }

    public CircuitInspectionSummary circuitSummary(final int index) {
        return circuitSummaries.get(index);
    }

    public Map<OperationKind, Integer> operationKindHistogram() {
        return Map.copyOf(operationKindHistogram);
    }

    public Map<String, Integer> gateHistogram() {
        return gateHistogram;
    }

    public List<TargetCompatibilitySummary> targetCompatibilitySummaries() {
        return targetCompatibilitySummaries;
    }

    public List<InspectionDiagnostic> diagnostics() {
        return diagnostics;
    }

    public int diagnosticCount() {
        return diagnostics.size();
    }
}