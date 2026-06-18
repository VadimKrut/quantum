/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.oracle;

import java.util.List;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.api.QuantumCircuitBuilder;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmDifficulty;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterDefinition;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterSet;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmEntry;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmGenerator;

import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.BALANCED_PARAMETER;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.QUBITS_PARAMETER;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.SECRET_MASK_PARAMETER;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.c;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.descriptor;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.entry;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.q;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.validateSecretMask;

/**
 * Oracle-based алгоритмы для проверки скрытых функций и phase kickback.
 */
public final class OracleAlgorithms {

    private static final String FUNCTION_KIND_PARAMETER = "functionKind";

    private OracleAlgorithms() {
    }

    /**
     * Создает Deutsch-Jozsa oracle probe.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry deutschJozsa() {
        return entry(
            descriptor(
                "oracle.deutsch-jozsa",
                "Deutsch-Jozsa Oracle Probe",
                "Проверяет constant/balanced oracle через фазовый kickback и измерение входного регистра.",
                AlgorithmCategory.ORACLE,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "oracle",
                    "deutsch-jozsa",
                    "kickback"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/api/qiskit/release-notes/0.9"),
                List.of(
                    AlgorithmParameterDefinition.integer(
                        QUBITS_PARAMETER,
                        "Количество входных qubit без oracle-ancilla.",
                        3,
                        1,
                        16
                    ),
                    AlgorithmParameterDefinition.bool(
                        BALANCED_PARAMETER,
                        "Использовать balanced oracle вместо constant oracle.",
                        true
                    )
                )
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int inputQubits = parameters.integer(QUBITS_PARAMETER);
                    final boolean balanced = parameters.bool(BALANCED_PARAMETER);
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("deutsch_jozsa")
                        .qreg("q", inputQubits + 1)
                        .creg("c", inputQubits)
                        .x(q(inputQubits));
                    for (int i = 0; i <= inputQubits; i++) {
                        circuit.h(q(i));
                    }
                    if (balanced) {
                        for (int i = 0; i < inputQubits; i++) {
                            circuit.cx(
                                q(i),
                                q(inputQubits)
                            );
                        }
                    }
                    for (int i = 0; i < inputQubits; i++) {
                        circuit.h(q(i));
                        circuit.measure(
                            q(i),
                            c(i)
                        );
                    }
                    return circuit.build();
                }
            }
        );
    }

    /**
     * Создает Bernstein-Vazirani hidden-string probe.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry bernsteinVazirani() {
        return entry(
            descriptor(
                "oracle.bernstein-vazirani",
                "Bernstein-Vazirani",
                "Восстанавливает hidden bit string через один oracle-call и измерение входного регистра.",
                AlgorithmCategory.CRYPTOGRAPHY,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "oracle",
                    "hidden-string",
                    "cryptography"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/api/qiskit/release-notes/0.9"),
                List.of(
                    AlgorithmParameterDefinition.integer(
                        QUBITS_PARAMETER,
                        "Количество входных qubit без oracle-ancilla.",
                        4,
                        1,
                        32
                    ),
                    AlgorithmParameterDefinition.longInteger(
                        SECRET_MASK_PARAMETER,
                        "Битовая маска hidden string.",
                        11L,
                        0L,
                        Long.MAX_VALUE
                    )
                )
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int inputQubits = parameters.integer(QUBITS_PARAMETER);
                    final long secretMask = parameters.longInteger(SECRET_MASK_PARAMETER);
                    validateSecretMask(
                        inputQubits,
                        secretMask
                    );
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("bernstein_vazirani")
                        .qreg("q", inputQubits + 1)
                        .creg("c", inputQubits)
                        .x(q(inputQubits));
                    for (int i = 0; i <= inputQubits; i++) {
                        circuit.h(q(i));
                    }
                    for (int i = 0; i < inputQubits; i++) {
                        if (((secretMask >> i) & 1L) == 1L) {
                            circuit.cx(
                                q(i),
                                q(inputQubits)
                            );
                        }
                    }
                    for (int i = 0; i < inputQubits; i++) {
                        circuit.h(q(i));
                        circuit.measure(
                            q(i),
                            c(i)
                        );
                    }
                    return circuit.build();
                }
            }
        );
    }

    /**
     * Создает single-bit Deutsch probe для constant zero, constant one, identity и not.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry deutschSingleBit() {
        return entry(
            descriptor(
                "oracle.deutsch-single-bit",
                "Deutsch Single-Bit Oracle",
                "Различает constant и balanced single-bit oracle через один запрос.",
                AlgorithmCategory.ORACLE,
                AlgorithmDifficulty.INTRODUCTORY,
                List.of(
                    "oracle",
                    "deutsch",
                    "kickback"
                ),
                List.of("https://quantumai.google/cirq/experiments/textbook_algorithms"),
                List.of(AlgorithmParameterDefinition.integer(
                    FUNCTION_KIND_PARAMETER,
                    "0=constant zero, 1=constant one, 2=identity, 3=not.",
                    2,
                    0,
                    3
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int functionKind = parameters.integer(FUNCTION_KIND_PARAMETER);
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("deutsch_single_bit")
                        .qreg("q", 2)
                        .creg("c", 1)
                        .x(q(1))
                        .h(q(0))
                        .h(q(1));
                    if (functionKind == 1) {
                        circuit.x(q(1));
                    } else if (functionKind == 2) {
                        circuit.cx(q(0), q(1));
                    } else if (functionKind == 3) {
                        circuit.x(q(1))
                            .cx(q(0), q(1));
                    }
                    circuit.h(q(0))
                        .measure(q(0), c(0));
                    return circuit.build();
                }
            }
        );
    }

    /**
     * Создает parity phase-kickback probe для заданной hidden mask.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry parityKickback() {
        return entry(
            descriptor(
                "oracle.parity-kickback",
                "Parity Kickback",
                "Кодирует parity mask в фазу через oracle-ancilla и восстанавливает mask измерением.",
                AlgorithmCategory.ORACLE,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "oracle",
                    "parity",
                    "kickback"
                ),
                List.of("https://quantumai.google/cirq/experiments/textbook_algorithms"),
                List.of(
                    AlgorithmParameterDefinition.integer(
                        QUBITS_PARAMETER,
                        "Количество входных qubit без oracle-ancilla.",
                        3,
                        1,
                        16
                    ),
                    AlgorithmParameterDefinition.longInteger(
                        SECRET_MASK_PARAMETER,
                        "Битовая parity mask.",
                        5L,
                        0L,
                        Long.MAX_VALUE
                    )
                )
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int inputQubits = parameters.integer(QUBITS_PARAMETER);
                    final long secretMask = parameters.longInteger(SECRET_MASK_PARAMETER);
                    validateSecretMask(
                        inputQubits,
                        secretMask
                    );
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("parity_kickback")
                        .qreg("q", inputQubits + 1)
                        .creg("c", inputQubits)
                        .x(q(inputQubits));
                    for (int index = 0; index <= inputQubits; index++) {
                        circuit.h(q(index));
                    }
                    for (int index = 0; index < inputQubits; index++) {
                        if (((secretMask >> index) & 1L) == 1L) {
                            circuit.cx(
                                q(index),
                                q(inputQubits)
                            );
                        }
                    }
                    for (int index = 0; index < inputQubits; index++) {
                        circuit.h(q(index))
                            .measure(
                                q(index),
                                c(index)
                            );
                    }
                    return circuit.build();
                }
            }
        );
    }
}