/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnostic;
import ru.pathcreator.vadim.quantum.application.simulation.diagnostic.SimulationDiagnosticCode;
import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;

/**
 * Проверяет текстовое представление симуляции без запуска JavaFX.
 */
final class DesktopSimulationTextRendererTest {

    @Test
    void labelsCollapsedStateVectorSeparatelyFromMeasurementFrequencies() {
        final SimulationResult result = SimulationResult.of(
            3,
            3,
            1024,
            Map.of(
                "000",
                525L,
                "111",
                499L
            ),
            List.of(new StateVectorAmplitude(
                "000",
                1.0,
                0.0
            )),
            List.of(SimulationDiagnostic.warning(
                SimulationDiagnosticCode.STATE_VECTOR_AFTER_MEASUREMENT,
                "Captured state vector is the first deterministic trajectory after measurement collapse.",
                0,
                SimulationDiagnostic.NO_INDEX
            ))
        );

        final String text = new DesktopSimulationTextRenderer().render(
            result,
            true
        );

        assertTrue(text.contains("Measurement frequencies"));
        assertTrue(text.contains("000: 0.5126953125"));
        assertTrue(text.contains("111: 0.4873046875"));
        assertTrue(text.contains("Collapsed trajectory probabilities"));
        assertFalse(text.contains("Exact probabilities"));
    }
}