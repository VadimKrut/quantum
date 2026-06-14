/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.storage;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.model.CircuitName;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

/**
 * Плотное представление circuit для больших gate-based потоков операций.
 */
public final class CompactQuantumCircuit {

    private static final byte OPERATION_GATE = 1;
    private static final byte OPERATION_MEASURE = 2;
    private static final byte OPERATION_RESET = 3;
    private static final byte OPERATION_BARRIER = 4;
    private static final byte OPERATION_FALLBACK = 127;

    private final CircuitName name;
    private final QuantumRegisterShape[] quantumRegisters;
    private final ClassicalRegisterShape[] classicalRegisters;
    private final Gate[] gatePool;
    private final ParameterExpression[] parameterPool;
    private final Operation[] fallbackPool;
    private final byte[] operationKinds;
    private final int[] gateIndexes;
    private final int[] qubitStarts;
    private final int[] qubitCounts;
    private final int[] parameterStarts;
    private final int[] parameterCounts;
    private final int[] classicalBitIndexes;
    private final int[] qubitIndexes;
    private final int[] fallbackIndexes;
    private final int[] metadataIndexes;
    private final OperationMetadata[] metadataValues;

    private CompactQuantumCircuit(
        final CircuitName name,
        final QuantumRegisterShape[] quantumRegisters,
        final ClassicalRegisterShape[] classicalRegisters,
        final Gate[] gatePool,
        final ParameterExpression[] parameterPool,
        final Operation[] fallbackPool,
        final byte[] operationKinds,
        final int[] gateIndexes,
        final int[] qubitStarts,
        final int[] qubitCounts,
        final int[] parameterStarts,
        final int[] parameterCounts,
        final int[] classicalBitIndexes,
        final int[] qubitIndexes,
        final int[] fallbackIndexes,
        final int[] metadataIndexes,
        final OperationMetadata[] metadataValues
    ) {
        this.name = name;
        this.quantumRegisters = quantumRegisters;
        this.classicalRegisters = classicalRegisters;
        this.gatePool = gatePool;
        this.parameterPool = parameterPool;
        this.fallbackPool = fallbackPool;
        this.operationKinds = operationKinds;
        this.gateIndexes = gateIndexes;
        this.qubitStarts = qubitStarts;
        this.qubitCounts = qubitCounts;
        this.parameterStarts = parameterStarts;
        this.parameterCounts = parameterCounts;
        this.classicalBitIndexes = classicalBitIndexes;
        this.qubitIndexes = qubitIndexes;
        this.fallbackIndexes = fallbackIndexes;
        this.metadataIndexes = metadataIndexes;
        this.metadataValues = metadataValues;
    }

    public static CompactQuantumCircuit from(final QuantumCircuit circuit) {
        if (circuit == null) {
            throw new IllegalArgumentException("Quantum circuit must not be null.");
        }
        final Builder builder = new Builder(circuit);
        builder.appendOperations(circuit);
        return builder.build();
    }

    public CircuitName name() {
        return name;
    }

    public int operationCount() {
        return operationKinds.length;
    }

    public int compactOperationCount() {
        int count = 0;
        for (int i = 0; i < operationKinds.length; i++) {
            if (operationKinds[i] != OPERATION_FALLBACK) {
                count++;
            }
        }
        return count;
    }

    public int fallbackOperationCount() {
        return fallbackPool.length;
    }

    public Operation operation(final int index) {
        validateOperationIndex(index);
        return switch (operationKinds[index]) {
            case OPERATION_GATE -> gateOperation(index);
            case OPERATION_MEASURE -> measureOperation(index);
            case OPERATION_RESET -> resetOperation(index);
            case OPERATION_BARRIER -> barrierOperation(index);
            case OPERATION_FALLBACK -> fallbackPool[fallbackIndexes[index]];
            default -> throw new IllegalStateException("Unknown compact operation kind.");
        };
    }

    public OperationMetadata operationMetadata(final int index) {
        validateOperationIndex(index);
        final int metadataIndex = metadataIndex(index);
        if (metadataIndex < 0) {
            return OperationMetadata.empty();
        }
        return metadataValues[metadataIndex];
    }

