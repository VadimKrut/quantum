/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.chemistry;

import java.util.List;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmDifficulty;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterDefinition;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterSet;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmEntry;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmGenerator;

import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.THETA_PARAMETER;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.descriptor;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.entry;

/**
 * Chemistry-oriented ansatz программы, которые строятся как обычный Quantum IR.
 */
public final class ChemistryAlgorithms {

    private ChemistryAlgorithms() {
    }

    /**
     * Создает компактный H2 variational ansatz.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry hydrogenVariationalAnsatz() {
        return entry(
            descriptor(
                "chemistry.h2-variational-ansatz",
                "H2 Variational Ansatz",
                "Компактный двухкубитный variational ansatz для проверок chemistry workflow.",
                AlgorithmCategory.CHEMISTRY,
                AlgorithmDifficulty.ADVANCED,
                List.of(
                    "chemistry",
                    "vqe",
                    "ansatz"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/api/qiskit/release-notes/0.9"),
                List.of(AlgorithmParameterDefinition.decimal(
                    THETA_PARAMETER,
                    "Угол параметризованного ansatz.",
                    0.7853981633974483,
                    -6.283185307179586,
                    6.283185307179586
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final double theta = parameters.decimal(THETA_PARAMETER);
                    return Quantum.programBuilder()
                        .circuit("h2_variational_ansatz")
                        .qreg("q", 2)
                        .creg("c", 2)
                        .x("q[0]")
                        .ry(theta, "q[1]")
                        .cx("q[1]", "q[0]")
                        .rz(theta / 2.0, "q[0]")
                        .cx("q[1]", "q[0]")
                        .ry(-theta, "q[1]")
                        .measure("q[0]", "c[0]")
                        .measure("q[1]", "c[1]")
                        .build();
                }
            }
        );
    }
}