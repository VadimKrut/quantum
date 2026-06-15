/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.inspection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.BlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.DelayOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReferenceKind;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.operation.TimingBoxOperation;
import ru.pathcreator.vadim.quantum.domain.operation.WhileLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Сервис inspection для Quantum IR программ.
 */
public final class QuantumProgramInspector {

    private final CapabilityPreflightChecker preflightChecker;

    public QuantumProgramInspector() {
        this.preflightChecker = new CapabilityPreflightChecker();
    }

    public ProgramInspectionResult inspect(final QuantumProgram program) {
        return inspect(
            program,
            List.of()
        );
    }

    public ProgramInspectionResult inspect(
        final QuantumProgram program,
        final List<IntegrationCapabilityProfile> targetProfiles
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Inspected quantum program must not be null.");
        }
        if (targetProfiles == null) {
            throw new IllegalArgumentException("Inspection target profiles must not be null.");
        }
        final ArrayList<CircuitInspectionSummary> circuits = new ArrayList<>();
        final ArrayList<InspectionDiagnostic> diagnostics = new ArrayList<>();
        final EnumMap<OperationKind, Integer> operationHistogram = emptyOperationHistogram();
        final LinkedHashMap<String, Integer> gateHistogram = new LinkedHashMap<>();
        int qubitCount = 0;
        int classicalBitCount = 0;
        int operationCount = 0;
        int gateCount = 0;
        int measurementCount = 0;
        int timingOperationCount = 0;
        int controlOperationCount = 0;
        int callableInvocationCount = 0;
        for (int i = 0; i < program.circuitCount(); i++) {
            final CircuitInspectionSummary circuitSummary = inspectCircuit(
                program.circuit(i),
                i,
                diagnostics
            );
            circuits.add(circuitSummary);
            qubitCount += circuitSummary.qubitCount();
            classicalBitCount += circuitSummary.classicalBitCount();
            operationCount += circuitSummary.operationCount();
            gateCount += circuitSummary.gateCount();
            measurementCount += circuitSummary.measurementCount();
            timingOperationCount += circuitSummary.operationKindHistogram().get(OperationKind.DELAY)
                + circuitSummary.operationKindHistogram().get(OperationKind.TIMING_BOX);
            controlOperationCount += circuitSummary.operationKindHistogram().get(OperationKind.CONTROLLED)
                + circuitSummary.operationKindHistogram().get(OperationKind.CLASSICALLY_CONTROLLED)
                + circuitSummary.operationKindHistogram().get(OperationKind.CONDITIONAL_BLOCK)
                + circuitSummary.operationKindHistogram().get(OperationKind.FOR_LOOP)
                + circuitSummary.operationKindHistogram().get(OperationKind.SYMBOLIC_FOR_LOOP)
                + circuitSummary.operationKindHistogram().get(OperationKind.WHILE_LOOP);
            callableInvocationCount += circuitSummary.operationKindHistogram().get(OperationKind.CALLABLE_INVOCATION);
            mergeOperationHistogram(
                operationHistogram,
                circuitSummary.operationKindHistogram()
            );
            mergeGateHistogram(
                gateHistogram,
                circuitSummary.gateHistogram()
            );
        }
        final ArrayList<TargetCompatibilitySummary> compatibilitySummaries = inspectTargets(
            program,
            targetProfiles,
            diagnostics
        );
        return new ProgramInspectionResult(
            program.computationModel(),
            program.circuitCount(),
            qubitCount,
            classicalBitCount,
            operationCount,
            gateCount,
            measurementCount,
            timingOperationCount,
            controlOperationCount,
            program.callableDefinitionCount() + program.externalCallableDeclarationCount(),
            callableInvocationCount,
            program.calibrationDefinitionCount(),
            circuits,
            operationHistogram,
            gateHistogram,
            compatibilitySummaries,
            diagnostics
        );
    }

    private CircuitInspectionSummary inspectCircuit(
        final QuantumCircuit circuit,
        final int circuitIndex,
        final ArrayList<InspectionDiagnostic> diagnostics
    ) {
        final CircuitAccumulator accumulator = new CircuitAccumulator(circuit);
        for (int i = 0; i < circuit.operationCount(); i++) {
            inspectOperation(
                circuit.operation(i),
                circuitIndex,
                i,
                accumulator,
                diagnostics,
                true
            );
        }
        return accumulator.toSummary();
    }

    private void inspectOperation(
        final Operation operation,
        final int circuitIndex,
        final int operationIndex,
        final CircuitAccumulator accumulator,
        final ArrayList<InspectionDiagnostic> diagnostics,
        final boolean includeDepth
    ) {
        accumulator.incrementOperation(operation.kind());
        if (operation instanceof GateOperation gateOperation) {
            inspectGate(
                gateOperation,
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics,
                includeDepth
            );
        } else if (operation instanceof MeasureOperation measureOperation) {
            inspectMeasure(
                measureOperation,
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics,
                includeDepth
            );
        } else if (operation instanceof ResetOperation resetOperation) {
            inspectQuantumReferenceDepth(
                resetOperation.qubitReference(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics,
                includeDepth
            );
        } else if (operation instanceof BarrierOperation barrierOperation) {
            inspectBarrierDepth(
                barrierOperation,
                accumulator,
                includeDepth
            );
        } else if (operation instanceof DelayOperation delayOperation) {
            accumulator.markDepthApproximate(diagnostics, circuitIndex, operationIndex, InspectionDiagnosticCode.TIMING_DEPTH_APPROXIMATION);
            for (int i = 0; i < delayOperation.qubitCount(); i++) {
                inspectQuantumReferenceDepth(
                    delayOperation.reference(i),
                    circuitIndex,
                    operationIndex,
                    accumulator,
                    diagnostics,
                    includeDepth
                );
            }
        } else if (operation instanceof ControlledOperation controlledOperation) {
            accumulator.markDepthApproximate(diagnostics, circuitIndex, operationIndex, InspectionDiagnosticCode.CONTROL_FLOW_DEPTH_APPROXIMATION);
            inspectOperation(
                controlledOperation.operation(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics,
                false
            );
        } else if (operation instanceof ClassicallyControlledOperation controlledOperation) {
            accumulator.markDepthApproximate(diagnostics, circuitIndex, operationIndex, InspectionDiagnosticCode.CONTROL_FLOW_DEPTH_APPROXIMATION);
            inspectOperation(
                controlledOperation.operation(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics,
                false
            );
        } else if (operation instanceof BlockOperation blockOperation) {
            inspectBlock(
                blockOperation.body(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics
            );
        } else if (operation instanceof ConditionalBlockOperation conditionalOperation) {
            accumulator.markDepthApproximate(diagnostics, circuitIndex, operationIndex, InspectionDiagnosticCode.CONTROL_FLOW_DEPTH_APPROXIMATION);
            inspectBlock(
                conditionalOperation.thenBlock(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics
            );
            if (conditionalOperation.hasElseBlock()) {
                inspectBlock(
                    conditionalOperation.elseBlock(),
                    circuitIndex,
                    operationIndex,
                    accumulator,
                    diagnostics
                );
            }
        } else if (operation instanceof ForLoopOperation loopOperation) {
            accumulator.markDepthApproximate(diagnostics, circuitIndex, operationIndex, InspectionDiagnosticCode.CONTROL_FLOW_DEPTH_APPROXIMATION);
            inspectBlock(
                loopOperation.body(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics
            );
        } else if (operation instanceof SymbolicForLoopOperation loopOperation) {
            accumulator.markDepthApproximate(diagnostics, circuitIndex, operationIndex, InspectionDiagnosticCode.CONTROL_FLOW_DEPTH_APPROXIMATION);
            inspectBlock(
                loopOperation.body(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics
            );
        } else if (operation instanceof WhileLoopOperation loopOperation) {
            accumulator.markDepthApproximate(diagnostics, circuitIndex, operationIndex, InspectionDiagnosticCode.CONTROL_FLOW_DEPTH_APPROXIMATION);
            inspectBlock(
                loopOperation.body(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics
            );
        } else if (operation instanceof TimingBoxOperation boxOperation) {
            accumulator.markDepthApproximate(diagnostics, circuitIndex, operationIndex, InspectionDiagnosticCode.TIMING_DEPTH_APPROXIMATION);
            inspectBlock(
                boxOperation.body(),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics
            );
        }
    }

    private void inspectBlock(
        final OperationBlock block,
        final int circuitIndex,
        final int operationIndex,
        final CircuitAccumulator accumulator,
        final ArrayList<InspectionDiagnostic> diagnostics
    ) {
        for (int i = 0; i < block.operationCount(); i++) {
            inspectOperation(
                block.operation(i),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics,
                false
            );
        }
    }

    private void inspectGate(
        final GateOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final CircuitAccumulator accumulator,
        final ArrayList<InspectionDiagnostic> diagnostics,
        final boolean includeDepth
    ) {
        accumulator.gateCount++;
        incrementGateHistogram(
            accumulator.gateHistogram,
            operation.gate().gateName()
        );
        if (operation.parameterCount() > 0) {
            accumulator.parameterizedGateCount++;
        }
        if (operation.gate() instanceof ModifiedGate) {
            accumulator.modifiedGateCount++;
        }
        if (operation.qubitCount() == 2) {
            accumulator.twoQubitGateCount++;
        } else if (operation.qubitCount() > 2) {
            accumulator.multiQubitGateCount++;
        }
        if (!includeDepth) {
            return;
        }
        final ArrayList<Qubit> qubits = new ArrayList<>(operation.qubitCount());
        for (int i = 0; i < operation.qubitCount(); i++) {
            final Qubit qubit = staticQubitOrNull(
                operation.qubitReference(i),
                circuitIndex,
                operationIndex,
                accumulator,
                diagnostics
            );
            if (qubit == null) {
                return;
            }
            qubits.add(qubit);
        }
        accumulator.advanceDepth(qubits);
    }

    private void inspectMeasure(
        final MeasureOperation operation,
        final int circuitIndex,
        final int operationIndex,
        final CircuitAccumulator accumulator,
        final ArrayList<InspectionDiagnostic> diagnostics,
        final boolean includeDepth
    ) {
        accumulator.measurementCount++;
        final String bitKey = operation.bit().toString();
        accumulator.classicalWrites.put(
            bitKey,
            accumulator.classicalWrites.getOrDefault(bitKey, 0) + 1
        );
        final Qubit qubit = staticQubitOrNull(
            operation.qubitReference(),
            circuitIndex,
            operationIndex,
            accumulator,
            diagnostics
        );
        if (qubit != null) {
            accumulator.measuredQubits.put(
                qubit.toString(),
                Boolean.TRUE
            );
            if (includeDepth) {
                accumulator.advanceDepth(List.of(qubit));
            }
        }
    }

    private void inspectQuantumReferenceDepth(
        final QuantumReference reference,
        final int circuitIndex,
        final int operationIndex,
        final CircuitAccumulator accumulator,
        final ArrayList<InspectionDiagnostic> diagnostics,
        final boolean includeDepth
    ) {
        if (!includeDepth) {
            return;
        }
        final Qubit qubit = staticQubitOrNull(
            reference,
            circuitIndex,
            operationIndex,
            accumulator,
            diagnostics
        );
        if (qubit != null) {
            accumulator.advanceDepth(List.of(qubit));
        }
    }

    private void inspectBarrierDepth(
        final BarrierOperation operation,
        final CircuitAccumulator accumulator,
        final boolean includeDepth
    ) {
        if (!includeDepth) {
            return;
        }
        final ArrayList<Qubit> qubits = new ArrayList<>(operation.qubitCount());
        for (int i = 0; i < operation.qubitCount(); i++) {
            qubits.add(operation.qubit(i));
        }
        accumulator.advanceDepth(qubits);
    }

    private Qubit staticQubitOrNull(
        final QuantumReference reference,
        final int circuitIndex,
        final int operationIndex,
        final CircuitAccumulator accumulator,
        final ArrayList<InspectionDiagnostic> diagnostics
    ) {
        if (reference.kind() == QuantumReferenceKind.STATIC_QUBIT) {
            return reference.qubit();
        }
        accumulator.markDepthApproximate(
            diagnostics,
            circuitIndex,
            operationIndex,
            InspectionDiagnosticCode.DYNAMIC_QUBIT_REFERENCE_DEPTH_APPROXIMATION
        );
        return null;
    }

    private ArrayList<TargetCompatibilitySummary> inspectTargets(
        final QuantumProgram program,
        final List<IntegrationCapabilityProfile> targetProfiles,
        final ArrayList<InspectionDiagnostic> diagnostics
    ) {
        final ArrayList<TargetCompatibilitySummary> summaries = new ArrayList<>();
        for (int i = 0; i < targetProfiles.size(); i++) {
            final IntegrationCapabilityProfile profile = targetProfiles.get(i);
            if (profile == null) {
                throw new IllegalArgumentException("Inspection target profile must not be null.");
            }
            final CapabilityPreflightResult preflight = preflightChecker.check(
                program,
                profile
            );
            summaries.add(TargetCompatibilitySummary.of(
                profile,
                preflight
            ));
            for (int j = 0; j < preflight.diagnostics().size(); j++) {
                final IntegrationDiagnostic diagnostic = preflight.diagnostics().get(j);
                diagnostics.add(InspectionDiagnostic.targetWarning(
                    diagnostic.message(),
                    profile.targetName()
                ));
            }
        }
        return summaries;
    }

    private static EnumMap<OperationKind, Integer> emptyOperationHistogram() {
        final EnumMap<OperationKind, Integer> histogram = new EnumMap<>(OperationKind.class);
        final OperationKind[] kinds = OperationKind.values();
        for (int i = 0; i < kinds.length; i++) {
            histogram.put(
                kinds[i],
                0
            );
        }
        return histogram;
    }

    private static void mergeOperationHistogram(
        final EnumMap<OperationKind, Integer> target,
        final java.util.Map<OperationKind, Integer> source
    ) {
        final OperationKind[] kinds = OperationKind.values();
        for (int i = 0; i < kinds.length; i++) {
            target.put(
                kinds[i],
                target.get(kinds[i]) + source.get(kinds[i])
            );
        }
    }

    private static void mergeGateHistogram(
        final LinkedHashMap<String, Integer> target,
        final java.util.Map<String, Integer> source
    ) {
        final String[] names = source.keySet().toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            target.put(
                name,
                target.getOrDefault(name, 0) + source.get(name)
            );
        }
    }

    private static void incrementGateHistogram(
        final LinkedHashMap<String, Integer> histogram,
        final String gateName
    ) {
        histogram.put(
            gateName,
            histogram.getOrDefault(gateName, 0) + 1
        );
    }

    private static final class CircuitAccumulator {

        private final QuantumCircuit circuit;
        private final EnumMap<OperationKind, Integer> operationHistogram;
        private final LinkedHashMap<String, Integer> gateHistogram;
        private final LinkedHashMap<String, Boolean> allQubits;
        private final LinkedHashMap<String, Boolean> measuredQubits;
        private final LinkedHashMap<String, Integer> classicalWrites;
        private final IdentityHashMap<Qubit, Integer> qubitDepths;
        private int operationCount;
        private int gateCount;
        private int measurementCount;
        private int parameterizedGateCount;
        private int modifiedGateCount;
        private int twoQubitGateCount;
        private int multiQubitGateCount;
        private int approximateDepth;
        private boolean depthPrecise;

        private CircuitAccumulator(final QuantumCircuit circuit) {
            this.circuit = circuit;
            this.operationHistogram = emptyOperationHistogram();
            this.gateHistogram = new LinkedHashMap<>();
            this.allQubits = new LinkedHashMap<>();
            this.measuredQubits = new LinkedHashMap<>();
            this.classicalWrites = new LinkedHashMap<>();
            this.qubitDepths = new IdentityHashMap<>();
            this.depthPrecise = true;
            collectBits();
        }

        private void collectBits() {
            for (int i = 0; i < circuit.quantumRegisterCount(); i++) {
                final QuantumRegister register = circuit.quantumRegister(i);
                for (int j = 0; j < register.size(); j++) {
                    final Qubit qubit = register.get(j);
                    allQubits.put(
                        qubit.toString(),
                        Boolean.TRUE
                    );
                    qubitDepths.put(
                        qubit,
                        0
                    );
                }
            }
            for (int i = 0; i < circuit.classicalRegisterCount(); i++) {
                final ClassicalRegister register = circuit.classicalRegister(i);
                for (int j = 0; j < register.size(); j++) {
                    classicalWrites.put(
                        ClassicalBit.of(
                            register,
                            j
                        ).toString(),
                        0
                    );
                }
            }
        }

        private void incrementOperation(final OperationKind kind) {
            operationCount++;
            operationHistogram.put(
                kind,
                operationHistogram.get(kind) + 1
            );
        }

        private void advanceDepth(final List<Qubit> qubits) {
            int nextDepth = 0;
            for (int i = 0; i < qubits.size(); i++) {
                final Integer depth = qubitDepths.get(qubits.get(i));
                if (
                    depth != null
                    && depth > nextDepth
                ) {
                    nextDepth = depth;
                }
            }
            nextDepth++;
            for (int i = 0; i < qubits.size(); i++) {
                qubitDepths.put(
                    qubits.get(i),
                    nextDepth
                );
            }
            if (nextDepth > approximateDepth) {
                approximateDepth = nextDepth;
            }
        }

        private void markDepthApproximate(
            final ArrayList<InspectionDiagnostic> diagnostics,
            final int circuitIndex,
            final int operationIndex,
            final InspectionDiagnosticCode code
        ) {
            depthPrecise = false;
            diagnostics.add(InspectionDiagnostic.warning(
                code,
                "Circuit depth is approximate for this operation.",
                circuitIndex,
                operationIndex
            ));
        }

        private CircuitInspectionSummary toSummary() {
            return new CircuitInspectionSummary(
                circuit.name().value(),
                allQubits.size(),
                classicalWrites.size(),
                operationCount,
                gateCount,
                measurementCount,
                parameterizedGateCount,
                modifiedGateCount,
                twoQubitGateCount,
                multiQubitGateCount,
                approximateDepth,
                depthPrecise,
                operationHistogram,
                gateHistogram,
                neverMeasuredQubits(),
                overwrittenClassicalBits()
            );
        }

        private List<String> neverMeasuredQubits() {
            final ArrayList<String> result = new ArrayList<>();
            final String[] qubits = allQubits.keySet().toArray(new String[0]);
            for (int i = 0; i < qubits.length; i++) {
                if (!measuredQubits.containsKey(qubits[i])) {
                    result.add(qubits[i]);
                }
            }
            return result;
        }

        private List<String> overwrittenClassicalBits() {
            final ArrayList<String> result = new ArrayList<>();
            final String[] bits = classicalWrites.keySet().toArray(new String[0]);
            for (int i = 0; i < bits.length; i++) {
                if (classicalWrites.get(bits[i]) > 1) {
                    result.add(bits[i]);
                }
            }
            return result;
        }
    }
}