    public QuantumCircuit toCircuit(final QuantumProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        final QuantumCircuit circuit = program.createCircuit(name.value());
        final QuantumRegister[] createdQuantumRegisters = createQuantumRegisters(circuit);
        final ClassicalRegister[] createdClassicalRegisters = createClassicalRegisters(circuit);
        final Qubit[] qubits = qubits(createdQuantumRegisters);
        final ClassicalBit[] classicalBits = classicalBits(createdClassicalRegisters);
        circuit.reserveOperationCapacity(operationCount());
        for (int i = 0; i < operationCount(); i++) {
            appendOperation(
                circuit,
                i,
                qubits,
                classicalBits
            );
            final OperationMetadata metadata = operationMetadata(i);
            if (!metadata.isEmpty()) {
                circuit.setOperationMetadata(
                    circuit.operationCount() - 1,
                    metadata
                );
            }
        }
        return circuit;
    }

    private GateOperation gateOperation(final int index) {
        return GateOperation.parameterized(
            gatePool[gateIndexes[index]],
            parameters(index),
            qubits(index)
        );
    }

    private MeasureOperation measureOperation(final int index) {
        return new MeasureOperation(
            qubit(qubitIndexes[qubitStarts[index]]),
            classicalBit(classicalBitIndexes[index])
        );
    }

    private ResetOperation resetOperation(final int index) {
        return new ResetOperation(qubit(qubitIndexes[qubitStarts[index]]));
    }

    private BarrierOperation barrierOperation(final int index) {
        return new BarrierOperation(qubits(index));
    }

    private void appendOperation(
        final QuantumCircuit circuit,
        final int operationIndex,
        final Qubit[] qubits,
        final ClassicalBit[] classicalBits
    ) {
        switch (operationKinds[operationIndex]) {
            case OPERATION_GATE -> circuit.parameterizedGate(
                gatePool[gateIndexes[operationIndex]],
                parameters(operationIndex),
                mappedQubits(
                    operationIndex,
                    qubits
                )
            );
            case OPERATION_MEASURE -> circuit.measure(
                qubits[qubitIndexes[qubitStarts[operationIndex]]],
                classicalBits[classicalBitIndexes[operationIndex]]
            );
            case OPERATION_RESET -> circuit.reset(qubits[qubitIndexes[qubitStarts[operationIndex]]]);
            case OPERATION_BARRIER -> circuit.barrier(mappedQubits(
                operationIndex,
                qubits
            ));
            case OPERATION_FALLBACK -> throw new IllegalStateException(
                "Fallback operations cannot be materialized into a different circuit safely."
            );
            default -> throw new IllegalStateException("Unknown compact operation kind.");
        }
    }

    private ParameterExpression[] parameters(final int operationIndex) {
        final int count = parameterCounts[operationIndex];
        final ParameterExpression[] parameters = new ParameterExpression[count];
        final int start = parameterStarts[operationIndex];
        for (int i = 0; i < count; i++) {
            parameters[i] = parameterPool[start + i];
        }
        return parameters;
    }

    private Qubit[] qubits(final int operationIndex) {
        final int count = qubitCounts[operationIndex];
        final Qubit[] qubits = new Qubit[count];
        final int start = qubitStarts[operationIndex];
        for (int i = 0; i < count; i++) {
            qubits[i] = qubit(qubitIndexes[start + i]);
        }
        return qubits;
    }

    private Qubit[] mappedQubits(
        final int operationIndex,
        final Qubit[] qubits
    ) {
        final int count = qubitCounts[operationIndex];
        final Qubit[] mapped = new Qubit[count];
        final int start = qubitStarts[operationIndex];
        for (int i = 0; i < count; i++) {
            mapped[i] = qubits[qubitIndexes[start + i]];
        }
        return mapped;
    }

    private Qubit qubit(final int index) {
        int remaining = index;
        for (int i = 0; i < quantumRegisters.length; i++) {
            if (remaining < quantumRegisters[i].register.size()) {
                return quantumRegisters[i].register.get(remaining);
            }
            remaining -= quantumRegisters[i].register.size();
        }
        throw new IllegalStateException("Compact qubit index is outside of circuit bounds.");
    }

    private ClassicalBit classicalBit(final int index) {
        int remaining = index;
        for (int i = 0; i < classicalRegisters.length; i++) {
            if (remaining < classicalRegisters[i].register.size()) {
                return classicalRegisters[i].register.get(remaining);
            }
            remaining -= classicalRegisters[i].register.size();
        }
        throw new IllegalStateException("Compact classical bit index is outside of circuit bounds.");
    }

