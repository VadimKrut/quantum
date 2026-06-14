/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.style;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeStyleTest {

    private static final String EXPECTED_HEADER = """
        /*
         * Copyright 2026 Vadim Aleksandrovich Zaletaev
         *
         * This Source Code Form is subject to the terms of the Mozilla Public
         * License, v. 2.0. If a copy of the MPL was not distributed with this
         * file, You can obtain one at https://mozilla.org/MPL/2.0/.
         * SPDX-License-Identifier: MPL-2.0
         */""";

    private static final Pattern TYPE_DECLARATION = Pattern.compile(
        "\\b(public\\s+)?(final\\s+)?(class|interface|enum|record)\\s+\\w[^\\{]*\\{"
    );

    private static final String[] FORBIDDEN_MOJIBAKE_MARKERS = new String[] {
        "\u0420\u040E",
        "\u0420\u045F",
        "\u0420\u0491",
        "\u0420\u00B5",
        "\u0420\u00B0",
        "\u0420\u0451",
        "\u0421\u0402",
        "\u0421\u201A",
        "\u0421\u040C",
        "\u0421\u2039"
    };

    private static final String[] MODULES = new String[] {
        "quantum-core",
        "quantum-application",
        "quantum-json",
        "quantum-openqasm2",
        "quantum-openqasm3",
        "quantum-quil",
        "quantum-api"
    };

    @Test
    void javaFilesFollowProjectCodeStyle() throws IOException {
        final ArrayList<Path> files = new ArrayList<>();
        final Path projectRoot = projectRoot();
        for (int index = 0; index < MODULES.length; index++) {
            collectJavaFiles(
                projectRoot.resolve(MODULES[index]).resolve(Path.of("src", "main", "java")),
                files
            );
            collectJavaFiles(
                projectRoot.resolve(MODULES[index]).resolve(Path.of("src", "test", "java")),
                files
            );
        }

        final ArrayList<String> issues = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            inspectJavaFile(files.get(index), issues);
        }

        assertTrue(issues.isEmpty(), issueMessage(issues));
    }

    private static Path projectRoot() {
        final Path current = Path.of("").toAbsolutePath().normalize();
        for (int index = 0; index < MODULES.length; index++) {
            if (Files.isDirectory(current.resolve(MODULES[index]))) {
                return current;
            }
        }
        final Path parent = current.getParent();
        if (parent != null) {
            for (int index = 0; index < MODULES.length; index++) {
                if (Files.isDirectory(parent.resolve(MODULES[index]))) {
                    return parent;
                }
            }
        }
        return current;
    }

    private static void collectJavaFiles(
        final Path root,
        final ArrayList<Path> files
    ) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (final DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
            for (final Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    collectJavaFiles(entry, files);
                } else if (entry.getFileName().toString().endsWith(".java")) {
                    files.add(entry);
                }
            }
        }
    }

    private static void inspectJavaFile(
        final Path path,
        final ArrayList<String> issues
    ) throws IOException {
        final byte[] data = Files.readAllBytes(path);
        if (startsWithBom(data)) {
            issues.add("BOM: " + path);
        }
        if (endsWithLineBreak(data)) {
            issues.add("Final line break after last brace: " + path);
        }

        final String text = new String(data, StandardCharsets.UTF_8);
        final String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if (!normalized.startsWith(EXPECTED_HEADER)) {
            issues.add("Header mismatch: " + path);
        }

        inspectLines(path, normalized, issues);
        inspectBlankLineAfterFirstType(path, normalized, issues);
    }

    private static boolean startsWithBom(final byte[] data) {
        return data.length >= 3
            && data[0] == (byte) 0xEF
            && data[1] == (byte) 0xBB
            && data[2] == (byte) 0xBF;
    }

    private static boolean endsWithLineBreak(final byte[] data) {
        return data.length > 0
            && (data[data.length - 1] == '\n' || data[data.length - 1] == '\r');
    }

    private static void inspectLines(
        final Path path,
        final String text,
        final ArrayList<String> issues
    ) {
        final String[] lines = text.split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            final String line = lines[index];
            final int lineNumber = index + 1;
            if (!line.equals(line.stripTrailing())) {
                issues.add("Trailing whitespace: " + path + ":" + lineNumber);
            }
            if (line.indexOf('\t') >= 0) {
                issues.add("Tab character: " + path + ":" + lineNumber);
            }
            if (containsForbiddenVar(line)) {
                issues.add("Forbidden local type inference: " + path + ":" + lineNumber);
            }
            if (containsForbiddenFunctionalApi(line)) {
                issues.add("Forbidden functional API usage: " + path + ":" + lineNumber);
            }
            if (containsForbiddenMojibake(line)) {
                issues.add("Mojibake text: " + path + ":" + lineNumber);
            }
        }
    }

    private static boolean containsForbiddenVar(final String line) {
        final String token = String.valueOf(new char[] {'v', 'a', 'r'});
        return line.contains(" " + token + " ") || line.stripLeading().startsWith(token + " ");
    }

    private static boolean containsForbiddenFunctionalApi(final String line) {
        return line.contains(".stream" + "()")
            || line.contains("java.util." + "stream")
            || line.contains("Optional" + "<");
    }

    private static boolean containsForbiddenMojibake(final String line) {
        for (int index = 0; index < FORBIDDEN_MOJIBAKE_MARKERS.length; index++) {
            if (line.contains(FORBIDDEN_MOJIBAKE_MARKERS[index])) {
                return true;
            }
        }
        return false;
    }

    private static void inspectBlankLineAfterFirstType(
        final Path path,
        final String text,
        final ArrayList<String> issues
    ) {
        final String[] lines = text.split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            if (TYPE_DECLARATION.matcher(lines[index]).find()) {
                if (index + 1 < lines.length && !lines[index + 1].isBlank()) {
                    issues.add(
                        "Missing blank line after type declaration: " + path + ":" + (index + 1)
                    );
                }
                return;
            }
        }
    }

    private static String issueMessage(final ArrayList<String> issues) {
        final StringBuilder message = new StringBuilder("Code style violations:");
        final int limit = Math.min(issues.size(), 100);
        for (int index = 0; index < limit; index++) {
            message.append(System.lineSeparator()).append(issues.get(index));
        }
        if (issues.size() > limit) {
            message.append(System.lineSeparator())
                .append("... and ")
                .append(issues.size() - limit)
                .append(" more");
        }
        return message.toString();
    }
}