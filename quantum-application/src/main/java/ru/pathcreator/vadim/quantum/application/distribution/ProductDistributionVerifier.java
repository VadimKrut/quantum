/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.distribution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Проверяет распакованный Quantum distribution bundle по manifest и обязательным файлам.
 */
public final class ProductDistributionVerifier {

    private static final String FORMAT = "quantum-product-distribution";
    private static final String MANIFEST_FILE = "manifest.properties";

    public ProductDistributionVerificationResult verify(final Path distributionDirectory) {
        if (distributionDirectory == null) {
            throw new IllegalArgumentException("Product distribution directory must not be null.");
        }
        final Path directory = distributionDirectory.toAbsolutePath().normalize();
        final Path archive = directory.resolveSibling(directory.getFileName().toString() + ".zip");
        final ArrayList<ProductDistributionVerificationIssue> issues = new ArrayList<>();
        long verifiedFiles = 0L;

        if (!Files.isDirectory(directory)) {
            issues.add(issue(
                "DISTRIBUTION_DIRECTORY_MISSING",
                directory.toString(),
                "Product distribution directory does not exist."
            ));
            return ProductDistributionVerificationResult.of(
                directory,
                archive,
                Files.isRegularFile(archive),
                0L,
                issues
            );
        }

        final Path manifestPath = directory.resolve(MANIFEST_FILE);
        final Map<String, String> manifest = readManifest(
            manifestPath,
            issues
        );
        if (!manifest.isEmpty()) {
            verifyManifestHeader(
                manifest,
                manifestPath,
                issues
            );
            verifiedFiles = verifyManifestFiles(
                directory,
                manifest,
                issues
            );
        }
        verifyRequiredFiles(
            directory,
            issues
        );
        verifyArchive(
            archive,
            issues
        );
        return ProductDistributionVerificationResult.of(
            directory,
            archive,
            Files.isRegularFile(archive),
            verifiedFiles,
            issues
        );
    }

    private static Map<String, String> readManifest(
        final Path manifestPath,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        final HashMap<String, String> manifest = new HashMap<>();
        if (!Files.isRegularFile(manifestPath)) {
            issues.add(issue(
                "MANIFEST_MISSING",
                manifestPath.toString(),
                "Distribution manifest is missing."
            ));
            return manifest;
        }
        try {
            final List<String> lines = Files.readAllLines(
                manifestPath,
                StandardCharsets.UTF_8
            );
            for (int index = 0; index < lines.size(); index++) {
                final String line = lines.get(index);
                final int separator = line.indexOf('=');
                if (separator > 0) {
                    manifest.put(
                        line.substring(0, separator),
                        line.substring(separator + 1)
                    );
                }
            }
        } catch (final IOException exception) {
            issues.add(issue(
                "MANIFEST_READ_FAILED",
                manifestPath.toString(),
                exception.getMessage()
            ));
        }
        return manifest;
    }

    private static void verifyManifestHeader(
        final Map<String, String> manifest,
        final Path manifestPath,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        if (!FORMAT.equals(manifest.get("format"))) {
            issues.add(issue(
                "MANIFEST_FORMAT_INVALID",
                manifestPath.toString(),
                "Distribution manifest format is invalid."
            ));
        }
        if (!"1".equals(manifest.get("version"))) {
            issues.add(issue(
                "MANIFEST_VERSION_INVALID",
                manifestPath.toString(),
                "Distribution manifest version is invalid."
            ));
        }
        if (manifest.getOrDefault(
            "projectVersion",
            ""
        ).isBlank()) {
            issues.add(issue(
                "PROJECT_VERSION_INVALID",
                manifestPath.toString(),
                "Distribution project version is missing."
            ));
        }
    }

    private static long verifyManifestFiles(
        final Path directory,
        final Map<String, String> manifest,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        final long count = parseLong(
            manifest.get("fileCount"),
            -1L
        );
        if (count < 0L) {
            issues.add(issue(
                "MANIFEST_FILE_COUNT_INVALID",
                directory.resolve(MANIFEST_FILE).toString(),
                "Distribution manifest file count is invalid."
            ));
            return 0L;
        }
        long verified = 0L;
        for (long index = 0L; index < count; index++) {
            final String prefix = "file." + index + ".";
            final String path = manifest.get(prefix + "path");
            final String expectedBytes = manifest.get(prefix + "bytes");
            final String expectedSha = manifest.get(prefix + "sha256");
            if (path == null || expectedBytes == null || expectedSha == null) {
                issues.add(issue(
                    "MANIFEST_FILE_ENTRY_INCOMPLETE",
                    prefix,
                    "Distribution manifest file entry is incomplete."
                ));
                continue;
            }
            verifyManifestFile(
                directory,
                path,
                parseLong(
                    expectedBytes,
                    -1L
                ),
                expectedSha,
                issues
            );
            verified++;
        }
        return verified;
    }

