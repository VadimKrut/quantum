/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.contract;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;

/**
 * Importer из внешнего текстового представления в Quantum IR.
 */
public interface QuantumImporter {

    /**
     * Возвращает внешний формат importer.
     *
     * @return внешний формат
     */
    IntegrationFormat format();

    /**
     * Выполняет import с настройками по умолчанию.
     *
     * @param source внешний текст
     * @return результат import
     */
    default ImportResult importProgram(final String source) {
        return importProgram(
            source,
            ImportOptions.defaults()
        );
    }

    /**
     * Выполняет import с явными настройками.
     *
     * @param source внешний текст
     * @param options настройки import
     * @return результат import
     */
    ImportResult importProgram(
        final String source,
        final ImportOptions options
    );
}