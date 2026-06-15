/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.capability;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.QuantumReference;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapabilityPreflightCheckerTest {

    @Test
    void passesWhenProfileSupportsRequiredFeatures() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.reset(q.get(0));

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.OPENQASM_2,
                EnumSet.of(
                    IntegrationCapability.QUANTUM_REGISTERS,
                    IntegrationCapability.RESET
                )
            )
        );

        assertTrue(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.EXPORTABLE,
            result.status()
        );
    }

    @Test
    void reportsMissingTargetCapability() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.reset(q.get(0));

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.empty(IntegrationFormat.OPENQASM_2)
        );

        assertFalse(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            result.status()
        );
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_TARGET_CAPABILITY,
            result.diagnostics().get(0).code()
        );
    }

    @Test
    void reportsDynamicQubitReferencesWhenTargetDoesNotSupportThem() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister buffer = circuit.createQuantumRegister(
            "buffer",
            4
        );
        circuit.gateReferences(
            StandardGate.H,
            QuantumReference.dynamicIndex(
                buffer,
                ClassicalExpression.variable("address")
            )
        );

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.QUIL,
                EnumSet.of(IntegrationCapability.QUANTUM_REGISTERS)
            )
        );

        assertFalse(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            result.status()
        );
        assertEquals(
            IntegrationDiagnosticCode.UNSUPPORTED_TARGET_CAPABILITY,
            result.diagnostics().get(0).code()
        );
        assertTrue(result.diagnostics().get(0).message().contains("dynamic qubit references"));
    }

    @Test
    void reportsSemanticLossForMissingCallableSupport() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.addExternalCallableDeclaration(new ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration(
            "external_job",
            null
        ));

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.empty(IntegrationFormat.OPENQASM_2)
        );

        assertFalse(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_WITHOUT_LOSS,
            result.status()
        );
    }

    @Test
    void exposesCompleteTargetProfileMetadata() {
        final IntegrationCapabilityProfile profile = IntegrationCapabilityProfile.of(
            IntegrationFormat.OPENQASM_3,
            "test-target",
            "1.2.3",
            8,
            EnumSet.of(IntegrationCapability.QUANTUM_REGISTERS),
            Set.of("h"),
            EnumSet.of(ParameterExpressionKind.NUMERIC),
            TargetConnectivityGraph.undirected(new long[][] {
                new long[] {
                    0,
                    1
                }
            }),
            Map.of(
                "vendor",
                "test"
            )
        );

        assertEquals(
            "test-target",
            profile.targetName()
        );
        assertEquals(
            "1.2.3",
            profile.targetVersion()
        );
        assertEquals(
            8,
            profile.maxQubitCount()
        );
        assertTrue(profile.supportsNativeGate("h"));
        assertTrue(profile.supportsParameterKind(ParameterExpressionKind.NUMERIC));
        assertTrue(profile.connectivityGraph().supportsInteraction(
            1,
            0
        ));
        assertEquals(
            "test",
            profile.metadata().get("vendor")
        );
    }

    @Test
    void reportsQubitLimitViolation() {
        final QuantumProgram program = QuantumProgram.gateBased();
        program.createCircuit("main")
            .createQuantumRegister(
                "q",
                3
            );

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.OPENQASM_3,
                "limited",
                "1",
                2,
                EnumSet.of(IntegrationCapability.QUANTUM_REGISTERS),
                Set.of(),
                Set.of(),
                TargetConnectivityGraph.allToAll(),
                Map.of()
            )
        );

        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            result.status()
        );
        assertTrue(result.diagnostics().get(0).message().contains("qubit count"));
    }

    @Test
    void reportsUnsupportedNativeGateWithoutLowering() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.x(q.get(0));

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.OPENQASM_3,
                "native-only",
                "1",
                IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
                EnumSet.of(IntegrationCapability.QUANTUM_REGISTERS),
                Set.of("h"),
                Set.of(),
                TargetConnectivityGraph.allToAll(),
                Map.of()
            )
        );

        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            result.status()
        );
        assertTrue(result.diagnostics().get(0).message().contains("native gate x"));
    }

    @Test
    void marksLoweringRequiredForNonnativeGateWhenDecompositionIsAvailable() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.x(q.get(0));

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.OPENQASM_3,
                "lowering",
                "1",
                IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
                EnumSet.of(
                    IntegrationCapability.QUANTUM_REGISTERS,
                    IntegrationCapability.GATE_DECOMPOSITION
                ),
                Set.of("h"),
                Set.of(),
                TargetConnectivityGraph.allToAll(),
                Map.of()
            )
        );

        assertTrue(result.isSuccess());
        assertEquals(
            CapabilityPreflightStatus.LOWERING_REQUIRED,
            result.status()
        );
    }

    @Test
    void reportsUnsupportedParameterKind() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.rz(
            ParameterExpression.named("theta"),
            q.get(0)
        );

        final CapabilityPreflightResult result = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.OPENQASM_3,
                "numeric-only",
                "1",
                IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
                EnumSet.of(IntegrationCapability.QUANTUM_REGISTERS),
                Set.of("rz"),
                EnumSet.of(ParameterExpressionKind.NUMERIC),
                TargetConnectivityGraph.allToAll(),
                Map.of()
            )
        );

        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            result.status()
        );
        assertTrue(result.diagnostics().get(0).message().contains("NAMED"));
    }

    @Test
    void checksConnectivityAndAllowsLoweringWhenAvailable() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("main");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        circuit.cx(
            q.get(0),
            q.get(2)
        );

        final CapabilityPreflightResult unsupported = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.OPENQASM_3,
                "line",
                "1",
                3,
                EnumSet.of(IntegrationCapability.QUANTUM_REGISTERS),
                Set.of("cx"),
                Set.of(),
                TargetConnectivityGraph.undirected(new long[][] {
                    new long[] {
                        0,
                        1
                    },
                    new long[] {
                        1,
                        2
                    }
                }),
                Map.of()
            )
        );
        final CapabilityPreflightResult lowering = new CapabilityPreflightChecker().check(
            program,
            IntegrationCapabilityProfile.of(
                IntegrationFormat.OPENQASM_3,
                "line",
                "1",
                3,
                EnumSet.of(
                    IntegrationCapability.QUANTUM_REGISTERS,
                    IntegrationCapability.GATE_DECOMPOSITION
                ),
                Set.of("cx"),
                Set.of(),
                TargetConnectivityGraph.undirected(new long[][] {
                    new long[] {
                        0,
                        1
                    },
                    new long[] {
                        1,
                        2
                    }
                }),
                Map.of()
            )
        );

        assertEquals(
            CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET,
            unsupported.status()
        );
        assertEquals(
            CapabilityPreflightStatus.LOWERING_REQUIRED,
            lowering.status()
        );
    }
}