/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.benchmark;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.capability.TargetConnectivityGraph;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductBenchmarkRunnerTest {

    @Test
    void benchmarksEveryProductStageForProgram() {
        final ProductBenchmarkReport report = new ProductBenchmarkRunner().run(
            bellProgram(),
            integration(),
            ProductBenchmarkOptions.builder()
                .warmupIterations(0)
                .measurementIterations(1)
                .workflowOptions(ProductWorkflowOptions.builder()
                    .runBackendDryRun(false)
                    .build())
                .build()
        );

        assertTrue(report.isSuccess());
        assertEquals(
            8,
            report.stageCount()
        );
        assertEquals(
            "validate",
            report.stages().get(0).stage()
        );
        assertEquals(
            "workflow",
            report.stages().get(7).stage()
        );
        assertTrue(report.totalAverageNanos() >= 0L);
        for (BenchmarkStageResult stage : report.stages()) {
            assertEquals(
                1,
                stage.measurementIterations()
            );
            assertTrue(stage.maxNanos() >= stage.minNanos());
        }
    }

    @Test
    void benchmarksExternalImportWhenSourceIsProvided() {
        final ProductBenchmarkReport report = new ProductBenchmarkRunner().runExternal(
            "ignored",
            integration(),
            integration(),
            ProductBenchmarkOptions.builder()
                .warmupIterations(0)
                .measurementIterations(1)
                .workflowOptions(ProductWorkflowOptions.builder()
                    .runBackendDryRun(false)
                    .build())
                .build()
        );

        assertTrue(report.isSuccess());
        assertTrue(report.hasInputFormat());
        assertEquals(
            IntegrationFormat.OPENQASM_3,
            report.inputFormat()
        );
        assertEquals(
            "import",
            report.stages().get(0).stage()
        );
        assertEquals(
            9,
            report.stageCount()
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

    private static QuantumIntegration integration() {
        return new QuantumIntegration() {

            @Override
            public IntegrationFormat format() {
                return IntegrationFormat.OPENQASM_3;
            }

            @Override
            public IntegrationCapabilityProfile capabilityProfile() {
                return IntegrationCapabilityProfile.of(
                    IntegrationFormat.OPENQASM_3,
                    "Benchmark Test",
                    "1",
                    IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
                    EnumSet.allOf(IntegrationCapability.class),
                    Set.of(StandardGate.H.gateName(), StandardGate.CX.gateName()),
                    EnumSet.allOf(ParameterExpressionKind.class),
                    TargetConnectivityGraph.allToAll(),
                    Map.of("test", "benchmark")
                );
            }

            @Override
            public ExportResult exportProgram(
                final QuantumProgram program,
                final ExportOptions options
            ) {
                return ExportResult.success(
                    IntegrationFormat.OPENQASM_3,
                    "OPENQASM 3.0;"
                );
            }

            @Override
            public ImportResult importProgram(
                final String source,
                final ImportOptions options
            ) {
                return ImportResult.success(
                    IntegrationFormat.OPENQASM_3,
                    bellProgram()
                );
            }
        };
    }
}