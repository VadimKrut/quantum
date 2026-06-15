/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.capability.TargetConnectivityGraph;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumExporter;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryBackendJobRegistryTest {

    @Test
    void tracksAcceptedDryRunJobWithExecutionSnapshot() {
        final InMemoryBackendJobRegistry registry = new InMemoryBackendJobRegistry();
        final DryRunQuantumBackend backend = new DryRunQuantumBackend(
            "local-dry-run",
            "Local Dry Run",
            "1",
            exporter(fullProfile())
        );

        final BackendJobRecord record = registry.submit(
            backend,
            bellProgram(),
            BackendJobOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(64)
                    .seed(11L)
                    .build())
                .build()
        );

        assertTrue(record.isAccepted());
        assertTrue(record.hasBackendJobId());
        assertTrue(record.hasExecutionResult());
        assertEquals(
            BackendJobStatus.COMPLETED,
            record.status()
        );
        assertEquals(
            64,
            record.executionResult().simulationResult().shots()
        );
        assertEquals(
            1,
            registry.history().count()
        );
        assertEquals(
            record.trackingId(),
            registry.history().records().get(0).trackingId()
        );
    }

    @Test
    void tracksRejectedSubmissionWithoutBackendJobId() {
        final InMemoryBackendJobRegistry registry = new InMemoryBackendJobRegistry();
        final DryRunQuantumBackend backend = new DryRunQuantumBackend(
            "restricted",
            "Restricted",
            "1",
            exporter(restrictedProfile())
        );

        final BackendJobRecord record = registry.submit(
            backend,
            bellProgram(),
            BackendJobOptions.defaults()
        );

        assertFalse(record.isAccepted());
        assertFalse(record.hasBackendJobId());
        assertFalse(record.hasExecutionResult());
        assertEquals(
            BackendJobStatus.FAILED,
            record.status()
        );
        assertEquals(
            BackendDiagnosticCode.PREFLIGHT_FAILED,
            record.diagnostics().get(0).code()
        );
        assertThrows(
            IllegalStateException.class,
            record::backendJobId
        );
    }

    @Test
    void preservesSubmissionOrderAndSupportsClear() {
        final InMemoryBackendJobRegistry registry = new InMemoryBackendJobRegistry();
        final DryRunQuantumBackend backend = new DryRunQuantumBackend(
            "local-dry-run",
            "Local Dry Run",
            "1",
            exporter(fullProfile())
        );

        final BackendJobRecord first = registry.submit(
            backend,
            bellProgram(),
            BackendJobOptions.defaults()
        );
        final BackendJobRecord second = registry.submit(
            backend,
            bellProgram(),
            BackendJobOptions.defaults()
        );

        assertEquals(
            first.trackingId(),
            registry.history().records().get(0).trackingId()
        );
        assertEquals(
            second.trackingId(),
            registry.history().records().get(1).trackingId()
        );

        registry.clear();

        assertTrue(registry.history().isEmpty());
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.record(first.trackingId())
        );
    }

    private static QuantumProgram bellProgram() {
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

    private static QuantumExporter exporter(final IntegrationCapabilityProfile profile) {
        return new QuantumExporter() {
            @Override
            public IntegrationFormat format() {
                return profile.format();
            }

            @Override
            public IntegrationCapabilityProfile capabilityProfile() {
                return profile;
            }

            @Override
            public ExportResult exportProgram(
                final QuantumProgram program,
                final ExportOptions options
            ) {
                return ExportResult.success(
                    profile.format(),
                    "exported"
                );
            }
        };
    }

    private static IntegrationCapabilityProfile fullProfile() {
        return IntegrationCapabilityProfile.of(
            IntegrationFormat.OPENQASM_3,
            "DryRun",
            "1",
            IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
            EnumSet.allOf(IntegrationCapability.class),
            Set.of(
                "h",
                "cx",
                "measure"
            ),
            Set.of(
                ParameterExpressionKind.NUMERIC,
                ParameterExpressionKind.KNOWN_CONSTANT,
                ParameterExpressionKind.NAMED,
                ParameterExpressionKind.UNARY,
                ParameterExpressionKind.BINARY
            ),
            TargetConnectivityGraph.allToAll(),
            Map.of()
        );
    }

    private static IntegrationCapabilityProfile restrictedProfile() {
        final EnumSet<IntegrationCapability> capabilities = EnumSet.allOf(IntegrationCapability.class);
        capabilities.remove(IntegrationCapability.GATE_DECOMPOSITION);
        return IntegrationCapabilityProfile.of(
            IntegrationFormat.OPENQASM_3,
            "Restricted",
            "1",
            IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
            capabilities,
            Set.of("h"),
            Set.of(ParameterExpressionKind.NUMERIC),
            TargetConnectivityGraph.allToAll(),
            Map.of()
        );
    }
}