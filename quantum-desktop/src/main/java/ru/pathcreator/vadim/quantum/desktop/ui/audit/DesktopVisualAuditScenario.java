/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import java.nio.file.Path;
import java.util.List;

/**
 * Описывает один сценарий матричного screenshot-аудита desktop-визуализации.
 */
public record DesktopVisualAuditScenario(
    String name,
    String fixtureName,
    String tabName,
    Integer inspectStep,
    Integer operationIndex,
    String outputPath
) {

    public static List<DesktopVisualAuditScenario> defaults(final String outputDirectory) {
        final Path root = Path.of(outputDirectory);
        return List.of(
            scenario("dense-spectrum-circuit", "dense-spectrum", "Visual Circuit", 12, 12, root),
            scenario("dense-spectrum-gate-info", "dense-spectrum", "Gate Info", 12, 12, root),
            scenario("dense-spectrum-full-ir-surface", "dense-spectrum", "Full IR Surface", 12, 12, root),
            scenario("qft16-late-measure-circuit", "qft16", "Visual Circuit", 229, 229, root),
            scenario("qft16-qsphere", "qft16", "Q-Sphere", 218, 218, root),
            scenario("qft16-resources", "qft16", "Resources", 218, 218, root),
            scenario("qft16-java-dsl", "qft16", "Java DSL", 218, 218, root),
            scenario("chemistry16-qsphere", "chemistry16", "Q-Sphere", 160, 160, root),
            scenario("grover16-circuit", "grover16", "Visual Circuit", 180, 180, root),
            scenario("grover16-controlled-gate-circuit", "grover16", "Visual Circuit", 112, 112, root),
            scenario("grover16-preflight", "grover16", "Preflight", 112, 112, root),
            scenario("grover16-compatibility", "grover16", "Compatibility", 112, 112, root),
            scenario("bell-simulation", "bell", "Simulation", null, null, root),
            scenario("bell-native-json", "bell", "Native JSON", null, null, root),
            scenario("bell-generated-export", "bell", "Generated Export", null, null, root),
            scenario("bell-diagnostics", "bell", "Diagnostics", null, null, root),
            scenario("dense-spectrum-transform", "dense-spectrum", "Transform", 12, 12, root),
            scenario("external-openqasm2-to-openqasm3", "bell", "External Formats", null, null, root),
            scenario("dense-spectrum-inspector", "dense-spectrum", "Inspector", 12, 12, root)
        );
    }

    private static DesktopVisualAuditScenario scenario(
        final String name,
        final String fixtureName,
        final String tabName,
        final Integer inspectStep,
        final Integer operationIndex,
        final Path outputDirectory
    ) {
        return new DesktopVisualAuditScenario(
            name,
            fixtureName,
            tabName,
            inspectStep,
            operationIndex,
            outputDirectory.resolve(name + ".png").toString()
        );
    }
}