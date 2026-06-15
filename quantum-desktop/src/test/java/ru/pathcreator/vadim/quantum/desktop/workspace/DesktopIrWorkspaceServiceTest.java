/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.desktop.workflow.DesktopExecutionOptions;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

class DesktopIrWorkspaceServiceTest {

    private final DesktopIrWorkspaceService service = new DesktopIrWorkspaceService();

    @Test
    void buildsNativeIrWorkspaceAndRunsCoreActions() {
        final QuantumProgram program = service.buildProgram(
            "main",
            "q",
            2,
            "c",
            2,
            List.of(
                new DesktopIrOperationSpec(
                    "H",
                    "q[0]",
                    "q[1]",
                    "q[0]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "CX",
                    "q[0]",
                    "q[1]",
                    "q[0]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "MEASURE",
                    "q[0]",
                    "q[1]",
                    "q[0]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "MEASURE",
                    "q[1]",
                    "q[0]",
                    "q[0]",
                    "c[1]",
                    Math.PI / 2.0
                )
            )
        );

        assertTrue(service.validate(program).isValid());
        assertEquals(
            4,
            program.circuit(0).operationCount()
        );
        assertTrue(service.writeJson(program).content().contains("\"computationModel\""));
        assertTrue(service.readJson(service.writeJson(program).content()).isSuccess());
        assertTrue(service.timeline(program).circuits().get(0).steps().size() >= 4);
        assertEquals(
            1,
            service.inspect(
                program,
                IntegrationFormat.OPENQASM_3
            ).circuitCount()
        );
        assertEquals(
            2,
            service.resources(
                program,
                24
            ).qubitCount()
        );
        assertTrue(service.preflight(
            program,
            IntegrationFormat.OPENQASM_3
        ).isSuccess());
        assertTrue(service.compatibility(
            program,
            128,
            7L,
            DesktopExecutionOptions.defaults()
        ).validation().isValid());
        assertTrue(service.transform(
            program,
            IntegrationFormat.OPENQASM_3,
            true,
            true,
            false,
            true
        ).isSuccess());
        assertTrue(service.generateJavaDsl(
            "main",
            "q",
            2,
            "c",
            2,
            programOperations()
        ).contains(".cx(\"q[0]\", \"q[1]\")"));
        assertTrue(service.simulate(
            program,
            128,
            7L
        ).isSuccess());
        assertTrue(service.export(
            program,
            IntegrationFormat.OPENQASM_3,
            DesktopExecutionOptions.defaults()
        ).isSuccess());
    }

    private static List<DesktopIrOperationSpec> programOperations() {
        return List.of(
            new DesktopIrOperationSpec(
                "H",
                "q[0]",
                "q[1]",
                "q[0]",
                "c[0]",
                Math.PI / 2.0
            ),
            new DesktopIrOperationSpec(
                "CX",
                "q[0]",
                "q[1]",
                "q[0]",
                "c[0]",
                Math.PI / 2.0
            )
        );
    }

    @Test
    void buildsRicherGraphicalOperationStream() {
        final QuantumProgram program = service.buildProgram(
            "graphical",
            "q",
            3,
            "c",
            3,
            List.of(
                new DesktopIrOperationSpec(
                    "RY",
                    "q[0]",
                    "q[1]",
                    "q[2]",
                    "c[0]",
                    Math.PI / 3.0
                ),
                new DesktopIrOperationSpec(
                    "CZ",
                    "q[0]",
                    "q[1]",
                    "q[2]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "CCX",
                    "q[0]",
                    "q[1]",
                    "q[2]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "BARRIER",
                    "q[0]",
                    "q[2]",
                    "q[1]",
                    "c[0]",
                    Math.PI / 2.0
                ),
                new DesktopIrOperationSpec(
                    "MEASURE",
                    "q[2]",
                    "q[0]",
                    "q[1]",
                    "c[2]",
                    Math.PI / 2.0
                )
            )
        );

        assertTrue(service.validate(program).isValid());
        assertEquals(
            5,
            program.circuit(0).operationCount()
        );
        assertTrue(service.writeJson(program).content().contains("\"graphical\""));
        assertTrue(service.export(
            program,
            IntegrationFormat.OPENQASM_3,
            DesktopExecutionOptions.defaults()
        ).isSuccess());
    }

