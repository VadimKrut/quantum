/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.education;

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
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.c;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.descriptor;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.entry;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.measureAll;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.q;

/**
 * Учебные алгоритмы и схемы для проверки запутанности и базовых инвариантов.
 */
public final class EntanglementAlgorithms {

    private EntanglementAlgorithms() {
    }

    /**
     * Создает запись Bell State.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry bellState() {
        return entry(
            descriptor(
                "education.bell-state",
                "Bell State",
                "Создает двухкубитное запутанное состояние и измеряет оба qubit.",
                AlgorithmCategory.ENTANGLEMENT,
                AlgorithmDifficulty.INTRODUCTORY,
                List.of(
                    "bell",
                    "entanglement",
                    "measurement"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of()
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    return Quantum.programBuilder()
                        .circuit("bell")
                        .qreg("q", 2)
                        .creg("c", 2)
                        .h("q[0]")
                        .cx("q[0]", "q[1]")
                        .measure("q[0]", "c[0]")
                        .measure("q[1]", "c[1]")
                        .build();
                }
            }
        );
    }

    /**
     * Создает параметризуемую запись GHZ State.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry ghzState() {
        return entry(
            descriptor(
                "education.ghz-state",
                "GHZ State",
                "Создает n-qubit GHZ-состояние через цепочку CX и измеряет все qubit.",
                AlgorithmCategory.ENTANGLEMENT,
                AlgorithmDifficulty.INTRODUCTORY,
                List.of(
                    "ghz",
                    "entanglement",
                    "scalable"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of(AlgorithmParameterDefinition.integer(
                    QUBITS_PARAMETER,
                    "Количество qubit в GHZ-состоянии.",
                    3,
                    2,
                    64
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int qubits = parameters.integer(QUBITS_PARAMETER);
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("ghz")
                        .qreg("q", qubits)
                        .creg("c", qubits)
                        .h("q[0]");
                    for (int i = 1; i < qubits; i++) {
                        circuit.cx(
                            q(i - 1),
                            q(i)
                        );
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
     * Создает three-qubit repetition encoding probe для проверки повторяющихся CX и измерений.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry repetitionEncodingProbe() {
        return entry(
            descriptor(
                "education.repetition-encoding-probe",
                "Repetition Encoding Probe",
                "Кодирует один логический bit в три qubit и измеряет физические копии.",
                AlgorithmCategory.DIAGNOSTIC,
                AlgorithmDifficulty.INTRODUCTORY,
                List.of(
                    "repetition",
                    "encoding",
                    "diagnostic"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of()
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    return Quantum.programBuilder()
                        .circuit("repetition_encoding_probe")
                        .qreg("q", 3)
                        .creg("c", 3)
                        .x("q[0]")
                        .cx("q[0]", "q[1]")
                        .cx("q[0]", "q[2]")
                        .measure(q(0), c(0))
                        .measure(q(1), c(1))
                        .measure(q(2), c(2))
                        .build();
                }
            }
        );
    }
}