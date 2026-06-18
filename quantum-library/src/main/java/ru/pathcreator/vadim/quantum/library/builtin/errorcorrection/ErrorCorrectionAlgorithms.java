/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.errorcorrection;

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

import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.c;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.descriptor;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.entry;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.q;

/**
 * Учебные схемы кодирования и syndrome-проверок для базовой коррекции ошибок.
 */
public final class ErrorCorrectionAlgorithms {

    private static final String LOGICAL_BIT_PARAMETER = "logicalBit";
    private static final String FLIP_INDEX_PARAMETER = "flipIndex";

    private ErrorCorrectionAlgorithms() {
    }

    /**
     * Создает bit-flip repetition code с опциональной одиночной ошибкой.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry bitFlipCodeProbe() {
        return entry(
            descriptor(
                "error-correction.bit-flip-code-probe",
                "Bit-Flip Code Probe",
                "Кодирует logical bit в три физические копии и опционально применяет X к одной линии.",
                AlgorithmCategory.ERROR_CORRECTION,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "error-correction",
                    "repetition",
                    "bit-flip"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of(
                    AlgorithmParameterDefinition.integer(
                        LOGICAL_BIT_PARAMETER,
                        "Кодируемый logical bit.",
                        1,
                        0,
                        1
                    ),
                    AlgorithmParameterDefinition.integer(
                        FLIP_INDEX_PARAMETER,
                        "Индекс физического qubit для X-ошибки или -1 без ошибки.",
                        -1,
                        -1,
                        2
                    )
                )
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int logicalBit = parameters.integer(LOGICAL_BIT_PARAMETER);
                    final int flipIndex = parameters.integer(FLIP_INDEX_PARAMETER);
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("bit_flip_code_probe")
                        .qreg("q", 3)
                        .creg("c", 3);
                    if (logicalBit == 1) {
                        circuit.x(q(0));
                    }
                    circuit.cx(q(0), q(1))
                        .cx(q(0), q(2));
                    if (flipIndex >= 0) {
                        circuit.x(q(flipIndex));
                    }
                    for (int index = 0; index < 3; index++) {
                        circuit.measure(
                            q(index),
                            c(index)
                        );
                    }
                    return circuit.build();
                }
            }
        );
    }

    /**
     * Создает phase-flip encoding probe через H-базис.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry phaseFlipEncodingProbe() {
        return entry(
            descriptor(
                "error-correction.phase-flip-encoding-probe",
                "Phase-Flip Encoding Probe",
                "Кодирует logical bit, переводит три физические линии в X-базис и измеряет после обратного H.",
                AlgorithmCategory.ERROR_CORRECTION,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "error-correction",
                    "phase-flip",
                    "basis-change"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of(AlgorithmParameterDefinition.integer(
                    LOGICAL_BIT_PARAMETER,
                    "Кодируемый logical bit.",
                    1,
                    0,
                    1
                ))
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    final int logicalBit = parameters.integer(LOGICAL_BIT_PARAMETER);
                    final QuantumCircuitBuilder circuit = Quantum.programBuilder()
                        .circuit("phase_flip_encoding_probe")
                        .qreg("q", 3)
                        .creg("c", 3);
                    if (logicalBit == 1) {
                        circuit.x(q(0));
                    }
                    circuit.cx(q(0), q(1))
                        .cx(q(0), q(2));
                    for (int index = 0; index < 3; index++) {
                        circuit.h(q(index));
                    }
                    for (int index = 0; index < 3; index++) {
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