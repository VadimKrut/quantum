/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.verification;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.simulation.options.SimulationOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossFormatVerificationRunnerTest {

    @Test
    void verifiesRoundTripThroughEveryTarget() {
        final CrossFormatVerificationReport report = new CrossFormatVerificationRunner().verify(
            "source",
            integration(IntegrationFormat.OPENQASM_2),
            new QuantumIntegration[] {
                integration(IntegrationFormat.OPENQASM_3),
                integration(IntegrationFormat.QUIL)
            },
            SimulationOptions.builder()
                .shots(64)
                .seed(3L)
                .build()
        );

        assertTrue(report.isSuccess());
        assertTrue(report.importSuccess());
        assertTrue(report.validation().isValid());
        assertEquals(
            2,
            report.targets().size()
        );
        for (int index = 0; index < report.targets().size(); index++) {
            assertTrue(report.targets().get(index).isSuccess());
            assertTrue(report.targets().get(index).simulationEquivalent());
        }
    }

    private static QuantumIntegration integration(final IntegrationFormat format) {
        return new QuantumIntegration() {

            @Override
            public IntegrationFormat format() {
                return format;
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
                    bellProgram(),
                    List.of()
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