/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.engine;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет точную математику симулятора, а не только факт успешного запуска.
 */
final class QuantumSimulatorExactSemanticsTest {

    private static final double EPSILON = 1.0e-10;
    private static final double ONE_OVER_SQRT_TWO = 1.0 / Math.sqrt(2.0);

    @Test
    void simulatesEverySingleQubitStandardGateWithExpectedAmplitudes() {
        assertState(
            simulateSingle(StandardGate.ID),
            amplitude("0", 1.0, 0.0)
        );
        assertState(
            simulateSingle(StandardGate.H),
            amplitude("0", ONE_OVER_SQRT_TWO, 0.0),
            amplitude("1", ONE_OVER_SQRT_TWO, 0.0)
        );
        assertState(
            simulateSingle(StandardGate.X),
            amplitude("1", 1.0, 0.0)
        );
        assertState(
            simulateSingle(StandardGate.Y),
            amplitude("1", 0.0, 1.0)
        );
        assertState(
            simulateAfterPreparation(StandardGate.Z),
            amplitude("0", ONE_OVER_SQRT_TWO, 0.0),
            amplitude("1", -ONE_OVER_SQRT_TWO, 0.0)
        );
        assertState(
            simulateAfterBitFlip(StandardGate.S),
            amplitude("1", 0.0, 1.0)
        );
        assertState(
            simulateAfterBitFlip(StandardGate.SDG),
            amplitude("1", 0.0, -1.0)
        );
        assertState(
            simulateAfterBitFlip(StandardGate.T),
            amplitude("1", ONE_OVER_SQRT_TWO, ONE_OVER_SQRT_TWO)
        );
        assertState(
            simulateAfterBitFlip(StandardGate.TDG),
            amplitude("1", ONE_OVER_SQRT_TWO, -ONE_OVER_SQRT_TWO)
        );
    }

    @Test
    void simulatesParameterizedSingleQubitGatesWithExpectedAmplitudes() {
        assertState(
            simulateSingle(
                StandardGate.RX,
                Math.PI
            ),
            amplitude("1", 0.0, -1.0)
        );
        assertState(
            simulateSingle(
                StandardGate.RY,
                Math.PI
            ),
            amplitude("1", 1.0, 0.0)
        );
        assertState(
            simulateSingle(
                StandardGate.RZ,
                Math.PI
            ),
            amplitude("0", 0.0, -1.0)
        );
        assertState(
            simulateAfterBitFlip(
                StandardGate.PHASE,
                Math.PI / 3.0
            ),
            amplitude(
                "1",
                0.5,
                Math.sqrt(3.0) / 2.0
            )
        );
        assertState(
            simulateSingle(
                StandardGate.U,
                Math.PI,
                0.0,
                Math.PI
            ),
            amplitude("1", 1.0, 0.0)
        );
    }

    @Test
    void simulatesControlledAndMultiQubitStandardGatesWithExpectedAmplitudes() {
        assertState(
            simulateControlled(StandardGate.CX),
            amplitude("11", 1.0, 0.0)
        );
        assertState(
            simulateControlled(StandardGate.CY),
            amplitude("11", 0.0, 1.0)
        );
        assertState(
            simulateControlledPhase(StandardGate.CZ),
            amplitude("00", 0.5, 0.0),
            amplitude("01", 0.5, 0.0),
            amplitude("10", 0.5, 0.0),
            amplitude("11", -0.5, 0.0)
        );
        assertState(
            simulateControlledPhase(
                StandardGate.CPHASE,
                Math.PI / 2.0
            ),
            amplitude("00", 0.5, 0.0),
            amplitude("01", 0.5, 0.0),
            amplitude("10", 0.5, 0.0),
            amplitude("11", 0.0, 0.5)
        );
        assertState(
            simulateControlled(StandardGate.CH),
            amplitude("01", ONE_OVER_SQRT_TWO, 0.0),
            amplitude("11", ONE_OVER_SQRT_TWO, 0.0)
        );
        assertState(
            simulateSwap(),
            amplitude("10", 1.0, 0.0)
        );
        assertState(
            simulateToffoli(),
            amplitude("111", 1.0, 0.0)
        );
    }

