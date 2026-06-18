/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Сохраняет JSON manifest matrix visual audit рядом со screenshot-файлами.
 */
public final class DesktopVisualAuditManifestWriter {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    public void write(
        final String outputDirectory,
        final DesktopVisualAuditManifest manifest
    ) throws IOException {
        final Path directory = Path.of(outputDirectory);
        Files.createDirectories(directory);
        objectMapper.writeValue(
            directory.resolve("manifest.json").toFile(),
            manifest
        );
    }
}