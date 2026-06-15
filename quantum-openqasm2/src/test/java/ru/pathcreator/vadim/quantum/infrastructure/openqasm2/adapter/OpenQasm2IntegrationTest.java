/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.adapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecomposition;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionPacks;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRegistry;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRule;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportTextMode;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.UnsupportedGatePolicy;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinitionKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.metadata.OperationMetadata;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BarrierOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicallyControlledOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.MeasureOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ResetOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenQasm2IntegrationTest {

    @Test
    void exportsBellCircuitToOpenQasm2() {
        final QuantumProgram program = createBellProgram();

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            h q[0];
            cx q[0],q[1];
            measure q[0] -> c[0];
            measure q[1] -> c[1];
            """,
            result.content()
        );
    }

    @Test
    void emitsCanonicalOpenQasm2WhenLosslessModeHasNoSourceSnapshot() {
        final ExportResult result = new OpenQasm2Integration().exportProgram(
            createBellProgram(),
            ExportOptions.of(
                true,
                false,
                UnsupportedGatePolicy.OPAQUE_IF_POSSIBLE,
                ExportTextMode.LOSSLESS_WHEN_AVAILABLE,
                GateDecompositionRegistry.empty()
            )
        );

        assertTrue(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.OUTPUT_MODE_DOWNGRADED,
            result.diagnostic(0).code()
        );
    }

    @Test
    void failsLosslessModeDowngradeWhenWarningsAreErrors() {
        final ExportResult result = new OpenQasm2Integration().exportProgram(
            createBellProgram(),
            ExportOptions.of(
                true,
                true,
                UnsupportedGatePolicy.OPAQUE_IF_POSSIBLE,
                ExportTextMode.LOSSLESS_WHEN_AVAILABLE,
                GateDecompositionRegistry.empty()
            )
        );

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.OUTPUT_MODE_DOWNGRADED,
            result.diagnostic(0).code()
        );
    }

    @Test
    void exportsParameterizedResetBarrierAndPhase() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("ops");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );

        circuit.phase(
            ParameterExpression.named("theta"),
            q.get(0)
        )
            .reset(q.get(1))
            .barrier(
                q.get(0),
                q.get(1)
            );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            u1(theta) q[0];
            reset q[1];
            barrier q[0],q[1];
            """,
            result.content()
        );
    }

    @Test
    void exportsCompositeAndOpaqueGateDefinitions() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition myh = GateDefinition.composite(
            "myh",
            List.of(),
            List.of("a"),
            List.of(GateBodyOperation.of(
                StandardGate.H,
                new ParameterExpression[0],
                "a"
            ))
        );
        final GateDefinition blackbox = GateDefinition.opaque(
            "blackbox",
            List.of("theta"),
            List.of("a")
        );
        program.addGateDefinition(myh)
            .addGateDefinition(blackbox);
        final QuantumCircuit circuit = program.createCircuit("custom");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.gate(
            myh,
            q.get(0)
        )
            .parameterizedGate(
                blackbox,
                new ParameterExpression[] {ParameterExpression.of(0.5)},
                q.get(0)
            );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            opaque blackbox(theta) a;
            qreg q[1];
            h q[0];
            blackbox(0.5) q[0];
            """,
            result.content()
        );
    }

    @Test
    void exportsSupportedModifiedGateInsideCompositeDefinitionBody() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.composite(
            "controlled_phase_flip",
            List.of(),
            List.of(
                "control",
                "target"
            ),
            List.of(GateBodyOperation.of(
                ModifiedGate.of(
                    StandardGate.Z,
                    List.of(GateModifier.controlled(1))
                ),
                new ParameterExpression[0],
                "control",
                "target"
            ))
        ));
        final QuantumCircuit circuit = program.createCircuit("modified_body");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        circuit.gate(
            program.gateDefinition(0),
            q.get(0),
            q.get(1)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            cz q[0],q[1];
            """,
            result.content()
        );
    }

    @Test
    void exportsIntrinsicGateInsideCompositeDefinitionBodyAsOpaqueDuringExport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition intrinsic = GateDefinition.of(
            "hardware_only",
            1,
            0
        );
        program.addGateDefinition(intrinsic);
        program.addGateDefinition(GateDefinition.composite(
            "uses_hardware_only",
            List.of(),
            List.of("q"),
            List.of(GateBodyOperation.of(
                intrinsic,
                new ParameterExpression[0],
                "q"
            ))
        ));
        final QuantumCircuit circuit = program.createCircuit("unsupported_body");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.gate(
            program.gateDefinition(1),
            q.get(0)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            opaque hardware_only q0;
            qreg q[1];
            hardware_only q[0];
            """,
            result.content()
        );
    }

    @Test
    void exportsControlledResetOperation() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("conditional");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );

        circuit.controlled(
            ClassicalCondition.equalTo(
                c,
                1
            ),
            new ResetOperation(q.get(0))
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            creg c[1];
            if(c==1) reset q[0];
            """,
            result.content()
        );
    }

    @Test
    void exportsRegisterEqualityPredicateControlOperation() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("predicate_control");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );

        circuit.classicallyControlled(
            ClassicalPredicate.compare(
                ClassicalExpression.register(c),
                ClassicalComparisonOperator.EQUAL,
                ClassicalExpression.integer(2)
            ),
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            )
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            creg c[2];
            if(c==2) x q[0];
            """,
            result.content()
        );
    }

    @Test
    void exportsPredicateControlledMeasureAndBarrierOperations() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("predicate_qop");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        final ClassicalPredicate predicate = ClassicalPredicate.compare(
            ClassicalExpression.register(c),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(1)
        );

        circuit.classicallyControlled(
            predicate,
            new MeasureOperation(
                q.get(0),
                c.get(0)
            )
        )
            .classicallyControlled(
                predicate,
                new BarrierOperation(
                    q.get(0),
                    q.get(1)
                )
            );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            if(c==1) measure q[0] -> c[0];
            if(c==1) barrier q[0],q[1];
            """,
            result.content()
        );
    }

    @Test
    void rejectsClassicalAssignmentDuringExport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("assignment");
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.assign(new ClassicalAssignment(
            ClassicalExpression.bit(c.get(0)),
            ClassicalExpression.integer(1)
        ));

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
            result.diagnostic(0).code()
        );
    }

    @Test
    void rejectsUnsupportedClassicalPredicateDuringExport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("predicate");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.classicallyControlled(
            ClassicalPredicate.compare(
                ClassicalExpression.register(c),
                ClassicalComparisonOperator.NOT_EQUAL,
                ClassicalExpression.integer(0)
            ),
            GateOperation.of(
                StandardGate.X,
                q.get(0)
            )
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_OPERATION,
            result.diagnostic(0).code()
        );
    }

    @Test
    void rejectsInvalidDomainProgramBeforeExport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("invalid");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.cx(
            q.get(0),
            q.get(0)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.DOMAIN_VALIDATION_FAILED,
            result.diagnostic(0).code()
        );
    }

    @Test
    void exportsCustomIntrinsicGateAsOpaqueDuringExport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("custom");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        final GateDefinition customGate = GateDefinition.of(
            "custom_gate",
            1,
            0
        );
        program.addGateDefinition(customGate);

        circuit.gate(
            customGate,
            q.get(0)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            opaque custom_gate q0;
            qreg q[1];
            custom_gate q[0];
            """,
            result.content()
        );
    }

    @Test
    void rejectsCustomIntrinsicGateWhenDecompositionIsRequiredButMissing() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("custom");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "custom_gate",
            1,
            0
        );
        program.addGateDefinition(customGate);
        circuit.gate(
            customGate,
            q.get(0)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(
            program,
            ExportOptions.of(
                true,
                false,
                UnsupportedGatePolicy.REQUIRE_DECOMPOSITION,
                GateDecompositionRegistry.empty()
            )
        );

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_GATE,
            result.diagnostic(0).code()
        );
    }

    @Test
    void exportsCustomIntrinsicGateThroughDecompositionRegistry() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("custom");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final GateDefinition customGate = GateDefinition.of(
            "custom_h",
            1,
            0
        );
        program.addGateDefinition(customGate);
        circuit.gate(
            customGate,
            q.get(0)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(
            program,
            ExportOptions.of(
                true,
                false,
                UnsupportedGatePolicy.REQUIRE_DECOMPOSITION,
                GateDecompositionRegistry.of(List.of(new GateDecompositionRule() {
                    @Override
                    public boolean supports(final Gate gate) {
                        return customGate.equals(gate);
                    }

                    @Override
                    public GateDecomposition decompose(final GateOperation operation) {
                        return GateDecomposition.of(List.of(GateOperation.of(
                            StandardGate.H,
                            operation.qubit(0)
                        )));
                    }
                }))
            )
        );

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            h q[0];
            """,
            result.content()
        );
    }

    @Test
    void rejectsCustomGateDefinitionWithReservedQelibNameDuringExport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("reserved_qelib");
        circuit.createQuantumRegister(
            "q",
            1
        );
        program.addGateDefinition(GateDefinition.opaque(
            "u3",
            List.of(
                "theta",
                "phi",
                "lambda"
            ),
            List.of("q")
        ));

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_GATE,
            result.diagnostic(0).code()
        );
    }

    @Test
    void lowersSupportedModifiedGatesDuringExport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("modified");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );

        circuit.gate(
            ModifiedGate.of(
                StandardGate.X,
                List.of(GateModifier.controlled(1))
            ),
            q.get(0),
            q.get(1)
        )
            .gate(
                ModifiedGate.of(
                    StandardGate.S,
                    List.of(GateModifier.inverse())
                ),
                q.get(1)
            )
            .gate(
                ModifiedGate.of(
                    StandardGate.H,
                    List.of(GateModifier.annotation("calibrated"))
                ),
                q.get(0)
            )
            .gate(
                ModifiedGate.of(
                    StandardGate.Y,
                    List.of(GateModifier.controlled(1))
                ),
                q.get(0),
                q.get(1)
            )
            .gate(
                ModifiedGate.of(
                    StandardGate.Z,
                    List.of(GateModifier.controlled(1))
                ),
                q.get(0),
                q.get(1)
            )
            .gate(
                ModifiedGate.of(
                    StandardGate.H,
                    List.of(GateModifier.controlled(1))
                ),
                q.get(0),
                q.get(1)
            )
            .gate(
                ModifiedGate.of(
                    StandardGate.X,
                    List.of(
                        GateModifier.inverse(),
                        GateModifier.controlled(1)
                    )
                ),
                q.get(0),
                q.get(1)
            )
            .gate(
                ModifiedGate.of(
                    StandardGate.X,
                    List.of(GateModifier.power(0.5))
                ),
                q.get(0)
            );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            cx q[0],q[1];
            sdg q[1];
            h q[0];
            cy q[0],q[1];
            cz q[0],q[1];
            ch q[0],q[1];
            cx q[0],q[1];
            sx q[0];
            """,
            result.content()
        );
    }

    @Test
    void rejectsUnsupportedModifiedGateDuringExportWithCapabilityDiagnostic() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("modified_power");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.gate(
            ModifiedGate.of(
                StandardGate.H,
                List.of(GateModifier.power(0.5))
            ),
            q.get(0)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_GATE,
            result.diagnostic(0).code()
        );
    }

    @Test
    void exportsRepeatModifierThroughBuiltInDecompositionPack() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("repeat");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );

        circuit.gate(
            ModifiedGate.of(
                StandardGate.X,
                List.of(GateModifier.repeat(3))
            ),
            q.get(0)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(
            program,
            ExportOptions.of(
                true,
                false,
                UnsupportedGatePolicy.REQUIRE_DECOMPOSITION,
                GateDecompositionPacks.repeatModifiers()
            )
        );

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            x q[0];
            x q[0];
            x q[0];
            """,
            result.content()
        );
    }

    @Test
    void mergesMultipleCircuitsDuringExportWhenRegisterNamesDoNotConflict() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit first = program.createCircuit("first");
        final QuantumRegister q = first.createQuantumRegister(
            "q",
            1
        );
        first.h(q.get(0));
        final QuantumCircuit second = program.createCircuit("second");
        final QuantumRegister r = second.createQuantumRegister(
            "r",
            1
        );
        second.x(r.get(0));

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            qreg r[1];
            h q[0];
            x r[0];
            """,
            result.content()
        );
    }

    @Test
    void rejectsMultipleCircuitMergeWithDuplicateRegisterNames() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.createCircuit("first")
            .createQuantumRegister(
                "q",
                1
            );
        program.createCircuit("second")
            .createQuantumRegister(
                "q",
                1
            );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_CIRCUIT_STRUCTURE,
            result.diagnostic(0).code()
        );
    }

    @Test
    void importsWithWhitespaceBeforeGateParametersAndCaseSensitiveCustomGateName() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            gate cH a,b {
                h b;
                cx a,b;
            }
            u1 (pi/2) q[0];
            cH q[0],q[1];
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().gateDefinitionCount()
        );
        assertEquals(
            "cH",
            result.program().gateDefinition(0).gateName()
        );
    }

    @Test
    void skipsBinaryFilesWhileResolvingIncludeDirectory(
        @TempDir final Path tempDir
    ) throws IOException {
        Files.writeString(
            tempDir.resolve("qelib1.inc"),
            ""
        );
        Files.write(
            tempDir.resolve("preview.png"),
            new byte[] {
                (byte) 0x89,
                (byte) 0x50,
                (byte) 0x4E,
                (byte) 0x47
            }
        );

        final ImportResult result = new OpenQasm2Integration().importProgram(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            h q[0];
            """,
            ImportOptions.defaults().withIncludeDirectory(tempDir.toString())
        );

        assertTrue(result.isSuccess());
    }

    @Test
    void importsBellCircuitFromOpenQasm2() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            h q[0];
            cx q[0],q[1];
            measure q[0] -> c[0];
            measure q[1] -> c[1];
            """;

        final ImportResult result = new OpenQasm2Integration().importProgram(source);

        assertTrue(result.isSuccess());
        final QuantumProgram program = result.program();
        assertEquals(
            1,
            program.circuitCount()
        );
        final QuantumCircuit circuit = program.circuit(0);
        assertEquals(
            1,
            circuit.quantumRegisterCount()
        );
        assertEquals(
            1,
            circuit.classicalRegisterCount()
        );
        assertEquals(
            4,
            circuit.operationCount()
        );
        assertGate(
            circuit,
            0,
            StandardGate.H
        );
        assertGate(
            circuit,
            1,
            StandardGate.CX
        );
        assertInstanceOf(
            MeasureOperation.class,
            circuit.operation(2)
        );
        assertInstanceOf(
            MeasureOperation.class,
            circuit.operation(3)
        );
        final OperationMetadata metadata = circuit.operationMetadata(0);
        assertFalse(metadata.isEmpty());
        assertEquals(
            "openqasm2",
            metadata.source().format()
        );
        assertEquals(
            5,
            metadata.location().line()
        );
        assertEquals(
            1,
            metadata.location().column()
        );
    }

    @Test
    void importsRegisterWideOperationsFromOpenQasm2() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            h q;
            barrier q;
            reset q;
            measure q -> c;
            """;

        final ImportResult result = new OpenQasm2Integration().importProgram(source);

        assertTrue(result.isSuccess());
        final QuantumCircuit circuit = result.program().circuit(0);
        assertEquals(
            7,
            circuit.operationCount()
        );
        assertGate(
            circuit,
            0,
            StandardGate.H
        );
        assertGate(
            circuit,
            1,
            StandardGate.H
        );
        final BarrierOperation barrier = assertInstanceOf(
            BarrierOperation.class,
            circuit.operation(2)
        );
        assertEquals(
            2,
            barrier.qubitCount()
        );
        assertInstanceOf(
            ResetOperation.class,
            circuit.operation(3)
        );
        assertInstanceOf(
            ResetOperation.class,
            circuit.operation(4)
        );
        assertInstanceOf(
            MeasureOperation.class,
            circuit.operation(5)
        );
        assertInstanceOf(
            MeasureOperation.class,
            circuit.operation(6)
        );
    }

    @Test
    void importsPhaseAsQuantumIrPhaseGate() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            u1(theta) q[0];
            """;

        final ImportResult result = new OpenQasm2Integration().importProgram(source);

        assertTrue(result.isSuccess());
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(0)
        );
        assertEquals(
            StandardGate.PHASE,
            operation.gate()
        );
        assertEquals(
            "theta",
            operation.parameter(0).name()
        );
    }

    @Test
    void importsParameterExpressionTreeFromOpenQasm2() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            u1(pi/2) q[0];
            """;

        final ImportResult result = new OpenQasm2Integration().importProgram(source);

        assertTrue(result.isSuccess());
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(0)
        );
        assertEquals(
            ParameterExpressionKind.BINARY,
            operation.parameter(0).kind()
        );
        assertEquals(
            "pi/2.0",
            operation.parameter(0).toString()
        );
    }

    @Test
    void importsQelibUniversalGatesFromOpenQasm2() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            U(pi/2,0,pi) q[0];
            u3(pi/2,0,pi) q[0];
            u2(0,pi) q[0];
            crz(pi/4) q[0],q[1];
            cu(1,2,3,4) q[0],q[1];
            """;

        final ImportResult result = new OpenQasm2Integration().importProgram(source);

        assertTrue(result.isSuccess());
        final QuantumCircuit circuit = result.program().circuit(0);
        assertEquals(
            5,
            circuit.operationCount()
        );
        assertGateName(
            circuit,
            0,
            "u"
        );
        assertGateName(
            circuit,
            1,
            "u3"
        );
        assertGateName(
            circuit,
            2,
            "u2"
        );
        assertGateName(
            circuit,
            3,
            "crz"
        );
        assertGateName(
            circuit,
            4,
            "cu"
        );
    }

    @Test
    void importsEveryRegisteredQelibGateFromOpenQasm2() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[5];
            U(1,2,3) q[0];
            u(1,2,3) q[0];
            u3(1,2,3) q[0];
            u2(1,2) q[0];
            u1(1) q[0];
            p(1) q[0];
            phase(1) q[0];
            id q[0];
            u0(1) q[0];
            x q[0];
            y q[0];
            z q[0];
            h q[0];
            s q[0];
            sdg q[0];
            t q[0];
            tdg q[0];
            rx(1) q[0];
            ry(1) q[0];
            rz(1) q[0];
            sx q[0];
            sxdg q[0];
            CX q[0],q[1];
            cx q[0],q[1];
            cy q[0],q[1];
            cz q[0],q[1];
            ch q[0],q[1];
            swap q[0],q[1];
            crx(1) q[0],q[1];
            cry(1) q[0],q[1];
            crz(1) q[0],q[1];
            cu1(1) q[0],q[1];
            cp(1) q[0],q[1];
            cu3(1,2,3) q[0],q[1];
            csx q[0],q[1];
            cu(1,2,3,4) q[0],q[1];
            rxx(1) q[0],q[1];
            rzz(1) q[0],q[1];
            ccx q[0],q[1],q[2];
            cswap q[0],q[1],q[2];
            rccx q[0],q[1],q[2];
            rc3x q[0],q[1],q[2],q[3];
            c3x q[0],q[1],q[2],q[3];
            c3sqrtx q[0],q[1],q[2],q[3];
            c4x q[0],q[1],q[2],q[3],q[4];
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            45,
            result.program().circuit(0).operationCount()
        );
    }

    @Test
    void exportsQelibUniversalGatesToOpenQasm2() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("qelib");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );

        circuit.parameterizedGate(
            GateDefinition.of(
                "u3",
                1,
                3
            ),
            new ParameterExpression[] {
                ParameterExpression.pi(),
                ParameterExpression.of(0.0),
                ParameterExpression.pi()
            },
            q.get(0)
        );
        circuit.parameterizedGate(
            GateDefinition.of(
                "cu",
                2,
                4
            ),
            new ParameterExpression[] {
                ParameterExpression.of(1.0),
                ParameterExpression.of(2.0),
                ParameterExpression.of(3.0),
                ParameterExpression.of(4.0)
            },
            q.get(0),
            q.get(1)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            u3(pi,0,pi) q[0];
            cu(1,2,3,4) q[0],q[1];
            """,
            result.content()
        );
    }

    @Test
    void importsCompositeGateBodyUsingQelibUniversalGate() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            gate custom(theta,phi,lambda) a {
              u3(theta,phi,lambda) a;
            }
            qreg q[1];
            custom(pi/2,0,pi) q[0];
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().gateDefinitionCount()
        );
        assertGateName(
            result.program().circuit(0),
            0,
            "custom"
        );
    }

    @Test
    void importsCustomIncludeSourceFromImportOptions() {
        final ImportOptions options = ImportOptions.defaults().withIncludedSource(
            "custom.inc",
            """
            gate myh a {
              h a;
            }
            """
        );
        final ImportResult result = new OpenQasm2Integration().importProgram(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            include "custom.inc";
            qreg q[1];
            myh q[0];
            """,
            options
        );

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().gateDefinitionCount()
        );
        assertGateName(
            result.program().circuit(0),
            0,
            "myh"
        );
    }

    @Test
    void importsCustomIncludeSourceFromFilesystemDirectory(@TempDir final Path tempDirectory) throws IOException {
        Files.writeString(
            tempDirectory.resolve("custom.inc"),
            """
            gate myh a {
              h a;
            }
            """
        );
        final ImportResult result = new OpenQasm2Integration().importProgram(
            """
            OPENQASM 2.0;
            include "custom.inc";
            qreg q[1];
            myh q[0];
            """,
            ImportOptions.defaults().withIncludeDirectory(tempDirectory.toString())
        );

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().gateDefinitionCount()
        );
        assertGateName(
            result.program().circuit(0),
            0,
            "myh"
        );
    }

    @Test
    void rejectsMissingCustomIncludeSourceDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            include "missing.inc";
            qreg q[1];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
            result.diagnostic(0).code()
        );
        assertEquals(
            "OpenQASM 2 include source is not available: missing.inc.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsCustomIncludeCycleDuringImport() {
        final ImportOptions options = ImportOptions.defaults()
            .withIncludedSource(
                "a.inc",
                "include \"b.inc\";"
            )
            .withIncludedSource(
                "b.inc",
                "include \"a.inc\";"
            );
        final ImportResult result = new OpenQasm2Integration().importProgram(
            """
            OPENQASM 2.0;
            include "a.inc";
            qreg q[1];
            """,
            options
        );

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "OpenQASM 2 include cycle detected: a.inc.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void importsControlledGateFromOpenQasm2() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            creg c[1];
            if(c==1) x q[0];
            """;

        final ImportResult result = new OpenQasm2Integration().importProgram(source);

        assertTrue(result.isSuccess());
        final ClassicallyControlledOperation controlledOperation = assertInstanceOf(
            ClassicallyControlledOperation.class,
            result.program().circuit(0).operation(0)
        );
        assertEquals(
            ClassicalComparisonOperator.EQUAL,
            controlledOperation.predicate().comparisonOperator()
        );
        assertSame(
            result.program().circuit(0).classicalRegister(0),
            controlledOperation.predicate().leftExpression().register()
        );
        assertEquals(
            1L,
            controlledOperation.predicate().rightExpression().integerValue()
        );
        final GateOperation gateOperation = assertInstanceOf(
            GateOperation.class,
            controlledOperation.operation()
        );
        assertEquals(
            StandardGate.X,
            gateOperation.gate()
        );
    }

    @Test
    void roundTripsOpenQasm2ConditionalGateThroughPredicateControl() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            creg c[2];
            if(c==2) x q[0];
            """;

        final ImportResult imported = new OpenQasm2Integration().importProgram(source);

        assertTrue(imported.isSuccess());
        final ExportResult exported = new OpenQasm2Integration().exportProgram(imported.program());

        assertTrue(exported.isSuccess());
        assertEquals(
            source,
            exported.content()
        );
    }

    @Test
    void importsOpaqueAndCompositeGateDefinitionsFromOpenQasm2() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            opaque blackbox(theta) a;
            gate myh a {
              h a;
            }
            qreg q[1];
            myh q[0];
            blackbox(pi/2) q[0];
            """;

        final ImportResult result = new OpenQasm2Integration().importProgram(source);

        assertTrue(result.isSuccess());
        assertEquals(
            2,
            result.program().gateDefinitionCount()
        );
        assertEquals(
            GateDefinitionKind.OPAQUE,
            result.program().gateDefinition(0).kind()
        );
        assertEquals(
            GateDefinitionKind.COMPOSITE,
            result.program().gateDefinition(1).kind()
        );
        assertEquals(
            2,
            result.program().circuit(0).operationCount()
        );
    }

    @Test
    void roundTripsOpaqueAndCompositeGateDefinitionsFromOpenQasm2() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            opaque blackbox(theta) a;
            gate myh a {
              h a;
            }
            qreg q[1];
            myh q[0];
            blackbox(pi/2) q[0];
            """;

        final ImportResult imported = new OpenQasm2Integration().importProgram(source);

        assertTrue(imported.isSuccess());
        final ExportResult exported = new OpenQasm2Integration().exportProgram(imported.program());

        assertTrue(exported.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            opaque blackbox(theta) a;
            qreg q[1];
            h q[0];
            blackbox(pi/2) q[0];
            """,
            exported.content()
        );
    }

    @Test
    void importsNestedCompositeGateDefinitionsFromOpenQasm2() {
        final String source = """
            OPENQASM 2.0;
            include "qelib1.inc";
            gate base a {
              h a;
            }
            gate wrapped a {
              base a;
            }
            qreg q[1];
            wrapped q[0];
            """;

        final ImportResult result = new OpenQasm2Integration().importProgram(source);

        assertTrue(result.isSuccess());
        assertEquals(
            2,
            result.program().gateDefinitionCount()
        );
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            result.program().circuit(0).operation(0)
        );
        assertEquals(
            "wrapped",
            operation.gate().gateName()
        );
    }

    @Test
    void exportsNestedCompositeGateDefinitionsToOpenQasm2() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final GateDefinition base = GateDefinition.composite(
            "base",
            List.of(),
            List.of("a"),
            List.of(GateBodyOperation.of(
                StandardGate.H,
                new ParameterExpression[0],
                "a"
            ))
        );
        final GateDefinition wrapped = GateDefinition.composite(
            "wrapped",
            List.of(),
            List.of("a"),
            List.of(GateBodyOperation.of(
                base,
                new ParameterExpression[0],
                "a"
            ))
        );
        program.addGateDefinition(base)
            .addGateDefinition(wrapped);
        final QuantumCircuit circuit = program.createCircuit("nested");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.gate(
            wrapped,
            q.get(0)
        );

        final ExportResult result = new OpenQasm2Integration().exportProgram(program);

        assertTrue(result.isSuccess());
        assertEquals(
            """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            h q[0];
            """,
            result.content()
        );
    }

    @Test
    void failsImportWhenDomainValidationRejectsImportedProgram() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            cx q[0],q[0];
            """);

        assertFalse(result.isSuccess());
        assertFalse(result.hasProgram());
        assertEquals(
            IntegrationDiagnosticCode.IMPORT_VALIDATION_FAILED,
            result.diagnostic(0).code()
        );
    }

    @Test
    void rejectsInvalidOpenQasm2Input() {
        final ImportResult result = new OpenQasm2Integration().importProgram("qreg q[1];");

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
    }

    @Test
    void rejectsUnsupportedOpenQasm2Gate() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            unknown_gate q[0];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_GATE,
            result.diagnostic(0).code()
        );
    }

    @Test
    void rejectsCustomGateDefinitionWithReservedQelibNameDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            opaque u3(theta,phi,lambda) q;
            qreg q[1];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_GATE,
            result.diagnostic(0).code()
        );
    }

    @Test
    void rejectsUnknownIndexedRegisterDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            h missing[0];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "Unknown quantum register: missing.",
            result.diagnostic(0).message()
        );
        assertEquals(
            4,
            result.diagnostic(0).line()
        );
        assertEquals(
            1,
            result.diagnostic(0).column()
        );
    }

    @Test
    void rejectsRegisterSizeOutsideJavaIntRangeDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2147483648];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "Integer index or size is outside Java int range.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsIndexedOperandOutsideJavaIntRangeDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            h q[2147483648];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "Integer index or size is outside Java int range.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsClassicalConditionValueOutsideJavaLongRangeDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            creg c[1];
            if(c==9223372036854775808) x q[0];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "Expected non-negative integer.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void importsConditionalMeasureAndBarrierStatementsFromOpenQasm2() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            if(c==1) measure q[0] -> c[0];
            if(c==1) barrier q;
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            2,
            result.program().circuit(0).operationCount()
        );
        assertInstanceOf(
            ClassicallyControlledOperation.class,
            result.program().circuit(0).operation(0)
        );
        assertInstanceOf(
            ClassicallyControlledOperation.class,
            result.program().circuit(0).operation(1)
        );
    }

    @Test
    void rejectsInvalidGateParameterExpressionDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            rx(pi/) q[0];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
            result.diagnostic(0).code()
        );
        assertEquals(
            "Cannot parse OpenQASM 2 parameter expression: pi/.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsInvalidCompositeGateBodyParameterExpressionDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            gate bad(theta) a {
              rx(theta/) a;
            }
            qreg q[1];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
            result.diagnostic(0).code()
        );
        assertEquals(
            "Cannot parse OpenQASM 2 parameter expression: theta/.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsUnclosedCompositeGateBodyDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            gate bad a {
              h a;
            qreg q[1];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "OpenQASM 2 source has an unclosed gate body.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsUnexpectedClosingBraceDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            }
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "OpenQASM 2 source has an unexpected closing brace.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsUnbalancedParameterListDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[1];
            u3((pi/2,0,pi) q[0];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "OpenQASM 2 parameter list has an unclosed parenthesis.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsUnbalancedGateDeclarationParameterListDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            opaque bad(theta,phi)) a;
            qreg q[1];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.PARSE_ERROR,
            result.diagnostic(0).code()
        );
        assertEquals(
            "OpenQASM 2 declaration list has an unexpected closing parenthesis.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsOpenQasmOperationInsideCompositeGateBodyDuringImport() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            gate bad a {
              measure a -> a;
            }
            qreg q[1];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_INPUT_FEATURE,
            result.diagnostic(0).code()
        );
        assertEquals(
            "OpenQASM 2 gate body supports gate operations only.",
            result.diagnostic(0).message()
        );
    }

    @Test
    void rejectsCompositeGateBodyUsingUndeclaredQubitDuringImportValidation() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            gate bad a {
              h b;
            }
            qreg q[1];
            """);

        assertFalse(result.isSuccess());
        assertEquals(
            IntegrationDiagnosticCode.IMPORT_VALIDATION_FAILED,
            result.diagnostic(0).code()
        );
    }

    @Test
    void importsCompositeGateDefinitionWithCommentsInsideBody() {
        final ImportResult result = new OpenQasm2Integration().importProgram("""
            OPENQASM 2.0;
            include "qelib1.inc";
            gate myh a {
              // comment inside gate body
              h a;
            }
            qreg q[1]; // trailing comment after register
            myh q[0];
            """);

        assertTrue(result.isSuccess());
        assertEquals(
            1,
            result.program().gateDefinitionCount()
        );
        assertEquals(
            1,
            result.program().gateDefinition(0).bodyOperations().size()
        );
        assertEquals(
            1,
            result.program().circuit(0).operationCount()
        );
    }

    @Test
    void roundTripsBellCircuitThroughOpenQasm2() {
        final QuantumIntegration integration = new OpenQasm2Integration();
        final ExportResult exportResult = integration.exportProgram(createBellProgram());

        final ImportResult importResult = integration.importProgram(exportResult.content());

        assertTrue(importResult.isSuccess());
        assertEquals(
            1,
            importResult.program().circuitCount()
        );
        assertEquals(
            4,
            importResult.program().circuit(0).operationCount()
        );
    }

    private static QuantumProgram createBellProgram() {
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
        return program;
    }

    private static void assertGate(
        final QuantumCircuit circuit,
        final int operationIndex,
        final StandardGate expectedGate
    ) {
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            circuit.operation(operationIndex)
        );
        assertSame(
            expectedGate,
            operation.gate()
        );
    }

    private static void assertGateName(
        final QuantumCircuit circuit,
        final int operationIndex,
        final String expectedGateName
    ) {
        final GateOperation operation = assertInstanceOf(
            GateOperation.class,
            circuit.operation(operationIndex)
        );
        assertEquals(
            expectedGateName,
            operation.gate().gateName()
        );
    }
}