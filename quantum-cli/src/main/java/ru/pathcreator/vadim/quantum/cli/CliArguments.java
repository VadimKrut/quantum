/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.cli;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;

final class CliArguments {

    private final String command;
    private final Map<String, String> options;
    private final java.util.Set<String> flags;

    private CliArguments(
        final String command,
        final Map<String, String> options,
        final java.util.Set<String> flags
    ) {
        this.command = command;
        this.options = Map.copyOf(options);
        this.flags = java.util.Set.copyOf(flags);
    }

    static CliArguments parse(final String[] args) {
        if (
            args == null
            || args.length == 0
        ) {
            return new CliArguments(
                "help",
                Map.of(),
                java.util.Set.of()
            );
        }
        final String command = normalize(args[0]);
        final LinkedHashMap<String, String> options = new LinkedHashMap<>();
        final java.util.LinkedHashSet<String> flags = new java.util.LinkedHashSet<>();
        int index = 1;
        while (index < args.length) {
            final String token = args[index];
            if (!token.startsWith("--")) {
                throw new CliUsageException("Unexpected positional argument: " + token + ".");
            }
            final String name = normalize(token.substring(2));
            if (isFlag(name)) {
                flags.add(name);
                index++;
            } else {
                if (index + 1 >= args.length) {
                    throw new CliUsageException("Option requires a value: --" + name + ".");
                }
                options.put(
                    name,
                    args[index + 1]
                );
                index += 2;
            }
        }
        return new CliArguments(
            command,
            options,
            flags
        );
    }

    String command() {
        return command;
    }

    boolean help() {
        return command.equals("help")
            || flag("help");
    }

    boolean hasInputPath() {
        return options.containsKey("input");
    }

    Path requiredInputPath() {
        if (!hasInputPath()) {
            throw new CliUsageException("Missing required option: --input.");
        }
        return Path.of(options.get("input"));
    }

    boolean hasOutputPath() {
        return options.containsKey("output");
    }

    Path outputPath() {
        return Path.of(options.get("output"));
    }

    Path pathOption(
        final String name,
        final Path defaultValue
    ) {
        final String value = options.get(normalize(name));
        return value == null
            ? defaultValue
            : Path.of(value);
    }

    String inputFormatName() {
        return normalize(options.getOrDefault(
            "input-format",
            "auto"
        ));
    }

    String outputFormatName() {
        return normalize(options.getOrDefault(
            "output-format",
            "json"
        ));
    }

    IntegrationFormat requiredOutputFormat() {
        final String value = outputFormatName();
        return switch (value) {
            case "openqasm2", "qasm2", "openqasm_2" -> IntegrationFormat.OPENQASM_2;
            case "openqasm3", "qasm3", "openqasm_3" -> IntegrationFormat.OPENQASM_3;
            case "quil" -> IntegrationFormat.QUIL;
            default -> throw new CliUsageException("Command requires --output-format openqasm2|openqasm3|quil.");
        };
    }

    String outputMode() {
        final String value = normalize(options.getOrDefault(
            "format",
            "text"
        ));
        if (
            !value.equals("text")
            && !value.equals("json")
        ) {
            throw new CliUsageException("--format must be text or json.");
        }
        return value;
    }

    boolean flag(final String name) {
        return flags.contains(normalize(name));
    }

    boolean hasOption(final String name) {
        return options.containsKey(normalize(name));
    }

    int intOption(
        final String name,
        final int defaultValue
    ) {
        final String value = options.get(normalize(name));
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CliUsageException("Option --" + name + " must be an integer.");
        }
    }

    long longOption(
        final String name,
        final long defaultValue
    ) {
        final String value = options.get(normalize(name));
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new CliUsageException("Option --" + name + " must be a long integer.");
        }
    }

    boolean booleanOption(
        final String name,
        final boolean defaultValue
    ) {
        final String value = options.get(normalize(name));
        if (value == null) {
            return defaultValue;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new CliUsageException("Option --" + name + " must be true or false.");
    }

    private static boolean isFlag(final String name) {
        return name.equals("help")
            || name.equals("fast")
            || name.equals("remove-identity")
            || name.equals("inline-composite")
            || name.equals("canonicalize-parameters")
            || name.equals("state-vector")
            || name.equals("skip-validation")
            || name.equals("skip-inspection")
            || name.equals("skip-preflight")
            || name.equals("skip-transformation")
            || name.equals("skip-transformed-validation")
            || name.equals("skip-transformed-inspection")
            || name.equals("skip-transformed-preflight")
            || name.equals("skip-resources")
            || name.equals("skip-timeline")
            || name.equals("skip-simulation")
            || name.equals("skip-compiler")
            || name.equals("skip-backend")
            || name.equals("skip-import");
    }

    private static String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }
}