/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.arithmetic;

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

import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.descriptor;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.entry;

/**
 * Обратимые арифметические схемы для проверки carry, parity и basis-state логики.
 */
public final class ArithmeticAlgorithms {

    private static final String LEFT_PARAMETER = "left";
    private static final String RIGHT_PARAMETER = "right";

    private ArithmeticAlgorithms() {
    }

    /**
     * Создает обратимый half-adder для двух classical input bits, закодированных в qubit.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry halfAdder() {
        return entry(
            descriptor(
                "arithmetic.half-adder",
                "Half Adder",
                "Считает sum=a xor b и carry=a and b через CNOT/CCX без потери входных qubit.",
                AlgorithmCategory.ARITHMETIC,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "arithmetic",
                    "adder",
                    "ccx"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of(
                    AlgorithmParameterDefinition.integer(
                        LEFT_PARAMETER,
                        "Первый входной bit.",
                        1,
                        0,
                        1
                    ),
                    AlgorithmParameterDefinition.integer(
                        RIGHT_PARAMETER,
                        "Второй входной bit.",
                        1,
                        0,
                        1
                    )
                )
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int left = parameters.integer(LEFT_PARAMETER);
                    final int right = parameters.integer(RIGHT_PARAMETER);
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("half_adder")
                        .qreg("q", 4)
                        .creg("c", 4);
                    if (left == 1) {
                        circuit.x("q[0]");
                    }
                    if (right == 1) {
                        circuit.x("q[1]");
                    }
                    circuit.cx("q[0]", "q[2]")
                        .cx("q[1]", "q[2]")
                        .ccx("q[0]", "q[1]", "q[3]")
                        .measure("q[0]", "c[0]")
                        .measure("q[1]", "c[1]")
                        .measure("q[2]", "c[2]")
                        .measure("q[3]", "c[3]");
                    return circuit.build();
                }
            }
        );
    }

    /**
     * Создает трехбитный parity checker с отдельным выходным qubit.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry parityChecker() {
        return entry(
            descriptor(
                "arithmetic.parity-checker",
                "Parity Checker",
                "Копит xor трех входных bits в отдельный parity qubit и измеряет все линии.",
                AlgorithmCategory.ARITHMETIC,
                AlgorithmDifficulty.INTRODUCTORY,
                List.of(
                    "arithmetic",
                    "parity",
                    "xor"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of(AlgorithmParameterDefinition.integer(
                    "value",
                    "Трехбитное входное значение от 0 до 7.",
                    5,
                    0,
                    7
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int value = parameters.integer("value");
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("parity_checker")
                        .qreg("q", 4)
                        .creg("c", 4);
                    for (int bit = 0; bit < 3; bit++) {
                        if (((value >> bit) & 1) == 1) {
                            circuit.x("q[" + bit + "]");
                        }
                    }
                    circuit.cx("q[0]", "q[3]")
                        .cx("q[1]", "q[3]")
                        .cx("q[2]", "q[3]");
                    for (int bit = 0; bit < 4; bit++) {
                        circuit.measure(
                            "q[" + bit + "]",
                            "c[" + bit + "]"
                        );
                    }
                    return circuit.build();
                }
            }
        );
    }
}