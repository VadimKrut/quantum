/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.contract;

import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Exporter из Quantum IR во внешнее представление.
 */
public interface QuantumExporter {

    /**
     * Возвращает внешний формат exporter.
     *
     * @return внешний формат
     */
    IntegrationFormat format();

    /**
     * Возвращает профиль возможностей exporter.
     *
     * @return профиль возможностей
     */
    default IntegrationCapabilityProfile capabilityProfile() {
        return IntegrationCapabilityProfile.empty(format());
    }

    /**
     * Выполняет export с настройками по умолчанию.
     *
     * @param program Quantum IR программа
     * @return результат export
     */
    default ExportResult exportProgram(final QuantumProgram program) {
        return exportProgram(
            program,
            ExportOptions.defaults()
        );
    }

    /**
     * Выполняет export с явными настройками.
     *
     * @param program Quantum IR программа
     * @param options настройки export
     * @return результат export
     */
    ExportResult exportProgram(
        final QuantumProgram program,
        final ExportOptions options
    );
}