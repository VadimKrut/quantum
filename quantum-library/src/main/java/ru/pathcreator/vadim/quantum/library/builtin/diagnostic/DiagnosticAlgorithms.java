/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.builtin.diagnostic;

import java.util.List;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmCategory;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmDifficulty;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterSet;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmEntry;
import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmGenerator;

import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.descriptor;
import static ru.pathcreator.vadim.quantum.library.builtin.BuiltInAlgorithmSupport.entry;

/**
 * Диагностические программы для регресса gate surface, экспорта и симуляции.
 */
public final class DiagnosticAlgorithms {

    private DiagnosticAlgorithms() {
    }

    /**
     * Создает широкую схему с основными single/multi-qubit gates.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry standardGateSurfaceProbe() {
        return entry(
            descriptor(
                "diagnostic.standard-gate-surface-probe",
                "Standard Gate Surface Probe",
                "Покрывает H/X/Y/Z/S/T/RX/RY/RZ/PHASE/CX/CY/CZ/CH/SWAP/CCX и измерения.",
                AlgorithmCategory.DIAGNOSTIC,
                AlgorithmDifficulty.INTERMEDIATE,
                List.of(
                    "diagnostic",
                    "gate-surface",
                    "regression"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of()
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    return Quantum.programBuilder()
                        .circuit("standard_gate_surface_probe")
                        .qreg("q", 5)
                        .creg("c", 5)
                        .h("q[0]")
                        .x("q[1]")
                        .y("q[2]")
                        .z("q[3]")
                        .s("q[4]")
                        .t("q[0]")
                        .rx(Math.PI / 3.0, "q[1]")
                        .ry(Math.PI / 4.0, "q[2]")
                        .rz(Math.PI / 5.0, "q[3]")
                        .phase(Math.PI / 6.0, "q[4]")
                        .cx("q[0]", "q[1]")
                        .cy("q[1]", "q[2]")
                        .cz("q[2]", "q[3]")
                        .ch("q[3]", "q[4]")
                        .swap("q[0]", "q[4]")
                        .ccx("q[0]", "q[1]", "q[2]")
                        .barrier("q[0]", "q[4]")
                        .measure("q[0]", "c[0]")
                        .measure("q[1]", "c[1]")
                        .measure("q[2]", "c[2]")
                        .measure("q[3]", "c[3]")
                        .measure("q[4]", "c[4]")
                        .build();
                }
            }
        );
    }

    /**
     * Создает reset/id/barrier probe с детерминированным результатом.
     *
     * @return запись библиотеки
     */
    public static QuantumAlgorithmEntry resetAndIdentityProbe() {
        return entry(
            descriptor(
                "diagnostic.reset-identity-probe",
                "Reset And Identity Probe",
                "Проверяет reset, id и barrier на простой детерминированной схеме.",
                AlgorithmCategory.DIAGNOSTIC,
                AlgorithmDifficulty.INTRODUCTORY,
                List.of(
                    "diagnostic",
                    "reset",
                    "identity"
                ),
                List.of("https://quantum.cloud.ibm.com/docs/en/guides/composer"),
                List.of()
            ),
            new QuantumAlgorithmGenerator() {

                @Override
                public QuantumProgram generate(final AlgorithmParameterSet parameters) {
                    return Quantum.programBuilder()
                        .circuit("reset_identity_probe")
                        .qreg("q", 2)
                        .creg("c", 2)
                        .x("q[0]")
                        .reset("q[0]")
                        .id("q[0]")
                        .x("q[1]")
                        .barrier("q[0]", "q[1]")
                        .measure("q[0]", "c[0]")
                        .measure("q[1]", "c[1]")
                        .build();
                }
            }
        );
    }
}