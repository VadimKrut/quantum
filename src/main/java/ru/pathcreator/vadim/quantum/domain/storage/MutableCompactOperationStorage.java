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
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;

/**
 * Mutable compact storage для операций circuit: operation tape с primitive indexes и object pools.
 */
public final class MutableCompactOperationStorage {

    private static final byte OPERATION_GATE = 1;
    private static final byte OPERATION_MEASURE = 2;
    private static final byte OPERATION_RESET = 3;
    private static final byte OPERATION_BARRIER = 4;
    private static final byte OPERATION_FALLBACK = 127;

    private final LinkedHashMap<Gate, Integer> gatePoolIndexes = new LinkedHashMap<>();
    private final LinkedHashMap<ParameterExpression, Integer> parameterPoolIndexes = new LinkedHashMap<>();
    private final IdentityHashMap<Qubit, Integer> qubitPoolIndexes = new IdentityHashMap<>();
    private final IdentityHashMap<ClassicalBit, Integer> classicalBitPoolIndexes = new IdentityHashMap<>();
    private byte[] kinds = new byte[0];
    private int[] gateIndexes = new int[0];
    private int[] qubitStarts = new int[0];
    private int[] qubitCounts = new int[0];
    private int[] parameterStarts = new int[0];
    private int[] parameterCounts = new int[0];
    private int[] classicalBitIndexes = new int[0];
    private Operation[] fallbackOperations = new Operation[0];
    private OperationMetadata[] metadata = new OperationMetadata[0];
    private int[] qubitIndexes = new int[0];
    private int[] parameterIndexes = new int[0];
    private Gate[] gatePool = new Gate[0];
    private ParameterExpression[] parameterPool = new ParameterExpression[0];
    private Qubit[] qubitPool = new Qubit[0];
    private ClassicalBit[] classicalBitPool = new ClassicalBit[0];
    private int size;
    private int qubitIndexSize;
    private int parameterIndexSize;
    private int gatePoolSize;
    private int parameterPoolSize;
    private int qubitPoolSize;
    private int classicalBitPoolSize;

    public int size() {
        return size;
    }

