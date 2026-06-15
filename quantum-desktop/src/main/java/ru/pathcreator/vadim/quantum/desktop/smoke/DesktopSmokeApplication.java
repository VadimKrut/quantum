/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.smoke;

import java.io.PrintStream;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Headless entry point для packaged desktop smoke-проверки.
 */
public final class DesktopSmokeApplication {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;

    private DesktopSmokeApplication() {
    }

    public static int run(
        final String[] args,
        final PrintStream out,
        final PrintStream err
    ) {
        try {
            final Path projectRoot = pathOption(
                args,
                "--project-root",
                Path.of(".")
            );
            final Path corpusRoot = pathOption(
                args,
                "--corpus",
                Path.of("examples")
            );
            final DesktopSmokeReport report = new DesktopSmokeRunner().run(
                projectRoot,
                corpusRoot
            );
            new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(
                    out,
                    report
                );
            out.println();
            return report.isSuccess()
                ? EXIT_SUCCESS
                : EXIT_FAILURE;
        } catch (final Exception exception) {
            err.println(exception.getMessage() == null
                ? exception.toString()
                : exception.getMessage());
            return EXIT_FAILURE;
        }
    }

    private static Path pathOption(
        final String[] args,
        final String name,
        final Path defaultValue
    ) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return Path.of(args[i + 1]);
            }
        }
        return defaultValue;
    }
}