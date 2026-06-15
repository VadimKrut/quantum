/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compatibility;

import java.util.EnumSet;
import java.util.List;
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
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.application.workflow.ProductWorkflowOptions;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCompatibilityMatrixRunnerTest {

    @Test
    void buildsCompatibilityMatrixForValidProgramAndTargets() {
        final ProductCompatibilityMatrix matrix = new ProductCompatibilityMatrixRunner().run(
            bellProgram(),
            List.of(
                integration(IntegrationFormat.OPENQASM_2),
                integration(IntegrationFormat.OPENQASM_3)
            ),
            ProductWorkflowOptions.builder()
                .simulationOptions(SimulationOptions.builder()
                    .shots(64)
                    .seed(7L)
                    .build())
                .resourceMaxQubits(12)
                .runBackendDryRun(false)
                .build()
        );

        assertTrue(matrix.isSuccess());
        assertTrue(matrix.validation().isValid());
        assertTrue(matrix.simulation().isSuccess());
        assertEquals(
            4,
            matrix.inspection().operationCount()
        );
        assertEquals(
            2,
            matrix.targets().size()
        );
        for (TargetCompatibilityReport target : matrix.targets()) {
            assertEquals(
                TargetCompatibilityStatus.EXPORTABLE,
                target.status()
            );
            assertEquals(
                5,
                target.checks().size()
            );
            for (CompatibilityCheckResult check : target.checks()) {
                assertTrue(check.isSuccess());
            }
        }
    }

    @Test
    void reportsInvalidProgramWithoutTargetExecution() {
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

        final ProductCompatibilityMatrix matrix = new ProductCompatibilityMatrixRunner().run(
            program,
            List.of(integration(IntegrationFormat.QUIL))
        );

        assertFalse(matrix.isSuccess());
        assertFalse(matrix.validation().isValid());
        assertEquals(
            TargetCompatibilityStatus.INVALID_PROGRAM,
            matrix.targets().get(0).status()
        );
        assertEquals(
            CompatibilityCheckStatus.NOT_RUN,
            matrix.targets().get(0).checks().get(2).status()
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
                    "Compatibility Test " + format.name(),
                    "1",
                    IntegrationCapabilityProfile.UNBOUNDED_QUBIT_COUNT,
                    EnumSet.allOf(IntegrationCapability.class),
                    Set.of(StandardGate.H.gateName(), StandardGate.CX.gateName()),
                    EnumSet.allOf(ParameterExpressionKind.class),
                    TargetConnectivityGraph.allToAll(),
                    Map.of("test", "compatibility")
                );
            }

            @Override
            public ExportResult exportProgram(
                final QuantumProgram program,
                final ExportOptions options
            ) {
                return ExportResult.success(
                    format,
                    format.name()
                );
            }

            @Override
            public ImportResult importProgram(
                final String source,
                final ImportOptions options
            ) {
                throw new UnsupportedOperationException("Import is not used by compatibility tests.");
            }
        };
    }
}