/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.transform;

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

import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.QUBITS_PARAMETER;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.appendQft;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.c;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.descriptor;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.entry;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.measureAll;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.q;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.registerOnlyCircuit;

/**
 * Алгоритмы, где основной смысл в фазовых преобразованиях и смене базиса.
 */
public final class TransformAlgorithms {

    private static final String BASIS_VALUE_PARAMETER = "basisValue";

    private TransformAlgorithms() {
    }

    /**
     * Создает QFT-запись.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry quantumFourierTransform() {
        return qftEntry(
            "transform.qft",
            "Quantum Fourier Transform",
            "Строит canonical QFT-схему с controlled phase rotations и финальными swaps.",
            false
        );
    }

    /**
     * Создает inverse QFT-запись.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry inverseQuantumFourierTransform() {
        return qftEntry(
            "transform.inverse-qft",
            "Inverse Quantum Fourier Transform",
            "Строит обратную QFT-схему с отрицательными controlled phase rotations.",
            true
        );
    }

    /**
     * Создает simple quantum phase estimation для eigenstate |1> и phase-gate oracle.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry phaseEstimation() {
        return entry(
            descriptor(
                "transform.phase-estimation",
                "Phase Estimation",
                "Оценивает фазу phase-gate eigenstate через counting register и inverse QFT.",
                AlgorithmCategory.TRANSFORM,
                AlgorithmDifficulty.ADVANCED,
                List.of(
                    "phase-estimation",
                    "qpe",
                    "inverse-qft"
                ),
                List.of("https://quantumai.google/cirq/experiments/textbook_algorithms"),
                List.of(AlgorithmParameterDefinition.integer(
                    QUBITS_PARAMETER,
                    "Количество qubit в counting register.",
                    3,
                    2,
                    8
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int countingQubits = parameters.integer(QUBITS_PARAMETER);
                    final int eigenQubit = countingQubits;
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("phase_estimation")
                        .qreg("q", countingQubits + 1)
                        .creg("c", countingQubits);
                    circuit.x(q(eigenQubit));
                    for (int i = 0; i < countingQubits; i++) {
                        circuit.h(q(i));
                    }
                    for (int control = 0; control < countingQubits; control++) {
                        final int repetitions = 1 << control;
                        for (int repeat = 0; repeat < repetitions; repeat++) {
                            circuit.cphase(
                                Math.PI / 2.0,
                                q(control),
                                q(eigenQubit)
                            );
                        }
                    }
                    appendQft(
                        circuit,
                        countingQubits,
                        true
                    );
                    for (int i = 0; i < countingQubits; i++) {
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
     * Создает схему подготовки basis state с измерением всех qubit.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry basisPreparation() {
        return entry(
            descriptor(
                "transform.basis-preparation",
                "Basis Preparation",
                "Готовит computational basis state по integer value и измеряет все qubit.",
                AlgorithmCategory.TRANSFORM,
                AlgorithmDifficulty.INTRODUCTORY,
                List.of(
                    "basis",
                    "preparation",
                    "deterministic"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of(
                    AlgorithmParameterDefinition.integer(
                        QUBITS_PARAMETER,
                        "Количество qubit.",
                        4,
                        1,
                        16
                    ),
                    AlgorithmParameterDefinition.longInteger(
                        BASIS_VALUE_PARAMETER,
                        "Computational basis value.",
                        10L,
                        0L,
                        Long.MAX_VALUE
                    )
                )
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int qubits = parameters.integer(QUBITS_PARAMETER);
                    final long basisValue = parameters.longInteger(BASIS_VALUE_PARAMETER);
                    ensureBasisValueFits(
                        qubits,
                        basisValue
                    );
                    final QuantumCircuitBuilder circuit = registerOnlyCircuit(
                        "basis_preparation",
                        qubits
                    );
                    for (int index = 0; index < qubits; index++) {
                        if (((basisValue >> index) & 1L) == 1L) {
                            circuit.x(q(index));
                        }
                    }
                    measureAll(
                        circuit,
                        qubits
                    );
                    return circuit.build();
                }
            }
        );
    }

    /**
     * Создает QFT -> inverse QFT round-trip для basis state.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry qftRoundTrip() {
        return entry(
            descriptor(
                "transform.qft-round-trip",
                "QFT Round Trip",
                "Готовит basis state, применяет QFT и inverse QFT, затем проверяет восстановление измерением.",
                AlgorithmCategory.TRANSFORM,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "qft",
                    "inverse-qft",
                    "round-trip"
                ),
                List.of("https://quantumai.google/cirq/experiments/textbook_algorithms"),
                List.of(
                    AlgorithmParameterDefinition.integer(
                        QUBITS_PARAMETER,
                        "Количество qubit.",
                        4,
                        2,
                        10
                    ),
                    AlgorithmParameterDefinition.longInteger(
                        BASIS_VALUE_PARAMETER,
                        "Computational basis value.",
                        9L,
                        0L,
                        Long.MAX_VALUE
                    )
                )
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int qubits = parameters.integer(QUBITS_PARAMETER);
                    final long basisValue = parameters.longInteger(BASIS_VALUE_PARAMETER);
                    ensureBasisValueFits(
                        qubits,
                        basisValue
                    );
                    final QuantumCircuitBuilder circuit = registerOnlyCircuit(
                        "qft_round_trip",
                        qubits
                    );
                    for (int index = 0; index < qubits; index++) {
                        if (((basisValue >> index) & 1L) == 1L) {
                            circuit.x(q(index));
                        }
                    }
                    appendQft(
                        circuit,
                        qubits,
                        false
                    );
                    appendQft(
                        circuit,
                        qubits,
                        true
                    );
                    measureAll(
                        circuit,
                        qubits
                    );
                    return circuit.build();
                }
            }
        );
    }

    /**
     * Создает Clifford echo, где последовательность H/S/CX отменяется обратной частью.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry cliffordEcho() {
        return entry(
            descriptor(
                "transform.clifford-echo",
                "Clifford Echo",
                "Проверяет обратимость Clifford-подцепочки через прямую и обратную последовательность gates.",
                AlgorithmCategory.TRANSFORM,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "clifford",
                    "echo",
                    "round-trip"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of()
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    return Quantum.programBuilder()
                        .circuit("clifford_echo")
                        .qreg("q", 3)
                        .creg("c", 3)
                        .x(q(0))
                        .h(q(1))
                        .s(q(1))
                        .cx(q(1), q(2))
                        .cz(q(0), q(2))
                        .cz(q(0), q(2))
                        .cx(q(1), q(2))
                        .sdg(q(1))
                        .h(q(1))
                        .measure(q(0), c(0))
                        .measure(q(1), c(1))
                        .measure(q(2), c(2))
                        .build();
                }
            }
        );
    }

    private static QuantumAlgorithmEntry qftEntry(
        final String id,
        final String displayName,
        final String summary,
        final boolean inverse
    ) {
        return entry(
            descriptor(
                id,
                displayName,
                summary,
                AlgorithmCategory.TRANSFORM,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "qft",
                    "fourier",
                    "phase"
                ),
                List.of("https://quantumai.google/cirq/experiments/textbook_algorithms"),
                List.of(AlgorithmParameterDefinition.integer(
                    QUBITS_PARAMETER,
                    "Размер регистра преобразования.",
                    4,
                    2,
                    16
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int qubits = parameters.integer(QUBITS_PARAMETER);
                    final QuantumCircuitBuilder circuit = registerOnlyCircuit(
                        inverse ? "inverse_qft" : "qft",
                        qubits
                    );
                    appendQft(
                        circuit,
                        qubits,
                        inverse
                    );
                    measureAll(
                        circuit,
                        qubits
                    );
                    return circuit.build();
                }
            }
        );
    }

    private static void ensureBasisValueFits(
        final int qubits,
        final long basisValue
    ) {
        if (
            qubits < Long.SIZE - 1
            && basisValue >= (1L << qubits)
        ) {
            throw new IllegalArgumentException("Basis value does not fit into requested qubit count.");
        }
    }
}