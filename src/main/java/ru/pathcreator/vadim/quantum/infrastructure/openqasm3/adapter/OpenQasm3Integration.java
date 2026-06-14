/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm3.adapter;

import ru.pathcreator.vadim.quantum.application.integration.options.ExportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;
import ru.pathcreator.vadim.quantum.application.integration.result.ImportResult;
import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumIntegration;
import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * Двунаправленная интеграция OpenQASM 3.0.
 */
public final class OpenQasm3Integration implements QuantumIntegration {

    /**
     * Exporter OpenQASM 3.0.
     */
    private final OpenQasm3Exporter exporter;

    /**
     * Importer OpenQASM 3.0.
     */
    private final OpenQasm3Importer importer;

    /**
     * Создает OpenQASM 3.0 integration adapter.
     */
    public OpenQasm3Integration() {
        this.exporter = new OpenQasm3Exporter();
        this.importer = new OpenQasm3Importer();
    }

    @Override
    public IntegrationFormat format() {
        return IntegrationFormat.OPENQASM_3;
    }

    @Override
    public IntegrationCapabilityProfile capabilityProfile() {
        return exporter.capabilityProfile();
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