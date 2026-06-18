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

final class PhaseDiskProjectionTest {

    @Test
    void projectsPurePlusStateToEquatorialDisk() {
        final List<SingleQubitPhaseDiskState> states = new PhaseDiskProjection().project(SimulationResult.of(
            1,
            0,
            0,
            Map.of(),
            List.of(
                new StateVectorAmplitude(
                    "0",
                    Math.sqrt(0.5),
                    0.0
                ),
                new StateVectorAmplitude(
                    "1",
                    Math.sqrt(0.5),
                    0.0
                )
            ),
            List.of()
        ), 8);

        assertEquals(
            1,
            states.size()
        );
        assertEquals(
            0.5,
            states.get(0).oneProbability(),
            1.0e-12
        );
        assertEquals(
            0.0,
            states.get(0).phase(),
            1.0e-12
        );
        assertEquals(
            1.0,
            states.get(0).purity(),
            1.0e-12
        );
    }

    @Test
    void projectsBellStateQubitsAsMaximallyMixedLocalStates() {
        final List<SingleQubitPhaseDiskState> states = new PhaseDiskProjection().project(SimulationResult.of(
            2,
            0,
            0,
            Map.of(),
            List.of(
                new StateVectorAmplitude(
                    "00",
                    Math.sqrt(0.5),
                    0.0
                ),
                new StateVectorAmplitude(
                    "11",
                    Math.sqrt(0.5),
                    0.0
                )
            ),
            List.of()
        ), 8);

        assertEquals(
            2,
            states.size()
        );
        for (int i = 0; i < states.size(); i++) {
            assertEquals(
                i,
                states.get(i).qubitIndex()
            );
            assertEquals(
                0.5,
                states.get(i).oneProbability(),
                1.0e-12
            );
            assertEquals(
                0.0,
                states.get(i).purity(),
                1.0e-12
            );
        }
    }

    @Test
    void respectsQubitRenderLimit() {
        final List<SingleQubitPhaseDiskState> states = new PhaseDiskProjection().project(SimulationResult.of(
            3,
            0,
            0,
            Map.of(),
            List.of(new StateVectorAmplitude(
                "000",
                1.0,
                0.0
            )),
            List.of()
        ), 2);

        assertEquals(
            2,
            states.size()
        );
        assertEquals(
            0,
            states.get(0).qubitIndex()
        );
        assertEquals(
            1,
            states.get(1).qubitIndex()
        );
    }
}