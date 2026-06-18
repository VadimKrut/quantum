/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import java.util.Map;

/**
 * Хранит параметры одного автоматического screenshot-прогона desktop UI.
 */
public final class DesktopScreenshotRequest {

    private static final String DEFAULT_FIXTURE = "blank";
    private static final String DEFAULT_TAB = "Q-Sphere";
    private static final String DEFAULT_PATH = "target/desktop-q-sphere-screenshot.png";

    private final String fixtureName;
    private final String tabName;
    private final String outputPath;
    private final Integer inspectStep;
    private final Integer operationIndex;

    private DesktopScreenshotRequest(
        final String fixtureName,
        final String tabName,
        final String outputPath,
        final Integer inspectStep,
        final Integer operationIndex
    ) {
        this.fixtureName = fixtureName;
        this.tabName = tabName;
        this.outputPath = outputPath;
        this.inspectStep = inspectStep;
        this.operationIndex = operationIndex;
    }

    public static DesktopScreenshotRequest from(final Map<String, String> parameters) {
        return new DesktopScreenshotRequest(
            canonicalFixtureName(firstPresent(
                parameters,
                DEFAULT_FIXTURE,
                "fixture",
                "template"
            )),
            firstPresent(
                parameters,
                DEFAULT_TAB,
                "screenshot-tab",
                "tab"
            ),
            firstPresent(
                parameters,
                DEFAULT_PATH,
                "screenshot-path",
                "screenshot-output",
                "output"
            ),
            optionalInteger(
                parameters,
                "inspect-step"
            ),
            optionalInteger(
                parameters,
                "screenshot-operation-index",
                "operation-index"
            )
        );
    }

    public String fixtureName() {
        return fixtureName;
    }

    public String tabName() {
        return tabName;
    }

    public String outputPath() {
        return outputPath;
    }

    public Integer inspectStep() {
        return inspectStep;
    }

    public Integer operationIndex() {
        return operationIndex;
    }

    private static String canonicalFixtureName(final String fixtureName) {
        if (fixtureName == null) {
            return DEFAULT_FIXTURE;
        }
        final String normalized = fixtureName.trim().toLowerCase();
        if (
            normalized.startsWith("dense")
            || normalized.contains("spectrum")
        ) {
            return "dense-spectrum";
        }
        if (
            normalized.startsWith("qft")
            || normalized.contains("qft")
        ) {
            return "qft16";
        }
        if (
            normalized.startsWith("chem")
            || normalized.contains("ansatz")
        ) {
            return "chemistry16";
        }
        if (
            normalized.startsWith("grover")
            || normalized.contains("oracle")
        ) {
            return "grover16";
        }
        if (normalized.startsWith("bell")) {
            return "bell";
        }
        return DEFAULT_FIXTURE;
    }

    private static String firstPresent(
        final Map<String, String> parameters,
        final String fallback,
        final String... keys
    ) {
        final String key = firstPresentKey(
            parameters,
            keys
        );
        return key == null ? fallback : parameters.get(key);
    }

    private static Integer optionalInteger(
        final Map<String, String> parameters,
        final String... keys
    ) {
        final String key = firstPresentKey(
            parameters,
            keys
        );
        if (key == null) {
            return null;
        }
        try {
            return Integer.valueOf(parameters.get(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalArgumentException(
                "Screenshot parameter " + key + " must be an integer.",
                exception
            );
        }
    }

    private static String firstPresentKey(
        final Map<String, String> parameters,
        final String... keys
    ) {
        for (final String key : keys) {
            final String value = parameters.get(key);
            if (
                value != null
                && !value.isBlank()
            ) {
                return key;
            }
        }
        return null;
    }
}