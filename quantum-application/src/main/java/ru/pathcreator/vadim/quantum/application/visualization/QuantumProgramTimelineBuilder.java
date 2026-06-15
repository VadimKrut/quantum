/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.visualization;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.Operation;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

public final class QuantumProgramTimelineBuilder {

    public ProgramTimeline build(final QuantumProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Timeline program must not be null.");
        }
        final ArrayList<CircuitTimeline> circuits = new ArrayList<>(program.circuitCount());
        for (int i = 0; i < program.circuitCount(); i++) {
            circuits.add(circuitTimeline(program.circuit(i)));
        }
        return new ProgramTimeline(circuits);
    }

    private static CircuitTimeline circuitTimeline(final QuantumCircuit circuit) {
        final ArrayList<String> quantumWires = quantumWires(circuit);
        final ArrayList<String> classicalWires = classicalWires(circuit);
        final ArrayList<CircuitTimelineStep> steps = new ArrayList<>(circuit.operationCount());
        for (int i = 0; i < circuit.operationCount(); i++) {
            steps.add(step(
                i,
                circuit.operation(i)
            ));
        }
        return new CircuitTimeline(
            circuit.name().value(),
            quantumWires,
            classicalWires,
            steps
        );
    }

    private static CircuitTimelineStep step(
        final int operationIndex,
        final Operation operation
    ) {
        if (operation instanceof GateOperation gateOperation) {
            return gateStep(
                operationIndex,
                gateOperation
            );
        }
        if (operation instanceof MeasureOperation measureOperation) {
            return measureStep(
                operationIndex,
                measureOperation
            );
        }
        if (operation instanceof ResetOperation resetOperation) {
            return new CircuitTimelineStep(
                operationIndex,
                operation.kind(),
                "reset",
                List.of(referenceLabel(resetOperation.qubitReference())),
                List.of(),
                resetOperation.qubitReference().isStatic()
            );
        }
        if (operation instanceof BarrierOperation barrierOperation) {
            final ArrayList<String> wires = new ArrayList<>(barrierOperation.qubitCount());
            for (int i = 0; i < barrierOperation.qubitCount(); i++) {
                wires.add(qubitLabel(barrierOperation.qubit(i)));
            }
            return new CircuitTimelineStep(
                operationIndex,
                operation.kind(),
                "barrier",
                wires,
                List.of(),
                true
            );
        }
        return new CircuitTimelineStep(
            operationIndex,
            operation.kind(),
            operation.kind().name().toLowerCase(),
            List.of(),
            List.of(),
            false
        );
    }

    private static CircuitTimelineStep gateStep(
        final int operationIndex,
        final GateOperation operation
    ) {
        final ArrayList<String> wires = new ArrayList<>(operation.qubitCount());
        boolean staticPlacement = true;
        for (int i = 0; i < operation.qubitCount(); i++) {
            final QuantumReference reference = operation.qubitReference(i);
            wires.add(referenceLabel(reference));
            staticPlacement = staticPlacement && reference.isStatic();
        }
        return new CircuitTimelineStep(
            operationIndex,
            operation.kind(),
            operation.gate().gateName(),
            wires,
            List.of(),
            staticPlacement
        );
    }

    private static CircuitTimelineStep measureStep(
        final int operationIndex,
        final MeasureOperation operation
    ) {
        return new CircuitTimelineStep(
            operationIndex,
            operation.kind(),
            "measure",
            List.of(referenceLabel(operation.qubitReference())),
            List.of(classicalLabel(operation.bit())),
            operation.qubitReference().isStatic()
        );
    }

    private static ArrayList<String> quantumWires(final QuantumCircuit circuit) {
        final ArrayList<String> wires = new ArrayList<>();
        for (int registerIndex = 0; registerIndex < circuit.quantumRegisterCount(); registerIndex++) {
            final QuantumRegister register = circuit.quantumRegister(registerIndex);
            for (int bitIndex = 0; bitIndex < register.size(); bitIndex++) {
                wires.add(register.name().value() + "[" + bitIndex + "]");
            }
        }
        return wires;
    }

    private static ArrayList<String> classicalWires(final QuantumCircuit circuit) {
        final ArrayList<String> wires = new ArrayList<>();
        for (int registerIndex = 0; registerIndex < circuit.classicalRegisterCount(); registerIndex++) {
            final ClassicalRegister register = circuit.classicalRegister(registerIndex);
            for (int bitIndex = 0; bitIndex < register.size(); bitIndex++) {
                wires.add(register.name().value() + "[" + bitIndex + "]");
            }
        }
        return wires;
    }

    private static String referenceLabel(final QuantumReference reference) {
        if (reference.isStatic()) {
            return qubitLabel(reference.qubit());
        }
        return reference.kind().name().toLowerCase();
    }

    private static String qubitLabel(final Qubit qubit) {
        return qubit.register().name().value() + "[" + qubit.index() + "]";
    }

    private static String classicalLabel(final ClassicalBit bit) {
        return bit.register().name().value() + "[" + bit.index() + "]";
    }
}