    public int compactSize() {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (kinds[i] != OPERATION_FALLBACK) {
                count++;
            }
        }
        return count;
    }

    public void ensureCapacity(final int minimumCapacity) {
        if (minimumCapacity < 0) {
            throw new IllegalArgumentException("Operation capacity must not be negative.");
        }
        ensureOperationCapacity(minimumCapacity);
    }

    public void add(final Operation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation must not be null.");
        }
        if (operation instanceof GateOperation gateOperation && canCompact(gateOperation)) {
            addGate(gateOperation);
        } else if (operation instanceof MeasureOperation measureOperation && measureOperation.qubitReference().isStatic()) {
            addMeasure(measureOperation);
        } else if (operation instanceof ResetOperation resetOperation && resetOperation.qubitReference().isStatic()) {
            addReset(resetOperation);
        } else if (operation instanceof BarrierOperation barrierOperation) {
            addBarrier(barrierOperation);
        } else {
            addFallback(operation);
        }
    }

    public Operation get(final int index) {
        validateIndex(index);
        return switch (kinds[index]) {
            case OPERATION_GATE -> gateOperation(index);
            case OPERATION_MEASURE -> new MeasureOperation(
                qubitPool[qubitIndexes[qubitStarts[index]]],
                classicalBitPool[classicalBitIndexes[index]]
            );
            case OPERATION_RESET -> new ResetOperation(qubitPool[qubitIndexes[qubitStarts[index]]]);
            case OPERATION_BARRIER -> new BarrierOperation(qubits(index));
            case OPERATION_FALLBACK -> fallbackOperations[index];
            default -> throw new IllegalStateException("Unknown compact operation kind.");
        };
    }

    public OperationMetadata metadata(final int index) {
        validateIndex(index);
        final OperationMetadata value = metadata[index];
        if (value == null) {
            return OperationMetadata.empty();
        }
        return value;
    }

    public void setMetadata(
        final int index,
        final OperationMetadata value
    ) {
        validateIndex(index);
        if (value == null) {
            throw new IllegalArgumentException("Operation metadata must not be null.");
        }
        metadata[index] = value;
    }

    public List<Operation> operations() {
        final ArrayList<Operation> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(get(i));
        }
        return List.copyOf(result);
    }

    private void addGate(final GateOperation operation) {
        final int index = addHeader(OPERATION_GATE);
        gateIndexes[index] = gatePoolIndex(operation.gate());
        qubitStarts[index] = qubitIndexSize;
        qubitCounts[index] = operation.qubitCount();
        parameterStarts[index] = parameterIndexSize;
        parameterCounts[index] = operation.parameterCount();
        ensureQubitIndexCapacity(qubitIndexSize + operation.qubitCount());
        for (int i = 0; i < operation.qubitCount(); i++) {
            qubitIndexes[qubitIndexSize] = qubitPoolIndex(operation.qubit(i));
            qubitIndexSize++;
        }
        ensureParameterIndexCapacity(parameterIndexSize + operation.parameterCount());
        for (int i = 0; i < operation.parameterCount(); i++) {
            parameterIndexes[parameterIndexSize] = parameterPoolIndex(operation.parameter(i));
            parameterIndexSize++;
        }
    }

    private void addMeasure(final MeasureOperation operation) {
        final int index = addHeader(OPERATION_MEASURE);
        qubitStarts[index] = qubitIndexSize;
        qubitCounts[index] = 1;
        ensureQubitIndexCapacity(qubitIndexSize + 1);
        qubitIndexes[qubitIndexSize] = qubitPoolIndex(operation.qubit());
        qubitIndexSize++;
        classicalBitIndexes[index] = classicalBitPoolIndex(operation.bit());
    }

    private void addReset(final ResetOperation operation) {
        final int index = addHeader(OPERATION_RESET);
        qubitStarts[index] = qubitIndexSize;
        qubitCounts[index] = 1;
        ensureQubitIndexCapacity(qubitIndexSize + 1);
        qubitIndexes[qubitIndexSize] = qubitPoolIndex(operation.qubit());
        qubitIndexSize++;
    }

    private void addBarrier(final BarrierOperation operation) {
        final int index = addHeader(OPERATION_BARRIER);
        qubitStarts[index] = qubitIndexSize;
        qubitCounts[index] = operation.qubitCount();
        ensureQubitIndexCapacity(qubitIndexSize + operation.qubitCount());
        for (int i = 0; i < operation.qubitCount(); i++) {
            qubitIndexes[qubitIndexSize] = qubitPoolIndex(operation.qubit(i));
            qubitIndexSize++;
        }
    }

    private void addFallback(final Operation operation) {
        final int index = addHeader(OPERATION_FALLBACK);
        fallbackOperations[index] = operation;
    }

    private int addHeader(final byte kind) {
        ensureOperationCapacity(size + 1);
        final int index = size;
        kinds[index] = kind;
        gateIndexes[index] = -1;
        qubitStarts[index] = qubitIndexSize;
        qubitCounts[index] = 0;
        parameterStarts[index] = parameterIndexSize;
        parameterCounts[index] = 0;
        classicalBitIndexes[index] = -1;
        fallbackOperations[index] = null;
        metadata[index] = null;
        size++;
        return index;
    }

    private GateOperation gateOperation(final int index) {
        return GateOperation.parameterized(
            gatePool[gateIndexes[index]],
            parameters(index),
            qubits(index)
        );
    }

    private Qubit[] qubits(final int operationIndex) {
        final int count = qubitCounts[operationIndex];
        final Qubit[] result = new Qubit[count];
        final int start = qubitStarts[operationIndex];
        for (int i = 0; i < count; i++) {
            result[i] = qubitPool[qubitIndexes[start + i]];
        }
        return result;
    }

    private ParameterExpression[] parameters(final int operationIndex) {
        final int count = parameterCounts[operationIndex];
        final ParameterExpression[] result = new ParameterExpression[count];
        final int start = parameterStarts[operationIndex];
        for (int i = 0; i < count; i++) {
            result[i] = parameterPool[parameterIndexes[start + i]];
        }
        return result;
    }

    private int gatePoolIndex(final Gate gate) {
        final Integer existing = gatePoolIndexes.get(gate);
        if (existing != null) {
            return existing;
        }
        ensureGatePoolCapacity(gatePoolSize + 1);
        final int index = gatePoolSize;
        gatePoolIndexes.put(
            gate,
            index
        );
        gatePool[index] = gate;
        gatePoolSize++;
        return index;
    }

    private int parameterPoolIndex(final ParameterExpression parameter) {
        final Integer existing = parameterPoolIndexes.get(parameter);
        if (existing != null) {
            return existing;
        }
        ensureParameterPoolCapacity(parameterPoolSize + 1);
        final int index = parameterPoolSize;
        parameterPoolIndexes.put(
            parameter,
            index
        );
        parameterPool[index] = parameter;
        parameterPoolSize++;
        return index;
    }

    private int qubitPoolIndex(final Qubit qubit) {
        final Integer existing = qubitPoolIndexes.get(qubit);
        if (existing != null) {
            return existing;
        }
        ensureQubitPoolCapacity(qubitPoolSize + 1);
        final int index = qubitPoolSize;
        qubitPoolIndexes.put(
            qubit,
            index
        );
        qubitPool[index] = qubit;
        qubitPoolSize++;
        return index;
    }

    private int classicalBitPoolIndex(final ClassicalBit bit) {
        final Integer existing = classicalBitPoolIndexes.get(bit);
        if (existing != null) {
            return existing;
        }
        ensureClassicalBitPoolCapacity(classicalBitPoolSize + 1);
        final int index = classicalBitPoolSize;
        classicalBitPoolIndexes.put(
            bit,
            index
        );
        classicalBitPool[index] = bit;
        classicalBitPoolSize++;
        return index;
    }

    private static boolean canCompact(final GateOperation operation) {
        for (int i = 0; i < operation.qubitCount(); i++) {
            if (!operation.qubitReference(i).isStatic()) {
                return false;
            }
        }
        return true;
    }

    private void ensureOperationCapacity(final int minimumCapacity) {
        if (minimumCapacity <= kinds.length) {
            return;
        }
        final int newCapacity = grownCapacity(
            kinds.length,
            minimumCapacity
        );
        kinds = Arrays.copyOf(
            kinds,
            newCapacity
        );
        gateIndexes = Arrays.copyOf(
            gateIndexes,
            newCapacity
        );
        qubitStarts = Arrays.copyOf(
            qubitStarts,
            newCapacity
        );
        qubitCounts = Arrays.copyOf(
            qubitCounts,
            newCapacity
        );
        parameterStarts = Arrays.copyOf(
            parameterStarts,
            newCapacity
        );
        parameterCounts = Arrays.copyOf(
            parameterCounts,
            newCapacity
        );
        classicalBitIndexes = Arrays.copyOf(
            classicalBitIndexes,
            newCapacity
        );
        fallbackOperations = Arrays.copyOf(
            fallbackOperations,
            newCapacity
        );
        metadata = Arrays.copyOf(
            metadata,
            newCapacity
        );
    }

    private void ensureQubitIndexCapacity(final int minimumCapacity) {
        if (minimumCapacity <= qubitIndexes.length) {
            return;
        }
        qubitIndexes = Arrays.copyOf(
            qubitIndexes,
            grownCapacity(
                qubitIndexes.length,
                minimumCapacity
            )
        );
    }

    private void ensureParameterIndexCapacity(final int minimumCapacity) {
        if (minimumCapacity <= parameterIndexes.length) {
            return;
        }
        parameterIndexes = Arrays.copyOf(
            parameterIndexes,
            grownCapacity(
                parameterIndexes.length,
                minimumCapacity
            )
        );
    }

    private void ensureGatePoolCapacity(final int minimumCapacity) {
        if (minimumCapacity <= gatePool.length) {
            return;
        }
        gatePool = Arrays.copyOf(
            gatePool,
            grownCapacity(
                gatePool.length,
                minimumCapacity
            )
        );
    }

    private void ensureParameterPoolCapacity(final int minimumCapacity) {
        if (minimumCapacity <= parameterPool.length) {
            return;
        }
        parameterPool = Arrays.copyOf(
            parameterPool,
            grownCapacity(
                parameterPool.length,
                minimumCapacity
            )
        );
    }

    private void ensureQubitPoolCapacity(final int minimumCapacity) {
        if (minimumCapacity <= qubitPool.length) {
            return;
        }
        qubitPool = Arrays.copyOf(
            qubitPool,
            grownCapacity(
                qubitPool.length,
                minimumCapacity
            )
        );
    }

    private void ensureClassicalBitPoolCapacity(final int minimumCapacity) {
        if (minimumCapacity <= classicalBitPool.length) {
            return;
        }
        classicalBitPool = Arrays.copyOf(
            classicalBitPool,
            grownCapacity(
                classicalBitPool.length,
                minimumCapacity
            )
        );
    }

    private static int grownCapacity(
        final int current,
        final int minimum
    ) {
        int grown = current == 0 ? 8 : current + (current >> 1);
        if (grown < minimum) {
            grown = minimum;
        }
        return grown;
    }

    private void validateIndex(final int index) {
        if (
            index < 0
            || index >= size
        ) {
            throw new IllegalArgumentException("Operation index is outside of circuit bounds.");
        }
    }
}