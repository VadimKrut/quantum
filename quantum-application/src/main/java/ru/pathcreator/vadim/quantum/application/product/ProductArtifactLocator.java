/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.product;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Находит собранные артефакты продукта без привязки к конкретной Maven-версии.
 */
public final class ProductArtifactLocator {

    private ProductArtifactLocator() {
    }

    public static Path findPackagedJar(
        final Path projectRoot,
        final String module
    ) throws IOException {
        if (projectRoot == null) {
            throw new IllegalArgumentException("Project root must not be null.");
        }
        if (module == null || module.isBlank()) {
            throw new IllegalArgumentException("Maven module name must not be blank.");
        }
        final Path target = projectRoot.toAbsolutePath().normalize().resolve(module).resolve("target");
        if (!Files.isDirectory(target)) {
            return null;
        }
        Path candidate = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
            target,
            module + "-*.jar"
        )) {
            for (final Path jar : stream) {
                final String fileName = jar.getFileName().toString();
                if (isAuxiliaryJar(fileName)) {
                    continue;
                }
                if (candidate == null || fileName.compareTo(candidate.getFileName().toString()) < 0) {
                    candidate = jar;
                }
            }
        }
        return candidate;
    }

    public static Path requiredPackagedJar(
        final Path projectRoot,
        final String module
    ) throws IOException {
        final Path jar = findPackagedJar(
            projectRoot,
            module
        );
        if (jar != null) {
            return jar;
        }
        throw new IOException(
            "Required packaged jar is missing for module " + module + ": "
                + projectRoot.toAbsolutePath().normalize().resolve(module).resolve("target")
                + java.io.File.separator + module + "-*.jar"
        );
    }

    public static boolean hasPackagedJar(
        final Path projectRoot,
        final String module
    ) throws IOException {
        return findPackagedJar(
            projectRoot,
            module
        ) != null;
    }

    public static String projectVersion(final Path projectRoot) throws IOException {
        final String revision = readRevision(projectRoot);
        if (revision != null) {
            return revision;
        }
        final Path cliJar = findPackagedJar(
            projectRoot,
            "quantum-cli"
        );
        if (cliJar != null) {
            final String version = versionFromJarName(
                "quantum-cli",
                cliJar.getFileName().toString()
            );
            if (version != null) {
                return version;
            }
        }
        return "unknown";
    }

    private static String readRevision(final Path projectRoot) throws IOException {
        final Path pom = projectRoot.toAbsolutePath().normalize().resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            return null;
        }
        final String content = Files.readString(
            pom,
            StandardCharsets.UTF_8
        );
        final String revision = tagValue(
            content,
            "revision"
        );
        if (revision != null) {
            return revision;
        }
        final String version = tagValue(
            content,
            "version"
        );
        if (version != null && !version.contains("${")) {
            return version;
        }
        return null;
    }

    private static String tagValue(
        final String content,
        final String tag
    ) {
        final String startTag = "<" + tag + ">";
        final String endTag = "</" + tag + ">";
        final int start = content.indexOf(startTag);
        final int end = content.indexOf(
            endTag,
            start + startTag.length()
        );
        if (start < 0 || end <= start) {
            return null;
        }
        final String value = content.substring(
            start + startTag.length(),
            end
        ).trim();
        if (value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String versionFromJarName(
        final String module,
        final String fileName
    ) {
        final String prefix = module + "-";
        final String suffix = ".jar";
        if (!fileName.startsWith(prefix) || !fileName.endsWith(suffix)) {
            return null;
        }
        return fileName.substring(
            prefix.length(),
            fileName.length() - suffix.length()
        );
    }

    private static boolean isAuxiliaryJar(final String fileName) {
        return fileName.startsWith("original-")
            || fileName.endsWith("-sources.jar")
            || fileName.endsWith("-javadoc.jar")
            || fileName.endsWith("-tests.jar");
    }
}