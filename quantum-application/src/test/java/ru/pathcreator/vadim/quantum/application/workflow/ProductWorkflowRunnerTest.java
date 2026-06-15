/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.workflow;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.compiler.CompilerStageStatus;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapability;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.capability.TargetConnectivityGraph;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductWorkflowRunnerTest {

    @Test
    void runsCompleteProductWorkflowForBellProgram() {
        final ProductWorkflowReport report = new ProductWorkflowRunner().run(
            bellProgram(),
            fullIntegration(),
            ProductWorkflowOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(128)
                    .seed(11L)
                    .build())
                .resourceMaxQubits(12)
                .runBackendDryRun(true)
                .build()
        );

        assertTrue(report.isSuccess());
        assertEquals(
            ProductWorkflowStatus.COMPLETED,
            report.status()
        );
        assertEquals(
            IntegrationFormat.OPENQASM_3,
            report.targetFormat()
        );
        assertTrue(report.validation().isValid());
        assertEquals(
            4,
            report.inspection().operationCount()
        );
        assertTrue(report.preflight().isSuccess());
        assertEquals(
            64L,
            report.resources().estimatedStateVectorBytes()
        );
        assertEquals(
            1,
            report.timeline().circuits().size()
        );
        assertTrue(report.simulation().isSuccess());
        assertTrue(report.compiler().isSuccess());
        assertTrue(report.hasBackendSubmission());
        assertTrue(report.backendSubmission().isAccepted());
        assertTrue(report.hasBackendExecution());
        assertTrue(report.backendExecution().isSuccess());
    }

    @Test
    void reportsValidationFailureWithoutRunningTargetStages() {
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

        final ProductWorkflowReport report = new ProductWorkflowRunner().run(
            program,
            fullIntegration()
        );

        assertEquals(
            ProductWorkflowStatus.VALIDATION_FAILED,
            report.status()
        );
        assertTrue(!report.validation().isValid());
        assertEquals(
            1,
            report.inspection().operationCount()
        );
        assertEquals(
            1,
            report.timeline().circuits().size()
        );
        assertTrue(report.preflight() == null);
        assertTrue(report.simulation() == null);
        assertTrue(report.compiler() == null);
        assertTrue(!report.hasBackendSubmission());
    }

    @Test
    void canRunFastWorkflowWithoutOptionalAnalysisStages() {
        final ProductWorkflowReport report = new ProductWorkflowRunner().run(
            bellProgram(),
            fullIntegration(),
            ProductWorkflowOptions.builder()
                .fastWorkflow()
                .runBackendDryRun(false)
                .build()
        );

        assertTrue(report.isSuccess());
        assertEquals(
            ProductWorkflowStatus.COMPLETED,
            report.status()
        );
        assertTrue(report.validation() != null);
        assertTrue(report.inspection() == null);
        assertTrue(report.preflight() == null);
        assertTrue(report.resources() == null);
        assertTrue(report.timeline() == null);
        assertTrue(report.simulation() == null);
        assertTrue(report.compiler() != null);
        assertTrue(report.compiler().isSuccess());
        assertEquals(
            CompilerStageStatus.SKIPPED,
            report.compiler().stageRecords().get(0).status()
        );
        assertTrue(!report.hasBackendSubmission());
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

    private static QuantumIntegration fullIntegration() {
        return new QuantumIntegration() {

            @Override
            public IntegrationFormat format() {
                return IntegrationFormat.OPENQASM_3;
            }

            @Override
            public IntegrationCapabilityProfile capabilityProfile() {
                return IntegrationCapabilityProfile.of(
                    IntegrationFormat.OPENQASM_3,
                    "Product Test",
                    "1",
                    IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
                    EnumSet.allOf(IntegrationCapability.class),
                    Set.of(StandardGate.H.gateName(), StandardGate.CX.gateName()),
                    EnumSet.allOf(ParameterExpressionKind.class),
                    TargetConnectivityGraph.allToAll(),
                    Map.of("test", "product-workflow")
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
                throw new UnsupportedOperationException("Import is not used by product workflow runner tests.");
            }
        };
    }
}