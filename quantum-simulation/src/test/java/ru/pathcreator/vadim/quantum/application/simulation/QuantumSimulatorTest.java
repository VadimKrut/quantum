/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.engine;

import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnosticSeverity;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationUnsupportedOperationPolicy;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;
import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumSimulatorTest {

    @Test
    void simulatesBellCountsWithDeterministicSeed() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bell");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .measure(
                q.get(0),
                c.get(0)
            )
            .measure(
                q.get(1),
                c.get(1)
            );

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(512)
                .seed(12L)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            2,
            result.qubitCount()
        );
        assertEquals(
            512L,
            result.counts().get("00") + result.counts().get("11")
        );
        assertEquals(
            2,
            result.counts().size()
        );
        assertEquals(
            result.counts(),
            new QuantumSimulator().simulate(
                program,
                SimulationOptions.builder()
                    .shots(512)
                    .seed(12L)
                    .build()
            ).counts()
        );
    }

    @Test
    void simulatesGhzThreeQubitEntanglement() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("ghz");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            3
        );
        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .cx(
                q.get(1),
                q.get(2)
            );
        for (int i = 0; i < 3; i++) {
            circuit.measure(
                q.get(i),
                c.get(i)
            );
        }

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(600)
                .seed(5L)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            2,
            result.counts().size()
        );
        assertTrue(result.counts().containsKey("000"));
        assertTrue(result.counts().containsKey("111"));
    }

    @Test
    void evaluatesParameterizedRotationsAndStateVector() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("rotation");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.rx(
            ParameterExpression.named("theta"),
            q.get(0)
        );

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(0)
                .parameterBindings(ParameterBindings.builder()
                    .put(
                        "theta",
                        Math.PI
                    )
                    .build())
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            0.0,
            result.stateVector().get(0).real(),
            1.0e-10
        );
        assertEquals(
            -1.0,
            result.stateVector().get(1).imaginary(),
            1.0e-10
        );
    }

    @Test
    void resetsBasisStateWithoutMixedStateApproximation() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("reset");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.x(q.get(0))
            .reset(q.get(0));

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(0)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            1.0,
            result.stateVector().get(0).real(),
            1.0e-10
        );
        assertEquals(
            0.0,
            result.stateVector().get(1).real(),
            1.0e-10
        );
    }

    @Test
    void resetsSuperpositionByMeasurementCollapse() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("reset_superposition");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0))
            .reset(q.get(0));

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(128)
                .seed(9L)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            128L,
            result.counts().get("0")
        );
        assertEquals(
            1.0,
            result.stateVector().get(0).real(),
            1.0e-10
        );
    }

    @Test
    void measurementFeedsLaterClassicalControlWithinSameShot() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("measure_then_control");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.x(q.get(0))
            .measure(
                q.get(0),
                c.get(0)
            )
            .classicallyControlled(
                ClassicalPredicate.compare(
                    ClassicalExpression.bit(c.get(0)),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(1)
                ),
                GateOperation.of(
                    StandardGate.X,
                    q.get(1)
                )
            )
            .measure(
                q.get(1),
                c.get(1)
            );

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(64)
                .seed(1L)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("11")
        );
    }

    @Test
    void supportsClassicalAssignmentAndClassicalControl() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("classical_control");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        final ClassicalBit bit = c.get(0);
        circuit.assign(new ClassicalAssignment(
            ClassicalExpression.bit(bit),
            ClassicalExpression.integer(1)
        ));
        circuit.classicallyControlled(
            ClassicalPredicate.compare(
                ClassicalExpression.register(c),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(1)
            ),
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            )
        );

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(64)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("1")
        );
    }

    @Test
    void executesBlockOperationsInOrder() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("block");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.block(OperationBlock.of(
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            ),
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            )
        ));

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(0)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            1.0,
            result.stateVector().get(0).real(),
            1.0e-10
        );
    }

    @Test
    void reportsUnsupportedOperationsAsDiagnostics() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("unsupported");
        circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.branch(BranchOperation.always("end"));

        final SimulationResult result = new QuantumSimulator().simulate(program);

        assertFalse(result.isSuccess());
        assertEquals(
            SimulationDiagnosticCode.UNSUPPORTED_OPERATION,
            result.diagnostics().get(0).code()
        );
    }

    @Test
    void canSkipUnsupportedOperationsWhenExplicitlyRequested() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("skip_unsupported");
        circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.branch(BranchOperation.always("end"));

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .unsupportedOperationPolicy(SimulationUnsupportedOperationPolicy.DIAGNOSTIC_SKIP)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            SimulationDiagnosticSeverity.WARNING,
            result.diagnostics().get(0).severity()
        );
    }

    @Test
    void reportsTooManyQubitsBeforeAllocatingStateVector() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("too_large");
        circuit.createQuantumRegister(
            "q",
            5
        );

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .maxQubits(4)
                .build()
        );

        assertFalse(result.isSuccess());
        assertEquals(
            SimulationDiagnosticCode.TOO_MANY_QUBITS,
            result.diagnostics().get(0).code()
        );
    }
}