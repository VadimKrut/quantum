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
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableGateOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableMeasureOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateMatrix;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumSimulatorInvariantTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void everySupportedStandardGatePreservesStateNorm() {
        for (final StandardGate gate : StandardGate.values()) {
            final QuantumProgram program = singleGateProgram(gate);

            final SimulationResult result = new QuantumSimulator().simulate(
                program,
                SimulationOptions.builder()
                    .shots(0)
                    .build()
            );

            assertTrue(
                result.isSuccess(),
                gate.gateName()
            );
            assertEquals(
                1.0,
                norm(result.stateVector()),
                EPSILON,
                gate.gateName()
            );
        }
    }

    @Test
    void inverseModifiedSingleQubitGatesRoundTripToInitialState() {
        final StandardGate[] gates = new StandardGate[] {
            StandardGate.H,
            StandardGate.X,
            StandardGate.Y,
            StandardGate.Z,
            StandardGate.S,
            StandardGate.SDG,
            StandardGate.T,
            StandardGate.TDG,
            StandardGate.RX,
            StandardGate.RY,
            StandardGate.RZ,
            StandardGate.PHASE,
            StandardGate.U
        };
        for (final StandardGate gate : gates) {
            final QuantumProgram program = QuantumProgram.gateBased();
            final QuantumCircuit circuit = program.createCircuit("inverse_" + gate.gateName());
            final QuantumRegister q = circuit.createQuantumRegister(
                "q",
                1
            );
            circuit.h(q.get(0));
            appendGate(
                circuit,
                gate,
                q.get(0)
            );
            appendGate(
                circuit,
                ModifiedGate.of(
                    gate,
                    List.of(GateModifier.inverse())
                ),
                parameters(gate),
                q.get(0)
            );

            final SimulationResult result = new QuantumSimulator().simulate(
                program,
                SimulationOptions.builder()
                    .shots(0)
                    .build()
            );

            assertTrue(
                result.isSuccess(),
                gate.gateName()
            );
            assertStateEquals(
                new QuantumSimulator().simulate(preparedOneQubit()).stateVector(),
                result.stateVector(),
                gate.gateName()
            );
        }
    }

    @Test
    void controlledModifiedGateUsesLeadingControlQubits() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("controlled_modified");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.x(q.get(0));
        circuit.parameterizedGate(
            ModifiedGate.of(
                StandardGate.X,
                List.of(GateModifier.controlled(1))
            ),
            new ParameterExpression[0],
            q.get(0),
            q.get(1)
        );
        circuit.measure(
            q.get(0),
            c.get(0)
        );
        circuit.measure(
            q.get(1),
            c.get(1)
        );

        final SimulationResult result = new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(128)
                .seed(1L)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            128L,
            result.counts().get("11")
        );
    }

    @Test
    void repeatModifiedGateAppliesBaseGateMultipleTimes() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("repeat_modified");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.parameterizedGate(
            ModifiedGate.of(
                StandardGate.X,
                List.of(GateModifier.repeat(2))
            ),
            new ParameterExpression[0],
            q.get(0)
        );

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
            EPSILON
        );
    }

    @Test
    void branchJumpsToLabelWithProgramCounterSemantics() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("branch");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.x(q.get(0));
        circuit.branch(BranchOperation.always("done"));
        circuit.x(q.get(0));
        circuit.label("done");
        circuit.measure(
            q.get(0),
            c.get(0)
        );

        final SimulationResult result = simulateCounts(program);

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("1")
        );
    }

    @Test
    void symbolicForLoopPublishesIndexForDynamicQuantumReference() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("symbolic_loop_dynamic_ref");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            3
        );
        circuit.symbolicForLoop(new SymbolicForLoopOperation(
            "i",
            null,
            ClassicalExpression.integer(0L),
            ClassicalExpression.integer(1L),
            ClassicalExpression.integer(2L),
            OperationBlock.of(new GateOperation(
                StandardGate.X,
                new QuantumReference[] {
                    QuantumReference.dynamicIndex(
                        q,
                        ClassicalExpression.variable("i")
                    )
                },
                new ParameterExpression[0]
            ))
        ));
        circuit.measure(
            q.get(0),
            c.get(0)
        );
        circuit.measure(
            q.get(1),
            c.get(1)
        );
        circuit.measure(
            q.get(2),
            c.get(2)
        );

        final SimulationResult result = simulateCounts(program);

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("111")
        );
    }

    @Test
    void classicalArrayInitializerAndSymbolicReferenceDriveExecution() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("array_symbolic");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        circuit.classicalArrayDeclaration(new ClassicalArrayDeclarationOperation(
            "address",
            ClassicalType.sized(
                ClassicalTypeKind.UNSIGNED_INTEGER,
                32
            ),
            List.of(ClassicalExpression.integer(2L)),
            "{0,1}"
        ));
        circuit.block(OperationBlock.of(new GateOperation(
            StandardGate.X,
            new QuantumReference[] {
                QuantumReference.dynamicIndex(
                    q,
                    ClassicalExpression.symbolicReference("address[1]")
                )
            },
            new ParameterExpression[0]
        )));
        circuit.assign(new ClassicalAssignment(
            ClassicalExpression.symbolicReference("address[0]"),
            ClassicalExpression.integer(1L)
        ));
        circuit.block(OperationBlock.of(new GateOperation(
            StandardGate.X,
            new QuantumReference[] {
                QuantumReference.dynamicIndex(
                    q,
                    ClassicalExpression.symbolicReference("address[0]")
                )
            },
            new ParameterExpression[0]
        )));
        circuit.measure(
            q.get(0),
            c.get(0)
        );
        circuit.measure(
            q.get(1),
            c.get(1)
        );

        final SimulationResult result = simulateCounts(program);

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("00")
        );
    }

    @Test
    void callableDefinitionExecutesBoundBody() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addCallableDefinition(new CallableDefinition(
            "flip_and_measure",
            CallableOperationBlock.of(
                CallableGateOperation.of(
                    StandardGate.X,
                    "target"
                ),
                new CallableMeasureOperation(
                    "target",
                    "out"
                )
            ),
            CallableArgument.qubit("target"),
            CallableArgument.classical(
                "out",
                ClassicalType.of(ClassicalTypeKind.BIT)
            )
        ));
        final QuantumCircuit circuit = program.createCircuit("callable");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.callableInvocation(new CallableInvocationOperation(
            "flip_and_measure",
            null,
            List.of(ClassicalExpression.bit(c.get(0))),
            List.of(QuantumReference.staticQubit(q.get(0)))
        ));

        final SimulationResult result = simulateCounts(program);

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("1")
        );
    }

    @Test
    void integerPowerModifierExecutesWithoutDecomposition() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("integer_power");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.parameterizedGate(
            ModifiedGate.of(
                StandardGate.X,
                List.of(GateModifier.power(3.0))
            ),
            new ParameterExpression[0],
            q.get(0)
        );
        circuit.measure(
            q.get(0),
            c.get(0)
        );

        final SimulationResult result = simulateCounts(program);

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("1")
        );
    }

    @Test
    void compositeGateDefinitionExecutesBodyOperations() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition flipTwiceThenFlip = GateDefinition.composite(
            "flip_body",
            List.of(),
            List.of("target"),
            List.of(
                GateBodyOperation.of(
                    StandardGate.X,
                    new ParameterExpression[0],
                    "target"
                ),
                GateBodyOperation.of(
                    StandardGate.X,
                    new ParameterExpression[0],
                    "target"
                ),
                GateBodyOperation.of(
                    StandardGate.X,
                    new ParameterExpression[0],
                    "target"
                )
            )
        );
        program.addGateDefinition(flipTwiceThenFlip);
        final QuantumCircuit circuit = program.createCircuit("composite_gate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.parameterizedGate(
            flipTwiceThenFlip,
            new ParameterExpression[0],
            q.get(0)
        );
        circuit.measure(
            q.get(0),
            c.get(0)
        );

        final SimulationResult result = simulateCounts(program);

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("1")
        );
    }

    @Test
    void matrixGateDefinitionExecutesNumericMatrix() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition matrixX = GateDefinition.matrix(
            "matrix_x",
            List.of(),
            List.of("target"),
            GateMatrix.of(new String[][] {
                new String[] {"0", "1"},
                new String[] {"1", "0"}
            })
        );
        program.addGateDefinition(matrixX);
        final QuantumCircuit circuit = program.createCircuit("matrix_gate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.parameterizedGate(
            matrixX,
            new ParameterExpression[0],
            q.get(0)
        );
        circuit.measure(
            q.get(0),
            c.get(0)
        );

        final SimulationResult result = simulateCounts(program);

        assertTrue(result.isSuccess());
        assertEquals(
            64L,
            result.counts().get("1")
        );
    }

    private static QuantumProgram singleGateProgram(final StandardGate gate) {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("gate_" + gate.gateName());
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            Math.max(
                gate.arity(),
                1
            )
        );
        circuit.h(q.get(0));
        appendGate(
            circuit,
            gate,
            qubits(
                q,
                gate.arity()
            )
        );
        return program;
    }

    private static SimulationResult simulateCounts(final QuantumProgram program) {
        return new QuantumSimulator().simulate(
            program,
            SimulationOptions.builder()
                .shots(64)
                .seed(1L)
                .build()
        );
    }

    private static QuantumProgram preparedOneQubit() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("prepared");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0));
        return program;
    }

    private static void appendGate(
        final QuantumCircuit circuit,
        final StandardGate gate,
        final Qubit... qubits
    ) {
        appendGate(
            circuit,
            gate,
            parameters(gate),
            qubits
        );
    }

    private static void appendGate(
        final QuantumCircuit circuit,
        final ru.pathcreator.vadim.quantum.domain.gate.Gate gate,
        final ParameterExpression[] parameters,
        final Qubit... qubits
    ) {
        circuit.parameterizedGate(
            gate,
            parameters,
            qubits
        );
    }

    private static ParameterExpression[] parameters(final StandardGate gate) {
        if (gate == StandardGate.RX || gate == StandardGate.RY || gate == StandardGate.RZ || gate == StandardGate.PHASE || gate == StandardGate.CPHASE) {
            return new ParameterExpression[] {ParameterExpression.of(Math.PI / 3.0)};
        }
        if (gate == StandardGate.U) {
            return new ParameterExpression[] {
                ParameterExpression.of(Math.PI / 5.0),
                ParameterExpression.of(Math.PI / 7.0),
                ParameterExpression.of(Math.PI / 11.0)
            };
        }
        return new ParameterExpression[0];
    }

    private static Qubit[] qubits(
        final QuantumRegister register,
        final int count
    ) {
        final Qubit[] qubits = new Qubit[count];
        for (int i = 0; i < count; i++) {
            qubits[i] = register.get(i);
        }
        return qubits;
    }

    private static double norm(final List<StateVectorAmplitude> stateVector) {
        double norm = 0.0;
        for (int i = 0; i < stateVector.size(); i++) {
            norm += stateVector.get(i).real() * stateVector.get(i).real()
                + stateVector.get(i).imaginary() * stateVector.get(i).imaginary();
        }
        return norm;
    }

    private static void assertStateEquals(
        final List<StateVectorAmplitude> expected,
        final List<StateVectorAmplitude> actual,
        final String label
    ) {
        assertEquals(
            expected.size(),
            actual.size(),
            label
        );
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(
                expected.get(i).real(),
                actual.get(i).real(),
                EPSILON,
                label + " real " + i
            );
            assertEquals(
                expected.get(i).imaginary(),
                actual.get(i).imaginary(),
                EPSILON,
                label + " imaginary " + i
            );
        }
    }
}