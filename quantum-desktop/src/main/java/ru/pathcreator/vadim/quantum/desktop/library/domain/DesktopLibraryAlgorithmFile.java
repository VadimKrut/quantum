/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.library.domain;

import ru.pathcreator.vadim.quantum.library.domain.QuantumAlgorithmDescriptor;

/**
 * Пользовательская запись desktop-библиотеки: описание алгоритма и компактный Java DSL.
 */
public final class DesktopLibraryAlgorithmFile {

    private final QuantumAlgorithmDescriptor descriptor;
    private final String javaDslSource;

    /**
     * Создает файл библиотеки без тяжелого JSON-представления.
     *
     * @param descriptor описание записи библиотеки
     * @param javaDslSource Java DSL, который восстанавливает Quantum IR программу
     */
    public DesktopLibraryAlgorithmFile(
        final QuantumAlgorithmDescriptor descriptor,
        final String javaDslSource
    ) {
        if (descriptor == null) {
            throw new IllegalArgumentException("Algorithm descriptor must not be null.");
        }
        if (
            javaDslSource == null
            || javaDslSource.isBlank()
        ) {
            throw new IllegalArgumentException("Algorithm Java DSL source must not be blank.");
        }
        this.descriptor = descriptor;
        this.javaDslSource = javaDslSource;
    }

    /**
     * Возвращает описание алгоритма.
     *
     * @return описание алгоритма
     */
    public QuantumAlgorithmDescriptor descriptor() {
        return descriptor;
    }

    /**
     * Возвращает компактный Java DSL.
     *
     * @return Java DSL
     */
    public String javaDslSource() {
        return javaDslSource;
    }
}