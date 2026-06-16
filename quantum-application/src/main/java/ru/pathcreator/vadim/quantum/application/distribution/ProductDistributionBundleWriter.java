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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a local release distribution bundle from an already packaged project.
 */
public final class ProductDistributionBundleWriter {

    private static final String VERSION = "0.1.0";
    private static final String MANIFEST_FILE = "manifest.properties";

    public ProductDistributionBundleResult write(
        final Path outputDirectory,
        final Path projectRoot
    ) throws IOException {
        return write(
            outputDirectory,
            projectRoot,
            null
        );
    }

    public ProductDistributionBundleResult write(
        final Path outputDirectory,
        final Path projectRoot,
        final Path productReportDirectory
    ) throws IOException {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Product distribution output directory must not be null.");
        }
        if (projectRoot == null) {
            throw new IllegalArgumentException("Product distribution project root must not be null.");
        }
        final Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Product distribution project root must be a directory.");
        }
        final Path output = outputDirectory.toAbsolutePath().normalize();
        resetDirectory(output);

        final Path tools = output.resolve("tools");
        final Path libraries = output.resolve("lib");
        final Path examples = output.resolve("smoke-corpus");
        final Path report = output.resolve("report");
        Files.createDirectories(tools);
        Files.createDirectories(libraries);
        Files.createDirectories(examples);
        Files.createDirectories(report);

        final ArrayList<Path> files = new ArrayList<>(64);
        copyRequiredFile(
            root.resolve("LICENSE"),
            output.resolve("LICENSE"),
            files
        );
        copyOptionalFile(
            root.resolve("README.md"),
            output.resolve("PROJECT-README.md"),
            files
        );
        writeFile(
            output.resolve("README.md"),
            quickstart(),
            files
        );
        writeSmokeCorpus(
            examples,
            files
        );
        copyTools(
            root,
            tools,
            files
        );
        copyJars(
            root,
            libraries,
            files
        );
        if (productReportDirectory != null) {
            copyDirectory(
                productReportDirectory.toAbsolutePath().normalize(),
                report,
                files
            );
        } else {
            writeFile(
                report.resolve("README.md"),
                "# Product Report" + System.lineSeparator()
                    + System.lineSeparator()
                    + "Run `tools\\quantum.ps1 product-report` from a source checkout to create a fresh report."
                    + System.lineSeparator(),
                files
            );
        }

        files.sort(Comparator.comparing(path -> relative(output, path)));
        final Path manifest = output.resolve(MANIFEST_FILE);
        writeFile(
            manifest,
            manifest(output, files),
            files
        );
        files.sort(Comparator.comparing(path -> relative(output, path)));
        final Path archive = output.resolveSibling(output.getFileName().toString() + ".zip");
        writeArchive(
            output,
            archive,
            files
        );
        return ProductDistributionBundleResult.of(
            output,
            archive,
            sha256(archive),
            manifest,
            output.resolve("README.md"),
            output.resolve("LICENSE"),
            examples,
            tools,
            libraries,
            report,
            files
        );
    }

    private static void copyTools(
        final Path root,
        final Path tools,
        final List<Path> files
    ) throws IOException {
        writeFile(
            tools.resolve("quantum.ps1"),
            cliLauncher(),
            files
        );
        writeFile(
            tools.resolve("quantum-desktop.ps1"),
            desktopLauncher(),
            files
        );
        writeFile(
            tools.resolve("product-smoke.ps1"),
            distributionSmoke(),
            files
        );
        writeFile(
            tools.resolve("verify-distribution.ps1"),
            integrityVerifier(),
            files
        );
    }

    private static void writeSmokeCorpus(
        final Path directory,
        final List<Path> files
    ) throws IOException {
        writeFile(
            directory.resolve("openqasm2").resolve("bell.qasm"),
            "OPENQASM 2.0;" + System.lineSeparator()
                + "include \"qelib1.inc\";" + System.lineSeparator()
                + "qreg q[2];" + System.lineSeparator()
                + "creg c[2];" + System.lineSeparator()
                + "h q[0];" + System.lineSeparator()
                + "cx q[0],q[1];" + System.lineSeparator()
                + "measure q[0] -> c[0];" + System.lineSeparator()
                + "measure q[1] -> c[1];" + System.lineSeparator(),
            files
        );
        writeFile(
            directory.resolve("openqasm3").resolve("ghz.qasm"),
            "OPENQASM 3.0;" + System.lineSeparator()
                + "include \"stdgates.inc\";" + System.lineSeparator()
                + "qubit[3] q;" + System.lineSeparator()
                + "bit[3] c;" + System.lineSeparator()
                + "h q[0];" + System.lineSeparator()
                + "cx q[0], q[1];" + System.lineSeparator()
                + "cx q[1], q[2];" + System.lineSeparator()
                + "c = measure q;" + System.lineSeparator(),
            files
        );
        writeFile(
            directory.resolve("quil").resolve("bell.quil"),
            "DECLARE ro BIT[2]" + System.lineSeparator()
                + "H 0" + System.lineSeparator()
                + "CNOT 0 1" + System.lineSeparator()
                + "MEASURE 0 ro[0]" + System.lineSeparator()
                + "MEASURE 1 ro[1]" + System.lineSeparator(),
            files
        );
    }

    private static void copyJars(
        final Path root,
        final Path libraries,
        final List<Path> files
    ) throws IOException {
        copyRequiredFile(
            root.resolve("quantum-cli").resolve("target").resolve("quantum-cli-" + VERSION + ".jar"),
            libraries.resolve("quantum-cli-" + VERSION + ".jar"),
            files
        );
        copyRequiredFile(
            root.resolve("quantum-desktop").resolve("target").resolve("quantum-desktop-" + VERSION + ".jar"),
            libraries.resolve("quantum-desktop-" + VERSION + ".jar"),
            files
        );
    }

    private static void copyRequiredFile(
        final Path source,
        final Path target,
        final List<Path> files
    ) throws IOException {
        ensureRequiredFile(source);
        Files.createDirectories(target.getParent());
        Files.copy(
            source,
            target,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        );
        files.add(target);
    }

    private static void copyOptionalFile(
        final Path source,
        final Path target,
        final List<Path> files
    ) throws IOException {
        if (!Files.isRegularFile(source)) {
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(
            source,
            target,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        );
        files.add(target);
    }

    private static void ensureRequiredFile(final Path source) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IOException("Required distribution file is missing: " + source);
        }
    }

    private static void copyDirectory(
        final Path source,
        final Path target,
        final List<Path> files
    ) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IOException("Required distribution directory is missing: " + source);
        }
        Files.walkFileTree(
            source,
            new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(
                    final Path directory,
                    final BasicFileAttributes attributes
                ) throws IOException {
                    Files.createDirectories(target.resolve(source.relativize(directory)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(
                    final Path file,
                    final BasicFileAttributes attributes
                ) throws IOException {
                    final Path destination = target.resolve(source.relativize(file));
                    Files.createDirectories(destination.getParent());
                    Files.copy(
                        file,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    );
                    files.add(destination);
                    return FileVisitResult.CONTINUE;
                }
            }
        );
    }

    private static void writeFile(
        final Path path,
        final String content,
        final List<Path> files
    ) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(
            path,
            content,
            StandardCharsets.UTF_8
        );
        files.add(path);
    }

    private static void writeArchive(
        final Path output,
        final Path archive,
        final List<Path> files
    ) throws IOException {
        Files.createDirectories(archive.getParent());
        Files.deleteIfExists(archive);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            for (int index = 0; index < files.size(); index++) {
                final Path file = files.get(index);
                final ZipEntry entry = new ZipEntry(output.getFileName().toString() + "/" + relative(output, file));
                zip.putNextEntry(entry);
                Files.copy(
                    file,
                    zip
                );
                zip.closeEntry();
            }
        }
    }

    private static void resetDirectory(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            return;
        }
        if (!Files.isDirectory(directory)) {
            throw new IOException("Product distribution output path is not a directory: " + directory);
        }
        Files.walkFileTree(
            directory,
            new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(
                    final Path file,
                    final BasicFileAttributes attributes
                ) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(
                    final Path current,
                    final IOException exception
                ) throws IOException {
                    if (exception != null) {
                        throw exception;
                    }
                    if (!current.equals(directory)) {
                        Files.delete(current);
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        );
    }

    private static String quickstart() {
        return "# Quantum Distribution" + System.lineSeparator()
            + System.lineSeparator()
            + "This bundle contains packaged CLI, desktop UI, smoke corpus, tools, license, and product report files."
            + System.lineSeparator()
            + System.lineSeparator()
            + "## Commands" + System.lineSeparator()
            + System.lineSeparator()
            + "```powershell" + System.lineSeparator()
            + ".\\tools\\quantum.ps1 help" + System.lineSeparator()
            + ".\\tools\\quantum.ps1 validate --input .\\smoke-corpus\\openqasm2\\bell.qasm --format json" + System.lineSeparator()
            + ".\\tools\\quantum.ps1 simulate --input .\\smoke-corpus\\openqasm2\\bell.qasm --shots 1024 --seed 1 --format json" + System.lineSeparator()
            + ".\\tools\\quantum-desktop.ps1" + System.lineSeparator()
            + "```" + System.lineSeparator()
            + System.lineSeparator()
            + "If the source checkout has a README, it is included as `PROJECT-README.md`."
            + System.lineSeparator();
    }

    private static String cliLauncher() {
        return "param(" + System.lineSeparator()
            + "    [Parameter(ValueFromRemainingArguments = $true)]" + System.lineSeparator()
            + "    [string[]] $CliArguments" + System.lineSeparator()
            + ")" + System.lineSeparator()
            + System.lineSeparator()
            + "$ErrorActionPreference = 'Stop'" + System.lineSeparator()
            + "$tools = $PSScriptRoot" + System.lineSeparator()
            + "$bundle = Split-Path -Parent $tools" + System.lineSeparator()
            + "$jar = Join-Path $bundle 'lib\\quantum-cli-" + VERSION + ".jar'" + System.lineSeparator()
            + "$java = if (Test-Path \"$env:USERPROFILE\\.jdks\\graalvm-jdk-25\\bin\\java.exe\") {" + System.lineSeparator()
            + "    \"$env:USERPROFILE\\.jdks\\graalvm-jdk-25\\bin\\java.exe\"" + System.lineSeparator()
            + "} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\\java.exe'))) {" + System.lineSeparator()
            + "    Join-Path $env:JAVA_HOME 'bin\\java.exe'" + System.lineSeparator()
            + "} else {" + System.lineSeparator()
            + "    'java'" + System.lineSeparator()
            + "}" + System.lineSeparator()
            + "if (!(Test-Path $jar)) { throw \"CLI jar was not found: $jar\" }" + System.lineSeparator()
            + "& $java -jar $jar @CliArguments" + System.lineSeparator()
            + "exit $LASTEXITCODE" + System.lineSeparator();
    }

    private static String desktopLauncher() {
        return "$ErrorActionPreference = 'Stop'" + System.lineSeparator()
            + "$tools = $PSScriptRoot" + System.lineSeparator()
            + "$bundle = Split-Path -Parent $tools" + System.lineSeparator()
            + "$jar = Join-Path $bundle 'lib\\quantum-desktop-" + VERSION + ".jar'" + System.lineSeparator()
            + "if (!(Test-Path $jar)) { throw \"Desktop jar was not found: $jar\" }" + System.lineSeparator()
            + "java -jar $jar" + System.lineSeparator()
            + "exit $LASTEXITCODE" + System.lineSeparator();
    }

    private static String distributionSmoke() {
        return "param(" + System.lineSeparator()
            + "    [int] $Port = 18087" + System.lineSeparator()
            + ")" + System.lineSeparator()
            + System.lineSeparator()
            + "$ErrorActionPreference = 'Stop'" + System.lineSeparator()
            + "$tools = $PSScriptRoot" + System.lineSeparator()
            + "$bundle = Split-Path -Parent $tools" + System.lineSeparator()
            + "$java = if (Test-Path \"$env:USERPROFILE\\.jdks\\graalvm-jdk-25\\bin\\java.exe\") {" + System.lineSeparator()
            + "    \"$env:USERPROFILE\\.jdks\\graalvm-jdk-25\\bin\\java.exe\"" + System.lineSeparator()
            + "} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\\java.exe'))) {" + System.lineSeparator()
            + "    Join-Path $env:JAVA_HOME 'bin\\java.exe'" + System.lineSeparator()
            + "} else {" + System.lineSeparator()
            + "    'java'" + System.lineSeparator()
            + "}" + System.lineSeparator()
            + "& (Join-Path $tools 'verify-distribution.ps1') | Out-Null" + System.lineSeparator()
            + "& (Join-Path $tools 'quantum.ps1') help | Out-Null" + System.lineSeparator()
            + "$bell = Join-Path $bundle 'smoke-corpus\\openqasm2\\bell.qasm'" + System.lineSeparator()
            + "& (Join-Path $tools 'quantum.ps1') validate --input $bell --format json | Out-Null" + System.lineSeparator()
            + "& (Join-Path $tools 'quantum.ps1') simulate --input $bell --shots 64 --seed 5 --format json | Out-Null" + System.lineSeparator()
            + "& (Join-Path $tools 'quantum.ps1') compile --input $bell --output-format openqasm3 --format json | Out-Null" + System.lineSeparator()
            + "if (!(Test-Path (Join-Path $bundle 'lib\\quantum-desktop-" + VERSION + ".jar'))) { throw 'Desktop jar is missing.' }" + System.lineSeparator()
            + "Write-Output 'Quantum distribution smoke passed.'" + System.lineSeparator();
    }

    private static String integrityVerifier() {
        return "$ErrorActionPreference = 'Stop'" + System.lineSeparator()
            + "$tools = $PSScriptRoot" + System.lineSeparator()
            + "$bundle = Split-Path -Parent $tools" + System.lineSeparator()
            + "$manifestPath = Join-Path $bundle 'manifest.properties'" + System.lineSeparator()
            + "if (!(Test-Path -LiteralPath $manifestPath)) { throw \"Manifest was not found: $manifestPath\" }" + System.lineSeparator()
            + "$manifest = @{}" + System.lineSeparator()
            + "Get-Content -LiteralPath $manifestPath | ForEach-Object {" + System.lineSeparator()
            + "    if ($_ -and $_.Contains('=')) {" + System.lineSeparator()
            + "        $index = $_.IndexOf('=')" + System.lineSeparator()
            + "        $manifest[$_.Substring(0, $index)] = $_.Substring($index + 1)" + System.lineSeparator()
            + "    }" + System.lineSeparator()
            + "}" + System.lineSeparator()
            + "if ($manifest['format'] -ne 'quantum-product-distribution') { throw 'Unexpected distribution manifest format.' }" + System.lineSeparator()
            + "$count = [int]$manifest['fileCount']" + System.lineSeparator()
            + "for ($i = 0; $i -lt $count; $i++) {" + System.lineSeparator()
            + "    $path = $manifest[\"file.$i.path\"]" + System.lineSeparator()
            + "    $expectedBytes = [int64]$manifest[\"file.$i.bytes\"]" + System.lineSeparator()
            + "    $expectedSha = $manifest[\"file.$i.sha256\"]" + System.lineSeparator()
            + "    $file = Join-Path $bundle ($path -replace '/', [System.IO.Path]::DirectorySeparatorChar)" + System.lineSeparator()
            + "    if (!(Test-Path -LiteralPath $file)) { throw \"Manifest file is missing: $path\" }" + System.lineSeparator()
            + "    $actualBytes = (Get-Item -LiteralPath $file).Length" + System.lineSeparator()
            + "    if ($actualBytes -ne $expectedBytes) { throw \"Manifest byte mismatch for $path\" }" + System.lineSeparator()
            + "    $actualSha = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()" + System.lineSeparator()
            + "    if ($actualSha -ne $expectedSha) { throw \"Manifest SHA-256 mismatch for $path\" }" + System.lineSeparator()
            + "}" + System.lineSeparator()
            + "$required = @(" + System.lineSeparator()
            + "    'README.md'," + System.lineSeparator()
            + "    'LICENSE'," + System.lineSeparator()
            + "    'tools\\quantum.ps1'," + System.lineSeparator()
            + "    'tools\\quantum-desktop.ps1'," + System.lineSeparator()
            + "    'tools\\product-smoke.ps1'," + System.lineSeparator()
            + "    'tools\\verify-distribution.ps1'," + System.lineSeparator()
            + "    'lib\\quantum-cli-" + VERSION + ".jar'," + System.lineSeparator()
            + "    'lib\\quantum-desktop-" + VERSION + ".jar'" + System.lineSeparator()
            + ")" + System.lineSeparator()
            + "foreach ($relative in $required) {" + System.lineSeparator()
            + "    $file = Join-Path $bundle $relative" + System.lineSeparator()
            + "    if (!(Test-Path -LiteralPath $file)) { throw \"Required distribution file is missing: $relative\" }" + System.lineSeparator()
            + "}" + System.lineSeparator()
            + "Write-Output \"Quantum distribution verified: $count file(s).\"" + System.lineSeparator();
    }

    private static String manifest(
        final Path output,
        final List<Path> files
    ) throws IOException {
        final StringBuilder manifest = new StringBuilder(files.size() * 160);
        manifest.append("format=quantum-product-distribution").append(System.lineSeparator())
            .append("version=1").append(System.lineSeparator())
            .append("projectVersion=").append(VERSION).append(System.lineSeparator())
            .append("createdAt=").append(Instant.now()).append(System.lineSeparator())
            .append("fileCount=").append(files.size()).append(System.lineSeparator());
        for (int index = 0; index < files.size(); index++) {
            final Path file = files.get(index);
            final String prefix = "file." + index + ".";
            manifest.append(prefix).append("path=").append(relative(output, file)).append(System.lineSeparator())
                .append(prefix).append("bytes=").append(Files.size(file)).append(System.lineSeparator())
                .append(prefix).append("sha256=").append(sha256(file)).append(System.lineSeparator());
        }
        return manifest.toString();
    }

    private static String relative(
        final Path root,
        final Path file
    ) {
        return root.relativize(file).toString().replace(
            '\\',
            '/'
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