/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import java.util.List;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrFileWriteResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableGateOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateMatrix;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.metadata.ExternalSource;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.metadata.SourceLocation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumTest {

    @TempDir
    private Path tempDir;

    @Test
    void exposesStableFacadeForCoreWorkflow() {
        final QuantumProgram program = Quantum.gateBasedProgram();
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
            );

        assertTrue(Quantum.validate(program).isValid());
        assertEquals(
            CapabilityPreflightStatus.EXPORTABLE,
            Quantum.preflight(
                IntegrationFormat.OPENQASM_3,
                program
            ).status()
        );

        final String json = Quantum.writeJson(program).content();
        final QuantumProgram restored = Quantum.readJson(json).program();

        assertTrue(Quantum.exportOpenQasm2(restored).isSuccess());
        assertTrue(Quantum.exportOpenQasm3(restored).isSuccess());
    }

    @Test
    void readsJsonFileThroughPublicStreamingPath() {
        final QuantumProgram program = Quantum.gateBasedProgram();
        final QuantumCircuit circuit = program.createCircuit("file_api");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0));
        final Path path = tempDir.resolve("program.quantum.json");

        final QuantumIrFileWriteResult write = Quantum.writeJsonStreaming(
            path,
            program
        );
        final QuantumIrReadResult read = Quantum.readJson(path);

        assertTrue(write.isSuccess());
        assertTrue(read.isSuccess());
        assertEquals(
            1,
            read.program().circuit(0).operationCount()
        );
    }

    @Test
    void buildsGateBasedProgramThroughDsl() {
        final QuantumProgram program = Quantum.programBuilder()
            .circuit("dsl")
            .qreg(
                "q",
                3
            )
            .creg(
                "c",
                2
            )
            .h("q[0]")
            .cx(
                "q[0]",
                "q[1]"
            )
            .rz(
                ParameterExpression.divide(
                    ParameterExpression.pi(),
                    ParameterExpression.of(2.0)
                ),
                "q[1]"
            )
            .ccx(
                "q[0]",
                "q[1]",
                "q[2]"
            )
            .measure(
                "q[0]",
                "c[0]"
            )
            .measure(
                "q[1]",
                "c[1]"
            )
            .build();

        assertTrue(Quantum.validate(program).isValid());
        assertEquals(
            1,
            program.circuitCount()
        );
        assertEquals(
            6,
            program.circuit(0).operationCount()
        );
        assertEquals(
            OperationKind.MEASURE,
            program.circuit(0).operation(5).kind()
        );
        assertTrue(Quantum.exportOpenQasm3(program).isSuccess());
    }

    @Test
    void rejectsBadDslReferenceBeforeExport() {
        final QuantumCircuitBuilder circuit = Quantum.programBuilder()
            .circuit("bad_reference")
            .qreg(
                "q",
                1
            );

        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.h("missing[0]")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.h("q[2147483648]")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> circuit.h("q")
        );
    }

    @Test
    void dslCoversStructuredTimingClassicalAndMetadataIrSurface() {
        final QuantumProgram program = Quantum.programBuilder()
            .intrinsicGate(
                "intrinsic_custom",
                1,
                0
            )
            .opaqueGate(
                "opaque_custom",
                List.of("theta"),
                List.of("q")
            )
            .compositeGate(
                "composite_custom",
                List.of(),
                List.of("q"),
                List.of(GateBodyOperation.of(
                    StandardGate.H,
                    new ParameterExpression[0],
                    "q"
                ))
            )
            .matrixGate(
                "matrix_custom",
                List.of(),
                List.of("q"),
                GateMatrix.of(new String[][] {
                    new String[] {
                        "1",
                        "0"
                    },
                    new String[] {
                        "0",
                        "1"
                    }
                })
            )
            .classicalDeclaration(new ClassicalDeclaration(
                "global_flag",
                ClassicalType.of(ClassicalTypeKind.BOOLEAN)
            ))
            .callableDefinition(
                "prepare",
                CallableOperationBlock.of(CallableGateOperation.of(
                    StandardGate.H,
                    "q"
                )),
                CallableArgument.qubit("q")
            )
            .externalCallableDeclaration(
                "cost",
                ClassicalType.sized(
                    ClassicalTypeKind.FLOAT,
                    64
                ),
                CallableArgument.classical(
                    "theta",
                    ClassicalType.sized(
                        ClassicalTypeKind.ANGLE,
                        64
                    )
                )
            )
            .calibrationDefinition(
                "intrinsic_custom",
                List.of(),
                List.of("q"),
                "pulse",
                "play q"
            )
            .circuit("full_surface")
            .qreg(
                "q",
                2
            )
            .creg(
                "c",
                2
            )
            .gateReferences(
                StandardGate.CX,
                QuantumReference.hardwareQubit(0),
                QuantumReference.hardwareQubit(1)
            )
            .measureReference(
                QuantumReference.hardwareQubit(0),
                "c[0]"
            )
            .assign(new ClassicalAssignment(
                ClassicalExpression.symbolicReference("temporary"),
                ClassicalExpression.integer(1)
            ))
            .classicallyControlled(
                ClassicalPredicate.compare(
                    ClassicalExpression.integer(1),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(1)
                ),
                GateOperation.ofReferences(
                    StandardGate.H,
                    QuantumReference.hardwareQubit(1)
                )
            )
            .block(OperationBlock.of())
            .conditionalBlock(
                ClassicalPredicate.compare(
                    ClassicalExpression.integer(1),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(1)
                ),
                OperationBlock.of(),
                OperationBlock.of()
            )
            .forLoop(
                "i",
                0,
                1,
                2,
                OperationBlock.of()
            )
            .delayReferences(
                DurationExpression.duration(
                    10,
                    DurationUnit.NS
                ),
                QuantumReference.hardwareQubit(0)
            )
            .timingBox(
                DurationExpression.stretch("stretch"),
                OperationBlock.of()
            )
            .label("after")
            .branch(BranchOperation.always("after"))
            .halt()
            .waitInstruction()
            .setOperationMetadata(
                0,
                new OperationMetadata(
                    new ExternalSource(
                        "dsl-test",
                        "full surface"
                    ),
                    new SourceLocation(
                        1,
                        1
                    )
                )
            )
            .build();

        assertEquals(
            4,
            program.gateDefinitionCount()
        );
        assertEquals(
            1,
            program.classicalDeclarationCount()
        );
        assertEquals(
            1,
            program.callableDefinitionCount()
        );
        assertEquals(
            1,
            program.externalCallableDeclarationCount()
        );
        assertEquals(
            1,
            program.calibrationDefinitionCount()
        );
        assertEquals(
            13,
            program.circuit(0).operationCount()
        );
        assertEquals(
            OperationKind.GATE,
            program.circuit(0).operation(0).kind()
        );
        assertEquals(
            OperationKind.WAIT,
            program.circuit(0).operation(12).kind()
        );
        assertEquals(
            "dsl-test",
            program.circuit(0).operationMetadata(0).source().format()
        );
    }
}