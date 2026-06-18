/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.visualization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;

final class SimulationChartProjectionTest {

    @Test
    void normalizesMeasurementRowsAndSortsByProbability() {
        final List<MeasurementProbabilityRow> rows = new SimulationChartProjection().measurementRows(
            SimulationResult.of(
                2,
                2,
                10,
                Map.of(
                    "11",
                    3L,
                    "00",
                    7L
                ),
                List.of(),
                List.of()
            ),
            false
        );

        assertEquals(
            "00",
            rows.get(0).basisState()
        );
        assertEquals(
            0.7,
            rows.get(0).probability(),
            1.0e-12
        );
        assertEquals(
            "11",
            rows.get(1).basisState()
        );
        assertEquals(
            0.3,
            rows.get(1).probability(),
            1.0e-12
        );
    }

    @Test
    void computesStateVectorMagnitudeProbabilityAndPhase() {
        final List<StateVectorDisplayRow> rows = new SimulationChartProjection().stateVectorRows(
            SimulationResult.of(
                1,
                0,
                0,
                Map.of(),
                List.of(new StateVectorAmplitude(
                    "1",
                    0.0,
                    1.0
                )),
                List.of()
            ),
            false
        );

        assertEquals(
            1,
            rows.size()
        );
        assertEquals(
            1.0,
            rows.get(0).magnitude(),
            1.0e-12
        );
        assertEquals(
            1.0,
            rows.get(0).probability(),
            1.0e-12
        );
        assertEquals(
            Math.PI / 2.0,
            rows.get(0).phase(),
            1.0e-12
        );
    }
}