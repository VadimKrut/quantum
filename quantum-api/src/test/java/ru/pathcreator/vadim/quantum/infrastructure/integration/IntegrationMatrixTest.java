/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.integration;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.api.QuantumIntegrations;
import ru.pathcreator.vadim.quantum.api.QuantumIrFiles;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrReadResult;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpression;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationMatrixTest {

    @Test
    void javaJsonAndTextFormatsRoundTripPortableAlgorithms() {
        final List<AlgorithmCase> algorithms = List.of(
            new AlgorithmCase(
                "bell_pair",
                IntegrationMatrixTest::bellPair
            ),
            new AlgorithmCase(
                "ghz_three",
                IntegrationMatrixTest::ghzThree
            ),
            new AlgorithmCase(
                "phase_kickback",
                IntegrationMatrixTest::phaseKickback
            ),
            new AlgorithmCase(
                "bit_flip_encoding",
                IntegrationMatrixTest::bitFlipEncoding
            ),
            new AlgorithmCase(
                "toffoli_swap",
                IntegrationMatrixTest::toffoliSwap
            )
        );
        final List<QuantumIntegration> integrations = List.of(
            QuantumIntegrations.openQasm2(),
            QuantumIntegrations.openQasm3(),
            QuantumIntegrations.quil()
        );

        for (final AlgorithmCase algorithm : algorithms) {
            final QuantumProgram fromJava = algorithm.create();
            final QuantumIrWriteResult json = QuantumIrFiles.writeToString(fromJava);

            assertTrue(
                json.isSuccess(),
                algorithm.name() + " must be written to JSON"
            );
            assertTrue(
                json.content().contains(algorithm.name()),
                algorithm.name() + " JSON must keep the circuit name"
            );

            final QuantumIrReadResult fromJson = QuantumIrFiles.readFromString(json.content());

            assertTrue(
                fromJson.isSuccess(),
                algorithm.name() + " must be read from JSON"
            );
            assertEquals(
                operationCount(fromJava),
                operationCount(fromJson.program()),
                algorithm.name() + " JSON round-trip must keep operations"
            );

            for (final QuantumIntegration integration : integrations) {
                final ExportResult exported = integration.exportProgram(fromJson.program());

                assertTrue(
                    exported.isSuccess(),
                    algorithm.name() + " export to " + exported.format().displayName() + " must succeed: "
                        + exported.diagnostics()
                );

                final ImportResult imported = integration.importProgram(exported.content());

                assertTrue(
                    imported.isSuccess(),
                    algorithm.name() + " import from " + exported.format().displayName() + " must succeed: "
                        + imported.diagnostics()
                );
                assertEquals(
                    operationCount(fromJson.program()),
                    operationCount(imported.program()),
                    algorithm.name() + " " + exported.format().displayName()
                        + " round-trip must keep operations"
                );
            }
        }
    }

    @Test
    void realQuilExamplesImportToJsonAndExportToTextFormats() {
        final List<QuilExample> examples = List.of(
            new QuilExample(
                "bell_state",
                """
                DECLARE ro BIT[2]
                H 0
                CNOT 0 1
                MEASURE 0 ro[0]
                MEASURE 1 ro[1]
                """,
                4
            ),
            new QuilExample(
                "ghz_state",
                """
                DECLARE ro BIT[3]
                H 0
                CNOT 0 1
                CNOT 1 2
                MEASURE 0 ro[0]
                MEASURE 1 ro[1]
                MEASURE 2 ro[2]
                """,
                6
            ),
            new QuilExample(
                "rotation_chain",
                """
                DECLARE ro BIT[2]
                RX(pi) 0
                RY(1.5707963267948966) 1
                CNOT 0 1
                MEASURE 0 ro[0]
                MEASURE 1 ro[1]
                """,
                5
            ),
            new QuilExample(
                "reset_and_phase",
                """
                DECLARE ro BIT[2]
                RESET 0
                H 0
                CPHASE(3.141592653589793) 0 1
                MEASURE 0 ro[0]
                MEASURE 1 ro[1]
                """,
                5
            ),
            new QuilExample(
                "toffoli_swap",
                """
                DECLARE ro BIT[3]
                X 0
                X 1
                CCNOT 0 1 2
                SWAP 0 2
                MEASURE 0 ro[0]
                MEASURE 1 ro[1]
                MEASURE 2 ro[2]
                """,
                7
            )
        );
        final QuantumIntegration quil = QuantumIntegrations.quil();

        for (final QuilExample example : examples) {
            final ImportResult imported = quil.importProgram(example.content());

            assertTrue(
                imported.isSuccess(),
                example.name() + " Quil import must succeed: " + imported.diagnostics()
            );
            assertEquals(
                example.operationCount(),
                operationCount(imported.program()),
                example.name() + " operation count must match"
            );

            final QuantumIrWriteResult json = QuantumIrFiles.writeToString(imported.program());

            assertTrue(
                json.isSuccess(),
                example.name() + " imported Quil must be written to JSON"
            );
            assertTrue(
                QuantumIrFiles.readFromString(json.content()).isSuccess(),
                example.name() + " JSON must be readable"
            );

            assertTrue(
                QuantumIntegrations.openQasm2().exportProgram(imported.program()).isSuccess(),
                example.name() + " must export to OpenQASM 2"
            );
            assertTrue(
                QuantumIntegrations.openQasm3().exportProgram(imported.program()).isSuccess(),
                example.name() + " must export to OpenQASM 3"
            );
            assertTrue(
                quil.exportProgram(imported.program()).isSuccess(),
                example.name() + " must export back to Quil"
            );
        }
    }

    private static QuantumProgram bellPair() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bell_pair");
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

    private static QuantumProgram ghzThree() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("ghz_three");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            3
        );

        circuit.h(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .cx(
                q.get(1),
                q.get(2)
            );
        measureAll(
            circuit,
            q,
            c
        );
        return program;
    }

    private static QuantumProgram phaseKickback() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("phase_kickback");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            2
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            2
        );

        circuit.h(q.get(0))
            .h(q.get(1))
            .cz(
                q.get(0),
                q.get(1)
            )
            .h(q.get(0));
        measureAll(
            circuit,
            q,
            c
        );
        return program;
    }

    private static QuantumProgram bitFlipEncoding() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("bit_flip_encoding");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            3
        );

        circuit.x(q.get(0))
            .cx(
                q.get(0),
                q.get(1)
            )
            .cx(
                q.get(0),
                q.get(2)
            );
        measureAll(
            circuit,
            q,
            c
        );
        return program;
    }

    private static QuantumProgram toffoliSwap() {
        final QuantumProgram program = QuantumProgram.gateBased();
        final QuantumCircuit circuit = program.createCircuit("toffoli_swap");
        final QuantumRegister q = circuit.createQuantumRegister(
            "q",
            3
        );
        final ClassicalRegister c = circuit.createClassicalRegister(
            "c",
            3
        );

        circuit.x(q.get(0))
            .x(q.get(1))
            .ccx(
                q.get(0),
                q.get(1),
                q.get(2)
            )
            .swap(
                q.get(0),
                q.get(2)
            )
            .rz(
                ParameterExpression.divide(
                    ParameterExpression.pi(),
                    ParameterExpression.of(2.0)
                ),
                q.get(1)
            );
        measureAll(
            circuit,
            q,
            c
        );
        return program;
    }

    private static void measureAll(
        final QuantumCircuit circuit,
        final QuantumRegister q,
        final ClassicalRegister c
    ) {
        for (int i = 0; i < q.size(); i++) {
            circuit.measure(
                q.get(i),
                c.get(i)
            );
        }
    }

    private static int operationCount(final QuantumProgram program) {
        int count = 0;
        for (int i = 0; i < program.circuitCount(); i++) {
            count += program.circuit(i).operationCount();
        }
        return count;
    }

    @FunctionalInterface
    private interface ProgramFactory {

        QuantumProgram create();
    }

    private record AlgorithmCase(
        String name,
        ProgramFactory factory
    ) {

        QuantumProgram create() {
            return factory.create();
        }
    }

    private record QuilExample(
        String name,
        String content,
        int operationCount
    ) {
    }
}