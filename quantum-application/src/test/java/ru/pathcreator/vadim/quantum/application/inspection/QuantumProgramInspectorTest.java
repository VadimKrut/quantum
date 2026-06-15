/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.inspection;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.capability.TargetConnectivityGraph;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.callable.template.CallableOperationBlock;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalType;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalTypeKind;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.GateModifier;
import ru.pathcreator.vadim.quantum.domain.gate.modifier.ModifiedGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.BranchOperation;
import ru.pathcreator.vadim.quantum.domain.operation.CallableInvocationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalArrayDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalDeclarationOperation;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;
import ru.pathcreator.vadim.quantum.domain.timing.DurationExpression;
import ru.pathcreator.vadim.quantum.domain.timing.DurationUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumProgramInspectorTest {

    @Test
    void inspectsStaticCircuitWithPreciseDepthAndCoverage() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("static_metrics");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );
        final ModifiedGate inverseX = ModifiedGate.of(
            StandardGate.X,
            List.of(GateModifier.inverse())
        );
        circuit.h(q.get(0))
            .gate(
                inverseX,
                q.get(2)
            )
            .cx(
                q.get(0),
                q.get(1)
            )
            .ccx(
                q.get(0),
                q.get(1),
                q.get(2)
            )
            .rz(
                ParameterExpression.divide(
                    ParameterExpression.pi(),
                    ParameterExpression.of(2.0)
                ),
                q.get(2)
            )
            .barrier(
                q.get(0),
                q.get(2)
            )
            .reset(q.get(2))
            .measure(
                q.get(0),
                c.get(0)
            )
            .measure(
                q.get(1),
                c.get(0)
            )
            .measure(
                q.get(2),
                c.get(1)
            );

        final ProgramInspectionResult result = new QuantumProgramInspector().inspect(program);
        final CircuitInspectionSummary summary = result.circuitSummary(0);

        assertEquals(
            QuantumComputationModel.GATE_BASED_CIRCUIT,
            result.computationModel()
        );
        assertEquals(
            1,
            result.circuitCount()
        );
        assertEquals(
            3,
            result.qubitCount()
        );
        assertEquals(
            2,
            result.classicalBitCount()
        );
        assertEquals(
            10,
            result.operationCount()
        );
        assertEquals(
            5,
            result.gateCount()
        );
        assertEquals(
            3,
            result.measurementCount()
        );
        assertEquals(
            7,
            summary.approximateDepth()
        );
        assertTrue(summary.isDepthPrecise());
        assertEquals(
            1,
            summary.parameterizedGateCount()
        );
        assertEquals(
            1,
            summary.modifiedGateCount()
        );
        assertEquals(
            1,
            summary.twoQubitGateCount()
        );
        assertEquals(
            1,
            summary.multiQubitGateCount()
        );
        assertTrue(summary.neverMeasuredQubits().isEmpty());
        assertEquals(
            List.of("c[0]"),
            summary.overwrittenClassicalBits()
        );
        assertEquals(
            1,
            summary.gateHistogram().get("cx")
        );
        assertEquals(
            3,
            summary.operationKindHistogram().get(OperationKind.MEASURE)
        );
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void inspectsEveryCurrentOperationKindWithoutThrowing() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(GateDefinition.of(
            "custom_intrinsic",
            1,
            0
        ));
        program.addClassicalDeclaration(new ClassicalDeclaration(
            "globalCounter",
            ClassicalType.sized(
                ClassicalTypeKind.UNSIGNED_INTEGER,
                32
            )
        ));
        program.addCallableDefinition(new CallableDefinition(
            "prepare",
            CallableOperationBlock.of()
        ));
        program.addExternalCallableDeclaration(new ExternalCallableDeclaration(
            "externalCost",
            ClassicalType.sized(
                ClassicalTypeKind.FLOAT,
                64
            )
        ));
        program.addCalibrationDefinition(new CalibrationDefinition(
            "x",
            List.of(),
            List.of("q"),
            "pulse",
            "play q"
        ));
        final QuantumCircuit circuit = program.createCircuit("full_operation_surface");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        final ClassicalPredicate truePredicate = ClassicalPredicate.compare(
            ClassicalExpression.integer(1),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(1)
        );
        circuit.h(q.get(0))
            .measure(
                q.get(0),
                c.get(0)
            )
            .reset(q.get(1))
            .barrier(
                q.get(0),
                q.get(1)
            )
            .controlled(
                ClassicalCondition.equalTo(
                    c,
                    1L
                ),
                GateOperation.of(
                    StandardGate.X,
                    q.get(0)
                )
            )
            .assign(new ClassicalAssignment(
                ClassicalExpression.bit(c.get(0)),
                ClassicalExpression.integer(1)
            ))
            .classicalDeclaration(new ClassicalDeclarationOperation(
                new ClassicalDeclaration(
                    "localFlag",
                    ClassicalType.of(ClassicalTypeKind.BOOLEAN)
                ),
                ClassicalExpression.integer(1)
            ))
            .classicalArrayDeclaration(new ClassicalArrayDeclarationOperation(
                "localArray",
                ClassicalType.sized(
                    ClassicalTypeKind.UNSIGNED_INTEGER,
                    8
                ),
                List.of(ClassicalExpression.integer(4)),
                "{0, 1, 2, 3}"
            ))
            .callableInvocation(new CallableInvocationOperation(
                "prepare",
                null,
                List.of(ClassicalExpression.bit(c.get(0))),
                List.of(QuantumReference.staticQubit(q.get(0)))
            ))
            .classicallyControlled(
                truePredicate,
                GateOperation.of(
                    StandardGate.Y,
                    q.get(1)
                )
            )
            .block(OperationBlock.of(GateOperation.of(
                StandardGate.Z,
                q.get(0)
            )))
            .conditionalBlock(
                truePredicate,
                OperationBlock.of(GateOperation.of(
                    StandardGate.S,
                    q.get(0)
                )),
                OperationBlock.of(GateOperation.of(
                    StandardGate.T,
                    q.get(1)
                ))
            )
            .forLoop(
                "i",
                0L,
                1L,
                2L,
                OperationBlock.of(GateOperation.of(
                    StandardGate.H,
                    q.get(0)
                ))
            )
            .symbolicForLoop(new SymbolicForLoopOperation(
                "j",
                "uint[32]",
                ClassicalExpression.integer(0),
                ClassicalExpression.integer(1),
                ClassicalExpression.integer(2),
                OperationBlock.of(GateOperation.of(
                    StandardGate.X,
                    q.get(1)
                ))
            ))
            .whileLoop(
                truePredicate,
                OperationBlock.of(GateOperation.of(
                    StandardGate.Z,
                    q.get(1)
                ))
            )
            .delay(
                DurationExpression.duration(
                    10L,
                    DurationUnit.NS
                ),
                q.get(0)
            )
            .timingBox(
                DurationExpression.duration(
                    20L,
                    DurationUnit.NS
                ),
                OperationBlock.of(GateOperation.of(
                    StandardGate.H,
                    q.get(1)
                ))
            )
            .label("done")
            .branch(BranchOperation.always("done"))
            .halt()
            .waitInstruction();

        final ProgramInspectionResult result = new QuantumProgramInspector().inspect(program);
        final CircuitInspectionSummary summary = result.circuitSummary(0);

        for (final OperationKind kind : OperationKind.values()) {
            assertTrue(
                summary.operationKindHistogram().get(kind) > 0,
                "Missing operation kind in inspection histogram: " + kind
            );
        }
        assertEquals(
            2,
            result.callableDefinitionCount()
        );
        assertEquals(
            1,
            result.calibrationDefinitionCount()
        );
        assertEquals(
            6,
            result.controlOperationCount()
        );
        assertEquals(
            2,
            result.timingOperationCount()
        );
        assertEquals(
            1,
            result.callableInvocationCount()
        );
        assertFalse(summary.isDepthPrecise());
        assertTrue(result.diagnosticCount() >= 6);
        assertTrue(hasDiagnostic(
            result,
            InspectionDiagnosticCode.CONTROL_FLOW_DEPTH_APPROXIMATION
        ));
        assertTrue(hasDiagnostic(
            result,
            InspectionDiagnosticCode.TIMING_DEPTH_APPROXIMATION
        ));
    }

    @Test
    void reportsDynamicReferenceDepthAsStructuredDiagnostic() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("dynamic_depth");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            4
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.gateReferences(
            StandardGate.H,
            QuantumReference.dynamicIndex(
                q,
                ClassicalExpression.integer(2)
            )
        );

        final ProgramInspectionResult result = new QuantumProgramInspector().inspect(program);

        assertFalse(result.circuitSummary(0).isDepthPrecise());
        assertTrue(hasDiagnostic(
            result,
            InspectionDiagnosticCode.DYNAMIC_QUBIT_REFERENCE_DEPTH_APPROXIMATION
        ));
        assertTrue(result.circuitSummary(0).neverMeasuredQubits().contains("q[0]"));
        assertTrue(result.circuitSummary(0).neverMeasuredQubits().contains("q[3]"));
        assertEquals(
            1,
            result.classicalBitCount()
        );
        assertEquals(
            "c[0]",
            c.get(0).toString()
        );
    }

    @Test
    void includesTargetCompatibilitySummaryAndTargetDiagnostics() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("target_summary");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.x(q.get(0));
        final IntegrationCapabilityProfile profile = IntegrationCapabilityProfile.of(
            IntegrationFormat.OPENQASM_3,
            "Restricted test target",
            "1.0",
            IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
            EnumSet.of(IntegrationCapability.QUANTUM_REGISTERS),
            Set.of("h"),
            Set.of(),
            TargetConnectivityGraph.allToAll(),
            Map.of("purpose", "inspection-test")
        );

        final ProgramInspectionResult result = new QuantumProgramInspector().inspect(
            program,
            List.of(profile)
        );
        final TargetCompatibilitySummary summary = result.targetCompatibilitySummaries().get(0);

        assertEquals(
            "Restricted test target",
            summary.targetName()
        );
        assertEquals(
            "1.0",
            summary.targetVersion()
        );
        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            summary.status()
        );
        assertFalse(summary.isExportable());
        assertTrue(summary.diagnosticCount() > 0);
        assertTrue(hasTargetDiagnostic(
            result,
            "Restricted test target"
        ));
    }

    private static boolean hasDiagnostic(
        final ProgramInspectionResult result,
        final InspectionDiagnosticCode code
    ) {
        for (int i = 0; i < result.diagnostics().size(); i++) {
            if (result.diagnostics().get(i).code() == code) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTargetDiagnostic(
        final ProgramInspectionResult result,
        final String targetName
    ) {
        for (int i = 0; i < result.diagnostics().size(); i++) {
            final InspectionDiagnostic diagnostic = result.diagnostics().get(i);
            if (
                diagnostic.code() == InspectionDiagnosticCode.TARGET_COMPATIBILITY_WARNING
                && diagnostic.targetName().equals(targetName)
            ) {
                return true;
            }
        }
        return false;
    }
}