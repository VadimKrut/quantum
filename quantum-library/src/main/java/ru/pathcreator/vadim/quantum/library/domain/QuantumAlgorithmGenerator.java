/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.domain;

import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Генератор Quantum IR программы из проверенного набора параметров библиотеки.
 */
public interface QuantumAlgorithmGenerator {

    /**
     * Строит новую Quantum IR программу.
     *
     * @param parameters параметры алгоритма
     * @return новая Quantum IR программа
     */
    QuantumProgram generate(AlgorithmParameterSet parameters);
}