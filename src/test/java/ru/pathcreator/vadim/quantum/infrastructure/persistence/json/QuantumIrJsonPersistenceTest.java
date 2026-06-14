/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.persistence.json;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.api.QuantumIrFiles;
import ru.pathcreator.vadim.quantum.application.persistence.diagnostic.PersistenceDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableArgument;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableConditionalBlockOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableDelayOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableGateOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableMeasureOperation;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalBinaryOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRule;
import ru.pathcreator.vadim.quantum.domain.gate.GateValidationRuleErrorCollector;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.metadata.ExternalSource;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.metadata.SourceLocation;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumIrJsonPersistenceTest {

    @TempDir
    private Path tempDir;

    @Test
    void writesAndReadsFullCurrentIrModelCanonically() {
        final QuantumProgram program = createRichProgram();
        final QuantumIrWriteResult firstWrite = new QuantumIrJsonWriter().write(program);

        assertTrue(firstWrite.isSuccess());
        assertTrue(firstWrite.content().contains("\"format\" : \"pathcreator.quantum-ir\""));
        assertTrue(firstWrite.content().contains("\"kind\" : \"CLASSICALLY_CONTROLLED\""));
        assertTrue(firstWrite.content().contains("\"kind\" : \"CONDITIONAL_BLOCK\""));
        assertTrue(firstWrite.content().contains("\"kind\" : \"TIMING_BOX\""));
        assertTrue(firstWrite.content().contains("\"kind\" : \"MODIFIED\""));
        assertTrue(firstWrite.content().contains("\"classicalDeclarations\""));
        assertTrue(firstWrite.content().contains("\"callableDefinitions\""));
        assertTrue(firstWrite.content().contains("\"prepare_state\""));
        assertTrue(firstWrite.content().contains("\"externalCallableDeclarations\""));
        assertTrue(firstWrite.content().contains("\"calibrationDefinitions\""));
        assertTrue(firstWrite.content().contains("\"metadata\""));

        final QuantumIrReadResult read = new QuantumIrJsonReader().read(firstWrite.content());

        assertTrue(read.isSuccess());
        assertEquals(
            3,
            read.program().gateDefinitionCount()
        );
        assertEquals(
            1,
            read.program().circuitCount()
        );
        assertEquals(
            1,
            read.program().classicalDeclarationCount()
        );
        assertEquals(
            1,
            read.program().callableDefinitionCount()
        );
        assertEquals(
            1,
            read.program().externalCallableDeclarationCount()
        );
        assertEquals(
            1,
            read.program().calibrationDefinitionCount()
        );
        assertEquals(
            16,
            read.program().circuit(0).operationCount()
        );
        assertEquals(
            OperationKind.GATE,
            read.program().circuit(0).operation(0).kind()
        );
        assertEquals(
            OperationKind.CLASSICALLY_CONTROLLED,
            read.program().circuit(0).operation(9).kind()
        );
        assertEquals(
            OperationKind.TIMING_BOX,
            read.program().circuit(0).operation(15).kind()
        );

        final QuantumIrWriteResult secondWrite = new QuantumIrJsonWriter().write(read.program());

        assertTrue(secondWrite.isSuccess());
        assertEquals(
            firstWrite.content(),
            secondWrite.content()
        );
    }

    @Test
    void writesAndReadsDynamicQuantumReferences() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("dynamic_json");
        final QuantumRegister buffer = circuit.createQuantumRegister(
            "buffer",
            4
        );
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        final QuantumReference bufferAtAddress = QuantumReference.dynamicIndex(
            buffer,
            ClassicalExpression.binary(
                ClassicalBinaryOperator.ADD,
                ClassicalExpression.variable("address"),
                ClassicalExpression.integer(1)
            )
        );

        circuit.gateReferences(
            StandardGate.CY,
            bufferAtAddress,
            QuantumReference.staticQubit(q.get(0))
        );
        circuit.measureReference(
            bufferAtAddress,
            c.get(0)
        );

        final QuantumIrWriteResult written = new QuantumIrJsonWriter().write(program);
        final QuantumIrReadResult read = new QuantumIrJsonReader().read(written.content());

        assertTrue(written.isSuccess());
        assertTrue(written.content().contains("\"kind\" : \"DYNAMIC_REGISTER_INDEX\""));
        assertTrue(written.content().contains("\"kind\" : \"BINARY_OPERATION\""));
        assertTrue(read.isSuccess());
        final GateOperation gateOperation = (GateOperation) read.program().circuit(0).operation(0);
        final MeasureOperation measureOperation = (MeasureOperation) read.program().circuit(0).operation(1);
        assertEquals(
            ClassicalBinaryOperator.ADD,
            gateOperation.qubitReference(0).indexExpression().binaryOperator()
        );
        assertEquals(
            "address",
            measureOperation.qubitReference().indexExpression().leftExpression().variableName()
        );
    }

    @Test
    void writesAndReadsThroughFileApi() {
        final QuantumProgram program = createRichProgram();
        final Path path = tempDir.resolve("program.quantum.json");

        final QuantumIrWriteResult write = QuantumIrFiles.write(
            path,
            program
        );
        final QuantumIrReadResult read = QuantumIrFiles.read(path);
        final QuantumIrWriteResult writeAgain = QuantumIrFiles.writeToString(read.program());

        assertTrue(write.isSuccess());
        assertTrue(read.isSuccess());
        assertTrue(writeAgain.isSuccess());
        assertEquals(
            write.content(),
            writeAgain.content()
        );
    }

    @Test
    void rejectsMalformedAndUnsupportedJson() {
        final QuantumIrReadResult malformed = new QuantumIrJsonReader().read("{");
        final QuantumIrReadResult wrongFormat = new QuantumIrJsonReader().read("""
            {
              "format" : "foreign",
              "version" : 1,
              "program" : {}
            }
            """);

        assertFalse(malformed.isSuccess());
        assertEquals(
            PersistenceDiagnosticCode.MALFORMED_JSON,
            malformed.diagnostics().get(0).code()
        );
        assertFalse(wrongFormat.isSuccess());
        assertEquals(
            PersistenceDiagnosticCode.UNSUPPORTED_FORMAT,
            wrongFormat.diagnostics().get(0).code()
        );
    }

    @Test
    void rejectsUnknownReferencesDuringRead() {
        final QuantumIrReadResult result = new QuantumIrJsonReader().read("""
            {
              "format" : "pathcreator.quantum-ir",
              "version" : 1,
              "program" : {
                "computationModel" : "GATE_BASED_CIRCUIT",
                "gateDefinitions" : [],
                "circuits" : [ {
                  "name" : "broken",
                  "quantumRegisters" : [ {
                    "name" : "q",
                    "size" : 1
                  } ],
                  "classicalRegisters" : [],
                  "operations" : [ {
                    "kind" : "GATE",
                    "gate" : {
                      "kind" : "GATE_DEFINITION",
                      "name" : "missing_gate"
                    },
                    "parameters" : [],
                    "qubits" : [ "q[0]" ]
                  } ]
                } ]
              }
            }
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            PersistenceDiagnosticCode.UNKNOWN_REFERENCE,
            result.diagnostics().get(0).code()
        );
    }

    @Test
    void rejectsNonPortableCustomValidationRuleDuringWrite() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.of(
            "custom_rule_gate",
            1,
            0,
            List.of(new GateValidationRule() {
                @Override
                public void validate(
                    final GateOperation operation,
                    final GateValidationRuleErrorCollector collector
                ) {
                }
            })
        ));

        final QuantumIrWriteResult result = new QuantumIrJsonWriter().write(program);

        assertFalse(result.isSuccess());
        assertEquals(
            PersistenceDiagnosticCode.UNSUPPORTED_MODEL_FEATURE,
            result.diagnostics().get(0).code()
        );
    }

    private static QuantumProgram createRichProgram() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition customPhase = GateDefinition.of(
            "custom_phase",
            1,
            1,
            List.of()
        );
        final GateDefinition externalTwo = GateDefinition.opaque(
            "external_two",
            List.of("alpha"),
            List.of(
                "left",
                "right"
            )
        );
        final GateDefinition entangleThenPhase = GateDefinition.composite(
            "entangle_then_phase",
            List.of("theta"),
            List.of(
                "left",
                "right"
            ),
            List.of(
                GateBodyOperation.of(
                    StandardGate.H,
                    new ParameterExpression[0],
                    "left"
                ),
                GateBodyOperation.of(
                    StandardGate.CX,
                    new ParameterExpression[0],
                    "left",
                    "right"
                ),
                GateBodyOperation.of(
                    customPhase,
                    new ParameterExpression[] {ParameterExpression.named("theta")},
                    "right"
                )
            )
        );
        program.addGateDefinition(customPhase)
            .addGateDefinition(externalTwo)
            .addGateDefinition(entangleThenPhase);
        program.addClassicalDeclaration(new ClassicalDeclaration(
            "shot_counter",
            ClassicalType.sized(
                ClassicalTypeKind.UNSIGNED_INTEGER,
                64
            )
        ));
        program.addCallableDefinition(new CallableDefinition(
            "prepare_state",
            CallableOperationBlock.of(
                CallableGateOperation.of(
                    StandardGate.H,
                    "q"
                ),
                new CallableDelayOperation(
                    DurationExpression.duration(
                        5,
                        DurationUnit.NS
                    ),
                    "q"
                ),
                new CallableConditionalBlockOperation(
                    CallableClassicalPredicate.compare(
                        CallableClassicalExpression.argument("flag"),
                        ClassicalComparisonOperator.EQUAL,
                        CallableClassicalExpression.integer(1)
                    ),
                    CallableOperationBlock.of(CallableGateOperation.of(
                        StandardGate.X,
                        "q"
                    )),
                    CallableOperationBlock.of(CallableGateOperation.of(
                        StandardGate.Z,
                        "q"
                    ))
                ),
                new CallableMeasureOperation(
                    "q",
                    "flag"
                )
            ),
            CallableArgument.qubit("q"),
            CallableArgument.classical(
                "flag",
                ClassicalType.of(ClassicalTypeKind.BOOLEAN)
            )
        ));
        program.addExternalCallableDeclaration(new ExternalCallableDeclaration(
            "external_cost",
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
        ));
        program.addCalibrationDefinition(new CalibrationDefinition(
            "custom_phase",
            List.of("theta"),
            List.of("q"),
            "pulse",
            "play theta q"
        ));

        final QuantumCircuit circuit = program.createCircuit("rich_program");
        final ru.pathcreator.vadim.quantum.domain.register.QuantumRegister q = circuit.createQuantumRegister(
            "q",
            5
        );
        final ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            3
        );
        final ParameterExpression theta = ParameterExpression.add(
            ParameterExpression.divide(
                ParameterExpression.pi(),
                ParameterExpression.of(2.0)
            ),
            ParameterExpression.negate(ParameterExpression.named("delta"))
        );
        final ParameterExpression alpha = ParameterExpression.multiply(
            ParameterExpression.knownConstant("pi"),
            ParameterExpression.divide(
                ParameterExpression.named("scale"),
                ParameterExpression.of(4.0)
            )
        );
        circuit.parameterizedGate(
            entangleThenPhase,
            new ParameterExpression[] {theta},
            q.get(0),
            q.get(1)
        );
        circuit.parameterizedGate(
            externalTwo,
            new ParameterExpression[] {alpha},
            q.get(1),
            q.get(2)
        );
        circuit.gate(
            ModifiedGate.of(
                StandardGate.X,
                List.of(
                    GateModifier.controlled(1),
                    GateModifier.inverse()
                )
            ),
            q.get(2),
            q.get(3)
        );
        circuit.parameterizedGate(
            ModifiedGate.of(
                StandardGate.RZ,
                List.of(
                    GateModifier.power(0.5),
                    GateModifier.repeat(2),
                    GateModifier.annotation("logical_hint")
                )
            ),
            new ParameterExpression[] {ParameterExpression.named("phi")},
            q.get(4)
        );
        circuit.reset(q.get(4));
        circuit.barrier(
            q.get(0),
            q.get(1),
            q.get(2)
        );
        circuit.measure(
            q.get(0),
            c.get(0)
        );
        circuit.controlled(
            ClassicalCondition.equalTo(
                c,
                3
            ),
            GateOperation.of(
                StandardGate.Z,
                q.get(4)
            )
        );
        circuit.assign(new ClassicalAssignment(
            ClassicalExpression.bit(c.get(1)),
            ClassicalExpression.integer(1)
        ));
        circuit.classicallyControlled(
            ClassicalPredicate.and(
                ClassicalPredicate.compare(
                    ClassicalExpression.register(c),
                    ClassicalComparisonOperator.GREATER_THAN_OR_EQUAL,
                    ClassicalExpression.integer(1)
                ),
                ClassicalPredicate.not(ClassicalPredicate.compare(
                    ClassicalExpression.bit(c.get(2)),
                    ClassicalComparisonOperator.EQUAL,
                    ClassicalExpression.integer(0)
                ))
            ),
            GateOperation.of(
                StandardGate.Y,
                q.get(4)
            )
        );
        circuit.block(OperationBlock.of(GateOperation.of(
            StandardGate.H,
            q.get(3)
        )));
        circuit.conditionalBlock(
            ClassicalPredicate.compare(
                ClassicalExpression.bit(c.get(0)),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(1)
            ),
            OperationBlock.of(GateOperation.of(
                StandardGate.X,
                q.get(3)
            )),
            OperationBlock.of(new ResetOperation(q.get(3)))
        );
        circuit.forLoop(
            "i",
            0,
            1,
            2,
            OperationBlock.of(GateOperation.of(
                StandardGate.Z,
                q.get(2)
            ))
        );
        circuit.whileLoop(
            ClassicalPredicate.compare(
                ClassicalExpression.bit(c.get(1)),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(1)
            ),
            OperationBlock.of(GateOperation.of(
                StandardGate.Y,
                q.get(2)
            ))
        );
        circuit.delay(
            DurationExpression.duration(
                40,
                DurationUnit.NS
            ),
            q.get(0),
            q.get(1)
        );
        circuit.timingBox(
            DurationExpression.stretch("stretch_duration"),
            OperationBlock.of(GateOperation.of(
                StandardGate.S,
                q.get(1)
            ))
        );
        circuit.setOperationMetadata(
            0,
            new OperationMetadata(
                new ExternalSource(
                    "native-test",
                    "full persistence shape"
                ),
                new SourceLocation(
                    12,
                    7
                )
            )
        );
        return program;
    }
}