    @Test
    void buildsDenseGateSpectrumOperationStream() {
        final List<DesktopIrOperationSpec> operations = denseGateSpectrumOperations();
        final QuantumProgram program = service.buildProgram(
            "dense",
            "q",
            5,
            "c",
            5,
            operations
        );

        assertTrue(service.validate(program).isValid());
        assertEquals(
            operations.size(),
            program.circuit(0).operationCount()
        );
        assertEquals(
            5,
            service.resources(
                program,
                24
            ).qubitCount()
        );
        assertTrue(service.writeJson(program).hasContent());
        assertTrue(service.readJson(service.writeJson(program).content()).isSuccess());
        assertTrue(service.preflight(
            program,
            IntegrationFormat.OPENQASM_3
        ).isSuccess());
        assertTrue(service.export(
            program,
            IntegrationFormat.OPENQASM_3,
            DesktopExecutionOptions.defaults()
        ).isSuccess());
    }

    @Test
    void denseGateSpectrumSurvivesDesktopCrossSystemWorkflows() {
        final QuantumProgram program = service.buildProgram(
            "dense",
            "q",
            5,
            "c",
            5,
            denseGateSpectrumOperations()
        );
        final String json = service.writeJson(program).content();
        final QuantumProgram fromJson = service.readJson(json).program();

        assertTrue(service.validate(fromJson).isValid());
        assertTrue(service.simulate(
            fromJson,
            128,
            7L
        ).isSuccess());
        for (IntegrationFormat format : IntegrationFormat.values()) {
            assertTrue(service.targetProfile(format).format() == format);
            assertTrue(service.preflight(
                fromJson,
                format
            ).isSuccess());
            assertTrue(service.export(
                fromJson,
                format,
                DesktopExecutionOptions.defaults()
            ).isSuccess());
        }
    }

    private static List<DesktopIrOperationSpec> denseGateSpectrumOperations() {
        final java.util.ArrayList<DesktopIrOperationSpec> operations = new java.util.ArrayList<>();
        for (int round = 0; round < 4; round++) {
            operations.add(operation(
                "H",
                "q[0]"
            ));
            operations.add(operation(
                "X",
                "q[1]"
            ));
            operations.add(operation(
                "Y",
                "q[2]"
            ));
            operations.add(operation(
                "Z",
                "q[3]"
            ));
            operations.add(operation(
                "S",
                "q[4]"
            ));
            operations.add(operation(
                "T",
                "q[0]"
            ));
            operations.add(rotation(
                "RX",
                "q[1]",
                Math.PI / (round + 2.0)
            ));
            operations.add(rotation(
                "RY",
                "q[2]",
                Math.PI / (round + 3.0)
            ));
            operations.add(rotation(
                "RZ",
                "q[3]",
                Math.PI / (round + 4.0)
            ));
            operations.add(rotation(
                "PHASE",
                "q[4]",
                Math.PI / (round + 5.0)
            ));
            operations.add(operation(
                "CX",
                "q[0]",
                "q[1]"
            ));
            operations.add(operation(
                "CY",
                "q[1]",
                "q[2]"
            ));
            operations.add(operation(
                "CZ",
                "q[2]",
                "q[3]"
            ));
            operations.add(operation(
                "CH",
                "q[3]",
                "q[4]"
            ));
            operations.add(operation(
                "SWAP",
                "q[0]",
                "q[4]"
            ));
            operations.add(new DesktopIrOperationSpec(
                "CCX",
                "q[0]",
                "q[2]",
                "q[4]",
                "c[0]",
                Math.PI / 2.0
            ));
            operations.add(operation(
                "BARRIER",
                "q[1]",
                "q[3]"
            ));
        }
        operations.add(operation(
            "RESET",
            "q[4]"
        ));
        operations.add(measure(
            "q[0]",
            "c[0]"
        ));
        operations.add(measure(
            "q[1]",
            "c[1]"
        ));
        operations.add(measure(
            "q[2]",
            "c[2]"
        ));
        operations.add(measure(
            "q[3]",
            "c[3]"
        ));
        operations.add(measure(
            "q[4]",
            "c[4]"
        ));
        return operations;
    }

    private static DesktopIrOperationSpec operation(
        final String gate,
        final String primaryQubit
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            "q[0]",
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        );
    }

    private static DesktopIrOperationSpec operation(
        final String gate,
        final String primaryQubit,
        final String secondaryQubit
    ) {
        return new DesktopIrOperationSpec(
            gate,
            primaryQubit,
            secondaryQubit,
            "q[0]",
            "c[0]",
            Math.PI / 2.0
        );
    }

    private static DesktopIrOperationSpec rotation(
        final String gate,
        final String qubit,
        final double angle
    ) {
        return new DesktopIrOperationSpec(
            gate,
            qubit,
            "q[0]",
            "q[0]",
            "c[0]",
            angle
        );
    }

    private static DesktopIrOperationSpec measure(
        final String qubit,
        final String bit
    ) {
        return new DesktopIrOperationSpec(
            "MEASURE",
            qubit,
            "q[0]",
            "q[0]",
            bit,
            Math.PI / 2.0
        );
    }
}
