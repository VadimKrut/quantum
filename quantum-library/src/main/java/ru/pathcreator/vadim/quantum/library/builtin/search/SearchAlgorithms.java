/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.search;

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
 * Поисковые алгоритмы и amplitude-amplification probes.
 */
public final class SearchAlgorithms {

    private static final String MARKED_STATE_PARAMETER = "markedState";

    private SearchAlgorithms() {
    }

    /**
     * Создает двухкубитный Grover search для marked state |11>.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry twoQubitGrover() {
        return twoQubitGroverEntry(
            "search.two-qubit-grover",
            "Two-Qubit Grover Search",
            "Демонстрирует один шаг Grover search для отмеченного состояния |11>.",
            3,
            List.of()
        );
    }

    /**
     * Создает параметризованный двухкубитный Grover search.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry parameterizedTwoQubitGrover() {
        return twoQubitGroverEntry(
            "search.parameterized-two-qubit-grover",
            "Parameterized Two-Qubit Grover",
            "Демонстрирует один шаг Grover search для выбранного двухбитного marked state.",
            2,
            List.of(AlgorithmParameterDefinition.integer(
                MARKED_STATE_PARAMETER,
                "Отмеченное состояние от 0 до 3.",
                2,
                0,
                3
            ))
        );
    }

    private static QuantumAlgorithmEntry twoQubitGroverEntry(
        final String id,
        final String displayName,
        final String summary,
        final int defaultMarkedState,
        final List<AlgorithmParameterDefinition> parameters
    ) {
        return entry(
            descriptor(
                id,
                displayName,
                summary,
                AlgorithmCategory.SEARCH,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "grover",
                    "search",
                    "amplification"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/api/qiskit/release-notes/0.9"),
                parameters
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int markedState = parameters.contains(MARKED_STATE_PARAMETER)
                        ? parameters.integer(MARKED_STATE_PARAMETER)
                        : defaultMarkedState;
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("two_qubit_grover")
                        .qreg("q", 2)
                        .creg("c", 2)
                        .h("q[0]")
                        .h("q[1]");
                    applyMarkedStateOracle(
                        circuit,
                        markedState
                    );
                    applyTwoQubitDiffuser(circuit);
                    circuit.measure("q[0]", "c[0]")
                        .measure("q[1]", "c[1]");
                    return circuit.build();
                }
            }
        );
    }

    private static void applyMarkedStateOracle(
        final QuantumCircuitBuilder circuit,
        final int markedState
    ) {
        final boolean lowBitZero = (markedState & 1) == 0;
        final boolean highBitZero = (markedState & 2) == 0;
        if (lowBitZero) {
            circuit.x("q[0]");
        }
        if (highBitZero) {
            circuit.x("q[1]");
        }
        circuit.cz("q[0]", "q[1]");
        if (lowBitZero) {
            circuit.x("q[0]");
        }
        if (highBitZero) {
            circuit.x("q[1]");
        }
    }

    private static void applyTwoQubitDiffuser(final QuantumCircuitBuilder circuit) {
        circuit.h("q[0]")
            .h("q[1]")
            .x("q[0]")
            .x("q[1]")
            .cz("q[0]", "q[1]")
            .x("q[0]")
            .x("q[1]")
            .h("q[0]")
            .h("q[1]");
    }
}