    private QuantumRegister[] createQuantumRegisters(final QuantumCircuit circuit) {
        final QuantumRegister[] registers = new QuantumRegister[quantumRegisters.length];
        for (int i = 0; i < quantumRegisters.length; i++) {
            registers[i] = circuit.createQuantumRegister(
                quantumRegisters[i].name,
                quantumRegisters[i].size
            );
        }
        return registers;
    }

    private ClassicalRegister[] createClassicalRegisters(final QuantumCircuit circuit) {
        final ClassicalRegister[] registers = new ClassicalRegister[classicalRegisters.length];
        for (int i = 0; i < classicalRegisters.length; i++) {
            registers[i] = circuit.createClassicalRegister(
                classicalRegisters[i].name,
                classicalRegisters[i].size
            );
        }
        return registers;
    }

    private static Qubit[] qubits(final QuantumRegister[] registers) {
        int count = 0;
        for (int i = 0; i < registers.length; i++) {
            count += registers[i].size();
        }
        final Qubit[] qubits = new Qubit[count];
        int offset = 0;
        for (int i = 0; i < registers.length; i++) {
            for (int j = 0; j < registers[i].size(); j++) {
                qubits[offset + j] = registers[i].get(j);
            }
            offset += registers[i].size();
        }
        return qubits;
    }

    private static ClassicalBit[] classicalBits(final ClassicalRegister[] registers) {
        int count = 0;
        for (int i = 0; i < registers.length; i++) {
            count += registers[i].size();
        }
        final ClassicalBit[] bits = new ClassicalBit[count];
        int offset = 0;
        for (int i = 0; i < registers.length; i++) {
            for (int j = 0; j < registers[i].size(); j++) {
                bits[offset + j] = registers[i].get(j);
            }
            offset += registers[i].size();
        }
        return bits;
    }

