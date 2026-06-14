/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.quil.adapter;

import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Двунаправленная интеграция Quil.
 */
public final class QuilIntegration implements QuantumIntegration {

    private final QuilExporter exporter;
    private final QuilImporter importer;

    public QuilIntegration() {
        this.exporter = new QuilExporter();
        this.importer = new QuilImporter();
    }

    @Override
    public IntegrationFormat format() {
        return IntegrationFormat.QUIL;
    }

    @Override
    public ExportResult exportProgram(
        final QuantumProgram program,
        final ExportOptions options
    ) {
        return exporter.exportProgram(
            program,
            options
        );
    }

    @Override
    public ImportResult importProgram(
        final String source,
        final ImportOptions options
    ) {
        return importer.importProgram(
            source,
            options
        );
    }
}