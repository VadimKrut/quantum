/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.library.api;

import ru.pathcreator.vadim.quantum.library.builtin.BuiltInQuantumAlgorithmLibrary;
import ru.pathcreator.vadim.quantum.library.catalog.QuantumAlgorithmRegistry;
import ru.pathcreator.vadim.quantum.library.domain.AlgorithmParameterSet;

import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Публичный фасад библиотеки готовых Quantum IR программ.
 */
public final class QuantumAlgorithmLibrary {

    private static final QuantumAlgorithmRegistry BUILT_IN = BuiltInQuantumAlgorithmLibrary.create();

    private QuantumAlgorithmLibrary() {
    }

    /**
     * Возвращает встроенный реестр алгоритмов.
     *
     * @return встроенный реестр
     */
    public static QuantumAlgorithmRegistry builtIn() {
        return BUILT_IN;
    }

    /**
     * Создает программу по id со значениями параметров по умолчанию.
     *
     * @param id идентификатор алгоритма
     * @return Quantum IR программа
     */
    public static QuantumProgram generate(final String id) {
        return builtIn().get(id).generate();
    }

    /**
     * Создает программу по id с пользовательскими параметрами.
     *
     * @param id идентификатор алгоритма
     * @param parameters параметры алгоритма
     * @return Quantum IR программа
     */
    public static QuantumProgram generate(
        final String id,
        final AlgorithmParameterSet parameters
    ) {
        return builtIn().get(id).generate(parameters);
    }
}