    private int metadataIndex(final int operationIndex) {
        int low = 0;
        int high = metadataIndexes.length - 1;
        while (low <= high) {
            final int middle = (low + high) >>> 1;
            final int value = metadataIndexes[middle];
            if (value < operationIndex) {
                low = middle + 1;
            } else if (value > operationIndex) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    private void validateOperationIndex(final int index) {
        if (
            index < 0
            || index >= operationKinds.length
        ) {
            throw new IllegalArgumentException("Compact operation index is outside of circuit bounds.");
        }
    }

    private record QuantumRegisterShape(
        String name,
        int size,
        QuantumRegister register
    ) {
    }

    private record ClassicalRegisterShape(
        String name,
        int size,
        ClassicalRegister register
    ) {
    }

    private static final class Builder {

        private final CircuitName name;
        private final QuantumRegisterShape[] quantumRegisters;
        private final ClassicalRegisterShape[] classicalRegisters;
        private final IdentityHashMap<QuantumRegister, Integer> quantumRegisterOffsets;
        private final IdentityHashMap<ClassicalRegister, Integer> classicalRegisterOffsets;
        private final LinkedHashMap<Gate, Integer> gateIndexes;
        private final ArrayList<Gate> gates;
        private final ArrayList<ParameterExpression> parameters;
        private final ArrayList<Operation> fallbackOperations;
        private final ByteList operationKinds;
        private final IntList operationGateIndexes;
        private final IntList operationQubitStarts;
        private final IntList operationQubitCounts;
        private final IntList operationParameterStarts;
        private final IntList operationParameterCounts;
        private final IntList operationClassicalBitIndexes;
        private final IntList operationFallbackIndexes;
        private final IntList qubitIndexes;
        private final IntList metadataIndexes;
        private final ArrayList<OperationMetadata> metadataValues;

        private Builder(final QuantumCircuit circuit) {
            this.name = circuit.name();
            this.quantumRegisters = quantumRegisters(circuit);
            this.classicalRegisters = classicalRegisters(circuit);
            this.quantumRegisterOffsets = quantumRegisterOffsets(quantumRegisters);
            this.classicalRegisterOffsets = classicalRegisterOffsets(classicalRegisters);
            this.gateIndexes = new LinkedHashMap<>();
            this.gates = new ArrayList<>();
            this.parameters = new ArrayList<>();
            this.fallbackOperations = new ArrayList<>();
            this.operationKinds = new ByteList(circuit.operationCount());
            this.operationGateIndexes = new IntList(circuit.operationCount());
            this.operationQubitStarts = new IntList(circuit.operationCount());
            this.operationQubitCounts = new IntList(circuit.operationCount());
            this.operationParameterStarts = new IntList(circuit.operationCount());
            this.operationParameterCounts = new IntList(circuit.operationCount());
            this.operationClassicalBitIndexes = new IntList(circuit.operationCount());
            this.operationFallbackIndexes = new IntList(circuit.operationCount());
            this.qubitIndexes = new IntList(circuit.operationCount() * 2);
            this.metadataIndexes = new IntList(0);
            this.metadataValues = new ArrayList<>();
        }

        private void appendOperations(final QuantumCircuit circuit) {
            for (int i = 0; i < circuit.operationCount(); i++) {
                appendOperation(circuit.operation(i));
                appendMetadata(
                    i,
                    circuit.operationMetadata(i)
                );
            }
        }

        private void appendOperation(final Operation operation) {
            if (operation instanceof GateOperation gateOperation && canCompact(gateOperation)) {
                appendGate(gateOperation);
            } else if (operation instanceof MeasureOperation measureOperation) {
                appendMeasure(measureOperation);
            } else if (operation instanceof ResetOperation resetOperation && resetOperation.qubitReference().isStatic()) {
                appendReset(resetOperation);
            } else if (operation instanceof BarrierOperation barrierOperation) {
                appendBarrier(barrierOperation);
            } else {
                appendFallback(operation);
            }
        }

        private void appendGate(final GateOperation operation) {
            operationKinds.add(OPERATION_GATE);
            operationGateIndexes.add(gateIndex(operation.gate()));
            operationQubitStarts.add(qubitIndexes.size());
            operationQubitCounts.add(operation.qubitCount());
            operationParameterStarts.add(parameters.size());
            operationParameterCounts.add(operation.parameterCount());
            operationClassicalBitIndexes.add(-1);
            operationFallbackIndexes.add(-1);
            for (int i = 0; i < operation.qubitCount(); i++) {
                qubitIndexes.add(qubitIndex(operation.qubit(i)));
            }
            for (int i = 0; i < operation.parameterCount(); i++) {
                parameters.add(operation.parameter(i));
            }
        }

        private void appendMeasure(final MeasureOperation operation) {
            if (!operation.qubitReference().isStatic()) {
                appendFallback(operation);
                return;
            }
            operationKinds.add(OPERATION_MEASURE);
            operationGateIndexes.add(-1);
            operationQubitStarts.add(qubitIndexes.size());
            operationQubitCounts.add(1);
            operationParameterStarts.add(parameters.size());
            operationParameterCounts.add(0);
            operationClassicalBitIndexes.add(classicalBitIndex(operation.bit()));
            operationFallbackIndexes.add(-1);
            qubitIndexes.add(qubitIndex(operation.qubit()));
        }

        private void appendReset(final ResetOperation operation) {
            operationKinds.add(OPERATION_RESET);
            operationGateIndexes.add(-1);
            operationQubitStarts.add(qubitIndexes.size());
            operationQubitCounts.add(1);
            operationParameterStarts.add(parameters.size());
            operationParameterCounts.add(0);
            operationClassicalBitIndexes.add(-1);
            operationFallbackIndexes.add(-1);
            qubitIndexes.add(qubitIndex(operation.qubit()));
        }

        private void appendBarrier(final BarrierOperation operation) {
            operationKinds.add(OPERATION_BARRIER);
            operationGateIndexes.add(-1);
            operationQubitStarts.add(qubitIndexes.size());
            operationQubitCounts.add(operation.qubitCount());
            operationParameterStarts.add(parameters.size());
            operationParameterCounts.add(0);
            operationClassicalBitIndexes.add(-1);
            operationFallbackIndexes.add(-1);
            for (int i = 0; i < operation.qubitCount(); i++) {
                qubitIndexes.add(qubitIndex(operation.qubit(i)));
            }
        }

        private void appendFallback(final Operation operation) {
            operationKinds.add(OPERATION_FALLBACK);
            operationGateIndexes.add(-1);
            operationQubitStarts.add(qubitIndexes.size());
            operationQubitCounts.add(0);
            operationParameterStarts.add(parameters.size());
            operationParameterCounts.add(0);
            operationClassicalBitIndexes.add(-1);
            operationFallbackIndexes.add(fallbackOperations.size());
            fallbackOperations.add(operation);
        }

        private void appendMetadata(
            final int operationIndex,
            final OperationMetadata metadata
        ) {
            if (!metadata.isEmpty()) {
                metadataIndexes.add(operationIndex);
                metadataValues.add(metadata);
            }
        }

        private int gateIndex(final Gate gate) {
            final Integer existing = gateIndexes.get(gate);
            if (existing != null) {
                return existing;
            }
            final int index = gates.size();
            gateIndexes.put(
                gate,
                index
            );
            gates.add(gate);
            return index;
        }

        private int qubitIndex(final Qubit qubit) {
            final Integer offset = quantumRegisterOffsets.get(qubit.register());
            if (offset == null) {
                throw new IllegalArgumentException("Qubit does not belong to compacted circuit.");
            }
            return offset + qubit.index();
        }

        private int classicalBitIndex(final ClassicalBit bit) {
            final Integer offset = classicalRegisterOffsets.get(bit.register());
            if (offset == null) {
                throw new IllegalArgumentException("Classical bit does not belong to compacted circuit.");
            }
            return offset + bit.index();
        }

        private CompactQuantumCircuit build() {
            return new CompactQuantumCircuit(
                name,
                quantumRegisters,
                classicalRegisters,
                gates.toArray(Gate[]::new),
                parameters.toArray(ParameterExpression[]::new),
                fallbackOperations.toArray(Operation[]::new),
                operationKinds.toArray(),
                operationGateIndexes.toArray(),
                operationQubitStarts.toArray(),
                operationQubitCounts.toArray(),
                operationParameterStarts.toArray(),
                operationParameterCounts.toArray(),
                operationClassicalBitIndexes.toArray(),
                qubitIndexes.toArray(),
                operationFallbackIndexes.toArray(),
                metadataIndexes.toArray(),
                metadataValues.toArray(OperationMetadata[]::new)
            );
        }

        private static boolean canCompact(final GateOperation operation) {
            for (int i = 0; i < operation.qubitCount(); i++) {
                if (!operation.qubitReference(i).isStatic()) {
                    return false;
                }
            }
            return true;
        }

        private static QuantumRegisterShape[] quantumRegisters(final QuantumCircuit circuit) {
            final QuantumRegisterShape[] registers = new QuantumRegisterShape[circuit.quantumRegisterCount()];
            for (int i = 0; i < registers.length; i++) {
                final QuantumRegister register = circuit.quantumRegister(i);
                registers[i] = new QuantumRegisterShape(
                    register.name().value(),
                    register.size(),
                    register
                );
            }
            return registers;
        }

        private static ClassicalRegisterShape[] classicalRegisters(final QuantumCircuit circuit) {
            final ClassicalRegisterShape[] registers = new ClassicalRegisterShape[circuit.classicalRegisterCount()];
            for (int i = 0; i < registers.length; i++) {
                final ClassicalRegister register = circuit.classicalRegister(i);
                registers[i] = new ClassicalRegisterShape(
                    register.name().value(),
                    register.size(),
                    register
                );
            }
            return registers;
        }

        private static IdentityHashMap<QuantumRegister, Integer> quantumRegisterOffsets(final QuantumRegisterShape[] registers) {
            final IdentityHashMap<QuantumRegister, Integer> offsets = new IdentityHashMap<>();
            int offset = 0;
            for (int i = 0; i < registers.length; i++) {
                offsets.put(
                    registers[i].register(),
                    offset
                );
                offset += registers[i].size();
            }
            return offsets;
        }

        private static IdentityHashMap<ClassicalRegister, Integer> classicalRegisterOffsets(final ClassicalRegisterShape[] registers) {
            final IdentityHashMap<ClassicalRegister, Integer> offsets = new IdentityHashMap<>();
            int offset = 0;
            for (int i = 0; i < registers.length; i++) {
                offsets.put(
                    registers[i].register(),
                    offset
                );
                offset += registers[i].size();
            }
            return offsets;
        }
    }
}