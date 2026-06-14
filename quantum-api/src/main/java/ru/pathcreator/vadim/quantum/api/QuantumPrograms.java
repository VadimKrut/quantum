/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.api;

import ru.pathcreator.vadim.quantum.domain.model.QuantumComputationModel;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Публичный фасад для создания квантовых программ.
 */
public final class QuantumPrograms {

    private QuantumPrograms() {
    }

    /**
     * Создает программу для gate-based quantum circuits.
     *
     * @return gate-based квантовая программа
     */
    public static QuantumProgram gateBased() {
        return QuantumProgram.gateBased();
    }

    /**
     * Создает программу для указанной вычислительной модели.
     *
     * @param computationModel вычислительная модель программы
     * @return квантовая программа
     */
    public static QuantumProgram create(final QuantumComputationModel computationModel) {
        return QuantumProgram.create(computationModel);
    }
}