    @Test
    void laterShotDiagnosticsAreNotSilentlyLost() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("late_shot_error");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.h(q.get(0))
            .measure(
                q.get(0),
                c.get(0)
            )
            .conditionalBlock(
                ClassicalPredicate.compare(
                    ClassicalExpression.bit(c.get(0)),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(1)
                ),
                OperationBlock.of(new GateOperation(
                    StandardGate.X,
                    new QuantumReference[] {
                        QuantumReference.dynamicIndex(
                            q,
                            ClassicalExpression.integer(5L)
                        )
                    },
                    new ParameterExpression[0]
                )),
                null
            );

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(32)
                .seed(1L)
                .build()
        );

        assertFalse(result.isSuccess());
        boolean foundExpectedDiagnostic = false;
        for (int i = 0; i < result.diagnostics().size(); i++) {
            if (result.diagnostics().get(i).code() == SimulationDiagnosticCode.NON_STATIC_QUBIT_REFERENCE) {
                foundExpectedDiagnostic = true;
                break;
            }
        }
        assertTrue(foundExpectedDiagnostic);
    }

    private static SimulationResult simulateSingle(
        final StandardGate gate,
        final double... parameters
    ) {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("single_" + gate.gateName());
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.parameterizedGate(
            gate,
            parameterExpressions(parameters),
            q.get(0)
        );
        return simulateState(program);
    }

    private static SimulationResult simulateAfterPreparation(final StandardGate gate) {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("prepared_" + gate.gateName());
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0));
        circuit.parameterizedGate(
            gate,
            new ParameterExpression[0],
            q.get(0)
        );
        return simulateState(program);
    }

    private static SimulationResult simulateAfterBitFlip(
        final StandardGate gate,
        final double... parameters
    ) {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("flipped_" + gate.gateName());
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.x(q.get(0));
        circuit.parameterizedGate(
            gate,
            parameterExpressions(parameters),
            q.get(0)
        );
        return simulateState(program);
    }

    private static SimulationResult simulateControlled(final StandardGate gate) {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("controlled_" + gate.gateName());
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        circuit.x(q.get(0));
        circuit.parameterizedGate(
            gate,
            new ParameterExpression[0],
            q.get(0),
            q.get(1)
        );
        return simulateState(program);
    }

    private static SimulationResult simulateControlledPhase(
        final StandardGate gate,
        final double... parameters
    ) {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("controlled_phase_" + gate.gateName());
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        circuit.h(q.get(0))
            .h(q.get(1));
        circuit.parameterizedGate(
            gate,
            parameterExpressions(parameters),
            q.get(0),
            q.get(1)
        );
        return simulateState(program);
    }

    private static SimulationResult simulateSwap() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("swap");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        circuit.x(q.get(0))
            .swap(
                q.get(0),
                q.get(1)
            );
        return simulateState(program);
    }

    private static SimulationResult simulateToffoli() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("toffoli");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        circuit.x(q.get(0))
            .x(q.get(1))
            .ccx(
                q.get(0),
                q.get(1),
                q.get(2)
            );
        return simulateState(program);
    }

    private static SimulationResult simulateState(final QuantumProgram program) {
        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(0)
                .build()
        );
        assertTrue(result.isSuccess());
        return result;
    }

    private static ParameterExpression[] parameterExpressions(final double... values) {
        final ParameterExpression[] expressions = new ParameterExpression[values.length];
        for (int i = 0; i < values.length; i++) {
            expressions[i] = ParameterExpression.of(values[i]);
        }
        return expressions;
    }

    private static ExpectedAmplitude amplitude(
        final String basisState,
        final double real,
        final double imaginary
    ) {
        return new ExpectedAmplitude(
            basisState,
            real,
            imaginary
        );
    }

    private static void assertState(
        final SimulationResult result,
        final ExpectedAmplitude... expected
    ) {
        final HashMap<String, ExpectedAmplitude> expectedByBasis = new HashMap<>();
        for (int i = 0; i < expected.length; i++) {
            expectedByBasis.put(
                expected[i].basisState,
                expected[i]
            );
        }
        double norm = 0.0;
        for (final StateVectorAmplitude actual : result.stateVector()) {
            final ExpectedAmplitude expectedAmplitude = expectedByBasis.get(actual.basisState());
            final double expectedReal = expectedAmplitude == null ? 0.0 : expectedAmplitude.real;
            final double expectedImaginary = expectedAmplitude == null ? 0.0 : expectedAmplitude.imaginary;
            assertEquals(
                expectedReal,
                actual.real(),
                EPSILON,
                actual.basisState() + " real"
            );
            assertEquals(
                expectedImaginary,
                actual.imaginary(),
                EPSILON,
                actual.basisState() + " imaginary"
            );
            norm += actual.real() * actual.real() + actual.imaginary() * actual.imaginary();
        }
        assertEquals(
            1.0,
            norm,
            EPSILON
        );
    }

    private record ExpectedAmplitude(
        String basisState,
        double real,
        double imaginary
    ) {
    }
}