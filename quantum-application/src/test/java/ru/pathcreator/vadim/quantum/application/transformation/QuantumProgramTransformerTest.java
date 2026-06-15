/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.transformation;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.capability.TargetConnectivityGraph;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecomposition;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRegistry;
import ru.pathcreator.vadim.quantum.application.integration.decomposition.GateDecompositionRule;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.domain.gate.Gate;
import ru.pathcreator.vadim.quantum.domain.gate.GateBodyOperation;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.OperationKind;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.parameter.ParameterBindings;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumProgramTransformerTest {

    @Test
    void copiesProgramWithoutMutatingSourceWhenNoStepIsEnabled() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("copy_only");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.h(q.get(0));

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.none()
        );

        assertSame(
            program,
            result.originalProgram()
        );
        assertNotSame(
            program,
            result.transformedProgram()
        );
        assertEquals(
            1,
            program.circuit(0).operationCount()
        );
        assertEquals(
            StandardGate.H,
            ((GateOperation) result.transformedProgram().circuit(0).operation(0)).gate()
        );
        assertFalse(result.skippedSteps().isEmpty());
    }

    @Test
    void bindsAndCanonicalizesParameterTreesWithoutChangingSource() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("params");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        final ParameterExpression original = ParameterExpression.add(
            ParameterExpression.named("theta"),
            ParameterExpression.divide(
                ParameterExpression.pi(),
                ParameterExpression.of(2.0)
            )
        );
        circuit.rz(
            original,
            q.get(0)
        );

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.builder()
                .bindParameters(ParameterBindings.builder()
                    .put(
                        "theta",
                        0.5
                    )
                    .build())
                .canonicalizeParameterExpressions()
                .build()
        );
        final GateOperation transformed = (GateOperation) result.transformedProgram().circuit(0).operation(0);

        assertTrue(result.isSuccess());
        assertEquals(
            ParameterExpressionKind.NUMERIC,
            transformed.parameter(0).kind()
        );
        assertEquals(
            0.5 + Math.PI / 2.0,
            transformed.parameter(0).numericValue(),
            0.0000000001
        );
        assertEquals(
            original,
            ((GateOperation) program.circuit(0).operation(0)).parameter(0)
        );
        assertTrue(containsStep(
            result.appliedSteps(),
            TransformationStep.PARAMETER_BINDING
        ));
    }

    @Test
    void reportsUnboundParameterWhenCompleteBindingIsRequired() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("unbound");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.rx(
            ParameterExpression.named("theta"),
            q.get(0)
        );

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.builder()
                .bindParameters(ParameterBindings.empty())
                .requireCompleteParameterBinding()
                .build()
        );

        assertTrue(result.hasErrors());
        assertEquals(
            TransformationDiagnosticCode.UNBOUND_PARAMETER_SYMBOL,
            result.diagnostics().get(0).code()
        );
    }

    @Test
    void removesIdentityGateOnlyWhenRequested() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("identity");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.id(q.get(0));
        circuit.x(q.get(0));

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.builder()
                .removeIdentityGates()
                .build()
        );

        assertEquals(
            2,
            program.circuit(0).operationCount()
        );
        assertEquals(
            1,
            result.transformedProgram().circuit(0).operationCount()
        );
        assertEquals(
            StandardGate.X,
            ((GateOperation) result.transformedProgram().circuit(0).operation(0)).gate()
        );
        assertTrue(containsStep(
            result.appliedSteps(),
            TransformationStep.IDENTITY_GATE_REMOVAL
        ));
    }

    @Test
    void inlinesCompositeGateWithSymbolicParameterSubstitution() {
        final GateDefinition composite = GateDefinition.composite(
            "phase_pair",
            List.of("theta"),
            List.of(
                "a",
                "b"
            ),
            List.of(
                GateBodyOperation.of(
                    StandardGate.RZ,
                    new ParameterExpression[] {ParameterExpression.named("theta")},
                    "a"
                ),
                GateBodyOperation.of(
                    StandardGate.CX,
                    new ParameterExpression[0],
                    "a",
                    "b"
                )
            )
        );
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(composite);
        final QuantumCircuit circuit = program.createCircuit("composite");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        circuit.parameterizedGate(
            composite,
            new ParameterExpression[] {ParameterExpression.named("phi")},
            q.get(0),
            q.get(1)
        );

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.builder()
                .inlineCompositeGates()
                .build()
        );
        final QuantumCircuit transformed = result.transformedProgram().circuit(0);

        assertEquals(
            2,
            transformed.operationCount()
        );
        assertEquals(
            StandardGate.RZ,
            ((GateOperation) transformed.operation(0)).gate()
        );
        assertEquals(
            ParameterExpression.named("phi"),
            ((GateOperation) transformed.operation(0)).parameter(0)
        );
        assertEquals(
            StandardGate.CX,
            ((GateOperation) transformed.operation(1)).gate()
        );
        assertEquals(
            1,
            program.circuit(0).operationCount()
        );
    }

    @Test
    void skipsCompositeInliningForDynamicReferencesWithoutFailingTransformation() {
        final GateDefinition composite = GateDefinition.composite(
            "dynamic_safe",
            List.of(),
            List.of("a"),
            List.of(GateBodyOperation.of(
                StandardGate.H,
                new ParameterExpression[0],
                "a"
            ))
        );
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(composite);
        final QuantumCircuit circuit = program.createCircuit("dynamic");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        circuit.createClassicalRegister(
            "c",
            1
        );
        circuit.gateReferences(
            composite,
            QuantumReference.dynamicIndex(
                q,
                ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression.integer(1)
            )
        );

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.builder()
                .inlineCompositeGates()
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            composite,
            ((GateOperation) result.transformedProgram().circuit(0).operation(0)).gate()
        );
        assertEquals(
            TransformationDiagnosticCode.NON_STATIC_COMPOSITE_GATE_OPERATION,
            result.diagnostics().get(0).code()
        );
        assertTrue(containsStep(
            result.skippedSteps(),
            TransformationStep.COMPOSITE_GATE_INLINING
        ));
    }

    @Test
    void appliesOnlyDeclaredDecompositionRules() {
        final GateDefinition custom = GateDefinition.of(
            "custom_h",
            1,
            0
        );
        final QuantumProgram program = singleCustomGateProgram(custom);
        final GateDecompositionRegistry registry = GateDecompositionRegistry.of(List.of(new GateDecompositionRule() {

            @Override
            public boolean supports(final Gate gate) {
                return gate == custom;
            }

            @Override
            public GateDecomposition decompose(final GateOperation operation) {
                return GateDecomposition.of(List.of(GateOperation.ofReferences(
                    StandardGate.H,
                    operation.qubitReferences()
                )));
            }
        }));

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.builder()
                .applyDeclaredDecompositions(registry)
                .build()
        );

        assertTrue(result.isSuccess());
        assertEquals(
            StandardGate.H,
            ((GateOperation) result.transformedProgram().circuit(0).operation(0)).gate()
        );
        assertTrue(containsStep(
            result.appliedSteps(),
            TransformationStep.DECLARED_GATE_DECOMPOSITION
        ));
    }

    @Test
    void targetAwareLoweringReportsMissingRuleInsteadOfSilentFallback() {
        final GateDefinition custom = GateDefinition.of(
            "custom_x",
            1,
            0
        );
        final QuantumProgram program = singleCustomGateProgram(custom);

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.builder()
                .targetAwareLowering(nativeOnlyProfile(Set.of(StandardGate.H.gateName())))
                .build()
        );

        assertTrue(result.hasErrors());
        assertEquals(
            TransformationDiagnosticCode.MISSING_GATE_DECOMPOSITION_RULE,
            result.diagnostics().get(result.diagnostics().size() - 1).code()
        );
        assertTrue(containsStep(
            result.skippedSteps(),
            TransformationStep.TARGET_AWARE_LOWERING
        ));
    }

    @Test
    void transformsNestedBlockOperations() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("nested");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.block(OperationBlock.of(GateOperation.parameterizedReferences(
            StandardGate.RZ,
            new ParameterExpression[] {ParameterExpression.pi()},
            QuantumReference.staticQubit(q.get(0))
        )));

        final TransformationResult result = new QuantumProgramTransformer().transform(
            program,
            TransformationOptions.builder()
                .canonicalizeParameterExpressions()
                .build()
        );
        final OperationBlock body = ((ru.pathcreator.vadim.quantum.domain.operation.BlockOperation) result
            .transformedProgram()
            .circuit(0)
            .operation(0)).body();

        assertEquals(
            OperationKind.GATE,
            body.operation(0).kind()
        );
        assertEquals(
            ParameterExpressionKind.NUMERIC,
            ((GateOperation) body.operation(0)).parameter(0).kind()
        );
    }

    private static QuantumProgram singleCustomGateProgram(final GateDefinition gate) {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addGateDefinition(gate);
        final QuantumCircuit circuit = program.createCircuit("custom");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.gate(
            gate,
            q.get(0)
        );
        return program;
    }

    private static IntegrationCapabilityProfile nativeOnlyProfile(final Set<String> nativeGateNames) {
        return IntegrationCapabilityProfile.of(
            IntegrationFormat.OPENQASM_3,
            "unit-test-target",
            "1",
            IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
            EnumSet.of(
                IntegrationCapability.QUANTUM_REGISTERS,
                IntegrationCapability.CLASSICAL_REGISTERS,
                IntegrationCapability.MEASUREMENTS,
                IntegrationCapability.RESET,
                IntegrationCapability.BARRIER,
                IntegrationCapability.GATE_DECOMPOSITION
            ),
            nativeGateNames,
            Set.of(),
            TargetConnectivityGraph.allToAll(),
            Map.of()
        );
    }

    private static boolean containsStep(
        final List<TransformationStepRecord> records,
        final TransformationStep step
    ) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).step() == step) {
                return true;
            }
        }
        return false;
    }
}