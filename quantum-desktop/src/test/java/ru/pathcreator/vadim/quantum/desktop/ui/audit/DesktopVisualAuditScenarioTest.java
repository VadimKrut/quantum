/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class DesktopVisualAuditScenarioTest {

    @Test
    void defaultMatrixCoversCircuitQSphereAndSimulationViews() {
        final List<DesktopVisualAuditScenario> scenarios = DesktopVisualAuditScenario.defaults("target/audit");

        assertTrue(scenarios.size() >= 19);
        assertTrue(contains(
            scenarios,
            "dense-spectrum",
            "Visual Circuit"
        ));
        assertTrue(contains(
            scenarios,
            "qft16",
            "Q-Sphere"
        ));
        assertTrue(contains(
            scenarios,
            "chemistry16",
            "Q-Sphere"
        ));
        assertTrue(contains(
            scenarios,
            "grover16",
            "Visual Circuit"
        ));
        assertTrue(contains(
            scenarios,
            "bell",
            "Simulation"
        ));
        assertTrue(contains(
            scenarios,
            "bell",
            "Native JSON"
        ));
        assertTrue(contains(
            scenarios,
            "bell",
            "Generated Export"
        ));
        assertTrue(contains(
            scenarios,
            "dense-spectrum",
            "Full IR Surface"
        ));
        assertTrue(contains(
            scenarios,
            "grover16",
            "Preflight"
        ));
        assertTrue(contains(
            scenarios,
            "grover16",
            "Compatibility"
        ));
        assertTrue(contains(
            scenarios,
            "bell",
            "External Formats"
        ));
        final HashSet<String> names = new HashSet<>();
        final HashSet<String> outputPaths = new HashSet<>();
        for (int i = 0; i < scenarios.size(); i++) {
            assertTrue(names.add(scenarios.get(i).name()));
            assertTrue(outputPaths.add(scenarios.get(i).outputPath()));
            assertTrue(scenarios.get(i).outputPath().startsWith("target"));
            assertTrue(scenarios.get(i).outputPath().endsWith(".png"));
        }
    }

    private static boolean contains(
        final List<DesktopVisualAuditScenario> scenarios,
        final String template,
        final String tab
    ) {
        for (int i = 0; i < scenarios.size(); i++) {
            final DesktopVisualAuditScenario scenario = scenarios.get(i);
            if (
                template.equals(scenario.fixtureName())
                && tab.equals(scenario.tabName())
            ) {
                return true;
            }
        }
        return false;
    }
}