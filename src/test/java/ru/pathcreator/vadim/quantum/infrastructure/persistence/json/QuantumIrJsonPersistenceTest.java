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
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
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
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;

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
        assertTrue(firstWrite.content().contains("\"kind\" : \"MODIFIED\""));
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
            10,
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

        final QuantumIrWriteResult secondWrite = new QuantumIrJsonWriter().write(read.program());

        assertTrue(secondWrite.isSuccess());
        assertEquals(
            firstWrite.content(),
            secondWrite.content()
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