    private static void verifyManifestFile(
        final Path directory,
        final String manifestPath,
        final long expectedBytes,
        final String expectedSha,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        final Path file = directory.resolve(manifestPath.replace(
            '/',
            java.io.File.separatorChar
        )).normalize();
        if (!file.startsWith(directory)) {
            issues.add(issue(
                "MANIFEST_PATH_ESCAPES_DISTRIBUTION",
                manifestPath,
                "Distribution manifest path escapes the distribution directory."
            ));
            return;
        }
        if (!Files.isRegularFile(file)) {
            issues.add(issue(
                "MANIFEST_FILE_MISSING",
                manifestPath,
                "Manifest file is missing from distribution."
            ));
            return;
        }
        try {
            final long actualBytes = Files.size(file);
            if (actualBytes != expectedBytes) {
                issues.add(issue(
                    "MANIFEST_FILE_SIZE_MISMATCH",
                    manifestPath,
                    "Manifest file byte size does not match."
                ));
            }
            final String actualSha = sha256(file);
            if (!actualSha.equals(expectedSha)) {
                issues.add(issue(
                    "MANIFEST_FILE_SHA256_MISMATCH",
                    manifestPath,
                    "Manifest file SHA-256 does not match."
                ));
            }
        } catch (final IOException exception) {
            issues.add(issue(
                "MANIFEST_FILE_READ_FAILED",
                manifestPath,
                exception.getMessage()
            ));
        }
    }

    private static void verifyRequiredFiles(
        final Path directory,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        final String[] required = new String[] {
            "README.md",
            "LICENSE",
            "tools/quantum.ps1",
            "tools/quantum-desktop.ps1",
            "tools/product-smoke.ps1",
            "tools/verify-distribution.ps1"
        };
        for (int index = 0; index < required.length; index++) {
            final Path file = directory.resolve(required[index].replace(
                '/',
                java.io.File.separatorChar
            ));
            if (!Files.isRegularFile(file)) {
                issues.add(issue(
                    "REQUIRED_FILE_MISSING",
                    required[index],
                    "Required distribution file is missing."
                ));
            }
        }
        verifyRequiredJar(
            directory,
            "quantum-cli",
            issues
        );
        verifyRequiredJar(
            directory,
            "quantum-desktop",
            issues
        );
    }

    private static void verifyRequiredJar(
        final Path directory,
        final String module,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        final Path library = directory.resolve("lib");
        if (!Files.isDirectory(library)) {
            issues.add(issue(
                "REQUIRED_FILE_MISSING",
                "lib/" + module + "-*.jar",
                "Required distribution jar is missing."
            ));
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
            library,
            module + "-*.jar"
        )) {
            for (final Path ignored : stream) {
                return;
            }
            issues.add(issue(
                "REQUIRED_FILE_MISSING",
                "lib/" + module + "-*.jar",
                "Required distribution jar is missing."
            ));
        } catch (final IOException exception) {
            issues.add(issue(
                "REQUIRED_FILE_READ_FAILED",
                "lib/" + module + "-*.jar",
                exception.getMessage()
            ));
        }
    }

    private static void verifyArchive(
        final Path archive,
        final List<ProductDistributionVerificationIssue> issues
    ) {
        if (!Files.isRegularFile(archive)) {
            issues.add(issue(
                "ARCHIVE_MISSING",
                archive.toString(),
                "Distribution archive is missing."
            ));
            return;
        }
        try {
            if (Files.size(archive) == 0L) {
                issues.add(issue(
                    "ARCHIVE_EMPTY",
                    archive.toString(),
                    "Distribution archive is empty."
                ));
            }
        } catch (final IOException exception) {
            issues.add(issue(
                "ARCHIVE_READ_FAILED",
                archive.toString(),
                exception.getMessage()
            ));
        }
    }

    private static long parseLong(
        final String value,
        final long fallback
    ) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException exception) {
            return fallback;
        }
    }

    private static ProductDistributionVerificationIssue issue(
        final String code,
        final String path,
        final String message
    ) {
        return ProductDistributionVerificationIssue.of(
            code,
            path,
            message
        );
    }

    private static String sha256(final Path path) throws IOException {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(Files.readAllBytes(path));
            final StringBuilder value = new StringBuilder(hash.length * 2);
            for (int i = 0; i < hash.length; i++) {
                final String hex = Integer.toHexString(hash[i] & 0xff);
                if (hex.length() == 1) {
                    value.append('0');
                }
                value.append(hex);
            }
            return value.toString();
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 digest is not available.",
                exception
            );
        }
    }
}