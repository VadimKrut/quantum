/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.infrastructure.openqasm2.include;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnostic;
import ru.pathcreator.vadim.quantum.application.integration.diagnostic.IntegrationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.integration.options.ImportOptions;

/**
 * Resolver OpenQASM 2 include sources из in-memory options и явно разрешенных директорий.
 */
public final class OpenQasm2IncludeResolver {

    /**
     * Собирает include sources для parser.
     *
     * @param options import options
     * @return результат resolver
     */
    public OpenQasm2IncludeResolverResult resolve(final ImportOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Import options must not be null.");
        }
        final LinkedHashMap<String, String> sources = new LinkedHashMap<>(options.includedSources());
        for (int i = 0; i < options.includeDirectoryCount(); i++) {
            final Path directory;
            try {
                directory = Path.of(options.includeDirectories().get(i)).toAbsolutePath().normalize();
            } catch (final InvalidPathException exception) {
                return OpenQasm2IncludeResolverResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.INCLUDE_RESOLUTION_FAILED,
                    "OpenQASM 2 include directory path is invalid: " + options.includeDirectories().get(i) + "."
                ));
            }
            if (!Files.isDirectory(directory)) {
                return OpenQasm2IncludeResolverResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.INCLUDE_RESOLUTION_FAILED,
                    "OpenQASM 2 include directory does not exist: " + directory + "."
                ));
            }
            try {
                loadDirectorySources(
                    directory,
                    sources
                );
            } catch (final IllegalArgumentException exception) {
                return OpenQasm2IncludeResolverResult.failure(IntegrationDiagnostic.error(
                    IntegrationDiagnosticCode.INCLUDE_RESOLUTION_FAILED,
                    exception.getMessage()
                ));
            }
        }
        return OpenQasm2IncludeResolverResult.success(sources);
    }

    private static void loadDirectorySources(
        final Path directory,
        final LinkedHashMap<String, String> sources
    ) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (Files.isRegularFile(file)) {
                    loadSource(
                        file,
                        sources
                    );
                }
            }
        } catch (final IOException exception) {
            throw new IllegalArgumentException("OpenQASM 2 include directory cannot be read.", exception);
        }
    }

    private static void loadSource(
        final Path file,
        final LinkedHashMap<String, String> sources
    ) {
        final Path name = file.getFileName();
        if (name == null) {
            return;
        }
        final String includeName = name.toString();
        if (sources.containsKey(includeName)) {
            return;
        }
        try {
            sources.put(
                includeName,
                Files.readString(
                    file,
                    StandardCharsets.UTF_8
                )
            );
        } catch (final IOException exception) {
            throw new IllegalArgumentException("OpenQASM 2 include file cannot be read.", exception);
        }
    }
}