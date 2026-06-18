/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.protocol;

import java.util.List;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.bit.Qubit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmDifficulty;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterDefinition;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterSet;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmEntry;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmGenerator;

import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.MESSAGE_PARAMETER;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.descriptor;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.entry;

/**
 * Протокольные схемы, где важны encoding/decoding и порядок classical bits.
 */
public final class ProtocolAlgorithms {

    private static final String INPUT_BIT_PARAMETER = "inputBit";

    private ProtocolAlgorithms() {
    }

    /**
     * Создает superdense coding circuit для двухбитного сообщения.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry superdenseCoding() {
        return entry(
            descriptor(
                "protocol.superdense-coding",
                "Superdense Coding",
                "Кодирует два classical bits в один qubit через заранее созданную Bell pair.",
                AlgorithmCategory.CRYPTOGRAPHY,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "superdense",
                    "communication",
                    "bell"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of(AlgorithmParameterDefinition.integer(
                    MESSAGE_PARAMETER,
                    "Двухбитное сообщение от 0 до 3.",
                    3,
                    0,
                    3
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int message = parameters.integer(MESSAGE_PARAMETER);
                    final boolean lowBit = (message & 1) == 1;
                    final boolean highBit = (message & 2) == 2;
                    final QuantumProgram program = Quantum.programBuilder()
                        .circuit("superdense_coding")
                        .qreg("q", 2)
                        .creg("c", 2)
                        .h("q[0]")
                        .cx("q[0]", "q[1]")
                        .build();
                    appendEncodingAndDecoding(
                        program,
                        lowBit,
                        highBit
                    );
                    return program;
                }
            }
        );
    }

    /**
     * Создает coherent-вариант teleportation для basis state.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry basisStateTeleportation() {
        return entry(
            descriptor(
                "protocol.basis-state-teleportation",
                "Basis-State Teleportation",
                "Передает basis state с q[0] на q[2] через Bell pair и coherent correction gates.",
                AlgorithmCategory.PROTOCOL,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "teleportation",
                    "bell",
                    "communication"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of(AlgorithmParameterDefinition.integer(
                    INPUT_BIT_PARAMETER,
                    "Basis input bit, который должен быть восстановлен на целевом qubit.",
                    1,
                    0,
                    1
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int inputBit = parameters.integer(INPUT_BIT_PARAMETER);
                    final QuantumProgram program = Quantum.programBuilder()
                        .circuit("basis_state_teleportation")
                        .qreg("q", 3)
                        .creg("c", 1)
                        .build();
                    appendTeleportationCircuit(
                        program,
                        inputBit
                    );
                    return program;
                }
            }
        );
    }

    private static void appendEncodingAndDecoding(
        final QuantumProgram program,
        final boolean lowBit,
        final boolean highBit
    ) {
        final QuantumCircuit circuit = program.circuit(0);
        final Qubit alice = circuit.quantumRegister(0).get(0);
        final Qubit bob = circuit.quantumRegister(0).get(1);
        final ClassicalBit first = circuit.classicalRegister(0).get(0);
        final ClassicalBit second = circuit.classicalRegister(0).get(1);
        if (lowBit) {
            circuit.x(alice);
        }
        if (highBit) {
            circuit.z(alice);
        }
        circuit.cx(
            alice,
            bob
        );
        circuit.h(alice);
        circuit.measure(
            alice,
            first
        );
        circuit.measure(
            bob,
            second
        );
    }

    private static void appendTeleportationCircuit(
        final QuantumProgram program,
        final int inputBit
    ) {
        final QuantumCircuit circuit = program.circuit(0);
        final Qubit source = circuit.quantumRegister(0).get(0);
        final Qubit bellLeft = circuit.quantumRegister(0).get(1);
        final Qubit target = circuit.quantumRegister(0).get(2);
        final ClassicalBit output = circuit.classicalRegister(0).get(0);
        if (inputBit == 1) {
            circuit.x(source);
        }
        circuit.h(bellLeft);
        circuit.cx(
            bellLeft,
            target
        );
        circuit.cx(
            source,
            bellLeft
        );
        circuit.h(source);
        circuit.cx(
            bellLeft,
            target
        );
        circuit.cz(
            source,
            target
        );
        circuit.measure(
            target,
            output
        );
    }
}