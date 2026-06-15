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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DryRunQuantumBackendTest {

    @Test
    void submitsCompilesSimulatesAndReturnsCompletedResult() {
        final DryRunQuantumBackend backend = new DryRunQuantumBackend(
            "local-dry-run",
            "Local Dry Run",
            "1",
            exporter(fullProfile())
        );

        final BackendSubmissionResult submission = backend.submit(
            bellProgram(),
            BackendJobOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(128)
                    .seed(3L)
                    .build())
                .authenticationProfile("test-profile")
                .build()
        );

        assertTrue(submission.isAccepted());
        assertEquals(
            BackendJobStatus.COMPLETED,
            submission.status()
        );
        assertEquals(
            BackendJobStatus.COMPLETED,
            backend.status(submission.jobId()).status()
        );

        final BackendExecutionResult result = backend.result(submission.jobId());

        assertTrue(result.isSuccess());
        assertTrue(result.hasCompilerResult());
        assertTrue(result.hasSimulationResult());
        assertEquals(
            128,
            result.simulationResult().shots()
        );
        assertEquals(
            "test-profile",
            result.providerMetadata().get("authenticationProfile")
        );
    }

    @Test
    void rejectsProgramThatCannotPassBackendPreflight() {
        final DryRunQuantumBackend backend = new DryRunQuantumBackend(
            "restricted",
            "Restricted",
            "1",
            exporter(restrictedProfile())
        );
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("restricted");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            1
        );
        circuit.x(q.get(0));

        final BackendSubmissionResult submission = backend.submit(
            program,
            BackendJobOptions.defaults()
        );

        assertEquals(
            false,
            submission.isAccepted()
        );
        assertEquals(
            BackendDiagnosticCode.PREFLIGHT_FAILED,
            submission.diagnostics().get(0).code()
        );
    }

    @Test
    void cancellationOfCompletedDryRunJobReturnsWarning() {
        final DryRunQuantumBackend backend = new DryRunQuantumBackend(
            "local-dry-run",
            "Local Dry Run",
            "1",
            exporter(fullProfile())
        );
        final BackendSubmissionResult submission = backend.submit(
            bellProgram(),
            BackendJobOptions.defaults()
        );

        final BackendStatusResult cancelled = backend.cancel(submission.jobId());

        assertEquals(
            BackendJobStatus.COMPLETED,
            cancelled.status()
        );
        assertEquals(
            BackendDiagnosticCode.JOB_NOT_CANCELLABLE,
            cancelled.diagnostics().get(0).code()
        );
    }

    @Test
    void unknownJobIdIsRejected() {
        final DryRunQuantumBackend backend = new DryRunQuantumBackend(
            "local-dry-run",
            "Local Dry Run",
            "1",
            exporter(fullProfile())
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> backend.status(BackendJobId.of("missing"))
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
                "x",
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