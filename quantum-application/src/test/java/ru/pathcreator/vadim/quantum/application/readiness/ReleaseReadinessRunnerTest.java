/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.readiness;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.benchmark.ProductBenchmarkOptions;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.capability.TargetConnectivityGraph;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.regression.CorpusRegressionCase;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseReadinessRunnerTest {

    @Test
    void returnsReadyWhenCorpusBenchmarkAndProfilesPass() {
        final QuantumIntegration integration = integration(IntegrationFormat.OPENQASM_3);
        final ReleaseReadinessReport report = new ReleaseReadinessRunner().run(
            List.of(CorpusRegressionCase.of(
                "bell",
                "source",
                integration
            )),
            List.of(integration),
            integration,
            ProductBenchmarkOptions.builder()
                .warmupIterations(0)
                .measurementIterations(1)
                .workflowOptions(ProductWorkflowOptions.builder()
                    .simulationOptions(SimulationOptions.builder()
                        .shots(32)
                        .seed(8L)
                        .build())
                    .runBackendDryRun(false)
                    .build())
                .build()
        );

        assertEquals(
            ReleaseReadinessStatus.READY,
            report.status()
        );
        assertTrue(report.isReady());
        assertEquals(
            0,
            report.failedCheckCount()
        );
        assertTrue(report.corpusRegression().isSuccess());
        assertTrue(report.hasBenchmark());
        assertTrue(report.benchmark().isSuccess());
    }

    private static QuantumIntegration integration(final IntegrationFormat format) {
        return new QuantumIntegration() {

            @Override
            public IntegrationFormat format() {
                return format;
            }

            @Override
            public IntegrationCapabilityProfile capabilityProfile() {
                return IntegrationCapabilityProfile.of(
                    format,
                    "Readiness Test " + format.name(),
                    "1",
                    IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
                    EnumSet.allOf(IntegrationCapability.class),
                    Set.of(StandardGate.H.gateName(), StandardGate.CX.gateName()),
                    EnumSet.allOf(ParameterExpressionKind.class),
                    TargetConnectivityGraph.allToAll(),
                    Map.of("test", "readiness")
                );
            }

            @Override
            public ExportResult exportProgram(
                final QuantumProgram program,
                final ExportOptions options
            ) {
                return ExportResult.success(
                    format,
                    "roundtrip"
                );
            }

            @Override
            public ImportResult importProgram(
                final String source,
                final ImportOptions options
            ) {
                return ImportResult.success(
                    format,
                    bellProgram()
                );
            }
        };
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
}